/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.Authenticator;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.authenticator.DigestAuthenticator;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.authenticator.SSLAuthenticator;
import org.apache.catalina.authenticator.SpnegoAuthenticator;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.model.elements.SessionConfigurationModel;

import javax.servlet.SessionCookieConfig;

/**
 * A {@link LifecycleListener} that configures {@link PaxWebStandardContext} with highest-ranked
 * {@link OsgiContextModel} specific session and security information.
 */
public class OsgiContextConfiguration implements LifecycleListener {

	private final OsgiContextModel osgiContextModel;

	/** This is an {@link org.apache.catalina.Authenticator} configured for the context */
	private final Valve authenticationValve;

	private final LoginConfig loginConfig;
	private final boolean noAuth;

	public OsgiContextConfiguration(OsgiContextModel osgiContextModel, Configuration configuration) {
		this.osgiContextModel = osgiContextModel;

		if (osgiContextModel.getSecurityConfiguration().getLoginConfig() == null) {
			authenticationValve = null;
			loginConfig = new LoginConfig("NONE", null, null, null);
			noAuth = true;
			return;
		}

		SecurityConfigurationModel securityConfig = osgiContextModel.getSecurityConfiguration();
		LoginConfigModel loginConfig = securityConfig.getLoginConfig();

		// see org.apache.catalina.startup.ContextConfig.authenticatorConfig()

		this.loginConfig = new LoginConfig(loginConfig.getAuthMethod(), loginConfig.getRealmName(),
				loginConfig.getFormLoginPage(), loginConfig.getFormErrorPage());
		noAuth = false;

		// determine the Authenticator valve
		// Tomcat does it using /org/apache/catalina/startup/Authenticators.properties
		Authenticator authenticator = null;
		switch (loginConfig.getAuthMethod().toUpperCase()) {
			case "BASIC":
				authenticator = new BasicAuthenticator();
				break;
			case "DIGEST":
				DigestAuthenticator digestAuthenticator = new DigestAuthenticator();
				digestAuthenticator.setNonceValidity(configuration.security().getDigestAuthMaxNonceAge());
				authenticator = digestAuthenticator;
				break;
			case "CLIENT-CERT":
			case "CLIENT_CERT":
				authenticator = new SSLAuthenticator();
				break;
			case "FORM":
				authenticator = new FormAuthenticator();
				break;
			case "SPNEGO":
				authenticator = new SpnegoAuthenticator();
				break;
			case "NONE":
				authenticator = new NonLoginAuthenticator();
				break;
			default:
				// TODO: discover (OSGi, ServiceLoader) an authenticator, e.g., Keycloak
		}

		authenticationValve = (Valve) authenticator;
	}

	public Valve getAuthenticationValve() {
		return authenticationValve;
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
//		if (event.getLifecycle().getState() == LifecycleState.STARTING_PREP) {

			PaxWebStandardContext context = (PaxWebStandardContext) event.getSource();
			// org.apache.catalina.startup.ContextConfig.configureStart() is called during CONFIGURE_START_EVENT
			// and it's the method that calls:
			//  - org.apache.catalina.startup.ContextConfig.webConfig() (for pax-web-extender-war we did the
			//    same in BundleWebApplication.processMetadata())
			//  - org.apache.catalina.startup.ContextConfig.configureContext() (for pax-web-extender-war we
			//    did it in BundleWebApplication.buildModel())
			//  - org.apache.catalina.startup.ContextConfig.validateSecurityRoles()
			//  - org.apache.catalina.startup.ContextConfig.authenticatorConfig()
			//  - org.apache.catalina.Context.setConfigured(true)

			// alter session configuration
			SessionConfigurationModel sc = osgiContextModel.getSessionConfiguration();
			if (sc != null) {
				if (sc.getSessionTimeout() != null) {
					context.setSessionTimeout(sc.getSessionTimeout());
				}
				SessionCookieConfig scc = sc.getSessionCookieConfig();
				SessionCookieConfig config = context.getServletContext().getSessionCookieConfig();
				if (scc != null && config != null) {
					if (scc.getName() != null) {
						context.setSessionCookieName(scc.getName());
						config.setName(scc.getName());
					}
					if (scc.getDomain() != null) {
						context.setSessionCookieDomain(scc.getDomain());
						config.setDomain(scc.getDomain());
					}
					if (scc.getPath() != null) {
						context.setSessionCookiePath(scc.getPath());
						config.setPath(scc.getPath());
					}
					context.setUseHttpOnly(scc.isHttpOnly());
					config.setHttpOnly(scc.isHttpOnly());
					config.setSecure(scc.isSecure());
					config.setMaxAge(scc.getMaxAge());
					config.setComment(scc.getComment());

					if (sc.getTrackingModes().size() > 0) {
						context.getServletContext().setSessionTrackingModes(sc.getTrackingModes());
					}
				}
			}

			// alter security configuration
			context.setLoginConfig(this.loginConfig);

			// security constraints
			if (!noAuth) {
				SecurityConfigurationModel security = osgiContextModel.getSecurityConfiguration();
				boolean allAuthenticatedUsersIsAppRole = security.getSecurityRoles()
						.contains(SecurityConstraint.ROLE_ALL_AUTHENTICATED_USERS);

				for (SecurityConstraintModel scm : security.getSecurityConstraints()) {
					SecurityConstraint constraint = new SecurityConstraint();
					constraint.setDisplayName(scm.getName());
					constraint.setUserConstraint(scm.getTransportGuarantee().name());

					constraint.setAuthConstraint(scm.isAuthRolesSet());
					for (String role : scm.getAuthRoles()) {
						constraint.addAuthRole(role);
					}

					// <web-resource-collection> elements
					for (SecurityConstraintModel.WebResourceCollection col : scm.getWebResourceCollections()) {
						SecurityCollection wrc = new SecurityCollection();
						wrc.setName(col.getName());
						boolean methodSet = false;
						for (String method : col.getMethods()) {
							wrc.addMethod(method);
							methodSet = true;
						}
						if (!methodSet) {
							for (String method : col.getOmittedMethods()) {
								wrc.addOmittedMethod(method);
							}
						}
						for (String pattern : col.getPatterns()) {
							wrc.addPattern(pattern);
						}
						constraint.addCollection(wrc);
					}

					if (allAuthenticatedUsersIsAppRole) {
						constraint.treatAllAuthenticatedUsersAsApplicationRole();
					}
					context.addConstraint(constraint);
				}

				for (String role : security.getSecurityRoles()) {
					context.addSecurityRole(role);
				}

				context.getPipeline().addValve(authenticationValve);
			}

			context.setConfigured(true);
		}
	}

}
