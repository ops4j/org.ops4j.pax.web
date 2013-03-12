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
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.regex.Matcher;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.server.AbstractHttpConnection;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;
import org.osgi.service.http.HttpContext;

class ResourceServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// header constants
	private static final String IF_NONE_MATCH = "If-None-Match";
	private static final String IF_MATCH = "If-Match";
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	private static final String IF_RANGE = "If-Range";
	private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	private static final String KEEP_ALIVE = "Keep-Alive";

	private static final String ETAG = "ETag";

	private final HttpContext httpContext;
	private final String contextName;
	private final String alias;
	private final String name;
	private final MimeTypes mimeTypes = new MimeTypes();

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
	protected void doGet(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException,
			IOException {
		String mapping;
		Boolean included = request
				.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
		if (included != null && included) {
			String servletPath = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			String pathInfo = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
			mapping = URIUtil.addPaths(servletPath, pathInfo);
		} else {
			included = Boolean.FALSE;
			if (contextName.equals(alias)) {
				// special handling since resouceServlet has default name
				// attached to it
				if (!"default".equalsIgnoreCase(name)) {
					mapping = name + request.getRequestURI();
				} else {
					mapping = request.getRequestURI();
				}
			} else {
				mapping = request.getRequestURI().replaceFirst(contextName,
						"/");
				if (!"default".equalsIgnoreCase(name)) {
					mapping = mapping.replaceFirst(alias,
							Matcher.quoteReplacement(name)); // TODO
				}
			}
		}

		final URL url = httpContext.getResource(mapping);
		if (url == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// For Performanceimprovements turn caching on
		final Resource resource = ResourceEx.newResource(url, true);
		try {
			if (!resource.exists()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if (resource.isDirectory()) {
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
					if (resource.lastModified() / 1000 <= ifModifiedSince / 1000) {
						response.reset();
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.flushBuffer();
						return;
					}
				}
			} else if (request.getHeader(IF_UNMODIFIED_SINCE) != null) {
				long modifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);

				if (modifiedSince != -1) {
					if (resource.lastModified() / 1000 > modifiedSince / 1000) {
						response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
						return;
					}
				}
			}

			// set the etag
			response.setHeader(ETAG, eTag);
			String mimeType = httpContext.getMimeType(mapping);
			if (mimeType == null) {
				Buffer mimeTypeBuf = mimeTypes.getMimeByExtension(mapping);
				mimeType = mimeTypeBuf != null ? mimeTypeBuf.toString() : null;
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
				// TODO shall we handle also content encoding?
			}

			OutputStream out = response.getOutputStream();
			if (out != null) { // null should be just in unit testing
				if (out instanceof AbstractHttpConnection.Output) {
					((AbstractHttpConnection.Output) out).sendContent(resource
							.getInputStream());
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

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("context=").append(contextName)
				.append(",alias=").append(alias).append(",name=")
				.append(name).append("}").toString();
	}

	public abstract static class ResourceEx extends Resource {

		// CHECKSTYLE:SKIP
		private static final Method method;

		static {
			Method mth = null;
			try {
				mth = Resource.class.getDeclaredMethod("newResource",
						URL.class, boolean.class);
				mth.setAccessible(true);
			} catch (Throwable t) {// CHECKSTYLE:SKIP
				// Ignore
			}
			method = mth;
		}

		public static Resource newResource(URL url, boolean useCaches)
				throws IOException {
			try {
				return (Resource) method.invoke(null, url, useCaches);
			} catch (Throwable t) {// CHECKSTYLE:SKIP
				return Resource.newResource(url);
			}
		}
	}

}
