/* Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.web.jsp.JspServletWrapper;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.JspSupportUtils;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.ResourceModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

class HttpServiceStarted implements StoppableHttpService {

	private static final Log LOG = LogFactory.getLog(HttpServiceStarted.class);

	private final Bundle m_bundle;
	private final ClassLoader m_bundleClassLoader;
	private final ServerController m_serverController;

	private final ServerModel m_serverModel;
	private final ServiceModel m_serviceModel;
	private final ServerListener m_serverListener;
	private static SharedWebContainerContext sharedWebContainerContext;

    static {
        sharedWebContainerContext = new DefaultSharedWebContainerContext();
    }

	HttpServiceStarted(final Bundle bundle,
			final ServerController serverController,
			final ServerModel serverModel) {
		LOG.debug("Creating http service for: " + bundle);

		NullArgumentException.validateNotNull(bundle, "Bundle");
		NullArgumentException.validateNotNull(serverController,
				"Server controller");
		NullArgumentException.validateNotNull(serverModel, "Service model");

		m_bundle = bundle;
		m_bundleClassLoader = new BundleClassLoader(bundle);
		m_serverController = serverController;
		m_serverModel = serverModel;
		m_serviceModel = new ServiceModel();
		m_serverListener = new ServerListener() {
			public void stateChanged(final ServerEvent event) {
				LOG.debug("Handling event: [" + event + "]");

				if (event == ServerEvent.STARTED) {
					for (ServletModel model : m_serviceModel.getServletModels()) {
						m_serverController.addServlet(model);
					}
					for (EventListenerModel model : m_serviceModel
							.getEventListenerModels()) {
						m_serverController.addEventListener(model);
					}
					for (FilterModel filterModel : m_serviceModel
							.getFilterModels()) {
						m_serverController.addFilter(filterModel);
					}
					for (ErrorPageModel model : m_serviceModel
							.getErrorPageModels()) {
						m_serverController.addErrorPage(model);
					}
				}
			}
		};
		m_serverController.addListener(m_serverListener);
	}

	public synchronized void stop() {
		for (ServletModel model : m_serviceModel.getServletModels()) {
			m_serverModel.removeServletModel(model);
		}
		for (FilterModel model : m_serviceModel.getFilterModels()) {
			m_serverModel.removeFilterModel(model);
		}
		for (ContextModel contextModel : m_serviceModel.getContextModels()) {
			m_serverController.removeContext(contextModel.getHttpContext());
		}
		m_serverController.removeListener(m_serverListener);
		m_serverModel.deassociateHttpContexts(m_bundle);
	}

	public void registerServlet(final String alias, final Servlet servlet,
			final Dictionary initParams, final HttpContext httpContext)
			throws ServletException, NamespaceException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final ServletModel model = new ServletModel(contextModel, servlet,
				alias, initParams);
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			m_serverModel.addServletModel(model);
			serverSuccess = true;
			m_serviceModel.addServletModel(model);
			serviceSuccess = true;
			m_serverController.addServlet(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeServletModel(model);
				}
				if (serverSuccess) {
					m_serverModel.removeServletModel(model);
				}
			}
		}
	}

	public void registerResources(final String alias, final String name,
			final HttpContext httpContext) throws NamespaceException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final Servlet servlet = m_serverController.createResourceServlet(
				contextModel, alias, name);
		final ResourceModel model = new ResourceModel(contextModel, servlet,
				alias, name);
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			try {
				m_serverModel.addServletModel(model);
				serverSuccess = true;
			} catch (ServletException e) {
				// this should never happen as the servlet is created each time
				// so it cannot already be registered
			}
			m_serviceModel.addServletModel(model);
			serviceSuccess = true;
			m_serverController.addServlet(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeServletModel(model);
				}
				if (serverSuccess) {
					m_serverModel.removeServletModel(model);
				}
			}
		}
	}

	public void unregister(final String alias) {
		final ServletModel model = m_serviceModel
				.getServletModelWithAlias(alias);
		if (model == null) {
			throw new IllegalArgumentException("Alias [" + alias
					+ "] was never registered");
		}
		m_serverModel.removeServletModel(model);
		m_serviceModel.removeServletModel(model);
		m_serverController.removeServlet(model);
	}

	public HttpContext createDefaultHttpContext() {
		return new DefaultHttpContext(m_bundle);
	}

	/**
	 * @see WebContainer#registerServlet(Servlet, String[], Dictionary,
	 *      HttpContext)
	 */
	public void registerServlet(final Servlet servlet,
			final String[] urlPatterns, final Dictionary initParams,
			final HttpContext httpContext) throws ServletException {
		registerServlet(servlet, null, urlPatterns, initParams, httpContext);
	}

	/**
	 * @see WebContainer#registerServlet(javax.servlet.Servlet, String,
	 *      String[],java.util.Dictionary,org.osgi.service.http.HttpContext)
	 */
	public void registerServlet(final Servlet servlet,
			final String servletName, final String[] urlPatterns,
			final Dictionary initParams, final HttpContext httpContext)
			throws ServletException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final ServletModel model = new ServletModel(contextModel, servlet,
				servletName, urlPatterns, null, // no alias
				initParams);
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			try {
				m_serverModel.addServletModel(model);
				serverSuccess = true;
			} catch (NamespaceException ignore) {
				// as there is no alias there is no name space exception in this
				// case.
			}
			m_serviceModel.addServletModel(model);
			serviceSuccess = true;
			m_serverController.addServlet(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeServletModel(model);
				}
				if (serverSuccess) {
					m_serverModel.removeServletModel(model);
				}
			}
		}
	}

	/**
	 * @see WebContainer#unregisterServlet(Servlet)
	 */
	public void unregisterServlet(final Servlet servlet) {
		final ServletModel model = m_serviceModel.removeServlet(servlet);
		if (model != null) {
			m_serverModel.removeServletModel(model);
			m_serverController.removeServlet(model);
		}
	}

	public void registerEventListener(final EventListener listener,
			final HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final EventListenerModel model = new EventListenerModel(contextModel,
				listener);
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			m_serviceModel.addEventListenerModel(model);
			serviceSuccess = true;
			m_serverController.addEventListener(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeEventListener(listener);
				}
			}
		}
	}

	public void unregisterEventListener(final EventListener listener) {
		final EventListenerModel model = m_serviceModel
				.removeEventListener(listener);
		if (model != null) {
			m_serverController.removeEventListener(model);
		}
	}

	public void registerFilter(final Filter filter, final String[] urlPatterns,
			final String[] servletNames, final Dictionary initParams,
			final HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final FilterModel model = new FilterModel(contextModel, filter,
				urlPatterns, servletNames, initParams);
		boolean serverSuccess = false;
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			m_serverModel.addFilterModel(model);
			serverSuccess = true;
			m_serviceModel.addFilterModel(model);
			serviceSuccess = true;
			m_serverController.addFilter(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeFilter(filter);
				}
				if (serverSuccess) {
					m_serverModel.removeFilterModel(model);
				}
			}
		}
	}

	public void unregisterFilter(final Filter filter) {
		final FilterModel model = m_serviceModel.removeFilter(filter);
		if (model != null) {
			m_serverModel.removeFilterModel(model);
			m_serverController.removeFilter(model);
		}
	}

	/**
	 * @see WebContainer#setContextParam(Dictionary, HttpContext)
	 */
	public void setContextParam(final Dictionary params,
			final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		if (!m_serviceModel.canBeConfigured()) {
			throw new IllegalStateException(
					"Http context already used. Context params can be set only before first usage");
		}
		final ContextModel contextModel = getOrCreateContext(httpContext);
		contextModel.setContextParams(params);
		m_serviceModel.addContextModel(contextModel);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSessionTimeout(final Integer minutes,
			final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		if (!m_serviceModel.canBeConfigured()) {
			throw new IllegalStateException(
					"Http context already used. Session timeout can be set only before first usage");
		}
		final ContextModel contextModel = getOrCreateContext(httpContext);
		contextModel.setSessionTimeout(minutes);
		m_serviceModel.addContextModel(contextModel);
	}

	/**
	 * @see WebContainer#registerJsps(String[], HttpContext)
	 */
	public void registerJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		if (!JspSupportUtils.jspSupportAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		ContextModel contextModel = m_serviceModel.getContextModel(httpContext);
		if (contextModel != null && contextModel.getJspServlet() != null) {
			LOG.debug("JSP support already enabled");
			return;
		}
		final Servlet jspServlet = new JspServletWrapper(m_bundle);
		try {
			registerServlet(jspServlet,
					urlPatterns == null ? new String[] { "*.jsp" }
							: urlPatterns, null, // no initParams
					httpContext);
			if (contextModel == null) {
				contextModel = m_serviceModel.getContextModel(httpContext);
			}
			contextModel.setJspServlet(jspServlet);
		} catch (ServletException ignore) {
			// this should never happen
			LOG.error("Internal error. Please report.", ignore);
		}
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	public void unregisterJsps(final HttpContext httpContext) {
		if (!JspSupportUtils.jspSupportAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = m_serviceModel
				.getContextModel(httpContext);
		if (contextModel == null || contextModel.getJspServlet() == null) {
			throw new IllegalArgumentException(
					"Jsp suppport is not enabled for http context ["
							+ httpContext + "]");
		}
		try {
			unregisterServlet(contextModel.getJspServlet());
		} finally {
			contextModel.setJspServlet(null);
		}
	}

	/**
	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
	 */
	public void registerErrorPage(final String error, final String location,
			final HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final ErrorPageModel model = new ErrorPageModel(contextModel, error,
				location);
		boolean serviceSuccess = false;
		boolean controllerSuccess = false;
		try {
			m_serviceModel.addErrorPageModel(model);
			serviceSuccess = true;
			m_serverController.addErrorPage(model);
			controllerSuccess = true;
		} finally {
			// as this compensatory actions to work the remove methods should
			// not throw exceptions.
			if (!controllerSuccess) {
				if (serviceSuccess) {
					m_serviceModel.removeErrorPage(error, contextModel);
				}
			}
		}
	}

	/**
	 * @see WebContainer#unregisterErrorPage(String, HttpContext)
	 */
	public void unregisterErrorPage(final String error,
			final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ErrorPageModel model = m_serviceModel.removeErrorPage(error,
				m_serviceModel.getContextModel(httpContext));
		if (model != null) {
			m_serverController.removeErrorPage(model);
		}
	}

	/**
	 * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
	 */
	public void registerWelcomeFiles(final String[] welcomeFiles,
			final boolean redirect, final HttpContext httpContext) {
		ContextModel contextModel = m_serviceModel.getContextModel(httpContext);
		if (contextModel != null
				&& contextModel.getWelcomeFilesFilter() != null) {
			throw new IllegalStateException(
					"Welcome files already registered for this context");
		}
		final Filter welcomeFilesFilter = new WelcomeFilesFilter(welcomeFiles,
				redirect);
		try {
			registerFilter(welcomeFilesFilter, new String[] { "/*" }, null, // no
																			// servlet
																			// mappings
					null, // no initParams
					httpContext);
			if (contextModel == null) {
				contextModel = m_serviceModel.getContextModel(httpContext);
			}
			contextModel.setWelcomeFilesFilter(welcomeFilesFilter);
		} catch (Exception ignore) {
			// this should never happen
			LOG.error("Internal error. Please report.", ignore);
		}
	}

	/**
	 * @see WebContainer#unregisterWelcomeFiles(HttpContext)
	 */
	public void unregisterWelcomeFiles(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = m_serviceModel
				.getContextModel(httpContext);
		if (contextModel == null
				|| contextModel.getWelcomeFilesFilter() == null) {
			throw new IllegalArgumentException(
					"Welcome files are not registered for http context ["
							+ httpContext + "]");
		}
		try {
			unregisterFilter(contextModel.getWelcomeFilesFilter());
		} finally {
			contextModel.setWelcomeFilesFilter(null);
		}
	}

	public void registerLoginConfig(String authMethod, String realmName,
			HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		LoginConfigModel loginConfig = new LoginConfigModel(contextModel,
				authMethod, realmName);
		m_serviceModel.addLoginModel(loginConfig);
		m_serverController.addLoginConfig(loginConfig);
	}

	public void unregisterLoginConfig() {
		// TODO Auto-generated method stub

	}

	public void unregisterConstraintMapping() {
		// TODO Auto-generated method stub

	}

	public void registerConstraintMapping(String constraintName,
			String url, String mapping, String dataConstraint,
			boolean authentication, List<String> roles, HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		SecurityConstraintMappingModel secConstraintMapModel = new SecurityConstraintMappingModel(
				contextModel, constraintName, mapping, url, dataConstraint,
				authentication, roles);
		m_serviceModel.addSecurityConstraintMappingModel(secConstraintMapModel);
		m_serverController.addSecurityConstraintMapping(secConstraintMapModel);
	}

	// public void registerSecurityConstraint(String constraintName,
	// String constraint, boolean authenticate, List<String> roles, HttpContext
	// httpContext) {
	// final ContextModel contextModel = getOrCreateContext( httpContext );
	// LOG.debug( "Using context [" + contextModel + "]" );
	//
	// SecurityModel secModel = new SecurityModel(contextModel, constraintName,
	// constraint, authenticate, roles);
	//
	// m_serviceModel.addSecurityModel(secModel);
	// m_serverController.addSecurity(secModel);
	// }

	private ContextModel getOrCreateContext(final HttpContext httpContext) {
		HttpContext context = httpContext;
		if (context == null) {
			context = createDefaultHttpContext();
		}
		m_serverModel.associateHttpContext(context, m_bundle,
				httpContext instanceof SharedWebContainerContext);
		ContextModel contextModel = m_serviceModel.getContextModel(context);
		if (contextModel == null) {
			contextModel = new ContextModel(context, m_bundle,
					m_bundleClassLoader);
		}
		return contextModel;
	}

	public SharedWebContainerContext getDefaultSharedHttpContext() {
		return sharedWebContainerContext;
	}

}
