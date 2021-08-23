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
package org.ops4j.pax.web.itest.karaf.todo;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.karaf.AbstractKarafTestBase;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public class ExtendedFeaturesKarafTest extends AbstractKarafTestBase {

//	@Configuration
//	public Option[] config() {
//		return combine(baseConfig(),
//				features(
//						maven().groupId("org.ops4j.pax.web")
//								.artifactId("pax-web-features").type("xml")
//								.classifier("features").versionAsInProject(),
//						"pax-http")
//		);
//	}
//
//	@Test
//	public void testHttpFeature() throws Exception {
//
//		assertTrue(featuresService.isInstalled(featuresService
//				.getFeature("pax-http")));
//
//	}
//
//	@Test
//	public void testWhiteboardFeature() throws Exception {
//
//		featuresService.installFeature("pax-http-whiteboard");
//
//		assertTrue(featuresService.isInstalled(featuresService
//				.getFeature("pax-http-whiteboard")));
//		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-http")));
//	}
//
//	@Test
//	public void testWebFeature() throws Exception {
//
//		featuresService.installFeature("pax-war");
//
//		assertTrue(featuresService.isInstalled(featuresService
//				.getFeature("pax-war")));
//		assertTrue(featuresService.isInstalled(featuresService
//				.getFeature("pax-http-whiteboard")));
//		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-http")));
//
//	}
}
