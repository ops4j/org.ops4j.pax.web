package org.ops4j.pax.web.service.webapp.bridge;

import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.IOException;

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
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        logger.debug("service({},{})", req, res);
    }

}
