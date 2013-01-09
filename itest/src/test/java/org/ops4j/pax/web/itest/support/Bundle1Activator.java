package org.ops4j.pax.web.itest.support;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bundle1Activator implements BundleActivator {
	
    private static final transient Logger LOG = LoggerFactory.getLogger(Bundle1Activator.class);
	private ServiceRegistration httpContextReg;
	private ServiceRegistration bundle1ServletReg;
	private ServiceRegistration filterReg;

    
	@Override
	public void start(BundleContext context) throws Exception {
		
		ServiceReference serviceReference = context.getServiceReference(WebContainer.class.getName());
        
        while (serviceReference == null) {
        	serviceReference = context.getServiceReference(WebContainer.class.getName());
        }
        
        WebContainer service = (WebContainer) context.getService(serviceReference);
        
        HttpContext httpContext = service.getDefaultSharedHttpContext();
		
		Dictionary<String, String> props;

        // register a custom http context that forbids access
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared" );
        httpContextReg =
            context.registerService( HttpContext.class.getName(), httpContext, props );
        // and an servlet that cannot be accessed due to the above context
        props = new Hashtable<String, String>();
		props.put( ExtenderConstants.PROPERTY_ALIAS, Bundle1Servlet.ALIAS );
        props.put("servlet-name", "Bundle1Servlet");
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        bundle1ServletReg =
            context.registerService( Servlet.class.getName(), new Bundle1Servlet(), props );
        
        // register a filter
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_URL_PATTERNS, Bundle1Servlet.ALIAS+"/*" );
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        filterReg =
            context.registerService( Filter.class.getName(), new Bundle1Filter(), props );
        
        Dictionary<String, String> filterInit = new Hashtable<String, String>();
        filterInit.put("pattern", ".*");
        filterInit.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        
        service.registerFilter(new Bundle1SharedFilter(), new String[] { "/*" }, null, filterInit, (HttpContext) context.getService(httpContextReg.getReference()));
        
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null)
			filterReg.unregister();
		
		if (bundle1ServletReg != null)
			bundle1ServletReg.unregister();
		
		if (httpContextReg != null)
			httpContextReg.unregister();
	}

}
