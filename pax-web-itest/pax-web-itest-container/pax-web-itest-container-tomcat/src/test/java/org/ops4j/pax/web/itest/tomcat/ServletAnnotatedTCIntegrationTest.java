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
 package org.ops4j.pax.web.itest.tomcat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.ops4j.pax.web.service.WebContainerConstants;
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
@ExamReactorStrategy(PerMethod.class)
public class ServletAnnotatedTCIntegrationTest extends ITestBase {

	@Configuration
	public Option[] configure() {
		return combine(configureTomcat(), 
				streamBundle(bundle()
		                .add(AnnotatedTestServlet.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
		                .set(WebContainerConstants.CONTEXT_PATH_KEY, "/annotatedTest")
		                .set(Constants.IMPORT_PACKAGE, "javax.servlet")
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()));
	}

	@Before
	public void setUp() throws 	Exception {
		initServletListener("test");

		waitForServer("http://127.0.0.1:8282/");
		waitForServletListener();
	}

	@After
	public void tearDown() throws BundleException {
	}


	@Test
	public void testBundle1() throws Exception {
		
		waitForServer("http://127.0.0.1:8282/");
		
		Thread.sleep(200);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'TEST OK'",
						resp -> resp.contains("TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8282/annotatedTest/test");

//		testClient.testWebPath("http://127.0.0.1:8282/annotatedTest/test", "TEST OK");
	}
}
