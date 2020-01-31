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

import org.ops4j.pax.web.service.spi.config.Configuration;

/**
 * State Server Factory class provides methods to switch from the different
 * states. They are 3 valid states:
 * <ol>
 * <li>Installed State: it represents the initial state and the entry point.
 * Next states is Initialized State</li>
 * </ol>
 *
 * @author Romain Gilles
 */
interface ServerStateFactory {

	ServerState newInstalledState();

	ServerState newActiveState(ServerWrapper server, ServerState serverState);

	ServerState newInitializedState(Configuration configuration);
}
