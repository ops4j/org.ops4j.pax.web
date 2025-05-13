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
package org.ops4j.pax.web.itest.container.whiteboard;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.ops4j.pax.web.extender.samples.whiteboard.internal.WhiteboardFilter;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.TestActivator;
import org.ops4j.store.Store;
import org.ops4j.store.StoreFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
public abstract class AbstractSimultaneousWhiteboardIntegrationTest extends AbstractContainerTestBase {

	@Override
	protected Option[] baseConfigure() {
		File dir = new File("target/bundles");
		try {
			if (dir.isDirectory()) {
				FileUtils.cleanDirectory(dir);
			}
			dir.mkdirs();
			InputStream b1 = TinyBundles.bundle().addClass(TestActivator.class)
					.addClass(WhiteboardFilter.class)
					.setHeader(Constants.BUNDLE_ACTIVATOR, TestActivator.class.getName())
					.setHeader(Constants.BUNDLE_SYMBOLICNAME, "org.ops4j.pax.web.itest.SimultaneousTest")
					.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*")
					.build(TinyBundles.rawBuilder());
			Store<InputStream> store = StoreFactory.anonymousStore();
			File bundle1 = new File(dir, "b1.jar");
			Files.copy(b1, bundle1.toPath());
			b1.close();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return super.baseConfigure();
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForServletWithMapping("/", () -> {
			Bundle b1 = context.installBundle(new File("target/bundles/b1.jar").toURI().toURL().toString());
			b1.start();
			Bundle b2 = context.installBundle(sampleURI("whiteboard"));
			b2.start();
		});
	}

	@After
	public void tearDown() throws Exception {
		for (final Bundle b : context.getBundles()) {
			if ("org.ops4j.pax.web.itest.SimultaneousTest".equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
			if ("org.ops4j.pax.web.samples.whiteboard".equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}
	}

	@Test
	public void testWhiteBoardRoot() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Extender'",
						resp -> resp.contains("Hello Whiteboard Extender"))
				.doGETandExecuteTest("http://127.0.0.1:8181/root");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to the Welcome page'",
						resp -> resp.contains("Welcome to the Welcome page"))
				.doGETandExecuteTest("http://127.0.0.1:8181/");
	}

}
