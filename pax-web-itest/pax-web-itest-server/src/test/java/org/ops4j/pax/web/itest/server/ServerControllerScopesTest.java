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
import org.ops4j.pax.web.service.internal.views.DirectWebContainerView;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
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
		ServerController controller = Utils.create(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mock(Bundle.class);
		BundleContext context = mock(BundleContext.class);
		when(bundle.getBundleContext()).thenReturn(context);

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());
		server.createDefaultServletContextModel(controller);

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

		controller.stop();
	}

}
