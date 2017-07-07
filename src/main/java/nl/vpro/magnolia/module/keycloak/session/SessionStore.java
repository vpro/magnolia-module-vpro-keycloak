/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import javax.servlet.http.HttpSession;

/**
 * @author rico
 * @date 07/07/2017
 */
public interface SessionStore {

    void addSession(HttpSession session);

    void removeSession(HttpSession session);

    HttpSession getSession(String sessionId);
}
