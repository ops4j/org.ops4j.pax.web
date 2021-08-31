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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.samples.whiteboard.Control;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.Preprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, Control {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

	private ServiceRegistration<ServletContextHelper> c1Reg;
	private ServiceRegistration<ServletContextHelper> c2Reg;

	private ServiceRegistration<Servlet> s1Reg;
	private ServiceRegistration<Servlet> s2Reg;
	private ServiceRegistration<Servlet> s3Reg;

	private ServiceRegistration<Filter> f1Reg;
	private ServiceRegistration<Filter> f2Reg;
	private ServiceRegistration<Filter> f3Reg;

	private ServiceRegistration<Preprocessor> p1Reg;
	private ServiceRegistration<Preprocessor> p2Reg;
	private ServiceRegistration<Preprocessor> p3Reg;

	@SuppressWarnings("deprecation")
	public void start(final BundleContext bundleContext) {
		Dictionary<String, Object> props;

		WhiteboardServlet.counter = new AtomicInteger(0);
		WhiteboardFilter.counter = new AtomicInteger(0);
		WhiteboardPreprocessor.counter = new AtomicInteger(0);

		// two contexts with different paths

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c1");
		c1Reg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props);

		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, "c2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/c2");
		c2Reg = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() {
		}, props);

		List<String> events = new ArrayList<>();
		props = new Hashtable<>();
		props.put("events", "true");
		bundleContext.registerService(List.class, events, props);

		// filter1 registered as singleton into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "f1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, new String[] { "/*" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		WhiteboardFilter f1 = new WhiteboardFilter("f1", events);
		events.add("f1(" + f1.getId() + ") constructed once");
		f1Reg = bundleContext.registerService(Filter.class, f1, props);

		// filter2 registered as factory (bundle-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "f2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, new String[] { "/f2" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		f2Reg = bundleContext.registerService(Filter.class, new ServiceFactory<Filter>() {
			@Override
			public Filter getService(Bundle bundle, ServiceRegistration<Filter> registration) {
				WhiteboardFilter f2 = new WhiteboardFilter("f2", events);
				events.add(String.format("f2(%d) constructed for %s", f2.getId(), bundle.getSymbolicName()));
				return f2;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Filter> registration, Filter service) {
				WhiteboardFilter f2 = (WhiteboardFilter) service;
				f2.getEvents().add(String.format("f2(%d) unget for %s", f2.getId(), bundle.getSymbolicName()));
			}
		}, props);

		// filter3 registered as factory (prototype-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "f3");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, new String[] { "/f3" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		f3Reg = bundleContext.registerService(Filter.class, new PrototypeServiceFactory<Filter>() {
			@Override
			public Filter getService(Bundle bundle, ServiceRegistration<Filter> registration) {
				WhiteboardFilter f3 = new WhiteboardFilter("f3", events);
				events.add(String.format("f3(%d) constructed for %s", f3.getId(), bundle.getSymbolicName()));
				return f3;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Filter> registration, Filter service) {
				WhiteboardFilter f3 = (WhiteboardFilter) service;
				f3.getEvents().add(String.format("f3(%d) unget for %s", f3.getId(), bundle.getSymbolicName()));
			}
		}, props);

		// preprocessors are tricky... I mean they are straightforward, but "140.5.1 Servlet Pre-Processors" chapter
		// says:
		//      However, as pre-processors are called before dispatching, the targeted servlet context is not yet
		//      know. Therefore the FilterConfig.getServletContext returns the servlet context of the backing
		//      implementation, the same context as returned by the request. As a pre-processor instance is not
		//      associated with a specific servlet context, it is safe to implement it as a singleton.
		//
		// The "problem" in Pax web is that even if Preprocessors selects all available "contexts", they are NOT
		// "called before dispatching", because in Jetty/Tomcat/Undertow, dispatching already happened by selecting
		// target, real "servlet context" (by context path of the URI). So we HAVE to handle preprocessors
		// as prototypes too.

		// preprocessor1 registered as singleton into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "p1");
		// no need to specify context selector for preprocessors - it should be registered "to" all of them
