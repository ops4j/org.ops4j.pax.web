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
package org.ops4j.pax.web.itest.jetty.whiteboard;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.whiteboard.AbstractWhiteboardVirtualHostsIntegrationTest;
import org.ops4j.pax.web.service.PaxWebConfig;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Gareth Collins
 * @since Mar 2, 2013
 */
@RunWith(PaxExam.class)
public class WhiteboardVirtualHostsIntegrationTest extends AbstractWhiteboardVirtualHostsIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebJetty());
		// this will install a fragment attached to pax-web-jetty bundle, so it can find "jetty.xml" resource
		// used to alter the Jetty server
		MavenArtifactProvisionOption config = mavenBundle("org.ops4j.pax.web.samples", "config-fragment-jetty")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		Option[] authOptions = combine(serverOptions, config,
				systemProperty(PaxWebConfig.PID_CFG_CONNECTOR_LIST).value("jettyConn1"));
		return combine(authOptions, paxWebExtenderWhiteboard());
	}

}
