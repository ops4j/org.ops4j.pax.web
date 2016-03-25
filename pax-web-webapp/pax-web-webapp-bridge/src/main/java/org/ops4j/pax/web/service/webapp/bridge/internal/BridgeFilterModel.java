package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.model.FilterModel;

import javax.servlet.Filter;
import javax.servlet.ServletException;

/**
 * Created by loom on 18.01.16.
 */
public class BridgeFilterModel {

    private FilterModel filterModel;
    private BridgeFilterConfig bridgeFilterConfig;
    private boolean initialized = false;
    private Filter filter; 
    
    public BridgeFilterModel(FilterModel filterModel, BridgeFilterConfig bridgeFilterConfig) throws InstantiationException, IllegalAccessException {
        this.filterModel = filterModel;
        this.bridgeFilterConfig = bridgeFilterConfig;
        
        filter = filterModel.getFilter();
        if (filter == null) {
            Class<? extends Filter> filterClass = filterModel.getFilterClass();
            filter = filterClass.newInstance();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public FilterModel getFilterModel() {
        return filterModel;
    }

    public void init() throws ServletException {
        if (initialized) {
            throw new ServletException("Filter " + filterModel + " is already initialized");
        }
        filter.init(bridgeFilterConfig);
        initialized = true;
    }

    public void destroy() {
        if (!initialized) {
            return;
        }
        filterModel.getFilter().destroy();
        initialized = false;
    }

    public Filter getFilter() {
        return filter;
    }

}
