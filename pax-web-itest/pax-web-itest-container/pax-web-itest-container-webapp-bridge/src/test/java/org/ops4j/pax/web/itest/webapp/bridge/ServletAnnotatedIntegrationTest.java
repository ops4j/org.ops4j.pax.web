package org.ops4j.pax.web.itest.webapp.bridge;

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

import java.util.Dictionary;
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
@Ignore("PAXWEB-974")
public class ServletAnnotatedIntegrationTest extends ITestBase {

	@Configuration
	public Option[] configure() {
		System.out.println("Configuring Test Bridge");
		return combine(configureBridge(),
				streamBundle(bundle()
		                .add(AnnotatedTestServlet.class)
		                .add(AnnotatedMultipartTestServlet.class)
		                .set(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
		                .set(WebContainerConstants.CONTEXT_PATH_KEY, "/annotatedTest")
		                .set(Constants.IMPORT_PACKAGE, "javax.servlet")
		                .set(Constants.DYNAMICIMPORT_PACKAGE, "*")
		                .build()));
	}

	/**
	 * You will get a list of bundles installed by default plus your testcase,
	 * wrapped into a bundle called pax-exam-probe
	 */
	@Test
	public void listBundles() {
		for (Bundle b : getBundleContext().getBundles()) {
			Dictionary<String,String> headers = b.getHeaders();

			String ctxtPath = (String) headers.get(WEB_CONTEXT_PATH);
			if (ctxtPath != null) {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " : " + ctxtPath + " ("+b.getState()+")");
			} else {
				System.out.println("Bundle " + b.getBundleId() + " : "
						+ b.getSymbolicName() + " ("+b.getState()+")");
			}
		}
	}

	@Test
	public void testBundle1() throws Exception {

		testClient.testWebPath("http://localhost:9080/Pax-Exam-Probe/annotatedTest/test", "TEST OK");
		
	}
	
	@Test
	public void testMultipart() throws Exception {
		
		Map<String, Object> multiPartContent = new HashMap<String, Object>();
		multiPartContent.put("exampleFile", "file.part");
		testClient.testPostMultipart("http://localhost:9080/Pax-Exam-Probe/annotatedTest/multipartest", multiPartContent , "Part of file: exampleFile", 200);
	}
}
