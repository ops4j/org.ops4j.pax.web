/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal;

import static org.easymock.EasyMock.createMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class ExtenderContextTest extends Assert {

	private Bundle bundle;
	private BundleContext bundleContext;
	private HttpServiceTracker httpServiceTracker;

	@Before
	public void setUp() {
		bundle = createMock(Bundle.class);
		bundleContext = createMock(BundleContext.class);
		httpServiceTracker = new HttpServiceTracker(bundleContext);
	}

	@Test
	public void testHttpServiceTrackerAfterClose()
			throws InvalidSyntaxException {
		ExtenderContext extenderContext = new ExtenderContext();
		extenderContext.getHttpServiceTrackers().putIfAbsent(bundle,
				httpServiceTracker);
		extenderContext.getWebApplication(bundle, "httpContextId");
		assertEquals(1, extenderContext.getHttpServiceTrackers().size());
		extenderContext.closeServiceTracker();
		assertEquals(0, extenderContext.getHttpServiceTrackers().size());
	}

}
