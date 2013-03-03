/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
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

package org.apache.jasper;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagInfo;

import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.JspUtil;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.ServletWriter;
import org.apache.jasper.compiler.TagLibraryInfoImpl;
import org.apache.jasper.servlet.JasperLoader;
import org.apache.jasper.servlet.JspServletWrapper;

/**
 * A place holder for various things that are used through out the JSP engine.
 * This is a per-request/per-context data structure. Some of the instance
 * variables are set at different points.
 * 
 * Most of the path-related stuff is here - mangling names, versions, dirs,
 * loading resources and dealing with uris.
 * 
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Pierre Delisle
 * @author Costin Manolache
 * @author Kin-man Chung
 */
public class JspCompilationContext {

	private boolean isPackagedTagFile;

	private String className;
	private String jspUri;
	private boolean isErrPage;
	private String basePackageName;
	private String derivedPackageName;
	private String servletJavaFileName;
	private String javaPath;
	private String classFileName;
	private String contentType;
	private ServletWriter writer;
	private Options options;
	private JspServletWrapper jsw;
	private Compiler jspCompiler;
	private String classPath;

	private String baseURI;
	private String outputDir;
	private ServletContext context;
	private URLClassLoader loader;

	private JspRuntimeContext rctxt;

	private int removed = 0;

	private URL baseUrl;
	private Class<?> servletClass;

	private boolean isTagFile;
	private boolean protoTypeMode;
	private TagInfo tagInfo;
	private URL tagFileJarUrl;
	private ConcurrentHashMap<String, TagLibraryInfoImpl> taglibs;
	private ConcurrentHashMap<String, URL> tagFileJarUrls;

	// jspURI _must_ be relative to the context
	@SuppressWarnings("unchecked")
	public JspCompilationContext(String jspUri, boolean isErrPage,
			Options options, ServletContext context, JspServletWrapper jsw,
			JspRuntimeContext rctxt) throws JasperException {

		this.jspUri = canonicalURI(jspUri);
		this.isErrPage = isErrPage;
		this.options = options;
		this.jsw = jsw;
		this.context = context;

		this.baseURI = jspUri.substring(0, jspUri.lastIndexOf('/') + 1);
		// hack fix for resolveRelativeURI
		if (baseURI == null) {
			baseURI = "/";
		} else if (baseURI.charAt(0) != '/') {
			// strip the basde slash since it will be combined with the
			// uriBase to generate a file
			baseURI = "/" + baseURI;
		}
		if (baseURI.charAt(baseURI.length() - 1) != '/') {
			baseURI += '/';
		}

		this.rctxt = rctxt;
		this.basePackageName = Constants.JSP_PACKAGE_NAME;

		taglibs = (ConcurrentHashMap<String, TagLibraryInfoImpl>) context
				.getAttribute(Constants.JSP_TAGLIBRARY_CACHE);
		tagFileJarUrls = (ConcurrentHashMap<String, URL>) context
				.getAttribute(Constants.JSP_TAGFILE_JAR_URLS_CACHE);
	}

	public JspCompilationContext(String tagfile, TagInfo tagInfo,
			Options options, ServletContext context, JspServletWrapper jsw,
			JspRuntimeContext rctxt, URL tagFileJarUrl) throws JasperException {
		this(tagfile, false, options, context, jsw, rctxt);
		this.isTagFile = true;
		this.tagInfo = tagInfo;
		this.tagFileJarUrl = tagFileJarUrl;
		if (tagFileJarUrl != null) {
			isPackagedTagFile = true;
		}
	}

	/* ==================== Methods to override ==================== */

	/**
	 * Adds the given tag library with the given URI to the context-wide tag
	 * library cache.
	 * 
	 * @param uri
	 *            The tag library URI
	 * @param taglib
	 *            The tag library to add
	 */
	public void addTaglib(String uri, TagLibraryInfoImpl taglib) {
		taglibs.put(uri, taglib);
	}

	/**
	 * Gets the context-wide tag library cache.
	 * 
	 * @return The context-wide tag library cache
	 */
	public ConcurrentHashMap<String, TagLibraryInfoImpl> getTaglibs() {
		return taglibs;
	}

	/**
	 * Clears the context-wide tag library cache.
	 */
	public void clearTaglibs() {
		taglibs.clear();
	}

	/**
	 * Clears the context-wide mappings from JAR packaged tag file paths to
	 * their corresponding URLs.
	 */
	public void clearTagFileJarUrls() {
		tagFileJarUrls.clear();
	}

