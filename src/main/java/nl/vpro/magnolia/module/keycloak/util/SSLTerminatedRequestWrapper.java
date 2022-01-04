/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.net.HttpHeaders;

/**
 * @author rico
 */
public class SSLTerminatedRequestWrapper extends HttpServletRequestWrapper {

    public SSLTerminatedRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public boolean isSecure() {
        String protoHeader = getHeader(HttpHeaders.X_FORWARDED_PROTO);
        return (protoHeader != null && protoHeader.equalsIgnoreCase("https")) || super.isSecure();
    }
}
