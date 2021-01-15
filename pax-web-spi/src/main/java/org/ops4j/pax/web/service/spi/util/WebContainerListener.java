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

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * <p>A listener interface to be used with {@link WebContainerManager} to notify interested entities about lifecycle
 * events of {@link WebContainer}. This is desinged to be higher-level interface than pure
 * {@link org.osgi.framework.ServiceListener}.</p>
 *
 * <p>While it can't be guaranteed by the interface alone, {@link WebContainerManager} which is the invoker of
 * such listeners guarantees that, the notifications will be passed in single thread. Even if the {@link WebContainer}
 * service is immediately registered and unregistered a moment later,
 * {@link #webContainerChanged(ServiceReference, ServiceReference)} calls from single {@link WebContainerManager}
 * will never run in parallel.</p>
 */
public interface WebContainerListener {

	/**
	 * <p>Notification about changed reference to a {@link WebContainer}. When the reference is not changed in the
	 * underlying tracker (like changing a property of registered {@link WebContainer} that doesn't impact the
	 * service ranking), this notification is simply not sent.</p>
	 *
	 * <p>So in other words, a listener is always recommended to clean up the state from old reference (if not null)
	 * and pass the state to new reference (if not null).</p>
	 *
	 * <p>The listener is guaranteed to be invoked sequentially - even if {@link WebContainer} is immediately
	 * unregistered, next {@link #webContainerChanged} will be called after previous call finishes. The gurantee
	 * is given by {@link WebContainerManager}.</p>
	 *
	 * @param oldReference
	 * @param newReference
	 */
	void webContainerChanged(ServiceReference<WebContainer> oldReference, ServiceReference<WebContainer> newReference);

	/**
	 * Notification about bundle being stopped. Again, {@link WebContainerManager} will never call this method in
	 * parallel - also in parallel with {@link #webContainerChanged(ServiceReference, ServiceReference)}.
	 *
	 * @param bundle
	 */
	void bundleStopped(Bundle bundle);

}
