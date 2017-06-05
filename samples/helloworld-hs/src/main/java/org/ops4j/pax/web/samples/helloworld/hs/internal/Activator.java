/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */
public final class Activator implements BundleActivator,
		ServiceTrackerCustomizer<HttpService, HttpService> {

	private BundleContext bundleContext;

	private ServiceTracker<HttpService, HttpService> tracker;

	/**
	 * Called when the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		bundleContext = bc;
		tracker = new ServiceTracker<>(bc,
				HttpService.class, this);
		tracker.open();
	}

	/**
	 * Called when the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		tracker.close();
	}

	@Override
	public HttpService addingService(ServiceReference<HttpService> reference) {
		final HttpService httpService = (HttpService) bundleContext.getService(reference);
		if (httpService != null) {
			// create a default context to share between registrations
			final HttpContext httpContext = httpService
					.createDefaultHttpContext();
			// register the hello world servlet
			final Dictionary<String, Object> initParams = new Hashtable<>();
			initParams.put("from", "HttpService");
			try {
				httpService.registerServlet("/helloworld/hs", // alias
						new HelloWorldServlet("/helloworld/hs"), // registered
						// servlet
						initParams, httpContext);
				httpService.registerServlet("/*", // alias - using /* as alias
						// does work around issue PAXWEB-108
						new HelloWorldServlet("/"), // registered servlet
						initParams, httpContext);
				// register images as resources
				httpService
						.registerResources("/images", "/images", httpContext);
				// register images as resources under another alias
				httpService
						.registerResources("/alt-images", "/images", httpContext);
			} catch (ServletException e) {
				e.printStackTrace();
			} catch (NamespaceException e) {
				e.printStackTrace();
			}
		}
		return httpService;
	}

	@Override
	public void modifiedService(ServiceReference<HttpService> reference,
								HttpService service) {
		// ignore
	}

	@Override
	public void removedService(ServiceReference<HttpService> reference,
							   HttpService service) {
		try {
			service.unregister("/helloworld/hs");
			service.unregister("/*");
			service.unregister("/images");
			//CHECKSTYLE:OFF
		} catch (Exception e) {

		}
		//CHECKSTYLE:ON
	}
}
