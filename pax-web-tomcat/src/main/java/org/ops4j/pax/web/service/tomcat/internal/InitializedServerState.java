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
package org.ops4j.pax.web.service.tomcat.internal;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.INITIALIZED;

import java.util.ArrayList;
import java.util.Collection;

import org.ops4j.pax.web.service.spi.config.Configuration;

/**
 * @author Romain Gilles
 */
class InitializedServerState extends InstalledServerState {
	private Configuration configuration;
	private final ServerFactory serverFactory;

	private InitializedServerState(ServerStateFactory serverStateFactory,
								   Configuration configuration, ServerFactory serverFactory) {
		super(serverStateFactory);
		this.configuration = configuration;
		this.serverFactory = serverFactory;
	}

	static InitializedServerState getInstance(
			ServerStateFactory serverStateFactory, Configuration configuration,
			ServerFactory serverFactory) {
		return new InitializedServerState(serverStateFactory, configuration,
				serverFactory);
	}

	@Override
	public ServerState start() {
		// TODO create and start the server.
		// based on org.apache.catalina.startup.Catalina and
		// org.apache.catalina.startup.Tomcat code
		ServerWrapper server = serverFactory.newServer(configuration);
		server.start();
		return getServerStateFactory().newActiveState(server, this);
	}

	@Override
	public ServerState configure(Configuration config) {
		this.configuration = config;
		return this;
	}

	@Override
	Collection<String> getSupportedOperations() {
		ArrayList<String> result = new ArrayList<>(
				super.getSupportedOperations());
		result.add(formatSupportedOperation("start"));
		return result;
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public States getState() {
		return INITIALIZED;
	}

	@Override
	public Integer getHttpPort() {
		return configuration.server().getHttpPort();
	}

	@Override
	public Integer getHttpSecurePort() {
		return configuration.server().getHttpSecurePort();
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

}
