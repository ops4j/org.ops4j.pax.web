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
import org.ops4j.pax.web.itest.base.support.*;
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
public class SharedFilterIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureUndertow(),
				streamBundle(bundle()
		                .add(Bundle1Servlet.class)
		                .add(Bundle1Filter.class)
		                .add(Bundle1SharedFilter.class)
//		                .add(SharedContext.class)
		                .add(Bundle1Activator.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "BundleTest1")
		                .set(Constants.BUNDLE_ACTIVATOR, Bundle1Activator.class.getName())
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()),
		         streamBundle(bundle()
		        		 .add(Bundle2SharedServlet.class)
		        		 .add(Bundle2SharedFilter.class)
		        		 .add(Bundle2Activator.class)
		        		 .set(Constants.BUNDLE_SYMBOLICNAME, "BundleTest2")
		        		 .set(Constants.BUNDLE_ACTIVATOR, Bundle2Activator.class.getName())
		        		 .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		        		 .build()));
	}

	@Before
	public void setUp() throws 	Exception {
		waitForServer("http://127.0.0.1:8181/");
	}

	@After
	public void tearDown() throws BundleException {
	}


	@Test
	public void testBundle1() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to Bundle1'",
						resp -> resp.contains("Welcome to Bundle1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/bundle1/");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/bundle2/");

//		testClient.testWebPath("http://127.0.0.1:8181/bundle1/", "Welcome to Bundle1");
//		testClient.testWebPath("http://127.0.0.1:8181/bundle2/", null, 404, false);
	}
}
