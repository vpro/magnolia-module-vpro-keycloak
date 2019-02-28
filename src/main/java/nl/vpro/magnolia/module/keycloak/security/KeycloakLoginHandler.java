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

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.servlet.FilterRequestAuthenticator;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.spi.UserSessionManagement;

import nl.vpro.magnolia.module.keycloak.KeycloakModule;
import nl.vpro.magnolia.module.keycloak.KeycloakService;
import nl.vpro.magnolia.module.keycloak.config.KeycloakConfiguration;
import nl.vpro.magnolia.module.keycloak.session.PrincipalSessionStore;
import nl.vpro.magnolia.module.keycloak.util.SSLTerminatedRequestWrapper;

/**
 * @author rico
 * @date 08/06/2017
 */
@Slf4j
public class KeycloakLoginHandler extends LoginHandlerBase {
    // used in : /server/filters/login/loginHandlers

    public static String KEYCLOAK_LOGIN_HANDLER_USED = "keycloakLoginHandlerUsed";

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
        PreAuthActionsHandler preActions = new PreAuthActionsHandler(new UserSessionManagement() {
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

        // Execute admin calls
        if (preActions.handleRequest()) {
            // We handled the keycloak admin calls, request is done.
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
        }

        // Restore keycloak session information into the current session.
        // We have to do this as LoginFilter invalidates the session on which this information is stored.
        final HttpSession session = request.getSession(false);
        if (session != null) {
            Subject subject = (Subject) session.getAttribute(Subject.class.getName());
            if (subject != null) {
                if (session.getAttribute(KeycloakAccount.class.getName()) == null) {
                    final User principal = PrincipalUtil.findPrincipal(subject, User.class);
                    if (principal != null) {
                        OIDCFilterSessionStore.SerializableKeycloakAccount sAccount = principalSessionStore.get(principal.getName(), deployment.getRealm());
                        if (sAccount != null) {
                            session.setAttribute(KeycloakAccount.class.getName(), sAccount);
                            session.setAttribute(KeycloakSecurityContext.class.getName(), sAccount.getKeycloakSecurityContext());
                        }
                    }
                }
            }
        }

        // Check and refresh current session
        keycloakService.getNodesRegistrationManagement().tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, keycloakService.getSslPort());
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.debug("AUTHENTICATED");
            // Store keycloak session information into the store
            // Required since LoginFilter destroys this information on login.
            if (session != null) {
                OIDCFilterSessionStore.SerializableKeycloakAccount account = (OIDCFilterSessionStore.SerializableKeycloakAccount) session.getAttribute(KeycloakAccount.class.getName());
                principalSessionStore.add(account.getPrincipal().getName(), deployment.getRealm(), account);
            }
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

        return LoginResult.NOT_HANDLED;
    }

    private LoginResult jaasAuthenticate(HttpServletRequest request, HttpServletResponse response, KeycloakDeployment keycloakDeployment) {
        request.setAttribute(KEYCLOAK_LOGIN_HANDLER_USED, true);

        KeycloakAccount account = (KeycloakAccount) request.getAttribute(KeycloakAccount.class.getName());
        if (account == null) {
            account = (KeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
        }
        String principalName = account.getPrincipal().getName();

        CredentialsCallbackHandler callbackHandler = new PlainTextCallbackHandler(principalName, "".toCharArray());

        final String jaasChain = keycloakModule.getRealms().values().stream()
            .filter(config -> Objects.equals(keycloakDeployment.getRealm(), config.getRealmName()))
            .findFirst()
            .map(KeycloakConfiguration::getJaasChain)
            .filter(StringUtils::isNotEmpty)
            .orElse(getJaasChain());

        LoginResult result = authenticate(callbackHandler, jaasChain);
        if (result.getSubject() == null) {
            handleUnrecognizedUser(request, response);
            // return STATUS_IN_PROCESS so that LoginFilter will halt the filter chain
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
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
}
