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
import io.undertow.server.handlers.Cookie;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
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

	// as in org.eclipse.jetty.server.handler.ContextHandler._vhosts, _vhostswildcard and _vconnectors
	private String[] virtualHosts;
	private boolean[] virtualHostWildcards;
	private String[] connectorNames;

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

				// We'll do the VHost/connector mapping as in Jetty, because it's better (and consistent with the rest
				// of Pax Web) than io.undertow.server.handlers.NameVirtualHostHandler
				String hostHeader = exchange.getRequestHeaders().getFirst(Headers.HOST);
				if (hostHeader == null) {
					// in HTTP/2, "Host" header is replaced by ":authority" header
					hostHeader = incomingRequest.getServerName();
				}
				String connectorName = exchange.getConnection().getUndertowOptions().get(UndertowFactory.PAX_WEB_CONNECTOR_NAME);
				if (hostHeader.contains(":")) {
					// strip port number in Host header
					hostHeader = hostHeader.substring(0, hostHeader.lastIndexOf(":"));
				}
				if (!matches(hostHeader, connectorName)) {
					exchange.setStatusCode(StatusCodes.NOT_FOUND);
					return;
				}

				ServletInfo servletInfo = context.getCurrentServlet().getManagedServlet().getServletInfo();

				OsgiContextModel osgiContextModel = null;
				if (servletInfo instanceof PaxWebServletInfo) {
					PaxWebServletInfo paxWebServletInfo = (PaxWebServletInfo) servletInfo;

					HttpServletRequest req;
					if (!paxWebServletInfo.is404()) {
						// wrap request, so it returns servlet's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								paxWebServletInfo.getServletContext(), osgiSessionsBridge);
						osgiContextModel = paxWebServletInfo.getServletContext().getOsgiContextModel();
					} else {
						// wrap request, so it returns default context's servlet context
						req = new OsgiHttpServletRequestWrapper(incomingRequest,
								defaultServletContext, osgiSessionsBridge);
						osgiContextModel = defaultServletContext.getOsgiContextModel();
					}
					context.setServletRequest(req);
				}

				// attachment is such a great place to pass information down the request handling thread...
				// unfortunately session manipulation methods can't access the exchange
				if (osgiContextModel != null) {
					String sessionIdPrefix = osgiContextModel.getTemporaryLocation().replaceAll("/", "_");
					PaxWebSessionIdGenerator.sessionIdPrefix.set(sessionIdPrefix);
					String sessionCookie = context.getCurrentServletContext().getSessionCookieConfig().getName();
					Cookie cookie = exchange.getRequestCookie(sessionCookie);
					if (cookie != null) {
						PaxWebSessionIdGenerator.cookieSessionId.set(cookie.getValue());
					}
				}

				exchange.addExchangeCompleteListener((exchange1, nextListener) -> {
					PaxWebSessionIdGenerator.sessionIdPrefix.set(null);
					PaxWebSessionIdGenerator.cookieSessionId.set(null);
					nextListener.proceed();
				});
				// just proceed
				handler.handleRequest(exchange);
			}
		};
	}

	public OsgiServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

	// similar as in org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContext.setVirtualHosts
	public void setVirtualHosts(String[] virtualHosts) {
		int size = virtualHosts == null ? 0 : virtualHosts.length;
		if (size == 0) {
			this.virtualHosts = null;
			this.virtualHostWildcards = null;
			this.connectorNames = null;
		} else {
			this.virtualHosts = new String[size];
			this.virtualHostWildcards = new boolean[size];
			this.connectorNames = new String[size];

			for (int i = 0; i < size; i++) {
				this.virtualHosts[i] = null;
				this.virtualHostWildcards[i] = false;
				this.connectorNames[i] = null;

				String vh = virtualHosts[i];
				if (vh == null || "".equals(vh.trim())) {
					continue;
				}
				if (vh.startsWith("@")) {
					// connector only
					this.connectorNames[i] = vh.substring(1);
				} else {
					String host;
					String connector = null;
					int atpos = vh.indexOf("@");
					if (atpos >= 0) {
						// host@connector
						host = vh.substring(0, atpos).trim();
						connector = vh.substring(atpos + 1).trim();
					} else {
						// host only
						host = vh.trim();
					}
					if (connector != null && !"".equals(connector)) {
						this.connectorNames[i] = connector;
					}
					if (!"".equals(host)) {
						this.virtualHosts[i] = host;
						if (host.startsWith("*.")) {
							this.virtualHostWildcards[i] = true;
							// *.example.com -> .example.com
							this.virtualHosts[i] = host.substring(1);
						}
					}
				}
			}
		}
	}

	/**
	 * Method similar to {@code org.eclipse.jetty.server.handler.ContextHandler#checkVirtualHost()} and reimplements
	 * Undertow's approach to Virtual Hosts, but at context level. This is actually a copy of
	 * {@code org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContext#matches()}.
	 * @param vhost
	 * @param connectorName
	 */
	public boolean matches(String vhost, String connectorName) {
		if (this.virtualHosts == null || virtualHosts.length == 0) {
			return true;
		}

		for (int i = 0; i < virtualHosts.length; i++) {
			String vh = virtualHosts[i];
			String connector = connectorNames[i];

			if (connector != null) {
				// always chck connector name - if it doesn't match, return false regardless of Host header
				if (!connector.equalsIgnoreCase(connectorName)) {
					continue;
				}

				if (vh == null) {
					// plain @connectorName rule - Host header doesn't matter
					return true;
				}
			}

			if (vh != null) {
				if (virtualHostWildcards[i]) {
					// wildcard only at the beginning, and only for one additional subdomain level
					int index = vhost.indexOf(".");
					if (index >= 0 && vhost.substring(index).equalsIgnoreCase(vh)) {
						return true;
					}
				} else if (vhost.equalsIgnoreCase(vh)) {
					return true;
				}
			}
		}
		return false;
	}

}
