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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.ops4j.pax.web.itest.container.AbstractContainerTestBase;
import org.ops4j.pax.web.itest.utils.client.HttpTestClientFactory;
import org.ops4j.pax.web.itest.utils.web.FilterBundleActivator;
import org.ops4j.pax.web.itest.utils.web.ServletBundleActivator;
import org.ops4j.pax.web.itest.utils.web.SimpleOnlyFilter;
import org.ops4j.pax.web.itest.utils.web.TestServlet;
import org.ops4j.store.Store;
import org.ops4j.store.StoreFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * @author Achim Nierbeck (anierbeck)
 * @since Dec 30, 2012
 */
public abstract class AbstractSharedContextFilterIntegrationTest extends AbstractContainerTestBase {

	protected static final String SERVLET_BUNDLE = "ServletBundleTest";
	protected static final String FILTER_BUNDLE = "FilterBundleTest";

	@Override
	protected Option[] baseConfigure() {
		new File("target/bundles").mkdirs();
		try {
			InputStream b1 = TinyBundles.bundle()
					.addClass(TestServlet.class)
					.addClass(ServletBundleActivator.class)
					.setHeader(Constants.BUNDLE_SYMBOLICNAME, SERVLET_BUNDLE)
					.setHeader(Constants.BUNDLE_ACTIVATOR, ServletBundleActivator.class.getName())
					.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*")
					.build(TinyBundles.rawBuilder());
			Store<InputStream> store = StoreFactory.anonymousStore();
			File bundle1 = new File("target/bundles/b1.jar");
			bundle1.delete();
			Files.copy(b1, bundle1.toPath());
			b1.close();

			InputStream b2 = TinyBundles.bundle()
					.addClass(SimpleOnlyFilter.class)
					.addClass(FilterBundleActivator.class)
					.setHeader(Constants.BUNDLE_SYMBOLICNAME, FILTER_BUNDLE)
					.setHeader(Constants.BUNDLE_ACTIVATOR, FilterBundleActivator.class.getName())
					.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*")
					.build(TinyBundles.rawBuilder());
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
		configureAndWaitForFilterWithMapping("/sharedContext/*", () -> {
			Bundle b1 = context.installBundle(new File("target/bundles/b1.jar").toURI().toURL().toString());
			b1.start();
			Bundle b2 = context.installBundle(new File("target/bundles/b2.jar").toURI().toURL().toString());
			b2.start();
		});
	}

	@After
	public void tearDown() throws Exception {
		for (final Bundle b : context.getBundles()) {
			if (FILTER_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
			if (SERVLET_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}
	}

	@Test
	public void testBundle1() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
	}

	@Test
	public void testStop() throws Exception {
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'Hello Whiteboard Filter'",
						resp -> resp.contains("Hello Whiteboard Filter"))
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");

		for (final Bundle b : context.getBundles()) {
			if (FILTER_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'SimpleServlet: TEST OK'",
						resp -> resp.contains("SimpleServlet: TEST OK"))
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
	}

	@Test
	public void testStopServletBundle() throws Exception {
		for (final Bundle b : context.getBundles()) {
			if (SERVLET_BUNDLE.equalsIgnoreCase(b.getSymbolicName())) {
				b.stop();
			}
		}

		HttpTestClientFactory.createDefaultTestClient()
				.withReturnCode(404)
				.doGETandExecuteTest("http://127.0.0.1:8181/sharedContext/");
	}

}
