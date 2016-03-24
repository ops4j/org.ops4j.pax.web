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

import com.cedarsoft.test.utils.CatchAllExceptionsRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardServlet;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;

import javax.servlet.Servlet;
import javax.servlet.UnavailableException;
import java.util.Hashtable;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.MavenUtils.asInProject;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardDeregisteringContextIntegrationTest extends ITestBase {

	@Rule
	public CatchAllExceptionsRule catchAllExceptionsRule = new CatchAllExceptionsRule();
	
	private ServiceReference<WebContainer> serviceReference;
	private WebContainer webContainerService;

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty(),
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
						.noStart(),
				mavenBundle().groupId("com.cedarsoft.commons").artifactId("test-utils").version("6.0.1"),
				mavenBundle().groupId("com.cedarsoft.commons").artifactId("crypt").version("6.0.1"),
				mavenBundle().groupId("com.cedarsoft.commons").artifactId("xml-commons").version("6.0.1"),
				mavenBundle("commons-codec", "commons-codec").version(asInProject()),
				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-core").version("2.3.0"),
				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-databind").version("2.3.0"),
				mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations").version("2.3.0"),
				mavenBundle().groupId("com.google.guava").artifactId("guava").version("15.0"),

				wrappedBundle(mavenBundle("org.mockito","mockito-core").version("1.9.5")),
				wrappedBundle(mavenBundle("joda-time", "joda-time").version("2.3")),
				wrappedBundle(mavenBundle("org.objenesis", "objenesis").version("1.4")),
				wrappedBundle(mavenBundle("org.easytesting","fest-assert").version("1.4")),
				wrappedBundle(mavenBundle("org.easytesting","fest-reflect").version("1.4")),
				wrappedBundle(mavenBundle("xmlunit", "xmlunit").version("1.5")),
				wrappedBundle(mavenBundle("commons-io", "commons-io").version(asInProject())));
	}

	@Before
	public void setUp() throws BundleException, InterruptedException,
			UnavailableException {
		serviceReference = bundleContext
				.getServiceReference(WebContainer.class);

		while (serviceReference == null) {
			serviceReference = bundleContext.getServiceReference(WebContainer.class);
		}

		webContainerService = (WebContainer) bundleContext
				.getService(serviceReference);
	}

	@After
	public void tearDown() throws BundleException {
		webContainerService = null;
		if (bundleContext != null)
			bundleContext.ungetService(serviceReference);
		serviceReference = null;
	}

	@Test
	public void testDeregisterContext() throws Exception {
		Hashtable<String,String> props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "myContext");
		
		HttpContext httpContext = webContainerService.createDefaultHttpContext("myContext");
		
		ServiceRegistration<HttpContext> contextService = bundleContext.registerService(HttpContext.class, httpContext, props);
		
		props = new Hashtable<String, String>();
		props.put(ExtenderConstants.PROPERTY_ALIAS, "/ungetServletTest");
		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "myContext");
		
		ServiceRegistration<Servlet> servletService = bundleContext.registerService(Servlet.class, new WhiteboardServlet("ungetServletTest"), props);
		
		servletService.unregister();
		
		contextService.unregister();
		
	}

}
