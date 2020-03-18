/*
 * Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010 Achim Nierbeck.
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
package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;
import java.util.concurrent.Executor;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.ops4j.pax.web.annotations.PaxWebConfiguration;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.ReferencedWebContainerContext;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.ops4j.pax.web.jsp.JspServletWrapper;

/**
 * <p><em>Enabled</em> {@link org.osgi.service.http.HttpService} means we can register web components. When bundle
 * (for which the Http Service is scoped) is stopped, all available references to this service will switch
 * to <em>disabled</em> {@link org.osgi.service.http.HttpService} delegate to prevent further registration.</p>
 */
class HttpServiceEnabled implements StoppableHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceEnabled.class);
//	private static final String PAX_WEB_JSP_SERVLET = "jsp";

	@Deprecated
	private static MultiBundleWebContainerContext multiBundleWebContainerContext;

	// the bundle for which this HttpService was created by ServiceFactory
	final Bundle serviceBundle;

	private final ServerModel serverModel;

	/**
	 * A view into global {@link ServerModel} from the perspective of bundle-scoped
	 * {@link org.osgi.service.http.HttpService} or bundle-related Whiteboard service registrations.
	 */
	private final ServiceModel serviceModel;

	private final Executor executor;

	@Review("It'd be cleaner if serverController was used from ServerModel or a service operating on ServerModel")
	private final ServerController serverController;
	private ClassLoader bundleClassLoader;
	private final Configuration configuration;

	private ServerListener serverListener;
	private final ServletListener eventDispatcher;

//	private final Boolean showStacks;

