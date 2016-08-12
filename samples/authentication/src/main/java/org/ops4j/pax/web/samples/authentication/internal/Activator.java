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
package org.ops4j.pax.web.samples.authentication.internal;

import org.ops4j.pax.web.samples.authentication.AuthHttpContext;
import org.ops4j.pax.web.samples.authentication.StatusServlet;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator implements BundleActivator {

	private ServiceReference<HttpService> httpServiceRef;
	private HttpService httpService;

	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		httpServiceRef = bc.getServiceReference(HttpService.class);
		if (httpServiceRef != null) {
			httpService = (HttpService) bc.getService(httpServiceRef);
			httpService.registerServlet("/status", new StatusServlet(), null,
					null);
			httpService.registerServlet("/status-with-auth",
					new StatusServlet(), null, new AuthHttpContext());
		}
	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if (httpService != null) {
			bc.ungetService(httpServiceRef);
			httpServiceRef = null;
			httpService = null;
		}
	}
}
