/*
 * Copyright 2011 Achim Nierbeck.
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
package org.ops4j.pax.web.service.internal;

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

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.events.ElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementListener;
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
 * <p>Dispatcher of events related to registration/unregistration of <em>web elements</em> (servlets, filters, ...),
 * and <em>web contexts</em> (contexts like {@link org.osgi.service.http.context.ServletContextHelper}).</p>
 *
 * @author Achim Nierbeck
 */
public class WebEventDispatcher implements WebElementListener,
		ServiceTrackerCustomizer<WebElementListener, WebElementListener>, BundleListener {

	private static final Logger LOG = LoggerFactory.getLogger(WebEventDispatcher.class);

	private final BundleContext bundleContext;
	private final ExecutorService executor;

	/** {@link ServiceTracker} for {@link WebElementListener web listeners} */
	private final ServiceTracker<WebElementListener, WebElementListener> webElementListenerTracker;

	/** All tracked {@link WebElementListener web listeners} */
	private final Set<WebElementListener> listeners = new CopyOnWriteArraySet<>();

//	private final Map<Long, Map<String, ElementEvent>> states = new ConcurrentHashMap<>();

	public WebEventDispatcher(final BundleContext bundleContext, Configuration configuration) {
		this.bundleContext = bundleContext;
		this.executor = Executors.newFixedThreadPool(configuration.server().getEventDispatcherThreadCount(),
				new NamedThreadFactory("events"));

		this.webElementListenerTracker = new ServiceTracker<>(bundleContext, WebElementListener.class.getName(), this);
		this.webElementListenerTracker.open();
		this.bundleContext.addBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
//		if (event.getType() == BundleEvent.STOPPED || event.getType() == BundleEvent.UNINSTALLED) {
//			states.remove(event.getBundle().getBundleId());
//		}
	}

	@Override
	public WebElementListener addingService(ServiceReference<WebElementListener> reference) {
		WebElementListener listener = bundleContext.getService(reference);
		if (listener != null) {
			LOG.debug("New ServletListener added: {}", listener.getClass().getName());
			synchronized (listeners) {
				// TOCHECK: should we really send (and keep!) initial events?
//				sendInitialEvents(listener);
				listeners.add(listener);
			}
		}
		return listener;
	}

	@Override
	public void modifiedService(ServiceReference<WebElementListener> reference, WebElementListener service) {
	}

	@Override
	public void removedService(ServiceReference<WebElementListener> reference, WebElementListener service) {
		listeners.remove(service);
		bundleContext.ungetService(reference);
		LOG.debug("ServletListener is removed: {}", service.getClass().getName());
	}

	@Override
	public void registrationEvent(final ElementEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle " + event.getBundleName());
		}
		synchronized (listeners) {
			callListeners(event);
//			Map<String, ElementEvent> events
//					= states.computeIfAbsent(event.getBundleId(), k -> new LinkedHashMap<>());
//			events.put(event.getAlias(), event);
		}
	}

	void destroy() {
		bundleContext.removeBundleListener(this);
		webElementListenerTracker.close();
		executor.shutdown();
		// wait for the queued tasks to execute
		try {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
		}
	}

//	/**
//	 * When a {@link WebElementListener} is added after some web elements/contexts were already added, we'll
//	 * send those initial events to newly registered {@link WebElementListener}.
//	 * @param listener
//	 */
//	private void sendInitialEvents(WebElementListener listener) {
//		for (Map.Entry<Long, Map<String, ElementEvent>> entry : states.entrySet()) {
//			try {
//				if (entry.getValue() != null && !entry.getValue().isEmpty()) {
//					for (ElementEvent event : entry.getValue().values()) {
//						callListener(listener, new ElementEvent(event, true));
//					}
//				}
//			} catch (RejectedExecutionException ree) {
//				LOG.warn("Executor shut down", ree);
//				break;
//			}
//		}
//	}

	private void callListeners(ElementEvent servletEvent) {
		for (WebElementListener listener : listeners) {
			try {
				callListener(listener, servletEvent);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final WebElementListener listener, final ElementEvent event) {
		try {
			executor.invokeAny(Collections.<Callable<Void>>singleton(() -> {
				listener.registrationEvent(event);
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
