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

import java.util.List;

import org.eclipse.jetty.server.Connector;

public interface JettyFactory {

	JettyServer createServer();

	Connector createConnector(String name, int port, String host, boolean useNIO, 
			Boolean checkForwardedHeaders);

	/**
	 * Creates a secure (SSL) connector.
	 * 
	 * @param name
	 *            the name to give to this connector
	 * @param port
	 *            the port on which the secure port should run
	 * @param sslKeystore
	 *            the path to the keystore
	 * @param sslKeystorePassword
	 *            the keystore password.
	 * @param sslKeyPassword
	 *            the password of the server SSL/TLS private key entry in the key store.
	 * @param host
	 *            the address on which the secure port should listen
	 * @param sslKeystoreType
	 *            the SSL/TLS keystore type (e.g. jks, jceks, bks).
	 * @param sslKeyAlias
	 *            the alias of the SSL/TLS private key entry in the keystore.
	 * @param isClientAuthNeeded
	 *            true if the server requires client certificate authentication.
	 * @param isClientAuthWanted
	 *            true if the server accepts client certificate authentication.
	 * @param isClientAuthWanted
	 *            true if the server should use a non-blocking IO (NIO) connector.
	 * @param cipherSuiteIncluded
	 *            list of SSL/TLS cipher suites that are acceptable.
	 * @param cipherSuiteExcluded
	 *            list of SSL/TLS cipher suites that are not acceptable.
	 * @param protocolsIncluded
	 *            list of SSL/TLS protocols that are acceptable.
	 * @param protocolsExcluded
	 *            list of SSL/TLS protocols that are not acceptable.
	 * 
	 * @return a secure connector
	 * 
	 * @since 0.2.1
	 */
	Connector createSecureConnector(String name, int port, String sslKeystore,
			String sslKeystorePassword, String sslKeyPassword, String host,
			String sslKeystoreType, String sslKeyAlias,
			String trustStore, String trustStorePassword, String trustStoreType,
			boolean isClientAuthNeeded, boolean isClientAuthWanted, boolean useNIO,
			List<String> cipherSuiteIncluded, List<String> cipherSuiteExcluded,
			List<String> protocolsIncluded, List<String> protocolsExcluded);

}
