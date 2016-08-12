/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;

class ServerControllerFactoryImpl implements ServerControllerFactory {

	private Bundle bundle;
	private List<Handler> handlers = new ArrayList<>();
	private List<Connector> connectors = new ArrayList<>();

	public ServerControllerFactoryImpl(Bundle bundle) {
		this.bundle = bundle;
	}

	public void addHandler(Handler handler) {
		this.handlers.add(handler);
	}

	public void removeHandler(Handler handler) {
		handlers.remove(handler);
	}

	public void addConnector(Connector connector) {
		this.connectors.add(connector);
	}

	public void removeConnector(Connector connector) {
		this.connectors.remove(connector);
	}

	@Override
	public ServerController createServerController(ServerModel serverModel) {
		return new ServerControllerImpl(new JettyFactoryImpl(serverModel, bundle, handlers, connectors));
	}


}