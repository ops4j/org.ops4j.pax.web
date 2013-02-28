/* Copyright 2007 Niclas Hedhman.
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2010 Achim Nierbeck.
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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.ops4j.pax.web.jsp.JspServletWrapper;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.util.JspSupportUtils;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.ServletContextManager;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.ResourceModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServiceModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceStarted implements StoppableHttpService {

	private static final String PAX_WEB_JSP_SERVLET = "jsp";

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceStarted.class);

	private final Bundle m_bundle;
	private final ClassLoader m_bundleClassLoader;
	private final ServerController m_serverController;

	private final ServerModel m_serverModel;
	private final ServiceModel m_serviceModel;
	private final ServerListener m_serverListener;
	private final ServletListener m_eventDispatcher;

	private static SharedWebContainerContext sharedWebContainerContext;

	static {
		sharedWebContainerContext = new DefaultSharedWebContainerContext();
	}

	HttpServiceStarted(final Bundle bundle,
			final ServerController serverController,
			final ServerModel serverModel,
			final ServletListener eventDispatcher) {
		LOG.debug("Creating http service for: " + bundle);

		NullArgumentException.validateNotNull(bundle, "Bundle");
		NullArgumentException.validateNotNull(serverController,
				"Server controller");
		NullArgumentException.validateNotNull(serverModel, "Service model");

		m_bundle = bundle;
		m_bundleClassLoader = new BundleClassLoader(bundle);
		m_serverController = serverController;
		m_serverModel = serverModel;
		m_eventDispatcher = eventDispatcher;
		m_serviceModel = new ServiceModel();
		m_serverListener = new ServerListener() {
			public void stateChanged(final ServerEvent event) {
				LOG.debug("Handling event: [" + event + "]");

				if (event == ServerEvent.STARTED) {// TODO: additional events
													// might be needed here
					for (ServletModel model : m_serviceModel.getServletModels()) {
						servletEvent(ServletEvent.DEPLOYING, m_bundle, model);
						m_serverController.addServlet(model);
						servletEvent(ServletEvent.DEPLOYED, m_bundle, model);
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

	public void stop() {
		for (ServletModel model : m_serviceModel.getServletModels()) {
			servletEvent(ServletEvent.UNDEPLOYING, m_bundle, model);
			m_serverModel.removeServletModel(model);
			servletEvent(ServletEvent.UNDEPLOYED, m_bundle, model);
		}
		for (FilterModel model : m_serviceModel.getFilterModels()) {
			m_serverModel.removeFilterModel(model);
		}
		for (ContextModel contextModel : m_serviceModel.getContextModels()) {
			m_serverController.removeContext(contextModel.getHttpContext());
		}
		m_serverModel.deassociateHttpContexts(m_bundle);
	}

    public void registerServlet(final String alias, final Servlet servlet,
            final Dictionary initParams, final HttpContext httpContext)
            throws ServletException, NamespaceException {
        final ContextModel contextModel = getOrCreateContext(httpContext);
        LOG.debug("Using context [" + contextModel + "]");
        final ServletModel model = new ServletModel(contextModel, servlet, alias, initParams);
        registerServlet(contextModel, model);
    }
    
    private void servletEvent(int type, Bundle bundle, ServletModel model) {
        m_eventDispatcher.servletEvent(new ServletEvent(type, bundle, model.getAlias(),
                model.getName(), model.getUrlPatterns(), model.getServlet(), 
                model.getServletClass(), model.getContextModel().getHttpContext()));
    }
    
	private void registerServlet(ContextModel contextModel, ServletModel model) throws ServletException, NamespaceException {
		servletEvent(ServletEvent.DEPLOYING, m_bundle, model);
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
			if (model.getServlet() != null && !isWebAppWebContainerContext(contextModel)) {
			    String contextPath = "/" + contextModel.getContextName();
			    ServletContextManager.startContext(contextPath);
			}
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
				servletEvent(ServletEvent.FAILED, m_bundle, model);
			} else {
				servletEvent(ServletEvent.DEPLOYED, m_bundle, model);
			}
		}
	}

    private boolean isWebAppWebContainerContext(ContextModel contextModel) {
        return contextModel.getHttpContext().getClass().getName().equals("org.ops4j.pax.web.extender.war.internal.WebAppWebContainerContext");
    }

	public void registerResources(final String alias, final String name,
			final HttpContext httpContext) throws NamespaceException {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		final Servlet servlet = m_serverController.createResourceServlet(
				contextModel, alias, name);
		final ResourceModel model = new ResourceModel(contextModel, servlet,
				alias, name);
		servletEvent(ServletEvent.DEPLOYING, m_bundle, model);
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
				servletEvent(ServletEvent.FAILED, m_bundle, model);
			} else {
				servletEvent(ServletEvent.DEPLOYED, m_bundle, model);
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
		servletEvent(ServletEvent.UNDEPLOYING, m_bundle, model);
		m_serverModel.removeServletModel(model);
		m_serviceModel.removeServletModel(model);
		m_serverController.removeServlet(model);
		servletEvent(ServletEvent.UNDEPLOYED, m_bundle, model);
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
		try {
            registerServlet(contextModel, model);
        } catch (NamespaceException ignore) {
            // never thrown as model contains no alias
        }
	}

	/**
	 * @see WebContainer#unregisterServlet(Servlet)
	 */
	@Override
	public void unregisterServlet(final Servlet servlet) {
		final ServletModel model = m_serviceModel.removeServlet(servlet);
		if (model != null) {
			servletEvent(ServletEvent.UNDEPLOYING, m_bundle, model);
			m_serverModel.removeServletModel(model);
			m_serverController.removeServlet(model);
			servletEvent(ServletEvent.UNDEPLOYED, m_bundle, model);
		}
	}

	@Override
	public void registerServlet(Class<? extends Servlet> servletClass,
	        final String[] urlPatterns, final Dictionary initParams, 
	        final HttpContext httpContext)
	        throws ServletException {
        final ContextModel contextModel = getOrCreateContext(httpContext);
        LOG.debug("Using context [" + contextModel + "]");
        final ServletModel model = new ServletModel(contextModel, servletClass,
                null, urlPatterns, null, // no name, no alias
                initParams);
        try {
            registerServlet(contextModel, model);
        } catch (NamespaceException ignore) {
            // never thrown as model contains no alias
        }
	}
	
	@Override
	public void unregisterServlets(Class<? extends Servlet> servletClass) {
        final Set<ServletModel> models = m_serviceModel.removeServletClass(servletClass);
        if (models != null) {
            for (ServletModel model : models) {
                servletEvent(ServletEvent.UNDEPLOYING, m_bundle, model);
                m_serverModel.removeServletModel(model);
                m_serverController.removeServlet(model);
                servletEvent(ServletEvent.UNDEPLOYED, m_bundle, model);
            }
        }
	}
	
	@Override
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

	@Override
	public void unregisterEventListener(final EventListener listener) {
		final EventListenerModel model = m_serviceModel
				.removeEventListener(listener);
		if (model != null) {
			m_serverController.removeEventListener(model);
		}
	}

	@Override
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

	@Override
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
	@Override
	public void setContextParam(final Dictionary params,
			final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		Map<String, String> contextParams = contextModel.getContextParams();
		if (!contextParams.equals(params)) {
		    if (!m_serviceModel.canBeConfigured(httpContext)) {
	            throw new IllegalStateException(
	                    "Http context already used. Context params can be set/changed only before first usage");
	        }			
	        contextModel.setContextParams(params);
		}
		m_serviceModel.addContextModel(contextModel);
	}

	@Override
	public void setSessionTimeout(final Integer minutes,
			final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		Integer sessionTimeout = contextModel.getSessionTimeout();
		if (!(minutes == sessionTimeout || minutes != null && minutes.equals(sessionTimeout))) {
	        if (!m_serviceModel.canBeConfigured(httpContext)) {
	            throw new IllegalStateException(
	                    "Http context already used. Session timeout can be set/changed only before first usage");
	        }
	        contextModel.setSessionTimeout(minutes);
		}
		m_serviceModel.addContextModel(contextModel);
	}

	/**
	 * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final HttpContext httpContext) {
		registerJsps(urlPatterns, null, httpContext);
	}

	/**
	 * @see WebContainer#registerJsps(String[], HttpContext)
	 */
	@Override
	public void registerJsps(final String[] urlPatterns,
			final Dictionary initParams,
			final HttpContext httpContext) {
		registerJspServlet(urlPatterns, initParams, httpContext, null);
	}

	@Override
	public void registerJspServlet(final String[] urlPatterns, final HttpContext httpContext, final String jspFile) {
		registerJspServlet(urlPatterns, null, httpContext, jspFile);
	}
	
	@Override
	public void registerJspServlet(final String[] urlPatterns, Dictionary initParams, final HttpContext httpContext, final String jspFile) {
		if (!JspSupportUtils.jspSupportAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		final Servlet jspServlet = new JspServletWrapper(m_bundle, jspFile);
		final ContextModel contextModel = getOrCreateContext(httpContext);
		initParams = createInitParams(contextModel,
				initParams == null ? new Hashtable<String, String>() : initParams);
		m_serviceModel.addContextModel(contextModel);
		try {
			registerServlet(jspServlet, getJspServletName(jspFile),
					urlPatterns == null ? new String[] { "*.jsp" }
							: urlPatterns, initParams,
					httpContext);
		} catch (ServletException ignore) {
			// this should never happen
			LOG.error("Internal error. Please report.", ignore);
		}
		Map<Servlet, String[]> jspServlets = contextModel.getJspServlets();
		jspServlets.put(jspServlet, urlPatterns);
	}

	private String getJspServletName(String jspFile) {
		return jspFile == null ? PAX_WEB_JSP_SERVLET : null;
	}
	
	private Dictionary<String, String> createInitParams(
			ContextModel contextModel, Dictionary<String, String> initParams) {
		Queue<Configuration> configurations = new LinkedList<Configuration>();
		Configuration serverControllerConfiguration = m_serverController.getConfiguration();
		if (initParams != null) {
			PropertyResolver propertyResolver = new DictionaryPropertyResolver(initParams);
			Configuration configuration = new ConfigurationImpl(propertyResolver);
			configurations.add(configuration);
		}
		configurations.add(serverControllerConfiguration); 
		for (Configuration configuration : configurations) { 
			String scratchDir = configuration.getJspScratchDir();
			if (scratchDir == null) {
				File temporaryDirectory = configuration.getTemporaryDirectory();
				if (temporaryDirectory != null) {
					scratchDir = temporaryDirectory.toString();
				}
			}
			if (configuration.equals(serverControllerConfiguration)) {
				//[PAXWEB-225] creates a bundle specific scratch dir
				File tempDir = new File( scratchDir, contextModel.getContextName() );
				if ( !tempDir.exists())	{
					tempDir.mkdirs();
				}
				scratchDir = tempDir.toString();
			}

			Integer jspCheckInterval = configuration.getJspCheckInterval();
			Boolean jspClassDebugInfo = configuration.getJspClassDebugInfo();
			Boolean jspDevelopment = configuration.getJspDevelopment();
			Boolean jspEnablePooling = configuration.getJspEnablePooling();
			String jspIeClassId = configuration.getJspIeClassId();
			String jspJavaEncoding = configuration.getJspJavaEncoding();
			Boolean jspKeepgenerated = configuration.getJspKeepgenerated();
			String jspLogVerbosityLevel = configuration
				.getJspLogVerbosityLevel();
			Boolean jspMappedfile = configuration.getJspMappedfile();
			Integer jspTagpoolMaxSize = configuration.getJspTagpoolMaxSize();
			Boolean jspPrecompilation = configuration.getJspPrecompilation();

			// TODO: fix this with PAXWEB-226
			Map<String, Object> params = new HashMap<String, Object>(12);
			params.put("checkInterval", jspCheckInterval);
			params.put("classdebuginfo", jspClassDebugInfo);
			params.put("development", jspDevelopment);
			params.put("enablePooling", jspEnablePooling);
			params.put("ieClassId", jspIeClassId);
			params.put("javaEncoding", jspJavaEncoding);
			params.put("keepgenerated", jspKeepgenerated);
			params.put("logVerbosityLevel", jspLogVerbosityLevel);
			params.put("mappedfile", jspMappedfile);
			params.put("scratchdir", scratchDir);
			params.put("tagpoolMaxSize", jspTagpoolMaxSize);
			params.put("usePrecompiled", jspPrecompilation);
			
			params.keySet().removeAll(Collections.list(initParams.keys()));
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				Object param = entry.getValue();
				if (param != null) {
					String initParam = entry.getKey();
					initParams.put(initParam, param.toString());
				}
			}
			
		}
		LOG.debug("JSP scratchdir: "+initParams.get("scratchdir"));
		return initParams;
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final HttpContext httpContext) {
		if (!JspSupportUtils.jspSupportAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = m_serviceModel
				.getContextModel(httpContext);
		if (contextModel == null) {
			throw new IllegalArgumentException(
					"Jsp support is not enabled for http context ["
							+ httpContext + "]");
		}
		for (Iterator<Servlet> jspServlets = 
				contextModel.getJspServlets().keySet().iterator(); 
				jspServlets.hasNext();) {
			Servlet jspServlet = jspServlets.next();
			try {
				unregisterServlet(jspServlet);
			} finally {
				jspServlets.remove();
			}
		}
	}

	/**
	 * @see WebContainer#unregisterJsps(HttpContext)
	 */
	@Override
	public void unregisterJsps(final String[] urlPatterns, final HttpContext httpContext) {
		if (!JspSupportUtils.jspSupportAvailable()) {
			throw new UnsupportedOperationException(
					"Jsp support is not enabled. Is org.ops4j.pax.web.jsp bundle installed?");
		}
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = m_serviceModel
				.getContextModel(httpContext);
		if (contextModel == null) {
			throw new IllegalArgumentException(
					"Jsp support is not enabled for http context ["
							+ httpContext + "]");
		}
		for (Iterator<Map.Entry<Servlet, String[]>> jspServlets = 
				contextModel.getJspServlets().entrySet().iterator();
				jspServlets.hasNext();) {
			Map.Entry<Servlet, String[]> entry = jspServlets.next();
			String[] candidateUrlPatterns = entry.getValue();
			if (Arrays.equals(urlPatterns, candidateUrlPatterns)) {
				Servlet jspServlet = entry.getKey();
				try {
					unregisterServlet(jspServlet);
				} finally {
					jspServlets.remove();
				}
			}
		}
	}	

	/**
	 * @see WebContainer#registerErrorPage(String, String, HttpContext)
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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

	@Override
	public void registerLoginConfig(String authMethod, String realmName,
			String formLoginPage, String formErrorPage, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		String contextModelAuthMethod = contextModel.getAuthMethod();
		String contextModelRealmName = contextModel.getRealmName();
		String contextModelFormLoginPage = contextModel.getFormLoginPage();
		String contextModelFormErrorPage = contextModel.getFormErrorPage();
		if (!Arrays.asList(contextModelAuthMethod, contextModelRealmName, 
		    contextModelFormLoginPage, contextModelFormErrorPage).equals(
		    Arrays.asList(authMethod, realmName, formLoginPage, formErrorPage))) {
	        if (!m_serviceModel.canBeConfigured(httpContext)) {
	            throw new IllegalStateException(
	                    "Http context already used. Session timeout can be set/changed only before first usage");
	        }
	        contextModel.setAuthMethod(authMethod);
	        contextModel.setRealmName(realmName);
	        contextModel.setFormLoginPage(formLoginPage);
	        contextModel.setFormErrorPage(formErrorPage);
		}
		m_serviceModel.addContextModel(contextModel);
	}

	@Override
	public void unregisterLoginConfig(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = m_serviceModel
				.getContextModel(httpContext);
		if (contextModel == null || contextModel.getAuthMethod() == null
				|| contextModel.getRealmName() == null) {
			throw new IllegalArgumentException(
					"Security Realm and authorization method are not registered for http context ["
							+ httpContext + "]");
		}
		try {
			// NOP
		} finally {
			// NOP
		}
	}

	@Override
	public void registerConstraintMapping(String constraintName, String url,
			String mapping, String dataConstraint, boolean authentication,
			List<String> roles, HttpContext httpContext) {
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		SecurityConstraintMappingModel secConstraintMapModel = new SecurityConstraintMappingModel(
				contextModel, constraintName, mapping, url, dataConstraint,
				authentication, roles);
		m_serviceModel.addSecurityConstraintMappingModel(secConstraintMapModel);
		m_serverController.addSecurityConstraintMapping(secConstraintMapModel);
	}

	@Override
	public void unregisterConstraintMapping(final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		// NOP
	}

	@Override
	public void registerServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer,
			Class[] classes, final HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");

		Set<Class<?>> clazzes = new HashSet<Class<?>>();
		if (classes != null) {
		    for (Class clazz : classes) {
		        clazzes.add(clazz);
		    }
		}
        Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = contextModel.getContainerInitializers();
        Set<Class<?>> containerInitializersClasses = 
            containerInitializers == null ? null : containerInitializers.get(servletContainerInitializer);
        if (!clazzes.equals(containerInitializersClasses)) {
            if (!m_serviceModel.canBeConfigured(httpContext)) {
                throw new IllegalStateException(
                        "Http context already used. ServletContainerInitializer can be set/changed only before first usage");
            }
            contextModel.addContainerInitializer(servletContainerInitializer,
                    clazzes);
        }

		m_serviceModel.addContextModel(contextModel);

	}
	
	@Override
	public void registerJettyWebXml(URL jettyWebXmlURL,
			HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");		
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		URL contextModelJettyWebXmlURL = contextModel.getJettyWebXmlURL();
		if (!(contextModelJettyWebXmlURL == jettyWebXmlURL ||
		    contextModelJettyWebXmlURL != null && contextModelJettyWebXmlURL.equals(jettyWebXmlURL))) {
	        if (!m_serviceModel.canBeConfigured(httpContext)) {
	            throw new IllegalStateException(
	                    "Http context already used. ServletContainerInitializer can be set/changed only before first usage");
	        }
	        contextModel.setJettyWebXmlUrl(jettyWebXmlURL);
		}
		m_serviceModel.addContextModel(contextModel);

	}

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

	@Override
	public SharedWebContainerContext getDefaultSharedHttpContext() {
		return sharedWebContainerContext;
	}

	@Override
	public void unregisterServletContainerInitializer(HttpContext m_httpContext) {
		// TODO Auto-generated method stub

	}

    public void begin(HttpContext httpContext) {
        final ContextModel contextModel = getOrCreateContext(httpContext);
        LOG.debug("Using context [" + contextModel + "]");
        try {
            m_serverController.getContext( contextModel ).stop();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            LOG.warn("Exception starting HttpContext registration");
        }
    }

    public void end(HttpContext httpContext) {
        final ContextModel contextModel = getOrCreateContext(httpContext);
        LOG.debug("Using context [" + contextModel + "]");
        try {
            m_serverController.getContext( contextModel ).start();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            LOG.warn("Exception finalizing HttpContext registration");
        }
    }

    //Fix for PAXWEB-309
	private interface ServletPlus extends Servlet {
		public boolean isInitialized();
	}

	@Override
	public void setVirtualHosts(List<String> virtualHosts, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		if (!m_serviceModel.canBeConfigured(httpContext)) {
			throw new IllegalStateException(
					"Http context already used. ServletContainerInitializer can be set only before first usage");
		}
		
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		List<String> realVirtualHosts = virtualHosts;
		if (realVirtualHosts.size() == 0) {
			realVirtualHosts = this.m_serverController.getConfiguration().getVirtualHosts();
		}
		contextModel.setVirtualHosts(realVirtualHosts);
		m_serviceModel.addContextModel(contextModel);	
	}

	@Override
	public void setConnectors(List<String> connectors, HttpContext httpContext) {
		NullArgumentException.validateNotNull(httpContext, "Http context");
		if (!m_serviceModel.canBeConfigured(httpContext)) {
			throw new IllegalStateException(
					"Http context already used. ServletContainerInitializer can be set only before first usage");
		}
		
		final ContextModel contextModel = getOrCreateContext(httpContext);
		LOG.debug("Using context [" + contextModel + "]");
		List<String> realConnectors = connectors;
		if (realConnectors.size() == 0) {
			realConnectors = this.m_serverController.getConfiguration().getConnectors();
		}
		contextModel.setConnectors(realConnectors);
		m_serviceModel.addContextModel(contextModel);		
	}
}
