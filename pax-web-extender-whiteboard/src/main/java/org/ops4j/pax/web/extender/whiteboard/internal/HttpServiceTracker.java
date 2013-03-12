/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks http services.
 * 
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public class HttpServiceTracker extends
		ServiceTracker<HttpService, HttpService> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceTracker.class);
	/**
	 * An array of listeners to be notified when service come and go.
	 */
	private Collection<HttpServiceListener> listeners;
	/**
	 * The current used http service;
	 */
	private HttpService httpService;
	/**
	 * Lock for thread safe http service usage.
	 */
	private Lock lock;

	/**
	 * Tracks Http Services.
	 * 
	 * @param bundleContext
	 *            a bundle context; mandatory
	 */
	public HttpServiceTracker(final BundleContext bundleContext) {
		super(validateBundleContext(bundleContext), HttpService.class, null);
		listeners = new CopyOnWriteArrayList<HttpServiceListener>();
		lock = new ReentrantLock();
	}

	/**
	 * Validates that the bundle context is not null. If null will throw
	 * IllegalArgumentException
	 * 
	 * @param bundleContext
	 *            a bundle context
	 * 
	 * @return the bundle context if not null
	 */
	private static BundleContext validateBundleContext(
			BundleContext bundleContext) {
		if (bundleContext == null) {
			throw new IllegalArgumentException("Bundle context cannot be null");
		}
		return bundleContext;
	}

	/**
	 * Gets the service if one is not already available and notify listeners.
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public HttpService addingService(
			final ServiceReference<HttpService> serviceReference) {
		LOG.debug("HttpService available {}", serviceReference);
		lock.lock();
		HttpService addedHttpService = null;
		try {
			if (httpService != null) {
				return super.addingService(serviceReference);
			}
			httpService = super.addingService(serviceReference);
			addedHttpService = httpService;
		} finally {
			lock.unlock();
		}
		for (HttpServiceListener listener : listeners) {
			try {
				listener.available(addedHttpService);
			} catch (Exception ignore) { // CHECKSTYLE:SKIP
				LOG.error("Cannot register", ignore);
			}
		}
		return addedHttpService;
	}

	/**
	 * Notify listeners that the http service became unavailable. Then looks for
	 * another one and if available notifies listeners.
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,Object)
	 */
	@Override
	public void removedService(
			final ServiceReference<HttpService> serviceReference,
			final HttpService service) {
		LOG.debug("HttpService removed {}", serviceReference);
		lock.lock();
		HttpService removedHttpService = null;
		try {
			super.removedService(serviceReference, service);
			if (httpService != service) {
				return;
			}
			removedHttpService = httpService;
			httpService = null;
		} finally {
			lock.unlock();
		}
		for (HttpServiceListener listener : listeners) {
			listener.unavailable(removedHttpService);
		}
	}

	public void addListener(final HttpServiceListener listener) {
		listeners.add(listener);
		lock.lock();
		try {
			if (httpService != null) {
				try {
					listener.available(httpService);
				} catch (Exception ignore) { // CHECKSTYLE:SKIP
					LOG.error("Cannot register", ignore);
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public void removeListener(final HttpServiceListener listener) {
		listeners.remove(listener);
		lock.lock();
		try {
			if (httpService != null) {
				listener.unavailable(httpService);
			}
		} finally {
			lock.unlock();
		}
	}

}
