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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.MultiContainerTestSupport;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WhiteboardTempdirTest extends MultiContainerTestSupport {

	@Test
	public void oneServletWithTwoContexts() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		ServletContextHelper helper1 = new ServletContextHelper() {
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "c1",
				() -> helper1, 0L, 0, "/c"));

		ServletContextHelper helper2 = new ServletContextHelper() {
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d1",
				() -> helper2, 0L, 0, "/d"));

		// 2nd /d context with different helper and higher ranking
		ServletContextHelper helper3 = new ServletContextHelper() {
		};
		getServletContextHelperCustomizer().addingService(mockServletContextHelperReference(sample1, "d2",
				() -> helper3, 0L, 1, "/d"));

		List<String> events = new ArrayList<>(20);

		// servlet registered to /c and /d (with helper d1)
		ServiceReference<Servlet> servlet1Ref = mockServletReference(sample1, "servlet1",
				() -> new ProbeServlet("1", events), 0L, 0, "/s");
		when(servlet1Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d1))");
		ServletModel model1 = getServletCustomizer().addingService(servlet1Ref);

		// servlet registered to /c and /d (with helper d2)
		ServiceReference<Servlet> servlet2Ref = mockServletReference(sample1, "servlet2",
				() -> new ProbeServlet("2", events), 0L, 0, "/t");
		when(servlet2Ref.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT))
				.thenReturn("(|(osgi.http.whiteboard.context.name=c1)(osgi.http.whiteboard.context.name=d2))");
		ServletModel model2 = getServletCustomizer().addingService(servlet2Ref);

		httpGET(port, "/c/s");
		httpGET(port, "/c/t");
		httpGET(port, "/d/s");
		httpGET(port, "/d/t");

		assertThat(events.get(0), equalTo("tmp" + File.separatorChar + "c" + File.separatorChar + "c1"));
		assertThat(events.get(1), equalTo("tmp" + File.separatorChar + "c" + File.separatorChar + "c1"));
		assertThat(events.get(2), equalTo("tmp" + File.separatorChar + "d" + File.separatorChar + "d1"));
		assertThat(events.get(3), equalTo("tmp" + File.separatorChar + "d" + File.separatorChar + "d2"));

		getServletCustomizer().removedService(servlet1Ref, model1);
		getServletCustomizer().removedService(servlet2Ref, model2);
	}

	private static class ProbeServlet extends Utils.MyIdServlet {

		private final List<String> events;

		ProbeServlet(String id, List<String> events) {
			super(id);
			this.events = events;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			File tempdir = (File) req.getServletContext().getAttribute(ServletContext.TEMPDIR);
			events.add(new File("target").toPath().relativize(tempdir.toPath()).toString());
		}
	}

}
