/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak;

import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.objectfactory.Components;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.keycloak.adapters.*;
import org.keycloak.adapters.spi.SessionIdMapper;

import nl.vpro.magnolia.module.keycloak.util.MagnoliaPropertyResolver;

/**
 * @author rico
 * @date 08/06/2017
 */
@Singleton
@Slf4j
public class KeycloakService {

    private final MagnoliaConfigurationProperties magnoliaConfiguration;
    private final SessionIdMapper idMapper;

    private AdapterDeploymentContext deploymentContext;
    private NodesRegistrationManagement nodesRegistrationManagement;

    private int sslPort = 443;

    @Inject
    public KeycloakService(MagnoliaConfigurationProperties magnoliaConfiguration, SessionIdMapper idMapper) {
        this.magnoliaConfiguration = magnoliaConfiguration;
        this.idMapper = idMapper;
        init();
    }

    private void init() {
        String configResolverClass = magnoliaConfiguration.getProperty("keycloak.config.resolver");
        if (configResolverClass != null && !configResolverClass.isEmpty()) {
            try {
                final Object resolverObject = Components.getComponent(getClass().getClassLoader().loadClass(configResolverClass));
                deploymentContext = new AdapterDeploymentContext((KeycloakConfigResolver) resolverObject);
                log.info("Using {} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.info("The specified resolver {} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {}", new Object[]{configResolverClass, ex.getMessage()});
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            String fp = magnoliaConfiguration.getProperty("keycloak.config.file");
            InputStream is;
            if (fp != null && !fp.isEmpty()) {
                try {
                    is = new FileInputStream(fp);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                String path = "keycloak.json";
                String pathParam = magnoliaConfiguration.getProperty("keycloak.config.path");
                if (pathParam != null) path = pathParam;
                is = getClass().getClassLoader().getResourceAsStream(path);
            }
            KeycloakDeployment kd = createKeycloakDeploymentFrom(MagnoliaPropertyResolver.resolve(magnoliaConfiguration, is));
            deploymentContext = new AdapterDeploymentContext(kd);
            log.debug("Keycloak is using a per-deployment configuration.");
        }
        String property = null;
        try {
            property = magnoliaConfiguration.getProperty("keycloak.thisServer.sslPort");
            sslPort = Integer.valueOf(property);
        } catch (NumberFormatException nfe) {
            log.warn("Can not parse ssl port value {}, using default of 443", property);
        }
        // I can't find any usage of the data that is set here on te servlet context.
        // filterConfig.getServletContext().setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        nodesRegistrationManagement = new NodesRegistrationManagement();
    }

    private KeycloakDeployment createKeycloakDeploymentFrom(InputStream is) {
        if (is == null) {
            log.error("No adapter configuration. Keycloak is unconfigured and will deny all requests.");
            return new KeycloakDeployment();
        }

        return KeycloakDeploymentBuilder.build(is);
    }

    public AdapterDeploymentContext getDeploymentContext() {
        return deploymentContext;
    }

    public SessionIdMapper getIdMapper() {
        return idMapper;
    }

    public NodesRegistrationManagement getNodesRegistrationManagement() {
        return nodesRegistrationManagement;
    }

    public int getSslPort() {
        return sslPort;
    }
}
