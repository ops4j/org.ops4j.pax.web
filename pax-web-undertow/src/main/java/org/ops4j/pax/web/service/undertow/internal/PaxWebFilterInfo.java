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

import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.spi.servlet.ScopedFilter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.runtime.dto.DTOConstants;

/**
 * Special {@link FilterInfo} that can be configured from {@link FilterModel}.
 */
public class PaxWebFilterInfo extends FilterInfo {

	private final FilterModel filterModel;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.servlet.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;

	private ServiceReference<? extends Filter> serviceReference;

	private final boolean whiteboardTCCL;

	public PaxWebFilterInfo(FilterModel model, OsgiServletContext osgiServletContext,
			boolean whiteboardTCCL) {
		super(model.getName(), model.getActualClass(),
				new FilterModelFactory(model,
						new OsgiScopedServletContext(osgiServletContext, model.getRegisteringBundle()),
						whiteboardTCCL));

		this.osgiServletContext = osgiServletContext;

		this.filterModel = model;

		for (Map.Entry<String, String> param : filterModel.getInitParams().entrySet()) {
			addInitParam(param.getKey(), param.getValue());
		}
		setAsyncSupported(filterModel.getAsyncSupported() != null && filterModel.getAsyncSupported());

		filterModel.getInitParams().forEach(this::addInitParam);
		this.whiteboardTCCL = whiteboardTCCL;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public FilterInfo clone() {
		final FilterInfo info = new PaxWebFilterInfo(this.filterModel, this.osgiServletContext,
				this.whiteboardTCCL);

		info.setAsyncSupported(isAsyncSupported());
		getInitParams().forEach(info::addInitParam);

		return info;
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	/**
	 * An {@link InstanceFactory} that returns {@link Filter filter instance} from {@link FilterModel}.
	 */
	private static class FilterModelFactory implements InstanceFactory<Filter> {

		private final FilterModel model;
		private final OsgiScopedServletContext osgiScopedServletContext;
		private ServiceObjects<Filter> serviceObjects;

		private final boolean whiteboardTCCL;

		FilterModelFactory(FilterModel model, OsgiScopedServletContext osgiScopedServletContext, boolean whiteboardTCCL) {
			this.model = model;
			this.osgiScopedServletContext = osgiScopedServletContext;
			this.whiteboardTCCL = whiteboardTCCL;
		}

		@Override
		public InstanceHandle<Filter> createInstance() throws InstantiationException {
			Filter instance = model.getFilter();
			if (instance == null) {
				if (model.getElementReference() != null) {
					// obtain Filter using reference
					BundleContext context = model.getRegisteringBundle().getBundleContext();
					if (context != null) {
						if (!model.isPrototype()) {
							instance = context.getService(model.getElementReference());
						} else {
							serviceObjects = context.getServiceObjects(model.getElementReference());
							instance = serviceObjects.getService();
						}
					}
					if (instance == null) {
						model.setDtoFailureCode(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
						throw new RuntimeException("Can't get a Filter service from the reference " + model.getElementReference());
					}
				} else if (model.getFilterClass() != null) {
					try {
						instance = model.getFilterClass().getConstructor().newInstance();
					} catch (Exception e) {
						InstantiationException instantiationException = new InstantiationException(e.getMessage());
						instantiationException.initCause(e);
						throw instantiationException;
					}
				} else if (model.getElementSupplier() != null) {
					instance = model.getElementSupplier().get();
				}
			}

			Filter osgiInitializedFilter = new OsgiInitializedFilter(instance, model, this.osgiScopedServletContext, whiteboardTCCL);
			Filter scopedFilter = new ScopedFilter(osgiInitializedFilter, model);

			return new ImmediateInstanceHandle<Filter>(scopedFilter) {
				@Override
				public void release() {
					if (model.getElementReference() != null) {
						try {
							if (!model.isPrototype()) {
								BundleContext context = model.getRegisteringBundle().getBundleContext();
								if (context != null) {
									context.ungetService(model.getElementReference());
								}
							} else if (getInstance() != null) {
								Filter realFilter = getInstance();
								if (realFilter instanceof ScopedFilter) {
									realFilter = ((ScopedFilter) realFilter).getDelegate();
								}
								if (realFilter instanceof OsgiInitializedFilter) {
									realFilter = ((OsgiInitializedFilter) realFilter).getDelegate();
								}
								serviceObjects.ungetService(realFilter);
							}
						} catch (IllegalStateException e) {
							// bundle context has already been invalidated ?
						}
					}
					if (model.getRegisteringBundle() != null) {
						osgiScopedServletContext.releaseWebContainerContext(model.getRegisteringBundle());
					}
				}
			};
		}

		public OsgiScopedServletContext getServletContext() {
			return osgiScopedServletContext;
		}
	}

}
