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

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

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
			final String sslKeystoreType, String sslKeyAlias,
			String trustStore, String trustStorePassword, String trustStoreType,
			boolean isClientAuthNeeded, boolean isClientAuthWanted, boolean useNIO,
			List<String> cipherSuitesIncluded, List<String> cipherSuitesExcluded,
			List<String> protocolsIncluded, List<String> protocolsExcluded) {
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
		// Java key stores may contain more than one private key entry.
		// Specifying the alias tells jetty which one to use.
		if ( (null != sslKeyAlias) && (!"".equals(sslKeyAlias)) ) {
			sslContextFactory.setCertAlias(sslKeyAlias);
		}

		// Quite often it is useful to use a certificate trust store other than the JVM default.
		if ( (null != trustStore) && (!"".equals(trustStore)) ) {
			sslContextFactory.setTrustStore(trustStore);
		}
		if ( (null != trustStorePassword) && (!"".equals(trustStorePassword))) {
			sslContextFactory.setTrustStorePassword(trustStorePassword);
		}
		if ( (null != trustStoreType) && (!"".equals(trustStoreType)) ) {
			sslContextFactory.setTrustStoreType(trustStoreType);
		}

		// In light of well-known attacks against weak encryption algorithms such as RC4,
		// it is usefull to be able to include or exclude certain ciphersuites.
		// Due to the overwhelming number of cipher suites using regex to specify inclusions
		// and exclusions greatly simplifies configuration.
		final String[] cipherSuites;
		try {
			SSLContext context = SSLContext.getDefault();
			SSLSocketFactory sf = context.getSocketFactory();
			cipherSuites = sf.getSupportedCipherSuites();
		}
		catch (NoSuchAlgorithmException e) {

			throw new RuntimeException("Failed to get supported cipher suites.", e);
		}

		if (cipherSuitesIncluded != null && !cipherSuitesIncluded.isEmpty()) {
			final List<String> cipherSuitesToInclude = new ArrayList<String>();
			for (final String cipherSuite : cipherSuites) {
				for (final String includeRegex : cipherSuitesIncluded) {
					if (cipherSuite.matches(includeRegex)) {
						cipherSuitesToInclude.add(cipherSuite);
					}
				}
			}
			sslContextFactory.setIncludeCipherSuites(cipherSuitesToInclude.toArray(new String[cipherSuitesToInclude.size()]));
		}

		if (cipherSuitesExcluded != null && !cipherSuitesExcluded.isEmpty()) {
			final List<String> cipherSuitesToExclude = new ArrayList<String>();
			for (final String cipherSuite : cipherSuites) {
				for (final String excludeRegex : cipherSuitesExcluded) {
					if (cipherSuite.matches(excludeRegex)) {
						cipherSuitesToExclude.add(cipherSuite);
					}
				}
			}
			sslContextFactory.setExcludeCipherSuites(cipherSuitesToExclude.toArray(new String[cipherSuitesToExclude.size()]));
		}

		// In light of attacks against SSL 3.0 as "POODLE" it is useful to include or exclude
		// SSL/TLS protocols as needed.
		if ( (null != protocolsIncluded) && (!protocolsIncluded.isEmpty()) ) {
			sslContextFactory.setIncludeProtocols(protocolsIncluded.toArray(new String[protocolsIncluded.size()]));
		}
		if ( (null != protocolsExcluded) && (!protocolsExcluded.isEmpty()) ) {
			sslContextFactory.setExcludeProtocols(protocolsExcluded.toArray(new String[protocolsExcluded.size()]));
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
