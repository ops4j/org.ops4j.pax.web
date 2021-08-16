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
package org.ops4j.pax.web.itest.server.whiteboard;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.FilterMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.Preprocessor;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardFiltersTest extends MultiContainerTestSupport {

	@Test
	public void twoWaysToRegisterFilter() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// 1. Whiteboard registration as javax.servlet.Filter OSGi service

		ServiceReference<Filter> filterRef = mockFilterReference(sample1, "filter1",
				() -> new Utils.MyIdFilter("1"), 0L, 0, "/s");
		FilterModel model = getFilterCustomizer().addingService(filterRef);
		assertThat(httpGET(port, "/s?terminate=1"), endsWith(">F(1)<F(1)"));

		getFilterCustomizer().removedService(filterRef, model);
		assertThat(httpGET(port, "/s?terminate=1"), startsWith("HTTP/1.1 404"));

		// 2. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.FilterMapping
		//    OSGi service

		DefaultFilterMapping fm = new DefaultFilterMapping();
		fm.setFilter(new Utils.MyIdFilter("2"));
		fm.setUrlPatterns(new String[] { "/t" });
		ServiceReference<FilterMapping> filterMappingRef = mockReference(sample1, FilterMapping.class,
				null, () -> fm);
		FilterModel model2 = getFilterMappingCustomizer().addingService(filterMappingRef);
		assertThat(httpGET(port, "/t?terminate=2"), endsWith(">F(2)<F(2)"));

		getFilterMappingCustomizer().removedService(filterMappingRef, model2);
		assertThat(httpGET(port, "/t?terminate=2"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void oneServletWithTwoContextsAndAPreprocessor() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper1 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "1".equals(request.getParameter("token"));
			}
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper1, 0L, 0, "/c"));

		ServletContextHelper helper2 = new ServletContextHelper() {
			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				return "2".equals(request.getParameter("token"));
			}
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d1",
				() -> helper2, 0L, 0, "/d"));

		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		when(servletRef.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1))");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		final List<String> events = new LinkedList<>();

		Preprocessor preprocessor = new Preprocessor() {
			private FilterConfig config = null;

			@Override
			public void init(FilterConfig filterConfig) {
				config = filterConfig;
				events.add("INIT " + filterConfig.getServletContext().getContextPath());
			}

			@Override
			public void destroy() {
				events.add("DESTROY " + config.getServletContext().getContextPath());
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				events.add(((HttpServletRequest) request).getRequestURI());
				chain.doFilter(request, response);
			}
		};
		ServiceReference<?> ppRef = mockPreprocessorReference(sample1, "p1",
				() -> preprocessor, 0L, 0);
		FilterModel fmodel = getFilterCustomizer().addingService((ServiceReference<Filter>) ppRef);

		assertThat(httpGET(port, "/"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c/s?token=1"), endsWith("S(1)"));
		assertThat(httpGET(port, "/d/s?token=2"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c/s?token=2"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/d/s?token=1"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/c/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/d/s"), startsWith("HTTP/1.1 403"));
		assertThat(httpGET(port, "/e/s"), startsWith("HTTP/1.1 404"));

		getFilterCustomizer().removedService((ServiceReference<Filter>) ppRef, fmodel);

		assertThat(events.size(), equalTo(15));
		assertThat(events.get(0), equalTo("INIT /c"));
		assertThat(events.get(1), equalTo("INIT /d"));
		assertThat(events.get(2), equalTo("INIT "));
		assertThat(events.get(3), equalTo("/"));
		assertThat(events.get(4), equalTo("/s"));
		assertThat(events.get(5), equalTo("/c/s"));
		assertThat(events.get(6), equalTo("/d/s"));
		assertThat(events.get(7), equalTo("/c/s"));
		assertThat(events.get(8), equalTo("/d/s"));
		assertThat(events.get(9), equalTo("/c/s"));
		assertThat(events.get(10), equalTo("/d/s"));
		assertThat(events.get(11), equalTo("/e/s"));
		// there's a singleton storing a FilterConfig in init() called 3x - so we can't have nice DESTROY events here
		assertThat(events.get(12), equalTo("DESTROY "));
		assertThat(events.get(13), equalTo("DESTROY "));
		assertThat(events.get(14), equalTo("DESTROY "));
	}

}
