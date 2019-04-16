/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.config;

import info.magnolia.init.PropertySource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;

import static org.apache.commons.text.StringSubstitutor.*;

/**
 * @author r.jansen
 */
@Slf4j
public class MagnoliaRealmPropertyResolver {

    private static final String PREFIX = "keycloak.";

    public static InputStream resolve(String realm, PropertySource properties, InputStream in) {
        try {
            StringSubstitutor substitutor = new StringSubstitutor(key -> {
                if (key.startsWith(PREFIX)) {
                    String field = StringUtils.substringAfter(key, PREFIX);
                    String realmKey = PREFIX + realm + "." + field;
                    if (properties.hasProperty(realmKey)) {
                        return properties.getProperty(realmKey);
                    }
                }
                return properties.getProperty(key);
            }, DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ESCAPE);
            return IOUtils.toInputStream(substitutor.replace(IOUtils.toString(in)));
        } catch (IOException e) {
            log.error("Could not load inputstream {}", in);
            return null;
        }
    }
}
