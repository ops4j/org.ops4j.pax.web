/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * Scans for and loads Tag Library Descriptors contained in a web application.
 */
public class TldScanner {

    private static final Logger LOG = LoggerFactory.getLogger(TldScanner.class);
    private static final String MSG = "org.apache.jasper.servlet.TldScanner";
    private static final String TLD_EXT = ".tld";
    private static final String WEB_INF = "/WEB-INF/";
    private final ServletContext context;
//    private final TldParser tldParser;
//    private final Map<String, TldResourcePath> uriTldResourcePathMap = new HashMap<>();
//    private final Map<TldResourcePath, TaglibXml> tldResourcePathTaglibXmlMap = new HashMap<>();
    private final List<String> listeners = new ArrayList<>();

    /**
     * Initialize with the application's ServletContext.
     *
     * @param context
     *            the application's servletContext
     */
    public TldScanner(ServletContext context, boolean namespaceAware, boolean validation, boolean blockExternal) {
        this.context = context;

//        this.tldParser = new TldParser(namespaceAware, validation, blockExternal);
    }

    /**
     * Scan for TLDs in all places defined by the specification:
     * <ol>
     * <li>Tag libraries defined by the platform</li>
     * <li>Entries from &lt;jsp-config&gt; in web.xml</li>
     * <li>A resources under /WEB-INF</li>
     * <li>In jar files from /WEB-INF/lib</li>
     * <li>Additional entries from the container</li>
     * </ol>
     *
     * @throws IOException
     *             if there was a problem scanning for or loading a TLD
     * @throws SAXException
     *             if there was a problem parsing a TLD
     */
    public void scan() throws IOException, SAXException {
//        scanPlatform();
//        scanJspConfig();
//        scanResourcePaths(WEB_INF);
//        scanJars();
    }
//
//    /**
//     * Returns the map of URI to TldResourcePath built by this scanner.
//     *
//     * @return the map of URI to TldResourcePath
//     */
//    public Map<String, TldResourcePath> getUriTldResourcePathMap() {
//        return uriTldResourcePathMap;
//    }
//
//    /**
//     * Returns the map of TldResourcePath to parsed XML files built by this
//     * scanner.
//     *
//     * @return the map of TldResourcePath to parsed XML files
//     */
//    public Map<TldResourcePath, TaglibXml> getTldResourcePathTaglibXmlMap() {
//        return tldResourcePathTaglibXmlMap;
//    }

