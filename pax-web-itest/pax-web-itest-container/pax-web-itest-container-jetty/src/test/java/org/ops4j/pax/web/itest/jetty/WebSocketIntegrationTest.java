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
import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.websocket.WebSocketContainer;

import org.eclipse.jetty.websocket.jsr356.JettyClientContainerProvider;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.common.AbstractWebSocketIntegrationTest;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends AbstractWebSocketIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return combine(
				configureWebSocketJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("websocket-jsr356")
						.type("war")
						.version(VersionUtil.getProjectVersion()),
				mavenBundle().groupId("org.glassfish")
						.artifactId("javax.json").versionAsInProject());
	}

	protected WebSocketContainer getWebSocketContainer() {
		ClassLoader orig = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(JettyClientContainerProvider.class.getClassLoader());
			return JettyClientContainerProvider.getWebSocketContainer();
		} finally {
			Thread.currentThread().setContextClassLoader(orig);
		}
	}
}