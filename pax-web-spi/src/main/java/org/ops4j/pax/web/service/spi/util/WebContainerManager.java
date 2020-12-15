/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.ops4j.pax.web.service.views.PaxWebContainerView;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class combines a {@link org.osgi.framework.BundleListener} and {@link org.osgi.framework.ServiceListener} to
 * manage at least two scenarios:<ul>
 *     <li>pax-web-extender-whiteboard</li>
 *     <li>pax-web-extender-war</li>
 * </ul>
 * The scenarios are quite similar, because in both cases the information is collected from multiple bundles:<ul>
 *     <li>pax-web-extender-whiteboard - Whiteboard services being registered</li>
 *     <li>pax-web-extender-war - bundles with {@code Web-ContextPath} manifest header being installed</li>
 * </ul>
 * While this information may be processed only when proper registration of {@link org.ops4j.pax.web.service.WebContainer}
 * OSGi service is being registered.
 * </p>
 *
 * <p>This listener manages a (bundle-scoped) reference to <em>current</em> {@link org.ops4j.pax.web.service.WebContainer}
 * and binding to its lifecycle processes the information obtained from multiple <em>client</em> bundles that try
 * to install Web Application related elements/components into the current web container.</p>
 *
 * <p>Before Pax Web 8, for pax-web-extender-war, this was managed by
 * {@code org.ops4j.pax.web.extender.war.internal.tracker.ReplaceableService}. In Pax Web 8, for
 * pax-web-extender-whiteboard, initially I used separate {@link org.osgi.framework.ServiceListener}, but I think it's
 * a good idea to unify this functionality inside pax-web-spi bundle.</p>
 */
public class WebContainerManager implements BundleListener, ServiceTrackerCustomizer<WebContainer, ServiceReference<WebContainer>> {

	public static final Logger LOG = LoggerFactory.getLogger(WebContainerManager.class);

	private final BundleContext context;

	/** {@link ServiceTracker} to track {@link WebContainer} instances to register web components there */
	private final ServiceTracker<WebContainer, ServiceReference<WebContainer>> webContainerTracker;

	/** Current {@link ServiceReference} to compare with other references being added/removed. */
	private final AtomicReference<ServiceReference<WebContainer>> currentWebContainerRef = new AtomicReference<>();

	/**
	 * Higher level listener called during the lifecycle of both the {@link WebContainer} and the bundles that
	 * provide the web elements/contexts/applications.
	 */
	private final WebContainerListener listener;

	private final Executor pool;

	private final Map<ServiceReference<WebContainer>, Map<BundleContext, WebContainer>> containers = new HashMap<>();

	/**
	 * Creates a manager that delivers service events synchronously (for test purposes).
	 *
	 * @param context
	 * @param listener
	 */
	public WebContainerManager(BundleContext context, WebContainerListener listener) {
		this(context, listener, null);
	}

	/**
	 * <p>Creates a manager to handle:<ul>
	 *     <li>the lifecycle of {@link WebContainer} service reference(s)</li>
	 *     <li>the lifecycles of {@link org.osgi.framework.Bundle bundles} that want to interact with a
	 *     {@link WebContainer}.</li>
	 * </ul></p>
	 *
	 * @param context nothing can be done without it
	 * @param listener a required instance of high-level <em>listener to get notified about lifecycle changes</em>
	 * @param threadName a name of the thread in which the instance of {@link WebContainer} service reference will
	 *        be passed. If {@code null}, then events will be delivered synchronously.
	 */
	public WebContainerManager(BundleContext context, WebContainerListener listener, final String threadName) {
		this.context = context;
		this.listener = listener;

		final ThreadFactory tf = Executors.defaultThreadFactory();
		if (threadName != null) {
			pool = Executors.newSingleThreadExecutor(r -> {
				Thread thread = tf.newThread(r);
				thread.setName(threadName);
				return thread;
			});
		} else {
			pool = Runnable::run;
		}

		webContainerTracker = new ServiceTracker<>(context, WebContainer.class, this);
	}

