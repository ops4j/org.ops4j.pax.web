/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.itest.undertow.whiteboard;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.web.itest.container.whiteboard.AbstractWhiteboardIntegrationTest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class WhiteboardUndertowBundleIntegrationTest extends AbstractWhiteboardIntegrationTest {

	@Configuration
	public Option[] configure() {
		return combine(baseConfigureWithoutRuntime(),
				combine(paxWebJsp(), paxWebExtenderWhiteboard(),
						mavenBundle("jakarta.websocket", "jakarta.websocket-api").versionAsInProject(),
						mavenBundle("jakarta.websocket", "jakarta.websocket-client-api").versionAsInProject(),
						mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-undertow-bundle").versionAsInProject()));
	}

}
