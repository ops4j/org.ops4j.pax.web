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
package org.ops4j.pax.web.itest.container.httpservice;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.WaitCondition;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.ops4j.pax.web.itest.utils.WaitCondition.SLEEP_DURATION_MILLIS;

public abstract class AbstractHttpServiceProcessingWithConfigAdminIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/alt-images/*",
				() -> bundle = installAndStartBundle(sampleURI("hs-helloworld")));
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle != null) {
			bundle.stop();
			bundle.uninstall();
		}
	}

	@Test
	public void testContextAltering() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");

		Configuration cfg = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props = new Hashtable<>();
		configureContextProcessing(props);

		waitForReRegistration(() -> {
			try {
				cfg.update(props);
			} catch (IOException ignored) {
			}
		});

		performSecurityAssertion();

		waitForReRegistration(() -> {
			try {
				cfg.delete();
			} catch (IOException ignored) {
			}
		});

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'Hello World'",
						resp -> resp.contains("Hello World"))
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
	}

	protected void configureContextProcessing(Dictionary<String, String> props) {
		props.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-helloworld");
		props.put("context.id", "default");
		props.put("login.config.authMethod", "BASIC");
		props.put("login.config.realmName", "default");
		props.put("security.constraint.1.url", "/helloworld/*");
		props.put("security.constraint.1.roles", "role-manager, content-administrator");
	}

	/**
	 * Due to incomplete configuration, Undertow returns HTTP 401, while Jetty and Tomcat - 403.
	 * I'll investigate later...
	 *
	 * @throws Exception
	 */
	protected void performSecurityAssertion() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/helloworld/hs");
	}

	protected void waitForReRegistration(Runnable trigger) {
		trigger.run();
		try {
			// just in case - there's no event we could catch on restart of target context.
			Thread.sleep(200);
			new WaitCondition("Waiting for deployment") {
				@Override
				protected boolean isFulfilled() {
					try {
						String result = HttpTestClientFactory.createDefaultTestClient()
								.withReturnCode((int[]) null)
								.doGETandExecuteTest("http://127.0.0.1:8181/alt-images/logo.png");
						byte[] bytes = result.getBytes();
						return bytes.length > 4 && bytes[2] == (byte) 'P' && bytes[3] == (byte) 'N' && bytes[4] == (byte) 'G';
					} catch (Exception e) {
						throw new RuntimeException(e.getMessage(), e);
					}
				}
			}.waitForCondition(5000, SLEEP_DURATION_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
