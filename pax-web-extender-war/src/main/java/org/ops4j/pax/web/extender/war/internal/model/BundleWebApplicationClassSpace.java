/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.annotation.MultipartConfig;

import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import org.apache.tomcat.util.bcel.classfile.EnumElementValue;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.bcel.classfile.SimpleElementValue;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.MultipartDef;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.ops4j.pax.web.extender.war.internal.WarExtenderContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>A wrapper class to give access to different classpath-related aspects of the WAB.</p>
 *
 * <p>The aspects include web fragments, ordered jars and <em>reachable bundles</em> and are used to:<ul>
 *     <li>calculate final web descriptor from reachable fragments</li>
 *     <li>load {@link javax.servlet.ServletContainerInitializer ServletContainerInitializers (SCIs)}</li>
 *     <li>load annotated classes with annontations like {@link javax.servlet.annotation.WebServlet}.</li>
 * </ul></p>
 *
 * <p>It is important to distinguish {@code web-fragment.xml} fragments, JAR fragments and OSGi bundle fragments here.
 * Here's the definition:<ul>
 *     <li>{@code web-fragment.xml} is a partial descriptor that may enhance/alter the <em>main</em> {@code web.xml}
 *         descriptor of the WAB/WAR</li>
 *     <li>JAR fragment is defined in Servlet specification and means a JAR library inside WAR's {@code /WEB-INF/lib}
 *         location of the WAR. Such JAR fragment may be a source of {@code web-fragment.xml} descriptors, source of
 *         SCIs or source of annotated classes. In OSGi we have to extend this definition to all entries specified at
 *         {@code Bundle-ClassPath} manifest header</li>
 *     <li>and of course an OSGi fragment is a bundle attached to the WAB itself or any bundle wired to the WAB (through
 *         package or bundle wiring). Such bundles also may be a source of {@code web-fragment.xml} descriptors,
 *         SCIs and annotated classes.</li>
 * </ul></p>
 */
public class BundleWebApplicationClassSpace {

	public static final Logger LOG = LoggerFactory.getLogger(BundleWebApplicationClassSpace.class);

	/**
	 * Hardcoded equivalent of Tomcat's {@code tomcat.util.scan.StandardJarScanFilter.jarsToSkip} - bundles by
	 * symbolic name.
	 */
	private static final Set<String> IGNORED_BUNDLES;

	private static final Set<ServletContainerInitializer> NO_SCIS = new HashSet<>();

	private final Bundle wabBundle;
	private OsgiServletContextClassLoader wabClassLoader;

	private final WarExtenderContext extenderContext;

	/**
	 * <p>"JAR Fragments" from {@code Bundle-ClassPath} - these may be subject to fragment ordering.
	 * These will represent web fragments with {@link WebXml#getWebappJar()}
	 * = {@code true} whether or not they contain {@code META-INF/web-fragment.xml}.</p>
	 *
	 * <p>"WAB ClassPath" also includes OSGi fragments attached to WAB host. It doesn't contain reachable bundles
	 * (through Import-Package or Require-Bundle).</p>
	 *
	 * <p>The keys are fragment jar names.</p>
	 */
	private final Map<String, URL> wabClassPath = new LinkedHashMap<>();

	/**
	 * <p>"JAR Fragments" from reachable bundles that do not contain {@code META-INF/web-fragment.xml} descriptors,
	 * but are still being searched for SCIs and annotated classes.</p>
	 *
	 * <p>They are never accessible through {@link ServletContext#ORDERED_LIBS} context attribute.</p>
	 *
	 * <p>The keys are fragment jar names.</p>
	 */
	private final Map<String, Bundle> containerFragmentBundles = new LinkedHashMap<>();

	/**
	 * <p>"JAR Fragments" from reachable bundles that contain {@code META-INF/web-fragment.xml} descriptors and
	 * are being searched for SCIs and annotated classes.</p>
	 *
	 * <p>Together with {@link #wabClassPath} fragments they will be accessible through
	 * {@link ServletContext#ORDERED_LIBS} context attribute.</p>
	 *
	 * <p>The keys are fragment jar names.</p>
	 */
	private final Map<String, Bundle> applicationFragmentBundles = new LinkedHashMap<>();

	/**
	 * Actual mapping of "JAR Fragment" name to parsed instances of {@link WebXml}. Used to detect conflicts.
	 * Name is either from {@code web-fragment/name} XML element or from a jar name of the fragment.
	 */
	private final Map<String, WebXml> fragments = new HashMap<>();

	/**
	 * Ordered list of these {@link WebXml} which are selected using absolute/relative ordering, keyed by jar name.
	 * This map includes fragments selected by ordering, but also all "container fragments" (with or without
	 * web-fragment.xml).
	 */
	private final Map<String, WebXml> orderedFragments = new LinkedHashMap<>();

	/**
	 * <p>JAR names of the fragments which are accessible through {@link ServletContext#ORDERED_LIBS} context attribute.
	 * This is a subset of the keys from {@link #orderedFragments}, which doesn't include fragments without
	 * {@code web-fragment.xml}.</p>
	 *
	 * <p>In Tomcat, this subset doesn't include all <em>container fragments</em> (with or without
	 * {@code web-fragment.xml}).</p>
	 */
	private List<String> orderedLibs = null;

	// similar to org.apache.catalina.startup.ContextConfig.ok
	private boolean fragmentParsingOK = true;

	private WebXml mainWebXml;

	static {
		IGNORED_BUNDLES = new HashSet<>(Arrays.asList(
				"javax.el-api", // yes - even in mvn:jakarta.el/jakarta.el-api
				"jakarta.servlet-api",
				"jakarta.annotation-api",
				"org.ops4j.pax.logging.pax-logging-api",
				"org.ops4j.pax.web.pax-web-api",
				"org.ops4j.pax.web.pax-web-spi",
				"org.ops4j.pax.web.pax-web-tomcat-common",
				"org.eclipse.jdt.core.compiler.batch"
		));

		// Tomcat has also predefined list of skipped JARs, which contains:
		// - annotations-api.jar
		// - ant-junit*.jar
		// - ant-launcher.jar
		// - ant.jar
		// - asm-*.jar
		// - aspectj*.jar
		// - bootstrap.jar
		// - catalina-ant.jar
		// - catalina-ha.jar
		// - catalina-ssi.jar
		// - catalina-storeconfig.jar
		// - catalina-tribes.jar
		// - catalina.jar
		// - cglib-*.jar
		// - cobertura-*.jar
		// - commons-beanutils*.jar
		// - commons-codec*.jar
		// - commons-collections*.jar
		// - commons-daemon.jar
		// - commons-dbcp*.jar
		// - commons-digester*.jar
		// - commons-fileupload*.jar
		// - commons-httpclient*.jar
		// - commons-io*.jar
		// - commons-lang*.jar
		// - commons-logging*.jar
		// - commons-math*.jar
		// - commons-pool*.jar
		// - dom4j-*.jar
		// - easymock-*.jar
		// - ecj-*.jar
		// - el-api.jar
		// - geronimo-spec-jaxrpc*.jar
		// - h2*.jar
		// - hamcrest-*.jar
		// - hibernate*.jar
		// - httpclient*.jar
		// - icu4j-*.jar
		// - jasper-el.jar
		// - jasper.jar
		// - jaspic-api.jar
		// - jaxb-*.jar
		// - jaxen-*.jar
		// - jdom-*.jar
		// - jetty-*.jar
		// - jmx-tools.jar
		// - jmx.jar
		// - jsp-api.jar
		// - jstl.jar
		// - jta*.jar
		// - junit-*.jar
		// - junit.jar
		// - log4j*.jar
		// - mail*.jar
		// - objenesis-*.jar
		// - oraclepki.jar
		// - oro-*.jar
		// - servlet-api-*.jar
		// - servlet-api.jar
		// - slf4j*.jar
		// - taglibs-standard-spec-*.jar
		// - tagsoup-*.jar
		// - tomcat-api.jar
		// - tomcat-coyote.jar
		// - tomcat-dbcp.jar
		// - tomcat-i18n-*.jar
		// - tomcat-jdbc.jar
		// - tomcat-jni.jar
		// - tomcat-juli-adapters.jar
		// - tomcat-juli.jar
		// - tomcat-util-scan.jar
		// - tomcat-util.jar
		// - tomcat-websocket.jar
		// - tools.jar
		// - websocket-api.jar
		// - wsdl4j*.jar
		// - xercesImpl.jar
		// - xml-apis.jar
		// - xmlParserAPIs-*.jar
		// - xmlParserAPIs.jar
		// - xom-*.jar

		// TODO: we should parameterize these two skip lists
	}

	/**
	 * Creates a classpace for a {@link Bundle} with "main" web descriptor already parsed. In OSGi, there may be
	 * more {@code web.xml} descriptors found, when WAB itself is a host for some OSGi bundle fragments.
	 *
	 * @param wabBundle
	 * @param extenderContext
	 */
	public BundleWebApplicationClassSpace(Bundle wabBundle, WarExtenderContext extenderContext) {
		this.wabBundle = wabBundle;
		this.extenderContext = extenderContext;
	}

