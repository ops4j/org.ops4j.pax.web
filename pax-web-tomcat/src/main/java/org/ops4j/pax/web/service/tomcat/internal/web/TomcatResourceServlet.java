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
import java.io.IOException;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResource;
import org.apache.catalina.servlets.DefaultServlet;
import org.ops4j.pax.web.service.spi.config.ResourceConfiguration;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Extension of Tomcat's <em>default servlet</em> to satisfy the resource contract from Http Service and Whiteboard
 * Service specifications.</p>
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

	private String[] welcomeFiles;

	private boolean redirectWelcome = false;
	private boolean pathInfoOnly = true;

	public TomcatResourceServlet(File baseDirectory, String chroot, ResourceConfiguration resourceConfig) {
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
		this.resourceConfig = resourceConfig;
	}

	/**
	 * Welcome files can be set directly (in tests) or will be obtained during {@link #init()} from
	 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext}
	 * @param welcomeFiles
	 */
	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
		if (resources != null) {
			// strange, but it works like this...
			resources.setCachingAllowed(false);
			resources.setCachingAllowed(true);
		}
	}

	public void setWelcomeFilesRedirect(boolean welcomeFilesRedirect) {
		this.redirectWelcome = welcomeFilesRedirect;
	}

	@Override
	public void init() throws ServletException {
		super.init();

		super.listings = false;
		super.readOnly = true;
		super.showServerInfo = false;

		redirectWelcome = "true".equalsIgnoreCase(getInitParameter("redirectWelcome"));
		pathInfoOnly = !"false".equalsIgnoreCase(getInitParameter("pathInfoOnly"));

		// super.init() created DefaultServlet.resources (fortunately protected, not private) as:
		//     resources = (WebResourceRoot) getServletContext().getAttribute(Globals.RESOURCES_ATTR);
		// but we want to be able to create more "resource servlets" for different bases

		final ServletContext osgiScopedServletContext = getServletContext();

		int maxEntrySize = resourceConfig == null || resourceConfig.maxCacheEntrySize() == null
				? (int) resources.getCacheMaxSize() / 20 : resourceConfig.maxCacheEntrySize();

		// and tweak org.apache.catalina.servlets.DefaultServlet.resources
		resources = new OsgiStandardRoot(this.resources, baseDirectory, chroot, osgiScopedServletContext, maxEntrySize * 1024);

		resources.setCachingAllowed(true);
		// org.apache.catalina.webresources.Cache.maxSize
		resources.setCacheMaxSize(resourceConfig == null || resourceConfig.maxTotalCacheSize() == null
				? 10 * 1024 : resourceConfig.maxTotalCacheSize());
		// org.apache.catalina.webresources.Cache.objectMaxSize
		resources.setCacheObjectMaxSize(maxEntrySize);
		// org.apache.catalina.webresources.Cache.ttl
		resources.setCacheTtl(resourceConfig == null || resourceConfig.maxCacheTTL() == null
				? 5000 : resourceConfig.maxCacheTTL());

		LOG.info("Initialized Tomcat Resource Servlet for base=\"{}\" with cache maxSize={}kB, maxEntrySize={}kB, TTL={}ms",
				baseDirectory != null ? baseDirectory : chroot,
				resources.getCacheMaxSize(), resources.getCacheObjectMaxSize(), resources.getCacheTtl());

		if (welcomeFiles == null) {
			if (osgiScopedServletContext instanceof OsgiScopedServletContext) {
				welcomeFiles = ((OsgiScopedServletContext) osgiScopedServletContext).getWelcomeFiles();
				redirectWelcome = ((OsgiScopedServletContext) osgiScopedServletContext).isWelcomeFilesRedirect();
			} else if (osgiScopedServletContext instanceof OsgiServletContext) {
				welcomeFiles = ((OsgiServletContext) osgiScopedServletContext).getWelcomeFiles();
				redirectWelcome = ((OsgiServletContext) osgiScopedServletContext).isWelcomeFilesRedirect();
			}
		}
		if (welcomeFiles == null) {
			welcomeFiles = new String[0];
		}

		try {
			resources.start();
		} catch (LifecycleException e) {
			throw new ServletException(e.getMessage(), e);
		}
	}

	@Override
	protected void serveResource(HttpServletRequest request, HttpServletResponse response, boolean content, String inputEncoding) throws IOException, ServletException {
		// an override of this huge method is needed to handle welcome files
		// there are no good extension points of super.serveResource(), so we have to agree on some duplication.
		// knowing that original method does directory redirect after fetching a response which is a directory,
		// but requestURI doesn't end with slash, we'll handle only the case, where request URI really ends with
		// slash. However, trailing slash doesn't necessarily mean that such directory exists.

		boolean included = false;
		String requestURI = (String) request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
		if (requestURI == null) {
			// we're not included
			requestURI = request.getRequestURI();
		} else {
			included = true;
		}

		if (requestURI == null || !requestURI.endsWith("/")) {
			super.serveResource(request, response, content, inputEncoding);
			return;
		}

		String servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
		if (servletPath == null) {
			servletPath = !included ? request.getServletPath() : "";
		}
		if (servletPath.endsWith("/")) {
			// strange - when request is /sub/ mapped to / servlet, servlet path should be /, but is /sub/...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}

		// the request ends with slash
		// modelled after:
		//  - org.eclipse.jetty.server.ResourceService.sendWelcome()
		//  - org.eclipse.jetty.servlet.DefaultServlet.getWelcomeFile()

		// relative path will always start with slash and is relative to our servlet path
		String relativePath = getRelativePath(request, true);
		if ("".equals(relativePath)) {
			relativePath = "/";
		}

		// chapter 10.10 "Welcome files" of Servlet API specification defines two iterations. First, for
		// all welcome files, we have to check whether existing physical resource exists and return it.
		// only after NO physical resource is found for ALL welcome files, we have to check wheter there's
		// servlet mapping for given path (after appending welcome file to request URI with trailing slash)
		//
		// we can simply use request dispatcher even if org.eclipse.jetty.servlet.DefaultServlet.getWelcomeFile()
		// explicitly calls org.eclipse.jetty.servlet.ServletHandler.getMappedServlet(), because request
		// dispatcher internally performs full mapping

		String resolvedWelcome = null;

		// 1) physical resources (but checked for pathInfo only):
		for (String welcome : welcomeFiles) {
			String path = relativePath + welcome;
			WebResource resource = resources.getResource(path);
			if (resource.exists()) {
				// redirect/include/forward has to be done with our context + servlet path
				resolvedWelcome = pathInfoOnly ? servletPath + path : path;
				break;
			}
		}

		// 2) web component mapping
		RequestDispatcher dispatcher = null;
		if (resolvedWelcome == null) {
			for (String welcome : welcomeFiles) {
				// path uses servlet path as well - always - not only with !pathInfoOnly, but because
				// pathInfoOnly=false is set ONLY for "/" servlet, it doesn't really matter
				String path = servletPath + relativePath + welcome;
				dispatcher = request.getRequestDispatcher(path);
				if (dispatcher != null) {
					resolvedWelcome = path;
					break;
				}
			}
		}

		if (resolvedWelcome != null) {
			if (redirectWelcome) {
				if (included) {
					LOG.warn("Can't redirect to welcome page for INCLUDE dispatch");
					return;
				}
				String queryString = request.getQueryString();
				if (queryString != null && !"".equals(queryString)) {
					resolvedWelcome = resolvedWelcome + "?" + queryString;
				}
				resolvedWelcome = request.getContextPath() + resolvedWelcome;
				response.sendRedirect(response.encodeRedirectURL(resolvedWelcome));
			} else {
				if (dispatcher == null) {
					// physical welcome file
					dispatcher = request.getRequestDispatcher(resolvedWelcome);
				}
				if (included) {
					dispatcher.include(request, response);
				} else {
					dispatcher.forward(request, response);
				}
			}
			return;
		}

		// last check - if resource ending with / is a file and really doesn't exist (as directory)
		// we'll return 404, as 403 would suggest it exists
		WebResource resource = resources.getResource(relativePath);
		if (!resource.exists()) {
			if (request.getDispatcherType() == DispatcherType.ERROR) {
				response.sendError((Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
			}
			return;
		}

		// no dispatch? consequently we'll return 403
		if (request.getDispatcherType() == DispatcherType.ERROR) {
			response.sendError((Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
		} else {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, requestURI);
		}
	}

	/**
	 * <p>Override {@link DefaultServlet#getRelativePath(HttpServletRequest, boolean)} to use only path info. Just
	 * as {@link org.apache.catalina.servlets.WebdavServlet} and just as Jetty does it with {@code pathInfoOnly}
	 * servlet init parameter.</p>
	 *
	 * <p>As with Jetty, {@code pathInfoOnly} has to be {@code false} because servlet path and path info are
	 * confusing when servlet is mapped to "/".</p>
	 *
	 * @param request
	 * @param allowEmptyPath
	 * @return
	 */
	@Override
	protected String getRelativePath(HttpServletRequest request, boolean allowEmptyPath) {
		String pathInfo;
		String servletPath;

		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
		} else {
			pathInfo = request.getPathInfo();
		}
		if (pathInfo == null) {
			pathInfo = "";
		}
		if (request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH) != null) {
			servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
		} else {
			servletPath = request.getServletPath();
		}

		String pathInContext = pathInfo;

		if (!pathInfoOnly) {
			pathInContext = servletPath + pathInfo;
		}

		// our (commons-io) normalized path
		String childPath = Path.securePath(pathInContext);
		if (childPath == null) {
			return null;
		}
		if (childPath.length() > 0 && !childPath.startsWith("/")) {
			childPath = "/" + childPath;
		}

		return childPath;
	}

}
