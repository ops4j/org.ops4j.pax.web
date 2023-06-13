/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import org.ops4j.pax.web.service.whiteboard.FilterMapping;


/**
 * Default implementation of {@link FilterMapping}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class DefaultFilterMapping extends AbstractContextRelated implements FilterMapping {

	private Filter filter;
	private Class<? extends Filter> filterClass;
	private String filterName;
	private String[] urlPatterns = new String[0];
	private String[] regexPatterns = new String[0];
	private String[] servletNames = new String[0];
	private DispatcherType[] dispatcherTypes = new DispatcherType[] { DispatcherType.REQUEST };
	private Boolean asyncSupported;
	private Map<String, String> initParameters = new HashMap<>();

	@Override
	public Filter getFilter() {
		return filter;
	}

	@Override
	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

	@Override
	public String getFilterName() {
		return filterName;
	}

	@Override
	public String[] getUrlPatterns() {
		return urlPatterns;
	}

	@Override
	public String[] getRegexPatterns() {
		return regexPatterns;
	}

	@Override
	public String[] getServletNames() {
		return servletNames;
	}

	@Override
	public DispatcherType[] getDispatcherTypes() {
		return dispatcherTypes;
	}

	@Override
	public Boolean getAsyncSupported() {
		return asyncSupported;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return initParameters;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public void setFilterClass(Class<? extends Filter> filterClass) {
		this.filterClass = filterClass;
	}

	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}

	public void setUrlPatterns(String[] urlPatterns) {
		this.urlPatterns = urlPatterns;
	}

	public void setRegexPatterns(String[] regexPatterns) {
		this.regexPatterns = regexPatterns;
	}

	public void setServletNames(String[] servletNames) {
		this.servletNames = servletNames;
	}

	public void setDispatcherTypes(DispatcherType[] dispatcherTypes) {
		this.dispatcherTypes = dispatcherTypes;
	}

	public void setAsyncSupported(Boolean asyncSupported) {
		this.asyncSupported = asyncSupported;
	}

	public void setInitParameters(Map<String, String> initParameters) {
		this.initParameters = initParameters;
	}

	@Override
	public String toString() {
		return "DefaultFilterMapping{"
				+ "filter=" + filter
				+ ", filterClass=" + filterClass
				+ ", filterName='" + filterName + '\''
				+ ", urlPatterns=" + Arrays.toString(urlPatterns)
				+ ", regexPatterns=" + Arrays.toString(regexPatterns)
				+ ", servletNames=" + Arrays.toString(servletNames)
				+ ", dispatcherTypes=" + Arrays.toString(dispatcherTypes)
				+ ", asyncSupported=" + asyncSupported
				+ ", initParameters=" + initParameters
				+ ", contextSelectFilter='" + contextSelectFilter + '\''
				+ ", contextId='" + contextId + '\''
				+ '}';
	}

}
