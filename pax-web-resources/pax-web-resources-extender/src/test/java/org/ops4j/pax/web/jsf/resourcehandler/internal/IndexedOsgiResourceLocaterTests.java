/* Copyright 2016 Marc Schlegel
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
package org.ops4j.pax.web.jsf.resourcehandler.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.resources.extender.internal.IndexedOsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.web.jsf.resourcehandler.internal.OsgiResourceMatcher.isBundleResource;

public class IndexedOsgiResourceLocaterTests {

	private IndexedOsgiResourceLocator sut;
	private BundleContext context;
	private Bundle resourceBundleOne;
	private Bundle resourceBundleTwo;

	@Before
	public void setUp() {
		context = mock(BundleContext.class);
		resourceBundleOne = new BundleBuilder().withBundleId(100).withSymbolicName("resourcebundle-one")
				.buildWithResources("template.html", "base.css");
		resourceBundleTwo = new BundleBuilder().withBundleId(100).withSymbolicName("resourcebundle-two")
				.buildWithResources(
						"footer.html",
						"js/some.js",
						"folder/subfolder/a.js",
						"folder/subfolder/b.js",
						"folder/bla/a.js",
						"en/libraryname/test.css/2_4.css");

		sut = new IndexedOsgiResourceLocator(context);
		sut.register(resourceBundleOne);
		sut.register(resourceBundleTwo);
	}

	@Test
	public void resourcesAvailable() {

		assertThat("Resource doesn't match!", sut.locateResource("template.html"), isBundleResource(resourceBundleOne, "template.html"));
		assertThat("Resource doesn't match!", sut.locateResource("base.css"), isBundleResource(resourceBundleOne, "base.css"));
		assertThat("Resource doesn't match!", sut.locateResource("footer.html"), isBundleResource(resourceBundleTwo, "footer.html"));
		assertThat("Resource doesn't match!", sut.locateResource("js/some.js"), isBundleResource(resourceBundleTwo, "js/some.js"));
	}


	@Test
	public void resourceOverride() {
		Bundle overridingBundle = new BundleBuilder().withSymbolicName("resourcebundle-override")
				.buildWithResources("another.html", "template.html");
		when(context.getBundle(resourceBundleOne.getBundleId())).thenReturn(resourceBundleOne);

		sut.register(overridingBundle);

		assertThat("Resource doesn't match!", sut.locateResource("template.html"), isBundleResource(overridingBundle, "template.html"));
		assertThat("Resource doesn't match!", sut.locateResource("another.html"), isBundleResource(overridingBundle, "another.html"));
		assertThat("Resource doesn't match!", sut.locateResource("base.css"), isBundleResource(resourceBundleOne, "base.css"));
		assertThat("Resource doesn't match!", sut.locateResource("footer.html"), isBundleResource(resourceBundleTwo, "footer.html"));
		assertThat("Resource doesn't match!", sut.locateResource("js/some.js"), isBundleResource(resourceBundleTwo, "js/some.js"));
	}

	@Test
	public void resourceOverrideUninstalled() {
		Bundle overridingBundle = new BundleBuilder().withSymbolicName("resourcebundle-override")
				.buildWithResources("another.html", "template.html");
		when(context.getBundle(resourceBundleOne.getBundleId())).thenReturn(resourceBundleOne);

		sut.register(overridingBundle);
		sut.unregister(overridingBundle);

		// template.html must now be served from resourcebundle-one again
		assertThat("Resource doesn't match!", sut.locateResource("template.html"), isBundleResource(resourceBundleOne, "template.html"));
	}

	private static class BundleBuilder {

		private Long bundleId = generateBundleId();
		private String symbolicName = UUID.randomUUID().toString();

		private BundleBuilder withBundleId(int bundleId) {
			this.bundleId = (long) bundleId;
			return this;
		}

		private BundleBuilder withSymbolicName(String symbolicName) {
			this.symbolicName = symbolicName;
			return this;
		}

		private Bundle buildWithResources(String... resources) {
			Bundle bundle = mock(Bundle.class);
			when(bundle.getBundleId()).thenReturn(bundleId);
			when(bundle.getSymbolicName()).thenReturn(symbolicName);
			when(bundle.findEntries("/META-INF/resources/", "*.*", true))
					.thenReturn(Collections.enumeration(addResourceTestdataToMock(resources)));
			return bundle;
		}

		private Long generateBundleId() {
			long lowerRange = 0; // assign lower range value
			long upperRange = 1000000; // assign upper range value
			Random random = new Random();

			return lowerRange + (long) (random.nextDouble() * (upperRange - lowerRange));
		}

		private Collection<URL> addResourceTestdataToMock(String... paths) {
			Collection<URL> urls = new ArrayList<>(paths.length);
			for (String path : paths) {
				try {
					urls.add(new URL("file://" + bundleId + ".0:0/META-INF/resources/" + path));
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			return urls;
		}

	}

}
