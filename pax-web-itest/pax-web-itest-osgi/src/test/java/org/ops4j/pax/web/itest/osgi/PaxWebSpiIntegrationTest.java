/*
 * Copyright 2019 OPS4J.
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
package org.ops4j.pax.web.itest.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Manifest;
import javax.servlet.ServletContainerInitializer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.samples.tests.SneakIntoPaxWebSpi;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This test check whether pax-web-spi contains proper private packages and whether the imports are sufficient
 * to perform various tasks related to class/annotation discovery.
 */
@RunWith(PaxExam.class)
public class PaxWebSpiIntegrationTest extends AbstractControlledTestBase {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebSpiIntegrationTest.class);

	@Configuration
	public Option[] configure() {
		return combine(
				combine(baseConfigure(), paxWebCore()),

				// fragment that exports static methods that can test whether pax-web-spi has all that's needed
				// to do it's job (like scanning for resources)
				mavenBundle("org.ops4j.pax.web.samples", "pax-web-spi-fragment").versionAsInProject().noStart(),

				// pax-web-spi itself won't require XBean, but the above fragments still shows how it was used
				// requirement from pax-web-spi
				mavenBundle("org.apache.xbean", "xbean-finder").versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				// xbean-finder requires xbean-bundleutils
				mavenBundle("org.apache.xbean", "xbean-bundleutils").versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				// xbean-finder requires asm and asm-commons, and asm-commons requires asm-tree
				mavenBundle("org.ow2.asm", "asm").versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-commons").versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),
				mavenBundle("org.ow2.asm", "asm-tree").versionAsInProject().startLevel(START_LEVEL_TEST_BUNDLE - 1),

				// bundle and two fragments to perform searching of /META-INF/services/javax.servlet.ServletContainerInitializer
				mavenBundle("org.ops4j.pax.web.samples", "initializers").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "initializers-fragment1").versionAsInProject().noStart(),
				mavenBundle("org.ops4j.pax.web.samples", "initializers-fragment2").versionAsInProject().noStart(),

				// WARs to search their embedded content though Bundle/BundleWiring API
				// pax-web-runtime will later construct ResourceDelegatingBundleClassLoader for them
				mavenBundle("org.ops4j.pax.web.samples", "war-simplest-osgi").type("war").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-embedded").type("war").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons1").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons2").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons3").versionAsInProject()
		);
	}

	@Test
	public void scanningUsingOSGiAPIs() throws IOException {
		Bundle bundle = SneakIntoPaxWebSpi.whatsYourBundle();
		BundleWiring wiring = bundle.adapt(BundleWiring.class);

		assertThat(bundle.getSymbolicName(), equalTo("org.ops4j.pax.web.pax-web-spi"));
		assertNotNull(wiring);
		assertNotNull(wiring.getClassLoader());

		// 3.9.1 Bundle Class Path
		//
		// JAR, ZIP, directories, etc. are called "containers". Containers contain entries organized in hierarchical
		// paths. [...]
		// For a bundle, the search order for a named entry is:
		//  - First the container of the (host) bundle
		//  - Then the (optional) fragment containers in ascending id order
		// This search order is called the "entry path".
		// A resource (or class) is not loaded via the "entry path", but it is loaded through the "bundle class path".
		// The "bundle class path" provides an additional indirection on top of the "entry path". It defines an ordered
		// list of container paths.

		// 7.6 Container Scanning
		// 7.6.1 Bundle Class Path Order
		//    The method used to iterate over the resources in bundle class path order is listResources(String,String,int).
		// 7.6.2 Entry Order
		//    A Bundle Wiring reflects a resolved bundle. This wiring constitutes the bundle and any attached fragments.
		//    The findEntries(String,String,int) method is similar to the Bundle.findEntries(String,String,boolean)
		//    method. The Bundle's method will be identical when the bundle can be resolved, if the bundle cannot
		//    resolve the Bundle's findEntries method has a fallback that allows iteration without attached fragments.
		//    The Bundle Wiring's findEntries(String,String,int) is always against a resolved bundle because it is on a
		//    Bundle Wiring.

		// org.osgi.framework.Bundle.findEntries() - used by org.apache.xbean.osgi.bundle.util.BundleResourceFinder.scanDirectory()
		//    similar to org.osgi.framework.wiring.BundleWiring.findEntries() if the bundle is resolved. if bundle
		//    is not resolved, an attempt to resolve it must be performed
		//  > org.apache.felix.framework.Felix.findBundleEntries(bundle, ...)
		//     > org.apache.felix.framework.Felix.findBundleEntries(revision, ...)
		//        > org.apache.felix.framework.EntryFilterEnumeration recurse=<param>, fragments=true
		// org.osgi.framework.Bundle.getEntry()
		//  > org.apache.felix.framework.Felix.getBundleEntry()
		//     > org.apache.felix.framework.BundleRevisionImpl.getEntry()
		//        > org.apache.felix.framework.cache.Content.hasEntry() == true ? URL : null
		// org.osgi.framework.Bundle.getEntryPaths()
		//  > org.apache.felix.framework.Felix.getBundleEntryPaths()
		//     > org.apache.felix.framework.EntryFilterEnumeration recurse=false, fragments=false
		// org.osgi.framework.Bundle.getResource()
		//  > org.apache.felix.framework.Felix.getBundleResource()
		//     (wiring == null) > org.apache.felix.framework.BundleRevisionImpl.getResourceLocal()
		//      > org.apache.felix.framework.cache.Content.hasEntry() == true ? URL : nu
		//        just as org.osgi.framework.Bundle.getEntry()
		//     (wiring != null) > org.apache.felix.framework.BundleWiringImpl.getResourceByDelegation()
		//        searches classloader, wiring, boot delegation...
		// org.osgi.framework.Bundle.getResources()
		//  > org.apache.felix.framework.Felix.getBundleResources()
		//     (wiring == null) > org.apache.felix.framework.BundleRevisionImpl.getResourcesLocal()
		//     (wiring != null) > org.apache.felix.framework.BundleWiringImpl.getResourcesByDelegation()
		//      > org.apache.felix.framework.BundleWiringImpl.findResourcesByDelegation()
		//
		// org.osgi.framework.wiring.BundleWiring.findEntries()
		//  > org.apache.felix.framework.BundleWiringImpl.findEntries()
		//     > org.apache.felix.framework.Felix.findBundleEntries(revision, ...)
		// org.osgi.framework.wiring.BundleWiring.listResources()
		//  > org.apache.felix.framework.BundleWiringImpl.listResourcesInternal()
		//     > org.apache.felix.framework.BundleRevisionImpl.getContentPath()
		//        > org.apache.felix.framework.BundleWiringImpl.getResourcesByDelegation()

		// org.ops4j.pax.web.utils.ClassPathUtil.findResources() which is called for example as:
		//    ClassPathUtil.findResources(bundlesInClassSpace, "/META-INF", "*.taglib.xml", false)
		// internally calls org.osgi.framework.wiring.BundleWiring.listResources()

		// --- wiring method not involving classloader

		// entries return URLs from the content of bundle wiring's revision's content (yes)
		// and from attached fragments. That's why some entries may be repeated (e.g., /META-INF/MANIFEST.MF)
		List<URL> entries = wiring.findEntries("/META-INF", "*", BundleWiring.FINDENTRIES_RECURSE);

		// entries: java.util.List  = {org.apache.felix.framework.util.ImmutableList@4101}  size = 12
		//  0 = {java.net.URL@4125} "bundle://19.0:0/META-INF/MANIFEST.MF"
		//  1 = {java.net.URL@4126} "bundle://19.0:0/META-INF/maven/"
		//  2 = {java.net.URL@4127} "bundle://19.0:0/META-INF/maven/org.ops4j.pax.web/"
		//  3 = {java.net.URL@4128} "bundle://19.0:0/META-INF/maven/org.ops4j.pax.web/pax-web-spi/"
		//  4 = {java.net.URL@4129} "bundle://19.0:0/META-INF/maven/org.ops4j.pax.web/pax-web-spi/pom.properties"
		//  5 = {java.net.URL@4130} "bundle://19.0:0/META-INF/maven/org.ops4j.pax.web/pax-web-spi/pom.xml"
		//  6 = {java.net.URL@4131} "bundle://25.0:0/META-INF/MANIFEST.MF"
		//  7 = {java.net.URL@4132} "bundle://25.0:0/META-INF/maven/"
		//  8 = {java.net.URL@4133} "bundle://25.0:0/META-INF/maven/org.ops4j.pax.web.samples/"
		//  9 = {java.net.URL@4134} "bundle://25.0:0/META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/"
		//  10 = {java.net.URL@4135} "bundle://25.0:0/META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/pom.properties"
		//  11 = {java.net.URL@4136} "bundle://25.0:0/META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/pom.xml"

		List<Manifest> manifests = new LinkedList<>();
		for (URL entry : entries) {
			LOG.info("Wiring entry: {}", entry);
			if ("MANIFEST.MF".equals(new File(entry.getPath()).getName())) {
				manifests.add(new Manifest(entry.openStream()));
			}
		}
		int ok = 0;
		assertThat(manifests.size(), equalTo(2));
		for (Manifest mf : manifests) {
			if ("org.ops4j.pax.web.pax-web-spi".equals(mf.getMainAttributes().getValue("Bundle-SymbolicName"))) {
				ok++;
			}
			if ("org.ops4j.pax.web.pax-web-spi".equals(mf.getMainAttributes().getValue("Fragment-Host"))) {
				ok++;
			}
		}
		assertThat(ok, equalTo(2));

		// --- wiring method involving classloader

		// listResources() returns resources that can later be loaded using wiring's classloader
		// if the wiring has attached bundles, "first" resource wins
		Collection<String> resources = wiring.listResources("/META-INF", "*", BundleWiring.LISTRESOURCES_RECURSE);

		// resources: java.util.Collection  = {java.util.TreeSet@4102}  size = 10
		//  0 = "META-INF/MANIFEST.MF"
		//  1 = "META-INF/maven/"
		//  2 = "META-INF/maven/org.ops4j.pax.web.samples/"
		//  3 = "META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/"
		//  4 = "META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/pom.properties"
		//  5 = "META-INF/maven/org.ops4j.pax.web.samples/pax-web-spi-fragment/pom.xml"
		//  6 = "META-INF/maven/org.ops4j.pax.web/"
		//  7 = "META-INF/maven/org.ops4j.pax.web/pax-web-spi/"
		//  8 = "META-INF/maven/org.ops4j.pax.web/pax-web-spi/pom.properties"
		//  9 = "META-INF/maven/org.ops4j.pax.web/pax-web-spi/pom.xml"

		manifests = new LinkedList<>();
		for (String resource : resources) {
			LOG.info("Wiring resource: {}", resource);
			if ("MANIFEST.MF".equals(new File(resource).getName())) {
				manifests.add(new Manifest(wiring.getClassLoader().getResourceAsStream(resource)));
			}
		}
		ok = 0;
		assertThat(manifests.size(), equalTo(1));
		for (Manifest mf : manifests) {
			if ("org.ops4j.pax.web.pax-web-spi".equals(mf.getMainAttributes().getValue("Bundle-SymbolicName"))) {
				ok++;
			}
		}
		assertThat(ok, equalTo(1));

		// --- bundle methods not involving classloader

		// the same as wiring.findEntries() - considers fragments if bundle is resolved (i.e., has wiring)
		Enumeration<URL> e = bundle.findEntries("/META-INF", "MANIFEST.*", true);

		ok = 0;
		while (e.hasMoreElements()) {
			URL u = e.nextElement();
			LOG.info("Bundle entry: {}", u);
			ok++;
		}
		assertThat(ok, equalTo(2));

		// - Bundle entry: bundle://19.0:0/META-INF/MANIFEST.MF
		// - Bundle entry: bundle://25.0:0/META-INF/MANIFEST.MF

		Manifest m = new Manifest(bundle.getEntry("/META-INF/MANIFEST.MF").openStream());
		assertThat(m.getMainAttributes().getValue("Bundle-SymbolicName"), equalTo("org.ops4j.pax.web.pax-web-spi"));

		Enumeration<String> e2 = bundle.getEntryPaths("/META-INF");
		ok = 0;
		while (e2.hasMoreElements()) {
			String u = e2.nextElement();
			LOG.info("Bundle entry path: {}", u);
			ok++;
		}
		// it's not recursive, so it should find /META-INF/MANIFEST.MF (resource jar entry) and /META-INF/maven/ (dir jar entry)
		assertThat(ok, equalTo(2));

		// - Bundle entry path: META-INF/MANIFEST.MF
		// - Bundle entry path: META-INF/maven/

		// --- methods involving classloader

		m = new Manifest(bundle.getResource("/META-INF/MANIFEST.MF").openStream());
		assertThat(m.getMainAttributes().getValue("Bundle-SymbolicName"), equalTo("org.ops4j.pax.web.pax-web-spi"));

		e = bundle.getResources("/META-INF/MANIFEST.MF");
		ok = 0;
		while (e.hasMoreElements()) {
			URL u = e.nextElement();
			LOG.info("Bundle resource: {}", u);
			ok++;
		}
		assertThat(ok, equalTo(2));

		// - Bundle resource: bundle://19.0:1/META-INF/MANIFEST.MF
		// - Bundle resource: bundle://19.0:2/META-INF/MANIFEST.MF
	}

	@Test
	public void checkPaxSpiFragment() throws IOException {
		Bundle b = SneakIntoPaxWebSpi.whatsYourBundle();
		assertThat(b.getSymbolicName(), equalTo("org.ops4j.pax.web.pax-web-spi"));
	}

	@Test
	public void searchForServletContainerInitializers() throws Exception {
		Optional<Bundle> initializerBundles = Arrays.stream(context.getBundles())
				.filter(b -> "pax-web-initializers".equals(b.getSymbolicName())).findFirst();
		assertTrue(initializerBundles.isPresent());
		Bundle b = initializerBundles.get();

		List<Class<? extends ServletContainerInitializer>> initializers = SneakIntoPaxWebSpi.findInitializersUsingServiceLoader(b);
		assertThat(initializers.size(), equalTo(3));
		for (Class<? extends ServletContainerInitializer> c : initializers) {
			c.newInstance().onStartup(null, null);
		}

		initializers = SneakIntoPaxWebSpi.findInitializersUsingBundleWiring(b);
		assertThat(initializers.size(), equalTo(3));
		for (Class<? extends ServletContainerInitializer> c : initializers) {
			c.newInstance().onStartup(null, null);
		}

		initializers = SneakIntoPaxWebSpi.findInitializersUsingBundle(b);
		assertThat(initializers.size(), equalTo(3));
		for (Class<? extends ServletContainerInitializer> c : initializers) {
			c.newInstance().onStartup(null, null);
		}
	}

	@Test
	public void searchForWarResources() throws Exception {
		Optional<Bundle> initializerBundles = Arrays.stream(context.getBundles())
				.filter(b -> "org.ops4j.pax.web.samples.jsf-primefaces-embedded".equals(b.getSymbolicName())).findFirst();
		assertTrue(initializerBundles.isPresent());
		Bundle b = initializerBundles.get();

		SneakIntoPaxWebSpi.findFacesConfigs(b);
	}

}
