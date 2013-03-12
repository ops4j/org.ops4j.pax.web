/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.startup.Tomcat.ExistingStandardWrapper;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Romain Gilles
 */
class TomcatServerWrapper implements ServerWrapper {
	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServerWrapper.class);
	private final EmbeddedTomcat m_server;
	private final Map<HttpContext, Context> m_contexts = new ConcurrentHashMap<HttpContext, Context>();
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";

	private ServiceRegistration<ServletContext> servletContextService;

	private Map<String, Object> contextAttributes;

	private TomcatServerWrapper(final EmbeddedTomcat server) {
		NullArgumentException.validateNotNull(server, "server");
		this.m_server = server;
		((ContainerBase) m_server.getHost()).setStartChildren(false);
	}

	static ServerWrapper getInstance(final EmbeddedTomcat server) {
		return new TomcatServerWrapper(server);
	}

	@Override
	public void start() {
		LOG.debug("start server");
		try {
			final long t1 = System.nanoTime();
			m_server.getHost();
			m_server.start();
			final long t2 = System.nanoTime();
			if (LOG.isInfoEnabled()) {
				LOG.info("TomCat server startup in " + ((t2 - t1) / 1000000)
						+ " ms");
			}
		} catch (final LifecycleException e) {
			throw new ServerStartException(m_server.getServer().getInfo(), e);
		}
	}

	@Override
	public void stop() {
		LOG.debug("stop server");
		final LifecycleState state = m_server.getServer().getState();
		if (LifecycleState.STOPPING_PREP.compareTo(state) <= 0
				&& LifecycleState.DESTROYED.compareTo(state) >= 0) {
			throw new IllegalStateException("stop already called!");
		} else {
			try {
				m_server.stop();
				m_server.destroy();
			} catch (final Throwable e) {
				// throw new ServerStopException(
				// m_server.getServer().getInfo(), e );
				LOG.error("LifecycleException caught {}", e);
			}
		}
	}

	/*
	 * 
	 * LOG.debug("Adding servlet [" + model + "]"); final ServletMapping mapping
	 * = new ServletMapping(); mapping.setServletName(model.getName());
	 * mapping.setPathSpecs(model.getUrlPatterns()); final ServletContextHandler
	 * context = m_server.getOrCreateContext(model); final ServletHandler
	 * servletHandler = context.getServletHandler(); if (servletHandler == null)
	 * { throw new IllegalStateException(
	 * "Internal error: Cannot find the servlet holder"); }
	 * 
	 * ServletHolder servletHolder = null; if
	 * (isLazyInitializationRequired(model)) { servletHolder = new
	 * ServletHolder(model.getServlet().getClass()); } else { servletHolder =
	 * new ServletHolder(model.getServlet()); } final ServletHolder holder =
	 * servletHolder; holder.setName(model.getName()); if (model.getInitParams()
	 * != null) { holder.setInitParameters(model.getInitParams()); } // Jetty
	 * does not set the context class loader on adding the filters so // we do
	 * that instead try {
	 * ContextClassLoaderUtils.doWithClassLoader(context.getClassLoader(), new
	 * Callable<Void>() {
	 * 
	 * public Void call() { servletHandler.addServlet(holder);
	 * servletHandler.addServletMapping(mapping); return null; }
	 * 
	 * }); } catch (Exception e) { if (e instanceof RuntimeException) { throw
	 * (RuntimeException) e; }
	 * LOG.error("Ignored exception during servlet registration", e); }
	 */

	@Override
	public void addServlet(final ServletModel model) {
		LOG.debug("add servlet [{}]", model);
		final Context context = findOrCreateContext(model.getContextModel());
		final String servletName = model.getName();
		// Wrapper wrapper = Tomcat.addServlet( context, servletName,
		// model.getServlet() );

		Wrapper sw = null;

		if (model.getServlet() == null) {
			// will do class for name and set init params
			sw = context.createWrapper();
		} else {
			sw = new ExistingStandardWrapper(model.getServlet()) {

				@Override
				protected void initInternal() throws LifecycleException {
					super.initInternal();
					try {
						super.loadServlet();
					} catch (final ServletException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			};
		}

		sw.setName(servletName);
		context.addChild(sw);

		addServletMappings(context, servletName, model.getUrlPatterns());
		addInitParameters(sw, model.getInitParams());
	}

	@Override
	public void removeServlet(final ServletModel model) {
		LOG.debug("remove servlet [{}]", model);
		final Context context = findContext(model);
		if (context == null) {
			throw new TomcatRemoveServletException(
					"cannot remove servlet cannot find the associated container: "
							+ model);
		}
		final Container servlet = context.findChild(model.getName());
		if (servlet == null) {
			throw new TomcatRemoveServletException(
					"cannot find the servlet to remove: " + model);
		}
		context.removeChild(servlet);
	}

	@Override
	public void removeContext(final HttpContext httpContext) {
		LOG.debug("remove context [{}]", httpContext);

		try {
			if (servletContextService != null) {
				servletContextService.unregister();
			}
		} catch (final IllegalStateException e) {
			LOG.info("ServletContext service already removed");
		}

		final Context context = m_contexts.remove(httpContext);
		this.m_server.getHost().removeChild(context);
		if (context == null) {
			throw new RemoveContextException(
					"cannot remove the context because it does not exist: "
							+ httpContext);
			// LOG.warn("cannot remove the context because it does not exist: {}"
			// , httpContext );
			// return;
		}
		try {
			final LifecycleState state = context.getState();
			if (LifecycleState.DESTROYED != state
					|| LifecycleState.DESTROYING != state) {
				context.destroy();
			}
		} catch (final LifecycleException e) {
			throw new RemoveContextException("cannot destroy the context: "
					+ httpContext, e);
			// LOG.warn("cannot destroy the context: " + httpContext);
		}
	}

	@Override
	public void addEventListener(final EventListenerModel eventListenerModel) {
		LOG.debug("add event listener: [{}]", eventListenerModel);
		final ServletContext servletContext = findOrCreateServletContext(eventListenerModel);
		servletContext.addListener(eventListenerModel.getEventListener());
	}

	private ServletContext findOrCreateServletContext(final Model model) {
		final Context context = findOrCreateContext(model);
		return context.getServletContext();
	}

	@Override
	public void removeEventListener(final EventListenerModel eventListenerModel) {
		LOG.debug("remove event listener: [{}]", eventListenerModel);
		NullArgumentException.validateNotNull(eventListenerModel,
				"eventListenerModel");
		NullArgumentException.validateNotNull(
				eventListenerModel.getEventListener(),
				"eventListenerModel#weventListener");
		final Context context = findOrCreateContext(eventListenerModel);
		// TODO open a bug in tomcat
		if (!removeApplicationEventListener(context,
				eventListenerModel.getEventListener())) {
			if (!removeApplicationLifecycleListener(context,
					eventListenerModel.getEventListener())) {
				throw new RemoveEventListenerException(
						"cannot remove the event lister it is a not support class : "
								+ eventListenerModel);
			}
		}

	}

	private boolean removeApplicationLifecycleListener(final Context context,
			final EventListener eventListener) {
		if (!isApplicationLifecycleListener(eventListener)) {
			return false;
		}
		final List<Object> applicationLifecycleListeners = Arrays
				.asList(context.getApplicationLifecycleListeners());
		if (applicationLifecycleListeners.remove(eventListener)) {
			context.setApplicationLifecycleListeners(applicationLifecycleListeners
					.toArray());
			return true;
		}
		return false;
	}

	private boolean isApplicationLifecycleListener(
			final EventListener eventListener) {
		return (eventListener instanceof HttpSessionListener || eventListener instanceof ServletContextListener);
	}

	private boolean removeApplicationEventListener(final Context context,
			final EventListener eventListener) {
		if (!isApplicationEventListener(eventListener)) {
			return false;
		}
		final List<Object> applicationEventListener = Arrays.asList(context
				.getApplicationEventListeners());
		if (applicationEventListener.remove(eventListener)) {
			context.setApplicationEventListeners(applicationEventListener
					.toArray());
			return true;
		}
		return false;
	}

	private boolean isApplicationEventListener(final EventListener eventListener) {
		return (eventListener instanceof ServletContextAttributeListener
				|| eventListener instanceof ServletRequestListener
				|| eventListener instanceof ServletRequestAttributeListener || eventListener instanceof HttpSessionAttributeListener);
	}

	@Override
	public void addFilter(final FilterModel filterModel) {
		LOG.debug("add filter [{}]", filterModel);
		final ServletContext servletContext = findOrCreateServletContext(filterModel);
		final FilterRegistration.Dynamic filterRegistration = servletContext
				.addFilter(filterModel.getName(), filterModel.getFilter());
		if (filterModel.getServletNames() != null) {
			filterRegistration.addMappingForServletNames(
					getDispatcherTypes(filterModel), /*
													 * TODO get asynch
													 * supported?
													 */false,
					filterModel.getServletNames());
		} else if (filterModel.getUrlPatterns() != null) {
			filterRegistration.addMappingForServletNames(
					getDispatcherTypes(filterModel), /*
													 * TODO get asynch
													 * supported?
													 */false,
					filterModel.getUrlPatterns());
		} else {
			throw new AddFilterException(
					"cannot add filter to the context; at least a not empty list of servlet names or URL patterns in exclusive mode must be provided: "
							+ filterModel);
		}
		filterRegistration.setInitParameters(filterModel.getInitParams());
		// filterRegistration.setAsyncSupported(filterModel.); TODO FIXME see
		// how to get this info... ? see above
	}

	private EnumSet<DispatcherType> getDispatcherTypes(
			final FilterModel filterModel) {
		final ArrayList<DispatcherType> dispatcherTypes = new ArrayList<DispatcherType>(
				DispatcherType.values().length);
		for (final String dispatcherType : filterModel.getDispatcher()) {
			dispatcherTypes.add(DispatcherType.valueOf(dispatcherType
					.toUpperCase()));
		}
		final EnumSet<DispatcherType> result = EnumSet.copyOf(dispatcherTypes);
		return result;
	}

	@Override
	public void removeFilter(final FilterModel filterModel) {
		throw new UnsupportedOperationException("not yet implemented :(");
	}

	@Override
	public void addErrorPage(final ErrorPageModel model) {
		final Context context = findContext(model);
		if (context == null) {
			throw new AddErrorPageException(
					"cannot retrieve the associated context: " + model);
		}
		final ErrorPage errorPage = createErrorPage(model);
		context.addErrorPage(errorPage);
	}

	private ErrorPage createErrorPage(final ErrorPageModel model) {
		NullArgumentException.validateNotNull(model, "model");
		NullArgumentException.validateNotNull(model.getLocation(),
				"model#location");
		NullArgumentException.validateNotNull(model.getError(), "model#error");
		final ErrorPage errorPage = new ErrorPage();
		errorPage.setLocation(model.getLocation());
		final Integer errorCode = parseErrorCode(model.getError());
		if (errorCode != null) {
			errorPage.setErrorCode(errorCode);
		} else {
			errorPage.setExceptionType(model.getError());
		}
		return errorPage;
	}

	private Integer parseErrorCode(final String errorCode) {
		try {
			return Integer.parseInt(errorCode);
		} catch (final NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void removeErrorPage(final ErrorPageModel model) {
		final Context context = findContext(model);
		if (context == null) {
			throw new RemoveErrorPageException(
					"cannot retrieve the associated context: " + model);
		}
		final ErrorPage errorPage = createErrorPage(model);
		context.removeErrorPage(errorPage);
	}

	@Override
	public Servlet createResourceServlet(final ContextModel contextModel,
			final String alias, final String name) {
		LOG.debug("createResourceServlet( contextModel: {}, alias: {}, name: {})");
		return new TomcatResourceServlet(contextModel.getHttpContext(),
				contextModel.getContextName(), alias, name);
	}

	@Override
	public void addSecurityConstraintMapping(
			final SecurityConstraintMappingModel secMapModel) {// TODO
		throw new UnsupportedOperationException("not yet implemented :(");
	}

	@Override
	public LifeCycle getContext(final ContextModel model) {
		// final Context context = findContext( model );
		final Context context = findOrCreateContext(model);
		if (context == null) {
			throw new RemoveErrorPageException(
					"cannot retrieve the associated context: " + model);
		}
		return new LifeCycle() {
			@Override
			public void start() throws Exception {
				context.start();
			}

			@Override
			public void stop() throws Exception {
				context.stop();
			}
		};
	}

	private void addServletMappings(final Context context,
			final String servletName, final String[] urlPatterns) {
		NullArgumentException.validateNotNull(urlPatterns, "urlPatterns");
		for (final String urlPattern : urlPatterns) {// TODO add a enhancement
														// to tomcat it is in
														// the specification so
														// tomcat should provide
														// it out of the box
			context.addServletMapping(urlPattern, servletName);
		}
	}

	private void addInitParameters(final Wrapper wrapper,
			final Map<String, String> initParameters) {
		NullArgumentException.validateNotNull(initParameters, "initParameters");
		NullArgumentException.validateNotNull(wrapper, "wrapper");
		for (final Map.Entry<String, String> initParam : initParameters
				.entrySet()) {
			wrapper.addInitParameter(initParam.getKey(), initParam.getValue());
		}
	}

	private Context findOrCreateContext(final Model model) {
		NullArgumentException.validateNotNull(model, "model");
		return findOrCreateContext(model.getContextModel());
	}

	private Context findOrCreateContext(final ContextModel contextModel) {
		Context context = findContext(contextModel);
		if (context == null) {
			context = createContext(contextModel);
		}
		return context;
	}

	private Context createContext(final ContextModel contextModel) {
		final Bundle bundle = contextModel.getBundle();
		final BundleContext bundleContext = BundleUtils
				.getBundleContext(bundle);

		final Context context = m_server.addContext(
				contextModel.getContextParams(),
				getContextAttributes(bundleContext),
				contextModel.getContextName(), contextModel.getHttpContext(),
				contextModel.getAccessControllerContext(),
				contextModel.getContainerInitializers(),
				contextModel.getJettyWebXmlURL(),
				contextModel.getVirtualHosts(), contextModel.getConnectors(),
				m_server.getBasedir());

		context.setParentClassLoader(contextModel.getClassLoader());
		// TODO: is the context already configured?
		// TODO: how about security, classloader?
		// TODO: compare with JettyServerWrapper.addContext
		// TODO: what about the init parameters?
		/*
		 * Do not start context here, but register it to be started lazily. This
		 * ensures that all servlets, listeners, initializers etc. are
		 * registered before the context is started.
		 */
		ServletContextManager.addContext(context.getPath(),
				new TomcatServletContextWrapper(context, m_server.getHost()));

		final LifecycleState state = context.getState();
		if (state != LifecycleState.STARTED && state != LifecycleState.STARTING
				&& state != LifecycleState.STARTING_PREP) {

			// if( !context.isStarted() && !context.isStarting() )
			// {
			LOG.debug("Registering ServletContext as service. ");
			final Dictionary<String, String> properties = new Hashtable<String, String>();
			properties.put("osgi.web.symbolicname", bundle.getSymbolicName());

			final Dictionary<String, String> headers = bundle.getHeaders();
			final String version = (String) headers
					.get(Constants.BUNDLE_VERSION);
			if (version != null && version.length() > 0) {
				properties.put("osgi.web.version", version);
			}

			String webContextPath = (String) headers.get(WEB_CONTEXT_PATH);
			final String webappContext = (String) headers.get("Webapp-Context");

			final ServletContext servletContext = context.getServletContext();

			// This is the default context, but shouldn't it be called default?
			// See PAXWEB-209
			if ("/".equalsIgnoreCase(context.getPath())
					&& (webContextPath == null || webappContext == null)) {
				webContextPath = context.getPath();
			}

			// makes sure the servlet context contains a leading slash
			webContextPath = webContextPath != null ? webContextPath
					: webappContext;
			if (webContextPath != null && !webContextPath.startsWith("/")) {
				webContextPath = "/" + webContextPath;
			}

			if (webContextPath == null) {
				LOG.warn("osgi.web.contextpath couldn't be set, it's not configured");
			}

			properties.put("osgi.web.contextpath", webContextPath);

			servletContextService = bundleContext.registerService(
					ServletContext.class, servletContext, properties);
			LOG.debug("ServletContext registered as service. ");

		}
		m_contexts.put(contextModel.getHttpContext(), context);

		return context;
	}

	private Context findContext(final ContextModel contextModel) {
		final String contextName = contextModel.getContextName();
		return m_server.findContext(contextName);
	}

	private Context findContext(final Model model) {
		return findContext(model.getContextModel());
	}

	/**
	 * Returns a list of servlet context attributes out of configured properties
	 * and attribues containing the bundle context associated with the bundle
	 * that created the model (web element).
	 * 
	 * @param bundleContext
	 *            bundle context to be set as attribute
	 * 
	 * @return context attributes map
	 */
	private Map<String, Object> getContextAttributes(
			final BundleContext bundleContext) {
		final Map<String, Object> attributes = new HashMap<String, Object>();
		if (contextAttributes != null) {
			attributes.putAll(contextAttributes);
		}
		attributes.put(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE,
				bundleContext);
		attributes
				.put("org.springframework.osgi.web.org.osgi.framework.BundleContext",
						bundleContext);
		return attributes;
	}
}
