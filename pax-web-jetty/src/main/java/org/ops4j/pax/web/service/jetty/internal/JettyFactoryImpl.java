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
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;

class JettyFactoryImpl implements JettyFactory {

	/**
	 * Associated server model.
	 */
	private final ServerModel serverModel;
	private Bundle bundle;

	/**
	 * Constrcutor.
	 * 
	 * @param serverModel
	 *            asscociated server model
	 * @param bundle
	 */
	JettyFactoryImpl(final ServerModel serverModel, Bundle bundle) {
		NullArgumentException.validateNotNull(serverModel, "Service model");
		this.serverModel = serverModel;
		this.bundle = bundle;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JettyServer createServer() {
		return new JettyServerImpl(serverModel, bundle);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Connector createConnector(final String name, final int port,
			final String host, final boolean useNIO, final Boolean checkForwardedHeaders) {
		if (useNIO) {
			final SelectChannelConnector nioConnector = new NIOSocketConnectorWrapper();
			nioConnector.setName(name);
			nioConnector.setHost(host);
			nioConnector.setPort(port);
			nioConnector.setUseDirectBuffers(true);
			if (checkForwardedHeaders != null) {
				nioConnector.setForwarded(checkForwardedHeaders);
			}
			return nioConnector;
		} else {
			final SocketConnector connector = new SocketConnectorWrapper();
			connector.setName(name);
			connector.setPort(port);
			connector.setHost(host);
			if (checkForwardedHeaders != null) {
				connector.setForwarded(checkForwardedHeaders);
			}
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
			final boolean isClientAuthWanted, final boolean useNIO) {
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

		if (useNIO) {
			SslSelectChannelConnector connector = new SslSelectChannelConnector(sslContextFactory);
			connector.setName(name);
			connector.setPort(port);
			connector.setHost(host);
			connector.setConfidentialPort(port); // Fix for PAXWEB-430

			return connector;

		} else {
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

}
