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

import jakarta.servlet.ServletContextListener;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.extender.samples.whiteboard.TestSCL;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.junit.Assert.assertNotEquals;

public abstract class AbstractWhiteboardIsolationIntegrationTest extends AbstractContainerTestBase {

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
	}

}
