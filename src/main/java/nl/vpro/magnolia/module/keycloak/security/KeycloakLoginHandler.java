/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.PrincipalUtil;
import info.magnolia.cms.security.User;
import info.magnolia.cms.security.auth.callback.CredentialsCallbackHandler;
import info.magnolia.cms.security.auth.callback.PlainTextCallbackHandler;
import info.magnolia.cms.security.auth.login.LoginHandlerBase;
import info.magnolia.cms.security.auth.login.LoginResult;
import info.magnolia.i18nsystem.SimpleTranslator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.http.*;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.*;
import org.keycloak.adapters.servlet.*;
import org.keycloak.adapters.spi.*;

import nl.vpro.magnolia.module.keycloak.KeycloakModule;
import nl.vpro.magnolia.module.keycloak.KeycloakService;
import nl.vpro.magnolia.module.keycloak.config.KeycloakConfiguration;
import nl.vpro.magnolia.module.keycloak.session.PrincipalSessionStore;
import nl.vpro.magnolia.module.keycloak.util.SSLTerminatedRequestWrapper;

/**
 * @author rico
 */
@Slf4j
public class KeycloakLoginHandler extends LoginHandlerBase {
    // used in : /server/filters/login/loginHandlers

    public static final String KEYCLOAK_LOGIN_HANDLER_USED = "keycloakLoginHandlerUsed";

    private final KeycloakService keycloakService;

    private final KeycloakModule keycloakModule;

    private final SimpleTranslator simpleTranslator;

    private final PrincipalSessionStore principalSessionStore;

    @Getter
    @Setter
    private String jaasChain = "magnolia";

    @Inject
    public KeycloakLoginHandler(KeycloakService keycloakService, KeycloakModule keycloakModule, SimpleTranslator simpleTranslator, PrincipalSessionStore principalSessionStore) {
        this.keycloakService = keycloakService;
        this.keycloakModule = keycloakModule;
        this.simpleTranslator = simpleTranslator;
        this.principalSessionStore = principalSessionStore;
    }

    @Override
    public LoginResult handle(HttpServletRequest request, HttpServletResponse response) {
        // Setup configuration
        OIDCServletHttpFacade facade = new OIDCServletHttpFacade(new SSLTerminatedRequestWrapper(request), response);
        final AdapterDeploymentContext deploymentContext = keycloakService.getDeploymentContext();
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.warn("deployment not configured");
            return LoginResult.NOT_HANDLED;
        }
        final SessionIdMapper idMapper = keycloakService.getIdMapper();

        // Handle the admin calls from keycloak
        PreAuthActionsHandler preActions = getPreAuthActionsHandler(idMapper, deploymentContext, facade);

        // Execute admin calls
        if (preActions.handleRequest()) {
            // We handled the keycloak admin calls, request is done.
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
        }
        updateSessionAndIdMapper(request, deployment, idMapper);

