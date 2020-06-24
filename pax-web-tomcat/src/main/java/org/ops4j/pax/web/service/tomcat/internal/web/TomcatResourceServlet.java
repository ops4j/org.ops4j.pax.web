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
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.servlets.DefaultServlet;
import org.ops4j.pax.web.service.spi.config.ResourceConfiguration;
import org.ops4j.pax.web.service.spi.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of Tomcat's <em>default servlet</em> to satisfy the resource contract from Http Service and Whiteboard
 * Service specifications.
 */
public class TomcatResourceServlet extends DefaultServlet {

	public static final Logger LOG = LoggerFactory.getLogger(TomcatResourceServlet.class);

	/** If specified, this is the directory to fetch resource files from */
	private final File baseDirectory;

	/**
	 * If {@link #baseDirectory} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	private final ResourceConfiguration resourceConfig;

	public TomcatResourceServlet(File baseDirectory, String chroot, ResourceConfiguration resourceConfig) {
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
		this.resourceConfig = resourceConfig;
	}

	@Override
	public void init() throws ServletException {
		super.init();

		super.listings = false;
		super.readOnly = true;
		super.showServerInfo = false;

		// super.init() created DefaultServlet.resources (fortunately protected, not private) as:
		//     resources = (WebResourceRoot) getServletContext().getAttribute(Globals.RESOURCES_ATTR);
		// but we want to be able to create more "resource servlets" for different bases

		final ServletContext osgiScopedServletContext = getServletContext();

		// and tweak org.apache.catalina.servlets.DefaultServlet.resources
		resources = new OsgiStandardRoot(this.resources, baseDirectory, chroot, osgiScopedServletContext);

		resources.setCachingAllowed(true);
		// org.apache.catalina.webresources.Cache.maxSize
		resources.setCacheMaxSize(resourceConfig == null || resourceConfig.maxTotalCacheSize() == null
				? 10 * 1024 : resourceConfig.maxTotalCacheSize());
		// org.apache.catalina.webresources.Cache.objectMaxSize
		resources.setCacheObjectMaxSize(resourceConfig == null || resourceConfig.maxCacheEntrySize() == null
				? (int) resources.getCacheMaxSize() / 20 : resourceConfig.maxCacheEntrySize());
		// org.apache.catalina.webresources.Cache.ttl
		resources.setCacheTtl(resourceConfig == null || resourceConfig.maxCacheTTL() == null
				? 5000 : resourceConfig.maxCacheTTL());

		try {
			resources.start();
		} catch (LifecycleException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	/**
	 * Override {@link DefaultServlet#getRelativePath(HttpServletRequest, boolean)} to use only path info. Just
	 * as {@link org.apache.catalina.servlets.WebdavServlet} and just as Jetty does it with {@code pathInfoOnly}
	 * servlet init parameter.
	 *
	 * @param request
	 * @param allowEmptyPath
	 * @return
	 */
	@Override
	protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {
		String pathInfo;

		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
		} else {
			pathInfo = request.getPathInfo();
		}

		StringBuilder result = new StringBuilder();
		if (pathInfo != null) {
			result.append(pathInfo);
		}
		if (result.length() == 0) {
			result.append('/');
		}
		String pathInContext = result.toString();

		// our (commons-io) normalized path
		String childPath = Path.securePath(pathInContext);
		if (childPath == null) {
			return null;
		}
		if (!childPath.startsWith("/")) {
			childPath = "/" + childPath;
		}

		return childPath;
	}

}
