package org.ops4j.pax.web.itest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WebListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Achim Nierbeck
 */
@RunWith(JUnit4TestRunner.class)
public class WarFormAuthIntegrationTest extends ITestBase {

 Logger LOG = LoggerFactory.getLogger(WarFormAuthIntegrationTest.class);

	private Bundle installWarBundle;

	private WebListener webListener;
	
	@Configuration
    public static Option[] configurationDetailed()
    {

		Option[] options = baseConfigure();

		Option[] options2 = options(
        		mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jetty-auth-config-fragment").version(getProjectVersion())

        );
        
        List<Option> list = new ArrayList<Option>(Arrays.asList(options));
		list.addAll(Arrays.asList(options2));

		return (Option[]) list.toArray(new Option[list.size()]);
    }


	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");
		
		webListener = new WebListenerImpl();
		bundleContext.registerService(WebListener.class.getName(), webListener,
				null);
		String bundlePath = WEB_BUNDLE
				+ "mvn:org.ops4j.pax.web.samples/war-formauth/"
				+ getProjectVersion() + "/war?"
				+ WEB_CONTEXT_PATH + "=/war-formauth";
		installWarBundle = bundleContext.installBundle(bundlePath);
		installWarBundle.start();

		int count = 0;
		while (!((WebListenerImpl) webListener).gotEvent() && count < 50) {
			synchronized (this) {
				this.wait(100);
				count++;
			}
		}
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
			if (b.getState() != Bundle.ACTIVE && b.getState() != Bundle.RESOLVED)
				fail("Bundle should be active: " + b);

			Dictionary headers = b.getHeaders();
			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null)
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			else
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
		}

	}

	@Test
	public void testWC() throws Exception {

		testWebPath("http://127.0.0.1:8181/war-formauth/wc", "<h1>Hello World</h1>");
			
	}

	@Test
	public void testWC_example() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war-formauth/wc/example", "<title>Login Page for Examples</title>\r\n");
		
		BasicHttpContext basicHttpContext = testFormWebPath("http://127.0.0.1:8181/war-formauth/login.jsp", "admin", "admin", 200);
		
//		testWebPath("http://127.0.0.1:8181/war-formauth/wc/example", "<h1>Hello World</h1>", basicHttpContext);
			
	}

	private void testWebPath(String path, String expectedContent,
			BasicHttpContext basicHttpContext) throws IOException {
		testWebPath(path, expectedContent, 200, false, basicHttpContext);
	}


	private BasicHttpContext testFormWebPath(String path, String user, String passwd, int httpRC) throws ClientProtocolException, IOException {
		HttpGet httpget = null;
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpHost targetHost = new HttpHost("localhost", 8181, "http"); 
		BasicHttpContext localcontext = new BasicHttpContext();

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("j_username", user ));
		formparams.add(new BasicNameValuePair("j_password", passwd ));
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		HttpPost httppost = new HttpPost(path);
		httppost.setEntity(entity);

		HttpResponse response = httpclient.execute(targetHost, httppost, localcontext);
		
		CookieOrigin cookieOrigin = (CookieOrigin) localcontext.getAttribute(
		        ClientContext.COOKIE_ORIGIN);
		CookieSpec cookieSpec = (CookieSpec) localcontext.getAttribute(
		        ClientContext.COOKIE_SPEC);

		
		assertEquals("HttpResponseCode", httpRC, response
				.getStatusLine().getStatusCode());

		return localcontext;
	}


	@Ignore
	@Test
	public void testWC_SN() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war-formauth/wc/sn", "<h1>Hello World</h1>");

	}
	
	@Ignore
	@Test
	public void testSlash() throws Exception {

			
		testWebPath("http://127.0.0.1:8181/war-formauth/", "<h1>Hello World</h1>");

	}

	
	private class WebListenerImpl implements WebListener {

		private boolean event = false;

		public void webEvent(WebEvent event) {
			LOG.info("Got event: " + event);
			if (event.getType() == 2)
				this.event = true;
		}

		public boolean gotEvent() {
			return event;
		}

	}

}
