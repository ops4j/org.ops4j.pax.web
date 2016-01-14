package org.ops4j.pax.web.service.webapp.bridge.internal;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.webapp.bridge.DispatcherFilter;
import org.ops4j.pax.web.service.webapp.bridge.DispatcherServlet;
import org.ops4j.pax.web.service.webapp.bridge.EventDispatcher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import java.util.EventListener;
import java.util.Hashtable;

/**
 * Bundle activator to initialize Pax Web Webapp Bridge implementation
 */
public class Activator implements BundleActivator {

    private static final String VENDOR = "OPS4j";
    private static final String BRIDGE_IDENTIFIER_PROPERTY = "org.ops4j.pax.web.bridge";

    private EventDispatcher eventDispatcher;
    private DispatcherFilter dispatcherFilter;
    private DispatcherServlet dispatcherServlet;

    ServiceRegistration dispatcherFilterRegistration = null;
    ServiceRegistration dispatcherServletRegistration = null;
    ServiceRegistration eventDispatcherRegistration = null;

    ServiceRegistration serverControllerFactoryRegistration = null;

    BridgeServer bridgeServer = new BridgeServer();

    @Override
    public void start(BundleContext context) throws Exception {

        dispatcherFilter = new DispatcherFilter();
        dispatcherFilter.setBridgeServer(bridgeServer);
        Hashtable<String, Object> dispatcherFilterProperties = new Hashtable<String, Object>();
        dispatcherFilterProperties.put(BRIDGE_IDENTIFIER_PROPERTY, dispatcherFilter.getClass().getName());
        dispatcherFilterProperties.put(Constants.SERVICE_DESCRIPTION, "OPS4j Http Dispatcher for bridged servlet filter handling");
        dispatcherFilterProperties.put(Constants.SERVICE_VENDOR, VENDOR);
        dispatcherFilterRegistration = context.registerService(Filter.class.getName(), dispatcherFilter, dispatcherFilterProperties);

        dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setBridgeServer(bridgeServer);
        Hashtable<String, Object> dispatcherServletProperties = new Hashtable<String, Object>();
        dispatcherServletProperties.put(BRIDGE_IDENTIFIER_PROPERTY, dispatcherServlet.getClass().getName());
        dispatcherServletProperties.put(Constants.SERVICE_DESCRIPTION, "OPS4j Http Dispatcher for bridged servlet request handling");
        dispatcherServletProperties.put(Constants.SERVICE_VENDOR, VENDOR);
        dispatcherServletRegistration = context.registerService(HttpServlet.class.getName(), dispatcherServlet, dispatcherServletProperties);

        // Http Session event dispatcher
        eventDispatcher = new EventDispatcher();
        eventDispatcher.setBridgeServer(bridgeServer);
        Hashtable<String,Object> eventDispatcherProperties = new Hashtable<String, Object>();
        eventDispatcherProperties.put(BRIDGE_IDENTIFIER_PROPERTY, eventDispatcher.getClass().getName());
        eventDispatcherProperties.put(Constants.SERVICE_DESCRIPTION, "OPS4j Http Dispatcher for bridged servlet event handling");
        eventDispatcherProperties.put(Constants.SERVICE_VENDOR, VENDOR);
        eventDispatcherRegistration = context.registerService(EventListener.class.getName(), eventDispatcher, eventDispatcherProperties);

        BridgeServerControllerFactory bridgeServerControllerFactory = new BridgeServerControllerFactory(bridgeServer);
        context.registerService(ServerControllerFactory.class, bridgeServerControllerFactory, null);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (dispatcherFilterRegistration != null) {
            dispatcherFilterRegistration.unregister();
        }
        if (dispatcherServletRegistration != null) {
            dispatcherServletRegistration.unregister();
        }
        if (eventDispatcherRegistration != null) {
            eventDispatcherRegistration.unregister();
        }
        eventDispatcher = null;
        dispatcherFilter = null;
        dispatcherServlet = null;
    }
}
