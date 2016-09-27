package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.AnnotatedMultipartTestServlet;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.util.HashMap;
import java.util.Map;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;


/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class ServletAnnotatedIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(configureUndertow(),
				streamBundle(bundle()
		                .add(AnnotatedTestServlet.class)
		                .add(AnnotatedMultipartTestServlet.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
		                .set(WebContainerConstants.CONTEXT_PATH_KEY, "/annotatedTest")
		                .set(Constants.IMPORT_PACKAGE, "javax.servlet")
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()));
	}

	@Before
	public void setUp() throws 	Exception {
		waitForServer("http://127.0.0.1:8181/");

		initServletListener("test");
		
		waitForServletListener();
		
	}

	@After
	public void tearDown() throws BundleException {
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : bundleContext.getBundles()) {
			System.out.println("Bundle " + b.getBundleId() + " : "
					+ b.getSymbolicName());
		}

	}

	@Test
	public void testBundle1() throws Exception {

		testClient.testWebPath("http://127.0.0.1:8181/annotatedTest/test", "TEST OK");
		
	}
	
	@Test
	@Ignore("Needs investigation")
	public void testMultipart() throws Exception {
		
		Map<String, Object> multiPartContent = new HashMap<String, Object>();
		multiPartContent.put("exampleFile", "file.part");
		testClient.testPostMultipart("http://127.0.0.1:8181/annotatedTest/multipartest", multiPartContent , "Part of file: exampleFile", 200);
	}
}
