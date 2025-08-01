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
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenUrlReference;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

/**
 * @author achim
 */
public abstract class SpringWiredBaseKarafTest extends SpringEmbeddedBaseKarafTest {

	@Before
	@Override
	public void setUp() throws Exception {
		configureAndWaitForDeploymentUnlessInstalled("war-spring-wired", () -> {
			installAndStartBundle(sampleWarURI("war-spring-wired"));
		});
	}

	public Option springConfig() {
		MavenUrlReference karafSpringFeatures = maven()
				.groupId("org.apache.karaf.features").artifactId("spring")
				.type("xml").classifier("features").version(getKarafVersion());

		return features(karafSpringFeatures, "spring", "spring-web");
	}

}
