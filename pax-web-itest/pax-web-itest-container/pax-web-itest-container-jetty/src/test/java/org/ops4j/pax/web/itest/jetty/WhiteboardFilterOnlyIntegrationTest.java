package org.ops4j.pax.web.itest.jetty;

import static org.ops4j.pax.exam.OptionUtils.combine;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.SimpleOnlyFilter;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardFilterOnlyIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(
				configureJetty());/*,
				mavenBundle().groupId("org.ops4j.pax.web.samples")
						.artifactId("whiteboard").version(VersionUtil.getProjectVersion())
						.noStart());*/

	}

	@Test
	@Ignore("PAXWEB-483 - Just registering a filter doesn't work yet, cause no context available")
	public void testWhiteBoardFiltered() throws Exception {
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("urlPatterns", "/testfilter/*");
		SimpleOnlyFilter simpleFilter = new SimpleOnlyFilter();
		ServiceRegistration<Filter> filter = bundleContext.registerService(
				Filter.class, simpleFilter, props);

		testClient.testWebPath("http://127.0.0.1:8181/testFilter/testme",
				"Hello Whiteboard Filter");

		filter.unregister();

	}

}
