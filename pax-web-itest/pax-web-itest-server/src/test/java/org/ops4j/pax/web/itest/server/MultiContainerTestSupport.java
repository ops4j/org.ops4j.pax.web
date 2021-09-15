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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.felix.utils.extender.Extension;
import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.websocket.server.WsSci;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.ops4j.pax.web.extender.war.internal.WarExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.FilterTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.HttpContextTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ListenerTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ResourceTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletContextHelperTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.ServletTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ErrorPageMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.FilterMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.HttpContextMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.JspMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ListenerMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ResourceMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletContextHelperMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.ServletMappingTracker;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy.WelcomeFileMappingTracker;
import org.ops4j.pax.web.itest.server.controller.ServerControllerScopesTest;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerState;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.ContextKey;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.JspModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.ops4j.pax.web.service.undertow.PaxWebUndertowExtension;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.ops4j.pax.web.service.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.whiteboard.ListenerMapping;
import org.ops4j.pax.web.service.whiteboard.ResourceMapping;
import org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.ops4j.pax.web.service.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.websocket.internal.PaxWebWebSocketsServletContainerInitializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import static org.mockito.ArgumentMatchers.any;
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
	protected Bundle warExtenderBundle;
	protected BundleContext warExtenderBundleContext;
	protected Bundle jspBundle;

	protected Bundle wsGenericBundle;
	protected Bundle wsJettyBundle;
	protected Bundle wsTomcatBundle;
	protected Bundle wsUndertowBundle;

	protected Map<Bundle, HttpServiceEnabled> containers = new HashMap<>();
	protected ServiceReference<WebContainer> containerRef;

	protected WhiteboardExtenderContext whiteboardExtender;

	protected WarExtenderContext warExtender;
	protected ExecutorService warExtenderPool;
	protected Map<Bundle, Extension> wabs = new HashMap<>();

	private ServiceTrackerCustomizer<ServletContextHelper, OsgiContextModel> servletContextHelperCustomizer;
	private ServiceTrackerCustomizer<ServletContextHelperMapping, OsgiContextModel> servletContextHelperMappingCustomizer;
	private ServiceTrackerCustomizer<HttpContext, OsgiContextModel> httpContextCustomizer;
	private ServiceTrackerCustomizer<HttpContextMapping, OsgiContextModel> httpContextMappingCustomizer;

	// --- non "mapping" customizers

	private ServiceTrackerCustomizer<Servlet, ServletModel> servletCustomizer;
	private ServiceTrackerCustomizer<Filter, FilterModel> filterCustomizer;
	private ServiceTrackerCustomizer<Object, ServletModel> resourceCustomizer;
	private ServiceTrackerCustomizer<EventListener, EventListenerModel> listenerCustomizer;

	// --- "mapping" customizers

	private ServiceTrackerCustomizer<ServletMapping, ServletModel> servletMappingCustomizer;
	private ServiceTrackerCustomizer<FilterMapping, FilterModel> filterMappingCustomizer;
	private ServiceTrackerCustomizer<ResourceMapping, ServletModel> resourceMappingCustomizer;
	private ServiceTrackerCustomizer<WelcomeFileMapping, WelcomeFileModel> welcomeFileMappingCustomizer;
	private ServiceTrackerCustomizer<ErrorPageMapping, ErrorPageModel> errorPageMappingCustomizer;
	private ServiceTrackerCustomizer<ListenerMapping, EventListenerModel> listenerMappingCustomizer;
	private ServiceTrackerCustomizer<JspMapping, JspModel> jspMappingCustomizer;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ Runtime.JETTY },
				{ Runtime.TOMCAT },
				{ Runtime.UNDERTOW },
		});
	}

	@BeforeClass
	public static void initURLHandlersAndLogging() {
		String pkgs = System.getProperty("java.protocol.handler.pkgs");
		if (pkgs == null) {
			pkgs = "org.ops4j.pax.web.itest.server.support.protocols";
		} else {
			pkgs += "|org.ops4j.pax.web.itest.server.support.protocols";
		}
		System.setProperty("java.protocol.handler.pkgs", pkgs);

		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	@AfterClass
	public static void cleanupLogging() {
		SLF4JBridgeHandler.uninstall();
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

		controller = Utils.createServerController(props -> {
			String location = sessionPersistenceLocation();
			if (location != null) {
				props.put(PaxWebConfig.PID_CFG_SESSION_STORE_DIRECTORY, location);
			}
			props.put(PaxWebConfig.PID_CFG_SHOW_STACKS, "true");
		}, port, runtime, getClass().getClassLoader());

		if (enableJSP()) {
			jspBundle = mockBundle("org.ops4j.pax.web.pax-web-jsp", null, false);
			when(jspBundle.getBundleId()).thenReturn(101L);
			when(jspBundle.getState()).thenReturn(Bundle.ACTIVE);
			when(jspBundle.getEntry("/")).thenReturn(new URL("bundle://101.0:0/"));
			when(jspBundle.getResources("META-INF/services/javax.el.ExpressionFactory"))
					.thenReturn(Collections.enumeration(Collections.singletonList(
							org.ops4j.pax.web.jsp.JspServlet.class.getResource("/META-INF/services/javax.el.ExpressionFactory")))
					);
			when(jspBundle.getResources("META-INF/services/javax.servlet.ServletContainerInitializer"))
					.thenReturn(Collections.enumeration(Collections.singletonList(
							org.ops4j.pax.web.jsp.JspServlet.class.getResource("/META-INF/services/javax.servlet.ServletContainerInitializer")))
					);
			when(jspBundle.loadClass(anyString()))
					.thenAnswer(i -> JspServlet.class.getClassLoader().loadClass(i.getArgument(0, String.class)));
			when(jspBundle.getResource(anyString()))
					.thenAnswer(i -> JspServlet.class.getClassLoader().getResource(i.getArgument(0, String.class)));
		}

		controller.configure();
		controller.start();

		config = controller.getConfiguration();

		serverModel = new ServerModel(new Utils.SameThreadExecutor());

		containerRef = mock(ServiceReference.class);
		when(containerRef.getProperty(Constants.SERVICE_ID)).thenReturn(42L);

		if (enableWhiteboardExtender()) {
			whiteboardBundle = mockBundle("org.ops4j.pax.web.pax-web-extender-whiteboard", null, true);
			whiteboardBundleContext = whiteboardBundle.getBundleContext();

			OsgiContextModel.DEFAULT_CONTEXT_MODEL.setOwnerBundle(whiteboardBundle);

			when(whiteboardBundleContext.createFilter(anyString()))
					.thenAnswer(invocation -> FrameworkUtil.createFilter(invocation.getArgument(0, String.class)));

			when(whiteboardBundleContext.registerService(ArgumentMatchers.eq(ServletContext.class), any(ServletContext.class), any(Dictionary.class)))
					.thenReturn(mock(ServiceRegistration.class));

			// manually create mock for WebContainer service scoped to a pax-web-extender-whiteboard bundle
			HttpServiceEnabled container = new HttpServiceEnabled(whiteboardBundle, controller, serverModel, null, config);
			//		containers.put(whiteboardBundle, container);

			when(whiteboardBundleContext.getService(containerRef)).thenReturn(container);

			when(whiteboardBundleContext.getServiceReferences(WebContainer.class.getName(), null))
					.thenReturn(new ServiceReference[] { containerRef });
			whiteboardExtender = new WhiteboardExtenderContext(whiteboardBundleContext, true);

			servletContextHelperCustomizer = getCustomizer(ServletContextHelperTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			servletContextHelperMappingCustomizer = getCustomizer(ServletContextHelperMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			httpContextCustomizer = getCustomizer(HttpContextTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			httpContextMappingCustomizer = getCustomizer(HttpContextMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));

			servletCustomizer = getCustomizer(ServletTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			filterCustomizer = getCustomizer(FilterTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			resourceCustomizer = getCustomizer(ResourceTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			listenerCustomizer = getCustomizer(ListenerTracker.createTracker(whiteboardExtender, whiteboardBundleContext));

			servletMappingCustomizer = getCustomizer(ServletMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			filterMappingCustomizer = getCustomizer(FilterMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			resourceMappingCustomizer = getCustomizer(ResourceMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			welcomeFileMappingCustomizer = getCustomizer(WelcomeFileMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			errorPageMappingCustomizer = getCustomizer(ErrorPageMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			listenerMappingCustomizer = getCustomizer(ListenerMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
			jspMappingCustomizer = getCustomizer(JspMappingTracker.createTracker(whiteboardExtender, whiteboardBundleContext));
		}

		if (enableWarExtender()) {
			warExtenderBundle = mockBundle("org.ops4j.pax.web.pax-web-extender-war", null, false);
			warExtenderBundleContext = warExtenderBundle.getBundleContext();
			when(warExtenderBundleContext.getServiceReferences(WebContainer.class.getName(), null))
					.thenReturn(new ServiceReference[] { containerRef });

			warExtenderPool = Executors.newFixedThreadPool(1, new NamedThreadFactory("wab-extender"));
			warExtender = new WarExtenderContext(warExtenderBundleContext, warExtenderPool, true);
			warExtender.webContainerAdded(containerRef);
		}

		if (enableWebSockets()) {
			wsGenericBundle = mockBundle("org.ops4j.pax.web.pax-web-websocket", null, false);
			wsJettyBundle = mockBundle("org.eclipse.jetty.websocket.javax.websocket.server", null, false);
			wsTomcatBundle = mockBundle("org.ops4j.pax.web.pax-web-tomcat-websocket", null, false);
			wsUndertowBundle = mockBundle("org.ops4j.pax.web.pax-web-undertow-websocket", null, false);

			when(wsGenericBundle.getBundleId()).thenReturn(201L);
			when(wsJettyBundle.getBundleId()).thenReturn(202L);
			when(wsTomcatBundle.getBundleId()).thenReturn(203L);
			when(wsUndertowBundle.getBundleId()).thenReturn(204L);
			when(wsGenericBundle.getState()).thenReturn(Bundle.ACTIVE);
			when(wsJettyBundle.getState()).thenReturn(Bundle.ACTIVE);
			when(wsTomcatBundle.getState()).thenReturn(Bundle.ACTIVE);
			when(wsUndertowBundle.getState()).thenReturn(Bundle.ACTIVE);

			when(wsGenericBundle.loadClass(anyString()))
					.thenAnswer(i -> PaxWebWebSocketsServletContainerInitializer.class.getClassLoader().loadClass(i.getArgument(0, String.class)));
			when(wsJettyBundle.loadClass(anyString()))
					.thenAnswer(i -> WebSocketServerContainerInitializer.class.getClassLoader().loadClass(i.getArgument(0, String.class)));
			when(wsTomcatBundle.loadClass(anyString()))
					.thenAnswer(i -> WsSci.class.getClassLoader().loadClass(i.getArgument(0, String.class)));
			when(wsUndertowBundle.loadClass(anyString()))
					.thenAnswer(i -> PaxWebUndertowExtension.class.getClassLoader().loadClass(i.getArgument(0, String.class)));
		}
	}

	@After
	public void cleanup() throws Exception {
		if (enableWhiteboardExtender()) {
			stopWhiteboardService();
		}
		if (enableWarExtender()) {
			warExtender.shutdown();
		}
		if (controller != null) {
			if (controller.getState() == ServerState.STARTED) {
				controller.stop();
			}
			controller = null;
		}
	}

	protected String sessionPersistenceLocation() {
		return null;
	}

	protected boolean enableJSP() {
		return false;
	}

	protected boolean enableWarExtender() {
		return false;
	}

	protected boolean enableWhiteboardExtender() {
		return true;
	}

	protected boolean enableWebSockets() {
		return false;
	}

	protected void stopWhiteboardService() {
		containerRef = null;
		containers.values().forEach(HttpServiceEnabled::stop);
		containers.clear();
		if (whiteboardExtender != null) {
			whiteboardExtender.shutdown();
		}
	}

	protected Bundle mockBundle(String symbolicName) {
		return mockBundle(symbolicName, null, true);
	}

	protected Bundle mockBundle(String symbolicName, String contextPath) {
		return mockBundle(symbolicName, contextPath, true);
	}

	protected Bundle mockBundle(String symbolicName, boolean obtainWebContainer) {
		return mockBundle(symbolicName, null, obtainWebContainer);
	}

	/**
	 * Helper method to create mock {@link Bundle} with associated mock {@link BundleContext}.
	 * @param symbolicName
	 * @param contextPath a value for {@link PaxWebConstants#CONTEXT_PATH_HEADER}
	 * @param obtainWebContainer whether to configure bundle-scoped {@link WebContainer} reference
	 *                           for this bundle.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Bundle mockBundle(String symbolicName, String contextPath, boolean obtainWebContainer) {
		Bundle bundle = mock(Bundle.class);
		BundleContext bundleContext = mock(BundleContext.class);
		when(bundle.getSymbolicName()).thenReturn(symbolicName);
		when(bundle.getVersion()).thenReturn(Version.parseVersion("1.0.0"));
		when(bundle.toString()).thenReturn("Bundle \"" + symbolicName + "\"");
		when(bundle.getBundleContext()).thenReturn(bundleContext);
		when(bundleContext.getBundle()).thenReturn(bundle);

		if (contextPath != null) {
			Dictionary<String, String> headers = new Hashtable<>();
			headers.put(PaxWebConstants.CONTEXT_PATH_HEADER, contextPath);
			when(bundle.getHeaders()).thenReturn(headers);
		}

		BundleWiring wiring = mock(BundleWiring.class);
		when(bundle.adapt(BundleWiring.class)).thenReturn(wiring);
		when(wiring.getClassLoader()).thenReturn(this.getClass().getClassLoader());

		BundleRevision revision = mock(BundleRevision.class);
		when(bundle.adapt(BundleRevision.class)).thenReturn(revision);
		when(revision.getWiring()).thenReturn(wiring);
		when(revision.getBundle()).thenReturn(bundle);

		// prepare real lists to be populated when needed
		when(wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)).thenReturn(new LinkedList<>());
		when(wiring.getRequiredWires(null)).thenReturn(new LinkedList<>());
		when(wiring.getBundle()).thenReturn(bundle);

		when(bundleContext.registerService(ArgumentMatchers.eq(ServletContext.class), any(ServletContext.class),
				any(Dictionary.class)))
				.thenReturn(mock(ServiceRegistration.class));

		if (obtainWebContainer) {
			// this.containerRef is single reference, but it may be passed to getService() for
			// multiple bundle contexts (mocks)
			final HttpServiceEnabled container = new HttpServiceEnabled(bundle, controller, serverModel, null, config);
			when(bundleContext.getServiceReference(WebContainer.class)).thenReturn(containerRef);
			when(bundleContext.getService(containerRef)).thenReturn(container);
			containers.put(bundle, container);

			when(bundleContext.ungetService(containerRef)).thenAnswer(inv -> {
				// needed to correctly clean up servlets/filters added by SCIs
				container.stop();
				return true;
			});
		}

		if (enableJSP()) {
			when(bundleContext.getBundles()).thenReturn(new Bundle[] { bundle, jspBundle });
		} else {
			when(bundleContext.getBundles()).thenReturn(new Bundle[] { bundle });
		}

		try {
			when(bundle.getResources("META-INF/services/javax.servlet.ServletContainerInitializer"))
					.thenReturn(Collections.emptyEnumeration());
		} catch (IOException ignored) {
		}

		return bundle;
	}

	protected WebContainer container(Bundle bundle) {
		return containers.get(bundle);
	}

	protected void stopContainer(Bundle bundle) {
		HttpServiceEnabled wc = containers.remove(bundle);
		((StoppableHttpService) wc).stop();
		if (enableWhiteboardExtender()) {
			whiteboardExtender.bundleStopped(bundle);
		}
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
		if (enableWhiteboardExtender()) {
			when(whiteboardBundleContext.getService(schRef)).thenReturn(instance);
		}

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
				if (enableWhiteboardExtender()) {
					when(whiteboardBundleContext.getService(servletRef)).thenReturn(instance);
				}
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
		if (enableWhiteboardExtender()) {
			when(whiteboardBundleContext.getService(filterRef)).thenReturn(instance);
		}

		return filterRef;
	}

	protected ServiceReference<Preprocessor> mockPreprocessorReference(Bundle bundle, String name,
			Supplier<Preprocessor> supplier, Long serviceId, Integer rank) {
		Hashtable<String, Object> props = new Hashtable<>();

		Preprocessor instance = supplier.get();
		try {
			when(bundle.loadClass(instance.getClass().getName()))
					.thenAnswer((Answer<Class<?>>) invocation -> instance.getClass());
			when(bundle.loadClass(Preprocessor.class.getName()))
					.thenAnswer((Answer<Class<?>>) invocation -> Preprocessor.class);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		ServiceReference<Preprocessor> ref = mockReference(bundle, Preprocessor.class, props, null, serviceId, rank);
		when(bundle.getBundleContext().getService(ref)).thenReturn(instance);
		if (enableWhiteboardExtender()) {
			when(whiteboardBundleContext.getService(ref)).thenReturn(instance);
		}

		return ref;
	}

	protected <S> ServiceReference<S> mockReference(Bundle bundle, Class<S> clazz, Hashtable<String, Object> props,
			Supplier<S> supplier) {
		return mockReference(bundle, clazz, props, supplier, 0L, 0);
	}

	protected <S> ServiceReference<S> mockReference(Bundle bundle, Class<S> clazz, Hashtable<String, Object> props,
			Supplier<S> supplier, Long serviceId, Integer rank) {
		return mockReference(bundle, new Class<?>[] { clazz }, props, supplier, serviceId, rank);
	}

	@SuppressWarnings("unchecked")
	protected <S> ServiceReference<S> mockReference(Bundle bundle, Class<?>[] classes, Hashtable<String, Object> props,
			Supplier<S> supplier, Long serviceId, Integer rank) {
		ServiceReference<S> ref = mock(ServiceReference.class);
		when(ref.toString()).thenReturn("ref:" + Arrays.asList(classes));

		String[] names = Arrays.stream(classes).map(Class::getName).toArray(String[]::new);
		when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(names);

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
			if (enableWhiteboardExtender()) {
				when(whiteboardBundleContext.getService(ref)).thenReturn(instance);
			}
		}
		when(ref.getProperty(Constants.SERVICE_SCOPE))
				.thenReturn(singleton ? Constants.SCOPE_SINGLETON : Constants.SCOPE_BUNDLE);

		return ref;
	}

	/**
	 * Shorthand method to (re)configure mocked {@link ServiceReference} to return additional property
	 * @param ref
	 * @param contextNames
	 */
	protected void mockContextSelectProperty(ServiceReference<?> ref, String... contextNames) {
		Hashtable<String, Object> newProperties = new Hashtable<>();
		Dictionary<String, Object> current = ref.getProperties();
		if (current != null) {
			for (Enumeration<String> e = current.keys(); e.hasMoreElements(); ) {
				String key = e.nextElement();
				newProperties.put(key, current.get(key));
			}
		}

		if (contextNames.length == 1) {
			newProperties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(%s=%s)",
					HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, contextNames[0]));
		} else {
			StringBuilder contexts = new StringBuilder();
			for (String cn : contextNames) {
				contexts.append("(").append(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME).append("=");
				contexts.append(cn).append(")");
			}
			newProperties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|%s)", contexts));
		}

		newProperties.forEach((k, v) -> when(ref.getProperty(k)).thenReturn(v));
		when(ref.getPropertyKeys()).thenReturn(newProperties.keySet().toArray(new String[0]));
		when(ref.getProperties()).thenReturn(newProperties);
	}

	protected void mockProperty(ServiceReference<?> ref, String name, String value) {
		Hashtable<String, Object> newProperties = new Hashtable<>();
		Dictionary<String, Object> current = ref.getProperties();
		if (current != null) {
			for (Enumeration<String> e = current.keys(); e.hasMoreElements(); ) {
				String key = e.nextElement();
				newProperties.put(key, current.get(key));
			}
		}

		newProperties.put(name, value);

		newProperties.forEach((k, v) -> when(ref.getProperty(k)).thenReturn(v));
		when(ref.getPropertyKeys()).thenReturn(newProperties.keySet().toArray(new String[0]));
		when(ref.getProperties()).thenReturn(newProperties);
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

	public ServiceTrackerCustomizer<Object, ServletModel> getResourceCustomizer() {
		return resourceCustomizer;
	}

	public ServiceTrackerCustomizer<EventListener, EventListenerModel> getListenerCustomizer() {
		return listenerCustomizer;
	}

	public ServiceTrackerCustomizer<ServletMapping, ServletModel> getServletMappingCustomizer() {
		return servletMappingCustomizer;
	}

	public ServiceTrackerCustomizer<FilterMapping, FilterModel> getFilterMappingCustomizer() {
		return filterMappingCustomizer;
	}

	public ServiceTrackerCustomizer<ResourceMapping, ServletModel> getResourceMappingCustomizer() {
		return resourceMappingCustomizer;
	}

	public ServiceTrackerCustomizer<WelcomeFileMapping, WelcomeFileModel> getWelcomeFileMappingCustomizer() {
		return welcomeFileMappingCustomizer;
	}

	public ServiceTrackerCustomizer<ErrorPageMapping, ErrorPageModel> getErrorPageMappingCustomizer() {
		return errorPageMappingCustomizer;
	}

	public ServiceTrackerCustomizer<ListenerMapping, EventListenerModel> getListenerMappingCustomizer() {
		return listenerMappingCustomizer;
	}

	public ServiceTrackerCustomizer<JspMapping, JspModel> getJspMappingCustomizer() {
		return jspMappingCustomizer;
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

	protected void installWab(final Bundle wab) {
		when(wab.getState()).thenReturn(Bundle.ACTIVE);
		Extension extension = warExtender.createExtension(wab, null);
		wabs.put(wab, extension);
		final CountDownLatch latch = new CountDownLatch(1);
		warExtenderPool.submit(() -> {
			try {
				extension.start();
				latch.countDown();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected void uninstallWab(final Bundle wab) {
		try {
			when(wab.getState()).thenReturn(Bundle.STOPPING);
			wabs.remove(wab).destroy();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	protected void wireByPackage(Bundle b1, Bundle b2, String pkg) {
		List<BundleWire> wires = b1.adapt(BundleWiring.class).getRequiredWires(null);
		BundleWire wire = mock(BundleWire.class);
		wires.add(wire);

		BundleRequirement importPackage = mock(BundleRequirement.class);
		when(wire.getRequirement()).thenReturn(importPackage);
		when(importPackage.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);

		BundleCapability exportPackage = mock(BundleCapability.class);
		BundleRevision b2Revision = b2.adapt(BundleRevision.class);
		when(exportPackage.getRevision()).thenReturn(b2Revision);
		when(wire.getCapability()).thenReturn(exportPackage);
	}

	protected void wireByBundle(Bundle b1, Bundle b2) {
		List<BundleWire> wires = b1.adapt(BundleWiring.class).getRequiredWires(null);
		BundleWire wire = mock(BundleWire.class);
		wires.add(wire);

		BundleRequirement requireBundle = mock(BundleRequirement.class);
		when(wire.getRequirement()).thenReturn(requireBundle);
		when(requireBundle.getNamespace()).thenReturn(BundleNamespace.BUNDLE_NAMESPACE);

		BundleCapability targetBundleCap = mock(BundleCapability.class);
		BundleRevision b2Revision = b2.adapt(BundleRevision.class);
		when(targetBundleCap.getRevision()).thenReturn(b2Revision);
		when(wire.getCapability()).thenReturn(targetBundleCap);
	}

	protected ServerModelInternals serverModelInternals(ServerModel serverModel) {
		return new ServerModelInternals(serverModel);
	}

	protected ServiceModelInternals serviceModelInternals(Bundle bundle) {
		return new ServiceModelInternals(getField(containers.get(bundle), "serviceModel", ServiceModel.class));
	}

	protected ServiceModelInternals serviceModelInternals(WebContainer httpService) {
		return new ServiceModelInternals(getField(httpService, "serviceModel", ServiceModel.class));
	}

	/**
	 * Configures the value returned in {@link Constants#BUNDLE_CLASSPATH} entry. {@code root} base directory
	 * points to a hierarchy of entries available using {@link Bundle#getEntry(String)}.
	 * @param bundle
	 * @param root
	 * @param configurator
	 * @throws MalformedURLException
	 */
	protected void configureBundleClassPath(Bundle bundle, String root, Consumer<List<String>> configurator) throws IOException {
		List<String> entries = new ArrayList<>();
		configurator.accept(entries);

		long id = bundle.getBundleId();

		File jars = new File("target/bundles/bundle" + id);
		jars.mkdirs();

		for (String entry : entries) {

			File base = new File(root, entry);
			if (!base.isDirectory()) {
				continue;
			}
			if (base.getName().endsWith(".jar")) {
				// turn it into a real JAR
				File jar = pack(jars, base);
				when(bundle.getEntry(entry)).thenReturn(new URL(String.format("bundle://%d.0:0%s", id, jar.toURI().getPath())));
			} else {
				// it'll be available as normal directory
				when(bundle.getEntry(entry)).thenReturn(new URL(String.format("bundle://%d.0:0%s", id, base.toURI().getPath())));
			}
		}

		bundle.getHeaders().put(Constants.BUNDLE_CLASSPATH, String.join(", ", entries));
	}

	private File pack(File target, File base) throws IOException {
		File targetJar = new File(target, base.getName());
		try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(targetJar)) {
			Files.walk(base.toPath()).forEach(p -> {
				String name = base.toPath().relativize(p).toString();
				if ("".equals(name)) {
					return;
				}
				try {
					ArchiveEntry entry = zos.createArchiveEntry(p.toFile(), name);
					zos.putArchiveEntry(entry);
					if (p.toFile().isFile()) {
						try (FileInputStream fis = new FileInputStream(p.toFile())) {
							IOUtils.copy(fis, zos);
						}
					}
					zos.closeArchiveEntry();
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			});
		}
		return targetJar;
	}

	protected void attachBundleFragment(Bundle bundle, Bundle itsFragment) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		List<BundleWire> hostWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
		BundleWire fragmentWire = mock(BundleWire.class);
		hostWires.add(fragmentWire);
		BundleWiring fragmentWiring = itsFragment.adapt(BundleWiring.class);
		when(fragmentWire.getRequirerWiring()).thenReturn(fragmentWiring);

		BundleRevision fragmentRevision = mock(BundleRevision.class);
		when(fragmentRevision.getTypes()).thenReturn(BundleRevision.TYPE_FRAGMENT);
		when(itsFragment.adapt(BundleRevision.class)).thenReturn(fragmentRevision);
	}

	/**
	 * Class to verify {@link ServerModel} after performing a test
	 */
	public static class ServerModelInternals {

		public final Map<String, ServletContextModel> servletContexts = new HashMap<>();
		public final Map<ContextKey, TreeSet<OsgiContextModel>> bundleContexts = new HashMap<>();
		public final Map<String, TreeSet<OsgiContextModel>> sharedContexts = new HashMap<>();
		public final Map<String, OsgiContextModel> whiteboardContexts = new HashMap<>();
		public final Map<Servlet, ServletModel> servlets = new IdentityHashMap<>();
		public final Set<ServletModel> disabledServletModels = new TreeSet<>();
		public final Map<Filter, FilterModel> filters = new IdentityHashMap<>();
		public final Set<FilterModel> disabledFilterModels = new TreeSet<>();
		public final Set<ErrorPageModel> disabledErrorPageModels = new TreeSet<>();
		public final Map<EventListener, EventListenerModel> eventListeners = new IdentityHashMap<>();
		public final Map<ServletContainerInitializer, ContainerInitializerModel> containerInitializers = new IdentityHashMap<>();
		public final Map<Object, WebSocketModel> webSockets = new IdentityHashMap<>();
		public final Set<WebSocketModel> disabledWebSocketModels = new TreeSet<>();

		private final ServerModel model;
//		private final Map<String, VirtualHostModel> virtualHosts = new HashMap<>();
//		private final VirtualHostModel defaultHost = new VirtualHostModel();

		@SuppressWarnings("unchecked")
		public ServerModelInternals(ServerModel model) {
			this.model = model;
			servletContexts.putAll(getField(model, "servletContexts", Map.class));
			bundleContexts.putAll(getField(model, "bundleContexts", Map.class));
			sharedContexts.putAll(getField(model, "sharedContexts", Map.class));
//			whiteboardContexts.putAll(getField(model, "whiteboardContexts", Map.class));
			servlets.putAll(getField(model, "servlets", Map.class));
			disabledServletModels.addAll(getField(model, "disabledServletModels", Set.class));
			filters.putAll(getField(model, "filters", Map.class));
			disabledFilterModels.addAll(getField(model, "disabledFilterModels", Set.class));
			disabledErrorPageModels.addAll(getField(model, "disabledErrorPageModels", Set.class));
			eventListeners.putAll(getField(model, "eventListeners", Map.class));
			containerInitializers.putAll(getField(model, "containerInitializers", Map.class));
			webSockets.putAll(getField(model, "webSockets", Map.class));
			disabledWebSocketModels.addAll(getField(model, "disabledWebSocketModels", Set.class));
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
			clean &= disabledServletModels.stream().noneMatch(dsm -> dsm.getRegisteringBundle().equals(bundle));
			clean &= disabledFilterModels.stream().noneMatch(dfm -> dfm.getRegisteringBundle().equals(bundle));
			clean &= disabledErrorPageModels.stream().noneMatch(depm -> depm.getRegisteringBundle().equals(bundle));
			clean &= eventListeners.values().stream().noneMatch(elm -> elm.getRegisteringBundle().equals(bundle));
			clean &= containerInitializers.values().stream().noneMatch(cim -> cim.getRegisteringBundle().equals(bundle));
			clean &= webSockets.values().stream().noneMatch(wsm -> wsm.getRegisteringBundle().equals(bundle));
			clean &= disabledWebSocketModels.stream().noneMatch(wsm -> wsm.getRegisteringBundle().equals(bundle));
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
		public final Set<EventListenerModel> eventListenerModels = new HashSet<>();
		private final Map<ContextKey, Set<String>> welcomeFiles = new HashMap<>();
		private final Set<WelcomeFileModel> welcomeFileModels = new HashSet<>();
		private final Set<ErrorPageModel> errorPageModels = new HashSet<>();
		private final Set<ContainerInitializerModel> containerInitializerModels = new HashSet<>();
		private final Set<WebSocketModel> webSocketModels = new HashSet<>();

		private final ServiceModel model;

		@SuppressWarnings("unchecked")
		public ServiceModelInternals(ServiceModel model) {
			this.model = model;
			aliasMapping.putAll(getField(model, "aliasMapping", Map.class));
			servletModels.addAll(getField(model, "servletModels", Set.class));
			filterModels.addAll(getField(model, "filterModels", Set.class));
			eventListenerModels.addAll(getField(model, "eventListenerModels", Set.class));
			welcomeFiles.putAll(getField(model, "welcomeFiles", Map.class));
			welcomeFileModels.addAll(getField(model, "welcomeFileModels", Set.class));
			errorPageModels.addAll(getField(model, "errorPageModels", Set.class));
			containerInitializerModels.addAll(getField(model, "containerInitializerModels", Set.class));
			webSocketModels.addAll(getField(model, "webSocketModels", Set.class));
		}

		public boolean isEmpty() {
			return aliasMapping.isEmpty()
					&& servletModels.isEmpty()
					&& filterModels.isEmpty()
					&& eventListenerModels.isEmpty()
					&& welcomeFiles.isEmpty()
					&& welcomeFileModels.isEmpty()
					&& errorPageModels.isEmpty()
					&& webSocketModels.isEmpty()
					&& containerInitializerModels.isEmpty();
		}
	}

}
