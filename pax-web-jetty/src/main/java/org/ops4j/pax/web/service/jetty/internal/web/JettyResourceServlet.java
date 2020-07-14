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
package org.ops4j.pax.web.service.jetty.internal.web;

import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of Jetty's <em>default servlet</em> to satisfy the resource contract from Http Service and Whiteboard
 * Service specifications.
 */
public class JettyResourceServlet extends DefaultServlet {

	public static final Logger LOG = LoggerFactory.getLogger(JettyResourceServlet.class);

	/** If specified, this is the directory to fetch resource files from */
	private final PathResource baseUrlResource;

	/**
	 * If {@link #baseUrlResource} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	// super._welcomes can be cleared after super.init()...
	private String[] welcomeFiles;

	public JettyResourceServlet(PathResource baseUrlResource, String chroot) {
		this.baseUrlResource = baseUrlResource;
		this.chroot = chroot;
	}

	@Override
	public void init() throws UnavailableException {
		super.init();
		_welcomes = welcomeFiles;

		String maxCacheSize = getInitParameter("maxCacheSize");
		String maxCachedFileSize = getInitParameter("maxCachedFileSize");
		String maxCachedFiles = getInitParameter("maxCachedFiles");
		if (maxCacheSize == null) {
			maxCacheSize = Integer.toString(256 * 1024 * 1024 / 64);
		}
		if (maxCachedFileSize == null) {
			maxCachedFileSize = Integer.toString(128 * 1024 * 1024 / 64);
		}
		if (maxCachedFiles == null) {
			maxCachedFiles = "2048";
		}

		LOG.info("Initialized Jetty Resource Servlet for base=\"{}\" with cache maxSize={}kB, maxEntrySize={}kB, maxEntries={}",
				baseUrlResource != null ? baseUrlResource.getPath() : chroot,
				Integer.parseInt(maxCacheSize) / 1024,
				Integer.parseInt(maxCachedFileSize) / 1024,
				maxCachedFiles);
	}

	/**
	 * By making {@link DefaultServlet#_welcomes} protected, we can set those files without reinitializing the
	 * servlet
	 * @param welcomeFiles
	 */
	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
		_welcomes = welcomeFiles;
		if (_cache != null) {
			_cache.flushCache();
		}
	}

	@Override
	protected ContextHandler initContextHandler(ServletContext servletContext) {
		// necessary for super.init()
		if (servletContext instanceof ContextHandler.Context) {
			return ((ContextHandler.Context) servletContext).getContextHandler();
		}
		return ((ContextHandler.Context)((OsgiScopedServletContext)servletContext).getContainerServletContext()).getContextHandler();
	}

	@Override
	public Resource getResource(String pathInContext) {
		// our (commons-io) normalized path
		String childPath = Path.securePath(pathInContext);
		if (childPath == null) {
			return null;
		}
		if (childPath.startsWith("/")) {
			childPath = childPath.substring(1);
		}

		try {
			if (baseUrlResource != null) {
				// Pax Web special - direct access to configured directory with proper metadata handling
				// (size, lastModified) for caching purposes
				if ("".equals(childPath)) {
					// root directory access. Just return base resource and let super class handle welcome files
					return baseUrlResource;
				}
				return baseUrlResource.addPath(childPath);
			} else {
				// HttpService/Whiteboard behavior - resourceBase is prepended to argument for context resource
				// remember - under ServletContext there should be WebContainerContext that wraps
				// HttpContext or ServletContextHelper
				// before Pax Web 8 there was explicit delegation to HttpContext, but now, it's hidden
				// under Osgi(Scoped)ServletContext
				URL url = getServletContext().getResource(chroot + "/" + childPath);

				// we have to check if the URL points to the root of the bundle. Felix throws IOException
				// when opening connection for URIs like "bundle://22.0:1/"
				if (url != null) {
					if ("bundle".equals(url.getProtocol())) {
						if ("/".equals(url.getPath())) {
							// Felix, root of the bundle - return a resource which says it's a directory
							return new RootBundleURLResource(Resource.newResource(url));
						} else if (!url.getPath().endsWith("/")) {
							// unfortunately, due to https://issues.apache.org/jira/browse/FELIX-6294
							// we have to check ourselves if it's a directory and possibly append a slash
							// just as org.eclipse.osgi.storage.bundlefile.BundleFile#fixTrailingSlash() does it
							Resource potentialDirectory = Resource.newResource(url);
							if (potentialDirectory.exists() && potentialDirectory.length() == 0) {
								URL fixedURL = new URL(url.toExternalForm() + "/");
								Resource properDirectory = Resource.newResource(fixedURL);
								if (properDirectory.exists()) {
									return properDirectory;
								}
							}
						}
					}
				}

				// resource can be provided by custom HttpContext/ServletContextHelper, so we can't really
				// affect lastModified for caching purposes
				return Resource.newResource(url);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
