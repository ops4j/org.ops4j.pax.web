/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.ops4j.pax.web.service.whiteboard.ServletMapping;

/**
 * Default implementation of {@link ServletMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultServletMapping implements ServletMapping {

	/**
	 * Http Context id.
	 */
	private String httpContextId;
	/**
	 * Servlet.
	 */
	private Servlet servlet;

	/**
	 * Servlet Name.
	 */
	private String servletName;

	/**
	 * Alias.
	 */
	private String alias;
	/**
	 * Url patterns.
	 */
	private String[] urlPatterns;
	/**
	 * Initialization parameters.
	 */
	private Map<String, String> initParams;

	private Integer loadOnStartup;

	private Boolean asyncSupported;

	private MultipartConfigElement multipartConfig;

	private String[] errorPageParams;

	/**
	 * @see ServletMapping#getHttpContextId()
	 */
	public String getHttpContextId() {
		return httpContextId;
	}

	/**
	 * @see ServletMapping#getServlet()
	 */
	public Servlet getServlet() {
		return servlet;
	}

	/**
	 * @see ServletMapping#getServletName()
	 */
	public String getServletName() {
		return servletName;
	}

	/**
	 * @see ServletMapping#getAlias()
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @see ServletMapping#getUrlPatterns()
	 */
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	/**
	 * @see ServletMapping#getInitParams()
	 */
	public Map<String, String> getInitParams() {
		return initParams;
	}

	/**
	 * Setter.
	 *
	 * @param httpContextId id of the http context this servlet belongs to
	 */
	public void setHttpContextId(final String httpContextId) {
		this.httpContextId = httpContextId;
	}

	/**
	 * Setter.
	 *
	 * @param servlet mapped servlet
	 */
	public void setServlet(final Servlet servlet) {
		this.servlet = servlet;
	}

	/**
	 * Setter.
	 *
	 * @param servletName of the Servlet being mapped.
	 */
	public void setServletName(final String servletName) {
		this.servletName = servletName;
	}

	/**
	 * Setter.
	 *
	 * @param alias alias this servlet maps to
	 */
	public void setAlias(final String alias) {
		this.alias = alias;
	}

	/**
	 * Setter.
	 *
	 * @param urlPatterns array of url patterns
	 */
	public void setUrlPatterns(final String... urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	/**
	 * Seter.
	 *
	 * @param initParams map of initialization parameters
	 */
	public void setInitParams(final Map<String, String> initParams) {
		this.initParams = initParams;
	}

	/**
	 * @return the loadOnStartup
	 */
	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	/**
	 * @param loadOnStartup the loadOnStartup to set
	 */
	public void setLoadOnStartup(Integer loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	/**
	 * @return the asyncSupported
	 */
	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	/**
	 * @param asyncSupported the asyncSupported to set
	 */
	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	/**
	 * @return the multipartConfig
	 */
	public MultipartConfigElement getMultipartConfig() {
		return multipartConfig;
	}

	/**
	 * @param multipartConfig the multipartConfig to set
	 */
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		this.multipartConfig = multipartConfig;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() +
				"{" + "httpContextId=" + httpContextId +
				",urlPatterns=" + Arrays.deepToString(urlPatterns) +
				",initParams=" + initParams +
				",servlet=" + servlet + ", alias=" + alias +
				", servletName=" + servletName + "}";
	}

				@Override
				public Class<? extends Servlet> getServletClass() {
					return null;
				}

				@Override
				public String[] getErrorPages() {
					return new String[0];
				}

				@Override
				public String getContextSelectFilter() {
					return null;
				}

				@Override
				public String getContextId() {
					return null;
				}

}