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
package org.ops4j.pax.web.itest.undertow.httpservice;

import java.io.File;
import java.util.Dictionary;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.httpservice.AbstractHttpServiceProcessingWithConfigAdminIntegrationTest;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class HttpServiceProcessingWithWebFragmentIntegrationTest extends AbstractHttpServiceProcessingWithConfigAdminIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebUndertow());
		Option[] configOptions = combine(serverOptions, configAdmin());
		MavenArtifactProvisionOption auth = mavenBundle("org.ops4j.pax.web.samples", "auth-config-fragment-undertow")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		return combine(configOptions, auth);
	}

	@Override
	protected void configureContextProcessing(Dictionary<String, String> props) {
		props.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-helloworld");
		props.put("context.id", "default");
		props.put("context.webFragment", new File("../pax-web-itest-container-common/src/main/resources/security-fragment.xml").getAbsolutePath());
	}

}
