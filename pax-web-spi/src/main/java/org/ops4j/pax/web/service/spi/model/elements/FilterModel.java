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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.FilterEventData;
import org.ops4j.pax.web.service.spi.util.Path;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Set of parameters describing everything that's required to register a {@link Filter}.
 */
public class FilterModel extends ElementModel<Filter, FilterEventData> {

	/**
	 * <p>URL patterns and servlet names (per dispatcher type) as specified by:<ul>
	 *     <li>Pax Web specific extensions to {@link org.osgi.service.http.HttpService}</li>
	 *     <li>Whiteboard Service specification</li>
	 *     <li>Servlet API specification</li>
	 * </ul></p>
	 *
	 * <p>Additionally we store regex mappings here</p>
	 */
	private final List<Mapping> mappingsPerDispatcherTypes = new LinkedList<>();

	/* These 3 fields contain flat collections of mappings */

	private String[] flatUrlPatterns;
	private String[] flatServletNames;
	private String[] flatRegexPatterns;

	/**
	 * When using {@link javax.servlet.ServletContext#addFilter(String, Filter)} and
	 * {@link javax.servlet.FilterRegistration.Dynamic#addMappingForServletNames(EnumSet, boolean, String...)} we
	 * need to store distinct sets of mappings separately for different dispatchers (and order)
	 */
	private final List<DynamicMapping> dynamicUrlPatterns = new LinkedList<>();