	/**
	 * Returns an indication whether the fragment parsing was ok. If it wasn't, there's no point in trying to discover
	 * SCIs and annotated classes.
	 * @return
	 */
	public boolean isFragmentParsingOK() {
		return fragmentParsingOK;
	}

	public Set<WebXml> getOrderedFragments() {
		return new LinkedHashSet<>(orderedFragments.values());
	}

	public List<String> getOrderedLibs() {
		return orderedLibs;
	}

	/**
	 * Returns root URLs of the JARs included in WAB's {@code Bundle-ClassPath} and root URLs of the OSGi fragments
	 * of the WAB.
	 * @return
	 */
	public Collection<URL> getWabClassPath() {
		return wabClassPath.values();
	}

	/**
	 * Returns root URLs of the bundles reachable through {@code Import-Package} and {@code Require-Bundle}
	 * that contain {@code META-INF/web-fragment.xml}
	 * @return
	 */
	public Map<Bundle, URL> getApplicationFragmentBundles() {
		Map<Bundle, URL> result = new LinkedHashMap<>(applicationFragmentBundles.size());
		for (Bundle b : applicationFragmentBundles.values()) {
			result.put(b, b.getEntry("/"));
		}

		return result;
	}

	/**
	 * This method is based on Tomcat's {@code org.apache.catalina.startup.ContextConfig#processJarsForWebFragments()}.
	 * It scans the reachable JARs and bundles to find (and possibly parse) {@code web-fragment.xml} descriptors.
	 * The detected <em>fragments</em> determine the bundles/libs to scan for SCIs whether or not the metadata is
	 * complete.
	 *
	 * @param mainWebXml
	 * @param wabClassLoader
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public void initialize(WebXml mainWebXml, OsgiServletContextClassLoader wabClassLoader) {
		this.mainWebXml = mainWebXml;
		this.wabClassLoader = wabClassLoader;

		// 8.2.2 Ordering of web.xml and web-fragment.xml
		// this greatly impacts the scanning process for the web fragments and even SCIs
		// - A web-fragment.xml may have a top level <name> element, which will impact the ordering
		// - there are two different orderings:
		//   - absolute ordering specified in "main" web.xml (<absolute-ordering> element)
		//     - if there's no <others>, then only named fragments are considered and other are ignored
		//       - Excluded jars are not scanned for annotated servlets, filters or listeners.
		//       - If a discovered ServletContainerInitializer is loaded from an excluded jar, it will be ignored.
		//       - Irrespective of the setting of metadata-complete, jars excluded by <absolute-ordering> elements are
		//         not scanned for classes to be handled by any ServletContainerInitializer.
		//   - relative ordering (without <absolute-ordering> in "main" web.xml, with <ordering> elements in fragments)
		//       - order is determined using <before> and <after> elements in web-fragment.xml
		Set<String> absoluteOrdering = mainWebXml.getAbsoluteOrdering();
		boolean parseRequired = absoluteOrdering == null || !absoluteOrdering.isEmpty();

		Bundle paxWebJspBundle = Utils.getPaxWebJspBundle(wabBundle);

		// collect all the JARs and bundles that need to be scanned for web-fragments (if metadata-complete="false") or
		// that may provide SCIs (regardles of metadata-complete)
		// In JavaEE it's quite easy:
		//  - /WEB-INF/lib/*.jar files
		//  - the JARs from URLClassLoaders of the ClassLoader hierarchy starting from web context's ClassLoader
		// (Tomcat doesn't scan WEB-INF/classes/META-INF for fragments)
		// In OSGi/PaxWeb it's different, but respectively:
		//  - 128.3.6 Dynamic Content: *.jar files and directories from Bundle-ClassPath (not necessarily including
		//    /WEB-INF/lib/*.jar libs!)
		//  - Arbitrary decision in Pax Web: all the bundles wired to the current bundle (the WAB) via
		//    osgi.wiring.bundle (Require-Bundle) and osgi.wiring.package namespaces (Import-Package)
		//
		// I assume that the WAB bundle itself is not one of the bundles to be searched for web-fragment.xml.
		// These will be searched only from WAB's Bundle-ClassPath and bundle/package wires

		// see org.apache.catalina.startup.ContextConfig.processJarsForWebFragments()

		// 1) take WAB's JARs. We (and Tomcat too) don't want web-fragment.xml from /WEB-INF/classes (or generally
		// from directory entries on Bundle-ClassPath) and we don't want such entries to be represented as WebXml
		// fragments. Chapter 128.3.6 says about using the classes from directory entries of Bundle-ClassPath,
		// but doesn't mention anything about searching these directory entries for web fragments. So we're following
		// Tomcat here.

		LOG.trace("Searching for web fragments in WAB Bundle-ClassPath jars");
		URL[] jars = ClassPathUtil.getClassPathJars(wabBundle, false);
		for (URL url : jars) {
			LOG.trace("  Scanning embedded jar {}", url);
			try {
				WebXml fragment = process(url, parseRequired, extractJarFileName(url.toString()));
				// URL of the JAR, not of its /META-INF/web-fragment.xml, because there may be no such file
				fragment.setWebappJar(true);

				addFragment(fragment);
				wabClassPath.put(fragment.getJarName(), url);
			} catch (Exception e) {
				LOG.warn("  Problem scanning embedded jar {}: {}", url, e.getMessage(), e);
			}
		}

		// 2) The WAB itself may have attached bundle fragments, which should be treated (my decision) as webAppJars
		// i.e., as /WEB-INF/lib/*.jar libs in JavaEE or (using Tomcat terms) shared libraries available to all webapps

		LOG.trace("Searching for web fragments in WAB bundle fragments");
		BundleWiring wiring = wabBundle.adapt(BundleWiring.class);
		if (wiring != null) {
			List<BundleWire> hostWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
			if (hostWires != null) {
				for (BundleWire wire : hostWires) {
					Bundle b = wire.getRequirerWiring().getBundle();
					LOG.trace("  Scanning bundle fragment {}", b);
					try {
						// take bundle.getEntry("/") as the URL of the web fragment
						URL fragmentRootURL = b.getEntry("/");
						WebXml fragment = process(fragmentRootURL, parseRequired, extractJarFileName(b));
						fragment.setWebappJar(true);

						addFragment(fragment);
						wabClassPath.put(fragment.getJarName(), fragmentRootURL);
					} catch (Exception e) {
						LOG.warn("  Problem scanning bundle fragment {}: {}", b, e.getMessage(), e);
					}
				}
			}
		}

		// 3) Scan reachable bundles (ClassPath hierarchy in JavaEE) - without the WAB itself

		LOG.trace("Searching for web fragments in WAB wired bundles");
		Set<Bundle> processedBundles = new HashSet<>();
		Deque<Bundle> bundles = new LinkedList<>();
		// added as already processed, but we need it to get reachable bundles
		bundles.add(wabBundle);
		processedBundles.add(wabBundle);

		if (paxWebJspBundle != null) {
			// this will give us access to Jasper SCI even if WAB doesn't have explicit
			// wires to pax-web-jsp bundle or to other JSTL implementations
			bundles.add(paxWebJspBundle);
		}
		Bundle[] jettyWebSocketBundles = Utils.getJettyWebSocketBundles(wabBundle);
		if (jettyWebSocketBundles != null) {
			for (Bundle b : jettyWebSocketBundles) {
				// this will give us access to Jetty WebSocket SCIs even if WAB doesn't have explicit wires
				if (b != null) {
					bundles.add(b);
				}
			}
		}
		Bundle tomcatWebSocketBundle = Utils.getTomcatWebSocketBundle(wabBundle);
		if (tomcatWebSocketBundle != null) {
			// this will give us access to Tomcat WebSocket SCIs even if WAB doesn't have explicit wires
			bundles.add(tomcatWebSocketBundle);
		}

		while (bundles.size() > 0) {
			// org.apache.tomcat.util.scan.StandardJarScanner.processURLs() - Tomcat traverses CL hierarchy
			// and collects non-filtered (see conf/catalina.properties:
			// "tomcat.util.scan.StandardJarScanFilter.jarsToSkip" property) JARs from all URLClassLoaders
			Bundle scannedBundle = bundles.pop();
			if (IGNORED_BUNDLES.contains(scannedBundle.getSymbolicName()) || scannedBundle.getBundleId() == 0L) {
				continue;
			}

			Set<Bundle> reachable = new HashSet<>();
			ClassPathUtil.getBundlesInClassSpace(scannedBundle, reachable);
			for (Bundle rb : reachable) {
				if (!processedBundles.contains(rb) && !Utils.isFragment(rb)) {
					bundles.add(rb);
				}
			}
			if (processedBundles.contains(scannedBundle)) {
				continue;
			}

			LOG.trace("  Scanning wired bundle {}", scannedBundle);
			try {
				List<WebXml> fragmentList = process(scannedBundle, parseRequired);
				for (WebXml fragment : fragmentList) {
					addFragment(fragment);
					if (!fragment.getWebappJar()) {
						containerFragmentBundles.put(fragment.getJarName(), scannedBundle);
					} else {
						applicationFragmentBundles.put(fragment.getJarName(), scannedBundle);
					}
				}
				processedBundles.add(scannedBundle);
			} catch (Exception e) {
				LOG.warn("  Problem scanning wired bundle {}: {}", scannedBundle, e.getMessage(), e);
			}
		}

		// ServletContext, when passed is used to set important "javax.servlet.context.orderedLibs"
		// attribute, but at this stage, there's no real ServletContext yet. We should provide a mocked one
		AttributeCollectingServletContext context = new AttributeCollectingServletContext();

		// Tomcat orders the fragments according to 8.2.2 Ordering of web.xml and web-fragment.xml (Servlet spec)
		// and it orders:
		//  - container and app fragments according to <absolute-ordering> (if present)
		//  - at the end, container fragments (webappJar=false) are put at the end of the list (if any)
		//  - WAR's fragments (from /WEB-INF/lib/*.jar) are placed as first fragments
		Set<WebXml> fragments = WebXml.orderWebFragments(mainWebXml, this.fragments, context);
		for (WebXml fragment : fragments) {
			orderedFragments.put(fragment.getJarName(), fragment);
		}

		orderedLibs = (List<String>) context.getAttribute(ServletContext.ORDERED_LIBS);

		// After collecting the bundles associated with ordered web fragments, we can finish the "construction"
		// of WAB's classloader.
		// When the WAB is deployed, it'll also get container-specific bundles (like pax-web-undertow) and only
		// then it'll get "closed" to not accept more bundles.
		Set<Bundle> delegateBundles = new LinkedHashSet<>();
		delegateBundles.addAll(applicationFragmentBundles.values());
		delegateBundles.addAll(containerFragmentBundles.values());
		delegateBundles.remove(wabBundle);

		delegateBundles.forEach(this.wabClassLoader::addBundle);
	}

	/**
	 * <p>Process a JAR or other URL from Bundle's {@code Bundle-ClassPath} as a web fragment. This is considered (in
	 * Tomcat's terms) as "webapp JAR". The returned web fragment may, but doesn't have to be <em>backed</em> by
	 * some {@code META-INF/web-fragment.xml}, but it'll always have its {@code name} property set.</p>
	 *
	 * @param url
	 * @param parseRequired is {@code false} only if main {@code web.xml} has explicit, empty {@code <absolute-ordering>}.
	 * @param jarName
	 * @return
	 * @throws IOException
	 */
	private WebXml process(URL url, boolean parseRequired, String jarName) throws IOException {
		WebXml fragment = new WebXml();
		fragment.setName(jarName);
		fragment.setURL(url);
		fragment.setJarName(jarName);

		URL fragmentURL = null;
		if (parseRequired) {
			List<URL> urls = ClassPathUtil.findEntries(wabBundle, new URL[] { url }, "META-INF", "web-fragment.xml", false);
			// there should be at most one, because we pass one URL in the array
			if (urls.size() > 0) {
				fragmentURL = urls.get(0);
			}
		}

		// see org.apache.tomcat.util.descriptor.web.FragmentJarScannerCallback.scan()
		if (fragmentURL != null) {
			// this may fail, but we won't stop the parsing
			if (!extenderContext.getParser().parseWebXml(fragmentURL, fragment, true)) {
				fragmentParsingOK = false;
				LOG.trace("    Found web fragment with invalid descriptor, name: {}, url: {}, jarName: {}",
						fragment.getName(), url, jarName);
			} else {
				LOG.trace("    Found web fragment with descriptor, name: {}, url: {}, jarName: {}",
						fragment.getName(), url, jarName);
			}
		} else {
			LOG.trace("    Found web fragment without descriptor, url: {}, jarName: {}", url, jarName);
		}

		return fragment;
	}

