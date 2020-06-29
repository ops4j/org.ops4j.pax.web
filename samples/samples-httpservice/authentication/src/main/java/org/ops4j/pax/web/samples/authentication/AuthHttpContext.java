/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.authentication;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * Created by IntelliJ IDEA. User: alin.dreghiciu Date: Dec 10, 2007 Time:
 * 3:12:24 PM To change this template use File | Settings | File Templates.
 */
public class AuthHttpContext implements HttpContext {

	public boolean handleSecurity(HttpServletRequest req,
			HttpServletResponse res) throws IOException {

		if (req.getHeader("Authorization") == null) {
			res.addHeader("WWW-Authenticate", "Basic realm=\"Test Realm\"");
			res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
		if (authenticated(req)) {
			return true;
		} else {
			res.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}

	}

	protected boolean authenticated(HttpServletRequest request) {
		request.setAttribute(AUTHENTICATION_TYPE, HttpServletRequest.BASIC_AUTH);

		String authzHeader = request.getHeader("Authorization");
		String usernameAndPassword = new String(Base64.getDecoder().decode(authzHeader.substring(6).getBytes()));

		int userNameIndex = usernameAndPassword.indexOf(":");
		String username = usernameAndPassword.substring(0, userNameIndex);
		String password = usernameAndPassword.substring(userNameIndex + 1);

		// Here I will do lame hard coded credential check. HIGHLY NOT RECOMMENDED!
		boolean success = ((username.equals("admin") && password.equals("admin")));
		if (success) {
			request.setAttribute(REMOTE_USER, "admin");
		}
		return success;
	}

	public URL getResource(String s) {
		return null;
	}

	public String getMimeType(String s) {
		return null;
	}

}
