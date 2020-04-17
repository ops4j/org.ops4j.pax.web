/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.util.Path;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Set of parameters describing everything that's required to register a {@link Filter}.
 */
public class FilterModel extends ElementModel<Filter> {

	/**
	 * <p>URL patterns as specified by:<ul>
	 *     <li>Pax Web specific extensions to {@link org.osgi.service.http.HttpService}</li>
	 *     <li>Whiteboard Service specification</li>
	 *     <li>Servlet API specification</li>
	 * </ul></p>
	 */
	private String[] urlPatterns;

	/** Servlet names for filter mapping. {@code <filter-mapping>/<servlet-name>} */
	private String[] servletNames;

	/** Regex mapping from OSGi Whiteboard Service specification. Not available in Servet specification. */
	private String[] regexMapping;

	/** {@link DispatcherType Dispatcher types} for filter mapping. {@code <filter-mapping>/<dispatcher>} */
	private String[] dispatcherTypes;

	/** Filter name that defaults to FQCN of the {@link Filter}. {@code <filter>/<filter-name>} */
	private final String name;

	/**
	 * Init parameters of the filter as specified by {@link FilterConfig#getInitParameterNames()} and
	 * {@code <filter>/<init-param>} elements in {@code web.xml}.
	 */
	private Map<String, String> initParams;

	/** {@code <filter>/<async-supported>} */
	private Boolean asyncSupported;

	/**
	 * Both Http Service and Whiteboard service allows registration of filters using existing instance.
	 */
	private final Filter filter;

	/**
	 * Actual class of the filer, to be instantiated by servlet container itself. {@code <filter>/<filter-class>}.
	 * This can only be set when registering Pax Web specific
	 * {@link org.ops4j.pax.web.service.whiteboard.FilterMapping} "direct Whiteboard" service.
	 */
	private final Class<? extends Filter> filterClass;

	/**
	 * Constructor used for filter unregistration
	 * @param filterName
	 * @param filter
	 * @param filterClass
	 * @param reference
	 */
	public FilterModel(String filterName, Filter filter,
			Class<? extends Filter> filterClass, ServiceReference<? extends Filter> reference) {
		this.name = filterName;
		this.filter = filter;
		this.filterClass = filterClass;
		this.setElementReference(reference);
	}

	public FilterModel(String filterName, String[] urlPatterns, String[] servletNames, String[] regexMapping,
			Filter filter, Dictionary<String, String> initParams, Boolean asyncSupported) {
		this(urlPatterns, servletNames, regexMapping, null,
				filterName, Utils.toMap(initParams), asyncSupported, filter, null, null);
	}

	public FilterModel(String filterName, String[] urlPatterns, String[] servletNames, String[] regexMapping,
			Class<? extends Filter> filterClass, Dictionary<String, String> initParams, Boolean asyncSupported) {
		this(urlPatterns, servletNames, regexMapping, null,
				filterName, Utils.toMap(initParams), asyncSupported, null, filterClass, null);
	}

	@SuppressWarnings("deprecation")
	private FilterModel(String[] urlPatterns, String[] servletNames, String[] regexMapping, String[] dispatcherTypes,
			String name, Map<String, String> initParams, Boolean asyncSupported, Filter filter,
			Class<? extends Filter> filterClass, ServiceReference<? extends Filter> reference) {

		this.urlPatterns = Path.normalizePatterns(urlPatterns);
		this.servletNames = servletNames != null ? Arrays.copyOf(servletNames, servletNames.length) : null;
		this.regexMapping = regexMapping != null ? Arrays.copyOf(regexMapping, regexMapping.length) : null;

		this.initParams = initParams == null ? Collections.emptyMap() : initParams;
		this.asyncSupported = asyncSupported;
		this.filter = filter;
		this.filterClass = filterClass;
		setElementReference(reference);

		if (name == null) {
			// legacy method first
			name = this.initParams.get(PaxWebConstants.INIT_PARAM_FILTER_NAME);
			this.initParams.remove(PaxWebConstants.INIT_PARAM_FILTER_NAME);
		}
		if (name == null) {
			// Whiteboard Specification 140.5 Registering Servlet Filters
			Class<? extends Filter> c = getActualClass();
			if (c != null) {
				name = c.getName();
			}
		}
		if (name == null) {
			// no idea how to obtain the class, but this should not happen
			name = UUID.randomUUID().toString();
		}
		this.name = name;

		this.dispatcherTypes = dispatcherTypes;
		if (this.dispatcherTypes == null || this.dispatcherTypes.length == 0) {
			// legacy method to get filter dispatcher types
			String dispatchers = this.initParams.remove(PaxWebConstants.FILTER_MAPPING_DISPATCHER);
			if (dispatchers != null) {
				String[] types = dispatchers.split("\\s*,\\s*");
				this.dispatcherTypes = new String[types.length];
				int i = 0;
				for (String t : types) {
					this.dispatcherTypes[i++] = DispatcherType.valueOf(t.toUpperCase()).name();
				}
			} else {
				this.dispatcherTypes = new String[] { DispatcherType.REQUEST.name() };
			}
		}
	}

