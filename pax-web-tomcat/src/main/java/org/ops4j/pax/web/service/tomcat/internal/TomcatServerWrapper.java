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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.startup.Tomcat.ExistingStandardWrapper;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
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
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	private final EmbeddedTomcat server;
	private final Map<HttpContext, Context> contextMap = new ConcurrentHashMap<HttpContext, Context>();

	private ServiceRegistration<ServletContext> servletContextService;

	private Map<String, Object> contextAttributes;

	private TomcatServerWrapper(final EmbeddedTomcat server) {
		NullArgumentException.validateNotNull(server, "server");
		this.server = server;
		((ContainerBase) server.getHost()).setStartChildren(false);
	}

	static ServerWrapper getInstance(final EmbeddedTomcat server) {
		return new TomcatServerWrapper(server);
	}

	@Override
	public void start() {
		LOG.debug("start server");
		try {
			final long t1 = System.nanoTime();
			server.getHost();
			server.start();
			final long t2 = System.nanoTime();
			if (LOG.isInfoEnabled()) {
				LOG.info("TomCat server startup in " + ((t2 - t1) / 1000000)
						+ " ms");
			}
		} catch (final LifecycleException e) {
			throw new ServerStartException(server.getServer().getInfo(), e);
		}
	}

	@Override
	public void stop() {
		LOG.debug("stop server");
		final LifecycleState state = server.getServer().getState();
		if (LifecycleState.STOPPING_PREP.compareTo(state) <= 0
				&& LifecycleState.DESTROYED.compareTo(state) >= 0) {
			throw new IllegalStateException("stop already called!");
		} else {
			try {
				server.stop();
				server.destroy();
			} catch (final Throwable e) { //CHECKSTYLE:SKIP
				// throw new ServerStopException(
				// m_server.getServer().getInfo(), e );
				LOG.error("LifecycleException caught {}", e);
			}
		}
	}

	@Override
	public void addServlet(final ServletModel model) {
		LOG.debug("add servlet [{}]", model);
		final Context context = findOrCreateContext(model.getContextModel());
		final String servletName = model.getName();
		if (model.getServlet() == null) {
			// will do class for name and set init params
			try {
				final Servlet servlet = model.getServletFromName();

				if (servlet != null) {
					createServletWrapper(model, context, servletName, servlet);

					if (!model.getContextModel().isWebBundle()) {
						context.addLifecycleListener(new LifecycleListener() {

							@Override
							public void lifecycleEvent(LifecycleEvent event) {
								if (Lifecycle.BEFORE_START_EVENT
										.equalsIgnoreCase(event.getType())) {
									Map<String, ? extends ServletRegistration> servletRegistrations = context
											.getServletContext()
											.getServletRegistrations();
									if (!servletRegistrations.containsKey(servletName)) { //CHECKSTYLE:SKIP
										LOG.debug("need to re-register the servlet ...");
										createServletWrapper(model, context,
												servletName, servlet);
									}
								}
							}
						});
					}

				} else {
					final Wrapper sw = context.createWrapper();
					sw.setServletClass(model.getServletClass().getName());

					addServletWrapper(sw, servletName, context, model);

					if (!model.getContextModel().isWebBundle()) {
						context.addLifecycleListener(new LifecycleListener() {

							@Override
							public void lifecycleEvent(LifecycleEvent event) {
								if (Lifecycle.BEFORE_START_EVENT
										.equalsIgnoreCase(event.getType())) {
									Map<String, ? extends ServletRegistration> servletRegistrations = context
											.getServletContext()
											.getServletRegistrations();
									if (!servletRegistrations.containsKey(servletName)) { //CHECKSTYLE:SKIP
										LOG.debug("need to re-register the servlet ...");
										sw.setServletClass(model
												.getServletClass().getName());

										addServletWrapper(sw, servletName,
												context, model);
									}
								}
							}
						});
					}
				}

			} catch (InstantiationException e) {
				LOG.error("failed to create Servlet", e);
			} catch (IllegalAccessException e) {
				LOG.error("failed to create Servlet", e);
			} catch (ClassNotFoundException e) {
				LOG.error("failed to create Servlet", e);
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			createServletWrapper(model, context, servletName, null);

			if (!model.getContextModel().isWebBundle()) {
				context.addLifecycleListener(new LifecycleListener() {

					@Override
					public void lifecycleEvent(LifecycleEvent event) {
						if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
								.getType())) {
							Map<String, ? extends ServletRegistration> servletRegistrations = context
									.getServletContext()
									.getServletRegistrations();
							if (!servletRegistrations.containsKey(servletName)) {
								LOG.debug("need to re-register the servlet ...");
								createServletWrapper(model, context,
										servletName, null);
							}
						}
					}
				});
			}
		}
	}

	private void createServletWrapper(final ServletModel model,
			final Context context, final String servletName, Servlet servlet) {

		if (servlet != null) {
			final Wrapper sw = new ExistingStandardWrapper(servlet) {
				@Override
				protected void initInternal() throws LifecycleException {
					if (getServlet() == null) {
						LOG.warn("Wrapped Servlet is null!");
						return;
					}

					super.initInternal();

					try {
						ContextClassLoaderUtils.doWithClassLoader(model
								.getContextModel().getClassLoader(),
								new Callable<Void>() {

									@Override
									public Void call() {
										try {
											loadServlet();
										} catch (final ServletException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										return null;
									}

								});
					} catch (Exception e) { //CHECKSTYLE:SKIP
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						}
						LOG.error(
								"Ignored exception during servlet registration",
								e);
					}

				}
			};
			addServletWrapper(sw, servletName, context, model);
		} else {
			final Wrapper sw = new ExistingStandardWrapper(model.getServlet()) {

				@Override
				protected void initInternal() throws LifecycleException {
					if (getServlet() == null) {
						LOG.warn("Wrapped Servlet is null!");
						return;
					}

					super.initInternal();
					try {
						ContextClassLoaderUtils.doWithClassLoader(model
								.getContextModel().getClassLoader(),
								new Callable<Void>() {

									@Override
									public Void call() {
										try {
											loadServlet();
										} catch (final ServletException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										return null;
									}

								});
					} catch (Exception e) { //CHECKSTYLE:SKIP
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						}
						LOG.error(
								"Ignored exception during servlet registration",
								e);
					}
				}
			};
			addServletWrapper(sw, servletName, context, model);
		}

	}

	private void addServletWrapper(final Wrapper sw, final String servletName,
			final Context context, final ServletModel model) {

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

		final Context context = contextMap.remove(httpContext);
		this.server.getHost().removeChild(context);
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

		final Context context = findOrCreateContext(eventListenerModel);
		LifecycleState state = ((HttpServiceContext) context).getState();
		boolean restartContext = false;
		if ((LifecycleState.STARTING.equals(state) || LifecycleState.STARTED
				.equals(state))
				&& !eventListenerModel.getContextModel().isWebBundle()) {
			try {
				restartContext = true;
				((HttpServiceContext) context).stop();
			} catch (LifecycleException e) {
				LOG.warn("Can't reset the Lifecycle ... ", e);
			}
		}
		context.addLifecycleListener(new LifecycleListener() {

			@Override
			public void lifecycleEvent(LifecycleEvent event) {
				if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
						.getType())) {
					context.getServletContext().addListener(
							eventListenerModel.getEventListener());
				}
			}
		});

		if (restartContext) {
			try {
				((HttpServiceContext) context).start();
			} catch (LifecycleException e) {
				LOG.warn("Can't reset the Lifecycle ... ", e);
			}
		}
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

		final Context context = findOrCreateContext(filterModel);
		LifecycleState state = ((HttpServiceContext) context).getState();
		boolean restartContext = false;
		if ((LifecycleState.STARTING.equals(state) || LifecycleState.STARTED
				.equals(state)) && !filterModel.getContextModel().isWebBundle()) {
			try {
				restartContext = true;
				((HttpServiceContext) context).stop();
			} catch (LifecycleException e) {
				LOG.warn("Can't reset the Lifecycle ... ", e);
			}
		}
		context.addLifecycleListener(new LifecycleListener() {

			@Override
			public void lifecycleEvent(LifecycleEvent event) {
				if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
						.getType())) {
					FilterRegistration.Dynamic filterRegistration = context
							.getServletContext().addFilter(
									filterModel.getName(),
									filterModel.getFilter());
					if (filterRegistration == null) {
						filterRegistration = (Dynamic) context
								.getServletContext().getFilterRegistration(
										filterModel.getName());
						if (filterRegistration == null) {
							LOG.error("Can't register Filter due to unknown reason!");
						}
					}

					if (filterModel.getServletNames() != null
							&& filterModel.getServletNames().length > 0) {
						filterRegistration.addMappingForServletNames(
								getDispatcherTypes(filterModel), /*
																 * TODO get
																 * asynch
																 * supported?
																 */false,
								filterModel.getServletNames());
					} else if (filterModel.getUrlPatterns() != null
							&& filterModel.getUrlPatterns().length > 0) {
						filterRegistration.addMappingForUrlPatterns(
								getDispatcherTypes(filterModel), /*
																 * TODO get
																 * asynch
																 * supported?
																 */false,
								filterModel.getUrlPatterns());
					} else {
						throw new AddFilterException(
								"cannot add filter to the context; at least a not empty list of servlet names or URL patterns in exclusive mode must be provided: "
										+ filterModel);
					}
					filterRegistration.setInitParameters(filterModel
							.getInitParams());
					// filterRegistration.setAsyncSupported(filterModel.); TODO
					// FIXME see
					// how to get this info... ? see above
				}
			}
		});

		if (restartContext) {
			try {
				((HttpServiceContext) context).start();
			} catch (LifecycleException e) {
				LOG.warn("Can't reset the Lifecycle ... ", e);
			}
		}

	}

	private EnumSet<DispatcherType> getDispatcherTypes(
			final FilterModel filterModel) {
		final ArrayList<DispatcherType> dispatcherTypes = new ArrayList<DispatcherType>(
				DispatcherType.values().length);
		for (final String dispatcherType : filterModel.getDispatcher()) {
			dispatcherTypes.add(DispatcherType.valueOf(dispatcherType
					.toUpperCase()));
		}
		EnumSet<DispatcherType> result = EnumSet.noneOf(DispatcherType.class);
		if (dispatcherTypes != null && dispatcherTypes.size() > 0) {
			result = EnumSet.copyOf(dispatcherTypes);
		}
		return result;
	}

	@Override
	public void removeFilter(final FilterModel filterModel) {
		final Context context = findOrCreateContext(filterModel);
		FilterDef findFilterDef = context.findFilterDef(filterModel.getName());
		context.removeFilterDef(findFilterDef);
		FilterMap[] filterMaps = context.findFilterMaps();
		for (FilterMap filterMap : filterMaps) {
			if (filterMap.getFilterName().equalsIgnoreCase(
					filterModel.getName())) {
				context.removeFilterMap(filterMap);
			}
		}
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

	/**
	 * 
	 * <pre>
	 * {@code
	 *<security-constraint>
	 * 	<display-name>Restricted GET To Employees</display-name>
	 *  <web-resource-collection>
	 *    <web-resource-name>Restricted Access - Get Only</web-resource-name>
	 *    <url-pattern>/restricted/employee/*</url-pattern>
	 *    <http-method>GET</http-method>
	 *  </web-resource-collection>
	 *  <auth-constraint> 
	 * 	  <role-name>Employee</role-name>
	 *  </auth-constraint>
	 *  <user-data-constraint>
	 *    <transport-guarantee>NONE</transport-guarantee>
	 *  </user-data-constraint>
	 *</security-constraint>
	 * }
	 * </pre>
	 */
	@Override
	public void addSecurityConstraintMapping(
			final SecurityConstraintMappingModel secMapModel) {
		LOG.debug("add security contstraint mapping [{}]", secMapModel);
		final Context context = findOrCreateContext(secMapModel.getContextModel());
		
		String mappingMethod = secMapModel.getMapping();
		String constraintName = secMapModel.getConstraintName();
		String url = secMapModel.getUrl();
		String dataConstraint = secMapModel.getDataConstraint();
		List<String> roles = secMapModel.getRoles();
		boolean authentication = secMapModel.isAuthentication();
		
		
		SecurityConstraint[] constraints = context.findConstraints();
		SecurityConstraint secConstraint = new SecurityConstraint();
		boolean foundExisting = false;
		
		for (SecurityConstraint securityConstraint : constraints) {
			if (securityConstraint.getDisplayName().equalsIgnoreCase(constraintName)) {
				secConstraint = securityConstraint;
				foundExisting = true;
				continue;
			}
		}
		
		if (!foundExisting) {
			secConstraint.setDisplayName(secMapModel.getConstraintName());
			secConstraint.setAuthConstraint(authentication);
			for (String authRole : roles) {
				secConstraint.addAuthRole(authRole);
			}
			secConstraint.setUserConstraint(dataConstraint);
			context.addConstraint(secConstraint);
		}
		
		SecurityCollection collection = new SecurityCollection();
		collection.addMethod(mappingMethod);
		collection.addPattern(url);
		
		secConstraint.addCollection(collection);
		
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
				// ((ContainerBase)getHost()).setStartChildren(false);
				ContainerBase host = (ContainerBase) TomcatServerWrapper.this.server
						.getHost();
				host.setStartChildren(true);
				// getServer.getHost().
				if (!context.getAvailable()) {
					context.start();
				}
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
		for (final String urlPattern : urlPatterns) { // TODO add a enhancement
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
			// } else {
			// LifecycleState state = ((HttpServiceContext)context).getState();
			// if (LifecycleState.STARTING.equals(state) ||
			// LifecycleState.STARTED.equals(state)) {
			// try {
			// ((HttpServiceContext)context).stop();
			// ((HttpServiceContext)context).setState(LifecycleState.NEW);
			// ((HttpServiceContext)context).reload();
			// } catch (LifecycleException e) {
			// LOG.warn("Can't reset the Lifecycle ... ", e);
			// }
			// }
		}
		return context;
	}

	private Context createContext(final ContextModel contextModel) {
		final Bundle bundle = contextModel.getBundle();
		final BundleContext bundleContext = BundleUtils
				.getBundleContext(bundle);

		final Context context = server.addContext(
				contextModel.getContextParams(),
				getContextAttributes(bundleContext),
				contextModel.getContextName(), contextModel.getHttpContext(),
				contextModel.getAccessControllerContext(),
				contextModel.getContainerInitializers(),
				contextModel.getJettyWebXmlURL(),
				contextModel.getVirtualHosts(), contextModel.getConnectors(),
				server.getBasedir());

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
				new TomcatServletContextWrapper(context, server.getHost()));

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
		contextMap.put(contextModel.getHttpContext(), context);
		// ((LifecycleBase)context).setState(LifecycleState.STARTING_PREP);

		// m_server.getHost().fireContainerEvent(Container.ADD_CHILD_EVENT,
		// context);

		return context;
	}

	private Context findContext(final ContextModel contextModel) {
		final String contextName = contextModel.getContextName();
		return server.findContext(contextName);
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
