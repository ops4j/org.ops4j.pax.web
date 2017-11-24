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

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.common.AbstractJspSelfRegistrationIntegrationTest;

/**
 * The tests contained here will test the usage of the PAX Web Jsp directly with the HttpService, without
 * the need for a full servlet container environment. This is useful when integrating PAX Web JSP into an
 * existing servlet container using an HTTP Bridge service implementation such as the Felix Http bridge
 * service implementation.
 * <p>
 * This test validates the correction for PAXWEB-497 as well as the new functionality from PAXWEB-498.
 *
 * @author Serge Huber
 */
@RunWith(PaxExam.class)
@Ignore("FIXME: This worked before")
public class JspSelfRegistrationIntegrationTest extends AbstractJspSelfRegistrationIntegrationTest {

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}
}
