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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class wraps {@link OsgiServletContext} and is used to perform "dynamic registration operations" of
 * servlets, filters and listeners into single physical {@link ServletContext}. This wrapper is passed to
 * {@link javax.servlet.ServletContainerInitializer}s and allows dynamic configuration. While the context that's
 * wrapped doesn't allow this.</p>
 *
 * <p>Many {@link OsgiServletContext}s may point to single container-specific physical servlet context and many
 * {@link javax.servlet.ServletContainerInitializer}s may be used to register dynamic servlets/filters/listeners
 * through many {@link OsgiServletContext}s into single {@link ServletContext}. We need to keep track of the dynamic
 * web elements registered by multiple SCIs and actually register them later after all SCIs are invoked.</p>
 *
 * <p>2021-07-29 update: when implementing WebSockets (having problems in Undertow), I realized it's not that easy...
 * <ul>
 *     <li>registration of servlets and filters should always be allowed (chapter 4.4.3.5 of Servlets 4 spec),
 *         but for example Jetty calls {@code org.eclipse.jetty.servlet.ServletContextHandler.Context#checkDynamic()},
 *         Tomcat checks {@code !context.getState().equals(LifecycleState.STARTING_PREP)} and Undertow calls
 *         {@code io.undertow.servlet.spec.ServletContextImpl#ensureNotInitialized()}</li>
 *     <li>The basic, most "initial" way of adding listeners is through web.xml, web-fragment.xml or
 *         {@link javax.servlet.annotation.WebListener} and such listeners should always be able to add more
 *         listeners. Each container has it's own way to mark the point when new listeners can't be added:<ul>
 *             <li>Jetty calls {@code ContextHandler.Context#setEnabled(false)} when invoking first programmatic
 *                 listener, but new {@link javax.servlet.ServletContextListener} can't be added if
 *                 {@code org.eclipse.jetty.server.handler.ContextHandler.Context#_extendedListenerTypes} is
 *                 false</li>
 *             <li>Tomcat calls SCIs which can add {@link javax.servlet.ServletContextListener}, but then inside
 *                 {@code org.apache.catalina.core.StandardContext#listenerStart()}, {@code newServletContextListenerAllowed}
 *                 is set to {@code false}. Additionally all programmatic ServletContextListeners are marked as
 *                 "no pluggability listeners", so they can't add ANY new listener. ServletContextListeners added
 *                 in SCIs can add other listeners only.</li>
 *             <li>Undertow allows listeners to call {@link ServletContext#addListener} only if the adding
 *                 listener is NOT programmatic. But new {@link javax.servlet.ServletContextListener} can
 *                 never be added anyway</li>
 *         </ul></li>
 * </ul>
 * </p>
 */
public class OsgiDynamicServletContext implements ServletContext {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiDynamicServletContext.class);

	private final OsgiServletContext osgiContext;
	private final DynamicRegistrations registration;

	/** The collected names of the attributes which have to be cleared when the container is restarted */
	private final Set<String> attributesToClearBeforeRestart = new HashSet<>();

	public OsgiDynamicServletContext(OsgiServletContext osgiContext, DynamicRegistrations registration) {
		this.osgiContext = osgiContext;
		this.registration = registration;
	}

	/**
	 * This method has to be called after {@link SCIWrapper} calls wrapped
	 * {@link javax.servlet.ServletContainerInitializer}, so attributes potentially added by the SCI are removed
	 * when the context is restarted.
	 */
	public void rememberAttributesFromSCIs() {
		osgiContext.getAttributesToClearBeforeRestart().addAll(attributesToClearBeforeRestart);
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContext.getOsgiContextModel();
	}

	public ServletContext getContainerServletContext() {
		return osgiContext.getContainerServletContext();
	}

	// --- methods that throw UnsupportedOperationException (when context is started)

	// these methods allow OSGi-unaware registration of filters, servlets and listeners and according to
	// Servlet specification, should be used to configure "the context" from within ServletContainerInitializers
	// Without these methods, we can't have working JSPs/JSFs.

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		return registration.addFilter(osgiContext, filterName, className);
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		return registration.addFilter(osgiContext, filterName, filter);
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		return registration.addFilter(osgiContext, filterName, filterClass);
	}

	@Override
	public void addListener(String className) {
		registration.addListener(osgiContext, className);
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		registration.addListener(osgiContext, t);
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		registration.addListener(osgiContext, listenerClass);
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		return registration.addServlet(osgiContext, servletName, className);
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		return registration.addServlet(osgiContext, servletName, servlet);
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		return registration.addServlet(osgiContext, servletName, servletClass);
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException("declareRoles() is not supported.");
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		// called for example by
		// org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration$JerseyWebApplicationInitializer.onStartup()
		return osgiContext.getOsgiContextModel().getContextParams().putIfAbsent(name, value) == null;
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		throw new UnsupportedOperationException("setSessionTrackingModes() is not supported.");
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new UnsupportedOperationException("addJspFile() is not supported.");
	}

	@Override
	public Object getAttribute(String name) {
		return osgiContext.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return osgiContext.getAttributeNames();
	}

	@Override
	public void setAttribute(String name, Object object) {
		// remember the attribute added by SCI - it has to be removed when the context is restarted
		attributesToClearBeforeRestart.add(name);
		osgiContext.setAttribute(name, object);
	}

	@Override
	public void removeAttribute(String name) {
		osgiContext.removeAttribute(name);
	}

	@Override
	public String getContextPath() {
		return osgiContext.getContextPath();
	}

	@Override
	public ServletContext getContext(String uripath) {
		return osgiContext.getContext(uripath);
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return osgiContext.getDefaultSessionTrackingModes();
	}

	@Override
	public int getEffectiveMajorVersion() {
		return osgiContext.getEffectiveMajorVersion();
	}

	@Override
	public int getEffectiveMinorVersion() {
		return osgiContext.getEffectiveMinorVersion();
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return osgiContext.getEffectiveSessionTrackingModes();
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		return osgiContext.getFilterRegistration(filterName);
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return osgiContext.getFilterRegistrations();
	}

	@Override
	public int getMajorVersion() {
		return osgiContext.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return osgiContext.getMinorVersion();
	}

	@Override
	@SuppressWarnings({ "deprecation", "RedundantSuppression" })
	public Servlet getServlet(String name) throws ServletException {
		return osgiContext.getServlet(name);
	}

	@Override
	@SuppressWarnings({ "deprecation", "RedundantSuppression" })
	public Enumeration<String> getServletNames() {
		return osgiContext.getServletNames();
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		// this method returns a ServletRegistration by name. If a servlet is already regitered in the context
		// (wheter using Whiteboard, HttpService or even some internal Jetty/Tomcat/Undertow means), we should return
		// such registration.
		if (registration.getDynamicServletRegistrations().containsKey(servletName)) {
			return registration.getDynamicServletRegistrations().get(servletName);
		}

		// delegate to the container - you've been warned, as I'm not sure what will happen if you further configure
		// existing servlet ;)
		return osgiContext.getServletRegistration(servletName);
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		Map<String, ServletRegistration> regs = new HashMap<>();
		regs.putAll(osgiContext.getServletRegistrations());
		// override with our OSGi dynamic registrations
		regs.putAll(registration.getDynamicServletRegistrations());
		return regs;
	}

	@Override
	@SuppressWarnings({ "deprecation", "RedundantSuppression" })
	public Enumeration<Servlet> getServlets() {
		return osgiContext.getServlets();
	}

	@Override
	public String getVirtualServerName() {
		return osgiContext.getVirtualServerName();
	}

	@Override
	public String getServerInfo() {
		return osgiContext.getServerInfo();
	}

	@Override
	public void log(String msg) {
		osgiContext.log(msg);
	}

	@Override
	@SuppressWarnings({ "deprecation", "RedundantSuppression" })
	public void log(Exception exception, String msg) {
		osgiContext.log(exception, msg);
	}

	@Override
	public void log(String message, Throwable throwable) {
		osgiContext.log(message, throwable);
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return osgiContext.getSessionCookieConfig();
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return osgiContext.getJspConfigDescriptor();
	}

	@Override
	public int getSessionTimeout() {
		return osgiContext.getSessionTimeout();
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		osgiContext.setSessionTimeout(sessionTimeout);
	}

	@Override
	public String getRequestCharacterEncoding() {
		return osgiContext.getRequestCharacterEncoding();
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		osgiContext.setRequestCharacterEncoding(encoding);
	}

	@Override
	public String getResponseCharacterEncoding() {
		return osgiContext.getResponseCharacterEncoding();
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		osgiContext.setResponseCharacterEncoding(encoding);
	}

	@Override
	public String getMimeType(String file) {
		return osgiContext.getMimeType(file);
	}

	@Override
	public String getRealPath(String path) {
		return osgiContext.getRealPath(path);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return osgiContext.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return osgiContext.getResourceAsStream(path);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return osgiContext.getResourcePaths(path);
	}

	@Override
	public String getInitParameter(String name) {
		return osgiContext.getInitParameter(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return osgiContext.getInitParameterNames();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		return osgiContext.getRequestDispatcher(path);
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		return osgiContext.getNamedDispatcher(name);
	}

	@Override
	public String getServletContextName() {
		return osgiContext.getServletContextName();
	}

	@Override
	public ClassLoader getClassLoader() {
		return osgiContext.getClassLoader();
	}

}
