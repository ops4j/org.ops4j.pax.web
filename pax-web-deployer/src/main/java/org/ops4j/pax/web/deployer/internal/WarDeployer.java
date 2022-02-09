/* Copyright 2007 Alin Dreghiciu.
 * Copyright 2010 Achim Nierbeck.
 *
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
package org.ops4j.pax.web.deployer.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An Apache Felix FileInstall transform for WAR files.
 *
 * @author Alin Dreghiciu, Achim Nierbeck
 */
public class WarDeployer implements ArtifactUrlTransformer {

	private static final Logger LOG = LoggerFactory.getLogger(WarDeployer.class);

	/** Standard PATH separator */
	private static final String PATH_SEPERATOR = "/";

	public boolean canHandle(final File artifact) {
		JarFile jar = null;
		try {
			// the file needs to either end with .war
			// or with .war.jar (_jar upped_ by DirectoryWatcher)
			if (!artifact.getName().endsWith(".war") && !artifact.getName().endsWith(".war.jar")) {
				return false;
			}

			jar = new JarFile(artifact);
			JarEntry entry = jar.getJarEntry("WEB-INF/web.xml");
			// Only handle WAR artifacts
			if (entry == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("No WAR archive, will not handle artifact: " + artifact.getName());
				}
				return false;
			}
			// Only handle non OSGi bundles
			Manifest m = jar.getManifest();
			if (m != null
					&& m.getMainAttributes().getValue("Bundle-SymbolicName") != null
					&& m.getMainAttributes().getValue("Bundle-Version") != null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("This artifact has OSGi Manifest Header skipping: " + artifact.getName());
				}
				return false;
			}
		} catch (Exception e) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Can't handle file " + artifact.getName(), e);
			}
			return false;
		} finally {
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Failed to close WAR file", e);
					}
				}
			}
		}
		try {
			new URL("webbundle", null, artifact.toURI().toURL().toExternalForm());
		} catch (MalformedURLException e) {
			LOG.warn("File {} could not be transformed. Most probably that Pax URL WAR handler is not installed", artifact.getAbsolutePath());
			return false;
		}

		return true;
	}

	public URL transform(final URL artifact) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Transforming artifact with URL: " + artifact);
		}
		final String path = artifact.getPath();
		final String protocol = artifact.getProtocol();
		if (path != null) {
			int idx;
			// match the last slash to retrieve the name of the archive
			if ("jardir".equalsIgnoreCase(protocol)) {
				// just to make sure this works on all kinds of windows
				File fileInstance = new File(path);
				// with a jardir this is system specific
				idx = fileInstance.getAbsolutePath().lastIndexOf(File.separator);
			} else {
				// a standard file is not system specific, this is always a
				// standardized URL path
				idx = path.lastIndexOf(PATH_SEPERATOR);
			}
			// match the suffix so we get rid of it for displaying
			if (idx > 0) {
				final String[] name = DeployerUtils.extractNameVersionType(path.substring(idx + 1));
				final StringBuilder url = new StringBuilder();
				url.append(artifact.toExternalForm());
				if (artifact.toExternalForm().contains("?")) {
					url.append("&");
				} else {
					url.append("?");
				}
				url.append("Web-ContextPath=").append(name[0]);
				url.append("&");
				url.append("Bundle-SymbolicName=").append(name[0]);
				url.append("&");
				url.append("Bundle-Version=").append(name[1]);

				LOG.debug("Transformed URL of {} to following {}", path, url);
				return new URL("webbundle", null, url.toString());
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("No path for given artifact, retry with webbundle prepended");
		}
		return new URL("webbundle", null, artifact.toExternalForm());
	}

}
