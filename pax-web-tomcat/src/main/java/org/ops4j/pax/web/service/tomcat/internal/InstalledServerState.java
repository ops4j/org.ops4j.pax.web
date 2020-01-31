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

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.INSTALLED;

import org.ops4j.pax.web.service.spi.config.Configuration;

/**
 * @author Romain Gilles
 */
class InstalledServerState extends AbstractServerState {

	InstalledServerState(ServerStateFactory serverStateFactory) {
		super(serverStateFactory);
	}

	static ServerState getInstance(ServerStateFactory serverStateFactory) {
		return new InstalledServerState(serverStateFactory);
	}

	@Override
	public ServerState start() {
		return throwIllegalState();
	}

	@Override
	public ServerState stop() {
		return throwIllegalState();
	}

	@Override
	public boolean isStarted() {
		return false;
	}

	@Override
	public boolean isConfigured() {
		return false;
	}

	@Override
	public ServerState configure(Configuration configuration) {
		return getServerStateFactory().newInitializedState(configuration);
	}

	@Override
	public States getState() {
		return INSTALLED;
	}

}
