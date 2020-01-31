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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Tracks CM factory configurations which, in declarative way, are used to provide additional configuration
 * for given bundle's instance of {@link org.osgi.service.http.HttpService}/{@link WebContainer}.</p>
 */
public class HttpContextProcessing implements ManagedServiceFactory {

	public static Logger LOG = LoggerFactory.getLogger(HttpContextProcessing.class);
	public static final String PID = PaxWebConstants.PID + ".context";

	private static final String KEY_CONTEXT_ID = "context.id";
	private static final String KEY_BUNDLE_SN = "bundle.symbolicName";
	private static final String PREFIX_CONTEXT_PARAM = "context.param.";
	private static final String PREFIX_LOGIN_CONFIG = "login.config.";
	private static final String PREFIX_SECURITY = "security.";

	private final ExecutorService configExecutor = new ThreadPoolExecutor(0, 1,
			20, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamedThreadFactory("paxweb-context"));

	private final BundleContext serviceContext;
	private final ServerController serverController;

	private final ConcurrentMap<String, HttpContextTracker> httpContextTrackers = new ConcurrentHashMap<>();

	public HttpContextProcessing(BundleContext serviceContext, ServerController serverController) {
		this.serviceContext = serviceContext;
		this.serverController = serverController;
	}

	@Override
	public String getName() {
		return PID;
	}

	@Override
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
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

