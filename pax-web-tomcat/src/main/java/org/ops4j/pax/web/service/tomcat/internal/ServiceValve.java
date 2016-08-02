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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.osgi.service.http.HttpContext;

/**
 * @author anierbeck
 */
public class ServiceValve extends ValveBase {

	private HttpContext httpContext;

	public ServiceValve(HttpContext httpContext) {
		this.httpContext = httpContext;
	}

	@Override
	public void invoke(Request request, Response response) throws IOException,
			ServletException {
		// final HttpServiceRequestWrapper requestWrapper = new
		// HttpServiceRequestWrapper(
		// request);
		// final HttpServiceResponseWrapper responseWrapper = new
		// HttpServiceResponseWrapper(
		// response);

		if (httpContext.handleSecurity(request, response)) {
			getNext().invoke(request, response);
		} else {
			// on case of security constraints not fullfiled, handleSecurity is
			// supposed to set the right
			// headers but to be sure lets verify the response header for 401
			// (unauthorized)
			// because if the header is not set the processing will go on with
			// the rest of the contexts
			if (!response.isCommitted()) {
				if (!response.isError()) {
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					response.sendError(response.getStatus());
				}
			}
		}
	}

}
