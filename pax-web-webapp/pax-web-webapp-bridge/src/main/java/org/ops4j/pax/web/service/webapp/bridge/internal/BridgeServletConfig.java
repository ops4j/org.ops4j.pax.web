package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.ServletModel;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by loom on 14.01.16.
 */
public class BridgeServletConfig implements ServletConfig {

    private ServletModel servletModel;
    private BridgeServletContext bridgeServletContext;

    public BridgeServletConfig(ServletModel servletModel, BridgeServletContext bridgeServletContext) {
        this.servletModel = servletModel;
        this.bridgeServletContext = bridgeServletContext;
    }

    @Override
    public String getServletName() {
        return servletModel.getName();
    }

    @Override
    public ServletContext getServletContext() {
        return bridgeServletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return servletModel.getInitParams().get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>(servletModel.getInitParams().keySet()).elements();
    }
}
