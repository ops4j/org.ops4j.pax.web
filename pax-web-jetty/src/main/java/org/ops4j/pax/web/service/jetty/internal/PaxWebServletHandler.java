/*
 * Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServletRequestHttpWrapper;
import org.eclipse.jetty.server.ServletResponseHttpWrapper;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Specialized {@link ServletHandler} to be used inside specialized
 * {@link org.eclipse.jetty.servlet.ServletContextHandler} for Pax Web specific invocation and management of
 * servlets.</p>
 *
 * <p>Remember (a note to myself as well), {@link ServletHandler} in Jetty is not for handling single servlet, it's
 * for handling <strong>all</strong> the servlets within single
 * {@link org.eclipse.jetty.servlet.ServletContextHandler}.</p>
 */
public class PaxWebServletHandler extends ServletHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebServletHandler.class);

	/**
	 * {@link Preprocessor} instances are always registered to all contexts and are always mapped to all servlet
	 * chains, so handling them is easy.
	 */
	private final List<Preprocessor> preprocessors = new LinkedList<>();

	private static final Servlet default404Servlet = new Default404Servlet();

	@Review("Move this comment to server-agnostic code")
	PaxWebServletHandler() {
		// we need default servlet for these reasons:
		// 1. there HAS TO be something that'll send 404 if nothing is found within given ServletContextHandler
		// 2. without mapped servlet, even 404 one, no filter chain will be created, so we won't be able
		//    to configure a context with filters only.
		// 3. we should now if the invocation pipeline (filters + servlet) consists only of default 404 servlet, in
		//    which case we should skip OSGi Whiteboard Preprocessors and handleSecurity() (Specification doesn't
		//    say anything about it)
		setEnsureDefaultServlet(true);
	}

	@Override
	protected synchronized void doStart() throws Exception {
		// our version of default, fallback servlet registration
		if (getServletMapping("/") == null && isEnsureDefaultServlet()) {
			addServletWithMapping(new PaxWebServletHolder("default", default404Servlet), "/");
			getServletMapping("/").setDefault(true);
		}

		super.doStart();
	}

	@Override
	public synchronized void setServlets(ServletHolder[] holders) {
		// sanity check
		for (ServletHolder holder : holders) {
			if (!(holder instanceof PaxWebServletHolder)) {
				throw new IllegalArgumentException("This ServletHandler should manage only PaxWebServletHolders");
			}
		}
		super.setServlets(holders);
	}

	/**
	 * Jetty {@link ServletHandler#doHandle(String, Request, HttpServletRequest, HttpServletResponse)} is not just
	 * about calling a servlet. It's about preparation of entire chain of invocation and mapping of incoming request
	 * into some target servlet + associated filters.
	 *
	 * @param target
	 * @param baseRequest
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		if ("TRACE".equals(request.getMethod())) {
			// PAXWEB-229 - prevent https://owasp.org/www-community/attacks/Cross_Site_Tracing
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			baseRequest.setHandled(true);
			return;
		}

		if (getFilterMappings() == null || getFilterMappings().length == 0) {
			// getFilterChain() won't be called in super.doHandle(), but we need it anyway.
			// this if{} is based on super.doHandle()

			// this should never be null because of ServletHandler.setEnsureDefaultServlet(true)
			PaxWebServletHolder servletHolder = (PaxWebServletHolder)baseRequest.getUserIdentityScope();

			try {
				// we always create the chain
				FilterChain chain = getFilterChain(baseRequest, target, servletHolder);

				// unwrap any tunnelling of base Servlet request/responses
				ServletRequest req = request;
				if (req instanceof ServletRequestHttpWrapper)
					req = ((ServletRequestHttpWrapper) req).getRequest();
				ServletResponse res = response;
				if (res instanceof ServletResponseHttpWrapper)
					res = ((ServletResponseHttpWrapper) res).getResponse();

				// Do the filter/handling thang
				servletHolder.prepare(baseRequest, req, res);

				// chain still can be null if the servlet is default404Servlet
				if (chain != null) {
					chain.doFilter(req, res);
				} else {
					servletHolder.handle(baseRequest, req, res);
				}
			} finally {
				if (servletHolder != null)
					baseRequest.setHandled(true);
			}
		} else {
			// filters are present, so super.doHandle() will call our getFilterChain()
			// TODO: Jetty ensures mapped filters in the chain, but OSGi CPMN spec requires that these should
			//       be only the filters registered with the same context...
			super.doHandle(target, baseRequest, request, response);
		}
	}

	@Override
	protected FilterChain getFilterChain(final Request baseRequest, String pathInContext, ServletHolder servletHolder) {
		PaxWebServletHolder holder = (PaxWebServletHolder) servletHolder;

		// either super.getFilterChain() will return a chain that should be invoked once (which will provide
		// proper behavior filter -> filter -> ... -> filter -> servlet)
		// or we'll get null and servletHolder will be everything we have
		FilterChain chain = super.getFilterChain(baseRequest, pathInContext, servletHolder);

		if (chain == null && holder.isInstance() && holder.getInstance() == default404Servlet) {
			// no need to do OSGi Http / Whiteboard stuff. Jetty will just call default404Servlet
			return null;
		}

		// We need different FilterChain that will invoke (in this order):
		// 1. all org.osgi.service.http.whiteboard.Preprocessors
		// 2. handleSecurity() (on HttpContext or ServletContextHelper)
		// 3. original chain
		if (chain == null) { // 3a. (even if there's only a ServletHolder there)
			chain = (request, response) -> holder.handle(baseRequest, request, response);
		}
		return new OsgiFilterChain(preprocessors, holder.getServletContext(), holder.getOsgiContextModel(), chain);
	}

	private static class Default404Servlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

}
