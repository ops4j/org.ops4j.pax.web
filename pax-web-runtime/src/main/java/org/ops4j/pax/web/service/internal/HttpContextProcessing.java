/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.annotation.ServletSecurity;

import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.views.ProcessingWebContainerView;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.task.ContextParamsChange;
import org.ops4j.pax.web.service.spi.task.ContextStartChange;
import org.ops4j.pax.web.service.spi.task.ContextStopChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.SecurityConfigChange;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Tracks CM factory configurations which, in declarative way, are used to provide additional configuration
 * for given bundle's instance of {@link org.ops4j.pax.web.service.http.HttpService}/{@link WebContainer}.</p>
 */
public class HttpContextProcessing implements ManagedServiceFactory {

	public static final Logger LOG = LoggerFactory.getLogger(HttpContextProcessing.class);
	public static final String PID = PaxWebConstants.PID + ".context";

	private static final String KEY_CONTEXT_ID = "context.id";
	private static final String KEY_BUNDLE_SN = "bundle.symbolicName";
	private static final String KEY_WEB_FRAGMENT = "context.webFragment";
	private static final String KEY_WHITEBOARD = "whiteboard";
	private static final String PREFIX_CONTEXT_PARAM = "context.param.";
	private static final String PREFIX_LOGIN_CONFIG = "login.config.";
	private static final String PREFIX_SECURITY = "security.";
	private static final String PREFIX_SECURITY_ROLE = "security.roles";

	private final ExecutorService configExecutor = new ThreadPoolExecutor(0, 1,
			20, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("paxweb-context"));

	private final BundleContext serviceContext;

	private final ConcurrentMap<String, HttpContextTracker> httpContextTrackers = new ConcurrentHashMap<>();

