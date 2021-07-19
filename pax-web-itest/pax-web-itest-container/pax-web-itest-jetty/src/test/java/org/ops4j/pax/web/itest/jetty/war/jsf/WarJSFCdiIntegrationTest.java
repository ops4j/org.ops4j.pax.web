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
package org.ops4j.pax.web.itest.jetty.war.jsf;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.war.jsf.AbstractWarJsfCdiIntegrationTest;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarJSFCdiIntegrationTest extends AbstractWarJsfCdiIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		// myfaces-core-api doesn't import javax.inject and I'm getting
		// WELD-001111: interface javax.faces.annotation.FlowMap defined on org.apache.myfaces.cdi.JsfArtifactFlowMapProducer@2143ca58 is not a qualifier
		// if javax.inject is available only as bundle (== is excluded from maven-failsafe-plugin test classpath)
		Option[] tweakOptions1 = combine(serverOptions, bootDelegationPackage("javax.inject"));
		Option[] tweakOptions2 = combine(tweakOptions1, systemPackage("javax.inject;version=\"1.0\""));
		Option[] osgiOptions = combine(tweakOptions2, configAdmin());
		Option[] whiteboardOptions = combine(osgiOptions, paxWebExtenderWhiteboard());
		Option[] jspOptions = combine(whiteboardOptions, paxWebJsp());
		Option[] cdiOptions = combine(jspOptions, ariesCdiAndMyfaces());
		return combine(cdiOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-jsf23-cdi", () -> {
			installAndStartBundle(sampleWarURI("war-jsf23-cdi"));
		});
	}

	@Test
	@Ignore
	public void testCdi() throws Exception {
		// This test works fine when:
		// - ARIES-2053 is fixed
		// - I manually (in debugger) order the WAB deployment and calls to:
		//    - org.apache.aries.cdi.extension.servlet.weld.WeldServletExtension.afterDeploymentValidation()
		//    - org.apache.aries.cdi.extension.el.jsp.ELJSPExtension.afterDeploymentValidation()
		//   so the important aries-cdi/weld listeners are registered after the WAB is deployed (and its
		//   OsgiContextModel)
		// - TODO: the listeners added after the WAB context (real Jetty/Tomcat/Undertow) has started are called
		//       (ServletContextListener.contextInitialized()) in separate thread to not block Aries CCR thread and
		//       pax-web-config thread.
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'hello from working JSF 2.3/CDI 2.0 example, org.ops4j.pax.url.commons.handler.HandlerActivator$Handler'",
						resp -> resp.contains("hello from working JSF 2.3/CDI 2.0 example, org.ops4j.pax.url.commons.handler.HandlerActivator$Handler"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-cdi/");
	}

}
