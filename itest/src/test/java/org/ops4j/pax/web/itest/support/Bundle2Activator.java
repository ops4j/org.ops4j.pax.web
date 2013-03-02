package org.ops4j.pax.web.itest.support;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bundle2Activator implements BundleActivator {
	
    private static final transient Logger LOG = LoggerFactory.getLogger(Bundle2Activator.class);

    
	@Override
	public void start(BundleContext context) throws Exception {
		
		ServiceReference<WebContainer> serviceReference = context.getServiceReference(WebContainer.class);
        
        while (serviceReference == null) {
        	serviceReference = context.getServiceReference(WebContainer.class);
        }
        
        WebContainer service = (WebContainer) context.getService(serviceReference);

        Collection<ServiceReference<HttpContext>> serviceReferences = context.getServiceReferences(HttpContext.class, "(httpContext.id=shared)");

        if (serviceReferences.size() > 1)
        	throw new RuntimeException("should only be one http shared context");
        
        HttpContext httpContext = context.getService(serviceReferences.iterator().next());

		Dictionary<String, String> props;

        // register a custom http context that forbids access
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared" );
        props.put( ExtenderConstants.PROPERTY_ALIAS, Bundle2SharedServlet.ALIAS);
        service.registerServlet(new Bundle2SharedServlet(), new String[] {Bundle2SharedServlet.ALIAS}, props, httpContext);
        
        Dictionary<String, String> filterInit = new Hashtable<String, String>();
        filterInit.put("pattern", ".*");
        filterInit.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        
        service.registerFilter(new Bundle2SharedFilter(), new String[] { "/*" }, null, filterInit, httpContext);
        
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		
	}

}
