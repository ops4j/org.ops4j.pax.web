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

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.eclipse.jetty.servlet.FilterHolder;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.ServiceReference;

/**
 * Special {@link FilterHolder} to handle OSGi specific lifecycle related to
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}.
 */
public class PaxWebFilterHolder extends FilterHolder {

	private final FilterModel filterModel;

	private ServiceReference<? extends Filter> filterReference;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard filter */
	private final OsgiScopedServletContext servletContext;

	/**
	 * Initialize {@link PaxWebFilterHolder} with {@link FilterModel}. All its
	 * {@link FilterModel#getContextModels() OSGi contexts} will determinie when the filter will be used during
	 * request processing.
	 *
	 * @param filterModel
	 * @param osgiServletContext
	 */
	public PaxWebFilterHolder(FilterModel filterModel, OsgiServletContext osgiServletContext) {
		this.filterModel = filterModel;
		this.osgiServletContext = osgiServletContext;

		// name that binds a servlet with its mapping
		setName(filterModel.getName());
		if (filterModel.getFilterClass() != null) {
			setHeldClass(filterModel.getFilterClass());
		} else if (filterModel.getFilter() != null) {
			setFilter(filterModel.getFilter());
		} else {
			this.filterReference = filterModel.getElementReference();
		}

		setInitParameters(filterModel.getInitParams());
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported());

		// setup proper delegation for ServletContext
		servletContext = new OsgiScopedServletContext(this.osgiServletContext, filterModel.getRegisteringBundle());
	}

	@Override
	public void doStart() throws Exception {
		if (filterReference != null) {
			setHeldClass(Filter.class);
		}

		super.doStart();
	}

	/**
	 * Method called by {@code org.eclipse.jetty.servlet.FilterHolder#initialize()} - single place where {@link Filter}
	 * instance can be created. This is where we can get the filter from OSGi service registry.
	 * @return
	 */
	@Override
	protected synchronized Filter getInstance() {
		Filter instance = super.getInstance();
		if (instance == null) {
			// obtain Filter using reference
			ServiceReference<? extends Filter> ref = filterModel.getElementReference();
			if (ref != null) {
				instance =  filterModel.getRegisteringBundle().getBundleContext().getService(ref);
			}
		}

		if (instance == null && filterModel.getFilterClass() != null) {
			Class<? extends Filter> filterClass = filterModel.getFilterClass();
			try {
				instance = filterClass.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Can't instantiate Filter with class " + filterClass, e);
			}
		}

		return instance == null ? null : new OsgiInitializedFilter(instance, servletContext);
	}

	@Override
	public void destroyInstance(Object o) throws Exception {
		if (filterModel != null && filterModel.getElementReference() != null) {
			filterModel.getRegisteringBundle().getBundleContext().ungetService(filterModel.getElementReference());
		}
		super.destroyInstance(o);
	}

	/**
	 * Check whether current filter should be used within given {@link OsgiContextModel} according to
	 * "140.5 Registering Servlet Filters"
	 *
	 * @param targetContext
	 * @return
	 */
	public boolean matches(OsgiContextModel targetContext) {
		return filterModel.getContextModels().contains(targetContext);
	}

}
