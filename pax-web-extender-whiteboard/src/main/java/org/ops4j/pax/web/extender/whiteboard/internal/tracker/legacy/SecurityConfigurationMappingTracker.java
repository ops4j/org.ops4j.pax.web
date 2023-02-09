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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConfigurationMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultSecurityConstraintMapping;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.model.events.SecurityConfigurationEventData;
import org.ops4j.pax.web.service.whiteboard.SecurityConfigurationMapping;
import org.ops4j.pax.web.service.whiteboard.SecurityConstraintMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;

/**
 * Tracks {@link SecurityConfigurationMapping} services.
 *
 * @author Grzegorz Grzybek
 */
public class SecurityConfigurationMappingTracker extends AbstractMappingTracker<SecurityConfigurationMapping, SecurityConfigurationMapping, SecurityConfigurationEventData, SecurityConfigurationModel> {

	protected SecurityConfigurationMappingTracker(WhiteboardExtenderContext whiteboardExtenderContext, BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<SecurityConfigurationMapping, SecurityConfigurationModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new SecurityConfigurationMappingTracker(whiteboardExtenderContext, bundleContext).create(SecurityConfigurationMapping.class);
	}

	@Override
	protected SecurityConfigurationModel doCreateElementModel(Bundle bundle, SecurityConfigurationMapping service, Integer rank, Long serviceId) {
		SecurityConfigurationModel model = new SecurityConfigurationModel();

		// the <login-config> part
		if (service instanceof DefaultSecurityConfigurationMapping) {
			model.setLoginConfig(((DefaultSecurityConfigurationMapping) service).getLoginConfig());
			// assume that authMethod, realmName, formLoginPage and formErrorPage are already set
		} else {
			LoginConfigModel lcm = new LoginConfigModel();
			lcm.setAuthMethod(service.getAuthMethod());
			lcm.setRealmName(service.getRealmName());
			lcm.setFormLoginPage(service.getFormLoginPage());
			lcm.setFormErrorPage(service.getFormErrorPage());
			model.setLoginConfig(lcm);
		}

		// the <security-constraint> part
		Collection<SecurityConstraintMapping> securityConstraints = service.getSecurityConstraints();
		if (securityConstraints != null) {
			for (SecurityConstraintMapping constraint : securityConstraints) {
				SecurityConstraintModel scm;
				if (constraint instanceof DefaultSecurityConstraintMapping) {
					scm = ((DefaultSecurityConstraintMapping) constraint).getSecurityConstraint();
					// assume that name, authRoles, authRolesSet and transportGuarantee are already set
				} else {
					scm = new SecurityConstraintModel();
					scm.setName(constraint.getName());
					if (constraint.getAuthRoles() != null) {
						scm.getAuthRoles().addAll(constraint.getAuthRoles());
					}
					scm.setAuthRolesSet(constraint.isAuthRolesSet());
					scm.setTransportGuarantee(constraint.getTransportGuarantee());
				}
				model.getSecurityConstraints().add(scm);

				// the <security-constraint>/<web-resource-collection> part
				Collection<SecurityConstraintMapping.WebResourceCollectionMapping> webResourceCollections = constraint.getWebResourceCollections();
				if (webResourceCollections != null) {
					for (SecurityConstraintMapping.WebResourceCollectionMapping wrcm : webResourceCollections) {
						SecurityConstraintModel.WebResourceCollection wrc;
						if (wrcm instanceof DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping) {
							wrc = ((DefaultSecurityConstraintMapping.DefaultWebResourceCollectionMapping) wrcm).getWebResourceCollection();
							// assume that name, urlPatterns, httpMethods and httpMethodsOmmissions are already set
						} else {
							wrc = new SecurityConstraintModel.WebResourceCollection();
							wrc.setName(wrcm.getName());
							if (wrcm.getUrlPatterns() != null) {
								wrc.getPatterns().addAll(wrcm.getUrlPatterns());
							}
							if (wrcm.getHttpMethods() != null) {
								wrc.getMethods().addAll(wrcm.getHttpMethods());
							}
							if (wrcm.getHttpMethodOmissions() != null) {
								wrc.getOmittedMethods().addAll(wrcm.getHttpMethodOmissions());
							}
						}
						scm.getWebResourceCollections().add(wrc);
					}
				}
			}
		}

		// the <security-role> part
		if (service.getSecurityRoles() != null) {
			model.getSecurityRoles().addAll(service.getSecurityRoles());
		}

		return model;
	}

}
