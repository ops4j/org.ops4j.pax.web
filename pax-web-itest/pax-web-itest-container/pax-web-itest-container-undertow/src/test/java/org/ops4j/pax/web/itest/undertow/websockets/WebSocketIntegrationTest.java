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
package org.ops4j.pax.web.itest.undertow;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

import javax.websocket.WebSocketContainer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.common.AbstractWebSocketIntegrationTest;

import io.undertow.websockets.jsr.UndertowContainerProvider;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketIntegrationTest extends AbstractWebSocketIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return combine(
				configureWebSocketUndertow(),
                systemProperty("org.ops4j.pax.web.samples.websocket.register.programatically")
                    .value("true"),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("websocket-jsr356")
						.type("war")
						.version(VersionUtil.getProjectVersion()),
				mavenBundle().groupId("org.glassfish")
						.artifactId("javax.json").versionAsInProject());
	}

	@Test
	@Ignore (value = "PAXWEB-1027")
	public void testWebsocket() throws Exception {
		super.testWebsocket();
	}

	protected WebSocketContainer getWebSocketContainer() {
		ClassLoader orig = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(UndertowContainerProvider.class.getClassLoader());
			return UndertowContainerProvider.getWebSocketContainer();
		} finally {
			Thread.currentThread().setContextClassLoader(orig);
		}
	}
}