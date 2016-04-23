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
 package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.FilterBundleActivator;
import org.ops4j.pax.web.itest.base.support.ServletBundleActivator;
import org.ops4j.pax.web.itest.base.support.SimpleOnlyFilter;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class SharedContextFilterIntegrationTest extends ITestBase {

	private static final String SERVLET_BUNDLE = "ServletBundleTest";
	private static final String FILTER_BUNDLE = "FilterBundleTest";

	@Configuration
	public static Option[] configure() {
		return combine(
				configureUndertow(),
				streamBundle(bundle()
						.add(TestServlet.class)
						.add(ServletBundleActivator.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, SERVLET_BUNDLE)
						.set(Constants.BUNDLE_ACTIVATOR,
								ServletBundleActivator.class.getName())
						.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()),
				streamBundle(bundle()
						.add(SimpleOnlyFilter.class)
						.add(FilterBundleActivator.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, FILTER_BUNDLE)
						.set(Constants.BUNDLE_ACTIVATOR,
								FilterBundleActivator.class.getName())
						.set(Constants.DYNAMICIMPORT_PACKAGE, "*").build()));
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8181/");
	}

	@After
	public void tearDown() throws BundleException {
	}


	@Test
	public void testBundle1() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");

//		testClient.testWebPath("http://127.0.0.1:8181/sharedContext/", "Hello Whiteboard Filter");
	}
	
	@Test
	public void testStop() throws Exception {
		for (final Bundle b : bundleContext.getBundles()) {
			if (FILTER_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'SimpleServlet: TEST OK'",
						resp -> resp.contains("SimpleServlet: TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");

//		testClient.testWebPath("http://127.0.0.1:8181/sharedContext/", "SimpleServlet: TEST OK");
	}
}
