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

import java.io.FileNotFoundException;
import java.util.Hashtable;
import javax.servlet.Servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.itest.server.support.ErrorServlet;
import org.ops4j.pax.web.itest.server.support.ProblemServlet;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.service.whiteboard.ServletMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardErrorPagesTest extends MultiContainerTestSupport {

	@Test
	public void threeAndAHalfWaysToRegisterErrorPages() throws Exception {
		Bundle sample1 = mockBundle("sample1");
		BundleWiring bundleWiring = mock(BundleWiring.class);
		when(sample1.adapt(BundleWiring.class)).thenReturn(bundleWiring);
		when(bundleWiring.getClassLoader()).thenReturn(this.getClass().getClassLoader());

		// 0. We need a servlet that can actually cause problems

		ServiceReference<Servlet> problemServletRef = mockServletReference(sample1, "badServlet",
				ProblemServlet::new, 0L, 0, "/yikes");
		ServletModel problemServletModel = getServletCustomizer().addingService(problemServletRef);

		String th = FileNotFoundException.class.getName();
		String emsg = runtime == Runtime.JETTY ? th + ": x" : "x";

		// 1. Whiteboard registration of a Servlet with osgi.http.whiteboard.servlet.errorPage
		//    service property and _without_ osgi.http.whiteboard.servlet.pattern property

		Hashtable<String, Object> props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] { "461", th });
		ServiceReference<Servlet> servletRef = mockReference(sample1, Servlet.class, props, ErrorServlet::new, 0L, 0);
		ServletModel model = getServletCustomizer().addingService(servletRef);

		// ("null" because pathInfo is null, because generated mapping exact URI)
		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("null: [badServlet][null][null][x461][/yikes][461]"));
		assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), endsWith("null: [badServlet][" + th + "][" + th + "][" + emsg + "][/yikes][500]"));
		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		getServletCustomizer().removedService(servletRef, model);

		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<p><b>Message</b> x461</p><p>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<p><b>Message</b> x</p><p>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<body>x461</body>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<div class=\"value\">ex=java.io.FileNotFoundException"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		// 1+1/2. Whiteboard registration of a Servlet with osgi.http.whiteboard.servlet.errorPage
		//        service property and _with_ osgi.http.whiteboard.servlet.pattern property - normal servlet with
		//        error servlet functionality

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE, new String[] { "461", th });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/e1/*", "/e2/*" });
		servletRef = mockReference(sample1, Servlet.class, props, ErrorServlet::new, 0L, 0);
		model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("[badServlet][null][null][x461][/yikes][461]"));
		assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), endsWith("[badServlet][" + th + "][" + th + "][" + emsg + "][/yikes][500]"));
		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		getServletCustomizer().removedService(servletRef, model);

		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<p><b>Message</b> x461</p><p>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<p><b>Message</b> x</p><p>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<body>x461</body>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<div class=\"value\">ex=java.io.FileNotFoundException"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		// 2. Pax Web registration of ErrorPageMapping - needs to follow actual servlet registration

		DefaultServletMapping sm = new DefaultServletMapping();
		sm.setServlet(new ErrorServlet());
		sm.setUrlPatterns(new String[] { "/e" });
		ServiceReference<ServletMapping> servletMappingRef = mockReference(sample1, ServletMapping.class,
				null, () -> sm);
		ServletModel model2 = getServletMappingCustomizer().addingService(servletMappingRef);

		DefaultErrorPageMapping epm = new DefaultErrorPageMapping();
		epm.setErrors(new String[] { "461", th });
		epm.setLocation("/e");
		ServiceReference<ErrorPageMapping> epMappingRef = mockReference(sample1, ErrorPageMapping.class,
				null, () -> epm);
		ErrorPageModel epmModel = getErrorPageMappingCustomizer().addingService(epMappingRef);

		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("[badServlet][null][null][x461][/yikes][461]"));
		assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), endsWith("[badServlet][" + th + "][" + th + "][" + emsg + "][/yikes][500]"));
		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		getServletMappingCustomizer().removedService(servletMappingRef, model2);
		getErrorPageMappingCustomizer().removedService(epMappingRef, epmModel);

		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<p><b>Message</b> x461</p><p>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<p><b>Message</b> x</p><p>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<body>x461</body>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<div class=\"value\">ex=java.io.FileNotFoundException"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		// 3. Pax Web registration of ServletMapping with associated error pages

		DefaultServletMapping esm = new DefaultServletMapping();
		esm.setServlet(new ErrorServlet());
		esm.setUrlPatterns(new String[] { "/e" });
		esm.setErrorPages(new String[] { "461", th });
		ServiceReference<ServletMapping> eservletMappingRef = mockReference(sample1, ServletMapping.class,
				null, () -> esm);
		ServletModel emodel = getServletMappingCustomizer().addingService(eservletMappingRef);

		assertThat(httpGET(port, "/yikes?result=461&msg=x461"), endsWith("[badServlet][null][null][x461][/yikes][461]"));
		assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), endsWith("[badServlet][" + th + "][" + th + "][" + emsg + "][/yikes][500]"));
		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		getServletMappingCustomizer().removedService(eservletMappingRef, emodel);

		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/yikes</td>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/xikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<p><b>Message</b> x461</p><p>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<p><b>Message</b> x</p><p>"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<h1>HTTP Status 404 – Not Found</h1>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/yikes?result=461&msg=x461"), containsString("<body>x461</body>"));
			assertThat(httpGET(port, "/yikes?ex=" + th + "&msg=x"), containsString("<div class=\"value\">ex=java.io.FileNotFoundException"));
			assertThat(httpGET(port, "/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/xikes?result=461&msg=x461"), containsString("<body>Not Found</body>"));
		}

		getServletCustomizer().removedService(problemServletRef, problemServletModel);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
