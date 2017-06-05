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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.WaitCondition;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Dictionary;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 */
@RunWith(PaxExam.class)
public class HttpServiceTCIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Configuration
	public Option[] configure() {
		return configureTomcat();
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8282/");
		initServletListener(null);
		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForServletListener();

	}

	@After
	public void tearDown() throws BundleException {
		logger.info("tear down ... ");
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}

		Bundle[] bundles = bundleContext.getBundles();
		for (Bundle b : bundles) {
			Dictionary<?, ?> headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

		logger.info(" ... good bye ... ");
	}


	@Test
	public void testSubPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8282/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8282/images/logo.png");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8282/images/logo.png");
		// test image-serving from different alias
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Hello World resources should be available under /alt-images",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8282/alt-images/logo.png");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseHeaderAssertion("Other resource paths will be served by servlet mapped at /*",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().startsWith("text/html")))
				.doGETandExecuteTest("http://127.0.0.1:8282/alt2-images/logo.png");
	}

	@Test
	public void testRootPath() throws Exception {

		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8282/");
	}

	@Test
	public void testServletPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: "))
				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
						resp -> resp.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:8282/lall/blubb");
	}

	@Test
	public void testServletDeRegistration() throws Exception {

		if (installWarBundle != null) {
			installWarBundle.stop();
		}


		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8282/");
	}

	@Test
	public void testNCSALogger() throws Exception {
		testSubPath();

		String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		//access_log.2013-06-13.log
		final File logFile = new File("target/logs/access_log." + date + ".log");

		logger.info("Log-File: {}", logFile.getAbsoluteFile());

		new WaitCondition("logfile") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return logFile.exists();
			}
		}.waitForCondition();

		assertNotNull(logFile);

		boolean exists = logFile.getAbsoluteFile().exists();

		assertTrue(exists);

		FileInputStream fstream = new FileInputStream(logFile.getAbsoluteFile());
		DataInputStream in = new DataInputStream(fstream);
		final BufferedReader brCheck = new BufferedReader(new InputStreamReader(in));

		new WaitCondition("logfile content") {
			@Override
			protected boolean isFulfilled() throws Exception {
				return brCheck.readLine() != null;
			}
		}.waitForCondition();

		brCheck.close();
		in.close();
		fstream.close();

		fstream = new FileInputStream(logFile.getAbsoluteFile());
		in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine = br.readLine();

		assertNotNull(strLine);
		in.close();
		fstream.close();
	}

	@Test
	public void testRestartServlet() throws Exception {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.start();
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain Path Info: /lall/blubb'",
						response -> response.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:8282/lall/blubb");
	}
}