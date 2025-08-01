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
package org.ops4j.pax.web.samples.whiteboard.ds;

import java.io.IOException;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

@Component(
		service = { Servlet.class, WhiteboardServletWithContext.class }, // WhiteboardServletWithContext only for testing
		scope = ServiceScope.PROTOTYPE,
		property = {
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/servlet",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME + "=ServletWithContext",
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(osgi.http.whiteboard.context.name=CustomContext)"
		}
)
public class WhiteboardServletWithContext extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello from ServletWithContext");
	}

}
