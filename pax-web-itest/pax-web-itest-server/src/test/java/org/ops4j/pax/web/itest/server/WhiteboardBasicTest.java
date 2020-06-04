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
import java.net.URL;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
		assertThat(contexts1.keySet().iterator().next().getClass(), equalTo(OsgiScopedServletContext.class));

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

		contexts1.keySet().forEach(sc -> assertThat(sc.getClass(), equalTo(OsgiScopedServletContext.class)));
		contexts2.keySet().forEach(sc -> assertThat(sc.getClass(), equalTo(OsgiScopedServletContext.class)));
	}

	@Test
	public void quickReinitialization() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// Filters are a bit problematic. When new filter is registered, it has to be added to server's internal
		// structures in correct order (web.xml order in JavaEE and service rank order in OSGi Whiteboard).
		// If simply new filter is registered, ONLY if this filter should be added last, we can attempt optimized
		// registration. Otherwise, we have to recreate the filter list, which SHOULD lead to destroy() + init()
		// of these filters again.
		// Additionally, in Undertow we can't simply remove existing filters without recreating entire "context"
		// (ServletContext) created in DeploymentManagerImpl.deploy() and put into DeploymentImpl. So even when
		// adding single servlet, we have to recreate everything which usually leads to destroy() + init() for
		// each servlet of the context
		// In Tomcat and Jetty, we can freely and independently operate on servlets and filters and nothing in any
		// specification prevents us from doing so
		//
		//  - In Jetty, reinitialization is done in org.eclipse.jetty.servlet.ServletHandler.updateMappings() called
		//    from org.eclipse.jetty.servlet.ServletHandler.setFilterMappings()

		/*
		 * 2020-06-01 (no destroy() for filters in Jetty, Undertow is less flexible (!) than Jetty and Tomcat)
		 * Jetty:
		 * [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2),               f1.init(), f2.init(),            unreg(f2),                                           f1.init(),            unreg(s1), s1.destroy(),                          unreg(f1)              ]
		 * Tomcat:
		 * [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2), f1.destroy(), f1.init(), f2.init(),            unreg(f2), f1.destroy(), f2.destroy(),               f1.init(),            unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 * Undertow:
		 * [reg(s1), s1.init(), reg(f1), s1.destroy(), f1.init(), reg(f2), f1.destroy(), f1.init(), f2.init(), s1.init(), unreg(f2), s1.destroy(), f1.destroy(), f2.destroy(), f1.init(), s1.init(), unreg(s1), s1.destroy(), f1.destroy(), f1.init(), unreg(f1), f1.destroy()]
		 */

		/*
		 * 2020-06-02 (Jetty filters are destroyed, optimization for filter addition)
		 * Jetty:
		 *  - non optimized: [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2), f1.destroy(), f1.init(), f2.init(),            unreg(f2), f1.destroy(), f2.destroy(),               f1.init(),            unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 *  - optimized:     [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2),                          f2.init(),            unreg(f2), f1.destroy(), f2.destroy(),               f1.init(),            unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 * Tomcat:
		 *  - non optimized: [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2), f1.destroy(), f1.init(), f2.init(),            unreg(f2), f1.destroy(), f2.destroy(),               f1.init(),            unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 *  - no optimization possible
		 * Undertow:
		 *  - non optimized: [reg(s1), s1.init(), reg(f1), s1.destroy(), f1.init(), reg(f2), f1.destroy(), f1.init(), f2.init(), s1.init(), unreg(f2), s1.destroy(), f1.destroy(), f2.destroy(), f1.init(), s1.init(), unreg(s1), s1.destroy(), f1.destroy(), f1.init(), unreg(f1), f1.destroy()]
		 *  - optimized:     [reg(s1), s1.init(), reg(f1),               f1.init(), reg(f2),                          f2.init(),            unreg(f2), s1.destroy(), f1.destroy(), f2.destroy(), f1.init(), s1.init(), unreg(s1), s1.destroy(), f1.destroy(), f1.init(), unreg(f1), f1.destroy()]
		 */

		final Deque<String> log = new LinkedList<>();

		Servlet servlet = new Utils.MyIdServlet("s1") {
			@Override
			public void init() {
				log.add("s1.init()");
			}
			@Override
			public void destroy() {
				log.add("s1.destroy()");
			}
		};
		Filter filter1 = new Utils.MyIdFilter("f1") {
			@Override
			public void init() {
				log.add("f1.init()");
			}
			@Override
			public void destroy() {
				log.add("f1.destroy()");
			}
		};
		Filter filter2 = new Utils.MyIdFilter("f2") {
			@Override
			public void init() {
				log.add("f2.init()");
			}
			@Override
			public void destroy() {
				log.add("f2.destroy()");
			}
		};

		log.add("reg(s1)");
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1", () -> servlet, 0L, 0, "/s");
		ServletModel smodel = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), endsWith("S(s1)"));

		log.add("reg(f1)");
		ServiceReference<Filter> filter1Ref = mockFilterReference(sample1, "filter1", () -> filter1, 0L, 0, "/s");
		FilterModel fmodel1 = getFilterCustomizer().addingService(filter1Ref);
		log.add("reg(f2)");
		ServiceReference<Filter> filter2Ref = mockFilterReference(sample1, "filter2", () -> filter2, 0L, 0, "/s");
		FilterModel fmodel2 = getFilterCustomizer().addingService(filter2Ref);
		assertThat(httpGET(port, "/s"), endsWith(">F(f1)>F(f2)S(s1)<F(f2)<F(f1)"));

		log.add("unreg(f2)");
		getFilterCustomizer().removedService(filter2Ref, fmodel2);
		assertThat(httpGET(port, "/s"), endsWith(">F(f1)S(s1)<F(f1)"));

		log.add("unreg(s1)");
		getServletCustomizer().removedService(servletRef, smodel);
		assertThat(httpGET(port, "/s?terminate=f1"), endsWith(">F(f1)<F(f1)"));

		log.add("unreg(f1)");
		getFilterCustomizer().removedService(filter1Ref, fmodel1);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		System.out.println(log);

		assertThat(log.pop(), equalTo("reg(s1)"));
		assertThat(log.pop(), equalTo("s1.init()"));
		assertThat(log.pop(), equalTo("reg(f1)"));
		assertThat(log.pop(), equalTo("f1.init()"));
		assertThat(log.pop(), equalTo("reg(f2)"));
		if (runtime == Runtime.TOMCAT) {
			// For Tomcat, we can't add new filter to the list (even at the end) without reinitializing existing ones
			assertThat(log.pop(), equalTo("f1.destroy()"));
			assertThat(log.pop(), equalTo("f1.init()"));
		}
		assertThat(log.pop(), equalTo("f2.init()"));
		assertThat(log.pop(), equalTo("unreg(f2)"));
		if (runtime == Runtime.UNDERTOW) {
			// For Undertow, when filter is unregistered, we have to redeploy entire context
			assertThat(log.pop(), equalTo("s1.destroy()"));
		}
		assertThat(log.pop(), equalTo("f1.destroy()"));
		assertThat(log.pop(), equalTo("f2.destroy()"));
		assertThat(log.pop(), equalTo("f1.init()"));
		if (runtime == Runtime.UNDERTOW) {
			// For Undertow, when filter is unregistered, we have to redeploy entire context, so servlet is init()ed
			// again
			assertThat(log.pop(), equalTo("s1.init()"));
		}
		assertThat(log.pop(), equalTo("unreg(s1)"));
		assertThat(log.pop(), equalTo("s1.destroy()"));
		if (runtime == Runtime.UNDERTOW) {
			// For Undertow, when servlet is unregistered, we have to redeploy entire context and filters will be
			// reinitialized
			assertThat(log.pop(), equalTo("f1.destroy()"));
			assertThat(log.pop(), equalTo("f1.init()"));
		}
		assertThat(log.pop(), equalTo("unreg(f1)"));
		assertThat(log.pop(), equalTo("f1.destroy()"));
		assertTrue(log.isEmpty());
	}

	@Test
	public void normalReinitialization() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		/*
		 * Jetty:
		 *  - [reg(f1), f1.init(), reg(s1), s1.init(), reg(f2), f1.destroy(),                          f2.init(), f1.init(), unreg(f2),               f2.destroy(), f1.destroy(),            f1.init(), unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 * Tomcat:
		 *  - [reg(f1), f1.init(), reg(s1), s1.init(), reg(f2), f1.destroy(),                          f1.init(), f2.init(), unreg(f2),               f1.destroy(), f2.destroy(),            f1.init(), unreg(s1), s1.destroy(),                          unreg(f1), f1.destroy()]
		 * Undertow:
		 *  - [reg(f1), f1.init(), reg(s1), s1.init(), reg(f2), f1.destroy(), s1.destroy(), s1.init(), f1.init(), f2.init(), unreg(f2), s1.destroy(), f1.destroy(), f2.destroy(), s1.init(), f1.init(), unreg(s1), s1.destroy(), f1.destroy(), f1.init(), unreg(f1), f1.destroy()]
		 */

		final Deque<String> log = new LinkedList<>();

		// ensure load-on-startup is set
		Servlet servlet = new AnnotatedMyIdServlet("s1") {
			@Override
			public void init() {
				log.add("s1.init()");
			}
			@Override
			public void destroy() {
				log.add("s1.destroy()");
			}
		};
		Filter filter1 = new Utils.MyIdFilter("f1") {
			@Override
			public void init() {
				log.add("f1.init()");
			}
			@Override
			public void destroy() {
				log.add("f1.destroy()");
			}
		};
		Filter filter2 = new Utils.MyIdFilter("f2") {
			@Override
			public void init() {
				log.add("f2.init()");
			}
			@Override
			public void destroy() {
				log.add("f2.destroy()");
			}
		};

		log.add("reg(f1)");
		ServiceReference<Filter> filter1Ref = mockFilterReference(sample1, "filter1", () -> filter1, 0L, 0, "/s");
		FilterModel fmodel1 = getFilterCustomizer().addingService(filter1Ref);
		assertThat(httpGET(port, "/s?terminate=f1"), endsWith(">F(f1)<F(f1)"));

		log.add("reg(s1)");
		// mapping specified at annotation level
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1", () -> servlet, 0L, 0);
		ServletModel smodel = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/s"), endsWith(">F(f1)S(s1)<F(f1)"));

		log.add("reg(f2)");
		ServiceReference<Filter> filter2Ref = mockFilterReference(sample1, "filter2", () -> filter2, 0L, 1, "/s");
		FilterModel fmodel2 = getFilterCustomizer().addingService(filter2Ref);
		assertThat(httpGET(port, "/s"), endsWith(">F(f2)>F(f1)S(s1)<F(f1)<F(f2)"));

		log.add("unreg(f2)");
		getFilterCustomizer().removedService(filter2Ref, fmodel2);
		assertThat(httpGET(port, "/s"), endsWith(">F(f1)S(s1)<F(f1)"));

		log.add("unreg(s1)");
		getServletCustomizer().removedService(servletRef, smodel);
		assertThat(httpGET(port, "/s?terminate=f1"), endsWith(">F(f1)<F(f1)"));

		log.add("unreg(f1)");
		getFilterCustomizer().removedService(filter1Ref, fmodel1);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));

		System.out.println(log);

		assertThat(log.pop(), equalTo("reg(f1)"));
		assertThat(log.pop(), equalTo("f1.init()"));
		assertThat(log.pop(), equalTo("reg(s1)"));
		assertThat(log.pop(), equalTo("s1.init()"));
		assertThat(log.pop(), equalTo("reg(f2)"));
		assertThat("f2 registered with higher rank, so f1 is destroyed", log.pop(), equalTo("f1.destroy()"));
		if (runtime == Runtime.UNDERTOW) {
			assertThat(log.pop(), equalTo("s1.destroy()"));
			assertThat(log.pop(), equalTo("s1.init()"));
		}
		if (runtime == Runtime.JETTY) {
			// Jetty starts filters in order of mapping
			assertThat(log.pop(), equalTo("f2.init()"));
			assertThat(log.pop(), equalTo("f1.init()"));
		} else {
			// Tomcat and Undertow start filters in order of registration
			assertThat(log.pop(), equalTo("f1.init()"));
			assertThat(log.pop(), equalTo("f2.init()"));
		}
		assertThat(log.pop(), equalTo("unreg(f2)"));
		if (runtime == Runtime.UNDERTOW) {
			assertThat(log.pop(), equalTo("s1.destroy()"));
		}
		if (runtime == Runtime.JETTY) {
			// Jetty stops filters in order of mapping
			assertThat(log.pop(), equalTo("f2.destroy()"));
			assertThat(log.pop(), equalTo("f1.destroy()"));
		} else {
			// Tomcat and Undertow stop filters in order of registration
			assertThat(log.pop(), equalTo("f1.destroy()"));
			assertThat(log.pop(), equalTo("f2.destroy()"));
		}
		if (runtime == Runtime.UNDERTOW) {
			assertThat(log.pop(), equalTo("s1.init()"));
		}
		assertThat(log.pop(), equalTo("f1.init()"));
		assertThat(log.pop(), equalTo("unreg(s1)"));
		assertThat(log.pop(), equalTo("s1.destroy()"));
		if (runtime == Runtime.UNDERTOW) {
			assertThat(log.pop(), equalTo("f1.destroy()"));
			assertThat(log.pop(), equalTo("f1.init()"));
		}
		assertThat(log.pop(), equalTo("unreg(f1)"));
		assertThat(log.pop(), equalTo("f1.destroy()"));
		assertTrue(log.isEmpty());
	}

	@Test
	public void bundleResourceAccessInFiltersServletPipeline() throws Exception {
		Bundle sampleS1 = mockBundle("sampleS1");
		Bundle sampleF1 = mockBundle("sampleF1");
		Bundle sampleF2 = mockBundle("sampleF2");

		when(sampleS1.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///s1/" + inv.getArgument(0, String.class)));
		when(sampleF1.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///f1/" + inv.getArgument(0, String.class)));
		when(sampleF2.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///f2/" + inv.getArgument(0, String.class)));

		Servlet servlet = new Utils.MyIdServlet("s1") {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				if (req.getParameter("req").equals("true")) {
					resp.getWriter().print(" " + req.getServletContext().getResource("s1").toString());
				} else {
					resp.getWriter().print(" " + getServletContext().getResource("s1").toString());
				}
			}
		};
		Filter filter1 = new Utils.MyIdFilter("f1") {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				if (request.getParameter("req").equals("true")) {
					response.getWriter().print(" " + request.getServletContext().getResource("f1").toString());
				} else {
					response.getWriter().print(" " + getServletContext().getResource("f1").toString());
				}
				chain.doFilter(request, response);
			}
		};
		Filter filter2 = new Utils.MyIdFilter("f2") {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				if (request.getParameter("req").equals("true")) {
					response.getWriter().print(" " + request.getServletContext().getResource("f2").toString());
				} else {
					response.getWriter().print(" " + getServletContext().getResource("f2").toString());
				}
				chain.doFilter(request, response);
			}
		};

		ServiceReference<Servlet> servletRef = mockServletReference(sampleS1, "servlet1", () -> servlet, 0L, 0, "/s");
		ServletModel smodel = getServletCustomizer().addingService(servletRef);
		ServiceReference<Filter> filter1Ref = mockFilterReference(sampleF1, "filter1", () -> filter1, 0L, 0, "/s");
		FilterModel fmodel1 = getFilterCustomizer().addingService(filter1Ref);
		ServiceReference<Filter> filter2Ref = mockFilterReference(sampleF2, "filter2", () -> filter2, 0L, 0, "/s");
		FilterModel fmodel2 = getFilterCustomizer().addingService(filter2Ref);

		// Even if single ServletContextHelper is used, these are actually bundle scoped instances, so related bundle
		// is called to get the resource (entry)

		// for ServletContext obtained from the request, there's only one ServletContext
		assertThat(httpGET(port, "/s?req=true"), endsWith(new URL("file:///s1/f1").toString() + " "
				+ new URL("file:///s1/f2").toString() + " "
				+ new URL("file:///s1/s1").toString()));

		// for ServletContext obtained from the servlet/filter's config, we have separate contexts
		assertThat(httpGET(port, "/s?req=false"), endsWith(new URL("file:///f1/f1").toString() + " "
				+ new URL("file:///f2/f2").toString() + " "
				+ new URL("file:///s1/s1").toString()));
	}

	@Test
	public void bundleResourceAccessInFiltersOnlyPipeline() throws Exception {
		Bundle sampleF1 = mockBundle("sampleF1");
		Bundle sampleF2 = mockBundle("sampleF2");

		when(whiteboardBundle.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///wb/" + inv.getArgument(0, String.class)));
		when(sampleF1.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///f1/" + inv.getArgument(0, String.class)));
		when(sampleF2.getEntry(anyString()))
				.thenAnswer(inv -> new URL("file:///f2/" + inv.getArgument(0, String.class)));

		Filter filter1 = new Utils.MyIdFilter("f1") {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				if (request.getParameter("req").equals("true")) {
					response.getWriter().print(" " + request.getServletContext().getResource("f1").toString());
				} else {
					response.getWriter().print(" " + getServletContext().getResource("f1").toString());
				}
				if (!id.equals(request.getParameter("terminate"))) {
					chain.doFilter(request, response);
				}
			}
		};
		Filter filter2 = new Utils.MyIdFilter("f2") {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				if (request.getParameter("req").equals("true")) {
					response.getWriter().print(" " + request.getServletContext().getResource("f2").toString());
				} else {
					response.getWriter().print(" " + getServletContext().getResource("f2").toString());
				}
				if (!id.equals(request.getParameter("terminate"))) {
					chain.doFilter(request, response);
				}
			}
		};

		ServiceReference<Filter> filter1Ref = mockFilterReference(sampleF1, "filter1", () -> filter1, 0L, 0, "/s");
		FilterModel fmodel1 = getFilterCustomizer().addingService(filter1Ref);
		ServiceReference<Filter> filter2Ref = mockFilterReference(sampleF2, "filter2", () -> filter2, 0L, 0, "/s");
		FilterModel fmodel2 = getFilterCustomizer().addingService(filter2Ref);

		// Even if single ServletContextHelper is used, these are actually bundle scoped instances, so related bundle
		// is called to get the resource (entry)

		// for ServletContext obtained from the request, there's only one ServletContext - representing
		// ServletContextHelper bundle-scoped to the bundle for which ServletContextHelper itself was registered
		assertThat(httpGET(port, "/s?req=true&terminate=f2"), endsWith(new URL("file:///wb/f1").toString() + " "
				+ new URL("file:///wb/f2").toString()));

		// for ServletContext obtained from the servlet/filter's config, we have separate contexts
		assertThat(httpGET(port, "/s?req=false&terminate=f2"), endsWith(new URL("file:///f1/f1").toString() + " "
				+ new URL("file:///f2/f2").toString()));
	}

	@WebServlet(loadOnStartup = 1, urlPatterns = "/s")
	private static class AnnotatedMyIdServlet extends Utils.MyIdServlet {

		private Map<ServletContext, Boolean> contexts1;
		private Map<ServletContext, Boolean> contexts2;

		public AnnotatedMyIdServlet(String id) {
			super(id);
		}

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

			if (contexts2 != null) {
				// these two use servlet-scoped OsgiScopedServletContext - but it should be the same context
				// as the one used during request processing
				contexts2.put(getServletContext(), true);
				contexts2.put(getServletConfig().getServletContext(), true);

				contexts2.put(req.getServletContext(), true);
				contexts2.put(req.getSession().getServletContext(), true);
			}
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
