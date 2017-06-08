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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.UnavailableException;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.startup.ContextRuleSet;
import org.apache.catalina.startup.NamingRuleSet;
import org.apache.catalina.startup.Tomcat.ExistingStandardWrapper;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.apache.tomcat.util.descriptor.web.ErrorPage;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.JspConfigDescriptorImpl;
import org.apache.tomcat.util.descriptor.web.JspPropertyGroup;
import org.apache.tomcat.util.descriptor.web.JspPropertyGroupDescriptorImpl;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.descriptor.web.TaglibDescriptorImpl;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.util.ResourceDelegatingBundleClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * @author Romain Gilles
 */
class TomcatServerWrapper implements ServerWrapper {
	private final class WrappedServletLifecycleListener implements LifecycleListener {
		private final Context context;
		private final String servletName;
		private final ServletModel model;

		private WrappedServletLifecycleListener(Context context, String servletName, ServletModel model) {
			this.context = context;
			this.servletName = servletName;
			this.model = model;
		}

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
	}

	private final class SerlvetClassNameLifecycleListener implements LifecycleListener {
		private final Context context;
		private final Wrapper sw;
		private final ServletModel model;
		private final String servletName;

		private SerlvetClassNameLifecycleListener(Context context, Wrapper sw, ServletModel model, String servletName) {
			this.context = context;
			this.sw = sw;
			this.model = model;
			this.servletName = servletName;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (Lifecycle.AFTER_START_EVENT
					.equalsIgnoreCase(event.getType())) {
				Map<String, ? extends ServletRegistration> servletRegistrations = context
						.getServletContext()
						.getServletRegistrations();
				//CHECKSTYLE:OFF
				if (!servletRegistrations
						.containsKey(servletName)) {
					LOG.debug("need to re-register the servlet ...");
					sw.setServletClass(model
							.getServletClass().getName());

					addServletWrapper(sw, servletName,
							context, model);
				}
				//CHECKSTYLE:ON
			}
		}
	}

	private final class ServletLifecycleListener implements LifecycleListener {
		private final Servlet servlet;
		private final Context context;
		private final String servletName;
		private final ServletModel model;

		private ServletLifecycleListener(Servlet servlet, Context context, String servletName, ServletModel model) {
			this.servlet = servlet;
			this.context = context;
			this.servletName = servletName;
			this.model = model;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (Lifecycle.AFTER_START_EVENT
					.equalsIgnoreCase(event.getType())) {
				Map<String, ? extends ServletRegistration> servletRegistrations = context
						.getServletContext()
						.getServletRegistrations();
				//CHECKSTYLE:OFF
				if (!servletRegistrations
						.containsKey(servletName)) {
					LOG.debug("need to re-register the servlet ...");
					createServletWrapper(model, context,
							servletName, servlet);
				}
				//CHECKSTYLE:ON
			}
		}
	}

	private final class FilterLifecycleListener implements LifecycleListener {
		private final FilterModel filterModel;
		private final Context context;

		private FilterLifecycleListener(FilterModel filterModel, Context context) {
			this.filterModel = filterModel;
			this.context = context;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			if (Lifecycle.BEFORE_START_EVENT.equalsIgnoreCase(event
					.getType())) {
				FilterRegistration.Dynamic filterRegistration = null;
				if (filterModel.getFilter() != null) {
					filterRegistration = context
							.getServletContext().addFilter(
									filterModel.getName(),
									filterModel.getFilter());

				} else if (filterModel.getFilterClass() != null) {
					filterRegistration = context
							.getServletContext().addFilter(
									filterModel.getName(),
									filterModel.getFilterClass());
				}

				if (filterRegistration == null) {
					filterRegistration = (Dynamic) context
							.getServletContext().getFilterRegistration(
									filterModel.getName());
					if (filterRegistration == null) {
						LOG.error("Can't register Filter due to unknown reason!");
					}
				}

				filterRegistration.setAsyncSupported(filterModel.isAsyncSupported());

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
			}
		}
	}

