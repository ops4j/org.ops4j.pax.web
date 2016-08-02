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

import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.ops4j.pax.web.service.spi.model.ServerModel;

/**
 * @author Romaim Gilles
 */
public class TomcatServerControllerFactory implements ServerControllerFactory {

	private final ServerStateFactory serverStateFactory;

	private TomcatServerControllerFactory(ServerStateFactory serverStateFactory) {

		this.serverStateFactory = serverStateFactory;
	}

	@Override
	public ServerController createServerController(ServerModel serverModel) {
		return TomcatServerController.newInstance(serverStateFactory
				.newInstalledState());
	}

	static ServerControllerFactory newInstance(
			ServerStateFactory serverStateFactory) {
		return new TomcatServerControllerFactory(serverStateFactory);
	}
}
