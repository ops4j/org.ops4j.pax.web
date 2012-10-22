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
	}

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

}
