/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.itest.osgi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @author anierbeck
 */
public class ManifestIntegrationTest {

	private static final Logger LOG = LoggerFactory.getLogger(ManifestIntegrationTest.class);

	private static String projectVersion = null;

	@BeforeClass
	public static void loadVersion() throws IOException {
		Properties props = new Properties();
		props.load(ManifestIntegrationTest.class.getResourceAsStream("/version.properties"));
		projectVersion = props.getProperty("paxweb.version");
	}

	@Test
	public void testJettyBundleAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home") + "/.m2/repository");
		assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jetty-bundle/"
				+ projectVersion + "/pax-web-jetty-bundle-"
				+ projectVersion + ".jar");
		assertTrue("File exists: " + file, file.exists());

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() })) {
			Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
			assertTrue("Manifest entry found", resources.hasMoreElements());
			while (resources.hasMoreElements()) {
				Manifest manifest = null;
				URL url = null;
				try {
					url = resources.nextElement();
					LOG.debug("Found URL: {}", url);
					manifest = new Manifest(url.openStream());
					Assert.assertNotNull("Manifest not null", manifest);
				} catch (Throwable t) {
					Assert.fail("Caught Exception " + t.getMessage() + " on manifest: " + manifest + " and URL: " + url);
				}
			}
		}
	}

	@Test
	public void testJettyAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home") + "/.m2/repository");
		assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jetty/" + projectVersion
				+ "/pax-web-jetty-" + projectVersion + ".jar");
		assertTrue("File exists: " + file, file.exists());

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() })) {
			Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
			assertTrue("Manifest entry found", resources.hasMoreElements());
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Manifest manifest = new Manifest(url.openStream());
				Assert.assertNotNull("Manifest not null", manifest);
			}
		}
	}

	@Test
	public void testJspAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home") + "/.m2/repository");
		assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jsp/" + projectVersion
				+ "/pax-web-jsp-" + projectVersion + ".jar");
		assertTrue("File exists: " + file, file.exists());

		try (URLClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI().toURL() })) {
			Enumeration<URL> resources = classLoader.getResources(JarFile.MANIFEST_NAME);
			assertTrue("Manifest entry found", resources.hasMoreElements());
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				Manifest manifest = new Manifest(url.openStream());
				Assert.assertNotNull("Manifest not null", manifest);
			}
		}
	}

}
