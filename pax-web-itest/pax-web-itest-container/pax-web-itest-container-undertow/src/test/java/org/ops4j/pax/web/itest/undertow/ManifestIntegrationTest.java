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
 /**
 * 
 */
package org.ops4j.pax.web.itest.undertow;

import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.web.itest.base.VersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author anierbeck
 * 
 */
public class ManifestIntegrationTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(ManifestIntegrationTest.class);

	@Test
	public void testJettyBundleAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home")
				+ "/.m2/repository");
		Assert.assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jetty-bundle/"
				+ VersionUtil.getProjectVersion() + "/pax-web-jetty-bundle-"
				+ VersionUtil.getProjectVersion() + ".jar");
		Assert.assertTrue("File exists: " + file, file.exists());

		ClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI()
				.toURL() });
		Enumeration<URL> resources = classLoader
				.getResources(JarFile.MANIFEST_NAME);
		Assert.assertTrue("Manifest entry found", resources.hasMoreElements());
		while (resources.hasMoreElements()) {
			Manifest manifest = null;
			URL url = null;
			//CHECKSTYLE:OFF
			try {
				url = resources.nextElement();
				LOG.debug("Found URL: {}", url);
				manifest = new Manifest(url.openStream());
				Assert.assertNotNull("Manifest not null", manifest);
			} catch (Throwable t) {
				Assert.fail("Caught Exception " + t.getMessage()
						+ " on manifest: " + manifest + " and URL: " + url);
			}
			//CHECKSTYLE:ON
		}
	}

	@Test
	public void testJettAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home")
				+ "/.m2/repository");
		Assert.assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jetty/" + VersionUtil.getProjectVersion()
				+ "/pax-web-jetty-" + VersionUtil.getProjectVersion() + ".jar");
		Assert.assertTrue("File exists: " + file, file.exists());

		ClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI()
				.toURL() });
		Enumeration<URL> resources = classLoader
				.getResources(JarFile.MANIFEST_NAME);
		Assert.assertTrue("Manifest entry found", resources.hasMoreElements());
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			Manifest manifest = new Manifest(url.openStream());
			Assert.assertNotNull("Manifest not null", manifest);
		}
	}

	@Test
	public void testJspAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home")
				+ "/.m2/repository");
		Assert.assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jsp/" + VersionUtil.getProjectVersion()
				+ "/pax-web-jsp-" + VersionUtil.getProjectVersion() + ".jar");
		Assert.assertTrue("File exists: " + file, file.exists());

		ClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI()
				.toURL() });
		Enumeration<URL> resources = classLoader
				.getResources(JarFile.MANIFEST_NAME);
		Assert.assertTrue("Manifest entry found", resources.hasMoreElements());
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			Manifest manifest = new Manifest(url.openStream());
			Assert.assertNotNull("Manifest not null", manifest);
		}
	}
}
