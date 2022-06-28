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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;

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
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.AuthenticatorService;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.model.elements.SessionConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.SessionCookieConfig;

/**
 * A {@link LifecycleListener} that configures {@link PaxWebStandardContext} with highest-ranked
 * {@link OsgiContextModel} specific session and security information.
 */
public class OsgiContextConfiguration implements LifecycleListener {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiContextConfiguration.class);

	private final OsgiContextModel osgiContextModel;
	private final TomcatFactory tomcatFactory;

	/** This is an {@link org.apache.catalina.Authenticator} configured for the context */
	private Valve authenticationValve;

	private final Configuration configuration;

	private final Map<String, TreeMap<OsgiContextModel, SecurityConfigurationModel>> contextSecurityConstraints;

	public OsgiContextConfiguration(OsgiContextModel osgiContextModel, Configuration configuration,
			TomcatFactory tomcatFactory, Map<String, TreeMap<OsgiContextModel, SecurityConfigurationModel>> contextSecurityConstraints) {
		this.osgiContextModel = osgiContextModel;
		this.tomcatFactory = tomcatFactory;
		this.configuration = configuration;
		this.contextSecurityConstraints = contextSecurityConstraints;
	}

	public Valve getAuthenticationValve() {
		return authenticationValve;
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
//		if (event.getLifecycle().getState() == LifecycleState.STARTING_PREP) {

			// security configuration - from all relevant OsgiContextModels
			Map<OsgiContextModel, SecurityConfigurationModel> allSecConfigs = contextSecurityConstraints.get(osgiContextModel.getContextPath());
			SecurityConfigurationModel securityConfig = null;
			if (allSecConfigs != null && allSecConfigs.size() > 0) {
				securityConfig = allSecConfigs.values().iterator().next();
			}
			if (securityConfig == null) {
				// no context processing available - just use highest-ranked model
				securityConfig = osgiContextModel.getSecurityConfiguration();
				allSecConfigs = Collections.singletonMap(osgiContextModel, securityConfig);
			}
			LoginConfigModel loginConfig = securityConfig != null ? securityConfig.getLoginConfig() : null;

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

			// taking virtual host / connector configuration from OsgiContextModel - see
			// org.eclipse.jetty.server.handler.ContextHandler.checkVirtualHost() and similar pax-web-jetty code
			List<String> allVirtualHosts = new ArrayList<>();
			List<String> vhosts = new ArrayList<>(osgiContextModel.getVirtualHosts());
			if (vhosts.isEmpty()) {
				vhosts.addAll(Arrays.asList(configuration.server().getVirtualHosts()));
			}
			List<String> connectors = new ArrayList<>(osgiContextModel.getConnectors());
			if (connectors.isEmpty()) {
				connectors.addAll(Arrays.asList(configuration.server().getConnectors()));
			}
			for (String vhost : vhosts) {
				if (vhost == null || "".equals(vhost.trim())) {
					continue;
				}
				if (vhost.startsWith("@")) {
					// it is a connector
					allVirtualHosts.add(vhost);
				} else {
					// it is a normal virtual host (yes - don't process it anyway)
					allVirtualHosts.add(vhost);
				}
			}
			for (String c : connectors) {
				if (c == null || "".equals(c.trim())) {
					continue;
				}
				if (c.startsWith("@")) {
					// it is a connector, but should be specified as special Jetty's VHost - add without processing
					allVirtualHosts.add(c);
				} else {
					// it is a connector, but should be added as "@" prefixed VHost
					allVirtualHosts.add("@" + c);
				}
			}

			context.setVirtualHosts(allVirtualHosts.toArray(new String[0]));

			// Tomcat-specific context configuration
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(osgiContextModel.getClassLoader());
			try {
				List<URL> contextConfigs = new ArrayList<>();
				if (configuration.server().getContextConfigurationFile() != null) {
					LOG.info("Found global Tomcat context configuration file: {}", configuration.server().getContextConfigurationFile());
					contextConfigs.add(configuration.server().getContextConfigurationFile().toURI().toURL());
				}
				for (URL url : osgiContextModel.getServerSpecificDescriptors()) {
					String path = url.getPath();
					if (path.endsWith("/META-INF/context.xml")) {
						contextConfigs.add(url);
					}
				}
				for (URL url : contextConfigs) {
					String path = url.getPath();
					LOG.info("Processing context specific {} for {}", url, osgiContextModel.getContextPath());

					Digester digester = tomcatFactory.createContextDigester();
					if (osgiContextModel.getClassLoader() != null) {
						digester.setClassLoader(osgiContextModel.getClassLoader());
					} else {
						digester.setClassLoader(tccl);
					}
					digester.push(context.getParent());
					digester.push(context);

					try (InputStream is = url.openStream()) {
						digester.parse(is);
					} catch (IOException | SAXException e) {
						LOG.warn("Problem parsing {}: {}", url, e.getMessage(), e);
					}
				}
			} catch (MalformedURLException e) {
				LOG.warn("Can't process context configuration file: {}", e.getMessage(), e);
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}

			LoginConfig lc;
			boolean noAuth = false;

			if (loginConfig == null) {
				authenticationValve = null;
				lc = new LoginConfig("NONE", null, null, null);
				noAuth = true;
			} else {
				// see org.apache.catalina.startup.ContextConfig.authenticatorConfig()

				lc = new LoginConfig(loginConfig.getAuthMethod(), loginConfig.getRealmName(),
						loginConfig.getFormLoginPage(), loginConfig.getFormErrorPage());

				// Has an authenticator been configured already?
				authenticationValve = (Valve) context.getAuthenticator();
				if (authenticationValve == null) {

					// determine the Authenticator valve
					// Tomcat does it using /org/apache/catalina/startup/Authenticators.properties
					Authenticator authenticator = null;
					switch (loginConfig.getAuthMethod().toUpperCase()) {
					case "BASIC":
						authenticator = new BasicAuthenticator();
						if (lc.getRealmName() == null) {
							lc.setRealmName("default");
						}
						break;
					case "DIGEST":
						DigestAuthenticator digestAuthenticator = new DigestAuthenticator();
						digestAuthenticator.setNonceValidity(configuration.security().getDigestAuthMaxNonceAge());
						authenticator = digestAuthenticator;
						if (lc.getRealmName() == null) {
							lc.setRealmName("default");
						}
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
						Authenticator customAuthenticator = getAuthenticator(loginConfig.getAuthMethod().toUpperCase());
						if (customAuthenticator == null) {
							LOG.warn("Can't find Tomcat Authenticator for auth method {}",
									loginConfig.getAuthMethod().toUpperCase());
						} else {
							LOG.debug("Setting custom Tomcat authenticator {}", customAuthenticator);
							authenticator = customAuthenticator;
						}
					}

					authenticationValve = (Valve) authenticator;

					if (authenticationValve != null) {
						context.getPipeline().addValve(authenticationValve);
					}
				}
			}

			if (authenticationValve == null) {
				noAuth = true;
			}

			// alter security configuration
			context.setLoginConfig(lc);
			for (SecurityConstraint constr : context.findConstraints()) {
				context.removeConstraint(constr);
			}
			for (String role : context.findSecurityRoles()) {
				context.removeSecurityRole(role);
			}

			// security constraints
			if (!noAuth) {
				// roles and constraints are not taken only from the highest ranked
				// OsgiContextModel - they're
				// taken from all the OCMs for given context path - on order of OCM rank
				// it's up to user to take care of the conflicts, because simple rank-ordering
				// will add higher-ranked
				// rules first - the container may decide to override or reject the lower ranked
				// later.

				List<SecurityConstraintModel> allConstraints = new ArrayList<>();
				Set<String> allRoles = new LinkedHashSet<>();
				allSecConfigs.values().forEach(sec -> {
					allConstraints.addAll(sec.getSecurityConstraints());
					allRoles.addAll(sec.getSecurityRoles());
				});

				boolean allAuthenticatedUsersIsAppRole = allRoles
						.contains(SecurityConstraint.ROLE_ALL_AUTHENTICATED_USERS);

				for (SecurityConstraintModel scm : allConstraints) {
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

				for (String role : allRoles) {
					context.addSecurityRole(role);
				}
			}

			context.setConfigured(true);
		}
	}

	private Authenticator getAuthenticator(String method) {
		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
		for (AuthenticatorService svc : sl) {
			try {
				Valve auth = svc.getAuthenticatorService(method, Valve.class);
				if (auth != null && Authenticator.class.isAssignableFrom(auth.getClass())) {
					return (Authenticator) auth;
				}
			} catch (Throwable t) {
				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
			}
		}
		return null;
	}

}
