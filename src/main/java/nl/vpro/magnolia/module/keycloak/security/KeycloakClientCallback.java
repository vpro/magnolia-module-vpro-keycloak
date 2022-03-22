/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.auth.callback.AbstractHttpClientCallback;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.servlet.*;
import org.keycloak.adapters.spi.*;

import nl.vpro.magnolia.module.keycloak.KeycloakService;
import nl.vpro.magnolia.module.keycloak.util.SSLTerminatedRequestWrapper;

/**
 * @author rico
 */
@Slf4j
public class KeycloakClientCallback extends AbstractHttpClientCallback {
    // used in /server/filters/securityCallback/clientCallbacks

    private final KeycloakService keycloakService;

    @Inject
    public KeycloakClientCallback(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    @Override
    public boolean accepts(HttpServletRequest request) {
        return request.getAttribute(KeycloakLoginHandler.KEYCLOAK_LOGIN_HANDLER_USED) == null && super.accepts(request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) {
        final OIDCServletHttpFacade facade = new OIDCServletHttpFacade(new SSLTerminatedRequestWrapper(request), response);
        final AdapterDeploymentContext deploymentContext = keycloakService.getDeploymentContext();
        final KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            log.warn("deployment not configured");
            return;
        }
        final SessionIdMapper idMapper = keycloakService.getIdMapper();

        keycloakService.getNodesRegistrationManagement().tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();

        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, keycloakService.getSslPort());
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome != AuthOutcome.AUTHENTICATED) {
            AuthChallenge challenge = authenticator.getChallenge();
            if (challenge != null) {
                log.debug("challenge");
                challenge.challenge(facade);
            }
        }
    }
}

