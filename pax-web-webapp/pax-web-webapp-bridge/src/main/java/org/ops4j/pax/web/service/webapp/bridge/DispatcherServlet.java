package org.ops4j.pax.web.service.webapp.bridge;

import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServer;
import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Created by loom on 13.01.16.
 */
public class DispatcherServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private BridgeServer bridgeServer;

    public void setBridgeServer(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        logger.debug("service({},{})", req, res);
        String proxyContextPath = (String) req.getAttribute("org.ops4j.pax.web.service.webapp.bridge.proxyContextPath");
        String proxyServletPath = (String) req.getAttribute("org.ops4j.pax.web.service.webapp.bridge.proxyServletPath");
        String requestURI = req.getRequestURI();
        String newRequestURI = requestURI;
        if (requestURI.startsWith(proxyContextPath + proxyServletPath)) {
            newRequestURI = requestURI.substring(proxyContextPath.length() + proxyServletPath.length());
        }
        ContextModel contextModel = bridgeServer.getServerModel().matchPathToContext(newRequestURI);
        String newContextPath = contextModel.getContextName();
        BridgeServerModel.UrlPattern urlPattern = BridgeServerModel.matchPathToContext(bridgeServer.getBridgeServerModel().getServletUrlPatterns(), newRequestURI);
        if (urlPattern.getModel() instanceof ServletModel) {
            ServletModel servletModel = (ServletModel) urlPattern.getModel();
            servletModel.getServlet().service(req, res);
        }
    }

}
