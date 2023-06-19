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

import java.util.Hashtable;
import jakarta.servlet.Servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

/**
 * This is very complex test that checks how existing servlets, filters, listeners,
 * {@link jakarta.servlet.ServletContainerInitializer SCIs} behave when the highest-ranked
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext} changes for given physical context
 * (identified by context path) or when contexts come and go during the lifetime of servlet registration.
 */
@RunWith(Parameterized.class)
public class WhiteboardDynamicContextsTest extends MultiContainerTestSupport {

	// CP - context path
	// SC - jakarta.servlet.ServletContext
	// SCH - ServletContextHelper (CMPN 140)
	// HC - HttpContext (CMPN 102)
	// WCC - WebContainerContext (extension of HC)
	// OCM - OsgiContextModel - mapped 1:1 with WCC/SCH and mapped N:1 with SC/CP
	// OSC - OsgiServletContext - mapped 1:1 with OCM, but at ServerController level and mapped N:1 with SC/CP

	@Test
	public void schAndServlet() throws Exception {
		Bundle b = mockBundle("sample1");

		ServletContextHelper ctx = new ServletContextHelper() {
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		getServletContextHelperCustomizer()
				.addingService(mockReference(b, ServletContextHelper.class, properties, () -> ctx, 0L, 0));

		ServiceReference<Servlet> servletRef
				= mockServletReference(b, "s1", () -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		mockContextSelectProperty(servletRef, "c1");
		ServletModel model = getServletCustomizer().addingService(servletRef);
		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(b);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(b));
		assertTrue(serviceModelInternals.isEmpty());
	}

	/**
	 * This test seems silly when reading line by line, but such situation actually may occur in SCR environment,
	 * even with proper {@link org.osgi.service.component.annotations.Reference}s to implement kind of ordering
	 * between a {@link ServletContextHelper} and a {@link Servlet}.
	 * @throws Exception
	 */
	@Test
	public void servletAndSch() throws Exception {
		Bundle b = mockBundle("sample1");

		ServiceReference<Servlet> servletRef
				= mockServletReference(b, "s1", () -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		mockContextSelectProperty(servletRef, "c1");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));

		// only now we're registering ServletContextHelper pointed to by the above servlet. This will actually
		// (re)register the above servlet which becomes available
		ServletContextHelper ctx = new ServletContextHelper() {
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		getServletContextHelperCustomizer()
				.addingService(mockReference(b, ServletContextHelper.class, properties, () -> ctx, 0L, 0));

		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		getServletCustomizer().removedService(servletRef, model);
		assertThat(httpGET(port, "/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void changeSchAfterServletRegistration() throws Exception {
		Bundle b = mockBundle("sample1");

		ServletContextHelper ctx = new ServletContextHelper() {
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		ServiceReference<ServletContextHelper> contextRef
				= mockReference(b, ServletContextHelper.class, properties, () -> ctx, 0L, 0);
		OsgiContextModel contextModel = getServletContextHelperCustomizer().addingService(contextRef);

		ServiceReference<Servlet> servletRef
				= mockServletReference(b, "s1", () -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		mockContextSelectProperty(servletRef, "c1");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		// unregistration of existing SCH for /c1 path
		getServletContextHelperCustomizer().removedService(contextRef, contextModel);
		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));

		// registration of NEW SCH for /c2 path but same "c1" name, so it should match the selector of the servlet
		// leading to it's re-registration under new path
		mockProperty(contextRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c2");
		contextModel = getServletContextHelperCustomizer().addingService(contextRef);

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));
	}

	@Test
	public void modifyOneOfServletContextHelpers() throws Exception {
		Bundle b = mockBundle("sample1");

		ServiceReference<Servlet> servletRef
				= mockServletReference(b, "s1", () -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		mockContextSelectProperty(servletRef, "c1", "c2");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		ServletContextHelper ctx1 = new ServletContextHelper() {
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		ServiceReference<ServletContextHelper> context1Ref
				= mockReference(b, ServletContextHelper.class, properties, () -> ctx1, 0L, 0);
		OsgiContextModel cm1 = getServletContextHelperCustomizer().addingService(context1Ref);

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), startsWith("HTTP/1.1 404"));

		ServletContextHelper ctx2 = new ServletContextHelper() {
		};
		properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c2");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c2");
		ServiceReference<ServletContextHelper> context2Ref
				= mockReference(b, ServletContextHelper.class, properties, () -> ctx2, 0L, 0);
		OsgiContextModel cm2 = getServletContextHelperCustomizer().addingService(context2Ref);

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));

		// modification of one of the contexts
		mockProperty(context2Ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c2a");
		getServletContextHelperCustomizer().modifiedService(context2Ref, cm2);

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c2/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2a/s"), endsWith("S(1)"));

		getServletContextHelperCustomizer().removedService(context1Ref, cm1);
		getServletContextHelperCustomizer().removedService(context2Ref, cm2);

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2a/s"), startsWith("HTTP/1.1 404"));
	}

	@Test
	public void changeContextPathAfterServletRegistration() throws Exception {
		Bundle b = mockBundle("sample1");

		ServletContextHelper ctx = new ServletContextHelper() {
		};
		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		ServiceReference<ServletContextHelper> contextRef
				= mockReference(b, ServletContextHelper.class, properties, () -> ctx, 0L, 0);
		OsgiContextModel contextModel = getServletContextHelperCustomizer().addingService(contextRef);

		ServiceReference<Servlet> servletRef
				= mockServletReference(b, "s1", () -> new Utils.MyIdServlet("1"), 0L, 0, "/s");
		mockContextSelectProperty(servletRef, "c1");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		// org.osgi.framework.ServiceRegistration.setProperties()
		mockProperty(contextRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c2");
		getServletContextHelperCustomizer().modifiedService(contextRef, contextModel);

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2/s"), endsWith("S(1)"));

		mockProperty(contextRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
		getServletContextHelperCustomizer().modifiedService(contextRef, contextModel);

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/c2/s"), startsWith("HTTP/1.1 404"));
		assertThat(httpGET(port, "/s"), endsWith("S(1)"));
	}

}
