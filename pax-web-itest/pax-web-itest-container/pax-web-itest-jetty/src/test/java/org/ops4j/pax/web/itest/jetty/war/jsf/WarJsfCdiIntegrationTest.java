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
import org.osgi.framework.Bundle;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarJsfCdiIntegrationTest extends AbstractWarJsfCdiIntegrationTest {

	private Bundle wab;

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		Option[] osgiOptions = combine(serverOptions, configAdmin());
		Option[] jspOptions = combine(osgiOptions, paxWebJsp());
		Option[] cdiOptions = combine(jspOptions, ariesCdiAndMyfaces(containerSpecificCdiBundle()));
		return combine(cdiOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		wab = configureAndWaitForDeploymentUnlessInstalled("war-jsf23-cdi", () -> {
			installAndStartBundle(sampleWarURI("war-jsf23-cdi"));
		});
	}

	@Test
	public void testCdi() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '" + containerSpecificCdiBundle() + "'",
						resp -> resp.contains(containerSpecificCdiBundle()))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf23-cdi/");
	}

	@Override
	protected String containerSpecificCdiBundle() {
		return "pax-cdi-jetty-weld";
	}

}
