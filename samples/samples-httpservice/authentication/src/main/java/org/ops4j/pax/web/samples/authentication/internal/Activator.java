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
package org.ops4j.pax.web.samples.authentication.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.web.samples.authentication.AuthHttpContext;
import org.ops4j.pax.web.samples.authentication.StatusServlet;
import org.ops4j.pax.web.service.http.HttpService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator implements BundleActivator {

	private ServiceTracker<HttpService, HttpService> tracker;

	/**
	 * Called whenever the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		tracker = new ServiceTracker<HttpService, HttpService>(bc, HttpService.class, null);
		tracker.open();
		HttpService httpService = tracker.waitForService(5000);
		Dictionary<String, String> initParams = new Hashtable<>();
		// legacy way to specify servlet name - but there's no other way when using HttpService.
		initParams.put("servlet-name", "status1");
		httpService.registerServlet("/status", new StatusServlet(), initParams, null);
		initParams.put("servlet-name", "status2");
		httpService.registerServlet("/status-with-auth", new StatusServlet(), initParams, new AuthHttpContext());
	}

	/**
	 * Called whenever the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if (tracker != null) {
			tracker.close();
		}
	}

}
