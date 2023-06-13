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
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

import org.ops4j.pax.web.service.whiteboard.ServletMapping;

/**
 * Default implementation of {@link ServletMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultServletMapping extends AbstractContextRelated implements ServletMapping {

	private Servlet servlet;
	private Class<? extends Servlet> servletClass;
	private String servletName;
	private String[] urlPatterns = new String[0];
	private String alias;
	private String[] errorPages = new String[0];
	private Boolean asyncSupported;
	private MultipartConfigElement multipartConfig;
	private Map<String, String> initParameters = new HashMap<>();
	private Integer loadOnStartup;

	@Override
	public Servlet getServlet() {
		return servlet;
	}

	@Override
	public Class<? extends Servlet> getServletClass() {
		return servletClass;
	}

	@Override
	public String getServletName() {
		return servletName;
	}

	@Override
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public String[] getErrorPages() {
		return errorPages;
	}

	@Override
	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	@Override
	public MultipartConfigElement getMultipartConfig() {
		return multipartConfig;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters;
	}

	@Override
	public Integer getLoadOnStartup() {
		return loadOnStartup;
	}

	public void setServlet(Servlet servlet) {
		this.servlet = servlet;
	}

	public void setServletClass(Class<? extends Servlet> servletClass) {
		this.servletClass = servletClass;
	}

	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	public void setUrlPatterns(String[] urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public void setErrorPages(String[] errorPages) {
		this.errorPages = errorPages;
	}

	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		this.multipartConfig = multipartConfig;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	public void setLoadOnStartup(Integer loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	@Override
	public String toString() {
		return "DefaultServletMapping{"
				+ "servlet=" + servlet
				+ ", servletClass=" + servletClass
				+ ", servletName='" + servletName + '\''
				+ ", urlPatterns=" + Arrays.toString(urlPatterns)
				+ ", alias='" + alias + '\''
				+ ", errorPages=" + Arrays.toString(errorPages)
				+ ", asyncSupported=" + asyncSupported
				+ ", multipartConfig=" + multipartConfig
				+ ", initParameters=" + initParameters
				+ ", loadOnStartup=" + loadOnStartup
				+ ", contextSelectFilter='" + contextSelectFilter + '\''
				+ ", contextId='" + contextId + '\''
				+ '}';
	}

}
