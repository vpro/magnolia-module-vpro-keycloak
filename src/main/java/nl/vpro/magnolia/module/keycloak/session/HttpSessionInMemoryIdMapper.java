/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import info.magnolia.cms.security.User;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
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
    private final Map<String, Set<String>> principalToDeletedSessionId = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToSso = new ConcurrentHashMap<>();


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
        sessionToSso.clear();
        principalToDeletedSessionId.clear();
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
        if (sso != null) {
            sessionToSso.put(session, sso);
        }
        idMapper.map(sso, principal, session);
    }

    @Override
    public void removeSession(String session) {
        idMapper.removeSession(session);
        log.debug("Removed session {}", session);
        HttpSession httpSession = httpSessions.get(session);
        if (httpSession != null) {
            log.debug("Invalidated session {}", httpSession.getId());
            httpSession.invalidate();
        }
        sessionToSso.remove(session);
    }

    // Session store methods

    @Override
    public void addSession(HttpSession session) {
        final String sessionId = session.getId();
        httpSessions.put(sessionId, session);
        String principal = principalFromSession(session);
        if (principal != null) {
            Set<String> sessions = idMapper.getUserSessions(principal);
            if (!sessions.contains(sessionId)) {
                final Set<String> deletedSessions = principalToDeletedSessionId.get(principal);
                if (deletedSessions != null) {
                    sessions.forEach(idmapSession -> {
                        if (deletedSessions.contains(idmapSession)) {
                            String sso=sessionToSso.get(idmapSession);
                            map(sso, principal, sessionId);
                            deletedSessions.remove(idmapSession);
                            idMapper.removeSession(idmapSession);
                            sessionToSso.remove(idmapSession);
                        }
                    });
                    if (deletedSessions.isEmpty()) {
                        principalToDeletedSessionId.remove(principal);
                    }
                }
            } else {
                principalToDeletedSessionId.remove(principal);
            }
        }
        log.debug("Added session to store {}", sessionId);
    }

    @Override
    public void removeSession(HttpSession session) {
        final String sessionId = session.getId();
        httpSessions.remove(sessionId);

        String principal = principalFromSession(session);
        final Set<String> deletedUserSessions = principalToDeletedSessionId.computeIfAbsent(principal, k -> Collections.synchronizedSet(new HashSet<>()));
        deletedUserSessions.add(sessionId);
        principalToDeletedSessionId.put(principal, deletedUserSessions);
        log.debug("Removed session from store {}", session.getId());
    }

    private String principalFromSession(HttpSession session) {
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

    @Override
    public HttpSession getSession(String sessionId) {
        return httpSessions.get(sessionId);
    }
}
