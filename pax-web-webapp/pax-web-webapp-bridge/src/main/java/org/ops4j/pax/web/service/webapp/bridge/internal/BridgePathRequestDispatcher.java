package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Created by loom on 18.01.16.
 */
public class BridgePathRequestDispatcher extends AbstractBridgeRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(BridgePathRequestDispatcher.class);
    private String requestURI;
    private String queryString;

    public BridgePathRequestDispatcher(String requestURI, String queryString, BridgeServer bridgeServer) {
        super(bridgeServer, false);
        this.requestURI = requestURI;
        this.queryString = queryString;
    }

    public void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException {

        String foundContextName = "";
        for (String contextName : bridgeServer.getBridgeServletContexts().keySet()) {
            if (contextName.equals("")) {
                continue;
            }
            if (requestURI.startsWith("/" + contextName)) {
                foundContextName = contextName;
                break;
            }
        }

        BridgeServletContext bridgeServletContext = bridgeServer.getContextModel(foundContextName);
        if (bridgeServletContext == null) {
            logger.error("Couldn't find a context for request=" + requestURI + " !");
            return;
        }

        final String contextPath;
        if (foundContextName.length() > 0 && !foundContextName.startsWith("/")) {
            contextPath = "/" + foundContextName;
        } else {
            contextPath = foundContextName;
        }

        BridgeServerModel.UrlPattern urlPattern = BridgeServerModel.matchPathToContext(bridgeServer.getBridgeServerModel().getServletUrlPatterns(), requestURI);
        ServletModel servletModel = null;
        BridgeServletModel bridgeServletModel = null;
        if (urlPattern != null && (urlPattern.getModel() instanceof ServletModel)) {
            servletModel = (ServletModel) urlPattern.getModel();
            bridgeServletModel = bridgeServletContext.findServlet(servletModel);
        }

        if (servletModel instanceof ResourceModel) {
            Set<String> resourcePaths = bridgeServletContext.getResourcePaths(servletModel.getName());
            // we are processing a resource that is a directory
            if (resourcePaths != null && bridgeServletContext.welcomeFiles.size() > 0) {
                String requestPath = requestURI;
                if (!requestPath.endsWith("/")) {
                    requestPath += "/";
                }
                for (WelcomeFileModel welcomeFileModel : bridgeServletContext.welcomeFiles) {
                    for (String welcomeFile : welcomeFileModel.getWelcomeFiles()) {
                        urlPattern = BridgeServerModel.matchPathToContext(bridgeServer.getBridgeServerModel().getServletUrlPatterns(), requestPath + welcomeFile);
                        if (urlPattern != null && (urlPattern.getModel() instanceof ServletModel)) {
                            // we found a match for this welcome file
                            requestURI = requestPath + welcomeFile;
                            servletModel = (ServletModel) urlPattern.getModel();
                            bridgeServletModel = bridgeServletContext.findServlet(servletModel);
                            if (bridgeServletModel != null) {
                                break;
                            }
                        }
                    }
                    if (bridgeServletModel != null) {
                        break;
                    }
                }
            }
        }

        String servletPath = null;
        String pathInfo = null;
        if (bridgeServletModel != null) {
            String matchedUrlPattern = urlPattern.getUrlPattern();
            String servletPathMatch = matchedUrlPattern.substring(contextPath.length());
            String servletPathPart = requestURI.substring(contextPath.length());
            if (servletPathMatch.endsWith("/*")) {
                servletPathPart = servletPathMatch.substring(0, servletPathMatch.length() - 2);
            } else if (servletPathMatch.contains("/*.")) {
            }
            servletPath = servletPathPart;
            pathInfo = requestURI.substring(contextPath.length() + servletPath.length());
        }

        String servletName = null;
        if (bridgeServletModel != null) {
            servletName = bridgeServletModel.getServletModel().getName();
        }
        List<BridgeFilterModel> matchingBridgeFilterModels = BridgeServerModel.matchFiltersToPathAndServletName(bridgeServletContext.bridgeFilters, requestURI, servletName, currentDispatchMode);

        BridgeFilterChain filterChain = new BridgeFilterChain();
        for (BridgeFilterModel matchingBridgeFilterModel : matchingBridgeFilterModels) {
            filterChain.addFilter(matchingBridgeFilterModel.getFilter());
            if (!matchingBridgeFilterModel.isInitialized()) {
                matchingBridgeFilterModel.init();
            }
        }

        if (bridgeServletModel != null && !bridgeServletModel.isInitialized()) {
            bridgeServletModel.init();
        }

        if (bridgeServletModel != null) {
            filterChain.addFilter(new BridgeFilterChain.ServletDispatchingFilter(bridgeServletModel.getServlet()));
        }

        if (bridgeServletModel == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        Throwable errorDuringProcessing = null;
        BridgeHttpServletRequestWrapper bridgeHttpServletRequestWrapper = new BridgeHttpServletRequestWrapper(request, bridgeServletContext, contextPath, servletPath, pathInfo, queryString);
        try {
            filterChain.doFilter(bridgeHttpServletRequestWrapper, response);
        } catch (Throwable t) {
            errorDuringProcessing = t;
        }

        if (request.getAttribute("javax.servlet.error.request_uri") == null) {
            // we do this check to avoid error processing looping.
            handleErrors(errorDuringProcessing, bridgeHttpServletRequestWrapper, response, bridgeServletContext, bridgeServletModel);
        }

    }

}
