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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Gareth Collins
 * @since Mar 2, 2013
 */
@RunWith(PaxExam.class)
public class WhiteboardExtendedIntegrationTest extends ITestBase {

    private Bundle installWarBundle;
    
    @Configuration
    public static Option[] configure() {
        return combine(
                configureJetty(),
                mavenBundle().groupId("org.ops4j.pax.web.samples")
                    .artifactId("jetty-config-fragment")
                    .version(VersionUtil.getProjectVersion()).noStart(),
    				systemProperty("org.ops4j.pax.web.default.virtualhosts").value(
    						"127.0.0.1"),
    				systemProperty("org.ops4j.pax.web.default.connectors").value(
    						"default"));
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard-extended/"
                + VersionUtil.getProjectVersion();
        installWarBundle = installAndStartBundle(bundlePath);
    }

    @After
    public void tearDown() throws BundleException {
        if (installWarBundle != null) {
            installWarBundle.stop();
            installWarBundle.uninstall();
        }
    }


    // port = 8282, virtual host = localhost - virtual host is ignored
    @Test
    public void testWhiteBoardContextFound() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://localhost:8282/foo/whiteboard/");

//        testClient.testWebPath("http://localhost:8282/foo/whiteboard/", "Hello Whiteboard Extender");
    }
    
    @Test
    public void testWhiteBoardContextWrongServlet() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://localhost:8282/foo/whiteboard2/");

//        testClient.testWebPath("http://localhost:8282/foo/whiteboard2/", 404);
    }

    @Test
    public void testWhiteBoardContextRightVirtualHostOnly() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://localhost:8181/foo/whiteboard/");

//        testClient.testWebPath("http://localhost:8181/foo/whiteboard/", 404);
    }
        
    @Test
    public void testWhiteBoardContextRightConnectorOnly() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://127.0.0.1:8282/foo/whiteboard/");

//        testClient.testWebPath("http://127.0.0.1:8282/foo/whiteboard/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContextNotFoundWrongVirtualHostAndConnector() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://127.0.0.1:8181/foo/whiteboard/");

//        testClient.testWebPath("http://127.0.0.1:8181/foo/whiteboard/", 404);
    }
    
    // port = 8181
    @Test
    public void testWhiteBoardContext2FoundIP() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://127.0.0.1:8181/bar/whiteboard2/");

//        testClient.testWebPath("http://127.0.0.1:8181/bar/whiteboard2/", "Hello Whiteboard Extender");
    }
        
    @Test
    public void testWhiteBoardContext2FoundLocalhost() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://localhost:8181/bar/whiteboard2/");

//        testClient.testWebPath("http://localhost:8181/bar/whiteboard2/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext2NotFoundWrongConnector() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://localhost:8282/bar/whiteboard2/");

//        testClient.testWebPath("http://localhost:8282/bar/whiteboard2/", 404);
    }
    
    // Virtual Host = 127.0.0.1
    @Test
    public void testWhiteBoardContext3FoundDefaultPort() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://127.0.0.1:8282/whiteboard3/");

//        testClient.testWebPath("http://127.0.0.1:8282/whiteboard3/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext3FoundJettyPort() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://127.0.0.1:8181/whiteboard3/");

//        testClient.testWebPath("http://127.0.0.1:8181/whiteboard3/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext3NotFoundWrongVirtualHost() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://localhost:8181/whiteboard3/");

//        testClient.testWebPath("http://localhost:8181/whiteboard3/", 404);
    }
    
    // From configuration - port = 8181, Virtual Host = 127.0.0.1 - virtual host is ignored
    @Test
    public void testWhiteBoardContext4Found() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://127.0.0.1:8181/default/whiteboard4/");

//        testClient.testWebPath("http://127.0.0.1:8181/default/whiteboard4/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext4FoundRightVirtualHostOnly() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://127.0.0.1:8282/default/whiteboard4/");

//        testClient.testWebPath("http://127.0.0.1:8282/default/whiteboard4/", 404);
    }
        
    @Test
    public void testWhiteBoardContext4FoundRightConnectorOnly() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
                        resp -> resp.contains("Hello Whiteboard Extender"))
                .doGETandExecuteTest("http://localhost:8181/default/whiteboard4/");

//        testClient.testWebPath("http://localhost:8181/default/whiteboard4/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext4NotFoundWrongVirtualHostAndConnector() throws Exception {
        HttpTestClientFactory.createDefaultTestClient()
                .withReturnCode(404)
                .doGETandExecuteTest("http://localhost:8282/default/whiteboard4/");

//        testClient.testWebPath("http://localhost:8282/default/whiteboard4/", 404);
    }

}
