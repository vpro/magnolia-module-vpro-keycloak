/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.LogoutFilter;
import info.magnolia.init.MagnoliaConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;

import nl.vpro.magnolia.module.keycloak.KeycloakService;
import nl.vpro.magnolia.module.keycloak.util.SSLTerminatedRequestWrapper;

/**
 * @author rico
 */
@Slf4j
public class KeycloakLogoutFilter extends LogoutFilter {
    // used in /server/filters/logout

    private final MagnoliaConfigurationProperties properties;

    private final KeycloakService keycloakService;

    @Inject
    public KeycloakLogoutFilter(MagnoliaConfigurationProperties properties, KeycloakService keycloakService) {
        this.properties = properties;
        this.keycloakService = keycloakService;
    }

    @Override
    protected String resolveLogoutRedirectLink(HttpServletRequest request) {
        OIDCServletHttpFacade facade = new OIDCServletHttpFacade(new SSLTerminatedRequestWrapper(request), null);
        final AdapterDeploymentContext deploymentContext = keycloakService.getDeploymentContext();
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        String logoutUrl = properties.getProperty("keycloak.logout.url");
        if (deployment == null || !deployment.isConfigured()) {
            log.warn("deployment not configured");
        } else {
            final String logoutUrlKey = "keycloak." + deployment.getRealm() + ".logout.url";
            if (properties.hasProperty(logoutUrlKey)) {
                logoutUrl = properties.getProperty(logoutUrlKey);
            }
        }
        String encodedURI = null;

        try {
            encodedURI = URLEncoder.encode(String.valueOf(request.getRequestURL()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Can not encode url {}", encodedURI);
        }
        return logoutUrl + "?redirect_uri=" + encodedURI;
    }
}
