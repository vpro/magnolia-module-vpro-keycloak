/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keycloak.adapters.servlet.OIDCFilterSessionStore;

/**
 * @author r.jansen
 */
@Singleton
public class PrincipalSessionStore {
    private final Map<PrincipalKey, OIDCFilterSessionStore.SerializableKeycloakAccount> store = new ConcurrentHashMap<>();

    @Inject
    public PrincipalSessionStore() {
    }

    public void add(String subject, String realm, OIDCFilterSessionStore.SerializableKeycloakAccount a) {
        store.put(new PrincipalKey(subject, realm), a);
    }

    public boolean remove(String subject, String realm) {
        return store.remove(new PrincipalKey(subject, realm)) != null;
    }

    public OIDCFilterSessionStore.SerializableKeycloakAccount get(String subject, String realm) {
        return store.get(new PrincipalKey(subject, realm));
    }
}
