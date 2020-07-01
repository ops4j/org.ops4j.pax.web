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

import java.util.Arrays;
import java.util.Collections;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.internal.views.DirectWebContainerView;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.context.DefaultMultiBundleWebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

/**
 * These tests show proper scoping of {@link ServletContext servlet contexts}. We have to ensure that filters
 * and servlets use proper {@link ServletContext} depending on the target servlet and associated
 * {@link OsgiContextModel} - both during initialization and when processing HTTP requests.
 */
@RunWith(Parameterized.class)
public class ServerControllerScopesTest extends MultiContainerTestSupport {

	@Override
	public void initAll() throws Exception {
		configurePort();
	}

	/**
	 * {@link Servlet}, {@link Filter} and {@link org.osgi.service.http.context.ServletContextHelper}
	 * are registered from single {@link Bundle} and associated with each other.
	 * @throws Exception
	 */
	@Test
	public void singleServletContext() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());
		server.configureActiveServerController(controller);

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c1", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		// wc.createDefaultHttpContext() will immediately associate the context with "/"-related OsgiContextModel
//		WebContainerContext wcc1 = wc.createDefaultHttpContext("wcc1");
		WebContainerContext wcc1 = new DefaultHttpContext(bundle, "wcc1");

		batch = new Batch("Initialization Batch");
		OsgiContextModel cm1 = server.getOrCreateOsgiContextModel(wcc1, bundle, "/c1", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s1 = mock(ServiceReference.class);
		when(context.getService(s1)).thenAnswer(invocation -> new Utils.MyIdServlet("1"));
		@SuppressWarnings("unchecked")
		ServiceReference<Filter> f1 = mock(ServiceReference.class);
		when(context.getService(f1)).thenAnswer(invocation -> new Utils.MyIdFilter("1"));
		@SuppressWarnings("unchecked")
		ServiceReference<Filter> f2 = mock(ServiceReference.class);
		when(context.getService(f2)).thenAnswer(invocation -> new Utils.MyIdFilter("2"));

		DirectWebContainerView view = wc.adapt(DirectWebContainerView.class);

		view.registerServlet(Collections.singletonList(wcc1), new ServletModel.Builder("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(bundle, s1)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		// two filters, but the 2nd one is ranked higher
		view.registerFilter(Collections.singletonList(wcc1), new FilterModel.Builder("f1")
				.withUrlPatterns(new String[] { "/*" })
				.withFilterReference(bundle, f1)
				.withServiceRankAndId(10, 30)
				.build());
		view.registerFilter(Collections.singletonList(wcc1), new FilterModel.Builder("f2")
				.withUrlPatterns(new String[] { "/*" })
				.withFilterReference(bundle, f2)
				.withServiceRankAndId(15, 20)
				.build());

		// terminate=<id of the filter> is special request parameter telling the filters to NOT pass the control
		// to the chain any further. With deployment without default "/" servlet, filter mapping without target
		// servlet should work (IMO, though no spec says anything about it) as long as the filter is implemented
		// to be aware of this. Otherwise the filter would do own processing, but target servlet would simply
		// return HTTP 404 messing with filter's work.

		assertThat(httpGET(port, "/c1/s"), endsWith(">F(2)>F(1)S(1)<F(1)<F(2)"));
		assertThat(httpGET(port, "/c1/s2?terminate=1"), endsWith(">F(2)>F(1)<F(1)<F(2)"));

		view.unregisterFilter(new FilterModel.Builder("f1").build());

		assertThat(httpGET(port, "/c1/s"), endsWith(">F(2)S(1)<F(2)"));
		assertThat(httpGET(port, "/c1/s2?terminate=2"), endsWith(">F(2)<F(2)"));

		view.unregisterFilter(new FilterModel.Builder("f2").build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c1/s2"), startsWith("HTTP/1.1 404"));

		view.unregisterServlet(new ServletModel.Builder("s1").build());

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));

		((StoppableHttpService)wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

	/**
	 * {@link Servlet}, {@link Filter} and {@link org.osgi.service.http.context.ServletContextHelper}
	 * are registered through different bundle-scoped instances of {@link WebContainerContext}.
	 * @throws Exception
	 */
	@Test
	public void multipleServletContext() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle1 = mock(Bundle.class);
		BundleContext context1 = mock(BundleContext.class);
		when(bundle1.getBundleContext()).thenReturn(context1);
		Bundle bundle2 = mock(Bundle.class);
		BundleContext context2 = mock(BundleContext.class);
		when(bundle2.getBundleContext()).thenReturn(context2);
		Bundle bundle3 = mock(Bundle.class);
		BundleContext context3 = mock(BundleContext.class);
		when(bundle3.getBundleContext()).thenReturn(context3);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());
		server.configureActiveServerController(controller);

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc1 = new HttpServiceEnabled(bundle1, controller, server, null, config);
		HttpServiceEnabled wc2 = new HttpServiceEnabled(bundle2, controller, server, null, config);
		HttpServiceEnabled wc3 = new HttpServiceEnabled(bundle3, controller, server, null, config);

		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c1", batch);
		batch.accept(wc1.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wcc1 = new DefaultMultiBundleWebContainerContext(new DefaultHttpContext(null, "wcc1"));
		WebContainerContext wcc2 = new DefaultHttpContext(bundle2, "wcc2");
		WebContainerContext wcc3 = new DefaultHttpContext(bundle3, "wcc3");

		batch = new Batch("Initialization Batch");
		OsgiContextModel cm1 = server.getOrCreateOsgiContextModel(wcc1, null, "/c1", batch);
		OsgiContextModel cm2 = server.getOrCreateOsgiContextModel(wcc2, bundle2, "/c1", batch);
		OsgiContextModel cm3 = server.getOrCreateOsgiContextModel(wcc3, bundle3, "/c1", batch);
		batch.accept(wc1.getServiceModel());
		batch.accept(wc2.getServiceModel());
		batch.accept(wc3.getServiceModel());
		controller.sendBatch(batch);

		// servlet and filter2 are registered using the same context

		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> s1 = mock(ServiceReference.class);
		when(context2.getService(s1)).thenAnswer(invocation -> new Utils.MyIdServlet("1"));
		@SuppressWarnings("unchecked")
		ServiceReference<Filter> f1 = mock(ServiceReference.class);
		when(context3.getService(f1)).thenAnswer(invocation -> new Utils.MyIdFilter("1"));
		@SuppressWarnings("unchecked")
		ServiceReference<Filter> f2 = mock(ServiceReference.class);
		when(context2.getService(f2)).thenAnswer(invocation -> new Utils.MyIdFilter("2"));

		DirectWebContainerView view1 = wc1.adapt(DirectWebContainerView.class);
		DirectWebContainerView view2 = wc2.adapt(DirectWebContainerView.class);
		DirectWebContainerView view3 = wc3.adapt(DirectWebContainerView.class);

		view2.registerServlet(Collections.singletonList(wcc2), new ServletModel.Builder("s1")
				.withUrlPatterns(new String[] { "/s" })
				.withServletReference(bundle2, s1)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));

		// two filters, but the 2nd one is ranked higher
		view3.registerFilter(Arrays.asList(wcc1, wcc3), new FilterModel.Builder("f1")
				.withUrlPatterns(new String[] { "/*" })
				.withFilterReference(bundle3, f1)
				.withServiceRankAndId(10, 30)
				.build());
		view2.registerFilter(Arrays.asList(wcc1, wcc2), new FilterModel.Builder("f2")
				.withUrlPatterns(new String[] { "/*" })
				.withFilterReference(bundle2, f2)
				.withServiceRankAndId(15, 20)
				.build());

		assertThat(httpGET(port, "/c1/s"), endsWith(">F(2)S(1)<F(2)"));
		assertThat(httpGET(port, "/c1/s2?terminate=1"), endsWith(">F(2)>F(1)<F(1)<F(2)"));

		view3.unregisterFilter(new FilterModel.Builder("f1").build());

		assertThat(httpGET(port, "/c1/s"), endsWith(">F(2)S(1)<F(2)"));
		assertThat(httpGET(port, "/c1/s2?terminate=2"), endsWith(">F(2)<F(2)"));

		view2.unregisterFilter(new FilterModel.Builder("f2").build());

		assertThat(httpGET(port, "/c1/s"), endsWith("S(1)"));
		assertThat(httpGET(port, "/c1/s2"), startsWith("HTTP/1.1 404"));

		view2.unregisterServlet(new ServletModel.Builder("s1").build());

		assertThat(httpGET(port, "/c1/s"), startsWith("HTTP/1.1 404"));

		((StoppableHttpService)wc1).stop();
		((StoppableHttpService)wc2).stop();
		((StoppableHttpService)wc3).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals1 = serviceModelInternals(wc1);
		ServiceModelInternals serviceModelInternals2 = serviceModelInternals(wc2);
		ServiceModelInternals serviceModelInternals3 = serviceModelInternals(wc3);

		assertTrue(serverModelInternals.isClean(bundle1));
		assertTrue(serverModelInternals.isClean(bundle2));
		assertTrue(serverModelInternals.isClean(bundle3));
		assertTrue(serviceModelInternals1.isEmpty());
		assertTrue(serviceModelInternals2.isEmpty());
		assertTrue(serviceModelInternals3.isEmpty());
	}

}
