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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Hashtable;

import org.apache.felix.utils.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.parser.WebAppParser;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Similarly to independent {@code org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext}, this
 * <em>extender context</em> manages interaction between Bundles converted into <em>web applications</em> and
 * dynamically available {@link org.ops4j.pax.web.service.WebContainer} service, where all the web components and
 * web contexts may be installed/registered.
 */
public class WarExtenderContext {

	private static final Logger LOG = LoggerFactory.getLogger(WarExtenderContext.class);

	private final BundleContext bundleContext;

	/** This is were the lifecycle of {@link WebContainer} is managed. */
	private final WebContainerManager webContainerManager;




	/** Used to send events related to entire Web Applications being installed/uninstalled. */
	private WebApplicationEventDispatcher webApplicationEventDispatcher;

	/** Used to parser {@code web.xml} and fragmnets into a web application model */
	private WebAppParser webApplicationParser;

	private WebObserver webObserver;
	private ServiceRegistration<WarManager> registration;

	public WarExtenderContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;

		// dispatcher of events related to WAB lifecycle (128.5 Events)
		webApplicationEventDispatcher = new WebApplicationEventDispatcher(context);

		// web.xml, web-fragment.xml parser
		webApplicationParser = new WebAppParser(context);

		webObserver = new WebObserver(
				webApplicationParser,
				new WebAppPublisher(webApplicationEventDispatcher, context),
				webApplicationEventDispatcher,
				new DefaultWebAppDependencyManager(),
				context);

		registration = getBundleContext().registerService(
				WarManager.class, webObserver,
				new Hashtable<>());

		webContainerManager = new WebContainerManager(bundleContext, null, "HttpService->WarExtender");
		webContainerManager.initialize();
	}

	/**
	 * Cleans up everything related to pax-web-extender-war
	 */
	public void shutdown() {
		ServiceReference<WebContainer> ref = webContainerServiceRef.get();
		if (ref != null) {
			webContainerRemoved(ref);
		}

		if (webApplicationEventDispatcher != null) {
			webApplicationEventDispatcher.destroy();
			webApplicationEventDispatcher = null;
		}

		if (registration != null) {
			registration.unregister();
			registration = null;
		}

		webContainerManager.shutdown();
	}

	public Extension createExtension(Bundle bundle) {
		return null;
	}

}
