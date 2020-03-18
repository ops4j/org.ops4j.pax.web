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

	public PaxWebFilterMapping(FilterModel filterModel) {
		this.filterModel = filterModel;
		// remember when given mapping was created to detect if associated filter should be destroyed and initialized
		// again when configuration changes.
		this.timestamp = filterModel.getTimestamp();

		List<DispatcherType> types = new ArrayList<>();
		for (String type : filterModel.getDispatcherTypes()) {
			types.add(DispatcherType.valueOf(type));
		}
		this.setDispatcherTypes(EnumSet.copyOf(types));

		this.setFilterName(filterModel.getName());
		this.setPathSpecs(filterModel.getUrlPatterns());
		this.setServletNames(filterModel.getServletNames());

		// special mapping kind from Whiteboard Service spec
		String[] regexMapping = filterModel.getRegexMapping();
		if (regexMapping != null && regexMapping.length > 0) {
			this.setPathSpecs(new String[] { "/*" });
		}
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

}
