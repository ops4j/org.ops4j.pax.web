/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.whiteboard.security;

import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultJspMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConfigurationMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConstraintMapping;
import org.ops4j.pax.web.service.whiteboard.JspMapping;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ServletContextHelper> contextReg;
	private ServiceRegistration<ServletContextHelper> defaultContextReg;
	private ServiceRegistration<Servlet> secureServletReg;
	private ServiceRegistration<Servlet> protectedServletReg;
	private ServiceRegistration<Servlet> anonymousServletReg;
	private ServiceRegistration<Servlet> errorServletReg;
	private ServiceRegistration<SecurityConfigurationMapping> securityReg;
	private ServiceRegistration<JspMapping> jspReg;

	public void start(final BundleContext bundleContext) {
		Dictionary<String, Object> props;

		// getting authMethod from context property, we can test the same Whiteboard example with different
		// authentication methods - including KEYCLOAK
		String authMethod = bundleContext.getProperty("paxweb.authMethod");
		String realmName = bundleContext.getProperty("paxweb.realmName");
		String resolver = bundleContext.getProperty("paxweb.keycloak.resolver");

		// register a /pax-web-security context and the servlets will target both default and this context
		// so we can check security configuration with both of them
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/pax-web-security");
		if ("KEYCLOAK".equalsIgnoreCase(authMethod)) {
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "keycloak.config.file",
					"etc/whiteboard-customcontext-keycloak.json");
			if (resolver != null && !"".equals(resolver)) {
				// special configuration of the resolver to check problems detected
				// in https://github.com/keycloak/keycloak/pull/11704/files (keycloak-pax-web-jetty94 not seeing
				// org.keycloak.adapters.osgi.PathBasedKeycloakConfigResolver class (its package is not imported)
				props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "keycloak.config.resolver", resolver);
			}
		}
		contextReg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper(bundleContext.getBundle()) {
		}, props);

		// in theory, we should register "default" "/" context with higher priority just to set "/" context (init) parameters, but
		// Pax Web actually collects all the context parameters from all OSGi contexts bound to single target
		// ServletContext
		props = new Hashtable<>();
		// -1 is on purpose to get LOWER ranking than the default context - we want to see if the context (init)
		// parameter will be available anyway
		props.put(Constants.SERVICE_RANKING, -1);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		if ("KEYCLOAK".equalsIgnoreCase(authMethod)) {
			props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_INIT_PARAM_PREFIX + "keycloak.config.file",
					"etc/whiteboard-rootcontext-keycloak.json");
		}

		defaultContextReg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper(bundleContext.getBundle()) {
		}, props);

		// Pax Web specific Whiteboard registration of login configuration anf security constraints
		// normally it's done using WAB and web.xml or HttpContextProcessing (CM/properties-based alteration of
		// existing contexts)
		DefaultSecurityConfigurationMapping security = new DefaultSecurityConfigurationMapping();
		// target both contexts
		security.setContextSelectFilter(String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		if (authMethod != null && !"".equals(authMethod.trim())) {
			security.setAuthMethod(authMethod.trim());
		}
		if (realmName != null && !"".equals(realmName)) {
			security.setRealmName(realmName.trim());
		}
		security.getSecurityRoles().add("paxweb-admin");
		security.getSecurityRoles().add("paxweb-viewer");

		DefaultSecurityConstraintMapping adminConstraint = new DefaultSecurityConstraintMapping();
		adminConstraint.setName("very-secure-area");
		adminConstraint.getAuthRoles().add("paxweb-admin");
		DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping forAdmin = new DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping();
		forAdmin.setName("very-secure");
		forAdmin.getUrlPatterns().add("/very-secure/*");
		adminConstraint.getWebResourceCollections().add(forAdmin);
		security.getSecurityConstraints().add(adminConstraint);

		DefaultSecurityConstraintMapping adminAndViewerConstraint = new DefaultSecurityConstraintMapping();
		adminAndViewerConstraint.setName("secure-area");
		adminAndViewerConstraint.getAuthRoles().add("paxweb-admin");
		adminAndViewerConstraint.getAuthRoles().add("paxweb-viewer");
		DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping forAdminAndViewer = new DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping();
		forAdminAndViewer.setName("secure");
		forAdminAndViewer.getUrlPatterns().add("/secure/*");
		adminAndViewerConstraint.getWebResourceCollections().add(forAdminAndViewer);
		security.getSecurityConstraints().add(adminAndViewerConstraint);

		// the servlet invoking jakarta.servlet.http.HttpServletRequest.logout() must be under some constraint,
		// otherwise Keycloak won't notice it. Keep it even if Pax Web 10 no longer has dedicated
		// Keycloak support
		DefaultSecurityConstraintMapping logoutConstraint = new DefaultSecurityConstraintMapping();
		logoutConstraint.setName("logout-area");
		logoutConstraint.getAuthRoles().add("*");
		DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping forLogout = new DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping();
		forLogout.setName("logout");
		forLogout.getUrlPatterns().add("/logout");
		logoutConstraint.getWebResourceCollections().add(forLogout);
		security.getSecurityConstraints().add(logoutConstraint);

		securityReg = bundleContext.registerService(SecurityConfigurationMapping.class, security, null);

		DefaultJspMapping jsp = new DefaultJspMapping();
		jsp.setUrlPatterns(new String[] { "*.jsp" });
		jsp.setContextSelectFilter(security.getContextSelectFilter());
		jspReg = bundleContext.registerService(JspMapping.class, jsp, null);

		// Secure servlet - for admin only
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "secure-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/very-secure/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		secureServletReg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				return new SecureServlet();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}
		}, props);

		// Protected servlet - for admin and logged-in viewer
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "protected-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/secure/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		protectedServletReg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				return new ProtectedServlet();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}
		}, props);

		// Ordinary servlet - accessible for anonymous users
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "anonymous-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/app/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		anonymousServletReg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				return new AllWelcomeServlet();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}
		}, props);

		// Logout servlet
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "logout-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/logout");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		anonymousServletReg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				return new LogoutServlet();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}
		}, props);

		// Error servlet - accessible for anonymous users
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "error-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[]{"401", "403", "404"});
		errorServletReg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				return new ErrorServlet();
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
			}
		}, props);
	}

	public void stop(BundleContext bundleContext) {
		if (secureServletReg != null) {
			secureServletReg.unregister();
			secureServletReg = null;
		}
		if (protectedServletReg != null) {
			protectedServletReg.unregister();
			protectedServletReg = null;
		}
		if (anonymousServletReg != null) {
			anonymousServletReg.unregister();
			anonymousServletReg = null;
		}
		if (errorServletReg != null) {
			errorServletReg.unregister();
			errorServletReg = null;
		}
		if (jspReg != null) {
			jspReg.unregister();
			jspReg = null;
		}
		if (securityReg != null) {
			securityReg.unregister();
			securityReg = null;
		}
		if (contextReg != null) {
			contextReg.unregister();
			contextReg = null;
		}
		if (defaultContextReg != null) {
			defaultContextReg.unregister();
			defaultContextReg = null;
		}
	}

}
