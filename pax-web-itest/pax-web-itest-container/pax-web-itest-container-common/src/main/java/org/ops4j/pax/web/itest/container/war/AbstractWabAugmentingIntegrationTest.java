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
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.VersionUtils;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.ops4j.pax.web.service.http.HttpContext;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.junit.Assert.assertTrue;

public abstract class AbstractWabAugmentingIntegrationTest extends AbstractContainerTestBase {

	@Test
	public void addWhiteboardAndHttpServiceComponents() throws Exception {
		String wabUri = String.format("mvn:org.ops4j.pax.web.samples/war-simplest-osgi/%s/war",
				VersionUtils.getProjectVersion());
		Bundle wab = context.installBundle(wabUri);

		final CountDownLatch latch = new CountDownLatch(1);

		WebApplicationEventListener listener = event -> {
			if (!event.getContextPath().equals("/war-bundle")) {
				return;
			}
			if (event.getType() == WebApplicationEvent.State.DEPLOYED) {
				latch.countDown();
			}
		};
		ServiceRegistration<WebApplicationEventListener> reg
				= context.registerService(WebApplicationEventListener.class, listener, null);

		try {
			wab.start();
			assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello (WAB)'",
						resp -> resp.contains("Hello (WAB)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");

		// the WAB doesn't contain /servlet2 mapping
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet2");

		// now, we can use ANY bundleContext to register a Filter targetting the WAB's context using Whiteboard

		final Dictionary<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, "/*");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "f1");
		// that's the preferred way to target particular context - by context path and not by name
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				String.format("(%s=/war-bundle)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH));

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

		// we can also use WAB's bundleContext (and only this one!) to register a Servlet using HttpService
		// "default" context would be created (so we can't pass null as HttpContext), but we can use
		// org.ops4j.pax.web.service.WebContainer.createDefaultHttpContext(name) where name is the Web-ContextPath
		// of the WAB

		WebContainer wc = getWebContainer(wab.getBundleContext());
		HttpContext ctx = wc.createDefaultHttpContext("/war-bundle");
		// we're using httpContext == null, to attach to the "default" context for the bundle, which should be
		// a context created for the WAB - so already configured with WAB's Web-ContextPath
		wc.registerServlet(new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
				resp.getWriter().println("Hello (HttpService)");
				resp.getWriter().flush();
			}
		}, "s2", new String[] { "/servlet2" }, null, ctx);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello F1|Hello (WAB)'",
						resp -> resp.contains("Hello F1|Hello (WAB)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");

		// the WAB now should contain /servlet2 mapping
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello F1|Hello (HttpService)'",
						resp -> resp.contains("Hello F1|Hello (HttpService)"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet2");

		// now the magic - let's stop the WAB.
		//  - Whiteboard filter should still be registered, but with context selector pointing to unavailable context
		//  - HttpService servlet should be removed, as it was added through the bundle context of the WAB

		final CountDownLatch latch2 = new CountDownLatch(1);
		listener = event -> {
			if (!event.getContextPath().equals("/war-bundle")) {
				return;
			}
			if (event.getType() == WebApplicationEvent.State.UNDEPLOYED) {
				latch2.countDown();
			}
		};
		reg = context.registerService(WebApplicationEventListener.class, listener, null);

		try {
			wab.stop();
			assertTrue(latch2.await(5000, TimeUnit.MILLISECONDS));
		} finally {
			if (reg != null) {
				reg.unregister();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/any?chain=false");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/servlet2");

		// however, we can install a context which will satisfy the filter!

		final Dictionary<String, Object> props2 = new Hashtable<>();
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "default");
		props2.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/war-bundle");

		final ServiceRegistration<?>[] sc = new ServiceRegistration<?>[] { null };
		configureAndWaitForFilterWithMapping("/*", () -> {
			sc[0] = context.registerService(ServletContextHelper.class, new ServletContextHelper() { }, props2);
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello F1'",
						resp -> resp.contains("Hello F1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-bundle/any?chain=false");

		sc[0].unregister();
		filter[0].unregister();

		wab.uninstall();
	}

}
