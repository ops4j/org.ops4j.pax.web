/*
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
package org.ops4j.pax.web.samples.jersey;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.ops4j.pax.web.service.http.HttpContext;
import org.ops4j.pax.web.service.http.HttpService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class TestActivator implements BundleActivator {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private HttpService httpService;
	private ServiceTracker<HttpService, HttpService> httpServiceTracker;

	@Override
	public void start(BundleContext context) {
		logger.info("starting Jersey TestActivator");
		httpServiceTracker = new ServiceTracker<>(context, HttpService.class, null) {
			@Override
			public HttpService addingService(ServiceReference<HttpService> serviceRef) {
				logger.info("registering Jersey servlet");
				httpService = super.addingService(serviceRef);

				HttpContext httpContext = new CustomHttpContext(context.getBundle());

				registerJerseyServlet(httpContext);
				registerResources(httpContext);
				return httpService;
			}

			@Override
			public void removedService(ServiceReference<HttpService> ref, HttpService service) {
				super.removedService(ref, service);
				httpService = null;
			}
		};
		httpServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) {
		httpServiceTracker.close();
	}

	private void registerResources(HttpContext httpContext) {
		try {
			httpService.registerResources("/images", "/", null);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering resources failed", e);
		}
	}

	private void registerJerseyServlet(HttpContext httpContext) {
		try {
			ResourceConfig app = new ResourceConfig().register(new RootResource());
			httpService.registerServlet("/", new ServletContainer(app), null, null);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering Jersey servlet failed", e);
		}
	}

}
