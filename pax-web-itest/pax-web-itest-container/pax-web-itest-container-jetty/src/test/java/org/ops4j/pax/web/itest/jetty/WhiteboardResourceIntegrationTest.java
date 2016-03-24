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
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Servlet;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardResourceIntegrationTest extends ITestBase {

	private ServiceRegistration<ResourceMapping> service;
	private ServiceRegistration<Servlet> servlet;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
				.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
				.noStart());

	}

	@Before
	public void setUp() throws BundleException, InterruptedException {

		DefaultResourceMapping resourceMapping = new DefaultResourceMapping();
		resourceMapping.setAlias("/whiteboardresources");
		resourceMapping.setPath("/images");
		service = bundleContext.registerService(ResourceMapping.class,
				resourceMapping, null);
		
//		Dictionary<String, String> initParams = new Hashtable<String, String>();
//		initParams.put("alias", "/test-resources");
//		servlet = bundleContext.registerService(Servlet.class,
//				new WhiteboardServlet("/test-resources"), initParams);

	}

	@After
	public void tearDown() throws BundleException {
		service.unregister();
	}

	@Test
	public void testWhiteBoardFiltered() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseHeaderAssertion("Header 'Content-Type' must be 'image/png'",
						headers -> headers.anyMatch(header -> header.getKey().equals("Content-Type")
								&& header.getValue().equals("image/png")))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboardresources/ops4j.png");

//		HttpResponse httpResponse = testClient.getHttpResponse(
//				"http://127.0.0.1:8181/whiteboardresources/ops4j.png", false, null, false);
//		Header header = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
//		assertEquals("image/png", header.getValue());
	}

}
