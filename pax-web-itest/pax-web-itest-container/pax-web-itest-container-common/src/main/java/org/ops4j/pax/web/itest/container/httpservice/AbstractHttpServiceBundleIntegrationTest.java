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
package org.ops4j.pax.web.itest.container.httpservice;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractControlledTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractHttpServiceBundleIntegrationTest extends AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractHttpServiceBundleIntegrationTest.class);

	private Bundle hsBundle;

	@Before
	public void setup() throws Exception {
		configureAndWaitForServletWithMapping("/alt-images/*",
				() -> hsBundle = installAndStartBundle(sampleURI("mvn:org.ops4j.pax.web.samples/hs-helloworld/")));
	}

	@After
	public void cleanup() throws BundleException {
		if (hsBundle != null) {
			hsBundle.stop();
			hsBundle.uninstall();
		}
	}

	@Test
	public void testSubPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");
		// test image-serving from different alias
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /alt-images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt-images/logo.png");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Other resource paths will be served by servlet mapped at /*",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().startsWith("text/html")))
				.doGETandExecuteTest("http://127.0.0.1:8181/alt2-images/logo.png");
	}

	@Test
	public void testRootPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testServletPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Path Info: \"/lall/blubb\"'",
						resp -> resp.contains("Path Info: \"/lall/blubb\""))
				.doGETandExecuteTest("http://127.0.0.1:8181/lall/blubb");

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: \"/helloworld/hs\"'",
						resp -> resp.contains("Servlet Path: \"/helloworld/hs\""))
				.withResponseAssertion("Response must contain 'Path Info: \"/lall/blubb\"'",
						resp -> resp.contains("Path Info: \"/lall/blubb\""))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs/lall/blubb");
	}

	@Test
	public void testServletDeRegistration() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_OK)
				.doGETandExecuteTest("http://127.0.0.1:8181/");

		if (hsBundle != null) {
			hsBundle.stop();
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(HttpServletResponse.SC_NOT_FOUND)
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

	@Test
	public void testRootFilterRegistration() throws Exception {
		// before Pax Web 8 it was possible (though not well defined) to mix "contexts". Now, it is explicitly
		// forbidden (due to Whiteboard Service compliance) to mix filters and target servlet with different
		// "contexts". By "context" we mean "OSGi context", which in HttpService/WebContainer case is the context
		// passed to registerServlet() method. If null, default one is created, but it's "bound" to bundle for
		// which the HttpService/WebContainer instance is scoped

		// so because hsBundle registers two servlets and one is for "/" alias, they actually cover entire URI
		// namespace. The below registered filter can only be included in "request invocation pipeline" if
		// it shares the "context" with the servlet(s), so we have to obtain instance of WebContainer using
		// the same context
		WebContainer service = getWebContainer(hsBundle.getBundleContext());

		final String fullContent = "This content is Filtered by a javax.servlet.Filter";

		Filter filter = new Filter() {
			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				PrintWriter writer = response.getWriter();
				writer.write(fullContent);
				writer.flush();
				// no chain.doFilter()
			}

			@Override
			public void destroy() {
			}
		};
		final StringWriter writer = new StringWriter();

		filter.doFilter(null, (ServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ServletResponse.class }, (proxy, method, args) -> {
			if (method.getName().equals("getWriter")) {
				return new PrintWriter(writer);
			}
			return null;
		}), null);

		// Check if our example filter do write the string to the writer...
		assertEquals(fullContent, writer.toString());

		// Now register the Filter under some alias...
		service.registerFilter(filter, new String[] { "*", "/*", "/", "/some/random/path" }, null, null, null);

		// If it works, always the filter should take over and return the same string regardeless of the URL
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/some/notregistered/random/path");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test-context/");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'content is Filtered by'",
						resp -> resp.contains("content is Filtered by"))
				.doGETandExecuteTest("http://127.0.0.1:8181/images/logo.png");

		// of course we should be able to deregister :-)
		service.unregisterFilter(filter);
	}

}
