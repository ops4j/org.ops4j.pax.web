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
package org.ops4j.pax.web.itest.jetty.war;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.container.whiteboard.AbstractWhiteboardIntegrationTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarBasicIntegrationTest extends AbstractContainerTestBase {

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
		configureAndWaitForServletWithMapping("/servlet", () -> {
			// I'm not refreshing, so fragments have to be installed before their hosts
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
	public void test() {
		System.out.println(bundle.getState());
	}

}
