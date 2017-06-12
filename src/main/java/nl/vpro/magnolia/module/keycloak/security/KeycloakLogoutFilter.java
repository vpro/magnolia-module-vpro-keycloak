/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.LogoutFilter;
import info.magnolia.init.MagnoliaConfigurationProperties;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * @author rico
 * @date 08/06/2017
 */
public class KeycloakLogoutFilter extends LogoutFilter {
    // used in /server/filters/logout

    private final MagnoliaConfigurationProperties properties;

    @Inject
    public KeycloakLogoutFilter(MagnoliaConfigurationProperties properties) {
        this.properties = properties;
    }

    @Override
    protected String resolveLogoutRedirectLink(HttpServletRequest request) {
        return properties.getProperty("keycloak.logout.url");
    }
}
