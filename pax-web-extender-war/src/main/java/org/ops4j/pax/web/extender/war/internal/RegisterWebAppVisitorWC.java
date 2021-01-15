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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EventListener;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppJspServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppSecurityConstraint;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
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

	private static final String REGISTRATION_EXCEPTION_SKIPPING = "Registration exception. Skipping.";
	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RegisterWebAppVisitorWC.class);
	/**
	 * WebContainer to be used for registration.
	 */
//	private final WebContainer webContainer;
	/**
	 * Created http context (during webapp visit)
	 */
	private HttpContext httpContext;
	/**
	 * Class loader to be used in the created web app.
	 */
	private ClassLoader bundleClassLoader;

//	private WebAppDependencyHolder dependencyHolder;

	/**
	 * Creates a new registration visitor.
	 *
	 * @param dependencyHolder dependency holder. Cannot be null.
	 * @throws NullArgumentException if web container is null
	 */
//	RegisterWebAppVisitorWC(final WebAppDependencyHolder dependencyHolder) {
//		NullArgumentException
//				.validateNotNull(dependencyHolder, "Web container");
//		this.dependencyHolder = dependencyHolder;
//		this.webContainer = (WebContainer) dependencyHolder.getHttpService();
//	}

	/**
	 * Creates a default context that will be used for all following
	 * registrations, sets the context params and registers a resource for root
	 * of war.
	 *
	 * @throws NullArgumentException if web app is null
	 * @see WebAppVisitor#visit(BundleWebApplication)
	 */
	public void visit(final BundleWebApplication webApp) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("visiting webapp: {}", webApp);
		}
//		NullArgumentException.validateNotNull(webApp, "Web app");
//		bundleClassLoader = new BundleClassLoader(webApp.getBundle());
//		Set<Bundle> wiredBundles = ClassPathUtil.getBundlesInClassSpace(
//				webApp.getBundle(), new LinkedHashSet<>());
		ArrayList<Bundle> bundles = new ArrayList<>();
//		bundles.add(webApp.getBundle());
//		bundles.addAll(wiredBundles);
//		bundleClassLoader = new ResourceDelegatingBundleClassLoader(bundles);
//		httpContext = new WebAppWebContainerContext(
//				webContainer.createDefaultHttpContext(), webApp.getRootPath(),
//				webApp.getBundle(), webApp.getMimeMappings());
//		webApp.setHttpContext(httpContext);
		//CHECKSTYLE:OFF
//		try {
//			webContainer.setContextParam(RegisterWebAppVisitorHS
//					.convertInitParams(webApp.getContextParams()), httpContext);
//		} catch (Exception ignore) {
//			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
//		}
		//CHECKSTYLE:ON
		// set login Config PAXWEB-210
//		if (webApp.getLoginConfigs() != null) {
//			for (WebAppLoginConfig loginConfig : webApp.getLoginConfigs()) {
//				visit(loginConfig); // TODO: what about more than one login
//				 config? shouldn't it be just one?
//			}
//		}

		//CHECKSTYLE:OFF
		// set session timeout
//		if (webApp.getSessionTimeout() != null) {
			try {
//				webContainer.setSessionTimeout(
//						Integer.parseInt(webApp.getSessionTimeout()),
//						httpContext);
			} catch (Exception ignore) {
				LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
			}
//		}
		//CHECKSTYLE:ON

//		WebAppCookieConfig scc = webApp.getSessionCookieConfig();
//		if (scc != null) {
//			webContainer.setSessionCookieConfig(scc.getDomain(), scc.getName(), scc.getHttpOnly(), scc.getSecure(),
//					scc.getPath(), scc.getMaxAge(), httpContext);
//		}

//		for (WebAppServletContainerInitializer servletContainerInitializer : webApp
//				.getServletContainerInitializers()) {
//			webContainer.registerServletContainerInitializer(
//					servletContainerInitializer
//							.getServletContainerInitializer(),
//					servletContainerInitializer.getClasses(), httpContext);
//		}
//		ServletContainerInitializer initializer = dependencyHolder
//				.getServletContainerInitializer();
//		if (initializer != null) {
//			webContainer.registerServletContainerInitializer(initializer, null,
//					httpContext);
//		}

