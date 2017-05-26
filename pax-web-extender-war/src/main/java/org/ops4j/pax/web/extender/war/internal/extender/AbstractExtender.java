/*
 * Copyright 2013 Guillaume Nodet.
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
package org.ops4j.pax.web.extender.war.internal.extender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to write bundle extenders. This extender tracks started bundles
 * (or starting if they have a lazy activation policy) and will create an
 * {@link Extension} for each of them to manage it.
 * <p>
 * The extender will handle all concurrency and synchronization issues, see
 * {@link Extension} for more information about the additional constraints.
 * <p>
 * The extender guarantee that all extensions will be stopped synchronously with
 * the STOPPING event of a given bundle and that all extensions will be stopped
 * before the extender bundle is stopped.
 */
public abstract class AbstractExtender implements BundleActivator,
		BundleTrackerCustomizer<Bundle>, SynchronousBundleListener {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<Bundle, Extension> extensions = new ConcurrentHashMap<>();
	private final ConcurrentMap<Bundle, FutureTask<Void>> destroying = new ConcurrentHashMap<>();
	private volatile boolean stopping;
	private volatile boolean stopped;

	private boolean synchronous;
	private boolean preemptiveShutdown;
	private BundleContext bundleContext;
	private ExecutorService executors;
	private BundleTracker<Bundle> tracker;

	/**
	 * Check if the extender is synchronous or not. If the flag is set, the
	 * extender will start the extension synchronously with the bundle being
	 * tracked or started. Else, the starting of the extension will be delegated
	 * to a thread pool.
	 *
	 * @return if the extender is synchronous
	 */
	public boolean isSynchronous() {
		return synchronous;
	}

	/**
	 * Check if the extender performs a preemptive shutdown of all extensions
	 * when the framework is being stopped. The default behavior is to wait for
	 * the framework to stop the bundles and stop the extension at that time.
	 *
	 * @return if the extender use a preemptive shutdown
	 */
	public boolean isPreemptiveShutdown() {
		return preemptiveShutdown;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public ExecutorService getExecutors() {
		return executors;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public void setPreemptiveShutdown(boolean preemptiveShutdown) {
		this.preemptiveShutdown = preemptiveShutdown;
	}

	public void start(BundleContext context) throws Exception {
		bundleContext = context;
		bundleContext.addBundleListener(this);
		this.tracker = new BundleTracker<>(bundleContext, Bundle.ACTIVE
				| Bundle.STARTING, this);
		if (!this.synchronous) {
			this.executors = createExecutor();
		}
		doStart();
	}

	public void stop(BundleContext context) throws Exception {
		stopping = true;
		while (!extensions.isEmpty()) {
			Collection<Bundle> toDestroy = chooseBundlesToDestroy(extensions
					.keySet());
			if (toDestroy == null || toDestroy.isEmpty()) {
				toDestroy = new ArrayList<>(extensions.keySet());
			}
			for (Bundle bundle : toDestroy) {
				destroyExtension(bundle);
			}
		}
		doStop();
		if (executors != null) {
			executors.shutdown();
			try {
				executors.awaitTermination(60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// Ignore
			}
			executors = null;
		}
		stopped = true;
	}

	protected void doStart() throws Exception {
		startTracking();
	}

	protected void doStop() throws Exception {
		stopTracking();
	}

	protected void startTracking() {
		this.tracker.open();
	}

	protected void stopTracking() {
		this.tracker.close();
	}

	/**
	 * Create the executor used to start extensions asynchronously.
	 *
	 * @return an
	 */
	protected ExecutorService createExecutor() {
		return Executors.newScheduledThreadPool(3, new NamedThreadFactory("paxweb-extender"));
	}

	/**
	 * @param bundles
	 * @return bundles to destroy
	 */
	protected Collection<Bundle> chooseBundlesToDestroy(Set<Bundle> bundles) {
		return null;
	}

	public void bundleChanged(BundleEvent event) {
		if (stopped) {
			return;
		}
		Bundle bundle = event.getBundle();
		if (bundle.getState() != Bundle.ACTIVE
				&& bundle.getState() != Bundle.STARTING) {
			// The bundle is not in STARTING or ACTIVE state anymore
			// so destroy the bundleContext. Ignore our own bundle since it
			// needs to kick the orderly shutdown.
			if (bundle != this.bundleContext.getBundle()) {
				destroyExtension(bundle);
			}
		}
	}

	public Bundle addingBundle(Bundle bundle, BundleEvent event) {
		modifiedBundle(bundle, event, bundle);
		return bundle;
	}

	//CHECKSTYLE:OFF
	public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		// If the bundle being stopped is the system bundle,
		// do an orderly shutdown of all blueprint contexts now
		// so that service usage can actually be useful
		if (bundle.getBundleId() == 0 && bundle.getState() == Bundle.STOPPING) {
			if (preemptiveShutdown) {
				try {
					stop(bundleContext);
				} catch (Exception e) {
					logger.error("Error while performing preemptive shutdown",
							e);
				}
				return;
			}
		}
		if (bundle.getState() != Bundle.ACTIVE
				&& bundle.getState() != Bundle.STARTING) {
			// The bundle is not in STARTING or ACTIVE state anymore
			// so destroy the bundleContext. Ignore our own bundle since it
			// needs to kick the orderly shutdown and not unregister the
			// namespaces.
			if (bundle != this.bundleContext.getBundle()) {
				destroyExtension(bundle);
			}
			return;
		}
		// Do not track bundles given we are stopping
		if (stopping) {
			return;
		}
		// For starting bundles, ensure, it's a lazy activation,
		// else we'll wait for the bundle to become ACTIVE
		if (bundle.getState() == Bundle.STARTING) {
			String activationPolicyHeader = (String) bundle.getHeaders().get(
					Constants.BUNDLE_ACTIVATIONPOLICY);
			if (activationPolicyHeader == null
					|| !activationPolicyHeader
					.startsWith(Constants.ACTIVATION_LAZY)) {
				// Do not track this bundle yet
				return;
			}
		}
		createExtension(bundle);
	}
	//CHECKSTYLE:ON

	public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
		// Nothing to do
		destroyExtension(bundle);
	}

	//CHECKSTYLE:OFF
	private void createExtension(Bundle bundle) {
		try {
			BundleContext context = bundle.getBundleContext();
			if (context == null) {
				// The bundle has been stopped in the mean time
				return;
			}
			final Extension extension = doCreateExtension(bundle);
			if (extension == null) {
				// This bundle is not to be extended
				return;
			}
			synchronized (extensions) {
				if (extensions.putIfAbsent(bundle, extension) != null) {
					return;
				}
			}
			if (synchronous) {
				logger.debug("Starting extension for bundle {} synchronously",
						bundle.getSymbolicName());
				extension.start();
			} else {
				logger.debug(
						"Scheduling start of extension for bundle {} asynchronously",
						bundle.getSymbolicName());
				getExecutors().submit(() -> extension.start());
			}
		} catch (Throwable t) {
			logger.warn("Error while creating extension for bundle " + bundle,
					t);
		}
	}

	private void destroyExtension(final Bundle bundle) {
		FutureTask<Void> future;
		synchronized (extensions) {
			logger.debug("Starting destruction process for bundle {}",
					bundle.getSymbolicName());
			future = destroying.get(bundle);
			if (future == null) {
				final Extension extension = extensions.remove(bundle);
				if (extension != null) {
					logger.debug("Scheduling extension destruction for {}.",
							bundle.getSymbolicName());
					future = new FutureTask<>(() -> {
						logger.info("Destroying extension for bundle {}",
								bundle.getSymbolicName());
						try {
							extension.destroy();
						} finally {
							logger.debug(
									"Finished destroying extension for bundle {}",
									bundle.getSymbolicName());
							synchronized (extensions) {
								destroying.remove(bundle);
							}
						}
					}, null);
					destroying.put(bundle, future);
				} else {
					logger.debug(
							"Not an extended bundle or destruction of extension already finished for {}.",
							bundle.getSymbolicName());
				}
			} else {
				logger.debug("Destruction already scheduled for {}.",
						bundle.getSymbolicName());
			}
		}
		if (future != null) {
			try {
				logger.debug("Waiting for extension destruction for {}.",
						bundle.getSymbolicName());
				future.run();
				future.get();
			} catch (Throwable t) {
				logger.warn("Error while destroying extension for bundle "
						+ bundle, t);
			}
		}
	}
	//CHECKSTYLE:ON

	/**
	 * Create the extension for the given bundle, or null if the bundle is not
	 * to be extended.
	 *
	 * @param bundle the bundle to extend
	 * @return created {@link Extension}
	 * @throws Exception
	 */
	protected abstract Extension doCreateExtension(Bundle bundle)
			throws Exception;

}
