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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.VersionUtils;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.junit.Assert.assertTrue;

public abstract class AbstractWabWhiteboardConflictIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void whiteboardAndWab() throws Exception {

		// first a filter targetting normal context and soon-installed context from a WAB
		final Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "f1");
		// no "osgi.http.whiteboard.context.select" property, which means "(osgi.http.whiteboard.context.name=default)"
		// remember - "default" is also a name for WAB's context
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, String.format("(|(%s=default)(%s=/war-bundle))",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH));

		final ServiceRegistration<?>[] filter = new ServiceRegistration<?>[] { null };
		configureAndWaitForFilterWithMapping("/*", () -> {
			filter[0] = context.registerService(Filter.class, new Filter() {
				@Override
				public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
					response.getWriter().print("Hello F1|");
					if (!"false".equals(request.getParameter("chain"))) {
						chain.doFilter(request, response);
					}
				}
			}, props);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must ends with 'Hello F1|'",
						resp -> resp.endsWith("Hello F1|"))
				.doGETandExecuteTest("http://127.0.0.1:8181/any?chain=false");

		// this works, because /war-bundle/any is matching /* in "/" context
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must ends with 'Hello F1|'",
						resp -> resp.endsWith("Hello F1|"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/any?chain=false");

		// now the WAB - filter should immediately be registered to new context
		String wabUri = String.format("mvn:org.ops4j.pax.web.samples/war-simplest-osgi/%s/war",
				VersionUtils.getProjectVersion());
		Bundle wab = context.installBundle(wabUri);

		final CountDownLatch latch = new CountDownLatch(2);

		WebApplicationEventListener listener = event -> {
			if (!event.getContextPath().equals("/war-bundle")) {
				return;
			}
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				latch.countDown();
			}
		};
		WebElementEventListener elListener = event -> {
			if (event.getType() == WebElementEvent.State.DEPLOYED) {
				if (event.getData() instanceof FilterEventData) {
					if (event.getData().getContextNames().contains("/war-bundle")) {
						latch.countDown();
					}
				}
			}
		};

		ServiceRegistration<WebApplicationEventListener> reg1
				= context.registerService(WebApplicationEventListener.class, listener, null);
		ServiceRegistration<WebElementEventListener> reg2
				= context.registerService(WebElementEventListener.class, elListener, null);

		try {
			wab.start();
			assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			if (reg1 != null) {
				reg1.unregister();
			}
			if (reg2 != null) {
				reg2.unregister();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello F1'",
						resp -> resp.contains("Hello F1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/any?chain=false");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello F1|Hello (WAB)'",
						resp -> resp.contains("Hello F1|Hello (WAB)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");

		// let's explicitly NOT unregister the filter and stop the WAB checking (implicitly) if there are
		// any deadlocks when everything is cleaned up
//		filter[0].unregister();
//		wab.uninstall();
	}

}
