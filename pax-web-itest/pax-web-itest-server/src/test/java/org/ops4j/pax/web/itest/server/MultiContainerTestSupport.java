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
package org.ops4j.pax.web.itest.server;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;
import org.mockito.stubbing.Answer;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletContextHelperTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletContextHelperMappingTracker;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.getField;

public class MultiContainerTestSupport {

	public static final Logger LOG = LoggerFactory.getLogger(ServerControllerScopesTest.class);

	@Parameterized.Parameter
	public Runtime runtime;

	protected int port;

	protected ServerController controller;
	protected Configuration config;
	protected ServerModel serverModel;

	protected Bundle whiteboardBundle;
	protected BundleContext whiteboardBundleContext;

	protected HttpServiceEnabled container;
	protected ServiceReference<WebContainer> containerRef;

	protected ExtenderContext whiteboard;

	private ServiceTrackerCustomizer<ServletContextHelper, OsgiContextModel> servletContextHelperCustomizer;
	private ServiceTrackerCustomizer<ServletContextHelperMapping, OsgiContextModel> servletContextHelperMappingCustomizer;
	private ServiceTrackerCustomizer<HttpContext, OsgiContextModel> httpContextCustomizer;
	private ServiceTrackerCustomizer<HttpContextMapping, OsgiContextModel> httpContextMappingCustomizer;

