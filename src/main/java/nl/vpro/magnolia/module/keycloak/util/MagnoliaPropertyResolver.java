/*
 * Copyright (C) 2017 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.util;

import info.magnolia.init.MagnoliaConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author rico
 * @date 12/06/2017
 */
@Slf4j
public class MagnoliaPropertyResolver {

    public static InputStream resolve(MagnoliaConfigurationProperties properties, InputStream in) {
        StrSubstitutor substitutor = new StrSubstitutor(new StrLookup<String>() {
            @Override
            public String lookup(String key) {
                return properties.getProperty(key);
            }
        });
        try {
            return IOUtils.toInputStream(substitutor.replace(IOUtils.toString(in)));
        } catch (IOException e) {
            log.error("Could not load inputstream {}", in);
            return null;
        }
    }
}