	public HttpContextProcessing(BundleContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	@Override
	public String getName() {
		return PID;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) {
		LOG.info("Updated configuration for pid={}", pid);

		// we're in the context of ConfigurationAdmin UpdateThread, but we should not hold it too long
		configExecutor.submit(new ConfigurationChangeTask(pid, properties));
	}

	@Override
	public void deleted(String pid) {
		LOG.info("Deleted configuration for pid={}", pid);
		configExecutor.submit(new ConfigurationChangeTask(pid, null));
	}

	public void destroy() {
		configExecutor.shutdown();
	}

	/**
	 * Synchronous task (single instance running at given time) that's handling an update to context processing
	 * configuration.
	 */
	private class ConfigurationChangeTask implements Runnable {

		private final String pid;
		private final Dictionary<String, ?> properties;

		ConfigurationChangeTask(String pid, Dictionary<String, ?> properties) {
			this.pid = pid;
			this.properties = properties;
		}

		@Override
		public void run() {
			try {
				LOG.debug("Processing {} PID {}", pid, properties == null ? "removal" : "change");
				HttpContextTracker p = httpContextTrackers.computeIfAbsent(pid, HttpContextTracker::new);
				p.reconfigure(properties);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * <p>A class that operates on {@link org.ops4j.pax.web.service.http.HttpService}/{@link WebContainer} scoped
	 * for given, configured {@link Bundle}.</p>
	 * <p>We don't have to operate on wirings/revisions - that's not required for
	 * {@link org.osgi.framework.Constants#SCOPE_BUNDLE}.</p>
	 */
	private class HttpContextTracker implements BundleTrackerCustomizer<Object> {

		// real PID from factory PID holding given processor's configuration
		private final String pid;

		private Map<String, String> properties;
		// declared symbolic name of bundle to track
		private String symbolicName;
		// tracker for given symbolic name
		private BundleTracker<?> bundleTracker;
		// tracked bundle + version
		private Bundle bundle;
		private Version version;
		private String contextId;
		private boolean whiteboard;

		// transient values for current configuration
		private final Map<String, String> contextParams = new LinkedHashMap<>();
		// <login-config>
		private LoginConfigModel loginConfiguration;
		// <security-role>
		private final Set<String> securityRoles = new LinkedHashSet<>();
		// <security-constraint>
		private final List<SecurityConstraintModel> securityMappings = new LinkedList<>();

		HttpContextTracker(String pid) {
			this.pid = pid;
		}

		/**
		 * Sets new/changed/removed configuration in given processor - method to be run in "paxweb-context"
		 * single threaded executor.
		 * @param props
		 */
		public void reconfigure(Dictionary<String, ?> props) {
			if (props == null) {
				// we should stop tracking the configured bundle and its HttpService/WebContainer
				if (bundleTracker != null) {
					bundleTracker.close();
				}
				// ... and get rid of processor for given PID
				HttpContextTracker tracker = httpContextTrackers.remove(pid);
				if (tracker != null && symbolicName != null) {
					// tracker should be 'this', but let's be paranoid
					configExecutor.execute(tracker::cleanupContext);
				}
				properties = null;
			} else {
				// assuming we wouldn't be called with unchanged properties (that's ConfigurationManager job
				// to notify ManagedServices only when configuration has changed), we just have to clean up
				// previous configuration and apply the new one

				// even if updated configuration misses bundle selector, we have to cleanup existing customization
				if (bundleTracker != null) {
					bundleTracker.close();
					// we have to schedule cleanup first - it'll be executed before bundleTracker.addingBundle()
					// will schedule processing
					configExecutor.execute(this::cleanupContext);
				}

				if (props.get(KEY_BUNDLE_SN) == null) {
					LOG.warn("Incorrect {} configuration - missing {} selector", pid, KEY_BUNDLE_SN);
					return;
				}

				// non-null symbolicName is kind of indicator that the configuration is correct
				properties = Utils.toMap(props);
				symbolicName = properties.get(KEY_BUNDLE_SN).trim();
				contextId = properties.get(KEY_CONTEXT_ID);
				if (contextId == null) {
					contextId = PaxWebConstants.DEFAULT_CONTEXT_NAME;
				}
				whiteboard = "true".equalsIgnoreCase(properties.get(KEY_WHITEBOARD));

				if (bundleTracker == null) {
					bundleTracker = new BundleTracker<>(serviceContext, Bundle.ACTIVE, this);
					// we don't have to clean up anything
				}

				// we'll be tracking new (possibly) symbolic name, processContext() will be scheduled from
				// BundleTracker.addingBundle()
				bundleTracker.open();
			}
		}

		/**
		 * This method is executed in the context of "paxweb-context" executor - it actually uses
		 * configured bundle's {@link WebContainer} to register additional web items
		 */
		private void processContext() {
			OsgiContextModel osgiContextModel = getProcessedOsgiContextModel();
			if (osgiContextModel == null) {
				return;
			}
			LOG.info("Customizing {}", osgiContextModel);
			ProcessingWebContainerView view = getProcessingView();
			if (view == null) {
				return;
			}

			Batch batch = new Batch("Processing context \"" + contextId + "\" for bundle " + bundle.getSymbolicName());
			// we always have to restart the context
			batch.getOperations().add(new ContextStopChange(OpCode.MODIFY, osgiContextModel));

			// we could set the properties directly, but we want to do it in elegant way - using a batch
			// surrounded by restart of the target context.

			// Properties file (configuration admin config) can encode parts of security-related stuff from web.xml
			// and also (as a bonus) context parameters.
			// This "HTTP context processing" was designed as limited subset of the information content from
			// full web[-fragment].xml
			//
			// we handle:
			// 0) meta information to select the context (no web[-fragment].xml equivalent)
			//    context.id = <ID of the context or "default" if not present>
			//    bundle.symbolicName = <symbolic name of the bundle for which to obtain WebContainer instance>
			//
			// 1) context parameters
			//    <context-param>
			//      <param-name>token</param-name>
			//      <param-value>string</param-value>
			//    </context-param>
			// encoded as:
			//    context.param.param-name = param-value
			//
			// 2) security roles:
			//    <security-role>
			//      <role-name>token</role-name>
			//    </security-role>
			// encoded as:
			//    security.roles = role1, role2, ...
			//
			// 3) login configuration:
			//    <login-config>
			//      <auth-method>token</auth-method>
			//      <realm-name>token</realm-name>
			//      <form-login-config>
			//        <form-login-page>/token</form-login-page>
			//        <form-error-page>/token</form-error-page>
			//      </form-login-config>
			//    </login-config>
			// encoded as:
			//    login.config.authMethod = auth-method
			//    login.config.realmName = realm-name
			//    login.config.formLoginPage = form-login-page
			//    login.config.formErrorPage = form-error-page
			//
			// 4) security constraints (multiple)
			//    <security-constraint>
			//      <display-name>token</display-name>
			//      <web-resource-collection> <!--1 or more repetitions:-->
			//        <web-resource-name>token</web-resource-name>
			//        <url-pattern>string</url-pattern> <!--1 or more repetitions:-->
			//        <!--You have a CHOICE of the next 2 items at this level-->
			//        <http-method>token</http-method> <!--1 or more repetitions:-->
			//        <http-method-omission>token</http-method-omission> <!--1 or more repetitions:-->
			//      </web-resource-collection>
			//      <auth-constraint> <!--Optional:-->
			//        <role-name>token</role-name> <!--Zero or more repetitions:-->
			//      </auth-constraint>
			//      <user-data-constraint> <!--Optional:-->
			//        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
			//      </user-data-constraint>
			//    </security-constraint>
			// encoded as:
			//    security.[constraint.name].url(s) = url-pattern1, url-pattern2, ...
			//    security.[constraint.name].methods = GET, POST, ...
			//    security.[constraint.name].methodOmissions = GET, POST, ...
			//    security.[constraint.name].roles = role1, role2, ...
			//    security.[constraint.name].transportGuarantee = CONFIDENTIAL|INTEGRAL|NONE
			//
			// however if there's "context.webFragment" property, we assume it's web-fragment.xml location
			// that we can parse and extract relevant information from it

			String webFragmentLocation = properties.get(KEY_WEB_FRAGMENT);
			if (webFragmentLocation != null && !"".equals(webFragmentLocation.trim())) {
				File f = new File(webFragmentLocation);
				if (!f.isFile()) {
					LOG.warn("Web Fragment location {} is not accessible. Skipping.", webFragmentLocation);
				} else {
					LOG.info("Processing Web Fragment {}", webFragmentLocation);
					WebXml fragment = new WebXml();
					fragment.setDistributable(true);
					fragment.setOverridable(true);
					fragment.setAlwaysAddWelcomeFiles(false);
					fragment.setReplaceWelcomeFiles(true);
					try {
						fragment.setURL(f.toURI().toURL());
						WebXmlParser parser = new WebXmlParser(false, false, true);
						parser.setClassLoader(WebXmlParser.class.getClassLoader());
						parser.parseWebXml(fragment.getURL(), fragment, true);

						// context params
						contextParams.putAll(fragment.getContextParams());
						// security roles
						securityRoles.addAll(fragment.getSecurityRoles());
						// login config from web-fragment.xml
						LoginConfig loginConfig = fragment.getLoginConfig();
						if (loginConfig != null) {
							loginConfiguration = new LoginConfigModel();
							LOG.info("Registering login configuration in WebContainer for bundle \"" + symbolicName + "\": method={}, realm={}",
									loginConfig.getAuthMethod(), loginConfig.getRealmName());
							loginConfiguration.setAuthMethod(loginConfig.getAuthMethod());
							loginConfiguration.setRealmName(loginConfig.getRealmName());
							loginConfiguration.setFormLoginPage(loginConfig.getLoginPage());
							loginConfiguration.setFormErrorPage(loginConfig.getErrorPage());
						}
						// security constraints
						for (SecurityConstraint sc : fragment.getSecurityConstraints()) {
							SecurityConstraintModel constraint = new SecurityConstraintModel();
							// <display-name> (no <name> at this level)
							constraint.setName(sc.getDisplayName());
							// <web-resource-collection> elements
							for (SecurityCollection wrc : sc.findCollections()) {
								SecurityConstraintModel.WebResourceCollection collection = new SecurityConstraintModel.WebResourceCollection();
								collection.setName(wrc.getName());
								collection.getMethods().addAll(Arrays.asList(wrc.findMethods()));
								collection.getOmittedMethods().addAll(Arrays.asList(wrc.findOmittedMethods()));
								collection.getPatterns().addAll(Arrays.asList(wrc.findPatterns()));
								constraint.getWebResourceCollections().add(collection);
							}
							// <auth-constraint> elements
							constraint.setAuthRolesSet(sc.getAuthConstraint());
							constraint.getAuthRoles().addAll(Arrays.asList(sc.findAuthRoles()));
							// in case the roles were missing and used in <auth-constraint>, we have to add them here
							securityRoles.addAll(constraint.getAuthRoles());
							// <user-data-constraint>
							if (sc.getUserConstraint() != null && !"".equals(sc.getUserConstraint().trim())) {
								if (ServletSecurity.TransportGuarantee.NONE.toString().equals(sc.getUserConstraint())) {
									constraint.setTransportGuarantee(ServletSecurity.TransportGuarantee.NONE);
								} else {
									constraint.setTransportGuarantee(ServletSecurity.TransportGuarantee.CONFIDENTIAL);
								}
							}

							securityMappings.add(constraint);
						}
					} catch (IOException e) {
						LOG.warn("Failure parsing default {}: {}", webFragmentLocation, e.getMessage(), e);
					}
				}
			}

			// context params from Configuration Admin override those from web-fragment.xml
			contextParams.putAll(collectContextParams(properties));
			if (!contextParams.isEmpty()) {
				LOG.info("Setting context parameters in {}", osgiContextModel);
				batch.getOperations().add(new ContextParamsChange(OpCode.ADD, osgiContextModel, contextParams));
			}

			// login config from Configuration Admin - may override the config from web-fragment.xml
			LoginConfigModel cmLoginConfig = collectLoginConfiguration(properties);
			if (cmLoginConfig != null) {
				if (loginConfiguration == null) {
					loginConfiguration = new LoginConfigModel();
					LOG.info("Registering login configuration in {}: method={}, realm={}",
							osgiContextModel, cmLoginConfig.getAuthMethod(), cmLoginConfig.getRealmName());
				} else {
					LOG.info("Overriding login configuration in {}: method={}, realm={}",
							osgiContextModel, cmLoginConfig.getAuthMethod(), cmLoginConfig.getRealmName());
				}
				loginConfiguration.setAuthMethod(cmLoginConfig.getAuthMethod());
				loginConfiguration.setRealmName(cmLoginConfig.getRealmName());
				loginConfiguration.setFormLoginPage(cmLoginConfig.getFormLoginPage());
				loginConfiguration.setFormErrorPage(cmLoginConfig.getFormErrorPage());
			}

			// finally security mappings from CM have higher priority than the ones from web-fragment.xml
			List<SecurityConstraintModel> cmSecurityMappings = collectSecurityMappings(properties);
			cmSecurityMappings.addAll(securityMappings);
			securityMappings.clear();
			securityMappings.addAll(cmSecurityMappings);

			if (!securityMappings.isEmpty()) {
				LOG.info("Registering security mappings in {}", osgiContextModel);
			}

			// login config and security constraints
			batch.getOperations().add(new SecurityConfigChange(OpCode.ADD, osgiContextModel, loginConfiguration, securityMappings, new ArrayList<>(securityRoles)));

			// start after reconfiguration
			batch.getOperations().add(new ContextStartChange(OpCode.MODIFY, osgiContextModel));

			// apply the changes by sending the batch
			view.sendBatch(batch);
		}

		/**
		 * Brings given {@link WebContainer} to initial state - to be called within "paxweb-context" executor
		 */
		public void cleanupContext() {
			LOG.info("{}: Restoring WebContainer for bundle {}/{}", this, symbolicName, version);

			OsgiContextModel osgiContextModel = getProcessedOsgiContextModel();
			if (osgiContextModel == null) {
				return;
			}
			ProcessingWebContainerView view = getProcessingView();
			if (view == null) {
				return;
			}

			Batch batch = new Batch("Processing context \"" + contextId + "\" for bundle " + bundle.getSymbolicName());
			// we always have to restart the context
			batch.getOperations().add(new ContextStopChange(OpCode.MODIFY, osgiContextModel));

			// context params
			batch.getOperations().add(new ContextParamsChange(OpCode.DELETE, osgiContextModel, contextParams));

			// login config and security constraints - yes - login config be changed to null (even if there was something
			// configured before HTTP context processing)
			batch.getOperations().add(new SecurityConfigChange(OpCode.DELETE, osgiContextModel, loginConfiguration, securityMappings, new ArrayList<>(securityRoles)));

			// start after reconfiguration
			batch.getOperations().add(new ContextStartChange(OpCode.MODIFY, osgiContextModel));

			// apply the changes by sending the batch
			view.sendBatch(batch);
		}

		private LoginConfigModel collectLoginConfiguration(Map<String, String> properties) {
			if (properties != null) {
				if (properties.get(PREFIX_LOGIN_CONFIG + "authMethod") != null) {
					// all other properties are optional
					LoginConfigModel lc = new LoginConfigModel();
					lc.setAuthMethod(properties.get(PREFIX_LOGIN_CONFIG + "authMethod"));
					lc.setRealmName(properties.get(PREFIX_LOGIN_CONFIG + "realmName"));
					if (lc.getRealmName() == null) {
						lc.setRealmName("default");
					}
					lc.setFormLoginPage(properties.get(PREFIX_LOGIN_CONFIG + "formLoginPage"));
					lc.setFormErrorPage(properties.get(PREFIX_LOGIN_CONFIG + "formErrorPage"));
					return lc;
				}
			}
			return null;
		}

		private Map<String, String> collectContextParams(Map<String, String> properties) {
			Map<String, String> result = new HashMap<>();
			if (properties != null) {
				for (String k : properties.keySet()) {
					if (k != null && k.startsWith(PREFIX_CONTEXT_PARAM)) {
						String v = properties.get(k);
						String paramName = k.substring(PREFIX_CONTEXT_PARAM.length());
						result.put(paramName, v);
					}
				}
			}
			return result;
		}

		private List<SecurityConstraintModel> collectSecurityMappings(Map<String, String> properties) {
			List<SecurityConstraintModel> result = new LinkedList<>();
			if (properties != null) {
				final Map<String, SecurityConstraintModel> temp = new LinkedHashMap<>();
				for (String k : properties.keySet()) {
					if (k != null && k.startsWith(PREFIX_SECURITY)) {
						String paramName = k.substring(PREFIX_SECURITY.length());
						if (paramName.contains(".")) {
							String constraintName = paramName.substring(0, paramName.lastIndexOf('.'));
							SecurityConstraintModel model = temp.computeIfAbsent(constraintName, n -> {
								SecurityConstraintModel scm = new SecurityConstraintModel();
								scm.setName(constraintName);
								SecurityConstraintModel.WebResourceCollection collection = new SecurityConstraintModel.WebResourceCollection();
								collection.setName(constraintName); // same as <security-constraint>/<display-name>
								// only one <web-resource-collection> element per <security-constraint> in ConfigAdmin
								scm.getWebResourceCollections().add(collection);
								// no way to decide between "set no roles" (deny all) and "no roles set" (allow all)
								// so only "deny all behavior"
								scm.setAuthRolesSet(true);
								return scm;
							});

							SecurityConstraintModel.WebResourceCollection collection = model.getWebResourceCollections().get(0);

							// encoded as:
							//    security.[constraint.name].url(s) = url-pattern1, url-pattern2, ...
							//    security.[constraint.name].method(s) = GET, POST, ...
							//    security.[constraint.name].methodOmissions = GET, POST, ...
							//    security.[constraint.name].roles = role1, role2, ...
							//    security.[constraint.name].transportGuarantee = CONFIDENTIAL|INTEGRAL|NONE

							String v = properties.get(k);
							if (paramName.endsWith(".url")) {
								collection.getPatterns().add(v.trim());
							} else if (paramName.endsWith(".urls")) {
								collection.getPatterns().addAll(Arrays.asList(v.split("\\s*,\\s*")));
							} else if (paramName.endsWith(".method")) {
								collection.getMethods().add(v.trim());
							} else if (paramName.endsWith(".methods")) {
								collection.getMethods().addAll(Arrays.asList(v.split("\\s*,\\s*")));
							} else if (paramName.endsWith(".methodOmissions")) {
								collection.getOmittedMethods().addAll(Arrays.asList(v.split("\\s*,\\s*")));
							} else if (paramName.endsWith(".roles")) {
								model.getAuthRoles().addAll(Arrays.asList(v.split("\\s*,\\s*")));
							} else if (paramName.endsWith(".transportGuarantee")) {
								if (ServletSecurity.TransportGuarantee.CONFIDENTIAL.toString().equalsIgnoreCase(v.trim())
										|| "INTEGRAL".equalsIgnoreCase(v.trim())) {
									model.setTransportGuarantee(ServletSecurity.TransportGuarantee.CONFIDENTIAL);
								} else {
									model.setTransportGuarantee(ServletSecurity.TransportGuarantee.NONE);
								}
							}
						}
					}
				}

				result.addAll(temp.values());

			}
			return result;
		}

		private OsgiContextModel getProcessedOsgiContextModel() {
			ProcessingWebContainerView view = getProcessingView();
			if (view == null) {
				return null;
			}

			OsgiContextModel osgiContextModel = whiteboard ? view.getContextModel(null, contextId)
					: view.getContextModel(bundle, contextId);
			if (osgiContextModel == null) {
				if (whiteboard) {
					LOG.warn("Can't find whiteboard OsgiContextModel with name \"{}\"", contextId);
				} else {
					LOG.warn("Can't find OsgiContextModel with name \"{}\" for bundle {}", contextId, bundle);
				}
				return null;
			}

			return osgiContextModel;
		}

		private ProcessingWebContainerView getProcessingView() {
			WebContainer wc = getWebContainer();
			if (wc == null) {
				return null;
			}

			return wc.adapt(ProcessingWebContainerView.class);
		}

		@Override
		public Object addingBundle(Bundle bundle, BundleEvent event) {
			if (symbolicName.equals(bundle.getSymbolicName())) {
				// we have a bundle!
				this.bundle = bundle;
				this.version = bundle.getVersion();
				LOG.info("Found bundle \"" + symbolicName + "\", scheduling customization of its WebContainer");
				configExecutor.execute(this::processContext);
			}

			// we don't actually customize the bundle, so let's not return anything
			return null;
		}

		@Override
		public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
			// bundle has changed, but we don't need to do anything - our customization of
			// bundle-scoped WebContainer is still valid
		}

		@Override
		public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
			this.bundle = null;
			this.version = null;
			// no need to reconfigure the context, because everything will be cleaned up anyway
		}

		/**
		 * Gets a {@link WebContainer} service for tracked bundle
		 * @return
		 */
		private WebContainer getWebContainer() {
			Bundle currentBundle = bundle;
			if (currentBundle == null) {
				LOG.debug("Bundle context for {} bundle is no longer valid", symbolicName);
				return null;
			}

			BundleContext context = currentBundle.getBundleContext();
			if (context == null) {
				LOG.debug("Bundle context for {} bundle is no longer valid", symbolicName);
				return null;
			}
			try {
				ServiceReference<WebContainer> sr = context.getServiceReference(WebContainer.class);
				if (sr == null) {
					LOG.warn("Can't obtain service reference for WebContainer for bundle {}", symbolicName);
					return null;
				}
				// We know it's bundle-scoped, so no need for careful instance counting (via ServiceObjects)
				WebContainer wc = context.getService(sr);
				if (wc == null) {
					LOG.warn("Can't obtain WebContainer service for bundle {}", symbolicName);
					return null;
				}
				return wc;
			} catch (IllegalStateException e) {
				LOG.debug("Bundle context for {} bundle is no longer valid", symbolicName);
				return null;
			}
		}

		@Override
		public String toString() {
			return "HTTP Context Processor {" + "bundle=" + bundle + '}';
		}
	}

}
