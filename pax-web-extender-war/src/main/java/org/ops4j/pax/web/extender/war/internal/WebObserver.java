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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.service.spi.WarManager;
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
//	protected final WebAppPublisher publisher;
//
//	/**
//	 * Extender bundle context
//	 */
//	protected final BundleContext bundleContext;
//	/**
//	 * Event dispatcher used to notify listeners and send eventAdmin events
//	 */
//	protected final WebApplicationEventDispatcher eventDispatcher;
//	/**
//	 * Manage dependencies
//	 */
//	protected final DefaultWebAppDependencyManager dependencyManager;
//	/**
//	 * Parser to build the WebApp from the bundles
//	 */
//	protected final WebAppParser parser;

	/**
	 * Logger.
	 */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * The queue of published WebApp objects to a context.
         * The map was wrapped into map for virtual hosts.
	 */
	private final Map<String,Map<String, List<BundleWebApplication>>> contexts = new HashMap<>();
        
    /**
      * This virtual host is used if there is no Web-VirtualHosts in manifest.
      */
    private final String defaultVirtualHost = "default";

	/**
	 * Parse the web app and create the extension that will be managed by the
	 * extender.
	 */
//	public Extension createExtension(final Bundle bundle) {
//		// Start web app creation
//		try {
//			eventDispatcher.webEvent(webApp, WebEvent.DEPLOYING);
//			parser.parse(bundle, webApp);
//
//			String requireCapabilityHeader = ManifestUtil.getHeader(bundle,
//					"Require-Capability");
//			// If the header isn't present Pax-Web is able to take care of it.
//			// otherwise needs support by Pax-CDI
//			if (requireCapabilityHeader == null
//					|| !requireCapabilityHeader.contains("osgi.extender=pax.cdi")) {
//				webApp.setHasDependencies(true);
//				dependencyManager.addWebApp(webApp);
//			}
//
//			// Register the web app
//			synchronized (webApps) {
//				webApps.put(bundle.getBundleId(), webApp);
//			}
//
//			return new SimpleExtension(bundle) {
//				@Override
//				public void doStart() {
//					// Check if the web app has already been destroyed
//					synchronized (webApps) {
//						if (!webApps.containsKey(bundle.getBundleId())) {
//							return;
//						}
//					}
//					deploy(webApp);
//				}
//
//				@Override
//				public void doDestroy() {
//					// Flag this web app has destroyed by removing it
//					// from the list
//					dependencyManager.removeWebApp(webApp);
//					undeploy(webApp);
//					eventDispatcher.removeWebApp(webApp);
//				}
//			};
//			//CHECKSTYLE:OFF
//		} catch (Exception e) {
//			LOG.error(
//					"Error scanning web bundle " + bundle + ": "
//							+ e.getMessage(), e);
//			eventDispatcher.webEvent(webApp, WebEvent.FAILED, e);
//			return null;
//		}
//		//CHECKSTYLE:ON
//	}

	public void deploy(BundleWebApplication webApp) {
		Collection<Long> duplicateIds = null;
                for (Map.Entry<String,List<BundleWebApplication>> entry:getQueues(webApp).entrySet()) {
                    List<BundleWebApplication> queue = entry.getValue();
                    synchronized (queue) {
                            if (queue.isEmpty()) {
                                    queue.add(webApp);
                            } else {
                                    queue.add(webApp);
                                    duplicateIds = new LinkedList<>();
//                                    for (BundleWebApplication duplicateWebApp : queue) {
//                                            duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
//                                    }
                            }
                    }
                }
//		if (duplicateIds == null) {
//			publisher.publish(webApp);
//		} else {
//			webApp.setDeploymentState(WAITING);
//			eventDispatcher.webEvent(webApp, WAITING, duplicateIds);
//		}
	}

	public void undeploy(BundleWebApplication webApp) {
		// Are we the published web app??
		boolean unpublish = false;
		boolean undeploy = false;
		BundleWebApplication next = null;
                for (Map.Entry<String,List<BundleWebApplication>> entry:getQueues(webApp).entrySet()) {
                    List<BundleWebApplication> queue = entry.getValue();
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
//                                                    contexts.get(entry.getKey()).remove(webApp.getContextName());
                                            }
                                    }
                            } else if (queue.remove(webApp)) {
                                    undeploy = true;
                            }
                    }
                }
		if (unpublish) {
//			webApp.setDeploymentState(UNDEPLOYED);
//			eventDispatcher.webEvent(webApp, UNDEPLOYING);
//			publisher.unpublish(webApp);
//			eventDispatcher.webEvent(webApp, UNDEPLOYED);
		} else if (undeploy) {
//			webApp.setDeploymentState(UNDEPLOYED);
//			eventDispatcher.webEvent(webApp, UNDEPLOYED);
		} else {
			LOG.debug("Web application was not in the deployment queue");
		}
		if (next != null) {
//			eventDispatcher.webEvent(next, DEPLOYING);
//			publisher.publish(next);
		}
	}

	private Map<String,List<BundleWebApplication>> getQueues(BundleWebApplication webApp) {
                Map<String,List<BundleWebApplication>> queues = new HashMap<>();
                synchronized (contexts) {
//                    List<String>virtualHosts = webApp.getVirtualHostList();
//                    if (virtualHosts == null || virtualHosts.isEmpty()) {
//                        virtualHosts = new ArrayList<>();
//                        virtualHosts.add(defaultVirtualHost);
//                    }
//                    for (String virtualHost:virtualHosts) {
//                        Map<String,List<BundleWebApplication>> virtualHostContexts = contexts.get(virtualHost);
//                        if (virtualHostContexts == null) {
//                            virtualHostContexts = new HashMap<>();
//                            contexts.put(virtualHost, virtualHostContexts);
//                        }
//                        List<BundleWebApplication> queue = virtualHostContexts.get(webApp.getContextName());
//                        if (queue == null) {
//                            queue = new LinkedList<>();
//                            virtualHostContexts.put(webApp.getContextName(), queue);
//                        }
//                        queues.put(virtualHost, queue);
//                    }
                    return queues;
                }
	}

	public int start(long bundleId, String contextName) {
		BundleWebApplication webApp;
//		synchronized (webApps) {
//			webApp = webApps.get(bundleId);
//		}
//		if (webApp == null) {
//			return WAR_NOT_FOUND;
//		}
//		if (webApp.getDeploymentState() != UNDEPLOYED) {
//			return ALREADY_STARTED;
//		}
//		if (contextName != null) {
//			webApp.setContextName(contextName);
//		}
//		deploy(webApp);
		return SUCCESS;
	}

	public int stop(long bundleId) {
		BundleWebApplication webApp;
//		synchronized (webApps) {
//			webApp = webApps.get(bundleId);
//		}
//		if (webApp == null) {
//			return WAR_NOT_FOUND;
//		}
//		if (webApp.getDeploymentState() == UNDEPLOYED) {
//			return ALREADY_STOPPED;
//		}
//		undeploy(webApp);
		return SUCCESS;
	}

}