//	private final Object lock = new Object();

	static {
		multiBundleWebContainerContext = new DefaultMultiBundleWebContainerContext();
	}

	@Review("Should ServerListener be registered here?")
	HttpServiceEnabled(final Bundle bundle, final ServerController srvController,
			final ServerModel serverModel, final ServletListener eventDispatcher, final Configuration configuration) {
		LOG.debug("Creating active Http Service for: {}", bundle);

		this.serverModel = serverModel;
		this.serviceModel = new ServiceModel(serverModel);
		this.executor = serverModel.getExecutor();

		this.serviceBundle = bundle;

		this.serverController = srvController;

		this.eventDispatcher = eventDispatcher;
		this.configuration = configuration;

//				Set<Bundle> wiredBundles = ClassPathUtil.getBundlesInClassSpace(bundle,
//						new LinkedHashSet<>());
//				ArrayList<Bundle> bundles = new ArrayList<>();
//				bundles.add(bundle);
//				bundles.addAll(wiredBundles);
//				this.bundleClassLoader = new ResourceDelegatingBundleClassLoader(
//						bundles);
//				this.showStacks = configuration.server().isShowStacks();
//
//				this.serverListener = new ServerListener() {
//					@Override
//					@Review("Can't we do it better? Can we always register models via contoller and let it register them when needed?")
//					public void stateChanged(final ServerEvent event) {
//						LOG.debug("{}: Handling event: [{}]", this, event);
//
//						if (event == ServerEvent.STARTED) {
//							for (ServletModel model : serviceModel.getServletModels()) {
//								servletEvent(ServletEvent.State.DEPLOYING, serviceBundle,
//										model);
//								serverController.addServlet(model);
//								servletEvent(ServletEvent.State.DEPLOYED, serviceBundle,
//										model);
//							}
//							for (EventListenerModel model : serviceModel
//									.getEventListenerModels()) {
//								serverController.addEventListener(model);
//							}
//							for (FilterModel filterModel : serviceModel
//									.getFilterModels()) {
//								serverController.addFilter(filterModel);
//							}
//							for (ErrorPageModel model : serviceModel
//									.getErrorPageModels()) {
//								serverController.addErrorPage(model);
//							}
//						}
//					}
//
//					@Override
//					public String toString() {
//						return "ServerListener for " + serviceBundle;
//					}
//				};
//				this.serverController.addListener(serverListener);
	}

	// --- StoppableHttpService

	@Override
	@Review("Definitely good place to clean up things, but take care about shared contexts")
	public void stop() {
		LOG.debug("Stopping http service for: " + serviceBundle);
//		this.serverController.removeListener(serverListener);
//		for (ServletModel model : serviceModel.getServletModels()) {
//			servletEvent(ServletEvent.State.UNDEPLOYING, serviceBundle, model);
//			serverModel.removeServletModel(model);
//			servletEvent(ServletEvent.State.UNDEPLOYED, serviceBundle, model);
//		}
//		for (FilterModel model : serviceModel.getFilterModels()) {
//			serverModel.removeFilterModel(model);
//		}
//		for (OsgiContextModel contextModel : serviceModel.getContextModels()) {
//			serverController.removeContext(contextModel.getHttpContext());
//		}
//		serverModel.deassociateHttpContexts(serviceBundle);
	}

	// --- container views

	@Override
	public <T extends PaxWebContainerView> T adapt(Class<T> type) {
		return null;
	}

	// --- transactional access to web container

	@Override
	public void begin(HttpContext context) {
		// marks given context as participating in a transaction
	}

	@Override
	public void end(HttpContext context) {
		// ends a transaction and deassociates given context from any pending transaction
	}

	// --- different methods used to retrieve HttpContext

	@Override
	public WebContainerContext createDefaultHttpContext() {
		return new DefaultHttpContext(serviceBundle, WebContainerContext.DefaultContextIds.DEFAULT.getValue());
	}

	@Override
	public WebContainerContext createDefaultHttpContext(String contextID) {
		return new DefaultHttpContext(serviceBundle, contextID);
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext() {
		return new DefaultMultiBundleWebContainerContext();
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext(String contextId) {
		return new DefaultMultiBundleWebContainerContext(contextId);
	}

	@Override
	@Deprecated
	public MultiBundleWebContainerContext getDefaultSharedHttpContext() {
		return multiBundleWebContainerContext;
	}

	@Override
	public ReferencedWebContainerContext createReferencedContext(String contextId) {
		return new ReferencedWebContainerContext(serviceBundle, contextId);
	}

	// --- methods used to register a Servlet - with more options than in original HttpService.registerServlet()

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams, HttpContext httpContext)
			throws ServletException, NamespaceException {
		registerServlet(alias, servlet, initParams, null, null, httpContext);
	}

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext)
			throws ServletException, NamespaceException {
		// final stage of a path where we know about servlet registration using "alias" (the "old" HttpService way)
		ServletModel servletModel = new ServletModel(alias, servlet, initParams, loadOnStartup, asyncSupported);

		doRegisterServlet(httpContext, servletModel);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			HttpContext httpContext) throws ServletException {
		registerServlet(servlet, null, urlPatterns, initParams, null, null, null, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException {
		registerServlet(servlet, null, urlPatterns, initParams, loadOnStartup, asyncSupported, null, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		registerServlet(servlet, servletName, urlPatterns, initParams, null, null, null, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		registerServlet(servlet, servletName, urlPatterns, initParams, loadOnStartup, asyncSupported, null, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		// final stage of a path where we know about servlet registration using URL patterns and name
		ServletModel servletModel = new ServletModel(servletName, urlPatterns, servlet,
				initParams, loadOnStartup, asyncSupported, multiPartConfig);

		doRegisterServletWithoutAlias(httpContext, servletModel);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		registerServlet(servletClass, urlPatterns, initParams, null, null, null, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		registerServlet(servletClass, urlPatterns, initParams, loadOnStartup, asyncSupported, null, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		// final stage of a path where we register servlet by class name and URL patterns
		ServletModel servletModel = new ServletModel(urlPatterns, servletClass,
				initParams, loadOnStartup, asyncSupported, multiPartConfig);

		doRegisterServletWithoutAlias(httpContext, servletModel);
	}

	/**
	 * Register servlet knowing we can ignore {@link NamespaceException}.
	 *
	 * @param httpContext
	 * @param model
	 * @throws ServletException
	 */
	private void doRegisterServletWithoutAlias(HttpContext httpContext, ServletModel model) throws ServletException {
		try {
			doRegisterServlet(httpContext, model);
		} catch (NamespaceException ignored) {
		}
	}

	/**
	 * <p>Main, internal method to register given, fully defined {@link ServletModel} within an {@link OsgiContextModel}
	 * associated with given {@link HttpContext}.</p>
	 *
	 * <p>Method checks if the association is possible or creates one if there no {@link OsgiContextModel}
	 * available yet.</p>
	 *
	 * <p>Method runs semi transactionally - in single configuration/registration thread of Pax Web runtime.</p>
	 *
	 * @param httpContext
	 * @param model
	 * @throws ServletException
	 * @throws NamespaceException
	 */
	@PaxWebConfiguration
	private void doRegisterServlet(HttpContext httpContext, ServletModel model) throws ServletException, NamespaceException {
		LOG.debug("Passing registration of {} to configuration thread", model);

		final Batch batch = new Batch("Registration of " + model);
		final WebContainerContext webContext = unify(httpContext);

		try {
			servletEvent(ServletEvent.State.DEPLOYING, serviceBundle, model);

			serverModel.run(() -> {
				LOG.info("Registering {}", model);

				// each servlet registered through Http Service should be registered within single osgiContext
				// identified by given httpContext
				// TODO: for Whiteboard Service it's not that easy and OsgiContextModels are added during whiteboard
				//       service tracking
				OsgiContextModel contextModel
						= serverModel.getOrCreateOsgiContextModel(webContext, serviceBundle, batch);
				model.getContextModels().add(contextModel);

				// if the above association was correct, validate servlet model through server model
				// adding servlet model may lead to unregistration of some other, lower-ranked models, so batch
				// may have some unregistration changes added
				serverModel.addServletModel(model, batch);

				// just batch adding ServletModel to ServiceModel - no more validation needed
				batch.addServletModel(serviceModel, model);

				// only if validation was fine, pass the batch to ServerController, where the batch may fail again
				serverController.sendBatch(batch);

				// if server runtime has accepted the changes (hoping it'll be in clean state if it didn't), lets
				// actually apply the changes to global model (through ServiceModel)
				batch.accept(serviceModel);

				return null;
			});

			servletEvent(ServletEvent.State.DEPLOYED, serviceBundle, model);
		} catch (ServletException | NamespaceException e) {
			servletEvent(ServletEvent.State.FAILED, serviceBundle, model);
			throw e;
		} catch (Exception e) {
			servletEvent(ServletEvent.State.FAILED, serviceBundle, model);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- methods used to unregister a Servlet

	@Override
	public void unregister(final String alias) {
//				synchronized (lock) {
//					LOG.debug("Unregister servlet (alias={})", alias);
//					final ServletModel model = serviceModel.getServletModelWithAlias(alias);
//					if (model == null) {
//						throw new IllegalArgumentException("Alias [" + alias
//								+ "] was never registered");
//					}
//					servletEvent(ServletEvent.State.UNDEPLOYING, serviceBundle, model);
//					serverModel.removeServletModel(model);
//					serviceModel.removeServletModel(model);
//					serverController.removeServlet(model);
//					servletEvent(ServletEvent.State.UNDEPLOYED, serviceBundle, model);
//				}
	}

	@Override
	public void unregisterServlet(final Servlet servlet) {
//				final ServletModel model = serviceModel.removeServlet(servlet);
//				if (model != null) {
//					LOG.debug("Unregister servlet (servlet={})", servlet);
//					servletEvent(ServletEvent.State.UNDEPLOYING, serviceBundle, model);
//					serverModel.removeServletModel(model);
//					serverController.removeServlet(model);
//					servletEvent(ServletEvent.State.UNDEPLOYED, serviceBundle, model);
//				}
	}

	@Override
	public void unregisterServlet(String servletName) {
//				ServletModel model = serviceModel.removeServlet(servletName);
//				if (model != null) {
//					LOG.debug("Unregister servlet (name={})", servletName);
//					servletEvent(ServletEvent.State.UNDEPLOYING, serviceBundle, model);
//					serverModel.removeServletModel(model);
//					serverController.removeServlet(model);
//					servletEvent(ServletEvent.State.UNDEPLOYED, serviceBundle, model);
//				}
	}

	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
//				final Set<ServletModel> models = serviceModel.removeServletClass(servletClass);
//				if (models != null) {
//					for (ServletModel model : models) {
//						if (model != null) {
//							LOG.debug("Unregister servlet (servlet={})", model.getServlet());
//							servletEvent(ServletEvent.State.UNDEPLOYING, serviceBundle, model);
//							serverModel.removeServletModel(model);
//							serverController.removeServlet(model);
//							servletEvent(ServletEvent.State.UNDEPLOYED, serviceBundle, model);
//						}
//					}serverModel.
//				}
	}

	// --- methods used to register resources

	@Override
	public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
//				synchronized (lock) {
//					final OsgiContextModel contextModel = getOrCreateOsgiContextModel(httpContext);
//					LOG.debug("Register resources (alias={}). Using context [" + contextModel + "]");
//
//					// PAXWEB-1085, OSGi Enterprise R6 140.6 "Registering Resources", JavaEE Servlet spec "12.2 Specification of Mappings"
//					// "A string beginning with a ‘ / ’ character and ending with a ‘ /* ’ suffix is used for path mapping."
//					String osgiAlias = alias;
//					if (osgiAlias != null && osgiAlias.endsWith("/*")) {
//						osgiAlias = osgiAlias.substring(0, osgiAlias.length() - 2);
//					}
//					final Servlet servlet = serverController.createResourceServlet(contextModel, osgiAlias, name);
//					String resourceModelName = name;
//					if (!"default".equals(name)) {
//						// PAXWEB-1099 - we should be able to register multiple "resources" for same name (==basePath)
//						// but under different alias
//						resourceModelName = String.format("%s:%s", alias, "/".equals(name) ? "" : name);
//					}
//					final ResourceModel model = new ResourceModel(contextModel, servlet,
//							alias, resourceModelName);
//					try {
//						registerServlet(model);
//					} catch (ServletException e) {
//						LOG.error("Caught ServletException: ", e);
//						throw new NamespaceException("Resource cant be resolved: ", e);
//					}
//				}
	}

	// --- methods used to register a Filter

	@Override
	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		registerFilter(filter, null, urlPatterns, servletNames, initParams, null, httpContext);
	}

	@Override
	public void registerFilter(Filter filter, String filterName, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		// final stage of a path where we know about filter registration using an instance
		FilterModel filterModel = new FilterModel(filterName, urlPatterns, servletNames, null, filter,
				initParams, asyncSupported);

		doRegisterFilter(httpContext, filterModel);
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		registerFilter(filterClass, null, urlPatterns, servletNames, initParams, null, httpContext);
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String filterName, String[] urlPatterns,
			String[] servletNames, Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		// final stage of a path where we register filter using a class name
		FilterModel filterModel = new FilterModel(filterName, urlPatterns, servletNames, null, filterClass,
				initParams, asyncSupported);

		doRegisterFilter(httpContext, filterModel);
	}

	/**
	 * <p>Main, internal method to register given, fully defined {@link FilterModel} within an {@link OsgiContextModel}
	 * associated with given {@link HttpContext}.</p>
	 *
	 * <p>Method checks if the association is possible or creates one if there no {@link OsgiContextModel}
	 * available yet.</p>
	 *
	 * <p>Method runs semi transactionally - in single configuration/registration thread of Pax Web runtime.</p>
	 *
	 * @param httpContext
	 * @param model
	 * @throws ServletException
	 */
	@PaxWebConfiguration
	private void doRegisterFilter(HttpContext httpContext, FilterModel model) throws ServletException {
		LOG.debug("Passing registration of {} to configuration thread", model);

		final Batch batch = new Batch("Registration of " + model);
		final WebContainerContext webContext = unify(httpContext);

		try {
			serverModel.run(() -> {
				LOG.info("Registering {}", model);

				OsgiContextModel contextModel
						= serverModel.getOrCreateOsgiContextModel(webContext, serviceBundle, batch);
				model.getContextModels().add(contextModel);

				// we don't care about ranking here. Filters will be reorganized on every change anyway

				// batch change of entire model
				serverModel.addFilterModel(model, batch);

				// batch change of the model scoped to given service
				batch.addFilterModel(serviceModel, model);

				// send batch to Jetty/Tomcat/Undertow
				serverController.sendBatch(batch);

				// process the batch if server accepted == apply changes to the model
				batch.accept(serviceModel);

				return null;
			});
		} catch (NamespaceException cantHappenWheAddingFilters) {
		}
	}

	// --- methods used to unregister a Filter

	@Override
	public void unregisterFilter(Filter filter) {
	}

	@Override
	public void unregisterFilter(String filterName) {
	}

	@Override
	public void unregisterFilters(Class<? extends Filter> filterClass) {
	}

	// --- private support methods

	private void servletEvent(ServletEvent.State type, Bundle bundle, ServletModel model) {
		Class<? extends Servlet> servletClass = model.getServletClass();
		if (servletClass == null) {
			servletClass = model.getServlet().getClass();
		}
		eventDispatcher.servletEvent(new ServletEvent(type, bundle, model));
	}

	/**
	 * Ensure that proper {@link WebContainerContext} is used before operating on internal Pax Web model.
	 *
	 * @param httpContext
	 * @return
	 */
	private WebContainerContext unify(HttpContext httpContext) {
		// org.osgi.service.http.HttpContext -> org.ops4j.pax.web.service.WebContainerContext
		// to ensure an identity of the context
		final WebContainerContext context;
		if (httpContext == null) {
			context = createDefaultHttpContext();
		} else if (!(httpContext instanceof WebContainerContext)) {
			context = new WebContainerContextWrapper(serviceBundle, httpContext);
		} else {
			context = (WebContainerContext) httpContext;
		}

		return context;
	}

	/**
	 * Called to tell global {@link ServerModel} that given {@link OsgiContextModel} was initiated/created
	 * by given {@link WebContainerContext}.
	 * This method is not expected to throw any exception.
	 *
	 * @param webContext
	 * @param contextModel
	 */
	private void associateContextModel(WebContainerContext webContext, OsgiContextModel contextModel) {
		serverModel.associateHttpContext(webContext, contextModel);
	}
























//	private boolean isWebAppWebContainerContext(ContextModel contextModel) {
//		return contextModel
//				.getHttpContext()
//				.getClass()
//				.getName()
//				.equals("org.ops4j.pax.web.extender.war.internal.WebAppWebContainerContext");
//	}
//	@Override
//	public void registerEventListener(final EventListener listener,
//									  final HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Register event listener (listener={}). Using context [{}]", listener, contextModel);
//		final EventListenerModel model = new EventListenerModel(contextModel,
//				listener);
//		boolean serviceSuccess = false;
//		boolean controllerSuccess = false;
//		try {
//			serviceModel.addEventListenerModel(model);
//			serviceSuccess = true;
//			serverController.addEventListener(model);
//			controllerSuccess = true;
//		} finally {
//			// as this compensatory actions to work the remove methods should
//			// not throw exceptions.
//			if (!controllerSuccess) {
//				if (serviceSuccess) {
//					serviceModel.removeEventListener(listener);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void unregisterEventListener(final EventListener listener) {
//		final EventListenerModel model = serviceModel
//				.removeEventListener(listener);
//		if (model != null) {
//			LOG.debug("Unegister event listener (listener={})", listener);
//			serverController.removeEventListener(model);
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Override
//	public void registerFilter(final Filter filter, final String[] urlPatterns,
//							   final String[] servletNames,
//							   final Dictionary<String, ?> initParams,
//							   final HttpContext httpContext) {
//		registerFilter(filter, urlPatterns, servletNames, (Dictionary<String, String>) initParams, false, httpContext);
//	}
//
//	@Override
//	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
//							   Dictionary<String, String> initParams, Boolean asyncSupported, HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		if (LOG.isDebugEnabled()) {
//			if (urlPatterns != null) {
//				LOG.debug("Register filter (urlPatterns={}). Using context [{}]", Arrays.asList(urlPatterns), contextModel);
//			} else if (servletNames != null) {
//				LOG.debug("Register filter (servletNames={}). Using context [{}]", Arrays.asList(servletNames), contextModel);
//			} else {
//				LOG.debug("Register filter. Using context [{}]", contextModel);
//			}
//		}
//		final FilterModel model = new FilterModel(contextModel, filter,
//				urlPatterns, servletNames, initParams, asyncSupported);
//		if (initParams != null && !initParams.isEmpty()
//				&& initParams.get(PaxWebConstants.FILTER_RANKING) != null
//				&& serviceModel.getFilterModels().length > 0) {
//			String filterRankingString = initParams.get(PaxWebConstants.FILTER_RANKING);
//			Integer filterRanking = Integer.valueOf(filterRankingString);
//			FilterModel[] filterModels = serviceModel.getFilterModels();
//			Integer firstRanking = Integer.valueOf(filterModels[0].getInitParams().get(PaxWebConstants.FILTER_RANKING));
//			Integer lastRanking;
//
//			if (filterModels.length == 1) {
//				lastRanking = firstRanking;
//			} else {
//				lastRanking = Integer.valueOf(filterModels[filterModels.length - 1].getInitParams().get(PaxWebConstants.FILTER_RANKING));
//			}
//
//			//DO ordering of filters ...
//			if (filterRanking < firstRanking) {
//				//unregister the old one
//				Arrays.stream(filterModels).forEach(this::unregister);
//				//register the new model as first one
//				registerFilter(model);
//				//keep on going, and register the previously known one again.
//				Arrays.stream(filterModels).forEach(this::registerFilter);
//			} else if (filterRanking > lastRanking) {
//				registerFilter(model);
//			} else {
//				//unregister all filters ranked lower
//				List<FilterModel> filteredModels = Arrays.stream(filterModels)
//						.filter(removableFilterModel -> Integer.valueOf(removableFilterModel.getInitParams().get(PaxWebConstants.FILTER_RANKING)) > filterRanking)
//						.collect(Collectors.toList());
//				filteredModels.forEach(this::unregister);
//
//				//register the new model
//				registerFilter(model);
//
//				//re-register the filtered models
//				filteredModels.forEach(this::registerFilter);
//			}
//		} else {
//			registerFilter(model);
//		}
//	}
//
//
//	private void unregister(FilterModel model) {
//		if (model != null) {
//			LOG.debug("Unregister filter (filter={})", model.getFilter());
//			serviceModel.removeFilter(model.getName());
//			serverModel.removeFilterModel(model);
//			serverController.removeFilter(model);
//		}
//	}
//
//	private void registerFilter(FilterModel model) {
//		boolean serverSuccess = false;
//		boolean serviceSuccess = false;
//		boolean controllerSuccess = false;
//		try {
//			serverModel.addFilterModel(model);
//			serverSuccess = true;
//			serviceModel.addFilterModel(model);
//			serviceSuccess = true;
//			serverController.addFilter(model);
//			controllerSuccess = true;
//			ContextModel contextModel = model.getContextModel();
//			if (model.getFilter() != null && !isWebAppWebContainerContext(contextModel)) {
//				try {
//					serverController.getContext(contextModel).start();
//					// CHECKSTYLE:OFF
//				} catch (Exception e) {
//					LOG.error("Could not start the servlet context for context path ["
//							+ contextModel.getContextName() + "]", e);
//				} //CHECKSTYLE:ON
//			}
//		} finally {
//			// as this compensatory actions to work the remove methods should
//			// not throw exceptions.
//			if (!controllerSuccess) {
//				if (serviceSuccess) {
//					serviceModel.removeFilter(model.getName());
//				}
//				if (serverSuccess) {
//					serverModel.removeFilterModel(model);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
//							   Dictionary<String, String> initParams, HttpContext httpContext) {
//		registerFilter(filterClass, urlPatterns, servletNames, initParams, false, httpContext);
//	}
//
//	@Override
//	public void registerFilter(Class<? extends Filter> filterClass,
//							   String[] urlPatterns, String[] servletNames,
//							   Dictionary<String, String> initParams, boolean asyncSupported, HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		if (LOG.isDebugEnabled()) {
//			if (urlPatterns != null) {
//				LOG.debug("Register filter (urlPatterns={}). Using context [{}]", Arrays.asList(urlPatterns), contextModel);
//			} else if (servletNames != null) {
//				LOG.debug("Register filter (servletNames={}). Using context [{}]", Arrays.asList(servletNames), contextModel);
//			} else {
//				LOG.debug("Register filter. Using context [{}]", contextModel);
//			}
//		}
//		final FilterModel model = new FilterModel(contextModel, filterClass,
//				urlPatterns, servletNames, initParams, asyncSupported);
//		registerFilter(model);
//	}
//
//	@Override
//	public void unregisterFilter(final Filter filter) {
//		final FilterModel model = serviceModel.removeFilter(filter);
//		if (model != null) {
//			LOG.debug("Unregister filter (filter={})", filter);
//			serverModel.removeFilterModel(model);
//			serverController.removeFilter(model);
//		}
//	}
//
//	@Override
//	public void unregisterFilter(Class<? extends Filter> filterClass) {
//		final FilterModel model = serviceModel.removeFilter(filterClass);
//		if (model != null) {
//			LOG.debug("Unregister filter (class={})", filterClass);
//			serverModel.removeFilterModel(model);
//			serverController.removeFilter(model);
//		}
//	}
//
//	@Override
//	public void unregisterFilter(String filterName) {
//		final FilterModel model = serviceModel.removeFilter(filterName);
//		if (model != null) {
//			LOG.debug("Unregister filter (name={})", filterName);
//			serverModel.removeFilterModel(model);
//			serverController.removeFilter(model);
//		}
//	}
//
//	/**
//	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
//	 */
//	@Override
//	public void setContextParam(final Dictionary<String, ?> params,
//								final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		Map<String, String> contextParams = contextModel.getContextParams();
//		if (!contextParams.equals(params)) {
//			if (!serviceModel.canBeConfigured(httpContext)) {
//				try {
//					LOG.debug("Stopping context model {} to set context parameters", contextModel);
//					serverController.getContext(contextModel).stop();
//				} catch (Exception e) {
//					LOG.info(e.getMessage(), e);
//				}
//			}
//			contextModel.setContextParams(params);
//		}
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void setSessionTimeout(final Integer minutes,
//								  final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		Integer sessionTimeout = contextModel.getSessionTimeout();
//		if ((sessionTimeout != null && !sessionTimeout.equals(minutes)) || minutes != null) {
//			if (!serviceModel.canBeConfigured(httpContext)) {
//				throw new IllegalStateException(
//						"Http context already used. Session timeout can be set/changed only before first usage");
//			}
//			contextModel.setSessionTimeout(minutes);
//		}
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void setSessionCookieConfig(String domain, String name, Boolean httpOnly, Boolean secure, String path, Integer maxAge, HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		if (!serviceModel.canBeConfigured(httpContext)) {
//			throw new IllegalStateException(
//					"Http context already used. Session cookie configuration can be set/changed only before first usage");
//		}
//		contextModel.setSessionDomain(domain);
//		contextModel.setSessionCookie(name);
//		contextModel.setSessionCookieHttpOnly(httpOnly);
//		contextModel.setSessionCookieSecure(secure);
//		contextModel.setSessionPath(path);
//		contextModel.setSessionCookieMaxAge(maxAge);
//
//		serviceModel.addContextModel(contextModel);
//	}
//
//	/**
//	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
//	 */
//	@Override
//	public void registerJsps(final String[] urlPatterns,
//							 final HttpContext httpContext) {
//		registerJsps(urlPatterns, null, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerJsps(String[], HttpContext)
//	 */
//	@Override
//	public void registerJsps(final String[] urlPatterns,
//							 final Dictionary<String, ?> initParams,
//							 final HttpContext httpContext) {
//		registerJspServlet(urlPatterns, initParams, httpContext, null);
//	}
//
//	@Override
//	public void registerJspServlet(final String[] urlPatterns,
//								   final HttpContext httpContext, final String jspFile) {
//		registerJspServlet(urlPatterns, null, httpContext, jspFile);
//	}
//
//	@Override
//	public void registerJspServlet(final String[] urlPatterns,
//								   Dictionary<String, ?> initParams, final HttpContext httpContext,
//								   final String jspFile) {
////		if (!SupportUtils.isJSPAvailable()) {
////			throw new UnsupportedOperationException(
////					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
////		}
////		final Servlet jspServlet = new JspServletWrapper(serviceBundle, jspFile);
////		final ContextModel contextModel = getOrCreateContext(httpContext);
////		//CHECKSTYLE:OFF
////		initParams = createInitParams(contextModel, initParams == null ? new Hashtable<>() : initParams);
////		//CHECKSTYLE:ON
////		serviceModel.addContextModel(contextModel);
////		try {
////			registerServlet(jspServlet, getJspServletName(jspFile),
////					urlPatterns == null ? new String[]{"*.jsp"}
////							: urlPatterns, initParams, httpContext);
////		} catch (ServletException ignore) {
////			// this should never happen
////			LOG.error("Internal error. Please report.", ignore);
////		}
////		Map<Servlet, String[]> jspServlets = contextModel.getJspServlets();
////		jspServlets.put(jspServlet, urlPatterns);
//
//	}
//
//	private String getJspServletName(String jspFile) {
//		return null;
////		return jspFile == null ? PAX_WEB_JSP_SERVLET : null;
//	}
//
//	@SuppressWarnings("unchecked")
//	private Dictionary<String, ?> createInitParams(ContextModel contextModel,
//												   Dictionary<String, ?> initParams) {
//		NullArgumentException.validateNotNull(initParams, "Init params");
//		Queue<Configuration> configurations = new LinkedList<>();
//		Configuration serverControllerConfiguration = serverController.getConfiguration();
//
//		PropertyResolver propertyResolver = new DictionaryPropertyResolver(initParams);
//		Configuration c = new ConfigurationImpl(propertyResolver);
//		configurations.add(c);
//
//		configurations.add(serverControllerConfiguration);
//		for (Configuration configuration : configurations) {
//			String scratchDir = configuration.jsp().getJspScratchDir();
//			if (scratchDir == null) {
//				File temporaryDirectory = configuration.server().getTemporaryDirectory();
//				if (temporaryDirectory != null) {
//					scratchDir = temporaryDirectory.toString();
//				}
//			}
//			if (configuration.equals(serverControllerConfiguration)) {
//				// [PAXWEB-225] creates a bundle specific scratch dir
//				File tempDir = new File(scratchDir, contextModel.getContextName());
//				if (!tempDir.exists()) {
//					tempDir.mkdirs();
//				}
//				scratchDir = tempDir.toString();
//			}
//
//			Integer jspCheckInterval = configuration.jsp().getJspCheckInterval();
//			Boolean jspClassDebugInfo = configuration.jsp().getJspClassDebugInfo();
//			Boolean jspDevelopment = configuration.jsp().getJspDevelopment();
//			Boolean jspEnablePooling = configuration.jsp().getJspEnablePooling();
//			String jspIeClassId = configuration.jsp().getJspIeClassId();
//			String jspJavaEncoding = configuration.jsp().getJspJavaEncoding();
//			Boolean jspKeepgenerated = configuration.jsp().getJspKeepgenerated();
//			String jspLogVerbosityLevel = configuration.jsp().getJspLogVerbosityLevel();
//			Boolean jspMappedfile = configuration.jsp().getJspMappedfile();
//			Integer jspTagpoolMaxSize = configuration.jsp().getJspTagpoolMaxSize();
//			Boolean jspPrecompilation = configuration.jsp().getJspPrecompilation();
//
//			// TODO: fix this with PAXWEB-226
//			Map<String, Object> params = new HashMap<>(12);
//			params.put("checkInterval", jspCheckInterval);
//			params.put("classdebuginfo", jspClassDebugInfo);
//			params.put("development", jspDevelopment);
//			params.put("enablePooling", jspEnablePooling);
//			params.put("ieClassId", jspIeClassId);
//			params.put("javaEncoding", jspJavaEncoding);
//			params.put("keepgenerated", jspKeepgenerated);
//			params.put("logVerbosityLevel", jspLogVerbosityLevel);
//			params.put("mappedfile", jspMappedfile);
//			params.put("scratchdir", scratchDir);
//			params.put("tagpoolMaxSize", jspTagpoolMaxSize);
//			params.put("usePrecompiled", jspPrecompilation);
//
//			params.keySet().removeAll(Collections.list(initParams.keys()));
//			for (Map.Entry<String, Object> entry : params.entrySet()) {
//				Object param = entry.getValue();
//				if (param != null) {
//					String initParam = entry.getKey();
//					((Hashtable<String, Object>) initParams).put(initParam, param.toString());
//				}
//			}
//
//		}
//		LOG.debug("JSP scratchdir: " + initParams.get("scratchdir"));
//		return initParams;
//	}
//
//	/**
//	 * @see WebContainer#unregisterJsps(HttpContext)
//	 */
//	@Override
//	public void unregisterJsps(final HttpContext httpContext) {
//		if (!SupportUtils.isJSPAvailable()) {
//			throw new UnsupportedOperationException(
//					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
//		}
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = serviceModel
//				.getContextModel(httpContext);
//		if (contextModel == null) {
//			throw new IllegalArgumentException(
//					"Jsp support is not enabled for http context ["
//							+ httpContext + "]");
//		}
//		for (Iterator<Servlet> jspServlets = contextModel.getJspServlets()
//				.keySet().iterator(); jspServlets.hasNext(); ) {
//			Servlet jspServlet = jspServlets.next();
//			try {
//				unregisterServlet(jspServlet);
//			} finally {
//				jspServlets.remove();
//			}
//		}
//	}
//
//	/**
//	 * @see WebContainer#unregisterJsps(HttpContext)
//	 */
//	@Override
//	public void unregisterJsps(final String[] urlPatterns,
//							   final HttpContext httpContext) {
//		if (!SupportUtils.isJSPAvailable()) {
//			throw new UnsupportedOperationException(
//					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
//		}
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = serviceModel
//				.getContextModel(httpContext);
//		if (contextModel == null) {
//			throw new IllegalArgumentException(
//					"Jsp support is not enabled for http context ["
//							+ httpContext + "]");
//		}
//		for (Iterator<Map.Entry<Servlet, String[]>> jspServlets = contextModel
//				.getJspServlets().entrySet().iterator(); jspServlets.hasNext(); ) {
//			Map.Entry<Servlet, String[]> entry = jspServlets.next();
//			String[] candidateUrlPatterns = entry.getValue();
//			if (Arrays.equals(urlPatterns, candidateUrlPatterns)) {
//				Servlet jspServlet = entry.getKey();
//				try {
//					unregisterServlet(jspServlet);
//				} finally {
//					jspServlets.remove();
//				}
//			}
//		}
//	}
//
//	/**
//	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
//	 */
//	@Override
//	public void registerErrorPage(final String error, final String location,
//								  final HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Register error page (error={}, location={}). Using context [{}]", error, location, contextModel);
//		final ErrorPageModel model = new ErrorPageModel(contextModel, error,
//				location);
//		boolean serviceSuccess = false;
//		boolean controllerSuccess = false;
//		try {
//			serviceModel.addErrorPageModel(model);
//			serviceSuccess = true;
//			serverController.addErrorPage(model);
//			controllerSuccess = true;
//		} finally {
//			// as this compensatory actions to work the remove methods should
//			// not throw exceptions.
//			if (!controllerSuccess) {
//				if (serviceSuccess) {
//					serviceModel.removeErrorPage(error, contextModel);
//				}
//			}
//		}
//	}
//
//	/**
//	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
//	 */
//	@Override
//	public void unregisterErrorPage(final String error,
//									final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ErrorPageModel model = serviceModel.removeErrorPage(error,
//				serviceModel.getContextModel(httpContext));
//		if (model != null) {
//			LOG.debug("Unregister error page (error={})", error);
//			serverController.removeErrorPage(model);
//		}
//	}
//
//	/**
//	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
//	 */
//	@Override
//	public void registerWelcomeFiles(final String[] welcomeFiles,
//									 final boolean redirect, final HttpContext httpContext) {
//		ContextModel contextModel = serviceModel.getContextModel(httpContext);
//		if (LOG.isDebugEnabled()) {
//			LOG.debug("Register welcome files (welcomeFiles={}). Using context [{}]", Arrays.asList(welcomeFiles), contextModel);
//		}
//		//PAXWEB-123: try to use the setWelcomeFile method
//		final WelcomeFileModel model = new WelcomeFileModel(contextModel, welcomeFiles);
//
//		boolean serviceSuccess = false;
//		boolean controllerSuccess = false;
//		try {
//			serviceModel.addWelcomeFileModel(model);
//			serviceSuccess = true;
//			serverController.addWelcomFiles(model);
//			controllerSuccess = true;
//			if (model.getWelcomeFiles() != null && !isWebAppWebContainerContext(contextModel)) {
//				try {
//					serverController.getContext(contextModel).start();
//					// CHECKSTYLE:OFF
//				} catch (Exception e) {
//					LOG.error("Could not start the servlet context for context path ["
//							+ contextModel.getContextName() + "]", e);
//				} //CHECKSTYLE:ON
//			}
//		} finally {
//			// as this compensatory actions to work the remove methods should
//			// not throw exceptions.
//			if (!controllerSuccess) {
//				if (serviceSuccess) {
//					serviceModel.removeWelcomeFileModel(Arrays.toString(welcomeFiles), contextModel);
//				}
//			}
//		}
//	}
//
//	/**
//	 * @see WebContainer#unregisterWelcomeFiles(String[], HttpContext)
//	 */
//	@Override
//	public void unregisterWelcomeFiles(final String[] welcomeFiles, final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		NullArgumentException.validateNotNull(welcomeFiles, "WelcomeFiles");
//		final ContextModel contextModel = serviceModel
//				.getContextModel(httpContext);
//		//PAXWEB-123: try to use the setWelcomeFile method
//
//		final WelcomeFileModel model = serviceModel.removeWelcomeFileModel(Arrays.toString(welcomeFiles), contextModel);
//		if (model != null) {
//			if (LOG.isDebugEnabled()) {
//				LOG.debug("Unregister welcome files (welcomeFiles={})", Arrays.asList(welcomeFiles));
//			}
//			serverController.removeWelcomeFiles(model);
//		}
//		/*
//		if (contextModel == null
//				|| contextModel.getWelcomeFilesFilter() == null) {
//			throw new IllegalArgumentException(
//					"Welcome files are not registered for http context ["
//							+ httpContext + "]");
//		}
//		try {
//			unregisterFilter(contextModel.getWelcomeFilesFilter());
//		} finally {
//			contextModel.setWelcomeFilesFilter(null);
//		}
//		*/
//	}
//
//	@Override
//	public void registerLoginConfig(String authMethod, String realmName,
//									String formLoginPage, String formErrorPage, HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		String currentAuthMethod = contextModel.getAuthMethod();
//		String currentRealmName = contextModel.getRealmName();
//		String currentFormLoginPage = contextModel.getFormLoginPage();
//		String currentFormErrorPage = contextModel.getFormErrorPage();
//		List<String> currentConfiguration = Arrays.asList(currentAuthMethod, currentRealmName, currentFormLoginPage, currentFormErrorPage);
//		List<String> newConfiguration = Arrays.asList(authMethod, realmName, formLoginPage, formErrorPage);
//		if (!currentConfiguration.equals(newConfiguration)) {
//			if (!serviceModel.canBeConfigured(httpContext)) {
//				try {
//					LOG.debug("Stopping context model {} to register login configuration", contextModel);
//					serverController.getContext(contextModel).stop();
//				} catch (Exception e) {
//					LOG.error(e.getMessage(), e);
//				}
//			}
//			contextModel.setAuthMethod(authMethod);
//			contextModel.setRealmName(realmName);
//			contextModel.setFormLoginPage(formLoginPage);
//			contextModel.setFormErrorPage(formErrorPage);
//		}
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void unregisterLoginConfig(final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = serviceModel.getContextModel(httpContext);
//		if (contextModel == null || contextModel.getAuthMethod() == null || contextModel.getRealmName() == null) {
//			throw new IllegalArgumentException(
//					"Security Realm and authorization method are not registered for http context ["
//							+ httpContext + "]");
//		}
//
//		try {
//			LOG.debug("Stopping context model {} to unregister login configuration", contextModel);
//			serverController.getContext(contextModel).stop();
//		} catch (Exception e) {
//			LOG.error(e.getMessage(), e);
//		}
//
//		contextModel.setRealmName(null);
//		contextModel.setAuthMethod(null);
//		contextModel.setFormLoginPage(null);
//		contextModel.setFormErrorPage(null);
//	}
//
//	@Override
//	public void registerConstraintMapping(String constraintName, String mapping,
//										  String url, String dataConstraint, boolean authentication,
//										  List<String> roles, HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Register constraint mapping (name={}). Using context [{}]", constraintName, contextModel);
//		SecurityConstraintMappingModel secConstraintMapModel = new SecurityConstraintMappingModel(
//				contextModel, constraintName, mapping, url, dataConstraint,
//				authentication, roles);
//		serviceModel.addSecurityConstraintMappingModel(secConstraintMapModel);
//		serverController.addSecurityConstraintMapping(secConstraintMapModel);
//	}
//
//	@Override
//	public void unregisterConstraintMapping(final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = serviceModel.getContextModel(httpContext);
//
//		try {
//			LOG.debug("Stopping context model {} to unregister constraint mapping", contextModel);
//			serverController.getContext(contextModel).stop();
//		} catch (Exception e) {
//			LOG.error(e.getMessage(), e);
//		}
//
//		// Without changing WebContainer interface, we can't remove individual constraints...
//		SecurityConstraintMappingModel[] mappings = serviceModel.getSecurityConstraintMappings();
//		if (mappings != null) {
//			for (SecurityConstraintMappingModel model: mappings) {
//				if (model != null) {
//					serviceModel.removeSecurityConstraintMappingModel(model);
//					serverController.removeSecurityConstraintMapping(model);
//				}
//			}
//		}
//	}
//
//	@Override
//	public void registerServletContainerInitializer(
//			ServletContainerInitializer servletContainerInitializer,
//			Class<?>[] classes, final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//
//		Set<Class<?>> clazzes = new HashSet<>();
//		if (classes != null) {
//			Collections.addAll(clazzes, classes);
//		}
//		Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel
//				.getContainerInitializers();
//		Set<Class<?>> containerInitializersClasses = containerInitializers == null ? null
//				: containerInitializers.get(servletContainerInitializer);
//		if (!clazzes.equals(containerInitializersClasses)) {
//			if (!serviceModel.canBeConfigured(httpContext)) {
//				throw new IllegalStateException(
//						"Http context already used. ServletContainerInitializer can be set/changed only before first usage");
//			}
//			contextModel.addContainerInitializer(servletContainerInitializer,
//					clazzes);
//		}
//
//		serviceModel.addContextModel(contextModel);
//
//	}
//
//	@Override
//	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//		URL contextModelJettyWebXmlURL = contextModel.getJettyWebXmlURL();
//		if (!(contextModelJettyWebXmlURL == jettyWebXmlURL || contextModelJettyWebXmlURL != null
//				&& contextModelJettyWebXmlURL.equals(jettyWebXmlURL))) {
//			if (!serviceModel.canBeConfigured(httpContext)) {
//				throw new IllegalStateException(
//						"Http context already used. jetty-web.xml URL can be set/changed only before first usage");
//			}
//			contextModel.setJettyWebXmlUrl(jettyWebXmlURL);
//		}
//		serviceModel.addContextModel(contextModel);
//
//	}
//
//	@Override
//	public void unregisterServletContainerInitializer(HttpContext httpContext) {
//		//nothing to do
//	}
//
//	@Override
//	public void begin(HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Beginning and ssing context [" + contextModel + "]");
//		try {
//			serverController.getContext(contextModel);
//			contextModel.setWebBundle(true);
//			//CHECKSTYLE:OFF
//		} catch (RuntimeException e) {
//			LOG.error("Exception starting HttpContext registration", e);
//			throw e;
//		}
//		//CHECKSTYLE:ON
//	}
//
//	@Override
//	public void end(HttpContext httpContext) {
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//		try {
//			serverController.getContext(contextModel).start();
//			//CHECKSTYLE:OFF
//		} catch (Exception e) {
//			if (e instanceof RuntimeException) {
//				throw (RuntimeException) e;
//			}
//			LOG.error("Exception finalizing HttpContext registration", e);
//		}
//		//CHECKSTYLE:ON
//	}
//
//	@Override
//	public void setConnectorsAndVirtualHosts(List<String> connectors, List<String> virtualHosts,
//											 HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		if (!serviceModel.canBeConfigured(httpContext)) {
//			throw new IllegalStateException(
//					"Http context already used. Connectors and VirtualHosts can be set only before first usage");
//		}
//
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//		List<String> realVirtualHosts = new LinkedList<>(virtualHosts);
//		if (connectors.size() > 0) {
//			for (String connector : connectors) {
//				realVirtualHosts.add("@" + connector);
//			}
//		}
//		if (realVirtualHosts.size() == 0) {
//			realVirtualHosts = this.serverController.getConfiguration()
//					.server().getVirtualHosts();
//		}
//		if (LOG.isDebugEnabled()) {
//			StringBuilder sb = new StringBuilder("VirtualHostList=[");
//			for (String virtualHost : realVirtualHosts) {
//				sb.append(virtualHost).append(",");
//			}
//			sb.append("]");
//			LOG.debug(sb.toString());
//		}
//		contextModel.setVirtualHosts(realVirtualHosts);
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//
//		contextModel.addTagLibLocation(tagLibLocation);
//		contextModel.addTagLibUri(tagLibUri);
//
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void registerJspConfigPropertyGroup(List<String> includeCodes,
//											   List<String> includePreludes, List<String> urlPatterns, Boolean elIgnored, Boolean scriptingInvalid,
//											   Boolean isXml, HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http context");
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		LOG.debug("Using context [" + contextModel + "]");
//
//		contextModel.addJspIncludeCodes(includeCodes);
//		contextModel.addJspIncludePreludes(includePreludes);
//		contextModel.addJspUrlPatterns(urlPatterns);
//		contextModel.addJspElIgnored(elIgnored);
//		contextModel.addJspScriptingInvalid(scriptingInvalid);
//		contextModel.addJspIsXml(isXml);
//
//		serviceModel.addContextModel(contextModel);
//	}
//
//	@Override
//	public void registerWebSocket(final Object webSocket, final HttpContext httpContext) {
//		NullArgumentException.validateNotNull(httpContext, "Http Context");
//		NullArgumentException.validateNotNull(webSocket, "WebSocket");
//
//		ContextModel contextModel = getOrCreateContext(httpContext);
//
//		WebSocketModel model = new WebSocketModel(contextModel, webSocket);
//
//		boolean controllerSuccess = false;
//		boolean serviceSuccess = false;
//		try {
//			contextModel.addContainerInitializer(new ServletContainerInitializer() {
//
//				private Integer maxTry = 20;
//
//				@Override
//				public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
//					Callable<Boolean> task = () -> registerWebSocket(ctx, 1);
//
//					ExecutorService executor = Executors.newSingleThreadExecutor();
//					Future<Boolean> future = executor.submit(task);
//
//					try {
//						Boolean success = future.get(maxTry * 500 + 2000, TimeUnit.MILLISECONDS);
//						if (success) {
//							LOG.info("registered WebSocket");
//						} else {
//							LOG.error("Failed to create WebSocket, obviosly the endpoint couldn't be registered");
//						}
//					} catch (InterruptedException | ExecutionException | TimeoutException e) {
//						LOG.error("Failed to wait for registering of WebSocket", e);
//					}
//
//				}
//
//				private boolean registerWebSocket(ServletContext ctx, int registerTry) {
//					if (registerTry == maxTry) {
//						LOG.error("Tried to Register Websocket for {} times, will stop now", registerTry);
//						return false;
//					}
//					javax.websocket.server.ServerContainer serverContainer = (javax.websocket.server.ServerContainer) ctx.getAttribute(javax.websocket.server.ServerContainer.class.getName());
//					if (serverContainer != null) {
//						try {
//							serverContainer.addEndpoint(webSocket.getClass());
//							return true;
//						} catch (DeploymentException e) {
//							LOG.error("Failed to register WebSocket", e);
//							return false;
//						}
//					} else {
//						try {
//							LOG.debug("couldn't find ServerContainer, will try again in 500ms");
//							LOG.debug("this is the {} try", registerTry);
//							Thread.sleep(500);
//							return registerWebSocket(ctx, registerTry + 1);
//						} catch (InterruptedException e) {
//							LOG.error("Failed to register WebSocket due to: ", e);
//							return false;
//						}
//					}
//				}
//			}, null);
//			controllerSuccess = true;
//
//			serviceModel.addWebSocketModel(model);
//			serviceSuccess = true;
//		} finally {
//			// as this compensatory actions to work the remove methods should
//			// not throw exceptions.
//			if (!controllerSuccess) {
//				if (serviceSuccess) {
//					serviceModel.removeWebSocketModel(webSocket);
//				}
//			}
//		}
//
//		if (!isWebAppWebContainerContext(contextModel)) {
//			try {
//				serverController.getContext(contextModel).start();
//				// CHECKSTYLE:OFF
//			} catch (Exception e) {
//				LOG.error("Could not start the servlet context for context path ["
//						+ contextModel.getContextName() + "]", e);
//			} //CHECKSTYLE:ON
//		}
//
//	}
//
//	@Override
//	public void unregisterWebSocket(Object webSocket, HttpContext httpContext) {
//		// TODO Auto-generated method stub
//	}
//
////	@Override
////	public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator) {
////		return withWhiteboardDtoService(service -> service.calculateRequestInfoDTO(path, iterator, serverModel, serviceModel));
////	}
////
////	@Override
////	public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
////		return withWhiteboardDtoService(service -> service.createWhiteboardRuntimeDTO(iterator, serverModel, serviceModel));
////	}
//
//
//	/**
//	 * WhiteboardDtoService is registered as DS component. Should be removed if this class gets full DS support
//	 * @param function a function which is applied against WhiteboardDtoService
//	 * @param <T> Type of the functions return value
//	 * @return value provided by given function
//	 */
//	private <T> T withWhiteboardDtoService(Function<WhiteboardDtoService, T> function) {
//		final BundleContext bundleContext = serviceBundle.getBundleContext();
//		ServiceReference<WhiteboardDtoService> ref = bundleContext.getServiceReference(WhiteboardDtoService.class);
//		if (ref != null) {
//			WhiteboardDtoService service = bundleContext.getService(ref);
//			if (service != null) {
//				try {
//					return function.apply(service);
//				} finally {
//					bundleContext.ungetService(ref);
//				}
//			}
//		}
//		throw new IllegalStateException(String.format("Service '%s' could not be retrieved!", WhiteboardDtoService.class.getName()));
//	}
//
//	@Override
//	public String toString() {
//		return super.toString() + " for bundle " + serviceBundle;
//	}

//    @Override
//    public WebContainerDTO getWebcontainerDTO() {
//        WebContainerDTO dto = new WebContainerDTO();
//
//        dto.port = serverController.getHttpPort();
//        dto.securePort = serverController.getHttpSecurePort();
//        dto.listeningAddresses = serverController.getConfiguration().server().getListeningAddresses();
//
//        return dto;
//    }

}
