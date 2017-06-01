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
package org.ops4j.pax.web.itest.jetty;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.OptionUtils.combine;

@RunWith(PaxExam.class)
public class WarJsfCdiIntegrationTest extends ITestBase {

    // 1.0.0.RC2 misses pax-cdi-servlet
    // 1.0.0.RC1 has requirement: (&(osgi.wiring.package=org.ops4j.pax.web.service)(version>=3.0.0)(!(version>=5.0.0)))
    private static String VERSION_PAX_CDI = "1.0.0.RC2";

    private Option[] configureJsfAndCdi() {
        return options(
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                // API
                mavenBundle("javax.enterprise", "cdi-api").version("1.2"),
                mavenBundle("javax.validation", "validation-api").version("1.1.0.Final"),
                mavenBundle("javax.interceptor", "javax.interceptor-api").version("1.2"),
                // Common
                mavenBundle("com.google.guava", "guava").version("19.0"),
                mavenBundle("commons-io", "commons-io").version("1.4"),
                mavenBundle("commons-codec", "commons-codec").version("1.10"),
                mavenBundle("commons-beanutils", "commons-beanutils").version("1.8.3"),
                mavenBundle("commons-collections", "commons-collections").version("3.2.1"),
                mavenBundle("commons-digester", "commons-digester").version("1.8.1"),
                mavenBundle("org.apache.commons", "commons-lang3").version("3.4"),
                // JSF
                mavenBundle("org.glassfish", "javax.faces").version("2.2.13"),
                // Weld
                mavenBundle("org.jboss.classfilewriter", "jboss-classfilewriter").version("1.1.2.Final"),
                mavenBundle("org.jboss.logging", "jboss-logging").version("3.3.0.Final"),
                mavenBundle("org.jboss.weld", "weld-osgi-bundle").version("2.4.0.Final")
        );
    }

    @Configuration
    public Option[] config() {
        return combine(configureJetty(), configureJsfAndCdi());
    }

    @Before
    public void setUp() throws Exception{
        // Pax-CDI started later, because order is important
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-api").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-spi").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-extender").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-extension").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-servlet").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-web").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-web-weld").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-weld").version(VERSION_PAX_CDI).getURL());
    }

    @Test
    @Ignore
    public void testCdi() throws Exception {
        // prepare Bundle
        initWebListener();
        installAndStartBundle(mavenBundle()
                .groupId("org.ops4j.pax.web.samples")
                .artifactId("war-jsf-cdi")
                .versionAsInProject()
                .getURL());

        waitForWebListener();
        // Test
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello from CDI-Managed SessionBean'",
						resp -> resp.contains("Hello from CDI-Managed SessionBean"))
                .withResponseAssertion("Response must contain 'Hello from OSGi-Service'",
                        resp -> resp.contains("Hello from OSGi-Service"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-cdi/index.xhtml");
//        testClient.testWebPath("http://localhost:8181/war-jsf-cdi/index.xhtml", "Hello from CDI-Managed SessionBean");
    }
}