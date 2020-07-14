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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Set;
import java.util.jar.Manifest;
import javax.servlet.ServletContext;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.WebResourceSet;
import org.apache.catalina.webresources.AbstractResource;
import org.apache.catalina.webresources.AbstractResourceSet;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.EmptyResource;
import org.apache.catalina.webresources.FileResource;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.juli.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OsgiStandardRoot extends StandardRoot {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiStandardRoot.class);

	private final ServletContext osgiScopedServletContext;

	private final File baseDirectory;
	private final String chroot;
	private final WebResourceRoot root;
	private final int maxEntrySize;

	OsgiStandardRoot(WebResourceRoot root, File baseDirectory, String chroot, ServletContext osgiScopedServletContext, int maxEntrySize) {
		super(root.getContext());
		this.root = root;
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
		this.osgiScopedServletContext = osgiScopedServletContext;
		this.maxEntrySize = maxEntrySize;
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
						if (fullPath.equals("")) {
							fullPath = "/";
						}
						try {
							resource = osgiScopedServletContext.getResource(fullPath);
						} catch (MalformedURLException e) {
							TomcatResourceServlet.LOG.warn(e.getMessage(), e);
						}
						if (resource == null) {
							return new EmptyResource(root, path);
						}
					}
					if (resource.getProtocol().equals("file")) {
						try {
							File file = new File(resource.toURI());
							if (file.isFile() || file.isDirectory()) {
								return new FileResource(root, fullPath, file, true, null);
							}
							return new EmptyResource(root, path);
						} catch (URISyntaxException e) {
							LOG.warn(e.getMessage(), e);
							return new EmptyResource(root, path);
						}
					} else if (resource.getProtocol().equals("bundle")) {
						if ("/".equals(resource.getPath())) {
							// Felix, root of the bundle - return a resource which says it's a directory
							return new RootBundleURLResource(OsgiStandardRoot.this, resource, fullPath);
						} else if (!resource.getPath().endsWith("/")) {
							// unfortunately, due to https://issues.apache.org/jira/browse/FELIX-6294
							// we have to check ourselves if it's a directory and possibly append a slash
							// just as org.eclipse.osgi.storage.bundlefile.BundleFile#fixTrailingSlash() does it
							try {
								UrlResource potentialDirectory = new UrlResource(OsgiStandardRoot.this, resource, fullPath, maxEntrySize);
								if (potentialDirectory.exists()) {
									try (InputStream is = potentialDirectory.getInputStream()) {
										if (is == null || is.available() == 0) {
											URL fixedURL = new URL(resource.toExternalForm() + "/");
											UrlResource properDirectory = new UrlResource(OsgiStandardRoot.this, fixedURL, fullPath, maxEntrySize);
											if (properDirectory.exists()) {
												return properDirectory;
											}
										}
									}
								}
							} catch (IOException e) {
								LOG.warn("Problem checking directory bundle resource: {}", e.getMessage(), e);
								return new EmptyResource(root, path);
							}
						}
					}

					try {
						return new UrlResource(OsgiStandardRoot.this, resource, fullPath, maxEntrySize);
					} catch (IOException e) {
						LOG.warn(e.getMessage(), e);
						return new EmptyResource(root, path);
					}
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

	/**
	 * Based on org.eclipse.jetty.util.resource.URLResource
	 */
	private static class UrlResource extends AbstractResource {

		private final URL url;
		private final int maxEntrySize;

		private URLConnection urlConnection;
		private InputStream in;
		private final File file;
		private byte[] content;

		UrlResource(WebResourceRoot root, URL url, String fullPath, int maxEntrySize) throws IOException {
			super(root, fullPath);
			this.url = url;
			this.maxEntrySize = maxEntrySize;

			doGetInputStream(false);

			this.file = new File(url.getPath());
		}

		@Override
		protected InputStream doGetInputStream() {
			return doGetInputStream(true);
		}

		private synchronized InputStream doGetInputStream(boolean forceNew) {
			if (urlConnection == null) {
				try {
					this.urlConnection = this.url.openConnection();
					this.urlConnection.setUseCaches(true);
					this.in = this.urlConnection.getInputStream();

					if (getContentLength() <= maxEntrySize) {
						// we can cache the content, as
						// in org.eclipse.jetty.server.CachedContentFactory.CachedHttpContent.getDirectBuffer
						byte[] buf = new byte[4096];
						int read = -1;
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						try {
							while ((read = this.in.read(buf)) > 0) {
								baos.write(buf, 0, read);
							}
							content = baos.toByteArray();
						} finally {
							in.close();
							in = null;
						}
					}
				} catch (IOException e) {
					LOG.warn(e.getMessage(), e);
				}
			}
			InputStream result = in;
			if (forceNew) {
				in = null;
				urlConnection = null;
			}
			return result;
		}

		@Override
		protected Log getLog() {
			return null;
		}

		@Override
		public long getLastModified() {
			return urlConnection.getLastModified();
		}

		@Override
		public boolean exists() {
			doGetInputStream(false);
			return in != null || content != null;
		}

		@Override
		public boolean isVirtual() {
			return true;
		}

		@Override
		public boolean isDirectory() {
			return url.getPath().endsWith("/");
		}

		@Override
		public boolean isFile() {
			return !url.getPath().endsWith("/");
		}

		@Override
		public boolean delete() {
			return false;
		}

		@Override
		public String getName() {
			return file.getName();
		}

		@Override
		public long getContentLength() {
			return urlConnection.getContentLength();
		}

		@Override
		public String getCanonicalPath() {
			try {
				return file.getCanonicalPath();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		@Override
		public boolean canRead() {
			doGetInputStream(false);
			return in != null || content != null;
		}

		@Override
		public byte[] getContent() {
			return content;
		}

		@Override
		public long getCreation() {
			return urlConnection.getLastModified();
		}

		@Override
		public URL getURL() {
			return url;
		}

		@Override
		public URL getCodeBase() {
			return null;
		}

		@Override
		public Certificate[] getCertificates() {
			return new Certificate[0];
		}

		@Override
		public Manifest getManifest() {
			return null;
		}
	}

	private static class RootBundleURLResource extends AbstractResource {

		private final URL url;

		RootBundleURLResource(WebResourceRoot root, URL url, String fullPath) {
			super(root, fullPath);
			this.url = url;
		}

		@Override
		protected InputStream doGetInputStream() {
			return null;
		}

		@Override
		protected Log getLog() {
			return null;
		}

		@Override
		public long getLastModified() {
			return 0;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public boolean isVirtual() {
			return false;
		}

		@Override
		public boolean isDirectory() {
			return true;
		}

		@Override
		public boolean isFile() {
			return false;
		}

		@Override
		public boolean delete() {
			return false;
		}

		@Override
		public String getName() {
			return "/";
		}

		@Override
		public long getContentLength() {
			return 0;
		}

		@Override
		public String getCanonicalPath() {
			return "/";
		}

		@Override
		public boolean canRead() {
			return false;
		}

		@Override
		public byte[] getContent() {
			return new byte[0];
		}

		@Override
		public long getCreation() {
			return 0;
		}

		@Override
		public URL getURL() {
			return url;
		}

		@Override
		public URL getCodeBase() {
			return null;
		}

		@Override
		public Certificate[] getCertificates() {
			return new Certificate[0];
		}

		@Override
		public Manifest getManifest() {
			return null;
		}
	}

}
