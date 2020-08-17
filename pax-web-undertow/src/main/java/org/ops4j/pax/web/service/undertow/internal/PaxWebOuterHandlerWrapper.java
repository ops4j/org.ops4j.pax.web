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
import org.ops4j.pax.web.service.spi.servlet.OsgiHttpServletRequestWrapper;

/**
 * This {@link HandlerWrapper} ensures that {@link org.osgi.service.http.whiteboard.Preprocessor preprocessors},
 * filters and target servlet use proper request wrapper that returns proper {@link ServletContext}.
 */
public class PaxWebOuterHandlerWrapper implements HandlerWrapper {

	/** Default {@link ServletContext} to use for chains without target servlet (e.g., filters only) */
	private ServletContext defaultServletContext;

	@Override
	@SuppressWarnings("Convert2Lambda")
	public HttpHandler wrap(HttpHandler handler) {
		return new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				ServletInfo servletInfo = context.getCurrentServlet().getManagedServlet().getServletInfo();

//				if (context.getDeployment().getDeploymentState() != DeploymentManager.State.STARTED) {
//				}

				if (servletInfo instanceof PaxWebServletInfo) {
					PaxWebServletInfo paxWebServletInfo = (PaxWebServletInfo) servletInfo;

					HttpServletRequest incomingRequest = (HttpServletRequest) context.getServletRequest();

					HttpServletRequest req;
					if (!paxWebServletInfo.is404()) {
						// wrap request, so it returns servlet's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								paxWebServletInfo.getServletContext());
					} else {
						// wrap request, so it returns default context's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								defaultServletContext);
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

	public void setDefaultServletContext(ServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

}
