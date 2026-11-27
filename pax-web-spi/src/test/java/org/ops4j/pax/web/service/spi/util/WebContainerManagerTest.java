/*
 * Copyright 2026 OPS4J.
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

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.service.WebContainer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebContainerManagerTest {

	@Test
	public void stoppedBundleAfterShutdownIsIgnored() {
		BundleContext context = mock(BundleContext.class);
		WebContainerListener listener = mock(WebContainerListener.class);
		WebContainerManager manager = new WebContainerManager(context, listener, "pax-web-test");

		manager.initialize();
		manager.shutdown();

		Bundle bundle = mock(Bundle.class);
		when(bundle.getSymbolicName()).thenReturn("test.bundle");
		BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);

		assertDoesNotThrow(() -> manager.bundleChanged(event));
	}

	@Test
	public void stoppedBundleBeforeShutdownIsDeliveredSynchronously() {
		BundleContext context = mock(BundleContext.class);
		WebContainerListener listener = mock(WebContainerListener.class);
		WebContainerManager manager = new WebContainerManager(context, listener);

		manager.initialize();

		Bundle bundle = mock(Bundle.class);
		BundleEvent event = new BundleEvent(BundleEvent.STOPPED, bundle);

		manager.bundleChanged(event);

		verify(listener).bundleStopped(bundle);
	}

	@Test
	public void webContainerChangeBeforeShutdownIsDeliveredSynchronously() {
		BundleContext context = mock(BundleContext.class);
		WebContainerListener listener = mock(WebContainerListener.class);
		WebContainerManager manager = new WebContainerManager(context, listener);
		ServiceReference<WebContainer> reference = mock(ServiceReference.class);

		manager.initialize();

		manager.addingService(reference);

		verify(listener).webContainerChanged(null, reference);
	}

	@Test
	public void webContainerChangeAfterShutdownIsIgnored() {
		BundleContext context = mock(BundleContext.class);
		WebContainerListener listener = mock(WebContainerListener.class);
		WebContainerManager manager = new WebContainerManager(context, listener, "pax-web-test");
		ServiceReference<WebContainer> reference = mock(ServiceReference.class);

		manager.initialize();
		manager.shutdown();

		assertDoesNotThrow(() -> manager.addingService(reference));
	}

}
