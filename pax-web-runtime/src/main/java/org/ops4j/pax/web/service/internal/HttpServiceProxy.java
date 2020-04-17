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

import java.util.Dictionary;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This is the proxy registered as {@link org.osgi.framework.Constants#SCOPE_BUNDLE bundle scoped}
 * {@link org.osgi.framework.ServiceFactory} for {@link org.osgi.service.http.HttpService}.</p>
 *
 * <p>Registered OSGi service should not be <em>replaced</em>, so when bundle will be stopping and
 * {@link org.osgi.framework.BundleContext#ungetService(ServiceReference)} will be called, the actual service
 * will be replaced by <em>stopped</em> service preventing further manipulation (like new servlet registration).
 * This is to prevent other (possible) threads to use no longer valid {@link org.osgi.service.http.HttpService}.</p>
 */
class HttpServiceProxy implements StoppableHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceProxy.class);

	// actual service that may be replaced by "stopped" http service preventing further registration of web components
	private volatile StoppableHttpService delegate;

	private final Bundle serviceBundle;

	public HttpServiceProxy(Bundle serviceBundle, final StoppableHttpService delegate) {
		NullArgumentException.validateNotNull(delegate, "Delegate");
		LOG.debug("HttpServiceProxy created for {} and {}", delegate, serviceBundle);

		this.serviceBundle = serviceBundle;
		this.delegate = delegate;
	}

	// --- StoppableHttpService

	@Override
	public void stop() {
		LOG.debug("Stopping http service: [{} -> {}]", this, delegate);
		final StoppableHttpService stopping = delegate;

		// PAXWEB-1077: ServletContext becomes unavailable on restart when using Whiteboard and CustomContexts

		// first replace the delegate
		delegate = new HttpServiceDisabled(serviceBundle);

		// then cleanup the delegate without a risk of problems happening at user side
		stopping.stop();
	}

	// --- container views

	@Override
	public <T extends PaxWebContainerView> T adapt(Class<T> type) {
		return delegate.adapt(type);
	}

	// --- transactional access to web container

	@Override
	public void begin(HttpContext context) {
		delegate.begin(context);
	}

	@Override
	public void end(HttpContext context) {
		delegate.end(context);
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

//	@Override
//	public void registerResources(final String alias, final String name,
//			final HttpContext httpContext) throws NamespaceException {
//		LOG.debug("Registering resource: [" + alias + "] -> " + name);
//		delegate.registerResources(alias, name, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerEventListener(EventListener, HttpContext) )
//	 */
//	@Override
//	public void registerEventListener(final EventListener listener,
//			final HttpContext httpContext) {
//		LOG.debug("Registering event listener [" + listener + "]");
//		delegate.registerEventListener(listener, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterEventListener(EventListener)
//	 */
//	@Override
//	public void unregisterEventListener(final EventListener listener) {
//		LOG.debug("Unregistering event listener [" + listener + "]");
//		delegate.unregisterEventListener(listener);
//	}
//
//	/**
//	 * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary,
//	 * HttpContext)
//	 */
//	@Override
//	public void registerFilter(final Filter filter, final String[] urlPatterns,
//			final String[] aliases, final Dictionary<String, ?> initParams,
//			final HttpContext httpContext) {
//		LOG.debug("Registering filter [" + filter + "]");
//		delegate.registerFilter(filter, urlPatterns, aliases, initParams, httpContext);
//	}
//
//	@Override
//	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
//			Dictionary<String, String> initParams, Boolean asyncSupported, HttpContext httpContext) {
//		LOG.debug("Registering filter [" + filter + "]");
//		delegate.registerFilter(filter, urlPatterns, servletNames, initParams, asyncSupported, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary,
//	 * HttpContext)
//	 */
//	@Override
//	public void registerFilter(Class<? extends Filter> filterClass,
//			String[] urlPatterns, String[] servletNames,
//			Dictionary<String, String> initParams, HttpContext httpContext) {
//		LOG.debug("Registering filter with class [" + filterClass + "]");
//		delegate.registerFilter(filterClass, urlPatterns, servletNames, initParams, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary,
//	 * HttpContext)
//	 */
//	@Override
//	public void registerFilter(Class<? extends Filter> filterClass,
//			String[] urlPatterns, String[] servletNames,
//			Dictionary<String, String> initParams, boolean asyncSupported, HttpContext httpContext) {
//		LOG.debug("Registering filter with class [" + filterClass + "]");
//		delegate.registerFilter(filterClass, urlPatterns, servletNames, initParams, asyncSupported, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterFilter(Filter)
//	 */
//	@Override
//	public void unregisterFilter(final Filter filter) {
//		LOG.debug("Unregistering filter [" + filter + "]");
//		delegate.unregisterFilter(filter);
//	}
//
//	/**
//	 * @see WebContainer#unregisterFilter(Filter)
//	 */
//	@Override
//	public void unregisterFilter(Class<? extends Filter> filterClass) {
//		LOG.debug("Unregistering filter [" + filterClass + "]");
//		delegate.unregisterFilter(filterClass);
//	}
//
//	/**
//	 * @see WebContainer#unregisterFilter(Filter)
//	 */
//	@Override
//	public void unregisterFilter(final String filterName) {
//		LOG.debug("Unregistering filter [" + filterName + "]");
//		delegate.unregisterFilter(filterName);
//	}
//
//	/**
//	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
//	 */
//	@Override
//	public void setContextParam(final Dictionary<String, ?> params,
//			final HttpContext httpContext) {
//		LOG.debug("Setting context paramters [" + params
//				+ "] for http context [" + httpContext + "]");
//		delegate.setContextParam(params, httpContext);
//	}
//
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public void setSessionTimeout(final Integer minutes,
//			final HttpContext httpContext) {
//		LOG.debug("Setting session timeout to " + minutes
//				+ " minutes for http context [" + httpContext + "]");
//		delegate.setSessionTimeout(minutes, httpContext);
//	}
//
//	@Override
//	public void setSessionCookieConfig(String domain, String name, Boolean httpOnly, Boolean secure, String path, Integer maxAge, HttpContext httpContext) {
//		LOG.debug(String.format("Setting session cookie configuration to: domain=%s, name=%s, http-only=%b, secure=%b, path=%s, max-age=%d",
//				domain, name, httpOnly, secure, path, maxAge));
//		delegate.setSessionCookieConfig(domain, name, httpOnly, secure, path, maxAge, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerJsps(String[], HttpContext)
//	 */
//	@Override
//	public void registerJsps(final String[] urlPatterns,
//			final HttpContext httpContext) {
//		LOG.debug("Registering jsps");
//		delegate.registerJsps(urlPatterns, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
//	 */
//	@Override
//	public void registerJsps(final String[] urlPatterns,
//			final Dictionary<String, ?> initParams,
//			final HttpContext httpContext) {
//		LOG.debug("Registering jsps");
//		delegate.registerJsps(urlPatterns, initParams, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterJsps(HttpContext)
//	 */
//	@Override
//	public void unregisterJsps(final HttpContext httpContext) {
//		LOG.debug("Unregistering jsps");
//		delegate.unregisterJsps(httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterJsps(HttpContext)
//	 */
//	@Override
//	public void unregisterJsps(final String[] urlPatterns,
//			final HttpContext httpContext) {
//		LOG.debug("Unregistering jsps");
//		delegate.unregisterJsps(urlPatterns, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
//	 */
//	@Override
//	public void registerErrorPage(final String error, final String location,
//			final HttpContext httpContext) {
//		LOG.debug("Registering error page [" + error + "]");
//		delegate.registerErrorPage(error, location, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
//	 */
//	@Override
//	public void unregisterErrorPage(final String error,
//			final HttpContext httpContext) {
//		LOG.debug("Unregistering error page [" + error + "]");
//		delegate.unregisterErrorPage(error, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
//	 */
//	@Override
//	public void registerWelcomeFiles(final String[] welcomeFiles,
//			final boolean redirect, final HttpContext httpContext) {
//		LOG.debug("Registering welcome files [" + Arrays.toString(welcomeFiles)
//				+ "]");
//		delegate.registerWelcomeFiles(welcomeFiles, redirect, httpContext);
//	}
//
//	/**
//	 * @see WebContainer#unregisterWelcomeFiles(String[], HttpContext)
//	 */
//	@Override
//	public void unregisterWelcomeFiles(final String[] welcomeFiles, final HttpContext httpContext) {
//		LOG.debug("Unregistering welcome files");
//		delegate.unregisterWelcomeFiles(welcomeFiles, httpContext);
//	}
//
//	@Override
//	public void registerLoginConfig(String authMethod, String realmName,
//			String formLoginPage, String formErrorPage, HttpContext httpContext) {
//		LOG.debug("Registering LoginConfig for realm [ " + realmName + " ]");
//		delegate.registerLoginConfig(authMethod, realmName, formLoginPage,
//				formErrorPage, httpContext);
//	}
//
//	@Override
//	public void unregisterLoginConfig(final HttpContext httpContext) {
//		LOG.debug("Unregistering LoginConfig");
//		delegate.unregisterLoginConfig(httpContext);
//	}
//
//	@Override
//	public void registerConstraintMapping(String constraintName, String mapping,
//			String url, String dataConstraint, boolean authentication,
//			List<String> roles, HttpContext httpContext) {
//		LOG.debug("Registering constraint mapping for [ " + constraintName
//				+ " ] ");
//		delegate.registerConstraintMapping(constraintName, mapping, url,
//				dataConstraint, authentication, roles, httpContext);
//
//	}
//
//	@Override
//	public void unregisterConstraintMapping(final HttpContext httpContext) {
//		LOG.debug("Unregister constraint mapping");
//		delegate.unregisterConstraintMapping(httpContext);
//	}
//
//	@Override
//	public SharedWebContainerContext getDefaultSharedHttpContext() {
//		return delegate.getDefaultSharedHttpContext();
//	}
//
//	@Override
//	public void registerServletContainerInitializer(
//			ServletContainerInitializer servletContainerInitializer,
//			Class<?>[] classes, final HttpContext httpContext) {
//		delegate.registerServletContainerInitializer(
//				servletContainerInitializer, classes, httpContext);
//	}
//
//	@Override
//	public void unregisterServletContainerInitializer(HttpContext httpContext) {
//		delegate.unregisterServletContainerInitializer(httpContext);
//	}
//
//	@Override
//	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
//		delegate.registerJettyWebXml(jettyWebXmlURL, httpContext);
//	}
//
//	@Override
//	public void registerJspServlet(String[] urlPatterns,
//			HttpContext httpContext, String jspFile) {
//		delegate.registerJspServlet(urlPatterns, httpContext, jspFile);
//	}
//
//	@Override
//	public void registerJspServlet(String[] urlPatterns,
//			Dictionary<String, ?> initParams, HttpContext httpContext,
//			String jspFile) {
//		delegate.registerJspServlet(urlPatterns, initParams, httpContext,
//				jspFile);
//	}
//
//	@Override
//	public void setConnectorsAndVirtualHosts(List<String> connectors, List<String> virtualHosts, HttpContext httpContext) {
//		delegate.setConnectorsAndVirtualHosts(connectors, virtualHosts, httpContext);
//	}
//
//	@Override
//	public void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext) {
//		delegate.registerJspConfigTagLibs(tagLibLocation, tagLibUri, httpContext);
//	}
//
//	@Override
//	public void registerJspConfigPropertyGroup(List<String> includeCodes,
//			List<String> includePreludes, List<String> urlPatterns, Boolean elIgnored, Boolean scriptingInvalid,
//			Boolean isXml, HttpContext httpContext) {
//		delegate.registerJspConfigPropertyGroup(includeCodes, includePreludes, urlPatterns, elIgnored, scriptingInvalid, isXml, httpContext);
//	}
//
//	@Override
//	public void registerWebSocket(Object webSocket, HttpContext httpContext) {
//		delegate.registerWebSocket(webSocket, httpContext);
//	}
//
//	@Override
//	public void unregisterWebSocket(Object webSocket, HttpContext httpContext) {
//		delegate.unregisterWebSocket(webSocket, httpContext);
//	}
//
//	@Override
//	public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator) {
//		return delegate.calculateRequestInfoDTO(path, iterator);
//	}
//
//	@Override
//	public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
//		return delegate.createWhiteboardRuntimeDTO(iterator);
//	}
//
//	@Override
//	public WebContainerDTO getWebcontainerDTO() {
//		return delegate.getWebcontainerDTO();
//	}

}
