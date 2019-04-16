/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.config;

import info.magnolia.voting.voters.VoterSet;

import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author r.jansen
 */
public class KeycloakConfiguration {
    private String realmName;

    private String jaasChain;

    private VoterSet<HttpFacade.Request> voters;

    public VoterSet<HttpFacade.Request> getVoters() {
        return voters;
    }

    public void setVoters(VoterSet<HttpFacade.Request> voters) {
        this.voters = voters;
    }

    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    public String getJaasChain() {
        return jaasChain;
    }

    public void setJaasChain(String jaasChain) {
        this.jaasChain = jaasChain;
    }
}
