/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.samples.war.http2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

public class PushAwareServlet extends HttpServlet {

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pathInfo = req.getPathInfo();
		resp.setCharacterEncoding(StandardCharsets.UTF_8.toString());
		if (pathInfo == null) {
			resp.setContentType("text/plain");
			resp.getWriter().println("Hello (WAB)");
			resp.getWriter().flush();
		} else {
			switch (pathInfo) {
				case "/index.html":
					resp.setContentType("text/html");
					resp.getWriter().println("<!DOCTYPE html>\n" +
							"<html lang=\"en\">\n" +
							"<head>\n" +
							"    <meta charset=\"utf-8\">\n" +
							"    <title>Pax Web and HTTP/2</title>\n" +
							"    <script src=\"app.js\"></script>\n" +
							"    <link href=\"default.css\" rel=\"stylesheet\">\n" +
							"</head>\n" +
							"<body>\n" +
							"<h1>Hello world of HTTP/2</h1>\n" +
							"</body>\n" +
							"</html>\n");
					PushBuilder builder = req.newPushBuilder();
					if (builder != null) {
						// can be null on Jetty
						builder.path("servlet/default.css").push();
						builder.path("servlet/app.js").push();
					}
					break;
				case "/default.css":
					resp.setContentType("text/css");
					resp.getWriter().println("h1 { color: blue }\nh2 { color: green }\n");
					break;
				case "/app.js":
					resp.setContentType("application/javascript");
					resp.getWriter().println("document.write(\"<h2>Served with HTTP/2 Push</h2>\")\n");
					break;
				default:
					break;
			}
		}
		resp.getWriter().flush();
	}

}
