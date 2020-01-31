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

import org.ops4j.pax.web.service.spi.config.Configuration;

/**
 * @author Romaim Gilles
 */
public class TomcatServerStateFactory implements ServerStateFactory {
	private final ServerFactory serverFactory;

	TomcatServerStateFactory(ServerFactory serverFactory) {
		this.serverFactory = serverFactory;
	}

	@Override
	public ServerState newInstalledState() {
		return InstalledServerState.getInstance(this);
	}

	@Override
	public ServerState newActiveState(ServerWrapper server,
									  ServerState serverState) {
		if (serverState == null) {
			throw new IllegalArgumentException(
					"server state parameter must be not null");
		}
		if (serverState.getState() != INITIALIZED) {
			throw new IllegalArgumentException(
					String.format(
							"server state parameter must be in state: '%s'; and not: '%s'",
							INITIALIZED, serverState.getState()));
		}
		return ActiveServerState.getInstance(this, serverState, server);
	}

	@Override
	public ServerState newInitializedState(Configuration configuration) {
		return InitializedServerState.getInstance(this, configuration,
				serverFactory);
	}

	static ServerStateFactory newInstance(ServerFactory serverFactory) {
		return new TomcatServerStateFactory(serverFactory);
	}

}
