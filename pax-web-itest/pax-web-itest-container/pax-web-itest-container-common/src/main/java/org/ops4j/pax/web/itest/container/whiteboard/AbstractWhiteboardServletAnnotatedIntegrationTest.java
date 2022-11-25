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
package org.ops4j.pax.web.itest.container.whiteboard;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.AnnotatedTestFilter;
import org.ops4j.pax.web.itest.utils.web.AnnotatedTestServlet;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.osgi.framework.ServiceRegistration;

import static org.junit.Assert.assertTrue;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractWhiteboardServletAnnotatedIntegrationTest extends AbstractContainerTestBase {

	@Before
	public void waitBeforeTest() throws Exception {
		configureAndWaitForListener(8181);
	}

	@Test
	public void testWhiteboardServletRegistration() throws Exception {
		AnnotatedTestServlet annotatedTestServlet = new AnnotatedTestServlet();
		@SuppressWarnings("unchecked")
		final ServiceRegistration<Servlet>[] servletRegistration = new ServiceRegistration[1];
		configureAndWaitForServletWithMapping("/test", () -> {
			servletRegistration[0] = context.registerService(Servlet.class, annotatedTestServlet, null);
		});

		try {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'TEST OK'",
							resp -> resp.contains("TEST OK"))
					.doGETandExecuteTest("http://127.0.0.1:8181/test");
		} finally {
			configureAndWait(() -> {
				servletRegistration[0].unregister();
			}, events -> events.stream().filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED).count() == 1);
		}

		assertTrue(annotatedTestServlet.isInitCalled());
		assertTrue(annotatedTestServlet.isDestroyCalled());
	}

	@Test
	public void testWhiteboardServletRegistrationDestroyCalled() throws Exception {
		final AnnotatedTestServlet annotatedTestServlet = new AnnotatedTestServlet();
		@SuppressWarnings("unchecked")
		final ServiceRegistration<Servlet>[] servletRegistration = new ServiceRegistration[1];
		configureAndWaitForServletWithMapping("/test", () -> {
			servletRegistration[0] = context.registerService(Servlet.class, annotatedTestServlet, null);
		});

		try {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'TEST OK'",
							resp -> resp.contains("TEST OK"))
					.doGETandExecuteTest("http://127.0.0.1:8181/test");
		} finally {
			configureAndWait(() -> {
				servletRegistration[0].unregister();
			}, events -> events.stream().filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED).count() == 1);
		}

		assertTrue(annotatedTestServlet.isInitCalled());
		assertTrue(annotatedTestServlet.isDestroyCalled());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWhiteboardFilterRegistration() throws Exception {
		AnnotatedTestServlet annotatedTestServlet = new AnnotatedTestServlet();
		final ServiceRegistration<Servlet>[] servletRegistration = new ServiceRegistration[1];
		final ServiceRegistration<Filter>[] filterRegistration = new ServiceRegistration[1];
		configureAndWaitForFilterWithMapping("/*", () -> {
			filterRegistration[0] = context.registerService(Filter.class, new AnnotatedTestFilter(), null);
		});
		configureAndWaitForServletWithMapping("/test", () -> {
			servletRegistration[0] = context.registerService(Servlet.class, annotatedTestServlet, null);
		});

		try {
			HttpTestClientFactory.createDefaultTestClient()
					.withResponseAssertion("Response must contain 'TEST OK'",
							resp -> resp.contains("TEST OK"))
					.withResponseAssertion("Response must contain 'FILTER-INIT: true'",
							resp -> resp.contains("FILTER-INIT: true"))
					.doGETandExecuteTest("http://127.0.0.1:8181/test");
		} finally {
			configureAndWait(() -> {
				servletRegistration[0].unregister();
			}, events -> events.stream().filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED).count() == 1);
			configureAndWait(() -> {
				filterRegistration[0].unregister();
			}, events -> events.stream().filter(e -> e.getType() == WebElementEvent.State.UNDEPLOYED).count() == 1);
		}

		assertTrue(annotatedTestServlet.isInitCalled());
		assertTrue(annotatedTestServlet.isDestroyCalled());
	}

}
