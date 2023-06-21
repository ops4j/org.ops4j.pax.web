/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.jsp.internal;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.http.NamespaceException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 08, 2007
 */
public final class Activator implements BundleActivator, ServiceTrackerCustomizer<WebContainer, WebContainer> {

	private BundleContext bundleContext;

	private ServiceTracker<WebContainer, WebContainer> tracker;

	public void start(BundleContext bc) throws Exception {
		bundleContext = bc;
		tracker = new ServiceTracker<>(bc, WebContainer.class, this);
		tracker.open();
	}

	public void stop(BundleContext bc) throws Exception {
		tracker.close();
	}

	@Override
	@SuppressWarnings("unchecked")
	public WebContainer addingService(ServiceReference<WebContainer> reference) {
		final WebContainer webContainer = bundleContext.getService(reference);
		if (webContainer != null) {
			// turn on JSP support in the context
			webContainer.registerJsps(new String[] { "/helloworld/jsp/*" }, null, null);
			try {
				// register images as resources
				webContainer.registerResources("/images", "/images", null);

				// register precompiled JSP pages as servlets
				Class<? extends Servlet> c1 = (Class<? extends Servlet>) Class.forName("org.apache.jsp.simple_jsp");
				Class<? extends Servlet> c2 = (Class<? extends Servlet>) Class.forName("org.apache.jsp.using_002dtld_jsp");
				webContainer.registerServlet(c1, new String[] { "/helloworld/jspc/simple.jsp" }, null, null);
				webContainer.registerServlet(c2, new String[] { "/helloworld/jspc/using-tld.jsp" }, null, null);
			} catch (NamespaceException | ClassNotFoundException | ServletException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return webContainer;
	}

	@Override
	public void modifiedService(ServiceReference<WebContainer> reference, WebContainer service) {
		// ignore
	}

	@Override
	public void removedService(ServiceReference<WebContainer> reference, WebContainer service) {
		service.unregisterJsps(null);
	}

}
