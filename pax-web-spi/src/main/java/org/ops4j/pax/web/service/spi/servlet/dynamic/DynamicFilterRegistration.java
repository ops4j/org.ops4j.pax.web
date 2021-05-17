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
package org.ops4j.pax.web.service.spi.servlet.dynamic;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.servlet.DynamicRegistrations;

/**
 * Dynamic filter registration that passes Servlet API dynamic configuration of a filter to underlying
 * Pax-Web specific {@link FilterModel}.
 */
public class DynamicFilterRegistration implements FilterRegistration.Dynamic {

	private final FilterModel model;
	private final OsgiContextModel osgiContextModel;
	private final DynamicRegistrations registrations;

	public DynamicFilterRegistration(FilterModel model, OsgiContextModel osgiContextModel, DynamicRegistrations regs) {
		this.model = model;
		this.osgiContextModel = osgiContextModel;
		this.registrations = regs;
	}

	public FilterModel getModel() {
		return model;
	}

	@Override
	public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
		// we have easier task than in javax.servlet.ServletRegistration.addMapping(), because more servlets
		// may be mapped to single servlet by name or to an URL
		model.addDynamicServletNameMapping(dispatcherTypes, servletNames, isMatchAfter);
	}

	@Override
	public Collection<String> getServletNameMappings() {
		return model.getDynamicServletNames().stream()
				.map(FilterModel.Mapping::getServletNames)
				.flatMap(Arrays::stream)
				.collect(Collectors.toSet());
	}

	@Override
	public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
		model.addDynamicUrlPatternMapping(dispatcherTypes, urlPatterns, isMatchAfter);
	}

	@Override
	public Collection<String> getUrlPatternMappings() {
		return model.getDynamicUrlPatterns().stream()
				.map(FilterModel.Mapping::getUrlPatterns)
				.flatMap(Arrays::stream)
				.collect(Collectors.toSet());
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
		model.setAsyncSupported(isAsyncSupported);
	}

	@Override
	public String getName() {
		return model.getName();
	}

	@Override
	public String getClassName() {
		return model.getActualClass().getName();
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		if (model.getInitParams().containsKey(name)) {
			return false;
		}
		model.getInitParams().put(name, value);
		return true;
	}

	@Override
	public String getInitParameter(String name) {
		return model.getInitParams().get(name);
	}

	@Override
	public Set<String> setInitParameters(Map<String, String> initParameters) {
		Set<String> existingKeys = new LinkedHashSet<>(model.getInitParams().keySet());
		existingKeys.retainAll(initParameters.keySet());
		model.getInitParams().putAll(initParameters);
		return existingKeys;
	}

	@Override
	public Map<String, String> getInitParameters() {
		return model.getInitParams();
	}

}
