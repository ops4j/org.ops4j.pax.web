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
package org.ops4j.pax.web.service.tomcat.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerKey;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.Default404Servlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiFilterChain;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiSessionAttributeListener;
import org.ops4j.pax.web.service.spi.servlet.PreprocessorFilterConfig;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.Preprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link StandardContext} that keeps track of default
 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} and
 * {@link jakarta.servlet.ServletContext} to use for chains that do not have target servlet mapped. These are
 * required by filters which may be associated with such servlet-less chains.
 */
public class PaxWebStandardContext extends StandardContext {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebStandardContext.class);

	/**
	 * Name of an attribute that indicates a {@link PaxWebStandardContext} for given request processing
	 */
	public static final String PAXWEB_STANDARD_CONTEXT = ".paxweb.standard.context";
	/**
	 * Name of an attribute that indicates a {@link PaxWebStandardWrapper} for given request processing
	 */
	public static final String PAXWEB_STANDARD_WRAPPER = ".paxweb.standard.wrapper";
	/**
	 * Name of the attribute for real {@link org.apache.catalina.connector.Request}
	 */
	public static final String PAXWEB_TOMCAT_REQUEST = ".paxweb.tomcat.request";

	/**
	 * Default {@link ServletContext} to use for chains without target servlet (e.g., filters only)
	 */
	private OsgiServletContext defaultServletContext;
	/**
	 * Default {@link OsgiContextModel} to use for chains without target servlet (e.g., filters only)
	 */
	private OsgiContextModel defaultOsgiContextModel;
	/**
	 * Default {@link WebContainerContext} for chains without target {@link Servlet}
	 */
	private WebContainerContext defaultWebContainerContext;

	private String osgiInitFilterName;

	/**
	 * Highest ranked {@link OsgiServletContext} set when Tomcat's context starts
	 */
	private ServletContext osgiServletContext;

	/**
	 * {@link Preprocessor} are registered as filters, but without particular target
	 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}, so they're effectively registered in
	 * all available physical servlet contexts.
	 */
	private final List<PreprocessorFilterConfig> preprocessors = new LinkedList<>();

	private final Collection<SCIWrapper> servletContainerInitializers = new LinkedList<>();

	/**
	 * This maps keeps all the listeners in order, as expected by OSGi CMPN R7 Whiteboard specification.
	 */
	private final Map<EventListenerKey, Object> rankedListeners = new TreeMap<>();

	/**
	 * Here we'll keep the listeners without associated {@link EventListenerModel}
	 */
	private final List<Object> orderedListeners = new ArrayList<>();

	private final OsgiSessionAttributeListener osgiSessionsBridge;

	// as in org.eclipse.jetty.server.handler.ContextHandler._vhosts, _vhostswildcard and _vconnectors
	private String[] virtualHosts;
	private boolean[] virtualHostWildcards;
	private String[] connectorNames;
	private boolean whiteboardTCCL;

	public PaxWebStandardContext(Default404Servlet defaultServlet, OsgiSessionAttributeListener osgiSessionsBridge) {
		super();
		getPipeline().addValve(new PaxWebStandardContextValve((ValveBase) getPipeline().getBasic(), defaultServlet));
		this.osgiSessionsBridge = osgiSessionsBridge;
		this.setClearReferencesObjectStreamClassCaches(false);
		this.setClearReferencesRmiTargets(false);
		this.setClearReferencesThreadLocals(false);
	}

	/**
	 * Called just after creation of this {@link StandardContext} to add first filter that will handle OSGi specifics.
	 * Due to Tomcat's usage of static and final methods, it's really far from beautiful code.
	 */
	public void createInitialOsgiFilter() {
		// turn a chain into a filter - to satisfy Tomcat's static methods
		Filter osgiInitFilter = (request, response, chain) -> {
			// this is definitiely the first filter, so we should get these attributes
			PaxWebStandardContext delegate = PaxWebStandardContext.this;
			PaxWebStandardWrapper wrapper = (PaxWebStandardWrapper) request.getAttribute(PAXWEB_STANDARD_WRAPPER);
			request.removeAttribute(PAXWEB_STANDARD_WRAPPER);
			Request tomcatRequest = (Request) request.getAttribute(PAXWEB_TOMCAT_REQUEST);
			request.removeAttribute(PAXWEB_TOMCAT_REQUEST);

			if (wrapper == null) {
				Container[] children = PaxWebStandardContext.this.findChildren();
				for (Container c : children) {
					if (c instanceof PaxWebStandardWrapper && request instanceof HttpServletRequest
							&& c.getName() != null
							&& c.getName().equals(((HttpServletRequest) request).getHttpServletMapping().getServletName())) {
						wrapper = (PaxWebStandardWrapper) c;
					}
				}
			}

			// listener called when org.osgi.service.http.HttpContext.handleSecurity() returns true
			Consumer<HttpServletRequest> authListener = (req) -> {
				final Object user = req.getAttribute(ServletContextHelper.REMOTE_USER);
				final Object authType = req.getAttribute(ServletContextHelper.AUTHENTICATION_TYPE);

				if (user != null || authType != null) {
					// translate it into Tomcat specific authentication
					String userName = user != null ? user.toString() : null;
					String authMethod = authType != null ? authType.toString() : null;
					if (tomcatRequest.getPrincipal() == null) {
						tomcatRequest.setUserPrincipal(new GenericPrincipal(userName, null, Collections.emptyList()));
					}
				}
			};

			final OsgiFilterChain osgiChain;
			List<Preprocessor> preprocessorInstances = preprocessors.stream().map(PreprocessorFilterConfig::getInstance).collect(Collectors.toList());
			if (wrapper != null && !wrapper.is404()) {
				osgiChain = new OsgiFilterChain(new ArrayList<>(preprocessorInstances),
						wrapper.getServletContext(), wrapper.getWebContainerContext(), null, osgiSessionsBridge, authListener);
			} else {
				osgiChain = new OsgiFilterChain(new ArrayList<>(preprocessorInstances),
						delegate.getDefaultServletContext(), delegate.getDefaultWebContainerContext(), null, osgiSessionsBridge, authListener);
			}

			// this chain will be called (or not)
			osgiChain.setChain(chain);
			osgiChain.doFilter(request, response);
		};

		FilterModel filterModel = new FilterModel("__osgi@" + System.identityHashCode(osgiInitFilter),
				new String[] { "*" }, null, null, osgiInitFilter, null, true);
		filterModel.getMappingsPerDispatcherTypes().get(0).setDispatcherTypes(new DispatcherType[] {
				DispatcherType.ERROR,
				DispatcherType.FORWARD,
				DispatcherType.INCLUDE,
				DispatcherType.REQUEST,
				DispatcherType.ASYNC
		});
		PaxWebFilterDef osgiInitFilterDef = new PaxWebFilterDef(filterModel, true, null);
		osgiInitFilterDef.setWhiteboardTCCL(whiteboardTCCL);
		PaxWebFilterMap osgiInitFilterMap = new PaxWebFilterMap(filterModel, true);

		addFilterDef(osgiInitFilterDef);
		addFilterMapBefore(osgiInitFilterMap);
	}

	/**
	 * This method may be called long after initial filter was created. In Jetty and Undertow there's no
	 * <em>initial</em> filter, because we can do it better, but with Tomcat we have to do it like this.
	 *
	 * @param defaultServletContext
	 */
	public void setDefaultServletContext(OsgiServletContext defaultServletContext) {
		this.defaultServletContext = defaultServletContext;
	}

	/**
	 * We have to ensure that this {@link StandardContext} will always return
	 * proper instance of {@link jakarta.servlet.ServletContext} - especially in the events passed to listeners
	 *
	 * @param osgiServletContext
	 */
	public void setOsgiServletContext(ServletContext osgiServletContext) {
		this.osgiServletContext = osgiServletContext;
	}

	@Override
	public void addServletContainerInitializer(ServletContainerInitializer sci, Set<Class<?>> classes) {
		// we don't want initializers in Tomcat's context, because we manage them ourselves
	}

	public void setServletContainerInitializers(Collection<SCIWrapper> wrappers) {
		this.servletContainerInitializers.clear();
		this.servletContainerInitializers.addAll(wrappers);
	}

	@Override
	public ServletContext getServletContext() {
		// we have to initialize it if it's not done already
		ServletContext superContext = super.getServletContext();
		if (osgiServletContext != null) {
			return osgiServletContext;
		}
		return superContext;
	}

	@Override
	public boolean filterStart() {
		for (PreprocessorFilterConfig fc : preprocessors) {
			try {
				fc.getInstance().init(fc);
			} catch (ServletException e) {
				LOG.warn("Problem during preprocessor initialization: {}", e.getMessage(), e);
			}
		}

		return super.filterStart();
	}

	@Override
	public boolean filterStop() {
		boolean result = super.filterStop();

		// destroy the preprocessors
		for (PreprocessorFilterConfig fc : preprocessors) {
			fc.destroy();
		}

		return result;
	}

	/**
	 * Handy method to check if the context is started for OSGi purposes
	 *
	 * @return
	 */
	public boolean isStarted() {
		return getState() == LifecycleState.STARTED
				|| getState() == LifecycleState.STARTING
				|| getState() == LifecycleState.STARTING_PREP
				|| getState() == LifecycleState.INITIALIZING;
	}

	@Override
	public boolean listenerStart() {
		// This is a method overriden JUST because it is invoked right after original
		// org.apache.catalina.core.StandardContext.startInternal() invokes ServletContainerInitializers.
		// We have to call SCIs ourselves to pass better OsgiServletContext there.

		// I know listenerStart() is NOT the method which should invoke SCIs, but hey - we want to stay as consistent
		// between Jetty, Tomcat and Undertow in Pax Web as possible

		boolean ok = true;
		for (SCIWrapper wrapper : servletContainerInitializers) {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(getParentClassLoader());
				wrapper.onStartup();
			} catch (ServletException e) {
				LOG.error(sm.getString("standardContext.sciFail"), e);
				ok = false;
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}

		if (ok) {
			// first, Tomcat doesn't have to be aware of ANY application lifecycle listeners (call this method
			// through super pointer!)
			// only when it sets us the instances of listeners (we override this method) we can start returning
			// them - that's the only way to prevent Tomcat passing org.apache.catalina.core.StandardContext.NoPluggabilityServletContext
			// to our listeners
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(getParentClassLoader());
				super.setApplicationLifecycleListeners(new Object[0]);
				return super.listenerStart();
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		}

		return false;
	}

	@Override
	public ClassLoader bind(boolean usePrivilegedAction, ClassLoader originalClassLoader) {
		// no-op
		return originalClassLoader;
	}

	@Override
	protected synchronized void stopInternal() throws LifecycleException {
		// org.apache.catalina.core.StandardContext.resetContext() will be call so we have to preserve some
		// items from the context
		Container[] children = findChildren();

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(osgiServletContext.getClassLoader());
			// this will clear the listeners, but we'll add them again when (re)starting the context
			super.stopInternal();

			for (Container child : children) {
				if (child instanceof PaxWebStandardWrapper) {
					PaxWebStandardWrapper pwsw = ((PaxWebStandardWrapper) child);
					ServletModel model = pwsw.getServletModel();
					OsgiScopedServletContext osgiServletContext = (OsgiScopedServletContext) pwsw.getServletContext();
					PaxWebStandardWrapper wrapper = new PaxWebStandardWrapper(model,
							pwsw.getOsgiContextModel(), osgiServletContext.getOsgiContext(), this);
					wrapper.setWhiteboardTCCL(whiteboardTCCL);

					boolean isDefaultResourceServlet = model.isResourceServlet();
					for (String pattern : model.getUrlPatterns()) {
						isDefaultResourceServlet &= "/".equals(pattern);
					}
					if (model.isResourceServlet()) {
						wrapper.addInitParameter("pathInfoOnly", Boolean.toString(!isDefaultResourceServlet));
					}
					addChild(wrapper);

					// <servlet-mapping>
					String name = model.getName();
					for (String pattern : model.getUrlPatterns()) {
						removeServletMapping(pattern);
						addServletMappingDecoded(pattern, name, false);
					}

					// are there any error page declarations in the model?
					ErrorPageModel epm = model.getErrorPageModel();
					if (epm != null && epm.isValid()) {
						String location = epm.getLocation();
						for (String ex : epm.getExceptionClassNames()) {
							ErrorPage errorPage = new ErrorPage();
							errorPage.setExceptionType(ex);
							errorPage.setLocation(location);
							addErrorPage(errorPage);
						}
						for (int code : epm.getErrorCodes()) {
							ErrorPage errorPage = new ErrorPage();
							errorPage.setErrorCode(code);
							errorPage.setLocation(location);
							addErrorPage(errorPage);
						}
						if (epm.isXx4()) {
							for (int c = 400; c < 500; c++) {
								ErrorPage errorPage = new ErrorPage();
								errorPage.setErrorCode(c);
								errorPage.setLocation(location);
								addErrorPage(errorPage);
							}
						}
						if (epm.isXx5()) {
							for (int c = 500; c < 600; c++) {
								ErrorPage errorPage = new ErrorPage();
								errorPage.setErrorCode(c);
								errorPage.setLocation(location);
								addErrorPage(errorPage);
							}
						}
					}
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}

		// clear the OSGi context - new one will be set when the context is started again
		setOsgiServletContext(null);

		// remove the listeners without associated EventListenerModel from rankedListeners map
		rankedListeners.entrySet().removeIf(e -> e.getKey().getRanklessPosition() >= 0);
		// ALL listeners added without a model (listeners added by SCIs and other listeners) will be cleared
		orderedListeners.clear();
	}

	@Override
	public void addApplicationEventListener(Object listener) {
		addApplicationEventListener(null, listener);
	}

	/**
	 * Special {@code addApplicationEventListener()} that should be called instead of
	 * {@link #addApplicationEventListener(Object)}, because we want to sort the listeners according to
	 * Whiteboard/ranking rules.
	 *
	 * @param model
	 * @param listener
	 */
	public void addApplicationEventListener(EventListenerModel model, Object listener) {
		// we're not adding the listener to StandardContext - we'll add all listeners when the context is started

		if (model == null || model.isDynamic()) {
			orderedListeners.add(listener);
		} else {
			rankedListeners.put(EventListenerKey.ofModel(model), listener);
		}

		if (!ServletContextListener.class.isAssignableFrom(listener.getClass())) {
			// otherwise it'll be added anyway when context is started, because such listener can
			// be added only for stopped context
			if (isStarted()) {
				// we have to add it, because there'll be no restart
				super.addApplicationEventListener(listener);
			}
		}
	}

	@Override
	public void addApplicationLifecycleListener(Object listener) {
		addApplicationLifecycleListener(null, listener);
	}

	public void addApplicationLifecycleListener(EventListenerModel model, Object listener) {
		// for now, we mix lifecycle and event listeners
		addApplicationEventListener(model, listener);
	}

	/**
	 * When removing listeners, we have to remove them from managed ordered lists - whether it's lifecycle or
	 * event listener.
	 *
	 * @param listener
	 */
	public void removeListener(EventListenerModel model, Object listener) {
		if (model == null || model.isDynamic()) {
			orderedListeners.remove(listener);
		} else {
			rankedListeners.remove(EventListenerKey.ofModel(model));
		}
	}

	@Override
	public void setApplicationLifecycleListeners(Object[] listeners) {
		if (getState() == LifecycleState.STOPPING) {
			// it's null anyway
			super.setApplicationLifecycleListeners(listeners);
			return;
		}

		// when Tomcat sets here the listener instances, we'll alter the array with the instances we've collected

		// we have to prevent adding the same listeners multiple times - this may happen when Tomcat
		// context is restarted and we have a mixture of Whiteboards listeners, listeners added by SCIs and
		// listeners from other listener

		// SCIs may have added some listeners which we've hijacked, to order them according
		// to Whiteboard/ranking rules. Now it's perfect time to add them in correct order
		for (int pos = 0; pos < orderedListeners.size(); pos++) {
			Object el = orderedListeners.get(pos);
			rankedListeners.put(EventListenerKey.ofPosition(pos), el);
		}

		// Add all listeners as "pluggability listeners"
		List<Object> lifecycleListeners = new ArrayList<>();
		List<Object> eventListeners = new ArrayList<>();
		for (Object listener : rankedListeners.values()) {
			if (listener instanceof ServletContextListener || listener instanceof HttpSessionListener) {
				lifecycleListeners.add(listener);
			}
			// because ServletContextListener's implementation may implement other listener interfaces too
			eventListeners.add(listener);
		}

		super.setApplicationLifecycleListeners(lifecycleListeners.toArray());
		super.setApplicationEventListeners(eventListeners.toArray());
	}

	public void setDefaultOsgiContextModel(OsgiContextModel defaultOsgiContextModel, WebContainerContext resolvedWebContainerContext) {
		this.defaultOsgiContextModel = defaultOsgiContextModel;
		this.defaultWebContainerContext = resolvedWebContainerContext;
	}

	public OsgiServletContext getDefaultServletContext() {
		return defaultServletContext;
	}

	public OsgiContextModel getDefaultOsgiContextModel() {
		return defaultOsgiContextModel;
	}

	public WebContainerContext getDefaultWebContainerContext() {
		return defaultWebContainerContext;
	}

	public List<PreprocessorFilterConfig> getPreprocessors() {
		return preprocessors;
	}

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
	 * Tomcat's approach to Virtual Hosts, but at context level.
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

	public void setWhiteboardTCCL(boolean whiteboardTCCL) {
		this.whiteboardTCCL = whiteboardTCCL;
	}

}
