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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;

import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationFilterRegistration;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.ops4j.pax.web.service.WebContainerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 */
public class HttpServiceContext extends StandardContext {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceContext.class);

	public class HttpServiceSessionCookieConfig implements SessionCookieConfig {
		private boolean httpOnly;
		private boolean secure;
		private int maxAge = -1;
		private String comment;
		private String domain;
		private String name;
		private String path;
		private StandardContext context;

		@Override
		public boolean isHttpOnly() {
			return httpOnly;
		}

		@Override
		public void setHttpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		@Override
		public boolean isSecure() {
			return secure;
		}

		@Override
		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		@Override
		public int getMaxAge() {
			return maxAge;
		}

		@Override
		public void setMaxAge(int maxAge) {
			this.maxAge = maxAge;
		}

		@Override
		public String getComment() {
			return comment;
		}

		@Override
		public void setComment(String comment) {
			this.comment = comment;
		}

		@Override
		public String getDomain() {
			return domain;
		}

		@Override
		public void setDomain(String domain) {
			this.domain = domain;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public void setPath(String path) {
			this.path = path;
		}

		public StandardContext getContext() {
			return context;
		}

		public void setContext(StandardContext context) {
			this.context = context;
		}
	}

	public class ServletApplicationContext extends ApplicationContext {

		// we have to allow more flexible configuration wrt lifecycle
		private SessionCookieConfig sessionCookieConfig;

		public ServletApplicationContext(StandardContext context) {
			super(context);
		}

	    @Override
		public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
			FilterRegistration.Dynamic fr = super.addFilter(filterName, filter);
			// Workaround for duplicate WsFilter registration after bundle restart
			if (fr == null) {
				FilterDef filterDef = getContext().findFilterDef(filterName);
				if (filterDef != null) {
					fr = new ApplicationFilterRegistration(filterDef, getContext());
				}
			}
			return fr;
		}

		@Override
		public String getRealPath(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting real path: [{}]", path);
			}

			URL resource = getResource(path);
			if (resource != null) {
				String protocol = resource.getProtocol();
				if (protocol.equals("file")) {
					String fileName = resource.getFile();
					if (fileName != null) {
						File file = new File(fileName);
						if (file.exists()) {
							String realPath = file.getAbsolutePath();
							LOG.debug("found real path: [{}]", realPath);
							return realPath;
						}
					}
				}
			}
			return null;
		}

		@Override
		public URL getResource(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting resource: [" + path + "]");
			}
			URL resource = null;

			// IMPROVEMENT start PAXWEB-314
			try {
				resource = new URL(path);
				LOG.debug("resource: [" + path
						+ "] is already a URL, returning");
				return resource;
			} catch (MalformedURLException e) {
				// do nothing, simply log
				LOG.debug("not a URL or invalid URL: [" + path
						+ "], treating as a file path");
			}
			// IMPROVEMENT end PAXWEB-314

			// FIX start PAXWEB-233
			final String p;
			if (path.endsWith("/") && path.length() > 1) {
				p = path.substring(0, path.length() - 1);
			} else {
				p = path;
			}
			// FIX end

			try {
				resource = AccessController.doPrivileged(
						new PrivilegedExceptionAction<URL>() {
							@Override
							public URL run() throws Exception {
								return httpContext.getResource(p);
							}
						}, accessControllerContext);
				if (LOG.isDebugEnabled()) {
					LOG.debug("found resource: " + resource);
				}
				if (resource != null) {
					return resource;
				}
			} catch (PrivilegedActionException e) {
				LOG.warn("Unauthorized access: " + e.getMessage());
			}

			// the HttpServiceContext might contain resources
	        WebResourceRoot resources = getResources();
	        if (resources != null) {
	            resource = resources.getResource(path).getURL();
				if (LOG.isDebugEnabled()) {
					LOG.debug("found resource: " + resource);
				}
	        }

			return resource;

		}

		@Override
		public InputStream getResourceAsStream(final String path) {
			final URL url = getResource(path);
			if (url != null) {
				try {
					return AccessController.doPrivileged(
							new PrivilegedExceptionAction<InputStream>() {
								@Override
								public InputStream run() throws Exception {
									try {
										return url.openStream();
									} catch (IOException e) {
										LOG.warn("URL canot be accessed: "
												+ e.getMessage());
									}
									return null;
								}

							}, accessControllerContext);
				} catch (PrivilegedActionException e) {
					LOG.warn("Unauthorized access: " + e.getMessage());
				}

			}
			return null;
		}

		/**
		 * Delegate to http context in case that the http context is an
		 * {@link WebContainerContext}. {@inheritDoc}
		 */
		@Override
		public Set<String> getResourcePaths(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting resource paths for : [" + path + "]");
			}
			try {
				final Set<String> paths = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Set<String>>() {
							@Override
							public Set<String> run() throws Exception {
								return ((WebContainerContext) httpContext)
										.getResourcePaths(path);
							}
						}, accessControllerContext);
				if (paths == null) {
					return null;
				}
				// Servlet specs mandates that the paths must start with an
				// slash "/"
				final Set<String> slashedPaths = new HashSet<>();
				for (String foundPath : paths) {
					if (foundPath != null) {
						if (foundPath.trim().startsWith("/")) {
							slashedPaths.add(foundPath.trim());
						} else {
							slashedPaths.add("/" + foundPath.trim());
						}
					}
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("found resource paths: " + paths);
				}
				return slashedPaths;
			} catch (PrivilegedActionException e) {
				LOG.warn("Unauthorized access: " + e.getMessage());
				return null;
			}
		}

		@Override
		public SessionCookieConfig getSessionCookieConfig() {
			return sessionCookieConfig;
		}

	    @Override
		public void setAttribute(String name, Object value) {
			super.setAttribute(name, value);
//			if (Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE.equals(name) && value instanceof WsServerContainer) {
//				sc = (WsServerContainer) value;
//			}
		}

	    public void setSessionCookieConfig(SessionCookieConfig sessionCookieConfig) {
			this.sessionCookieConfig = sessionCookieConfig;
		}
	}

	private WebContainerContext httpContext;

	/**
	 * Access controller context of the bundle that registred the http context.
	 */
	private final AccessControlContext accessControllerContext;

	private Map<String, Object> contextAttributes = Collections.emptyMap();

//    private WsServerContainer sc;

	/**
	 * @param host
	 */
	public HttpServiceContext(Host host,
							  AccessControlContext accessControllerContext) {
		this.accessControllerContext = accessControllerContext;
	}

	public void setHttpContext(WebContainerContext httpContext) {
		this.httpContext = httpContext;
	}

	public void setContextAttributes(Map<String, Object> contextAttributes) {
		this.contextAttributes = contextAttributes;
	}

	@Override
	public ServletContext getServletContext() {
		if (context == null) {
			context = new ServletApplicationContext(this);
			((ServletApplicationContext) context).setSessionCookieConfig(new HttpServiceSessionCookieConfig());

			if (getAltDDName() != null) {
				context.setAttribute(Globals.ALT_DD_ATTR, getAltDDName());
			}
			// Preserve websocket server container over restart
//			if (sc != null) {
//				context.setAttribute(Constants.SERVER_CONTAINER_SERVLET_CONTEXT_ATTRIBUTE, sc);
//			}
			for (Map.Entry<String, Object> attribute : contextAttributes.entrySet()) {
				context.setAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		return super.getServletContext();
	}

}