	/**
	 * Initialization sets up bundle and service listeners assuming there are dedicated observers (to get customized
	 * notification) registered.
	 */
	public void initialize() {
		// bundle listener to manager per-bundle cache of information coming from given bundle
		// (elements and contexts in Whiteboard case, entire web applications in War case). This is needed, so
		// when the bundle is gone, all the information is cleared
		context.addBundleListener(this);

		// opening a tracker registers a listener for given filter and calls getServiceReferences(). The
		// initial array of refs is passed to a Tracked() object, then AbstractTracked.trackInitial() is called
		// Tracked object keeps a mapping betwen ServiceReference<> of incoming object and actual customized object
		// AbstractTracked.trackAdding() is called only for those refs without a mapping
		webContainerTracker.open(false);
	}

	/**
	 * Adapter method that's invoked from {@link ServiceTrackerCustomizer} and calls the listeners in a new thread.
	 * @param oldReference
	 * @param newReference
	 */
	private void webContainerChanged(ServiceReference<WebContainer> oldReference, ServiceReference<WebContainer> newReference) {
		currentWebContainerRef.set(newReference);
		pool.execute(() -> listener.webContainerChanged(oldReference, newReference));
	}

	/**
	 * Cleans up internal trackers and thread pools.
	 */
	public void shutdown() {
		webContainerTracker.close();
		context.removeBundleListener(this);

		if (pool instanceof ExecutorService) {
			((ExecutorService) pool).shutdown();
		}
	}

	public ServiceReference<WebContainer> currentWebContainerReference() {
		return currentWebContainerRef.get();
	}

	/**
	 * Helper method that makes it easier to obtain a {@link PaxWebContainerView} using passed
	 * {@link BundleContext} and {@link ServiceReference} to a {@link WebContainer}
	 *
	 * @param context
	 * @param ref
	 * @param viewClass
	 * @return
	 */
	public <T extends PaxWebContainerView> T containerView(BundleContext context, ServiceReference<WebContainer> ref, Class<T> viewClass) {
		if (ref == null || context == null) {
			return null;
		}
		Map<BundleContext, WebContainer> bundleContainers = containers.get(ref);
		if (bundleContainers != null) {
			WebContainer container = bundleContainers.get(context);
			if (container != null) {
				return container.adapt(viewClass);
			}
		}
		WebContainer webContainer = context.getService(ref);
		if (webContainer == null) {
			LOG.warn("Can't get a WebContainer service from {}", ref);
			return null;
		} else {
			containers.computeIfAbsent(ref, r -> new HashMap<>()).put(context, webContainer);
			return webContainer.adapt(viewClass);
		}
	}

	/**
	 * Returns a {@link WhiteboardWebContainerView} for the passed {@link BundleContext} and <em>current</em>
	 * reference to {@link WebContainer} service.
	 * @param context
	 * @return
	 */
	public WhiteboardWebContainerView whiteboardView(BundleContext context) {
		return containerView(context, currentWebContainerRef.get(), WhiteboardWebContainerView.class);
	}

	/**
	 * Returns a {@link WhiteboardWebContainerView} for the passed {@link BundleContext} and the passed
	 * reference to {@link WebContainer} service.
	 * @param context
	 * @param ref
	 * @return
	 */
	public WhiteboardWebContainerView whiteboardView(BundleContext context, ServiceReference<WebContainer> ref) {
		return containerView(context, ref, WhiteboardWebContainerView.class);
	}

	/**
	 * Helper method to release cached {@link PaxWebContainerView} for the passed {@link BundleContext} and
	 * <em>current</em> reference to a {@link WebContainer} service.
	 * @param context
	 */
	public void releaseContainer(BundleContext context) {
		releaseContainer(context, currentWebContainerRef.get());
	}

