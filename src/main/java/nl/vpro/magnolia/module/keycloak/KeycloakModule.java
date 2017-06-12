/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak;

import info.magnolia.init.MagnoliaConfigurationProperties;
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

    private final MagnoliaConfigurationProperties magnoliaConfiguration;

    @Inject
    public KeycloakModule(MagnoliaConfigurationProperties magnoliaConfigurationProperties) {
        magnoliaConfiguration = magnoliaConfigurationProperties;
    }


    @Override
    public void start(ModuleLifecycleContext moduleLifecycleContext) {
    }

    @Override
    public void stop(ModuleLifecycleContext moduleLifecycleContext) {
    }

}
