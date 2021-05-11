/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.itest.server.support.war.cb3.scis;

import java.io.IOException;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SCIFromContainerBundle3 implements ServletContainerInitializer {

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		ctx.addServlet("dynamic1", new HttpServlet() {
			private String msg;

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				this.msg = "World!";
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.getWriter().print(msg);
			}
		}).addMapping("/dynamic1");
		ctx.addFilter("dynamic-filter1", new Filter() {
			private String msg;

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				Filter.super.init(filterConfig);
				this.msg = "Hello ";
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				response.setContentLength(12);
				response.getWriter().print(msg);
				chain.doFilter(request, response);
			}
		}).addMappingForServletNames(null, false, "dynamic1");
	}

}
