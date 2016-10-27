/*
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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ResourceModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.util.ResourceDelegatingBundleClassLoader;
import org.ops4j.pax.web.utils.ServletContainerInitializerScanner;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ErrorPage;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.ConstructorInstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.ETag;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;

/**
 * @author Guillaume Nodet
 */
public class Context implements LifeCycle, HttpHandler, ResourceManager {

	private static final Logger LOG = LoggerFactory.getLogger(Context.class);

	private final IdentityManager identityManager;
	private final PathHandler path;
	private final ContextModel contextModel;
	private final List<ServletModel> servlets = new ArrayList<>();
	private final List<WelcomeFileModel> welcomeFiles = new ArrayList<>();
	private final List<ErrorPageModel> errorPages = new ArrayList<>();
	private final List<EventListenerModel> eventListeners = new ArrayList<>();
	private final List<SecurityConstraintMappingModel> securityConstraintMappings = new ArrayList<>();
	private final List<FilterModel> filters = new ArrayList<>();
	private final List<ContainerInitializerModel> containerInitializers = new ArrayList<>();
	private final List<ServiceRegistration<ServletContext>> registeredServletContexts = new ArrayList<>();
	private final ServletContainer container = ServletContainer.Factory.newInstance();
	private final AtomicBoolean started = new AtomicBoolean();
	private final ClassLoader classLoader;
	private volatile HttpHandler handler;

	private DeploymentManager manager;

	private Bundle undertowBundle;

	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;

