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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WarFormAuthIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WarFormAuthIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configurationDetailed() {
		return combine(
				configureUndertow(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("undertow-auth-config-fragment")
						.version(VersionUtil.getProjectVersion()).noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();

		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-formauth/"
				+ VersionUtil.getProjectVersion() + "/war?" + WEB_CONTEXT_PATH
				+ "=/war-formauth";
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
	public void testWC() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-formauth/wc");

//		testClient.testWebPath("http://127.0.0.1:8181/war-formauth/wc",
//				"<h1>Hello World</h1>");
	}

	@Test
	public void testWebContainerExample() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<title>Login Page for Examples</title>'",
						resp -> resp.contains("<title>Login Page for Examples</title>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-formauth/wc/example");
		HttpTestClientFactory.createDefaultTestClient()
				.authenticate("admin", "admin", "Test Realm")
				.doPOST("http://127.0.0.1:8181/war-formauth/login.jsp")
				.addParameter("j_username", "admin")
				.addParameter("j_password", "admin")
				.executeTest();

//		testClient.testWebPath("http://127.0.0.1:8181/war-formauth/wc/example",
//				"<title>Login Page for Examples</title>\r\n");
//		BasicHttpContext basicHttpContext = testFormWebPath(
//				"http://127.0.0.1:8181/war-formauth/login.jsp", "admin",
//				"admin", 200);
	}

//	private BasicHttpContext testFormWebPath(String path, String user,
//			String passwd, int httpRC) throws IOException {
//		DefaultHttpClient httpclient = new DefaultHttpClient();
//		HttpHost targetHost = new HttpHost("localhost", 8181, "http");
//		BasicHttpContext localcontext = new BasicHttpContext();
//
//		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
//		formparams.add(new BasicNameValuePair("j_username", user));
//		formparams.add(new BasicNameValuePair("j_password", passwd));
//		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
//				"UTF-8");
//		HttpPost httppost = new HttpPost(path);
//		httppost.setEntity(entity);
//
//		HttpResponse response = httpclient.execute(targetHost, httppost,
//				localcontext);
//
//		CookieOrigin cookieOrigin = (CookieOrigin) localcontext
//				.getAttribute(ClientContext.COOKIE_ORIGIN);
//		CookieSpec cookieSpec = (CookieSpec) localcontext
//				.getAttribute(ClientContext.COOKIE_SPEC);
//
//		assertEquals("HttpResponseCode", httpRC, response.getStatusLine()
//				.getStatusCode());
//
//		return localcontext;
//	}

	@Test
	public void testWebContainerSN() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-formauth/wc/sn");

//		testClient.testWebPath("http://127.0.0.1:8181/war-formauth/wc/sn",
//				"<h1>Hello World</h1>");
	}

	@Ignore
	@Test
	public void testSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
						resp -> resp.contains("<h1>Hello World</h1>"))
				.doGETandExecuteTest("http://127.0.0.1:8181/war-formauth/");

//		testClient.testWebPath("http://127.0.0.1:8181/war-formauth/",
//				"<h1>Hello World</h1>");
	}
}
