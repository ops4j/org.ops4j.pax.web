/*
 * Copyright 2011 Achim Nierbeck.
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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebEvent.WebTopic;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was inspired by BlueprintEventDispatcher for firing WebEvents
 * 
 */
public class WebEventDispatcher implements WebListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(WebEventDispatcher.class);

	private final ScheduledExecutorService executors;
	private EventAdmin eventAdminService;
	private LogService logService;
	private final ServiceTracker<WebListener, WebListener> webListenerTracker;
	private final Set<WebListener> listeners = new CopyOnWriteArraySet<WebListener>();
	private final Map<Bundle, WebEvent> states = new ConcurrentHashMap<Bundle, WebEvent>();

	public WebEventDispatcher(final BundleContext bundleContext,
			ScheduledExecutorService executors) {

		NullArgumentException.validateNotNull(bundleContext, "Bundle Context");
		NullArgumentException.validateNotNull(executors, "Thread executors");

		this.executors = executors;

		this.webListenerTracker = new ServiceTracker<WebListener, WebListener>(
				bundleContext, WebListener.class.getName(),
				new ServiceTrackerCustomizer<WebListener, WebListener>() {
					@Override
					public WebListener addingService(
							ServiceReference<WebListener> reference) {
						WebListener listener = bundleContext
								.getService(reference);

						synchronized (listeners) {
							sendInitialEvents(listener);
							listeners.add(listener);
						}

						return listener;
					}

					@Override
					public void modifiedService(
							ServiceReference<WebListener> reference,
							WebListener service) {
					}

					@Override
					public void removedService(
							ServiceReference<WebListener> reference,
							WebListener service) {
						listeners.remove(service);
						bundleContext.ungetService(reference);
					}
				});
		this.webListenerTracker.open();
	}

	void destroy() {
		executors.shutdown();
		// wait for the queued tasks to execute
		try {
			executors.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
		webListenerTracker.close();
		// clean up the EventAdmin tracker if we're using that
		eventAdminService = null;
	}

	private void sendInitialEvents(WebListener listener) {
		for (Map.Entry<Bundle, WebEvent> entry : states.entrySet()) {
			try {
				callListener(listener, new WebEvent(entry.getValue(), true));
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ops4j.pax.web.service.spi.WebListener#webEvent(org.ops4j.pax.web.
	 * service.spi.WebEvent)
	 */
	public void webEvent(final WebEvent webEvent) {

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + webEvent + " for bundle "
					+ webEvent.getBundle().getSymbolicName());
		}

		synchronized (listeners) {
			callListeners(webEvent);
			states.put(webEvent.getBundle(), webEvent);
		}

		final String topic;
		switch (webEvent.getType()) {
		case WebEvent.DEPLOYING:
			topic = WebTopic.DEPLOYING.toString();
			break;
		case WebEvent.DEPLOYED:
			topic = WebTopic.DEPLOYED.toString();
			break;
		case WebEvent.UNDEPLOYING:
			topic = WebTopic.UNDEPLOYING.toString();
			break;
		case WebEvent.UNDEPLOYED:
			topic = WebTopic.UNDEPLOYED.toString();
			break;
		case WebEvent.FAILED:
			topic = WebTopic.FAILED.toString();
			break;
		case WebEvent.WAITING:
			// topic = WebTopic.WAITING.toString();
			// A Waiting Event is not supported by the specification
			// therefore it is mapped to FAILED, because of collision.
			topic = WebTopic.FAILED.toString();
			break;
		default:
			topic = WebTopic.FAILED.toString();
		}

		if (eventAdminService != null) {

			try {
				executors.submit(new Runnable() {
					public void run() {
						Dictionary<String, Object> properties = new Hashtable<String, Object>();
						properties.put("bundle.symbolicName", webEvent
								.getBundle().getSymbolicName());
						properties.put("bundle.id", webEvent.getBundle()
								.getBundleId());
						properties.put("bundle", webEvent.getBundle());
						Object bundleVersionObject = webEvent.getBundle()
								.getHeaders().get(Constants.BUNDLE_VERSION);
						Version bundleVersion;
						if (bundleVersionObject instanceof Version) {
							bundleVersion = (Version) bundleVersionObject;
						} else if (bundleVersionObject instanceof String) {
							bundleVersion = new Version(
									(String) bundleVersionObject);
						} else {
							bundleVersion = new Version("0.0.0");
						}
						properties.put("bundle.version", bundleVersion);
						properties.put("context.path",
								webEvent.getContextPath());
						properties.put("timestamp", webEvent.getTimestamp());
						properties.put("extender.bundle",
								webEvent.getExtenderBundle());
						properties.put("extender.bundle.id", webEvent
								.getExtenderBundle().getBundleId());
						properties.put("extender.bundle.symbolicName", webEvent
								.getExtenderBundle().getSymbolicName());
						Object extenderBundleVersionObject = webEvent
								.getExtenderBundle().getHeaders()
								.get(Constants.BUNDLE_VERSION);
						Version extenderVersion;
						if (extenderBundleVersionObject instanceof Version) {
							extenderVersion = (Version) extenderBundleVersionObject;
						} else if (extenderBundleVersionObject instanceof String) {
							extenderVersion = new Version(
									(String) extenderBundleVersionObject);
						} else {
							extenderVersion = new Version("0.0.0");
						}
						properties.put("extender.bundle.version",
								extenderVersion);

						if (webEvent.getCause() != null) {
							properties.put("exception", webEvent.getCause());
						}

						if (webEvent.getCollisionIds() != null) {
							properties.put("collision",
									webEvent.getContextPath());
							properties.put("collision.bundles",
									webEvent.getCollisionIds());
						}

						Event event = new Event(topic, properties);
						EventAdmin adminService = getEventAdminService();
						if (adminService != null) {
							adminService.postEvent(event);
						}

					}
				});
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
			}

		}

		if (logService != null) {
			try {
				executors.submit(new Runnable() {
					public void run() {
						getLogService().log(LogService.LOG_DEBUG, topic);
					}
				});
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
			}

		} else {
			if (webEvent.getCause() != null) {
				LOG.error(webEvent.toString());
			} else {
				LOG.debug(topic);
			}
		}
	}

	private EventAdmin getEventAdminService() {
		return eventAdminService;
	}

	private LogService getLogService() {
		return logService;
	}

	private void callListeners(WebEvent webEvent) {
		for (WebListener listener : listeners) {
			try {
				callListener(listener, webEvent);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final WebListener listener, final WebEvent event) {
		try {
			executors.invokeAny(Collections
					.<Callable<Void>> singleton(new Callable<Void>() {
						public Void call() throws Exception {
							listener.webEvent(event);
							return null;
						}
					}), 60L, TimeUnit.SECONDS);
		} catch (InterruptedException ie) {
			LOG.warn("Thread interrupted", ie);
			Thread.currentThread().interrupt();
		} catch (TimeoutException te) {
			LOG.warn("Listener timed out, will be ignored", te);
			listeners.remove(listener);
		} catch (ExecutionException ee) {
			LOG.warn("Listener caused an exception, will be ignored", ee);
			listeners.remove(listener);
		}
	}

	public void setEventAdminService(Object eventService) {
		this.eventAdminService = (EventAdmin) eventService;
	}

	public void setLogService(Object logService) {
		this.logService = (LogService) logService;
	}

}