	/**
	 * Helper method to release cached {@link PaxWebContainerView}
	 * @param context
	 * @param ref
	 */
	public void releaseContainer(BundleContext context, ServiceReference<WebContainer> ref) {
		if (ref == null || context == null) {
			return;
		}
		Map<BundleContext, WebContainer> bundleContainers = containers.get(ref);
		if (bundleContainers != null) {
			WebContainer container = bundleContainers.remove(context);
			if (container != null) {
				context.ungetService(ref);
			}
			if (bundleContainers.isEmpty()) {
				containers.remove(ref);
			}
		}
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() == BundleEvent.STOPPED) {
			listener.bundleStopped(event.getBundle());
		}
	}

	// --- implementation of ServiceTrackerCustomizer
	//     We're never calling context.getService(), as this should be performed within a scope of actual
	//     WAR or Whiteboard bundle!

	@Override
	public ServiceReference<WebContainer> addingService(ServiceReference<WebContainer> reference) {
		// default customizer of ServiceTracker just calls "return context.getService(reference)"
		// this method is called when a ServiceTracker produces a mapping for given service reference
		// for the first time inside org.osgi.util.tracker.AbstractTracked.tracked map when initial refs are tracked
		// but also as a response to ServiceEvent.REGISTERED/ServiceEvent.MODIFIED when there's not existing mapping
		// in the AbstractTracked.tracked map

		// at this point, org.osgi.util.tracker.ServiceTracker.cachedService is not yet cleared and the reference
		// is not yet added to org.osgi.util.tracker.AbstractTracked.tracked map, but we need to know NOW whether
		// this new WebContainer is better than previous best one

		// current reference should be the best
		ServiceReference<WebContainer> currentReference = currentWebContainerRef.get();
		// but potentially the new reference is better

		if (currentReference == null) {
			this.webContainerChanged(null, reference);
		} else if (currentReference.compareTo(reference) < 0) {
			// better reference replaces the current one
			this.webContainerChanged(currentReference, reference);
		}

		return reference;
	}

	@Override
	public void modifiedService(ServiceReference<WebContainer> reference, ServiceReference<WebContainer> service) {
		// default customizer of ServiceTracker doesn't do anything
		// this method is called from Tracked as response to ServiceEvent.REGISTERED/ServiceEvent.MODIFIED when
		// there's existing mapping in the AbstractTracked.tracked map

		// at this point, org.osgi.util.tracker.AbstractTracked.modified() was ALREADY called, so there's no
		// cached reference and service and we can easily find the best service
		ServiceReference<WebContainer> bestReference = webContainerTracker.getServiceReference();
		ServiceReference<WebContainer> currentReference = currentWebContainerRef.get();

		// we don't actually care about the passed reference, because we want the best one and compare it with
		// current one
		if (bestReference != null) {
			if (currentReference == null) {
				// new reference
				this.webContainerChanged(null, bestReference);
			} else if (bestReference.compareTo(currentReference) > 0) {
				// better reference replaces the current one
				this.webContainerChanged(currentReference, bestReference);
			}
		} else if (currentReference != null) {
			// we have currentReference, but there's no best one...
			this.webContainerChanged(currentReference, null);
		}
	}

	@Override
	public void removedService(ServiceReference<WebContainer> reference, ServiceReference<WebContainer> service) {
		// default customizer of ServiceTracker just calls "context.ungetService(reference);"
		// this method is called from Tracked as response to ServiceEvent.MODIFIED_ENDMATCH/ServiceEvent.UNREGISTERING
		// but only if there's a mapping in AbstractTracked.tracked map

		// at this point the reference is already removed from org.osgi.util.tracker.AbstractTracked.tracked
		// and the cached ref/service are cleared

		ServiceReference<WebContainer> currentReference = currentWebContainerRef.get();
		if (currentReference == null) {
			// no need to call anyone
			return;
		}

		// just pick up next best (or null) reference from the available ones
		ServiceReference<WebContainer> bestReference = webContainerTracker.getServiceReference();
		this.webContainerChanged(currentReference, bestReference);
	}

}
