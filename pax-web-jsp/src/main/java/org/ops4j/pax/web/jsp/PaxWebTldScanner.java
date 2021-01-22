/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletContext;

import org.apache.jasper.servlet.TldScanner;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Version of {@link TldScanner} that know a bit more about OSGi.
 */
public class PaxWebTldScanner extends TldScanner {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebTldScanner.class);

	/** {@link Bundle} associated with a {@link ServletContext} where we perform TLD scanning. */
	private final Bundle bundle;

	private final Set<URL> scanned = new HashSet<>();

	public PaxWebTldScanner(ServletContext context, Bundle bundle) {
		super(context, true, true, true);
		this.bundle = bundle;
	}

	@Override
	public void scan() throws IOException, SAXException {
		// see "JSP.7.3.2 TLD resource path" of JSR 245 JSP Specification

		// 1. If the container is Java EE platform compliant, the Map Entries for the tag libraries that are part
		//    of the Java EE platform.
		//    In Pax Web the "platform" will be pax-web-jsp bundle and we'll just get standard tag library TLDs
		LOG.info("Searching for TlDs in pax-web-jsp bundle");
		scanPlatform();

		// 2. Taglib Map in web.xml - these are the ones registered using:
		//     - org.ops4j.pax.web.service.WebContainer.registerJspConfigTagLibs()
		//     - org.ops4j.pax.web.service.WebContainer.registerJspConfigPropertyGroup()
		//    these match /web-app/jsp-config/taglib/taglib-location elements in web.xml
		LOG.info("Searching for TlDs in context configuration (web.xml)");
		scanJspConfig();

		// 3. TLDs found as resources of ServletContext - that's done entirely by Tomcat, no OSGi here except
		//    the fact that ServletContext.getResourcePaths() and ServletContext.getResource() methods are backed
		//    by WebContainerContext/ServletContextHelper
		LOG.info("Searching for TlDs in /WEB-INF/");
		scanResourcePaths("/WEB-INF/");

		// 4. Tomcat calls javax.servlet.ServletContext.getResourcePaths("/WEB-INF/lib/") and processes
		//    all the JARs found, but because WEB-INF/lib/*.jar entries are added to Bundle-ClassPath entry of
		//    WABs, we're not calling super.scanJars() (thus we don't actually need any JarScanner)

		// 5. starting with single bundle - the one used to register given OsgiServletContext's OsgiContextModel - we
		//    we should scan:
		//     - this bundle
		//     - this bundle's fragments
		//     - (optionally?) this bundle's required bundles
		//     - (optionally?) this bundle's wires
		LOG.info("Searching for TlDs in bundle {}", bundle);
		scanBundle(bundle);
	}

	@Override
	protected void scanPlatform() {
		// By "platform" we mean only pax-web-jsp bundle, which should have standard taglib TLDs embedded
		// from org.apache.taglibs:taglibs-standard-impl
		Bundle paxWebJsp = FrameworkUtil.getBundle(this.getClass());

		List<URL> tlds = new LinkedList<>();

		if (paxWebJsp != null) {
			// it means that the classloader is org.osgi.framework.BundleReference, so it's loaded in OSGi env
			// and we can use methods to search the content of the bundle (including fragments, which we should
			// consider)
			BundleRevision rev = paxWebJsp.adapt(BundleRevision.class);
			if ((rev.getTypes() & BundleRevision.TYPE_FRAGMENT) > 0) {
				// pax-web-jsp should NOT be a fragment, or rather a bundle of this class is fragment. Just sanity
				// check and API showcase ;)
				return;
			}
			if (bundle.getState() == Bundle.INSTALLED) {
				// org.osgi.framework.Bundle.findEntries will attempt resolution, but we don't want it
				return;
			}
			Enumeration<URL> e = paxWebJsp.findEntries("/META-INF", "*.tld", true);
			while (e.hasMoreElements()) {
				tlds.add(e.nextElement());
			}
		} else {
			// we're probably running inside some unit test, but it'd be still nice to find the TLDs from pax-web-jsp
			ClassLoader classLoader = this.getClass().getClassLoader();
			try {
				tlds.addAll(ClassPathUtil.findEntries(classLoader, "/META-INF", "*.tld", true));
			} catch (IOException e) {
				LOG.warn("Problem getting TLD descriptors using ClassLoader {}", classLoader);
			}
		}

		for (URL tld : tlds) {
			try {
				parseTld(new TldResourcePath(tld, null));
			} catch (SAXException | IOException e) {
				LOG.warn("Problem parsing TLD at {}", tld);
			}
		}
	}

	/**
	 * Special Pax Web scanning for TLDs - the OSGi way
	 * @param bundle
	 */
	private void scanBundle(Bundle bundle) throws IOException {
		List<URL> tldURLs = new ArrayList<>(16);

		// TODO: check if /WEB-INF/classes/META-INF/*.tlds are scanned (if WEB-INF/classes is on Bundle-ClasPath it
		//       won't be returned by org.ops4j.pax.web.utils.ClassPathUtil.getClassPathJars())
		//       See org.apache.jasper.servlet.TldScanner.TldScannerCallback.scanWebInfClasses()

		// First: JARs from Bundle-ClassPath - we'll scan them separately, because we want to use Bundle.findEntries()
		// methods, which checks the fragments, but doesn't check classpath at all
		URL[] jars = ClassPathUtil.getClassPathJars(bundle, false);
		List<URL> jarTLDs = ClassPathUtil.findEntries(jars, "/META-INF", "*.tld", true);
		tldURLs.addAll(jarTLDs);

		// 2nd: scan the bundle itself and its fragments using org.osgi.framework.wiring.BundleWiring.findEntries() API.
		// This method doesn't involve classloaders. Just as with WABs, I've decided to treat all reachable bundles
		// (through Import-Package and Require-Bundle) as "application libraries" which also may provide TLDs (when
		// doing the same in pax-web-extender-war, we're searching the reachable bundles for web-fragment.xmls and SCIs)
		Set<Bundle> processedBundles = new HashSet<>();
		Bundle paxWebJsp = FrameworkUtil.getBundle(this.getClass());
		if (paxWebJsp != null) {
			// pax-web-jsp was already scanned in scanPlatform()
			processedBundles.add(paxWebJsp);
		}

		// transitive closure of reachable bundles (not fragments)
		Deque<Bundle> bundles = new LinkedList<>(Collections.singletonList(bundle));
		while (bundles.size() > 0) {
			Bundle b = bundles.pop();
			Set<Bundle> reachable = new HashSet<>();
			ClassPathUtil.getBundlesInClassSpace(b, reachable);
			for (Bundle rb : reachable) {
				if (!processedBundles.contains(rb) && !Utils.isFragment(rb)) {
					bundles.add(rb);
				}
			}
			processedBundles.add(b);
			List<URL> bundleTLDs = ClassPathUtil.findEntries(bundles, "META-INF", "*.tld", true, false);
			tldURLs.addAll(bundleTLDs);
		}

		// and finally parse all TLDs - the ones from Bundle-ClassPath are parsed first - just as with JavaEE
		for (URL tld : tldURLs) {
			try {
				parseTld(new TldResourcePath(tld, null));
			} catch (SAXException | IOException e) {
				LOG.warn("Problem parsing TLD at {}", tld);
			}
		}
	}

	@Override
	protected void parseTld(TldResourcePath path) throws IOException, SAXException {
		// super.parseTld() also check org.apache.jasper.servlet.TldScanner.tldResourcePathTaglibXmlMap, but
		// only after parsing the resource
		if (scanned.contains(path.getUrl())) {
			return;
		}
		LOG.info("Parsing TLD {}", path.getUrl());
		super.parseTld(path);
		scanned.add(path.getUrl());
	}

}
