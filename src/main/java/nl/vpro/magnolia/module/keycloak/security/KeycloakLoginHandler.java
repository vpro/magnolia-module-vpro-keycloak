/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.auth.callback.CredentialsCallbackHandler;
import info.magnolia.cms.security.auth.callback.PlainTextCallbackHandler;
import info.magnolia.cms.security.auth.login.LoginHandlerBase;
import info.magnolia.cms.security.auth.login.LoginResult;
import info.magnolia.i18nsystem.SimpleTranslator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.magnolia.module.keycloak.KeycloakService;
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

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author rico
 * @date 08/06/2017
 */
@Slf4j
public class KeycloakLoginHandler extends LoginHandlerBase {
    // used in : /server/filters/login/loginHandlers

    public static String KEYCLOAK_LOGIN_HANDLER_USED="keycloakLoginHandlerUsed";

    private final KeycloakService keycloakService;

    private final SimpleTranslator simpleTranslator;

    @Getter
    @Setter
    private String jaasChain = "magnolia";

    @Inject
    public KeycloakLoginHandler(KeycloakService keycloakService, SimpleTranslator simpleTranslator) {
        this.keycloakService = keycloakService;
        this.simpleTranslator = simpleTranslator;
    }

    @Override
    public LoginResult handle(HttpServletRequest request, HttpServletResponse response) {
        // Setup configuration
        OIDCServletHttpFacade facade = new OIDCServletHttpFacade(request, response);
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
                    logout(id);
                }

            }
        }, deploymentContext, facade);

        // Execute admin calls
        if (preActions.handleRequest()) {
            // We handled the keycloak admin calls, request is done.
            return new LoginResult(LoginResult.STATUS_IN_PROCESS);
        }

        // Check and refresh current session
        keycloakService.getNodesRegistrationManagement().tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, keycloakService.getSslPort());
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            log.debug("AUTHENTICATED");
            if (facade.isEnded()) {
                return jaasAuthenticate(request, response);
            }
            AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
            if (actions.handledRequest()) {
                return new LoginResult(LoginResult.STATUS_IN_PROCESS);
            } else {
                // Do we need this ?
                //HttpServletRequestWrapper wrapper = tokenStore.buildWrapper();
                // chain.doFilter(wrapper, res);
                return jaasAuthenticate(request, response);
            }
        }

        return LoginResult.NOT_HANDLED;
    }

    private void logout(String id) {
        // Note this is for logouts triggered by keycloak, not a logout by the magnolia environment
        // TODO invalidate a users session
        // TODO Also do magnolia logout.
/*

            if (request.getSession(false) != null) {
                request.getSession().invalidate();
            }
         */
    }

    private LoginResult jaasAuthenticate(HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute(KEYCLOAK_LOGIN_HANDLER_USED, true);

        KeycloakAccount account = (KeycloakAccount) request.getAttribute(KeycloakAccount.class.getName());
        if (account == null) {
            account = (KeycloakAccount) request.getSession().getAttribute(KeycloakAccount.class.getName());
        }
        String principalName = account.getPrincipal().getName();

        CredentialsCallbackHandler callbackHandler = new PlainTextCallbackHandler(principalName, "".toCharArray());
        LoginResult result = authenticate(callbackHandler, getJaasChain());
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
