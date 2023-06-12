/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletSecurityElement;
import jakarta.servlet.annotation.ServletSecurity;

import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.views.DynamicJEEWebContainerView;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicEventListenerRegistration;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicFilterRegistration;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicServletRegistration;
import org.osgi.framework.Bundle;

/**
 * A {@link ServletContextListener} that performs the dynamic registrations made by SCIs and other
 * {@link ServletContextListener servlet context listeners}.
 */
public class RegisteringContextListener implements ServletContextListener {

	private final DynamicRegistrations registrations;

	public RegisteringContextListener(DynamicRegistrations registrations) {
		this.registrations = registrations;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// register listeners
		for (DynamicEventListenerRegistration reg : registrations.getDynamicListenerRegistrations()) {
			Bundle bundle = reg.getModel().getRegisteringBundle();
			DynamicJEEWebContainerView container = registrations.getContainer(bundle);
			if (container != null) {
				container.registerListener(reg.getModel());
			}
		}

		// register servlets
		for (DynamicServletRegistration reg : registrations.getDynamicServletRegistrations().values()) {
			// just before the dynamic registration is registered, we have to do additional scanning

			// check @ServletSecurity (even if "13.4.1 @ServletSecurity Annotation" constraint this to the servlets
			// created using jakarta.servlet.ServletContext.createServlet()...)
			ServletModel model = reg.getModel();
			Class<?> cls = model.getServlet() != null ? model.getServlet().getClass() : model.getServletClass();
			ServletSecurity security = cls == null ? null : cls.getAnnotation(ServletSecurity.class);
			if (security != null && !model.isServletSecurityPresent()) {
				// it means user didn't call jakarta.servlet.ServletRegistration.Dynamic.setServletSecurity()
				// we can leverage the constructur that accepts the annotation
				reg.setServletSecurity(new ServletSecurityElement(security));
			}

			Bundle bundle = reg.getModel().getRegisteringBundle();
			DynamicJEEWebContainerView container = registrations.getContainer(bundle);
			if (container != null) {
				container.registerServlet(reg.getModel());
			}
		}

		// register filters
		for (DynamicFilterRegistration reg : registrations.getDynamicFilterRegistrations()) {
			Bundle bundle = reg.getModel().getRegisteringBundle();
			DynamicJEEWebContainerView container = registrations.getContainer(bundle);
			if (container != null) {
				container.registerFilter(reg.getModel());
			}
		}

		// clean the registrations
		registrations.getDynamicServletRegistrations().clear();
		registrations.getDynamicFilterRegistrations().clear();
		registrations.getDynamicListenerRegistrations().clear();
	}

}
