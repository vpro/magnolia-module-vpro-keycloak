/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.ExternalUser;
import info.magnolia.cms.security.auth.GroupList;
import info.magnolia.cms.security.auth.RoleList;

import java.util.Map;

/**
 * @author r.jansen
 */
public class KeycloakUser extends ExternalUser {

    protected KeycloakUser(Map<String, String> properties, GroupList groupList, RoleList roleList) {
        super(properties, groupList, roleList);
    }
}
