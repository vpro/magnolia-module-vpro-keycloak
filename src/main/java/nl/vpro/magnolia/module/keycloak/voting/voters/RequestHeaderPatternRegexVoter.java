/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.voting.voters;

import info.magnolia.voting.voters.AbstractBoolVoter;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author r.jansen
 */
public class RequestHeaderPatternRegexVoter extends AbstractBoolVoter<HttpFacade.Request> {

    @Getter
    @Setter
    private String headerName = StringUtils.EMPTY;

    @Getter
    private Pattern regex;

    public void setPattern(String pattern) {
        this.regex = Pattern.compile(pattern);
    }

    @Override
    protected boolean boolVote(HttpFacade.Request request) {
        if (regex != null && request != null) {
            String headerValue = request.getHeader(headerName);
            if (headerValue == null) {
                return false;
            }
            return regex.matcher(headerValue).matches();
        }
        return false;
    }

}
