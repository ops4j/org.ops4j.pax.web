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
package org.ops4j.pax.web.itest.tomcat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.tinybundles.core.InnerClassStrategy;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.TestServlet;
import org.ops4j.pax.web.itest.base.support.TestServletContainerInitializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

/**
 * @author Marc Schlegel
 */
@RunWith(PaxExam.class)
public class WarContainerInitializerIntegrationTest extends ITestBase {

    @Configuration
    public static Option[] configure() {
        return combine(configureTomcat(),
                mavenBundle().groupId("org.ops4j.pax.tinybundles").artifactId("tinybundles").version(asInProject()),
                mavenBundle().groupId("biz.aQute.bnd").artifactId("bndlib").version(asInProject())
        );
    }

    private Bundle installWarBundle(String webXml) throws Exception {
        InputStream in = bundle()
                .add(TestServlet.class, InnerClassStrategy.NONE)
                .add(TestServletContainerInitializer.class, InnerClassStrategy.NONE)
                .add("WEB-INF/web.xml",
                        WarContainerInitializerIntegrationTest.class.getClassLoader().getResourceAsStream(webXml))
                .add("META-INF/services/javax.servlet.ServletContainerInitializer",
                        WarContainerInitializerIntegrationTest.class.getClassLoader().getResourceAsStream("META-INF/services/javax.servlet.ServletContainerInitializer"))
                .set(Constants.BUNDLE_SYMBOLICNAME, "war-bundle")
                .set(Constants.IMPORT_PACKAGE, "javax.servlet, javax.servlet.annotation, javax.servlet.http, org.ops4j.pax.web.itest.base.support")
                .set(Constants.EXPORT_PACKAGE, "org.ops4j.pax.web.itest.jetty")
                .set("Web-ContextPath", "contextroot")
                .build();
        return bundleContext.installBundle("bundleLocation", in);
    }


    @Test
    public void testServlet_2_5() throws Exception {
        initWebListener();
        Bundle bundle = installWarBundle("web-2.5.xml");
        bundle.start();

        waitForWebListener();

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'TEST OK'!", resp -> resp.contains("TEST OK"))
                .withResponseAssertion("Response must NOT contain 'FILTER-INIT'! Since this WAR uses Servlet 2.5 no ContainerInitializer should be used", resp -> !resp.contains("FILTER-INIT"))
                .doGETandExecuteTest("http://127.0.0.1:8282/contextroot/servlet");

        bundle.uninstall();
    }


    @Test
    public void testServlet_3_0() throws Exception {
        initWebListener();

        Bundle bundle = installWarBundle("web-3.0.xml");
        bundle.start();

        waitForWebListener();

        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'TEST OK'!", resp -> resp.contains("TEST OK"))
                .withResponseAssertion("Filter is registered in Service-Locator for ContainerInitializer. Response must contain 'FILTER-INIT'", resp -> resp.contains("FILTER-INIT"))
                .doGETandExecuteTest("http://127.0.0.1:8282/contextroot/servlet");

        bundle.uninstall();
    }
}
