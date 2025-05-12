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
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.ee8.nested.ServletRequestHttpWrapper;
import org.eclipse.jetty.ee8.nested.ServletResponseHttpWrapper;
import org.eclipse.jetty.ee8.security.UserAuthentication;
import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.eclipse.jetty.ee8.servlet.FilterMapping;
import org.eclipse.jetty.ee8.servlet.ListenerHolder;
import org.eclipse.jetty.ee8.servlet.ServletHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.servlet.ServletMapping;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.ArrayUtil;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiSessionAttributeListener;
import org.ops4j.pax.web.service.spi.servlet.PreprocessorFilterConfig;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Specialized {@link ServletHandler} to be used inside specialized
 * {@link org.eclipse.jetty.ee8.servlet.ServletContextHandler} for Pax Web specific invocation and management of
 * servlets.</p>
 *
 * <p>Remember (a note to myself as well), {@link ServletHandler} in Jetty is not for handling single servlet, it's
 * for handling <strong>all</strong> the servlets within single
 * {@link org.eclipse.jetty.ee8.servlet.ServletContextHandler}.</p>
 */
public class PaxWebServletHandler extends ServletHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebServletHandler.class);

	/**
	 * {@link Preprocessor} instances are always registered to all contexts and are always mapped to all servlet
	 * chains, so handling them is easy. We keep only the configs, because we have to manage the lifecycle
	 * of OSGi services for {@link Preprocessor} instances.
	 */
	private final List<PreprocessorFilterConfig> preprocessors = new LinkedList<>();

	/** Default {@link ServletContext} to use for chains without target servlet (e.g., filters only) */
	private OsgiServletContext defaultServletContext;
	/** Default {@link OsgiContextModel} to use for chains without target servlet (e.g., filters only) */
	private OsgiContextModel defaultOsgiContextModel;
	/** Default {@link WebContainerContext} for chains without target {@link Servlet} */
	private WebContainerContext defaultWebContainerContext;

	private final OsgiSessionAttributeListener osgiSessionsBridge;

	/**
	 * Default servlet to be used when there's nothing mapped under "/" - this is to ensure that filter-only
	 * chains will work without problems.
	 */
	private final Servlet default404Servlet;

	private final ThreadLocal<PaxWebServletHolder> currentServletHolder = new ThreadLocal<>();

	/**
	 * Create new {@link ServletHandler} for given {@link org.eclipse.jetty.ee8.servlet.ServletContextHandler}
	 * @param default404Servlet this servlet will be used when there's no mapped servlet
	 */
	PaxWebServletHandler(Servlet default404Servlet, OsgiSessionAttributeListener osgiSessionsBridge) {
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
		this.osgiSessionsBridge = osgiSessionsBridge;
	}

	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

	public OsgiServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel, WebContainerContext resolvedWebContainerContext) {
		this.defaultOsgiContextModel = defaultOsgiContextModel;
		this.defaultWebContainerContext = resolvedWebContainerContext;
	}

	public OsgiContextModel getDefaultOsgiContextModel() {
		return defaultOsgiContextModel;
	}

	@Override
	protected synchronized void doStart() throws Exception {
		// our version of default, fallback servlet registration
		if (getServletMapping("/") == null && isEnsureDefaultServlet()) {
			addServletWithMapping(new PaxWebServletHolder("default", default404Servlet, true), "/");
			getServletMapping("/").setFromDefaultDescriptor(true);
		}

		super.doStart();
	}

	@Override
	public void initialize() throws Exception {
		// initialize preprocessors
		for (PreprocessorFilterConfig fc : preprocessors) {
			fc.getInstance().init(fc);
		}

		try {
			super.initialize();
		} catch (Exception e) {
			// See https://github.com/ops4j/org.ops4j.pax.web/issues/1725
			// we don't want entire context to become UNAVAILABLE just because some servlets/filters
			// thrown javax.servlet.UnavailableException
			// in OSGi it is quite common and we should handle exceptions like ClassNotFoundException
			// in less fatal way
			LOG.error(e.getMessage(), e);
		}
	}

	@Override
	protected synchronized void doStop() throws Exception {
		// before stopping, we have to remove dynamic filters (servlets are removed in clear() visitor of
		// JettyServerWrapper) exactly here, because otherwise there'll be wrong index of
		// org.eclipse.jetty.servlet.ServletHandler._matchAfterIndex

		List<PaxWebFilterHolder> newFilters = new ArrayList<>();
		List<PaxWebFilterMapping> newFilterMappings = new ArrayList<>();
		for (FilterHolder fh : getFilters()) {
			if (fh instanceof PaxWebFilterHolder) {
				PaxWebFilterHolder pwfh = (PaxWebFilterHolder) fh;
				if (pwfh.getFilterModel() == null || pwfh.getFilterModel().isDynamic()) {
					try {
						pwfh.destroyInstance(pwfh.getInstance());
					} catch (Exception e) {
						LOG.warn("Problem destroying filter {}: {}", pwfh, e.getMessage(), e);
					}
					continue;
				}
				newFilters.add(pwfh);
				newFilterMappings.addAll(pwfh.getMapping());
			}
		}
		setFilters(newFilters.toArray(new PaxWebFilterHolder[0]));
		setFilterMappings(newFilterMappings.toArray(new PaxWebFilterMapping[0]));

		// destroy the preprocessors
		for (PreprocessorFilterConfig fc : preprocessors) {
			fc.destroy();
		}

		// Jetty 10+ keeps only "durable" servlets/filters/listeners. We're handling it a bit differently,
		// so we have to preserve them (because there's no reflection-free access to
		// org.eclipse.jetty.servlet.ServletHandler._durable field)

		ServletHolder[] servlets = getServlets();
		ServletMapping[] servletMappings = getServletMappings();
		FilterHolder[] filters = getFilters();
		FilterMapping[] filterMappings = getFilterMappings();
		ListenerHolder[] listeners = getListeners();

		super.doStop();

		super.setServlets(servlets);
		setServletMappings(servletMappings);
		setFilters(filters);
		setFilterMappings(filterMappings);
		setListeners(listeners);
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
	 * Override the method from {@link org.eclipse.jetty.ee8.servlet.ServletContextHandler} just because
	 * {@code org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer} adds
	 * {@link FilterHolder} directly, while we use {@link PaxWebFilterHolder} array.
	 * @param holder
	 * @param pathSpec
	 * @param dispatches
	 */
	@Override
	public void addFilterWithMapping(FilterHolder holder, String pathSpec, EnumSet<DispatcherType> dispatches) {
		if (holder instanceof PaxWebFilterHolder) {
			super.addFilterWithMapping(holder, pathSpec, dispatches);
			return;
		}

		PaxWebFilterHolder paxWebFilterHolder = new PaxWebFilterHolder(holder, defaultServletContext);

		super.addFilterWithMapping(paxWebFilterHolder, pathSpec, dispatches);
	}

	@Override
	public void prependFilter(FilterHolder filter) {
		if (filter instanceof PaxWebFilterHolder) {
			super.prependFilter(filter);
			return;
		}

		PaxWebFilterHolder paxWebFilterHolder = new PaxWebFilterHolder(filter, defaultServletContext);

		super.prependFilter(paxWebFilterHolder);
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
				holder.stop();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		if (getServletMapping("/") == null && isEnsureDefaultServlet()) {
			addServletWithMapping(new PaxWebServletHolder("default", default404Servlet, true), "/");
			getServletMapping("/").setFromDefaultDescriptor(true);
		}
	}

	/**
	 * Jetty {@link ServletHandler#doHandle(String, org.eclipse.jetty.ee8.nested.Request, HttpServletRequest, HttpServletResponse)} is not just
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
	public void doHandle(String target, org.eclipse.jetty.ee8.nested.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//		super.doHandle(target, baseRequest, request, response);
		if ("TRACE".equals(request.getMethod())) {
			// PAXWEB-229 - prevent https://owasp.org/www-community/attacks/Cross_Site_Tracing
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			baseRequest.setHandled(true);
			return;
		}

		// whether there are filters or not, we *copy* code from super.doHandle() to ensure that
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

	protected FilterChain getOsgiFilterChain(final org.eclipse.jetty.ee8.nested.Request baseRequest, String pathInContext, ServletHolder servletHolder) {
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

		// listener called when org.osgi.service.http.HttpContext.handleSecurity() returns true
		Consumer<HttpServletRequest> authListener = (req) -> {
			final Object user = req.getAttribute(ServletContextHelper.REMOTE_USER);
			final Object authType = req.getAttribute(ServletContextHelper.AUTHENTICATION_TYPE);

			if (user != null || authType != null) {
				// translate it into Jetty specific authentication
				if (baseRequest.getAuthentication() == null) {
					String userName = user != null ? user.toString() : null;
					String authMethod = authType != null ? authType.toString() : null;
					Principal p = new UserPrincipal(userName, null);
					Subject s = new Subject(true, Collections.singleton(p), Collections.emptySet(), Collections.emptySet());
					baseRequest.setAuthentication(new UserAuthentication(authMethod, new DefaultUserIdentity(s, p)));
				}
			}
		};

		List<Preprocessor> preprocessorInstances = preprocessors.stream().map(PreprocessorFilterConfig::getInstance).collect(Collectors.toList());
		if (!holder.is404()) {
			return new OsgiFilterChain(new ArrayList<>(preprocessorInstances), holder.getOsgiServletContext(),
					holder.getWebContainerContext(), chain, osgiSessionsBridge, authListener);
		} else {
			return new OsgiFilterChain(new ArrayList<>(preprocessorInstances), defaultServletContext,
					defaultWebContainerContext, chain, osgiSessionsBridge, authListener);
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
	protected FilterChain getFilterChain(org.eclipse.jetty.ee8.nested.Request baseRequest, String pathInContext, ServletHolder servletHolder) {
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

	public List<PreprocessorFilterConfig> getPreprocessors() {
		return preprocessors;
	}

	private static class DefaultUserIdentity implements UserIdentity {
		private final Subject subject;
		private final Principal principal;

		private DefaultUserIdentity(Subject subject, Principal principal) {
			this.subject = subject;
			this.principal = principal;
		}

		@Override
		public Subject getSubject() {
			return subject;
		}

		@Override
		public Principal getUserPrincipal() {
			return principal;
		}

		@Override
		public boolean isUserInRole(String role) {
			return false;
		}
	}

}
