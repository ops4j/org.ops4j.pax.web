/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

/**
 * Servlet element in web.xml.
 *
 * @author Alin Dreghiciu
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 * @since 0.3.0, December 27, 2007
 */
public class WebAppServlet {

	/**
	 * Servlet name.
	 */
	private String servletName;
	/**
	 * Servlet class name.
	 */
	private String servletClassName;
	/**
	 * Servlet class. This is set during registration process and set to null
	 * during unregistration.
	 */
	private Class<? extends Servlet> servletClass;
	/**
	 * Servlet mapped url paterns. This is not set by the parser but by the web
	 * app while adding a servlet mapping.
	 */
	private final List<WebAppInitParam> initParams;
	/**
	 * Aliases corresponding to servlet mapped url paterns. This is not set by
	 * the parser but by the web app while adding a servlet mapping.
	 */
	private final Set<String> aliases;
	private int loadOnStartup;
	private boolean asyncSupported;
	private MultipartConfigElement multipartConfigurations;

	/**
	 * Creates a new web app servlet.
	 */
	public WebAppServlet() {
		aliases = new HashSet<>();
		initParams = new ArrayList<>();
	}

	/**
	 * Getter.
	 *
	 * @return servlet name
	 */
	public String getServletName() {
		return servletName;
	}

	/**
	 * Setter.
	 *
	 * @param servletName value to set. Cannot be null
	 * @throws NullArgumentException if servlet name is null
	 */
	public void setServletName(final String servletName) {
//		NullArgumentException.validateNotNull(servletName, "Servlet name");
		this.servletName = servletName;
		// sepcify filter name for Pax Web
		final WebAppInitParam initParam = new WebAppInitParam();
		initParam.setParamName("servlet-name");
		initParam.setParamValue(servletName);
		initParams.add(initParam);
	}

	/**
	 * Getter.
	 *
	 * @return servlet class name
	 */
	public String getServletClassName() {
		return servletClassName;
	}

	/**
	 * Setter.
	 *
	 * @param servletClassName value to set. Cannot be null
	 * @throws NullArgumentException if servlet class is null
	 */
	public void setServletClassName(final String servletClassName) {
//		NullArgumentException.validateNotNull(servletClassName,
//				"Servlet class name");
		this.servletClassName = servletClassName;
	}

	/**
	 * Getter.
	 *
	 * @return servletClass
	 */
	public Class<? extends Servlet> getServletClass() {
		return servletClass;
	}

	/**
	 * Setter.
	 *
	 * @param servletClass value to set.
	 */
	public void setServletClass(final Class<? extends Servlet> servletClass) {
		this.servletClass = servletClass;
	}

	/**
	 * Returns the aliases associated with this servlet. If there are no
	 * associated aliases an empty array is returned.
	 *
	 * @return array of aliases
	 */
	public String[] getAliases() {
		return aliases.toArray(new String[aliases.size()]);
	}

	/**
	 * Add an url mapping for servlet. The url mapping is converted to an alias
	 * by emoving trailing "*"
	 *
	 * @param urlPattern to be added. Cannot be null
	 * @throws NullArgumentException if url pattern is null
	 */
	public void addUrlPattern(final String urlPattern) {
//		NullArgumentException.validateNotNull(urlPattern, "Url pattern");
		aliases.add(urlPattern);
	}

	/**
	 * Add a init param for filter.
	 *
	 * @param param to be added
	 * @throws NullArgumentException if param, param name, param value is null
	 */
	public void addInitParam(final WebAppInitParam param) {
//		NullArgumentException.validateNotNull(param, "Init param");
//		NullArgumentException.validateNotNull(param.getParamName(),
//				"Init param name");
//		NullArgumentException.validateNotNull(param.getParamValue(),
//				"Init param value");
		initParams.add(param);
	}


	public void setMultipartConfig(MultipartConfigElement multipartConfigElement) {
//		NullArgumentException.validateNotNull(multipartConfigElement, "MultipartConfig");
		multipartConfigurations = multipartConfigElement;
	}

	/**
	 * Returns the init params associated with this filter. If there are no
	 * associated init params an empty array is returned.
	 *
	 * @return array of url patterns
	 */
	public WebAppInitParam[] getInitParams() {
		return initParams.toArray(new WebAppInitParam[initParams.size()]);
	}

	public MultipartConfigElement getMultipartConfig() {
		return multipartConfigurations;
	}

	public void setLoadOnStartup(String value) {
		if (value != null) {
			try {
				loadOnStartup = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				loadOnStartup = Integer.MAX_VALUE;
			}
		} else {
			loadOnStartup = Integer.MAX_VALUE;
		}

	}

	public int getLoadOnStartup() {
		return loadOnStartup;
	}

	public void setLoadOnStartup(int loadOnStartup) {
		this.loadOnStartup = loadOnStartup;
	}

	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public void setAsyncSupported(String value) {
		if (value != null) {
			asyncSupported = Boolean.parseBoolean(value);
		}
	}

	/* TODO: need to find the right spot to retrieve this information */
	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("servletName=").append(servletName)
				.append(",servletClass=").append(servletClassName)
				.append(",aliases=").append(aliases).append("}").toString();
	}

}