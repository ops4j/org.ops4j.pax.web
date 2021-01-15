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

import java.util.EnumSet;

import javax.servlet.DispatcherType;

//import org.ops4j.lang.NullArgumentException;

/**
 * Models filter mapping element in web.xml.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public class WebAppFilterMapping {

	/**
	 * Filter name.
	 */
	private String filterName;
	/**
	 * Mapped url pattern.
	 */
	private String urlPattern;
	/**
	 * Mapped servlet name.
	 */
	private String servletName;

	private EnumSet<DispatcherType> dispatcherSet;

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
	 * @param filterName value to set. Cannot be null.
	 * @throws NullArgumentException if filter name is null
	 */
	public void setFilterName(final String filterName) {
//		NullArgumentException.validateNotNull(filterName, "Filter name");
		this.filterName = filterName;
	}

	/**
	 * Getter.
	 *
	 * @return url pattern
	 */
	public String getUrlPattern() {
		return urlPattern;
	}

	/**
	 * Setter.
	 *
	 * @param urlPattern value to set
	 */
	public void setUrlPattern(final String urlPattern) {
		this.urlPattern = urlPattern;
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
	 * @param servletName value to set
	 */
	public void setServletName(final String servletName) {
		this.servletName = servletName;
	}

	public void setDispatcherTypes(EnumSet<DispatcherType> dispatcherType) {
		this.dispatcherSet = dispatcherType;
	}

	public EnumSet<DispatcherType> getDispatcherTypes() {
		return dispatcherSet;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("filterName=").append(filterName)
				.append(",urlPattern=").append(urlPattern)
				.append(",servletName=").append(servletName).append("}")
				.toString();
	}

}