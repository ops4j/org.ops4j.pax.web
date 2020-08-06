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
import javax.servlet.ServletContext;

import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.ScopedFilter;
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

	private ServiceReference<? extends Filter> filterReference;

	/** This {@link ServletContext} the highest ranked {@link OsgiServletContext} for given physical context */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard filter */
	private OsgiScopedServletContext servletContext = null;

	public PaxWebFilterDef(FilterModel filterModel, boolean initialFilter, OsgiServletContext osgiContext) {
		this.filterModel = filterModel;
		this.initialFilter = initialFilter;
		this.osgiServletContext = osgiContext;

		// name that binds a servlet with its mapping
		setFilterName(filterModel.getName());

		Filter instance = null;
		if (filterModel.getFilterClass() != null) {
			try {
				instance = filterModel.getFilterClass().newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Can't instantiate filter of class " + filterModel.getFilterClass());
			}
		} else if (filterModel.getFilter() != null) {
			instance = filterModel.getFilter();
		} else {
			this.filterReference = filterModel.getElementReference();

			if (filterReference != null) {
				// TODO: ensure it's ungotten later
				instance = filterModel.getRegisteringBundle().getBundleContext().getService(filterReference);
			}
		}

		if (instance == null && filterModel.getElementSupplier() != null) {
			instance = filterModel.getElementSupplier().get();
		}

		filterModel.getInitParams().forEach(this::addInitParameter);
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported() ? "true" : "false");

		// setup proper delegation for ServletContext
		if (this.osgiServletContext != null) {
			servletContext = new OsgiScopedServletContext(this.osgiServletContext, filterModel.getRegisteringBundle());
		}

		if (isInitial()) {
			setFilter(instance);
		} else {
			setFilter(instance == null || servletContext == null ? null : instance);
		}
	}

	@Override
	public void setFilter(Filter filter) {
		if (isInitial()) {
			super.setFilter(filter);
		} else {
			Filter delegate = filter == null ? null : new ScopedFilter(new OsgiInitializedFilter(filter, servletContext), filterModel);
			super.setFilter(delegate);
		}
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

	public boolean isInitial() {
		return initialFilter;
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

}
