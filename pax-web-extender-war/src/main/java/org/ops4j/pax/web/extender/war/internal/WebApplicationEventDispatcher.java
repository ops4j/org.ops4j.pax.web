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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class was inspired by BlueprintEventDispatcher for firing WebEvents.</p>
 *
 * <p>Dispatcher of events related to registration/unregistration of <em>web applications</em>. For events related
 * to particular <em>web elements</em> see {@code org.ops4j.pax.web.service.internal.WebElementEventDispatcher} in
 * pax-web-runtime and {@link WebElementEventListener}.</p>
 *
 * <p>It's activated using a method from {@link WebApplicationEventListener} that called to <em>send</em> the event
 * and the event is passed to other registered {@link WebApplicationEventListener}s.</p>
 */
public class WebApplicationEventDispatcher implements WebApplicationEventListener,
		ServiceTrackerCustomizer<WebApplicationEventListener, WebApplicationEventListener>, BundleListener {

	private static final Logger LOG = LoggerFactory.getLogger(WebApplicationEventDispatcher.class);

	private final BundleContext bundleContext;
	private final ExecutorService executor;

	/** {@link ServiceTracker} for {@link WebApplicationEventListener web app listeners} */
	private final ServiceTracker<WebApplicationEventListener, WebApplicationEventListener> webApplicationListenerTracker;

	/** All tracked {@link WebApplicationEventListener web app listeners} */
	private final Set<WebApplicationEventListener> listeners = new CopyOnWriteArraySet<>();

//	private final Map<Long, WebEvent> states = new ConcurrentHashMap<>();

	// TODO: Move EventAdmin and LogService trackers out of this dispatcher and register as
	//       WebApplicationEventListeners - the same way as with
	//       org.ops4j.pax.web.service.internal.WebElementEventDispatcher

//	private final Bundle bundle;
//	private final ScheduledExecutorService executors;
//	private final ServiceTracker<EventAdmin, EventAdmin> eventAdminTracker;
//	private final ServiceTracker<LogService, LogService> logServiceTracker;

	public WebApplicationEventDispatcher(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		this.executor = Executors.newFixedThreadPool(1, new NamedThreadFactory("wab-events"));

		this.webApplicationListenerTracker = new ServiceTracker<>(bundleContext, WebApplicationEventListener.class.getName(), this);
		this.webApplicationListenerTracker.open();

		this.bundleContext.addBundleListener(this);

//		// Use filter so that the package can be optional
//		Filter filterEvent = bundleContext.createFilter("(objectClass=org.osgi.service.event.EventAdmin)");
//		this.eventAdminTracker = new ServiceTracker<>(bundleContext, filterEvent, null);
//		this.eventAdminTracker.open();
//
//		// Use filter so that the package can be optional
//		Filter filterLog = bundleContext.createFilter("(objectClass=org.osgi.service.log.LogService)");
//		this.logServiceTracker = new ServiceTracker<>(bundleContext, filterLog, null);
//		this.logServiceTracker.open();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		// TODO_WAB: clean up listeners for given bundle
//		if (event.getType() == BundleEvent.STOPPED || event.getType() == BundleEvent.UNINSTALLED) {
//			states.remove(event.getBundle().getBundleId());
//		}
	}

	@Override
	public WebApplicationEventListener addingService(ServiceReference<WebApplicationEventListener> reference) {
		WebApplicationEventListener listener = bundleContext.getService(reference);
		if (listener != null) {
			LOG.debug("New WebApplicationEventListener added: {}", listener.getClass().getName());
			synchronized (listeners) {
				// TOCHECK: should we really send (and keep!) initial events? (it's only for Karaf command actually)
//				sendInitialEvents(listener);
				listeners.add(listener);
			}
		}
		return listener;
	}

	@Override
	public void modifiedService(ServiceReference<WebApplicationEventListener> reference, WebApplicationEventListener service) {
	}

	@Override
	public void removedService(ServiceReference<WebApplicationEventListener> reference, WebApplicationEventListener service) {
		listeners.remove(service);
		bundleContext.ungetService(reference);
		LOG.debug("WebApplicationEventListener is removed: {}", service.getClass().getName());
	}

	@Override
	public void webEvent(WebApplicationEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle " + event.getBundleName());
		}
		synchronized (listeners) {
			callListeners(event);
//			states.put(webEvent.getBundleId(), webEvent);
		}

//		final String topic;
//		switch (webEvent.getType()) {
//			case WebEvent.DEPLOYING:
//				topic = WebTopic.DEPLOYING.toString();
//				break;
//			case WebEvent.DEPLOYED:
//				topic = WebTopic.DEPLOYED.toString();
//				break;
//			case WebEvent.UNDEPLOYING:
//				topic = WebTopic.UNDEPLOYING.toString();
//				break;
//			case WebEvent.UNDEPLOYED:
//				topic = WebTopic.UNDEPLOYED.toString();
//				break;
//			case WebEvent.FAILED:
//				topic = WebTopic.FAILED.toString();
//				break;
//			case WebEvent.WAITING:
//				// topic = WebTopic.WAITING.toString();
//				// A Waiting Event is not supported by the specification
//				// therefore it is mapped to FAILED, because of collision.
//				topic = WebTopic.FAILED.toString();
//				break;
//			default:
//				topic = WebTopic.FAILED.toString();
//		}
//
//		try {
//			executors.submit(new Runnable() {
//				public void run() {
//					Dictionary<String, Object> properties = new Hashtable<>();
//					properties.put("bundle.symbolicName", webEvent
//							.getBundleName());
//					properties.put("bundle.id", webEvent.getBundleId());
//					String bundleVersionObject = webEvent.getBundleVersion();
//					Version bundleVersion;
//					if (bundleVersionObject != null) {
//						bundleVersion = new Version(
//								(String) bundleVersionObject);
//					} else {
//						bundleVersion = new Version("0.0.0");
//					}
//					properties.put("bundle.version", bundleVersion);
//					properties.put("context.path",
//							webEvent.getContextPath());
//					properties.put("timestamp", webEvent.getTimestamp());
//					properties.put("extender.bundle.id", webEvent
//							.getExtenderBundleId());
//					properties.put("extender.bundle.symbolicName", webEvent
//							.getExtenderBundleName());
//					String extenderBundleVersionObject = webEvent
//							.getExtenderBundleVersion();
//					Version extenderVersion;
//					if (extenderBundleVersionObject != null) {
//						extenderVersion = new Version(
//								(String) extenderBundleVersionObject);
//					} else {
//						extenderVersion = new Version("0.0.0");
//					}
//					properties.put("extender.bundle.version",
//							extenderVersion);
//
//					if (webEvent.getCause() != null) {
//						properties.put("exception", webEvent.getCause());
//					}
//
//					if (webEvent.getCollisionIds() != null) {
//						properties.put("collision",
//								webEvent.getContextPath());
//						properties.put("collision.bundles",
//								webEvent.getCollisionIds());
//					}
//
//					Event event = new Event(topic, properties);
//					EventAdmin adminService = getEventAdmin();
//					if (adminService != null) {
//						adminService.postEvent(event);
//					}
//
//				}
//			});
//		} catch (RejectedExecutionException ree) {
//			LOG.warn("Executor shut down", ree);
//		}
//
//		try {
//			executors.submit(new Runnable() {
//				public void run() {
//					LogService logService = getLogService();
//					if (logService != null) {
//						logService.log(LogService.LOG_DEBUG, topic);
//					} else {
//						if (webEvent.getCause() != null) {
//							LOG.error(webEvent.toString());
//						} else {
//							LOG.debug(topic);
//						}
//					}
//				}
//			});
//		} catch (RejectedExecutionException ree) {
//			LOG.warn("Executor shut down", ree);
//		}
	}

	void destroy() {
		bundleContext.removeBundleListener(this);
//		// clean up the EventAdmin tracker if we're using that
//		eventAdminTracker.close();
//		logServiceTracker.close();
		webApplicationListenerTracker.close();
		executor.shutdown();
		// wait for the queued tasks to execute
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
		}
	}

