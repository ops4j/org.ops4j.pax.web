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
package org.ops4j.pax.web.service.tomcat.internal.web;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import javax.servlet.ServletContext;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.AbstractResourceSet;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.EmptyResource;
import org.apache.catalina.webresources.FileResource;
import org.apache.catalina.webresources.StandardRoot;

class OsgiStandardRoot extends StandardRoot {

	private final ServletContext osgiScopedServletContext;

	private final File baseDirectory;
	private final String chroot;
	private final WebResourceRoot root;

	OsgiStandardRoot(WebResourceRoot root, File baseDirectory, String chroot, ServletContext osgiScopedServletContext) {
		super(root.getContext());
		this.root = root;
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
		this.osgiScopedServletContext = osgiScopedServletContext;
	}

	@Override
	protected WebResourceSet createMainResourceSet() {
		if (baseDirectory != null) {
			// directory based resource (Pax Web special)
			return new DirResourceSet(this, "/", baseDirectory.getAbsolutePath(), "/");
		} else {
			// HttpService / Whiteboard case - resources are fetched from root or subdir of a bundle
			// through ServletContext.getResource() -> ServletContextHelper.getResource()
			return new AbstractResourceSet() {
				@Override
				public boolean getClassLoaderOnly() {
					return false;
				}

				@Override
				public boolean getStaticOnly() {
					// on purpose - we don't want to return classes, but it's only a hint, we're using
					// ServletContext.getResource() -> ServletContextHelper.getResource() anyway
					return true;
				}

				@Override
				protected void initInternal() throws LifecycleException {
					// no op
				}

				@Override
				public WebResource getResource(String path) {
					// chroot is without trailing slash, path is always with leading slash because that's
					// a requirement of org.apache.catalina.webresources.StandardRoot.validate()
					String fullPath = chroot + path;
					if (fullPath.startsWith("/")) {
						fullPath = fullPath.substring(1);
					}
					URL resource = null;
					try {
						resource = osgiScopedServletContext.getResource(fullPath);
					} catch (MalformedURLException e) {
						TomcatResourceServlet.LOG.warn(e.getMessage(), e);
					}
					if (resource == null) {
						return new EmptyResource(root, path);
					}
					if (resource.getProtocol().equals("file")) {
						try {
							File file = new File(resource.toURI());
							if (!file.isFile()) {
								return new EmptyResource(root, path);
							}
							return new FileResource(root, fullPath, file, true, null);
						} catch (URISyntaxException e) {
							TomcatResourceServlet.LOG.warn(e.getMessage(), e);
							return new EmptyResource(root, path);
						}
					}

					return new EmptyResource(root, path);
				}

				@Override
				public String[] list(String path) {
					return new String[0];
				}

				@Override
				public Set<String> listWebAppPaths(String path) {
					return Collections.emptySet();
				}

				@Override
				public boolean mkdir(String path) {
					// not supported
					return false;
				}

				@Override
				public boolean write(String path, InputStream is, boolean overwrite) {
					// not supported
					return false;
				}

				@Override
				public URL getBaseUrl() {
					// not exposed
					return null;
				}

				@Override
				public void setReadOnly(boolean readOnly) {
					// ignored
				}

				@Override
				public boolean isReadOnly() {
					return true;
				}

				@Override
				public void gc() {
					// no op
				}
			};
		}
	}
}
