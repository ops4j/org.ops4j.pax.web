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
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.itest.base.support.TestActivator;
import org.ops4j.pax.web.itest.common.AbstractSimultaneousWhiteboardIntegrationTest;
import org.osgi.framework.Constants;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class SimultaneousWhiteboardIntegrationTest extends AbstractSimultaneousWhiteboardIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(asInProject()).noStart(),
				streamBundle(
						bundle().add(TestActivator.class)
								.add(WhiteboardFilter.class)
								.set(Constants.BUNDLE_ACTIVATOR, TestActivator.class.getName())
								.set(Constants.BUNDLE_SYMBOLICNAME,
										"org.ops4j.pax.web.itest.SimultaneousTest")
								.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
								.build()).noStart());
	}
}
