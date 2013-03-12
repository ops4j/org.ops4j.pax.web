package org.ops4j.pax.web.service.tomcat.internal;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.INITIALIZED;

import java.util.ArrayList;
import java.util.Collection;

import org.ops4j.pax.web.service.spi.Configuration;

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
	public ServerState configure(Configuration configuration) {
		this.configuration = configuration;
		return this;
	}

	@Override
	Collection<String> getSupportedOperations() {
		ArrayList<String> result = new ArrayList<String>(
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
		return configuration.getHttpPort();
	}

	@Override
	public Integer getHttpSecurePort() {
		return configuration.getHttpSecurePort();
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

}
