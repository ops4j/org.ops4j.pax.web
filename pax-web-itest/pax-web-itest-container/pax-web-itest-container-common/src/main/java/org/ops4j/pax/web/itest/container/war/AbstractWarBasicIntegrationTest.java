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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.container.whiteboard.AbstractWhiteboardIntegrationTest;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public abstract class AbstractWarBasicIntegrationTest extends AbstractContainerTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractWhiteboardIntegrationTest.class);

	private Bundle bundle;

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		Option[] jspOptions = combine(serverOptions, paxWebJsp());
		return combine(jspOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeployment(() -> {
			context.installBundle(String.format("mvn:org.ops4j.pax.web/pax-web-compatibility-el2/%s", System.getProperty("pax-web.version")));
			context.installBundle(String.format("mvn:org.ops4j.pax.web/pax-web-compatibility-interceptor12/%s", System.getProperty("pax-web.version")));
			context.installBundle(String.format("mvn:org.ops4j.pax.web/pax-web-compatibility-cdi12/%s", System.getProperty("pax-web.version")));
			context.installBundle(String.format("mvn:org.apache.myfaces.core/myfaces-api/%s", System.getProperty("myfaces.version")));
			context.installBundle(String.format("mvn:org.apache.myfaces.core/myfaces-impl/%s", System.getProperty("myfaces.version")));
			context.installBundle(String.format("mvn:commons-beanutils/commons-beanutils/%s", System.getProperty("commons-beanutils.version")));
			context.installBundle(String.format("mvn:commons-collections/commons-collections/%s", System.getProperty("commons-collections.version")));
			context.installBundle(String.format("mvn:commons-digester/commons-digester/%s", System.getProperty("commons-digester.version")));
			context.installBundle(String.format("mvn:jakarta.enterprise/jakarta.enterprise.cdi-api/%s", System.getProperty("jakarta-enterprise-cdi-api.version")));
			context.installBundle(String.format("mvn:jakarta.interceptor/jakarta.interceptor-api/%s", System.getProperty("jakarta-interceptor-api.version")));
			context.installBundle(String.format("mvn:jakarta.el/jakarta.el-api/%s", System.getProperty("jakarta-el-api.version")));
			context.installBundle(String.format("mvn:jakarta.websocket/jakarta.websocket-api/%s", System.getProperty("jakarta-websocket-api.version")));
			context.installBundle(String.format("mvn:jakarta.xml.bind/jakarta.xml.bind-api/%s", System.getProperty("jakarta-xml-bind-api.version")));
			context.installBundle(String.format("mvn:com.sun.activation/javax.activation/%s", System.getProperty("activation.version")));

			// I'm not refreshing, so fragments need to be installed before their hosts
			installAndStartBundle(sampleURI("container-bundle-3"));
			context.installBundle(sampleURI("container-fragment-1"));
			installAndStartBundle(sampleURI("container-bundle-1"));
			context.installBundle(sampleURI("container-fragment-2"));
			installAndStartBundle(sampleURI("container-bundle-2"));
			context.installBundle(sampleURI("the-wab-fragment"));
			bundle = installAndStartBundle(sampleWarURI("the-wab-itself"));
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void complexWab() throws Exception {
		// there should be a /wab-complex context that's (by default) redirecting to /wab-complex/
		HttpTestClientFactory.createDefaultTestClient(false)
				.withReturnCode(302)
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex");

		// servlet from web.xml
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must end with 'Hello'",
						resp -> resp.endsWith("Hello"))
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex/servlet");
		// servlet from a an SCI of container-bundle-3 - should not be reachable, because we scan only
		// direct "wires" of a WAB
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex/dynamic1");
		// annotated servlet from WAB fragment
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must end with 'Hello /xyz!'",
						resp -> resp.endsWith("Hello /xyz!"))
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex/as1/xyz");
		// resource from the WAB
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is just a static resource in the root directory of the WAB.'",
						resp -> resp.contains("This is just a static resource in the root directory of the WAB."))
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex/hello.txt");
		// resource from the WAB's fragment
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'This is just a static resource in the root directory of the WAB's fragment.'",
						resp -> resp.contains("This is just a static resource in the root directory of the WAB's fragment."))
				.doGETandExecuteTest("http://127.0.0.1:8181/wab-complex/hello-fragment.txt");
	}

}
