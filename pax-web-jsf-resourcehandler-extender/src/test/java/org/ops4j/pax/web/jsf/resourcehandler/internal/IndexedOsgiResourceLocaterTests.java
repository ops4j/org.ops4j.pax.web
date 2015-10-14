package org.ops4j.pax.web.jsf.resourcehandler.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.web.jsf.resourcehandler.internal.OsgiResourceMatcher.isBundleResource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.jsf.resourcehandler.extender.internal.IndexedOsgiResourceLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class IndexedOsgiResourceLocaterTests {

	private IndexedOsgiResourceLocator sut;
	private BundleContext context;
	private Bundle resourceBundleOne;
	private Bundle resourceBundleTwo;

	@Before
	public void setUp() throws Exception {
		context = mock(BundleContext.class);
		resourceBundleOne = new BundleBuilder().withBundleId(100).withSymbolicName("resourcebundle-one")
				.buildWithResources("template.html", "base.css");
		resourceBundleTwo = new BundleBuilder().withBundleId(100).withSymbolicName("resourcebundle-two")
				.buildWithResources("footer.html", "js/some.js");

		sut = new IndexedOsgiResourceLocator(context);
		sut.register(resourceBundleOne);
		sut.register(resourceBundleTwo);
	}

	@Test
	public void resourcesAvailable() throws Exception {

		assertThat("Resource doesnt match!", sut.createResource("template.html", null), isBundleResource(resourceBundleOne, "template.html"));
		assertThat("Resource doesnt match!", sut.createResource("base.css", null), isBundleResource(resourceBundleOne, "base.css"));
		assertThat("Resource doesnt match!", sut.createResource("footer.html", null), isBundleResource(resourceBundleTwo, "footer.html"));
		assertThat("Resource doesnt match!", sut.createResource("js/some.js", null), isBundleResource(resourceBundleTwo, "js/some.js"));
	}

	@Test
	public void resourceOverride() throws Exception {
		Bundle overridingBundle = new BundleBuilder().withSymbolicName("resourcebundle-override")
				.buildWithResources("another.html", "template.html");
		when(context.getBundle(resourceBundleOne.getBundleId())).thenReturn(resourceBundleOne);
		
		sut.register(overridingBundle);

		assertThat("Resource doesnt match!", sut.createResource("template.html", null), isBundleResource(overridingBundle, "template.html"));
		assertThat("Resource doesnt match!", sut.createResource("another.html", null), isBundleResource(overridingBundle, "another.html"));
		assertThat("Resource doesnt match!", sut.createResource("base.css", null), isBundleResource(resourceBundleOne, "base.css"));
		assertThat("Resource doesnt match!", sut.createResource("footer.html", null), isBundleResource(resourceBundleTwo, "footer.html"));
		assertThat("Resource doesnt match!", sut.createResource("js/some.js", null), isBundleResource(resourceBundleTwo, "js/some.js"));
	}
	
	@Test
	public void resourceOverrideUninstalled() throws Exception {
		Bundle overridingBundle = new BundleBuilder().withSymbolicName("resourcebundle-override")
				.buildWithResources("another.html", "template.html");
		when(context.getBundle(resourceBundleOne.getBundleId())).thenReturn(resourceBundleOne);
		
		sut.register(overridingBundle);
		sut.unregister(overridingBundle);
		
		// template.html must now be served from resourcebundle-one again
		assertThat("Resource doesnt match!", sut.createResource("template.html", null), isBundleResource(resourceBundleOne, "template.html"));
	}

	private class BundleBuilder {

		private Long bundleId = generateBundleId();
		private String symbolicName = UUID.randomUUID().toString();

		private BundleBuilder withBundleId(int bundleId) {
			this.bundleId = Long.valueOf(bundleId);
			return this;
		}

		private BundleBuilder withSymbolicName(String symbolicName) {
			this.symbolicName = symbolicName;
			return this;
		}

		private Bundle buildWithResources(String... resources) {
			Bundle bundle = mock(Bundle.class);
			when(bundle.getBundleId()).thenReturn(Long.valueOf(bundleId));
			when(bundle.getSymbolicName()).thenReturn(symbolicName);
			when(bundle.findEntries("/META-INF/resources/", "*.*", true))
					.thenReturn(Collections.enumeration(addResourceTestdataToMock(resources)));
			return bundle;
		}

		private Long generateBundleId() {
			long LOWER_RANGE = 0; // assign lower range value
			long UPPER_RANGE = 1000000; // assign upper range value
			Random random = new Random();

			return LOWER_RANGE + (long) (random.nextDouble() * (UPPER_RANGE - LOWER_RANGE));
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
