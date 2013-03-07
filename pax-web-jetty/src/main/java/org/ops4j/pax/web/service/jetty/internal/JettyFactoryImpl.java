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
package org.ops4j.pax.web.service.jetty.internal;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ServerModel;

class JettyFactoryImpl implements JettyFactory {

	/**
	 * Associated server model.
	 */
	private final ServerModel serverModel;

	/**
	 * Constrcutor.
	 * 
	 * @param serverModel
	 *            asscociated server model
	 */
	JettyFactoryImpl(final ServerModel serverModel) {
		NullArgumentException.validateNotNull(serverModel, "Service model");
		this.serverModel = serverModel;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JettyServer createServer() {
		return new JettyServerImpl(serverModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connector createConnector(final String name, final int port,
			final String host, final boolean useNIO) {
		if (useNIO) {
			final SelectChannelConnector nioConnector = new NIOSocketConnectorWrapper();
			nioConnector.setName(name);
			nioConnector.setHost(host);
			nioConnector.setPort(port);
			nioConnector.setUseDirectBuffers(true);
			return nioConnector;
		} else {
			final SocketConnector connector = new SocketConnectorWrapper();
			connector.setName(name);
			connector.setPort(port);
			connector.setHost(host);
			return connector;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connector createSecureConnector(final String name, final int port,
			final String sslKeystore, final String sslPassword,
			final String sslKeyPassword, final String host,
			final String sslKeystoreType, final boolean isClientAuthNeeded,
			final boolean isClientAuthWanted) {
		SslContextFactory sslContextFactory = new SslContextFactory(sslKeystore); // TODO:
																					// PAXWEB-339
																					// configurable
																					// ContextFactory
		sslContextFactory.setKeyStorePassword(sslKeyPassword);
		sslContextFactory.setKeyManagerPassword(sslPassword);
		sslContextFactory.setNeedClientAuth(isClientAuthNeeded);
		sslContextFactory.setWantClientAuth(isClientAuthWanted);
		if (sslKeystoreType != null) {
			sslContextFactory.setKeyStoreType(sslKeystoreType);
		}

		// create a https connector
		final SslSocketConnector connector = new SslSocketConnector(
				sslContextFactory);

		connector.setName(name);
		connector.setPort(port);
		connector.setHost(host);
		connector.setConfidentialPort(port); // Fix for PAXWEB-430

		return connector;
	}

}
