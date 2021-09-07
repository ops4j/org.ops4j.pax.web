/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.container.war;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.VersionUtils;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertTrue;

public abstract class AbstractWabConflictIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void testConflict() throws Exception {
		String wabUri = String.format("mvn:org.ops4j.pax.web.samples/war-simplest-osgi/%s/war",
				VersionUtils.getProjectVersion());
		Bundle wab = context.installBundle(wabUri);
		String warUri = String.format("webbundle:mvn:org.ops4j.pax.web.samples/war-simplest-javaee/%s/war?%s=%s&%s=%s",
				VersionUtils.getProjectVersion(),
				PaxWebConstants.CONTEXT_PATH_HEADER, "/war-bundle", // the same as for war-simplest-osgi
				Constants.BUNDLE_SYMBOLICNAME, "org.ops4j.pax.web.samples.war-simplest-javaee");
		Bundle war = context.installBundle(warUri);

		final CountDownLatch latch1 = new CountDownLatch(2);

		final String[] expectedOutputs = new String[] { null, null };
		final String[] nextToSucceed = new String[] { null };
		final Bundle[] toStop = new Bundle[] { null };

		WebApplicationEventListener listener = event -> {
			if (!event.getContextPath().equals("/war-bundle")) {
				return;
			}
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				// one of them succeeded
				if (event.getBundle().getSymbolicName().equals("org.ops4j.pax.web.samples.war-simplest-osgi")) {
					expectedOutputs[0] = "Hello (WAB)";
					expectedOutputs[1] = "Hello (WAR)";
					nextToSucceed[0] = "org.ops4j.pax.web.samples.war-simplest-javaee";
					toStop[0] = wab;
				}
				if (event.getBundle().getSymbolicName().equals("org.ops4j.pax.web.samples.war-simplest-javaee")) {
					expectedOutputs[0] = "Hello (WAR)";
					expectedOutputs[1] = "Hello (WAB)";
					nextToSucceed[0] = "org.ops4j.pax.web.samples.war-simplest-osgi";
					toStop[0] = war;
				}
				latch1.countDown();
			}
			if (event.getType() == WebApplicationEvent.State.FAILED) {
				// one of them failed
				latch1.countDown();
			}
		};
		ServiceRegistration<WebApplicationEventListener> reg
				= context.registerService(WebApplicationEventListener.class, listener, null);

		try {
			// we CAN'T be sure which WAB will win, because pax-web-extender-war has 3 threads (by default).
			wab.start();
			war.start();
			assertTrue(latch1.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedOutputs[0] + "'",
						resp -> resp.contains(expectedOutputs[0]))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");

		final CountDownLatch latch2 = new CountDownLatch(1);
		listener = event -> {
			if (!event.getContextPath().equals("/war-bundle")) {
				return;
			}
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				// one of them succeeded
				if (event.getBundle().getSymbolicName().equals(nextToSucceed[0])) {
					latch2.countDown();
				}
			}
		};
		reg = context.registerService(WebApplicationEventListener.class, listener, null);

		try {
			toStop[0].stop();
			assertTrue(latch2.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}

		// now, after stopping the bundle that won, we should immediately switch to new bundle
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + expectedOutputs[1] + "'",
						resp -> resp.contains(expectedOutputs[1]))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");
	}

}
