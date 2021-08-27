/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.undertow.internal;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.StatusCodes;
import org.ops4j.pax.web.service.spi.servlet.OsgiHttpServletRequestWrapper;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiSessionAttributeListener;

/**
 * This {@link HandlerWrapper} ensures that {@link org.osgi.service.http.whiteboard.Preprocessor preprocessors},
 * filters and target servlet use proper request wrapper that returns proper {@link ServletContext}.
 */
public class PaxWebOuterHandlerWrapper implements HandlerWrapper {

	/** Default {@link ServletContext} to use for chains without target servlet (e.g., filters only) */
	private OsgiServletContext defaultServletContext;

	private final OsgiSessionAttributeListener osgiSessionsBridge;

	public PaxWebOuterHandlerWrapper(OsgiSessionAttributeListener osgiSessionsBridge) {
		this.osgiSessionsBridge = osgiSessionsBridge;
	}

	@Override
	@SuppressWarnings("Convert2Lambda")
	public HttpHandler wrap(HttpHandler handler) {
		return new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				HttpServletRequest incomingRequest = (HttpServletRequest) context.getServletRequest();

				// "128.3.5 Static Content" is the only place where protected directories are mentioned.
				// We can handle them before proceeding further, even if
				// io.undertow.servlet.handlers.ServletInitialHandler.handleRequest() checked some of the prefixes
				// already
				String path = exchange.getRelativePath();
				if (path.regionMatches(true, 0, "/meta-inf", 0, "/meta-inf".length())
						|| path.regionMatches(true, 0, "/web-inf", 0, "/web-inf".length())
						|| path.regionMatches(true, 0, "/osgi-inf", 0, "/osgi-inf".length())
						|| path.regionMatches(true, 0, "/osgi-opt", 0, "/osgi-opt".length())) {
					exchange.setStatusCode(StatusCodes.NOT_FOUND);
					return;
				}

				ServletInfo servletInfo = context.getCurrentServlet().getManagedServlet().getServletInfo();

				if (servletInfo instanceof PaxWebServletInfo) {
					PaxWebServletInfo paxWebServletInfo = (PaxWebServletInfo) servletInfo;

					HttpServletRequest req;
					if (!paxWebServletInfo.is404()) {
						// wrap request, so it returns servlet's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								paxWebServletInfo.getServletContext(), osgiSessionsBridge);
					} else {
						// wrap request, so it returns default context's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								defaultServletContext, osgiSessionsBridge);
					}
					context.setServletRequest(req);
				}

				// just proceed
				handler.handleRequest(exchange);
			}
		};
	}

	public ServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

}
