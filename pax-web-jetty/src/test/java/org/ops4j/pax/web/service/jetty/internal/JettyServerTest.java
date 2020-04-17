/*
 * Copyright 2016 Tadayoshi Sato.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.jetty.internal;

import java.lang.management.ManagementFactory;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JettyServerTest {

	@Before
	public void setUp() throws Exception {
		cleanupMBeans();
	}

	@After
	public void tearDown() throws Exception {
		cleanupMBeans();
	}

	private void cleanupMBeans() throws Exception {
		Set<ObjectInstance> mbeans = queryMBeans();
		for (ObjectInstance mbean : mbeans) {
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(mbean.getObjectName());
		}
	}

	@Test
	public void startStopJMX() throws Exception {
//		JettyServer server = new JettyServerWrapper(new ServerModel(null), null);
//		try {
//			server.start();
//			Set<ObjectInstance> mbeans = queryMBeans();
//			assertFalse(mbeans.isEmpty());
//		} finally {
//			server.stop();
//			Set<ObjectInstance> mbeans = queryMBeans();
//			assertTrue(mbeans.isEmpty());
//		}
	}

	private Set<ObjectInstance> queryMBeans() throws MalformedObjectNameException {
		return ManagementFactory.getPlatformMBeanServer()
				.queryMBeans(new ObjectName("org.eclipse.jetty.*:*"), null);
	}

}
