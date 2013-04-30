/*
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

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.ops4j.pax.web.extender.war.internal.parser.WebAppParser.canSeeClass;
import static org.ops4j.pax.web.extender.war.internal.util.ManifestUtil.getHeader;
import static org.ops4j.pax.web.service.spi.WebEvent.DEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYED;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.WAITING;

public class WebObserver implements WarManager {

	/**
	 * Logger.
	 */
	static final Logger LOG = LoggerFactory.getLogger(WebObserver.class);
	/**
	 * Web app publisher.
	 */
	protected final WebAppPublisher publisher;
	/**
	 * Mapping between the bundle id and WebApp
	 */
	protected final Map<Long, WebApp> webApps = new HashMap<Long, WebApp>();
	/**
	 * Extender bundle context
	 */
	protected final BundleContext bundleContext;
	/**
	 * Event dispatcher used to notify listeners and send eventAdmin events
	 */
	protected final WebEventDispatcher eventDispatcher;
	/**
	 * Manage dependencies
	 */
	protected final DefaultWebAppDependencyManager dependencyManager;
	/**
	 * Parser to build the WebApp from the bundles
	 */
	protected final WebAppParser parser;

	/**
	 * Logger.
	 */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * The queue of published WebApp objects to a context.
	 */
	private final Map<String, List<WebApp>> contexts = new HashMap<String, List<WebApp>>();

	public WebObserver(WebAppParser parser, WebAppPublisher publisher,
			WebEventDispatcher eventDispatcher,
			DefaultWebAppDependencyManager dependencyManager,
			BundleContext bundleContext) {

		NullArgumentException.validateNotNull(parser, "Web App Parser");
		NullArgumentException.validateNotNull(publisher, "Web App Publisher");
		NullArgumentException.validateNotNull(eventDispatcher,
				"WebEvent Dispatcher");
		NullArgumentException.validateNotNull(dependencyManager,
				"DefaultWebAppDependencyManager");
		NullArgumentException.validateNotNull(bundleContext, "BundleContext");

		this.parser = parser;
		this.publisher = publisher;
		this.bundleContext = bundleContext;
		this.dependencyManager = dependencyManager;
		this.eventDispatcher = eventDispatcher;
	}

	/**
	 * Parse the web app and create the extension that will be managed by the
	 * extender.
	 */
	public Extension createExtension(final Bundle bundle) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		if (bundle.getState() != Bundle.ACTIVE) {
			LOG.debug("Bundle is not in ACTIVE state, ignore it!");
			return null;
		}

		// Check compatibility
		Boolean canSeeServletClass = canSeeClass(bundle, Servlet.class);
		if (Boolean.FALSE.equals(canSeeServletClass)) {
			LOG.debug(
					"Ignore bundle {} which is not compatible with this extender",
					bundle);
			return null;
		}

		// Check that this is a web bundle
		String contextName = getHeader(bundle, "Web-ContextPath");
		if (contextName == null) {
			return null;
		}
		contextName = contextName.trim();
		if (contextName.startsWith("/")) {
			contextName = contextName.substring(1);
		}

		// Start web app creation
		final WebApp webApp = new WebApp();
		webApp.setDeploymentState(WebEvent.UNDEPLOYED);
		webApp.setBundle(bundle);
		webApp.setContextName(contextName);
		try {
			eventDispatcher.webEvent(webApp, WebEvent.DEPLOYING);

			parser.parse(bundle, webApp);

			String requireCapabilityHeader = ManifestUtil.getHeader(bundle, "Require-Capability");
			String paxManagedBeansHeader = ManifestUtil.getHeader(bundle, "Pax-ManagedBeans");
			// If the header isn't present Pax-Web is able to take care of it.
			// otherwise needs support by Pax-CDI
			if (paxManagedBeansHeader == null 
					&& requireCapabilityHeader == null) { 
				webApp.setHasDependencies(true);
				dependencyManager.addWebApp(webApp);
			} else if (requireCapabilityHeader != null 
					&& !requireCapabilityHeader.contains("osgi.extender=pax.cdi")){
				//needs to be backward compatible
				webApp.setHasDependencies(true);
				dependencyManager.addWebApp(webApp);
			}

			// Register the web app
			synchronized (webApps) {
				webApps.put(bundle.getBundleId(), webApp);
			}

			return new Extension() {
				@Override
				public void start() {
					// Check if the web app has already been destroyed
					synchronized (webApps) {
						if (!webApps.containsKey(bundle.getBundleId())) {
							return;
						}
					}
					deploy(webApp);
				}

				@Override
				public void destroy() {
					// Flag this web app has destroyed by removing it
					// from the list
					synchronized (webApps) {
						webApps.remove(bundle.getBundleId());
					}
					undeploy(webApp);
					dependencyManager.removeWebApp(webApp);
				}
			};
		} catch (Exception e) { //CHECKSTYLE:SKIP
			LOG.error(
					"Error scanning web bundle " + bundle + ": "
							+ e.getMessage(), e);
			eventDispatcher.webEvent(webApp, WebEvent.FAILED, e);
			return null;
		}
	}

	public void deploy(WebApp webApp) {
		List<WebApp> queue = getQueue(webApp);
		synchronized (queue) {
			if (queue.isEmpty()) {
				queue.add(webApp);
				publisher.publish(webApp);
			} else {
				queue.add(webApp);
				Collection<Long> duplicateIds = new LinkedList<Long>();
				for (WebApp duplicateWebApp : queue) {
					duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
				}
				webApp.setDeploymentState(WAITING);
				eventDispatcher.webEvent(webApp, WAITING, duplicateIds);
			}
		}
	}

	public void undeploy(WebApp webApp) {
		// Are we the published web app??
		List<WebApp> queue = getQueue(webApp);
		synchronized (queue) {
			if (!queue.isEmpty() && queue.get(0) == webApp) {

				webApp.setDeploymentState(UNDEPLOYED);
				eventDispatcher.webEvent(webApp, UNDEPLOYING);
				publisher.unpublish(webApp);
				eventDispatcher.webEvent(webApp, UNDEPLOYED);
				queue.remove(0);

				// Below checks if another webapp is waiting for the context, if
				// so the webapp is published.
				LOG.debug("Check for a waiting webapp.");
				if (!queue.isEmpty()) {
					LOG.debug("Found another bundle waiting for the context");
					WebApp next = queue.get(0);

					eventDispatcher.webEvent(next, DEPLOYING);
					publisher.publish(next);
				} else {
					synchronized (contexts) {
						contexts.remove(webApp.getContextName());
					}
				}
			} else if (queue.remove(webApp)) {
				webApp.setDeploymentState(UNDEPLOYED);
				eventDispatcher.webEvent(webApp, UNDEPLOYED);
			} else {
				LOG.debug("Web application was not in the deployment queue");
			}
		}
	}

	private List<WebApp> getQueue(WebApp webApp) {
		synchronized (contexts) {
			List<WebApp> queue = contexts.get(webApp.getContextName());
			if (queue == null) {
				queue = new LinkedList<WebApp>();
				contexts.put(webApp.getContextName(), queue);
			}
			return queue;
		}
	}

	public int start(long bundleId, String contextName) {
		WebApp webApp;
		synchronized (webApps) {
			webApp = webApps.get(bundleId);
		}
		if (webApp == null) {
			return WAR_NOT_FOUND;
		}
		if (webApp.getDeploymentState() != UNDEPLOYED) {
			return ALREADY_STARTED;
		}
		if (contextName != null) {
			webApp.setContextName(contextName);
		}
		deploy(webApp);
		return SUCCESS;
	}

	public int stop(long bundleId) {
		WebApp webApp;
		synchronized (webApps) {
			webApp = webApps.get(bundleId);
		}
		if (webApp == null) {
			return WAR_NOT_FOUND;
		}
		if (webApp.getDeploymentState() == UNDEPLOYED) {
			return ALREADY_STOPPED;
		}
		undeploy(webApp);
		return SUCCESS;
	}

}