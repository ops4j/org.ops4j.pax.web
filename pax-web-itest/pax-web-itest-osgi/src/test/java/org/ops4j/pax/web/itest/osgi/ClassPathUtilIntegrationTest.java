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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

/**
 * This test check whether pax-web-spi contains proper private packages and whether the imports are enough
 * to perform various tasks involving xbean-finder and asm bundles (it's mostly about discovery).
 */
@RunWith(PaxExam.class)
public class ClassPathUtilIntegrationTest extends AbstractControlledBase2 {

	public static Logger LOG = LoggerFactory.getLogger(ClassPathUtilIntegrationTest.class);

	@Configuration
	public Option[] configure() {
		return combine(
				combine(baseConfigure(), paxWebCore()),

				// fragment that exports static methods that can test whether pax-web-spi has all that's needed
				// to do it's job (like scanning for resources)
				mavenBundle("org.ops4j.pax.web.samples", "pax-web-spi-fragment").versionAsInProject().noStart(),

				// bundle and two fragments to perform searching of /META-INF/services/javax.servlet.ServletContainerInitializer
				mavenBundle("org.ops4j.pax.web.samples", "initializers").versionAsInProject(),
				mavenBundle("org.ops4j.pax.web.samples", "initializers-fragment1").versionAsInProject().noStart(),
				mavenBundle("org.ops4j.pax.web.samples", "initializers-fragment2").versionAsInProject().noStart(),

				// WARs to search their embedded content through Bundle/BundleWiring API
				// pax-web-runtime will later construct ResourceDelegatingBundleClassLoader for them

				// simple WAR without dependencies:
				mavenBundle("org.ops4j.pax.web.samples", "war-simplest-osgi").type("war").versionAsInProject(),
				// more complex WAR - it has fragment attached and also has two required (Require-Bundle)
				// bundles - one from the WAR itself and another - from the attached fragment. Additionally the WAR
				// includes several JARs in Bundle-ClassPath and, of course, has wired bundles (like servlet-api bundle)
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-embedded").type("war").versionAsInProject(),
				// this is the fragment attached to WAR with additional Require-Bundle
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-embedded-fragment").versionAsInProject().noStart(),
				// bundles required due to Require-Bundle - by WAR ...
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons1").versionAsInProject(),
				// ... and by its fragment
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons2").versionAsInProject(),
				// and a bundle wired with Import-Package
				mavenBundle("org.ops4j.pax.web.samples", "jsf-primefaces-commons3").versionAsInProject()
		);
	}