	public Context(IdentityManager identityManager, PathHandler path, ContextModel contextModel) {
		this.identityManager = identityManager;
		this.path = path;
		this.contextModel = contextModel;

		ClassLoader classLoader = contextModel.getClassLoader();
		List<Bundle> bundles = ((ResourceDelegatingBundleClassLoader) classLoader).getBundles();
		BundleClassLoader parentClassLoader = new BundleClassLoader(FrameworkUtil.getBundle(getClass()));
		this.classLoader = new ResourceDelegatingBundleClassLoader(bundles, parentClassLoader);

		LOG.info("registering context {}, with context-name: {}", contextModel.getHttpContext(), contextModel.getContextName());

		undertowBundle = FrameworkUtil.getBundle(getClass());

		if (undertowBundle != null) {
			Filter filterPackage = null;
			try {
				filterPackage = undertowBundle.getBundleContext()
						.createFilter("(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
			} catch (InvalidSyntaxException e) {
				LOG.error("InvalidSyntaxException while waiting for PackageAdmin Service", e);
			}
			packageAdminTracker = new ServiceTracker<>(undertowBundle.getBundleContext(),
					filterPackage, null);
			packageAdminTracker.open();
		}
	}

	public ContextModel getContextModel() {
		return contextModel;
	}

	@Override
	public synchronized void start() throws Exception {
		if (started.compareAndSet(false, true)) {
			for (ServletModel servlet : servlets) {
				doStart(servlet);
			}
			createHandler();
		}
	}

	@Override
	public synchronized void stop() throws Exception {
		if (started.compareAndSet(true, false)) {
			for (ServletModel servlet : servlets) {
				doStop(servlet);
			}
		}
	}

	private void doStart(ServletModel servlet) throws ServletException {
		for (String pattern : servlet.getUrlPatterns()) {
			if (!contextModel.getContextName().isEmpty()) {
				pattern = "/" + contextModel.getContextName() + pattern;
			}
			if (pattern.endsWith("/*") || pattern.endsWith("/")) {
				if (pattern.endsWith("/*")) {
					pattern = pattern.substring(0, pattern.length() - 1);
				}
				path.addPrefixPath(pattern, this);
			} else {
				path.addExactPath(pattern, this);
			}
		}
	}

	private void doStop(ServletModel servlet) throws ServletException {
		for (String pattern : servlet.getUrlPatterns()) {
			if (pattern.endsWith("/*")) {
				pattern = pattern.substring(0, pattern.length() - 1);
				path.removePrefixPath(pattern);
			} else {
				path.removeExactPath(pattern);
			}
		}
	}

	public synchronized void destroy() {
		try {
			destroyHandler();
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		HttpHandler h = getHandler();
		if (h != null) {
			// Put back original request path
			String path = exchange.getRequestPath();
			if (!contextModel.getContextName().isEmpty()) {
				path = path.substring(contextModel.getContextName().length() + 1);
			}
			exchange.setRelativePath(path);
			h.handleRequest(exchange);
		} else {
			exchange.setResponseCode(StatusCodes.NOT_FOUND);
			exchange.endExchange();
		}
	}

	private HttpHandler getHandler() throws ServletException {
		if (handler == null) {
			synchronized (this) {
				createHandler();
			}
		}
		return handler;
	}

	private synchronized void createHandler() throws ServletException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader ncl = classLoader;
			Thread.currentThread().setContextClassLoader(ncl);
			doCreateHandler();
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	private synchronized void destroyHandler() throws ServletException {
		if (manager != null) {
			unregisterServletContext(manager.getDeployment().getServletContext());
			manager.stop();
			manager.undeploy();
			manager = null;
			handler = null;
		}
	}

	private String getContextPathForOsgi(final ServletContext servletContext){
		String contextPath = servletContext.getContextPath();
		// Undertows ServletContextImpl maps "/" to "". In OSGi path must start with /
		if (contextPath != null && !contextPath.startsWith("/")) {
			contextPath = "/" + contextPath;
		}else if(contextPath == null){
			LOG.warn("ContextPath not found, it's not configured. Assuming '/'");
			contextPath = "/";
		}
		return contextPath;
	}


	private void unregisterServletContext(final ServletContext servletContext) {
		String webContextPath = getContextPathForOsgi(servletContext);
		// find ServiceRegistration which matches the given ServletContext
		Optional<ServiceRegistration<ServletContext>> serviceReg = registeredServletContexts.stream().filter(reg -> reg.getReference() != null
				&& webContextPath.equals(reg.getReference().getProperty(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH)))
				.findFirst();
		if(serviceReg.isPresent()){
			try{
				serviceReg.get().unregister();
			}catch(IllegalStateException e){
				LOG.error("Error during unregistration of ServletContext service with path '{}'!",
						webContextPath, e);
			}finally {
				registeredServletContexts.remove(serviceReg.get());
			}
		}
	}

	private void registerServletContext(final ServletContext servletContext, final Bundle bundle) {
			String webContextPath = getContextPathForOsgi(servletContext);
			// Undertows ServletContextImpl maps "/" to "". In OSGi path must start with /
			String filter = String.format("(%s=%s)",
					WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);
		Optional<ServiceReference<ServletContext>> first;
		try {
			first = bundle.getBundleContext().getServiceReferences(ServletContext.class, filter).stream().findFirst();
		} catch (InvalidSyntaxException e) {
			LOG.warn("Could not get ServiceReference for ServletContext!", e);
			first = Optional.empty();
		}
		if(!first.isPresent()) {
				Dictionary<String, String> props = new Hashtable<>(2);
				props.put(WebContainerConstants.PROPERTY_SYMBOLIC_NAME, bundle.getSymbolicName());
				props.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);
				props.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_NAME, servletContext.getServletContextName());
				ServiceRegistration<ServletContext> serviceReg = bundle.getBundleContext().registerService(
						ServletContext.class,
						servletContext,
						props);
				registeredServletContexts.add(serviceReg);
				LOG.debug("ServletContext registered as service. ");
			}
	}

