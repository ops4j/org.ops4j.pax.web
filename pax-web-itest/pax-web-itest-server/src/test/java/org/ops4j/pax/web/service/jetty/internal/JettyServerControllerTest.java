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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.ConfigurationBuilder;
import org.ops4j.pax.web.service.internal.DefaultHttpContext;
import org.ops4j.pax.web.service.internal.MetaTypePropertyResolver;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyServerControllerTest {

	public static Logger LOG = LoggerFactory.getLogger(JettyServerControllerTest.class);

	private int port;

	@Before
	public void init() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);
		port = serverSocket.getLocalPort();
		serverSocket.close();
	}

	@Test
	public void justInstantiateWithoutOsgi() throws Exception {
		ServerController controller = create(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
			properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
		});

		controller.configure();
		controller.start();
		controller.stop();
	}

	@Test
	public void registerSingleServlet() throws Exception {
		ServerController controller = create(properties -> {
			new File("target/ncsa").mkdirs();
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED, "true");
			properties.put(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR, "target/ncsa");

			// this file should be used to reconfigure thread pool already set inside Pax Web version of Jetty Server
			properties.put(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES, "target/test-classes/jetty-server.xml");
		});
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);

		WebContainerContext wcc = new DefaultHttpContext(bundle) {
			@Override
			public URL getResource(String name) {
				// this should be used when calling ServletContext.getResource
				try {
					return new URL("file://" + name);
				} catch (MalformedURLException ignored) {
					return null;
				}
			}

			@Override
			public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
				LOG.info("handleSecurity(" + request + ")");
				return request.getHeader("Let-Me-In") != null;
			}
		};

		Servlet servlet = new HttpServlet() {
			private ServletConfig config;

			private final Map<ServletContext, Boolean> contexts = new IdentityHashMap<>();

			@Override
			public void init(ServletConfig config) throws ServletException {
				super.init(config);
				assertThat(config.getInitParameter("p1"), equalTo("v1"));
				assertThat(super.getInitParameter("p1"), equalTo("v1"));
				contexts.put(config.getServletContext(), true);
				this.config = config;
			}

			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				resp.setContentType("text/plain");

				contexts.put(getServletContext(), true);
				contexts.put(req.getServletContext(), true);
				contexts.put(req.getSession().getServletContext(), true);
				contexts.put(config.getServletContext(), true);
				contexts.put(getServletConfig().getServletContext(), true);

				assertThat(contexts.size(), equalTo(1));

				assertThat(super.getInitParameter("p1"), equalTo("v1"));

				// this should give us "file:/something"
				resp.getWriter().print(req.getServletContext().getResource("/something").toString());
			}
		};

		Batch batch = new Batch("Register Single Servlet");

		ServerModel server = new ServerModel(new SameThreadExecutor());
		ServletContextModel context = new ServletContextModel("/c");
		batch.addServletContextModel(server, context);

		OsgiContextModel osgiContext = new OsgiContextModel(wcc, bundle);
		osgiContext.setServletContextModel(context);
		batch.addOsgiContextModel(osgiContext);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet")
				.withUrlPatterns(new String[] { "/s/*" })
				.withServlet(servlet)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContext)
				.withRegisteringBundle(bundle)
				.build());

		controller.sendBatch(batch);

		String response = get(port, "/c/s/1", "Let-Me-In: true");
		assertTrue(response.endsWith("file:/something"));

		response = get(port, "/c/s/1");
		assertTrue(response.contains("HTTP/1.1 403"));

		controller.stop();
	}

	@Test
	public void registerFilterAndServlet() throws Exception {
		ServerController controller = create(null);
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		WebContainerContext wcc1 = new DefaultHttpContext(bundle);
		WebContainerContext wcc2 = new DefaultHttpContext(bundle, "special");

		// when single instance is added more than once (passed in ServletModel), init(ServletConfig)
		// operates on single instance and even the Whiteboard Service specification suggests using Prototype
		// Service. Otherwise, init() would be called more than once on single instance providing different
		// ServletConfig objects (with different - and usually wrong ServletContext)
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> ref = mock(ServiceReference.class);
		when(context.getService(ref)).thenReturn(new MyHttpServlet());

		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.getWriter().write(getFilterName() + "1");
				if (!"/d".equals(req.getServletContext().getContextPath())) {
					// in /d we know we don't map to any servlet
					chain.doFilter(req, resp);
				}
				resp.getWriter().write(getFilterName() + "2");
				resp.getWriter().close();
			}
		};

		Batch batch = new Batch("Register Servlet and Filter");

		// two contexts. servlet will be registered to /c, filter - to /c and /d
		ServerModel server = new ServerModel(new SameThreadExecutor());

		ServletContextModel contextC = new ServletContextModel("/c");
		ServletContextModel contextD = new ServletContextModel("/d");
		ServletContextModel contextE = new ServletContextModel("/e");
		batch.addServletContextModel(server, contextC);
		batch.addServletContextModel(server, contextD);
		batch.addServletContextModel(server, contextE);

		OsgiContextModel osgiContextC = new OsgiContextModel(wcc1, bundle, contextC);
		OsgiContextModel osgiContextC2 = new OsgiContextModel(wcc2, bundle, contextC);
		OsgiContextModel osgiContextD = new OsgiContextModel(wcc1, bundle, contextD);
		OsgiContextModel osgiContextE = new OsgiContextModel(wcc1, bundle, contextE);
		batch.addOsgiContextModel(osgiContextC);
		batch.addOsgiContextModel(osgiContextC2);
		batch.addOsgiContextModel(osgiContextD);
		batch.addOsgiContextModel(osgiContextE);

		Map<String, String> initParams = new HashMap<>();
		initParams.put("p1", "v1");

		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet1")
				.withUrlPatterns(new String[] { "/s/*" }) // responds to /*/s/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC) // responds to /c/s/*
				.withOsgiContextModel(osgiContextE) // responds to /e/s/*
				.withRegisteringBundle(bundle)
				.build());
		batch.addServletModel(server, new ServletModel.Builder()
				.withServletName("my-servlet2")
				.withUrlPatterns(new String[] { "/s2/*" }) // responds to /*/s2/* depending on context selector
				.withServletReference(ref)
				.withInitParams(initParams)
				.withOsgiContextModel(osgiContextC2) // responds to /c/s2/*
				.withRegisteringBundle(bundle)
				.build());

		Map<String, Set<FilterModel>> filters = new HashMap<>();
		Set<FilterModel> set = new TreeSet<>();
		// this filter is NOT registered to osgiContextC2, so should NOT be mapped to /c/s2/*
		set.add(new FilterModel.Builder()
				.withFilterName("my-filter")
				.withUrlPatterns(new String[] { "/*" }) // maps to /*/* depending on associated contexts
				.withFilter(filter)
				.withOsgiContextModel(osgiContextC) // maps to /c/*
				.withOsgiContextModel(osgiContextD) // maps to /d/*
				.withRegisteringBundle(bundle)
				.build());
		filters.put("/c", set);
		filters.put("/d", set);
		batch.updateFilters(filters);

		controller.sendBatch(batch);

		// filter -> servlet
		String response;
		response = get(port, "/c/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-servlet1[/c]my-filter2"));

		// just one filter in the chain, without target servlet
		response = get(port, "/d/s/1");
		System.out.println(response);
		assertTrue(response.contains("my-filter1my-filter2"));

		// just servlet, because /* filter doesn't use servlet's ServletContextHelper
		response = get(port, "/c/s2/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet2[/c]"));

		// just servlet, because /* filter isn't associated with OsgiContext for /e
		response = get(port, "/e/s/1");
		System.out.println(response);
		assertTrue(response.contains("\r\nmy-servlet1[/e]"));

		controller.stop();
	}

	private ServerController create(Consumer<Hashtable<Object, Object>> callback) {
		Hashtable<Object, Object> properties = new Hashtable<>(System.getProperties());
		properties.put(PaxWebConfig.PID_CFG_TEMP_DIR, "target/tmp");
		properties.put(PaxWebConfig.PID_CFG_HTTP_PORT, Integer.toString(port));

		if (callback != null) {
			callback.accept(properties);
		}

		// it wouldn't work in OSGi because MetaTypePropertyResolver's package is not exported
		MetaTypePropertyResolver metatypeResolver = new MetaTypePropertyResolver();
		DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(properties, metatypeResolver);
		Configuration config = ConfigurationBuilder.getConfiguration(resolver, new HashMap<>());

		ServerControllerFactory factory = new JettyServerControllerFactory(null, this.getClass().getClassLoader());
		return factory.createServerController(config);
	}

	private String get(int port, String request, String ... headers) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n").getBytes());
		for (String header : headers) {
			s.getOutputStream().write((header + "\r\n").getBytes());
		}
		s.getOutputStream().write(("Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		return sw.toString();
	}

	private static class SameThreadExecutor implements Executor {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	}

	private static class MyHttpServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			resp.getWriter().print(getServletName() + "[" + getServletConfig().getServletContext().getContextPath() + "]");
		}
	}

}
