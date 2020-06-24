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
package org.ops4j.pax.web.samples.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;

import org.apache.xbean.osgi.bundle.util.BundleResourceHelper;
import org.ops4j.pax.web.service.spi.util.ResourceDelegatingBundleClassLoader;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SneakIntoPaxWebSpi {

	public static final Logger LOG = LoggerFactory.getLogger(SneakIntoPaxWebSpi.class);

	private SneakIntoPaxWebSpi() {
	}

	public static Bundle whatsYourBundle() {
		return FrameworkUtil.getBundle(SneakIntoPaxWebSpi.class);
	}

	public static void useXBeanFinder() throws IOException {
		Bundle bundle = FrameworkUtil.getBundle(SneakIntoPaxWebSpi.class);
		Set<Bundle> wiredBundles = ClassPathUtil.getBundlesInClassSpace(bundle, new LinkedHashSet<>());
		// should be:
		// wiredBundles: java.util.Set  = {java.util.LinkedHashSet@4061}  size = 8
		//  0 = {org.apache.felix.framework.BundleImpl@4064} "javax.servlet-api [17]"
		//  1 = {org.apache.felix.framework.BundleImpl@4065} "org.ops4j.pax.web.pax-web-api [18]"
		//  2 = {org.apache.felix.framework.BundleImpl@4066} "org.apache.xbean.bundleutils [21]"
		//  3 = {org.apache.felix.framework.BundleImpl@4067} "org.ops4j.pax.logging.pax-logging-api [15]"
		//  4 = {org.apache.felix.framework.BundleImpl@4068} "org.apache.xbean.finder [20]"
		//  5 = {org.apache.felix.framework.BundleImpl@4069} "org.objectweb.asm [22]"
		//  6 = {org.apache.felix.framework.BundleImpl@4070} "org.objectweb.asm.commons [23]"
		//  7 = {org.apache.felix.framework.BundleImpl@4071} "org.objectweb.asm.tree [24]"

		ArrayList<Bundle> bundles = new ArrayList<>();
		bundles.add(bundle);
		bundles.addAll(wiredBundles);
		ClassLoader loader = new ResourceDelegatingBundleClassLoader(bundles);

		BundleResourceHelper finder1 = new BundleResourceHelper(bundle, false, true);
		BundleResourceHelper finder2 = new BundleResourceHelper(bundle, false, false);

		Enumeration<URL> resources1 = finder1.getResources("/*");
		Enumeration<URL> resources2 = finder2.getResources("/*");
	}

	/**
	 * Check whether we can correctly search for {@link ServletContainerInitializer initializers} using {@link ServiceLoader}
	 * when the bundle has attached fragments
	 * @param b
	 * @return
	 */
	public static List<Class<? extends ServletContainerInitializer>> findInitializersUsingServiceLoader(Bundle b) {
		List<Class<? extends ServletContainerInitializer>> initializers = new LinkedList<>();
		ServiceLoader<ServletContainerInitializer> loader = ServiceLoader.load(ServletContainerInitializer.class, b.adapt(BundleWiring.class).getClassLoader());
		for (ServletContainerInitializer initializer : loader) {
			LOG.info("ServiceLoader service: {}", initializer);
			initializers.add(initializer.getClass());
		}
		return initializers;
	}

	/**
	 * Check whether we can correctly search for {@link ServletContainerInitializer initializers} using
	 * {@link BundleWiring#findEntries}
	 * @param b
	 * @return
	 */
	public static List<Class<? extends ServletContainerInitializer>> findInitializersUsingBundleWiring(Bundle b) throws Exception {
		List<Class<? extends ServletContainerInitializer>> initializers = new LinkedList<>();

		BundleWiring wiring = b.adapt(BundleWiring.class);
		List<URL> urls = wiring.findEntries("/META-INF/services", "javax.servlet.ServletContainerInitializer", 0);
		for (URL url : urls) {
			LOG.info("Wiring entry: {}", url);
			String className = readService(url);
			initializers.add(wiring.getClassLoader().loadClass(className).asSubclass(ServletContainerInitializer.class));
		}
		return initializers;
	}

	/**
	 * Check whether we can correctly search for {@link ServletContainerInitializer initializers} using
	 * {@link Bundle#findEntries}
	 * @param b
	 * @return
	 */
	public static List<Class<? extends ServletContainerInitializer>> findInitializersUsingBundle(Bundle b) throws Exception {
		List<Class<? extends ServletContainerInitializer>> initializers = new LinkedList<>();

		Enumeration<URL> urls = b.findEntries("/META-INF/services", "javax.servlet.ServletContainerInitializer", false);
		if (urls != null) {
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				LOG.info("Bundle entry: {}", url);
				String className = readService(url);
				initializers.add(b.loadClass(className).asSubclass(ServletContainerInitializer.class));
			}
		}
		return initializers;
	}

	/**
	 * Having only bundle, try to find:<ul>
	 *     <li>{@code /WEB-INF/faces-config.xml}</li>
	 *     <li>{@code /WEB-INF/lib/primefaces-VERSION.jar!/META-INF/faces-config.xml}</li>
	 *     <li>{@code /WEB-INF/lib/primefaces-VERSION.jar!/META-INF/primefaces-p.taglib.xml}</li>
	 * </ul>
	 * @param b
	 */
	public static void findFacesConfigs(Bundle b) {
		List<URL> result = new LinkedList<>();
		Enumeration<URL> urls = b.findEntries("/WEB-INF/", "*faces-config.xml", false);
		if (urls != null) {
			while (urls.hasMoreElements()) {
				result.add(urls.nextElement());
			}
		}
		urls = b.findEntries("/META-INF/", "*faces-config.xml", false);
		if (urls != null) {
			while (urls.hasMoreElements()) {
				result.add(urls.nextElement());
			}
		}
		urls = b.findEntries("/META-INF/", "*.taglib.xml", false);
		if (urls != null) {
			while (urls.hasMoreElements()) {
				result.add(urls.nextElement());
			}
		}
		for (URL url : result) {
			LOG.info("Bundle entry: {}", url);
		}

		Set<Bundle> bundles = new HashSet<>();
		bundles = ClassPathUtil.getBundlesInClassSpace(b, bundles);
		for (Bundle bundle : bundles) {
			LOG.info("Bundle in class space: {}", bundle);
		}
		URL[] jars = ClassPathUtil.getClassPathJars(b, false);
		for (URL jar : jars) {
			LOG.info("Jar in class path: {}", jar);
		}

		List<URL> manifests = b.adapt(BundleWiring.class).findEntries("/META-INF/", "MANIFEST.MF", BundleWiring.FINDENTRIES_RECURSE);
		for (URL m : manifests) {
			LOG.info("Wiring entry: {}", m);
		}
	}

	private static String readService(URL url) throws IOException {
		String className = null;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#")) {
					className = line;
					break;
				}
			}
		}
		return className;
	}

}
