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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ops4j.pax.web.itest.container.war.jsf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.itest.utils.assertion.BundleMatchers;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.api.query.ResourceQueryMatcher;
import org.ops4j.pax.web.resources.api.query.ResourceQueryResult;
import org.ops4j.pax.web.resources.jsf.OsgiResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.web.itest.utils.assertion.Assert.assertThat;

/**
 * @author Marc Schlegel
 */
public class AbstractWarJSFResourcehandlerIntegrationTest extends AbstractContainerTestBase {

	/**
	 * The default implementation {@code org.ops4j.pax.web.resources.extender.internal.IndexedOsgiResourceLocator} is
	 * registered with {@link org.osgi.framework.Constants#SERVICE_RANKING} of -1, so when
	 * registering a new implementation, this new class must be served.
	 */
	@Test
	public void testServiceOverride() {
		OsgiResourceLocatorForTest expectedService = new OsgiResourceLocatorForTest();
		context.registerService(OsgiResourceLocator.class, new OsgiResourceLocatorForTest(), null);

		ServiceReference<OsgiResourceLocator> ref = context.getServiceReference(OsgiResourceLocator.class);

		if (ref != null) {
			OsgiResourceLocator service = context.getService(ref);
			if (service != null) {
				assertThat("'OsgiResourceLocatorForTest' must be found due to higher service-ranking!",
						service.getClass().getName(),
						serviceName -> expectedService.getClass().getName().equals(serviceName));
			} else {
				fail("Service could not be retrieved");
			}
		} else {
			fail("Service-Reference could not be retrieved");
		}
	}

