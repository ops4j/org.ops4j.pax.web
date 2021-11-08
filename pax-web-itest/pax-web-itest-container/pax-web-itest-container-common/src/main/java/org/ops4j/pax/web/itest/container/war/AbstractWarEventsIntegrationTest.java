/*
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

package org.ops4j.pax.web.itest.container.war;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Achim Nierbeck
 */
public abstract class AbstractWarEventsIntegrationTest extends AbstractContainerTestBase {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractWarEventsIntegrationTest.class);

	@Test
	public void testWC() throws Exception {
		final CountDownLatch latch = new CountDownLatch(3);
		final List<Event> events = new LinkedList<>();
		EventHandler handler = event -> {
			events.add(event);
			latch.countDown();
		};
		Dictionary<String, Object> props = new Hashtable<>();
 		props.put(EventConstants.EVENT_TOPIC, "org/osgi/service/web/*");
		ServiceRegistration<EventHandler> reg = context.registerService(EventHandler.class, handler, props);

		Bundle wab = configureAndWaitForDeploymentUnlessInstalled("war", () -> {
			installAndStartWebBundle("war", "/war");
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war/wc");

		wab.stop();

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		assertThat(events.size(), equalTo(4));
		assertThat(events.get(0).getTopic(), endsWith("DEPLOYING"));
		assertThat(events.get(1).getProperty("context.path"), equalTo("/war"));
		assertThat(events.get(2).getProperty("extender.bundle.symbolicName"), equalTo("org.ops4j.pax.web.pax-web-extender-war"));
		assertThat(events.get(3).getTopic(), endsWith("UNDEPLOYED"));

		reg.unregister();
	}

}
