/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rico
 * @date 07/07/2017
 */
@Singleton
@Slf4j
public class HttpSessionInMemoryIdMapper implements SessionIdMapper, SessionStore {
    private SessionIdMapper idMapper = new InMemorySessionIdMapper();
    private final Map<String, HttpSession> httpSessions = new ConcurrentHashMap<>();

    @Inject
    public HttpSessionInMemoryIdMapper() {
        log.debug("Constructed instance {}", this);
    }

    @Override
    public boolean hasSession(String id) {
        return idMapper.hasSession(id);
    }

    @Override
    public void clear() {
        idMapper.clear();
        httpSessions.values().forEach(HttpSession::invalidate);
    }

    @Override
    public Set<String> getUserSessions(String principal) {
        return idMapper.getUserSessions(principal);
    }

    @Override
    public String getSessionFromSSO(String sso) {
        return idMapper.getSessionFromSSO(sso);
    }

    @Override
    public void map(String sso, String principal, String session) {
        log.debug("Mapped (added) session {} {} {}", sso, principal, session);
        idMapper.map(sso, principal, session);
    }

    @Override
    public void removeSession(String session) {
        idMapper.removeSession(session);
        log.debug("Removed session {}", session);
        // TODO session is the sso session id not the http session id
        HttpSession httpSession = httpSessions.get(session);
        if (httpSession != null) {
            log.debug("Invalidated session {}", httpSession.getId());
            httpSession.invalidate();
        }
    }

    public void addSession(HttpSession session) {
        httpSessions.put(session.getId(), session);
        log.debug("Added session to store {}", session.getId());
    }

    public void removeSession(HttpSession session) {
        httpSessions.remove(session.getId());
        log.debug("Removed session from store {}", session.getId());
    }

    @Override
    public HttpSession getSession(String sessionId) {
        return httpSessions.get(sessionId);
    }
}