	/**
	 * Does multiple assertions in one test since container-startup is slow
	 * <p/>
	 * <ul>
	 * 	<li>Check if pax-web-resources-jsf is started</li>
	 * 	<li>Check if application under test (jsf-application-myfaces) is started
	 * 	<li>Test actual resource-handler
	 * 		<ul>
	 * 			<li>Test for occurence of 'Hello JSF' (jsf-application-myfaces)</li>
	 * 			<li>Test for occurence of 'Standard Header' (jsf-resourcebundle)</li>
	 * 			<li>Test for occurence of 'iceland.jpg' from library 'default' in version '2_0' (jsf-resourcebundle)</li>
	 * 			<li>Test for occurence of 'Customized Footer' (jsf-resourcebundle)</li>
	 *          <li>Access a resource (image) via HTTP which gets loaded from a other bundle (jsf-resourcebundle)</li>
	 * 		</ul>
	 * 	</li>
	 *  <li>Test localized resource
	 * 	    <ul>
	 * 			<li>Test for occurence of 'flag.png' from library 'layout' with default locale 'en' which resolves to 'iceland' (default in faces-config)</li>
	 * 	        <li>Test for occurence of 'flag.png' from library 'layout' with default locale 'de' which resolves to 'germany'</li>
	 * 	    </ul>
	 * 	</li>
	 * 	<li>Test resource-overide
	 * 	    <ul>
	 * 	        <li>Install another bundle (jsf-resourcebundle-override) which also serves  template/footer.xhtml</li>
	 * 	        <li>Test for occurence of 'Overriden Footer' (jsf-resourcebundle-override)</li>
	 * 			<li>Test for occurence of 'iceland.jpg' from library 'default' in version '3_0' (jsf-resourcebundle-override)</li>
	 * 	        <li>Uninstall the previously installed bundle</li>
	 * 	        <li>Test again, this time for occurence of 'Customized Footer' (jsf-resourcebundle)</li>
	 * 	    </ul>
	 * 	</li>
	 * 	<li>
	 * 	    Test {@link org.ops4j.pax.web.resources.jsf.OsgiResource#userAgentNeedsUpdate(FacesContext)}
	 * 	    with an If-Modified-Since header
	 * 	</li>
	 * 	<li>Test servletmapping with prefix (faces/*) rather than extension for both, page and image serving</li>
	 * </ul>
	 */
	@Test
	public void testJsfResourceHandler() throws Exception {
		final String pageUrl = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/index.xhtml";
		final String imageUrl = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/javax.faces.resource/images/iceland.jpg.xhtml?type=osgi&ln=default&lv=2_0";

		configureAndWaitForDeploymentUnlessInstalled("jsf-resourcehandler-myfaces", () -> {
			installAndStartBundle(sampleURI("jsf-resourcehandler-myfaces"));
		});

		// start testing
		BundleMatchers.isBundleActive("org.ops4j.pax.web.pax-web-resources-extender", context);
		BundleMatchers.isBundleActive("org.ops4j.pax.web.pax-web-resources-jsf", context);
		BundleMatchers.isBundleActive("org.ops4j.pax.web.samples.jsf-resourcehandler-resourcebundle", context);
		BundleMatchers.isBundleActive("org.ops4j.pax.web.samples.jsf-resourcehandler-myfaces", context);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion(
						"Some Content shall be included from the jsf-application-bundle to test internal view-resources",
						resp -> resp.contains("Hello Included Content"))
				.withResponseAssertion(
						"Standard header shall be loaded from resourcebundle to test external view-resources",
						resp -> resp.contains("Standard Header"))
				.withResponseAssertion(
						"Images shall be loaded from resourcebundle to test external resources",
						resp -> resp.contains("iceland.jpg"))
				.withResponseAssertion(
						"Customized footer shall be loaded from resourcebundle to test external view-resources",
						resp -> resp.contains("Customized Footer"))
				.withResponseAssertion(
						"Image-URL must be created from OsgiResource",
						resp -> resp.contains("/osgi-resourcehandler-myfaces/javax.faces.resource/images/iceland.jpg.xhtml?type=osgi&amp;ln=default&amp;lv=2_0"))
				.withResponseAssertion(
						"Flag-URL must be served from iceland-folder",
						resp -> resp.contains("/osgi-resourcehandler-myfaces/javax.faces.resource/flag.png.xhtml?type=osgi&amp;loc=iceland&amp;ln=layout"))
				.doGETandExecuteTest(pageUrl);
		// Test German image
		HttpTestClientFactory.createDefaultTestClient()
				// set header for german-locale in JSF
				.addRequestHeader("Accept-Language", "de")
				.withReturnCode(200)
				.withResponseAssertion(
						"Flag-URL must be served from germany-folder",
						resp -> resp.contains("/osgi-resourcehandler-myfaces/javax.faces.resource/flag.png.xhtml?type=osgi&amp;loc=germany&amp;ln=layout"))
				.doGETandExecuteTest(pageUrl);
		// test resource serving for image
		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest(imageUrl);
		// Install override bundle
		String bundlePath = mavenBundle()
				.groupId("org.ops4j.pax.web.samples")
				.artifactId("jsf-resourcehandler-resourcebundle-override").versionAsInProject().getURL();
		Bundle installedResourceBundle = installAndStartBundle(bundlePath);
		BundleMatchers.isBundleActive(installedResourceBundle.getSymbolicName(), context);

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion(
						"Overriden footer shall be loaded from resourcebundle-override  to test external view-resources which are overriden",
						resp -> resp.contains("Overriden Footer"))
				.withResponseAssertion(
						"Iceland-Picture shall be found in version 3.0 from resourcebunde-override",
						resp -> resp.contains("javax.faces.resource/images/iceland.jpg.xhtml?type=osgi&amp;ln=default&amp;lv=2_0&amp;rv=3_0.jpg"))
				.doGETandExecuteTest(pageUrl);

		// uninstall overriding bundle
		installedResourceBundle.stop();

		new WaitCondition("Customized footer shall be loaded from resourcebundle") {
			@Override
			protected boolean isFulfilled() {
				try {
					HttpTestClientFactory.createDefaultTestClient()
							.withResponseAssertion(
									"Customized footer shall be loaded from resourcebundle",
									resp -> resp.contains("Customized Footer"))
							.doGETandExecuteTest(pageUrl);
					return true;
				} catch (AssertionError | Exception e) {
					return false;
				}
			}
		}.waitForCondition(5000, 1000,
				() -> fail("After uninstalling 'jsf-resourcehandler-resourcebundle-override' " +
						"the customized foot must be loaded again."));

		// Test If-Modified-Since
		ZonedDateTime now = ZonedDateTime.of(
				LocalDateTime.now(),
				ZoneId.of(ZoneId.SHORT_IDS.get("ECT")));
		// "Modified-Since should mark response with 304"
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(304)
				.addRequestHeader("If-Modified-Since", now.format(DateTimeFormatter.RFC_1123_DATE_TIME))
				.doGETandExecuteTest(imageUrl);

		// Test second faces-mapping which uses a prefix (faces/*)
		final String pageUrlWithPrefixMapping = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/faces/index.xhtml";
		final String imageUrlWithPrefixMapping = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/faces/javax.faces.resource/images/iceland.jpg?type=osgi&ln=default&lv=2_0";

		HttpTestClientFactory.createDefaultTestClient()
				.doGETandExecuteTest(imageUrlWithPrefixMapping);
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion(
						"Image-URL must be created from OsgiResource. This time the second servlet-mapping (faces/*) must be used.",
						resp -> resp.contains("/osgi-resourcehandler-myfaces/faces/javax.faces.resource/images/iceland.jpg?type=osgi&amp;ln=default&amp;lv=2_0"))
				.doGETandExecuteTest(pageUrlWithPrefixMapping);

		try {
			testResourceUnavailble();
			fail("Expected IOException");
		} catch (IOException ignored) {
		}
	}