	private final List<DynamicMapping> dynamicServletNames = new LinkedList<>();

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
	 * Flag used for models registered using {@link javax.servlet.ServletContext#addFilter}
	 */
	private boolean dynamic = false;

	/** Flag to mark a {@link FilterModel} for {@link org.osgi.service.http.whiteboard.Preprocessor} */
	private boolean preprocessor = false;

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

	public FilterModel(String filterName, String[] urlPatterns, String[] servletNames, String[] regexPatterns,
			Filter filter, Dictionary<String, String> initParams, Boolean asyncSupported) {
		this(urlPatterns, servletNames, regexPatterns, null,
				filterName, Utils.toMap(initParams), asyncSupported, filter, null, null, null, null);
	}

	public FilterModel(String filterName, String[] urlPatterns, String[] servletNames, String[] regexPatterns,
			Class<? extends Filter> filterClass, Dictionary<String, String> initParams, Boolean asyncSupported) {
		this(urlPatterns, servletNames, regexPatterns, null,
				filterName, Utils.toMap(initParams), asyncSupported, null, filterClass, null, null, null);
	}

	@SuppressWarnings("deprecation")
	private FilterModel(String[] urlPatterns, String[] servletNames, String[] regexPatterns, String[] dispatcherTypes,
			String name, Map<String, String> initParams, Boolean asyncSupported, Filter filter,
			Class<? extends Filter> filterClass, ServiceReference<? extends Filter> reference,
			Supplier<? extends Filter> supplier, Bundle registeringBundle) {

		DispatcherType[] dts = new DispatcherType[dispatcherTypes == null ? 0 : dispatcherTypes.length];
		if (dts.length > 0) {
			for (int i = 0; i < dispatcherTypes.length; i++) {
				dts[i] = DispatcherType.valueOf(dispatcherTypes[i].toUpperCase(Locale.ROOT));
			}
		}
		if (dts.length == 0 && initParams != null) {
			// legacy method to get filter dispatcher types
			String dispatchers = initParams.remove(PaxWebConstants.INIT_PARAM_FILTER_MAPPING_DISPATCHER);
			if (dispatchers != null) {
				String[] types = dispatchers.split("\\s*,\\s*");
				dts = new DispatcherType[types.length];
				int i = 0;
				for (String t : types) {
					dts[i++] = DispatcherType.valueOf(t.toUpperCase());
				}
			} else {
				dts = new DispatcherType[] { DispatcherType.REQUEST };
			}
		}

		// this constructor allows only single <set of dispatcher types> -> <mapping>
		Mapping map = new Mapping();
		map.dispatcherTypes = dts;
		map.urlPatterns = urlPatterns != null ? Path.normalizePatterns(urlPatterns) : null;
		map.servletNames = servletNames != null ? Arrays.copyOf(servletNames, servletNames.length) : null;
		map.regexPatterns = regexPatterns != null ? Arrays.copyOf(regexPatterns, regexPatterns.length) : null;
		if (!(map.urlPatterns == null && map.servletNames == null && map.regexPatterns == null)) {
			this.mappingsPerDispatcherTypes.add(map);
		}

		this.initParams = initParams == null ? Collections.emptyMap() : new LinkedHashMap<>(initParams);
		this.asyncSupported = asyncSupported;
		this.filter = filter;
		this.filterClass = filterClass;
		setElementReference(reference);
		setElementSupplier(supplier);
		setRegisteringBundle(registeringBundle);

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
	}

	@Override
	public Boolean performValidation() {
		int sources = 0;
		sources += (filter != null ? 1 : 0);
		sources += (filterClass != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		sources += (getElementSupplier() != null ? 1 : 0);
		if (sources == 0) {
			throw new IllegalArgumentException("Filter Model must specify one of: filter instance, filter class"
					+ " or service reference");
		}
		if (sources != 1) {
			throw new IllegalArgumentException("Filter Model should specify a filter uniquely as instance, class"
					+ " or service reference");
		}

		if (preprocessor) {
			// tweak the mapping
			dynamicServletNames.clear();
			dynamicUrlPatterns.clear();
			this.mappingsPerDispatcherTypes.clear();
			Mapping mapping = new Mapping();
			mapping.setDispatcherTypes(new DispatcherType[] {
					DispatcherType.ERROR,
					DispatcherType.FORWARD,
					DispatcherType.INCLUDE,
					DispatcherType.REQUEST,
					DispatcherType.ASYNC
			});
			mapping.setUrlPatterns(new String[] { "/*" });
			this.mappingsPerDispatcherTypes.add(mapping);
		}

		if (!dynamic) {
			for (Mapping map : mappingsPerDispatcherTypes) {
				sources = 0;
				sources += (map.servletNames != null && map.servletNames.length > 0 ? 1 : 0);
				sources += (map.urlPatterns != null && map.urlPatterns.length > 0 ? 1 : 0);
				sources += (map.regexPatterns != null && map.regexPatterns.length > 0 ? 1 : 0);

				if (sources == 0) {
					throw new IllegalArgumentException("Please specify one of: servlet name mapping, url pattern mapping"
							+ " or regex mapping");
				}
			}
		}

		if (dynamic) {
			sources = 0;
			sources += (this.dynamicUrlPatterns != null && this.dynamicUrlPatterns.size() > 0 ? 1 : 0);
			sources += (this.dynamicServletNames != null && this.dynamicServletNames.size() > 0 ? 1 : 0);
			if (sources == 0) {
				throw new IllegalArgumentException("For dynamic filter registration, please specify one of:"
						+ " servlet name mapping or url pattern mapping");
			}
		}

		// no more mapping changes expected, so we can flatten url mappings, servlet names
		// and regexp mappings for toString() purposes
		List<String> up = new ArrayList<>();
		List<String> sn = new ArrayList<>();
		List<String> rp = new ArrayList<>();
		for (Mapping map : mappingsPerDispatcherTypes) {
			if (map.urlPatterns != null) {
				up.addAll(Arrays.asList(map.urlPatterns));
			}
			if (map.servletNames != null) {
				sn.addAll(Arrays.asList(map.servletNames));
			}
			if (map.regexPatterns != null) {
				rp.addAll(Arrays.asList(map.regexPatterns));
			}
		}
		if (!up.isEmpty()) {
			this.flatUrlPatterns = up.toArray(new String[0]);
		}
		if (!sn.isEmpty()) {
			this.flatServletNames = sn.toArray(new String[0]);
		}
		if (!rp.isEmpty()) {
			this.flatRegexPatterns = rp.toArray(new String[0]);
		}

		return Boolean.TRUE;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerFilter(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterFilter(this);
	}

	@Override
	public FilterEventData asEventData() {
		FilterEventData data = new FilterEventData(name, mappingsPerDispatcherTypes, filter, filterClass);
		setCommonEventProperties(data);
		return data;
	}

	@Override
	public int compareTo(ElementModel<Filter, FilterEventData> o) {
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
				+ (preprocessor ? ",preprocessor" : "")
				+ (flatUrlPatterns == null ? "" : ",urlPatterns=" + Arrays.toString(flatUrlPatterns))
				+ (flatServletNames == null ? "" : ",servletNames=" + Arrays.toString(flatServletNames))
				+ (flatRegexPatterns == null ? "" : ",regexPatterns=" + Arrays.toString(flatRegexPatterns))
				+ (filter == null ? "" : ",filter=" + filter)
				+ (filterClass == null ? "" : ",filterClass=" + filterClass)
				+ ",contexts=" + getContextModelsInfo()
				+ "}";
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

	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public boolean isPreprocessor() {
		return preprocessor;
	}

	public void setPreprocessor(boolean preprocessor) {
		this.preprocessor = preprocessor;
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
	public Class<? extends Filter> getActualClass() {
		if (this.filterClass != null) {
			return this.filterClass;
		} else if (this.filter != null) {
			return this.filter.getClass();
		} else if (this.getElementSupplier() != null) {
			Filter s = getElementSupplier().get();
			return s.getClass();
		}
		if (getElementReference() != null) {
			// TOUNGET:
			Filter f = getRegisteringBundle().getBundleContext().getService(getElementReference());
			if (f != null) {
				try {
					return f.getClass();
				} finally {
					// TOUNGET:
					getRegisteringBundle().getBundleContext().ungetService(getElementReference());
				}
			} else {
				// sane default, accepted by Undertow - especially if it has instance factory
				return Filter.class;
			}
		}

		return null; // even if it can't happen
	}

	public List<Mapping> getMappingsPerDispatcherTypes() {
		return mappingsPerDispatcherTypes;
	}

	public List<DynamicMapping> getDynamicUrlPatterns() {
		return dynamicUrlPatterns;
	}

	public List<DynamicMapping> getDynamicServletNames() {
		return dynamicServletNames;
	}

	public void addDynamicServletNameMapping(EnumSet<DispatcherType> dispatcherTypes, String[] servletNames, boolean isMatchAfter) {
		this.dynamicServletNames.add(DynamicMapping.forServletNames(dispatcherTypes, servletNames, isMatchAfter));
	}

	public void addDynamicUrlPatternMapping(EnumSet<DispatcherType> dispatcherTypes, String[] urlPatterns, boolean isMatchAfter) {
		this.dynamicUrlPatterns.add(DynamicMapping.forUrlPatterns(dispatcherTypes, urlPatterns, isMatchAfter));
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * This method should be used by actual runtime to obtain an instance of the {@link Filter}.
	 * TOUNGET: do proper service unget if needed!
	 * @return
	 */
	public Filter getInstance() {
		Filter instance = null;
		// obtain Filter using reference
		ServiceReference<? extends Filter> ref = getElementReference();
		if (ref != null) {
			// TOUNGET:
			instance =  getRegisteringBundle().getBundleContext().getService(ref);
		}
		if (instance == null && filterClass != null) {
			try {
				instance = filterClass.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException("Can't instantiate Filter with class " + filterClass, e);
			}
		}
		if (instance == null && getElementSupplier() != null) {
			instance = getElementSupplier().get();
		}

		return instance;
	}

	public static class Builder {

		private String[] urlPatterns;
		private String[] servletNames;
		private String[] regexPatterns;
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
		private boolean preprocessor = false;

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

		public FilterModel.Builder withRegexMapping(String[] regexPatterns) {
			this.regexPatterns = regexPatterns;
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

		public Builder isPreprocessor(boolean preprocessor) {
			this.preprocessor = preprocessor;
			return this;
		}

		public FilterModel build() {
			FilterModel model = new FilterModel(urlPatterns, servletNames, regexPatterns, dispatcherTypes,
					filterName, initParams, asyncSupported, filter, filterClass, reference, supplier,
					bundle);
			list.forEach(model::addContextModel);
			model.setServiceRank(this.rank);
			model.setServiceId(this.serviceId);
			model.setPreprocessor(preprocessor);
			return model;
		}
	}

	public static class Mapping {
		protected DispatcherType[] dispatcherTypes;
		protected String[] servletNames;
		protected String[] urlPatterns;
		protected String[] regexPatterns;

		public Mapping() {
		}

		public Mapping(String[] urlPatterns, String[] servletNames) {
			this.urlPatterns = urlPatterns;
			this.servletNames = servletNames;
		}

		public DispatcherType[] getDispatcherTypes() {
			return dispatcherTypes;
		}

		public void setDispatcherTypes(DispatcherType[] dispatcherTypes) {
			this.dispatcherTypes = dispatcherTypes;
		}

		public String[] getServletNames() {
			return servletNames;
		}

		public void setServletNames(String[] servletNames) {
			this.servletNames = servletNames;
		}

		public String[] getUrlPatterns() {
			return urlPatterns;
		}

		public void setUrlPatterns(String[] urlPatterns) {
			this.urlPatterns = urlPatterns;
		}

		public String[] getRegexPatterns() {
			return regexPatterns;
		}

		public void setRegexPatterns(String[] regexPatterns) {
			this.regexPatterns = regexPatterns;
		}
	}

	public static class DynamicMapping extends Mapping {
		protected boolean after;

		public static DynamicMapping forServletNames(EnumSet<DispatcherType> dispatcherTypes, String[] servletNames, boolean isMatchAfter) {
			DynamicMapping mapping = new DynamicMapping();
			mapping.after = isMatchAfter;
			if (dispatcherTypes != null) {
				mapping.dispatcherTypes = dispatcherTypes.toArray(new DispatcherType[0]);
			} else {
				mapping.dispatcherTypes = new DispatcherType[] { DispatcherType.REQUEST };
			}
			mapping.servletNames = servletNames;
			return mapping;
		}

		public static DynamicMapping forUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, String[] urlPatterns, boolean isMatchAfter) {
			DynamicMapping mapping = new DynamicMapping();
			mapping.after = isMatchAfter;
			if (dispatcherTypes != null) {
				mapping.dispatcherTypes = dispatcherTypes.toArray(new DispatcherType[0]);
			} else {
				mapping.dispatcherTypes = new DispatcherType[] { DispatcherType.REQUEST };
			}
			mapping.urlPatterns = urlPatterns;
			return mapping;
		}

		public boolean isAfter() {
			return after;
		}
	}

}
