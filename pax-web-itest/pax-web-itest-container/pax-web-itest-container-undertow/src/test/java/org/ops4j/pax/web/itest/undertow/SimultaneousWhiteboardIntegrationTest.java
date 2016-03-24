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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.TestActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class SimultaneousWhiteboardIntegrationTest extends ITestBase {
	private static final Logger LOG = LoggerFactory.getLogger(SimultaneousWhiteboardIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return combine(
				configureUndertow(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(asInProject()).noStart(),
				streamBundle(
						bundle().add(TestActivator.class)
								.add(WhiteboardFilter.class)
								.set(Constants.BUNDLE_ACTIVATOR, TestActivator.class.getName())
								.set(Constants.BUNDLE_SYMBOLICNAME,
										"org.ops4j.pax.web.itest.SimultaneousTest")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
								.build()).noStart());
	}
	
	@Before
	public void setUp() throws Exception {
		//org.ops4j.pax.web.extender.samples.whiteboard
		
		Bundle whiteBoardBundle = null;
		Bundle simultaneousTestBundle = null;
		
		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle bundle : bundles) {
			String symbolicName = bundle.getSymbolicName();
			if ("org.ops4j.pax.web.extender.samples.whiteboard".equals(symbolicName)) {
				whiteBoardBundle = bundle;
			} else if ("org.ops4j.pax.web.itest.SimultaneousTest".equals(symbolicName)) {
				simultaneousTestBundle = bundle;
			}
		}
		
		assertNotNull(simultaneousTestBundle);
		assertNotNull(whiteBoardBundle);
		
		simultaneousTestBundle.start();
		whiteBoardBundle.start();
		
		//org.ops4j.pax.web.itest.SimultaneousTest
	}


	@Test
	public void testWhiteBoardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");

//		testClient.testWebPath("http://127.0.0.1:8181/root", "Hello Whiteboard Extender");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");

//		testClient.testWebPath("http://127.0.0.1:8181/", "Welcome to the Welcome page");
	}

}
