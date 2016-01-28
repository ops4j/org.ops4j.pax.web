package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.FilterModel;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by loom on 18.01.16.
 */
public class BridgeFilterConfig implements FilterConfig {

    FilterModel filterModel;
    private BridgeServletContext bridgeServletContext;

    public BridgeFilterConfig(FilterModel filterModel, BridgeServletContext bridgeServletContext) {
        this.filterModel = filterModel;
        this.bridgeServletContext = bridgeServletContext;
    }

    @Override
    public String getFilterName() {
        return filterModel.getName();
    }

    @Override
    public ServletContext getServletContext() {
        return bridgeServletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return filterModel.getInitParams().get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return new Vector<String>(filterModel.getInitParams().keySet()).elements();
    }
}
