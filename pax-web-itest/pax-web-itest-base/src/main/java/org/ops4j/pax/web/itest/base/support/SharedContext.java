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
 package org.ops4j.pax.web.itest.base.support;

import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

public class SharedContext implements HttpContext {

	private static final Logger LOG = LoggerFactory
			.getLogger(SharedContext.class);

	public boolean handleSecurity(final HttpServletRequest request,
			final HttpServletResponse response) throws IOException {
		LOG.info("Forbiden access!");
		return false;
	}

	public URL getResource(final String name) {
		throw new IllegalStateException(
				"This method should not be possible to be called as the access is denied");
	}

	public String getMimeType(String s) {
		throw new IllegalStateException(
				"This method should not be possible to be called as the access is denied");
	}
}
