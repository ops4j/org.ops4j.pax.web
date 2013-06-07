/**
 * 
 */
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Valve;
import org.apache.catalina.comet.CometEvent;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.ops4j.lang.NullArgumentException;
import org.osgi.service.http.HttpContext;

/**
 * @author anierbeck
 *
 */
public class ServiceValve extends ValveBase {
	
	private HttpContext httpContext;

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
//		final HttpServiceRequestWrapper requestWrapper = new HttpServiceRequestWrapper(
//				request);
//		final HttpServiceResponseWrapper responseWrapper = new HttpServiceResponseWrapper(
//				response);
//		if (httpContext.handleSecurity(requestWrapper, responseWrapper)) {
			getNext().invoke(request, response);
//		} else {
			// on case of security constraints not fullfiled, handleSecurity is
			// supposed to set the right
			// headers but to be sure lets verify the response header for 401
			// (unauthorized)
			// because if the header is not set the processing will go on with
			// the rest of the contexts
//			if (!responseWrapper.isCommitted()) {
//				if (!responseWrapper.isStatusSet()) {
//					responseWrapper
//							.sendError(HttpServletResponse.SC_UNAUTHORIZED);
//				} else {
//					responseWrapper.sendError(responseWrapper.getStatus());
//				}
//			}
//		}		
	}

}
