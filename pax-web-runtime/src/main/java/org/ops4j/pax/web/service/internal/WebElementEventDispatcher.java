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
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
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
 * <p>Dispatcher of events related to registration/unregistration of <em>web elements</em> (servlets, filters, ...).</p>
 *
 * <p>It works at lower level than entire <em>web application</em> (or WAR/WAB).</p>
 *
 * <p>It's activated using a method from {@link WebElementEventListener} that called to <em>send</em> the event and the
 * event is passed to other registered {@link WebElementEventListener}s.</p>
 *
 * @author Achim Nierbeck
 */
public class WebElementEventDispatcher implements WebElementEventListener,
		ServiceTrackerCustomizer<WebElementEventListener, WebElementEventListener>, BundleListener {

	private static final Logger LOG = LoggerFactory.getLogger(WebElementEventDispatcher.class);

	private final BundleContext bundleContext;
	private final ExecutorService executor;

	/** {@link ServiceTracker} for {@link WebElementEventListener web element listeners} */
	private final ServiceTracker<WebElementEventListener, WebElementEventListener> webElementListenerTracker;

	/** All tracked {@link WebElementEventListener web element listeners} */
	private final Set<WebElementEventListener> listeners = new CopyOnWriteArraySet<>();

	public WebElementEventDispatcher(final BundleContext bundleContext, Configuration configuration) {
		this.bundleContext = bundleContext;
		this.executor = Executors.newFixedThreadPool(configuration.server().getEventDispatcherThreadCount(),
				new NamedThreadFactory("events"));

		this.webElementListenerTracker = new ServiceTracker<>(bundleContext, WebElementEventListener.class.getName(), this);
		this.webElementListenerTracker.open();

		this.bundleContext.addBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
	}

	@Override
	public WebElementEventListener addingService(ServiceReference<WebElementEventListener> reference) {
		WebElementEventListener listener = bundleContext.getService(reference);
		if (listener != null) {
			LOG.debug("New WebElementEventListener added: {}", listener.getClass().getName());
			synchronized (listeners) {
				listeners.add(listener);
			}
		}
		return listener;
	}

	@Override
	public void modifiedService(ServiceReference<WebElementEventListener> reference, WebElementEventListener service) {
	}

	@Override
	public void removedService(ServiceReference<WebElementEventListener> reference, WebElementEventListener service) {
		listeners.remove(service);
		bundleContext.ungetService(reference);
		LOG.debug("WebElementEventListener is removed: {}", service.getClass().getName());
	}

	@Override
	public void registrationEvent(final WebElementEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web element event " + event + " for bundle " + event.getBundleName());
		}
		synchronized (listeners) {
			callListeners(event);
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

	private void callListeners(WebElementEvent event) {
		for (WebElementEventListener listener : listeners) {
			try {
				callListener(listener, event);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final WebElementEventListener listener, final WebElementEvent event) {
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
