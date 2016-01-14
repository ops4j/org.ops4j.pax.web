package org.ops4j.pax.web.service.webapp.bridge;

import org.ops4j.pax.web.service.webapp.bridge.internal.BridgeServer;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by loom on 13.01.16.
 */
public class DispatcherFilter implements Filter {

    private BridgeServer bridgeServer;

    public void setBridgeServer(BridgeServer bridgeServer) {
        this.bridgeServer = bridgeServer;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

    }

    @Override
    public void destroy() {

    }
}