	private void doCreateHandler() throws ServletException {
		final HttpContext httpContext = contextModel.getHttpContext();
		DeploymentInfo deployment = new DeploymentInfo();
		deployment.setEagerFilterInit(true);
		deployment.setDeploymentName(contextModel.getContextName());
		deployment.setDisplayName(httpContext instanceof WebContainerContext ? ((WebContainerContext) httpContext).getContextId() : contextModel.getContextName());
		deployment.setContextPath('/' + contextModel.getContextName());
		deployment.setClassLoader(classLoader);
		BundleContext bundleContext = contextModel.getBundle().getBundleContext();
		if (bundleContext != null) {
			deployment.addServletContextAttribute(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE, bundleContext);
			deployment.addServletContextAttribute("org.springframework.osgi.web.org.osgi.framework.BundleContext", bundleContext);
		}
		deployment.setResourceManager(this);
		deployment.setIdentityManager(identityManager);
		if (contextModel.getRealmName() != null && contextModel.getAuthMethod() != null) {
			LoginConfig cfg = new LoginConfig(
					contextModel.getAuthMethod(),
					contextModel.getRealmName(),
					contextModel.getFormLoginPage(),
					contextModel.getFormErrorPage());
			deployment.setLoginConfig(cfg);
		}
		for (ServletModel servlet : servlets) {
			if (servlet instanceof ResourceModel
					&& "default".equalsIgnoreCase(servlet.getName())) {
				// this is a default resource, so ignore it
				continue;
			}
			ServletInfo info = new ServletInfo(
					servlet.getName(),
					clazz(servlet.getServletClass(), servlet.getServlet()),
					factory(servlet.getServletClass(), servlet.getServlet())
			);
			for (Map.Entry<String, String> param : servlet.getInitParams().entrySet()) {
				info.addInitParam(param.getKey(), param.getValue());
			}
			info.addMappings(servlet.getUrlPatterns());
			info.setAsyncSupported(servlet.getAsyncSupported() != null ? servlet.getAsyncSupported() : false);
			info.setLoadOnStartup(servlet.getLoadOnStartup() != null ? servlet.getLoadOnStartup() : -1);
			deployment.addServlet(info);
		}
		for (WelcomeFileModel welcomeFile : welcomeFiles) {
			deployment.addWelcomePages(welcomeFile.getWelcomeFiles());
		}
		for (ErrorPageModel errorPage : errorPages) {
			try {
				int error = Integer.parseInt(errorPage.getError());
				deployment.addErrorPage(new ErrorPage(errorPage.getLocation(), error));
			} catch (NumberFormatException nfe) {
				try {
					@SuppressWarnings("unchecked")
					Class<? extends Throwable> clazz = (Class<? extends Throwable>)
							classLoader.loadClass(errorPage.getError());
					deployment.addErrorPage(new ErrorPage(errorPage.getLocation(), clazz));
				} catch (ClassNotFoundException cnfe) {
					cnfe.addSuppressed(nfe);
					throw new IllegalArgumentException("Unsupported error: " + errorPage.getError(), cnfe);
				}
			}
		}

		Bundle bundle = contextModel.getBundle();
		ServletContainerInitializerScanner scanner = new ServletContainerInitializerScanner(bundle, undertowBundle, packageAdminTracker.getService());
		Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel.getContainerInitializers();
		if (containerInitializers == null) {
			containerInitializers = new HashMap<>();
			contextModel.setContainerInitializers(containerInitializers);
		}
		scanner.scanBundles(containerInitializers);

		for (Entry<ServletContainerInitializer, Set<Class<?>>> entry : contextModel.getContainerInitializers().entrySet()) {
			deployment.addServletContainerInitalizer(new ServletContainerInitializerInfo(
					clazz(null, entry.getKey()),
					factory(null, entry.getKey()),
					entry.getValue()
			));
		}

		if (!filters.isEmpty() && filters.get(0).getInitParams().get(WebContainerConstants.FILTER_RANKING) != null) {
			filters.sort((filter1, filter2) -> Integer.valueOf(filter1.getInitParams().get(WebContainerConstants.FILTER_RANKING)).compareTo(Integer.valueOf(filter2.getInitParams().get(WebContainerConstants.FILTER_RANKING))));
		}

		for (FilterModel filter : filters) {
			FilterInfo info = new FilterInfo(filter.getName(),
					clazz(filter.getFilterClass(), filter.getFilter()),
					factory(filter.getFilterClass(), filter.getFilter()));
			for (Map.Entry<String, String> param : filter.getInitParams().entrySet()) {
				info.addInitParam(param.getKey(), param.getValue());
			}

			deployment.addFilter(info);
			String[] dispatchers = filter.getDispatcher();
			if (dispatchers == null || dispatchers.length == 0) {
				dispatchers = new String[]{"request"};
			}
			for (String dispatcher : dispatchers) {
				DispatcherType dt = DispatcherType.valueOf(dispatcher.toUpperCase());
				String[] servletNames = filter.getServletNames();
				if (servletNames != null) {
					for (String servletName : servletNames) {
						deployment.addFilterServletNameMapping(filter.getName(), servletName, dt);
					}
				}
				String[] urlPatterns = filter.getUrlPatterns();
				if (urlPatterns != null) {
					for (String urlPattern : urlPatterns) {
						deployment.addFilterUrlMapping(filter.getName(), urlPattern, dt);
					}
				}
			}
		}
		for (SecurityConstraintMappingModel securityConstraintMapping : securityConstraintMappings) {
			SecurityConstraint info = new SecurityConstraint();
//            if (securityConstraintMapping.isAuthentication()) {
//                info.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
//            }
			info.addRolesAllowed(securityConstraintMapping.getRoles());
			String dataConstraint = securityConstraintMapping.getDataConstraint();
			if (dataConstraint == null || "NONE".equals(dataConstraint)) {
				info.setTransportGuaranteeType(TransportGuaranteeType.NONE);
			} else if ("INTEGRAL".equals(dataConstraint)) {
				info.setTransportGuaranteeType(TransportGuaranteeType.INTEGRAL);
			} else {
				info.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
			}
			WebResourceCollection wr = new WebResourceCollection();
			if (securityConstraintMapping.getMapping() != null) {
				wr.addHttpMethod(securityConstraintMapping.getMapping());
			}
			if (securityConstraintMapping.getUrl() != null) {
				wr.addUrlPattern(securityConstraintMapping.getUrl());
			}
			info.addWebResourceCollection(wr);
			deployment.addSecurityConstraint(info);
		}
		for (EventListenerModel listener : eventListeners) {
			ListenerInfo info = new ListenerInfo(
					clazz(null, listener.getEventListener()),
					factory(null, listener.getEventListener()));
			deployment.addListener(info);
		}

		if (isJspAvailable()) { // use JasperClassloader
			try {
				@SuppressWarnings("unchecked")
				Class<ServletContainerInitializer> clazz = (Class<ServletContainerInitializer>)
						classLoader.loadClass("org.ops4j.pax.web.jsp.JasperInitializer");
				deployment.addServletContainerInitalizer(new ServletContainerInitializerInfo(
						clazz, factory(clazz, null), null));
			} catch (ClassNotFoundException e) {
//                LOG.error("Unable to load JasperInitializer", e);
				e.printStackTrace();
			}
		}

		if (isWebSocketAvailable()) {
			XnioWorker xnioWorker = UndertowUtil.createWorker(contextModel.getClassLoader());
			if (xnioWorker != null) {
				deployment.addServletContextAttribute(
						io.undertow.websockets.jsr.WebSocketDeploymentInfo.ATTRIBUTE_NAME,
						new io.undertow.websockets.jsr.WebSocketDeploymentInfo()
								.setWorker(xnioWorker)
								.setBuffers(new DefaultByteBufferPool(true, 100))
				);
			}
		}

		// Add HttpContext security support
		deployment.addInnerHandlerChainWrapper(new HandlerWrapper() {
			@Override
			public HttpHandler wrap(final HttpHandler handler) {
				return exchange -> {
					// Verify security
					ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
					if (contextModel.getHttpContext().handleSecurity(src.getOriginalRequest(), src.getOriginalResponse())) {
						handler.handleRequest(exchange);
					} else {
						// on case of security constraints not fulfilled, handleSecurity is
						// supposed to set the right
						// headers but to be sure lets verify the response header for 401
						// (unauthorized)
						// because if the header is not set the processing will go on with
						// the rest of the contexts
						try {
							src.getOriginalResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
						} catch (IllegalStateException e) {
							try {
								src.getOriginalResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							} catch (IllegalStateException ee) {
								// Ignore
							}
						}
					}
				};
			}
		});

		manager = container.addDeployment(deployment);
		manager.deploy();
		registerServletContext(manager.getDeployment().getServletContext(), bundle);
		handler = manager.start();
	}


