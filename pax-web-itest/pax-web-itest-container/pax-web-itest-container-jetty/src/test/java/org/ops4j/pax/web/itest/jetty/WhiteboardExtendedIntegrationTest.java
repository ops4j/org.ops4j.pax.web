package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.OptionUtils.combine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

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

    /**
     * You will get a list of bundles installed by default plus your testcase,
     * wrapped into a bundle called pax-exam-probe
     */
    @Test
    public void listBundles() {
        for (Bundle b : bundleContext.getBundles()) {
            System.out.println("Bundle " + b.getBundleId() + " : "
                    + b.getSymbolicName());
        }

    }
    
    // port = 8282, virtual host = localhost - virtual host is ignored
    @Test
    public void testWhiteBoardContextFound() throws Exception {
        testClient.testWebPath("http://localhost:8282/foo/whiteboard/", "Hello Whiteboard Extender");
    }
    
    @Test
    public void testWhiteBoardContextWrongServlet() throws Exception {
        testClient.testWebPath("http://localhost:8282/foo/whiteboard2/", 404);
    }

    @Test
    public void testWhiteBoardContextRightVirtualHostOnly() throws Exception {
        testClient.testWebPath("http://localhost:8181/foo/whiteboard/", 404);
    }
        
    @Test
    public void testWhiteBoardContextRightConnectorOnly() throws Exception {
        testClient.testWebPath("http://127.0.0.1:8282/foo/whiteboard/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContextNotFoundWrongVirtualHostAndConnector() throws Exception {
        testClient.testWebPath(retrieveBaseUrl()+"/foo/whiteboard/", 404);
    }
    
    // port = 8181
    @Test
    public void testWhiteBoardContext2FoundIP() throws Exception {
        testClient.testWebPath(retrieveBaseUrl()+"/bar/whiteboard2/", "Hello Whiteboard Extender");
    }
        
    @Test
    public void testWhiteBoardContext2FoundLocalhost() throws Exception {
        testClient.testWebPath("http://localhost:8181/bar/whiteboard2/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext2NotFoundWrongConnector() throws Exception {
        testClient.testWebPath("http://localhost:8282/bar/whiteboard2/", 404);
    }
    
    // Virtual Host = 127.0.0.1
    @Test
    public void testWhiteBoardContext3FoundDefaultPort() throws Exception {
        testClient.testWebPath("http://127.0.0.1:8282/whiteboard3/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext3FoundJettyPort() throws Exception {
        testClient.testWebPath(retrieveBaseUrl()+"/whiteboard3/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext3NotFoundWrongVirtualHost() throws Exception {
        testClient.testWebPath("http://localhost:8181/whiteboard3/", 404);
    }
    
    // From configuration - port = 8181, Virtual Host = 127.0.0.1 - virtual host is ignored
    @Test
    public void testWhiteBoardContext4Found() throws Exception {
        testClient.testWebPath(retrieveBaseUrl()+"/default/whiteboard4/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext4FoundRightVirtualHostOnly() throws Exception {
        testClient.testWebPath("http://127.0.0.1:8282/default/whiteboard4/", 404);
    }
        
    @Test
    public void testWhiteBoardContext4FoundRightConnectorOnly() throws Exception {
        testClient.testWebPath("http://localhost:8181/default/whiteboard4/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext4NotFoundWrongVirtualHostAndConnector() throws Exception {
        testClient.testWebPath("http://localhost:8282/default/whiteboard4/", 404);
    }

}
