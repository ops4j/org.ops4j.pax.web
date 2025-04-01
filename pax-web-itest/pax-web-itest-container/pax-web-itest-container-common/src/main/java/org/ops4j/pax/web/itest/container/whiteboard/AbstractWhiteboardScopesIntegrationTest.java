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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import jakarta.servlet.ServletContextListener;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.Control;
import org.ops4j.pax.web.extender.samples.whiteboard.TestSCL;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public abstract class AbstractWhiteboardScopesIntegrationTest extends AbstractContainerTestBase {

	protected Bundle bundle;
	private ServiceRegistration<ServletContextListener> sclReg;
	private ServletContextListener ourScl;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/s3",
				() -> {
					bundle = installAndStartBundle(sampleURI("whiteboard-scopes"));

					Dictionary<String, Object> props = new Hashtable<>();
					props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(osgi.http.whiteboard.context.name=c1)");
					props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
					ourScl = new TestSCL();
					sclReg = context.registerService(ServletContextListener.class, ourScl, props);
				});
	}

	@Test
	public void testDifferentServletContextListeners() throws Exception {
		ServiceTracker<?, ?> t1 = new ServiceTracker<>(context, context.createFilter("(test=true)"), null);
		t1.open();
		TestSCL scl1 = (TestSCL) t1.getService();
		t1.close();

		TestSCL scl2 = (TestSCL) this.ourScl;

		// Listeners should get special ServletContext where getClassLoader() returns their own classloader
		// see org.osgi.test.cases.servlet.junit.ServletContextHelperTestCase.test_140_2_10to12()
		assertNotEquals(scl1.getRef().get().getClassLoader(), scl2.getRef().get().getClassLoader());

		sclReg.unregister();
	}

	@Test
	public void testWhiteboardRoot() throws Exception {
		ServiceTracker<?, ?> t1 = new ServiceTracker<>(context, context.createFilter("(events=true)"), null);
		t1.open();
		@SuppressWarnings("unchecked")
		List<String> events = (List<String>) t1.getService();
		t1.close();

		ServiceTracker<Control, Control> t2 = new ServiceTracker<>(context, Control.class, null);
		t2.open();
		Control control = t2.getService();
		t2.close();

		String c1s1 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s1");
		String c1s2 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s2");
		String c1s3 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s3");
		String c2s1 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s1");
		String c2s2 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s2");
		String c2s3 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s3");

		// singleton scope
		assertEquals(c1s1, c2s1);
		// bundle scope - still the same instance
		assertEquals(c1s2, c2s2);
		// prototype scope - the only way to get different servlet instances per target context
		assertNotEquals(c1s3, c2s3);

		// don't check the events. Jetty and Undertow have exactly 46 here, but Tomcat has 118...
		// The problem is that you can't simply add one filter and init() only the added one, you have to stop
		// (and thus destroy()) existing ones. So imagine what happens when you add 6 filters and preprocessors
		// one at a time...

		control.execute("change s3");

		control.execute("change f3");

		c1s1 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s1");
		c1s2 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s2");
		c1s3 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c1/s3");
		c2s1 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s1");
		c2s2 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s2");
		c2s3 = HttpTestClientFactory.createDefaultTestClient().doGETandExecuteTest("http://127.0.0.1:8181/c2/s3");

		// singleton scope
		assertEquals(c1s1, c2s1);
		// bundle scope - still the same instance
		assertEquals(c1s2, c2s2);
		// prototype scope - the only way to get different servlet instances per target context
		assertNotEquals(c1s3, c2s3);

		bundle.stop();
	}

}
