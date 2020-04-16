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

import javax.servlet.Filter;

import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.osgi.framework.ServiceReference;

/**
 * Extension of Tomcat's {@link FilterDef} which can tell if it's suitable for a chain with given target
 * servlet (scoped to some target {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}.
 * Similar to Pax Web Jetty's {@code org.ops4j.pax.web.service.jetty.internal.PaxWebFilterHolder}.
 */
public class PaxWebFilterDef extends FilterDef {

	private final FilterModel filterModel;

	/** Flag to mark the filter as "initial" - that handles preprocessors and security in difficult Tomcat env. */
	private final boolean initialFilter;

	// TODO: handle reference
	private ServiceReference<? extends Filter> filterReference;

	public PaxWebFilterDef(FilterModel filterModel, boolean initialFilter) {
		this.filterModel = filterModel;
		this.initialFilter = initialFilter;

		// name that binds a servlet with its mapping
		setFilterName(filterModel.getName());

		if (filterModel.getFilterClass() != null) {
			try {
				setFilter(filterModel.getFilterClass().newInstance());
			} catch (Exception e) {
				throw new RuntimeException("Can't instantiate filter of class " + filterModel.getFilterClass());
			}
		} else if (filterModel.getFilter() != null) {
			setFilter(filterModel.getFilter());
		} else {
			this.filterReference = filterModel.getElementReference();
		}

		filterModel.getInitParams().forEach(this::addInitParameter);
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported() ? "true" : "false");

	}

	/**
	 * Check whether current filter should be used within given {@link OsgiContextModel} according to
	 * "140.5 Registering Servlet Filters"
	 *
	 * @param targetContext
	 * @return
	 */
	public boolean matches(OsgiContextModel targetContext) {
		return initialFilter || filterModel.getContextModels().contains(targetContext);
	}

}
