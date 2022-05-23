/*
 * Copyright 2022 OPS4J.
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
package org.ops4j.pax.web.samples.helloworld.hs2.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class Activator implements BundleActivator, ServiceTrackerCustomizer<HttpService, HttpService> {

	private BundleContext bundleContext;

	private ServiceTracker<HttpService, HttpService> tracker;

	public void start(BundleContext bc) {
		bundleContext = bc;
		tracker = new ServiceTracker<>(bc, HttpService.class, this);
		tracker.open();
	}

	public void stop(BundleContext bc) {
		HttpService service = tracker.getService();
		if (service != null) {
			try {
				service.unregister("/test2");
			} catch (Exception ignored) {
			}
		}
		tracker.close();
	}

	@Override
	public HttpService addingService(ServiceReference<HttpService> reference) {
		final HttpService httpService = bundleContext.getService(reference);
		if (httpService != null) {
			try {
				httpService.registerServlet("/test2", new HelloWorldServlet(), null, null);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return httpService;
	}

	@Override
	public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
		// ignore
	}

	@Override
	public void removedService(ServiceReference<HttpService> reference, HttpService service) {
	}

}
