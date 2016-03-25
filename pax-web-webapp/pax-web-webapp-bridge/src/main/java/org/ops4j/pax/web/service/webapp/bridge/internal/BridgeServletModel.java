package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ServletModel;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServletModel {

    private ServletModel servletModel;
    private BridgeServletConfig bridgeServletConfig;
    private boolean initialized = false;
    private Servlet servlet;

    public BridgeServletModel(ServletModel servletModel, BridgeServletContext bridgeServletContext) {
        this.servletModel = servletModel;
        this.bridgeServletConfig = new BridgeServletConfig(servletModel, bridgeServletContext);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ServletModel getServletModel() {
        return servletModel;
    }

    public BridgeServletConfig getBridgeServletConfig() {
        return bridgeServletConfig;
    }

    public void init() throws ServletException {
        if (initialized) {
            throw new ServletException("Servlet " + servletModel + " is already initialized");
        }
        Servlet servlet = servletModel.getServlet();
        
        if (servlet == null) {
            try {
                Class<? extends Servlet> servletClass = servletModel.getServletClass();
                servlet = servletClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new ServletException("Servlet " + servletModel + " failed to initialize", e);
            }
        }
        this.servlet = servlet;
        servlet.init(bridgeServletConfig);
        initialized = true;
    }

    public void destroy() {
        if (!initialized) {
            return;
        }
        servletModel.getServlet().destroy();
        initialized = false;
    }

    public Servlet getServlet() {
        return servlet;
    }

}
