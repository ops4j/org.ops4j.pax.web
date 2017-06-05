/* Copyright 2007 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.util.ConversionUtil;
import org.ops4j.pax.web.service.spi.util.Path;

public class FilterModel extends Model {

	private final Filter filter;
	private final String[] urlPatterns;
	private final String[] servletNames;
	private final Map<String, String> initParams;
	private final String name;
	private final Set<String> dispatcher = new HashSet<>();
	private final Class<? extends Filter> filterClass;
	private final boolean asyncSupported;

	public FilterModel(final ContextModel contextModel, final Filter filter,
					   final String[] urlPatterns, final String[] servletNames,
					   final Dictionary<String, ?> initParameter, final boolean asyncSupported) {
		this(contextModel, filter, null, urlPatterns, servletNames, initParameter, asyncSupported);
	}

	public FilterModel(final ContextModel contextModel, final Class<? extends Filter> filterClass,
					   final String[] urlPatterns, final String[] servletNames,
					   final Dictionary<String, ?> initParameter, final boolean asyncSupported) {
		this(contextModel, null, filterClass, urlPatterns, servletNames, initParameter, asyncSupported);
	}

	public FilterModel(final ContextModel contextModel, final Filter filter,
					   final Class<? extends Filter> filterClass,
					   final String[] urlPatterns, final String[] servletNames,
					   final Dictionary<String, ?> initParameter,
					   final boolean asyncSupported) {
		super(contextModel);
		if (filterClass == null) {
			NullArgumentException.validateNotNull(filter, "Filter");
		}
		if (filter == null) {
			NullArgumentException.validateNotNull(filterClass, "FilterClass");
		}

		if (urlPatterns == null && servletNames == null) {
			throw new IllegalArgumentException(
					"Registered filter must have at least one url pattern or servlet name mapping");
		}

		this.filter = filter;
		this.filterClass = filterClass;
		if (urlPatterns != null) {
			this.urlPatterns = Path.normalizePatterns(Arrays.copyOf(urlPatterns, urlPatterns.length));
		} else {
			this.urlPatterns = null;
		}
		if (servletNames != null) {
			this.servletNames = Arrays.copyOf(servletNames, servletNames.length);
		} else {
			this.servletNames = null;
		}

		this.initParams = ConversionUtil.convertToMap(initParameter);
		String idName = initParams.get(WebContainerConstants.FILTER_NAME);
		if (idName == null) {
			idName = getId();
		}
		this.name = idName;
		this.asyncSupported = asyncSupported;
		setupDispatcher();
	}

	/*

     */
	private void setupDispatcher() {
		String dispatches = initParams
				.get(WebContainerConstants.FILTER_MAPPING_DISPATCHER);
		if (dispatches != null && dispatches.trim().length() > 0) {
			if (dispatches.indexOf(",") > -1) {
				// parse
				StringTokenizer tok = new StringTokenizer(dispatches.trim(),
						",");
				while (tok.hasMoreTokens()) {
					String element = tok.nextToken();
					if (element != null && element.trim().length() > 0) {
						dispatcher.add(DispatcherType.valueOf(element.trim().toUpperCase()).name());
					}
				}
			} else {
				dispatcher.add(DispatcherType.valueOf(dispatches.trim().toUpperCase()).name());
			}
		}
	}

	public Filter getFilter() {
		return filter;
	}

	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

	public String getName() {
		return name;
	}

	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	public String[] getServletNames() {
		return servletNames;
	}

	public Map<String, String> getInitParams() {
		return initParams;
	}

	public String[] getDispatcher() {
		return dispatcher.toArray(new String[dispatcher.size()]);
	}

	public boolean isAsyncSupported() {
		return asyncSupported;
	}

	/*
	 * From web app XSD:
	 * The logical name of the filter is declare by using filter-nameType. This name is used to map the
	 * filter. Each filter name is unique within the web application.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FilterModel that = (FilterModel) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(getContextModel().getContextName(), that.getContextModel().getContextName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, getContextModel().getContextName());
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("id=").append(getId())
				.append(",urlPatterns=").append(Arrays.toString(urlPatterns))
				.append(",servletNames=").append(Arrays.toString(servletNames))
				.append(",filter=").append(filter)
				.append(",filterClass=").append(filterClass)
				.append(",context=").append(getContextModel()).append("}").toString();
	}

}
