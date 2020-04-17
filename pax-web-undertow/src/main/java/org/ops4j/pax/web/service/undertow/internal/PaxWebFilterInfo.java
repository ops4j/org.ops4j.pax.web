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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.ServiceReference;

/**
 * Special {@link FilterInfo} that can be configured from {@link FilterModel}.
 */
public class PaxWebFilterInfo extends FilterInfo {

	private final FilterModel filterModel;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard filter - but only at init() time. */
	private final OsgiScopedServletContext servletContext;

	private ServiceReference<? extends Filter> serviceReference;

	public PaxWebFilterInfo(FilterModel model, OsgiServletContext osgiServletContext) {
		super(model.getName(), model.getActualClass(),
				new FilterModelFactory(model,
						new OsgiScopedServletContext(osgiServletContext, model.getRegisteringBundle())));

		this.osgiServletContext = osgiServletContext;

		this.filterModel = model;

		for (Map.Entry<String, String> param : filterModel.getInitParams().entrySet()) {
			addInitParam(param.getKey(), param.getValue());
		}
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported());

		filterModel.getInitParams().forEach(this::addInitParam);

		this.servletContext = ((FilterModelFactory)super.getInstanceFactory()).getServletContext();
	}


	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public FilterInfo clone() {
		final FilterInfo info = new PaxWebFilterInfo(this.filterModel, this.osgiServletContext);

		info.setAsyncSupported(isAsyncSupported());
		getInitParams().forEach(info::addInitParam);

		return info;
	}

	/**
	 * An {@link InstanceFactory} that returns {@link Filter filter instance} from {@link FilterModel}.
	 */
	private static class FilterModelFactory implements InstanceFactory<Filter> {

		private final FilterModel model;
		private final OsgiScopedServletContext osgiScopedServletContext;

		public FilterModelFactory(FilterModel model, OsgiScopedServletContext osgiScopedServletContext) {
			this.model = model;
			this.osgiScopedServletContext = osgiScopedServletContext;
		}

		@Override
		public InstanceHandle<Filter> createInstance() throws InstantiationException {
			Filter instance = model.getFilter();
			if (instance == null) {
				if (model.getElementReference() != null) {
					// obtain Filter using reference
					instance = model.getRegisteringBundle().getBundleContext().getService(model.getElementReference());
					if (instance == null) {
						throw new RuntimeException("Can't get a Filter service from the reference " + model.getElementReference());
					}
				} else if (model.getFilterClass() != null) {
					try {
						instance = model.getFilterClass().newInstance();
					} catch (Exception e) {
						InstantiationException instantiationException = new InstantiationException(e.getMessage());
						instantiationException.initCause(e);
						throw instantiationException;
					}
				}
			}

			// TODO: process annotations

			Filter osgiInitializedFilter = new OsgiInitializedFilter(instance, this.osgiScopedServletContext);
			Filter scopedFilter = new ScopedFilter(osgiInitializedFilter, model);

			return new ImmediateInstanceHandle<Filter>(scopedFilter);
		}

		public OsgiScopedServletContext getServletContext() {
			return osgiScopedServletContext;
		}
	}

	/**
	 * <p>{@link Filter} wrapper that can skip delegate's invocation if
	 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel} doesn't match.</p>
	 *
	 * <p>This is important because of:
	 * <blockquote>
	 *     140.5 Registering Servlet Filters
	 *      [...] Servlet filters are only applied to servlet requests if they are bound to the same Servlet
	 *      Context Helper and the same Http Whiteboard implementation.
	 * </blockquote></p>
	 *
	 * <p>In Jetty and in Tomcat we can configure the filters associated with invocation, but not in Undertow.</p>
	 */
	private static class ScopedFilter implements Filter {

		private final Filter filter;
		private final FilterModel model;

		public ScopedFilter(Filter filter, FilterModel model) {
			this.filter = filter;
			this.model = model;
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			filter.init(filterConfig);
		}

		@Override
		public void destroy() {
			filter.destroy();
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			ServletContext context = request.getServletContext();
			boolean skip = false;
			if (context instanceof OsgiScopedServletContext) {
				if (!model.getContextModels().contains(((OsgiScopedServletContext)context).getOsgiContextModel())) {
					skip = true;
				}
			}
			if (!skip) {
				// proceed with the filter
				filter.doFilter(request, response, chain);
			} else {
				// skip the filter, proceed with the chain
				chain.doFilter(request, response);
			}
		}
	}

}
