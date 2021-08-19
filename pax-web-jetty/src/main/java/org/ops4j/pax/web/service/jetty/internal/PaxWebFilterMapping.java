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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.FilterMapping;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;

/**
 * Special {@link FilterMapping} that rememebers {@link org.ops4j.pax.web.service.spi.model.elements.FilterModel}
 * that it represents.
 */
public class PaxWebFilterMapping extends FilterMapping {

	private final FilterModel filterModel;
	private final long timestamp;
	private final boolean after;

	/**
	 * {@link FilterMapping} with selected {@link FilterModel.Mapping}
	 * @param filterModel
	 * @param mapping
	 */
	public PaxWebFilterMapping(FilterModel filterModel, FilterModel.Mapping mapping) {
		this.filterModel = filterModel;
		// remember when given mapping was created to detect if associated filter should be destroyed and initialized
		// again when configuration changes.
		this.timestamp = filterModel.getTimestamp();

		List<DispatcherType> types = new ArrayList<>();
		Collections.addAll(types, mapping.getDispatcherTypes());
		this.setDispatcherTypes(EnumSet.copyOf(types));

		this.setFilterName(filterModel.getName());
		this.setPathSpecs(mapping.getUrlPatterns());
		this.setServletNames(mapping.getServletNames());

		// special mapping kind from Whiteboard Service spec
		String[] regexMapping = mapping.getRegexPatterns();
		if (regexMapping != null && regexMapping.length > 0) {
			this.setPathSpecs(new String[] { "/*" });
		}

		this.after = true;
	}

	/**
	 * Version of the filter mapping that uses the dynamic part of FilterModel's mapping.
	 * @param filterModel
	 * @param dynamicMapping
	 */
	public PaxWebFilterMapping(FilterModel filterModel, FilterModel.DynamicMapping dynamicMapping) {
		this.filterModel = filterModel;
		// remember when given mapping was created to detect if associated filter should be destroyed and initialized
		// again when configuration changes.
		this.timestamp = filterModel.getTimestamp();

		if (dynamicMapping.getDispatcherTypes() != null) {
			List<DispatcherType> types = Arrays.asList(dynamicMapping.getDispatcherTypes());
			this.setDispatcherTypes(EnumSet.copyOf(types));
		}

		this.setFilterName(filterModel.getName());
		this.setPathSpecs(dynamicMapping.getUrlPatterns());
		this.setServletNames(dynamicMapping.getServletNames());

		this.after = dynamicMapping.isAfter();
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	public boolean isAfter() {
		return after;
	}

}
