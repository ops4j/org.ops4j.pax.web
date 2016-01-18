package org.ops4j.pax.web.service.webapp.bridge.internal;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by loom on 18.01.16.
 */
public abstract class AbstractBridgeRequestDispatcher implements RequestDispatcher {

    public abstract void service(HttpServletRequest request, HttpServletResponse response, String currentDispatchMode) throws ServletException, IOException;

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        // todo not yet fully implemented
        service((HttpServletRequest) request, (HttpServletResponse) response, "FORWARD");
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        // todo not yet fully implemented
        service((HttpServletRequest) request, (HttpServletResponse) response, "INCLUDE");
    }

}