	/**
	 * Process a {@link Bundle} as a web fragment. This is also considered (in Tomcat's terms) as "webapp JAR"
	 * because it's quite hard to split all the runtime bundles into "platform" and "application" bundles.
	 *
	 * @param bundle
	 * @param parseRequired is {@code false} only if main {@code web.xml} has explicit, empty {@code <absolute-ordering>}.
	 * @return
	 * @throws IOException
	 */
	private List<WebXml> process(Bundle bundle, boolean parseRequired) throws IOException {
		// Tomcat's org.apache.catalina.startup.ContextConfig.webConfig() says that "web-fragment.xml files are ignored
		// for _container provided JARs_.". By "container provided JARs" Tomcat means "JARs are treated as application
		// provided until the common class loader is reached".
		// And common class loader is the CL which loaded org.apache.tomcat.util.scan.StandardJarScanner class, which
		// comes from $CATALINA_HOME/lib/tomcat-util-scan.jar
		//
		// https://tomcat.apache.org/tomcat-9.0-doc/class-loader-howto.html#Advanced_configuration allows to
		// configure (conf/catalina.properties: "shared.loader" property) "shared class loader" for which the
		// "common class loader" is a parent. So all jars inside "shared class loader" are actually application
		// libraries

		// In OSGi, we have to decide ourselves which reachable bundle is a "container" bundle and which one is
		// "application" bundle.
		// For example, user may have installed myfaces-impl.jar as a bundle (it contains META-INF/web-fragment.xml with
		// org.apache.myfaces.webapp.StartupServletContextListener) or simply package it inside the WAB.
		// This is out-of-specification decision, because OSGi CMPN 128 Web Application specification says only
		// about the WAB itself (and its Bundle-ClassPath).
		// To make things easier:
		//  - if a reachable bundle (or its bundle fragments) contains META-INF/web-fragment.xml it is treated as
		//    application web fragment (webappJar = true) and is subject to web fragment ordering and the
		//    web-fragment.xml is parsed
		//  - if a reachable bundle (or its bundle fragments) doesn't contain META-INF/web-fragment.xml, it is
		//    treated as container web fragment (webappJar = false), is NOT subject to web fragment ordering, but can
		//    still be a source of SCIs and annotated classes
		// Tomcat doesn't load META-INF/web-fragment.xml files from libraries in common classloader - even
		// if it exists.

		List<URL> fragmentURLs = new LinkedList<>();
		if (parseRequired) {
			List<URL> urls = ClassPathUtil.findEntries(bundle, "META-INF", "web-fragment.xml", false, false);
			// there may be more than one, because we access bundle fragments as well
			fragmentURLs.addAll(urls);
		}

		// see org.apache.tomcat.util.descriptor.web.FragmentJarScannerCallback.scan()
		if (fragmentURLs.size() == 0) {
			WebXml fragment = new WebXml();
			// mark as "container fragment", so it's not affected by the ordering mechanism
			fragment.setWebappJar(false);
			// in Tomcat, it's always URL in case of the location without META-INF/web-fragment.xml, so here it'll
			// be the root of the bundle
			URL fragmentRootURL = bundle.getEntry("/");
			fragment.setURL(fragmentRootURL);
			fragment.setJarName(extractJarFileName(bundle));
			fragment.setName(fragment.getJarName());

			LOG.trace("    Found web fragment without descriptor, url: {}, jarName: {}",
					fragmentRootURL, fragment.getJarName());

			return Collections.singletonList(fragment);
		} else {
			List<WebXml> fragments = new ArrayList<>(fragmentURLs.size());
			for (URL fragmentURL : fragmentURLs) {
				// this may fail, but we won't stop the parsing
				WebXml fragment = new WebXml();
				// mark as "application fragment", so it can be affected by the ordering mechanism because we're
				// loading web-fragment.xml from it
				fragment.setWebappJar(true);
				// we have to turn something like:
				// - bundleentry://48.fwk504807594/META-INF/web-fragment.xml
				// into:
				// - bundleentry://48.fwk504807594/
//				fragment.setURL(new URL(fragmentURL, ".."));
				fragment.setURL(new URL(String.format("%s://%s:%d/",
						fragmentURL.getProtocol(), fragmentURL.getHost(), fragmentURL.getPort())));
				fragment.setJarName(extractJarFileName(fragmentURL.toString()));
				boolean ok = extenderContext.getParser().parseWebXml(fragmentURL, fragment, true);
				if (fragment.getName() == null) {
					fragment.setName(fragment.getJarName());
				}
				if (ok) {
					LOG.trace("    Found web fragment with descriptor, name: {}, url: {}, jarName: {}",
							fragment.getName(), fragment.getURL(), fragment.getJarName());
				} else {
					LOG.trace("    Found web fragment with invalid descriptor, name: {}, url: {}, jarName: {}",
							fragment.getName(), fragment.getURL(), fragment.getJarName());
				}
				fragmentParsingOK &= ok;
				fragments.add(fragment);
			}
			return fragments;
		}
	}

