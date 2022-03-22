/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.config;

import info.magnolia.voting.voters.VoterSet;

import lombok.Getter;

import lombok.Setter;

import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author r.jansen
 */
@Getter
@Setter
public class KeycloakConfiguration {
    private String realmName;

    private String jaasChain;

    private VoterSet<HttpFacade.Request> voters;

}
