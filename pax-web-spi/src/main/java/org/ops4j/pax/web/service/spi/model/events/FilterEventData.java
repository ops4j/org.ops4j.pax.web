/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.model.events;

import java.util.Arrays;
import javax.servlet.Filter;

public class FilterEventData extends WebElementEventData {

	private final String filterName;
	private String[] urlPatterns;
	private String[] servletNames;
	private String[] regexMapping;
	private String[] dispatcherTypes;
	private final Filter filter;
	private final Class<? extends Filter> filterClass;

	public FilterEventData(String filterName, String[] urlPatterns, String[] servletNames,
			String[] regexMapping, String[] dispatcherTypes, Filter filter, Class<? extends Filter> filterClass) {
		this.filterName = filterName;
		if (urlPatterns != null) {
			this.urlPatterns = Arrays.copyOf(urlPatterns, urlPatterns.length);
		}
		if (servletNames != null) {
			this.servletNames = Arrays.copyOf(servletNames, servletNames.length);
		}
		if (regexMapping != null) {
			this.regexMapping = Arrays.copyOf(regexMapping, regexMapping.length);
		}
		if (dispatcherTypes != null) {
			this.dispatcherTypes = Arrays.copyOf(dispatcherTypes, dispatcherTypes.length);
		}
		this.filter = filter;
		this.filterClass = filterClass;
	}

	public String getFilterName() {
		return filterName;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String[] getServletNames() {
		return servletNames;
	}

	public String[] getRegexMapping() {
		return regexMapping;
	}

	public String[] getDispatcherTypes() {
		return dispatcherTypes;
	}

	public Filter getFilter() {
		return filter;
	}

	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

}
