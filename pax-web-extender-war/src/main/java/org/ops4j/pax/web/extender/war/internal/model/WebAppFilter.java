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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

//import org.ops4j.lang.NullArgumentException;

/**
 * Models filter element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebAppFilter {

	/**
	 * Filter name.
	 */
	private String filterName;
	/**
	 * Filter class name.
	 */
	private String filterClassName;
	/**
	 * Filter instance. This is set during registration process and set to null
	 * during unregistration.
	 */
	private Filter filter;
	/**
	 * List of filter init params.
	 */
	private final List<WebAppInitParam> initParams;
	/**
	 * Filter mapped url paterns. This is not set by the parser but by the web
	 * app while adding a filter mapping.
	 */
	private final Set<String> urlPatterns;
	/**
	 * Filter mapped servlet names. This is not set by the parser but by the web
	 * app while adding a filter mapping.
	 */
	private final Set<String> servletNames;

	private final EnumSet<DispatcherType> dispatcherTypes = EnumSet.noneOf(DispatcherType.class);

	private Class<? extends Filter> filterClass;

	private Boolean asyncSupported;

	/**
	 * Creates a new web app filter.
	 */
	public WebAppFilter() {
		this.urlPatterns = new HashSet<>();
		this.servletNames = new HashSet<>();
		this.initParams = new ArrayList<>();
	}

	/**
	 * Getter.
	 *
	 * @return filter name
	 */
	public String getFilterName() {
		return filterName;
	}

	/**
	 * Setter.
	 *
	 * @param filterName value to set. Cannot be null
	 * @throws NullArgumentException if filter name is null
	 */
	public void setFilterName(final String filterName) {
//		NullArgumentException.validateNotNull(filterName, "Filter name");
		this.filterName = filterName;
		// sepcify filter name for Pax Web
		final WebAppInitParam initParam = new WebAppInitParam();
		initParam.setParamName("filter-name");
		initParam.setParamValue(filterName);
		initParams.add(initParam);
	}

	/**
	 * Getter.
	 *
	 * @return filter class name
	 */
	public String getFilterClass() {
		return filterClassName;
	}

	/**
	 * Setter.
	 *
	 * @param filterClass value to set. Cannot be null.
	 * @throws NullArgumentException if filter class is null
	 */
	public void setFilterClass(final String filterClass) {
//		NullArgumentException.validateNotNull(filterClass, "Filter class");
		this.filterClassName = filterClass;
	}

	/**
	 * Setter method for Class
	 *
	 * @param filterClass - must extend Filter
	 */
	public void setFilterClass(Class<? extends Filter> filterClass) {
//		NullArgumentException.validateNotNull(filterClass, "Filter class");
		this.filterClass = filterClass;
	}

	/**
	 * Getter.
	 *
	 * @return filter
	 */
	public Filter getFilter() {
		return filter;
	}

	/**
	 * Setter.
	 *
	 * @param filter value to set
	 */
	public void setFilter(final Filter filter) {
		this.filter = filter;
	}

	/**
	 * Returns the url patterns associated with this filter. If there are no
	 * associated url patterns an empty array is returned.
	 *
	 * @return array of url patterns
	 */
	public String[] getUrlPatterns() {
		return urlPatterns.toArray(new String[urlPatterns.size()]);
	}

	/**
	 * Add an url mapping for filter.
	 *
	 * @param urlPattern to be added. Cannot be null
	 * @throws NullArgumentException if url pattern is null or empty
	 */
	public void addUrlPattern(final String urlPattern) {
//		NullArgumentException.validateNotEmpty(urlPattern, "Url pattern");
		urlPatterns.add(urlPattern);
	}

	/**
	 * Returns the servlet names associated with this filter. If there are no
	 * associated servlet names an empty array is returned.
	 *
	 * @return array of servlet names
	 */
	public String[] getServletNames() {
		return servletNames.toArray(new String[servletNames.size()]);
	}

	/**
	 * Add a servlet name for filter.
	 *
	 * @param servletName to be added. Cannot be null
	 * @throws NullArgumentException if servlet name is null or empty
	 */
	public void addServletName(final String servletName) {
//		NullArgumentException.validateNotEmpty(servletName, "Servlet name");
		servletNames.add(servletName);
	}

	public void addDispatcherType(DispatcherType dispatcherType) {
//		NullArgumentException.validateNotNull(dispatcherType, "DispatcherType");
		dispatcherTypes.add(dispatcherType);
	}

	public EnumSet<DispatcherType> getDispatcherTypes() {
		return dispatcherTypes;
	}

	/**
	 * Add a init param for filter.
	 *
	 * @param param to be added. Canot be null
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

	/**
	 * Returns the init params associated with this filter. If there are no
	 * associated init params an empty array is returned.
	 *
	 * @return array of url patterns
	 */
	public WebAppInitParam[] getInitParams() {
		return initParams.toArray(new WebAppInitParam[initParams.size()]);
	}

	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("filterName=").append(filterName)
				.append(",filterClassName=").append(filterClassName)
				.append(",urlPatterns=").append(urlPatterns)
				.append(",servletNames=").append(servletNames).append("}")
				.toString();
	}

}