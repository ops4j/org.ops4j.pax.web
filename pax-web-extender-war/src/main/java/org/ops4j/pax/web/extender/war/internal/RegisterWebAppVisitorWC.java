/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.EventListener;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityConstraint;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that registers a web application. Cannot be reused, it has to be
 * one per visit.
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */

class RegisterWebAppVisitorWC implements WebAppVisitor {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RegisterWebAppVisitorWC.class);
	/**
	 * WebContainer to be used for registration.
	 */
	private final WebContainer webContainer;
	/**
	 * Created http context (during webapp visit)
	 */
	private HttpContext httpContext;
	/**
	 * Class loader to be used in the created web app.
	 */
	private ClassLoader bundleClassLoader;

	private WebAppDependencyHolder dependencyHolder;

	/**
	 * Creates a new registration visitor.
	 * 
	 * @param webContainer
	 *            http service to be used for registration. Cannot be null.
	 * 
	 * @throws NullArgumentException
	 *             if web container is null
	 */
	RegisterWebAppVisitorWC(final WebAppDependencyHolder dependencyHolder) {
		NullArgumentException
				.validateNotNull(dependencyHolder, "Web container");
		this.dependencyHolder = dependencyHolder;
		this.webContainer = (WebContainer) dependencyHolder.getHttpService();
	}

	/**
	 * Creates a default context that will be used for all following
	 * registrations, sets the context params and registers a resource for root
	 * of war.
	 * 
	 * @throws NullArgumentException
	 *             if web app is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebApp)
	 */
	public void visit(final WebApp webApp) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("visiting webapp" + webApp);
		}
		NullArgumentException.validateNotNull(webApp, "Web app");
		bundleClassLoader = new BundleClassLoader(webApp.getBundle());
		httpContext = new WebAppWebContainerContext(
				webContainer.createDefaultHttpContext(), webApp.getRootPath(),
				webApp.getBundle(), webApp.getMimeMappings());
		webApp.setHttpContext(httpContext);
		try {
			webContainer.setContextParam(RegisterWebAppVisitorHS
					.convertInitParams(webApp.getContextParams()), httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
		// set login Config PAXWEB-210
		if (webApp.getLoginConfigs() != null) {
			for (WebAppLoginConfig loginConfig : webApp.getLoginConfigs()) {
				visit(loginConfig); // TODO: what about more than one login
									// config? shouldn't it be just one?
			}
		}

		// set session timeout
		if (webApp.getSessionTimeout() != null) {
			try {
				webContainer.setSessionTimeout(
						Integer.parseInt(webApp.getSessionTimeout()),
						httpContext);
			} catch (Throwable ignore) { // CHECKSTYLE:SKIP
				LOG.error("Registration exception. Skipping.", ignore);
			}
		}

		for (WebAppServletContainerInitializer servletContainerInitializer : webApp
				.getServletContainerInitializers()) {
			webContainer.registerServletContainerInitializer(
					servletContainerInitializer
							.getServletContainerInitializer(),
					servletContainerInitializer.getClasses(), httpContext);
		}
		ServletContainerInitializer initializer = dependencyHolder
				.getServletContainerInitializer();
		if (initializer != null) {
			webContainer.registerServletContainerInitializer(initializer, null,
					httpContext);
		}

		webContainer.setVirtualHosts(webApp.getVirtualHostList(), httpContext);
		webContainer.setConnectors(webApp.getConnectorList(), httpContext);

		if (webApp.getJettyWebXmlURL() != null) {
			webContainer.registerJettyWebXml(webApp.getJettyWebXmlURL(),
					httpContext);
		}

		webContainer.begin(httpContext);

		// TODO: context is started with the resource servlet, all needed
		// functions before that need to be placed here

		// register resource jspServlet
		try {
			webContainer.registerResources("/", "default", httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
		// register welcome files
		try {
			final String[] welcomeFiles = webApp.getWelcomeFiles();
			if (welcomeFiles != null && welcomeFiles.length > 0) {
				webContainer.registerWelcomeFiles(welcomeFiles, true, // redirect
						httpContext);
			}
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}

		// register JSP support
		try {
			webContainer
					.registerJsps(
					// Fix for PAXWEB-208
							new String[] { "*.jsp", "*.jspx", "*.jspf",
									"*.xsp", "*.JSP", "*.JSPX", "*.JSPF",
									"*.XSP" }, httpContext);
		} catch (UnsupportedOperationException ignore) {
			LOG.warn(ignore.getMessage());
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	/**
	 * Registers servlets with web container.
	 * 
	 * @throws NullArgumentException
	 *             if servlet is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppServlet)
	 */
	public void visit(final WebAppServlet webAppServlet) {
		NullArgumentException.validateNotNull(webAppServlet, "Web app servlet");
		final String[] urlPatterns = webAppServlet.getAliases();
		if (urlPatterns == null || urlPatterns.length == 0) {
			LOG.warn("Servlet [" + webAppServlet
					+ "] does not have any mapping. Skipped.");
		}
		try {
			if (webAppServlet instanceof WebAppJspServlet) {
				webContainer.registerJspServlet(urlPatterns, httpContext,
						((WebAppJspServlet) webAppServlet).getJspPath());
			} else {
				Class<? extends Servlet> servletClass = RegisterWebAppVisitorHS
						.loadClass(Servlet.class, bundleClassLoader,
								webAppServlet.getServletClassName());
				webContainer.registerServlet(servletClass, urlPatterns,
						RegisterWebAppVisitorHS.convertInitParams(webAppServlet
								.getInitParams()), webAppServlet
								.getLoadOnStartup(), webAppServlet
								.getAsyncSupported(), httpContext);
			}
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}

	}

	/**
	 * Registers filters with web container.
	 * 
	 * @throws NullArgumentException
	 *             if filter is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppFilter)
	 */
	public void visit(final WebAppFilter webAppFilter) {
		NullArgumentException.validateNotNull(webAppFilter, "Web app filter");
		final String[] urlPatterns = webAppFilter.getUrlPatterns();
		final String[] servletNames = webAppFilter.getServletNames();
		if ((urlPatterns == null || urlPatterns.length == 0)
				&& (servletNames == null || servletNames.length == 0)) {
			LOG.warn("Filter [" + webAppFilter
					+ "] does not have any mapping. Skipped.");
		}
		try {
			final Filter filter = RegisterWebAppVisitorHS.newInstance(
					Filter.class, bundleClassLoader,
					webAppFilter.getFilterClass());
			webAppFilter.setFilter(filter);
			webContainer.registerFilter(filter, urlPatterns, servletNames,
					RegisterWebAppVisitorHS.convertInitParams(webAppFilter
							.getInitParams()), httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	/**
	 * Registers listeners with web container.
	 * 
	 * @throws NullArgumentException
	 *             if listener is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppListener)
	 */
	public void visit(final WebAppListener webAppListener) {
		NullArgumentException.validateNotNull(webAppListener,
				"Web app listener");
		try {
			final EventListener listener = RegisterWebAppVisitorHS.newInstance(
					EventListener.class, bundleClassLoader,
					webAppListener.getListenerClass());
			webAppListener.setListener(listener);
			webContainer.registerEventListener(listener, httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	/**
	 * Registers error pages with web container.
	 * 
	 * @throws NullArgumentException
	 *             if listener is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppListener)
	 */
	public void visit(final WebAppErrorPage webAppErrorPage) {
		NullArgumentException.validateNotNull(webAppErrorPage,
				"Web app error page");
		try {
			webContainer.registerErrorPage(webAppErrorPage.getError(),
					webAppErrorPage.getLocation(), httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	public void visit(WebAppLoginConfig loginConfig) {
		NullArgumentException.validateNotNull(loginConfig,
				"Web app login config");
		try {
			webContainer.registerLoginConfig(loginConfig.getAuthMethod(),
					loginConfig.getRealmName(), loginConfig.getFormLoginPage(),
					loginConfig.getFormErrorPage(), httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
		NullArgumentException.validateNotNull(constraintMapping,
				"Web app constraint mappings");
		try {
			WebAppSecurityConstraint securityConstraint = constraintMapping
					.getSecurityConstraint();

			webContainer.registerConstraintMapping(
					constraintMapping.getConstraintName(),
					constraintMapping.getUrl(), constraintMapping.getMapping(),
					securityConstraint.getDataConstraint(),
					securityConstraint.getAuthenticate(),
					securityConstraint.getRoles(), httpContext);
		} catch (Throwable ignore) { // CHECKSTYLE:SKIP
			LOG.error("Registration exception. Skipping", ignore);
		}
	}

	public void end() {
		webContainer.end(httpContext);
	}

}
