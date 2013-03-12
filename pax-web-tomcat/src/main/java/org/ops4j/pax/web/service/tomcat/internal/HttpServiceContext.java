/**
 * 
 */
package org.ops4j.pax.web.service.tomcat.internal;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletSecurityElement;

import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.SecurityConstraint;
import org.osgi.service.http.HttpContext;

/**
 * @author achim
 * 
 */
public class HttpServiceContext extends StandardContext {

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

	private HttpContext httpContext;
	private final CtxtTomcatInterceptFilter ctxtInterceptFilter;

	/**
	 * 
	 */
	public HttpServiceContext() {
		// TODO add a filtermapping with a new TomcatInterceptFilter here
		ctxtInterceptFilter = new CtxtTomcatInterceptFilter();

		// need to take care of adding to "root" context.
		// filterRegistration.addMappingForServletNames( getDispatcherTypes(
		// filterModel ), /*TODO get asynch supported?*/ false,
		// filterModel.getUrlPatterns() );

		// filterRegistration.setInitParameters( filterModel.getInitParams() );

	}

	public void setHttpContext(HttpContext httpContext) {
		this.httpContext = httpContext;
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
	public void addFilterMap(FilterMap filterMap) {
		// TODO Auto-generated method stub
		super.addFilterMap(filterMap);
	}

	@Override
	public void addFilterMapBefore(FilterMap filterMap) {
		// TODO Auto-generated method stub
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

}
