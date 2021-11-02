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
package org.ops4j.pax.web.itest.tomcat.war.jsf;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.war.jsf.AbstractWarJSFWiredIntegrationTest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarJSFTomcatBundleWiredIntegrationTest extends AbstractWarJSFWiredIntegrationTest {

	@Configuration
	public Option[] configure() {
		Option[] serverOptions = combine(baseConfigureWithoutRuntime(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-tomcat-bundle").versionAsInProject());
		Option[] jspOptions = combine(serverOptions, paxWebJsp());
		// MyFaces and dependencies are installed as bundles
		Option[] jsfOptions = combine(jspOptions, myfaces());
		return combine(jsfOptions, paxWebExtenderWar());
	}

}
