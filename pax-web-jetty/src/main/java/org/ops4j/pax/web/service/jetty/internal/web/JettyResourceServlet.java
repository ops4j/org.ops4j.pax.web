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

import javax.servlet.ServletContext;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.ee8.nested.ContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.http.content.ValidatingCachingHttpContentFactory;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of Jetty's <em>default servlet</em> to satisfy the resource contract from Http Service and Whiteboard
 * Service specifications.
 */
public class JettyResourceServlet extends DefaultServlet {

	public static final Logger LOG = LoggerFactory.getLogger(JettyResourceServlet.class);

	public static final ThreadLocal<Resource> BASE_RESOURCE = new ThreadLocal<>();

	/** If specified, this is the directory to fetch resource files from */
	private final PathResource baseUrlResource;

	/**
	 * If {@link #baseUrlResource} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	private PaxWebResource baseResource;

	public JettyResourceServlet(PathResource baseUrlResource, String chroot) {
		this.baseUrlResource = baseUrlResource;
		this.chroot = chroot;
	}

	@Override
	public void init() throws UnavailableException {
		this.baseResource = new PaxWebResource(getServletContext(), baseUrlResource, chroot);
		try {
			BASE_RESOURCE.set(baseResource);
			super.init();

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
		} finally {
			BASE_RESOURCE.remove();
		}
	}

	/**
	 * By making {@link DefaultServlet#_welcomes} protected, we can set those files without reinitializing the
	 * servlet
	 * @param welcomeFiles
	 */
	public void setWelcomeFiles(String[] welcomeFiles) {
		_welcomes = welcomeFiles;
		if (_resourceService != null && _resourceService.getHttpContentFactory() instanceof ValidatingCachingHttpContentFactory cache) {
			cache.flushCache();
		}
	}

	public void setWelcomeFilesRedirect(boolean welcomeFilesRedirect) {
		if (_resourceService != null) {
			// already init()ed
			_resourceService.setRedirectWelcome(welcomeFilesRedirect);
		}
	}

	@Override
	protected ContextHandler initContextHandler(ServletContext servletContext) {
		// necessary for super.init() and to reimplement
		// org.eclipse.jetty.server.handler.ContextHandler.getBaseResource()
		ServletContextHandler handler;
		if (servletContext instanceof OsgiServletContext osgiServletContext) {
			return super.initContextHandler(osgiServletContext.getContainerServletContext());
		}
		if (servletContext instanceof OsgiScopedServletContext osgiScopedServletContext) {
			return super.initContextHandler(osgiScopedServletContext.getContainerServletContext());
		}
		return super.initContextHandler(servletContext);
	}

}