		public ConfigurationChangeTask(String pid, Dictionary<String, ?> properties) {
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
	 * <p>A class that operates on {@link HttpService}/{@link WebContainer} scoped
	 * for given, configured {@link Bundle}.</p>
	 * <p>We don't have to operate on wirings/revisions - that's not required for
	 * {@link org.osgi.framework.Constants#SCOPE_BUNDLE}.</p>
	 */
	private class HttpContextTracker implements BundleTrackerCustomizer<Object> {

		// real PID from factory PID holding given processor's configuration
		private final String pid;

		private Dictionary<String, ?> properties;
		// declared symbolic name of bundle to track
		private String symbolicName;
		// tracker for given symbolic name
		private BundleTracker<?> bundleTracker;
		// tracked bundle + version
		private Bundle bundle;
		private Version version;

		// transient values for current configuration
		private HttpContext httpContext;
		private Dictionary<String, String> contextParams;
		private LoginConfiguration loginConfiguration;
		private List<SecurityConstraintsMapping> securityMappings = new LinkedList<>();

		public HttpContextTracker(String pid) {
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

				// non-null symbolicName is kind if indicator that the configuration is correct
				properties = props;
				symbolicName = properties.get(KEY_BUNDLE_SN).toString().trim();

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
			LOG.info("Customizing WebContainer for bundle {}/{}", symbolicName, version);

			WebContainer wc = getWebContainer();
			if (wc == null) {
				return;
			}

			// we have all we need - it's finally the time to apply PID configuration to WebContainer
			String contextId = (String) properties.get(KEY_CONTEXT_ID);
			if (contextId == null) {
				contextId = WebContainerContext.DefaultContextIds.DEFAULT.getValue();
			}
			// shared contexts are not supported for now...
			httpContext = wc.createDefaultHttpContext(contextId);

			contextParams = collectContextParams(properties);
			if (contextParams != null && !contextParams.isEmpty()) {
				LOG.info("Setting context parameters in WebContainer for bundle \"" + symbolicName + "\": {}", contextParams);
				wc.setContextParam(contextParams, httpContext);
			}

			loginConfiguration = collectLoginConfiguration(properties);
			if (loginConfiguration != null) {
				LOG.info("Registering login configuration in WebContainer for bundle \"" + symbolicName + "\": method={}, realm={}",
						loginConfiguration.authMethod, loginConfiguration.realmName);
				wc.registerLoginConfig(loginConfiguration.authMethod,
						loginConfiguration.realmName,
						loginConfiguration.formLoginPage,
						loginConfiguration.formErrorPage,
						httpContext);
			}

			securityMappings = collectSecurityMappings(properties);
			if (securityMappings != null && !securityMappings.isEmpty()) {
				for (SecurityConstraintsMapping scm: securityMappings) {
					LOG.info("Registering security mappings in WebContainer for bundle \"" + symbolicName + "\": " + scm);
					wc.registerConstraintMapping(scm.name, scm.method, scm.url, null, true, scm.roles, httpContext);
				}
			}

			// altering the context required stopping it - let's (knowing the details) start it again
			wc.end(httpContext);
		}

		/**
		 * Brings given {@link WebContainer} to initial state - to be called within "paxweb-context" executor
		 */
		public void cleanupContext() {
			LOG.info("{}: Restoring WebContainer for bundle {}/{}", this, symbolicName, version);

			WebContainer wc = getWebContainer();
			if (wc == null || httpContext == null) {
				return;
			}

			// we have to revert previous configuration - but current WebContainer interface doesn't allow
			// us to clean everything correctly
			if (securityMappings.size() > 0) {
				wc.unregisterConstraintMapping(httpContext);
				securityMappings.clear();
			}
			if (loginConfiguration != null) {
				// even if before processing there could be another login configuration applied...
				wc.unregisterLoginConfig(httpContext);
				loginConfiguration = null;
			}
			if (contextParams != null && !contextParams.isEmpty()) {
				wc.setContextParam(null, httpContext);
				contextParams = null;
			}

			// cleaning the context required stopping it - let's (knowing the details) start it again
			wc.end(httpContext);

			httpContext = null;
		}

		private LoginConfiguration collectLoginConfiguration(Dictionary<String,?> properties) {
			if (properties != null) {
				if (properties.get(PREFIX_LOGIN_CONFIG + "authMethod") != null) {
					// all other properties are optional
					LoginConfiguration lc = new LoginConfiguration();
					lc.authMethod = properties.get(PREFIX_LOGIN_CONFIG + "authMethod").toString();
					lc.realmName = (String) properties.get(PREFIX_LOGIN_CONFIG + "realmName");
					if (lc.realmName == null) {
						lc.realmName = UUID.randomUUID().toString();
					}
					lc.formLoginPage = (String) properties.get(PREFIX_LOGIN_CONFIG + "formLoginPage");
					lc.formErrorPage = (String) properties.get(PREFIX_LOGIN_CONFIG + "formErrorPage");
					return lc;
				}
			}
			return null;
		}

		private Dictionary<String, String> collectContextParams(Dictionary<String,?> properties) {
			Hashtable<String, String> result = new Hashtable<>();
			if (properties != null) {
				for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
					String k = e.nextElement();
					if (k != null && k.startsWith(PREFIX_CONTEXT_PARAM)) {
						String v = (String) properties.get(k);
						String paramName = k.substring(PREFIX_CONTEXT_PARAM.length());
						result.put(paramName, v);
					}
				}
			}
			return result;
		}

		private List<SecurityConstraintsMapping> collectSecurityMappings(Dictionary<String,?> properties) {
			List<SecurityConstraintsMapping> result = new LinkedList<>();
			if (properties != null) {
				Map<String, SecurityConstraintsMapping> temp = new HashMap<>();
				for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
					String k = e.nextElement();
					if (k != null && k.startsWith(PREFIX_SECURITY)) {
						String paramName = k.substring(PREFIX_SECURITY.length());
						if (paramName.contains(".")) {
							String constraintName = paramName.substring(0, paramName.lastIndexOf('.'));
							SecurityConstraintsMapping model = temp.computeIfAbsent(constraintName, n -> new SecurityConstraintsMapping());
							model.name = constraintName;
							String v = (String) properties.get(k);
							if (paramName.endsWith(".url")) {
								model.url = v;
							} else if (paramName.endsWith(".method")) {
								model.method = v;
							} else if (paramName.endsWith(".roles")) {
								model.roles.addAll(Arrays.asList(v.split("\\s*,\\s*")));
							}
						}
					}
				}

				for (SecurityConstraintsMapping scm: temp.values()) {
					if (scm.url != null) {
						result.add(scm);
					}
				}

			}
			return result;
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
		}

		/**
		 * Gets a {@link WebContainer} service for tracked bundle
		 * @return
		 */
		private WebContainer getWebContainer() {
			Bundle _bundle = bundle;
			if (_bundle == null) {
				LOG.debug("Bundle context for {} bundle is no longer valid", symbolicName);
				return null;
			}

			BundleContext context = _bundle.getBundleContext();
			try {
				ServiceReference<WebContainer> sr = context.getServiceReference(WebContainer.class);
				if (sr == null) {
					LOG.warn("Can't obtain service reference for WebContainer for bundle {}", symbolicName);
					return null;
				}
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

		private class LoginConfiguration {
			String authMethod;
			String realmName;
			String formLoginPage;
			String formErrorPage;
		}

		private class SecurityConstraintsMapping {
			String name;
			String url;
			String method;
			List<String> roles = new LinkedList<>();

			@Override
			public String toString() {
				return "SecurityConstraintsMapping{" + "name='" + name + '\'' +
						", url='" + url + '\'' +
						", roles=" + roles +
						'}';
			}
		}
	}

}