	private String extractJarFileName(String uri) {
		if (uri.startsWith("bundle://") && uri.indexOf('.') > 10) {
			// Felix: bundle://42.0:0/META-INF/web-fragment.xml
			//        org.apache.felix.framework.BundleRevisionImpl.createURL()
			//        m_id = bundle ID (id + '.' + revision), port = content index
			try {
				String id = uri.substring(9, uri.indexOf('.'));
				Bundle b = wabBundle.getBundleContext().getBundle(Long.parseLong(id));
				return extractJarFileName(b);
			} catch (Exception e) {
				return uri;
			}
		} else if (uri.startsWith("bundleentry://") && uri.indexOf('.') > 15) {
			// Equinox: bundleentry://42.fwk1161322357/META-INF/web-fragment.xml
			//          org.eclipse.osgi.framework.util.SecureAction.getURL()
			//          id = org.eclipse.osgi.storage.BundleInfo.bundleId + '.fwk' + org.eclipse.osgi.container.ModuleContainer.hashCode()
			try {
				String id = uri.substring(14, uri.indexOf('.'));
				Bundle b = wabBundle.getBundleContext().getBundle(Long.parseLong(id));
				return extractJarFileName(b);
			} catch (Exception e) {
				return uri;
			}
		}
		if (uri.endsWith("!/")) {
			uri = uri.substring(0, uri.length() - 2);
		}
		return uri.substring(uri.lastIndexOf('/') + 1);
	}

	private String extractJarFileName(Bundle bundle) {
		return String.format("%s-%s.bundle", bundle.getSymbolicName(), bundle.getVersion().toString());
	}

	/**
	 * See {@code org.apache.tomcat.util.descriptor.web.FragmentJarScannerCallback.addFragment()}
	 * @param fragment
	 */
	private void addFragment(WebXml fragment) {
		if (fragments.containsKey(fragment.getName())) {
			String duplicateName = fragment.getName();
			// this will fortunately cause an error later
			// this may happen if the WAB has a bundle both wired and embedded, but unfortunately we can't
			// provide nice message, as both jarName and URL properties of WebXml will be different.
			fragments.get(duplicateName).setDuplicated(true);
			if (fragment.getJarName() != null) {
				// Rename the current fragment so it doesn't clash
				LOG.warn("There already exists a web fragment named {}. Renaming to {}.", duplicateName, fragment.getJarName());
				fragment.setName(fragment.getJarName());
			} else {
				throw new IllegalArgumentException("Can't add a fragment named \"" + fragment.getName() + "\". It is already added.");
			}
		}
		fragments.put(fragment.getName(), fragment);
	}

	/**
	 * Entire class space is used to discover and load instances of {@link ServletContainerInitializer}.
	 * @return
	 */
	public List<ServletContainerInitializer> loadSCIs() throws IOException {
		// a list of URIs of /META-INF/service/javax.servlet.ServletContainerInitializer from reachable
		// "container" bundles (ones without META-INF/web-fragment.xml) each has to be loaded using relevant bundle
		Map<Bundle, List<URL>> containerSCIURLs = new LinkedHashMap<>();
		// a list of URIs of /META-INF/service/javax.servlet.ServletContainerInitializer from reachable
		// "application" bundles (ones with META-INF/web-fragment.xml)
		Map<Bundle, List<URL>> applicationSCIURLs = new LinkedHashMap<>();

		// a list of URIs of /META-INF/service/javax.servlet.ServletContainerInitializer from WAB's Bundle-ClassPath
		// each has to be loaded using WAB itself
		List<URL> wabSCIURLs = new LinkedList<>();

		// Tomcat loads:
		// - all /META-INF/services/javax.servlet.ServletContainerInitializer files from
		//   the parent of webapp's class loader (the "container provided SCIs")
		// - SCIs from the webapp and its /WEB-INF/lib/*.jars (each or those mentioned in
		//   javax.servlet.context.orderedLibs context attribute)
		// - java.lang.ClassLoader.getResources() method is used
		// see: org.apache.catalina.startup.WebappServiceLoader.load()

		// so while web-fragment.xml (and even libs without web-fragment.xml when metadata-complete="false")
		// descriptors are processed in established order for the purpose of manifest building, they are
		// scanned for ServletContainerInitializers in strict order:
		// 1. container libs
		// 2. /WEB-INF/lib/*.jar libs (potentially filtered by the absolute ordering)
		// Tomcat ensures that a WAR may override an SCI implementation by using single
		// javax.servlet.ServletContext.getClassLoader() to load SCI implementations whether the
		// /META-INF/service/javax.servlet.ServletContainerInitializer was loaded from container or webapp lib

		// We can't load the services as entries (without using classloaders), because a bundle may have
		// custom Bundle-ClassPath (potentially with many entries). So while META-INF/web-fragment.xml has
		// fixed location in a bundle (or fragment of WAB's JAR),
		// META-INF/services/javax.servlet.ServletContainerInitializer has to be found using classLoaders, to
		// pick up for example WEB-INF/classes/META-INF/servcontainerSCIURLs = {java.util.LinkedHashMap@5634}  size = 13ices/javax.servlet.ServletContainerInitializer if
		// WEB-INF/classes is on a Bundle-ClassPath

		String sciService = "META-INF/services/" + ServletContainerInitializer.class.getName();

		// container bundles are the ones without META-INF/web-fragment.xml, so they're unique, as there is only
		// one (descriptorless) fragment for a reachable bundle
		LOG.trace("Searching for ServletContainerInitializers in container fragments");
		for (String fragment : containerFragmentBundles.keySet()) {
			Bundle reachableBundle = containerFragmentBundles.get(fragment);
			LOG.trace("  Scanning container fragment {}", fragment);
			List<URL> urls = ClassPathUtil.getResources(Collections.singletonList(reachableBundle), sciService);
			containerSCIURLs.put(reachableBundle, urls);
			if (LOG.isTraceEnabled()) {
				for (URL url : urls) {
					LOG.trace("    Found SCI service {}", url);
				}
			}
		}

		if (orderedLibs == null) {
			// all reachable bundles that contain META-INF/web-fragment.xml descriptors (so we have to make
			// the bundle collection unique), but mind that user may both have a bundle called
			// (org.apache.tomcat.util.descriptor.web.WebXml.getJarName()) "myfaces-impl-2.3.3.jar" and have the same
			// library on Bundle-ClassPath.
			Set<Bundle> processed = new HashSet<>();
			LOG.trace("Searching for ServletContainerInitializers in application fragments");
			for (Map.Entry<String, Bundle> reachableBundle : applicationFragmentBundles.entrySet()) {
				String fragmentJarName = reachableBundle.getKey();
				Bundle b = reachableBundle.getValue();
				if (!wabClassPath.containsKey(fragmentJarName)) {
					// only if user doesn't have the same fragment embedded
					if (processed.add(b)) {
						LOG.trace("  Scanning application fragment {}", fragmentJarName);
						List<URL> urls = ClassPathUtil.getResources(Collections.singletonList(b), sciService);
						applicationSCIURLs.put(b, urls);
						if (LOG.isTraceEnabled()) {
							for (URL url : urls) {
								LOG.trace("    Found SCI service {}", url);
							}
						}
					}
				}
			}

			// all entries from Bundle-ClassPath, so ClassLoader kind of access - we can't impact the order
			// of Bundle-ClassPath entries, but it doesn't really matter. Tomcat uses
			// javax.servlet.ServletContext.getClassLoader().getResources()
			LOG.trace("Searching for ServletContainerInitializers in the WAB");
			LOG.trace("  Scanning the WAB");
			List<URL> urls = ClassPathUtil.getResources(Collections.singletonList(wabBundle), sciService);
			wabSCIURLs.addAll(urls);
			if (LOG.isTraceEnabled()) {
				for (URL url : urls) {
					LOG.trace("    Found SCI service {}", url);
				}
			}
		} else {
			// we can't use ClassPathUtil.getResources() because maybe we have to skip some JARs from Bundle-ClassPath

			// before checking ORDERED_LIBS, we (and Tomcat) always checks WEB-INF/classes/META-INF/services
			// Tomcat calls javax.servlet.ServletContext.getResource(), here we have to load the SCI service
			// from the WAB itself, but ensure (which isn't straightforward) that we skip embedded JARs and
			// WAB bundle fragments)
			// - ClassPathUtil.listResources() uses BundleWiring.listResources() which removes duplicates,
			//   so we can't use it
			// - bundle.getResources() is ok, but returns also URLs from fragments and other libs on Bundle-ClassPath
			//   but we want only the URLs for non-jar entries of Bundle-ClassPath (not necessarily only
			//   /WEB-INF/classes!) and (later) from jar entries listed in ORDERED_LIBS and even fragments
			//   we (out of spec) list in ORDERED_LIST
			//   Because we're checking roots from the WAB anyway (later), we can't use bundle.getResources(), as
			//    - bundle://46.0:5/META-INF/services/javax.servlet.ServletContainerInitializer, and
			//    - bundle://45.0:0/META-INF/services/javax.servlet.ServletContainerInitializer
			//   are actually the same resources (first URL is WAB's fragemnt seen through WAB's classloader
			//   and second is an URL of the fragment itself. Same for:
			//    - bundle://46.0:2/META-INF/services/javax.servlet.ServletContainerInitializer, and
			//    - jar:bundle://46.0:0/WEB-INF/lib/the-wab-jar-8.0.0-SNAPSHOT.jar!/META-INF/services/javax.servlet.ServletContainerInitializer

			LOG.trace("Searching for ServletContainerInitializers in the WAB");
			LOG.trace("  Scanning the WAB directory entries");
			List<URL> wabURLs = ClassPathUtil.findEntries(wabBundle, ClassPathUtil.getClassPathNonJars(wabBundle),
					"META-INF/services", ServletContainerInitializer.class.getName(), false);
			wabSCIURLs.addAll(wabURLs);
			if (LOG.isTraceEnabled()) {
				for (URL url : wabURLs) {
					LOG.trace("    Found SCI service {}", url);
				}
			}

			// selected (also: none) entries from Bundle-ClassPath - only JARs (JavaEE compliant) and
			// WAB-attached bundle fragments (Pax Web addition - we list them as "<bundle-sn>-<bundle-version>.bundle"
			// entries)
			// ORDERED_LIBS contains jar names, so we have to translate them back
			// ORDERED_LIBS doesn't contain jar names for fragments without web-fragment.xml
			LOG.trace("Searching for ServletContainerInitializers in ordered fragments");
			Map<Bundle, String> processed = new HashMap<>();
			for (String jarName : orderedLibs) {
				if (wabClassPath.containsKey(jarName)) {
					LOG.trace("  Scanning embedded JAR {}", jarName);
					List<URL> urls = ClassPathUtil.findEntries(wabBundle, new URL[] { orderedFragments.get(jarName).getURL() },
							"META-INF/services", ServletContainerInitializer.class.getName(), false);
					wabSCIURLs.addAll(urls);
					if (LOG.isTraceEnabled()) {
						for (URL url : urls) {
							LOG.trace("    Found SCI service {}", url);
						}
					}
				} else if (applicationFragmentBundles.containsKey(jarName)) {
					// some web fragments may be associated with the same bundle (when a bundle has bundle fragments)
					// so we have to skip those
					Bundle fragmentBundle = applicationFragmentBundles.get(jarName);
					if (processed.containsKey(fragmentBundle)) {
						LOG.trace("  Skipping application fragment {} (SCIs already loaded through {} from fragment {})",
								jarName, fragmentBundle, processed.get(fragmentBundle));
						continue;
					}
					processed.put(fragmentBundle, jarName);
					LOG.trace("  Scanning application fragment {}", jarName);
					// take it from reachable bundles containing META-INF/web-fragment.xml
					List<URL> urls = ClassPathUtil.findEntries(fragmentBundle, new URL[] { orderedFragments.get(jarName).getURL() },
							"META-INF/services", ServletContainerInitializer.class.getName(), false);
					applicationSCIURLs.put(fragmentBundle, urls);
					if (LOG.isTraceEnabled()) {
						for (URL url : urls) {
							LOG.trace("    Found SCI service {}", url);
						}
					}
				}
			}
		}

		final List<ServletContainerInitializer> detectedSCIs = new LinkedList<>();

		LOG.trace("Instantiating ServletContainerInitializers");

		// SCIs from bundles without web-fragment.xml - loaded from respective bundles
		containerSCIURLs.forEach((b, urls) -> {
			LOG.trace("  Loading SCIs from {}", b);
			for (URL url : urls) {
				loadSCI(url, b, detectedSCIs);
			}
		});

		// SCIs from bundles with web-fragment.xml - loaded from respective bundles
		applicationSCIURLs.forEach((b, urls) -> {
			LOG.trace("  Loading SCIs from {}", b);
			for (URL url : urls) {
				loadSCI(url, b, detectedSCIs);
			}
		});

		// WAB SCIs - loaded from the WAB itself
		LOG.trace("  Loading SCIs from WAB");
		wabSCIURLs.forEach(url -> loadSCI(url, wabBundle, detectedSCIs));

		return detectedSCIs;
	}

