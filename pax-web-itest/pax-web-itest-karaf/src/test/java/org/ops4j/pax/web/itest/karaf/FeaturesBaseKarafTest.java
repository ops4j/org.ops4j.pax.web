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

import java.rmi.NoSuchObjectException;
import java.util.Hashtable;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.junit.Assert.assertTrue;

/**
 * @author achim
 */
@RunWith(PaxExam.class)
public abstract class FeaturesBaseKarafTest extends AbstractKarafTestBase {

	@Test
	public void test() throws Exception {
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-war")));
		assertTrue(featuresService.isInstalled(featuresService.getFeature("pax-web-whiteboard")));
	}

	public JMXConnector getJMXConnector() throws Exception {
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root");
		Hashtable<String, Object> env = new Hashtable<>();
		String[] credentials = new String[] { "karaf", "karaf" };
		env.put(JMXConnector.CREDENTIALS, credentials);
		int count = 15;
		Exception last = null;
		while (count > 0) {
			try {
				return JMXConnectorFactory.connect(url, env);
			} catch (NoSuchObjectException e) {
				count--;
				last = e;
				Thread.sleep(400);
			}
		}
		throw last;
	}

}
