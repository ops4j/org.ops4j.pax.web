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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.ServletRequestHttpWrapper;
import org.eclipse.jetty.server.ServletResponseHttpWrapper;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ArrayUtil;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
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

	/** Default {@link ServletContext} to use for chains without target servlet (e.g., filters only) */
	private OsgiServletContext defaultServletContext;
	/** Default {@link OsgiContextModel} to use for chains without target servlet (e.g., filters only) */
	private OsgiContextModel defaultOsgiContextModel;
	/** Default {@link WebContainerContext} for chains without target {@link Servlet} */
	private WebContainerContext defaultWebContainerContext;

	/**
	 * Default servlet to be used when there's nothing mapped under "/" - this is to ensure that filter-only
	 * chains will work without problems.
	 */
	private final Servlet default404Servlet;

	private final ThreadLocal<PaxWebServletHolder> currentServletHolder = new ThreadLocal<>();

	/**
	 * Create new {@link ServletHandler} for given {@link org.eclipse.jetty.servlet.ServletContextHandler}
	 * @param default404Servlet this servlet will be used when there's no mapped servlet
	 */
	@Review("Move this comment to server-agnostic code")
	PaxWebServletHandler(Servlet default404Servlet) {
		// we need default servlet for these reasons:
		// 1. there HAS TO be something that'll send 404 if nothing is found within given ServletContextHandler
		// 2. without mapped servlet, even 404 one, no filter chain will be created, so we won't be able
		//    to configure a context with filters only.
		// 3. we should know if the invocation pipeline (filters + servlet) consists only of default 404 servlet, in
		//    which case we should skip OSGi Whiteboard Preprocessors and handleSecurity() (Specification doesn't
		//    say anything about it)
		setEnsureDefaultServlet(true);

		// that's important, we will use the cache but on OSGi-specific terms (different key)
		setFilterChainsCached(true);
		// a bit more than default, because we're also caching by context-specific cache key
		int cacheSize = getMaxFilterChainsCacheSize();
		setMaxFilterChainsCacheSize(2 * cacheSize);

		setFilters(new PaxWebFilterHolder[0]);

		this.default404Servlet = default404Servlet;
	}

	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

	public OsgiServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel) {
		// TODO: release previous WebContainerContext
		this.defaultOsgiContextModel = defaultOsgiContextModel;
		if (defaultOsgiContextModel != null) {
			this.defaultWebContainerContext = defaultOsgiContextModel.resolveHttpContext(defaultOsgiContextModel.getOwnerBundle());
		}
	}

	public OsgiContextModel getDefaultOsgiContextModel() {
		return defaultOsgiContextModel;
	}

	@Override
	protected synchronized void doStart() throws Exception {
		// our version of default, fallback servlet registration
		if (getServletMapping("/") == null && isEnsureDefaultServlet()) {
			addServletWithMapping(new PaxWebServletHolder("default", default404Servlet, true), "/");
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
	 * Special method that makes it easier later to remove given holder with associated mapping
	 * @param holder
	 * @param mapping
	 */
	public void addServletWithMapping(PaxWebServletHolder holder, ServletMapping mapping) {
		holder.setMapping(mapping);
		addServlet(holder);
		addServletMapping(mapping);
	}

	/**
	 * Removes {@link PaxWebServletHolder} and its mapping - both associated with given {@link ServletModel}
	 * @param model
	 */
	public void removeServletWithMapping(ServletModel model) {
		ServletHolder[] holders = getServlets();
		ServletMapping[] mappings = getServletMappings();

		if (holders != null && mappings != null) {
			// there's only one servlet with given name - ensured by many classes in Pax Web
			PaxWebServletHolder holder = (PaxWebServletHolder) getServlet(model.getName());
			if (holder == null) {
				throw new IllegalArgumentException("Can't unregister servlet named \"" + model.getName() + "\" "
						+ "from Jetty servlet handler of " + defaultOsgiContextModel.getContextPath() + " context");
			}

			ServletMapping mapping = holder.getMapping();
			setServlets(ArrayUtil.removeFromArray(holders, holder));
			setServletMappings(ArrayUtil.removeFromArray(mappings, mapping));

			// if servlet is still started stop the servlet holder (=servlet.destroy()) as Jetty will not do that
			LOG.debug("Stopping servlet holder {}", holder);
			try {
//				ContextClassLoaderUtils.doWithClassLoader(
//						context.getClassLoader(), new Callable<Void>() {
//
//							@Override
//							public Void call() throws Exception {
//								holder.stop();
//								return null;
//							}
//
//						});
				holder.stop();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		if (getServletMapping("/") == null && isEnsureDefaultServlet()) {
			addServletWithMapping(new PaxWebServletHolder("default", default404Servlet, true), "/");
			getServletMapping("/").setDefault(true);
		}
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

		// wheter there are filters or not, we *copy* code from super.doHandle() to ensure that
		// getOsgiFilterChain() is called

		// this should never be null because of ServletHandler.setEnsureDefaultServlet(true)
		PaxWebServletHolder servletHolder = (PaxWebServletHolder)baseRequest.getUserIdentityScope();

		try {
			// we always create the chain, because we have to call handleSecurity()/finishSecurity()
			FilterChain chain = getOsgiFilterChain(baseRequest, target, servletHolder);

			// unwrap any tunnelling of base Servlet request/responses
			ServletRequest req = request;
			if (req instanceof ServletRequestHttpWrapper) {
				req = ((ServletRequestHttpWrapper) req).getRequest();
			}
			ServletResponse res = response;
			if (res instanceof ServletResponseHttpWrapper) {
				res = ((ServletResponseHttpWrapper) res).getResponse();
			}

			// set some attributes in the request
			servletHolder.prepare(baseRequest, req, res);

			// chain still can be null if the servlet is default404Servlet
			if (chain != null) {
				chain.doFilter(req, res);
			} else {
				servletHolder.handle(baseRequest, req, res);
			}
		} finally {
			if (servletHolder != null) {
				baseRequest.setHandled(true);
			}
		}
	}

	protected FilterChain getOsgiFilterChain(final Request baseRequest, String pathInContext, ServletHolder servletHolder) {
		PaxWebServletHolder holder = (PaxWebServletHolder) servletHolder;

		// either super.getFilterChain() will return a chain that should be invoked once (which will provide
		// proper behavior filter -> filter -> ... -> filter -> servlet)
		// or we'll get null and servletHolder will be everything we have

		// remember - if we want to leverage Jetty's filter caching, we have to wrap every filter
		// with a wrapper that decides if the filter should actually be called, because of "140.5 Registering Servlet
		// Filters":
		//
		//     Servlet filters are only applied to servlet requests if they are bound to the same Servlet Context
		//     Helper and the same Http Whiteboard implementation.
		//
		// otherwise we'd have to construct the chain on every call.
		// also, we have to handle case where filters are called in a chain that doesn't have a target servlet at all

		FilterChain chain = getFilterChain(baseRequest, pathInContext, servletHolder);

		// 140.5.1 Servlet Pre-Processors
		// A Preprocessor is invoked before request dispatching is performed. If multiple pre-processors
		// are registered they are invoked in the order as described for servlet filters.
		//
		// this means that even if there's no matching target servlet or filters, we HAVE to call preprocessors
		// felix.http doesn't call handleSecurity() if there's no mapped servlet
		// (see org.apache.felix.http.base.internal.dispatch.Dispatcher#dispatch())

		// We need different FilterChain that will invoke (in this order):
		// 1. all org.osgi.service.http.whiteboard.Preprocessors
		// 2. handleSecurity() (on HttpContext or ServletContextHelper)
		// 3. original chain
		if (chain == null) {
			// 3a. even if there's only a ServletHolder there == null chain
			// 3b. if the holder is for known 404 servlet, we still need a chain that calls 404 servlet
			chain = (request, response) -> holder.handle(baseRequest, request, response);
		}
		if (!holder.is404()) {
			return new OsgiFilterChain(preprocessors, holder.getServletContext(),
					holder.getWebContainerContext(), chain);
		} else {
			return new OsgiFilterChain(preprocessors, defaultServletContext, defaultWebContainerContext, chain);
		}
	}

	/**
	 * Overriden, because we want our own cache management, where key includes proper OSGi context
	 * @param baseRequest
	 * @param pathInContext
	 * @param servletHolder
	 * @return
	 */
	@Override
	protected FilterChain getFilterChain(Request baseRequest, String pathInContext, ServletHolder servletHolder) {
		PaxWebServletHolder holder = (PaxWebServletHolder) servletHolder;

		// calculate caching key for filter chain
		WebContainerContext wcc = holder.getWebContainerContext();
		String prefix = wcc == null ? "" : wcc.getContextId() + "|";
		if (wcc != null && wcc.isShared()) {
			prefix = "~|" + prefix;
		}
		String contextlessKey = pathInContext == null ? holder.getName() : pathInContext;
		String key = prefix + contextlessKey;

		int dispatch = FilterMapping.dispatch(baseRequest.getDispatcherType());

		FilterChain chain = _chainCache[dispatch].get(key);
		if (chain != null) {
			return chain;
		}

		// always clear contextlessKey in parent cache, so super.getFilterChain will create new filter chain
		_chainCache[dispatch].remove(contextlessKey);

		// After an update to Jetty 9.4.34, we have to use different way of rejecting filters from the chain if
		// they don't match OSGi context... See https://github.com/eclipse/jetty.project/pull/5271
		currentServletHolder.set(holder);
		try {
			chain = super.getFilterChain(baseRequest, pathInContext, servletHolder);
		} finally {
			currentServletHolder.remove();
		}

		// the above chain:
		// 1) may be null if there are no filters at all
		// 2) may be not null, but all the filters may have to be removed because they could not match target servlet
		// 3) created new cache entry which we don't want directly, but it can be useful when accessing
		//    a servlet through different OSGi context

		if (chain != null) {
			_chainCache[dispatch].put(key, chain);
		}

		return chain;
	}

	@Override
	protected FilterChain newFilterChain(FilterHolder filterHolder, FilterChain chain) {
		// This is where we can narrow the list of filters, which Jetty decided to map to given servlet
		// we can additionally take OSGi context into account
		PaxWebServletHolder holder = currentServletHolder.get();

		OsgiContextModel targetContext = holder.getOsgiContextModel();
		if (targetContext == null) {
			targetContext = defaultOsgiContextModel;
		}

		PaxWebFilterHolder fHolder = (PaxWebFilterHolder) filterHolder;
		if (fHolder.matches(targetContext)) {
			// create new chain with filterHolder called first and existing chain called later
			return super.newFilterChain(filterHolder, chain);
		} else {
			// just return existing chain without using this filterHolder
			return chain;
		}
	}

}
