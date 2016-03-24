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
 package org.ops4j.pax.web.itest.jetty;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FilterIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
	}

	@After
	public void tearDown() throws BundleException {
	}


	@Test
	public void testSimpleFilter() throws Exception {
		super.testSimpleFilter();
	}
	
	@Test
	@Ignore
	public void testFilterWar() throws Exception{
		String bundlePath = WEB_BUNDLE 
				+ "mvn:org.ops4j.pax.web.samples/simple-filter/" 
				+ VersionUtil.getProjectVersion() 
				+ "/war?" 
				+ WEB_CONTEXT_PATH
				+ "=/web-filter";
		Bundle installWarBundle = installAndStartBundle(bundlePath);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain test from previous FilterChain",
						resp -> resp.contains("Filtered"))
				.doGETandExecuteTest("http://127.0.0.1:8181/web-filter/me.filter");

//		testClient.testWebPath("http://127.0.0.1:8181/web-filter/me.filter",
//				"Filtered");
		
		installWarBundle.uninstall();
        
	}
}
