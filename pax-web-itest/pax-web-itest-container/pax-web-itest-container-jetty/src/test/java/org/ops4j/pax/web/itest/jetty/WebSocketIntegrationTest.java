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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return combine(
				configureWebSocketJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("websocket-jsr356")
						.type("war")
						.version(VersionUtil.getProjectVersion()),
				mavenBundle().groupId("javax.json")
						.artifactId("javax.json-api").versionAsInProject());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		waitForWebListener();
	}


	@Test
	public void testWebsocket() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Chatroom'",
						resp -> resp.contains("Chatroom"))
				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/index.html");

		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8181/websocket/resource/js/jquery-1.10.2.min.js");


//		testClient.testWebPath("http://127.0.0.1:8181/websocket/index.html",
//				"Chatroom");
//		testClient
//				.testWebPath(
//						"http://127.0.0.1:8181/websocket/resource/js/jquery-1.10.2.min.js",
//						200);
	}

}

