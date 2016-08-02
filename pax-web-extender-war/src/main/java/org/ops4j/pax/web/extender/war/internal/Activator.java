/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Hashtable;

import org.ops4j.pax.web.extender.war.internal.extender.AbstractExtender;
import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings("deprecation")
public class Activator extends AbstractExtender {

	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;
	private WebObserver webObserver;
	private WebEventDispatcher webEventDispatcher;
	private ServiceRegistration<WarManager> registration;

	@Override
	protected void doStart() throws Exception {
		logger.debug("Pax Web WAR Extender - Starting");

		BundleContext bundleContext = getBundleContext();

		webEventDispatcher = new WebEventDispatcher(bundleContext);

		Filter filterPackage = bundleContext.createFilter("(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
		packageAdminTracker = new ServiceTracker<>(bundleContext, filterPackage, null);
		packageAdminTracker.open();

		DefaultWebAppDependencyManager dependencyManager = new DefaultWebAppDependencyManager();

		webObserver = new WebObserver(new WebAppParser(packageAdminTracker),
				new WebAppPublisher(webEventDispatcher, bundleContext), webEventDispatcher, dependencyManager,
				bundleContext);

		startTracking();
		registration = getBundleContext().registerService(
				WarManager.class, webObserver,
				new Hashtable<>());

		logger.debug("Pax Web WAR Extender - Started");
	}

	@Override
	protected void doStop() throws Exception {
		logger.debug("Pax Web WAR Extender - Stopping");
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		stopTracking();
		webEventDispatcher.destroy();
		packageAdminTracker.close();
		logger.debug("Pax Web WAR Extender - Stopped");
	}

	@Override
	protected Extension doCreateExtension(Bundle bundle) throws Exception {
		return webObserver.createExtension(bundle);
	}

}
