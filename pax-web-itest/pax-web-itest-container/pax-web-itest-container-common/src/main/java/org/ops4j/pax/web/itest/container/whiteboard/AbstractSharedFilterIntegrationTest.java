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

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.Bundle1Activator;
import org.ops4j.pax.web.itest.utils.web.Bundle1Filter;
import org.ops4j.pax.web.itest.utils.web.Bundle1Servlet;
import org.ops4j.pax.web.itest.utils.web.Bundle1SharedFilter;
import org.ops4j.pax.web.itest.utils.web.Bundle2Activator;
import org.ops4j.pax.web.itest.utils.web.Bundle2SharedFilter;
import org.ops4j.pax.web.itest.utils.web.Bundle2SharedServlet;
import org.ops4j.store.Store;
import org.ops4j.store.StoreFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractSharedFilterIntegrationTest extends AbstractContainerTestBase {

	@Override
	protected Option[] baseConfigure() {
		File dir = new File("target/bundles");
		dir.mkdirs();
		try {
			InputStream b1 = TinyBundles.bundle()
					.addClass(Bundle1Servlet.class)
					.addClass(Bundle1Filter.class)
					.addClass(Bundle1SharedFilter.class)
					.addClass(Bundle1Activator.class)
					.setHeader(Constants.BUNDLE_SYMBOLICNAME, "BundleTest1")
					.setHeader(Constants.BUNDLE_ACTIVATOR, Bundle1Activator.class.getName())
					.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*").build();
			Store<InputStream> store = StoreFactory.anonymousStore();
			File bundle1 = new File("target/bundles/b1.jar");
			bundle1.delete();
			Files.copy(b1, bundle1.toPath());
			b1.close();

			InputStream b2 = TinyBundles.bundle()
					.addClass(Bundle2SharedServlet.class)
					.addClass(Bundle2SharedFilter.class)
					.addClass(Bundle2Activator.class)
					.setHeader(Constants.BUNDLE_SYMBOLICNAME, "BundleTest2")
					.setHeader(Constants.BUNDLE_ACTIVATOR, Bundle2Activator.class.getName())
					.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*").build();
			File bundle2 = new File("target/bundles/b2.jar");
			bundle2.delete();
			Files.copy(b2, bundle2.toPath());
			b2.close();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		return super.baseConfigure();
	}

	@Before
	public void setUp() throws Exception {
		configureAndWaitForFilterWithMapping("/*", () -> {
			Bundle b1 = context.installBundle(new File("target/bundles/b1.jar").toURI().toURL().toString());
			b1.start();
			Bundle b2 = context.installBundle(new File("target/bundles/b2.jar").toURI().toURL().toString());
			b2.start();
		});
	}

	@Test
	public void testBundle1() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Welcome to Bundle1'",
						resp -> resp.contains("Welcome to Bundle1"))
				.doGETandExecuteTest("http://127.0.0.1:8181/bundle1/");
		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/bundle2/");
	}

}
