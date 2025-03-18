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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import java.util.logging.Logger;

@Path("/")
public class RootResource {

	private final Logger logger = Logger.getLogger(getClass().getName());

	@Context
	private HttpServletRequest request;

	@GET
	public String createSession() {
		String message;
		if (request == null) {
			logger.severe("request is null!!!!!");
			throw new RuntimeException();
		}
		HttpSession session = request.getSession(false);
		if (session == null) {
			session = request.getSession();
			message = "New session created: " + session.getId();
		} else {
			message = "Session already exists: " + session.getId();
		}
		logger.info(message);
		return htmlPage(message);
	}

	private String htmlPage(String message) {
		return "<html><body>" + message + "<br><br>"
				+ "If this <a href=\"/images/success.png\">link</a> leads to an image the test is successful"
				+ "</body></html>";
	}

}
