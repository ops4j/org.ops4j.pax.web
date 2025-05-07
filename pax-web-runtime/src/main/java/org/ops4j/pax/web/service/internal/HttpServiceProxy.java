/*
 * Copyright 2007 Alin Dreghiciu.
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
import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.descriptor.JspPropertyGroupDescriptor;
import jakarta.servlet.descriptor.TaglibDescriptor;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.service.http.HttpContext;
import org.ops4j.pax.web.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This is the proxy registered as {@link org.osgi.framework.Constants#SCOPE_BUNDLE bundle scoped}
 * {@link org.osgi.framework.ServiceFactory} for {@link org.ops4j.pax.web.service.http.HttpService}.</p>
 *
 * <p>Registered OSGi service should not be <em>replaced</em>, so when bundle will be stopping and
 * {@link org.osgi.framework.BundleContext#ungetService(ServiceReference)} will be called, the actual service
 * will be replaced by <em>stopped</em> service preventing further manipulation (like new servlet registration).
 * This is to prevent other (possible) threads to use no longer valid {@link org.ops4j.pax.web.service.http.HttpService}.</p>
 */
class HttpServiceProxy implements WebContainer, StoppableHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceProxy.class);

	// actual service that may be replaced by "stopped" http service preventing further registration of web components
	private volatile WebContainer delegate;

	private final Bundle serviceBundle;

	HttpServiceProxy(Bundle serviceBundle, final WebContainer delegate) {
		LOG.debug("HttpServiceProxy created for {}", delegate);

		this.serviceBundle = serviceBundle;
		this.delegate = delegate;
	}

	// --- StoppableHttpService

	@Override
	public void stop() {
		if (delegate instanceof StoppableHttpService) {
			LOG.debug("Stopping http service: {}", delegate);
			StoppableHttpService stopping;
			synchronized (this) {
				stopping = (StoppableHttpService) delegate;

				// PAXWEB-1077: ServletContext becomes unavailable on restart when using Whiteboard and CustomContexts

				// first replace the delegate
				delegate = new HttpServiceDisabled(serviceBundle);
			}

			// then cleanup the delegate without a risk of problems happening at user side
			if (stopping != null) {
				stopping.stop();
			}
		} else {
			LOG.warn("Http service has already been stopped");
		}
	}

	// --- container views

	@Override
	public <T extends PaxWebContainerView> T adapt(Class<T> type) {
		return delegate.adapt(type);
	}

	// --- different methods used to retrieve HttpContext

	@Override
	public HttpContext createDefaultHttpContext() {
		return delegate.createDefaultHttpContext();
	}

	@Override
	public HttpContext createDefaultHttpContext(String contextId) {
		return delegate.createDefaultHttpContext(contextId);
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext() {
		return delegate.createDefaultSharedHttpContext();
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext(String contextId) {
		return delegate.createDefaultSharedHttpContext(contextId);
	}

	// --- methods used to register a Servlet - with more options than in original HttpService.registerServlet()

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams, HttpContext httpContext)
			throws ServletException, NamespaceException {
		delegate.registerServlet(alias, servlet, initParams, httpContext);
	}

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext)
			throws ServletException, NamespaceException {
		delegate.registerServlet(alias, servlet, initParams, loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servlet, urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servlet, urlPatterns, initParams, loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servlet, servletName, urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servlet, servletName, urlPatterns, initParams, loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servlet, servletName, urlPatterns, initParams, loadOnStartup, asyncSupported,
				multiPartConfig, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servletClass, urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servletClass, urlPatterns, initParams, loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns, Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported, MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servletClass, urlPatterns, initParams, loadOnStartup, asyncSupported,
				multiPartConfig, httpContext);
	}

	// --- methods used to unregister a Servlet

	@Override
	public void unregister(String alias) {
		delegate.unregister(alias);
	}

	@Override
	public void unregisterServlet(Servlet servlet) {
		delegate.unregisterServlet(servlet);
	}

	@Override
	public void unregisterServlet(String servletName) {
		delegate.unregisterServlet(servletName);

	}

	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		delegate.unregisterServlets(servletClass);
	}

	// --- HttpService.registerResources()

	@Override
	public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
		delegate.registerResources(alias, name, context);
	}

	// --- methods used to register a Filter

	@Override
	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		delegate.registerFilter(filter, urlPatterns, servletNames, initParams, httpContext);
	}

	@Override
	public void registerFilter(Filter filter, String filterName, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		delegate.registerFilter(filter, filterName, urlPatterns, servletNames, initParams,
				asyncSupported, httpContext);
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		delegate.registerFilter(filterClass, urlPatterns, servletNames, initParams, httpContext);
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String filterName, String[] urlPatterns,
			String[] servletNames, Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		delegate.registerFilter(filterClass, filterName, urlPatterns, servletNames, initParams,
				asyncSupported, httpContext);
	}

	// --- methods used to unregister a Filter

	@Override
	public void unregisterFilter(Filter filter) {
		delegate.unregisterFilter(filter);
	}

	@Override
	public void unregisterFilter(String filterName) {
		delegate.unregisterFilter(filterName);
	}

	@Override
	public void unregisterFilters(Class<? extends Filter> filterClass) {
		delegate.unregisterFilters(filterClass);
	}

	// --- methods used to register an EventListener

	@Override
	public void registerEventListener(final EventListener listener, final HttpContext httpContext) {
		delegate.registerEventListener(listener, httpContext);
	}

	// --- methods used to unregister an EventListener

	@Override
	public void unregisterEventListener(final EventListener listener) {
		delegate.unregisterEventListener(listener);
	}

	// --- methods used to register welcome pages

	@Override
	public void registerWelcomeFiles(String[] welcomeFiles, boolean redirect, HttpContext httpContext) {
		delegate.registerWelcomeFiles(welcomeFiles, redirect, httpContext);
	}

	// --- methods used to unregister welcome pages

	@Override
	public void unregisterWelcomeFiles(String[] welcomeFiles, HttpContext httpContext) {
		delegate.unregisterWelcomeFiles(welcomeFiles, httpContext);
	}

	// --- methods used to register error pages

	@Override
	public void registerErrorPage(String error, String location, HttpContext httpContext) {
		delegate.registerErrorPage(error, location, httpContext);
	}

	@Override
	public void registerErrorPages(String[] errors, String location, HttpContext httpContext) {
		delegate.registerErrorPages(errors, location, httpContext);
	}

	// --- methods used to unregister error pages

	@Override
	public void unregisterErrorPage(String error, HttpContext httpContext) {
		delegate.unregisterErrorPage(error, httpContext);
	}

	@Override
	public void unregisterErrorPages(String[] errors, HttpContext httpContext) {
		delegate.unregisterErrorPages(errors, httpContext);
	}

	// methods used to register / configure JSPs

	@Override
	public void registerJsps(String[] urlPatterns, Dictionary<String, String> initParams, HttpContext httpContext) {
		delegate.registerJsps(urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerJspServlet(String jspFile, String[] urlPatterns, Dictionary<String, String> initParams, HttpContext httpContext) {
		delegate.registerJspServlet(jspFile, urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext) {
		delegate.registerJspConfigTagLibs(tagLibLocation, tagLibUri, httpContext);
	}

	@Override
	public void registerJspConfigTagLibs(Collection<TaglibDescriptor> tagLibs, HttpContext httpContext) {
		delegate.registerJspConfigTagLibs(tagLibs, httpContext);
	}

	@Override
	public void registerJspConfigPropertyGroup(List<String> includeCodas, List<String> includePreludes,
			List<String> urlPatterns, Boolean elIgnored, Boolean scriptingInvalid, Boolean isXml,
			HttpContext httpContext) {
		delegate.registerJspConfigPropertyGroup(includeCodas, includePreludes, urlPatterns,
				elIgnored, scriptingInvalid, isXml, httpContext);
	}

	@Override
	public void registerJspConfigPropertyGroup(JspPropertyGroupDescriptor descriptor, HttpContext httpContext) {
		delegate.registerJspConfigPropertyGroup(descriptor, httpContext);
	}

	// methods used to unregister / unconfigure JSPs

	@Override
	public void unregisterJsps(HttpContext httpContext) {
		delegate.unregisterJsps(httpContext);
	}

	@Override
	public void unregisterJspServlet(String jspFile, HttpContext httpContext) {
		delegate.unregisterJspServlet(jspFile, httpContext);
	}

	// methods used to register ServletContainerInitializers

	@Override
	public void registerServletContainerInitializer(ServletContainerInitializer initializer, Class<?>[] classes, HttpContext httpContext) {
		delegate.registerServletContainerInitializer(initializer, classes, httpContext);
	}

	// methods used to unregister ServletContainerInitializers

	@Override
	public void unregisterServletContainerInitializer(ServletContainerInitializer initializer, HttpContext httpContext) {
		delegate.unregisterServletContainerInitializer(initializer, httpContext);
	}

	// methods used to configure session

	@Override
	public void setSessionTimeout(Integer minutes, HttpContext httpContext) {
		delegate.setSessionTimeout(minutes, httpContext);
	}

	@Override
	public void setSessionCookieConfig(String domain, String name, Boolean httpOnly, Boolean secure, String path,
			Integer maxAge, HttpContext httpContext) {
		delegate.setSessionCookieConfig(domain, name, httpOnly, secure, path, maxAge, httpContext);
	}

	@Override
	public void setSessionCookieConfig(SessionCookieConfig config, HttpContext httpContext) {
		delegate.setSessionCookieConfig(config, httpContext);
	}

	// methods used to alter context init parameters

	@Override
	public void setContextParams(Dictionary<String, Object> params, HttpContext httpContext) {
		delegate.setContextParams(params, httpContext);
	}

	// methods used to register annotated web socket endpoints

	@Override
	public void registerWebSocket(Object webSocket, HttpContext httpContext) {
		delegate.registerWebSocket(webSocket, httpContext);
	}

	// methods used to unregister annotated web socket endpoints

	@Override
	public void unregisterWebSocket(Object webSocket, HttpContext httpContext) {
		delegate.unregisterWebSocket(webSocket, httpContext);
	}

	// #1823: methods used to configure security (login configuration and security constraints)

	@Override
	public void registerLoginConfig(String authMethod, String realmName, String formLoginPage, String formErrorPage, HttpContext httpContext) {
		delegate.registerLoginConfig(authMethod, realmName, formLoginPage, formErrorPage, httpContext);
	}

	@Override
	public void registerConstraintMapping(String constraintName, String httpMethod, String url, String dataConstraint, boolean authentication, List<String> roles, HttpContext httpContext) {
		delegate.registerConstraintMapping(constraintName, httpMethod, url, dataConstraint, authentication, roles, httpContext);
	}

	// #1823: methods used to un-configure security (login configuration and security constraints)

	@Override
	public void unregisterLoginConfig(HttpContext httpContext) {
		delegate.unregisterLoginConfig(httpContext);
	}

	@Override
	public void unregisterConstraintMapping(HttpContext httpContext) {
		delegate.unregisterConstraintMapping(httpContext);
	}

	@Override
	public String toString() {
		return "Proxy for " + delegate.toString();
	}

}
