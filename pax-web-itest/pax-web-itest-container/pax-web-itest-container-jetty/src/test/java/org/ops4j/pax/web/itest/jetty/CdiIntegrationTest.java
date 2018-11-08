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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.base.TestConfiguration.*;
import static org.ops4j.pax.web.itest.common.ITestBase.configureJetty;

/**
 * @author Marc Schlegel
 */
@RunWith(PaxExam.class)
public class CdiIntegrationTest extends ITestBase {

    Logger LOG = LoggerFactory.getLogger(CdiIntegrationTest.class);
    private static final String VERSION_PAX_CDI = "1.0.0";

    private Option[] configureJsfAndCdi() {
        return options(
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
                // API
                mavenBundle("javax.annotation", "javax.annotation-api").version("1.2"),
                mavenBundle("javax.el", "javax.el-api").version("3.0.0"),
                mavenBundle("javax.enterprise", "cdi-api").version("1.2"),
                mavenBundle("javax.interceptor", "javax.interceptor-api").version("1.2"),
                mavenBundle("javax.validation", "validation-api").version("1.1.0.Final"),
                // Common
                mavenBundle("com.google.guava", "guava").version("19.0"),
                mavenBundle("commons-io", "commons-io").version("1.4"),
                mavenBundle("commons-codec", "commons-codec").version("1.10"),
                mavenBundle("commons-beanutils", "commons-beanutils").version("1.9.3"),
                mavenBundle("commons-collections", "commons-collections").version("3.2.2"),
                mavenBundle("commons-digester", "commons-digester").version("1.8.1"),
                mavenBundle("org.apache.commons", "commons-lang3").version("3.4"),
                // JSF
                mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-jsp").version(asInProject()),
                mavenBundle("org.apache.myfaces.core", "myfaces-api").version("2.2.12"),
                mavenBundle("org.apache.myfaces.core", "myfaces-impl").version("2.2.12"),
                // Weld
                mavenBundle("org.jboss.classfilewriter", "jboss-classfilewriter").version("1.1.2.Final"),
                mavenBundle("org.jboss.weld", "weld-osgi-bundle").version("2.4.5.Final")
        );
    }

    @Configuration
    public Option[] config() {
        return combine(org.ops4j.pax.web.itest.common.ITestBase.configureJetty(), configureJsfAndCdi());
    }

    @Before
    public void setUp() throws Exception{
        // Pax-CDI started later, because order is important
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-api").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-spi").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-extender").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-extension").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-web").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-jetty-weld").version(VERSION_PAX_CDI).getURL());
        installAndStartBundle(mavenBundle().groupId("org.ops4j.pax.cdi").artifactId("pax-cdi-weld").version(VERSION_PAX_CDI).getURL());
    }

    @Test
    public void testCdi() throws Exception {
        LOG.info("--------------- Installing WAR");
        // prepare Bundle
        initWebListener();
        final Bundle webAppBundle = installAndStartBundle(mavenBundle()
                .groupId("org.ops4j.pax.web.samples")
                .artifactId("war-jsf22-cdi")
                .versionAsInProject()
                .type("war")
                .getURL());

//      final Bundle webAppBundle = installAndStartBundle(mavenBundle()
//              .groupId("org.ops4j.pax.cdi.samples")
//              .artifactId("pax-cdi-sample4-jsf")
//              .version(VERSION_PAX_CDI)
//              .getURL());


        waitForWebListener();
        LOG.info("--------------- 1st Test");
        // Test
//      HttpTestClientFactory.createDefaultTestClient()
//              .withResponseAssertion("Response must contain 'Hello from CDI-Managed SessionBean'",
//                      resp -> resp.contains("Hello from CDI-Managed SessionBean"))
//              .doGETandExecuteTest("http://127.0.0.1:8181/war-jsf-cdi/index.xhtml");
        // As described in PAXWEB-760
        // 1. Call Http-Request which involves a CDI bean
        HttpTestClientFactory.createDefaultTestClient()
            .withResponseAssertion("Hello from CDI Managed bean expected", resp -> resp.contains("Hello from CDI-Managed SessionBean"))
            .doGET("http://localhost:8181/war-jsf22-cdi/start.xhtml")
            .executeTest();
            
//        .testWebPath("http://localhost:8181/war-jsf-cdi/index.xhtml", "Hello from CDI-Managed SessionBean");
//      testClient.testWebPath("http://127.0.0.1:8181/sample4/poll.jsf", "Which OSGi framework do you prefer");
        // 2. Update Bundle
        LOG.info("--------------- 2nd Update Webapp");
        webAppBundle.stop();
        new WaitCondition("Webapp stopped") {
            @Override
            protected boolean isFulfilled() throws Exception {
                return webAppBundle.getState() == Bundle.RESOLVED;
            }
        }.waitForCondition();
        initWebListener();
        webAppBundle.start();
        new WaitCondition("Webapp started") {
            @Override
            protected boolean isFulfilled() throws Exception {
                return webAppBundle.getState() == Bundle.ACTIVE;
            }
        }.waitForCondition();
        waitForWebListener();
        // 3. Call Http-Request which involves a CDI bean again
        LOG.info("--------------- 3rd Test after update");
//        testClient.testWebPath("http://127.0.0.1:8181/war-jsf-cdi/index.xhtml", "Hello from CDI-Managed SessionBean");
        HttpTestClientFactory.createDefaultTestClient()
        .withResponseAssertion("Hello from CDI Managed bean expected", resp -> resp.contains("Hello from CDI-Managed SessionBean"))
        .doGET("http://127.0.0.1:8181/war-jsf22-cdi/start.xhtml")
        .executeTest();
//      testClient.testWebPath("http://127.0.0.1:8181/sample4/poll.jsf", "Which OSGi framework do you prefer");
    }
}
