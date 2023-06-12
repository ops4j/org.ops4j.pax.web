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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.HttpMethodConstraintElement;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.ServletSecurity;

import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
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
		Set<String> conflicts = new LinkedHashSet<>();

		List<SecurityConstraintModel> newConstraints = new ArrayList<>();

		for (String urlPattern : getMappings()) {
			boolean hasConflict = false;
			for (SecurityConstraintModel sc : osgiContextModel.getSecurityConfiguration().getSecurityConstraints()) {
				for (SecurityConstraintModel.WebResourceCollection col : sc.getWebResourceCollections()) {
					if (col.getPatterns().contains(urlPattern)) {
						// there's a conflict - web.xml (or fragments) already contains <security-constraint>
						// for given URL
						conflicts.add(urlPattern);
						hasConflict = true;
						break;
					}
				}
				if (hasConflict) {
					break;
				}
			}

			if (!hasConflict) {
				// we can add the constraint for single mapping of the dynamic servlet
				Collection<HttpMethodConstraintElement> withMethods = constraint.getHttpMethodConstraints();
				for (HttpMethodConstraintElement c : withMethods) {
					SecurityConstraintModel scm = new SecurityConstraintModel();
					scm.setServletModel(model);
					SecurityConstraintModel.WebResourceCollection col = new SecurityConstraintModel.WebResourceCollection();
					scm.getWebResourceCollections().add(col);
					scm.setTransportGuarantee(c.getTransportGuarantee());
					scm.getAuthRoles().addAll(Arrays.asList(c.getRolesAllowed()));
					if (scm.getAuthRoles().isEmpty()) {
						// I think it'll do what I want - no roles means DENY all
						scm.setAuthRolesSet(c.getEmptyRoleSemantic() == ServletSecurity.EmptyRoleSemantic.DENY);
					} else {
						scm.setAuthRolesSet(true);
					}
					col.getPatterns().add(urlPattern);
					col.getMethods().add(c.getMethodName());

					newConstraints.add(scm);
				}

				// Add the constraint for all the other methods
				SecurityConstraintModel scm = new SecurityConstraintModel();
				scm.setServletModel(model);
				SecurityConstraintModel.WebResourceCollection col = new SecurityConstraintModel.WebResourceCollection();
				scm.getWebResourceCollections().add(col);
				scm.setTransportGuarantee(constraint.getTransportGuarantee());
				scm.getAuthRoles().addAll(Arrays.asList(constraint.getRolesAllowed()));
				if (scm.getAuthRoles().isEmpty()) {
					// I think it'll do what I want - no roles means DENY all
					scm.setAuthRolesSet(constraint.getEmptyRoleSemantic() == ServletSecurity.EmptyRoleSemantic.DENY);
				} else {
					scm.setAuthRolesSet(true);
				}
				col.getPatterns().add(urlPattern);
				col.getOmittedMethods().addAll(constraint.getMethodNames());

				newConstraints.add(scm);
			}
		}

		osgiContextModel.getSecurityConfiguration().getSecurityConstraints().addAll(newConstraints);

		if (!newConstraints.isEmpty()) {
			model.setServletSecurityPresent(true);
		}

		return conflicts;
	}

	@Override
	public void setMultipartConfig(MultipartConfigElement multipartConfig) {
		model.setMultipartConfigElement(multipartConfig);
	}

	@Override
	public void setRunAsRole(String roleName) {
		model.setRunAs(roleName);
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
		return model.getRunAs();
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