        // Check and refresh current session
        keycloakService.getNodesRegistrationManagement().tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, keycloakService.getSslPort());

        AuthOutcome outcome = authenticator.authenticate();
        if (outcome != AuthOutcome.AUTHENTICATED) {
            return LoginResult.NOT_HANDLED;
        }
        return handleAuthenticated(deployment, request, response, facade);
    }

    private PreAuthActionsHandler getPreAuthActionsHandler(final SessionIdMapper idMapper,
                                                           final AdapterDeploymentContext deploymentContext,
                                                           final OIDCServletHttpFacade facade) {
        return new PreAuthActionsHandler(new UserSessionManagement() {
            @Override
            public void logoutAll() {
                if (idMapper != null) {
                    idMapper.clear();
                }
            }

            @Override
            public void logoutHttpSessions(List<String> ids) {
                log.debug("logoutHttpSessions");
                for (String id : ids) {
                    log.debug("removed idMapper: " + id);
                    idMapper.removeSession(id);
                }
            }
        }, deploymentContext, facade);
    }

    /**
     * Restore keycloak session information into the current session.
     * We have to do this as LoginFilter invalidates the session on which this information is stored.
     */
    private void updateSessionAndIdMapper(final HttpServletRequest request, final KeycloakDeployment deployment,
                                          final SessionIdMapper idMapper) {
        Optional.ofNullable(request.getSession(false)).ifPresent(session -> getSubject(session)
            .flatMap(subject -> Optional.ofNullable(PrincipalUtil.findPrincipal(subject, User.class)))
            .ifPresent(principal ->
                // Get account from session ...
                Optional.ofNullable(getAccount(session)
                        // .. and otherwise from the principal session store
                        .orElseGet(() -> principalSessionStore.get(principal.getName(), deployment.getRealm())))
                    // If account was found in either session or principal session store, handle idmapper-actions
                    .ifPresent(account -> {
                        session.setAttribute(KeycloakAccount.class.getName(), account);
                        session.setAttribute(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
                        // Update the idMapper, otherwise keycloak-attributes will not be available in the session-object
                        if (!idMapper.hasSession(session.getId())) {
                            idMapper.map(account.getKeycloakSecurityContext().getToken().getSessionState(), principal.getName(), session.getId());
                        }
                    })
            )
        );
    }

    /**
     * If the user is (being) authenticated, get and set the right session- and Keycloak-properties
     */
    private LoginResult handleAuthenticated(final KeycloakDeployment deployment,
                                            final HttpServletRequest request, final HttpServletResponse response,
                                            final OIDCServletHttpFacade facade) {
        return Optional.ofNullable(request.getSession(false))
            .flatMap(session -> getAccount(session)
                .map(account -> getSubject(session)
                    // If we are already authenticated AND registered to the principal session store, we shouldn't try to authenticate again,
                    // because it will make Magnolia invalidate the session
                    .filter(subject -> principalSessionStore.get(account.getPrincipal().getName(), deployment.getRealm()) != null)
                    .map(principal -> LoginResult.NOT_HANDLED)
                    .orElseGet(() -> {
                        log.debug("AUTHENTICATED");
                        // Add account to session store
                        principalSessionStore.add(account.getPrincipal().getName(), deployment.getRealm(), account);
                        String tokenUrl = account.getKeycloakSecurityContext().getToken().getIssuer();
                        String deploymentUrl = deployment.getRealmInfoUrl();
                        if (!Objects.equals(tokenUrl, deploymentUrl)) {
                            log.error("Deployment is not correct for this token: {} , {}", tokenUrl, deploymentUrl);
                            log.error("Incoming url is : {} ", request.getRequestURL() + "?" + request.getQueryString());
                        }
                        return null;
                    })
                )
            ).orElseGet(() -> handleInitialLogin(deployment, request, response, facade));
    }

    /**
     * Handle the initial login for every new session
     */
    private LoginResult handleInitialLogin(final KeycloakDeployment deployment, final HttpServletRequest request,
                                           final HttpServletResponse response, final OIDCServletHttpFacade facade) {
        if (facade.isEnded()) {
            return jaasAuthenticate(request, response, deployment);
        }
        AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
        if (actions.handledRequest()) {
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
        } else {
            return jaasAuthenticate(request, response, deployment);
        }
    }

    private Optional<Subject> getSubject(@Nonnull final HttpSession session) {
        return Optional.ofNullable((Subject) session.getAttribute(Subject.class.getName()));
    }

    private Optional<OIDCFilterSessionStore.SerializableKeycloakAccount> getAccount(@Nonnull final HttpSession session) {
        return Optional.ofNullable((OIDCFilterSessionStore.SerializableKeycloakAccount) session.getAttribute(KeycloakAccount.class.getName()));
    }

    private LoginResult jaasAuthenticate(HttpServletRequest request, HttpServletResponse response, KeycloakDeployment keycloakDeployment) {
        request.setAttribute(KEYCLOAK_LOGIN_HANDLER_USED, true);

        KeycloakAccount account = (KeycloakAccount) request.getAttribute(KeycloakAccount.class.getName());
        if (account == null) {
            account = (KeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
        }
        final String principalName = account.getPrincipal().getName();

        final CredentialsCallbackHandler callbackHandler = new PlainTextCallbackHandler(principalName, "".toCharArray());

        final String configuredJaasChain = keycloakModule.getRealms().values().stream()
            .filter(config -> Objects.equals(keycloakDeployment.getRealm(), config.getRealmName()))
            .findFirst()
            .map(KeycloakConfiguration::getJaasChain)
            .filter(StringUtils::isNotEmpty)
            .orElse(getJaasChain());

        final LoginResult result = authenticate(callbackHandler, configuredJaasChain);
        if (result.getSubject() == null) {
            handleUnrecognizedUser(request, response);
            // return STATUS_IN_PROCESS so that LoginFilter will halt the filter chain
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
        }
        if (result.getStatus() == LoginResult.STATUS_SUCCEEDED && requiresRedirect(request)) {
            return new LoginResult(LoginResult.STATUS_SUCCEEDED_REDIRECT_REQUIRED, result.getSubject());
        }
        return result;
    }

    private void handleUnrecognizedUser(HttpServletRequest request, HttpServletResponse response) {
        // log them out so they can refresh after someone fixes their permissions
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        try {
            response.getWriter().write(simpleTranslator.translate("vpro-keycloak.unrecognizedUserMessage"));
        } catch (Exception e) {
            log.error("Unable to write to response body.", e);
        }
    }

    protected boolean requiresRedirect(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getQueryString())) {
            return false;
        }
        // Check for oauth parameters:  code , state and session_state
        Map<String, String[]> parameterMap = request.getParameterMap();
        return parameterMap.containsKey("code") || parameterMap.containsKey("state") || parameterMap.containsKey("session_state");
    }
}
