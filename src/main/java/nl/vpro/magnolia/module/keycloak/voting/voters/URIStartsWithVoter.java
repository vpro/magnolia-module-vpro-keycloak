/*
 * Copyright (C) 2019 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.magnolia.module.keycloak.voting.voters;

import info.magnolia.cms.util.ServletUtil;
import info.magnolia.context.MgnlContext;
import info.magnolia.voting.voters.AbstractBoolVoter;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author r.jansen
 */
public class URIStartsWithVoter extends AbstractBoolVoter<HttpFacade.Request> {
    @Getter
    @Setter
    private String pattern = StringUtils.EMPTY;

    @Override
    protected boolean boolVote(HttpFacade.Request request) {
        String uri = getURI(request);
        if (StringUtils.isEmpty(uri)) {
            return false;
        }
        return uri.startsWith(this.getPattern());
    }

    private String getURI(Object value) {
        if (MgnlContext.hasInstance()) {
            return MgnlContext.getAggregationState().getCurrentURI();
        } else {
            HttpFacade.Request request = (HttpFacade.Request) value;
            return ServletUtil.stripPathParameters(request.getRelativePath());
        }
    }


}
