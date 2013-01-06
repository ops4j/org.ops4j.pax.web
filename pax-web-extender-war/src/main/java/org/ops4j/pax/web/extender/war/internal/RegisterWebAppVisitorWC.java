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
	private final WebContainer m_webContainer;
	/**
	 * Created http context (during webapp visit)
	 */
	private HttpContext m_httpContext;
	/**
	 * Class loader to be used in the created web app.
	 */
	private ClassLoader m_bundleClassLoader;
	
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
		NullArgumentException.validateNotNull(dependencyHolder, "Web container");
		this.dependencyHolder = dependencyHolder;
		m_webContainer = (WebContainer) dependencyHolder.getHttpService();
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
		if (LOG.isDebugEnabled())
			LOG.debug("visiting webapp" + webApp);
		NullArgumentException.validateNotNull(webApp, "Web app");
		m_bundleClassLoader = new BundleClassLoader(webApp.getBundle());
		m_httpContext = new WebAppWebContainerContext(
				m_webContainer.createDefaultHttpContext(),
				webApp.getRootPath(), webApp.getBundle(),
				webApp.getMimeMappings());
		webApp.setHttpContext(m_httpContext);
		try {
			m_webContainer.setContextParam(RegisterWebAppVisitorHS
					.convertInitParams(webApp.getContextParams()),
					m_httpContext);
		} catch (Throwable ignore) {
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
				m_webContainer.setSessionTimeout(
						Integer.parseInt(webApp.getSessionTimeout()),
						m_httpContext);
			} catch (Throwable ignore) {
				LOG.error("Registration exception. Skipping.", ignore);
			}
		}

		for (WebAppServletContainerInitializer servletContainerInitializer : webApp.getServletContainerInitializers()) {
			m_webContainer.registerServletContainerInitializer(
					servletContainerInitializer.getServletContainerInitializer(),
					servletContainerInitializer.getClasses(), m_httpContext);
		}
		ServletContainerInitializer initializer = dependencyHolder.getServletContainerInitializer();
		if (initializer != null) {
			m_webContainer.registerServletContainerInitializer(initializer, null, m_httpContext);
		}

		m_webContainer.setVirtualHosts(webApp.getVirtualHostList(), m_httpContext);
		m_webContainer.setConnectors(webApp.getConnectorList(), m_httpContext);

		if (webApp.getJettyWebXmlURL() != null)
			m_webContainer.registerJettyWebXml(webApp.getJettyWebXmlURL(), m_httpContext);

        m_webContainer.begin(m_httpContext);

        //TODO: context is started with the resource servlet, all needed functions before that need to be placed here

        // register resource jspServlet
		try {
			m_webContainer.registerResources("/", "default", m_httpContext);
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping.", ignore);
		}
		// register welcome files
		try {
			final String[] welcomeFiles = webApp.getWelcomeFiles();
			if (welcomeFiles != null && welcomeFiles.length > 0) {
				m_webContainer.registerWelcomeFiles(welcomeFiles, true, // redirect
						m_httpContext);
			}
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping.", ignore);
		}

		// register JSP support
		try {
			m_webContainer.registerJsps(
					// Fix for PAXWEB-208
					new String[] { "*.jsp", "*.jspx", "*.jspf", "*.xsp",
							"*.JSP", "*.JSPX", "*.JSPF", "*.XSP" },
					m_httpContext);
		} catch (UnsupportedOperationException ignore) {
			LOG.warn(ignore.getMessage());
		} catch (Throwable ignore) {
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
				m_webContainer.registerJspServlet(urlPatterns, m_httpContext, ((WebAppJspServlet) webAppServlet).getJspPath());
			} else {
				Class<? extends Servlet> servletClass = RegisterWebAppVisitorHS.loadClass(
						Servlet.class, m_bundleClassLoader,
						webAppServlet.getServletClassName());
				m_webContainer.registerServlet(servletClass, urlPatterns,
						RegisterWebAppVisitorHS.convertInitParams(webAppServlet
								.getInitParams()), m_httpContext);
			}
		} catch (Throwable ignore) {
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
					Filter.class, m_bundleClassLoader,
					webAppFilter.getFilterClass());
			webAppFilter.setFilter(filter);
			m_webContainer.registerFilter(filter, urlPatterns, servletNames,
					RegisterWebAppVisitorHS.convertInitParams(webAppFilter
							.getInitParams()), m_httpContext);
		} catch (Throwable ignore) {
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
					EventListener.class, m_bundleClassLoader,
					webAppListener.getListenerClass());
			webAppListener.setListener(listener);
			m_webContainer.registerEventListener(listener, m_httpContext);
		} catch (Throwable ignore) {
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
			m_webContainer.registerErrorPage(webAppErrorPage.getError(),
					webAppErrorPage.getLocation(), m_httpContext);
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	public void visit(WebAppLoginConfig loginConfig) {
		NullArgumentException.validateNotNull(loginConfig,
				"Web app login config");
		try {
			m_webContainer.registerLoginConfig(loginConfig.getAuthMethod(),
					loginConfig.getRealmName(), loginConfig.getFormLoginPage(),
					loginConfig.getFormErrorPage(), m_httpContext);
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping.", ignore);
		}
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
		NullArgumentException.validateNotNull(constraintMapping,
				"Web app constraint mappings");
		try {
			WebAppSecurityConstraint securityConstraint = constraintMapping
					.getSecurityConstraint();

			m_webContainer.registerConstraintMapping(
					constraintMapping.getConstraintName(),
					constraintMapping.getUrl(), constraintMapping.getMapping(),
					securityConstraint.getDataConstraint(),
					securityConstraint.getAuthenticate(),
					securityConstraint.getRoles(), m_httpContext);
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping", ignore);
		}
	}

    public void end() {
        m_webContainer.end(m_httpContext);
    }

}