	/**
	 * Using class loader related methods to scan for resources/entries.
	 * @throws Exception
	 */
	@Test
	public void classLoaderResources() throws Exception {
		Optional<Bundle> jsfPrimefacesEmbedded = Arrays.stream(context.getBundles())
				.filter(b -> "org.ops4j.pax.web.samples.jsf-primefaces-embedded".equals(b.getSymbolicName())).findFirst();
		assertTrue(jsfPrimefacesEmbedded.isPresent());
		Bundle b = jsfPrimefacesEmbedded.get();


		// --- Bundle.getResources() involve class loader

		// Felix:
		// - Bundle.getResources() #1 URL: bundle://30.0:1/conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getResources() #1 URL: bundle://30.0:2/conflicting.properties, source: jsf-primefaces-jar:/
		// - Bundle.getResources() #1 URL: bundle://30.0:10/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - Bundle.getResources() #4 URL: bundle://30.0:1/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getResources() #4 URL: bundle://30.0:2/s1/conflicting.properties, source: jsf-primefaces-jar:/s1
		// - Bundle.getResources() #4 URL: bundle://30.0:10/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - Bundle.getResources() #5 URL: bundle://33.0:1/org/ops4j/pax/web/samples/jsf2/JustSomeClass.class, source: null
		// - Bundle.getResources() #6 URL: bundle://34.0:1/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null
		// Equinox:
		// - Bundle.getResources() #1 URL: bundleresource://30.fwk1715248762/conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getResources() #1 URL: bundleresource://30.fwk1715248762:1/conflicting.properties, source: jsf-primefaces-jar:/
		// - Bundle.getResources() #1 URL: bundleresource://30.fwk1715248762:9/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - Bundle.getResources() #4 URL: bundleresource://30.fwk1715248762/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getResources() #4 URL: bundleresource://30.fwk1715248762:1/s1/conflicting.properties, source: jsf-primefaces-jar:/s1
		// - Bundle.getResources() #4 URL: bundleresource://30.fwk1715248762:9/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - Bundle.getResources() #5 URL: bundleresource://33.fwk1715248762/org/ops4j/pax/web/samples/jsf2/JustSomeClass.class, source: null
		// - Bundle.getResources() #6 URL: bundleresource://34.fwk1715248762/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null

		// observation related to finding conflicting.properties resource:
		// - resource is retrieved from bundles wired using Import-Package only if resource's package is on
		//   Import-Package list. If it is, bundles wired using Require-Bundle and/or own content is NOT checked
		// - if imported bundles are not checked, bundle wired using Require-Bundle is checked, but search continues
		// - local content is checked. local content includes all "roots" from Bundle-ClassPath (like embedded jars)
		//   AND all attached fragments - these constitue internal content of the host bundle
		// - WEB-INF/classes/conflicting.properties is returned because it's one of the "roots" ("WEB-INF/classes")
		//   on Bundle-ClassPath. It's not returned using "WEB-INF/classes/conflicting.properties" name, only
		//   "conflicting.properties" name works
		// - "WEB-INF/conflicting.properties" doesn't work either, due to the same reason

		Enumeration<URL> resources1 = b.getResources("conflicting.properties");
		while (resources1 != null && resources1.hasMoreElements()) {
			URL url = resources1.nextElement();
			LOG.info("Bundle.getResources() #1 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources2 = b.getResources("WEB-INF/classes/conflicting.properties");
		while (resources2 != null && resources2.hasMoreElements()) {
			URL url = resources2.nextElement();
			LOG.info("Bundle.getResources() #2 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources3 = b.getResources("WEB-INF/conflicting.properties");
		while (resources3 != null && resources3.hasMoreElements()) {
			URL url = resources3.nextElement();
			LOG.info("Bundle.getResources() #3 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources4 = b.getResources("s1/conflicting.properties");
		while (resources4 != null && resources4.hasMoreElements()) {
			URL url = resources4.nextElement();
			LOG.info("Bundle.getResources() #4 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources5 = b.getResources("org/ops4j/pax/web/samples/jsf2/JustSomeClass.class");
		while (resources5 != null && resources5.hasMoreElements()) {
			URL url = resources5.nextElement();
			LOG.info("Bundle.getResources() #5 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources6 = b.getResources("org/ops4j/pax/web/samples/jsf3/JustSomeClass.class");
		while (resources6 != null && resources6.hasMoreElements()) {
			URL url = resources6.nextElement();
			LOG.info("Bundle.getResources() #6 URL: {}, source: {}", url, read(url, "property.from"));
		}

		// --- ClassLoader.getResources()

		// Felix:
		// - ClassLoader.getResources() #1 URL: bundle://30.0:1/conflicting.properties, source: WAR:src/main/resources
		// - ClassLoader.getResources() #1 URL: bundle://30.0:2/conflicting.properties, source: jsf-primefaces-jar:/
		// - ClassLoader.getResources() #1 URL: bundle://30.0:10/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - ClassLoader.getResources() #4 URL: bundle://30.0:1/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - ClassLoader.getResources() #4 URL: bundle://30.0:2/s1/conflicting.properties, source: jsf-primefaces-jar:/s1
		// - ClassLoader.getResources() #4 URL: bundle://30.0:10/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - ClassLoader.getResources() #5 URL: bundle://33.0:1/org/ops4j/pax/web/samples/jsf2/JustSomeClass.class, source: null
		// - ClassLoader.getResources() #6 URL: bundle://34.0:1/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null
		// Equinox:
		// - ClassLoader.getResources() #1 URL: bundleresource://30.fwk485937598/conflicting.properties, source: WAR:src/main/resources
		// - ClassLoader.getResources() #1 URL: bundleresource://30.fwk485937598:1/conflicting.properties, source: jsf-primefaces-jar:/
		// - ClassLoader.getResources() #1 URL: bundleresource://30.fwk485937598:9/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - ClassLoader.getResources() #4 URL: bundleresource://30.fwk485937598/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - ClassLoader.getResources() #4 URL: bundleresource://30.fwk485937598:1/s1/conflicting.properties, source: jsf-primefaces-jar:/s1
		// - ClassLoader.getResources() #4 URL: bundleresource://30.fwk485937598:9/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - ClassLoader.getResources() #5 URL: bundleresource://33.fwk485937598/org/ops4j/pax/web/samples/jsf2/JustSomeClass.class, source: null
		// - ClassLoader.getResources() #6 URL: bundleresource://34.fwk485937598/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null

		// exactly the same as Bundle.getResources() - calls BundleWiringImpl.getResourcesByDelegation()

		BundleWiring wiring = b.adapt(BundleWiring.class);
		ClassLoader cl = wiring.getClassLoader();

		Enumeration<URL> resources1b = cl.getResources("conflicting.properties");
		while (resources1b != null && resources1b.hasMoreElements()) {
			URL url = resources1b.nextElement();
			LOG.info("ClassLoader.getResources() #1 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources2b = b.getResources("WEB-INF/classes/conflicting.properties");
		while (resources2b != null && resources2b.hasMoreElements()) {
			URL url = resources2b.nextElement();
			LOG.info("ClassLoader.getResources() #2 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources3b = b.getResources("WEB-INF/conflicting.properties");
		while (resources3b != null && resources3b.hasMoreElements()) {
			URL url = resources3b.nextElement();
			LOG.info("ClassLoader.getResources() #3 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources4b = b.getResources("s1/conflicting.properties");
		while (resources4b != null && resources4b.hasMoreElements()) {
			URL url = resources4b.nextElement();
			LOG.info("ClassLoader.getResources() #4 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources5b = b.getResources("org/ops4j/pax/web/samples/jsf2/JustSomeClass.class");
		while (resources5b != null && resources5b.hasMoreElements()) {
			URL url = resources5b.nextElement();
			LOG.info("ClassLoader.getResources() #5 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> resources6b = b.getResources("org/ops4j/pax/web/samples/jsf3/JustSomeClass.class");
		while (resources6b != null && resources6b.hasMoreElements()) {
			URL url = resources6b.nextElement();
			LOG.info("ClassLoader.getResources() #6 URL: {}, source: {}", url, read(url, "property.from"));
		}

		// --- BundleWiring.listResources() involve class loader

		// Felix:
		// - BundleWiring.listResources() #1 Name: conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.listResources() #1 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #1 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #2 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #2 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #3 Name: conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.listResources() #3 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #3 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #5 Name: org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null
		// Equinox:
		// - BundleWiring.listResources() #1 Name: conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.listResources() #1 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #1 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #2 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #2 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #3 Name: conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.listResources() #3 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.listResources() #3 Name: s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.listResources() #5 Name: org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null

		// observation:
		// - BundleWiring.listResources() returns names instead of URLs and Collection instead of Enumeration, so
		//   duplicate names are removed and there's no way to tell which actual resource will be returned
		// - returned name can be used as argument to BundleWiring.getClassLoader().getResource(name)

		Collection<String> resources1a = wiring.listResources("", "conflicting.properties", BundleWiring.LISTRESOURCES_RECURSE);
		for (String name : resources1a) {
			LOG.info("BundleWiring.listResources() #1 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Collection<String> resources2a = wiring.listResources("s1", "conflicting.properties", BundleWiring.LISTRESOURCES_RECURSE);
		for (String name : resources2a) {
			LOG.info("BundleWiring.listResources() #2 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Collection<String> resources3a = wiring.listResources("", "conflicting.properties", BundleWiring.LISTRESOURCES_RECURSE | BundleWiring.LISTRESOURCES_LOCAL);
		for (String name : resources3a) {
			LOG.info("BundleWiring.listResources() #3 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Collection<String> resources4a = wiring.listResources("org/ops4j/pax/web/samples/jsf3", "JustSomeClass.class", BundleWiring.LISTRESOURCES_LOCAL);
		for (String name : resources4a) {
			LOG.info("BundleWiring.listResources() #4 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Collection<String> resources5a = wiring.listResources("org/ops4j/pax/web/samples/jsf3", "JustSomeClass.class", 0);
		for (String name : resources5a) {
			LOG.info("BundleWiring.listResources() #5 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		// --- Bundle.getResource()

		// Felix:
		// - Bundle.getResource() #1: bundle://30.0:1/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getResource() #2: bundle://34.0:1/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null
		// Equinox:
		// - Bundle.getResource() #1: bundleresource://30.fwk1715248762/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getResource() #2: bundleresource://34.fwk1715248762/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null

		URL url1 = b.getResource("s1/conflicting.properties");
		LOG.info("Bundle.getResource() #1: {}, source: {}", url1, read(url1, "property.from"));

		URL url2 = b.getResource("org/ops4j/pax/web/samples/jsf3/JustSomeClass.class");
		LOG.info("Bundle.getResource() #2: {}, source: {}", url2, read(url2, "property.from"));

		// --- ClassLoader.getResource

		// Felix:
		// - ClassLoader.getResource() #1: bundle://30.0:1/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - ClassLoader.getResource() #2: bundle://34.0:1/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null
		// Equinox:
		// - ClassLoader.getResource() #1: bundleresource://30.fwk1715248762/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - ClassLoader.getResource() #2: bundleresource://34.fwk1715248762/org/ops4j/pax/web/samples/jsf3/JustSomeClass.class, source: null

		URL url1a = cl.getResource("s1/conflicting.properties");
		LOG.info("ClassLoader.getResource() #1: {}, source: {}", url1a, read(url1, "property.from"));

		URL url2a = cl.getResource("org/ops4j/pax/web/samples/jsf3/JustSomeClass.class");
		LOG.info("ClassLoader.getResource() #2: {}, source: {}", url2a, read(url2, "property.from"));
	}

	/**
	 * Using methods not referring to class loaders when scanning for resources/entries.
	 * @throws Exception
	 */
	@Test
	public void nonClassLoaderResources() throws Exception {
		Optional<Bundle> jsfPrimefacesEmbedded = Arrays.stream(context.getBundles())
				.filter(b -> "org.ops4j.pax.web.samples.jsf-primefaces-embedded".equals(b.getSymbolicName())).findFirst();
		assertTrue(jsfPrimefacesEmbedded.isPresent());
		Bundle b = jsfPrimefacesEmbedded.get();
		BundleWiring wiring = b.adapt(BundleWiring.class);

		// --- Bundle.findEntries()

		// Felix:
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/conflicting.properties, source: WAR:src/main/resources
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/conflicting.properties, source: WAR:/WEB-INF
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/WEB-INF/s1/s2/conflicting.properties, source: WAR:/WEB-INF/s1/s2
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/conflicting.properties, source: WAR:/
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.findEntries() #1 URL: bundle://30.0:0/s1/s2/conflicting.properties, source: WAR:/s1/s2
		// - Bundle.findEntries() #1 URL: bundle://31.0:0/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - Bundle.findEntries() #1 URL: bundle://31.0:0/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - Bundle.findEntries() #1 URL: bundle://31.0:0/s1/s2/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1/s2
		// - Bundle.findEntries() #2 URL: bundle://30.0:0/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.findEntries() #2 URL: bundle://31.0:0/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// Equinox:
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/classes/conflicting.properties, source: WAR:src/main/resources
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/classes/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/classes/s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/conflicting.properties, source: WAR:/WEB-INF
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/WEB-INF/s1/s2/conflicting.properties, source: WAR:/WEB-INF/s1/s2
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/conflicting.properties, source: WAR:/
		// - Bundle.findEntries() #1 URL: bundleentry://31.fwk2035616217/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.findEntries() #1 URL: bundleentry://31.fwk2035616217/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - Bundle.findEntries() #1 URL: bundleentry://30.fwk2035616217/s1/s2/conflicting.properties, source: WAR:/s1/s2
		// - Bundle.findEntries() #1 URL: bundleentry://31.fwk2035616217/s1/s2/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1/s2
		// - Bundle.findEntries() #2 URL: bundleentry://30.fwk2035616217/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.findEntries() #2 URL: bundleentry://31.fwk2035616217/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1

		// observation:
		// - no Bundle-ClassPath is checked, resources are looked up directly in the bundle content
		// - attached fragments are treated as additional contents

		Enumeration<URL> urls1 = b.findEntries("", "conflicting.properties", true);
		while (urls1 != null && urls1.hasMoreElements()) {
			URL url = urls1.nextElement();
			LOG.info("Bundle.findEntries() #1 URL: {}, source: {}", url, read(url, "property.from"));
		}

		Enumeration<URL> urls2 = b.findEntries("s1", "conflicting.properties", false);
		while (urls2 != null && urls2.hasMoreElements()) {
			URL url = urls2.nextElement();
			LOG.info("Bundle.findEntries() #2 URL: {}, source: {}", url, read(url, "property.from"));
		}

		// --- Bundle.getEntryPaths()

		// Felix:
		// - Bundle.getEntryPaths() #1 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getEntryPaths() #1 Name: s1/s2/, source: null
		// - Bundle.getEntryPaths() #2 Name: META-INF/, source: null
		// - Bundle.getEntryPaths() #2 Name: WEB-INF/, source: <null>
		// - Bundle.getEntryPaths() #2 Name: conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getEntryPaths() #2 Name: helloWorld.xhtml, source: <null>
		// - Bundle.getEntryPaths() #2 Name: s1/, source: null
		// - Bundle.getEntryPaths() #2 Name: success.xhtml, source: <null>
		// - Bundle.getEntryPaths() #3 Name: META-INF/, source: null
		// - Bundle.getEntryPaths() #3 Name: WEB-INF/, source: <null>
		// - Bundle.getEntryPaths() #3 Name: conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getEntryPaths() #3 Name: helloWorld.xhtml, source: <null>
		// - Bundle.getEntryPaths() #3 Name: s1/, source: null
		// - Bundle.getEntryPaths() #3 Name: success.xhtml, source: <null>
		// Equinox:
		// - Bundle.getEntryPaths() #1 Name: s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - Bundle.getEntryPaths() #1 Name: s1/s2/, source: null
		// - Bundle.getEntryPaths() #2 Name: META-INF/, source: null
		// - Bundle.getEntryPaths() #2 Name: WEB-INF/, source: <null>
		// - Bundle.getEntryPaths() #2 Name: conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getEntryPaths() #2 Name: helloWorld.xhtml, source: <null>
		// - Bundle.getEntryPaths() #2 Name: s1/, source: null
		// - Bundle.getEntryPaths() #2 Name: success.xhtml, source: <null>
		// - Bundle.getEntryPaths() #3 Name: META-INF/, source: null
		// - Bundle.getEntryPaths() #3 Name: WEB-INF/, source: <null>
		// - Bundle.getEntryPaths() #3 Name: conflicting.properties, source: WAR:src/main/resources
		// - Bundle.getEntryPaths() #3 Name: helloWorld.xhtml, source: <null>
		// - Bundle.getEntryPaths() #3 Name: s1/, source: null
		// - Bundle.getEntryPaths() #3 Name: success.xhtml, source: <null>

		// observation:
		// - same as org.osgi.framework.Bundle.findEntries() except:
		//    - no recursion
		//    - "*" pattern assumed
		//    - no conversion of entry names to URLs
		// - this method can be used to list directories inside bundle

		Enumeration<String> names1 = b.getEntryPaths("s1");
		while (names1 != null && names1.hasMoreElements()) {
			String name = names1.nextElement();
			LOG.info("Bundle.getEntryPaths() #1 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Enumeration<String> names2 = b.getEntryPaths("/");
		while (names2 != null && names2.hasMoreElements()) {
			String name = names2.nextElement();
			LOG.info("Bundle.getEntryPaths() #2 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		Enumeration<String> names3 = b.getEntryPaths("");
		while (names3 != null && names3.hasMoreElements()) {
			String name = names3.nextElement();
			LOG.info("Bundle.getEntryPaths() #3 Name: {}, source: {}", name, read(name, wiring, "property.from"));
		}

		// --- BundleWiring.findEntries()

		// Felix:
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/classes/s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/conflicting.properties, source: WAR:/WEB-INF
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/WEB-INF/s1/s2/conflicting.properties, source: WAR:/WEB-INF/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/conflicting.properties, source: WAR:/
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/s1/conflicting.properties, source: WAR:/s1
		// - BundleWiring.findEntries() #1 URL: bundle://30.0:0/s1/s2/conflicting.properties, source: WAR:/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundle://31.0:0/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - BundleWiring.findEntries() #1 URL: bundle://31.0:0/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - BundleWiring.findEntries() #1 URL: bundle://31.0:0/s1/s2/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1/s2
		// - BundleWiring.findEntries() #2 URL: bundle://30.0:0/s1/conflicting.properties, source: WAR:/s1
		// - BundleWiring.findEntries() #2 URL: bundle://31.0:0/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - BundleWiring.findEntries() #3 URL: bundle://31.0:0/META-INF/maven/org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment/pom.properties, source: jsf-primefaces-embedded-fragment
		// Equinox:
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/classes/conflicting.properties, source: WAR:src/main/resources
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/classes/s1/conflicting.properties, source: WAR:src/main/resources/s1
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/classes/s1/s2/conflicting.properties, source: WAR:src/main/resources/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/conflicting.properties, source: WAR:/WEB-INF
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/WEB-INF/s1/s2/conflicting.properties, source: WAR:/WEB-INF/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/conflicting.properties, source: WAR:/
		// - BundleWiring.findEntries() #1 URL: bundleentry://31.fwk1392794732/conflicting.properties, source: jsf-primefaces-embedded-fragment:/
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/s1/conflicting.properties, source: WAR:/s1
		// - BundleWiring.findEntries() #1 URL: bundleentry://31.fwk1392794732/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - BundleWiring.findEntries() #1 URL: bundleentry://30.fwk1392794732/s1/s2/conflicting.properties, source: WAR:/s1/s2
		// - BundleWiring.findEntries() #1 URL: bundleentry://31.fwk1392794732/s1/s2/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1/s2
		// - BundleWiring.findEntries() #2 URL: bundleentry://30.fwk1392794732/s1/conflicting.properties, source: WAR:/s1
		// - BundleWiring.findEntries() #2 URL: bundleentry://31.fwk1392794732/s1/conflicting.properties, source: jsf-primefaces-embedded-fragment:/s1
		// - BundleWiring.findEntries() #3 URL: bundleentry://31.fwk1392794732/META-INF/maven/org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment/pom.properties, artifact: jsf-primefaces-embedded-fragment

		// observation:
		// - same as Bundle.findEntries()

		List<URL> urls1a = wiring.findEntries("", "conflicting.properties", BundleWiring.FINDENTRIES_RECURSE);
		for (URL url : urls1a) {
			LOG.info("BundleWiring.findEntries() #1 URL: {}, source: {}", url, read(url, "property.from"));
		}

		List<URL> urls2a = wiring.findEntries("s1", "conflicting.properties", 0);
		for (URL url : urls2a) {
			LOG.info("BundleWiring.findEntries() #2 URL: {}, source: {}", url, read(url, "property.from"));
		}

		List<URL> urls3a = wiring.findEntries("META-INF/maven/org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment", "pom.properties", 0);
		for (URL url : urls3a) {
			LOG.info("BundleWiring.findEntries() #3 URL: {}, artifact: {}", url, read(url, "artifactId"));
		}

		// --- Bundle.getEntry()

		// Felix:
		// - Bundle.getEntry() #1 URL: bundle://30.0:0/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.getEntry() #2 URL: bundle://30.0:0/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - Bundle.getEntry() #3 URL: null, source: <null>
		// Equinox:
		// - Bundle.getEntry() #1 URL: bundleentry://30.fwk2035616217/s1/conflicting.properties, source: WAR:/s1
		// - Bundle.getEntry() #2 URL: bundleentry://30.fwk2035616217/WEB-INF/s1/conflicting.properties, source: WAR:/WEB-INF/s1
		// - Bundle.getEntry() #3 URL: null, source: <null>

		// observation:
		// - no attached fragments are checked!
		// - only main bundle's content (no Bundle-ClassPath related contents) is checked despite the fact that Bundle.findEntries()
		//   checks the attached fragments

		URL url1 = b.getEntry("s1/conflicting.properties");
		LOG.info("Bundle.getEntry() #1 URL: {}, source: {}", url1, read(url1, "property.from"));

		URL url2 = b.getEntry("/WEB-INF/s1/conflicting.properties");
		LOG.info("Bundle.getEntry() #2 URL: {}, source: {}", url2, read(url2, "property.from"));

		URL url3 = b.getEntry("META-INF/maven/org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment/pom.properties");
		LOG.info("Bundle.getEntry() #3 URL: {}, source: {}", url3, read(url3, "property.from"));
	}

	@Test
	public void scanningUsingDifferentMethods() throws Exception {
		LOG.info("=== Bundles");
		for (Bundle b : context.getBundles()) {
			String info = String.format("#%02d: %s/%s (%s)",
					b.getBundleId(), b.getSymbolicName(), b.getVersion(), b.getLocation());
			LOG.info(info);
		}

		// --- getting "classpath jars" ---

		// we start with single bundle (in pax-web, usually a WAR bundle, or the bundle registering
		// some servlets) and "classpath jars" may include:
		//  - bundles wired _from_ current bundle using Import-Package
		//  - bundles wired _from_ current bundle using Require-Bundle
		//  - bundle's ordinary jars and even directories embedded in current bundle and listed in Bundle-ClassPath
		//  - bundle fragments wired _to_ current bundle using Fragment-Host

		Optional<Bundle> jsfPrimefacesEmbedded = Arrays.stream(context.getBundles())
				.filter(b -> "org.ops4j.pax.web.samples.jsf-primefaces-embedded".equals(b.getSymbolicName())).findFirst();
		assertTrue(jsfPrimefacesEmbedded.isPresent());
		Bundle b = jsfPrimefacesEmbedded.get();
		URL[] jars = ClassPathUtil.getClassPathJars(b, true);
		LOG.info("=== Classpath JARs for bundle {}/{}", b.getSymbolicName(), b.getVersion());
		for (URL jar : jars) {
			LOG.info("Classpath JAR: {}", jar);
		}

		// for Felix:
		// - === Classpath JARs for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/myfaces-api-2.2.12.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/myfaces-impl-2.2.12.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/commons-collections-3.2.2.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/commons-beanutils-1.9.4.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/commons-logging-1.2.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/commons-digester-1.8.jar!/
		// - Classpath JAR: jar:bundle://30.0:0/WEB-INF/lib/primefaces-7.0.jar!/
		// if "true" was passed to org.ops4j.pax.web.utils.ClassPathUtil.getClassPathJars(), additionally:
		// - Classpath JAR: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT
		// - Classpath JAR: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		// - Classpath JAR: mvn:javax.servlet/javax.servlet-api/4.0.1
		//
		// for Equinox:
		// - === Classpath JARs for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/myfaces-api-2.2.12.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/myfaces-impl-2.2.12.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/commons-collections-3.2.2.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/commons-beanutils-1.9.4.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/commons-logging-1.2.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/commons-digester-1.8.jar!/
		// - Classpath JAR: jar:bundleentry://30.fwk1976804832/WEB-INF/lib/primefaces-7.0.jar!/
		// if "true" was passed to org.ops4j.pax.web.utils.ClassPathUtil.getClassPathJars(), additionally:
		// - Classpath JAR: mvn:javax.servlet/javax.servlet-api/4.0.1
		// - Classpath JAR: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		// - Classpath JAR: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT

		// --- reading manifests from manually crafted (by ClassPathUtil) jar URIs ---

		LOG.info("=== /META-INF/MANIFEST.MF loaded from JARs for bundle {}/{}", b.getSymbolicName(), b.getVersion());
		for (URL jar : jars) {
			if (!jar.getProtocol().equals("jar")) {
				LOG.info("Skipping MANIFEST.MF URL: {}", jar);
				continue;
			}
			URL manifestUrl = new URL(jar, "/META-INF/MANIFEST.MF");
			LOG.info("MANIFEST.MF URL: {}", jar);
			Manifest mf = new Manifest(manifestUrl.openStream());
			LOG.info("    Resolved MANIFEST.MF URL: {} ({})", manifestUrl, mf.getMainAttributes().getValue("Bundle-SymbolicName"));
		}

		// for Felix:
		// - === /META-INF/MANIFEST.MF loaded from JARs for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/myfaces-api-2.2.12.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/myfaces-api-2.2.12.jar!/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/META-INF/MANIFEST.MF (org.apache.geronimo.specs.geronimo-atinject_1.0_spec)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/myfaces-impl-2.2.12.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/myfaces-impl-2.2.12.jar!/META-INF/MANIFEST.MF (org.apache.myfaces.core.impl)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-collections-3.2.2.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-collections-3.2.2.jar!/META-INF/MANIFEST.MF (org.apache.commons.collections)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-beanutils-1.9.4.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-beanutils-1.9.4.jar!/META-INF/MANIFEST.MF (org.apache.commons.commons-beanutils)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-logging-1.2.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-logging-1.2.jar!/META-INF/MANIFEST.MF (org.apache.commons.logging)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-digester-1.8.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/commons-digester-1.8.jar!/META-INF/MANIFEST.MF (null)
		// - MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/primefaces-7.0.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundle://30.0:0/WEB-INF/lib/primefaces-7.0.jar!/META-INF/MANIFEST.MF (org.primefaces)
		// - Skipping MANIFEST.MF URL: mvn:javax.servlet/javax.servlet-api/4.0.1
		// - Skipping MANIFEST.MF URL: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT
		// - Skipping MANIFEST.MF URL: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		//
		// - org.apache.felix.framework.URLHandlersStreamHandlerProxy.openConnection(java.net.URL) uses
		//   sun.net.www.protocol.jar.Handler for "jar:" protocol
		// - sun.net.www.protocol.jar.Handler.openConnection() returns just an instance of
		//   sun.net.www.protocol.jar.JarURLConnection
		//   java.net.JarURLConnection.jarFileURL == "bundle://30.0:0/WEB-INF/lib/myfaces-api-2.2.12.jar"
		// - sun.net.www.protocol.jar.URLJarFile.getJarFile() is called if cache is empty
		// - sun.net.www.protocol.jar.URLJarFile.isFileURL() returns false, so ...
		// - sun.net.www.protocol.jar.URLJarFile.retrieve() is called
		//    - normal java.net.URL.openConnection().getInputStream() is called
		//    - for "bundle:", Felix uses org.apache.felix.framework.URLHandlersBundleStreamHandler()
		//    - org.apache.felix.framework.URLHandlersBundleURLConnection() is used
		//    - InputStream returned is retrieved from org.apache.felix.framework.BundleRevisionImpl.getInputStream()
		//    - then from org.apache.felix.framework.cache.Content.getEntryAsStream()
		//       - org.apache.felix.framework.URLHandlersBundleURLConnection.m_classPathIdx is taken from "port" of URL
		//       - if the index was > 0, org.apache.felix.framework.BundleRevisionImpl.getContentPath() would be checked
		//         here's an example of WAB with Bundle-ClassPath and attached fragment:
		//            result = {java.util.ArrayList@4350}  size = 10
		//             0 = {o.a.f.f.cache.ContentDirectoryContent@4354} "CONTENT DIR WEB-INF/classes/ (JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar)"
		//             1 = {o.a.f.f.cache.JarContent@4355} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/myfaces-api-2.2.12.jar"
		//             2 = {o.a.f.f.cache.JarContent@4356} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar"
		//             3 = {o.a.f.f.cache.JarContent@4357} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/myfaces-impl-2.2.12.jar"
		//             4 = {o.a.f.f.cache.JarContent@4358} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-collections-3.2.2.jar"
		//             5 = {o.a.f.f.cache.JarContent@4359} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-beanutils-1.9.4.jar"
		//             6 = {o.a.f.f.cache.JarContent@4360} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-logging-1.2.jar"
		//             7 = {o.a.f.f.cache.JarContent@4361} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-digester-1.8.jar"
		//             8 = {o.a.f.f.cache.JarContent@4362} "JAR xxx/target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/primefaces-7.0.jar"
		//             9 = {o.a.f.f.cache.JarContent@4363} "JAR xxx/target/paxexam/bundle31/version0.0/bundle.jar"
		//    - then from org.apache.felix.framework.util.WeakZipFileFactory.WeakZipFile.getEntry()
		//    - and finally from java.util.zip.ZipFile.getInputStream() where entry is "WEB-INF/lib/myfaces-api-2.2.12.jar"
		// - sun.net.www.protocol.jar.URLJarFile.retrieve() gets input stream to embedded JAR and
		//   copies it to temporary directory (Path tmpFile = Files.createTempFile("jar_cache", null))
		//   which is /tmp/jar_cache8100048147215018178.tmp
		// - then sun.net.www.protocol.jar.JarURLConnection.connect() calls
		// - java.util.jar.JarFile.getEntry() and java.util.jar.JarFile.getInputStream() on the temporary jar
		//   with entry "META-INF/MANIFEST.MF"
		// - final input stream is is sun.net.www.protocol.jar.JarURLConnection$JarURLInputStream instance

		// for Equinox:
		// - === /META-INF/MANIFEST.MF loaded from JARs for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/myfaces-api-2.2.12.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/myfaces-api-2.2.12.jar!/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar!/META-INF/MANIFEST.MF (org.apache.geronimo.specs.geronimo-atinject_1.0_spec)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/myfaces-impl-2.2.12.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/myfaces-impl-2.2.12.jar!/META-INF/MANIFEST.MF (org.apache.myfaces.core.impl)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-collections-3.2.2.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-collections-3.2.2.jar!/META-INF/MANIFEST.MF (org.apache.commons.collections)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-beanutils-1.9.4.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-beanutils-1.9.4.jar!/META-INF/MANIFEST.MF (org.apache.commons.commons-beanutils)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-logging-1.2.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-logging-1.2.jar!/META-INF/MANIFEST.MF (org.apache.commons.logging)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-digester-1.8.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/commons-digester-1.8.jar!/META-INF/MANIFEST.MF (null)
		// - MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/primefaces-7.0.jar!/
		// -     Resolved MANIFEST.MF URL: jar:bundleentry://30.fwk540325452/WEB-INF/lib/primefaces-7.0.jar!/META-INF/MANIFEST.MF (org.primefaces)
		// - Skipping MANIFEST.MF URL: mvn:javax.servlet/javax.servlet-api/4.0.1
		// - Skipping MANIFEST.MF URL: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		// - Skipping MANIFEST.MF URL: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT

		// --- getting "classpace bundles" ---

		//  - bundles wired _from_ current bundle using Import-Package
		//  - bundles wired _from_ current bundle using Require-Bundle
		//  - bundle fragments wired _to_ current bundle using Fragment-Host

		Set<Bundle> bundles = new HashSet<>();
		ClassPathUtil.getBundlesInClassSpace(b, bundles);
		LOG.info("=== Bundles in Classspace for bundle {}/{}", b.getSymbolicName(), b.getVersion());
		for (Bundle bundle : bundles) {
			boolean fragment = (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
			LOG.info("ClassSpace bundle: {}" + (fragment ? " [fragment]" : ""), bundle.getLocation());
		}

		// for Felix:
		// - === Bundles in Classspace for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - ClassSpace bundle: mvn:javax.servlet/javax.servlet-api/4.0.1
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment/8.0.0-SNAPSHOT [fragment]
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT
		//
		// for Equinox:
		// - === Bundles in Classspace for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - ClassSpace bundle: mvn:javax.servlet/javax.servlet-api/4.0.1
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-embedded-fragment/8.0.0-SNAPSHOT [fragment]
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons1/8.0.0-SNAPSHOT
		// - ClassSpace bundle: mvn:org.ops4j.pax.web.samples/jsf-primefaces-commons2/8.0.0-SNAPSHOT

		// --- org.osgi.framework.wiring.BundleWiring.listResources() and org.osgi.framework.Bundle.getResource() ---

		// "list" verb and "resource" noun are used for operations involving classloaders - as
		// in org.osgi.framework.wiring.BundleWiring.listResources()

		bundles.add(b);
		LOG.info("=== /META-INF/MANIFEST.MF loaded from bundles from Classspace for bundle {}/{}", b.getSymbolicName(), b.getVersion());
		List<URL> urls = ClassPathUtil.listResources(bundles, "/META-INF", "MANIFEST.MF", true);
		for (URL url : urls) {
			Manifest mf = new Manifest(url.openStream());
			LOG.info("URL for /META-INF/MANIFEST.MF: {} ({})", url, mf.getMainAttributes().getValue("Bundle-SymbolicName"));
		}

		// for Felix:
		// - === /META-INF/MANIFEST.MF loaded from bundles from Classspace for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - URL for /META-INF/MANIFEST.MF: bundle://33.0:1/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-commons2)
		// - URL for /META-INF/MANIFEST.MF: bundle://30.0:2/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - URL for /META-INF/MANIFEST.MF: bundle://17.0:1/META-INF/MANIFEST.MF (javax.servlet-api)
		// - URL for /META-INF/MANIFEST.MF: bundle://32.0:1/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-commons1)
		//
		// for Equinox:
		// - === /META-INF/MANIFEST.MF loaded from bundles from Classspace for bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - URL for /META-INF/MANIFEST.MF: bundleresource://30.fwk1561063579:1/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - URL for /META-INF/MANIFEST.MF: bundleresource://17.fwk1561063579/META-INF/MANIFEST.MF (javax.servlet-api)
		// - URL for /META-INF/MANIFEST.MF: bundleresource://32.fwk1561063579/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-commons1)
		// - URL for /META-INF/MANIFEST.MF: bundleresource://33.fwk1561063579/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-commons2)

		BundleWiring wiring = b.adapt(BundleWiring.class);
		urls = wiring.findEntries("/META-INF", "MANIFEST.MF", 0);
		LOG.info("=== /META-INF/MANIFEST.MF loaded from wiring of bundle {}/{}", b.getSymbolicName(), b.getVersion());
		for (URL url : urls) {
			Manifest mf = new Manifest(url.openStream());
			LOG.info("Wiring entry: {} ({})", url, mf.getMainAttributes().getValue("Bundle-SymbolicName"));
		}

		// for Felix:
		// - === /META-INF/MANIFEST.MF loaded from wiring of bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Wiring entry: bundle://30.0:0/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded)
		// - Wiring entry: bundle://31.0:0/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded-fragment)
		//
		// for Equinox:
		// - === /META-INF/MANIFEST.MF loaded from wiring of bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Wiring entry: bundleentry://30.fwk1561063579/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded)
		// - Wiring entry: bundleentry://31.fwk1561063579/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded-fragment)

		// Using classloader of wiring and its java.lang.ClassLoader.getResources() method
		// Felix: org.apache.felix.framework.BundleWiringImpl$BundleClassLoader
		// Equinox: org.eclipse.osgi.internal.loader.EquinoxClassLoader
		//
		// for example in Felix, entire "content path" is considered inside org.apache.felix.framework.BundleRevisionImpl#getResourcesLocal():
		// contentPath = {java.util.ArrayList@4224}  size = 10
		//  0 = {org.apache.felix.framework.cache.ContentDirectoryContent@4227} "CONTENT DIR WEB-INF/classes/ (JAR /data/sour.../target/paxexam/bundle30/version0.0/bundle.jar)"
		//  1 = {org.apache.felix.framework.cache.JarContent@4228} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/myfaces-api-2.2.12.jar"
		//  2 = {org.apache.felix.framework.cache.JarContent@4229} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/geronimo-atinject_1.0_spec-1.0.jar"
		//  3 = {org.apache.felix.framework.cache.JarContent@4230} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/myfaces-impl-2.2.12.jar"
		//  4 = {org.apache.felix.framework.cache.JarContent@4231} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-collections-3.2.2.jar"
		//  5 = {org.apache.felix.framework.cache.JarContent@4232} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-beanutils-1.9.4.jar"
		//  6 = {org.apache.felix.framework.cache.JarContent@4233} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-logging-1.2.jar"
		//  7 = {org.apache.felix.framework.cache.JarContent@4234} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/commons-digester-1.8.jar"
		//  8 = {org.apache.felix.framework.cache.JarContent@4235} "JAR /data/.../target/paxexam/bundle30/version0.0/bundle.jar-embedded/WEB-INF/lib/primefaces-7.0.jar"
		//  9 = {org.apache.felix.framework.cache.JarContent@4236} "JAR /data/.../target/paxexam/bundle31/version0.0/bundle.jar"

		Enumeration<URL> urlsEnum = wiring.getClassLoader().getResources("/META-INF/MANIFEST.MF");
		LOG.info("=== /META-INF/MANIFEST.MF loaded from classLoader.getResources() of wiring of bundle {}/{}", b.getSymbolicName(), b.getVersion());
		while (urlsEnum.hasMoreElements()) {
			URL url = urlsEnum.nextElement();
			Manifest mf = new Manifest(url.openStream());
			LOG.info("Resource: {} ({})", url, mf.getMainAttributes().getValue("Bundle-SymbolicName"));
		}

		// for Felix:
		// - === /META-INF/MANIFEST.MF loaded from classLoader.getResources() of wiring of bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Resource: bundle://30.0:2/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - Resource: bundle://30.0:3/META-INF/MANIFEST.MF (org.apache.geronimo.specs.geronimo-atinject_1.0_spec)
		// - Resource: bundle://30.0:4/META-INF/MANIFEST.MF (org.apache.myfaces.core.impl)
		// - Resource: bundle://30.0:5/META-INF/MANIFEST.MF (org.apache.commons.collections)
		// - Resource: bundle://30.0:6/META-INF/MANIFEST.MF (org.apache.commons.commons-beanutils)
		// - Resource: bundle://30.0:7/META-INF/MANIFEST.MF (org.apache.commons.logging)
		// - Resource: bundle://30.0:8/META-INF/MANIFEST.MF (null)
		// - Resource: bundle://30.0:9/META-INF/MANIFEST.MF (org.primefaces)
		// - Resource: bundle://30.0:10/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded-fragment)
		//
		// for Equinox:
		// - === /META-INF/MANIFEST.MF loaded from classLoader.getResources() of wiring of bundle org.ops4j.pax.web.samples.jsf-primefaces-embedded/8.0.0.SNAPSHOT
		// - Resource: bundleresource://30.fwk547441493:1/META-INF/MANIFEST.MF (org.apache.myfaces.core.api)
		// - Resource: bundleresource://30.fwk547441493:2/META-INF/MANIFEST.MF (org.apache.geronimo.specs.geronimo-atinject_1.0_spec)
		// - Resource: bundleresource://30.fwk547441493:3/META-INF/MANIFEST.MF (org.apache.myfaces.core.impl)
		// - Resource: bundleresource://30.fwk547441493:4/META-INF/MANIFEST.MF (org.apache.commons.collections)
		// - Resource: bundleresource://30.fwk547441493:5/META-INF/MANIFEST.MF (org.apache.commons.commons-beanutils)
		// - Resource: bundleresource://30.fwk547441493:6/META-INF/MANIFEST.MF (org.apache.commons.logging)
		// - Resource: bundleresource://30.fwk547441493:7/META-INF/MANIFEST.MF (null)
		// - Resource: bundleresource://30.fwk547441493:8/META-INF/MANIFEST.MF (org.primefaces)
		// - Resource: bundleresource://30.fwk547441493:9/META-INF/MANIFEST.MF (org.ops4j.pax.web.samples.jsf-primefaces-embedded-fragment)

		// and finally a universal, non-classloader related method
		// "find" verb and "entries" noun are related to non-classloader usage - as
		// in org.osgi.framework.wiring.BundleWiring.findEntries()

		urls = ClassPathUtil.findEntries(bundles, "/META-INF", "MANIFEST.MF", true, false);
	}

	private String read(URL url, String property) throws IOException {
		if (url == null) {
			return "<null>";
		}
		byte[] buf = new byte[128];
		int read = -1;
		StringWriter sw = new StringWriter();
		try (InputStream is = url.openStream()) {
			Properties props = new Properties();
			props.load(is);
			return (String) props.get(property);
		}
	}

	private String read(String name, BundleWiring wiring, String property) throws IOException {
		URL url = wiring.getClassLoader().getResource(name);
		return read(url, property);
	}

}