	/** ---------- Class path and loader ---------- */

	/**
	 * The classpath that is passed off to the Java compiler.
	 */
	public String getClassPath() {
		if (classPath != null)
			return classPath;
		return rctxt.getClassPath();
	}

	/**
	 * The classpath that is passed off to the Java compiler.
	 */
	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

	/**
	 * What class loader to use for loading classes while compiling this JSP?
	 */
	public ClassLoader getClassLoader() {
		if (loader != null)
			return loader;
		return rctxt.getParentClassLoader();
	}

	public void setClassLoader(URLClassLoader loader) {
		this.loader = loader;
	}

	/** ---------- Input/Output ---------- */

	/**
	 * The output directory to generate code into. The output directory is make
	 * up of the scratch directory, which is provide in Options, plus the
	 * directory derived from the package name.
	 */
	public String getOutputDir() {
		if (outputDir == null) {
			createOutputDir();
		}

		return outputDir;
	}

	/**
	 * Create a compiler object for parsing only.
	 */
	public Compiler createParser() throws JasperException {
		jspCompiler = new Compiler(this, jsw);
		return jspCompiler;
	}

	/**
	 * Create a "Compiler" object.
	 */
	public Compiler createCompiler(boolean jspcMode) throws JasperException {
		if (jspCompiler != null) {
			return jspCompiler;
		}

		jspCompiler = new Compiler(this, jsw, jspcMode);
		return jspCompiler;
	}

	public Compiler getCompiler() {
		return jspCompiler;
	}

	/** ---------- Access resources in the webapp ---------- */

	/**
	 * Get the full value of a URI relative to this compilations context uses
	 * current file as the base.
	 */
	public String resolveRelativeUri(String uri) {
		// sometimes we get uri's massaged from File(String), so check for
		// a root directory deperator char
		if (uri.startsWith("/") || uri.startsWith(File.separator)) {
			return uri;
		} else {
			return baseURI + uri;
		}
	}

	/**
	 * Gets a resource as a stream, relative to the meanings of this context's
	 * implementation.
	 * 
	 * @return a null if the resource cannot be found or represented as an
	 *         InputStream.
	 */
	public java.io.InputStream getResourceAsStream(String res)
			throws JasperException {
		return context.getResourceAsStream(canonicalURI(res));
	}

	public URL getResource(String res) throws MalformedURLException {
		try {
			return context.getResource(canonicalURI(res));
		} catch (JasperException ex) {
			throw new MalformedURLException(ex.getMessage());
		}
	}

	public Set getResourcePaths(String path) throws JasperException {
		return context.getResourcePaths(canonicalURI(path));
	}

	/**
	 * Gets the actual path of a URI relative to the context of the compilation.
	 */
	public String getRealPath(String path) {
		if (context != null) {
			return context.getRealPath(path);
		}
		return path;
	}

	/**
	 * Gets the context-wide mappings from JAR packaged tag file paths to their
	 * corresponfing URLs.
	 */
	public ConcurrentHashMap<String, URL> getTagFileJarUrls() {
		return this.tagFileJarUrls;
	}

	/**
	 * Returns the JAR file in which the tag file for which this
	 * JspCompilationContext was created is packaged, or null if this
	 * JspCompilationContext does not correspond to a tag file, or if the
	 * corresponding tag file is not packaged in a JAR.
	 */
	public URL getTagFileJarUrl() {
		return this.tagFileJarUrl;
	}

	/* ==================== Common implementation ==================== */

	/**
	 * Just the class name (does not include package name) of the generated
	 * class.
	 */
	public String getServletClassName() {

		if (className != null) {
			return className;
		}

		if (isTagFile) {
			className = tagInfo.getTagClassName();
			int lastIndex = className.lastIndexOf('.');
			if (lastIndex != -1) {
				className = className.substring(lastIndex + 1);
			}
		} else {
			int iSep = jspUri.lastIndexOf('/') + 1;
			className = JspUtil.makeJavaIdentifier(jspUri.substring(iSep));
		}
		return className;
	}

	public void setServletClassName(String className) {
		this.className = className;
	}

	/**
	 * Path of the JSP URI. Note that this is not a file name. This is the
	 * context rooted URI of the JSP file.
	 */
	public String getJspFile() {
		return jspUri;
	}

	/**
	 * Are we processing something that has been declared as an errorpage?
	 */
	public boolean isErrorPage() {
		return isErrPage;
	}

