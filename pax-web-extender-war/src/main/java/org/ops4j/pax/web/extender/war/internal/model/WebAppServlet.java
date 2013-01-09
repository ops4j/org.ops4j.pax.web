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
import javax.servlet.Servlet;
import org.ops4j.lang.NullArgumentException;

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
	private String m_servletName;
	/**
	 * Servlet class name.
	 */
	private String m_servletClassName;
	/**
	 * Servlet class. This is set during registration process and set to null
	 * during unregistration.
	 */
	private Class<? extends Servlet> m_servletClass;
	/**
	 * Servlet mapped url paterns. This is not set by the parser but by the web
	 * app while adding a servlet mapping.
	 */
	private final List<WebAppInitParam> m_initParams;
	/**
	 * Aliases corresponding to servlet mapped url paterns. This is not set by
	 * the parser but by the web app while adding a servlet mapping.
	 */
	private final Set<String> m_aliases;
	private int m_loadOnStartup;
	private boolean asyncSupported;

	/**
	 * Creates a new web app servlet.
	 */
	public WebAppServlet() {
		m_aliases = new HashSet<String>();
		m_initParams = new ArrayList<WebAppInitParam>();
	}

	/**
	 * Getter.
	 * 
	 * @return servlet name
	 */
	public String getServletName() {
		return m_servletName;
	}

	/**
	 * Setter.
	 * 
	 * @param servletName
	 *            value to set. Cannot be null
	 * 
	 * @throws NullArgumentException
	 *             if servlet name is null
	 */
	public void setServletName(final String servletName) {
		NullArgumentException.validateNotNull(servletName, "Servlet name");
		m_servletName = servletName;
		// sepcify filter name for Pax Web
		final WebAppInitParam initParam = new WebAppInitParam();
		initParam.setParamName("servlet-name");
		initParam.setParamValue(servletName);
		m_initParams.add(initParam);
	}

	/**
	 * Getter.
	 * 
	 * @return servlet class name
	 */
	public String getServletClassName() {
		return m_servletClassName;
	}

	/**
	 * Setter.
	 * 
	 * @param servletClassName
	 *            value to set. Cannot be null
	 * 
	 * @throws NullArgumentException
	 *             if servlet class is null
	 */
	public void setServletClassName(final String servletClassName) {
		NullArgumentException.validateNotNull(servletClassName, "Servlet class name");
		m_servletClassName = servletClassName;
	}

	/**
	 * Getter.
	 * 
	 * @return servletClass
	 */
	public Class<? extends Servlet> getServletClass() {
		return m_servletClass;
	}

	/**
	 * Setter.
	 * 
	 * @param servletClass
	 *            value to set.
	 */
	public void setServletClass(final Class<? extends Servlet> servletClass) {
		m_servletClass = servletClass;
	}

	/**
	 * Returns the aliases associated with this servlet. If there are no
	 * associated aliases an empty array is returned.
	 * 
	 * @return array of aliases
	 */
	public String[] getAliases() {
		return m_aliases.toArray(new String[m_aliases.size()]);
	}

	/**
	 * Add an url mapping for servlet. The url mapping is converted to an alias
	 * by emoving trailing "*"
	 * 
	 * @param urlPattern
	 *            to be added. Cannot be null
	 * 
	 * @throws NullArgumentException
	 *             if url pattern is null
	 */
	public void addUrlPattern(final String urlPattern) {
		NullArgumentException.validateNotNull(urlPattern, "Url pattern");
		m_aliases.add(urlPattern);
	}

	/**
	 * Add a init param for filter.
	 * 
	 * @param param
	 *            to be added
	 * 
	 * @throws NullArgumentException
	 *             if param, param name, param value is null
	 */
	public void addInitParam(final WebAppInitParam param) {
		NullArgumentException.validateNotNull(param, "Init param");
		NullArgumentException.validateNotNull(param.getParamName(),
				"Init param name");
		NullArgumentException.validateNotNull(param.getParamValue(),
				"Init param value");
		m_initParams.add(param);
	}

	/**
	 * Returns the init params associated with this filter. If there are no
	 * associated init params an empty array is returned.
	 * 
	 * @return array of url patterns
	 */
	public WebAppInitParam[] getInitParams() {
		return m_initParams.toArray(new WebAppInitParam[m_initParams.size()]);
	}

	public void setLoadOnStartup(String value) {
		if (value != null) {
			try {
				m_loadOnStartup = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				m_loadOnStartup = Integer.MAX_VALUE;
			}
		} else
			m_loadOnStartup = Integer.MAX_VALUE;

	}

	public int getLoadOnStartup() {
		return m_loadOnStartup;
	}


	public void setLoadOnStartup(int loadOnStartup) {
		m_loadOnStartup = loadOnStartup;
	}

	public void setAsyncSupported(boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public void setAsyncSupported(String value) {
		if (value != null) {
			asyncSupported = Boolean.parseBoolean(value);
		}
	}

	@Override
	public String toString() {
		return new StringBuffer().append(this.getClass().getSimpleName())
				.append("{").append("servletName=").append(m_servletName)
				.append(",servletClass=").append(m_servletClassName)
				.append(",aliases=").append(m_aliases).append("}").toString();
	}
}