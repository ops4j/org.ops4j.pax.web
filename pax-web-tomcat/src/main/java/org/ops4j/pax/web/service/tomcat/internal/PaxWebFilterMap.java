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

import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

/**
 * Special Tomcat's {@link FilterMap} that remembers original {@link FilterModel}
 */
public class PaxWebFilterMap extends FilterMap {

	private final FilterModel filterModel;

	/** Flag to mark the filter as "initial" - that handles preprocessors and security in difficult Tomcat env. */
	private final boolean initialFilter;

	public PaxWebFilterMap(FilterModel filterModel, boolean initialFilter) {
		this.filterModel = filterModel;
		this.initialFilter = initialFilter;

		setFilterName(filterModel.getName());

		if (filterModel.getServletNames() != null) {
			for (String servletName : filterModel.getServletNames()) {
				addServletName(servletName);
			}
		}
		if (filterModel.getRegexMapping() != null && filterModel.getRegexMapping().length > 0) {
			// special mapping kind from Whiteboard Service spec
			addURLPatternDecoded("*");
		} else if (filterModel.getUrlPatterns() != null) {
			for (String urlPattern : filterModel.getUrlPatterns()) {
				addURLPatternDecoded(urlPattern);
			}
		}
		if (filterModel.isDynamic()) {
			filterModel.getDynamicServletNames().forEach(dm -> {
				if (!dm.isAfter()) {
					for (String sn : dm.getServletNames()) {
						addServletName(sn);
					}
				}
			});
			filterModel.getDynamicUrlPatterns().forEach(dm -> {
				if (!dm.isAfter()) {
					for (String pattern : dm.getUrlPatterns()) {
						addURLPatternDecoded(pattern);
					}
				}
			});
			filterModel.getDynamicServletNames().forEach(dm -> {
				if (dm.isAfter()) {
					for (String sn : dm.getServletNames()) {
						addServletName(sn);
					}
				}
			});
			filterModel.getDynamicUrlPatterns().forEach(dm -> {
				if (dm.isAfter()) {
					for (String pattern : dm.getUrlPatterns()) {
						addURLPatternDecoded(pattern);
					}
				}
			});
		}
		if (filterModel.getDispatcherTypes() != null) {
			for (String dispatcherType : filterModel.getDispatcherTypes()) {
				setDispatcher(dispatcherType);
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
