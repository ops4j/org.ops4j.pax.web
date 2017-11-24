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

package org.ops4j.pax.web.itest.tomcat;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractWarExtendedIntegrationTest;

/**
 * @author Grzegorz Grzybek
 */
@RunWith(PaxExam.class)
@Ignore
public class WarExtendedIntegrationTest extends AbstractWarExtendedIntegrationTest {

	@Configuration
	public static Option[] configure() {
		List<Option> options = new LinkedList<>();
		// PAXWEB-1084 - websocket jars publish (in META-INF/services sense)
		// org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer
		// class which, since jetty 9.2.21 adds by default websocket support to each HttpServiceContext
		// all the below bundles are required to activate
		// org.eclipse.jetty.websocket:javax-websocket-server-impl so it configures HttpServiceContexts
		options.addAll(Arrays.asList(
				mavenBundle().groupId("org.apache.aries")
						.artifactId("org.apache.aries.util").version(asInProject()),
				// org.apache.aries.spifly.dynamic.bundle bundle provides
				// [osgi.extender;osgi.extender="osgi.serviceloader.registrar";version:Version="1.0"]
				// capability required by websocket-server, without which javax-websocket-server-impl
				// wouldn't start
				mavenBundle().groupId("org.apache.aries.spifly")
						.artifactId("org.apache.aries.spifly.dynamic.bundle").version(asInProject()),
				mavenBundle().groupId("javax.websocket")
						.artifactId("javax.websocket-api").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-api").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-client").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-server").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-servlet").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("websocket-common").version(asInProject()),
				// javax-websocket-server-impl bundle declares
				// /META-INF/services/javax.servlet.ServletContainerInitializer
				// which adds org.eclipse.jetty.websocket.jsr356.server.ServerContainer to each
				// context (webapp, HttpServiceContext, ...)
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-server-impl").version(asInProject()),
				mavenBundle().groupId("org.eclipse.jetty.websocket")
						.artifactId("javax-websocket-client-impl").version(asInProject())
		));
		options.addAll(Arrays.asList(configureTomcat()));
		return options.toArray(new Option[options.size()]);
	}
}
