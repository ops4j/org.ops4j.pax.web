/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ops4j.pax.web.itest.jetty;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.web.itest.base.assertion.Assert.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.assertion.BundleMatchers;
import org.ops4j.pax.web.resources.jsf.OsgiResource;
import org.ops4j.pax.web.resources.api.OsgiResourceLocator;
import org.ops4j.pax.web.resources.api.ResourceInfo;
import org.ops4j.pax.web.resources.extender.internal.IndexedOsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Marc Schlegel
 */
@RunWith(PaxExam.class)
public class WarJsfResourcehandlerIntegrationTest extends ITestBase {

    private Option[] configureMyfacesWithSamples() {
        return options(
                mavenBundle("org.ops4j.pax.web", "pax-web-jsp").versionAsInProject(),
                // MyFaces
                mavenBundle("org.apache.myfaces.core", "myfaces-api").versionAsInProject(),
                mavenBundle("org.apache.myfaces.core", "myfaces-impl").versionAsInProject(),
                mavenBundle("javax.annotation", "javax.annotation-api").version("1.2"),
                mavenBundle("javax.interceptor", "javax.interceptor-api").version("1.2"),
                mavenBundle("javax.enterprise", "cdi-api").version("1.2"),
                mavenBundle("javax.validation", "validation-api").version("1.1.0.Final"),
                mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.javax-inject").version("1_2"),
                // Commons
                mavenBundle("commons-io", "commons-io").version("1.4"),
                mavenBundle("commons-codec", "commons-codec").version("1.10"),
                mavenBundle("commons-beanutils", "commons-beanutils").version("1.8.3"),
                mavenBundle("commons-collections", "commons-collections").version("3.2.1"),
                mavenBundle("commons-digester", "commons-digester").version("1.8.1"),
                mavenBundle("org.apache.commons", "commons-lang3").version("3.4"),
                // Resources-Extender, Jsf-Resourcehandler and test-bundles
                mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-resources-extender").versionAsInProject(),
                mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-resources-jsf").versionAsInProject(),
                mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jsf-resourcehandler-myfaces").versionAsInProject(),
                mavenBundle().groupId("org.ops4j.pax.web.samples").artifactId("jsf-resourcehandler-resourcebundle").versionAsInProject()
        );
    }

    @Configuration
    public Option[] config() {
        return combine(configureJetty(), configureMyfacesWithSamples());
    }

