package org.ops4j.pax.web.jsf.resourcehandler.extender.internal;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ops4j.pax.web.jsf.resourcehandler.extender.OsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, BundleListener {

	/** marks bundle as relevant for scanning */
	private static final String HEADER_JSF_RESOURCE = "WebResources";
    
	private transient Logger logger;
    private BundleContext context;
    private List<OsgiResourceLocator> osgiResourceLocatorServices = new CopyOnWriteArrayList<>();

    private ServiceTracker<OsgiResourceLocator, OsgiResourceLocator> trackerResourceLocator;
    
    public Activator() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        
        IndexedOsgiResourceLocator indexedRegistryService = new IndexedOsgiResourceLocator(context);
        
    	trackerResourceLocator = new ServiceTracker<OsgiResourceLocator, OsgiResourceLocator>(context, OsgiResourceLocator.class.getName(), new ServiceTrackerCustomizer<OsgiResourceLocator, OsgiResourceLocator>() {
            @Override
            public OsgiResourceLocator addingService(ServiceReference<OsgiResourceLocator> reference) {
            	OsgiResourceLocator service = (OsgiResourceLocator)context.getService(reference);
            	if(service != null){
            		osgiResourceLocatorServices.add(service);
            		logger.info("New OsgiResourceLocator from bundle '{}' available...Scanning all bundles.", 
            				reference.getBundle().getSymbolicName());
            		fullBundleScan(service);
            		return service;
            	}
                return null;
            }

            @Override
            public void modifiedService(ServiceReference<OsgiResourceLocator> reference, OsgiResourceLocator service) {
            	// not interesting
            }

            @Override
            public void removedService(ServiceReference<OsgiResourceLocator> reference, OsgiResourceLocator service) {
            	logger.info("OsgiResourceLocator from bundle '{}' removed.", 
            			reference.getBundle().getSymbolicName());
            	osgiResourceLocatorServices.remove(service);
                context.ungetService(reference);
            }
        });
        trackerResourceLocator.open();
        // register service
        Dictionary<String, Object> props = new Hashtable<>(1);
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(-1));
        context.registerService(OsgiResourceLocator.class, indexedRegistryService, props);
        context.addBundleListener(Activator.this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        trackerResourceLocator.close();
        context.removeBundleListener(this);
    }
    
    @Override
    public void bundleChanged(BundleEvent event) {
        if(event.getType() == BundleEvent.STARTED && isJsfBundleForExtenderStartingOrActive(event.getBundle())) {
            osgiResourceLocatorServices.forEach(service -> service.register(event.getBundle()));
        }else if(isJsfBundleForExtenderStopping(event)) {
        	osgiResourceLocatorServices.forEach(service -> service.unregister(event.getBundle()));
        }
    }
    
    private boolean isJsfBundleForExtenderStartingOrActive(Bundle bundle){
        if(bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.ACTIVE){
            return bundle.getHeaders().get(HEADER_JSF_RESOURCE) != null;
        }
        return false;
    }
    
    private boolean isJsfBundleForExtenderStopping(BundleEvent event){
        Bundle bundle = event.getBundle();
        if(event.getType() == BundleEvent.STOPPED){
            return bundle.getHeaders().get(HEADER_JSF_RESOURCE) != null;
        }
        return false;
    }
    
    private void fullBundleScan(OsgiResourceLocator service){
        Arrays.stream(context.getBundles())
                .filter(this::isJsfBundleForExtenderStartingOrActive)
                .forEach(b -> service.register(b));
    }
}
