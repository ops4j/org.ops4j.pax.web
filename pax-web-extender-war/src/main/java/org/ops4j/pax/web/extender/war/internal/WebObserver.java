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

import java.util.ArrayList;
import static org.ops4j.pax.web.extender.war.internal.parser.WebAppParser.canSeeClass;
import static org.ops4j.pax.web.extender.war.internal.util.ManifestUtil.getHeader;
import static org.ops4j.pax.web.service.spi.WebEvent.DEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYED;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.WAITING;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Servlet;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.extender.SimpleExtension;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected final Map<Long, WebApp> webApps = new HashMap<>();
	/**
	 * Extender bundle context
	 */
	protected final BundleContext bundleContext;
	/**
	 * Event dispatcher used to notify listeners and send eventAdmin events
	 */
	protected final WebApplicationEventDispatcher eventDispatcher;
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
         * The map was wrapped into map for virtual hosts.
	 */
	private final Map<String,Map<String, List<WebApp>>> contexts = new HashMap<>();
        
    /**
      * This virtual host is used if there is no Web-VirtualHosts in manifest.
      */
    private final String defaultVirtualHost = "default";
        
	public WebObserver(WebAppParser parser, WebAppPublisher publisher,
					   WebApplicationEventDispatcher eventDispatcher,
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

			String requireCapabilityHeader = ManifestUtil.getHeader(bundle,
					"Require-Capability");
			// If the header isn't present Pax-Web is able to take care of it.
			// otherwise needs support by Pax-CDI
			if (requireCapabilityHeader == null
					|| !requireCapabilityHeader.contains("osgi.extender=pax.cdi")) {
				webApp.setHasDependencies(true);
				dependencyManager.addWebApp(webApp);
			}

			// Register the web app
			synchronized (webApps) {
				webApps.put(bundle.getBundleId(), webApp);
			}

			return new SimpleExtension(bundle) {
				@Override
				public void doStart() {
					// Check if the web app has already been destroyed
					synchronized (webApps) {
						if (!webApps.containsKey(bundle.getBundleId())) {
							return;
						}
					}
					if (Optional.ofNullable(ManifestUtil.getHeader(bundle, "Webapp-Deploy")).orElse("true").equals("true")) {
	                    deploy(webApp);
	                } else {
	                    eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED,"/" + webApp.getContextName(), webApp.getBundle(),
	                            bundleContext.getBundle()));
	                }
				}

				@Override
				public void doDestroy() {
					// Flag this web app has destroyed by removing it
					// from the list
					synchronized (webApps) {
						webApps.remove(bundle.getBundleId());
					}
					dependencyManager.removeWebApp(webApp);
					undeploy(webApp);
					eventDispatcher.removeWebApp(webApp);
				}
			};
			//CHECKSTYLE:OFF
		} catch (Exception e) {
			LOG.error(
					"Error scanning web bundle " + bundle + ": "
							+ e.getMessage(), e);
			eventDispatcher.webEvent(webApp, WebEvent.FAILED, e);
			return null;
		}
		//CHECKSTYLE:ON
	}

	public void deploy(WebApp webApp) {
		Collection<Long> duplicateIds = null;
                for (Map.Entry<String,List<WebApp>> entry:getQueues(webApp).entrySet()) {
                    List<WebApp> queue = entry.getValue();
                    synchronized (queue) {
                            if (queue.isEmpty()) {
                                    queue.add(webApp);
                            } else {
                                    queue.add(webApp);
                                    duplicateIds = new LinkedList<>();
                                    for (WebApp duplicateWebApp : queue) {
                                            duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
                                    }
                            }
                    }
                }
		if (duplicateIds == null) {
			publisher.publish(webApp);
		} else {
			webApp.setDeploymentState(WAITING);
			eventDispatcher.webEvent(webApp, WAITING, duplicateIds);
		}
	}

	public void undeploy(WebApp webApp) {
		// Are we the published web app??
		boolean unpublish = false;
		boolean undeploy = false;
		WebApp next = null;
                for (Map.Entry<String,List<WebApp>> entry:getQueues(webApp).entrySet()) {
                    List<WebApp> queue = entry.getValue();
                    synchronized (queue) {
                            if (!queue.isEmpty() && queue.get(0) == webApp) {
                                    unpublish = true;
                                    undeploy = true;
                                    queue.remove(0);
                                    LOG.debug("Check for a waiting webapp.");
                                    if (!queue.isEmpty()) {
                                            LOG.debug("Found another bundle waiting for the context");
                                            next = queue.get(0);
                                    } else {
                                            synchronized (contexts) {
                                                    contexts.get(entry.getKey()).remove(webApp.getContextName());
                                            }
                                    }
                            } else if (queue.remove(webApp)) {
                                    undeploy = true;
                            }
                    }
                }
		if (unpublish) {
			webApp.setDeploymentState(UNDEPLOYED);
			eventDispatcher.webEvent(webApp, UNDEPLOYING);
			publisher.unpublish(webApp);
			eventDispatcher.webEvent(webApp, UNDEPLOYED);
		} else if (undeploy) {
			webApp.setDeploymentState(UNDEPLOYED);
			eventDispatcher.webEvent(webApp, UNDEPLOYED);
		} else {
			LOG.debug("Web application was not in the deployment queue");
		}
		if (next != null) {
			eventDispatcher.webEvent(next, DEPLOYING);
			publisher.publish(next);
		}
	}

	private Map<String,List<WebApp>> getQueues(WebApp webApp) {
                Map<String,List<WebApp>> queues = new HashMap<>();
                synchronized (contexts) {
                    List<String>virtualHosts = webApp.getVirtualHostList();
                    if (virtualHosts == null || virtualHosts.isEmpty()) {
                        virtualHosts = new ArrayList<>();
                        virtualHosts.add(defaultVirtualHost);
                    }
                    for (String virtualHost:virtualHosts) {
                        Map<String,List<WebApp>> virtualHostContexts = contexts.get(virtualHost);
                        if (virtualHostContexts == null) {
                            virtualHostContexts = new HashMap<>();
                            contexts.put(virtualHost, virtualHostContexts);
                        }
                        List<WebApp> queue = virtualHostContexts.get(webApp.getContextName());
                        if (queue == null) {
                            queue = new LinkedList<>();
                            virtualHostContexts.put(webApp.getContextName(), queue);
                        }
                        queues.put(virtualHost, queue);
                    }
                    return queues;
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