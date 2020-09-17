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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

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
	 * @return list or urls to jars that composes the Bundle-ClassPath. Each JAR is returned as URL in the form
	 *         {@code jar:<location>!/}, where {@code <location>} may be for example
	 *         {@code bundle://30.0:0/WEB-INF/lib/myfaces-impl-2.2.12.jar}
	 */
	public static URL[] getClassPathJars(final Bundle bundle, boolean useClassSpace) {
		final List<URL> urls = new ArrayList<>();
		final String bundleClasspath = bundle.getHeaders() == null ? null : bundle.getHeaders().get("Bundle-ClassPath");
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

		Set<Bundle> bundles = new HashSet<>();

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

	public static List<URL> findEntries(Bundle bundle, String path, String pattern, boolean recurse, boolean useBundleClasspath) throws IOException {
		return findEntries(Collections.singletonList(bundle), path, pattern, recurse, useBundleClasspath);
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
	public static List<URL> findEntries(Iterable<Bundle> bundles, String path, String pattern, boolean recurse, boolean useBundleClasspath) throws IOException {
		List<URL> resources = new LinkedList<>();
		if (pattern != null) {
			while (pattern.startsWith("/")) {
				pattern = pattern.substring(1);
			}
		} else {
			pattern = "*";
		}
		for (Bundle bundle : bundles) {
			int options = recurse ? BundleWiring.FINDENTRIES_RECURSE : 0;
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			if (wiring != null) {
				List<URL> entries = wiring.findEntries(normalizeBase(path, false), pattern, options);
				resources.addAll(entries);

				if (useBundleClasspath) {
					URL[] jars = getClassPathJars(bundle, false);
					resources.addAll(findEntries(jars, normalizeBase(path, true), pattern, recurse));
				}
			}
		}
		return resources;
	}

	/**
	 * Normalizes the path as <em>base</em>, which means changing "/" to empty string, changing non-empty path
	 * to a pat that doesn't start with slash and where we can ask for trailing slash if needed.
	 * @param path
	 * @param ensureTrailingSlash should non-empty base path end with slash?
	 * @return
	 */
	private static String normalizeBase(String path, boolean ensureTrailingSlash) {
		String base = path == null ? "" : path;
		while (base.startsWith("/")) {
			base = base.substring(1);
		}
		if (ensureTrailingSlash && !"".equals(base) && !base.endsWith("/")) {
			base += "/";
		}
		return base;
	}

	/**
	 * <p>This method matches {@link Bundle#findEntries(String, String, boolean)} but should be used when we don't
	 * have access to any {@link Bundle}. It is used as a fallback in some resource-finding methods when
	 * {@link org.osgi.framework.FrameworkUtil#getBundle(Class)} doesn't return anything (which may be a case in
	 * unit tests).</p>
	 *
	 * <p>This method is inspired by Spring's
	 * {@code org.springframework.core.io.support.PathMatchingResourcePatternResolver#doFindAllClassPathResources()}
	 * where the most important problem of {@code ""} resource base was cleverly solved. Though we won't traverse
	 * up the classloader parents and we won't check {@link ClassLoader#getSystemResources(String)}.</p>
	 *
	 * <p>Spring's {@code org.springframework.core.io.support.PathMatchingResourcePatternResolver#getResources()}
	 * allows passing argument like {@code classpath*:/META-INF/** /*.tld} (ANT-style), however
	 * {@code org.springframework.util.AntPathMatcher#doMatch()} is a bit complicated and very different from
	 * {@link Bundle#findEntries(String, String, boolean)}, so we'll just test the entry for:<ul>
	 *     <li>begins with {@code path}</li>
	 *     <li>ends (or matches) {@code pattern}, where the patterns is simple glob ({@code *} and {@code ?}
	 *         supported).</li>
	 *     <li>{@code /META-INF/tlds/my.tld} should NOT match when {@code path=/META-INF/tlds} and
	 *         {@code pattern=tlds/*.tld}</li>
	 * </ul></p>
	 *
	 * @param loader
	 * @param path never a pattern. Should indicate the <em>base</em> used for relative path searching and
	 *        is always treated as base <em>directory</em>
	 * @param pattern file pattern (only for matching last path segment == no slashes) to match. If this pattern
	 *        is {@code null}, we assume {@code *} which means <em>directory listing in the base path</em>
	 * @param recurse whether we should look at any depth or only within {@code path}
	 * @return
	 */
	public static List<URL> findEntries(ClassLoader loader, String path, String pattern, boolean recurse)
			throws IOException {
		if (pattern == null) {
			pattern = "*";
		}
		while (pattern.startsWith("/")) {
			pattern = pattern.substring(1);
		}

		String base = normalizeBase(path, true);

		List<URL> roots = new LinkedList<>();
		if ("".equals(base)) {
			// can't call cl.getResources(base), we have to find the roots just as in
			// org.springframework.core.io.support.PathMatchingResourcePatternResolver.doFindAllClassPathResources()
			// but only in current loader
			// sun.misc.URLClassPath.loaders may contain two (by default) kinds of loaders:
			// - sun.misc.URLClassPath.FileLoader
			// - sun.misc.URLClassPath.JarLoader
			// and simply in JarLoader, java.util.jar.JarFile.getJarEntry("") doesn't return anything, while
			// in FileLoader we get sun.misc.URLClassPath.Loader.getBaseURL() as the URL for "".
			if (loader instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader) loader).getURLs();
				if (urls.length == 1) {
					// let's handle surefire here
					if (urls[0].toExternalForm().endsWith("jar")) {
						urls = jarToItsClassPath(urls[0]);
					}
				}
				for (URL entry : urls) {
					if ("jar".equals(entry.getProtocol())) {
						roots.add(entry);
					} else {
						if (entry.getPath().endsWith(".jar")) {
							// don't bother with ZIPs, WARs, ...
							roots.add(new URL("jar:" + entry.toExternalForm() + "!/"));
						} else if ("file".equals(entry.getProtocol())) {
							// this may be file:/path/to/maven/project/test-classes/ for example
							File dir = new File(URI.create(entry.toExternalForm()));
							if (dir.isDirectory()) {
								roots.add(dir.toURI().toURL());
							}
						}
					}
				}
			}
		} else {
			// let's rely only on java.lang.ClassLoader.getResources()
			Enumeration<URL> e = loader.getResources(base);
			while (e.hasMoreElements()) {
				roots.add(e.nextElement());
			}
		}

		List<URL> resources = new LinkedList<>();
		scanRoots(roots, pattern, recurse, resources);

		return resources;
	}

	/**
	 * Used in tests, when there's only surefire on the classpath
	 * @param url
	 * @return
	 */
	public static URL[] jarToItsClassPath(URL url) throws IOException {
		URL[] urls = new URL[] { url };
		URLConnection con = new URL("jar:" + urls[0].toExternalForm() + "!/").openConnection();
		if (con instanceof JarURLConnection) {
			String cp = ((JarURLConnection) con).getManifest().getMainAttributes().getValue("Class-Path");
			if (cp != null) {
				String[] entries = cp.split(" ");
				urls = new URL[entries.length];
				int pos = 0;
				for (String e : entries) {
					urls[pos++] = new URL(e);
				}
			}
		}

		return urls;
	}

	/**
	 * Third {@code findEntries()} method - this one starts with an array of roots (which are for example
	 * JARs from {@code Bundle-ClassPath}, but may be other "roots".
	 * @param roots URLs that should end with "/" (jar: URLs should end with "!/")
	 * @param path
	 * @param pattern
	 * @param recurse
	 * @return
	 * @throws IOException
	 */
	public static List<URL> findEntries(URL[] roots, String path, String pattern, boolean recurse)
			throws IOException {
		List<URL> resources = new LinkedList<>();

		while (pattern.startsWith("/")) {
			pattern = pattern.substring(1);
		}

		// change roots into roots + the base path
		List<URL> newRoots = new LinkedList<>();
		String base = normalizeBase(path, true);
		for (URL root : roots) {
			newRoots.add(new URL(root, base));
		}

		scanRoots(newRoots, pattern, recurse, resources);
		return resources;
	}

	private static void scanRoots(List<URL> roots, String pattern, boolean recurse, List<URL> resources) {
		Pattern p = Pattern.compile(pattern.replaceAll("\\?", ".").replaceAll("\\*", ".*"));

		for (URL root : roots) {
			String protocol = root.getProtocol();
			if ("jar".equals(protocol) && !jarClassPathEntryExists(root)) {
				continue;
			}

			if ("jar".equals(protocol)) {
				scanJar(root, p, recurse, resources);
			} else if ("file".equals(protocol)) {
				// assume it exists, otherwise it shouldn't be on roots list
				scanDirectory(new File(URI.create(root.toExternalForm())), p, recurse, resources);
			}
		}
	}

	/**
	 * Similar to Spring's {@code PathMatchingResourcePatternResolver#doFindPathMatchingJarResources()}
	 * @param root
	 * @param pattern
	 * @param recurse
	 * @param result
	 */
	private static void scanJar(URL root, Pattern pattern, boolean recurse, Collection<URL> result) {
		// root can be "jar:<location>!/" or "jar:<location>!/some/base"
		try {
			URLConnection con = root.openConnection();
			JarFile jar = null;
			// this should never start with "/"
			String rootEntryPath = null;
			boolean closeJar = false;

			if (con instanceof JarURLConnection) {
				// should be the case in most of the times
				JarURLConnection jarCon = (JarURLConnection) con;
				jar = jarCon.getJarFile();
				JarEntry jarEntry = jarCon.getJarEntry();
				rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
				closeJar = !jarCon.getUseCaches();
			} else {
				String location = root.getFile();
				int separator = location.indexOf("!/");
				if (separator != -1) {
					jar = new JarFile(location.substring(0, separator));
					rootEntryPath = location.substring(separator + 2);
				} else {
					jar = new JarFile(location);
					rootEntryPath = "";
				}
				closeJar = true;
			}

			// actual scanning
			if (!"".equals(rootEntryPath) && !rootEntryPath.endsWith("/")) {
				// we need the root to be a directory entry, unless it's the root
				rootEntryPath += "/";
			}
			for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				if (entryPath.startsWith(rootEntryPath)) {
					String relativePath = entryPath.substring(rootEntryPath.length());
					if (relativePath.startsWith("/")) {
						relativePath = relativePath.substring(1);
					}
					if (matches(relativePath, pattern, recurse)) {
						// org.springframework.core.io.UrlResource.createRelativeURL()
						// # can appear in filenames, java.net.URL should not treat it as a fragment
						relativePath = relativePath.replaceAll("#", "%23");
						result.add(new URL(root, relativePath));
					}
				}
			}
		} catch (IOException e) {
			LOG.warn(e.getMessage(), e);
		}
	}

	/**
	 * Similar to Spring's {@code PathMatchingResourcePatternResolver#doFindPathMatchingFileResources()}
	 * @param file
	 * @param pattern
	 * @param recurse
	 */
	private static void scanDirectory(File file, Pattern pattern, boolean recurse, final Collection<URL> result) {
		// org.springframework.core.io.support.PathMatchingResourcePatternResolver.doRetrieveMatchingFiles()
		if (!file.isDirectory()) {
			return;
		}
		try {
			final Path root = file.toPath();
			Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					String relativePath = root.relativize(file).toString();
					if (matches(relativePath, pattern, recurse)) {
						result.add(new URL(root.toUri().toURL(), relativePath));
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Returns {@code true} if passed path matches the pattern
	 * @param path
	 * @param pattern
	 * @param recurse
	 * @return
	 */
	private static boolean matches(String path, Pattern pattern, boolean recurse) {
		String nPath = path.replaceAll("\\\\", "/");
		return recurse ? pattern.matcher(nPath).find() : pattern.matcher(nPath).matches();
	}

	/**
	 * Checks whether jar: location exists as proper <em>classpath element</em>
	 * @param root
	 * @return
	 */
	public static boolean jarClassPathEntryExists(URL root) {
		String protocol = root.getProtocol();
		if (!"jar".equals(protocol)) {
			throw new IllegalArgumentException(root + " url not supported");
		}

		try {
			URLConnection con = root.openConnection();
			// as in org.springframework.core.io.AbstractFileResolvingResource.exists()
			if (con.getContentLengthLong() > 0L) {
				return true;
			}
			con.getInputStream().close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
