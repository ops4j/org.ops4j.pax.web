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

import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConfigurationMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConstraintMapping;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ServletContextHelper> contextReg;
	private ServiceRegistration<Servlet> secureServletReg;
	private ServiceRegistration<Servlet> protectedServletReg;
	private ServiceRegistration<Servlet> anonymousServletReg;
	private ServiceRegistration<Servlet> errorServletReg;
	private ServiceRegistration<SecurityConfigurationMapping> securityReg;

	public void start(final BundleContext bundleContext) {
		Dictionary<String, Object> props;

		// register a /pax-web-security context and the servlets will target both default and this context
		// so we can check security configuration with both of them
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/pax-web-security");
		contextReg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props);

		// Pax Web specific Whiteboard registration of login configuration anf security constraints
		// normally it's done using WAB and web.xml or HttpContextProcessing (CM/properties-based alteration of
		// existing contexts)
		DefaultSecurityConfigurationMapping security = new DefaultSecurityConfigurationMapping();
		// target both contexts
		security.setContextSelectFilter(String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		// getting authMethod from context property, we can test the same Whiteboard example with different
		// authentication methods - including KEYCLOAK
		String authMethod = bundleContext.getProperty("paxweb.authMethod");
		String realmName = bundleContext.getProperty("paxweb.realmName");
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
		securityReg = bundleContext.registerService(SecurityConfigurationMapping.class, security, props);

		// Secure servlet - for admin only
		SecureServlet secureServlet = new SecureServlet();
		props = new Hashtable<>();
		props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "secure-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/very-secure/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		secureServletReg = bundleContext.registerService(Servlet.class, secureServlet, props);

		// Protected servlet - for admin and logged-in viewer
		ProtectedServlet protectedServlet = new ProtectedServlet();
		props = new Hashtable<>();
		props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "protected-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/secure/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		protectedServletReg = bundleContext.registerService(Servlet.class, protectedServlet, props);

		// Ordinary servlet - accessible for anonymous users
		AllWelcomeServlet anonymousServlet = new AllWelcomeServlet();
		props = new Hashtable<>();
		props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "anonymous-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/app/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		anonymousServletReg = bundleContext.registerService(Servlet.class, anonymousServlet, props);

		// Error servlet - accessible for anonymous users
		ErrorServlet errorServlet = new ErrorServlet();
		props = new Hashtable<>();
		props.put(Constants.SERVICE_SCOPE, Constants.SCOPE_PROTOTYPE);
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "error-servlet");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/error/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=%s)(%s=%s))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "pax-web-security"));
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[]{"401", "403", "404"});
		errorServletReg = bundleContext.registerService(Servlet.class, errorServlet, props);
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
		if (securityReg != null) {
			securityReg.unregister();
			securityReg = null;
		}
		if (contextReg != null) {
			contextReg.unregister();
			contextReg = null;
		}
	}

}
