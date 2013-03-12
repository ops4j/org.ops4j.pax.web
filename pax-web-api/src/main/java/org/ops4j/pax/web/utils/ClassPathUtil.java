/*
 * Copyright 2012 Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
public class ClassPathUtil {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ClassPathUtil.class);

	private ClassPathUtil() {
		// munch
	}

	/**
	 * Returns a list of urls to jars that composes the Bundle-ClassPath.
	 * 
	 * @param bundle
	 *            the bundle from which the class path should be taken
	 * 
	 * @return list or urls to jars that composes the Bundle-ClassPath.
	 */
	public static URL[] getClassPathJars(final Bundle bundle) {
		final List<URL> urls = new ArrayList<URL>();
		final String bundleClasspath = (String) bundle.getHeaders().get(
				"Bundle-ClassPath");
		if (bundleClasspath != null) {
			String[] segments = bundleClasspath.split(",");
			for (String segment : segments) {
				final URL url = bundle.getEntry(segment);
				if (url != null) {
					if (url.toExternalForm().endsWith("jar")) {
						LOG.debug("Using url: " + url);
						try {
							URL jarUrl = new URL("jar:" + url.toExternalForm()
									+ "!/");
							urls.add(jarUrl);
						} catch (MalformedURLException ignore) {
							LOG.debug(ignore.getMessage());
						}
					}
				}
			}
		}
		LOG.debug("Bundle-ClassPath URLs: " + urls);
		// adds the depending bundles to the "classloader" space
		urls.addAll(getLocationsOfBundlesInClassSpace(bundle));
		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * Gets the locations of the bundles in the Class Space. Beware, in Karaf
	 * this will return the URL with which the bundle was originally
	 * provisioned, i.e. could potentially return wrap:..., mvn:..., etc. and
	 * even include URL parameters (i.e. ?Webapp-Context=...).
	 * 
	 * @param bundle
	 *            the bundle for which to perform the lookup
	 * 
	 * @return list of locations of bundles in class space
	 * 
	 */

	private static List<URL> getLocationsOfBundlesInClassSpace(Bundle bundle) {
		List<URL> urls = new ArrayList<URL>();
		Set<Bundle> importedBundles = getBundlesInClassSpace(bundle,
				new HashSet<Bundle>());
		for (Bundle importedBundle : importedBundles) {
			URL url = getLocationOfBundle(importedBundle);
			if (url != null) {
				urls.add(url);
			}
		}
		return urls;
	}

	private static URL getLocationOfBundle(Bundle importedBundle) {
		URL url = null;
		try {
			url = new URL(importedBundle.getLocation());
		} catch (MalformedURLException e) {
			try {
				url = importedBundle.getEntry("/");
				// CHECKSTYLE:SKIP
			} catch (Exception e2) {
				LOG.warn("Exception while calculating location of bundle", e);
			}
		}
		return url;
	}

	/**
	 * Gets a list of bundles that are imported or required by this bundle.
	 * 
	 * @param bundle
	 *            the bundle for which to perform the lookup
	 * 
	 * @return list of imported and required bundles
	 * 
	 */
	public static Set<Bundle> getBundlesInClassSpace(Bundle bundle,
			Set<Bundle> bundleSet) {
		return getBundlesInClassSpace(bundle.getBundleContext(), bundle,
				bundleSet);
	}

	private static Set<Bundle> getBundlesInClassSpace(BundleContext context,
			Bundle bundle, Set<Bundle> bundleSet) {
		Set<Bundle> bundles = new HashSet<Bundle>(); // The set containing the
														// bundles either being
														// imported or required
		if (bundle == null) {
			LOG.error("Incoming bundle is null");
			return bundles;
		}
		if (context == null) {
			LOG.error("Incoming context is null");
			return bundles;
		}

		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class); 
		//this will give us all required Wires (including require-bundle)
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(null);
		for (BundleWire bundleWire : requiredWires) {
			Bundle exportingBundle = bundleWire.getCapability().getRevision()
					.getBundle();

			if (exportingBundle.getBundleId() == 0) {
				continue; //system bundle is skipped this one isn't needed
			}
			if (!bundles.contains(exportingBundle)) {
				bundles.add(exportingBundle);
			}
		}

		Set<Bundle> transitiveBundles = new HashSet<Bundle>();

		if (!bundleSet.containsAll(bundles)) { //now let's scan transitively 
			bundles.removeAll(bundleSet);
			bundleSet.addAll(bundles);
			for (Bundle importedBundle : bundles) {
				transitiveBundles.addAll(getBundlesInClassSpace(context,
						importedBundle, bundleSet));
			}
		}
		return bundleSet;
	}

}