    /**
     * The default implementation {@link IndexedOsgiResourceLocator} is
     * registered with {@link Constants#SERVICE_RANKING} of -1, so when
     * registering a new implementation, this new class must be served.
     */
    @Test
    public void testServiceOverride() throws Exception {

        OsgiResourceLocatorForTest expectedService = new OsgiResourceLocatorForTest();
        bundleContext.registerService(OsgiResourceLocator.class, new OsgiResourceLocatorForTest(), null);

        ServiceReference<OsgiResourceLocator> ref = bundleContext.getServiceReference(OsgiResourceLocator.class);

        if (ref != null) {
            OsgiResourceLocator service = bundleContext.getService(ref);
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
     *
     * <pre>
     * <ul>
     * 	<li>Check if pax-web-resources-jsf is started</li>
     * 	<li>Check if application under test (jsf-application-myfaces) is started
     * 	<li>Test actual resource-handler
     * 		<ul>
     * 			<li>Test for occurence of 'Hello JSF' (jsf-application-myfaces)</li>
     * 			<li>Test for occurence of 'Standard Header' (jsf-resourcebundle)</li>
     * 			<li>Test for occurence of 'iceland.jpg' (jsf-resourcebundle)</li>
     * 			<li>Test for occurence of 'Customized Footer' (jsf-resourcebundle)</li>
     *          <li>Access a resource (image) via HTTP which gets loaded from a other bundle (jsf-resourcebundle)</li>
     * 		</ul>
     * 	</li>
     * 	<li>Test resource-overide
     * 	    <ul>
     * 	        <li>Install another bundle (jsf-resourcebundle-override) which also serves  template/footer.xhtml</li>
     * 	        <li>Test for occurence of 'Overriden Footer' (jsf-resourcebundle-override)</li>
     * 	        <li>Uninstall the previously installed bundle</li>
     * 	        <li>Test again, this time for occurence of 'Customized Footer' (jsf-resourcebundle)</li>
     * 	    </ul>
     * 	</li>
     * 	<li>
     * 	    Test {@link OsgiResource#userAgentNeedsUpdate(FacesContext)}
     * 	    with an If-Modified-Since header
     * 	</li>
     * 	<li>Test servletmapping with prefix (faces/*) rather than extension for both, page and image serving</li>
     * </ul>
     * </pre>
     */
    @Test
    public void testJsfResourceHandler() throws Exception {
        // prepare Bundle
        initWebListener();
        installAndStartBundle(
                mavenBundle()
                        .groupId("org.ops4j.pax.web.samples")
                        .artifactId("jsf-resourcehandler-myfaces")
                        .versionAsInProject()
                        .getURL());

        waitForWebListener();
        
        // start testing
        final String pageUrl = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/index.xhtml";
        final String imageUrl = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/javax.faces.resource/iceland.jpg.xhtml?ln=images";
        BundleMatchers.isBundleActive("org.ops4j.pax.web.pax-web-resources-extender", bundleContext);
        BundleMatchers.isBundleActive("org.ops4j.pax.web.pax-web-resources-jsf", bundleContext);
        BundleMatchers.isBundleActive("jsf-resourcehandler-myfaces", bundleContext);
        // call url and check
        String response = testClient.testWebPath(pageUrl, HttpStatus.SC_OK);
        assertThat("Some Content shall be included from the jsf-application-bundle to test internal view-resources",
                response,
                resp -> StringUtils.contains(resp, "Hello Included Content"));
        assertThat("Standard header shall be loaded from resourcebundle to test external view-resources",
                response,
                resp -> StringUtils.contains(resp, "Standard Header"));
        assertThat("Images shall be loaded from resourcebundle to test external resources",
                response,
                resp -> StringUtils.contains(resp, "iceland.jpg"));
        assertThat("Customized footer shall be loaded from resourcebundle to test external view-resources",
                response,
                resp -> StringUtils.contains(resp, "Customized Footer"));
        
        // test resource serving for image
        testClient.testWebPath(imageUrl, HttpStatus.SC_OK);
        // Install override bundle
        String bundlePath = mavenBundle()
                .groupId("org.ops4j.pax.web.samples")
                .artifactId("jsf-resourcehandler-resourcebundle-override").versionAsInProject().getURL();
        Bundle installedResourceBundle = installAndStartBundle(bundlePath);
        BundleMatchers.isBundleActive(installedResourceBundle.getSymbolicName(), bundleContext);
        // call url
        response = testClient.testWebPath(pageUrl, HttpStatus.SC_OK);
        assertThat("Overriden footer shall be loaded from resourcebundle-override  to test external view-resources which are overriden",
                response,
                resp -> StringUtils.contains(resp, "Overriden Footer"));
        // uninstall overriding bundle
        installedResourceBundle.stop();
        
        Thread.sleep(1000); //to fast for tests, resource isn't fully gone yet 
        
        // call url and test that previously shadowed resource (footer) is served again
        response = testClient.testWebPath(pageUrl, HttpStatus.SC_OK);
        assertThat("Customized footer shall be loaded from resourcebundle",
                response,
                resp -> StringUtils.contains(resp, "Customized Footer"));

        // Test If-Modified-Since
        HttpGet request = new HttpGet(imageUrl);
        HttpClient client = HttpClients.createDefault();

        ZoneId zone = ZoneId.of(ZoneId.SHORT_IDS.get("ECT"));
        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(), zone);
        request.setHeader(HttpHeaders.IF_MODIFIED_SINCE, now.format(DateTimeFormatter.RFC_1123_DATE_TIME));

        HttpResponse httpResponse = client.execute(request);
        assertThat("Modified-Since should mark response with 304",
                httpResponse.getStatusLine().getStatusCode(),
                statusCode -> statusCode == HttpStatus.SC_NOT_MODIFIED);
        
        // Test second faces-mapping which uses a prefix (faces/*)
        final String pageUrlWithPrefixMapping = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/faces/index.xhtml";
        final String imageUrlWithPrefixMapping = "http://127.0.0.1:8181/osgi-resourcehandler-myfaces/faces/javax.faces.resource/iceland.jpg?ln=images";
        response = testClient.testWebPath(pageUrlWithPrefixMapping, HttpStatus.SC_OK);
        assertThat("Image must be served with prefix-servlet-mapping", 
        		response, 
        		resp -> StringUtils.contains(resp, "/osgi-resourcehandler-myfaces/faces/javax.faces.resource/iceland.jpg?ln=images"));
        testClient.testWebPath(imageUrlWithPrefixMapping, HttpStatus.SC_OK);
    }
    
    /**
     * After a JSF thread received a resource, the bundle with the resource might be uninstalled
     * anyway. This can happen before the actual bytes are served.
     * 
     * <ol>
     * 	<li>createResource</li>
     * 	<li>resourcebundle uninstalled</li>
     * 	<li>resource.getInputStream</li>
     * </ol>
     * 
     * According to the spec, IOException is the only one catched later on.
     */
    @Test(expected = IOException.class)
    public void testResourceUnavailble () throws Exception {
    	ServiceReference<OsgiResourceLocator> sr = bundleContext.getServiceReference(OsgiResourceLocator.class);
    	OsgiResourceLocator resourceLocator = bundleContext.getService(sr);
    	
    	ResourceInfo resourceInfo = resourceLocator.locateResource("images/iceland.jpg");
    	Resource resource = new OsgiResource(resourceInfo.getUrl(), "some-name", null, resourceInfo.getLastModified());
    	// uninstall bundle
    	Arrays.stream(bundleContext.getBundles())
    		.filter(bundle -> bundle.getSymbolicName().equals("jsf-resourcehandler-resourcebundle"))
    		.findFirst().get().uninstall();
    	Thread.sleep(1000); //to fast for tests, resource isn't fully gone yet 
    	
    	try{
            resource.getInputStream();
    	    fail("IOException expected due to missing resource!");
    	}finally {
    	    bundleContext.ungetService(sr);
        }
    }


    /**
     * Fake service-impl for {@link OsgiResourceLocator} because Mockito 2+ is
     * currently not running in pax-exam
     */
    private class OsgiResourceLocatorForTest implements OsgiResourceLocator {

        private ResourceInfo resource;

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
    }
}
