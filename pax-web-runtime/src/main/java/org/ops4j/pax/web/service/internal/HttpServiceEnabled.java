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

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.ops4j.pax.web.annotations.PaxWebConfiguration;
import org.ops4j.pax.web.annotations.PaxWebTesting;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.views.DirectWebContainerView;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.context.UniqueMultiBundleWebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.context.UniqueWebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
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
public class HttpServiceEnabled implements StoppableHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceEnabled.class);
//	private static final String PAX_WEB_JSP_SERVLET = "jsp";

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

	private final WhiteboardWebContainerView whiteboardContainerView = new WhiteboardWebContainer();
	private final DirectWebContainerView directContainerView = new DirectWebContainer();

//	private final Boolean showStacks;

//	private final Object lock = new Object();

	@Review("Should ServerListener be registered here?")
	public HttpServiceEnabled(final Bundle bundle, final ServerController srvController,
			final ServerModel serverModel, final ServletListener eventDispatcher, final Configuration configuration) {
		LOG.debug("Creating active Http Service for: {}", bundle);

		this.serverModel = serverModel;
		this.serviceModel = new ServiceModel(serverModel, srvController, bundle);
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

	@PaxWebTesting
	public ServiceModel getServiceModel() {
		return serviceModel;
	}

	// --- StoppableHttpService

	@Override
	@Review("Definitely good place to clean up things, but take care of shared contexts")
	public void stop() {
		LOG.debug("Stopping http service for: " + serviceBundle);

		// TODO: cleanup all the OsgiContextModels in ServerModel associated with serviceBundle of this service

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
		if (type == WhiteboardWebContainerView.class) {
			// give access to ServerModel and other bundle-agnostic internals of the runtime
			return type.cast(whiteboardContainerView);
		}
		if (type == DirectWebContainerView.class) {
			// direct HttpService like registration of models - mostly for test purpose, as DirectWebContainerView
			// is not in exported package
			return type.cast(directContainerView);
		}
		return null;
	}

	// --- transactional access to web container

	@Override
	public void begin(HttpContext context) {
		// marks given context as participating in a transaction
	}

	@Override
	public void end(HttpContext context) {
		// ends a transaction and deassociates given context from a pending transaction
	}

	// --- different methods used to retrieve HttpContext
	//     "102.10.3.1 public HttpContext createDefaultHttpContext()" says that "a new HttpContext object is created
	//     each time this method is called", but we actually don't want "default" context to mean something
	//     different each time it's "created".
	//     That's why the "contexts" returned from the below methods are unique instances, but underneath they
	//     delegate to something with "the same" concept precisely defined by Pax Web (Http Service specification
	//     doesn't make the concept of "the same" precise).

	@Override
	public WebContainerContext createDefaultHttpContext() {
		OsgiContextModel context = serverModel.getBundleContextModel(PaxWebConstants.DEFAULT_CONTEXT_NAME, serviceBundle);
		// no way it can be null. no way it contains indirect reference to HttpContext
		return new UniqueWebContainerContextWrapper(context.resolveHttpContext(serviceBundle));
	}

	@Override
	public WebContainerContext createDefaultHttpContext(String contextId) {
		OsgiContextModel context = serverModel.getBundleContextModel(contextId, serviceBundle);
		if (context == null) {
			// create one in batch through ServiceModel and ensure its stored at ServerModel as well
			context = serviceModel.createDefaultHttpContext(contextId);
		}
		return new UniqueWebContainerContextWrapper(context.resolveHttpContext(serviceBundle));
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext() {
		return createDefaultSharedHttpContext(PaxWebConstants.DEFAULT_SHARED_CONTEXT_NAME);
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext(String contextId) {
		OsgiContextModel context = serverModel.getSharedContextModel(contextId);
		if (context == null) {
			// create one in batch through ServerModel, as shared contexts are not associated with any "owner" bundle
			context = serverModel.createDefaultSharedtHttpContext(contextId);
		}
		MultiBundleWebContainerContext sharedContext = (MultiBundleWebContainerContext) context.resolveHttpContext(serviceBundle);
		return new UniqueMultiBundleWebContainerContextWrapper(sharedContext);
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

		doRegisterServlet(Collections.singletonList(httpContext), servletModel);
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
			doRegisterServlet(Collections.singletonList(httpContext), model);
		} catch (NamespaceException ignored) {
		}
	}

	/**
	 * <p>Main, internal (but public for testing purpose) method to register given, fully defined {@link ServletModel}
	 * within an {@link OsgiContextModel} associated with given list of {@link HttpContext}.</p>
	 *
	 * <p>Method checks if the association is possible or creates one if there no {@link OsgiContextModel}
	 * available yet.</p>
	 *
	 * <p>Method should run semi transactionally - in single configuration/registration thread of Pax Web runtime.</p>
	 *
	 * @param httpContexts
	 * @param model
	 * @throws ServletException
	 * @throws NamespaceException
	 */
	private void doRegisterServlet(Collection<HttpContext> httpContexts, ServletModel model) throws ServletException, NamespaceException {
		LOG.debug("Passing registration of {} to configuration thread", model);

		if (model.getRegisteringBundle() == null) {
			// HttpService case. In Whiteboard, bundle is always provided with the model up front
			model.setRegisteringBundle(this.serviceBundle);
		}

		try {
			servletEvent(ServletEvent.State.DEPLOYING, model.getRegisteringBundle(), model);

			model.performValidation();

			final Batch batch = new Batch("Registration of " + model);

			serverModel.run(() -> {
				translateContexts(httpContexts, model, batch);

				LOG.info("Registering {}", model);

				// adding servlet model may lead to unregistration of some other, lower-ranked models, so batch
				// may have some unregistration changes added
				serverModel.addServletModel(model, batch);

				// only if validation was fine, pass the batch to ServerController, where the batch may fail again
				serverController.sendBatch(batch);

				// if server runtime has accepted the changes (hoping it'll be in clean state if it didn't), lets
				// actually apply the changes to global model (through ServiceModel)
				batch.accept(serviceModel);

				return null;
			});

			servletEvent(ServletEvent.State.DEPLOYED, model.getRegisteringBundle(), model);
		} catch (ServletException | NamespaceException e) {
			servletEvent(ServletEvent.State.FAILED, model.getRegisteringBundle(), model);
			throw e;
		} catch (Exception e) {
			servletEvent(ServletEvent.State.FAILED, model.getRegisteringBundle(), model);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- methods used to unregister a Servlet

	@Override
	public void unregister(final String alias) {
		ServletModel model = new ServletModel(alias, null, null, null, (ServiceReference<? extends Servlet>)null);
		doUnregisterServlet(model);
	}

	@Override
	public void unregisterServlet(final Servlet servlet) {
		ServletModel model = new ServletModel(null, null, servlet, null, null);
		doUnregisterServlet(model);
	}

	@Override
	public void unregisterServlet(String servletName) {
		ServletModel model = new ServletModel(null, servletName, null, null, null);
		doUnregisterServlet(model);
	}

	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		ServletModel model = new ServletModel(null, null, null, servletClass, null);
		doUnregisterServlet(model);
	}

	/**
	 * Actual servlet unregistration methods - for all supported criteria
	 * @param model
	 */
	private void doUnregisterServlet(ServletModel model) {
		final String alias = model.getAlias();
		final String name = model.getName();
		final Servlet instance = model.getServlet();
		final Class<? extends Servlet> servletClass = model.getServletClass();
		final ServiceReference<? extends Servlet> reference = model.getElementReference();

		final Bundle registeringBundle = model.getRegisteringBundle() == null ?
				serviceBundle : model.getRegisteringBundle();

		try {
			servletEvent(ServletEvent.State.UNDEPLOYING, registeringBundle, model);

			serverModel.run(() -> {
				List<ServletModel> toUnregister = new LinkedList<>();

				if (alias != null) {
					LOG.info("Unregistering servlet by alias \"{}\"", alias);

					ServletModel found = null;
					for (ServletModel existing : serviceModel.getServletModels()) {
						if (existing.getAlias().equals(alias)) {
							found = existing;
							break;
						}
					}
					Map<String, ServletModel> mapping = serviceModel.getAliasMapping().get(alias);
					if (mapping == null || mapping.size() == 0 || found == null) {
						throw new IllegalArgumentException("Alias \"" + alias + "\" was never registered by "
								+ registeringBundle);
					}
					toUnregister.add(found);
				} else if (servletClass != null) {
					LOG.info("Unregistering servlet by class \"{}\"", servletClass);

					for (ServletModel existing : serviceModel.getServletModels()) {
						if (existing.getServletClass().equals(servletClass)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Servlet of \"" + servletClass.getName() + "\" class "
								+ "was never registered by " + registeringBundle);
					}
				} else if (instance != null) {
					LOG.info("Unregistering servlet \"{}\"", instance);

					for (ServletModel existing : serviceModel.getServletModels()) {
						if (existing.getServlet().equals(instance)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Servlet \"" + instance + "\" "
								+ "was never registered by " + registeringBundle);
					}
				} else if (reference != null) {
					LOG.info("Unregistering servlet by refernce \"{}\"", reference);

					for (ServletModel existing : serviceModel.getServletModels()) {
						if (existing.getElementReference().equals(reference)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Servlet with reference \"" + reference + "\" "
								+ "was never registered by " + registeringBundle);
					}
				} else if (name != null) {
					LOG.info("Unregistering servlet by name \"{}\"", name);

					for (ServletModel existing : serviceModel.getServletModels()) {
						if (existing.getName().equals(name)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Servlet named \"" + name + "\" was never registered by "
								+ registeringBundle);
					}
				} else {
					throw new IllegalArgumentException("No criteria for servlet unregistration specified");
				}

				final Batch batch = new Batch("Unregistration of servlets: " + toUnregister);

				// removing servlet model may lead to reactivation of some previously disabled models
				serverModel.removeServletModels(toUnregister, batch);

				// only if validation was fine, pass the batch to ServerController, where the batch may fail again
				serverController.sendBatch(batch);

				// if server runtime has accepted the changes (hoping it'll be in clean state if it didn't), lets
				// actually apply the changes to global model (through ServiceModel)
				batch.accept(serviceModel);

				return null;
			});

			servletEvent(ServletEvent.State.UNDEPLOYED, registeringBundle, model);
		} catch (Exception e) {
			servletEvent(ServletEvent.State.FAILED, registeringBundle, model);
			throw new RuntimeException(e.getMessage(), e);
		}
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

		doRegisterFilter(Collections.singletonList(httpContext), filterModel);
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

		doRegisterFilter(Collections.singletonList(httpContext), filterModel);
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
	 * @param httpContexts
	 * @param model
	 * @throws ServletException
	 */
	private void doRegisterFilter(Collection<HttpContext> httpContexts, final FilterModel model) throws ServletException {
		LOG.debug("Passing registration of {} to configuration thread", model);

		if (model.getRegisteringBundle() == null) {
			// HttpService case. In Whiteboard, bundle is always provided with the model up front
			model.setRegisteringBundle(this.serviceBundle);
		}

		try {
			model.performValidation();

			final Batch batch = new Batch("Registration of " + model);

			serverModel.run(() -> {
				translateContexts(httpContexts, model, batch);

				LOG.info("Registering {}", model);

				// batch change of entire model
				serverModel.addFilterModel(model, batch);

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
		FilterModel model = new FilterModel(null, filter, null, null);
		doUnregisterFilter(model);
	}

	@Override
	public void unregisterFilter(String filterName) {
		FilterModel model = new FilterModel(filterName, null, null, null);
		doUnregisterFilter(model);
	}

	@Override
	public void unregisterFilters(Class<? extends Filter> filterClass) {
		FilterModel model = new FilterModel(null, null, filterClass, null);
		doUnregisterFilter(model);
	}

	/**
	 * Main method for {@link Filter} unregistration
	 * @param model
	 */
	private void doUnregisterFilter(FilterModel model) {
		final String name = model.getName();
		final Filter instance = model.getFilter();
		final Class<? extends Filter> filterClass = model.getFilterClass();

		final Bundle registeringBundle = model.getRegisteringBundle() == null ?
				serviceBundle : model.getRegisteringBundle();

		try {
			serverModel.run(() -> {
				List<FilterModel> toUnregister = new LinkedList<>();

				if (name != null) {
					LOG.info("Unregistering filter by name \"{}\"", name);

					for (FilterModel existing : serviceModel.getFilterModels()) {
						if (existing.getName().equals(name)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Filter named \"" + name + "\" was never registered by "
								+ registeringBundle);
					}
				} else if (filterClass != null) {
					LOG.info("Unregistering filter by class \"{}\"", filterClass);

					for (FilterModel existing : serviceModel.getFilterModels()) {
						if (existing.getFilterClass().equals(filterClass)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Servlet of \"" + filterClass.getName() + "\" class "
								+ "was never registered by " + registeringBundle);
					}
				} else if (instance != null) {
					LOG.info("Unregistering filter \"{}\"", instance);

					for (FilterModel existing : serviceModel.getFilterModels()) {
						if (existing.getFilter().equals(instance)) {
							toUnregister.add(existing);
						}
					}
					if (toUnregister.size() == 0) {
						throw new IllegalArgumentException("Filter \"" + instance + "\" "
								+ "was never registered by " + registeringBundle);
					}
				} else {
					throw new IllegalArgumentException("No criteria for filter unregistration specified");
				}

				final Batch batch = new Batch("Unregistration of filters: " + toUnregister);

				serverModel.removeFilterModels(toUnregister, batch);

				serverController.sendBatch(batch);

				batch.accept(serviceModel);

				return null;
			});
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- methods used to register an EventListener

	@Override
	public void registerEventListener(final EventListener listener, final HttpContext httpContext) {
		doRegisterEventListener(Collections.singletonList(httpContext), new EventListenerModel(listener));
	}

	private void doRegisterEventListener(Collection<HttpContext> httpContexts, EventListenerModel model) {
		LOG.debug("Passing registration of {} to configuration thread", model);

		if (model.getRegisteringBundle() == null) {
			model.setRegisteringBundle(this.serviceBundle);
		}

		final Batch batch = new Batch("Registration of " + model);

		try {
			model.performValidation();

			serverModel.run(() -> {
				translateContexts(httpContexts, model, batch);

				LOG.info("Registering {}", model);

				serverModel.addEventListenerModel(model, batch);

				serverController.sendBatch(batch);

				batch.accept(serviceModel);

				return null;
			});
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- methods used to unregister an EventListener

	@Override
	public void unregisterEventListener(final EventListener listener) {
		try {
			serverModel.run(() -> {
				final Batch batch = new Batch("Unregistration of EventListener: " + listener);

//				serverModel.removeEventListenerModel(listener, batch);

				serverController.sendBatch(batch);

				batch.accept(serviceModel);

				return null;
			});
		} catch (ServletException | NamespaceException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- private support methods

	private void servletEvent(ServletEvent.State type, Bundle bundle, ServletModel model) {
		if (eventDispatcher != null) {
			eventDispatcher.servletEvent(new ServletEvent(type, bundle, model));
		}
	}

	/**
	 * <p>In Http Service scenario, user passes an instance of {@link HttpContext} with the registration. Internally,
	 * each such context has to be translated into {@link OsgiContextModel} - the same model as it's tracked by
	 * Whiteboard Service. This method converts passed collection of {@link HttpContext} contexts and sets them
	 * in passed {@link ElementModel}.</p>
	 *
	 * <p>In Whiteboard Service scenario there's no such translation - {@link OsgiContextModel} instances are
	 * passed already in the model.</p>
	 *
	 * <p>This method creates {@link OsgiContextModel} instances - but only as batch operations to be invoked later.</p>
	 *
	 * @param httpContexts
	 * @param model
	 * @param batch
	 */
	@PaxWebConfiguration
	private void translateContexts(Collection<HttpContext> httpContexts, ElementModel<?> model, Batch batch) {
		if (httpContexts.size() > 0 && !model.hasContextModels()) {
			// Http Service scenario - HttpContext(s)/WebContainerContext(s) are passed with the registration
			final Collection<WebContainerContext> webContexts
					= httpContexts.stream().map(this::unify).collect(Collectors.toList());
			webContexts.forEach(wc -> {
				// HttpService scenario, so only "/" context path
				OsgiContextModel contextModel = serverModel.getOrCreateOsgiContextModel(wc, serviceBundle,
						PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);
				model.addContextModel(contextModel);
			});
		} else if (model.hasContextModels()) {
			// DON'T register OsgiContextModels carried with Whiteboard-registered WebElement. These should
			// be registered explicitly, before servlet is registered
			// keep the below code commented.
//			// Whiteboard Service scenario - OsgiContextModel(s) are passed together with the model, but we
//			// have to ensure they're registered in ServerModel and ServerController
//			model.getContextModels().forEach(ocm -> {
//				serverModel.registerOsgiContextModelIfNeeded(ocm, batch);
//			});
		}
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
	 * Private <em>view class</em> for Whiteboard registration of web elements.
	 */
	private class WhiteboardWebContainer implements WhiteboardWebContainerView {

		@Override
		public void registerServlet(ServletModel model) {
			try {
				doRegisterServlet(Collections.emptyList(), model);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		@Override
		public void unregisterServlet(ServletModel servletModel) {
			doUnregisterServlet(servletModel);
		}

		@Override
		public void registerFilter(FilterModel model) {
			try {
				doRegisterFilter(Collections.emptyList(), model);
			} catch (ServletException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		@Override
		public void unregisterFilter(FilterModel filterModel) {
			doUnregisterFilter(filterModel);
		}

		@Override
		public List<OsgiContextModel> getOsgiContextModels(Bundle bundle) {
			return serverModel.getOsgiContextModels(bundle);
		}

		@Override
		public void addWhiteboardOsgiContextModel(OsgiContextModel model) {
			serverModel.runSilently(() -> {
				Batch batch = new Batch("Registration of " + model);
				serverModel.registerOsgiContextModelIfNeeded(model, batch);
				serverController.sendBatch(batch);
				batch.accept(serviceModel);
				return null;
			});
		}

		@Override
		public void removeWhiteboardOsgiContextModel(OsgiContextModel model) {
			serverModel.runSilently(() -> {
				Batch batch = new Batch("Unregistration of " + model);
				serverModel.unregisterOsgiContextModel(model, batch);
				serverController.sendBatch(batch);
				batch.accept(serviceModel);
				return null;
			});
		}
	}

	private class DirectWebContainer implements DirectWebContainerView {

		@Override
		public void registerServlet(Collection<HttpContext> contexts, ServletModel model) throws ServletException, NamespaceException {
			doRegisterServlet(contexts, model);
		}

		@Override
		public void registerFilter(Collection<HttpContext> contexts, FilterModel model) throws ServletException {
			doRegisterFilter(contexts, model);
		}

		@Override
		public void unregisterServlet(ServletModel model) {
			doUnregisterServlet(model);
		}

		@Override
		public void unregisterFilter(FilterModel model) {
			doUnregisterFilter(model);
		}
	}























//	private boolean isWebAppWebContainerContext(ContextModel contextModel) {
//		return contextModel
//				.getHttpContext()
//				.getClass()
//				.getName()
//				.equals("org.ops4j.pax.web.extender.war.internal.WebAppWebContainerContext");
//	}

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
//		if (!SupportUtils.isJSPAvailable()) {
//			throw new UnsupportedOperationException(
//					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
//		}
//		final Servlet jspServlet = new JspServletWrapper(serviceBundle, jspFile);
//		final ContextModel contextModel = getOrCreateContext(httpContext);
//		//CHECKSTYLE:OFF
//		initParams = createInitParams(contextModel, initParams == null ? new Hashtable<>() : initParams);
//		//CHECKSTYLE:ON
//		serviceModel.addContextModel(contextModel);
//		try {
//			registerServlet(jspServlet, getJspServletName(jspFile),
//					urlPatterns == null ? new String[]{"*.jsp"}
//							: urlPatterns, initParams, httpContext);
//		} catch (ServletException ignore) {
//			// this should never happen
//			LOG.error("Internal error. Please report.", ignore);
//		}
//		Map<Servlet, String[]> jspServlets = contextModel.getJspServlets();
//		jspServlets.put(jspServlet, urlPatterns);
//
//	}

	private String getJspServletName(String jspFile) {
		return null;
//		return jspFile == null ? PAX_WEB_JSP_SERVLET : null;
	}

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
//		final OsgiContextModel contextModel = serviceModel
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

//	@Override
//	public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator) {
//		return withWhiteboardDtoService(service -> service.calculateRequestInfoDTO(path, iterator, serverModel, serviceModel));
//	}
//
//	@Override
//	public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
//		return withWhiteboardDtoService(service -> service.createWhiteboardRuntimeDTO(iterator, serverModel, serviceModel));
//	}


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

	@Override
	public String toString() {
		return super.toString() + " for bundle " + serviceBundle;
	}

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
