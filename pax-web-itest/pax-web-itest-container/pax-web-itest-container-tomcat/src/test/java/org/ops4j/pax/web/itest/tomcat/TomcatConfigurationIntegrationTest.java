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

import org.junit.After;
import org.junit.Assert;
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
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.common.ITestBase.configureTomcat;

import java.io.File;
import java.io.FilenameFilter;
import java.net.InetAddress;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class TomcatConfigurationIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
	    // delete the access log before the server starts
	    purgeLogDir();
		return combine(
				configureTomcat(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("tomcat-config-fragment")
						.version(VersionUtil.getProjectVersion()).noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		logger.info("Setting up test");

		initWebListener();

		final String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war/" + VersionUtil.getProjectVersion()
				+ "/war?" + WEB_CONTEXT_PATH + "=/test";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		waitForWebListener();
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}

	@Test
	public void testWeb() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://localhost:8282/test/wc/example");
		// the valve configured in the tomcat-server.xml should have written an access log
		checkAccessLog();
	}

	@Test
	public void testWebIP() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8282/test/wc/example");
        // the valve configured in the tomcat-server.xml should have written an access log
        checkAccessLog();
	}

	/*
	 * The web.xml contains another connector with an alternate port. Check that this also works
	 */
	@Test
	public void testWebAlternatePort() throws Exception {
	    HttpTestClientFactory.createDefaultTestClient()
	            .withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
	                    resp -> resp.contains("<h1>Hello World</h1>"))
	            .doGETandExecuteTest("http://127.0.0.1:8283/test/wc/example");
	    // the valve configured in the tomcat-server.xml should have written an access log
	    checkAccessLog();
	}

	/*
	 * The default connector should bind to 0.0.0.0 try another address
	 */
    @Test
    public void testWebAlternateIp() throws Exception {
        String hostname = InetAddress.getLocalHost().getHostAddress();
        HttpTestClientFactory.createDefaultTestClient()
                .withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
                        resp -> resp.contains("<h1>Hello World</h1>"))
                .doGETandExecuteTest("http://" + hostname + ":8282/test/wc/example");
        // the valve configured in the tomcat-server.xml should have written an access log
        checkAccessLog();
    }

    private void checkAccessLog() {
        File[] files = getLogDir().listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("localhost_access_log.*log");
            }});
        Assert.assertTrue("http access log is missing", files.length == 1);
    }

	private static void purgeLogDir() {
	    purgeDirectory(getLogDir());
	}

	private static File getLogDir() {
	    File logDir = new File("target/target/logs");
	    if (logDir.exists() && !logDir.isDirectory()) {
	        logDir.delete();
	    }
	    if (!logDir.exists()) {
	        logDir.mkdirs();
	    }
	    return logDir;
	}
	
	private static void purgeDirectory(File dir) {
	    for (File file: dir.listFiles()) {
	        if (!file.isDirectory()) file.delete();
	    }
	}
}
