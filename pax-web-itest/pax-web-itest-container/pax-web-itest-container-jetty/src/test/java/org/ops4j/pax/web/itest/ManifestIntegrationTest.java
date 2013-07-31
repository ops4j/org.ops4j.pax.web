/**
 * 
 */
package org.ops4j.pax.web.itest;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
				+ getProjectVersion() + "/pax-web-jetty-bundle-"
				+ getProjectVersion() + ".jar");
		Assert.assertTrue("File exists: " + file, file.exists());

		ClassLoader classLoader = new URLClassLoader(new URL[] { file.toURI()
				.toURL() });
		Enumeration<URL> resources = classLoader
				.getResources(JarFile.MANIFEST_NAME);
		Assert.assertTrue("Manifest entry found", resources.hasMoreElements());
		while (resources.hasMoreElements()) {
			Manifest manifest = null;
			URL url = null;
			try {
				url = resources.nextElement();
				LOG.debug("Found URL: {}", url);
				manifest = new Manifest(url.openStream());
				Assert.assertNotNull("Manifest not null", manifest);
			} catch (Throwable t) { // CHECKSTYLE:SKIP
				Assert.fail("Caught Exception " + t.getMessage()
						+ " on manifest: " + manifest + " and URL: " + url);
			}
		}
	}

	@Test
	public void testJettAccess() throws Exception {
		File repodir = new File(System.getProperty("user.home")
				+ "/.m2/repository");
		Assert.assertTrue("Repository dir exists: " + repodir, repodir.exists());
		File file = new File(repodir.getAbsolutePath()
				+ "/org/ops4j/pax/web/pax-web-jetty/" + getProjectVersion()
				+ "/pax-web-jetty-" + getProjectVersion() + ".jar");
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
				+ "/org/ops4j/pax/web/pax-web-jsp/" + getProjectVersion()
				+ "/pax-web-jsp-" + getProjectVersion() + ".jar");
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

	protected static String getProjectVersion() {
		String projectVersion = System.getProperty("ProjectVersion");
		System.out.println("*** The ProjectVersion is " + projectVersion
				+ " ***");
		return projectVersion;
	}
}
