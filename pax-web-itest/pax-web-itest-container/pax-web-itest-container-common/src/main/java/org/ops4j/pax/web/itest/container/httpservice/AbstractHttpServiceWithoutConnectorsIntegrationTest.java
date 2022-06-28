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
package org.ops4j.pax.web.itest.container.httpservice;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.OptionUtils.combine;

public abstract class AbstractHttpServiceWithoutConnectorsIntegrationTest extends AbstractContainerTestBase {

	private Bundle hsBundle;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		org.osgi.service.cm.Configuration config = caService.getConfiguration(PaxWebConstants.PID, null);

		Dictionary<String, Object> props = new Hashtable<>();

		props.put(PaxWebConfig.PID_CFG_HTTP_ENABLED, "false");

		configureAndWaitForServletWithMapping("/alt-images/*", () -> {
			hsBundle = installAndStartBundle(sampleURI("hs-helloworld"));
			Dictionary<String, Object> current = config.getProperties();
			if (current == null || !"false".equals(current.get(PaxWebConfig.PID_CFG_HTTP_ENABLED))) {
				configureAndWait(() -> {
					try {
						config.update(props);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}, list -> {
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException(e);
					}
					return true;
				});
			}
		});
	}

	@After
	public void tearDown() throws BundleException {
		if (hsBundle != null) {
			hsBundle.stop();
			hsBundle.uninstall();
		}
	}

	@Test
	public void testSubPath() throws Exception {
		ServiceTracker<?, ?> tracker = new ServiceTracker<>(context, "org.ops4j.pax.web.service.spi.ServerControllerFactory", null);
		tracker.open();
		assertNotNull(tracker.waitForService(5000));
	}

}
