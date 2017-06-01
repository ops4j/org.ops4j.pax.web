/*  Copyright 2007 Niclas Hedhman.
 *  Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHandler;
import org.ops4j.lang.NullArgumentException;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceServletHandler extends ServletHandler {

	private static final String METHOD_TRACE = "TRACE";
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceServletHandler.class);
	private final HttpContext httpContext;

	HttpServiceServletHandler(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		this.httpContext = httpContext;
		setEnsureDefaultServlet(false);
	}

	@Override
	public void doHandle(final String target, final Request baseRequest,
						 final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		if (request.getMethod().equals(METHOD_TRACE)) {
			throw new ServletException("HTTP TRACE method is disabled");
		}
		// we have to set the jetty request as a request attribute if not
		// already set in order to be able to handle the
		// case that the request has been wrapped with a custom wrapper (case of
		// redirect/forward).
		// this is needed by HttpServiceRequestWrapper in order to handle
		// authentication
		// and should actually happen only once on initial request
		if (baseRequest
				.getAttribute(HttpServiceRequestWrapper.JETTY_REQUEST_ATTR_NAME) == null) {
			baseRequest.setAttribute(
					HttpServiceRequestWrapper.JETTY_REQUEST_ATTR_NAME, request);
		}
		final HttpServiceRequestWrapper requestWrapper = new HttpServiceRequestWrapper(
				request);
		final HttpServiceResponseWrapper responseWrapper = new HttpServiceResponseWrapper(
				response);
		if (httpContext.handleSecurity(requestWrapper, responseWrapper)) {
			LOG.debug("handling request {}, {}", requestWrapper, responseWrapper);
			super.doHandle(target, baseRequest, request, response);
		} else {
			// on case of security constraints not fulfilled, handleSecurity is
			// supposed to set the right
			// headers but to be sure lets verify the response header for 401
			// (unauthorized)
			// because if the header is not set the processing will go on with
			// the rest of the contexts
			LOG.debug("Security constraint not fulfilled");
			if (!responseWrapper.isCommitted()) {
				if (!responseWrapper.isStatusSet()) {
					responseWrapper
							.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				} else {
					responseWrapper.sendError(responseWrapper.getStatus());
				}
			}
		}
	}

}