	private static <T> Class<? extends T> clazz(Class<? extends T> clazz, T instance) {
		if (clazz != null) {
			return clazz;
		} else {
			return (Class<T>) instance.getClass();
		}
	}

	private static <T> InstanceFactory<? extends T> factory(Class<? extends T> clazz, T instance) throws ServletException {
		if (instance != null) {
			return new ImmediateInstanceFactory<>(instance);
		} else {
			try {
				Constructor<? extends T> cns = clazz.getDeclaredConstructor();
				cns.setAccessible(true);
				return new ConstructorInstanceFactory<>(cns);
			} catch (NoSuchMethodException e) {
				throw new ServletException("Unable to create factory", e);
			}
		}
	}


	@Override
	public Resource getResource(String path) throws IOException {
		HttpContext context = contextModel.getHttpContext();
		if (context instanceof WebContainerContext) {
			final URL resource = context.getResource(path);
			if (resource == null) {
				return null;
			} else if (resource.toString().endsWith("/")) {
				return new DirectoryResource(resource);
			} else {
				return new URLResource(resource, resource.openConnection(), path);
			}
		} else {
			String modPath = path;
			if (modPath.startsWith("/")) {
				modPath = path.substring(1);
			}
			final String realPath = modPath;
			final URL resource = classLoader.getResource(realPath);
			if (resource == null) {
				return null;
			} else {
				return new URLResource(resource, resource.openConnection(), path);
			}
		}
	}

	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {

	}

	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {

	}