	private ServiceTrackerCustomizer<Servlet, ServletModel> servletCustomizer;
	private ServiceTrackerCustomizer<Filter, FilterModel> filterCustomizer;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Runtime.JETTY },
				{ Runtime.TOMCAT },
				{ Runtime.UNDERTOW },
		});
	}

	public void configurePort() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);
		port = serverSocket.getLocalPort();
		serverSocket.close();
	}

	@Before
	@SuppressWarnings("unchecked")
	public void initAll() throws Exception {
		configurePort();

		controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		config = controller.getConfiguration();

		serverModel = new ServerModel(new Utils.SameThreadExecutor());
		serverModel.configureActiveServerController(controller);

		whiteboardBundle = mockBundle("org.ops4j.pax.web.pax-web-extender-whiteboard");
		whiteboardBundleContext = whiteboardBundle.getBundleContext();

		OsgiContextModel.DEFAULT_CONTEXT_MODEL.setOwnerBundle(whiteboardBundle);

		when(whiteboardBundleContext.createFilter(anyString()))
				.thenAnswer(invocation -> FrameworkUtil.createFilter(invocation.getArgument(0, String.class)));

		container = new HttpServiceEnabled(whiteboardBundle, controller, serverModel, null, config);

		containerRef = mock(ServiceReference.class);
		when(whiteboardBundleContext.getService(containerRef)).thenReturn(container);

		whiteboard = new ExtenderContext(null, whiteboardBundleContext);
		whiteboard.webContainerAdded(containerRef);

		servletContextHelperCustomizer = getCustomizer(ServletContextHelperTracker.createTracker(whiteboard, whiteboardBundleContext));
		servletContextHelperMappingCustomizer = getCustomizer(ServletContextHelperMappingTracker.createTracker(whiteboard, whiteboardBundleContext));
		httpContextCustomizer = getCustomizer(HttpContextTracker.createTracker(whiteboard, whiteboardBundleContext));
		httpContextMappingCustomizer = getCustomizer(HttpContextMappingTracker.createTracker(whiteboard, whiteboardBundleContext));

		servletCustomizer = getCustomizer(ServletTracker.createTracker(whiteboard, whiteboardBundleContext));
		filterCustomizer = getCustomizer(FilterTracker.createTracker(whiteboard, whiteboardBundleContext));
	}

	@After
	public void cleanup() throws Exception {
		if (controller != null) {
			controller.stop();
			controller = null;
		}
		stopWhiteboardService();
	}

	protected void stopWhiteboardService() {
		if (containerRef != null) {
			whiteboard.webContainerRemoved(containerRef);
			containerRef = null;
		}
		if (container != null) {
			container.stop();
		}
	}

	/**
	 * Helper method to create mock {@link Bundle} with associated mock {@link BundleContext}.
	 * @param symbolicName
	 * @return
	 */
	protected Bundle mockBundle(String symbolicName) {
		Bundle bundle = mock(Bundle.class);
		BundleContext bundleContext = mock(BundleContext.class);
		when(bundle.getSymbolicName()).thenReturn(symbolicName);
		when(bundle.toString()).thenReturn("Bundle \"" + symbolicName + "\"");
		when(bundle.getBundleContext()).thenReturn(bundleContext);
		when(bundleContext.getBundle()).thenReturn(bundle);

		return bundle;
	}

	/**
	 * Creates mock {@link ServiceReference} to represent OSGi-registered {@link ServletContextHelper} instance
	 * @param bundle
	 * @param name
	 * @param supplier
	 * @param serviceId
	 * @param rank
	 * @param contextPath
	 * @return
	 */
	protected ServiceReference<ServletContextHelper> mockServletContextHelperReference(Bundle bundle, String name,
			Supplier<ServletContextHelper> supplier, Long serviceId, Integer rank, String contextPath) {
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, name);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

		ServletContextHelper instance = supplier.get();

		ServiceReference<ServletContextHelper> schRef
				= mockReference(bundle, ServletContextHelper.class, props, null, serviceId, rank);
		when(bundle.getBundleContext().getService(schRef)).thenReturn(instance);
		when(whiteboardBundleContext.getService(schRef)).thenReturn(instance);

		return schRef;
	}

	/**
	 * Creates mock {@link ServiceReference} to represent OSGi-registered {@link Servlet} instance
	 * @param bundle
	 * @param name
	 * @param supplier
	 * @param serviceId
	 * @param rank
	 * @param patterns
	 * @return
	 */
	protected ServiceReference<Servlet> mockServletReference(Bundle bundle, String name,
			Supplier<Servlet> supplier, Long serviceId, Integer rank, String... patterns) {
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, name);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, patterns);

		ServiceReference<Servlet> servletRef = mockReference(bundle, Servlet.class, props, null, serviceId, rank);

		try {
			when(bundle.loadClass(Servlet.class.getName()))
					.thenAnswer((Answer<Class<?>>) invocation -> Servlet.class);
			if (supplier != null) {
				Servlet instance = supplier.get();
				when(bundle.loadClass(instance.getClass().getName()))
						.thenAnswer((Answer<Class<?>>) invocation -> instance.getClass());

				when(bundle.getBundleContext().getService(servletRef)).thenReturn(instance);
				when(whiteboardBundleContext.getService(servletRef)).thenReturn(instance);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return servletRef;
	}

	/**
	 * Creates mock {@link ServiceReference} to represent OSGi-registered {@link Filter} instance
	 * @param bundle
	 * @param name
	 * @param supplier
	 * @param serviceId
	 * @param rank
	 * @param patterns
	 * @return
	 */
	protected ServiceReference<Filter> mockFilterReference(Bundle bundle, String name,
			Supplier<Filter> supplier, Long serviceId, Integer rank, String... patterns) {
		Hashtable<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, name);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, patterns);

		Filter instance = supplier.get();
		try {
			when(bundle.loadClass(instance.getClass().getName()))
					.thenAnswer((Answer<Class<?>>) invocation -> instance.getClass());
			when(bundle.loadClass(Filter.class.getName()))
					.thenAnswer((Answer<Class<?>>) invocation -> Filter.class);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		ServiceReference<Filter> filterRef = mockReference(bundle, Filter.class, props, null, serviceId, rank);
		when(bundle.getBundleContext().getService(filterRef)).thenReturn(instance);
		when(whiteboardBundleContext.getService(filterRef)).thenReturn(instance);

		return filterRef;
	}

	protected <S> ServiceReference<S> mockReference(Bundle bundle, Class<S> clazz, Hashtable<String, Object> props,
			Supplier<S> supplier) {
		return mockReference(bundle, clazz, props, supplier, 0L, 0);
	}

	@SuppressWarnings("unchecked")
	protected <S> ServiceReference<S> mockReference(Bundle bundle, Class<S> clazz, Hashtable<String, Object> props,
			Supplier<S> supplier, Long serviceId, Integer rank) {
		ServiceReference<S> ref = mock(ServiceReference.class);
		when(ref.toString()).thenReturn("ref:" + clazz.toString());

		when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { clazz.getName() });

		when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(serviceId);
		when(ref.getProperty(Constants.SERVICE_RANKING)).thenReturn(rank);

		when(ref.getBundle()).thenReturn(bundle);

		if (props != null) {
			props.forEach((k, v) -> when(ref.getProperty(k)).thenReturn(v));
			when(ref.getPropertyKeys()).thenReturn(props.keySet().toArray(new String[0]));
			when(ref.getProperties()).thenReturn(props);
		}
		boolean singleton = true;
		if (supplier != null) {
			S instance = supplier.get();
			if (instance != supplier.get()) {
				singleton = false;
			}
			when(bundle.getBundleContext().getService(ref)).thenReturn(instance);
			when(whiteboardBundleContext.getService(ref)).thenReturn(instance);
		}
		when(ref.getProperty(Constants.SERVICE_SCOPE))
				.thenReturn(singleton ? Constants.SCOPE_SINGLETON : Constants.SCOPE_BUNDLE);

		return ref;
	}

	public ServiceTrackerCustomizer<ServletContextHelper, OsgiContextModel> getServletContextHelperCustomizer() {
		return servletContextHelperCustomizer;
	}

	public ServiceTrackerCustomizer<ServletContextHelperMapping, OsgiContextModel> getServletContextHelperMappingCustomizer() {
		return servletContextHelperMappingCustomizer;
	}

	public ServiceTrackerCustomizer<HttpContext, OsgiContextModel> getHttpContextCustomizer() {
		return httpContextCustomizer;
	}

	public ServiceTrackerCustomizer<HttpContextMapping, OsgiContextModel> getHttpContextMappingCustomizer() {
		return httpContextMappingCustomizer;
	}

	public ServiceTrackerCustomizer<Servlet, ServletModel> getServletCustomizer() {
		return servletCustomizer;
	}

	public ServiceTrackerCustomizer<Filter, FilterModel> getFilterCustomizer() {
		return filterCustomizer;
	}

	@SuppressWarnings("unchecked")
	protected <S, T> ServiceTrackerCustomizer<S, T> getCustomizer(ServiceTracker<S, T> tracker) {
		tracker.open();
		try {
			Field f = ServiceTracker.class.getDeclaredField("customizer");
			f.setAccessible(true);
			return (ServiceTrackerCustomizer<S, T>) f.get(tracker);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected ServerModelInternals serverModelInternals(ServerModel serverModel) {
		return new ServerModelInternals(serverModel);
	}

	protected ServiceModelInternals serviceModelInternals(WebContainer webContainer) {
		return new ServiceModelInternals(getField(webContainer, "serviceModel", ServiceModel.class));
	}

	/**
	 * Class to verify {@link ServerModel} after performing a test
	 */
	public static class ServerModelInternals {

		public final Map<String, ServletContextModel> servletContexts = new HashMap<>();
		public final Map<ServerModel.ContextKey, TreeSet<OsgiContextModel>> bundleContexts = new HashMap<>();
		public final Map<String, TreeSet<OsgiContextModel>> sharedContexts = new HashMap<>();
		public final Map<String, OsgiContextModel> whiteboardContexts = new HashMap<>();
		public final Map<Servlet, ServletModel> servlets = new IdentityHashMap<>();
		public final Set<ServletModel> disabledServletModels = new TreeSet<>();
		public final Map<Filter, FilterModel> filters = new IdentityHashMap<>();
		public final Set<FilterModel> disabledFilterModels = new TreeSet<>();

		private final ServerModel model;
//		private final Map<String, VirtualHostModel> virtualHosts = new HashMap<>();
//		private final VirtualHostModel defaultHost = new VirtualHostModel();

		@SuppressWarnings("unchecked")
		public ServerModelInternals(ServerModel model) {
			this.model = model;
			servletContexts.putAll(getField(model, "servletContexts", Map.class));
			bundleContexts.putAll(getField(model, "bundleContexts", Map.class));
			sharedContexts.putAll(getField(model, "sharedContexts", Map.class));
			whiteboardContexts.putAll(getField(model, "whiteboardContexts", Map.class));
			servlets.putAll(getField(model, "servlets", Map.class));
			disabledServletModels.addAll(getField(model, "disabledServletModels", Set.class));
			filters.putAll(getField(model, "filters", Map.class));
			disabledFilterModels.addAll(getField(model, "disabledFilterModels", Set.class));
		}

		/**
		 * Checks whether the {@link ServerModel} is not tracking anything related to given {@link Bundle}
		 * @param bundle
		 * @return
		 */
		public boolean isClean(Bundle bundle) {
			// there can be the default, bundle-scoped context for pax-web-extender-whiteboard bundle
			boolean clean = bundleContexts.keySet().stream()
					.filter(ck -> !(ck.bundle.equals(bundle) && "default".equals(ck.contextId)))
					.noneMatch(ck -> ck.bundle.equals(bundle));
			// there can be the default Whiteboard ServletContextModel (customized into OsgiContextModel)
			clean &= whiteboardContexts.values().stream()
					.filter(ocm -> ocm != OsgiContextModel.DEFAULT_CONTEXT_MODEL)
					.noneMatch(ocm -> ocm.getOwnerBundle().equals(bundle));
			clean &= servlets.values().stream().noneMatch(sm -> sm.getRegisteringBundle().equals(bundle));
			clean &= filters.values().stream().noneMatch(fm -> fm.getRegisteringBundle().equals(bundle));
			clean &= disabledServletModels.stream().noneMatch(sm -> sm.getRegisteringBundle().equals(bundle));
			clean &= disabledFilterModels.stream().noneMatch(fm -> fm.getRegisteringBundle().equals(bundle));
			return clean;
		}
	}

	/**
	 * Class to verify {@link ServiceModel} after performing a test
	 */
	public static class ServiceModelInternals {

		public final Map<String, Map<String, ServletModel>> aliasMapping = new HashMap<>();
		public final Set<ServletModel> servletModels = new HashSet<>();
		public final Set<FilterModel> filterModels = new HashSet<>();

		private final ServiceModel model;
//		private final Map<EventListener, EventListenerModel> eventListenerModels = new HashMap<>();

		@SuppressWarnings("unchecked")
		public ServiceModelInternals(ServiceModel model) {
			this.model = model;
			aliasMapping.putAll(getField(model, "aliasMapping", Map.class));
			servletModels.addAll(getField(model, "servletModels", Set.class));
			filterModels.addAll(getField(model, "filterModels", Set.class));
		}

		public boolean isEmpty() {
			return aliasMapping.isEmpty() && servletModels.isEmpty() && filterModels.isEmpty();
		}
	}

}
