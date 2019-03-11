/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.*;
import info.magnolia.cms.security.auth.Entity;
import info.magnolia.cms.security.auth.GroupList;
import info.magnolia.cms.security.auth.RoleList;
import info.magnolia.jaas.principal.GroupListImpl;
import info.magnolia.jaas.principal.RoleListImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.cache.annotation.CacheResult;
import javax.inject.Inject;
import javax.jcr.AccessDeniedException;
import javax.security.auth.Subject;

import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.representations.AccessToken;
import com.google.inject.Provider;

import nl.vpro.magnolia.jsr107.DefaultCacheSettings;
import nl.vpro.magnolia.module.keycloak.session.PrincipalSessionStore;


/**
 * @author r.jansen
 */
@Slf4j
public class KeycloakUserManager extends ExternalUserManager {
    private static final String CACHE_NAME = "keycloak-user-manager-cache";

    @Getter
    @Setter
    private String realmName;

    @Getter
    @Setter
    private String groupPrefix = "";

    @Getter
    @Setter
    private String keycloakRealm = "";

    private final Provider<PrincipalSessionStore> principalStoreProvider;

    private final Provider<SecuritySupport> securitySupportProvider;

    @Inject
    public KeycloakUserManager(Provider<PrincipalSessionStore> principalStoreProvider, Provider<SecuritySupport> securitySupportProvider) {
        this.principalStoreProvider = principalStoreProvider;
        this.securitySupportProvider = securitySupportProvider;
    }

    public User getUser(Subject subject) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Method getUser(Subject) is deprecated");
    }

    public User getUser(Map<String, String> properties, GroupList groupList, RoleList roleList) {
        throw new UnsupportedOperationException(
            "Method getUser(Map<String,String>, GroupList, RoleList) is not supported");
    }

    @Override
    public User getUser(String name) throws UnsupportedOperationException {
        return getUser(name, keycloakRealm).orElse(null);
    }

    @CacheResult(cacheName = CACHE_NAME)
    @DefaultCacheSettings(overflowToDisk = false, maxElementsInMemory = 10000, timeToLiveSeconds = 600)
    private Optional<User> getUser(String name, String realm) {
        final OIDCFilterSessionStore.SerializableKeycloakAccount keycloakAccount = principalStoreProvider.get().get(name, realm);
        return Optional.ofNullable(keycloakAccount != null ? getUser(keycloakAccount) : null);
    }

    private KeycloakUser getUser(OIDCFilterSessionStore.SerializableKeycloakAccount account) {
        Map<String, String> properties = new HashMap<>();
        final AccessToken token = account.getKeycloakSecurityContext().getToken();
        properties.put(Entity.NAME, account.getPrincipal().getName());
        properties.put(Entity.LANGUAGE, token.getLocale());
        properties.put(Entity.EMAIL, token.getEmail());
        properties.put(Entity.FULL_NAME, token.getPreferredUsername());
        properties.put("id", token.getId());

        // We map keycloak groups to roles, as that is more convenient to use
        // Then we collect all subgroups and their roles.
        GroupList groups = new GroupListImpl();
        final RoleList roles = new RoleListImpl();
        account.getRoles().stream().map(role -> groupPrefix + role).forEach(groupName -> collect(groupName, groups, roles));
        return new KeycloakUser(properties, groups, roles);
    }

    /**
     * Recursively add the group and its siblings into groupList
     * Also add all roles assigned to the group and its siblings into roleList
     */
    private void collect(String groupName, GroupList groupList, RoleList roleList) {
        GroupManager groupManager = securitySupportProvider.get().getGroupManager();
        groupList.add(groupName);
        try {
            Group group = groupManager.getGroup(groupName);
            if (group != null) {
                group.getRoles().forEach(roleList::add);
                // Deprecated: But it calls getSuperGroups which is strange. I would expect subgroups.
                // LDAPUserManager does it as well and it is working.
                groupManager.getAllGroups(groupName).forEach(subGroupName -> collect(subGroupName, groupList, roleList));
            }
        } catch (AccessDeniedException e) {
            log.error("Can not retrieve groups and roles for {} : {}", groupName, e.getMessage());
        }
    }
}