	private void loadSCI(URL url, Bundle bundle, List<ServletContainerInitializer> scis) {
		LOG.trace("    Loading {}", url);
		try (InputStream is = url.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				String name = line.trim();
				if (name.startsWith("#")) {
					continue;
				}
				int idx = name.indexOf('#');
				if (idx > 0) {
					name = name.substring(0, idx).trim();
				}
				if (name.length() > 0) {
					try {
						Class<?> sciClass = bundle.loadClass(name);
						ServletContainerInitializer sci = (ServletContainerInitializer) sciClass.newInstance();
						LOG.trace("      Loaded SCI {}", sci.getClass());
						scis.add(sci);
					} catch (ClassNotFoundException | ClassCastException | InstantiationException | IllegalAccessException e) {
						LOG.error("      Problem loading SCI class from {}: {}", url, e.getMessage(), e);
					}
				}
			}
		} catch (IOException e) {
			LOG.error("    Problem reading SCI service class from {}: {}", url, e.getMessage(), e);
		}
	}

	/**
	 * <p>Multi-purpose method to detect two kinds of types from the class space:<ul>
	 *     <li>Types annotated with annotations from "8.1 Annotations and pluggability" of the servlet spec (at least
	 *         those mentioned in 8.1.1-8.1.5 (See {@code org.apache.catalina.startup.ContextConfig#processClass()}).</li>
	 *     <li>Types matching the configuration from {@link javax.servlet.annotation.HandlesTypes} specified for SCIs.</li>
	 * </ul>
	 * </p>
	 *
	 * @param htToSci populated mapping of values from {@link javax.servlet.annotation.HandlesTypes} to SCIs that
	 *        are interested in related types
	 * @param sciToHt newly constructed mapping of previously detected SCIs to actual types that have to be passed to
	 *        {@link ServletContainerInitializer#onStartup(Set, ServletContext)}.
	 * @param thereAreHTClasses {@code true} if any of the SCIs has any non-annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 * @param thereAreHTAnnotations {@code true} if any of the SCIs has any annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 * @return
	 */
	public void scanClasses(Map<Class<?>, Set<ServletContainerInitializer>> htToSci,
			Map<ServletContainerInitializer, Set<Class<?>>> sciToHt,
			boolean thereAreHTClasses, boolean thereAreHTAnnotations) throws IOException {

		Map<String, ClassCacheEntry> javaClassCache = new HashMap<>();
		ClassCacheEntry root = new ClassCacheEntry();
		root.scis = NO_SCIS;
		root.superClassName = Object.class.getName();
		root.interfaceNames = new String[0];
		javaClassCache.put(root.superClassName, root);

		// 1. scan classes in non-JAR entries from WAB's Bundle-ClassPath
		//     - always for types from @HandlesTypes
		//     - possibly for other annotated types if metadata-complete="false" on this.mainWebXml
		LOG.trace("Scanning classes in WAB directory entries");

		URL[] urls = ClassPathUtil.getClassPathNonJars(wabBundle);
		boolean htOnly = mainWebXml.isMetadataComplete();
		Set<String> processedRoots = new HashSet<>();

		for (URL url : urls) {
			LOG.trace("  Scanning embedded directory: {}", url);
			List<URL> classes = ClassPathUtil.findEntries(wabBundle, new URL[] { url }, "/", "*.class", true);
			for (URL u : classes) {
				processedRoots.add(u.toExternalForm());
				if (!u.getPath().endsWith(".class")) {
					// skip entries like
					// "jar:bundle://40.0:0/WEB-INF/lib/spring-core-5.3.6.jar!/org/springframework/core/type/classreading/"
					continue;
				}
				LOG.trace("    Scanning {}", u);
				processClass(u, mainWebXml, wabBundle, htOnly, htToSci, sciToHt, javaClassCache,
						thereAreHTClasses, thereAreHTAnnotations);
			}
		}

		// 2. scan all ordered jars - not only those from javax.servlet.ServletContext.ORDERED_LIBS, but really
		//    all "web fragments" - both with and without web-fragment.xml descriptors.
		//     - always for types from @HandlesTypes
		//     - annotated types are NOT scanned if:
		//        - this.mainWebXml's metadata-complete="true"
		//        - fragment's metadata-complete="true"
		//        - fragment doesn't have web-fragment.xml (or it's a "container jar" in Tomcat) -> getWebappJar() == false
		LOG.trace("Scanning classes in ordered fragments");

		Map<String, Bundle> allFragmentBundles = new LinkedHashMap<>(containerFragmentBundles);
		allFragmentBundles.putAll(applicationFragmentBundles);
		Map<Bundle, String> processed = new HashMap<>();

		for (Map.Entry<String, WebXml> entry : orderedFragments.entrySet()) {
			String jarName = entry.getKey();
			WebXml fragment = entry.getValue();
			boolean bundleFragment = jarName.endsWith(".bundle");
			Bundle fragmentBundle = allFragmentBundles.get(jarName);
			if (fragmentBundle == null) {
				// for embedded jars from Bundle-ClassPath of the WAB itself
				fragmentBundle = wabBundle;
			}
			if (processed.containsKey(fragmentBundle) && bundleFragment) {
				// because if it endsWith(".jar") it has different fragment.getURL() pointing to embedded jar
				LOG.trace("  Skipping ordered fragment {} (already scanned through {} from fragment {})",
						jarName, fragmentBundle, processed.get(fragmentBundle));
				continue;
			}
			if (bundleFragment) {
				processed.put(fragmentBundle, jarName);
			}
			LOG.trace("  Scanning ordered fragment {}, {} ({})", jarName, fragment.getURL(),
					fragment.getWebappJar() ? "WAB" : "container");

			List<URL> classes = ClassPathUtil.findEntries(fragmentBundle,
					new URL[] { fragment.getURL() }, "/", "*.class", true);
			boolean fragmentHtOnly = htOnly || fragment.isMetadataComplete() || !fragment.getWebappJar();
			for (URL u : classes) {
				if (fragmentBundle == wabBundle && bundleFragment) {
					// when a bundle fragment is scanned through WABs bundle, *.class resources found using
					// org.osgi.framework.Bundle.findEntries() will check both attached bundle fragments and
					// normal directories within the WAB - we should skip these directories that were already
					// scanned (among directory entries of Bundle-ClassPath)
					String ef = u.toExternalForm();
					boolean skip = false;
					for (String pr : processedRoots) {
						if (ef.startsWith(pr)) {
							skip = true;
							break;
						}
					}
					if (skip) {
						LOG.trace("    Skipping {}", u);
						continue;
					}
				}
				if (!u.getPath().endsWith(".class")) {
					// skip entries like
					// "jar:bundle://40.0:0/WEB-INF/lib/spring-core-5.3.6.jar!/org/springframework/core/type/classreading/"
					continue;
				}
				LOG.trace("    Scanning {}", u);
				processClass(u, fragment, fragmentBundle, fragmentHtOnly, htToSci, sciToHt, javaClassCache,
						thereAreHTClasses, thereAreHTAnnotations);
			}
		}

		javaClassCache.clear();
	}

	/**
	 * Check the class whether it's one of the types mentioned in {@link javax.servlet.annotation.HandlesTypes}
	 * and also potentially check it for annotations like {@link javax.servlet.annotation.WebServlet}.
	 * @param url an URI for {@code *.class} file
	 * @param fragment a {@link WebXml} representing a "web fragment" - whether or not it is associated with
	 *        {@code web-fragment.xml}
	 * @param bundle {@link Bundle} used to load the classes from
	 * @param fragmentHtOnly {@code true} if the fragment is "metadata complete" or it's a "container fragment"
	 * @param htToSci
	 * @param sciToHt
	 * @param javaClassCache
	 * @param thereAreHTClasses {@code true} if any of the SCIs has any non-annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 * @param thereAreHTAnnotations {@code true} if any of the SCIs has any annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 */
	private void processClass(URL url, WebXml fragment, Bundle bundle, boolean fragmentHtOnly,
			Map<Class<?>, Set<ServletContainerInitializer>> htToSci,
			Map<ServletContainerInitializer, Set<Class<?>>> sciToHt, Map<String, ClassCacheEntry> javaClassCache,
			boolean thereAreHTClasses, boolean thereAreHTAnnotations) {
		try (InputStream is = url.openStream()) {
			ClassParser parser = new ClassParser(is);
			JavaClass clazz = parser.parse();
			if (thereAreHTClasses || thereAreHTAnnotations) {
				if ((clazz.getAccessFlags() & org.apache.tomcat.util.bcel.Const.ACC_ANNOTATION) == 0) {
					// check only a non-annotation *.class, whether it:
					// - is annotated with any annotation from @HandlesTypes
					// - implements an interface from @HandlesTypes
					// - extends a class from from @HandlesTypes
					checkHandlesTypes(clazz, bundle, htToSci, sciToHt, javaClassCache,
							thereAreHTClasses, thereAreHTAnnotations);
				}
			}

			if (!fragmentHtOnly) {
				// do not check if the class should be scanned for annotations like @WebServlet, @WebFilter, ...
				checkClass(fragment, bundle, htToSci, clazz, javaClassCache);
			}
		} catch (IOException e) {
			LOG.warn("Can't read {}: {}", url, e.getMessage(), e);
		}
	}

	/**
	 * <p>Checks whether the passed {@link JavaClass} is <em>indirectly</em> referred to from an "interest list" of any
	 * SCI that has {@link javax.servlet.annotation.HandlesTypes} annoation.</p>
	 *
	 * <p>The goal is to turn the types mentioned in {@link javax.servlet.annotation.HandlesTypes} into actual types
	 * that are either annotated, extend or implement the types from {@link javax.servlet.annotation.HandlesTypes}.</p>
	 *
	 * @param clazz
	 * @param bundle
	 * @param htToSci input map of types from {@link javax.servlet.annotation.HandlesTypes} to SCIs
	 * @param sciToHt map under construction of SCIs to actual types passed later to
	 *        {@link ServletContainerInitializer#onStartup(Set, ServletContext)}
	 * @param javaClassCache
	 * @param thereAreHTClasses {@code true} if any of the SCIs has any non-annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 * @param thereAreHTAnnotations {@code true} if any of the SCIs has any annotation types among values of
	 *        {@link javax.servlet.annotation.HandlesTypes}
	 */
	private void checkHandlesTypes(JavaClass clazz, Bundle bundle,
			Map<Class<?>, Set<ServletContainerInitializer>> htToSci,
			Map<ServletContainerInitializer, Set<Class<?>>> sciToHt,
			Map<String, ClassCacheEntry> javaClassCache, boolean thereAreHTClasses, boolean thereAreHTAnnotations) {

		String className = clazz.getClassName();
		Class<?> loadedClass = null;

		if (thereAreHTClasses) {
			// check if this JavaClass:
			//  - has a superclass mentioned in @HandlesTypes
			//  - implements an interface mentioned in @HandlesTypes
			// it's not that easy... for example, an SCI may have simply javax.servlet.Servlet in its @HandlesTypes
			// and passed JavaClass is a class loaded from bundle-A that extends a class loaded from bundle-B, which
			// extends org.springframework.web.servlet.DispatcherServlet class loaded from spring-web, which
			// *is* a javax.servlet.Servlet. we can get org/springframework/web/servlet/DispatcherServlet.class
			// resource (to analyze using BCEL/ASM) using only spring-web bundle and while bundle-B *should* be wired
			// to spring-web, spring-B *doesn't have to*.

			// let's have this hierarchy:
			// java.lang.Object
			// +-- com.example.Impl1 implements com.example.Interface1
			//     +-- com.example.Impl2 implements com.example.Interface2
			//
			// and there are two SCIs:
			// - com.example.SCI1 with @HandlesTypes({com.example.Interface1})
			// - com.example.SCI2 with @HandlesTypes({com.example.Interface2})
			//
			// when calling SCI.onStartup():
			// - SCI1 should get both Impl1 and Impl2
			// - SCI2 should get Impl2 only

			addSuperClassesAndInterfacesToTheCache(clazz, className, bundle, htToSci, javaClassCache);
			ClassCacheEntry cce = javaClassCache.get(className);
			if (!cce.scis.isEmpty()) {
				// we have to load the class
				try {
					LOG.trace("      Loading {}, using {}", className, bundle);
					loadedClass = bundle.loadClass(className);
				} catch (Throwable t) {
					if (LOG.isTraceEnabled()) {
						LOG.trace("      Can't load {}, using {}: {}. Skipping.", className, bundle, t.getMessage());
					}
					return;
				}
				if (loadedClass == null) {
					return;
				}
				// we already know which SCIs have @HT with this class' superclass or interfaces
				for (ServletContainerInitializer sci : cce.scis) {
					sciToHt.computeIfAbsent(sci, s -> new HashSet<>()).add(loadedClass);
				}
			}
		}

		if (thereAreHTAnnotations) {
			// check if this JavaClass:
			//  - is annotated with a type mentioned in @HandlesTypes (see https://bz.apache.org/bugzilla/show_bug.cgi?id=65244)
			if (clazz.getAnnotationEntries() == null) {
				return;
			}
			for (AnnotationEntry ae : clazz.getAnnotationEntries()) {
				// type is in "Ljavax/servlet/annotation/HandlesTypes;" form
				String annotationClassName = className(ae.getAnnotationType());
				if (annotationClassName != null) {
					// does any SCI have @HT with this exact annotation?
					for (Map.Entry<Class<?>, Set<ServletContainerInitializer>> entry : htToSci.entrySet()) {
						Class<?> c = entry.getKey();
						if (!c.isAnnotation()) {
							continue;
						}
						Set<ServletContainerInitializer> scis = entry.getValue();
						if (annotationClassName.equals(c.getName())) {
							if (loadedClass == null) {
								// could've been loaded when checking for types
								try {
									LOG.trace("      Loading {} annotated with {}, using {}",
											className, annotationClassName, bundle);
									loadedClass = bundle.loadClass(className);
								} catch (Throwable t) {
									if (LOG.isTraceEnabled()) {
										LOG.trace("      Can't load {} annotated with {}, using {}: {}. Skipping.",
												className, annotationClassName, bundle, t.getMessage());
									}
									return;
								}
								if (loadedClass == null) {
									return;
								}
							}
							// all SCIs interested in this type needs to get real related class
							for (ServletContainerInitializer sci : entry.getValue()) {
								sciToHt.computeIfAbsent(sci, s -> new HashSet<>()).add(loadedClass);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Quite easy checking whether the {@link JavaClass} is an annotated servlet, filter or listener. These are three
	 * required types to check according to chapters 8.1.1-8.1.5 of the servlet specification.
	 *
	 * @param fragment
	 * @param bundle
	 * @param htToSci
	 * @param clazz
	 * @param javaClassCache
	 */
	private void checkClass(WebXml fragment, Bundle bundle, Map<Class<?>, Set<ServletContainerInitializer>> htToSci, JavaClass clazz, Map<String, ClassCacheEntry> javaClassCache) {
		AnnotationEntry[] ae = clazz.getAnnotationEntries();
		if (ae == null) {
			return;
		}

		String webElementClassName = clazz.getClassName();
		addSuperClassesAndInterfacesToTheCache(clazz, webElementClassName, bundle, htToSci, javaClassCache);

		for (AnnotationEntry ann : ae) {
			switch (className(ann.getAnnotationType())) {
				case "javax.servlet.annotation.WebServlet":
					LOG.trace("      Processing annotated servlet {}", webElementClassName);
					processAnnotatedServletClass(webElementClassName, fragment, bundle, ann, clazz, javaClassCache);
					return;
				case "javax.servlet.annotation.WebFilter":
					LOG.trace("      Processing annotated filter {}", webElementClassName);
					processAnnotatedFilterClass(webElementClassName, fragment, bundle, ann, clazz);
					return;
				case "javax.servlet.annotation.WebListener":
					LOG.trace("      Processing annotated listener {}", webElementClassName);
					fragment.addListener(webElementClassName);
					return;
				default:
			}
		}
	}

	private void processAnnotatedServletClass(String className, WebXml fragment, Bundle bundle, AnnotationEntry ann, JavaClass clazz, Map<String, ClassCacheEntry> javaClassCache) {
		// no idea whether to process annotations on superclasses/interfaces...
		boolean extendsHttpServlet = false;
		String sc = clazz.getSuperclassName();
		while (true) {
			if ("java.lang.Object".equals(sc)) {
				break;
			}
			if ("javax.servlet.http.HttpServlet".equals(sc)) {
				extendsHttpServlet = true;
				break;
			}
			sc = javaClassCache.get(sc).superClassName;
		}
		if (!extendsHttpServlet) {
			LOG.warn("{} annotated with @WebServlet doesn't extend javax.servlet.http.HttpServlet."
					+ " See chapter 8.1.1 of the Servlet specification.", className);
			return;
		}

		String servletName = className;
		String[] urlPatterns = null;
		Map<String, String> initParams = new LinkedHashMap<>();
		String loadOnStartup = null;
		String asyncSupported = null;
		String description = null;
		String displayName = null;

		List<ElementValuePair> pairs = ann.getElementValuePairs();
		for (ElementValuePair evp : pairs) {
			String annKey = evp.getNameString();
			ElementValue annValue = evp.getValue();

			switch (annKey) {
				case "name":
					servletName = annValue.stringifyValue();
					break;
				case "value":
				case "urlPatterns":
					if (urlPatterns != null) {
						LOG.warn("{} has both value and urlPatterns in @WebServlet annotation", className);
						break;
					}
					urlPatterns = extractStrings(className, annValue);
					break;
				case "loadOnStartup":
					loadOnStartup = annValue.stringifyValue();
					break;
				case "initParams":
					extractInitParams(className, annValue, initParams);
					break;
				case "asyncSupported":
					asyncSupported = annValue.stringifyValue();
					break;
				case "description":
					description = annValue.stringifyValue();
					break;
				case "displayName":
					displayName = annValue.stringifyValue();
					break;
				default:
					// skip small/large icons
					break;
			}
		}

		boolean override = false;
		ServletDef servlet = fragment.getServlets().get(servletName);
		if (servlet != null) {
			// annotated servlet overrides a servlet from web(-fragment).xml
			override = true;
		} else {
			servlet = new ServletDef();
			servlet.setServletName(servletName);
			// yes - we can't override a class in existing servlet
			servlet.setServletClass(className);
		}

		if (servlet.getAsyncSupported() == null) {
			servlet.setAsyncSupported(asyncSupported);
		}
		if (servlet.getLoadOnStartup() == null && loadOnStartup != null) {
			servlet.setLoadOnStartup(loadOnStartup);
		}
		if (servlet.getDescription() == null) {
			servlet.setDescription(description);
		}
		if (servlet.getDisplayName() == null) {
			servlet.setDisplayName(displayName);
		}

		if (override) {
			Map<String, String> existingInitParams = servlet.getParameterMap();
			for (Map.Entry<String, String> e : initParams.entrySet()) {
				String key = e.getKey();
				if (!existingInitParams.containsKey(key)) {
					servlet.addInitParameter(e.getKey(), e.getValue());
				}
			}
		} else {
			for (Map.Entry<String, String> e : initParams.entrySet()) {
				servlet.addInitParameter(e.getKey(), e.getValue());
			}
		}

		// get remaining annotation entries to find @javax.servlet.annotation.MultipartConfig
		AnnotationEntry[] ae = clazz.getAnnotationEntries();
		for (AnnotationEntry e : ae) {
			if (MultipartConfig.class.getName().equals(className(e.getAnnotationType()))) {
				MultipartDef multipartDef = new MultipartDef();
				servlet.setMultipartDef(multipartDef);
				multipartDef.setFileSizeThreshold("0");
				multipartDef.setMaxFileSize("-1");
				multipartDef.setMaxRequestSize("-1");

				for (ElementValuePair evp : e.getElementValuePairs()) {
					String annKey = evp.getNameString();
					ElementValue annValue = evp.getValue();
					switch (annKey) {
						case "location":
							multipartDef.setLocation(annValue.stringifyValue());
							break;
						case "fileSizeThreshold":
							multipartDef.setFileSizeThreshold(annValue.stringifyValue());
							break;
						case "maxFileSize":
							multipartDef.setMaxFileSize(annValue.stringifyValue());
							break;
						case "maxRequestSize":
							multipartDef.setMaxRequestSize(annValue.stringifyValue());
							break;
						default:
							break;
					}
				}

				break;
			}
		}

		if (!override) {
			fragment.addServlet(servlet);
		}
		if (!fragment.getServletMappings().containsValue(servletName) && urlPatterns != null) {
			// do not add new mappings for this servlet name if ANY exists
			for (String p : urlPatterns) {
				fragment.addServletMapping(p, servletName);
			}
		}
	}

	private void processAnnotatedFilterClass(String className, WebXml fragment, Bundle bundle, AnnotationEntry ann, JavaClass clazz) {
		// no idea whether to process annotations on superclasses/interfaces...
		boolean hasFilter = false;
		for (String name : clazz.getInterfaceNames()) {
			if ("javax.servlet.Filter".equals(name)) {
				hasFilter = true;
				break;
			}
		}

		if (!hasFilter) {
			LOG.warn("{} annotated with @WebFilter doesn't implement javax.servlet.Filter."
					+ " See chapter 8.1.2 of the Servlet specification.", className);
			return;
		}

		String filterName = className;
		String[] urlPatterns = null;
		String[] servletNames = null;
		String[] dispatcherTypes = null;
		Map<String, String> initParams = new LinkedHashMap<>();
		String asyncSupported = null;
		String description = null;
		String displayName = null;

		List<ElementValuePair> pairs = ann.getElementValuePairs();
		for (ElementValuePair evp : pairs) {
			String annKey = evp.getNameString();
			ElementValue annValue = evp.getValue();

			switch (annKey) {
				case "filterName":
					filterName = annValue.stringifyValue();
					break;
				case "value":
				case "urlPatterns":
					if (urlPatterns != null) {
						LOG.warn("{} has both value and urlPatterns in @WebFilter annotation", className);
						break;
					}
					urlPatterns = extractStrings(className, annValue);
					break;
				case "initParams":
					extractInitParams(className, annValue, initParams);
					break;
				case "asyncSupported":
					asyncSupported = annValue.stringifyValue();
					break;
				case "description":
					description = annValue.stringifyValue();
					break;
				case "displayName":
					displayName = annValue.stringifyValue();
					break;
				case "servletNames":
					if (!(annValue instanceof ArrayElementValue || annValue instanceof SimpleElementValue)) {
						LOG.warn("{} has wrong servletNames annotation. Not an array/string.", className);
					} else {
						servletNames = extractStrings(className, annValue);
					}
					break;
				case "dispatcherTypes":
					if (!(annValue instanceof ArrayElementValue || annValue instanceof EnumElementValue)) {
						LOG.warn("{} has wrong dispatcherTypes annotation. Not an array/enum.", className);
					} else {
						dispatcherTypes = extractStrings(className, annValue);
					}
					break;
				default:
					// skip small/large icons
					break;
			}
		}

		boolean override = false;
		FilterDef filter = fragment.getFilters().get(filterName);
		if (filter != null) {
			// annotated fitler overrides a filter from web(-fragment).xml
			override = true;
		} else {
			filter = new FilterDef();
			filter.setFilterName(filterName);
			// yes - we can't override a class in existing filter
			filter.setFilterClass(className);
		}

		if (filter.getAsyncSupported() == null) {
			filter.setAsyncSupported(asyncSupported);
		}
		if (filter.getDescription() == null) {
			filter.setDescription(description);
		}
		if (filter.getDisplayName() == null) {
			filter.setDisplayName(displayName);
		}

		if (override) {
			Map<String, String> existingInitParams = filter.getParameterMap();
			for (Map.Entry<String, String> e : initParams.entrySet()) {
				String key = e.getKey();
				if (!existingInitParams.containsKey(key)) {
					filter.addInitParameter(e.getKey(), e.getValue());
				}
			}
		} else {
			for (Map.Entry<String, String> e : initParams.entrySet()) {
				filter.addInitParameter(e.getKey(), e.getValue());
			}
		}

		if (!override) {
			FilterMap filterMap = new FilterMap();
			fragment.addFilter(filter);
			filterMap.setFilterName(filterName);
			if (urlPatterns != null) {
				for (String up : urlPatterns) {
					filterMap.addURLPattern(up);
				}
			}
			if (servletNames != null) {
				for (String sn : servletNames) {
					filterMap.addServletName(sn);
				}
			}
			if (dispatcherTypes != null) {
				for (String dt : dispatcherTypes) {
					filterMap.setDispatcher(dt);
				}
			}
			fragment.addFilterMapping(filterMap);
		} else {
			// override only when not present on existing mapping
			FilterMap existingMapping = null;
			for (FilterMap fm : fragment.getFilterMappings()) {
				if (filterName.equals(fm.getFilterName())) {
					existingMapping = fm;
					break;
				}
			}
			if (existingMapping != null) {
				String[] existingServletNames = existingMapping.getServletNames();
				String[] existingURLPatterns = existingMapping.getURLPatterns();
				String[] existingDispatcherNames = existingMapping.getDispatcherNames();
				if ((existingServletNames == null || existingServletNames.length == 0) && servletNames != null) {
					for (String sn : servletNames) {
						existingMapping.addServletName(sn);
					}
				}
				if ((existingURLPatterns == null || existingURLPatterns.length == 0) && urlPatterns != null) {
					for (String up : urlPatterns) {
						existingMapping.addURLPattern(up);
					}
				}
				if ((existingDispatcherNames == null || existingDispatcherNames.length == 0) && dispatcherTypes != null) {
					for (String dt : dispatcherTypes) {
						existingMapping.setDispatcher(dt);
					}
				}
			}
		}
	}

	private String[] extractStrings(String className, ElementValue annValue) {
		String[] result = null;
		if (!(annValue instanceof ArrayElementValue || annValue instanceof SimpleElementValue)) {
			LOG.warn("{} has wrong urlPatterns annotation. Not an array/string.", className);
		} else {
			if (annValue instanceof ArrayElementValue) {
				ArrayElementValue aev = (ArrayElementValue) annValue;
				ElementValue[] arr = aev.getElementValuesArray();
				result = new String[arr.length];
				for (int i = 0; i < arr.length; i++) {
					ElementValue v = arr[i];
					result[i] = v.stringifyValue();
				}
			} else {
				result = new String[] { annValue.stringifyValue() };
			}
		}

		return result;
	}

	private void extractInitParams(String className, ElementValue annValue, Map<String, String> initParams) {
		if (!(annValue instanceof ArrayElementValue || annValue instanceof AnnotationElementValue)) {
			LOG.warn("{} has wrong initParams annotation. Not an array or @WebInitParam.", className);
		} else {
			if (annValue instanceof ArrayElementValue) {
				// array of @WebInitParams
				ArrayElementValue aev = (ArrayElementValue) annValue;
				for (ElementValue ev : aev.getElementValuesArray()) {
					if (ev instanceof AnnotationElementValue) {
						extractInitParam((AnnotationElementValue) ev, initParams);
					}
				}
			} else {
				// single @WebInitParam
				extractInitParam((AnnotationElementValue) annValue, initParams);
			}
		}
	}

	private void extractInitParam(AnnotationElementValue aev, Map<String, String> initParams) {
		if (aev.getAnnotationEntry() != null) {
			String name = null;
			String value = null;
			for (ElementValuePair evp : aev.getAnnotationEntry().getElementValuePairs()) {
				String ak = evp.getNameString();
				ElementValue av = evp.getValue();
				switch (ak) {
					case "name":
						name = av.stringifyValue();
						break;
					case "value":
						value = av.stringifyValue();
						break;
					default:
						break;
				}
			}
			if (name != null && value != null) {
				initParams.put(name, value);
			}
		}
	}

	private ClassCacheEntry addSuperClassesAndInterfacesToTheCache(JavaClass clazz, String className,
			Bundle bundle, Map<Class<?>, Set<ServletContainerInitializer>> htToSci,
			Map<String, ClassCacheEntry> javaClassCache) {

		ClassCacheEntry cce = javaClassCache.get(className);
		if (cce != null) {
			return cce;
		}
		cce = new ClassCacheEntry();
		cce.superClassName = clazz.getSuperclassName();
		cce.interfaceNames = clazz.getInterfaceNames();
		javaClassCache.put(className, cce);

		addToCache(cce.superClassName, bundle, htToSci, javaClassCache);
		for (String name : cce.interfaceNames) {
			addToCache(name, bundle, htToSci, javaClassCache);
		}

		configureRelevantSCIs(cce, htToSci, javaClassCache);

		return cce;
	}

	private void addToCache(String className, Bundle bundle,
			Map<Class<?>, Set<ServletContainerInitializer>> htToSci, Map<String, ClassCacheEntry> javaClassCache) {

		if (javaClassCache.containsKey(className)) {
			return;
		}
		String resName = className.replace('.', '/') + ".class";
		// this is where we can't just single bundle associated with web fragment, because for example (samples-war),
		// the bundle for the-wab-itself can't load
		// resource org/ops4j/pax/web/samples/war/cb3/utils/IFace3.class, though it can load
		// resource org/ops4j/pax/web/samples/war/cb1/utils/Cb1IFace3.class (bundle://50.0:1/org/ops4j/pax/web/samples/war/cb1/utils/Cb1IFace3.class)
		URL url = wabClassLoader.getResource(resName);
		if (url == null) {
			return;
		}
		try (InputStream is = url.openStream()) {
			ClassParser parser = new ClassParser(is);
			JavaClass clazz = parser.parse();
			addSuperClassesAndInterfacesToTheCache(clazz, className, bundle, htToSci, javaClassCache);
		} catch (Exception e) {
			LOG.warn("Can't get class resource {}: {}", url, e.getMessage(), e);
		}
	}

	private void configureRelevantSCIs(ClassCacheEntry cce, Map<Class<?>, Set<ServletContainerInitializer>> htToSci,
			Map<String, ClassCacheEntry> javaClassCache) {

		Set<ServletContainerInitializer> result = new HashSet<>();

		// super class
		ClassCacheEntry sce = javaClassCache.get(cce.superClassName);
		if (sce != null) {
			if (sce.scis == null) {
				configureRelevantSCIs(sce, htToSci, javaClassCache);
			}
			result.addAll(sce.scis);
			// Tomcat does it outside of the if branch, but I don't think we have to add SCIs with @HT pointing
			// to a super class that couldn't be loaded
			result.addAll(findRelevantSCIs(cce.superClassName, htToSci));
		}

		// interfaces
		for (String name : cce.interfaceNames) {
			ClassCacheEntry ie = javaClassCache.get(name);
			if (ie != null) {
				if (ie.scis == null) {
					configureRelevantSCIs(ie, htToSci, javaClassCache);
				}
				result.addAll(ie.scis);
				// Tomcat does it outside of the if branch, but I don't think we have to add SCIs with @HT pointing
				// to a interfaces that couldn't be loaded
				result.addAll(findRelevantSCIs(name, htToSci));
			}
		}

		cce.scis = result;
	}

	private Collection<? extends ServletContainerInitializer> findRelevantSCIs(String name, Map<Class<?>,
			Set<ServletContainerInitializer>> htToSci) {
		for (Class<?> clazz : htToSci.keySet()) {
			if (name.equals(clazz.getName())) {
				return htToSci.get(clazz);
			}
		}
		return NO_SCIS;
	}

	private String className(String annotationBcelType) {
		if (annotationBcelType != null && annotationBcelType.startsWith("L") && annotationBcelType.endsWith(";")) {
			return annotationBcelType.substring(1, annotationBcelType.length() - 1).replace('/', '.');
		}
		return null;
	}

	/**
	 * See {@code org.apache.catalina.startup.ContextConfig.JavaClassCacheEntry}
	 */
	private static final class ClassCacheEntry {
		String superClassName;
		String[] interfaceNames;
		Set<ServletContainerInitializer> scis;
	}

}
