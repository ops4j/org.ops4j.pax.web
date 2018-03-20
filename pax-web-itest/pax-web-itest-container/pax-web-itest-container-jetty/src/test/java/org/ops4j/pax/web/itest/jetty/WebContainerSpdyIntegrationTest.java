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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.MavenUtils.asInProject;

import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebContainerSpdyIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory
			.getLogger(WebContainerSpdyIntegrationTest.class);

	private Bundle installWarBundle;

	@Configuration
	public static Option[] configure() {
		return OptionUtils.combine(
		        configureSpdyJetty(),
		        mavenBundle().groupId("org.eclipse.jetty.http2")
                        .artifactId("http2-http-client-transport").version(asInProject()),
                mavenBundle().groupId("org.eclipse.jetty.http2")
                        .artifactId("http2-client").version(asInProject()),
                mavenBundle().groupId("org.eclipse.jetty")
                        .artifactId("jetty-io").version(asInProject()),
                mavenBundle().groupId("org.eclipse.jetty")
                        .artifactId("jetty-alpn-client").version(asInProject()),
		        systemProperty("org.osgi.service.http.secure.enabled").value(
						"true"),
				systemProperty("org.ops4j.pax.web.ssl.keystore").value(
						WebContainerSpdyIntegrationTest.class.getClassLoader().getResource("keystore").getFile()),
				systemProperty("org.ops4j.pax.web.ssl.password").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.keypassword").value(
						"password"),
				systemProperty("org.ops4j.pax.web.ssl.clientauthneeded").value(
						"required"));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		initWebListener();
		final String bundlePath = "mvn:org.ops4j.pax.web.samples/helloworld-wc/"
				+ VersionUtil.getProjectVersion();
		installWarBundle = installAndStartBundle(bundlePath);
		waitForWebListener();
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
		for (final Bundle b : bundleContext.getBundles()) {
			if (b.getState() != Bundle.ACTIVE
					&& b.getState() != Bundle.RESOLVED) {
			    if (!b.getSymbolicName().contains("alpn"))
			        fail("Bundle should be active: " + b);
			}

			final Dictionary<String, String> headers = b.getHeaders();
			final String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath);
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName());
			}
		}

	}
	
	@Test
	@Ignore("org.eclipse.jetty.io.NegotiatingClientConnectionFactory not found by org.eclipse.jetty.alpn.client")
	public void testSimpleWebContextPathAvailability() throws Exception {
      HttpTestClientFactory.createDefaultTestClient()
      .withResponseAssertion("Response must contain '<h1>Hello World</h1>'",
              resp -> resp.contains("<h1>Hello World</h1>"))
      .doGETandExecuteTest("https://127.0.0.1:8443/helloworld/wc");	    
	}

	@Test
	@Ignore("org.eclipse.jetty.io.NegotiatingClientConnectionFactory not found by org.eclipse.jetty.alpn.client")
	public void testWebContextPath() throws Exception {
	    HTTP2Client client = new HTTP2Client();
        SslContextFactory sslContextFactory = new SslContextFactory(true);
        client.addBean(sslContextFactory);
        client.start();
        
        FuturePromise<Session> sessionPromise = new FuturePromise<>();
        client.connect(sslContextFactory, new InetSocketAddress("127.0.0.1", 8443), new ServerSessionListener.Adapter(), sessionPromise);
        
        Session session = sessionPromise.get(5, TimeUnit.SECONDS);
        
        HttpFields requestFields = new HttpFields();
        requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
        
        MetaData.Request request = new MetaData.Request("GET", new HttpURI("https://127.0.0.1:8443/helloworld/wc"), HttpVersion.HTTP_2, requestFields);
        HeadersFrame headersFrame = new HeadersFrame(request, null, true);
        
        
        Stream.Listener responseListener = new Stream.Listener.Adapter()
        {
            public String response;
            
            @Override
            public void onData(Stream stream, DataFrame frame, Callback callback)
            {
                byte[] bytes = new byte[frame.getData().remaining()];
                frame.getData().get(bytes);
                response = new String(bytes);

                LOG.info(
                        "---------------- Response with content received from '{}' ----------------\n" +
                                "---------------- START Response-Body ----------------\n" +
                                "{}\n" +
                                "---------------- END Response-Body ----------------"
                        , request.getURI(), response);
                
                doAssert(response);
                callback.succeeded();
            }
            
        };

        session.newStream(headersFrame, new FuturePromise<>(), responseListener);

        Thread.sleep(TimeUnit.SECONDS.toMillis(20));
        
        client.stop();
        
	}
	
	private void doAssert(String response) {
	    assertTrue(response.contains("Hello World"));
	}
}
