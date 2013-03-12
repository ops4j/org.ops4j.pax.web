/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.internal;

import java.net.URL;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceStopped implements StoppableHttpService {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceStopped.class);

	HttpServiceStopped() {
		LOG.debug("Changing HttpService state to " + this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.http.HttpService#registerServlet(java.lang.String,
	 * javax.servlet.Servlet, java.util.Dictionary,
	 * org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(final String alias, final Servlet servlet,
			@SuppressWarnings("rawtypes") final Dictionary initParams,
			final HttpContext httpContext) throws ServletException,
			NamespaceException {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerServlet(String alias, Servlet servlet,
			Dictionary initParams, Integer loadOnStartup,
			Boolean asyncSupported, HttpContext httpContext)
			throws ServletException, NamespaceException {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerResources(final String alias, final String name,
			final HttpContext httpContext) throws NamespaceException {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void unregister(final String alias) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public HttpContext createDefaultHttpContext() {
		LOG.warn("Http service has already been stopped");
		return null;
	}

	@Override
	public void stop() {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see WebContainer#registerServlet(Servlet, String[], Dictionary,
	 *      HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
			final String[] urlPatterns, final Dictionary<String, ?> initParams,
			final HttpContext httpContext) throws ServletException {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see WebContainer#registerServlet(javax.servlet.Servlet, String,
	 *      String[],java.util.Dictionary,org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
			final String servletName, final String[] urlPatterns,
			final Dictionary<String, ?> initParams,
			final HttpContext httpContext) throws ServletException {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see WebContainer#unregisterServlet(Servlet)
	 */
	@Override
	public void unregisterServlet(final Servlet servlet) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see org.ops4j.pax.web.service.WebContainer#registerServlet(java.lang.Class,
	 *      java.lang.String[], java.util.Dictionary,
	 *      org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			HttpContext httpContext) throws ServletException {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns,
			Dictionary<String, ?> initParams, Integer loadOnStartup,
			Boolean asyncSupported, HttpContext httpContext)
			throws ServletException {
		this.registerServlet(servlet, urlPatterns, initParams, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		this.registerServlet(servlet, servletName, urlPatterns, initParams,
				httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		this.registerServlet(servletClass, urlPatterns, initParams, httpContext);
	}

	/**
	 * Does nothing.
	 * 
	 * @see org.ops4j.pax.web.service.WebContainer#unregisterServlets(java.lang.Class)
	 */
	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see WebContainer#registerEventListener(java.util.EventListener,
	 *      HttpContext)
	 */
	@Override
	public void registerEventListener(final EventListener listener,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * Does nothing.
	 * 
	 * @see WebContainer#unregisterEventListener(java.util.EventListener)
	 */
	@Override
	public void unregisterEventListener(final EventListener listener) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary,
	 *      HttpContext)
	 */
	@Override
	public void registerFilter(final Filter filter, final String[] urlPatterns,
			final String[] servletNames,
			final Dictionary<String, ?> initParams,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterFilter(Filter)
	 */
	@Override
	public void unregisterFilter(final Filter filter) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
	 */
	@Override
	public void setContextParam(final Dictionary<String, ?> params,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSessionTimeout(final Integer minutes,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerJsps(String[], HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final Dictionary<String, ?> initParams,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
	 */
	@Override
	public void registerErrorPage(final String error, final String location,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
	 */
	@Override
	public void unregisterErrorPage(final String error,
			final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
	 */
	@Override
	public void registerWelcomeFiles(final String[] welcomeFiles,
			final boolean rediect, final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterWelcomeFiles(HttpContext)
	 */
	@Override
	public void unregisterWelcomeFiles(final HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerLoginConfig()
	 */
	@Override
	public void registerLoginConfig(String authMethod, String realmName,
			String formLoginPage, String formErrorPage, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterLoginConfig()
	 */
	@Override
	public void unregisterLoginConfig(HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#registerConstraintMapping(java.lang.String,
	 *      java.lang.String, java.lang.String, java.lang.String, boolean,
	 *      java.util.List, org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerConstraintMapping(String constraintName,
			String mapping, String url, String dataConstraint,
			boolean authentication, List<String> roles, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#unregisterConstraintMapping()
	 */
	@Override
	public void unregisterConstraintMapping(HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	/**
	 * @see WebContainer#getDefaultSharedHttpContext()
	 */
	@Override
	public SharedWebContainerContext getDefaultSharedHttpContext() {
		LOG.warn("Http service has already been stopped");
		return null;
	}

	@Override
	public void registerServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer,
			Class<?>[] classes, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void unregisterServletContainerInitializer(HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspServlet(String[] urlPatterns,
			HttpContext httpContext, String jspFile) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void registerJspServlet(String[] urlPatterns,
			Dictionary<String, ?> initParams, HttpContext httpContext,
			String jspFile) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void setVirtualHosts(List<String> virtualHosts,
			HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void setConnectors(List<String> connectors, HttpContext httpContext) {
		LOG.warn("Http service has already been stopped");
	}

	@Override
	public void begin(HttpContext httpContext) {
	}

	@Override
	public void end(HttpContext httpContext) {
	}

}
