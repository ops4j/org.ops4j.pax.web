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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.Filter;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

public class FilterEventData extends WebElementEventData {

	private final String filterName;
	private String[] urlPatterns;
	private String[] servletNames;
	private String[] regexMapping;
	private String[] dispatcherTypes;
	private final Filter filter;
	private final Class<? extends Filter> filterClass;

	public FilterEventData(String filterName, List<FilterModel.Mapping> mapping, Filter filter, Class<? extends Filter> filterClass) {
		this.filterName = filterName;
		List<String> urlPatterns = new ArrayList<>();
		List<String> servletNames = new ArrayList<>();
		List<String> regexMapping = new ArrayList<>();
		// I explicitly drop the DispatcherType -> mapping relation flattening all mapping information
		for (FilterModel.Mapping m : mapping) {
			String[] up = m.getUrlPatterns();
			if (up != null) {
				this.urlPatterns = Arrays.copyOf(up, up.length);
			}
			String[] sn = m.getServletNames();
			if (sn != null) {
				this.servletNames = Arrays.copyOf(sn, sn.length);
			}
			String[] rm = m.getRegexPatterns();
			if (rm != null) {
				this.regexMapping = Arrays.copyOf(rm, rm.length);
			}
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

	public Filter getFilter() {
		return filter;
	}

	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

}
