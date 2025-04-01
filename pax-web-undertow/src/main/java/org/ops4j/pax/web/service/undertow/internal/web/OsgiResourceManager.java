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
package org.ops4j.pax.web.service.undertow.internal.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import jakarta.servlet.ServletContext;

import io.undertow.server.handlers.resource.PathResource;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import org.ops4j.pax.web.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiResourceManager implements ResourceManager {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiResourceManager.class);

	/**
	 * The {@link jakarta.servlet.ServletContext} which (according to OSGi CMPN web specifications) should delegate
	 * to {@link org.ops4j.pax.web.service.http.HttpContext} / {@link org.osgi.service.servlet.context.ServletContextHelper}.
	 */
	private final ServletContext osgiScopedServletContext;

	/**
	 * 2nd parameter of {@link org.ops4j.pax.web.service.http.HttpService#registerResources(String, String, HttpContext)}
	 * or {@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_RESOURCE_PREFIX}
	 */
	private final String chroot;

	// used when the resource is file:
	private final FileETagFunction fileETagFunction;
	private final PathResourceManager pathResourceManager;

	private boolean directFileMapping = false;

	public OsgiResourceManager(String chroot, ServletContext osgiScopedServletContext) {
		this.chroot = chroot;
		this.osgiScopedServletContext = osgiScopedServletContext;
		this.fileETagFunction = new FileETagFunction();
		File location = (File) osgiScopedServletContext.getAttribute(ServletContext.TEMPDIR);
		if (location == null) {
			location = new File(System.getProperty("java.io.tmpdir"));
		}
		this.pathResourceManager = (PathResourceManager) PathResourceManager.builder()
				// base won't be used
				.setBase(location.toPath())
				.build();

		try {
			URL url = chroot == null ? null : this.osgiScopedServletContext.getResource(chroot);
			directFileMapping = url != null && !url.getPath().endsWith("/");
		} catch (Exception ignored) {
		}
	}

	@Override
	public Resource getResource(String path) throws IOException {
		// Almost the same as in org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet.OsgiStandardRoot

		// chroot is without trailing slash, path is always without leading slash
		String fullPath = directFileMapping ? chroot : chroot + "/" + path;
		if (!fullPath.startsWith("/")) {
			fullPath = "/" + fullPath;
		}
		URL resource = null;
		try {
			resource = osgiScopedServletContext.getResource(fullPath);
		} catch (MalformedURLException e) {
			LOG.warn(e.getMessage(), e);
		}
		if (resource == null) {
			return null;
		}

		Resource res = null;
		if (resource.getProtocol().equals("file")) {
			try {
				Path file = Paths.get(resource.toURI());
				if (file.toFile().isFile()) {
					res = new PathResource(file, pathResourceManager, resource.getPath(), fileETagFunction.generate(file));
				} else if (file.toFile().exists()) {
					// could be a directory
					res = new PathResource(file, pathResourceManager, resource.getPath(), null);
				} else {
					return null;
				}
			} catch (URISyntaxException e) {
				LOG.warn(e.getMessage(), e);
				return null;
			}
		} else if (resource.getProtocol().equals("bundle") && !resource.getPath().endsWith("/")) {
			// unfortunately, due to https://issues.apache.org/jira/browse/FELIX-6294
			// we have to check ourselves if it's a directory and possibly append a slash
			// just as org.eclipse.osgi.storage.bundlefile.BundleFile#fixTrailingSlash() does it
			URLResource potentialDirectory = new URLResource(resource, resource.getPath());
			if (potentialDirectory.getContentLength() == null || potentialDirectory.getContentLength() == 0L) {
				try (InputStream is = potentialDirectory.getUrl().openStream()) {
					if (is == null || is.available() == 0) {
						URL fixedURL = new URL(resource.toExternalForm() + "/");
						try (InputStream is2 = fixedURL.openStream()) {
							if (is2 != null && is2.available() == 0) {
								res = new URLResource(fixedURL, fixedURL.getPath());
							}
						}
					}
				}
			}
		}

		if (res == null) {
			res = new URLResource(resource, resource.getPath());
		}

		return res;
	}

	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {
		// no op
	}

	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {
		// no op
	}

	@Override
	public void close() throws IOException {
		// no op
	}

}