    /**
     * Returns a list of all listeners declared by scanned TLDs.
     *
     * @return a list of listener class names
     */
    public List<String> getListeners() {
        return listeners;
    }
//
//    /**
//     * Set the class loader used by the digester to create objects as a result
//     * of this scan. Normally this only needs tobe set when using JspC.
//     */
//    public void setClassLoader(ClassLoader classLoader) {
//        tldParser.setClassLoader(classLoader);
//    }
//
//    /**
//     * Scan for TLDs required by the platform specification.
//     */
//    protected void scanPlatform() {
//    }
//
//    /**
//     * Scan for TLDs defined in &lt;jsp-config&gt;.
//     */
//    protected void scanJspConfig() throws IOException, SAXException {
//        JspConfigDescriptor jspConfigDescriptor = context.getJspConfigDescriptor();
//        if (jspConfigDescriptor == null) {
//            return;
//        }
//
//        Collection<TaglibDescriptor> descriptors = jspConfigDescriptor.getTaglibs();
//        for (TaglibDescriptor descriptor : descriptors) {
//            if (descriptor == null) {
//                continue;
//            }
//            String taglibURI = descriptor.getTaglibURI();
//            String resourcePath = descriptor.getTaglibLocation();
//            // Note: Whilst the Servlet 2.4 DTD implies that the location must
//            // be a context-relative path starting with '/', JSP.7.3.6.1 states
//            // explicitly how paths that do not start with '/' should be
//            // handled.
//            if (!resourcePath.startsWith("/")) {
//                resourcePath = WEB_INF + resourcePath;
//            }
//            if (uriTldResourcePathMap.containsKey(taglibURI)) {
//                LOG.warn(Localizer.getMessage(MSG + ".webxmlSkip", resourcePath, taglibURI));
//                continue;
//            }
//
//            if (LOG.isTraceEnabled()) {
//                LOG.trace(Localizer.getMessage(MSG + ".webxmlAdd", resourcePath, taglibURI));
//            }
//
//            URL url = context.getResource(resourcePath);
//            if (url != null) {
//                TldResourcePath tldResourcePath;
//                if (resourcePath.endsWith(".jar")) {
//                    // if the path points to a jar file, the TLD is presumed to
//                    // be
//                    // inside at META-INF/taglib.tld
//                    tldResourcePath = new TldResourcePath(url, resourcePath, "META-INF/taglib.tld");
//                } else {
//                    tldResourcePath = new TldResourcePath(url, resourcePath);
//                }
//                // parse TLD but store using the URI supplied in the descriptor
//                TaglibXml tld = tldParser.parse(tldResourcePath);
//                uriTldResourcePathMap.put(taglibURI, tldResourcePath);
//                tldResourcePathTaglibXmlMap.put(tldResourcePath, tld);
//                if (tld.getListeners() != null) {
//                    listeners.addAll(tld.getListeners());
//                }
//            } else {
//                LOG.warn(Localizer.getMessage(MSG + ".webxmlFailPathDoesNotExist", resourcePath, taglibURI));
//                continue;
//            }
//        }
//    }
//
//    /**
//     * Scan web application resources for TLDs, recursively.
//     *
//     * @param startPath
//     *            the directory resource to scan
//     * @throws IOException
//     *             if there was a problem scanning for or loading a TLD
//     * @throws SAXException
//     *             if there was a problem parsing a TLD
//     */
//    protected void scanResourcePaths(String startPath) throws IOException, SAXException {
//
//        Set<String> dirList = context.getResourcePaths(startPath);
//        if (dirList != null) {
//            for (String path : dirList) {
//                if (path.startsWith("/WEB-INF/classes/")) {
//                    // Skip: JSP.7.3.1
//                } else if (path.startsWith("/WEB-INF/lib/")) {
//                    // Skip: JSP.7.3.1
//                } else if (path.endsWith("/")) {
//                    scanResourcePaths(path);
//                } else if (path.startsWith("/WEB-INF/tags/")) {
//                    // JSP 7.3.1: in /WEB-INF/tags only consider implicit.tld
//                    if (path.endsWith("/implicit.tld")) {
//                        parseTld(path);
//                    }
//                } else if (path.endsWith(TLD_EXT)) {
//                    parseTld(path);
//                }
//            }
//        }
//    }
//
//    /**
//     * Scan for TLDs in JARs in /WEB-INF/lib.
//     *
//     * @throws IOException
//     */
//    public void scanJars() throws IOException {
//
//        ClassLoader webappLoader = Thread.currentThread().getContextClassLoader();
//
//        ClassLoader parentLoader = webappLoader.getParent();
//
//        ResourceDelegatingBundleClassLoader classLoader = null;
//        if (webappLoader instanceof ResourceDelegatingBundleClassLoader) {
//            classLoader = (ResourceDelegatingBundleClassLoader) webappLoader;
//        } else if (parentLoader instanceof ResourceDelegatingBundleClassLoader) {
//            classLoader = (ResourceDelegatingBundleClassLoader) parentLoader;
//        } else {
//            if ("org.apache.catalina.loader".equals(webappLoader.getClass().getPackage().getName())) {
//                ClassLoader parent = webappLoader.getParent();
//                if (parent instanceof ResourceDelegatingBundleClassLoader) {
//                    classLoader = (ResourceDelegatingBundleClassLoader) parent;
//                }
//            }
//        }
//
//        List<Bundle> bundles = classLoader == null ? Collections.<Bundle>emptyList() : classLoader.getBundles();
//        for (Bundle bundle : bundles) {
//            Collection<Enumeration<URL>> enumerations;
//            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
//            if (bundleWiring == null) {
//                Enumeration<URL> urls = bundle.findEntries("META-INF", "*.tld", true);
//                enumerations = Collections.singleton(urls);
//            } else {
//                Collection<String> resources = bundleWiring.listResources("META-INF", "*.tld",
//                        BundleWiring.LISTRESOURCES_RECURSE);
//                enumerations = new ArrayList<>(resources.size());
//                for (String resource : resources) {
//                    Enumeration<URL> urls = bundle.getResources(resource);
//                    enumerations.add(urls);
//                }
//            }
//            for (Enumeration<URL> urls : enumerations) {
//                if (urls != null) {
//                    while (urls.hasMoreElements()) {
//                        URL url = urls.nextElement();
//                        LOG.info("found TLD {}", url);
//                        TldResourcePath tldResourcePath = new TldResourcePath(url, null, null);
//                        try {
//                            parseTld(tldResourcePath);
//                        } catch (SAXException e) {
//                            throw new IOException(e);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    protected void parseTld(String resourcePath) throws IOException, SAXException {
//        TldResourcePath tldResourcePath = new TldResourcePath(context.getResource(resourcePath), resourcePath);
//        parseTld(tldResourcePath);
//    }
//
//    protected void parseTld(TldResourcePath path) throws IOException, SAXException {
//        if (tldResourcePathTaglibXmlMap.containsKey(path)) {
//            // TLD has already been parsed as a result of processing web.xml
//            return;
//        }
//        TaglibXml tld = tldParser.parse(path);
//        String uri = tld.getUri();
//        if (uri != null) {
//            if (!uriTldResourcePathMap.containsKey(uri)) {
//                uriTldResourcePathMap.put(uri, path);
//            }
//        }
//        tldResourcePathTaglibXmlMap.put(path, tld);
//        if (tld.getListeners() != null) {
//            listeners.addAll(tld.getListeners());
//        }
//    }
}
