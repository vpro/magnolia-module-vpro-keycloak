/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.util;

import info.magnolia.init.PropertySource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;

import static org.apache.commons.text.StringSubstitutor.*;

/**
 * @author rico
 * @date 12/06/2017
 */
@Slf4j
public class MagnoliaPropertyResolver {

    private MagnoliaPropertyResolver() {
    }

    public static InputStream resolve(PropertySource properties, InputStream in) {
        try {
            StringSubstitutor substitutor = new StringSubstitutor(properties::getProperty, DEFAULT_PREFIX, DEFAULT_SUFFIX, DEFAULT_ESCAPE);
            return IOUtils.toInputStream(substitutor.replace(IOUtils.toString(in)));
        } catch (IOException e) {
            log.error("Could not load inputstream {}", in);
            return null;
        }
    }
}
