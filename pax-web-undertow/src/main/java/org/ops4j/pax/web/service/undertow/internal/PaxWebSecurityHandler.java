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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

public class PaxWebSecurityHandler implements HandlerWrapper {

	/** Default {@link OsgiContextModel} to use for chains without target servlet (e.g., filters only) */
	private OsgiContextModel defaultOsgiContextModel;

	/** Default {@link WebContainerContext} for chains without target {@link Servlet} */
	private WebContainerContext defaultWebContainerContext;

	@Override
	@SuppressWarnings("Convert2Lambda")
	public HttpHandler wrap(HttpHandler handler) {
		return new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				ServletInfo servletInfo = context.getCurrentServlet().getManagedServlet().getServletInfo();
				if (servletInfo instanceof PaxWebServletInfo) {
					PaxWebServletInfo paxWebServletInfo = (PaxWebServletInfo) servletInfo;

					HttpServletRequest req = (HttpServletRequest) context.getServletRequest();
					HttpServletResponse res = (HttpServletResponse) context.getServletResponse();

					WebContainerContext webContext;
					if (!paxWebServletInfo.is404()) {
						webContext = paxWebServletInfo.getWebContainerContext();
					} else {
						webContext = defaultWebContainerContext;
					}

					try {
						if (webContext == null || webContext.handleSecurity(req, res)) {
							// continue normally with normal filters and target servlet
							handler.handleRequest(exchange);
						} else {
							// authentication failed
							if (!res.isCommitted()) {
								// Pax Web before 8.0.0 was sending HTTP 401 here, but the thing is that it should be
								// the role of actual implementation of handleSecurity() to respond with 401 if there's
								// a need (for example when returning "WWW-Authenticate: Basic Realm") which will make
								// the response committed
								// When it's not committed, we can assume that user has no chance to authenticate
								res.sendError(HttpServletResponse.SC_FORBIDDEN);
							}
						}
					} finally {
						if (webContext != null) {
							webContext.finishSecurity(req, res);
						}
					}
				}
			}
		};
	}

	public OsgiContextModel getDefaultOsgiContextModel() {
		return defaultOsgiContextModel;
	}

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel) {
		if (this.defaultOsgiContextModel != null) {
			// release previous WebContainerContext
			this.defaultOsgiContextModel.releaseHttpContext(this.defaultOsgiContextModel.getOwnerBundle());
			this.defaultWebContainerContext = null;
		}
		this.defaultOsgiContextModel = defaultOsgiContextModel;
		if (defaultOsgiContextModel != null) {
			this.defaultWebContainerContext = defaultOsgiContextModel.resolveHttpContext(defaultOsgiContextModel.getOwnerBundle());
		}
	}

}
