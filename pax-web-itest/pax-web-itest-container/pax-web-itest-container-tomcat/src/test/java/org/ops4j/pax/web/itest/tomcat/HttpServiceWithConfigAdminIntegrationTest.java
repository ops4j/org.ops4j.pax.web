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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;


/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class HttpServiceWithConfigAdminIntegrationTest extends ITestBase {

	private Bundle installWarBundle;

	@Inject
	private ConfigurationAdmin caService;


	@Configuration
	public static Option[] configure() {
		return combine(configureTomcat(),
				mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.4.0"));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException, IOException {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(WebContainerConstants.PID);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(WebContainerConstants.PROPERTY_LISTENING_ADDRESSES, "127.0.0.1");
		props.put(WebContainerConstants.PROPERTY_HTTP_PORT, "8282");

		config.setBundleLocation(null);
		config.update(props);

		String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-hs/" + VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);

		waitForServer("http://127.0.0.1:8282/");

		// Wait a second. This is really ugly but without that the tests flicker
		Thread.sleep(1500);
	}

	@After
	public void tearDown() throws BundleException {
		if (installWarBundle != null) {
			installWarBundle.stop();
			installWarBundle.uninstall();
		}
	}


	@Test
	public void testSubPath() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8282/helloworld/hs");
		// test image-serving
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest("http://127.0.0.1:8282/images/logo.png");
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
	}

	/**
	 * Tests reconfiguration to another port
	 *
	 * @throws Exception when any error occurs
	 */
	@Test
	public void testReconfiguration() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: "))
				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
						resp -> resp.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:8282/lall/blubb");

		org.osgi.service.cm.Configuration config = caService.getConfiguration(WebContainerConstants.PID);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(WebContainerConstants.PROPERTY_LISTENING_ADDRESSES, "127.0.0.1");
		props.put(WebContainerConstants.PROPERTY_HTTP_PORT, "9191");

		config.setBundleLocation(null);
		config.update(props);

		waitForServer("http://127.0.0.1:9191/");

		// Wait a second. This is really ugly but without that the tests flicker
		Thread.sleep(1500);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Servlet Path: '",
						resp -> resp.contains("Servlet Path: "))
				.withResponseAssertion("Response must contain 'Path Info: /lall/blubb'",
						resp -> resp.contains("Path Info: /lall/blubb"))
				.doGETandExecuteTest("http://127.0.0.1:9191/lall/blubb");
	}

}
