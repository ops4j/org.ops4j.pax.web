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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.repository;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WhiteboardR7JaxRsIntegrationTest extends ITestBase {

    @Configuration
    public static Option[] configure() {
        return combine(configureUndertow(),
                // aries-jax-rs-whiteboard not yet released
                repository("http://repository.apache.org/content/groups/snapshots/")
                        .id("aries-snapshots")
                        .allowSnapshots()
                        .disableReleases(),
                // Deps
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.service.jaxrs").versionAsInProject(),
                mavenBundle().groupId("javax.json").artifactId("javax.json-api").version("1.0"),
                mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").version("2.0.1"),
                mavenBundle().groupId("org.apache.ws.xmlschema").artifactId("xmlschema-core").version("2.2.1"),
                mavenBundle().groupId("org.ow2.asm").artifactId("asm").version("5.2"), // Undertow already uses ASM 6_Alpa
                // Runtime
                // Do not add this to Maven-Dependencies because Exam-Probe will get wrong imports and cause Classloader-issues
                mavenBundle().groupId("org.apache.aries.jax.rs").artifactId("org.apache.aries.jax.rs.api").version("0.0.1-SNAPSHOT"),
                mavenBundle().groupId("org.apache.aries.jax.rs").artifactId("org.apache.aries.jax.rs.whiteboard").version("0.0.1-SNAPSHOT"),
                // Sample
                mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("whiteboard-ds-jaxrs").versionAsInProject()
        );
    }

    @Test
    public void testWhiteboardJaxRsApplication() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello from JAXRS'",
                        resp -> resp.contains("Hello from JAXRS"))
                .doGETandExecuteTest("http://127.0.0.1:8181/jaxrs-application");
    }
}
