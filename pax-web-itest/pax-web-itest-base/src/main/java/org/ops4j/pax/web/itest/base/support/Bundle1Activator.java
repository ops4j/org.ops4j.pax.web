/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.ops4j.pax.web.itest.base.support;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

public class Bundle1Activator implements BundleActivator {
	
	private ServiceRegistration<HttpContext> httpContextReg;
	private ServiceRegistration<Servlet> bundle1ServletReg;
	private ServiceRegistration<Filter> filterReg;

    
	@Override
	public void start(BundleContext context) throws Exception {
		
		ServiceReference<WebContainer> serviceReference = context.getServiceReference(WebContainer.class);
        
        while (serviceReference == null) {
        	serviceReference = context.getServiceReference(WebContainer.class);
        }
        
        WebContainer service = (WebContainer) context.getService(serviceReference);
        
        HttpContext httpContext = service.getDefaultSharedHttpContext();
		
		Dictionary<String, String> props;

        // register a custom http context that forbids access
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared" );
        httpContextReg =
            context.registerService( HttpContext.class, httpContext, props );
        // and an servlet that cannot be accessed due to the above context
        props = new Hashtable<String, String>();
		props.put( ExtenderConstants.PROPERTY_ALIAS, Bundle1Servlet.ALIAS );
        props.put("servlet-name", "Bundle1Servlet");
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        bundle1ServletReg =
            context.registerService( Servlet.class, new Bundle1Servlet(), props );
        
        // register a filter
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_URL_PATTERNS, Bundle1Servlet.ALIAS + "/*" );
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        filterReg =
            context.registerService( Filter.class, new Bundle1Filter(), props );
        
        Dictionary<String, String> filterInit = new Hashtable<String, String>();
        filterInit.put("pattern", ".*");
        filterInit.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "shared");
        
        service.registerFilter(new Bundle1SharedFilter(), new String[] { "/*" }, null, filterInit, (HttpContext) context.getService(httpContextReg.getReference()));
        
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null) {
			filterReg.unregister();
		}
		
		if (bundle1ServletReg != null) {
			bundle1ServletReg.unregister();
		}
		
		if (httpContextReg != null) {
			httpContextReg.unregister();
		}
	}

}
