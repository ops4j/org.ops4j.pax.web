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

import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.jasper.servlet.TldScanner;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.SimpleInstanceManager;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Pax Web extends original initializer, so it is possible to override the {@link TldScanner}.</p>
 *
 * <p>This initializer is declared in {@code /META-INF/services/javax.servlet.ServletContainerInitializer}
 * for pax-web-extender-war purpose. For Whiteboard and HttpService purposes, it is used directly to configure the
 * context(s) when JSP support is required.</p>
 *
 * <p>According to Servlet specification 8.3 "JSP container pluggability", JSP processing/parsing/setup is no longer
 * performed by "Servlet container" itself and instead can be delegated to "JSP container" using
 * {@link ServletContainerInitializer} mechanism.</p>
 *
 * <p>Neither Whiteboard (CMPN 140) nor HttpService (CMPN 102) specifications say anything about JSPs. The only
 * relevant specification is "128 Web Applications Specification" which defines WABs (Web Application Bundles), but
 * there is not many details about how exactly JSPs (and TLDs) should be supported.</p>
 *
 * <p>So we start with the asumption that CMPN 128 specification is about supporting WARs in OSGi runtime in very
 * similar way to how they work in JavaEE environments. The most important <em>common ground</em> is how to use
 * classloaders to find necessary resources. Details of how TLDs should be located are described inline below. Here's
 * the outline: in JavaEE, WAR is associated with single classloader, which includes:<ul>
 *     <li>{@code /WEB-INF/classes} directory</li>
 *     <li>each of the {@code /WEN-INF/lib/*.jar}</li>
 * </ul>
 * In Whiteboard (HttpService), there's no such thing as WAR, there's only
 * {@link org.osgi.service.http.context.ServletContextHelper} ({@link org.osgi.service.http.HttpContext}), which are
 * <em>customized</em> internally into {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}. These can
 * be mixed in many ways with dynamic servlets, filters and other web elements. However, when we finally get to
 * pax-web-extender-war, we have to marks some instance of {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}
 * as a <em>context</em> associated with WAB/WAR.</p>
 *
 * <p>Because there should be no such thing as <em>classpath</em> in OSGi application (only a mesh of bundles), an
 * <em>entry point</em> to searching for TLDs should be a bundle. When this initializer is being passed an
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}
 * (associated with single {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}) we'll get the <em>owner
 * bundle</em> of such context (model). If it's a different implementation of {@link ServletContext} we'll look for
 * {@code osgi-bundlecontext} context attribute according to "128.6.1 Bundle Context Access". When there's no such
 * attribute, we'll explicitly do nothing.</p>
 */
public class JasperInitializer extends org.apache.jasper.servlet.JasperInitializer {

	public static final Logger LOG = LoggerFactory.getLogger(JasperInitializer.class);

	// When Tomcat start with any web application installed (e.g., /manager), ServletContainerInitializers
	// are loaded in org.apache.catalina.startup.ContextConfig.processServletContainerInitializers()
	// the services found are:
	//  - org.apache.tomcat.websocket.server.WsSci
	//  - org.apache.jasper.servlet.JasperInitializer (using URLClassLoader for Tomcat's lib/ directory, while
	//    the loader used is ParallelWebappClassLoader for e.g., /manager webapp)

	@Override
	public void onStartup(Set<Class<?>> types, ServletContext context) throws ServletException {
		// we'll skip contexts which don't give us access to some Bundle/BundleContext being the "root" of
		// a "web application" (with all the Pax Web/OSGi quirks)
		Bundle ownerBundle = getOwnerBundle(context);
		if (ownerBundle == null) {
			String contextPath = context.getContextPath();
			if ("".equals(contextPath)) {
				contextPath = "/";
			}
			LOG.warn("Can't get a bundle associated with servlet context \"{}\". Is this a valid servlet context: {}?",
					contextPath, context);
			return;
		}

		// TOCHECK: override the instance manager required by org.apache.jasper.servlet.JspServletWrapper.getServlet()
		context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

		// set configured JarScanner into the context
		// To be honest, we're not calling org.apache.jasper.servlet.TldScanner.scanJars() at all, because
		// this method always searches for /WEB-INF/*.jar paths. And in OSGi we don't necessarily have them
		// on Bundle-ClassPath!
		// but I'm leaving this fragment to show how it works by default
		StandardJarScanner jarScanner = new StandardJarScanner();
		// disable searching for /WEB-INF/classes/META-INF/*.tld
		jarScanner.setScanAllDirectories(false);
		// disable scanning files which are not jar: URL or don't have .jar extension
		jarScanner.setScanAllFiles(false);
		// do not collect URLs from URLClassLoaders in the hierarchy
		// Tomcat for example scans these:
		// classPathUrlsToProcess = {java.util.LinkedList@3191}  size = 33
		//   0 = {java.net.URL@2817} "file:/data/servers/apache-tomcat-9.0.37/lib/"
		//   1 = {java.net.URL@2818} "file:/data/servers/apache-tomcat-9.0.37/lib/tomcat-coyote.jar"
		//   2 = {java.net.URL@2819} "file:/data/servers/apache-tomcat-9.0.37/lib/tomcat-websocket.jar"
		//   3 = {java.net.URL@2820} "file:/data/servers/apache-tomcat-9.0.37/lib/tomcat-i18n-ru.jar"
		//   4 = {java.net.URL@2821} "file:/data/servers/apache-tomcat-9.0.37/lib/catalina-tribes.jar"
		//   5 = {java.net.URL@2822} "file:/data/servers/apache-tomcat-9.0.37/lib/jasper.jar"
		// ...
		// then these:
		// result = {java.net.URL[2]@3605}
		//   0 = {java.net.URL@3606} "file:/data/servers/apache-tomcat-9.0.37/bin/bootstrap.jar"
		//   1 = {java.net.URL@3607} "file:/data/servers/apache-tomcat-9.0.37/bin/tomcat-juli.jar"
		jarScanner.setScanClassPath(false);
		// do not scan JARs declared in Class-Path MANIFEST.MF entry of other scanned JARs (doesn't affect jars
		// in WEB-INF/lib, which are never scanned for Class-Path header)
		jarScanner.setScanManifest(false);
		// let's keep it this way - so we can use Tomcat's tomcat.util.scan.StandardJarScanFilter.jarsToSkip and
		// tomcat.util.scan.StandardJarScanFilter.jarsToScan properties
		jarScanner.setJarScanFilter(new StandardJarScanFilter());
		context.setAttribute(JarScanner.class.getName(), jarScanner);

		// call to perform TLD scanning, which consists of 3 stages:
		// 1) create org.apache.jasper.servlet.TldScanner
		//    1a) create org.apache.tomcat.util.descriptor.tld.TldParser
		// 2) call org.apache.jasper.servlet.TldScanner.scan() to detect all *.tld files
		// 3) get all the listeners declared in *.tld files and pass them (as strings) to
		//    javax.servlet.ServletContext.addListener(java.lang.String)
		super.onStartup(types, context);
	}

	@Override
	protected TldScanner newTldScanner(ServletContext context, boolean namespaceAware, boolean validate, boolean blockExternal) {
		// Original org.apache.jasper.servlet.TldScanner.scan() looks for *.tld files:
		// - conforming to "http://java.sun.com/xml/ns/javaee" namespace XSD, but parsed in more flexible way using:
		//    - org.apache.tomcat.util.descriptor.tld.TldRuleSet digester rules
		//    - org.apache.tomcat.util.descriptor.tld.TaglibXml object pushed as top level bean
		//   (the top-level element should be "{http://java.sun.com/xml/ns/javaee}taglib")
		// - in places defined by "JSP.7.3.2 TLD resource path" of JSR 245 JSP Specification:
		//    1) JavaEE Platform entries: JSP standard tag library and JSF libraries
		//        - Tomcat does nothing here
		//    2) JSP 7.3.3 /web-app/jsp-config/taglib/taglib-location elements from web.xml
		//        - Tomcat calls javax.servlet.ServletContext.getJspConfigDescriptor() and then
		//          javax.servlet.descriptor.JspConfigDescriptor.getTaglibs() - we have relevant methods in
		//          WebContainer interface
		//        - for each location (with /WEB-INF/ prepended if it's relative),
		//          javax.servlet.ServletContext.getResource() is called
		//           - if it's *.jar, then META-INF/taglib.tld entry is being checked
		//           - it can't be inside /WEB-INF/classes
		//           - it can't be inside /WEB-INF/lib
		//           - it can't be inside /WEB-INF/tags, unless it's /WEB-INF/tags/implicit.tld
		//    3) JSP 7.3.4 Implicit TLDs from
		//        - WEB-INF/lib/**/*.jar files (only META-INF/**/*.tld entries)
		//           - uses org.apache.tomcat.util.scan.StandardJarScanner
		//              - calls javax.servlet.ServletContext.getResourcePaths("/WEB-INF/lib/")
		//              - for each JAR, tomcat.util.scan.StandardJarScanFilter.jarsToSkip and
		//                tomcat.util.scan.StandardJarScanFilter.jarsToScan system properties are checked, which
		//                are comma-separated simple names of jar files
		//              - javax.servlet.ServletContext.getResource() is called for non-skipped JAR
		//              - by default, Class-Path MANIFEST.MF entry is checked for JARs unless JARs from webapp are
		//                checked
		//              - for each JAR, only META-INF/**/*.tld entries are checked
		//        - WEB-INF/classes/META-INF/*.tld files are checked (but not subdirectories) - only if
		//          scanAllDirectories is enabled and Tomcat adds this support because of "one or more JARs have been
		//          unpacked into WEB-INF/classes as happens with some IDEs"
		//        - WEB-INF/**/*.tld files (but not in WEB-INF/classes or WEB-INF/lib or WEB-INF/tags (implicit.tld
		//          is allowed))
		//           - Tomcat calls javax.servlet.ServletContext.getResourcePaths("/WEB-INF/") and does a DFS for
		//             subpaths with the above reservations
		//        - Tomcat does "Scan the classpath" (org.apache.tomcat.util.scan.StandardJarScanner.scanClassPath)
		//          which navigates up the classloaders and for URLClassLoaders, all java.net.URLClassLoader.getURLs()
		//          are scanned (with skip/scan restrictions mentioned above)
		//    4) JSP 7.3.5 Implicit Map Entries from the Container
		//
		// All TLDs should be specified (in URI -> location mapping) as:
		//  - context relative path of *.tld itself
		//  - context relative path to a JAR file that has META-INF/taglib.tld entry
		//
		// it's worth noting that Tomcat doesn't provide support for standard tag libs unless user explicitly ships
		// stdtags in WEB-INF/lib, like in $TOMCAT_HOME/webapps/examples/WEB-INF/lib/taglibs-standard-impl-1.2.5.jar

		// In Pax Web TldScanner, we:
		//  - interpret "JavaEE Platform entries" as everything that's contained in pax-web-jsp bundle. This allows
		//    use to scan for standard tag library TLDs without user bundle's declaration
		//  - skip standard classloader scanning (traversing up the CL hierarchy) - instead we'll use BundleWiring
		//    API and ClassPathUtil to get a closure of bundles to be scanned (or in case of WAB, change the process
		//    accordingly to not go outside of the WAB maybe)
		//  - don't scan JARs available in /WEB-INF/lib/*.jar by default - ONLY if these JARs are available on
		//    Bundle-ClassPath according to "128.3.7 Content Serving Example":
		//        [...] the tag classes in foo.jar must not [be found] because foo.jar is not part of the bundle class
		//        path.

		return new PaxWebTldScanner(context, getOwnerBundle(context));
	}

	/**
	 * Returns a {@link Bundle} associated with given {@link ServletContext}.
	 * @param context
	 * @return
	 */
	private Bundle getOwnerBundle(ServletContext context) {
		if (context instanceof OsgiServletContext) {
			return ((OsgiServletContext) context).getOsgiContextModel().getOwnerBundle();
		}
		Object attribute = context.getAttribute(PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT);
		if (attribute instanceof BundleContext) {
			return ((BundleContext) attribute).getBundle();
		}
		if (attribute != null) {
			LOG.warn("Wrong type of {} attribute. Expected {}, got {}", PaxWebConstants.CONTEXT_PARAM_BUNDLE_CONTEXT,
					BundleContext.class.getName(), attribute.getClass().getName());
		}
		return null;
	}

}
