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
package org.ops4j.pax.web.samples.custom.context;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private final Logger logger = Logger.getLogger(getClass().getName());

	private HttpService httpService;
	private ServiceTracker<HttpService, HttpService> httpServiceTracker;
	private ServiceRegistration<HttpContext> reg;


	@Override
	public void start(BundleContext context) {
		httpServiceTracker = new ServiceTracker<HttpService, HttpService>(context, HttpService.class, null) {
			@Override
			public HttpService addingService(ServiceReference<HttpService> serviceRef) {
				logger.info("registering servlet");
				httpService = super.addingService(serviceRef);

				HttpContext httpContext = new CustomHttpContext(context.getBundle());

				// first register the elements, so we can show that Whiteboard registration of HttpContext
				// LATER will re-register the elements to different context
				registerServlet(httpContext);
				registerResources(httpContext);

				// whiteboard-register this context, so we can set its context path
				Dictionary<String, Object> props = new Hashtable<>();
				props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "custom");
				props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, "/c");
				reg = context.registerService(HttpContext.class, httpContext, props);

				return httpService;
			}

			@Override
			public void removedService(ServiceReference<HttpService> ref, HttpService service) {
				super.removedService(ref, service);
				if (httpService != null) {
					httpService.unregister("/www");
					httpService.unregister("/s");
					httpService = null;
				}
				if (reg != null) {
					reg.unregister();
					reg = null;
				}
			}
		};
		httpServiceTracker.open();
	}

	@Override
	public void stop(BundleContext context) {
		if (httpService != null) {
			httpService.unregister("/www");
			httpService.unregister("/s");
			httpService = null;
		}
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
		httpServiceTracker.close();
	}

	private void registerResources(HttpContext httpContext) {
		try {
			httpService.registerResources("/www", "/images", httpContext);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering resources failed", e);
		}
	}

	private void registerServlet(HttpContext httpContext) {
		try {
			httpService.registerServlet("/s", new HelloServlet(), null, httpContext);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Registering servlet failed", e);
		}
	}

}
