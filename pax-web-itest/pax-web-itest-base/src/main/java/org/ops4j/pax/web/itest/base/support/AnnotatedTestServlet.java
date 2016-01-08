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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet (value = "/test", name = "test")
public class AnnotatedTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private boolean initCalled = false;
	private boolean destroyCalled = false;

	@Override
	public void init(ServletConfig config) throws ServletException {
		this.initCalled = true;
		super.init(config);
	}
	
	@Override
	public void destroy() {
		this.destroyCalled = true;
		super.destroy();
	}
	
	public boolean isInitCalled() {
		return initCalled;
	}
	
	public boolean isDestroyCalled() {
		return destroyCalled;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getWriter().write("TEST OK");
	}
}