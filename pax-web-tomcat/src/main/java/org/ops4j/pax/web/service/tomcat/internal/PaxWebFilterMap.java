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
package org.ops4j.pax.web.service.tomcat.internal;

import jakarta.servlet.DispatcherType;

import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

/**
 * Special Tomcat's {@link FilterMap} that remembers original {@link FilterModel}
 */
public class PaxWebFilterMap extends FilterMap {

	private final FilterModel filterModel;

	/** Flag to mark the filter as "initial" - that handles preprocessors and security in difficult Tomcat env. */
	private final boolean initialFilter;

	/**
	 * Creates a {@link FilterMap} that has to be configured later (dynamic or static mappings
	 * per dispatcher type)
	 * @param filterModel
	 */
	public PaxWebFilterMap(FilterModel filterModel, FilterModel.Mapping mapping) {
		this.filterModel = filterModel;
		this.initialFilter = false;

		setFilterName(filterModel.getName());
		addMaping(mapping);
	}

	public PaxWebFilterMap(FilterModel filterModel, FilterModel.DynamicMapping mapping) {
		this.filterModel = filterModel;
		this.initialFilter = false;

		setFilterName(filterModel.getName());
		addDynamicMaping(mapping);
	}

	/**
	 * Create a {@link FilterMap} with just one mapping
	 * @param filterModel
	 * @param initialFilter
	 */
	public PaxWebFilterMap(FilterModel filterModel, boolean initialFilter) {
		this.filterModel = filterModel;
		this.initialFilter = initialFilter;

		setFilterName(filterModel.getName());

		if (filterModel.getMappingsPerDispatcherTypes().size() != 1) {
			throw new IllegalArgumentException("Filter Mapping should have one set of mappings specified");
		}

		addMaping(filterModel.getMappingsPerDispatcherTypes().get(0));
	}

	public void addMaping(FilterModel.Mapping mapping) {
		if (mapping.getDispatcherTypes() != null) {
			for (DispatcherType dispatcherType : mapping.getDispatcherTypes()) {
				setDispatcher(dispatcherType.name());
			}
		}
		if (mapping.getRegexPatterns() != null && mapping.getRegexPatterns().length > 0) {
			// special mapping kind from Whiteboard Service spec
			addURLPatternDecoded("*");
		} else if (mapping.getUrlPatterns() != null && mapping.getUrlPatterns().length > 0) {
			for (String urlPattern : mapping.getUrlPatterns()) {
				addURLPatternDecoded(urlPattern);
			}
		} else if (mapping.getServletNames() != null && mapping.getServletNames().length > 0) {
			for (String servletName : mapping.getServletNames()) {
				addServletName(servletName);
			}
		}
	}

	public void addDynamicMaping(FilterModel.DynamicMapping mapping) {
		if (mapping.getDispatcherTypes() != null) {
			for (DispatcherType dispatcherType : mapping.getDispatcherTypes()) {
				setDispatcher(dispatcherType.name());
			}
		}
		if (mapping.getUrlPatterns() != null && mapping.getUrlPatterns().length > 0) {
			for (String urlPattern : mapping.getUrlPatterns()) {
				addURLPatternDecoded(urlPattern);
			}
		} else if (mapping.getServletNames() != null && mapping.getServletNames().length > 0) {
			for (String servletName : mapping.getServletNames()) {
				addServletName(servletName);
			}
		}
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	public boolean isInitial() {
		return initialFilter;
	}

}
