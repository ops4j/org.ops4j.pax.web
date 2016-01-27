package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by loom on 18.01.16.
 */
public class BridgeNamedRequestDispatcher extends AbstractBridgeRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(BridgePathRequestDispatcher.class);
    private BridgeServletContext bridgeServletContext;
    private BridgeServletModel bridgeServletModel;

    public BridgeNamedRequestDispatcher(BridgeServletContext bridgeServletContext, BridgeServletModel bridgeServletModel, BridgeServer bridgeServer) {
        super(bridgeServer, true);
        this.bridgeServletContext = bridgeServletContext;
        this.bridgeServletModel = bridgeServletModel;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException {

        List<BridgeFilterModel> matchingBridgeFilterModels = BridgeServerModel.matchFiltersToPathAndServletName(bridgeServletContext.bridgeFilters, null, bridgeServletModel.getServletModel().getName(), currentDispatchMode);

        BridgeFilterChain filterChain = new BridgeFilterChain();
        for (BridgeFilterModel bridgeFilterModel : matchingBridgeFilterModels) {
            filterChain.addFilter(bridgeFilterModel.getFilterModel().getFilter());
            if (!bridgeFilterModel.isInitialized()) {
                bridgeFilterModel.init();
            }
        }

        if (!bridgeServletModel.isInitialized()) {
            bridgeServletModel.init();
        }

        filterChain.addFilter(new BridgeFilterChain.ServletDispatchingFilter(bridgeServletModel.getServletModel().getServlet()));

        String contextPath = request.getContextPath();
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();

        Throwable errorDuringProcessing = null;
        BridgeHttpServletRequestWrapper bridgeHttpServletRequestWrapper = new BridgeHttpServletRequestWrapper(request, bridgeServletContext, contextPath, servletPath, pathInfo, request.getQueryString());
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
