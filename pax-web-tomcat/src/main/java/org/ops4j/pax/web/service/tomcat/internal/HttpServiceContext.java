/**
 * 
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletSecurityElement;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Valve;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.naming.resources.DirContextURLStreamHandler;
import org.apache.tomcat.util.http.RequestUtil;
import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
public class HttpServiceContext extends StandardContext {
	
	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceContext.class);

	private class CtxtTomcatInterceptFilter extends TomcatInterceptFilter {

		private Map<String, Servlet> servletMap = new HashMap<String, Servlet>();
		private Map<String, Filter> filterMap = new HashMap<String, Filter>();

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			// TODO Auto-generated method stub

		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub

		}

		@Override
		protected URL getMostSpecificResourceURLForPath(String pathInfo) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Servlet getMostSpecificServletForPath(String pathInfo) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected List<Filter> getFiltersRegisteredForPath(String pathInfo) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void copy(InputStream inputStream,
				ServletOutputStream outputstream) {
			// TODO Auto-generated method stub

		}

	}
	
	public class ServletApplicationContext extends ApplicationContext {

		public ServletApplicationContext(StandardContext context) {
			super(context);
			// TODO Auto-generated constructor stub
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
			if (path != null && path.endsWith("/") && path.length() > 1) {
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
			} catch (PrivilegedActionException e) {
				LOG.warn("Unauthorized access: " + e.getMessage());
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
		@SuppressWarnings("unchecked")
		// Cannot remove this warning as it is an issue with the
		// javax.servlet.ServletContext interface
		@Override
		public Set<String> getResourcePaths(final String path) {
			if (httpContext instanceof WebContainerContext) {
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
					final Set<String> slashedPaths = new HashSet<String>();
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
			} else {
				return super.getResourcePaths(path);
			}
		}

		@Override
		public String getMimeType(final String name) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting mime type for: [" + name + "]");
			}
			// Check the OSGi HttpContext
			String mime = httpContext.getMimeType(name);
			if (mime != null) {
				return mime;
			}

			// Delegate to the parent class (the Jetty
			// ServletContextHandler.Context)
			return super.getMimeType(name);
		}
		
	}

	private HttpContext httpContext;
	private final CtxtTomcatInterceptFilter ctxtInterceptFilter;
	private Host host;
	private Valve serviceValve = new ServiceValve();
	
	/**
	 * Access controller context of the bundle that registred the http context.
	 */
	private final AccessControlContext accessControllerContext;

	/**
	 * @param host 
	 * 
	 */
	public HttpServiceContext(Host host, AccessControlContext accessControllerContext) {
		// TODO add a filtermapping with a new TomcatInterceptFilter here
		ctxtInterceptFilter = new CtxtTomcatInterceptFilter();

		// need to take care of adding to "root" context.
		// filterRegistration.addMappingForServletNames( getDispatcherTypes(
		// filterModel ), /*TODO get asynch supported?*/ false,
		// filterModel.getUrlPatterns() );
		this.accessControllerContext = accessControllerContext;
		this.host = host;
		
	}

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
		((ServiceValve)serviceValve).setHttpContext(httpContext);
	}

	@Override
	public void addApplicationEventListener(Object listener) {
		// TODO Auto-generated method stub
		super.addApplicationEventListener(listener);

	}

	@Override
	public void addApplicationLifecycleListener(Object listener) {
		// TODO Auto-generated method stub
		super.addApplicationLifecycleListener(listener);
	}

	@Override
	public void addApplicationListener(String listener) {
		// TODO Auto-generated method stub
		super.addApplicationListener(listener);
	}

	@Override
	public void addApplicationParameter(ApplicationParameter parameter) {
		// TODO Auto-generated method stub
		super.addApplicationParameter(parameter);
	}

	@Override
	public void addConstraint(SecurityConstraint constraint) {
		// TODO Auto-generated method stub
		super.addConstraint(constraint);
	}

	@Override
	public void addContainerListener(ContainerListener listener) {
		// TODO Auto-generated method stub
		super.addContainerListener(listener);
	}

	@Override
	public void addErrorPage(ErrorPage errorPage) {
		// TODO Auto-generated method stub
		super.addErrorPage(errorPage);
	}

	@Override
	public void addFilterDef(FilterDef filterDef) {
		// TODO Auto-generated method stub
		super.addFilterDef(filterDef);
	}

	@Override
	public void addFilterMap(final FilterMap filterMap) {
		super.addFilterMap(filterMap);
	}

	@Override
	public void addFilterMapBefore(final FilterMap filterMap) {
		super.addFilterMapBefore(filterMap);
	}

	@Override
	public void addServletContainerInitializer(ServletContainerInitializer sci,
			Set<Class<?>> classes) {
		// TODO Auto-generated method stub
		super.addServletContainerInitializer(sci, classes);
	}

	@Override
	public void addServletMapping(String pattern, String name) {
		// TODO Auto-generated method stub
		super.addServletMapping(pattern, name);
	}

	@Override
	public void addServletMapping(String pattern, String name,
			boolean jspWildCard) {
		// TODO Auto-generated method stub
		super.addServletMapping(pattern, name, jspWildCard);
	}

	@Override
	public Set<String> addServletSecurity(
			ApplicationServletRegistration registration,
			ServletSecurityElement servletSecurityElement) {
		// TODO Auto-generated method stub
		return super.addServletSecurity(registration, servletSecurityElement);
	}

	@Override
	public void addWelcomeFile(String name) {
		// TODO Auto-generated method stub
		super.addWelcomeFile(name);
	}

	@Override
	public void addChild(Container child) {
		// TODO Auto-generated method stub
		super.addChild(child);
	}
	
	@Override
	public ServletContext getServletContext() {
		if (context == null) {
            context = new ServletApplicationContext(this);
            if (getAltDDName() != null)
                context.setAttribute(Globals.ALT_DD_ATTR,getAltDDName());
        }
        return super.getServletContext();
	}
	
	@Override
	protected synchronized void setState(LifecycleState state)
			throws LifecycleException {
		super.setState(state);
	}

}