//TODO: VirtualHost and JettyWebXML are missing
//		webContainer.setConnectorsAndVirtualHosts(webApp.getConnectorList(), webApp.getVirtualHostList(), httpContext);
//
//		if (webApp.getJettyWebXmlURL() != null) {
//			webContainer.registerJettyWebXml(webApp.getJettyWebXmlURL(),
//					httpContext);
//		}

//		LOG.debug("webcontainer begin!");
//		webContainer.begin(httpContext);

		//CHECKSTYLE:OFF
//		LOG.debug("registering welcome files");
		// register welcome files
//		try {
//			final String[] welcomeFiles = webApp.getWelcomeFiles();
//			if (welcomeFiles != null && welcomeFiles.length > 0) {
////				webContainer.registerWelcomeFiles(welcomeFiles, true, // redirect
////						httpContext);
//			}
//		} catch (Exception ignore) {
//			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
//		}

//		LOG.debug("registering default resources");
		// register resource jspServlet
//		try {
//			webContainer.registerResources("/", "default", httpContext);
//		} catch (Exception ignore) {
//			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
//		}

//		LOG.debug("registering jsps");
		// register JSP support
//		try {
//			webContainer
//					.registerJsps(
//							// Fix for PAXWEB-208
//							new String[]{"*.jsp", "*.jspx", "*.jspf",
//									"*.xsp", "*.JSP", "*.JSPX", "*.JSPF",
//									"*.XSP"}, httpContext);
//		} catch (UnsupportedOperationException ignore) {
//			LOG.warn(ignore.getMessage());
//		} catch (Exception ignore) {
//			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
//		}
		//CHECKSTYLE:ON

