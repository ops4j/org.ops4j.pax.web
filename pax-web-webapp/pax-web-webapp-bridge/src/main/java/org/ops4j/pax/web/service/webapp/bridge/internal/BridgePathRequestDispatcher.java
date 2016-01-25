package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by loom on 18.01.16.
 */
public class BridgePathRequestDispatcher extends AbstractBridgeRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(BridgePathRequestDispatcher.class);
    private String requestURI;
    private BridgeServer bridgeServer;

    public BridgePathRequestDispatcher(String requestURI, BridgeServer bridgeServer) {
        this.requestURI = requestURI;
        this.bridgeServer = bridgeServer;
    }

    public void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException {
        ContextModel contextModel = bridgeServer.getServerModel().matchPathToContext(requestURI);
        if (contextModel == null) {
            logger.error("Couldn't find a context for request=" + requestURI + " !");
            return;
        }
        BridgeServletContext bridgeServletContext = bridgeServer.getContextModel(contextModel.getContextName());

        final String newContextPath;
        if (contextModel.getContextName().length() > 0 && !contextModel.getContextName().startsWith("/")) {
            newContextPath = "/" + contextModel.getContextName();
        } else {
            newContextPath = contextModel.getContextName();
        }

        BridgeFilterChain filterChain = new BridgeFilterChain();
        List<BridgeServerModel.UrlPattern> matchingFilterUrlPatterns = BridgeServerModel.matchAllFiltersPathToContext(bridgeServer.getBridgeServerModel().getFilterUrlPatterns(), requestURI);
        for (BridgeServerModel.UrlPattern matchingFilterUrlPattern : matchingFilterUrlPatterns) {

            FilterModel filterModel = (FilterModel) matchingFilterUrlPattern.getModel();

            if (!matchesFilterDispatchers(filterModel.getDispatcher(), currentDispatchMode)) {
                continue;
            }

            filterChain.addFilter(filterModel.getFilter());
            BridgeFilterModel bridgeFilterModel = bridgeServletContext.findFilter(filterModel);
            if (bridgeFilterModel != null && !bridgeFilterModel.isInitialized()) {
                bridgeFilterModel.init();
            }
        }

        BridgeServerModel.UrlPattern urlPattern = BridgeServerModel.matchPathToContext(bridgeServer.getBridgeServerModel().getServletUrlPatterns(), requestURI);
        if (urlPattern.getModel() instanceof ServletModel) {
            ServletModel servletModel = (ServletModel) urlPattern.getModel();
            BridgeServletModel bridgeServletModel = bridgeServletContext.findServlet(servletModel);
            if (!bridgeServletModel.isInitialized()) {
                bridgeServletModel.init();
            }
            String matchedUrlPattern = urlPattern.getUrlPattern();
            String servletPathMatch = matchedUrlPattern.substring(newContextPath.length());
            String servletPathPart = requestURI.substring(newContextPath.length());
            if (servletPathMatch.endsWith("/*")) {
                servletPathPart = servletPathMatch.substring(0, servletPathMatch.length()-2);
            } else if (servletPathMatch.contains("/*.")) {
            }
            final String finalServletPath = servletPathPart;
            final String pathInfo = requestURI.substring(newContextPath.length() + finalServletPath.length());

            filterChain.addFilter(new BridgeFilterChain.ServletDispatchingFilter(servletModel.getServlet()));

            filterChain.doFilter(new BridgeHttpServletRequestWrapper(request, bridgeServletContext, newContextPath, finalServletPath, pathInfo), response);

        } else {
            logger.error("Couldn't resolve a servlet for path " + requestURI);
        }

    }

    private boolean matchesFilterDispatchers(String[] dispatchers, String currentDispatcher) {
        if (dispatchers == null || dispatchers.length == 0) {
            if ("REQUEST".equals(currentDispatcher)) {
                return true;
            } else {
                return false;
            }
        }
        for (String dispatcher : dispatchers) {
            if (dispatcher.toLowerCase().equals(currentDispatcher.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

}
