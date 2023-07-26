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
package org.ops4j.pax.web.itest.undertow.war;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.web.itest.container.war.AbstractWarPostIntegrationTest;

import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarPostIntegrationTest extends AbstractWarPostIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigure(), paxWebUndertow());
		// this will install a fragment attached to pax-web-undertow bundle, so it can find "undertow.xml" resource
		// used to configure the Undertow server
		MavenArtifactProvisionOption postConfig = mavenBundle("org.ops4j.pax.web.samples", "limit-post-config-fragment-undertow")
				.versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1).noStart();
		Option[] postConfigOptions = combine(serverOptions, postConfig);
		return combine(postConfigOptions, paxWebExtenderWar());
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-limited-post", () -> {
			// I'm not refreshing, so fragments need to be installed before their hosts
			// this will install a fragment attached to war-limited-post WAB to add custom error page, because
			// Undertow simply returns HTTP 500 after throwing io.undertow.server.RequestTooBigException
			context.installBundle(sampleURI("war-limited-post-fragment-undertow"));
			installAndStartWebBundle("war-limited-post", "/war-limited-post");
		});
	}

	@Override
	protected int getPostSizeExceededHttpResponseCode() {
		// TODO: create an UNDERTOW jira issue. io.undertow.server.RequestTooBigException should be checked
		//       as the cause of IllegalStateException and better error code should be used (413)
		return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	}

}
