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

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWebConsoleIntegrationTest;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WebConsoleIntegrationTest extends AbstractWebConsoleIntegrationTest {

	@Configuration
	public static Option[] configure() {

		return OptionUtils
				.combine(
						configureJetty(),
						workingDirectory("target/paxexam/"),
						systemProperty(
								"org.ops4j.pax.logging.DefaultServiceLog.level")
								.value("TRACE"),
						systemProperty("org.osgi.service.http.hostname").value(
								"127.0.0.1"),
						systemProperty("org.osgi.service.http.port").value(
								"8181"),
						systemProperty("java.protocol.handler.pkgs").value(
								"org.ops4j.pax.url"),
						systemProperty(
								"org.ops4j.pax.url.war.importPaxLoggingPackages")
								.value("true"),
						systemProperty("org.ops4j.pax.web.log.ncsa.enabled")
								.value("true"),
						mavenBundle()
								.groupId("org.apache.felix")
								.artifactId("org.apache.felix.bundlerepository")
								.version("1.6.2"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.configadmin")
								.version("1.2.8"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.shell")
								.version("1.4.2"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.shell.tui")
								.version("1.4.1"),
						mavenBundle().groupId("org.apache.felix")
								.artifactId("org.apache.felix.webconsole")
								.version("3.1.8")
				);
	}
}