//		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
//				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		WhiteboardPreprocessor p1 = new WhiteboardPreprocessor("p1", events);
		events.add("p1(" + p1.getId() + ") constructed once");
		p1Reg = bundleContext.registerService(Preprocessor.class, p1, props);

		// filter2 registered as factory (bundle-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "p2");
		p2Reg = bundleContext.registerService(Preprocessor.class, new ServiceFactory<Preprocessor>() {
			@Override
			public Preprocessor getService(Bundle bundle, ServiceRegistration<Preprocessor> registration) {
				WhiteboardPreprocessor p2 = new WhiteboardPreprocessor("p2", events);
				events.add(String.format("p2(%d) constructed for %s", p2.getId(), bundle.getSymbolicName()));
				return p2;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Preprocessor> registration, Preprocessor service) {
				WhiteboardPreprocessor p2 = (WhiteboardPreprocessor) service;
				p2.getEvents().add(String.format("p2(%d) unget for %s", p2.getId(), bundle.getSymbolicName()));
			}
		}, props);

		// filter3 registered as factory (prototype-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "p3");
		p3Reg = bundleContext.registerService(Preprocessor.class, new PrototypeServiceFactory<Preprocessor>() {
			@Override
			public Preprocessor getService(Bundle bundle, ServiceRegistration<Preprocessor> registration) {
				WhiteboardPreprocessor p3 = new WhiteboardPreprocessor("p3", events);
				events.add(String.format("p3(%d) constructed for %s", p3.getId(), bundle.getSymbolicName()));
				return p3;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Preprocessor> registration, Preprocessor service) {
				WhiteboardPreprocessor p3 = (WhiteboardPreprocessor) service;
				p3.getEvents().add(String.format("p3(%d) unget for %s", p3.getId(), bundle.getSymbolicName()));
			}
		}, props);

		// servlet1 registered as singleton into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s1");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/s1" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		props.put(PaxWebConstants.SERVICE_PROPERTY_LOAD_ON_STARTUP, 1);
		WhiteboardServlet s1 = new WhiteboardServlet("s1", events);
		events.add("s1(" + s1.getId() + ") constructed once");
		s1Reg = bundleContext.registerService(Servlet.class, s1, props);

		// servlet2 registered as factory (bundle-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s2");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/s2" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		props.put(PaxWebConstants.SERVICE_PROPERTY_LOAD_ON_STARTUP, 2);
		s2Reg = bundleContext.registerService(Servlet.class, new ServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				WhiteboardServlet s2 = new WhiteboardServlet("s2", events);
				events.add(String.format("s2(%d) constructed for %s", s2.getId(), bundle.getSymbolicName()));
				return s2;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
				WhiteboardServlet s2 = (WhiteboardServlet) service;
				s2.getEvents().add(String.format("s2(%d) unget for %s", s2.getId(), bundle.getSymbolicName()));
			}
		}, props);

		// servlet3 registered as factory (prototype-scoped) into two contexts
		props = new Hashtable<>();
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, "s3");
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/s3" });
		props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
				"(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=c2))");
		props.put(PaxWebConstants.SERVICE_PROPERTY_LOAD_ON_STARTUP, 3);
		s3Reg = bundleContext.registerService(Servlet.class, new PrototypeServiceFactory<Servlet>() {
			@Override
			public Servlet getService(Bundle bundle, ServiceRegistration<Servlet> registration) {
				WhiteboardServlet s3 = new WhiteboardServlet("s3", events);
				events.add(String.format("s3(%d) constructed for %s", s3.getId(), bundle.getSymbolicName()));
				return s3;
			}

			@Override
			public void ungetService(Bundle bundle, ServiceRegistration<Servlet> registration, Servlet service) {
				WhiteboardServlet s3 = (WhiteboardServlet) service;
				s3.getEvents().add(String.format("s3(%d) unget for %s", s3.getId(), bundle.getSymbolicName()));
			}
		}, props);

		bundleContext.registerService(Control.class, this, null);
	}

	@Override
	public void execute(String command) {
		String[] argv = command.split(" ");
		String op = argv[0];
		String arg = argv[1];
		if ("change".equals(op)) {
			if ("s3".equals(arg)) {
				String[] props = s3Reg.getReference().getPropertyKeys();
				Dictionary<String, Object> newProps = new Hashtable<>();
				for (String key : props) {
					newProps.put(key, s3Reg.getReference().getProperty(key));
				}
				newProps.put("uuid", UUID.randomUUID().toString());
				// should trigger re-registration
				s3Reg.setProperties(newProps);
			}
			if ("f3".equals(arg)) {
				String[] props = f3Reg.getReference().getPropertyKeys();
				Dictionary<String, Object> newProps = new Hashtable<>();
				for (String key : props) {
					newProps.put(key, f3Reg.getReference().getProperty(key));
				}
				newProps.put("uuid", UUID.randomUUID().toString());
				// should trigger re-registration
				f3Reg.setProperties(newProps);
			}
		}
	}

	public void stop(BundleContext bundleContext) {
		for (ServiceRegistration<?> r : new ServiceRegistration<?>[] {
				p1Reg, p2Reg, p3Reg, f1Reg, f2Reg, f3Reg, s1Reg, s2Reg, s3Reg, c1Reg, c2Reg
		}) {
			if (r != null) {
				r.unregister();
			}
		}
	}

}
