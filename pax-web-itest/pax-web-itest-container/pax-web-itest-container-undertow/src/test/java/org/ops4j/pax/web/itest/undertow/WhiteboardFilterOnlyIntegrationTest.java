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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.base.support.SimpleOnlyFilter;
import org.osgi.framework.ServiceRegistration;

import javax.servlet.Filter;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class WhiteboardFilterOnlyIntegrationTest extends ITestBase {

	@Configuration
	public static Option[] configure() {
		return combine(
				configureUndertow());/*,
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

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/testFilter/testme");

//		testClient.testWebPath("http://127.0.0.1:8181/testFilter/testme",
//				"Hello Whiteboard Filter");

		filter.unregister();

	}

}
