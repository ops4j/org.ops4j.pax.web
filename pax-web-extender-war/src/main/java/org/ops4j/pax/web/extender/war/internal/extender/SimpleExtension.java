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

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleExtension implements Extension {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final Bundle bundle;
	private final BundleContext bundleContext;
	private final AtomicBoolean destroyed = new AtomicBoolean(false);

	public SimpleExtension(Bundle bundle) {
		this.bundle = bundle;
		this.bundleContext = bundle.getBundleContext();
	}

	public boolean isDestroyed() {
		synchronized (getLock()) {
			return destroyed.get();
		}
	}

	// CHECKSTYLE:OFF
	public void start() {
		synchronized (getLock()) {
			if (destroyed.get()) {
				return;
			}
			if (bundle.getState() != Bundle.ACTIVE) {
				return;
			}
			if (bundle.getBundleContext() != bundleContext) {
				return;
			}
			try {
				doStart();
			} catch (Exception e) {
				logger.warn("Error starting extension for bundle " + bundle, e);
			}
		}
	}

	public void destroy() {
		synchronized (getLock()) {
			destroyed.set(true);
		}
		try {
			doDestroy();
		} catch (Exception e) {
			logger.warn("Error stopping extension for bundle " + bundle, e);
		}
	}
	// CHECKSTYLE:ON

	protected Object getLock() {
		return this;
	}

	protected abstract void doStart() throws Exception;

	protected abstract void doDestroy() throws Exception;

}