	/**
	 * After a JSF thread received a resource, the bundle with the resource might be uninstalled
	 * anyway. This can happen before the actual bytes are served.
	 * <p>
	 * <ol>
	 * <li>createResource</li>
	 * <li>resourcebundle uninstalled</li>
	 * <li>resource.getInputStream</li>
	 * </ol>
	 * <p>
	 * According to the spec, IOException is the only one catched later on.
	 */
//	@Test(expected = IOException.class)
	public void testResourceUnavailble() throws Exception {
		ServiceReference<OsgiResourceLocator> sr = context.getServiceReference(OsgiResourceLocator.class);
		OsgiResourceLocator resourceLocator = context.getService(sr);

		ResourceInfo resourceInfo = resourceLocator.locateResource("default/2_0/images/iceland.jpg");
		FailsafeWorkaround.test(resourceInfo, context, sr);
	}

	/**
	 * Fake service-impl for {@link OsgiResourceLocator} because Mockito 2+ is
	 * currently not running in pax-exam
	 */
	private static class OsgiResourceLocatorForTest implements OsgiResourceLocator {

		private final ResourceInfo resource;

		OsgiResourceLocatorForTest() {
			URL url;
			try {
				url = new URL("bundle://" + FrameworkUtil.getBundle(this.getClass()).getBundleId()
						+ "0.0//META-INF/resources/hello.html");
			} catch (MalformedURLException e) {
				e.printStackTrace();
				url = null;
			}
			resource = new ResourceInfo(url, LocalDateTime.now(), 0L);
		}

		@Override
		public void register(Bundle bundle) {
			System.out.println("Register called on OsgiResourceLocatorForTest");
		}

		@Override
		public void unregister(Bundle bundle) {
			System.out.println("Unregister called on OsgiResourceLocatorForTest");
		}

		@Override
		public ResourceInfo locateResource(String resourceName) {
			return resource;
		}

		@Override
		public <R extends ResourceQueryResult, Q extends ResourceQueryMatcher> Collection<R> findResources(
				Q queryMatcher) {
			return null;
		}
	}

	private static class FailsafeWorkaround {

		public static void test(ResourceInfo resourceInfo, BundleContext context, ServiceReference<OsgiResourceLocator> sr) throws Exception {
			Resource resource = new OsgiResource(resourceInfo.getUrl(), null, "iceland.jpg", null, "default", "2_0", resourceInfo.getLastModified());
			// uninstall bundle
			Arrays.stream(context.getBundles())
					.filter(bundle -> bundle.getSymbolicName().equals("org.ops4j.pax.web.samples.jsf-resourcehandler-resourcebundle"))
					.findFirst().orElseThrow(() -> new AssertionError("Bundle 'jsf-resourcehandler-resourcebundle' not found"))
					.uninstall();
			Thread.sleep(1000); //to fast for tests, resource isn't fully gone yet

			try {
				resource.getInputStream();
				fail("IOException expected due to missing resource!");
			} finally {
				context.ungetService(sr);
			}
		}

	}

}
