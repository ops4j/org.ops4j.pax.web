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
package org.ops4j.pax.web.service.spi.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextAttributeEvent;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Implementation of {@link ServletContext} for the contract described in 140.2.6 "Behavior of the Servlet Context"
 * from OSGi CMPN R7 Whiteboard Service specification. That's the 1:1 mapping with single
 * {@link org.osgi.service.servlet.context.ServletContextHelper} (or {@link org.ops4j.pax.web.service.http.HttpContext}).</p>
 *
 * <p>When handling single servlet (or generally filters+servlet chain), we have to provide special facade for
 * {@link ServletContext} from Servlet API in the form of {@link OsgiServletContext}. And during actual request
 * processing, we need yet another facade to provide required {@link ServletContext#getClassLoader()} behavior.</p>
 */
public class OsgiServletContext implements ServletContext {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiServletContext.class);

	private ServletContext containerServletContext;
	private final OsgiContextModel osgiContextModel;
	private final ServletContextModel servletContextModel;

	private final ClassLoader classLoader;

	/**
	 * {@link WebContainerContext} obtained from {@link OsgiContextModel} in the context of the bundle registering
	 * the <em>context</em> ({@link org.ops4j.pax.web.service.http.HttpContext} or
	 * {@link org.osgi.service.servlet.context.ServletContextHelper}). This {@link WebContainerContext} is used by
	 * default, but during actual request processing, different {@link WebContainerContext} should be used - the one
	 * obtained (e.g., from OSGi service registry, {@link org.osgi.framework.ServiceFactory}) in the context of a
	 * bundle that registered target {@link Servlet}.
	 */
	private final WebContainerContext defaultWebContainerContext;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>();

	/** Welcome files are kept at this level - to be accessed by resource servlets, uniquely for each OSGi context */
	private String[] welcomeFiles;

	private boolean welcomeFilesRedirect = false;

	/** If this context is registered as OSGi service, here's the registration */
	private ServiceRegistration<ServletContext> registration;

	private final List<ServletContextAttributeListener> attributeListeners = new CopyOnWriteArrayList<>();

	private final SessionCookieConfig defaultSessionCookieConfig;

	/** The collected names of the attributes which have to be cleared when the container is restarted */
	private final Set<String> attributesToClearBeforeRestart = new HashSet<>();

	private boolean acceptsServletContextListeners = true;

	/**
	 * Constructor called when {@link OsgiContextModel} is passed to given
	 * {@link org.ops4j.pax.web.service.spi.ServerController}. We still can't grab an instance of
	 * {@link WebContainerContext} in the constructor, because it has to be resolved in the context of particular
	 * web request invocation - target {@link Servlet} determines the {@link OsgiContextModel} used and the
	 * {@link org.osgi.framework.Bundle} associated with the target {@link Servlet} is used to resolve actual
	 * {@link WebContainerContext}.
	 *  @param containerServletContext
	 * @param osgiContextModel
	 * @param servletContextModel
	 * @param loader
	 */
	public OsgiServletContext(ServletContext containerServletContext, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel, SessionCookieConfig defaultSessionCookieConfig,
			ClassLoader loader) {
		this.containerServletContext = containerServletContext;
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;

		Bundle ownerBundle = osgiContextModel.getOwnerBundle();
		this.defaultWebContainerContext = osgiContextModel.resolveHttpContext(ownerBundle);

		// This attribute is defined in 128.6.1 "Bundle Context Access" in the context of WAB applications
		// but we have to store bundle context of the bundle of the OsgiContextModel in all cases
		// (WAB, Whiteboard, HttpService) anyway
		if (ownerBundle != null && ownerBundle.getBundleContext() != null) {
			// remember - if a servlet, which is whiteboard-registered using different bundle accesses this attribute,
			// it'll get a BundleContext associated with ServletContextHelper (OsgiContextModel) instead of own
			// BundleContext!
			// this is not specified by Whiteboard specification, but by "Web Applications" specification, so we
			// have a little freedom here
			this.attributes.put(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT, ownerBundle.getBundleContext());
			// Spring variant:
			this.attributes.put(PaxWebConstants.CONTEXT_PARAM_SPRING_BUNDLE_CONTEXT, ownerBundle.getBundleContext());
		}

		// additional attributes set when OsgiContextModel was created
		this.attributes.putAll(osgiContextModel.getInitialContextAttributes());

		this.defaultSessionCookieConfig = defaultSessionCookieConfig;

		if (loader == null) {
			if (ownerBundle != null && ownerBundle.adapt(BundleWiring.class) != null) {
				// possibly in testing scenario
				this.classLoader = ownerBundle.adapt(BundleWiring.class).getClassLoader();
			} else {
				this.classLoader = null;
			}
		} else {
			this.classLoader = loader;
		}
	}

	public WebContainerContext getResolvedWebContainerContext() {
		return defaultWebContainerContext;
	}

	/**
	 * <p>A server wrapper that finds this {@link OsgiServletContext} to be highest ranked for given
	 * physical {@link ServletContext} should register it as OSGi service for given context path.</p>
	 *
	 * <p>This behavior is defined in "128.3.4 Publishing the Servlet Context" and concerns only WAB contexts, but
	 * I see no problems registering the current {@link OsgiServletContext} for given context path for all cases
	 * (including HttpService and Whiteboard).</p>
	 */
	public void register() {
		if (registration == null) {
			try {
				LOG.info("Registering {} as OSGi service for \"{}\" context path", this, osgiContextModel.getContextPath());

				// osgiContextModel has an "owner bundle":
				//  - when backed by ServletContextHelper from Whiteboard - always, even if such context is always
				//    "shared"
				//  - when backed by HttpContext registered as OSGi service - always, because there is some bundle
				//    registering the service
				//  - when backed by HttpContext passed to httpService.registerXXX(..., httpContext) - always, because
				//    it's the bundle associated with the httpService instance
				//  - when backed by shared (multi bundle) HttpContext passed to httpService.registerXXX(..., httpContext) -
				//    never, because we can't choose any particular bundle, so we choose pax-web-spi bundle

				Bundle bundle = osgiContextModel.getOwnerBundle();
				if (bundle == null) {
					bundle = FrameworkUtil.getBundle(OsgiServletContext.class);
				}

				BundleContext bc = bundle == null ? null : bundle.getBundleContext();
				if (bc != null) {
					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put(PaxWebConstants.SERVICE_PROPERTY_WEB_SYMBOLIC_NAME, bundle.getSymbolicName());
					properties.put(PaxWebConstants.SERVICE_PROPERTY_WEB_VERSION,
							bundle.getVersion() == null ? Version.emptyVersion.toString() : bundle.getVersion().toString());
					properties.put(PaxWebConstants.SERVICE_PROPERTY_WEB_SERVLETCONTEXT_PATH, osgiContextModel.getContextPath());
					properties.put(PaxWebConstants.SERVICE_PROPERTY_WEB_SERVLETCONTEXT_NAME, osgiContextModel.getName());
					registration = bc.registerService(ServletContext.class, this, properties);
				}
			} catch (Exception e) {
				LOG.error("Error registering {} as OSGi service: {}", this, e.getMessage(), e);
			}
		}
	}

	/**
	 * A server wrapper that finds this {@link OsgiServletContext} to no longer be highest ranked for given
	 * physical {@link ServletContext} should unregister it from OSGi service registry.
	 */
	public void unregister() {
		if (registration != null) {
			try {
				LOG.info("Unegistering {} as OSGi service for \"{}\" context path", this, osgiContextModel.getContextPath());

				registration.unregister();
			} catch (Exception e) {
				if (osgiContextModel.getOwnerBundle().getState() == Bundle.ACTIVE) {
					LOG.error("Error unregistering {} from OSGi registry: {}", this, e.getMessage(), e);
				}
			} finally {
				registration = null;
			}
		}
	}

	/**
	 * Everywhere the {@link OsgiServletContext} is created, we have to call
	 * {@link org.osgi.framework.BundleContext#ungetService(ServiceReference)} on the {@link WebContainerContext}
	 * reference when needed.
	 */
	public void releaseWebContainerContext() {
		osgiContextModel.releaseHttpContext(osgiContextModel.getOwnerBundle());
	}

	public Set<String> getAttributesToClearBeforeRestart() {
		return attributesToClearBeforeRestart;
	}

	/**
	 * This method removes the attributes set by {@link jakarta.servlet.ServletContainerInitializer SCIs} in previous
	 * restart of the context.
	 */
	public void clearAttributesFromPreviousCycle() {
		for (String name : attributesToClearBeforeRestart) {
			removeAttribute(name);
		}
		attributesToClearBeforeRestart.clear();

		attributes.clear();
		Bundle ownerBundle = osgiContextModel.getOwnerBundle();
		if (ownerBundle != null && ownerBundle.getBundleContext() != null) {
			this.attributes.put(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT, ownerBundle.getBundleContext());
			this.attributes.put(PaxWebConstants.CONTEXT_PARAM_SPRING_BUNDLE_CONTEXT, ownerBundle.getBundleContext());
		}

		// additional attributes set when OsgiContextModel was created
		this.attributes.putAll(osgiContextModel.getInitialContextAttributes());
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public ServletContextModel getServletContextModel() {
		return servletContextModel;
	}

	/**
	 * We have to be able to replace server-specific {@link ServletContext}, because we can't freely
	 * replace web elements in Undertow. In Undertow we have to recreate entire context, thus getting new
	 * {@link ServletContext}.
	 * Also Tomcat replaces a context (facade) inside StandardContext
	 * @param containerServletContext
	 */
	public void setContainerServletContext(ServletContext containerServletContext) {
		this.containerServletContext = containerServletContext;
	}

	public ServletContext getContainerServletContext() {
		return containerServletContext;
	}

	public String[] getWelcomeFiles() {
		return welcomeFiles;
	}

	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
	}

	public boolean isWelcomeFilesRedirect() {
		return welcomeFilesRedirect;
	}

	public void setWelcomeFilesRedirect(boolean redirect) {
		this.welcomeFilesRedirect = redirect;
	}

	/**
	 * This will mark the {@link OsgiServletContext} as a context that still alows for dynamic registration, but
	 * not if the listener implements {@link jakarta.servlet.ServletContextListener}
	 */
	public void noMoreServletContextListeners() {
		this.acceptsServletContextListeners = false;
	}

	/**
	 * Can {@link jakarta.servlet.ServletContextListener} be registered?
	 * @return
	 */
	public boolean acceptsServletContextListeners() {
		return acceptsServletContextListeners;
	}

	/**
	 * This method should be called with the associated context starts, so SCIs can register
	 * {@link jakarta.servlet.ServletContextListener} listeners
	 */
	public void allowServletContextListeners() {
		this.acceptsServletContextListeners = true;
	}

	// --- methods that throw UnsupportedOperationException (we can add filters/servlets/listeners, but using
	//     different context wrapper)

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		throw new UnsupportedOperationException("addFilter() is not supported.");
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		throw new UnsupportedOperationException("addFilter() is not supported.");
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException("addFilter() is not supported.");
	}

	@Override
	public void addListener(String className) {
		throw new UnsupportedOperationException("addListener() is not supported.");
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		throw new UnsupportedOperationException("addListener() is not supported.");
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException("addListener() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		throw new UnsupportedOperationException("addServlet() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		throw new UnsupportedOperationException("addServlet() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException("addServlet() is not supported.");
	}

	// --- methods that throw UnsupportedOperationException (always)

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createFilter() is not supported.");
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createListener() is not supported.");
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("createServlet() is not supported.");
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new IllegalStateException("declareRoles() is not supported.");
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new IllegalStateException("setInitParameter() is not supported.");
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		throw new IllegalStateException("setSessionTrackingModes() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new IllegalStateException("addJspFile() is not supported.");
	}

	// --- methods that are scoped to HttpContext/ServletContextHelper

	@Override
	public Object getAttribute(String name) {
		Object value = attributes.get(name);
		if (value == null) {
			// let's check real context - this delegation is important even if Whiteboard specification doesn't mention
			// this. The problem is that many components rely on internal Jetty/Tomcat/Undertow attributes that
			// may be set early during configuration
			value = containerServletContext.getAttribute(name);
		}
		return value;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		Set<String> keys = new LinkedHashSet<>();
		// first - containers attributes:
		for (Enumeration<String> e = containerServletContext.getAttributeNames(); e.hasMoreElements(); ) {
			keys.add(e.nextElement());
		}
		// scoped attributes
		keys.addAll(this.attributes.keySet());
		return Collections.enumeration(keys);
	}

	@Override
	public void setAttribute(String name, Object object) {
		// inspired by org.eclipse.jetty.server.handler.ContextHandler.Context.setAttribute
		Object oldValue = attributes.get(name);
		if (object == null) {
			attributes.remove(name);
		} else {
			attributes.put(name, object);
		}

		if (!attributeListeners.isEmpty()) {
			ServletContextAttributeEvent event = new ServletContextAttributeEvent(this, name, oldValue == null ? object : oldValue);

			for (ServletContextAttributeListener l : attributeListeners) {
				if (oldValue == null) {
					l.attributeAdded(event);
				} else if (object == null) {
					l.attributeRemoved(event);
				} else {
					l.attributeReplaced(event);
				}
			}
		}
	}

	@Override
	public void removeAttribute(String name) {
		// inspired by org.eclipse.jetty.server.handler.ContextHandler.Context.setAttribute
		Object oldValue = attributes.get(name);
		attributes.remove(name);

		if (oldValue != null && !attributeListeners.isEmpty()) {
			ServletContextAttributeEvent event = new ServletContextAttributeEvent(this, name, oldValue);

			for (ServletContextAttributeListener l : attributeListeners) {
				l.attributeRemoved(event);
			}
		}
	}

	/**
	 * Non standard method, so we can handle {@link ServletContextAttributeListener} listeners in special,
	 * per context way.
	 * @param eventListener
	 */
	public void addServletContextAttributeListener(ServletContextAttributeListener eventListener) {
		attributeListeners.add(eventListener);
	}

	/**
	 * Non standard method, so we can handle {@link ServletContextAttributeListener} listeners in special,
	 * per context way.
	 * @param eventListener
	 */
	public void removeServletContextAttributeListener(ServletContextAttributeListener eventListener) {
		attributeListeners.remove(eventListener);
	}

	// --- methods simply delegating to server-specific ServletContext. Backed by the Servlet Container.

	@Override
	public String getContextPath() {
		// Return the web context path of the Servlet Context.
		// to comply to Servlets specification, we have to return "" instead of "/"
		String contextPath = osgiContextModel.getContextPath();
		if ("/".equals(contextPath)) {
			return "";
		}
		return contextPath;
	}

	@Override
	public ServletContext getContext(String uripath) {
		return containerServletContext.getContext(uripath);
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return containerServletContext.getDefaultSessionTrackingModes();
	}

	@Override
	public int getEffectiveMajorVersion() {
		return containerServletContext.getEffectiveMajorVersion();
	}

	@Override
	public int getEffectiveMinorVersion() {
		return containerServletContext.getEffectiveMinorVersion();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return containerServletContext.getEffectiveSessionTrackingModes();
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		return containerServletContext.getFilterRegistration(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return containerServletContext.getFilterRegistrations();
	}

	@Override
	public int getMajorVersion() {
		return containerServletContext.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return containerServletContext.getMinorVersion();
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		return containerServletContext.getServletRegistration(servletName);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return containerServletContext.getServletRegistrations();
	}

	@Override
	public String getVirtualServerName() {
		return containerServletContext.getVirtualServerName();
	}

	@Override
	public String getServerInfo() {
		return containerServletContext.getServerInfo();
	}

	@Override
	public void log(String msg) {
		containerServletContext.log(msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		containerServletContext.log(message, throwable);
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		// according to 140.2.6 "Behavior of the Servlet Context", this method should delegate to the container,
		// but Pax Web does it better
		SessionCookieConfig scc = osgiContextModel.getSessionConfiguration().getSessionCookieConfig();
		if (scc == null) {
			// return default
			return defaultSessionCookieConfig;
		}
		return scc;
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		// according to 140.2.6 "Behavior of the Servlet Context", this method should return null
		// but I don't agree - I hope TCK won't mind
		return osgiContextModel.getJspConfigDescriptor();
	}

	// --- methods also delegating to server-specific ServletContext, but added in Servlet spec 4.0

	@Override
	public int getSessionTimeout() {
		// according to 140.2.6 "Behavior of the Servlet Context", this method should delegate to the container,
		// but Pax Web does it better
		return osgiContextModel.getSessionConfiguration().getSessionTimeout();
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		containerServletContext.setSessionTimeout(sessionTimeout);
	}

	@Override
	public String getRequestCharacterEncoding() {
		return containerServletContext.getRequestCharacterEncoding();
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		containerServletContext.setRequestCharacterEncoding(encoding);
	}

	@Override
	public String getResponseCharacterEncoding() {
		return containerServletContext.getResponseCharacterEncoding();
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		containerServletContext.setResponseCharacterEncoding(encoding);
	}

	// --- methods backed by the ServletContextHelper.

	@Override
	public String getMimeType(String file) {
		return getMimeType(defaultWebContainerContext, file);
	}

	public String getMimeType(WebContainerContext context, String file) {
		String mimeType = context.getMimeType(file);
		if (mimeType == null) {
			// let's check real context
			mimeType = containerServletContext.getMimeType(file);
		}
		return mimeType;
	}

	@Override
	public String getRealPath(String path) {
		return getRealPath(defaultWebContainerContext, path);
	}

	public String getRealPath(WebContainerContext context, String path) {
		return context.getRealPath(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return getResource(defaultWebContainerContext, path);
	}

	public URL getResource(WebContainerContext context, String path) throws MalformedURLException {
		// According to 128.6.3 Resource Lookup, in case of WAB, it is explicitly mentioned that
		// Bundle.findEntries() method must be used.
		// In Whiteboard/HttpService case, this delegates to ServletContextHelper/HttpContext, so it is clear that
		// for WABs, if no ServletContextHelper is available with higher rank, we should use some default
		// ServletContextHelper that acts accordingly to 128.6.3, but because default implementation of
		// ServletContextHelper is roughly compatible, we'll leave it as is - but still we need to support a case
		// when a WAB is installed, but then, higher-ranked ServletContextHelper is registered for WABs context path.

		// special jakarta.faces.FACELETS_LIBRARIES handling
		@SuppressWarnings("unchecked")
		Map<String, URL> mapping = (Map<String, URL>) osgiContextModel.getInitialContextAttributes()
				.get(PaxWebConstants.CONTEXT_PARAM_PAX_WEB_FACELETS_LIBRARIES);
		if (mapping != null && mapping.containsKey(path)) {
			return mapping.get(path);
		}

		return context.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return getResourceAsStream(defaultWebContainerContext, path);
	}

	public InputStream getResourceAsStream(WebContainerContext context, String path) {
		URL resource = context.getResource(path);
		if (resource != null) {
			try {
				return resource.openStream();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return getResourcePaths(defaultWebContainerContext, path);
	}

	public Set<String> getResourcePaths(WebContainerContext context, String path) {
		return context.getResourcePaths(path);
	}

	// --- methods backed by the OsgiContextModel (object "customized" by trackers from ServletContextHelper)

	@Override
	public String getInitParameter(String name) {
		// Mind the confusion between getInitParameter() method name and <context-param> web.xml element
		// these are actually the same
		String param = osgiContextModel.getContextParams().get(name);
		if (param == null) {
			// let's check real context, as there may be some interesting parameters which are container-specific
			param = containerServletContext.getInitParameter(name);
		}
		return param;
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		Set<String> keys = new LinkedHashSet<>();
		// first - containers parameters:
		for (Enumeration<String> e = containerServletContext.getInitParameterNames(); e.hasMoreElements(); ) {
			keys.add(e.nextElement());
		}
		// scoped attributes
		keys.addAll(osgiContextModel.getContextParams().keySet());
		return Collections.enumeration(keys);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// this should be narrowed to servlets registered only to the same ServletContextHelper
		// but we cede the path resolution to the container, so we can't actually tell...
		//
		// for example, if there are two servlets registered using two ServletContextHelpers that use the same
		// context path:
		//  - servlet1 mapped to /s1/*, associated with ServletContextHelper sch1
		//  - servlet2 mapped to /s2/*, associated with ServletContextHelper sch2
		// then getRequestDispatcher("/s1/a/b/c") will be eventually handled by servlet1, but only after
		// full org.eclipse.jetty.server.handler.ContextHandler.handle() processing again.
		// we don't have direct info that /s1/a/b/c should match servlet1 without calculating it

		return containerServletContext.getRequestDispatcher(path);
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		if (osgiContextModel.isWab()) {
			return containerServletContext.getNamedDispatcher(name);
		}

		ServletModel servletModel = servletContextModel.getServletNameMapping().get(name);
		if (servletModel == null) {
			return null;
		}

		if (servletModel.getContextModels().contains(osgiContextModel)) {
			// only if given servlet is registered using curent osgi context model (among others)
			return containerServletContext.getNamedDispatcher(name);
		}

		return null;
	}

	@Override
	public String getServletContextName() {
		return osgiContextModel.isWab() ? osgiContextModel.getDisplayName() : osgiContextModel.getName();
	}

	// --- methods dependent on which actual servlet/filter uses the context

	@Override
	public ClassLoader getClassLoader() {
		// According to Whiteboard specification, this method should return:
		//  - a classloader of the bundle registering ServletContextHelper service (generally)
		//  - a classloader of the bundle registering a Servlet/Filter service (when accesing from Servlet/Filter)
		// According to Web Applications specification (chapter 128 of the OSGi CMPN specification) there's ...
		// nothing important about classloaders, so we'll return Pax Web specific OsgiServletContextClassLoader
		// that delegates to a set of "reachable" bundles

		// The current state (to indicate differences between Pax Web and the specifications) is:
		// Whiteboard/HttpService specs:
		//  - OsgiScopedServletContext.getClassLoader() will return a classloader for a bundle registering
		//    the servlet/filter - fully conforming to Whiteboard specification
		//  - OsgiServletContext.getClassLoader() will return a OsgiServletContextClassLoader collecting few
		//    additional bundles (like pax-web-jsp or pax-web-jetty|tomcat|undertow)
		// WAB spec
		//  - both contexts will return the same OsgiServletContextClassLoader with bundles "reachable" from WAB

		return this.classLoader;
	}

	@Override
	public String toString() {
		return "OsgiServletContext{model=" + osgiContextModel + "}";
	}

}
