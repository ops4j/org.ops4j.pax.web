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

import java.util.LinkedList;
import java.util.List;
import javax.servlet.FilterConfig;

import org.apache.catalina.Container;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;

/**
 * <p>Highly Tomcat-aware version of {@link StandardContext} that's needed when determining filters for the chain
 * for specific target servlet. The associated filters should be filtered by Servlet's
 * {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}</p>
 *
 * <p>The methods from super class are <strong>not</strong> working correctly, because this class doesn't do
 * proper delegation. Only implemented methods are correct.</p>
 *
 * <p>New instance of this cheap class is created in every call to
 * {@link PaxWebStandardWrapperValve#invoke(Request, Response)}.</p>
 */
public class PaxWebScopedStandardContext extends StandardContext {

	private final PaxWebStandardWrapper wrapper;
	private final PaxWebStandardContext delegate;

	public PaxWebScopedStandardContext(PaxWebStandardWrapper paxWebStandardWrapper, PaxWebStandardContext delegate) {
		this.wrapper = paxWebStandardWrapper;
		this.delegate = delegate;
	}

	@Override
	public FilterMap[] findFilterMaps() {
		FilterMap[] maps = delegate.findFilterMaps();

		OsgiContextModel targetContext = wrapper.getOsgiContextModel();
		if (targetContext == null) {
			targetContext = delegate.getDefaultOsgiContextModel();
		}

		List<FilterMap> osgiScopedFilters = new LinkedList<>();

		for (FilterMap filter : maps) {
			PaxWebFilterMap fDef = (PaxWebFilterMap) filter;
			if (fDef.isInitial() || fDef.getFilterModel().getContextModels().contains(targetContext)) {
				osgiScopedFilters.add(filter);
			}
		}

		// the caller will further narrow the list for given target servlet's name / request URI
		return osgiScopedFilters.toArray(new FilterMap[0]);
	}

	// that's a bit tricky - I've delegated only these methods that I found necessary (the more tests, the better)

	@Override
	public FilterDef findFilterDef(String filterName) {
		return delegate.findFilterDef(filterName);
	}

	@Override
	public FilterConfig findFilterConfig(String name) {
		return delegate.findFilterConfig(name);
	}

	@Override
	public LifecycleState getState() {
		return delegate.getState();
	}

	@Override
	public Container getParent() {
		return delegate.getParent();
	}

	@Override
	public String getPath() {
		return delegate.getPath();
	}

	// equals/hashCode have to be delegated to make Tomcat's mapper work in INCLUDE dispatcher

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

}
