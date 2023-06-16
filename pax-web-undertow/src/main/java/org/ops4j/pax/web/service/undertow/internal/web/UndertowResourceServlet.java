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
import java.nio.file.Paths;
import java.util.Enumeration;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Extension of {@link DefaultServlet}, so we can use many of such servlets to serve resources from different bases
 * for the purpose of "resource" handling specified in HttpService and Whiteboard specifications.</p>
 *
 * <p>Due to caching infrastructure of Undertow, we need this servlet to implement {@link ResourceManager}, because
 * {@link io.undertow.server.handlers.resource.CachingResourceManager} should be created up front. The details
 * of {@link ResourceManager} will be set up in {@link jakarta.servlet.Servlet#init(ServletConfig)}.</p>
 *
 * <p>This servlet extends forked version of oroginal {@link io.undertow.servlet.handlers.DefaultServlet} and
 * is implemented as if the original version was more extensible. After forking I could implement everything
 * inside the forked version, but if in future, Undertow opens the default servlet more, we could get rid of
 * the fork.</p>
 */
public class UndertowResourceServlet extends DefaultServlet implements ResourceManager {

	public static final Logger LOG = LoggerFactory.getLogger(UndertowResourceServlet.class);

	private ResourceManager cachingResourceManager;

	/** If specified, this is the directory to fetch resource files from */
	private final File baseDirectory;

	/**
	 * If {@link #baseDirectory} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.servlet.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	/** The real {@link ResourceManager} configured in {@link jakarta.servlet.Servlet#init(ServletConfig)} */
	private ResourceManager resourceManager;

	private String[] welcomeFiles;

	private boolean redirectWelcome = false;
	private boolean pathInfoOnly = true;

	private boolean cacheConfigurable = false;
	private int metadataCacheSize;
	private Integer maxEntrySize;
	private Integer maxSize;
	private Integer maxAge;

	public UndertowResourceServlet(File baseDirectory, String chroot) {
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
	}

	/**
	 * This method can be used in tests to set ready, but not managable {@link CachingResourceManager}
	 * @param cachingResourceManager
	 */
	public void setCachingResourceManager(CachingResourceManager cachingResourceManager) {
		this.cachingResourceManager = cachingResourceManager;
	}

	/**
	 * This method should be called if we want to be able to recreate (clean) the cache after setting new
	 * welcome files.
	 * @param metadataCacheSize
	 * @param maxEntrySize
	 * @param maxSize
	 * @param maxAge
	 */
	public void setCachingConfiguration(int metadataCacheSize, Integer maxEntrySize, Integer maxSize, Integer maxAge) {
		this.metadataCacheSize = metadataCacheSize;
		this.maxEntrySize = maxEntrySize;
		this.maxSize = maxSize;
		this.maxAge = maxAge;
		this.cacheConfigurable = true;
	}

	public void setWelcomeFiles(String[] welcomeFiles) {
		this.welcomeFiles = welcomeFiles;
		configureCache();
	}

	public void setWelcomeFilesRedirect(boolean welcomeFilesRedirect) {
		this.redirectWelcome = welcomeFilesRedirect;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		configureCache();

		// we need to call super.init() with:
		//  - a config that returns proper servletContext implementation
		//  - this servletContext has to return proper deployment
		//  - deployment is used to get deployment info and servlet context
		//  - deployment info is needed to call:
		//     - io.undertow.servlet.api.DeploymentInfo.getDefaultServletConfig() (can be null)
		//     - io.undertow.servlet.api.DeploymentInfo.getPreCompressedResources()
		//     - io.undertow.servlet.api.DeploymentInfo.getResourceManager() - that's the most important method
		//       we have to override
		//
		// I know it's not very good design, but pax-web-jetty and pax-web-tomcat use server native "resource servlet"
		// so I tried hard to use Undertow specific "resource servlet" as well
		// this special context is used to obtain tweaked io.undertow.servlet.api.Deployment object
		FlexibleDeployment deployment = new FlexibleDeployment(config.getServletContext(), cachingResourceManager);
		ServletContext flexibleServletContext = new FlexibleServletContextImpl(deployment);

		super.init(new ServletConfig() {
			@Override
			public String getServletName() {
				return config.getServletName();
			}

			@Override
			public ServletContext getServletContext() {
				return flexibleServletContext;
			}

			@Override
			public String getInitParameter(String name) {
				return config.getInitParameter(name);
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return config.getInitParameterNames();
			}
		});

		redirectWelcome = "true".equalsIgnoreCase(getInitParameter("redirectWelcome"));
		pathInfoOnly = !"false".equalsIgnoreCase(getInitParameter("pathInfoOnly"));

		ServletContext osgiScopedServletContext = config.getServletContext();
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

		// not we can configure our pieces needed for io.undertow.server.handlers.resource.ResourceManager
		if (baseDirectory != null) {
			this.resourceManager = FileResourceManager.builder()
					.setBase(Paths.get(baseDirectory.getAbsolutePath()))
					.setETagFunction(new FileETagFunction())
					.build();
		} else {
			// resource will be obtained from HttpContext.getResource() / ServletContextHelper.getResource()
			// assuming that servletContext is Osgi[Scoped]ServletContext that delegate to WebContainerContext
			// it's important to get ServletContext from the passed config!
			this.resourceManager = new OsgiResourceManager(chroot, config.getServletContext());
		}
	}

	private void configureCache() {
		if (cacheConfigurable) {
			// io.undertow.server.handlers.file.FileHandlerStressTestCase#simpleFileStressTest uses "1024, 10, 10480"
			// see:
			// this.pool = new LimitedBufferSlicePool(..., sliceSize, sliceSize * slicesPerPage, maxMemory / (sliceSize * slicesPerPage));
			int maxMemory = maxSize;
			int maxRegions = 1;
			int maxRegionSize = maxSize;
			int slicePerPage = 32;
			int sliceSize = maxSize / slicePerPage;
			DirectBufferCache cache = new DirectBufferCache(sliceSize, slicePerPage, maxMemory);
			cachingResourceManager
					= new CachingResourceManager(metadataCacheSize, maxEntrySize, cache, this, maxAge);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// we leave the most of the work do be done in original default servlet, but handle "directory access"
		// first to add support for welcome files at this level (just as we did with TomcatResourceServlet) and
		// what Jetty provides out of the box

		boolean included = false;
		String requestURI = (String) req.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
		if (requestURI == null) {
			// we're not included
			requestURI = req.getRequestURI();
		} else {
			included = true;
		}

		if (requestURI == null || !requestURI.endsWith("/")) {
			super.doGet(req, resp);
			return;
		}

		String servletPath = (String) req.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
		if (servletPath == null) {
			servletPath = !included ? req.getServletPath() : "";
		}
		if (servletPath.endsWith("/")) {
			// strange - when req is /sub/ mapped to / servlet, servlet path should be /, but is /sub/...
			servletPath = servletPath.substring(0, servletPath.length() - 1);
		}

		// the req ends with slash
		// modelled after:
		//  - org.eclipse.jetty.server.ResourceService.sendWelcome()
		//  - org.eclipse.jetty.servlet.DefaultServlet.getWelcomeFile()

		// relative path will always start with slash and is relative to our servlet path
		String relativePath = getPath(req);
		if ("".equals(relativePath)) {
			relativePath = "/";
		}

		// chapter 10.10 "Welcome files" of Servlet API specification defines two iterations. First, for
		// all welcome files, we have to check whether existing physical resource exists and return it.
		// only after NO physical resource is found for ALL welcome files, we have to check wheter there's
		// servlet mapping for given path (after appending welcome file to req URI with trailing slash)
		//
		// we can simply use req dispatcher even if org.eclipse.jetty.servlet.DefaultServlet.getWelcomeFile()
		// explicitly calls org.eclipse.jetty.servlet.ServletHandler.getMappedServlet(), because req
		// dispatcher internally performs full mapping

		HttpServerExchange exchange = requireCurrentServletRequestContext().getOriginalRequest().getExchange();
		String resolvedWelcome = null;

		// 1) physical resources (but checked for pathInfo only):
		for (String welcome : welcomeFiles) {
			String path = relativePath + welcome;
			Resource resource = resourceSupplier.getResource(exchange, path);
			if (resource != null) {
				// redirect/include/forward has to be done with our context + servlet path
				resolvedWelcome = pathInfoOnly ? servletPath + path : path;
				break;
			}
		}

		// 2) web component mapping
		RequestDispatcher dispatcher = null;
		if (resolvedWelcome == null) {
			for (String welcome : welcomeFiles) {
				// path uses servlet path as well
				String path = servletPath + relativePath + welcome;
				dispatcher = req.getRequestDispatcher(path);
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
				String queryString = req.getQueryString();
				if (queryString != null && !"".equals(queryString)) {
					resolvedWelcome = resolvedWelcome + "?" + queryString;
				}
				resolvedWelcome = req.getContextPath() + resolvedWelcome;
				resp.sendRedirect(resp.encodeRedirectURL(resolvedWelcome));
			} else {
				if (dispatcher == null) {
					// physical welcome file
					dispatcher = req.getRequestDispatcher(resolvedWelcome);
				}
				if (included) {
					dispatcher.include(req, resp);
				} else {
					dispatcher.forward(req, resp);
				}
			}
			return;
		}

		// last check - if resource ending with / is a file and really doesn't exist (as directory)
		// we'll return 404, as 403 would suggest it exists
		Resource resource = resourceSupplier.getResource(exchange, relativePath);
		if (resource == null) {
			if (req.getDispatcherType() == DispatcherType.ERROR) {
				resp.sendError((Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, requestURI);
			}
			return;
		}

		// no dispatch? consequently we'll return 403
		if (req.getDispatcherType() == DispatcherType.ERROR) {
			resp.sendError((Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
		} else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, requestURI);
		}

		super.doGet(req, resp);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// https://github.com/ops4j/org.ops4j.pax.web/issues/1664
		//
		// instead of relying on reflection used in jakarta.servlet.http.HttpServlet.doOptions()
		// we simply return fixed set of methods (to be compatible with Jetty and Tomcat)
		resp.setHeader("Allow", "OPTIONS, GET, HEAD, POST");
	}

	@Override
	public Resource getResource(String path) throws IOException {
		HttpServletRequestImpl originalRequest = requireCurrentServletRequestContext().getOriginalRequest();
		String pathInfo = originalRequest.getPathInfo();
		if (pathInfo == null && pathInfoOnly) {
			// fix private io.undertow.servlet.handlers.DefaultServlet.getPath(), which returns servletPath
			// when pathInfo is null - we never want the servletPath when accessing resources - we only
			// need pathInfo.
			// When resource servlet is mapped to /p, incoming request /p/ is handled correctly - "" resource is
			// passed to this getResource(), but when the request is /p, "p" is passed here
			String servletPath = originalRequest.getServletPath();
			if (servletPath != null && servletPath.startsWith("/")) {
				servletPath = servletPath.substring(1);
			}
			if (path.equals(servletPath)) {
				// path was set to servletPath without leading slash - we need actual path info - path under
				// the servlet path
				path = "";
			}
		}
		return resourceManager.getResource(path);
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
		resourceManager.close();
	}

}
