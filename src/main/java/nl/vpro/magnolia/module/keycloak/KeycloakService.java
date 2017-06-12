/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak;

import info.magnolia.init.MagnoliaConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import nl.vpro.magnolia.module.keycloak.util.MagnoliaPropertyResolver;
import org.keycloak.adapters.*;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author rico
 * @date 08/06/2017
 */
@Singleton
@Slf4j
public class KeycloakService {

    private final MagnoliaConfigurationProperties magnoliaConfiguration;

    protected AdapterDeploymentContext deploymentContext;
    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();
    protected NodesRegistrationManagement nodesRegistrationManagement;

    @Inject
    public KeycloakService(MagnoliaConfigurationProperties magnoliaConfiguration) {
        this.magnoliaConfiguration = magnoliaConfiguration;
        init();
    }

    private void init() {
        String configResolverClass = magnoliaConfiguration.getProperty("keycloak.config.resolver");
        if (configResolverClass != null) {
            try {
                KeycloakConfigResolver configResolver = (KeycloakConfigResolver) getClass().getClassLoader().loadClass(configResolverClass).newInstance();
                deploymentContext = new AdapterDeploymentContext(configResolver);
                log.info("Using {} to resolve Keycloak configuration on a per-request basis.", configResolverClass);
            } catch (Exception ex) {
                log.info( "The specified resolver {} could NOT be loaded. Keycloak is unconfigured and will deny all requests. Reason: {}", new Object[]{configResolverClass, ex.getMessage()});
                deploymentContext = new AdapterDeploymentContext(new KeycloakDeployment());
            }
        } else {
            String fp = magnoliaConfiguration.getProperty("keycloak.config.file");
            InputStream is;
            if (fp != null) {
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
        // Ik kan in de keycloak source code geen echt gebruik vinden van de data die hier in de servlet context wordt gezet.
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
}
