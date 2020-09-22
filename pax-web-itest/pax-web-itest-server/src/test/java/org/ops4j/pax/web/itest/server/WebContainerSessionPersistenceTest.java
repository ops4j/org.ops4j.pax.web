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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.web.itest.server.support.Utils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.internal.HttpServiceEnabled;
import org.ops4j.pax.web.service.internal.StoppableHttpService;
import org.osgi.framework.Bundle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.web.itest.server.support.Utils.httpGET;

@RunWith(Parameterized.class)
public class WebContainerSessionPersistenceTest extends MultiContainerTestSupport {

	@Override
	protected String sessionPersistenceLocation() {
		File location = new File("target/sessions");
		location.mkdirs();
		try {
			FileUtils.cleanDirectory(location);
			return location.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Test
	public void persistentSessions() throws Exception {
		Bundle sample1 = mockBundle("sample1");

		WebContainer wc = new HttpServiceEnabled(sample1, controller, serverModel, null, config);

		// we need some "active" component to test the configuration
		wc.registerServlet("/visit", new TestServlet("1"), null, null);

		// no sessions
		String response = httpGET(port, "/visit");
		assertTrue(response.endsWith("counter: 1"));
		response = httpGET(port, "/visit");
		assertTrue(response.endsWith("counter: 1"));

		// sessions managed with httpclient5
		BasicCookieStore store = new BasicCookieStore();
		CloseableHttpClient client = HttpClients.custom().setDefaultCookieStore(store).build();
		HttpGet get = new HttpGet("http://127.0.0.1:" + port + "/visit");
		CloseableHttpResponse res = client.execute(get);
		assertNotNull(res.getHeader("Set-Cookie"));
		assertTrue(store.getCookies().stream().anyMatch(c -> "JSESSIONID".equals(c.getName())));
		Cookie c = store.getCookies().get(0);
		assertThat(c.getPath(), equalTo("/"));
		assertTrue(c.containsAttribute("httponly"));
		assertNull(c.getExpiryDate());

		assertThat(EntityUtils.toString(res.getEntity()), equalTo("counter: 1"));
		res = client.execute(get);
		assertThat(EntityUtils.toString(res.getEntity()), equalTo("counter: 2"));

		((StoppableHttpService) wc).stop();

		ServerModelInternals serverModelInternals = serverModelInternals(serverModel);
		ServiceModelInternals serviceModelInternals = serviceModelInternals(wc);

		assertTrue(serverModelInternals.isClean(sample1));
		assertTrue(serviceModelInternals.isEmpty());

		if (runtime == Runtime.UNDERTOW) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("target/sessions/ROOT"));
			Object sessions = ois.readObject();
			assertNotNull(sessions);
			assertTrue(sessions instanceof LinkedHashMap);
			assertThat(((Map<?, ?>) sessions).size(), equalTo(3));
		} else if (runtime == Runtime.TOMCAT) {
			controller.stop();
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("target/sessions/SESSIONS.ser"));
			Object count = ois.readObject();
			assertNotNull(count);
			assertThat((Integer) count, equalTo(3));
		} else if (runtime == Runtime.JETTY) {
			String[] names = new File("target/sessions").list();
			assertNotNull(names);
			assertThat(names.length, equalTo(3));
		}
	}

	private static class TestServlet extends Utils.MyIdServlet {

		TestServlet(String id) {
			super(id);
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			Integer counter = (Integer) req.getSession().getAttribute("counter");
			if (counter == null) {
				counter = 1;
			} else {
				counter++;
			}
			req.getSession().setAttribute("counter", counter);
			resp.getWriter().print(String.format("counter: %d", counter));
		}
	}

}
