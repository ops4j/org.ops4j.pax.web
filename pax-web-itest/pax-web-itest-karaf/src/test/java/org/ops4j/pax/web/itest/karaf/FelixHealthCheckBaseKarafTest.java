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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFilePutOption;
import org.osgi.framework.Bundle;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
public abstract class FelixHealthCheckBaseKarafTest extends AbstractKarafTestBase {

	protected Option[] felixHealthCheckOptions() {
		return new Option[] {
				scrConfig(),
				mavenBundle("org.osgi", "org.osgi.service.condition").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.healthcheck.api").versionAsInProject(),
				mavenBundle("org.apache.felix", "org.apache.felix.healthcheck.core").versionAsInProject().noStart(),
				new KarafDistributionConfigurationFilePutOption("etc/org.apache.felix.hc.core.impl.servlet.HealthCheckExecutorServlet.cfg", "servletPath", "/system/health"),
				new KarafDistributionConfigurationFilePutOption("etc/org.apache.felix.hc.core.impl.servlet.HealthCheckExecutorServlet.cfg", "servletContextName", "default"),
		};
	}

	@Before
	public void setUp() throws Exception {
		final Bundle hc = bundle("org.apache.felix.healthcheck.core");
		hc.stop();
		configureAndWaitForServletWithMapping("/system/health", hc::start);
	}

	@Test
	public void testHealthCheck() throws Exception {
		createTestClientForKaraf()
				.withReturnCode(200)
				.doGETandExecuteTest("http://localhost:8181/system/health.html");
	}

}
