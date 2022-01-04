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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.vpro.magnolia.jsr107.CreateConfigurationTasks;
import nl.vpro.magnolia.module.keycloak.security.KeycloakClientCallback;
import nl.vpro.magnolia.module.keycloak.security.KeycloakLoginHandler;
import nl.vpro.magnolia.module.keycloak.security.KeycloakLogoutFilter;
import nl.vpro.magnolia.module.keycloak.security.KeycloakUserManager;

/**
 * @author rico
 */
public class KeycloakModuleVersionHandler extends DefaultModuleVersionHandler {

    public static final String PROPERTY_CLASS = "class";
    private final Task setLogoutFilterClass = new CheckAndModifyPropertyValueTask("/server/filters/logout", PROPERTY_CLASS, LogoutFilter.class.getName(), KeycloakLogoutFilter.class.getName());

    public KeycloakModuleVersionHandler() {
        register(DeltaBuilder.update("1.0", "")
            .addTask(setLogoutFilterClass)
        );
        register(DeltaBuilder.update("1.2", "")
            .addTasks(getCacheTasks())
        );
    }

    @Override
    protected List<Task> getExtraInstallTasks(InstallContext installContext) {
        List<Task> tasks = new ArrayList<>();
        tasks.addAll(baseInstallTasks());
        tasks.addAll(getCacheTasks());
        return tasks;
    }

    private List<Task> baseInstallTasks() {
        return Arrays.asList(
            setLogoutFilterClass,
            new CreateNodeTask("", "/server/filters/login/loginHandlers", "keycloak", NodeTypes.ContentNode.NAME),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/login/loginHandlers/keycloak", "jaasChain", "magnolia-sso"),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/login/loginHandlers/keycloak", PROPERTY_CLASS, KeycloakLoginHandler.class.getName()),
            new CreateNodeTask("", "/server/filters/securityCallback/clientCallbacks", "keycloak", NodeTypes.ContentNode.NAME),
            new OrderNodeBeforeTask("/server/filters/securityCallback/clientCallbacks/keycloak", "form"),
            new SetPropertyTask(RepositoryConstants.CONFIG, "/server/filters/securityCallback/clientCallbacks/keycloak", PROPERTY_CLASS, KeycloakClientCallback.class.getName())
        );
    }

    private List<Task> getCacheTasks() {
        return new ArrayList<>(CreateConfigurationTasks.createConfigurationTasks(KeycloakUserManager.class));
    }
}
