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
package org.ops4j.pax.web.itest.server.controller;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import jakarta.servlet.Servlet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.Runtime;
import org.ops4j.pax.web.itest.server.support.ErrorServlet;
import org.ops4j.pax.web.itest.server.support.ProblemServlet;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.ops4j.pax.web.service.internal.views.DirectWebContainerView;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

/**
 * These tests show registration of <em>error pages</em> using {@link WebContainer} (no Whiteboard).
 */
@RunWith(Parameterized.class)
public class ServerControllerErrorPagesTest extends MultiContainerTestSupport {

	@Override
	public void initAll() throws Exception {
		configurePort();
	}

	/**
	 * <p>Test for service registration of <em>error pages</em> to different OSGi contexts and handling conflicts.</p>
	 *
	 * @throws Exception
	 */
	@Test
	public void registerConflictingErrorPages() throws Exception {
		ServerController controller = Utils.createServerController(null, port, runtime, getClass().getClassLoader());
		controller.configure();
		controller.start();

		Bundle bundle = mockBundle("b1", false);
		BundleContext context = bundle.getBundleContext();

		ServerModel server = new ServerModel(new Utils.SameThreadExecutor());

		Configuration config = controller.getConfiguration();
		HttpServiceEnabled wc = new HttpServiceEnabled(bundle, controller, server, null, config);

		Batch batch = new Batch("Initialization Batch");
		server.getOrCreateServletContextModel("/c1", batch);
		server.getOrCreateServletContextModel("/c2", batch);
		server.getOrCreateServletContextModel("/c3", batch);
		server.getOrCreateServletContextModel("/c4", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		WebContainerContext wcc1 = new DefaultHttpContext(bundle, "wcc1");
		WebContainerContext wcc2 = new DefaultHttpContext(bundle, "wcc2");
		WebContainerContext wcc3 = new DefaultHttpContext(bundle, "wcc3");
		WebContainerContext wcc4 = new DefaultHttpContext(bundle, "wcc4");

		// 4 logical OSGi context models
		batch = new Batch("Initialization Batch");
		OsgiContextModel cm1 = server.getOrCreateOsgiContextModel(wcc1, bundle, "/c1", batch);
		OsgiContextModel cm2 = server.getOrCreateOsgiContextModel(wcc2, bundle, "/c2", batch);
		OsgiContextModel cm3 = server.getOrCreateOsgiContextModel(wcc3, bundle, "/c3", batch);
		OsgiContextModel cm4 = server.getOrCreateOsgiContextModel(wcc4, bundle, "/c4", batch);
		batch.accept(wc.getServiceModel());
		controller.sendBatch(batch);

		// 1 error servlets (with prefix mapping) and 1 "problem causing" servlet registered to all 4 contexts
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> errorServletRef = mock(ServiceReference.class);
		@SuppressWarnings("unchecked")
		ServiceReference<Servlet> problemServletRef = mock(ServiceReference.class);
		when(context.getService(errorServletRef)).thenAnswer(invocation -> new ErrorServlet());
		when(context.getService(problemServletRef)).thenAnswer(invocation -> new ProblemServlet());

		long serviceId = 0;

		DirectWebContainerView view = wc.adapt(DirectWebContainerView.class);

		view.registerServlet(Arrays.asList(wcc1, wcc2, wcc3, wcc4), new ServletModel.Builder()
				.withServletName("errorServlet")
				.withUrlPatterns(new String[] { "/error/*" })
				.withServletReference(errorServletRef)
				.withServiceRankAndId(0, ++serviceId)
				.build());
		view.registerServlet(Arrays.asList(wcc1, wcc2, wcc3, wcc4), new ServletModel.Builder()
				.withServletName("badServlet")
				.withUrlPatterns(new String[] { "/yikes" })
				.withServletReference(problemServletRef)
				.withServiceRankAndId(0, ++serviceId)
				.build());

		String th = FileNotFoundException.class.getName();
		String emsg = null;
		if (runtime == Runtime.JETTY) {
			emsg = th + ": x";
		} else {
			emsg = "x";
		}

		// errorPages#1 registered in /c1 and /c2
		ErrorPageModel em1 = new ErrorPageModel(new String[] { "461", th }, "/error/ep1");
		em1.setServiceRank(0);
		em1.setServiceId(++serviceId);
		view.registerErrorPages(Arrays.asList(wcc1, wcc2), em1);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep1: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep1: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		if (runtime == Runtime.JETTY) {
			assertThat(httpGET(port, "/c1/yikes?result=462&msg=x462"), containsString("<th>URI:</th><td>/c1/yikes</td>"));
			assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), containsString("<th>URI:</th><td>/c3/yikes</td>"));
		} else if (runtime == Runtime.TOMCAT) {
			assertThat(httpGET(port, "/c1/yikes?result=462&msg=x462"), containsString("<p><b>Message</b> x462</p><p>"));
			assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), containsString("<p><b>Message</b> x461</p><p>"));
		} else if (runtime == Runtime.UNDERTOW) {
			assertThat(httpGET(port, "/c1/yikes?result=462&msg=x462"), containsString("<body>x462</body>"));
			assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), containsString("<body>x461</body>"));
		}

		// errorPages#2 registered in /c3 - no conflict
		ErrorPageModel em2 = new ErrorPageModel(new String[] { "461", th }, "/error/ep2");
		em2.setServiceRank(3);
		em2.setServiceId(++serviceId);
		view.registerErrorPages(Collections.singletonList(wcc3), em2);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep1: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep1: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep2: [badServlet][null][null][x461][/c3/yikes][461]"));

		// errorPages#3 registered to /c1, but with higher service ID - should be marked as disabled
		ErrorPageModel em3 = new ErrorPageModel(new String[] { "461", th }, "/error/ep3");
		em3.setServiceRank(0);
		em3.setServiceId(++serviceId);
		view.registerErrorPages(Collections.singletonList(wcc1), em3);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep1: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep1: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep2: [badServlet][null][null][x461][/c3/yikes][461]"));

		// errorPages#4 registered to /c2 and /c3 - ranked higher than ep#1 in /c2, but ranked lower than ep#2 in /c3
		ErrorPageModel em4 = new ErrorPageModel(new String[] { "461", th }, "/error/ep4");
		em4.setServiceRank(2);
		em4.setServiceId(++serviceId);
		view.registerErrorPages(Arrays.asList(wcc2, wcc3), em4);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep1: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep1: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep2: [badServlet][null][null][x461][/c3/yikes][461]"));

		// errorPages#5 registered to /c2 and /c4 - ranked higher than ep#1 in /c2, so:
		//  - ep#1 is deactivated in /c1 and /c2
		//  - ep#3 is activated in /c1
		//  - ep#5 MAY be activated in /c2 and /c4, but in /c2, ep#4 is ranked higher than ep#5
		//  - ep#4 is ranked lower than ep#2 in /c3, so it won't be activated ANYWHERE
		//  - ep#5 will thus be activated in /c2 and /c4
		ErrorPageModel em5 = new ErrorPageModel(new String[] { "461", th }, "/error/ep5");
		em5.setServiceRank(1);
		em5.setServiceId(++serviceId);
		view.registerErrorPages(Arrays.asList(wcc2, wcc4), em5);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep3: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep5: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep2: [badServlet][null][null][x461][/c3/yikes][461]"));
		assertThat(httpGET(port, "/c4/yikes?result=461&msg=x461"), endsWith("/ep5: [badServlet][null][null][x461][/c4/yikes][461]"));

		// errorPages#6 registered to /c4 - ranked lower than ep#5 in /c4, so added as disabled
		ErrorPageModel em6 = new ErrorPageModel(new String[] { "461", th }, "/error/ep6");
		em6.setServiceRank(0);
		em6.setServiceId(++serviceId);
		view.registerErrorPages(Collections.singletonList(wcc4), em6);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep3: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep5: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep2: [badServlet][null][null][x461][/c3/yikes][461]"));
		assertThat(httpGET(port, "/c4/yikes?result=461&msg=x461"), endsWith("/ep5: [badServlet][null][null][x461][/c4/yikes][461]"));

		// errorPages#2 unregistered, ep#4 can be activated in /c3 and can be activated in /c2 because ep#5 in /c2 is ranked
		// lower than ep#4, so ep#5 disabled in /c4, so ep#6 enabled in /c4
		view.unregisterErrorPages(em2);

		assertThat(httpGET(port, "/c1/yikes?result=461&msg=x461"), endsWith("/ep3: [badServlet][null][null][x461][/c1/yikes][461]"));
		assertThat(httpGET(port, "/c2/yikes?ex=" + th + "&msg=x"), endsWith("/ep4: [badServlet][" + th + "][" + th + "][" + emsg + "][/c2/yikes][500]"));
		assertThat(httpGET(port, "/c3/yikes?result=461&msg=x461"), endsWith("/ep4: [badServlet][null][null][x461][/c3/yikes][461]"));
		assertThat(httpGET(port, "/c4/yikes?result=461&msg=x461"), endsWith("/ep6: [badServlet][null][null][x461][/c4/yikes][461]"));

		((StoppableHttpService) wc).stop();
		controller.stop();

		ServerModelInternals serverModelInternals = serverModelInternals(server);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(bundle));
		assertTrue(serviceModelInternals.isEmpty());
	}

}
