/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak;

import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.module.ModuleLifecycle;
import info.magnolia.module.ModuleLifecycleContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import nl.vpro.magnolia.module.keycloak.config.KeycloakConfiguration;

/**
 * @author rico
 * @date 08/06/2017
 */
@Slf4j
public class KeycloakModule implements ModuleLifecycle {

    @Getter
    @Setter
    private Map<String, KeycloakConfiguration> realms = new HashMap<>();

    private String defaultRealm;

    private final MagnoliaConfigurationProperties configurationProperties;

    @Inject
    public KeycloakModule(MagnoliaConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }


    @Override
    public void start(ModuleLifecycleContext moduleLifecycleContext) {
    }

    @Override
    public void stop(ModuleLifecycleContext moduleLifecycleContext) {
    }

    public String getDefaultRealm() {
        return defaultRealm != null ? defaultRealm : configurationProperties.getProperty("keycloak.realm");
    }

    public void setDefaultRealm(String defaultRealm) {
        this.defaultRealm = defaultRealm;
    }
}
