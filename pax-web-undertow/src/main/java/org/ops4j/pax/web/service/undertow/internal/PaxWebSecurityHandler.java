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

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.undertow.internal.security.OsgiSecurityContext;
import org.osgi.service.http.context.ServletContextHelper;

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
							if (webContext != null) {
								final Object user = req.getAttribute(ServletContextHelper.REMOTE_USER);
								final Object authType = req.getAttribute(ServletContextHelper.AUTHENTICATION_TYPE);
								SecurityContext sc = exchange.getSecurityContext();
								// if we didn't authenticate using the server (for example using <login-config> or
								// non-OSGi security configuration), then if we have one of:
								//  - org.osgi.service.http.authentication.remote.user
								//  - org.osgi.service.http.authentication.type
								// we have to behave as we were authenticated...
								// see: https://github.com/ops4j/org.ops4j.pax.web/issues/1907
								if (sc == null) {
									if (user != null || authType != null) {
										exchange.setSecurityContext(new OsgiSecurityContext(exchange, user, authType));
									}
								} else if (!sc.isAuthenticated()) {
									// this is less obvious, but for now we'll override this status only if there's no
									// need to authenticate at Undertow level
									if (!sc.isAuthenticationRequired() && (user != null || authType != null)) {
										exchange.setSecurityContext(new OsgiSecurityContext(exchange, user, authType));
									}
								}
							}

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

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel, WebContainerContext resolvedWebContainerContext) {
		this.defaultOsgiContextModel = defaultOsgiContextModel;
		this.defaultWebContainerContext = resolvedWebContainerContext;
	}

}
