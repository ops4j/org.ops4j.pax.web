/* Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010 Achim Nierbeck.
 *
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
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.jsp.JspServletWrapper;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.WebContainerDTO;
import org.ops4j.pax.web.service.internal.util.SupportUtils;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ResourceModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WebSocketModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.util.ResourceDelegatingBundleClassLoader;
import org.ops4j.pax.web.service.whiteboard.WhiteboardElement;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.runtime.dto.RequestInfoDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceStarted implements StoppableHttpService {

	private static final String PAX_WEB_JSP_SERVLET = "jsp";

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceStarted.class);
	private static SharedWebContainerContext sharedWebContainerContext;

	final Bundle serviceBundle;
	private final ClassLoader bundleClassLoader;
	private final ServerController serverController;

	private final ServerModel serverModel;
	private final ServiceModel serviceModel;
	private final ServerListener serverListener;
	private final ServletListener eventDispatcher;

	private final Object lock = new Object();

	static {
		sharedWebContainerContext = new DefaultSharedWebContainerContext();
	}

	HttpServiceStarted(final Bundle bundle,
					   final ServerController srvController,
					   final ServerModel serverModel, final ServletListener eventDispatcher) {
		LOG.debug("Creating http service for: " + bundle);

		NullArgumentException.validateNotNull(bundle, "Bundle");
		NullArgumentException.validateNotNull(srvController,
				"Server controller");
		NullArgumentException.validateNotNull(serverModel, "Service model");

		this.serviceBundle = bundle;
		Set<Bundle> wiredBundles = ClassPathUtil.getBundlesInClassSpace(bundle,
				new LinkedHashSet<>());
		ArrayList<Bundle> bundles = new ArrayList<>();
		bundles.add(bundle);
		bundles.addAll(wiredBundles);
		this.bundleClassLoader = new ResourceDelegatingBundleClassLoader(
				bundles);
		this.serverController = srvController;
		this.serverModel = serverModel;
		this.eventDispatcher = eventDispatcher;
		this.serviceModel = new ServiceModel();
		this.serverListener = new ServerListener() {
			@Override
			public void stateChanged(final ServerEvent event) {
				LOG.debug("{}: Handling event: [{}]", this, event);

				if (event == ServerEvent.STARTED) {
					for (ServletModel model : serviceModel.getServletModels()) {
						servletEvent(ServletEvent.DEPLOYING, serviceBundle,
								model);
						serverController.addServlet(model);
						servletEvent(ServletEvent.DEPLOYED, serviceBundle,
								model);
					}
					for (EventListenerModel model : serviceModel
							.getEventListenerModels()) {
						serverController.addEventListener(model);
					}
					for (FilterModel filterModel : serviceModel
							.getFilterModels()) {
						serverController.addFilter(filterModel);
					}
					for (ErrorPageModel model : serviceModel
							.getErrorPageModels()) {
						serverController.addErrorPage(model);
					}
				}
			}

			@Override
			public String toString() {
				return "ServerListener for " + serviceBundle;
			}
		};
		this.serverController.addListener(serverListener);
	}

	@Override
	public void stop() {
		LOG.debug("Stopping http service for: " + serviceBundle);
		this.serverController.removeListener(serverListener);
		for (ServletModel model : serviceModel.getServletModels()) {
			servletEvent(ServletEvent.UNDEPLOYING, serviceBundle, model);
			serverModel.removeServletModel(model);
			servletEvent(ServletEvent.UNDEPLOYED, serviceBundle, model);
		}
		for (FilterModel model : serviceModel.getFilterModels()) {
			serverModel.removeFilterModel(model);
		}
		for (ContextModel contextModel : serviceModel.getContextModels()) {
			serverController.removeContext(contextModel.getHttpContext());
		}
		serverModel.deassociateHttpContexts(serviceBundle);
	}

	/**
	 * From Http Service - cannot fix generics until underlying Http Service is
	 * corrected
	 */
	@Override
	public void registerServlet(final String alias, final Servlet servlet,
								@SuppressWarnings("rawtypes") final Dictionary initParams,
								final HttpContext httpContext) throws ServletException,
			NamespaceException {
		synchronized (lock) {
			this.registerServlet(alias, servlet, initParams, null, null,
					httpContext);
		}
	}

	@Override
	public void registerServlet(final String alias, final Servlet servlet,
								@SuppressWarnings("rawtypes") final Dictionary initParams,
								final Integer loadOnStartup, Boolean asyncSupported,
								final HttpContext httpContext) throws ServletException,
			NamespaceException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register servlet (alias={}). Using context [{}]", alias, contextModel);
		@SuppressWarnings("unchecked")
		final ServletModel model = new ServletModel(contextModel, servlet,
				alias, initParams, loadOnStartup, asyncSupported);
		registerServlet(model);
	}

	private void servletEvent(int type, Bundle bundle, ServletModel model) {
		Class<? extends Servlet> servletClass = model.getServletClass();
		if (servletClass == null) {
			servletClass = model.getServlet().getClass();
		}
		eventDispatcher.servletEvent(new ServletEvent(type, bundle, model
				.getAlias(), model.getName(), model.getUrlPatterns(), model.getServlet(), servletClass, model.getContextModel()
				.getHttpContext()));
	}

	private void registerServlet(ServletModel model)
			throws ServletException, NamespaceException {
		servletEvent(ServletEvent.DEPLOYING, serviceBundle, model);
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			serverModel.addServletModel(model);
			serverSuccess = true;
			serviceModel.addServletModel(model);
			serviceSuccess = true;
			serverController.addServlet(model);
			controllerSuccess = true;
			ContextModel contextModel = model.getContextModel();
			if (model.getServlet() != null && !isWebAppWebContainerContext(contextModel)) {
				try {
					serverController.getContext(contextModel).start();
					// CHECKSTYLE:OFF
				} catch (Exception e) {
					LOG.error("Could not start the servlet context for context path ["
							+ contextModel.getContextName() + "]", e);
				} //CHECKSTYLE:ON
			}
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeServletModel(model);
				}
				if (serverSuccess) {
					serverModel.removeServletModel(model);
				}
				servletEvent(ServletEvent.FAILED, serviceBundle, model);
			} else {
				servletEvent(ServletEvent.DEPLOYED, serviceBundle, model);
			}
		}
	}

	private boolean isWebAppWebContainerContext(ContextModel contextModel) {
		return contextModel
				.getHttpContext()
				.getClass()
				.getName()
				.equals("org.ops4j.pax.web.extender.war.internal.WebAppWebContainerContext");
	}

	@Override
	public void registerResources(final String alias, final String name,
								  final HttpContext httpContext) throws NamespaceException {
		synchronized (lock) {
			final ContextModel contextModel = getOrCreateContext(httpContext);
			LOG.debug("Register resources (alias={}). Using context [" + contextModel + "]");
			final Servlet servlet = serverController.createResourceServlet(
					contextModel, alias, name);
			String resourceModelName = name;
			if (!"default".equals(name)) {
				// PAXWEB-1099 - we should be able to register multiple "resources" for same name (==basePath)
				// but under different alias
				resourceModelName = alias + ":" + name;
			}
			final ResourceModel model = new ResourceModel(contextModel, servlet,
					alias, resourceModelName);
			try {
				registerServlet(model);
			} catch (ServletException e) {
				LOG.error("Caught ServletException: ", e);
				throw new NamespaceException("Resource cant be resolved: ", e);
			}
		}
	}

	@Override
	public void unregister(final String alias) {
		synchronized (lock) {
			LOG.debug("Unregister servlet (alias={})", alias);
			final ServletModel model = serviceModel.getServletModelWithAlias(alias);
			if (model == null) {
				throw new IllegalArgumentException("Alias [" + alias
						+ "] was never registered");
			}
			servletEvent(ServletEvent.UNDEPLOYING, serviceBundle, model);
			serverModel.removeServletModel(model);
			serviceModel.removeServletModel(model);
			serverController.removeServlet(model);
			servletEvent(ServletEvent.UNDEPLOYED, serviceBundle, model);
		}
	}

	@Override
	public WebContainerContext createDefaultHttpContext() {
		return new DefaultHttpContext(serviceBundle, WebContainerContext.DefaultContextIds.DEFAULT.getValue());
	}

	@Override
	public WebContainerContext createDefaultHttpContext(String contextID) {
		return new DefaultHttpContext(serviceBundle, contextID);
	}

	@Override
	public SharedWebContainerContext createDefaultSharedHttpContext() {
		return new DefaultSharedWebContainerContext();
	}

	/**
	 * @see WebContainer#registerServlet(Servlet, String[], Dictionary,
	 * HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
								final String[] urlPatterns, final Dictionary<String, ?> initParams,
								final HttpContext httpContext) throws ServletException {
		registerServlet(servlet, null, urlPatterns, initParams, null, null,
				httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns,
								Dictionary<String, ?> initParams, Integer loadOnStartup,
								Boolean asyncSupported, HttpContext httpContext)
			throws ServletException {
		registerServlet(servlet, null, urlPatterns, initParams, loadOnStartup,
				asyncSupported, httpContext);
	}

	/**
	 * @see WebContainer#registerServlet(javax.servlet.Servlet, String,
	 * String[], java.util.Dictionary, org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
								final String servletName, final String[] urlPatterns,
								final Dictionary<String, ?> initParams,
								final HttpContext httpContext) throws ServletException {
		registerServlet(servlet, servletName, urlPatterns, initParams, null,
				null, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName,
								String[] urlPatterns, Dictionary<String, ?> initParams,
								Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig,
								HttpContext httpContext) throws ServletException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register servlet (name={}). Using context [{}]", servletName, contextModel);
		final ServletModel model = new ServletModel(contextModel, servlet,
				servletName, urlPatterns, null, // no alias
				initParams, loadOnStartup, asyncSupported, multiPartConfig);
		try {
			registerServlet(model);
		} catch (NamespaceException ignore) {
			// never thrown as model contains no alias
		}
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName,
								String[] urlPatterns, Dictionary<String, ?> initParams,
								Integer loadOnStartup, Boolean asyncSupported,
								HttpContext httpContext) throws ServletException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register servlet (name={}). Using context [{}]", servletName, contextModel);
		final ServletModel model = new ServletModel(contextModel, servlet,
				servletName, urlPatterns, null, // no alias
				initParams, loadOnStartup, asyncSupported, null);
		try {
			registerServlet(model);
		} catch (NamespaceException ignore) {
			// never thrown as model contains no alias
		}
	}


	/**
	 * @see WebContainer#unregisterServlet(Servlet)
	 */
	@Override
	public void unregisterServlet(final Servlet servlet) {
		final ServletModel model = serviceModel.removeServlet(servlet);
		if (model != null) {
			LOG.debug("Unregister servlet (servlet={})", servlet);
			servletEvent(ServletEvent.UNDEPLOYING, serviceBundle, model);
			serverModel.removeServletModel(model);
			serverController.removeServlet(model);
			servletEvent(ServletEvent.UNDEPLOYED, serviceBundle, model);
		}
	}

	@Override
	public void unregisterServlet(String servletName) {
		ServletModel model = serviceModel.removeServlet(servletName);
		if (model != null) {
			LOG.debug("Unregister servlet (name={})", servletName);
			servletEvent(ServletEvent.UNDEPLOYING, serviceBundle, model);
			serverModel.removeServletModel(model);
			serverController.removeServlet(model);
			servletEvent(ServletEvent.UNDEPLOYED, serviceBundle, model);
		}
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
								final String[] urlPatterns, final Dictionary<String, ?> initParams,
								final HttpContext httpContext) throws ServletException {
		this.registerServlet(servletClass, urlPatterns, initParams, null, null,
				httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
								String[] urlPatterns, Dictionary<String, ?> initParams,
								Integer loadOnStartup, Boolean asyncSupported,
								HttpContext httpContext) throws ServletException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register servlet (class={}). Using context [{}]", servletClass, contextModel);
		final ServletModel model = new ServletModel(contextModel, servletClass,
				null, urlPatterns, null, // no name, no alias
				initParams, loadOnStartup, asyncSupported, null);
		try {
			registerServlet(model);
		} catch (NamespaceException ignore) {
			// never thrown as model contains no alias
		}
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
								String[] urlPatterns, Dictionary<String, ?> initParams,
								Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig,
								HttpContext httpContext) throws ServletException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register servlet (class={}). Using context [{}]", servletClass, contextModel);
		final ServletModel model = new ServletModel(contextModel, servletClass,
				null, urlPatterns, null, // no name, no alias
				initParams, loadOnStartup, asyncSupported, multiPartConfig);
		try {
			registerServlet(model);
		} catch (NamespaceException ignore) {
			// never thrown as model contains no alias
		}
	}

	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		final Set<ServletModel> models = serviceModel
				.removeServletClass(servletClass);
		if (models != null) {
			for (ServletModel model : models) {
				if (model != null) {
					LOG.debug("Unregister servlet (servlet={})", model.getServlet());
				}
				servletEvent(ServletEvent.UNDEPLOYING, serviceBundle, model);
				serverModel.removeServletModel(model);
				serverController.removeServlet(model);
				servletEvent(ServletEvent.UNDEPLOYED, serviceBundle, model);
			}
		}
	}

	@Override
	public void registerEventListener(final EventListener listener,
									  final HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register event listener (listener={}). Using context [{}]", listener, contextModel);
		final EventListenerModel model = new EventListenerModel(contextModel,
				listener);
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			serviceModel.addEventListenerModel(model);
			serviceSuccess = true;
			serverController.addEventListener(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeEventListener(listener);
				}
			}
		}
	}

	@Override
	public void unregisterEventListener(final EventListener listener) {
		final EventListenerModel model = serviceModel
				.removeEventListener(listener);
		if (model != null) {
			LOG.debug("Unegister event listener (listener={})", listener);
			serverController.removeEventListener(model);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerFilter(final Filter filter, final String[] urlPatterns,
							   final String[] servletNames,
							   final Dictionary<String, ?> initParams,
							   final HttpContext httpContext) {
		registerFilter(filter, urlPatterns, servletNames, (Dictionary<String, String>) initParams, false, httpContext);
	}

	@Override
	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
							   Dictionary<String, String> initParams, Boolean asyncSupported, HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		if (LOG.isDebugEnabled()) {
			if (urlPatterns != null) {
				LOG.debug("Register filter (urlPatterns={}). Using context [{}]", Arrays.asList(urlPatterns), contextModel);
			} else if (servletNames != null) {
				LOG.debug("Register filter (servletNames={}). Using context [{}]", Arrays.asList(servletNames), contextModel);
			} else {
				LOG.debug("Register filter. Using context [{}]", contextModel);
			}
		}
		final FilterModel model = new FilterModel(contextModel, filter,
				urlPatterns, servletNames, initParams, asyncSupported);
		if (initParams != null && !initParams.isEmpty()
				&& initParams.get(WebContainerConstants.FILTER_RANKING) != null
				&& serviceModel.getFilterModels().length > 0) {
			String filterRankingString = initParams.get(WebContainerConstants.FILTER_RANKING);
			Integer filterRanking = Integer.valueOf(filterRankingString);
			FilterModel[] filterModels = serviceModel.getFilterModels();
			Integer firstRanking = Integer.valueOf(filterModels[0].getInitParams().get(WebContainerConstants.FILTER_RANKING));
			Integer lastRanking;

			if (filterModels.length == 1) {
				lastRanking = firstRanking;
			} else {
				lastRanking = Integer.valueOf(filterModels[filterModels.length - 1].getInitParams().get(WebContainerConstants.FILTER_RANKING));
			}

			//DO ordering of filters ...
			if (filterRanking < firstRanking) {
				//unregister the old one
				Arrays.stream(filterModels).forEach(this::unregister);
				//register the new model as first one
				registerFilter(model);
				//keep on going, and register the previously known one again.
				Arrays.stream(filterModels).forEach(this::registerFilter);
			} else if (filterRanking > lastRanking) {
				registerFilter(model);
			} else {
				//unregister all filters ranked lower
				List<FilterModel> filteredModels = Arrays.stream(filterModels)
						.filter(removableFilterModel -> Integer.valueOf(removableFilterModel.getInitParams().get(WebContainerConstants.FILTER_RANKING)) > filterRanking)
						.collect(Collectors.toList());
				filteredModels.forEach(this::unregister);

				//register the new model
				registerFilter(model);

				//re-register the filtered models
				filteredModels.forEach(this::registerFilter);
			}
		} else {
			registerFilter(model);
		}
	}


	private void unregister(FilterModel model) {
		if (model != null) {
			LOG.debug("Unregister filter (filter={})", model.getFilter());
			serviceModel.removeFilter(model.getName());
			serverModel.removeFilterModel(model);
			serverController.removeFilter(model);
		}
	}

	private void registerFilter(FilterModel model) {
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			serverModel.addFilterModel(model);
			serverSuccess = true;
			serviceModel.addFilterModel(model);
			serviceSuccess = true;
			serverController.addFilter(model);
			controllerSuccess = true;
			ContextModel contextModel = model.getContextModel();
			if (model.getFilter() != null && !isWebAppWebContainerContext(contextModel)) {
				try {
					serverController.getContext(contextModel).start();
					// CHECKSTYLE:OFF
				} catch (Exception e) {
					LOG.error("Could not start the servlet context for context path ["
							+ contextModel.getContextName() + "]", e);
				} //CHECKSTYLE:ON
			}
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeFilter(model.getName());
				}
				if (serverSuccess) {
					serverModel.removeFilterModel(model);
				}
			}
		}
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
							   Dictionary<String, String> initParams, HttpContext httpContext) {
		registerFilter(filterClass, urlPatterns, servletNames, initParams, false, httpContext);
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass,
							   String[] urlPatterns, String[] servletNames,
							   Dictionary<String, String> initParams, boolean asyncSupported, HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		if (LOG.isDebugEnabled()) {
			if (urlPatterns != null) {
				LOG.debug("Register filter (urlPatterns={}). Using context [{}]", Arrays.asList(urlPatterns), contextModel);
			} else if (servletNames != null) {
				LOG.debug("Register filter (servletNames={}). Using context [{}]", Arrays.asList(servletNames), contextModel);
			} else {
				LOG.debug("Register filter. Using context [{}]", contextModel);
			}
		}
		final FilterModel model = new FilterModel(contextModel, filterClass,
				urlPatterns, servletNames, initParams, asyncSupported);
		registerFilter(model);
	}

	@Override
	public void unregisterFilter(final Filter filter) {
		final FilterModel model = serviceModel.removeFilter(filter);
		if (model != null) {
			LOG.debug("Unregister filter (filter={})", filter);
			serverModel.removeFilterModel(model);
			serverController.removeFilter(model);
		}
	}

	@Override
	public void unregisterFilter(Class<? extends Filter> filterClass) {
		final FilterModel model = serviceModel.removeFilter(filterClass);
		if (model != null) {
			LOG.debug("Unregister filter (class={})", filterClass);
			serverModel.removeFilterModel(model);
			serverController.removeFilter(model);
		}
	}

	@Override
	public void unregisterFilter(String filterName) {
		final FilterModel model = serviceModel.removeFilter(filterName);
		if (model != null) {
			LOG.debug("Unregister filter (name={})", filterName);
			serverModel.removeFilterModel(model);
			serverController.removeFilter(model);
		}
	}

	/**
	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
	 */
	@Override
	public void setContextParam(final Dictionary<String, ?> params,
								final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		Map<String, String> contextParams = contextModel.getContextParams();
		if (!contextParams.equals(params)) {
			if (!serviceModel.canBeConfigured(httpContext)) {
				throw new IllegalStateException(
						"Http context already used. Context params can be set/changed only before first usage");
			}
			contextModel.setContextParams(params);
		}
		serviceModel.addContextModel(contextModel);
	}

	@Override
	public void setSessionTimeout(final Integer minutes,
								  final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		Integer sessionTimeout = contextModel.getSessionTimeout();
		if (!(minutes == sessionTimeout || minutes != null // FIXME comparison?
				&& minutes.equals(sessionTimeout))) {
			if (!serviceModel.canBeConfigured(httpContext)) {
				throw new IllegalStateException(
						"Http context already used. Session timeout can be set/changed only before first usage");
			}
			contextModel.setSessionTimeout(minutes);
		}
		serviceModel.addContextModel(contextModel);
	}

	/**
	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
							 final HttpContext httpContext) {
		registerJsps(urlPatterns, null, httpContext);
	}

	/**
	 * @see WebContainer#registerJsps(String[], HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
							 final Dictionary<String, ?> initParams,
							 final HttpContext httpContext) {
		registerJspServlet(urlPatterns, initParams, httpContext, null);
	}

	@Override
	public void registerJspServlet(final String[] urlPatterns,
								   final HttpContext httpContext, final String jspFile) {
		registerJspServlet(urlPatterns, null, httpContext, jspFile);
	}

	@Override
	public void registerJspServlet(final String[] urlPatterns,
								   Dictionary<String, ?> initParams, final HttpContext httpContext,
								   final String jspFile) {
		if (!SupportUtils.isJSPAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		final Servlet jspServlet = new JspServletWrapper(serviceBundle, jspFile);
		final ContextModel contextModel = getOrCreateContext(httpContext);
		//CHECKSTYLE:OFF
		initParams = createInitParams(contextModel,
				initParams == null ? new Hashtable<>()
						: initParams);
		//CHECKSTYLE:ON
		serviceModel.addContextModel(contextModel);
		try {
			registerServlet(jspServlet, getJspServletName(jspFile),
					urlPatterns == null ? new String[]{"*.jsp"}
							: urlPatterns, initParams, httpContext);
		} catch (ServletException ignore) {
			// this should never happen
			LOG.error("Internal error. Please report.", ignore);
		}
		Map<Servlet, String[]> jspServlets = contextModel.getJspServlets();
		jspServlets.put(jspServlet, urlPatterns);

	}

	private String getJspServletName(String jspFile) {
		return jspFile == null ? PAX_WEB_JSP_SERVLET : null;
	}

	@SuppressWarnings("unchecked")
	private Dictionary<String, ?> createInitParams(ContextModel contextModel,
												   Dictionary<String, ?> initParams) {
		Queue<Configuration> configurations = new LinkedList<>();
		Configuration serverControllerConfiguration = serverController
				.getConfiguration();
		if (initParams != null) {
			PropertyResolver propertyResolver = new DictionaryPropertyResolver(
					initParams);
			Configuration configuration = new ConfigurationImpl(
					propertyResolver);
			configurations.add(configuration);
		}
		configurations.add(serverControllerConfiguration);
		for (Configuration configuration : configurations) {
			String scratchDir = configuration.getJspScratchDir();
			if (scratchDir == null) {
				File temporaryDirectory = configuration.getTemporaryDirectory();
				if (temporaryDirectory != null) {
					scratchDir = temporaryDirectory.toString();
				}
			}
			if (configuration.equals(serverControllerConfiguration)) {
				// [PAXWEB-225] creates a bundle specific scratch dir
				File tempDir = new File(scratchDir,
						contextModel.getContextName());
				if (!tempDir.exists()) {
					tempDir.mkdirs();
				}
				scratchDir = tempDir.toString();
			}

			Integer jspCheckInterval = configuration.getJspCheckInterval();
			Boolean jspClassDebugInfo = configuration.getJspClassDebugInfo();
			Boolean jspDevelopment = configuration.getJspDevelopment();
			Boolean jspEnablePooling = configuration.getJspEnablePooling();
			String jspIeClassId = configuration.getJspIeClassId();
			String jspJavaEncoding = configuration.getJspJavaEncoding();
			Boolean jspKeepgenerated = configuration.getJspKeepgenerated();
			String jspLogVerbosityLevel = configuration
					.getJspLogVerbosityLevel();
			Boolean jspMappedfile = configuration.getJspMappedfile();
			Integer jspTagpoolMaxSize = configuration.getJspTagpoolMaxSize();
			Boolean jspPrecompilation = configuration.getJspPrecompilation();

			// TODO: fix this with PAXWEB-226
			Map<String, Object> params = new HashMap<>(12);
			params.put("checkInterval", jspCheckInterval);
			params.put("classdebuginfo", jspClassDebugInfo);
			params.put("development", jspDevelopment);
			params.put("enablePooling", jspEnablePooling);
			params.put("ieClassId", jspIeClassId);
			params.put("javaEncoding", jspJavaEncoding);
			params.put("keepgenerated", jspKeepgenerated);
			params.put("logVerbosityLevel", jspLogVerbosityLevel);
			params.put("mappedfile", jspMappedfile);
			params.put("scratchdir", scratchDir);
			params.put("tagpoolMaxSize", jspTagpoolMaxSize);
			params.put("usePrecompiled", jspPrecompilation);

			params.keySet().removeAll(Collections.list(initParams.keys()));
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object param = entry.getValue();
				if (param != null) {
					String initParam = entry.getKey();
					((Hashtable<String, Object>) initParams).put(initParam,
							param.toString());
				}
			}

		}
		LOG.debug("JSP scratchdir: " + initParams.get("scratchdir"));
		return initParams;
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final HttpContext httpContext) {
		if (!SupportUtils.isJSPAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = serviceModel
				.getContextModel(httpContext);
		if (contextModel == null) {
			throw new IllegalArgumentException(
					"Jsp support is not enabled for http context ["
							+ httpContext + "]");
		}
		for (Iterator<Servlet> jspServlets = contextModel.getJspServlets()
				.keySet().iterator(); jspServlets.hasNext(); ) {
			Servlet jspServlet = jspServlets.next();
			try {
				unregisterServlet(jspServlet);
			} finally {
				jspServlets.remove();
			}
		}
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final String[] urlPatterns,
							   final HttpContext httpContext) {
		if (!SupportUtils.isJSPAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = serviceModel
				.getContextModel(httpContext);
		if (contextModel == null) {
			throw new IllegalArgumentException(
					"Jsp support is not enabled for http context ["
							+ httpContext + "]");
		}
		for (Iterator<Map.Entry<Servlet, String[]>> jspServlets = contextModel
				.getJspServlets().entrySet().iterator(); jspServlets.hasNext(); ) {
			Map.Entry<Servlet, String[]> entry = jspServlets.next();
			String[] candidateUrlPatterns = entry.getValue();
			if (Arrays.equals(urlPatterns, candidateUrlPatterns)) {
				Servlet jspServlet = entry.getKey();
				try {
					unregisterServlet(jspServlet);
				} finally {
					jspServlets.remove();
				}
			}
		}
	}

	/**
	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
	 */
	@Override
	public void registerErrorPage(final String error, final String location,
								  final HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register error page (error={}, location={}). Using context [{}]", error, location, contextModel);
		final ErrorPageModel model = new ErrorPageModel(contextModel, error,
				location);
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			serviceModel.addErrorPageModel(model);
			serviceSuccess = true;
			serverController.addErrorPage(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeErrorPage(error, contextModel);
				}
			}
		}
	}

	/**
	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
	 */
	@Override
	public void unregisterErrorPage(final String error,
									final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ErrorPageModel model = serviceModel.removeErrorPage(error,
				serviceModel.getContextModel(httpContext));
		if (model != null) {
			LOG.debug("Unregister error page (error={})", error);
			serverController.removeErrorPage(model);
		}
	}

	/**
	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
	 */
	@Override
	public void registerWelcomeFiles(final String[] welcomeFiles,
									 final boolean redirect, final HttpContext httpContext) {
		ContextModel contextModel = serviceModel.getContextModel(httpContext);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Register welcome files (welcomeFiles={}). Using context [{}]", Arrays.asList(welcomeFiles), contextModel);
		}
		//PAXWEB-123: try to use the setWelcomeFile method
		final WelcomeFileModel model = new WelcomeFileModel(contextModel, welcomeFiles);

		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			serviceModel.addWelcomeFileModel(model);
			serviceSuccess = true;
			serverController.addWelcomFiles(model);
			controllerSuccess = true;
			if (model.getWelcomeFiles() != null && !isWebAppWebContainerContext(contextModel)) {
				try {
					serverController.getContext(contextModel).start();
					// CHECKSTYLE:OFF
				} catch (Exception e) {
					LOG.error("Could not start the servlet context for context path ["
							+ contextModel.getContextName() + "]", e);
				} //CHECKSTYLE:ON
			}
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeWelcomeFileModel(Arrays.toString(welcomeFiles), contextModel);
				}
			}
		}
	}

	/**
	 * @see WebContainer#unregisterWelcomeFiles(String[], HttpContext)
	 */
	@Override
	public void unregisterWelcomeFiles(final String[] welcomeFiles, final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		NullArgumentException.validateNotNull(welcomeFiles, "WelcomeFiles");
		final ContextModel contextModel = serviceModel
				.getContextModel(httpContext);
		//PAXWEB-123: try to use the setWelcomeFile method

		final WelcomeFileModel model = serviceModel.removeWelcomeFileModel(Arrays.toString(welcomeFiles), contextModel);
		if (model != null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unregister welcome files (welcomeFiles={})", Arrays.asList(welcomeFiles));
			}
			serverController.removeWelcomeFiles(model);
		}
		/*
		if (contextModel == null
				|| contextModel.getWelcomeFilesFilter() == null) {
			throw new IllegalArgumentException(
					"Welcome files are not registered for http context ["
							+ httpContext + "]");
		}
		try {
			unregisterFilter(contextModel.getWelcomeFilesFilter());
		} finally {
			contextModel.setWelcomeFilesFilter(null);
		}
		*/
	}

	@Override
	public void registerLoginConfig(String authMethod, String realmName,
									String formLoginPage, String formErrorPage, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		String contextModelAuthMethod = contextModel.getAuthMethod();
		String contextModelRealmName = contextModel.getRealmName();
		String contextModelFormLoginPage = contextModel.getFormLoginPage();
		String contextModelFormErrorPage = contextModel.getFormErrorPage();
		if (!Arrays.asList(contextModelAuthMethod, contextModelRealmName,
				contextModelFormLoginPage, contextModelFormErrorPage).equals(
				Arrays.asList(authMethod, realmName, formLoginPage,
						formErrorPage))) {
			if (!serviceModel.canBeConfigured(httpContext)) {
				throw new IllegalStateException(
						"Http context already used. Session timeout can be set/changed only before first usage");
			}
			contextModel.setAuthMethod(authMethod);
			contextModel.setRealmName(realmName);
			contextModel.setFormLoginPage(formLoginPage);
			contextModel.setFormErrorPage(formErrorPage);
		}
		serviceModel.addContextModel(contextModel);
	}

	@Override
	public void unregisterLoginConfig(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = serviceModel
				.getContextModel(httpContext);
		if (contextModel == null || contextModel.getAuthMethod() == null
				|| contextModel.getRealmName() == null) {
			throw new IllegalArgumentException(
					"Security Realm and authorization method are not registered for http context ["
							+ httpContext + "]");
		}
		try {
			// NOP
		} finally { // NOPMD
			// NOP
		}
	}

	@Override
	public void registerConstraintMapping(String constraintName, String url,
										  String mapping, String dataConstraint, boolean authentication,
										  List<String> roles, HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Register constraint mapping (name={}). Using context [{}]", constraintName, contextModel);
		SecurityConstraintMappingModel secConstraintMapModel = new SecurityConstraintMappingModel(
				contextModel, constraintName, mapping, url, dataConstraint,
				authentication, roles);
		serviceModel.addSecurityConstraintMappingModel(secConstraintMapModel);
		serverController.addSecurityConstraintMapping(secConstraintMapModel);
	}

	@Override
	public void unregisterConstraintMapping(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		// NOP
	}

	@Override
	public void registerServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer,
			Class<?>[] classes, final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");

		Set<Class<?>> clazzes = new HashSet<>();
		if (classes != null) {
			Collections.addAll(clazzes, classes);
		}
		Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel
				.getContainerInitializers();
		Set<Class<?>> containerInitializersClasses = containerInitializers == null ? null
				: containerInitializers.get(servletContainerInitializer);
		if (!clazzes.equals(containerInitializersClasses)) {
			if (!serviceModel.canBeConfigured(httpContext)) {
				throw new IllegalStateException(
						"Http context already used. ServletContainerInitializer can be set/changed only before first usage");
			}
			contextModel.addContainerInitializer(servletContainerInitializer,
					clazzes);
		}

		serviceModel.addContextModel(contextModel);

	}

	@Override
	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		URL contextModelJettyWebXmlURL = contextModel.getJettyWebXmlURL();
		if (!(contextModelJettyWebXmlURL == jettyWebXmlURL || contextModelJettyWebXmlURL != null
				&& contextModelJettyWebXmlURL.equals(jettyWebXmlURL))) {
			if (!serviceModel.canBeConfigured(httpContext)) {
				throw new IllegalStateException(
						"Http context already used. ServletContainerInitializer can be set/changed only before first usage");
			}
			contextModel.setJettyWebXmlUrl(jettyWebXmlURL);
		}
		serviceModel.addContextModel(contextModel);

	}

	private ContextModel getOrCreateContext(final HttpContext httpContext) {
		final WebContainerContext context;
		if (httpContext == null) {
			context = createDefaultHttpContext();
		} else if (!(httpContext instanceof WebContainerContext)) {
			context = new WebContainerContextWrapper(serviceBundle, httpContext);
		} else {
			context = (WebContainerContext) httpContext;
		}
		serverModel.associateHttpContext(context, serviceBundle,
				httpContext instanceof SharedWebContainerContext);
		ContextModel contextModel = serviceModel.getContextModel(context);
		if (contextModel == null) {
			contextModel = new ContextModel(context, serviceBundle,
					bundleClassLoader);
			contextModel.setVirtualHosts(serverController.getConfiguration()
					.getVirtualHosts());
		}
		return contextModel;
	}

	@Override
	public SharedWebContainerContext getDefaultSharedHttpContext() {
		return sharedWebContainerContext;
	}

	@Override
	public void unregisterServletContainerInitializer(HttpContext httpContext) {
		//nothing to do
	}

	@Override
	public void begin(HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Beginning and ssing context [" + contextModel + "]");
		try {
			serverController.getContext(contextModel);
			contextModel.setWebBundle(true);
			//CHECKSTYLE:OFF
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Exception starting HttpContext registration", e);
		}
		//CHECKSTYLE:ON
	}

	@Override
	public void end(HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		try {
			serverController.getContext(contextModel).start();
			//CHECKSTYLE:OFF
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			LOG.error("Exception finalizing HttpContext registration", e);
		}
		//CHECKSTYLE:ON
	}

	@Override
	public void setConnectorsAndVirtualHosts(List<String> connectors, List<String> virtualHosts,
											 HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		if (!serviceModel.canBeConfigured(httpContext)) {
			throw new IllegalStateException(
					"Http context already used. ServletContainerInitializer can be set only before first usage");
		}

		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		List<String> realVirtualHosts = new LinkedList<>(virtualHosts);
		if (connectors.size() > 0) {
			for (String connector : connectors) {
				realVirtualHosts.add("@" + connector);
			}
		}
		if (realVirtualHosts.size() == 0) {
			realVirtualHosts = this.serverController.getConfiguration()
					.getVirtualHosts();
		}
		if (LOG.isDebugEnabled()) {
			StringBuilder sb = new StringBuilder("VirtualHostList=[");
			for (String virtualHost : realVirtualHosts) {
				sb.append(virtualHost).append(",");
			}
			sb.append("]");
			LOG.debug(sb.toString());
		}
		contextModel.setVirtualHosts(realVirtualHosts);
		serviceModel.addContextModel(contextModel);
	}

	@Override
	public void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");

		contextModel.addTagLibLocation(tagLibLocation);
		contextModel.addTagLibUri(tagLibUri);

		serviceModel.addContextModel(contextModel);
	}

	@Override
	public void registerJspConfigPropertyGroup(List<String> includeCodes,
											   List<String> includePreludes, List<String> urlPatterns, Boolean elIgnored, Boolean scriptingInvalid,
											   Boolean isXml, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");

		contextModel.addJspIncludeCodes(includeCodes);
		contextModel.addJspIncludePreludes(includePreludes);
		contextModel.addJspUrlPatterns(urlPatterns);
		contextModel.addJspElIgnored(elIgnored);
		contextModel.addJspScriptingInvalid(scriptingInvalid);
		contextModel.addJspIsXml(isXml);

		serviceModel.addContextModel(contextModel);
	}

	@Override
	public void registerWebSocket(final Object webSocket, final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http Context");
		NullArgumentException.validateNotNull(webSocket, "WebSocket");

		ContextModel contextModel = getOrCreateContext(httpContext);

		WebSocketModel model = new WebSocketModel(contextModel, webSocket);

		boolean controllerSuccess = false;
		boolean serviceSuccess = false;
		try {
			contextModel.addContainerInitializer(new ServletContainerInitializer() {

				private Integer maxTry = 20;

				@Override
				public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
					Callable<Boolean> task = new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return registerWebSocket(ctx, 1);
						}
					};

					ExecutorService executor = Executors.newSingleThreadExecutor();
					Future<Boolean> future = executor.submit(task);

					try {
						Boolean success = future.get(maxTry * 500 + 2000, TimeUnit.MILLISECONDS);
						if (success) {
							LOG.info("registered WebSocket");
						} else {
							LOG.error("Failed to create WebSocket, obviosly the endpoint couldn't be registered");
						}
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						LOG.error("Failed to wait for registering of WebSocket", e);
					}

				}

				private boolean registerWebSocket(ServletContext ctx, int registerTry) {
					if (registerTry == maxTry) {
						LOG.error("Tried to Register Websocket for {} times, will stop now", registerTry);
						return false;
					}
					javax.websocket.server.ServerContainer serverContainer = (javax.websocket.server.ServerContainer) ctx.getAttribute(javax.websocket.server.ServerContainer.class.getName());
					if (serverContainer != null) {
						try {
							serverContainer.addEndpoint(webSocket.getClass());
							return true;
						} catch (DeploymentException e) {
							LOG.error("Failed to register WebSocket", e);
							return false;
						}
					} else {
						try {
							LOG.debug("couldn't find ServerContainer, will try again in 500ms");
							LOG.debug("this is the {} try", registerTry);
							Thread.sleep(500);
							return registerWebSocket(ctx, registerTry++);
						} catch (InterruptedException e) {
							LOG.error("Failed to register WebSocket due to: ", e);
							return false;
						}
					}
				}
			}, null);
			controllerSuccess = true;

			serviceModel.addWebSocketModel(model);
			serviceSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					serviceModel.removeWebSocketModel(webSocket);
				}
			}
		}

		if (!isWebAppWebContainerContext(contextModel)) {
			try {
				serverController.getContext(contextModel).start();
				// CHECKSTYLE:OFF
			} catch (Exception e) {
				LOG.error("Could not start the servlet context for context path ["
						+ contextModel.getContextName() + "]", e);
			} //CHECKSTYLE:ON
		}

	}

	@Override
	public void unregisterWebSocket(Object webSocket, HttpContext httpContext) {
		// TODO Auto-generated method stub
	}

	@Override
	public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator) {
		return withWhiteboardDtoService(service -> service.calculateRequestInfoDTO(path, iterator, serverModel, serviceModel));
	}

	@Override
	public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
		return withWhiteboardDtoService(service -> service.createWhiteboardRuntimeDTO(iterator, serverModel, serviceModel));
	}

	
	/**
	 * WhiteboardDtoService is registered as DS component. Should be removed if this class gets full DS support
	 * @param function a function which is applied against WhiteboardDtoService
	 * @param <T> Type of the functions return value
	 * @return value provided by given function
	 */
	private <T> T withWhiteboardDtoService(Function<WhiteboardDtoService, T> function){
		final BundleContext bundleContext = serviceBundle.getBundleContext();
		ServiceReference<WhiteboardDtoService> ref = bundleContext.getServiceReference(WhiteboardDtoService.class);
		if(ref != null){
			WhiteboardDtoService service = bundleContext.getService(ref);
			if(service != null){
				try{
					return function.apply(service);
				}finally {
					bundleContext.ungetService(ref);
				}
			}
		}
		throw new IllegalStateException(String.format("Service '%s' could not be retrieved!", WhiteboardDtoService.class.getName()));
	}

	@Override
	public String toString() {
		return super.toString() + " for bundle " + serviceBundle;
	}

    @Override
    public WebContainerDTO getWebcontainerDTO() {
        WebContainerDTO dto = new WebContainerDTO();
        
        dto.port = serverController.getHttpPort();
        dto.securePort = serverController.getHttpSecurePort();
        dto.listeningAddresses = serverController.getConfiguration().getListeningAddresses();
        
        return dto;
    }
}
