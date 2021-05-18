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
 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ops4j.pax.web.itest.undertow;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.TestConfiguration;
import org.ops4j.pax.web.itest.common.AbstractWarJsfResourcehandlerIntegrationTest;

/**
 * @author Marc Schlegel
 */
@RunWith(PaxExam.class)
public class WarJsfResourcehandlerIntegrationTest extends AbstractWarJsfResourcehandlerIntegrationTest {

	@Configuration
	public Option[] config() {
		return combine(configureUndertow(),
				TestConfiguration.jsfBundlesWithDependencies(),
				// Resources-Extender, Jsf-Resourcehandler and test-bundles
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-resources-extender").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-resources-jsf").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jsf-resourcehandler-myfaces").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jsf-resourcehandler-resourcebundle").versionAsInProject()
		);
	}
}