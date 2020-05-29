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
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.wiring.BundleWiring;

/**
 * <p>Implementation of {@link ServletContext} for the contract described in 140.2.6 "Behavior of the Servlet Context"
 * from OSGi CMPN R7 Whiteboard Service specification. That's the 1:1 mapping with single
 * {@link org.osgi.service.http.context.ServletContextHelper} (or {@link org.osgi.service.http.HttpContext}).</p>
 *
 * <p>When handling single servlet (or generally filters+servlet chain), we have to provide special facade for
 * {@link ServletContext} from Servlet API in the form of {@link OsgiServletContext}. And during actual request
 * processing, we need yet another facade to provide required {@link ServletContext#getClassLoader()} behavior.</p>
 */
public class OsgiServletContext implements ServletContext {

	private ServletContext containerServletContext;
	private final OsgiContextModel osgiContextModel;
	private final ServletContextModel servletContextModel;

	/**
	 * {@link WebContainerContext} obtained from {@link OsgiContextModel} in the context of the bundle registering
	 * the <em>context</em> ({@link org.osgi.service.http.HttpContext} or
	 * {@link org.osgi.service.http.context.ServletContextHelper}). This {@link WebContainerContext} is used by
	 * default, but during actual request processing, different {@link WebContainerContext} should be used - the one
	 * obtained (e.g., from OSGi service registry, {@link org.osgi.framework.ServiceFactory}) in the context of a
	 * bundle that registered target {@link Servlet}.
	 */
	private final WebContainerContext defaultWebContainerContext;

	private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	/**
	 * Constructor called when {@link OsgiContextModel} is passed to given
	 * {@link org.ops4j.pax.web.service.spi.ServerController}. We still can't grab an instance of
	 * {@link WebContainerContext} in the constructor, because it has to be resolved in the context of particular
	 * web request invocation - target {@link Servlet} determines the {@link OsgiContextModel} used and the
	 * {@link org.osgi.framework.Bundle} associated with the target {@link Servlet} is used to resolve actual
	 * {@link WebContainerContext}.
	 *
	 * @param containerServletContext
	 * @param osgiContextModel
	 * @param servletContextModel
	 */
	public OsgiServletContext(ServletContext containerServletContext, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel) {
		this.containerServletContext = containerServletContext;
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;

		this.defaultWebContainerContext = osgiContextModel.resolveHttpContext(osgiContextModel.getOwnerBundle());
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	/**
	 * We have to be able to replace server-specific {@link ServletContext}, because we can't freely
	 * replace web elements in Undertow. In Undertow we have to recreate entire context, thus getting new
	 * {@link ServletContext}.
	 * @param containerServletContext
	 */
	public void setContainerServletContext(ServletContext containerServletContext) {
		this.containerServletContext = containerServletContext;
	}

	// --- methods that throw UnsupportedOperationException

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
		throw new UnsupportedOperationException("declareRoles() is not supported.");
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new UnsupportedOperationException("setInitParameter() is not supported.");
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		throw new UnsupportedOperationException("setSessionTrackingModes() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new UnsupportedOperationException("addJspFile() is not supported.");
	}

	// --- methods that are scoped to HttpContext/ServletContextHelper

	@Override
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(this.attributes.keySet());
	}

	@Override
	public void setAttribute(String name, Object object) {
		attributes.put(name, object);
		// TODO: fire listeners
	}

	@Override
	public void removeAttribute(String name) {
		attributes.remove(name);
		// TODO: fire listeners
	}

	// --- methods simply delegating to server-specific ServletContext. Backed by the Servlet Container.

	@Override
	public String getContextPath() {
		// Return the web context path of the Servlet Context.
		// This takes into account the osgi.http.whiteboard.context.path of the Servlet Context Helper and the path
		// of the Http runtime.
		// But this is the same
		return containerServletContext.getContextPath();
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
	@SuppressWarnings("deprecation")
	public Servlet getServlet(String name) throws ServletException {
		return containerServletContext.getServlet(name);
	}

	@Override
	@SuppressWarnings("deprecation")
	public Enumeration<String> getServletNames() {
		return containerServletContext.getServletNames();
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
	@SuppressWarnings("deprecation")
	public Enumeration<Servlet> getServlets() {
		return containerServletContext.getServlets();
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
	@SuppressWarnings("deprecation")
	public void log(Exception exception, String msg) {
		containerServletContext.log(exception, msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		containerServletContext.log(message, throwable);
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return containerServletContext.getSessionCookieConfig();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		// according to 140.2.6 "Behavior of the Servlet Context", this method should return null
		// but I don't agree
		return containerServletContext.getJspConfigDescriptor();
	}

	// --- methods also delegating to server-specific ServletContext, but added in Servlet spec 4.0

	@Override
	public int getSessionTimeout() {
		return containerServletContext.getSessionTimeout();
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

	String getMimeType(WebContainerContext context, String file) {
		return context.getMimeType(file);
	}

	@Override
	public String getRealPath(String path) {
		return getRealPath(defaultWebContainerContext, path);
	}

	String getRealPath(WebContainerContext context, String path) {
		return context.getRealPath(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return getResource(defaultWebContainerContext, path);
	}

	URL getResource(WebContainerContext context, String path) throws MalformedURLException {
		return context.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return getResourceAsStream(defaultWebContainerContext, path);
	}

	InputStream getResourceAsStream(WebContainerContext context, String path) {
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

	Set<String> getResourcePaths(WebContainerContext context, String path) {
		return context.getResourcePaths(path);
	}


	// --- methods backed by the OsgiContextModel (object "customized" by trackers from ServletContextHelper)

	@Override
	public String getInitParameter(String name) {
		// Mind the confusion between getInitParameter() method name and <context-param> web.xml element
		// these are actually the same
		return osgiContextModel.getContextParams().get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(osgiContextModel.getContextParams().keySet());
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		// TOCHECK: this should be narrowed to servlets registered only to the same ServletContextHelper
		//          but we cede the path resolution to the container, so we can't actually tell...
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
		return osgiContextModel.getName();
	}

	// --- methods dependent on which actual servlet/filter uses the context

	@Override
	public ClassLoader getClassLoader() {
		// at Servlet (or Filter) level, this method returns classLoader of a bundle registering given Servlet
		// (or Filter). Here, it's a bundle registering ServletContextHelper (or HttpContext)
		return osgiContextModel.getOwnerBundle().adapt(BundleWiring.class).getClassLoader();
	}

}
