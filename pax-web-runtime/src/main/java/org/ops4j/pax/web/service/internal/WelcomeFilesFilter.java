/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves the welcome files if the request path ends with "/".
 * 
 * @author Alin Dreghiciu
 * @since 0.3.0, January 16, 2008
 */
class WelcomeFilesFilter implements Filter {

	private static final Logger LOG = LoggerFactory
			.getLogger(WelcomeFilesFilter.class);

	/**
	 * Aray of welcome files.
	 */
	private final String[] welcomeFiles;
	/**
	 * True if the client should be rediected to welcome file or false if
	 * forwarded
	 */
	private final boolean redirect;
	/**
	 * Filter config.
	 */
	private FilterConfig filterConfig;

	/**
	 * Creates a welcome files filter.
	 * 
	 * @param welcomeFiles
	 *            array of welcome files
	 * @param redirect
	 *            true if the client should be rediected to welcome file or
	 *            false if forwarded
	 * 
	 * @throws NullArgumentException
	 *             if: welcome files array is null or empty entries in array are
	 *             null or empty entries in array start or end with "/"
	 */
	WelcomeFilesFilter(final String[] welcomeFiles, boolean redirect) {
		NullArgumentException.validateNotNull(welcomeFiles, "Welcome files");
		if (welcomeFiles.length == 0) {
			throw new NullArgumentException("Welcome files is be empty");
		}
		for (String welcomeFile : welcomeFiles) {
			if (welcomeFile == null || welcomeFile.trim().length() == 0) {
				throw new NullArgumentException(
						"Welcome files entry is null or empty");
			}
			if (welcomeFile.startsWith("/")) {
				throw new NullArgumentException("Welcome files entry ["
						+ welcomeFile + "] starts with '/'");
			}
			if (welcomeFile.endsWith("/")) {
				throw new NullArgumentException("Welcome files entry ["
						+ welcomeFile + "] ends with '/'");
			}
		}
		this.welcomeFiles = welcomeFiles;
		this.redirect = redirect;
	}

	/**
	 * Store the filter config.
	 * 
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	/**
	 * Serves the welcome files if request path ends with "/".
	 * 
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(final ServletRequest request,
			final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		LOG.debug("Apply welcome files filter...");
		if (welcomeFiles.length > 0 && request instanceof HttpServletRequest) {
			String servletPath = (((HttpServletRequest) request)
					.getServletPath());
			String pathInfo = ((HttpServletRequest) request).getPathInfo();

			LOG.debug("Servlet path: " + servletPath);
			LOG.debug("Path info: " + pathInfo);

			if ((pathInfo != null && pathInfo.endsWith("/"))
					|| (servletPath != null && servletPath.endsWith("/"))) {
				final ServletContext servletContext = filterConfig
						.getServletContext();
				for (String welcomeFile : welcomeFiles) {
					final String welcomePath = addPaths(servletPath,
							addPaths(pathInfo, welcomeFile));
					final URL welcomeFileUrl = servletContext
							.getResource(welcomePath);
					if (welcomeFileUrl != null) {
						if (redirect && response instanceof HttpServletResponse) {
							((HttpServletResponse) response)
									.sendRedirect(welcomeFile);
							return;
						} else {
							final RequestDispatcher requestDispatcher = request
									.getRequestDispatcher(welcomePath);
							if (requestDispatcher != null) {
								requestDispatcher.forward(request, response);
								return;
							}
						}
					}
				}
			}
		} else {
			if (welcomeFiles.length == 0) {
				LOG.debug("Welcome filter not applied as there are no welcome files configured.");
			}
			if (!(request instanceof HttpServletRequest)) {
				LOG.debug("Welcome filter not applied as the request is not an "
						+ HttpServletRequest.class.getSimpleName());
			}
		}
		// if we are here means that the request was not handled by welcome
		// files filter so, go on
		// ClassLoader cl; //TODO initialize Classloader.
		// try {
		// cl = WelcomeFilesFilter.class.getClassLoader();
		// ContextClassLoaderUtils.doWithClassLoader(cl,
		// new Callable<Void>() {
		//
		// public Void call() {
		// try {
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		// }
		// return null;
		// }
		//
		// });
		// } catch (Exception e) {
		// if (e instanceof RuntimeException) {
		// throw (RuntimeException) e;
		// }
		// LOG.error("Ignored exception during filter execution", e);
		// }
		chain.doFilter(request, response);
	}

	/**
	 * Does nothing.
	 * 
	 * @see javax.servlet.Filter#destroy()
	 */
	@Override
	public void destroy() {
		// does nothing
	}

	private static String addPaths(final String path1, final String path2) {
		if (path1 == null || path1.length() == 0) {
			if (path1 != null && path2 == null) {
				return path1;
			}
			return path2;
		}
		if (path2 == null || path2.length() == 0) {
			return path1;
		}

		int split = path1.indexOf(';');
		if (split < 0) {
			split = path1.indexOf('?');
		}
		if (split == 0) {
			return path2 + path1;
		}
		if (split < 0) {
			split = path1.length();
		}

		StringBuffer buf = new StringBuffer(path1.length() + path2.length() + 2);
		buf.append(path1);

		if (buf.charAt(split - 1) == '/') {
			if (path2.startsWith("/")) {
				buf.deleteCharAt(split - 1);
				buf.insert(split - 1, path2);
			} else {
				buf.insert(split, path2);
			}
		} else {
			if (path2.startsWith("/")) {
				buf.insert(split, path2);
			} else {
				buf.insert(split, '/');
				buf.insert(split + 1, path2);
			}
		}

		return buf.toString();
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("welcomeFiles=")
				.append(Arrays.toString(welcomeFiles)).append("}").toString();
	}
}
