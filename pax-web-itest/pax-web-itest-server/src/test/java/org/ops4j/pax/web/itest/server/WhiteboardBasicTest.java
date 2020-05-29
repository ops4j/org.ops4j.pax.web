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
package org.ops4j.pax.web.itest.server;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardBasicTest extends MultiContainerTestSupport {

	@Test
	public void justServletWithDefaultContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void justServletAndFilterWithDefaultContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		ServletModel sm = getServletCustomizer().addingService(servletRef);
		ServiceReference<Filter> filterRef = mockFilterReference(sample1, "filter1",
				() -> new Utils.MyIdFilter("1"), 0L, 0, "/s");
		FilterModel fm = getFilterCustomizer().addingService(filterRef);
		assertThat(httpGET(port, "/s"), endsWith(">F(1)S(1)<F(1)"));

		getFilterCustomizer().removedService(filterRef, fm);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, sm);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void servletInitWithSingleServletContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		final Map<ServletContext, Boolean> contexts1 = new IdentityHashMap<>();
		final Map<ServletContext, Boolean> contexts2 = new IdentityHashMap<>();
		Servlet servlet = new AnnotatedMyIdServlet(contexts1, contexts2);

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1", () -> servlet, 0L, 0);
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));

		contexts1.putAll(contexts2);
		assertThat("However and whenever (init() or service()) we obtain ServletContext it must be the same",
				contexts1.size(), equalTo(1));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void servletAndFilterInitWithSingleServletContext() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		when(sample1.toString()).thenReturn("sample1");
		Bundle sample2 = mockBundle("sample1");
		when(sample2.toString()).thenReturn("sample2");

		final Map<ServletContext, Boolean> contexts1 = new IdentityHashMap<>();
		final Map<ServletContext, Boolean> contexts2 = new IdentityHashMap<>();

		Servlet servlet = new AnnotatedMyIdServlet(contexts1, contexts2);
		Filter filter = new AnnotatedMyIdFilter(contexts1, contexts2);

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1", () -> servlet, 0L, 0);
		ServletModel smodel = getServletCustomizer().addingService(servletRef);
		ServiceReference<Filter> filterRef = mockFilterReference(sample2, "filter1", () -> filter, 0L, 0);
		FilterModel fmodel = getFilterCustomizer().addingService(filterRef);

		assertThat(httpGET(port, "/s"), endsWith(">F(1)S(1)<F(1)"));

		assertThat("Filter.init() and Servlet.init() should get different context",
				contexts1.size(), equalTo(2));
		assertThat("When processing request, there should be only one, Servlet-specific ServletContext",
				contexts2.size(), equalTo(1));
	}

	@WebServlet(loadOnStartup = 1, urlPatterns = "/s")
	private static class AnnotatedMyIdServlet extends Utils.MyIdServlet {

		private final Map<ServletContext, Boolean> contexts1;
		private final Map<ServletContext, Boolean> contexts2;

		public AnnotatedMyIdServlet(Map<ServletContext, Boolean> contexts1, Map<ServletContext, Boolean> contexts2) {
			super("1");
			this.contexts1 = contexts1;
			this.contexts2 = contexts2;
		}

		@Override
		public void init() {
			contexts1.put(getServletContext(), true);
			contexts1.put(getServletConfig().getServletContext(), true);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			super.doGet(req, resp);

			// these two use servlet-scoped OsgiScopedServletContext - but it should be the same context
			// as the one used during request processing
			contexts2.put(getServletContext(), true);
			contexts2.put(getServletConfig().getServletContext(), true);

			contexts2.put(req.getServletContext(), true);
			contexts2.put(req.getSession().getServletContext(), true);
		}
	}

	@WebFilter(urlPatterns = "/s")
	private static class AnnotatedMyIdFilter extends Utils.MyIdFilter {

		private final Map<ServletContext, Boolean> contexts1;
		private final Map<ServletContext, Boolean> contexts2;

		public AnnotatedMyIdFilter(Map<ServletContext, Boolean> contexts1, Map<ServletContext, Boolean> contexts2) {
			super("1");
			this.contexts1 = contexts1;
			this.contexts2 = contexts2;
		}

		@Override
		public void init() {
			contexts1.put(getServletContext(), true);
			contexts1.put(getFilterConfig().getServletContext(), true);
		}

		@Override
		protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws ServletException, IOException {
			super.doFilter(req, resp, chain);

			// these two use filter-scoped OsgiScopedServletContext, so it should be different than the one
			// used during request processing, where target servlet's specific OsgiScopedServletContext is used
//			contexts2.put(getServletContext(), true);
//			contexts2.put(getFilterConfig().getServletContext(), true);

			// here, request's ServletContext is used, so should be the one from Servlet
			contexts2.put(req.getServletContext(), true);
			contexts2.put(req.getSession().getServletContext(), true);
		}
	}

}
