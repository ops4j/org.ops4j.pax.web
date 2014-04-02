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

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
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
	public ServerConnector createConnector(final Server server,
			final String name, final int port, final String host, 
			final Boolean checkForwaredHeaders) {

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(8443);
		httpConfig.setOutputBufferSize(32768);
		if (checkForwaredHeaders) {
			httpConfig.addCustomizer(new ForwardedRequestCustomizer());
		}

		// HTTP connector
		ServerConnector http = new ServerConnector(server,
				new HttpConnectionFactory(httpConfig));
		http.setPort(port);
		http.setHost(host);
		http.setName(name);
		http.setIdleTimeout(30000);

		return http;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createSecureConnector(Server server,
			final String name, final int port, final String sslKeystore,
			final String sslPassword, final String sslKeyPassword,
			final String host, final String sslKeystoreType,
			final boolean isClientAuthNeeded, final boolean isClientAuthWanted) {

		// SSL Context Factory for HTTPS and SPDY
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(sslKeystore);
		sslContextFactory.setKeyStorePassword(sslKeyPassword);
		sslContextFactory.setKeyManagerPassword(sslPassword);
		sslContextFactory.setNeedClientAuth(isClientAuthNeeded);
		sslContextFactory.setWantClientAuth(isClientAuthWanted);
		if (sslKeystoreType != null) {
			sslContextFactory.setKeyStoreType(sslKeystoreType);
		}

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(port);
		httpConfig.setOutputBufferSize(32768);

		// HTTPS Configuration
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		// HTTPS connector
		ServerConnector https = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConfig));
		https.setPort(port);
		https.setName(name);
		https.setHost(host);
		https.setIdleTimeout(500000);

		return https;
	}

}
