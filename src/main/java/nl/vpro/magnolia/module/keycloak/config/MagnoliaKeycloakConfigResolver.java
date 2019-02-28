/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.config;

import info.magnolia.init.MagnoliaConfigurationProperties;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.spi.HttpFacade;

import nl.vpro.magnolia.module.keycloak.KeycloakModule;

/**
 * @author r.jansen
 */
@Singleton
public class MagnoliaKeycloakConfigResolver implements KeycloakConfigResolver {
    private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<>();

    private final KeycloakModule keycloakModule;

    private final MagnoliaConfigurationProperties magnoliaConfiguration;

    @Inject
    public MagnoliaKeycloakConfigResolver(KeycloakModule keycloakModule, MagnoliaConfigurationProperties magnoliaConfiguration) {
        this.keycloakModule = keycloakModule;
        this.magnoliaConfiguration = magnoliaConfiguration;
    }

    @Override
    public KeycloakDeployment resolve(HttpFacade.Request request) {
        String realm = keycloakModule.getRealms().values().stream()
            .filter(keycloakConfiguration -> keycloakConfiguration.getVoters().vote(request) > 0)
            .findFirst()
            .map(KeycloakConfiguration::getRealmName)
            .orElse(keycloakModule.getDefaultRealm());

        KeycloakDeployment deployment = cache.get(realm);
        if (null == deployment) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("keycloak.json");
            if (is == null) {
                throw new IllegalStateException("Not able to find the file keycloak.json");
            }
            deployment = KeycloakDeploymentBuilder.build(MagnoliaRealmPropertyResolver.resolve(realm, magnoliaConfiguration, is));
            cache.put(realm, deployment);
        }

        return deployment;
    }
}
