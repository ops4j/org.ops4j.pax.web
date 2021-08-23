/*
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
package org.ops4j.pax.web.itest.karaf;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;

import static org.junit.Assert.assertNotNull;

/**
 * @author achim
 */
public class FeaturesJettyKarafTest extends FeaturesBaseKarafTest {

	@Configuration
	public Option[] config() {
		return jettyConfig();
	}

	@Test
	public void testJmx() throws Exception {
		try (JMXConnector connector = this.getJMXConnector()) {
			MBeanServerConnection connection = connector.getMBeanServerConnection();
			ObjectName name = new ObjectName("org.eclipse.jetty.server.handler:type=contexthandlercollection,id=0");
			Object handlers = connection.getAttribute(name, "handlers");
			assertNotNull(handlers);
		}
	}

}
