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
package org.ops4j.pax.web.itest.karaf;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Ignore;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

/**
 * @author Achim Nierbeck
 */
@Ignore("https://github.com/jetty/jetty.project/issues/12926")
public class PostLimitJettyKarafTest extends PostLimitBaseKarafTest {

	@Configuration
	public Option[] configuration() {
		return jettyConfig();
	}

	@Override
	protected int getPostSizeExceededHttpResponseCode() {
		return HttpServletResponse.SC_BAD_REQUEST;
	}

}
