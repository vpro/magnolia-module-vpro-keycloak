/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.setup;

import info.magnolia.cms.security.LogoutFilter;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.*;
import info.magnolia.repository.RepositoryConstants;
import nl.vpro.magnolia.module.keycloak.security.KeycloakClientCallback;
import nl.vpro.magnolia.module.keycloak.security.KeycloakLoginHandler;
import nl.vpro.magnolia.module.keycloak.security.KeycloakLogoutFilter;

import java.util.Arrays;
import java.util.List;

/**
 * @author rico
 * @date 08/06/2017
 */
public class KeycloakModuleVersionHandler extends DefaultModuleVersionHandler {

    private Task setLogoutFilterClass = new CheckAndModifyPropertyValueTask("/server/filters/logout", "class", LogoutFilter.class.getName(), KeycloakLogoutFilter.class.getName());

    public KeycloakModuleVersionHandler() {
        register(DeltaBuilder.update("1.0", "")
            .addTask(setLogoutFilterClass)
        );
    }

    @Override
    protected List<Task> getExtraInstallTasks(InstallContext installContext) {
        return baseInstallTasks();
    }

    private List<Task> baseInstallTasks() {
        return Arrays.asList(
            setLogoutFilterClass,
            new CreateNodeTask("", "/server/filters/login/loginHandlers", "keycloak", NodeTypes.ContentNode.NAME),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/login/loginHandlers/keycloak", "jaasChain", "magnolia-sso"),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/login/loginHandlers/keycloak", "class", KeycloakLoginHandler.class.getName()),
            new CreateNodeTask("", "/server/filters/securityCallback/clientCallbacks", "keycloak", NodeTypes.ContentNode.NAME),
            new OrderNodeBeforeTask("/server/filters/securityCallback/clientCallbacks/keycloak", "form"),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/securityCallback/clientCallbacks/keycloak", "class", KeycloakClientCallback.class.getName())
        );
    }
}
