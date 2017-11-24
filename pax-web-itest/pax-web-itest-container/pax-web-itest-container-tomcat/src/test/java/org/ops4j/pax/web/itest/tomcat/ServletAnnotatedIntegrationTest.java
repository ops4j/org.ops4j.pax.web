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
package org.ops4j.pax.web.itest.tomcat;

import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.support.AnnotatedMultipartTestServlet;
import org.ops4j.pax.web.itest.base.support.AnnotatedTestServlet;
import org.ops4j.pax.web.itest.common.AbstractServletAnnotatedIntegrationTest;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.osgi.framework.Constants;


/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
@RunWith(PaxExam.class)
public class ServletAnnotatedIntegrationTest extends AbstractServletAnnotatedIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return combine(configureTomcat(),
				streamBundle(bundle()
						.add(AnnotatedTestServlet.class)
						.add(AnnotatedMultipartTestServlet.class)
						.set(Constants.BUNDLE_SYMBOLICNAME, "AnnotatedServletTest")
						.set(WebContainerConstants.CONTEXT_PATH_KEY, "/annotatedTest")
						.set(Constants.IMPORT_PACKAGE, "javax.servlet")
						.set(Constants.DYNAMICIMPORT_PACKAGE, "*")
						.build()));
	}
}
