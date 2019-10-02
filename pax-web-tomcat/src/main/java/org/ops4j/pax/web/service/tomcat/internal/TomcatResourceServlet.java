/*
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
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.connector.ResponseFacade;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * can be based on org.apache.catalina.servlets.DefaultServlet
 *
 * @author Romain Gilles Date: 7/26/12 Time: 10:41 AM
 */
public class TomcatResourceServlet extends HttpServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final int SECOND = 1000;

	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatResourceServlet.class);

	// header constants
	private static final String IF_NONE_MATCH = "If-None-Match";
	private static final String IF_MATCH = "If-Match";
	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	private static final String IF_RANGE = "If-Range";
	private static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	private static final String KEEP_ALIVE = "Keep-Alive";

	private static final String ETAG = "ETag";

	/**
	 * The input buffer size to use when serving resources.
	 */
	protected int input = 2048;

	private final HttpContext httpContext;
	private final String contextName;
	private final String alias;
	private final String name;
	private final Context context;
	private String[] welcomes;

	public TomcatResourceServlet(final HttpContext httpContext,
								 final String contextName, final String alias, final String name,
								 final Context context) {
		this.httpContext = httpContext;
		this.contextName = "/" + contextName;
		this.alias = alias;
		if ("/".equals(name)) {
			this.name = "";
		} else {
			this.name = name;
		}
		this.context = context;
	}

	@Override
	public void init() throws ServletException {
	    welcomes = context.findWelcomeFiles();
	    if (welcomes == null) {
	        welcomes = new String[]{"index.html", "index.jsp"};
	    }
	}

	@Override
	protected void doGet(HttpServletRequest request,
						 HttpServletResponse response) throws ServletException, IOException {
		if (response.isCommitted()) {
			return;
		}

		String mapping = null;
		Boolean included = request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null;
		String pathInfo = null;
		if (included) {
			String servletPath = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			pathInfo = (String) request
					.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (servletPath == null) {
				servletPath = request.getServletPath();
				pathInfo = request.getPathInfo();
			}
			mapping = addPaths(servletPath, pathInfo);
		} else {
			included = Boolean.FALSE;
			// getRequestURI will return full path with context name
			mapping = request.getRequestURI();
			if (!"/".equals(contextName) && mapping.startsWith(contextName)) {
				mapping = mapping.substring(contextName.length());
			}
			if (!"/".equals(alias) && mapping.startsWith(alias)) {
				mapping = mapping.substring(alias.length());
			}
			if (!name.isEmpty() && !"default".equals(name)) {
				mapping = name + mapping;
			}
		}

	    boolean endsWithSlash = (mapping == null ? request.getServletPath()
	                : mapping).endsWith("/");

		final URL url = httpContext.getResource(mapping);

		if (url == null || "//".equals(mapping) && "bundleentry".equalsIgnoreCase(url.getProtocol())
				|| "/".equals(mapping) && "bundleentry".equalsIgnoreCase(url.getProtocol())) {
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			return;
		}
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		URLConnection connection = null;
		try {
			boolean foundResource;
			try {
				// new Resource(url.openStream());
				connection = url.openConnection();
				connection.connect();
				foundResource = true;
			} catch (IOException ioex) {
				foundResource = false;
			}

			if (!foundResource && !endsWithSlash) {
				if (!response.isCommitted()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				}
				return;
			}

			// let's check if this is maybe a directory. org.osgi.framework.Bundle.getResource()
			// returns proper URL for directory entry and we can't tell if it's a directory or not
			boolean possibleDirectoryBundleEntry = false;
			if (foundResource) {
				try (InputStream peek = url.openStream()) {
					possibleDirectoryBundleEntry = peek.available() == 0;
				}
			}

			String welcome = possibleDirectoryBundleEntry || endsWithSlash ? getWelcomeFile(mapping) : null;

			// else look for a welcome file
			if (null != welcome) {
				LOG.debug("welcome={}", welcome);
				// Forward to the index
				RequestDispatcher dispatcher = request.getRequestDispatcher(welcome);
				if (dispatcher != null) {
					if (included) {
						dispatcher.include(request, response);
						return;
					} else {
						dispatcher.forward(request, response);
						return;
					}
				}
			} else if (!foundResource) {
				// still not found anything, then do the following ...
				if (!response.isCommitted()) {
					if (endsWithSlash) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					} else {
						response.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
				return;
			} else if (foundResource && url.getPath().endsWith("/")) {
				// directory listing
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			// if the request contains an etag and its the same for the
			// resource, we deliver a NOT MODIFIED response
			String eTag = String.valueOf(connection.getLastModified());
			if ((request.getHeader(IF_NONE_MATCH) != null) && (eTag.equals(request.getHeader(IF_NONE_MATCH)))) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			} else if (request.getHeader(IF_MODIFIED_SINCE) != null) {
				long ifModifiedSince = request.getDateHeader(IF_MODIFIED_SINCE);
				if (connection.getLastModified() > 0) {
					// resource.lastModified()/1000 <= ifmsl/1000
					if (connection.getLastModified() / SECOND <= ifModifiedSince / SECOND) {
						response.reset();
						response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
						response.flushBuffer();
						return;
					}
				}
			} else if (request.getHeader(IF_UNMODIFIED_SINCE) != null) {
				long modifiedSince = request.getDateHeader(IF_UNMODIFIED_SINCE);

				if (modifiedSince != -1) {
					if (connection.getLastModified() / SECOND > modifiedSince / SECOND) {
						response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
						return;
					}
				}
			}

			// set the etag
			response.setHeader(ETAG, eTag);

			// String mimeType = m_httpContext.getMimeType(mapping);
			String mimeType = getServletContext().getMimeType(url.getFile());
			/*
			 * No Fallback if (mimeType == null) { Buffer mimeTypeBuf =
			 * mimeTypes.getMimeByExtension(mapping); mimeType = mimeTypeBuf !=
			 * null ? mimeTypeBuf.toString() : null; }
			 */

			if (mimeType == null) {
				try {
					if (url.openConnection() != null) {
						mimeType = url.openConnection().getContentType();
					}
				} catch (IOException | NullPointerException ignore) {
					// we do not care about such an exception as the fact that
					// we are using also the connection for
					// finding the mime type is just a "nice to have" not an
					// requirement
				}
			}

			if (mimeType == null) {
				ServletContext servletContext = getServletConfig().getServletContext();
				mimeType = servletContext.getMimeType(mapping);
			}

			if (mimeType != null) {
				response.setContentType(mimeType);
			}

			ServletOutputStream out = response.getOutputStream();
			if (out != null) { // null should be just in unit testing
				ServletResponse r = response;
				while (r instanceof ServletResponseWrapper) {
					r = ((ServletResponseWrapper) r).getResponse();
				}
				if (r instanceof ResponseFacade) {
					((ResponseFacade) r).getContentWritten();
				}

				IOException ioException = copyRange(url.openStream(), out);

				if (ioException != null) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				}
			}
		} finally {
			if (connection != null) {
				try {
					connection.getInputStream().close();
				} catch (IOException e) {
					// ignore
				}
			}
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
	private String getWelcomeFile(String pathInContext) throws MalformedURLException, IOException {

		if (welcomes == null) {
			return null;
		}

		if (!pathInContext.endsWith("/")) {
			pathInContext = pathInContext + "/";
		}
		if (!"default".equals(name) && pathInContext.startsWith(name)) {
			// in Tomcat, welcome-files registered in HttpService directly, with non-default resource servlet
		}
		for (int i = 0; i < welcomes.length; i++) {
			if (httpContext.getResource(pathInContext + welcomes[i]) != null) {
				return pathInContext + welcomes[i];
			}
		}
		return null;
	}

	private static String addPaths(String p1, String p2) {
		if (p1 == null || p1.length() == 0) {
			if (p1 != null && p2 == null) {
				return p1;
			}
			return p2;
		}
		if (p2 == null || p2.length() == 0) {
			return p1;
		}
		int split = p1.indexOf(';');
		if (split < 0) {
			split = p1.indexOf('?');
		}
		if (split == 0) {
			return p2 + p1;
		}
		if (split < 0) {
			split = p1.length();
		}
		StringBuilder buf = new StringBuilder(p1.length() + p2.length() + 2);
		buf.append(p1);
		if (buf.charAt(split - 1) == '/') {
			if (p2.startsWith("/")) {
				buf.deleteCharAt(split - 1);
				buf.insert(split - 1, p2);
			} else {
				buf.insert(split, p2);
			}
		} else {
			if (p2.startsWith("/")) {
				buf.insert(split, p2);
			} else {
				buf.insert(split, '/');
				buf.insert(split + 1, p2);
			}
		}
		return buf.toString();
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream.
	 *
	 * @param istream The input stream to read from
	 * @param ostream The output stream to write to
	 * @return Exception which occurred during processing
	 */
	protected IOException copyRange(InputStream istream,
									ServletOutputStream ostream) {

		// first check if the istream is valid
		if (istream == null) {
			return new IOException("Incoming stream is null");
		}

		// Copy the input stream to the output stream
		IOException exception = null;
		byte buffer[] = new byte[input];
		int len = buffer.length;
		while (true) {
			try {
				len = istream.read(buffer);
				if (len == -1) {
					break;
				}
				ostream.write(buffer, 0, len);
			} catch (IOException e) {
				exception = e;
				len = -1;
				break;
			}
		}
		return exception;

	}
}
