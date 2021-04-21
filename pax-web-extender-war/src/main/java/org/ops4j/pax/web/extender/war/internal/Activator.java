/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.utils.extender.AbstractExtender;
import org.apache.felix.utils.extender.Extension;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.util.NamedThreadFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link org.osgi.framework.BundleActivator} for pax-web-extender-war, which sets all the things up.
 */
public class Activator extends AbstractExtender {

	public static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private static final int DEFAULT_POOL_SIZE = 3;

	/**
	 * WAR Extender context - this is where the Bundles are managed as <em>extensions</em> and interaction with
	 * {@link org.osgi.service.http.HttpService} / {@link org.ops4j.pax.web.service.WebContainer} happens.
	 */
	private WarExtenderContext warExtenderContext;

	private int poolSize = DEFAULT_POOL_SIZE;

	private final Map<Bundle, Extension> extensions = new ConcurrentHashMap<>();

	@Override
	protected ExecutorService createExecutor() {
		return Executors.newScheduledThreadPool(poolSize, new NamedThreadFactory("wab-extender"));
	}

	@Override
	public void start(BundleContext context) throws Exception {
		String poolSizeValue = context.getProperty(PaxWebConstants.BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_THREADS);
		if (poolSizeValue != null && !"".equals(poolSizeValue)) {
			try {
				poolSize = Integer.parseInt(poolSizeValue);
			} catch (NumberFormatException ignored) {
			}
		}

		// let's be explicit - even if asynchronous mode is default. Please do not change this.
		setSynchronous(false);

		super.start(context);
	}

	@Override
	protected void doStart() throws Exception {
		LOG.debug("Starting Pax Web WAR Extender");

		BundleContext context = getBundleContext();

		// WarExtenderContext (just like WhiteboardExtenderContext) manages the lifecycle of both the bundles
		// being extended and the WebContainer OSGi service where the WARs are to be installed
		warExtenderContext = new WarExtenderContext(getBundleContext(), getExecutors());

		// start tracking the extensions (Bundle -> WebApp)
		startTracking();

		LOG.debug("Pax Web WAR Extender started");
	}

	@Override
	protected void doStop() throws Exception {
		LOG.debug("Stopping Pax Web WAR Extender");

		// stop tracking the extensions
		stopTracking();

		warExtenderContext.shutdown();

		LOG.debug("Pax Web WAR Extender stopped");
	}

	@Override
	protected Extension doCreateExtension(final Bundle bundle) throws Exception {
		Extension extension = warExtenderContext.createExtension(bundle, () -> extensions.remove(bundle));
		if (extension != null) {
			extensions.put(bundle, extension);
		}
		return extension;
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		// prevent confusing "Starting destruction process" for bundles that were never tracked
		Bundle bundle = event.getBundle();
		if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
			if (extensions.containsKey(bundle)) {
				super.bundleChanged(event);
			}
		}
	}

	@Override
	public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
		// Nothing to do
		if (extensions.containsKey(bundle)) {
			super.removedBundle(bundle, event, object);
		}
	}

	@Override
	protected void debug(Bundle bundle, String msg) {
		if (LOG.isDebugEnabled()) {
			if (bundle == null) {
				LOG.debug("(no bundle): " + msg);
			} else {
				LOG.debug(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg);
			}
		}
	}

	@Override
	protected void warn(Bundle bundle, String msg, Throwable t) {
		if (bundle == null) {
			if (t != null) {
				LOG.warn("(no bundle): " + msg, t);
			} else {
				LOG.warn("(no bundle): " + msg);
			}
		} else {
			if (t != null) {
				LOG.warn(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg, t);
			} else {
				LOG.warn(bundle.getSymbolicName() + "/" + bundle.getVersion() + ": " + msg);
			}
		}
	}

	@Override
	protected void error(String msg, Throwable t) {
		if (t != null) {
			LOG.error(msg, t);
		} else {
			LOG.error(msg);
		}
	}

}
