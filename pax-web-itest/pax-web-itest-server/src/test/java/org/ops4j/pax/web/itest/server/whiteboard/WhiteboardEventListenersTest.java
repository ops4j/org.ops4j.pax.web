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
import java.util.EventListener;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultListenerMapping;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.whiteboard.ListenerMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;
import static org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContext.PAXWEB_STANDARD_WRAPPER;
import static org.ops4j.pax.web.service.tomcat.internal.PaxWebStandardContext.PAXWEB_TOMCAT_REQUEST;

@RunWith(Parameterized.class)
public class WhiteboardEventListenersTest extends MultiContainerTestSupport {

	@Test
	public void twoWaysToRegisterEventListeners() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		// servlet that sets an attribute to request, session and context
		Servlet servlet = new Utils.MyIdServlet("1") {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				super.doGet(req, resp);
				// even if we wrap request object, attributes are passed to wrapped request
				req.setAttribute("a", "1");
				req.getSession().setAttribute("b", "2");
				// this is tricky because we store attributes by OsgiServletContext, which is
				// in 1:1 relation with OsgiContextModel
				req.getServletContext().setAttribute("c", "3");
			}
		};
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> servlet, 0L, 0, "/s");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		// 1. Whiteboard registration as an OSGi service of listener class

		Hashtable<String, Object> properties = new Hashtable<>();
		// OSGi CMPN Whiteboard properties
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		Class<?>[] classes = new Class<?>[] {
				ServletRequestAttributeListener.class,
				HttpSessionAttributeListener.class,
				ServletContextAttributeListener.class
		};

		Map<String, EventObject> events = new LinkedHashMap<>();
		ServiceReference<EventListener> elRef = mockReference(sample1, classes, properties, () -> new AttributeListener(events), 0L, 0);
		EventListenerModel elModel = getListenerCustomizer().addingService(elRef);

		httpGET(port, "/s");
		events.entrySet().removeIf(e -> e.getKey().contains("jetty")
				|| e.getKey().equals(PAXWEB_STANDARD_WRAPPER)
				|| e.getKey().equals(PAXWEB_TOMCAT_REQUEST));
		assertThat(events.size(), equalTo(3));
		Iterator<Map.Entry<String, EventObject>> it = events.entrySet().iterator();
		assertThat(((ServletRequestAttributeEvent) it.next().getValue()).getValue(), equalTo("1"));
		assertThat(((HttpSessionBindingEvent) it.next().getValue()).getValue(), equalTo("2"));
		assertThat(((ServletContextAttributeEvent) it.next().getValue()).getValue(), equalTo("3"));

		getListenerCustomizer().removedService(elRef, elModel);

		httpGET(port, "/s");
		events.entrySet().removeIf(e -> e.getKey().contains("jetty")
				|| e.getKey().equals(PAXWEB_STANDARD_WRAPPER)
				|| e.getKey().equals(PAXWEB_TOMCAT_REQUEST));
		assertThat("No new events should be added", events.size(), equalTo(3));

		// 2. Whiteboard registration as Pax Web specific org.ops4j.pax.web.service.whiteboard.ListenerMapping
		//    OSGi service

		events.clear();
		DefaultListenerMapping lm = new DefaultListenerMapping();
		lm.setListener(new AttributeListener(events));
		ServiceReference<ListenerMapping> elMappingRef = mockReference(sample1, ListenerMapping.class,
				null, () -> lm);
		elModel = getListenerMappingCustomizer().addingService(elMappingRef);

		httpGET(port, "/s");
		events.entrySet().removeIf(e -> e.getKey().contains("jetty")
				|| e.getKey().equals(PAXWEB_STANDARD_WRAPPER)
				|| e.getKey().equals(PAXWEB_TOMCAT_REQUEST));
		assertThat(events.size(), equalTo(3));
		it = events.entrySet().iterator();
		assertThat(((ServletRequestAttributeEvent) it.next().getValue()).getValue(), equalTo("1"));
		assertThat(((HttpSessionBindingEvent) it.next().getValue()).getValue(), equalTo("2"));
		assertThat(((ServletContextAttributeEvent) it.next().getValue()).getValue(), equalTo("3"));

		getListenerMappingCustomizer().removedService(elMappingRef, elModel);

		httpGET(port, "/s");
		events.entrySet().removeIf(e -> e.getKey().contains("jetty")
				|| e.getKey().equals(PAXWEB_STANDARD_WRAPPER)
				|| e.getKey().equals(PAXWEB_TOMCAT_REQUEST));
		assertThat("No new events should be added", events.size(), equalTo(3));

		getServletCustomizer().removedService(servletRef, model);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void servletContextListeners() {
		Bundle sample1 = mockBundle("sample1");

		// here we check that IF javax.servlet.ServletContextListener is registered before first "active"
		// web element (i.e., one that can handle requests), then such listener should get "context initialized"
		// event

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		List<ServletContextEvent> events = new LinkedList<>();
		ServiceReference<EventListener> elRef = mockReference(sample1, new Class<?>[] { ServletContextListener.class }, properties,
				() -> new ContextListener(events), 0L, 0);
		EventListenerModel elModel = getListenerCustomizer().addingService(elRef);

		assertThat(events.size(), equalTo(0));

		// now register a servlet - this will start and initialize the context

		Servlet servlet = new Utils.MyIdServlet("1");
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> servlet, 0L, 0, "/s");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		// without any actual request the context should start&initialize
		assertThat(events.size(), equalTo(1));
		assertThat(events.get(0).getServletContext().getContextPath(), equalTo(""));

		getServletCustomizer().removedService(servletRef, model);
		getListenerCustomizer().removedService(elRef, elModel);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	@Test
	public void servletContextListenersOrdering() {
		Bundle sample1 = mockBundle("sample1");

		List<String> events = new LinkedList<>();

		Hashtable<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceReference<EventListener> el1Ref = mockReference(sample1, new Class<?>[] { ServletContextListener.class }, properties,
				() -> new NamedContextListener("l1", events), 0L, 0);
		EventListenerModel el1Model = getListenerCustomizer().addingService(el1Ref);

		assertThat(events.size(), equalTo(0));

		// now register a servlet - this will start and initialize the context

		Servlet servlet = new Utils.MyIdServlet("1");
		ServiceReference<Servlet> servletRef = mockServletReference(sample1, "servlet1",
				() -> servlet, 0L, 0, "/s");
		ServletModel model = getServletCustomizer().addingService(servletRef);

		// without any actual request the context should start&initialize
		assertThat(events.size(), equalTo(1));
		assertThat(events.get(0), equalTo("l1 initialized in \"\""));

		// now register a higher ranked ServletContextListener - this SHOULD restart the context and listeners
		// should be called in proper order

		properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceReference<EventListener> el2Ref = mockReference(sample1, new Class<?>[] { ServletContextListener.class }, properties,
				() -> new NamedContextListener("l2", events), 0L, 1);
		EventListenerModel el2Model = getListenerCustomizer().addingService(el2Ref);

		// because the context was already started, there's no need to register or call the servlet.

		assertThat(events.size(), equalTo(4));
		assertThat(events.get(1), equalTo("l1 destroyed in \"\""));
		assertThat(events.get(2), equalTo("l2 initialized in \"\""));
		assertThat(events.get(3), equalTo("l1 initialized in \"\""));

		getServletCustomizer().removedService(servletRef, model);
		getListenerCustomizer().removedService(el1Ref, el1Model);
		getListenerCustomizer().removedService(el2Ref, el2Model);

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(sample1);

		assertTrue(serverModelInternals.isClean(whiteboardBundle));
		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());
	}

	private static class AttributeListener implements ServletRequestAttributeListener, HttpSessionAttributeListener,
			ServletContextAttributeListener {

		private final Map<String, EventObject> events;

		AttributeListener(Map<String, EventObject> events) {
			this.events = events;
		}

		@Override
		public void attributeAdded(ServletContextAttributeEvent scae) {
			events.put(scae.getName(), scae);
		}

		@Override
		public void attributeRemoved(ServletContextAttributeEvent scae) {

		}

		@Override
		public void attributeReplaced(ServletContextAttributeEvent scae) {
			events.put(scae.getName(), scae);
		}

		@Override
		public void attributeAdded(ServletRequestAttributeEvent srae) {
			events.put(srae.getName(), srae);
		}

		@Override
		public void attributeRemoved(ServletRequestAttributeEvent srae) {

		}

		@Override
		public void attributeReplaced(ServletRequestAttributeEvent srae) {
			events.put(srae.getName(), srae);
		}

		@Override
		public void attributeAdded(HttpSessionBindingEvent se) {
			events.put(se.getName(), se);
		}

		@Override
		public void attributeRemoved(HttpSessionBindingEvent se) {

		}

		@Override
		public void attributeReplaced(HttpSessionBindingEvent se) {
			events.put(se.getName(), se);
		}
	}

	private static class ContextListener implements ServletContextListener {

		private final List<ServletContextEvent> events;

		ContextListener(List<ServletContextEvent> events) {
			this.events = events;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			events.add(sce);
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			events.add(sce);
		}
	}

	private static class NamedContextListener implements ServletContextListener {

		private final List<String> events;
		private final String name;

		NamedContextListener(String name, List<String> events) {
			this.name = name;
			this.events = events;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			events.add(String.format("%s initialized in \"%s\"", name, sce.getServletContext().getContextPath()));
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			events.add(String.format("%s destroyed in \"%s\"", name, sce.getServletContext().getContextPath()));
		}
	}

}
