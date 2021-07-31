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
package org.ops4j.pax.web.service.spi.servlet;

import java.util.Iterator;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.views.DynamicJEEWebContainerView;
import org.ops4j.pax.web.service.spi.servlet.dynamic.DynamicEventListenerRegistration;
import org.osgi.framework.Bundle;

/**
 * <p>The {@link javax.servlet.ServletContainerInitializer} that's supposed to collect all the servlets/filters/listeners
 * that may have beed dynamically added when other SCIs were called and actually register them using full Pax Web
 * procedure of going through the {@link org.ops4j.pax.web.service.spi.model.ServerModel}, batches and server
 * controllers.</p>
 *
 * <p>This SCI is supposed to be called within Pax Web configuration thread and call back to the controller. It's
 * quite complex flow, as the SCI itself is called from within <em>ensure container started</em> method inside
 * the server wrapper. So the server starts, calls the SCIs, which may potentially add servlets, these servlets (and
 * filters and listeners too) turned into {@link org.ops4j.pax.web.service.spi.model.elements.ServletModel}s are then
 * added to batches, passed to the bundle-scoped {@link org.ops4j.pax.web.service.WebContainer} instances and
 * eventually sent to {@link org.ops4j.pax.web.service.spi.ServerController} again, which configures Jetty/Tomcat/
 * Undertow contexts (without trying to start them again!).</p>
 */
public class RegisteringContainerInitializer extends SCIWrapper {

	private final DynamicRegistrations registrations;
	private final OsgiServletContext osgiServletContext;

	public RegisteringContainerInitializer(OsgiServletContext osgiServletContext, DynamicRegistrations registrations) {
		super(null, new ContainerInitializerModel(null, null));
		this.registrations = registrations;

		// This has to always be the last one - lowest rank
		this.getModel().setServiceRank(Integer.MIN_VALUE);
		this.getModel().setServiceId(Long.MAX_VALUE);

		this.osgiServletContext = osgiServletContext;
	}

	@Override
	public void onStartup() throws ServletException {
		// we are the last SCI in the chain, so here's the last place where ServletContextListeners can be registered

		// first we will register all ServletContextListeners added by SCIs and then register
		// a ServletContextListeners that will register servlets/filters/listeners added both by SCIs AND
		// other ServletContextListeners...

		for (Iterator<DynamicEventListenerRegistration> iterator = registrations.getDynamicListenerRegistrations().iterator(); iterator.hasNext(); ) {
			DynamicEventListenerRegistration reg = iterator.next();
			if (reg.getModel().getEventListener() instanceof ServletContextListener) {
				Bundle bundle = reg.getModel().getRegisteringBundle();
				DynamicJEEWebContainerView container = registrations.getContainer(bundle);
				if (container != null) {
					container.registerListener(reg.getModel());
				}
				iterator.remove();
			}
		}

		osgiServletContext.getContainerServletContext().addListener(new RegisteringContextListener(registrations));

		osgiServletContext.noMoreServletContextListeners();
	}

	@Override
	public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
		onStartup();
	}

}
