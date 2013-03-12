package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.Configuration;

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
