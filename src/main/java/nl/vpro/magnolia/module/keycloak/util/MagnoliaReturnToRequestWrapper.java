package nl.vpro.magnolia.module.keycloak.util;

import info.magnolia.cms.security.auth.login.FormLogin;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MagnoliaReturnToRequestWrapper extends HttpServletRequestWrapper {

    public MagnoliaReturnToRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getQueryString() {
        String q = super.getQueryString();
        if (q == null || q.isEmpty()) {
            return FormLogin.PARAMETER_RETURN_TO + "=" + URLEncoder.encode(getRequestURI(), UTF_8);
        }
        return q + "&" + FormLogin.PARAMETER_RETURN_TO + "=" + URLEncoder.encode(getRequestURI() + "?" + q, UTF_8);
    }
}
