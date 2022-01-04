/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import info.magnolia.cms.security.User;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.servlet.http.HttpSession;

import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;

/**
 * @author rico
 */
@Singleton
@Slf4j
public class HttpSessionInMemoryIdMapper implements SessionIdMapper, SessionStore {
    private final SessionIdMapper idMapper = new InMemorySessionIdMapper();
    private final Map<String, HttpSession> httpSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPrincipal = new ConcurrentHashMap<>();


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
        sessionToPrincipal.clear();
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
        sessionToPrincipal.put(session, principal);
        idMapper.map(sso, principal, session);
        log.debug("Mapped (added) session {} {} {}", sso, principal, session);
    }

    @Override
    public void removeSession(String session) {
        idMapper.removeSession(session);
        String principal = sessionToPrincipal.get(session);
        if (principal != null) {
            sessionsForPrincipal(principal).forEach(httpSession -> {
                httpSession.invalidate();
                idMapper.removeSession(httpSession.getId());
                log.debug("Invalidated session {}", httpSession.getId());
            });
        }
        HttpSession httpSession = httpSessions.get(session);
        if (httpSession != null) {
            log.debug("Invalidated session {}", httpSession.getId());
            httpSession.invalidate();
        }
        sessionToPrincipal.remove(session);
        log.debug("Removed session {}", session);
    }

    private Collection<HttpSession> sessionsForPrincipal(String principal) {
        return httpSessions.values().stream().filter(session -> principal.equals(principalForSession(session))).collect(Collectors.toList());
    }

    private String principalForSession(HttpSession session) {
        javax.security.auth.Subject subject = (Subject) session.getAttribute("javax.security.auth.Subject");
        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal instanceof User) {
                    return principal.getName();
                }
            }
        }
        return null;
    }

    // Session store methods

    @Override
    public void addSession(HttpSession session) {
        final String sessionId = session.getId();
        httpSessions.put(sessionId, session);
        log.debug("Added session to store {}", sessionId);
    }

    @Override
    public void removeSession(HttpSession session) {
        final String sessionId = session.getId();
        httpSessions.remove(sessionId);
        log.debug("Removed session from store {}", session.getId());
    }

    @Override
    public HttpSession getSession(String sessionId) {
        return httpSessions.get(sessionId);
    }
}