//		WebAppJspConfig jspConfigDescriptor = webApp.getJspConfigDescriptor();
//		if (jspConfigDescriptor != null) {
//			for (WebAppTagLib webAppTagLib : jspConfigDescriptor.getTagLibConfigs()) {
////				webContainer.registerJspConfigTagLibs(webAppTagLib.getTagLibLocation(), webAppTagLib.getTagLibUri(), httpContext);
//			}
//			for (WebAppJspPropertyGroup webAppJspPropertyGroup : jspConfigDescriptor.getJspPropertyGroups()) {
//				Boolean elIgnored = webAppJspPropertyGroup.getElIgnored();
//				List<String> includeCodes = webAppJspPropertyGroup.getIncludeCodes();
//				List<String> includePreludes = webAppJspPropertyGroup.getIncludePreludes();
//				Boolean isXml = webAppJspPropertyGroup.getIsXml();
//				Boolean scriptingInvalid = webAppJspPropertyGroup.getScriptingInvalid();
//				List<String> urlPatterns = webAppJspPropertyGroup.getUrlPatterns();
//
////				webContainer.registerJspConfigPropertyGroup(includeCodes, includePreludes, urlPatterns, elIgnored, scriptingInvalid, isXml, httpContext);
//			}
//		}
	}

	/**
	 * Registers servlets with web container.
	 *
	 * @throws NullArgumentException if servlet is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppServlet)
	 */
	public void visit(final WebAppServlet webAppServlet) {
//		NullArgumentException.validateNotNull(webAppServlet, "Web app servlet");
		final String[] urlPatterns = webAppServlet.getAliases();
		if (urlPatterns == null || urlPatterns.length == 0) {
			LOG.warn("Servlet [" + webAppServlet
					+ "] does not have any mapping. Skipped.");
		}
		try {
			if (webAppServlet instanceof WebAppJspServlet) {
//				webContainer.registerJspServlet(urlPatterns, httpContext,
//						((WebAppJspServlet) webAppServlet).getJspPath());
			} else {
				Class<? extends Servlet> servletClass = RegisterWebAppVisitorHS
						.loadClass(Servlet.class, bundleClassLoader,
								webAppServlet.getServletClassName());
//				webContainer.registerServlet(servletClass, urlPatterns,
//						RegisterWebAppVisitorHS.convertInitParams(webAppServlet
//								.getInitParams()), webAppServlet
//								.getLoadOnStartup(), webAppServlet
//								.getAsyncSupported(), webAppServlet.getMultipartConfig()
//						, httpContext);
			}
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
		}
		//CHECKSTYLE:ON
	}

	/**
	 * Registers filters with web container.
	 *
	 * @throws NullArgumentException if filter is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppFilter)
	 */
	public void visit(final WebAppFilter webAppFilter) {
//		NullArgumentException.validateNotNull(webAppFilter, "Web app filter");
		LOG.debug("registering filter: {}", webAppFilter);
		final String[] urlPatterns = webAppFilter.getUrlPatterns();
		final String[] servletNames = webAppFilter.getServletNames();
		if ((urlPatterns == null || urlPatterns.length == 0)
				&& (servletNames == null || servletNames.length == 0)) {
			LOG.warn("Filter [" + webAppFilter
					+ "] does not have any mapping. Skipped.");
		}
		boolean asyncSupported = false;
		if (webAppFilter.getAsyncSupported() != null) {
			asyncSupported = webAppFilter.getAsyncSupported();
		}

		try {
//			final Filter filter = RegisterWebAppVisitorHS.newInstance(
//					Filter.class, bundleClassLoader,
//					webAppFilter.getFilterClass());

			String filterName = webAppFilter.getFilterName();

			Class<? extends Filter> filterClass = RegisterWebAppVisitorHS.loadClass(Filter.class, bundleClassLoader, webAppFilter.getFilterClass());

			webAppFilter.setFilterClass(filterClass);
			Dictionary<String, String> initParams = RegisterWebAppVisitorHS.convertInitParams(webAppFilter
					.getInitParams());
			DispatcherType[] dispatcherTypes = webAppFilter.getDispatcherTypes().toArray(new DispatcherType[webAppFilter.getDispatcherTypes().size()]);
			StringBuilder dispatcherTypeString = new StringBuilder();
			for (int i = 0; i < dispatcherTypes.length; i++) {
				dispatcherTypeString.append(dispatcherTypes[i].name());
				if (i < dispatcherTypes.length) {
					dispatcherTypeString.append(",");
				}
			}
			initParams.put(PaxWebConstants.INIT_PARAM_FILTER_MAPPING_DISPATCHER, dispatcherTypeString.toString());
//			initParams.put(PaxWebConstants.FILTER_NAME, filterName);

//			webContainer.registerFilter(filterClass,  filterName, urlPatterns, servletNames, initParams, (Boolean) asyncSupported, httpContext);
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
		}
		//CHECKSTYLE:ON
	}

	/**
	 * Registers listeners with web container.
	 *
	 * @throws NullArgumentException if listener is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppListener)
	 */
	public void visit(final WebAppListener webAppListener) {
//		NullArgumentException.validateNotNull(webAppListener,
//				"Web app listener");
		try {
			final EventListener listener = RegisterWebAppVisitorHS.newInstance(
					EventListener.class, bundleClassLoader,
					webAppListener.getListenerClass());
			webAppListener.setListener(listener);
//			webContainer.registerEventListener(listener, httpContext);
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
		}
		//CHECKSTYLE:ON
	}

	/**
	 * Registers error pages with web container.
	 *
	 * @throws NullArgumentException if listener is null
	 * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebAppListener)
	 */
	public void visit(final WebAppErrorPage webAppErrorPage) {
//		NullArgumentException.validateNotNull(webAppErrorPage,
//				"Web app error page");
		try {
//			webContainer.registerErrorPage(webAppErrorPage.getError(),
//					webAppErrorPage.getLocation(), httpContext);
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
		}
		//CHECKSTYLE:ON
	}

	public void visit(WebAppLoginConfig loginConfig) {
//		NullArgumentException.validateNotNull(loginConfig,
//				"Web app login config");
		try {
//			webContainer.registerLoginConfig(loginConfig.getAuthMethod(),
//					loginConfig.getRealmName(), loginConfig.getFormLoginPage(),
//					loginConfig.getFormErrorPage(), httpContext);
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error(REGISTRATION_EXCEPTION_SKIPPING, ignore);
		}
		//CHECKSTYLE:ON
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
//		NullArgumentException.validateNotNull(constraintMapping,
//				"Web app constraint mappings");
		try {
			WebAppSecurityConstraint securityConstraint = constraintMapping
					.getSecurityConstraint();

//			webContainer.registerConstraintMapping(
//					constraintMapping.getConstraintName(),
//					constraintMapping.getMapping(),
//					constraintMapping.getUrl(),
//					securityConstraint.getDataConstraint(),
//					securityConstraint.getAuthenticate(),
//					securityConstraint.getRoles(), httpContext);
			//CHECKSTYLE:OFF
		} catch (Exception ignore) {
			LOG.error("Registration exception. Skipping", ignore);
		}
		//CHECKSTYLE:ON
	}

	public void end() {
//		webContainer.end(httpContext);
	}

}
