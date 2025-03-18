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
package org.ops4j.pax.web.samples.jersey;

import java.net.URL;
import java.util.logging.Logger;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.ops4j.pax.web.service.http.HttpContext;
import org.osgi.framework.Bundle;

class CustomHttpContext implements HttpContext {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private final Bundle bundle;

	CustomHttpContext(Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public URL getResource(String name) {
		return bundle.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		StringBuilder info = new StringBuilder();
		info.append("\n  Request=").append(request.getClass().getName()).append("\n  Cookies:\n");
		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				info.append("    ").append(cookie.getName()).append("=").append(cookie.getValue()).append("\n");
			}
		} else {
			info.append("    no cookie found");
		}
		info.append("\n  Session=").append(session).append("\n");
		logger.info(info.toString());

		boolean success = session != null;
		logger.info("#### Test " + (success ? "successful!" : "failed!"));

		return success || request.getCookies() == null;
	}

}
