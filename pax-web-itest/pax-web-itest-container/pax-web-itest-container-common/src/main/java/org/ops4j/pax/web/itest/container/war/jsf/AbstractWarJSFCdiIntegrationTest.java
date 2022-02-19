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
package org.ops4j.pax.web.itest.container.war.jsf;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;

public class AbstractWarJSFCdiIntegrationTest extends AbstractContainerTestBase {

	@Before
	public void setUp() {
		// simple waiting for deployment is not enough, because war-jsf23-cdi bundle is subject to CDI extension
		// as well. And because org.apache.aries.cdi.extension.servlet.weld whiteboard-registers
		// additional listener (org.jboss.weld.module.web.servlet.WeldInitialListener), the /war-jsf23-cdi
		// will be restarted
		configureAndWait(() -> {
			installAndStartBundle(sampleWarURI("war-jsf23-cdi"));
		}, events -> {
			// when the WAB is starting, the context will be deployed together with all the listeners and SCIs
			// but at the same time Aries CDI will be extending our WAB as well, registering a context listener
			// we can assume tha
			LOG.info("~~~ events: {}", events.size());
			for (WebElementEvent ev : events) {
				if (ev.getType() == WebElementEvent.State.DEPLOYED && ev.getData() instanceof EventListenerEventData) {
					EventListenerEventData eled = (EventListenerEventData) ev.getData();
					LOG.info("~~~ evd: {}/{} ({})", eled.getListener(), eled.getElementReference(), eled.getOriginBundle());
				}
			}
			if (events.stream().filter(e -> e.getType() == WebElementEvent.State.DEPLOYED).count() >= 4) {
				// give it one more second... After all, we've only detected listener registrations
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.error(e.getMessage(), e);
					Thread.currentThread().interrupt();
					throw new RuntimeException(e.getMessage(), e);
				}
				return true;
			}
			return false;
		});
	}

	@Test
	public void testCdi() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'hello from working JSF 2.3/CDI 2.0 example, org.ops4j.pax.url.commons.handler.HandlerActivator$Handler'",
						resp -> resp.contains("hello from working JSF 2.3/CDI 2.0 example, org.ops4j.pax.url.commons.handler.HandlerActivator$Handler"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-cdi/");
	}

}
