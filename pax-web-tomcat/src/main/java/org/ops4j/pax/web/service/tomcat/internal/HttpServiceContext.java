/**
 * 
 */
package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.core.StandardContext;
import org.osgi.service.http.HttpContext;

/**
 * @author achim
 *
 */
public class HttpServiceContext extends StandardContext {

	private HttpContext httpContext;
	
	/**
	 * 
	 */
	public HttpServiceContext() {
		// TODO Auto-generated constructor stub
		
        //TODO add a filtermapping with a new TomcatInterceptFilter here
        		
        //need to take care of adding to "root" context.
        //filterRegistration.addMappingForServletNames( getDispatcherTypes( filterModel ), /*TODO get asynch supported?*/ false, filterModel.getUrlPatterns() );
		
        //filterRegistration.setInitParameters( filterModel.getInitParams() );

	}

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

}
