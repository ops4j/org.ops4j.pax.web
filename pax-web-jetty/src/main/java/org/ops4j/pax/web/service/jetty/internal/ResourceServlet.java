/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.ops4j.pax.web.annotations.Review;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Review("This should be universal as template class (for Jetty/Tomcat/Undertow) with server specific extensions/delegates")
class ResourceServlet extends HttpServlet implements ResourceFactory {

	private static final int SECOND = 1000;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	// header constants
	// CHECKSTYLE:OFF
	private static final String IF_NONE_MATCH = "If-None-Match";
	@SuppressWarnings("unused")
	private static final String IF_MATCH = "If-Match";
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	@SuppressWarnings("unused")
	private static final String IF_RANGE = "If-Range";
	private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	@SuppressWarnings("unused")
	private static final String KEEP_ALIVE = "Keep-Alive";
	private static final String ETAG = "ETag";
	// CHECKSTYLE:ON

	private static final Logger LOG = LoggerFactory
			.getLogger(ResourceServlet.class);

	private final HttpContext httpContext;
	private final String contextName;
	private final String alias;
	private final String name;
	private final MimeTypes mimeTypes = new MimeTypes();

	private String[] welcomes;

	ResourceServlet(final HttpContext httpContext, final String contextName,
					final String alias, final String name) {
		this.httpContext = httpContext;
		this.contextName = "/" + contextName;
		this.alias = alias;
		if ("/".equals(name)) {
			this.name = "";
		} else {
			this.name = name;
		}

	}

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ContextHandler contextHandler = initContextHandler(servletContext);
		welcomes = contextHandler.getWelcomeFiles();
		if (welcomes == null) {
			welcomes = new String[]{"index.html", "index.jsp"};
		}
	}

	/**
	 * Compute the field _contextHandler.<br/>
	 * In the case where the DefaultServlet is deployed on the HttpService it is
	 * likely that this method needs to be overwritten to unwrap the
	 * ServletContext facade until we reach the original jetty's ContextHandler.
	 *
	 * @param servletContext The servletContext of this servlet.
	 * @return the jetty's ContextHandler for this servletContext.
	 */
	protected ContextHandler initContextHandler(ServletContext servletContext) {
		ContextHandler.Context scontext = ContextHandler.getCurrentContext();
		if (scontext == null) {
			if (servletContext instanceof ContextHandler.Context) {
				return ((ContextHandler.Context) servletContext)
						.getContextHandler();
			} else {
				throw new IllegalArgumentException("The servletContext "
						+ servletContext + " "
						+ servletContext.getClass().getName() + " is not "
						+ ContextHandler.Context.class.getName());
			}
		} else {
			return ContextHandler.getCurrentContext().getContextHandler();
		}
	}
	
	

	@SuppressWarnings("deprecation")
	@Override
	protected void service(final HttpServletRequest request,
						 final HttpServletResponse response) throws ServletException,
			IOException {
		if (response.isCommitted()) {
			return;
		}

		String mapping;
		Boolean included = request
				.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
		String pathInfo = null;

		if (included != null && included) {
			String servletPath = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			pathInfo = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
			mapping = URIUtil.addPaths(servletPath, pathInfo);
		} else {
			included = Boolean.FALSE;
			// getRequestURI will return full path with context name
			mapping = request.getRequestURI();
			if (!"/".equals(contextName)) {
				mapping = mapping.substring(contextName.length());
			}
			if (!"/".equals(alias)) {
				mapping = mapping.substring(alias.length());
			}
			if (!name.isEmpty() && !"default".equals(name)) {
				mapping = name + mapping;
			}
		}

		// String pathInContext = URIUtil.addPaths(mapping,pathInfo);
		boolean endsWithSlash = (mapping == null ? request.getServletPath()
				: mapping).endsWith(URIUtil.SLASH);

		final URL url = httpContext.getResource(mapping);
		if (url == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Resource resource;
		try {
			// For Performance improvements turn caching on
			resource = ResourceEx.newResource(url, true);
		} catch (IOException e) {
			log("failed to retrieve Resource for URL:" + url, e);
			resource = null;
		}

		try {

			if ((resource == null || (!resource.exists()) && getWelcomeFile(mapping) == null)) {
				if (!response.isCommitted()) {
					if (mapping.equals("/")) {
						// root directory listing, but no "dir entry" from bundle
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
				return;
			}

			// let's check if this is maybe a directory. org.osgi.framework.Bundle.getResource()
			// returns proper URL for directory entry and we can't tell if it's a directory or not
			boolean possibleDirectoryBundleEntry = !resource.exists();
			if (resource.exists()) {
				try (InputStream peek = resource.getInputStream()) {
					possibleDirectoryBundleEntry = peek.available() == 0;
				}
			}
			String welcome = possibleDirectoryBundleEntry ? getWelcomeFile(mapping) : null;
			boolean redirect = false;

			// else look for a welcome file
			if (null != welcome) {
				LOG.debug("welcome={}", welcome);
				// Forward to the index
				if (redirect) {
					response.sendRedirect(welcome);
					return;
				} else {
					if (!mapping.endsWith("/")) {
						// we found welcome file by _accident_ because org.osgi.framework.Bundle.getResource()
						// doesn't distinguish between directories and files.
						// we have to alter the welcome path to correctly handle relative locations
						welcome = mapping + "/" + welcome;
					}
					RequestDispatcher dispatcher = request
							.getRequestDispatcher(welcome);
					if (dispatcher != null) {
						if (included) {
							dispatcher.include(request, response);
							return;
						} else {
							// only used as marker in org.eclipse.jetty.security.SecurityHandler.checkSecurity()
							request.setAttribute(
									"org.eclipse.jetty.server.welcome", welcome);
							dispatcher.forward(request, response);
							return;
						}
					}
				}
			} else if (resource != null && resource.isDirectory()) {
				// directory listing
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			// if the request contains an etag and its the same for the
			// resource, we deliver a NOT MODIFIED response
			String eTag = String.valueOf(resource.lastModified());
			if ((request.getHeader(IF_NONE_MATCH) != null)
					&& (eTag.equals(request.getHeader(IF_NONE_MATCH)))) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			} else if (request.getHeader(IF_MODIFIED_SINCE) != null) {
				long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
				if (resource.lastModified() != -1) {
					// resource.lastModified()/1000 <= ifmsl/1000
					if (resource.lastModified() / SECOND <= ifModifiedSince / SECOND) {
						response.reset();
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.flushBuffer();
						return;
					}
				}
			} else if (request.getHeader(IF_UNMODIFIED_SINCE) != null) {
				long modifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);

				if (modifiedSince != -1) {
					if (resource.lastModified() / SECOND > modifiedSince / SECOND) {
						response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
						return;
					}
				}
			}

			// set the etag
			response.setHeader(ETAG, eTag);
			String mimeType = httpContext.getMimeType(mapping);
			if (mimeType == null) {
				mimeType = mimeTypes.getMimeByExtension(mapping);
			}

			if (mimeType == null) {
				try {
					mimeType = url.openConnection().getContentType();
				} catch (IOException ignore) {
					// we do not care about such an exception as the fact that
					// we are using also the connection for
					// finding the mime type is just a "nice to have" not an
					// requirement
				}
			}

			if (mimeType == null) {
				ServletContext servletContext = getServletConfig()
						.getServletContext();
				mimeType = servletContext.getMimeType(mapping);
			}

			if (mimeType != null) {
				response.setContentType(mimeType);
			}

			OutputStream out = response.getOutputStream();
			if (out != null) { // null should be just in unit testing
				if (out instanceof HttpOutput) {
					((HttpOutput) out).sendContent(resource.getInputStream());
				} else {
					// Write content normally
					resource.writeTo(out, 0, resource.length());
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} finally {
			resource.release();
		}
	}

	/**
	 * Finds a matching welcome file for the supplied {@link Resource}. This
	 * will be the first entry in the list of configured {@link #_welcomes
	 * welcome files} that existing within the directory referenced by the
	 * <code>Resource</code>. If the resource is not a directory, or no matching
	 * file is found, then it may look for a valid servlet mapping. If there is
	 * none, then <code>null</code> is returned. The list of welcome files is
	 * read from the {@link ContextHandler} for this servlet, or
	 * <code>"index.jsp" , "index.html"</code> if that is <code>null</code>.
	 *
	 * @param resource
	 * @return The path of the matching welcome file in context or null.
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private String getWelcomeFile(String pathInContext)
			throws MalformedURLException, IOException {
		if (welcomes == null) {
			return null;
		}

		for (int i = 0; i < welcomes.length; i++) {
			String welcomeInContext = URIUtil.addPaths(pathInContext,
					welcomes[i]);
			final URL url = httpContext.getResource(welcomeInContext);
			final Resource welcome = ResourceEx.newResource(url, true);
			if (welcome != null && welcome.exists()) {
				return welcomes[i];
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("context=").append(contextName)
				.append(",alias=").append(alias).append(",name=").append(name)
				.append("}").toString();
	}

	public abstract static class ResourceEx extends Resource {

		private static final Method METHOD;

		static {
			Method mth = null;
			try {
				mth = Resource.class.getDeclaredMethod("newResource",
						URL.class, boolean.class);
				mth.setAccessible(true);
				// CHECKSTYLE:OFF
			} catch (Throwable t) {
				// Ignore
			}
			// CHECKSTYLE:ON
			METHOD = mth;
		}

		public static Resource newResource(URL url, boolean useCaches)
				throws IOException {
			try {
				return (Resource) METHOD.invoke(null, url, useCaches);
				// CHECKSTYLE:OFF
			} catch (Throwable t) {
				return Resource.newResource(url);
			}
			// CHECKSTYLE:ON
		}
	}

	@Override
	public Resource getResource(String path) {
		final URL url = httpContext.getResource(path);
		if (url == null) {
			return null;
		}

		try {
			// For Performance improvements turn caching on
			return ResourceEx.newResource(url, true);
		} catch (IOException e) {
			log("failed to retrieve Resource for URL:" + url, e);
			return null;
		}
	}

}
