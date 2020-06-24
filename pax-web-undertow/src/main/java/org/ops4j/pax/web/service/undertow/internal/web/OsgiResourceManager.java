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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletContext;

import io.undertow.server.handlers.resource.PathResource;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.handlers.resource.URLResource;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiResourceManager implements ResourceManager {

	public static final Logger LOG = LoggerFactory.getLogger(OsgiResourceManager.class);

	/**
	 * The {@link javax.servlet.ServletContext} which (according to OSGi CMPN web specifications) should delegate
	 * to {@link org.osgi.service.http.HttpContext} / {@link org.osgi.service.http.context.ServletContextHelper}.
	 */
	private final ServletContext osgiScopedServletContext;

	/**
	 * 2nd parameter of {@link org.osgi.service.http.HttpService#registerResources(String, String, HttpContext)}
	 * or {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_RESOURCE_PREFIX}
	 */
	private final String chroot;

	// used when the resource is file:
	private final FileETagFunction fileETagFunction;
	private final PathResourceManager pathResourceManager;

	public OsgiResourceManager(String chroot, ServletContext osgiScopedServletContext) {
		this.chroot = chroot;
		this.osgiScopedServletContext = osgiScopedServletContext;
		this.fileETagFunction = new FileETagFunction();
		this.pathResourceManager = (PathResourceManager) PathResourceManager.builder()
				// base won't be used
				.setBase(Paths.get(ServletContext.TEMPDIR))
				.build();
	}

	@Override
	public Resource getResource(String path) throws IOException {
		// Almost the same as in org.ops4j.pax.web.service.tomcat.internal.web.TomcatResourceServlet.OsgiStandardRoot

		// chroot is without trailing slash, path is always without leading slash
		String fullPath = chroot + "/" + path;
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

		Resource res;
		if (resource.getProtocol().equals("file")) {
			try {
				Path file = Paths.get(resource.toURI());
				if (!file.toFile().isFile()) {
					// we don't want directories, we want 404 instead
					return null;
				}
				res = new PathResource(file, pathResourceManager, resource.getPath(), fileETagFunction.generate(file));
			} catch (URISyntaxException e) {
				LOG.warn(e.getMessage(), e);
				return null;
			}
		} else {
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