	@Override
	public void close() throws IOException {

	}

	private boolean isJspAvailable() {
		try {
			return (org.ops4j.pax.web.jsp.JspServletWrapper.class != null);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	private boolean isWebSocketAvailable() {
		try {
			return (io.undertow.websockets.jsr.WebSocketDeploymentInfo.class != null);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	public synchronized void addServlet(ServletModel model) throws ServletException {
		if (servlets.add(model)) {
			destroyHandler();
			if (started.get()) {
				doStart(model);
			}
		}
	}

	public synchronized void removeServlet(ServletModel model) throws ServletException {
		if (servlets.remove(model)) {
			destroyHandler();
			if (started.get()) {
				doStop(model);
			}
		}
	}

	public synchronized void addWelcomeFile(WelcomeFileModel welcomeFile) throws ServletException {
		if (welcomeFiles.add(welcomeFile)) {
			destroyHandler();
		}
	}

	public synchronized void removeWelcomeFile(WelcomeFileModel welcomeFile) throws ServletException {
		if (welcomeFiles.remove(welcomeFile)) {
			destroyHandler();
		}
	}

	public void addErrorPage(ErrorPageModel model) throws ServletException {
		if (errorPages.add(model)) {
			destroyHandler();
		}
	}

	public void removeErrorPage(ErrorPageModel model) throws ServletException {
		if (errorPages.remove(model)) {
			destroyHandler();
		}
	}

	public void addEventListener(EventListenerModel model) throws ServletException {
		if (eventListeners.add(model)) {
			destroyHandler();
		}
	}

	public void removeEventListener(EventListenerModel model) throws ServletException {
		if (eventListeners.remove(model)) {
			destroyHandler();
		}
	}

	public void addFilter(FilterModel model) throws ServletException {
		if (filters.add(model)) {
			destroyHandler();
		}
	}

	public void removeFilter(FilterModel model) throws ServletException {
		if (filters.remove(model)) {
			destroyHandler();
		}
	}

	public void addSecurityConstraintMapping(SecurityConstraintMappingModel model) throws ServletException {
		if (securityConstraintMappings.add(model)) {
			destroyHandler();
		}
	}

	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel model) throws ServletException {
		if (securityConstraintMappings.remove(model)) {
			destroyHandler();
		}
	}

	public void addContainerInitializerModel(ContainerInitializerModel model) throws ServletException {
		if (containerInitializers.add(model)) {
			destroyHandler();
		}
	}

	public void removeContainerInitializerModel(ContainerInitializerModel model) throws ServletException {
		if (containerInitializers.remove(model)) {
			destroyHandler();
		}
	}

	private class DirectoryResource implements Resource {
		private final URL url;

		public DirectoryResource(URL url) throws IOException {
			this.url = url;
		}

		@Override
		public String getPath() {
			return url.getPath();
		}

		@Override
		public Date getLastModified() {
			return null;
		}

		@Override
		public String getLastModifiedString() {
			return null;
		}

		@Override
		public ETag getETag() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public boolean isDirectory() {
			return true;
		}

		@Override
		public List<Resource> list() {
			try {
				List<Resource> children = new ArrayList<>();
				if (contextModel.getHttpContext() instanceof WebContainerContext) {
					WebContainerContext ctx = (WebContainerContext) contextModel.getHttpContext();
					Set<String> rps = ctx.getResourcePaths(getPath());
					if (rps != null) {
						for (String child : rps) {
							children.add(getResource(child));
						}
					}
				}
				return children;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public String getContentType(MimeMappings mimeMappings) {
			return null;
		}

		@Override
		public void serve(Sender sender, HttpServerExchange exchange, IoCallback completionCallback) {

		}

		@Override
		public Long getContentLength() {
			return null;
		}

		@Override
		public String getCacheKey() {
			return null;
		}

		@Override
		public File getFile() {
			return null;
		}

		@Override
		public File getResourceManagerRoot() {
			return null;
		}

		@Override
		public URL getUrl() {
			return url;
		}

		@Override
		public Path getFilePath() {
			return null;
		}

		@Override
		public Path getResourceManagerRootPath() {
			return null;
		}
	}
}
