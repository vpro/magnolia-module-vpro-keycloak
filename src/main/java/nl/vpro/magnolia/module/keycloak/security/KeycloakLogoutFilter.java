/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.LogoutFilter;
import info.magnolia.init.MagnoliaConfigurationProperties;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author rico
 * @date 08/06/2017
 */
@Slf4j
public class KeycloakLogoutFilter extends LogoutFilter {
    // used in /server/filters/logout

    private final MagnoliaConfigurationProperties properties;

    @Inject
    public KeycloakLogoutFilter(MagnoliaConfigurationProperties properties) {
        this.properties = properties;
    }

    @Override
    protected String resolveLogoutRedirectLink(HttpServletRequest request) {
        String encodedURI= null;
        try {
            encodedURI = URLEncoder.encode(String.valueOf(request.getRequestURL()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Can not encode url {}", encodedURI);
        }
        return properties.getProperty("keycloak.logout.url")+"?redirect_uri="+ encodedURI;
    }
}
