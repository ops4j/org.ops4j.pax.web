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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to perform bundle scanning - using only OSGi Core APIs (without using e.g., XBean Finder)
 * @author achim
 */
public class ClassPathUtil {

	private static final Logger LOG = LoggerFactory.getLogger(ClassPathUtil.class);

	private ClassPathUtil() {
		// munch
	}

	/**
	 * <p>Returns a list of urls for jars that compose the Bundle-ClassPath and (with {@code useClassSpace} = {@code true})
	 * also a list of different URLs for bundles in <em>class space</em> (which includes bundles for imported packages,
	 * fragments, wires of fragments and required bundles).</p>
	 * <p>This method doesn't return the URL of passed bundle itself.</p>
	 *
	 * @param bundle the bundle from which the class path should be taken
	 * @param useClassSpace wheter to add also bundles reachable via different <em>wires</em> (required bundles,
	 * imported packages and fragments)
	 * @return list or urls to jars that composes the Bundle-ClassPath.
	 */
	public static URL[] getClassPathJars(final Bundle bundle, boolean useClassSpace) {
		final List<URL> urls = new ArrayList<>();
		final String bundleClasspath = bundle.getHeaders().get("Bundle-ClassPath");
		if (bundleClasspath != null) {
			String[] segments = bundleClasspath.split("\\s*,\\s*");
			for (String segment : segments) {
				final URL url = bundle.getEntry(segment);
				if (url != null && url.toExternalForm().endsWith("jar")) {
					try {
						// sun.net.www.protocol.jar.Handler.separator = "!/"
						urls.add(new URL("jar:" + url.toExternalForm() + "!/"));
					} catch (MalformedURLException ignore) {
						LOG.debug(ignore.getMessage());
					}
				}
			}
		}
		LOG.debug("Bundle-ClassPath URLs: " + urls);

		if (useClassSpace) {
			// adds the depending bundles to the "classloader" space
			urls.addAll(getLocationsOfBundlesInClassSpace(bundle));
		}

		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * Returns a list of urls to jars that composes the Bundle-ClassPath and also a list of different URLs for bundles
	 * in <em>class space</em> (which includes bundles for imported packages, fragments, wires of fragments and
	 * required bundles)
	 *
	 * @param bundle the bundle from which the class path should be taken
	 * @return list or urls to jars that composes the Bundle-ClassPath.
	 *
	 * @deprecated use {@link #getClassPathJars(Bundle, boolean)}.
	 */
	@Deprecated
	public static URL[] getClassPathJars(final Bundle bundle) {
		return getClassPathJars(bundle, true);
	}

	/**
	 * Gets the locations of the bundles in the Class Space. Beware, in Karaf
	 * this will return the URL with which the bundle was originally
	 * provisioned, i.e. could potentially return wrap:..., mvn:..., etc. and
	 * even include URL parameters (i.e. ?Webapp-Context=...).
	 *
	 * @param bundle the bundle for which to perform the lookup
	 * @return list of locations of bundles in class space
	 */
	private static List<URL> getLocationsOfBundlesInClassSpace(Bundle bundle) {
		List<URL> urls = new ArrayList<>();
		Set<Bundle> importedBundles = getBundlesInClassSpace(bundle, new HashSet<>());
		for (Bundle importedBundle : importedBundles) {
			URL url = getLocationOfBundle(importedBundle);
			if (url != null) {
				urls.add(url);
			}
		}
		return urls;
	}

	private static URL getLocationOfBundle(Bundle bundle) {
		URL url = null;
		try {
			url = new URL(bundle.getLocation());
		} catch (MalformedURLException e) {
			try {
				url = bundle.getEntry("/");
			} catch (Exception e2) {
				LOG.warn("Exception while calculating location of bundle", e);
			}
		}
		return url;
	}

	/**
	 * Gets a list of bundles that are imported or required by {@link Bundle}. This method also returns
	 * attached fragments.
	 *
	 * @param bundle the bundle for which to perform the lookup
	 * @param bundleSet set that's both returned and filled with bundles in the <em>class space</em>.
	 * @return list of imported and required bundles
	 */
	public static Set<Bundle> getBundlesInClassSpace(Bundle bundle, Set<Bundle> bundleSet) {
		return getBundlesInClassSpace(bundle.getBundleContext(), bundle, bundleSet);
	}

	/**
	 * Gets a list of bundles that are imported or required by {@link BundleContext}. This method also returns
	 * attached fragments.
	 *
	 * @param context
	 * @param bundle
	 * @param bundleSet
	 * @return
	 */
	public static Set<Bundle> getBundlesInClassSpace(BundleContext context, Bundle bundle, Set<Bundle> bundleSet) {
		BundleWiring bundleWiring = bundle == null ? null : bundle.adapt(BundleWiring.class);

		if (bundle == null || context == null || bundleWiring == null) {
			throw new IllegalArgumentException("Bundle, BundleContext or BundleWiring is null");
		}

		Set<Bundle> bundles = new HashSet<>(); // The set containing the

		// This will give us all required Wires (including Require-Bundle). We're mostly interested in
		// wires from namespaces:
		//  - org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE = "osgi.wiring.bundle"
		//  - org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE = "osgi.wiring.package"
		List<BundleWire> requiredWires = bundleWiring.getRequiredWires(null);
		for (BundleWire bundleWire : requiredWires) {
			if (!(bundleWire.getRequirement().getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)
					|| (bundleWire.getRequirement().getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)))) {
				continue;
			}
			Bundle exportingBundle = bundleWire.getCapability().getRevision().getBundle();

			if (exportingBundle.getBundleId() == 0) {
				continue; // system bundle is skipped this one isn't needed
			}
			bundles.add(exportingBundle);
		}

		// fragments attached to checked bundle
		List<BundleWire> providedWires = bundleWiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
		if (providedWires != null) {
			for (BundleWire wire : providedWires) {
				Bundle b = wire.getRequirerWiring().getBundle();
				bundles.add(b);
			}
		}

		if (!bundleSet.containsAll(bundles)) {
			// there are new bundles found, that are not in the set that we're collecting,

			// leave the new ones
			bundles.removeAll(bundleSet);
			// include the new ones in the set we're collecting
			bundleSet.addAll(bundles);
			// collect transitively
			for (Bundle b : bundles) {
				getBundlesInClassSpace(context, b, bundleSet);
			}
		}

		// Sanity checkpoint to remove uninstalled bundles
		bundleSet.removeIf(b -> b.getState() == Bundle.UNINSTALLED);

		return bundleSet;
	}

