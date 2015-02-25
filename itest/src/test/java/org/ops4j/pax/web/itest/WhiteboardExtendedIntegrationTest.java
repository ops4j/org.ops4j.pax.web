package org.ops4j.pax.web.itest;

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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @author Gareth Collins
 * @since Mar 2, 2013
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class WhiteboardExtendedIntegrationTest extends ITestBase {

    private Bundle installWarBundle;
    
    @Configuration
    public static Option[] configure() {
        return combine(
                	configureJetty(),
                	mavenBundle().groupId("org.ops4j.pax.web.samples")
                    	.artifactId("jetty-config-fragment")
                    	.version(getProjectVersion()).noStart(),
    				systemProperty("org.ops4j.pax.web.default.virtualhosts").value(
    						"127.0.0.1"),
    				systemProperty("org.ops4j.pax.web.default.connectors").value(
    						"default"),
					systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level")
    						.value("DEBUG")
    			);
    }

    @Before
    public void setUp() throws BundleException, InterruptedException {
        String bundlePath = "mvn:org.ops4j.pax.web.samples/whiteboard-extended/"
                + getProjectVersion();
        installWarBundle = installAndStartBundle(bundlePath);
        Thread.sleep(250);
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
    
    @Test
    public void testWhiteBoardContextFound() throws Exception {
        testWebPath("http://localhost:8282/foo/whiteboard/", "Hello Whiteboard Extender");
    }
    
    @Test
    public void testWhiteBoardContextWrongServlet() throws Exception {
        testWebPath("http://localhost:8282/foo/whiteboard2/", 404);
    }

    @Test
    public void testWhiteBoardContextNotFoundWrongConnector() throws Exception {
        testWebPath("http://localhost:8181/foo/whiteboard/", 404);
    }
        
    @Test
    public void testWhiteBoardContextNotFoundWrongVirtualHost() throws Exception {
        testWebPath("http://127.0.0.1:8282/foo/whiteboard/", 404);
    }
        
    @Test
    public void testWhiteBoardContext2Found() throws Exception {
        testWebPath("http://127.0.0.1:8181/bar/whiteboard2/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext2NotFoundWrongConnector() throws Exception {
        testWebPath("http://127.0.0.1:8282/bar/whiteboard2/", 404);
    }
        
    @Test
    public void testWhiteBoardContext2NotFoundWrongVirtualHost() throws Exception {
        testWebPath("http://localhost:8181/bar/whiteboard2/", 404);
    }
    
    @Test
    public void testWhiteBoardContext3Found() throws Exception {
        testWebPath("http://127.0.0.1:8282/whiteboard3/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext3NotFoundWrongConnector() throws Exception {
        testWebPath("http://127.0.0.1:8181/whiteboard3/", 404);
    }
        
    @Test
    public void testWhiteBoardContext3NotFoundWrongVirtualHost() throws Exception {
        testWebPath("http://localhost:8282/whiteboard3/", 404);
    }
    
    @Test
    public void testWhiteBoardContext4Found() throws Exception {
        testWebPath("http://127.0.0.1:8181/default/whiteboard4/", "Hello Whiteboard Extender");
    }

    @Test
    public void testWhiteBoardContext4NotFoundWrongConnector() throws Exception {
        testWebPath("http://127.0.0.1:8282/default/whiteboard4/", 404);
    }
        
    @Test
    public void testWhiteBoardContext4NotFoundWrongVirtualHost() throws Exception {
        testWebPath("http://localhost:8181/default/whiteboard4/", 404);
    }

}
