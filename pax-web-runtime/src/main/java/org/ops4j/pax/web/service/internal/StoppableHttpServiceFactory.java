/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServiceFactory} that manages bundle-scoped lifecycle of {@link HttpService}.
 */
public abstract class StoppableHttpServiceFactory implements ServiceFactory<StoppableHttpService> {

	private static final Logger LOG = LoggerFactory.getLogger(StoppableHttpServiceFactory.class);

	private final ServerController serverController;
	private final ServerModel serverModel;
	private final WebElementEventDispatcher webElementEventDispatcher;

	public StoppableHttpServiceFactory(ServerController serverController, ServerModel serverModel, WebElementEventDispatcher webElementEventDispatcher) {
		this.serverController = serverController;
		this.serverModel = serverModel;
		this.webElementEventDispatcher = webElementEventDispatcher;
	}

	@Override
	public StoppableHttpService getService(final Bundle bundle, final ServiceRegistration<StoppableHttpService> serviceRegistration) {
		LOG.info("Binding HTTP Service for bundle: [{}]", bundle);
		return createService(bundle, serverController, serverModel, webElementEventDispatcher);
	}

	@Override
	public void ungetService(final Bundle bundle, final ServiceRegistration<StoppableHttpService> serviceRegistration, final StoppableHttpService httpService) {
		LOG.info("Unbinding HTTP Service from bundle: [{}]", bundle);
		httpService.stop();
	}

	abstract StoppableHttpService createService(Bundle bundle, ServerController serverController, ServerModel serverModel, WebElementEventDispatcher webElementEventDispatcher);

}
