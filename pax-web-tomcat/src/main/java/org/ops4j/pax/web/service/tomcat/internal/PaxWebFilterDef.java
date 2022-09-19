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

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.GenericFilter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.ScopedFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;

/**
 * Extension of Tomcat's {@link FilterDef} which can tell if it's suitable for a chain with given target
 * servlet (scoped to some target {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}.
 * Similar to Pax Web Jetty's {@code org.ops4j.pax.web.service.jetty.internal.PaxWebFilterHolder}.
 */
public class PaxWebFilterDef extends FilterDef {

	private final FilterModel filterModel;

	/** Flag to mark the filter as "initial" - that handles preprocessors and security in difficult Tomcat env. */
	private final boolean initialFilter;

	private ServiceReference<Filter> filterReference;

	/** This {@link ServletContext} is scoped to particular Whiteboard filter */
	private OsgiScopedServletContext servletContext = null;

	private boolean whiteboardTCCL;

	public PaxWebFilterDef(FilterModel filterModel, boolean initialFilter, OsgiServletContext osgiContext) {
		this.filterModel = filterModel;
		this.initialFilter = initialFilter;

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
		} else if (filterModel.getElementSupplier() != null) {
			instance = filterModel.getElementSupplier().get();
		} else {
			this.filterReference = filterModel.getElementReference();
		}

		filterModel.getInitParams().forEach(this::addInitParameter);
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported() ? "true" : "false");

		// setup proper delegation for ServletContext
		if (osgiContext != null) {
			servletContext = new OsgiScopedServletContext(osgiContext, filterModel.getRegisteringBundle());
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
			Filter delegate = filter == null ? new LifecycleFilter()
					: new ScopedFilter(new OsgiInitializedFilter(filter, filterModel, servletContext, whiteboardTCCL), filterModel);
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

	public void setWhiteboardTCCL(boolean whiteboardTCCL) {
		this.whiteboardTCCL = whiteboardTCCL;
	}

	/**
	 * This filter can instantiate the target filter. Only needed for Tomcat, where the "holder" is not that
	 * extensible.
	 */
	private class LifecycleFilter implements Filter {

		private Filter filter;
		private ServiceObjects<Filter> serviceObjects;

		LifecycleFilter() {
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			if (filterReference != null) {
				// it SHOULD be a reference
				Filter instance = null;
				BundleContext context = filterModel.getRegisteringBundle().getBundleContext();
				if (context != null) {
					if (!filterModel.isPrototype()) {
						instance = context.getService(filterReference);
					} else {
						serviceObjects = context.getServiceObjects(filterReference);
						instance = serviceObjects.getService();
					}
				}

				if (instance == null) {
					filterModel.setDtoFailureCode(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
				}

				filter = new ScopedFilter(new OsgiInitializedFilter(instance, filterModel, servletContext, whiteboardTCCL), filterModel);
			} else {
				// strange...
				filter = new GenericFilter() {
					@Override
					public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
						chain.doFilter(request, response);
					}
				};
			}
			filter.init(filterConfig);
		}

		@Override
		public void destroy() {
			filter.destroy();
			if (filterReference != null) {
				if (!filterModel.isPrototype()) {
					BundleContext context = filterModel.getRegisteringBundle().getBundleContext();
					if (context != null) {
						context.ungetService(filterReference);
					}
				} else {
					Filter realFilter = filter;
					if (realFilter instanceof ScopedFilter) {
						realFilter = ((ScopedFilter) realFilter).getDelegate();
					}
					if (realFilter instanceof OsgiInitializedFilter) {
						realFilter = ((OsgiInitializedFilter) realFilter).getDelegate();
					}
					serviceObjects.ungetService(realFilter);
				}
			}
			if (servletContext != null && filterModel.getRegisteringBundle() != null) {
				servletContext.releaseWebContainerContext(filterModel.getRegisteringBundle());
			}
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			// proceed with the filter
			filter.doFilter(request, response, chain);
		}

	}

}
