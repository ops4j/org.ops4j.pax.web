package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by loom on 18.01.16.
 */
public class BridgeNamedRequestDispatcher extends AbstractBridgeRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(BridgePathRequestDispatcher.class);
    private String servletName;
    private BridgeServer bridgeServer;

    public BridgeNamedRequestDispatcher(String servletName, BridgeServer bridgeServer) {
        this.servletName = servletName;
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException {

    }
}