	public void setErrorPage(boolean isErrPage) {
		this.isErrPage = isErrPage;
	}

	public boolean isTagFile() {
		return isTagFile;
	}

	public TagInfo getTagInfo() {
		return tagInfo;
	}

	public void setTagInfo(TagInfo tagi) {
		tagInfo = tagi;
	}

	/**
	 * True if we are compiling a tag file in prototype mode. ie we only
	 * generate codes with class for the tag handler with empty method bodies.
	 */
	public boolean isPrototypeMode() {
		return protoTypeMode;
	}

	public void setPrototypeMode(boolean pm) {
		protoTypeMode = pm;
	}

	/**
	 * Package name for the generated class is make up of the base package name,
	 * which is user settable, and the derived package name. The derived package
	 * name directly mirrors the file heirachy of the JSP page.
	 */
	public String getServletPackageName() {
		if (isTagFile()) {
			String className = tagInfo.getTagClassName();
			int lastIndex = className.lastIndexOf('.');
			String pkgName = "";
			if (lastIndex != -1) {
				pkgName = className.substring(0, lastIndex);
			}
			return pkgName;
		} else {
			String dPackageName = getDerivedPackageName();
			if (dPackageName.length() == 0) {
				return basePackageName;
			}
			return basePackageName + '.' + getDerivedPackageName();
		}
	}

	private String getDerivedPackageName() {
		if (derivedPackageName == null) {
			String jspUri;
			try {
				jspUri = new URI(this.jspUri).getPath();
				if (jspUri == null) {
					jspUri = "";
				}
			} catch (URISyntaxException e) {
				jspUri = this.jspUri;
			}
			int iSep = jspUri.lastIndexOf('/');
			derivedPackageName = (iSep > 0) ? JspUtil.makeJavaPackage(jspUri
					.substring(1, iSep)) : "";
		}
		return derivedPackageName;
	}

	/**
	 * The package name into which the servlet class is generated.
	 */
	public void setServletPackageName(String servletPackageName) {
		this.basePackageName = servletPackageName;
	}

	/**
	 * Full path name of the Java file into which the servlet is being
	 * generated.
	 */
	public String getServletJavaFileName() {

		if (servletJavaFileName == null) {
			servletJavaFileName = getOutputDir() + getServletClassName()
					+ ".java";
		}
		return servletJavaFileName;
	}

	/**
	 * Get hold of the Options object for this context.
	 */
	public Options getOptions() {
		return options;
	}

	public ServletContext getServletContext() {
		return context;
	}

	public JspRuntimeContext getRuntimeContext() {
		return rctxt;
	}

	/**
	 * Full class name
	 */
	public String getFullClassName() {
		if (isTagFile()) {
			return tagInfo.getTagClassName();
		}
		return getServletPackageName() + '.' + getServletClassName();
	}

	/**
	 * Path of the Java file relative to the work directory.
	 */
	public String getJavaPath() {

		if (javaPath != null) {
			return javaPath;
		}

		javaPath = getFullClassName().replace('.', '/') + ".java";
		return javaPath;
	}

	public String getClassFileName() {

		if (classFileName == null) {
			classFileName = getOutputDir() + getServletClassName() + ".class";
		}
		return classFileName;
	}

	/**
	 * Get the content type of this JSP.
	 * 
	 * Content type includes content type and encoding.
	 */
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Where is the servlet being generated?
	 */
	public ServletWriter getWriter() {
		return writer;
	}

	public void setWriter(ServletWriter writer) {
		this.writer = writer;
	}

	/**
	 * Gets the 'location' of the TLD associated with the given taglib 'uri'.
	 * 
	 * @return An array of two Strings: The first element denotes the real path
	 *         to the TLD. If the path to the TLD points to a jar file, then the
	 *         second element denotes the name of the TLD entry in the jar file.
	 *         Returns null if the given uri is not associated with any tag
	 *         library 'exposed' in the web application.
	 */
	public String[] getTldLocation(String uri) throws JasperException {
		String[] location = getOptions().getTldLocationsCache()
				.getLocation(uri);
		return location;
	}

	/**
	 * Are we keeping generated code around?
	 */
	public boolean keepGenerated() {
		return getOptions().getKeepGenerated();
	}

	// ==================== Removal ====================

	public void incrementRemoved() {
		if (removed > 1) {
			jspCompiler.removeGeneratedFiles();
			if (rctxt != null)
				rctxt.removeWrapper(jspUri);
		}
		removed++;
	}

