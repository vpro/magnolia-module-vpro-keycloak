/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.security;

import info.magnolia.cms.security.SecuritySupport;
import info.magnolia.cms.security.User;
import info.magnolia.cms.security.UserManager;
import info.magnolia.jaas.sp.AbstractLoginModule;
import info.magnolia.jaas.sp.UserAwareLoginModule;
import info.magnolia.objectfactory.Components;

import javax.security.auth.login.LoginException;

/**
 * @author r.jansen
 */
public class KeycloakLoginModule extends AbstractLoginModule implements UserAwareLoginModule {

    private User user;

    @Override
    public void validateUser() throws LoginException {
        // Not used in this implementation
    }

    @Override
    public void setACL() {
        // Not used in this implementation
    }

    @Override
    public void setEntity() {
        this.subject.getPrincipals().add(getUser());
        this.subject.getPrincipals().add(this.realm);

        for (String group : this.getUser().getAllGroups()) {
            addGroupName(group);
        }

        for (String role : this.getUser().getAllRoles()) {
            addRoleName(role);
        }
    }

    @Override
    public User getUser() {
        if (this.user == null) {
            final UserManager userManager = Components.getComponent(SecuritySupport.class).getUserManager(realm.getName());
            if (userManager == null) {
                throw new IllegalArgumentException(String.format("No UserManager found for realm %s", realm.getName()));
            }
            this.user = userManager.getUser(name);
        }
        return this.user;
    }
}
