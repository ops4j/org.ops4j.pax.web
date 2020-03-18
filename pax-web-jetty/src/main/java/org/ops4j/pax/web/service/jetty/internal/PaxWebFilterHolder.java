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

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.osgi.framework.ServiceReference;

/**
 * Special {@link FilterHolder} to handle OSGi specific lifecycle related to
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}.
 */
public class PaxWebFilterHolder extends FilterHolder {

	private final FilterModel filterModel;

	private ServiceReference<? extends Filter> filterReference;

	/**
	 * Initialize {@link PaxWebFilterHolder} with {@link FilterModel}. All its
	 * {@link FilterModel#getContextModels() OSGi contexts} will determinie when the filter will be used during
	 * request processing.
	 *
	 * @param sch
	 * @param filterModel
	 */
	public PaxWebFilterHolder(ServletContextHandler sch, FilterModel filterModel) {
		this.filterModel = filterModel;

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

		return instance == null ? null : new PaxWebFilterHolder.OsgiInitializedFilter(instance);
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

	/**
	 * {@link Filter} wrapper that uses correct {@link FilterConfig} wrapper that returns correct wrapper
	 * for {@link javax.servlet.ServletContext}
	 */
	@Review("Move to pax-web-spi")
	private class OsgiInitializedFilter implements Filter {

		private final Filter filter;

		public OsgiInitializedFilter(Filter filter) {
			this.filter = filter;
		}

		@Override
		public void init(final FilterConfig config) throws ServletException {
			filter.init(new FilterConfig() {
				@Override
				public String getFilterName() {
					return config.getFilterName();
				}

				@Override
				public ServletContext getServletContext() {
					// TODO: this should come either from the servlet that's at the end of current chain or
					//       be the "best" ServletContext for given ServletContextModel
					return null;
				}

				@Override
				public String getInitParameter(String name) {
					return config.getInitParameter(name);
				}

				@Override
				public Enumeration<String> getInitParameterNames() {
					return config.getInitParameterNames();
				}
			});
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			filter.doFilter(request, response, chain);
		}

		@Override
		public void destroy() {
			filter.destroy();
		}
	}

}
