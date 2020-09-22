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
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <em>Disabled</em> {@link org.osgi.service.http.HttpService} is set into actually registered, bundle-scoped
 * Http Service when the bundle stops.
 */
class HttpServiceDisabled implements WebContainer {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceDisabled.class);

	final Bundle serviceBundle;

	HttpServiceDisabled(Bundle serviceBundle) {
		this.serviceBundle = serviceBundle;
		LOG.debug("Changing HttpService state to " + this);
	}

	// --- container views

	@Override
	public <T extends PaxWebContainerView> T adapt(Class<T> type) {
		return null;
	}

	// --- transactional access to web container

	@Override
	public void begin(HttpContext httpContext) {
	}

	@Override
	public void end(HttpContext httpContext) {
	}

	// --- different methods used to retrieve HttpContext

	@Override
	public HttpContext createDefaultHttpContext() {
		LOG.warn("Http service has already been stopped");
		return null;
	}

	@Override
	public HttpContext createDefaultHttpContext(String contextID) {
		LOG.warn("Http service has already been stopped");
		return null;
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext() {
		return null;
	}

	@Override
	public MultiBundleWebContainerContext createDefaultSharedHttpContext(String contextId) {
		return null;
	}

	// --- methods used to register a Servlet - with more options than in original HttpService.registerServlet()

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams, HttpContext httpContext)
			throws ServletException, NamespaceException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext)
			throws ServletException, NamespaceException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns, Dictionary<String, String> initParams,
			Integer loadOnStartup, Boolean asyncSupported, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass, String[] urlPatterns,
			Dictionary<String, String> initParams, Integer loadOnStartup, Boolean asyncSupported,
			MultipartConfigElement multiPartConfig, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	// --- methods used to unregister a Servlet

	@Override
	public void unregister(final String alias) {
		LOG.warn("");
	}

	@Override
	public void unregisterServlet(final Servlet servlet) {
		LOG.warn("");
	}

	@Override
	public void unregisterServlet(String servletName) {
		LOG.warn("");
	}

	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		LOG.warn("");
	}

	// --- methods used to register resources

	@Override
	public void registerResources(String alias, String name, HttpContext context) throws NamespaceException {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to register a Filter

	@Override
	public void registerFilter(Filter filter, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerFilter(Filter filter, String filterName, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String[] urlPatterns, String[] servletNames,
			Dictionary<String, String> initParams, HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	@Override
	public void registerFilter(Class<? extends Filter> filterClass, String filterName, String[] urlPatterns,
			String[] servletNames, Dictionary<String, String> initParams, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		LOG.warn("");
	}

	// --- methods used to unregister a Filter

	@Override
	public void unregisterFilter(Filter filter) {
		LOG.warn("");
	}

	@Override
	public void unregisterFilter(String filterName) {
		LOG.warn("");
	}

	@Override
	public void unregisterFilters(Class<? extends Filter> filterClass) {
		LOG.warn("");
	}

	// --- methods used to register an EventListener

	@Override
	public void registerEventListener(final EventListener listener, final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to unregister an EventListener

	@Override
	public void unregisterEventListener(final EventListener listener) {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to register welcome pages

	@Override
	public void registerWelcomeFiles(String[] welcomeFiles, boolean redirect, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to unregister welcome pages

	@Override
	public void unregisterWelcomeFiles(String[] welcomeFiles, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to register error pages

	@Override
	public void registerErrorPage(String error, String location, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerErrorPages(String[] errors, String location, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// --- methods used to unregister error pages

	@Override
	public void unregisterErrorPage(String error, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void unregisterErrorPages(String[] errors, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to register / configure JSPs

	@Override
	public void registerJsps(String[] urlPatterns, Dictionary<String, String> initParams, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspServlet(String jspFile, String[] urlPatterns, Dictionary<String, String> initParams, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspConfigTagLibs(String tagLibLocation, String tagLibUri, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspConfigTagLibs(Collection<TaglibDescriptor> tagLibs, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspConfigPropertyGroup(List<String> includeCodas, List<String> includePreludes,
			List<String> urlPatterns, Boolean elIgnored, Boolean scriptingInvalid, Boolean isXml,
			HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspConfigPropertyGroup(JspPropertyGroupDescriptor descriptor, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to unregister / unconfigure JSPs

	@Override
	public void unregisterJsps(HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void unregisterJspServlet(String jspFile, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to register ServletContainerInitializers

	@Override
	public void registerServletContainerInitializer(ServletContainerInitializer initializer, Class<?>[] classes, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to unregister ServletContainerInitializers

	@Override
	public void unregisterServletContainerInitializer(ServletContainerInitializer initializer, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to configure session

	@Override
	public void setSessionTimeout(Integer minutes, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void setSessionCookieConfig(String domain, String name, Boolean httpOnly, Boolean secure, String path,
			Integer maxAge, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void setSessionCookieConfig(SessionCookieConfig config, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	// methods used to alter context init parameters

	@Override
	public void setContextParams(Dictionary<String, Object> params, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}







//	/**
//	 * @see WebContainer#registerLoginConfig(String, String, String, String, HttpContext)
//	 */
//	@Override
//	public void registerLoginConfig(String authMethod, String realmName,
//									String formLoginPage, String formErrorPage, HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	/**
//	 * @see WebContainer#unregisterLoginConfig(HttpContext)
//	 */
//	@Override
//	public void unregisterLoginConfig(HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	/**
//	 * @see WebContainer#registerConstraintMapping(java.lang.String,
//	 * java.lang.String, java.lang.String, java.lang.String, boolean,
//	 * java.util.List, org.osgi.service.http.HttpContext)
//	 */
//	@Override
//	public void registerConstraintMapping(String constraintName,
//										  String mapping, String url, String dataConstraint,
//										  boolean authentication, List<String> roles, HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	/**
//	 * @see WebContainer#unregisterConstraintMapping(HttpContext)
//	 */
//	@Override
//	public void unregisterConstraintMapping(HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	@Override
//	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}

//	@Override
//	public void setConnectorsAndVirtualHosts(List<String> connectors, List<String> virtualHosts,
//											 HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	@Override
//	public void registerWebSocket(Object webSocket, HttpContext httpContext) {
//		LOG.warn("Http service has already been stopped");
//	}
//
//	@Override
//	public void unregisterWebSocket(Object webSocket, HttpContext httpContext) {
//		LOG.warn("Http service has already been stoped");
//	}
//
//	@Override
//	public RequestInfoDTO calculateRequestInfoDTO(String path, Iterator<WhiteboardElement> iterator) {
//		LOG.warn("Http service has already been stoped");
//		// FIXME check if null valid
//		return null;
//	}
//
//	@Override
//	public RuntimeDTO createWhiteboardRuntimeDTO(Iterator<WhiteboardElement> iterator) {
//		LOG.warn("Http service has already been stoped");
//		// FIXME check if null valid
//		return null;
//	}

	@Override
	public String toString() {
		return super.toString() + " for bundle " + serviceBundle;
	}

//    @Override
//    public WebContainerDTO getWebcontainerDTO() {
//        LOG.warn("Http service has already been stoped");
//        return null;
//    }

}
