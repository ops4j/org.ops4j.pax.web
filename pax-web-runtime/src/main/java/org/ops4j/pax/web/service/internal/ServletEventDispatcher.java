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

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 * 
 */
public class ServletEventDispatcher implements ServletListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServletEventDispatcher.class);

	private final ScheduledExecutorService executors;
	private final ServiceTracker<ServletListener,ServletListener> servletListenerTracker;
	private final Set<ServletListener> listeners = new CopyOnWriteArraySet<ServletListener>();
	private final Map<Bundle, ServletEvent> states = new ConcurrentHashMap<Bundle, ServletEvent>();

	public ServletEventDispatcher(final BundleContext bundleContext) {
		NullArgumentException.validateNotNull(bundleContext, "Bundle Context");
		this.executors = Executors.newScheduledThreadPool(3, new ThreadFactory() {

	            private final AtomicInteger count = new AtomicInteger();

	            public Thread newThread(Runnable r) {
	                final Thread t = Executors.defaultThreadFactory().newThread(r);
	                t.setName("ServletEventDispatcher" + ": " + count.incrementAndGet());
	                t.setDaemon(true);
	                return t;
	            }
	        });

		this.servletListenerTracker = new ServiceTracker<ServletListener,ServletListener>(bundleContext,
				ServletListener.class.getName(),
				new ServiceTrackerCustomizer<ServletListener,ServletListener>() {
					public ServletListener addingService(ServiceReference<ServletListener> reference) {
						ServletListener listener = bundleContext.getService(reference);
						if (listener != null) {
        						LOG.debug("New ServletListener added: {}", listener.getClass().getName());
        						synchronized (listeners) {
        							sendInitialEvents(listener);
        							listeners.add(listener);
        						}
						}
						return listener;
					}

					public void modifiedService(ServiceReference<ServletListener> reference,
							ServletListener service) {
					}

					public void removedService(ServiceReference<ServletListener> reference,
							ServletListener service) {
						listeners.remove(service);
						bundleContext.ungetService(reference);
						LOG.debug("ServletListener is removed: {}", service.getClass().getName());
					}
				});
		this.servletListenerTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ops4j.pax.web.service.spi.ServletListener#servletEvent(org.ops4j.
	 * pax.web.service.spi.ServletEvent)
	 */
	public void servletEvent(final ServletEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending web event " + event + " for bundle "
					+ event.getBundle().getSymbolicName());
		}
		synchronized (listeners) {
			callListeners(event);
			states.put(event.getBundle(), event);
		}
	}
	
	void destroy() {
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
		for (Map.Entry<Bundle, ServletEvent> entry : states.entrySet()) {
			try {
				callListener(listener, new ServletEvent(entry.getValue(), true));
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
			final ServletEvent event) throws RejectedExecutionException {
		try {
			executors.invokeAny(Collections
					.<Callable<Void>> singleton(new Callable<Void>() {
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
