/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.util;

import com.google.common.net.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * @author rico
 * @date 07/07/2017
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
