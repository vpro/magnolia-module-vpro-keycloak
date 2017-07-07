/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak;

import info.magnolia.module.ModuleLifecycle;
import info.magnolia.module.ModuleLifecycleContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

/**
 * @author rico
 * @date 08/06/2017
 */
@Slf4j
public class KeycloakModule implements ModuleLifecycle {

    @Inject
    public KeycloakModule() {
    }


    @Override
    public void start(ModuleLifecycleContext moduleLifecycleContext) {
    }

    @Override
    public void stop(ModuleLifecycleContext moduleLifecycleContext) {
    }

}
