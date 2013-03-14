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
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServiceProxy implements StoppableHttpService {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceProxy.class);
	private StoppableHttpService delegate;

	public HttpServiceProxy(final StoppableHttpService delegate) {
		NullArgumentException.validateNotNull(delegate, "Delegate");
		LOG.debug("HttpServiceProxy created for HttpService {}", delegate);
		this.delegate = delegate;
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
		LOG.debug("Registering servlet: [" + alias + "] -> " + servlet);
		delegate.registerServlet(alias, servlet, initParams, httpContext);
	}

	@Override
	public void registerResources(final String alias, final String name,
			final HttpContext httpContext) throws NamespaceException {
		LOG.debug("Registering resource: [" + alias + "] -> " + name);
		delegate.registerResources(alias, name, httpContext);
	}

	@Override
	public void unregister(final String alias) {
		LOG.debug("Unregistering [" + alias + "]");
		delegate.unregister(alias);
	}

	@Override
	public HttpContext createDefaultHttpContext() {
		LOG.debug("Creating a default context");
		return delegate.createDefaultHttpContext();
	}

	@Override
	public synchronized void stop() {
		LOG.debug("Stopping http service: [" + this + "]");
		final StoppableHttpService stopping = delegate;
		delegate = new HttpServiceStopped();
		stopping.stop();

	}

	/**
	 * @see WebContainer#registerServlet(Servlet, String[], Dictionary,
	 *      HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
			final String[] urlPatterns, final Dictionary<String, ?> initParams,
			final HttpContext httpContext) throws ServletException {
		LOG.debug("Registering servlet [" + servlet + "]");
		delegate.registerServlet(servlet, urlPatterns, initParams, httpContext);
	}

	/**
	 * @see WebContainer#registerServlet(javax.servlet.Servlet, String,
	 *      String[],java.util.Dictionary,org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(final Servlet servlet,
			final String servletName, final String[] urlPatterns,
			final Dictionary<String, ?> initParams,
			final HttpContext httpContext) throws ServletException {
		LOG.debug("Registering servlet [" + servlet + "] with name ["
				+ servletName + "]");
		delegate.registerServlet(servlet, servletName, urlPatterns, initParams,
				httpContext);
	}

	/**
	 * @see WebContainer#unregisterServlet(Servlet)
	 */
	@Override
	public void unregisterServlet(final Servlet servlet) {
		LOG.debug("Unregistering servlet [" + servlet + "]");
		delegate.unregisterServlet(servlet);
	}

	/**
	 * @see org.ops4j.pax.web.service.WebContainer#registerServlet(java.lang.Class,
	 *      java.lang.String[], java.util.Dictionary,
	 *      org.osgi.service.http.HttpContext)
	 */
	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			HttpContext httpContext) throws ServletException {
		LOG.debug("Registering servlet class [{}]", servletClass);
		delegate.registerServlet(servletClass, urlPatterns, initParams,
				httpContext);
	}

	/**
	 * @see org.ops4j.pax.web.service.WebContainer#unregisterServlets(java.lang.Class)
	 */
	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
		LOG.debug("Unregistering servlet class [{}]", servletClass);
		delegate.unregisterServlets(servletClass);
	}

	/**
	 * @see WebContainer#registerEventListener(EventListener, HttpContext) )
	 */
	@Override
	public void registerEventListener(final EventListener listener,
			final HttpContext httpContext) {
		LOG.debug("Registering event listener [" + listener + "]");
		delegate.registerEventListener(listener, httpContext);
	}

	/**
	 * @see WebContainer#unregisterEventListener(EventListener)
	 */
	@Override
	public void unregisterEventListener(final EventListener listener) {
		LOG.debug("Unregistering event listener [" + listener + "]");
		delegate.unregisterEventListener(listener);
	}

	/**
	 * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary,
	 *      HttpContext)
	 */
	@Override
	public void registerFilter(final Filter filter, final String[] urlPatterns,
			final String[] aliases, final Dictionary<String, ?> initParams,
			final HttpContext httpContext) {
		LOG.debug("Registering filter [" + filter + "]");
		delegate.registerFilter(filter, urlPatterns, aliases, initParams,
				httpContext);
	}

	/**
	 * @see WebContainer#unregisterFilter(Filter)
	 */
	@Override
	public void unregisterFilter(final Filter filter) {
		LOG.debug("Unregistering filter [" + filter + "]");
		delegate.unregisterFilter(filter);
	}

	/**
	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
	 */
	@Override
	public void setContextParam(final Dictionary<String, ?> params,
			final HttpContext httpContext) {
		LOG.debug("Setting context paramters [" + params
				+ "] for http context [" + httpContext + "]");
		delegate.setContextParam(params, httpContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setSessionTimeout(final Integer minutes,
			final HttpContext httpContext) {
		LOG.debug("Setting session timeout to " + minutes
				+ " minutes for http context [" + httpContext + "]");
		delegate.setSessionTimeout(minutes, httpContext);
	}

	/**
	 * @see WebContainer#registerJsps(String[], HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		LOG.debug("Registering jsps");
		delegate.registerJsps(urlPatterns, httpContext);
	}

	/**
	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final Dictionary<String, ?> initParams,
			final HttpContext httpContext) {
		LOG.debug("Registering jsps");
		delegate.registerJsps(urlPatterns, initParams, httpContext);
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final HttpContext httpContext) {
		LOG.debug("Unregistering jsps");
		delegate.unregisterJsps(httpContext);
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		LOG.debug("Unregistering jsps");
		delegate.unregisterJsps(urlPatterns, httpContext);
	}

	/**
	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
	 */
	@Override
	public void registerErrorPage(final String error, final String location,
			final HttpContext httpContext) {
		LOG.debug("Registering error page [" + error + "]");
		delegate.registerErrorPage(error, location, httpContext);
	}

	/**
	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
	 */
	@Override
	public void unregisterErrorPage(final String error,
			final HttpContext httpContext) {
		LOG.debug("Unregistering error page [" + error + "]");
		delegate.unregisterErrorPage(error, httpContext);
	}

	/**
	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
	 */
	@Override
	public void registerWelcomeFiles(final String[] welcomeFiles,
			final boolean redirect, final HttpContext httpContext) {
		LOG.debug("Registering welcome files [" + Arrays.toString(welcomeFiles)
				+ "]");
		delegate.registerWelcomeFiles(welcomeFiles, redirect, httpContext);
	}

	/**
	 * @see WebContainer#unregisterWelcomeFiles(HttpContext)
	 */
	@Override
	public void unregisterWelcomeFiles(final HttpContext httpContext) {
		LOG.debug("Unregistering welcome files");
		delegate.unregisterWelcomeFiles(httpContext);
	}

	@Override
	public void registerLoginConfig(String authMethod, String realmName,
			String formLoginPage, String formErrorPage, HttpContext httpContext) {
		LOG.debug("Registering LoginConfig for realm [ " + realmName + " ]");
		delegate.registerLoginConfig(authMethod, realmName, formLoginPage,
				formErrorPage, httpContext);
	}

	@Override
	public void unregisterLoginConfig(final HttpContext httpContext) {
		LOG.debug("Unregistering LoginConfig");
		delegate.unregisterLoginConfig(httpContext);
	}

	@Override
	public void registerConstraintMapping(String constraintName, String url,
			String mapping, String dataConstraint, boolean authentication,
			List<String> roles, HttpContext httpContext) {
		LOG.debug("Registering constraint mapping for [ " + constraintName
				+ " ] ");
		delegate.registerConstraintMapping(constraintName, url, mapping,
				dataConstraint, authentication, roles, httpContext);

	}

	@Override
	public void unregisterConstraintMapping(final HttpContext httpContext) {
		LOG.debug("Unregister constraint mapping");
		delegate.unregisterConstraintMapping(httpContext);
	}

	@Override
	public SharedWebContainerContext getDefaultSharedHttpContext() {
		return delegate.getDefaultSharedHttpContext();
	}

	@Override
	public void registerServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer,
			Class<?>[] classes, final HttpContext httpContext) {
		delegate.registerServletContainerInitializer(
				servletContainerInitializer, classes, httpContext);
	}

	@Override
	public void unregisterServletContainerInitializer(HttpContext httpContext) {
		delegate.unregisterServletContainerInitializer(httpContext);
	}

	@Override
	public void registerJettyWebXml(URL jettyWebXmlURL, HttpContext httpContext) {
		delegate.registerJettyWebXml(jettyWebXmlURL, httpContext);
	}

	@Override
	public void registerJspServlet(String[] urlPatterns,
			HttpContext httpContext, String jspFile) {
		delegate.registerJspServlet(urlPatterns, httpContext, jspFile);
	}

	@Override
	public void registerJspServlet(String[] urlPatterns,
			Dictionary<String, ?> initParams, HttpContext httpContext,
			String jspFile) {
		delegate.registerJspServlet(urlPatterns, initParams, httpContext,
				jspFile);
	}

	@Override
	public void setVirtualHosts(List<String> virtualHosts,
			HttpContext httpContext) {
		delegate.setVirtualHosts(virtualHosts, httpContext);
	}

	@Override
	public void setConnectors(List<String> connectors, HttpContext httpContext) {
		delegate.setConnectors(connectors, httpContext);
	}

	@Override
	public void begin(HttpContext httpContext) {
		delegate.begin(httpContext);
	}

	@Override
	public void end(HttpContext httpContext) {
		delegate.end(httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String[] urlPatterns,
			Dictionary<String, ?> initParams, Integer loadOnStartup,
			Boolean asyncSupported, HttpContext httpContext)
			throws ServletException {
		delegate.registerServlet(servlet, urlPatterns, initParams,
				loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Servlet servlet, String servletName,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {

		delegate.registerServlet(servlet, servletName, urlPatterns, initParams,
				loadOnStartup, asyncSupported, httpContext);
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
			String[] urlPatterns, Dictionary<String, ?> initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException {
		delegate.registerServlet(servletClass, urlPatterns, initParams,
				loadOnStartup, asyncSupported, httpContext);

	}

	@Override
	public void registerServlet(String alias, Servlet servlet,
			@SuppressWarnings("rawtypes") Dictionary initParams,
			Integer loadOnStartup, Boolean asyncSupported,
			HttpContext httpContext) throws ServletException,
			NamespaceException {
		delegate.registerServlet(alias, servlet, initParams, loadOnStartup,
				asyncSupported, httpContext);
	}

	public void start(Bundle bundle, ServerController serverController, ServerModel serverModel, ServletEventDispatcher servletEventDispatcher) {
		delegate  = new HttpServiceStarted(bundle,
				serverController, serverModel,
				servletEventDispatcher);
	}

	public boolean isStopped() {
		return delegate instanceof HttpServiceStopped;
	}
}
