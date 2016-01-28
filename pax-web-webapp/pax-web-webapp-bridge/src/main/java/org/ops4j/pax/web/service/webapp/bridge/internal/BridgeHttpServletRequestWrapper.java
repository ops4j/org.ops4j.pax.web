package org.ops4j.pax.web.service.webapp.bridge.internal;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * Created by loom on 25.01.16.
 */
public class BridgeHttpServletRequestWrapper extends HttpServletRequestWrapper {

    public static final String PAX_WEB_CONTEXT_SESSION_PREFIX = "org.ops4j.pax.web.service.webapp.bridge.internal.session";

    private BridgeServletContext bridgeServletContext;

    private String contextPath;
    private String servletPath;
    private String pathInfo;
    private String queryString;

    public BridgeHttpServletRequestWrapper(HttpServletRequest request, BridgeServletContext bridgeServletContext, String contextPath, String servletPath, String pathInfo, String queryString) {
        super(request);
        this.bridgeServletContext = bridgeServletContext;
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public String getRequestURI() {
        return getContextPath() + getServletPath() + getPathInfo();
    }

    @Override
    public StringBuffer getRequestURL() {
        return super.getRequestURL();
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        HttpSession parentSession = super.getSession(create);
        if (parentSession instanceof BridgeHttpSession) {
            return parentSession;
        } else {
            BridgeHttpSession bridgeHttpSession = (BridgeHttpSession) parentSession.getAttribute(PAX_WEB_CONTEXT_SESSION_PREFIX + bridgeServletContext.getContextModel().getContextName());
            if (bridgeHttpSession == null && create) {
                bridgeHttpSession = new BridgeHttpSession(parentSession, bridgeServletContext);
                parentSession.setAttribute(PAX_WEB_CONTEXT_SESSION_PREFIX + bridgeServletContext.getContextModel().getContextName(), bridgeHttpSession);
            }
            return bridgeHttpSession;
        }
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        int queryStringPos = path.indexOf("?");
        String queryString = null;
        if (queryStringPos > -1) {
            queryString = path.substring(queryStringPos+1);
            path = path.substring(0, queryStringPos);
        }
        return new BridgePathRequestDispatcher(getContextPath() + path, queryString, bridgeServletContext.getBridgeServer());
    }

    @Override
    public String getQueryString() {
        if (queryString == null) {
            return super.getQueryString();
        } else {
            return queryString;
        }
    }
}
