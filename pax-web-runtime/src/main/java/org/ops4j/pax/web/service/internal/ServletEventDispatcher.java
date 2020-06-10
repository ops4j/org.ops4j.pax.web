/* Copyright 2011 Achim Nierbeck.
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

package org.ops4j.pax.web.service.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
public class ServletEventDispatcher implements ServletListener, BundleListener {

	private static final int THREAD_POOL_SIZE = 3;

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServletEventDispatcher.class);

	private final BundleContext bundleContext;
	private final ScheduledExecutorService executors;
	private final ServiceTracker<ServletListener, ServletListener> servletListenerTracker;
	private final Set<ServletListener> listeners = new CopyOnWriteArraySet<>();
	private final Map<Long, Map<String, ServletEvent>> states = new ConcurrentHashMap<>();

	public ServletEventDispatcher(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		this.executors = Executors.newScheduledThreadPool(THREAD_POOL_SIZE,
				new ThreadFactory() {

					private final AtomicInteger count = new AtomicInteger();

					@Override
					public Thread newThread(Runnable r) {
						final Thread t = Executors.defaultThreadFactory()
								.newThread(r);
						t.setName("ServletEventDispatcher" + ": "
								+ count.incrementAndGet());
						t.setDaemon(true);
						return t;
					}
				});

		this.servletListenerTracker = new ServiceTracker<>(
				bundleContext,
				ServletListener.class.getName(),
				new ServiceTrackerCustomizer<ServletListener, ServletListener>() {
					@Override
					public ServletListener addingService(
							ServiceReference<ServletListener> reference) {
						ServletListener listener = bundleContext
								.getService(reference);
						if (listener != null) {
							LOG.debug("New ServletListener added: {}", listener
									.getClass().getName());
							synchronized (listeners) {
								sendInitialEvents(listener);
								listeners.add(listener);
							}
						}
						return listener;
					}

					@Override
					public void modifiedService(
							ServiceReference<ServletListener> reference,
							ServletListener service) {
					}

					@Override
					public void removedService(
							ServiceReference<ServletListener> reference,
							ServletListener service) {
						listeners.remove(service);
						bundleContext.ungetService(reference);
						LOG.debug("ServletListener is removed: {}", service
								.getClass().getName());
					}
				});
		this.servletListenerTracker.open();
		this.bundleContext.addBundleListener(this);
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STOPPED
				|| event.getType() == BundleEvent.UNINSTALLED) {
			states.remove(event.getBundle().getBundleId());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ops4j.pax.web.service.spi.ServletListener#servletEvent(org.ops4j.
	 * pax.web.service.spi.ServletEvent)
	 */
	@Override
	public void servletEvent(final ServletEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle "
					+ event.getBundleName());
		}
		synchronized (listeners) {
			callListeners(event);
			Map<String, ServletEvent> events = states.get(event.getBundleId());
			if (events == null) {
				events = new LinkedHashMap<>();
				states.put(event.getBundleId(), events);
			}
			events.put(event.getAlias(), event);
		}
	}

	void destroy() {
		bundleContext.removeBundleListener(this);
		servletListenerTracker.close();
		executors.shutdown();
		// wait for the queued tasks to execute
		try {
			executors.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	private void sendInitialEvents(ServletListener listener) {
		for (Map.Entry<Long, Map<String, ServletEvent>> entry : states
				.entrySet()) {
			try {
				if (entry.getValue() != null && !entry.getValue().isEmpty()) {
					for (ServletEvent event : entry.getValue().values()) {
						callListener(listener, new ServletEvent(event, true));
					}
				}
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListeners(ServletEvent servletEvent) {
		for (ServletListener listener : listeners) {
			try {
				callListener(listener, servletEvent);
			} catch (RejectedExecutionException ree) {
				LOG.warn("Executor shut down", ree);
				break;
			}
		}
	}

	private void callListener(final ServletListener listener,
							  final ServletEvent event) {
		try {
			executors.invokeAny(Collections
					.<Callable<Void>>singleton(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							listener.servletEvent(event);
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

}