	public boolean isRemoved() {
		if (removed > 1) {
			return true;
		}
		return false;
	}

	// ==================== Compile and reload ====================

	public void compile() throws JasperException, FileNotFoundException {
		createCompiler(false);
		if (isPackagedTagFile || jspCompiler.isOutDated()) {
			try {
				jspCompiler.compile(true);
				jsw.setReload(true);
				jsw.setCompilationException(null);
			} catch (JasperException ex) {
				// Cache compilation exception
				jsw.setCompilationException(ex);
				throw ex;
			} catch (Exception ex) {
				ex.printStackTrace();
				JasperException je = new JasperException(
						Localizer.getMessage("jsp.error.unable.compile"), ex);
				// Cache compilation exception
				jsw.setCompilationException(je);
				throw je;
			}
		}
	}

	// ==================== Manipulating the class ====================

	public Class<?> load() throws JasperException {
		try {

			String name = getFullClassName();

			if (options.getUsePrecompiled()) {
				servletClass = getClassLoader().loadClass(name);
			} else {
				servletClass = getJspLoader().loadClass(name);
			}
		} catch (ClassNotFoundException cex) {
			throw new JasperException(
					Localizer.getMessage("jsp.error.unable.load"), cex);
		} catch (Exception ex) {
			throw new JasperException(
					Localizer.getMessage("jsp.error.unable.compile"), ex);
		}
		removed = 0;
		return servletClass;
	}

	public ClassLoader getJspLoader() {
		return new JasperLoader(new URL[] { baseUrl }, getClassLoader(),
				rctxt.getPermissionCollection(), rctxt.getCodeSource(),
				rctxt.getBytecodes());
	}

	public void makeOutputDir() {
		synchronized (outputDirLock) {
			File outDirFile = new File(outputDir);
			outDirFile.mkdirs();
		}
	}

	// ==================== Private methods ====================

	static Object outputDirLock = new Object();

	private void createOutputDir() {
		String path = null;
		if (isTagFile()) {
			String tagName = tagInfo.getTagClassName();
			path = tagName.replace('.', '/');
			path = path.substring(0, path.lastIndexOf('/'));
		} else {
			path = getServletPackageName().replace('.', '/');
		}

		try {
			// Append servlet or tag handler path to scratch dir
			baseUrl = options.getScratchDir().toURI().toURL();
			File f = new File(options.getScratchDir(), path);
			outputDir = f.getPath() + File.separator;
			makeOutputDir();
		} catch (Exception e) {
			throw new IllegalStateException("No output directory: "
					+ e.getMessage());
		}
	}

	private static final boolean isPathSeparator(char c) {
		return (c == '/' || c == '\\');
	}

	private static final String canonicalURI(String s) throws JasperException {

		if (s == null)
			return null;
		try { // PAXWEB-289
			return new URI(s).normalize().toString();
		} catch (URISyntaxException e) {
			StringBuffer result = new StringBuffer();
			final int len = s.length();
			int pos = 0;
			while (pos < len) {
				char c = s.charAt(pos);
				if (isPathSeparator(c)) {
					/*
					 * multiple path separators. 'foo///bar' -> 'foo/bar'
					 */
					while (pos + 1 < len && isPathSeparator(s.charAt(pos + 1))) {
						++pos;
					}
	
					if (pos + 1 < len && s.charAt(pos + 1) == '.') {
						/*
						 * a single dot at the end of the path - we are done.
						 */
						if (pos + 2 >= len)
							break;
	
						switch (s.charAt(pos + 2)) {
						/*
						 * self directory in path foo/./bar -> foo/bar
						 */
						case '/':
						case '\\':
							pos += 2;
							continue;
	
							/*
							 * two dots in a path: go back one hierarchy.
							 * foo/bar/../baz -> foo/baz
							 */
						case '.':
							// only if we have exactly _two_ dots.
							if (pos + 3 < len && isPathSeparator(s.charAt(pos + 3))) {
								pos += 3;
								int separatorPos = result.length() - 1;
								if (separatorPos < 0) {
									throw new JasperException(Localizer.getMessage(
											"jsp.error.badpath", s));
								}
								while (separatorPos >= 0
										&& !isPathSeparator(result
												.charAt(separatorPos))) {
									--separatorPos;
								}
								if (separatorPos >= 0)
									result.setLength(separatorPos);
								continue;
							}
						}
					}
				}
				result.append(c);
				++pos;
			}
			return result.toString();
		}
	}
}