	/**
	 * <p>This method uses {@link BundleWiring#listResources} that delegates to classloader. If there are
	 * more visible resources with the same name, only one is returned.
	 * Underneath this method uses {@link BundleWiring#listResources}.</p>
	 * <p>This method is not a good choice to discover manifests or e.g., {@code faces-context.xml} files,
	 * if for single bundle many resources with the same path may be returned (as in case of WAR with Bundle-ClassPath
	 * or with fragments).</p>
	 *
	 * @param bundles
	 * @param path
	 * @param pattern
	 * @param recurse
	 * @return
	 */
	public static List<URL> listResources(Iterable<Bundle> bundles, String path, String pattern, boolean recurse) {
		List<URL> resources = new ArrayList<>();
		for (Bundle bundle : bundles) {
			int options = BundleWiring.LISTRESOURCES_LOCAL | (recurse ? BundleWiring.LISTRESOURCES_RECURSE : 0);
			boolean isFragment = bundle.adapt(BundleRevision.class) != null
					&& (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
			if (!isFragment) {
				// for fragment, org.osgi.framework.wiring.BundleWiring.listResources() should return "empty
				// collection" and it's exactly the case with Equinox, however Felix returns some names...
				// and later org.osgi.framework.Bundle.getResource() returns null for this name
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				Collection<String> names = wiring.listResources(path, pattern, options);
				for (String name : names) {
					resources.add(bundle.getResource(name));
				}
			}
		}
		return resources;
	}

	/**
	 * This method uses {@link BundleWiring#findEntries} that doesn't involve classloaders. This way we can find
	 * multiple resources with the same path. Normally {@link BundleWiring#findEntries} doesn't check JARs listed
	 * on {@code Bundle-ClassPath} (because it doesn't work at classloader level), however we can explicitly tell
	 * it to do so.
	 *
	 * @param bundles
	 * @param path
	 * @param pattern
	 * @param recurse
	 * @param useBundleClasspath additionally search the JARs on Bundle-ClassPath
	 * @return
	 */
	public static List<URL> findEntries(Iterable<Bundle> bundles, String path, String pattern, boolean recurse, boolean useBundleClasspath) {
		List<URL> resources = new ArrayList<>();
		for (Bundle bundle : bundles) {
			int options = recurse ? BundleWiring.FINDENTRIES_RECURSE : 0;
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			List<URL> entries = wiring.findEntries(path, pattern, options);
			resources.addAll(entries);

			if (useBundleClasspath) {
				URL[] jars = getClassPathJars(bundle, false);
//				JarFile jar =
			}
		}
		return resources;
	}

}
