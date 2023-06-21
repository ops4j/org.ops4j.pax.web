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
import jakarta.servlet.http.HttpServletResponse;

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

public abstract class AbstractDoubleHttpServiceProcessingIntegrationTest extends AbstractContainerTestBase {

	private Bundle bundle1;
	private Bundle bundle2;
	private Bundle bundle3;

	@Inject
	private ConfigurationAdmin caService;

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/test1/*",
				() -> bundle1 = installAndStartBundle(sampleURI("hs-1")));
		configureAndWaitForServletWithMapping("/test2/*",
				() -> bundle2 = installAndStartBundle(sampleURI("hs-2")));
		configureAndWaitForServletWithMapping("/whiteboard/*",
				() -> bundle3 = installAndStartBundle(sampleURI("whiteboard-1")));
	}

	@After
	public void tearDown() throws BundleException {
		if (bundle1 != null) {
			bundle1.stop();
			bundle1.uninstall();
		}
		if (bundle2 != null) {
			bundle2.stop();
			bundle2.uninstall();
		}
		if (bundle3 != null) {
			bundle3.stop();
			bundle3.uninstall();
		}
	}

	@Test
	public void testContextAltering() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");

		Configuration cfg1 = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props = new Hashtable<>();
		props.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-1");
		props.put("context.id", "default");
		props.put("login.config.authMethod", "BASIC");
		props.put("login.config.realmName", "default");
		props.put("security.constraint.1.url", "/test1/safe/*");
		props.put("security.constraint.1.roles", "role-manager, content-administrator");

		LOG.info("==== Creating security config for hs-1");

		waitForReRegistration(() -> {
			try {
				cfg1.update(props);
			} catch (IOException ignored) {
			}
		}, "test1", HttpServletResponse.SC_UNAUTHORIZED);

		LOG.info("==== Checking after securing hs-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");

		LOG.info("==== Creating security config for hs-2");

		Configuration cfg2 = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props2 = new Hashtable<>();
		props2.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-2");
		props2.put("context.id", "default");
		props2.put("login.config.authMethod", "BASIC");
		props2.put("login.config.realmName", "default");
		props2.put("security.constraint.1.url", "/test2/safe/*");
		props2.put("security.constraint.1.roles", "role-manager, content-administrator");

		waitForReRegistration(() -> {
			try {
				cfg2.update(props2);
			} catch (IOException ignored) {
			}
		}, "test2", HttpServletResponse.SC_UNAUTHORIZED);

		LOG.info("==== Checking after securing hs-2");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");

		LOG.info("==== Removing security config for hs-1");

		waitForReRegistration(() -> {
			try {
				cfg1.delete();
			} catch (IOException ignored) {
			}
		}, "test1", HttpServletResponse.SC_OK);

		LOG.info("==== Checking after unsecuring hs-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");

		LOG.info("==== Uninstalling hs-1");

		waitForReRegistration(() -> {
			try {
				bundle1.stop();
				bundle1.uninstall();
				bundle1 = null;
			} catch (Exception ignored) {
			}
		}, "test1", HttpServletResponse.SC_NOT_FOUND);

		LOG.info("==== Checking after uninstallation of hs-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/unsafe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");

		LOG.info("==== Removing security config for hs-2");

		waitForReRegistration(() -> {
			try {
				cfg2.delete();
			} catch (IOException ignored) {
			}
		}, "test2", HttpServletResponse.SC_OK);

		LOG.info("==== Checking after unsecuring hs-2");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
	}

	@Test
	public void testContextAlteringAndBundleStopping() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'whiteboard1'",
						resp -> resp.contains("whiteboard1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		Configuration cfg1 = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props = new Hashtable<>();
		props.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-1");
		props.put("context.id", "default");
		props.put("login.config.authMethod", "BASIC");
		props.put("login.config.realmName", "default");
		props.put("security.constraint.1.url", "/test1/safe/*");
		props.put("security.constraint.1.roles", "role-manager, content-administrator");

		LOG.info("==== Creating security config for hs-1");

		waitForReRegistration(() -> {
			try {
				cfg1.update(props);
			} catch (IOException ignored) {
			}
		}, "test1", HttpServletResponse.SC_UNAUTHORIZED);

		LOG.info("==== Checking after securing hs-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'whiteboard1'",
						resp -> resp.contains("whiteboard1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		LOG.info("==== Creating security config for hs-2");

		Configuration cfg2 = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props2 = new Hashtable<>();
		props2.put("bundle.symbolicName", "org.ops4j.pax.web.samples.hs-2");
		props2.put("context.id", "default");
		props2.put("login.config.authMethod", "BASIC");
		props2.put("login.config.realmName", "default");
		props2.put("security.constraint.1.url", "/test2/safe/*");
		props2.put("security.constraint.1.roles", "role-manager, content-administrator");

		waitForReRegistration(() -> {
			try {
				cfg2.update(props2);
			} catch (IOException ignored) {
			}
		}, "test2", HttpServletResponse.SC_UNAUTHORIZED);

		LOG.info("==== Checking after securing hs-2");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'whiteboard1'",
						resp -> resp.contains("whiteboard1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		LOG.info("==== Creating security config for whiteboard-1");

		Configuration cfg3 = caService.createFactoryConfiguration("org.ops4j.pax.web.context", null);
		Dictionary<String, String> props3 = new Hashtable<>();
		props3.put("bundle.symbolicName", "org.ops4j.pax.web.samples.whiteboard-1");
		props3.put("context.id", "custom");
		props3.put("whiteboard", "true");
		props3.put("login.config.authMethod", "BASIC");
		props3.put("login.config.realmName", "default");
		props3.put("security.constraint.1.url", "/whiteboard/*");
		props3.put("security.constraint.1.roles", "role-manager, content-administrator");

		waitForReRegistration(() -> {
			try {
				cfg3.update(props3);
			} catch (IOException ignored) {
			}
		}, "whiteboard", HttpServletResponse.SC_UNAUTHORIZED);

		LOG.info("==== Checking after securing whiteboard-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test1'",
						resp -> resp.contains("test1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'whiteboard1'",
						resp -> resp.contains("whiteboard1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		LOG.info("==== Uninstalling hs-1");

		waitForReRegistration(() -> {
			try {
				bundle1.stop();
				bundle1.uninstall();
				bundle1 = null;
			} catch (Exception ignored) {
			}
		}, "test1", HttpServletResponse.SC_NOT_FOUND);

		LOG.info("==== Checking after uninstallation of hs-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/unsafe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'whiteboard1'",
						resp -> resp.contains("whiteboard1"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		LOG.info("==== Uninstalling whiteboard-1");

		waitForReRegistration(() -> {
			try {
				bundle3.stop();
				bundle3.uninstall();
				bundle3 = null;
			} catch (Exception ignored) {
			}
		}, "whiteboard", HttpServletResponse.SC_NOT_FOUND);

		LOG.info("==== Checking after uninstallation of whiteboard-1");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/unsafe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(401)
				.withResponseHeaderAssertion("Response should contain WWW-Authenticate header",
						headers -> headers.anyMatch(
								header -> header.getKey().equals("WWW-Authenticate")
										&& header.getValue().contains("default")
						)
				)
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.authenticate("manager", "manager", "default")
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");

		LOG.info("==== Removing security config for hs-2");

		waitForReRegistration(() -> {
			try {
				cfg2.delete();
			} catch (IOException ignored) {
			}
		}, "test2", HttpServletResponse.SC_OK);

		LOG.info("==== Checking after unsecuring hs-2");

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/unsafe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/test1/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(200)
				.withResponseAssertion("Response must contain 'test2'",
						resp -> resp.contains("test2"))
				.doGETandExecuteTest("http://127.0.0.1:8181/test2/safe/hello");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/whiteboard");
	}

	protected void waitForReRegistration(Runnable trigger, String bundle, int resultCode) {
		trigger.run();
		try {
			Thread.sleep(200);
			new WaitCondition("Waiting for deployment") {
				@Override
				protected boolean isFulfilled() {
					try {
						String result = HttpTestClientFactory.createDefaultTestClient()
								.withReturnCode(resultCode)
								.doGETandExecuteTest("http://127.0.0.1:8181/" + bundle + "/safe/hello");
						if (resultCode == HttpServletResponse.SC_OK) {
							return result.startsWith(bundle);
						}
						return true;
					} catch (Throwable e) {
						return false;
					}
				}
			}.waitForCondition(5000, SLEEP_DURATION_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
