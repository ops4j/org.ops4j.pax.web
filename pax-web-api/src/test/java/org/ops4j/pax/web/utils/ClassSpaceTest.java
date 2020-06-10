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
package org.ops4j.pax.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassSpaceTest {

	public static final Logger LOG = LoggerFactory.getLogger(ClassSpaceTest.class);

	@Test
	public void classpath() {
	}

	@Test
	public void classLoaderGetResources() throws Exception {
		// most fundamental method: java.lang.ClassLoader.getResources()
		// it always checks the hierarchy of classloaders, when parent's getResources() is called first
		// in this case, test's classloader is sun.misc.Launcher.AppClassLoader, which has
		// parent sun.misc.Launcher.ExtClassLoader
		// ExtClassLoader has no parent and java.lang.ClassLoader.getBootstrapResources(name) is called
		// sun.misc.Launcher.AppClassLoader is URLClassLoader with urls taken from java.class.path
		// sun.misc.Launcher.ExtClassLoader is URLClassLoader with urls taken from java.ext.dirs system property
		// sun.misc.Launcher.BootClassPathHolder is just holder for urls taken from sun.boot.class.path system property
		//
		// sun.boot.class.path is (after conversion to URL[]):
		// - "file:$JAVA_HOME/jre/lib/resources.jar"
		// - "file:$JAVA_HOME/jre/lib/rt.jar"
		// - "file:$JAVA_HOME/jre/lib/sunrsasign.jar"
		// - "file:$JAVA_HOME/jre/lib/jsse.jar"
		// - "file:$JAVA_HOME/jre/lib/jce.jar"
		// - "file:$JAVA_HOME/jre/lib/charsets.jar"
		// - "file:$JAVA_HOME/jre/lib/jfr.jar"
		// - "file:$JAVA_HOME/jre/classes"
		//
		// java.ext.dirs is split by pathSeparator and jars and subdirectories are listed, it is
		// (after conversion to URL[]):
		// - "file:$JAVA_HOME/jre/lib/ext/sunjce_provider.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/dnsns.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/jaccess.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/nashorn.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/jfxrt.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/cldrdata.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/sunpkcs11.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/localedata.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/sunec.jar"
		// - "file:$JAVA_HOME/jre/lib/ext/zipfs.jar"
		//
		// java.class.path is split by pathSeparator and jars and directories are listed, it is
		// (after conversion to URL[]):
		// - "file:${user.dir}/pax-web-api/target/surefire/surefirebooter8138695327232269372.jar"
		// (this booter is single jar having META-INF/MANIFEST.MF with Class-Path entry pointing to maven classpath
		// in IDEA, java.class.path is explicit)

		// Different classloaders should use the same getResources() method, but may implement findResources() invoked
		// in getResources() differently.
		// URLClassLoader implements findResources() using sun.misc.URLClassPath.findResources(), which uses
		// list of sun.misc.URLClassPath.Loader instances and their findResource() (singular form now!). There are two
		// implementations:
		// - sun.misc.URLClassPath.FileLoader - returns java.io.FileInputStream instance
		// - sun.misc.URLClassPath.JarLoader - returns result of java.util.jar.JarFile.getInputStream(ZipEntry)
		//
		// When then resource name is "", only file-based classpath entries are checked, that's why e.g.,
		// org.springframework.core.io.support.PathMatchingResourcePatternResolver#doFindAllClassPathResources()
		// additionally checks JAR roots _explicitly_ for "" path
		// Generally it's related to which entries were put to jar/zip when it was created. For example, `rt.jar` has
		// _jar entry_ "java/lang/Object.class", but doesn't have "java/" or "java/lang/" entry. While
		// `osgi.core-7.0.0.jar` has "org/", "org/osgi/", "org/osgi/dto/" and "org/osgi/dto/DTO.class" _jar entries_.

//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("/META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources(""); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/"); e.hasMoreElements(); ) {
		for (Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("java"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("java/"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("java/lang/Object.class"); e.hasMoreElements(); ) {
//		for (Enumeration<URL> e = getClass().getClassLoader().getResources("java/"); e.hasMoreElements(); ) {
			LOG.info("URL: {}", e.nextElement());
		}
	}

	@Test
	public void classLoaderFindResources() throws Exception {
		// ext classloader. meta-index file prevents returning META-INF/MANIFEST.MF
		for (Enumeration<URL> e = ((URLClassLoader)getClass().getClassLoader().getParent()).findResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
			LOG.info("URL 1: {}", e.nextElement());
		}
		// but META-INF/services/java.nio.file.spi.FileSystemProvider is allowed
		for (Enumeration<URL> e = ((URLClassLoader)getClass().getClassLoader().getParent()).findResources("META-INF/services/java.nio.file.spi.FileSystemProvider"); e.hasMoreElements(); ) {
			LOG.info("URL 1: {}", e.nextElement());
		}
		// app classloader
		for (Enumeration<URL> e = ((URLClassLoader)getClass().getClassLoader()).findResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
			LOG.info("URL 2a: {}", e.nextElement());
		}
		// system classloader - usually same as app classloader. Taken from:
		// 1) sun.misc.Launcher.getClassLoader() == sun.misc.Launcher.AppClassLoader.getAppClassLoader()
		// 2) -Djava.system.class.loader
		for (Enumeration<URL> e = ((URLClassLoader) ClassLoader.getSystemClassLoader()).findResources("META-INF/MANIFEST.MF"); e.hasMoreElements(); ) {
			LOG.info("URL 2b: {}", e.nextElement());
		}

		LOG.info("System classloader: {}", System.getProperty("java.system.class.loader"));
	}

	@Test
	public void classLoaderGetResource() throws IOException {
		// will get the URL only from first JAR/FileLoader found in sun.misc.URLClassPath.loaders
		LOG.info("URL: {}", getClass().getClassLoader().getResource("org/ops4j"));

		// for directory, sun.net.www.protocol.file.FileURLConnection.getInputStream() will get ...
		// ... '\n'-separated list of directory entries returned from java.io.File.list()
		InputStream is = getClass().getClassLoader().getResourceAsStream("org/ops4j/pax/web/service");
		byte[] buf = new byte[1024];
		int read = -1;
		StringWriter sw = new StringWriter();
		while (is != null && (read = is.read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		LOG.info("Content of directory:\n{}", sw.toString());
	}

}
