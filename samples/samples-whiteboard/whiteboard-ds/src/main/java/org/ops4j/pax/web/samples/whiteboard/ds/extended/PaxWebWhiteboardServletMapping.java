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
package org.ops4j.pax.web.samples.whiteboard.ds.extended;

import java.io.IOException;
import java.util.Map;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.service.component.annotations.Component;

@Component
public class PaxWebWhiteboardServletMapping implements ServletMapping {

	private static final Servlet SERVLET = new HttpServlet() {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().println("Hello from " + PaxWebWhiteboardServletMapping.class.getName());
		}
	};

	@Override
	public Class<? extends Servlet> getServletClass() {
		return null;
	}

	@Override
	public String[] getErrorPages() {
		return new String[0];
	}

	@Override
	public Map<String, String> getInitParameters() {
		return null;
	}

	@Override
	public String getContextSelectFilter() {
		return null;
	}

	@Override
	public String getContextId() {
		return PaxWebWhiteboardHttpContext.CONTEXT_ID;
	}

	@Override
	public Servlet getServlet() {
		return SERVLET;
	}

	@Override
	public String getServletName() {
		return PaxWebWhiteboardServletMapping.class.getName();
	}

	@Override
	public String getAlias() {
		return "/servlet-mapping";
	}

	@Override
	public String[] getUrlPatterns() {
		return new String[0];
	}

	@Override
	public Integer getLoadOnStartup() {
		return 1;
	}

	@Override
	public Boolean getAsyncSupported() {
		return false;
	}

	@Override
	public MultipartConfigElement getMultipartConfig() {
		return null;
	}

}
