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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;

/**
 * Again, Undertow's DefaultServlet is very inflexible - we can't override crucial
 * methods and we can't access private fields. Also, DefaultServlet.init() assumes certain implementation of
 * javax.servlet.ServletContext and we'd get ClassCastException in Pax Web anyway. So we have to create
 * new "resource servlet" from scratch
 *
 * @author Guillaume Nodet
 * @author Grzegorz Grzybek
 */
public class ResourceServlet extends HttpServlet implements ResourceManager {

	private final Context context;
	private final HttpHandler handler;
	// alias = part of the request URL after "/<context-name>" denoting the resource servlet
	private final String alias;
	// name = "default" when accessing resources from the root of the bundle or "/<base-path>", when
	// accessing resources from some path under root of the bundle
	private final String name;
	private List<String> welcomePages;

	public ResourceServlet(final Context context, String alias, String name) {
		this.context = context;
		this.alias = alias;
		if ("/".equals(name)) {
			this.name = "";
		} else {
			this.name = name;
		}
		this.handler = new ResourceHandler(this, new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				src.getOriginalResponse().sendError(404);
			}
		});
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		if (!(request instanceof HttpServletRequestImpl)) {
			throw new IllegalStateException("Request is not an instance of " + HttpServletRequestImpl.class.getName());
		}
		HttpServerExchange exchange = ((HttpServletRequestImpl) request).getExchange();
		try {
			handler.handleRequest(exchange);
		} catch (IOException | ServletException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	public Resource getResource(String path) throws IOException {
		// remember - differently than in org.ops4j.pax.web.service.jetty.internal.ResourceServlet.service(),
		// here path is already relative to context!

//		String contextName = context.getContextModel().getContextName();
//		if (contextName.isEmpty()) {
//			contextName = "/";
//		}
		String mapping = path;
		if (!"/".equals(alias)) {
			mapping = mapping.substring(alias.length());
		}
		if (!name.isEmpty() && !"default".equals(name)) {
			mapping = name + mapping;
		}
		return context.getResource(mapping);
	}

	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {

	}

	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {

	}

	@Override
	public void close() throws IOException {

	}

	/**
	 * Reconfigures default welcome pages with ones provided externally
	 * @param welcomePages
	 */
	public void configureWelcomeFiles(List<String> welcomePages) {
		this.welcomePages = welcomePages;
		((ResourceHandler) handler).setWelcomeFiles();
		((ResourceHandler) handler).addWelcomeFiles(welcomePages.toArray(new String[welcomePages.size()]));
	}

}
