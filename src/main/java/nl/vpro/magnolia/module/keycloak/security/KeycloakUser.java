/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.ExternalUser;
import info.magnolia.cms.security.auth.GroupList;
import info.magnolia.cms.security.auth.RoleList;

import java.util.Map;
import java.util.UUID;

/**
 * @author r.jansen
 */
public class KeycloakUser extends ExternalUser {

    public static final String ID_PROP = "id";
    public static final String REALM_PROP = "realm";

    protected KeycloakUser(Map<String, String> properties, GroupList groupList, RoleList roleList) {
        super(properties, groupList, roleList);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + getName()+ " (" + getId() + "@" + getRealm() + ") " + getRoles() + " " + getGroups();
    }

    public UUID getId() {
        return UUID.fromString(getProperty(ID_PROP));
    }

    public String getRealm() {
        return getProperty(REALM_PROP);
    }
}
