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
package org.ops4j.pax.web.itest.karaf;

import java.io.File;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileReplacementOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

public class BasicAuthTomcatKarafTest extends BasicAuthBaseKarafTest {

	@Configuration
	public Option[] configuration() {
		Option[] basicOptions = combine(tomcatConfig(), paxWebJsp());
		// this will install a fragment attached to pax-web-tomcat bundle, so it can find "tomcat-server.xml" resource
		// used to configure the Tomcat server
		MavenArtifactProvisionOption auth = mavenBundle("org.ops4j.pax.web.samples", "auth-config-fragment-tomcat")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		KarafDistributionConfigurationFileReplacementOption realm
				= new KarafDistributionConfigurationFileReplacementOption("target/test-classes/tomcat-users.xml",
				new File("target/test-classes/auth/tomcat-users.xml"));
		return combine(basicOptions, auth, realm);
	}

}