//	private void sendInitialEvents(WebListener listener) {
//		for (Map.Entry<Long, WebEvent> entry : states.entrySet()) {
//			try {
//				callListener(listener, new WebEvent(entry.getValue(), true));
//			} catch (RejectedExecutionException ree) {
//				LOG.warn("Executor shut down", ree);
//				break;
//			}
//		}
//	}

//	public void removeWebApp(WebApp webApp) {
//		states.remove(webApp.getBundle().getBundleId());
//	}
//
//	public void webEvent(WebApp webApp, int type) {
//		webEvent(webApp, type, (Throwable) null);
//	}
//
//	public void webEvent(WebApp webApp, int type, Throwable t) {
//		webEvent(new WebEvent(type, "/" + webApp.getContextName(),
//				webApp.getBundle(), bundle, t));
//	}
//
//	public void webEvent(WebApp webApp, int type, Collection<Long> ids) {
//		webEvent(new WebEvent(type, "/" + webApp.getContextName(),
//				webApp.getBundle(), bundle, ids));
//	}
//
//	public void webEvent(WebApp webApp, int type, HttpService httpService) {
//		webEvent(new WebEvent(type, "/" + webApp.getContextName(),
//				webApp.getBundle(), bundle, httpService,
//				webApp.getHttpContext()));
//	}

	private void callListeners(WebApplicationEvent event) {
		for (WebApplicationEventListener listener : listeners) {
			try {
				callListener(listener, event);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final WebApplicationEventListener listener, final WebApplicationEvent event) {
		try {
			executor.invokeAny(Collections.<Callable<Void>>singleton(() -> {
				listener.webEvent(event);
				return null;
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

}
