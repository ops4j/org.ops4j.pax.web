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
