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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.ee8.servlet.BaseHolder;
import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.ops4j.pax.web.service.jetty.internal.web.PaxWebWebSocketUpgradeFilter;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedFilter;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Special {@link FilterHolder} to handle OSGi specific lifecycle related to
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}.
 */
public class PaxWebFilterHolder extends FilterHolder {

	public static final Logger LOG = LoggerFactory.getLogger(PaxWebFilterHolder.class);

	private final FilterModel filterModel;

	private ServiceReference<Filter> filterReference;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard filter */
	private final OsgiScopedServletContext servletContext;

	// mappings remembered for the holder
	private List<PaxWebFilterMapping> mapping;

	private ServiceObjects<Filter> serviceObjects;

	private boolean whiteboardTCCL;

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

		if (filterModel != null) {
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

		// setup proper delegation for ServletContext
		if (filterModel != null) {
			servletContext = new OsgiScopedServletContext(this.osgiServletContext, filterModel.getRegisteringBundle());
		} else {
			servletContext = null;
		}
	}

	public PaxWebFilterHolder(FilterHolder holder, OsgiServletContext defaultServletContext) {
		this.filterModel = null;
		this.osgiServletContext = defaultServletContext;
		this.servletContext = null;

		setName(holder.getName());
		setAsyncSupported(holder.isAsyncSupported());
		setInitParameters(holder.getInitParameters());
		try {
			// unfortunately, if I want to keep an array of PaxWebFilterHolders, I have to do the reflection here...
			Method getInstance = BaseHolder.class.getDeclaredMethod("getInstance");
			getInstance.setAccessible(true);
			setFilter((Filter) getInstance.invoke(holder));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			LOG.error("Can't manage dynamic filter: {}", e.getMessage(), e);
		}
		setClassName(holder.getClassName());
		setDisplayName(holder.getDisplayName());
		setHeldClass(holder.getHeldClass());
		setServletHandler(holder.getServletHandler());
	}

	@Override
	public void doStart() throws Exception {
		if (filterReference != null) {
			setHeldClass(Filter.class);
		} else if (filterModel != null && filterModel.getElementSupplier() != null) {
			setHeldClass(filterModel.getElementSupplier().get().getClass());
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
		if (instance == null && filterReference != null) {
			BundleContext context = filterModel.getRegisteringBundle().getBundleContext();
			if (context != null) {
				if (!filterModel.isPrototype()) {
					instance = context.getService(filterReference);
				} else {
					serviceObjects = context.getServiceObjects(filterReference);
					instance = serviceObjects.getService();
				}
			}
		}
		if (instance == null && filterModel.getElementSupplier() != null) {
			instance = filterModel.getElementSupplier().get();
		}

		if (instance == null && getHeldClass() != null) {
			// case of org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter added by Jetty's SCI
			Class<? extends Filter> heldClass = getHeldClass();
			try {
				Constructor<? extends Filter> ctr = heldClass.getConstructor();
				instance = ctr.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Can't instantiate Filter with class " + heldClass, e);
			}
		}

		if (instance == null) {
			filterModel.setDtoFailureCode(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
		}

		if (instance != null && "org.eclipse.jetty.websocket.servlet.WebSocketUpgradeFilter".equals(instance.getClass().getName())) {
			// very special handling - this filter (in org.eclipse.jetty.websocket.core.server.internal.CreatorNegotiator.negotiate())
			// casts request's ServletContext to org.eclipse.jetty.server.handler.ContextHandler.Context
			// it has changed when upgrading from Jetty 9 to Jetty 10
			instance = new PaxWebWebSocketUpgradeFilter(instance);
		}

		return instance == null ? null
				: new OsgiInitializedFilter(instance, filterModel, servletContext == null ? osgiServletContext : servletContext,
				whiteboardTCCL);
	}

	@Override
	public void destroyInstance(Object o) {
		super.destroyInstance(o);
		if (filterModel != null && filterReference != null) {
			if (!filterModel.isPrototype()) {
				BundleContext context = filterModel.getRegisteringBundle().getBundleContext();
				if (context != null) {
					context.ungetService(filterReference);
				}
			} else {
				Filter realFilter = (Filter) o;
				if (realFilter instanceof FilterHolder.Wrapper) {
					realFilter = ((FilterHolder.Wrapper) realFilter).getWrapped();
				}
				if (realFilter instanceof OsgiInitializedFilter) {
					realFilter = ((OsgiInitializedFilter) realFilter).getDelegate();
				}
				serviceObjects.ungetService(realFilter);
			}
		}
		if (filterModel != null && filterModel.getRegisteringBundle() != null) {
			servletContext.releaseWebContainerContext(filterModel.getRegisteringBundle());
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (isStarted()) {
			super.doFilter(request, response, chain);
		} else {
			String name = getName();
			if (name == null) {
				name = getClassName();
			}
			if (name == null) {
				name = this.toString();
			}
			throw new UnavailableException("Filter " + name + " is unavailable");
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
		return filterModel == null || filterModel.getContextModels().contains(targetContext);
	}

	public FilterModel getFilterModel() {
		return filterModel;
	}

	public void setMapping(List<PaxWebFilterMapping> mapping) {
		this.mapping = mapping;
	}

	public List<PaxWebFilterMapping> getMapping() {
		return mapping;
	}

	public void setWhiteboardTCCL(boolean whiteboardTCCL) {
		this.whiteboardTCCL = whiteboardTCCL;
	}

}