	private final class OsgiExistingStandardWrapper extends
			ExistingStandardWrapper {
		private final ServletModel model;

		private OsgiExistingStandardWrapper(Servlet existing, ServletModel model) {
			super(existing);
			this.model = model;
		}

		@Override
		public synchronized void load() throws ServletException {
			try {
				instance =  loadServlet();
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				LOG.error(
						"Ignored exception during servlet registration",
						e);
			}

			if (!instanceInitialized) {
				initServlet(instance);
			}

			// skip the JMX part not needed here!
		}

		private synchronized void initServlet(Servlet servlet)
				throws ServletException {

			if (instanceInitialized && !singleThreadModel) {
				return;
			}

			// Call the initialization method of this servlet
			try {
				instanceSupport.fireInstanceEvent(
						InstanceEvent.BEFORE_INIT_EVENT, servlet);

				if (Globals.IS_SECURITY_ENABLED) {

					Object[] args = new Object[]{(facade)};
					SecurityUtil.doAsPrivilege("init", servlet,
							classType, args);
					args = null;
				} else {
					servlet.init(facade);
				}

				instanceInitialized = true;

				instanceSupport.fireInstanceEvent(
						InstanceEvent.AFTER_INIT_EVENT, servlet);
			} catch (UnavailableException f) {
				instanceSupport.fireInstanceEvent(
						InstanceEvent.AFTER_INIT_EVENT, servlet, f);
				unavailable(f);
				throw f;
			} catch (ServletException f) {
				instanceSupport.fireInstanceEvent(
						InstanceEvent.AFTER_INIT_EVENT, servlet, f);
				// If the servlet wanted to be unavailable it would have
				// said so, so do not call unavailable(null).
				throw f;
			} catch (Throwable f) {
				ExceptionUtils.handleThrowable(f);
				getServletContext().log("StandardWrapper.Throwable", f);
				instanceSupport.fireInstanceEvent(
						InstanceEvent.AFTER_INIT_EVENT, servlet, f);
				// If the servlet wanted to be unavailable it would have
				// said so, so do not call unavailable(null).
				throw new ServletException(sm.getString(
						"standardWrapper.initException", getName()), f);
			}
		}
	}

	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServerWrapper.class);
	private static final String WEB_CONTEXT_PATH = "Web-ContextPath";
	private final EmbeddedTomcat server;
	private final Map<HttpContext, Context> contextMap = new ConcurrentHashMap<>();

	private final Map<FilterModel, FilterLifecycleListener> filterLifecycleListenerMap = new ConcurrentHashMap<>();

	private final Map<ServletModel, LifecycleListener> servletLifecycleListenerMap = new ConcurrentHashMap<>();

	private ServiceRegistration<ServletContext> servletContextService;

	private Map<String, Object> contextAttributes;

	private TomcatServerWrapper(final EmbeddedTomcat server) {
		NullArgumentException.validateNotNull(server, "server");
		this.server = server;
		((ContainerBase) server.getHost()).setStartChildren(false);
		TomcatURLStreamHandlerFactory.disable();
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
			throw new ServerStartException(server.getServer().toString(), e);
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
			//CHECKSTYLE:OFF
			try {
				server.stop();
				server.destroy();
			} catch (final Throwable e) {
				LOG.error("LifecycleException caught {}", e);
			}
			//CHECKSTYLE:ON
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
						ServletLifecycleListener listener = new ServletLifecycleListener(servlet, context, servletName, model);
						servletLifecycleListenerMap.put(model, listener);
						context.addLifecycleListener(listener);
					}

				} else {
					final Wrapper sw = context.createWrapper();
					sw.setServletClass(model.getServletClass().getName());

					addServletWrapper(sw, servletName, context, model);

					if (!model.getContextModel().isWebBundle()) {
						SerlvetClassNameLifecycleListener listener = new SerlvetClassNameLifecycleListener(context, sw, model, servletName);
						servletLifecycleListenerMap.put(model, listener);
						context.addLifecycleListener(listener);
					}
				}

			} catch (InstantiationException | SecurityException | ClassNotFoundException | IllegalAccessException e) {
				LOG.error("failed to create Servlet", e);
			}

		} else {
			createServletWrapper(model, context, servletName, null);

			if (!model.getContextModel().isWebBundle()) {
				WrappedServletLifecycleListener listener = new WrappedServletLifecycleListener(context, servletName, model);
				servletLifecycleListenerMap.put(model, listener);
				context.addLifecycleListener(listener);
			}
		}
	}

	private void createServletWrapper(final ServletModel model,
									  final Context context, final String servletName, Servlet servlet) {

		if (servlet != null) {
			Wrapper sw = new OsgiExistingStandardWrapper(model.getServlet(),
					model);
			addServletWrapper(sw, servletName, context, model);
		} else {
			Wrapper sw = new OsgiExistingStandardWrapper(model.getServlet(),
					model);
			addServletWrapper(sw, servletName, context, model);
		}

	}

	private void addServletWrapper(final Wrapper sw, final String servletName,
								   final Context context, final ServletModel model) {

		sw.setName(servletName);
		context.addChild(sw);

		addServletMappings(context, servletName, model.getUrlPatterns());
		addInitParameters(sw, model.getInitParams());

		if (model.getAsyncSupported() != null) {
			sw.setAsyncSupported(model.getAsyncSupported());
		}
		if (model.getLoadOnStartup() != null) {
			sw.setLoadOnStartup(model.getLoadOnStartup());
		}
		if (model.getMultipartConfig() != null) {
			sw.setMultipartConfigElement(model.getMultipartConfig());
		}

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

		LOG.info("remove Servlet");
		LifecycleListener listener = servletLifecycleListenerMap.remove(model);
		context.removeLifecycleListener(listener);

		final Container servlet = context.findChild(model.getName());
		if (servlet == null) {
//			throw new TomcatRemoveServletException(
//					"cannot find the servlet to remove: " + model);
			LOG.warn("cannot find the servlet to remove: {}", model);
		} else {
			String[] urlPatterns = model.getUrlPatterns();
			Arrays.stream(urlPatterns).forEach(pattern -> context.removeServletMapping(pattern));
			context.removeChild(servlet);
		}

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
		if (context == null) {
			throw new RemoveContextException(
					"cannot remove the context because it does not exist: "
							+ httpContext);
		}
		try {
			context.stop();
		} catch (LifecycleException e) {
			throw new RemoveContextException("cannot stop the context: "
					+ httpContext, e);
		}
		this.server.getHost().removeChild(context);
		try {
			final LifecycleState state = context.getState();
			if (LifecycleState.DESTROYED != state
					|| LifecycleState.DESTROYING != state) {
				context.destroy();
			}
		} catch (final LifecycleException e) {
			throw new RemoveContextException("cannot destroy the context: "
					+ httpContext, e);
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

	@Override
	public void removeEventListener(final EventListenerModel eventListenerModel) {
		LOG.debug("remove event listener: [{}]", eventListenerModel);
		NullArgumentException.validateNotNull(eventListenerModel,
				"eventListenerModel");
		NullArgumentException.validateNotNull(
				eventListenerModel.getEventListener(),
				"eventListenerModel#weventListener");
		final Context context = findOrCreateContext(eventListenerModel);

		LOG.info("removing event listener");
		// TODO open a bug in tomcat
		if (!removeApplicationEventListener(context,
				eventListenerModel.getEventListener())) {
			if (!removeApplicationLifecycleListener(context,
					eventListenerModel.getEventListener())) {
//				throw new RemoveEventListenerException(
//						"cannot remove the event lister it is a not support class : "
//								+ eventListenerModel);
				LOG.warn("cannot remove the event lister it is a not support class : {}", eventListenerModel);
			}
		}
	}

	private boolean removeApplicationLifecycleListener(final Context context,
													   final EventListener eventListener) {
		if (!isApplicationLifecycleListener(eventListener)) {
			return false;
		}

		Object[] applicationLifecycleListeners = context.getApplicationLifecycleListeners();

		List<EventListener> listeners = new ArrayList<>();
		boolean found = filterEventListener(listeners, applicationLifecycleListeners, eventListener);

		if (found) {
			context.setApplicationLifecycleListeners(listeners.toArray());
		}
		return found;
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
		Object[] applicationEventListeners = context
				.getApplicationEventListeners();

		List<EventListener> newEventListeners = new ArrayList<>();
		boolean found = filterEventListener(newEventListeners, applicationEventListeners, eventListener);


		if (found) {
			context.setApplicationEventListeners(newEventListeners
					.toArray());
		}
		return found;
	}

	private boolean filterEventListener(List<EventListener> listeners, Object[] applicationEventListeners, EventListener eventListener) {

		boolean found = false;

		for (Object object : applicationEventListeners) {
			EventListener listener = (EventListener) object;
			if (listener != eventListener) {
				listeners.add(listener);
			} else {
				found = true;
			}
		}

		return found;

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


		FilterLifecycleListener listener = new FilterLifecycleListener(filterModel, context);
		filterLifecycleListenerMap.put(filterModel, listener);

		context.addLifecycleListener(listener);

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
		final ArrayList<DispatcherType> dispatcherTypes = new ArrayList<>(
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
		final Context context = findContext(filterModel);

		LOG.info("removing ServletFilter: {}", filterModel);
		((StandardContext) context).filterStop();

		FilterLifecycleListener filterLifecycleListener = filterLifecycleListenerMap.remove(filterModel);
		context.removeLifecycleListener(filterLifecycleListener);

		FilterDef findFilterDef = context.findFilterDef(filterModel.getName());
		LOG.info("removing ServletFilter with name: {}", filterModel.getName());
		context.removeFilterDef(findFilterDef);
		LOG.info("filterDefs now contain: {}", context.findFilterDefs());
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
		final Context context = findOrCreateContext(model);
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
			if (!ErrorPageModel.ERROR_PAGE.equalsIgnoreCase(model.getError())) {
				errorPage.setExceptionType(model.getError());
			}
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

		LOG.info("remove error page");
		final ErrorPage errorPage = createErrorPage(model);
		context.removeErrorPage(errorPage);

	}

	@Override
	public Servlet createResourceServlet(final ContextModel contextModel,
										 final String alias, final String name) {
		LOG.debug("createResourceServlet( contextModel: {}, alias: {}, name: {})");
		final Context context = findOrCreateContext(contextModel);
		return new TomcatResourceServlet(contextModel.getHttpContext(),
				contextModel.getContextName(), alias, name, context);
	}

	@Override
	public void addSecurityConstraintMapping(
			final SecurityConstraintMappingModel secMapModel) {
		LOG.debug("add security contstraint mapping [{}]", secMapModel);
		final Context context = findOrCreateContext(secMapModel
				.getContextModel());

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
			if (securityConstraint.getDisplayName().equalsIgnoreCase(
					constraintName)) {
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
		final Context context = findOrCreateContext(model);
		if (context == null) {
			throw new RemoveErrorPageException(
					"cannot retrieve the associated context: " + model);
		}
		return new LifeCycle() {
			@Override
			public void start() throws Exception {
				ContainerBase host = (ContainerBase) TomcatServerWrapper.this.server
						.getHost();
				host.setStartChildren(true);

				if (!context.getState().isAvailable()) {
					LOG.info("server is available, in state {}",
							context.getState());
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
		HttpContext httpContext = contextModel.getHttpContext();
		Context context = contextMap.get(httpContext);

		if (context == null) {
			context = server.findContext(contextModel);
		}
		if (context == null) {
			context = createContext(contextModel);
		}
		return context;
	}

	private Context createContext(final ContextModel contextModel) {
		final Bundle bundle = contextModel.getBundle();
		final BundleContext bundleContext = BundleUtils
				.getBundleContext(bundle);
		final WebContainerContext httpContext = contextModel.getHttpContext();

		final Context context = server.addContext(
				contextModel.getContextParams(),
				getContextAttributes(bundleContext),
				contextModel.getContextName(), contextModel.getHttpContext(),
				contextModel.getAccessControllerContext(),
				contextModel.getContainerInitializers(),
				contextModel.getJettyWebXmlURL(),
				contextModel.getVirtualHosts(), null /*contextModel.getConnectors() */,
				server.getBasedir());

		context.setDisplayName(httpContext.getContextId());
		// Similar to the Jetty fix for PAXWEB-725
		// Without this the el implementation is not found
        ClassLoader classLoader = contextModel.getClassLoader();
        List<Bundle> bundles = ((ResourceDelegatingBundleClassLoader) classLoader).getBundles();
        ClassLoader parentClassLoader = getClass().getClassLoader();
        ResourceDelegatingBundleClassLoader containerSpecificClassLoader = new ResourceDelegatingBundleClassLoader(bundles, parentClassLoader);
        context.setParentClassLoader(containerSpecificClassLoader);

		// support default context.xml in configurationDir or config fragment
		URL defaultContextUrl = getDefaultContextXml();
		// support MTA-INF/context.xml in war
		URL configFile = bundle.getEntry(org.apache.catalina.startup.Constants.ApplicationContextXml);
		if (defaultContextUrl != null || configFile != null) {
			Digester digester = createContextDigester();
			if (defaultContextUrl != null) {
				processContextConfig(context, digester, defaultContextUrl);
			}
			if (configFile != null) {
				context.setConfigFile(configFile);
				processContextConfig(context, digester, configFile);
			}
		}

        // TODO: how about security, classloader?
		// TODO: compare with JettyServerWrapper.addContext
		// TODO: what about the init parameters?

		configureJspConfigDescriptor(context, contextModel);

		final LifecycleState state = context.getState();
		if (state != LifecycleState.STARTED && state != LifecycleState.STARTING
				&& state != LifecycleState.STARTING_PREP) {

			LOG.debug("Registering ServletContext as service. ");
			final Dictionary<String, String> properties = new Hashtable<>();
			properties.put(WebContainerConstants.PROPERTY_SYMBOLIC_NAME, bundle.getSymbolicName());

			final Dictionary<String, String> headers = bundle.getHeaders();
			final String version = headers.get(Constants.BUNDLE_VERSION);
			if (version != null && version.length() > 0) {
				properties.put("osgi.web.version", version);
			}

			String webContextPath = headers.get(WEB_CONTEXT_PATH);
			final String webappContext = headers.get("Webapp-Context");

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
				LOG.warn(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH +
						" couldn't be set, it's not configured. Assuming '/'");
				webContextPath = "/";
			}

			properties.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);
			properties.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME, context.getServletContext().getServletContextName());

			servletContextService = bundleContext.registerService(
					ServletContext.class, servletContext, properties);
			LOG.debug("ServletContext registered as service. ");
		}
		contextMap.put(contextModel.getHttpContext(), context);

		return context;
	}

	private URL getDefaultContextXml() {
		// get the resource URL from the config fragment
		URL defaultContextUrl = getClass().getResource("/context.xml");
		// overwrite with context xml from configuration directory if it exists
		File configurationFile = new File(server.getConfigurationDir(), "context.xml");
		if (configurationFile.exists()) {
			try {
				defaultContextUrl = configurationFile.toURI().toURL();
			} catch (MalformedURLException e) {
				LOG.error("cannot access default context file", e);
			}
		}
		return defaultContextUrl;
	}

	private Digester createContextDigester() {
		Digester digester = new Digester();
		digester.setValidating(false);
		digester.setRulesValidation(true);
		HashMap<Class<?>, List<String>> fakeAttributes = new HashMap<>();
		ArrayList<String> attrs = new ArrayList<>();
		attrs.add("className");
		fakeAttributes.put(Object.class, attrs);
		digester.setFakeAttributes(fakeAttributes);
		RuleSet contextRuleSet = new ContextRuleSet("", false);
		digester.addRuleSet(contextRuleSet);
		RuleSet namingRuleSet = new NamingRuleSet("Context/");
		digester.addRuleSet(namingRuleSet);
		return digester;
	}

    private void processContextConfig(Context context, Digester digester, URL contextXml) {

        if (LOG.isDebugEnabled()) {
        	LOG.debug("Processing context [" + context.getName()
                    + "] configuration file [" + contextXml + "]");
        }

        InputSource source = null;
        InputStream stream = null;

        try {
            source = new InputSource(contextXml.toString());
            URLConnection xmlConn = contextXml.openConnection();
            xmlConn.setUseCaches(false);
            stream = xmlConn.getInputStream();
        } catch (Exception e) {
            LOG.error("Cannot read context file" , e);
        }

        if (source == null) {
            return;
        }

        try {
            source.setByteStream(stream);
            digester.setClassLoader(this.getClass().getClassLoader());
            digester.setUseContextClassLoader(false);
            digester.push(context.getParent());
            digester.push(context);
            XmlErrorHandler errorHandler = new XmlErrorHandler();
            digester.setErrorHandler(errorHandler);
            digester.parse(source);
            if (errorHandler.getWarnings().size() > 0 ||
                    errorHandler.getErrors().size() > 0) {
                for (SAXParseException e : errorHandler.getWarnings()) {
                    LOG.warn("Warning in XML processing", e.getMessage(), source);
                }
                for (SAXParseException e : errorHandler.getErrors()) {
                    LOG.warn("Error in XML processing", e.getMessage(), source);
                }
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Successfully processed context [" + context.getName()
                        + "] configuration file [" + contextXml + "]");
            }
        } catch (SAXParseException e) {
            LOG.error("Cannot parse config file {}", context.getName(), e);
            LOG.error("at {} {}",
                             "" + e.getLineNumber(),
                             "" + e.getColumnNumber());
        } catch (Exception e) {
        	LOG.error("Cannot parse context {}",
                    context.getName(), e);
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                LOG.error("Cannot close context configuration", e);
            }
        }
    }

	private void configureJspConfigDescriptor(Context context, ContextModel model) {

		Boolean elIgnored = model.getJspElIgnored();
		Boolean isXml = model.getJspIsXml();
		Boolean scriptingInvalid = model.getJspScriptingInvalid();


		Collection<JspPropertyGroupDescriptor> jspPropertyGroupDescriptors = null;
		Collection<TaglibDescriptor> taglibs = null;

		if (elIgnored != null || isXml != null || scriptingInvalid != null
				|| model.getJspIncludeCodes() != null
				|| model.getJspUrlPatterns() != null
				|| model.getJspIncludePreludes() != null) {
			JspPropertyGroup jspPropertyGroup = new JspPropertyGroup();
			JspPropertyGroupDescriptorImpl jspPropertyGroupDescriptor = new JspPropertyGroupDescriptorImpl(jspPropertyGroup);
			if (jspPropertyGroupDescriptors == null) {
				jspPropertyGroupDescriptors = new ArrayList<>();
			}
			jspPropertyGroupDescriptors.add(jspPropertyGroupDescriptor);

			if (model.getJspIncludeCodes() != null) {
				for (String includeCoda : model.getJspIncludeCodes()) {
					jspPropertyGroup.addIncludeCoda(includeCoda);
				}
			}

			if (model.getJspUrlPatterns() != null) {
				for (String urlPattern : model.getJspUrlPatterns()) {
					jspPropertyGroup.addUrlPattern(urlPattern);
				}
			}

			if (model.getJspIncludePreludes() != null) {
				for (String prelude : model.getJspIncludePreludes()) {
					jspPropertyGroup.addIncludePrelude(prelude);
				}
			}

			if (elIgnored != null) {
				jspPropertyGroup.setElIgnored(elIgnored.toString());
			}
			if (isXml != null) {
				jspPropertyGroup.setIsXml(isXml.toString());
			}
			if (scriptingInvalid != null) {
				jspPropertyGroup.setScriptingInvalid(scriptingInvalid.toString());
			}

		}


		if (model.getTagLibLocation() != null || model.getTagLibUri() != null) {
			TaglibDescriptorImpl tagLibDescriptor = new TaglibDescriptorImpl(model.getTagLibLocation(), model.getTagLibUri());
			if (taglibs == null) {
				taglibs = new ArrayList<>();
			}
			taglibs.add(tagLibDescriptor);
		}

		if (jspPropertyGroupDescriptors != null || taglibs != null) {
			JspConfigDescriptor jspConfig = new JspConfigDescriptorImpl(jspPropertyGroupDescriptors, taglibs);
			((Context) context.getServletContext()).setJspConfigDescriptor(jspConfig);
		}
	}

	private Context findContext(final ContextModel contextModel) {
		return server.findContext(contextModel);
	}

	private Context findContext(final Model model) {
		return findContext(model.getContextModel());
	}

	/**
	 * Returns a list of servlet context attributes out of configured properties
	 * and attribues containing the bundle context associated with the bundle
	 * that created the model (web element).
	 *
	 * @param bundleContext bundle context to be set as attribute
	 * @return context attributes map
	 */
	private Map<String, Object> getContextAttributes(
			final BundleContext bundleContext) {
		final Map<String, Object> attributes = new HashMap<>();
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

	@Override
	public void addWelcomeFiles(WelcomeFileModel model) {
		final Context context = findOrCreateContext(model.getContextModel());

		for (String welcomeFile : model.getWelcomeFiles()) {
			context.addWelcomeFile(welcomeFile);
		}
	}

	@Override
	public void removeWelcomeFiles(WelcomeFileModel model) {
		final Context context = findOrCreateContext(model.getContextModel());

		LOG.info("removing welcome files");
		for (String welcomeFile : model.getWelcomeFiles()) {
			context.removeWelcomeFile(welcomeFile);
		}
	}
}
