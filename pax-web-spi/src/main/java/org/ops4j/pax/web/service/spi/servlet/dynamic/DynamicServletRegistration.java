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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.DynamicRegistrations;

public class DynamicServletRegistration implements ServletRegistration.Dynamic {

	private final ServletModel model;
	private final OsgiContextModel osgiContextModel;
	private final ServletContextModel servletContextModel;

	private final DynamicRegistrations registrations;

	public DynamicServletRegistration(ServletModel model, OsgiContextModel osgiContextModel,
			ServletContextModel servletContextModel, DynamicRegistrations regs) {
		this.model = model;
		// "close" the context list
		this.model.getContextModels();
		this.osgiContextModel = osgiContextModel;
		this.servletContextModel = servletContextModel;
		this.registrations = regs;
	}

	public ServletModel getModel() {
		return model;
	}

	@Override
	public void setLoadOnStartup(int loadOnStartup) {
		model.setLoadOnStartup(loadOnStartup);
	}

	@Override
	public Set<String> setServletSecurity(ServletSecurityElement constraint) {
		// We won't support this
		return Collections.emptySet();
	}

	@Override
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		model.setMultipartConfigElement(multipartConfig);
	}

	@Override
	public void setRunAsRole(String roleName) {
		// We won't support this
	}

	@Override
	public void setAsyncSupported(boolean isAsyncSupported) {
		model.setAsyncSupported(isAsyncSupported);
	}

	@Override
	public Set<String> addMapping(String... urlPatterns) {
		// this method may be used (by ServletContainerInitializers) many times to build a list of mappings for a
		// servlet. However, each time we have to consult a list of mappings of ALL other existing servlets
		// including dynamic registrations added so far

		Set<String> conflicts = new LinkedHashSet<>();

		// from current ServletContextModel
		Set<String> existing = new HashSet<>(servletContextModel.getServletUrlPatternMapping().keySet());
		// from existing registrations (except current registration)
		registrations.getDynamicServletRegistrations().values().forEach(r -> {
			if (r != DynamicServletRegistration.this) {
				// tomcat additionally checks "if (wrapper.isOverridable())", so it's possible to alter "/" mapping
				existing.addAll(r.getMappings());
			}
		});

		for (String p : urlPatterns) {
			if (existing.contains(p)) {
				conflicts.add(p);
			}
		}

		if (conflicts.isEmpty()) {
			existing.clear();
			// previously added
			if (model.getUrlPatterns() != null) {
				existing.addAll(Arrays.asList(model.getUrlPatterns()));
			}
			// newly added
			existing.addAll(Arrays.asList(urlPatterns));
			model.setUrlPatterns(existing.toArray(new String[existing.toArray().length]));

			// no conflicts!
			return Collections.emptySet();
		}

		return conflicts;
	}

	@Override
	public Collection<String> getMappings() {
		return model.getUrlPatterns() == null ? Collections.emptyList()
				: Collections.unmodifiableList(Arrays.asList(model.getUrlPatterns()));
	}

	@Override
	public String getRunAsRole() {
		// We won't support this
		return null;
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