	@Override
	public Boolean performValidation() {
		int sources = 0;
		sources += (filter != null ? 1 : 0);
		sources += (filterClass != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		if (sources == 0) {
			throw new IllegalArgumentException("Filter Model must specify one of: filter instance, filter class"
					+ " or service reference");
		}
		if (sources != 1) {
			throw new IllegalArgumentException("Filter Model should specify a filter uniquely as instance, class"
					+ " or service reference");
		}

		sources = 0;
		sources += (this.servletNames != null && this.servletNames.length > 0 ? 1 : 0);
		sources += (this.urlPatterns != null && this.urlPatterns.length > 0 ? 1 : 0);
		sources += (this.regexMapping != null && this.regexMapping.length > 0 ? 1 : 0);

		if (sources == 0) {
			throw new IllegalArgumentException("Please specify one of: servlet name mapping, url pattern mapping"
					+ " or regex mapping");
		}

		return Boolean.TRUE;
	}

	@Override
	public int compareTo(ElementModel<Filter> o) {
		int superCompare = super.compareTo(o);
		if (superCompare == 0 && o instanceof FilterModel) {
			// this happens in non-Whiteboard scenario
			return this.name.compareTo(((FilterModel)o).name);
		}
		return superCompare;
	}

	@Override
	public String toString() {
		return "FilterModel{id=" + getId()
				+ ",name='" + name + "'"
				+ (urlPatterns == null ? "" : ",urlPatterns=" + Arrays.toString(urlPatterns))
				+ (servletNames == null ? "" : ",servletNames=" + Arrays.toString(servletNames))
				+ (regexMapping == null ? "" : ",regexMapping=" + Arrays.toString(regexMapping))
				+ (filter == null ? "" : ",filter=" + filter)
				+ (filterClass == null ? "" : ",filterClass=" + filterClass)
				+ ",contexts=" + contextModels
				+ "}";
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String[] getServletNames() {
		return servletNames;
	}

	public String[] getRegexMapping() {
		return regexMapping;
	}

	public void setDispatcherTypes(String[] dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

	public String[] getDispatcherTypes() {
		return dispatcherTypes;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	public Filter getFilter() {
		return filter;
	}

	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

	/**
	 * Returns a {@link Class} of the filter whether it is registered as instance, class or reference.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Filter> getActualClass() {
		if (this.filterClass != null) {
			return this.filterClass;
		} else if (this.filter != null) {
			return this.filter.getClass();
		}
		if (getElementReference() != null) {
			String className = Utils.getFirstObjectClass(getElementReference());
			if (className != null) {
				try {
					return (Class<? extends Filter>) getRegisteringBundle().loadClass(className);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Can't load a class for the filter: " + e.getMessage(), e);
				}
			} else {
				// sane default, accepted by Undertow - especially if it has instance factory
				return Filter.class;
			}
		}

		return null; // even if it can't happen
	}

	public static class Builder {

		private String[] urlPatterns;
		private String[] servletNames;
		private String[] regexMapping;
		private String[] dispatcherTypes;
		private String filterName;
		private Map<String, String> initParams;
		private Boolean asyncSupported;
		private Filter filter;
		private Class<? extends Filter> filterClass;
		private ServiceReference<? extends Filter> reference;
		private Supplier<? extends Filter> supplier;
		private final List<OsgiContextModel> list = new LinkedList<>();
		private Bundle bundle;
		private int rank;
		private long serviceId;

		public Builder() {
		}

		public Builder(String filterName) {
			this.filterName = filterName;
		}

		public FilterModel.Builder withUrlPatterns(String[] urlPatterns) {
			this.urlPatterns = urlPatterns;
			return this;
		}

		public FilterModel.Builder withServletNames(String[] servletNames) {
			this.servletNames = servletNames;
			return this;
		}

		public FilterModel.Builder withRegexMapping(String[] regexMapping) {
			this.regexMapping = regexMapping;
			return this;
		}

		public FilterModel.Builder withDispatcherTypes(String[] dispatcherTypes) {
			this.dispatcherTypes = dispatcherTypes;
			return this;
		}

		public FilterModel.Builder withFilterName(String filterName) {
			this.filterName = filterName;
			return this;
		}

		public FilterModel.Builder withInitParams(Map<String, String> initParams) {
			this.initParams = initParams;
			return this;
		}

		public FilterModel.Builder withAsyncSupported(Boolean asyncSupported) {
			this.asyncSupported = asyncSupported;
			return this;
		}

		public FilterModel.Builder withFilter(Filter filter) {
			this.filter = filter;
			return this;
		}

		public FilterModel.Builder withFilterClass(Class<? extends Filter> filterClass) {
			this.filterClass = filterClass;
			return this;
		}

		public FilterModel.Builder withFilterReference(ServiceReference<? extends Filter> reference) {
			this.reference = reference;
			return this;
		}

		public FilterModel.Builder withFilterReference(Bundle bundle, ServiceReference<? extends Filter> reference) {
			this.bundle = bundle;
			this.reference = reference;
			return this;
		}

		public FilterModel.Builder withFilterSupplier(Supplier<? extends Filter> supplier) {
			this.supplier = supplier;
			return this;
		}

		public FilterModel.Builder withOsgiContextModel(OsgiContextModel osgiContextModel) {
			this.list.add(osgiContextModel);
			return this;
		}

		public FilterModel.Builder withOsgiContextModels(final Collection<OsgiContextModel> osgiContextModels) {
			this.list.addAll(osgiContextModels);
			return this;
		}

		public FilterModel.Builder withRegisteringBundle(Bundle bundle) {
			this.bundle = bundle;
			return this;
		}

		public FilterModel.Builder withServiceRankAndId(int rank, long id) {
			this.rank = rank;
			this.serviceId = id;
			return this;
		}

		public FilterModel build() {
			FilterModel model = new FilterModel(urlPatterns, servletNames, regexMapping, dispatcherTypes,
					filterName, initParams, asyncSupported, filter, filterClass, reference);
			list.forEach(model::addContextModel);
			model.setRegisteringBundle(this.bundle);
			model.setServiceRank(this.rank);
			model.setServiceId(this.serviceId);
			model.setElementSupplier(this.supplier);
			return model;
		}
	}

}
