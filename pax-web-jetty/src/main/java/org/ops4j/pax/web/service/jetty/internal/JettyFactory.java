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
import org.eclipse.jetty.server.Server;

public interface JettyFactory {

	JettyServer createServer(Integer maxThreads, Integer minThreads, Integer threadIdleTimeout);

	Connector createConnector(Server server, String name, int port, Integer idleTimeout, int httpSecurePort, String host,
							  Boolean checkForwaredHeaders);

	/**
	 * Creates a secure (SSL) connector.
	 *
	 * @param name                    the name to give to this connector
	 * @param port                    the port on which the secure port should run
	 * @param idleTimeout             the server connector idle timeout
	 * @param sslKeystore             the path to the keystore
	 * @param sslKeystorePassword     the keystore password
	 * @param sslKeyPassword          the password of the server SSL/TLS private key entry in the key store.
	 * @param host                    the address on which the secure port should listen
	 * @param sslKeystoreType         the SSL/TLS key store type (e.g. jks, jceks, bks).
	 * @param sslKeyAlias             the alias of the server SSL/TLS private key entry in the key store.
	 * @param cipherSuitesIncluded    a list of regular expressions used to match excluded cipher suites.
	 * @param cipherSuitesExcluded    a list of regular expressions used to match included cipher suites.
	 * @param protocolsIncluded       list of SSL/TLS protocols that are acceptable.
	 * @param protocolsExcluded       list of SSL/TLS protocols that are not acceptable.
	 * @param sslRenegotiationAllowed whether TLS renegotiation is allowed.
	 * @param crlPath                 the CRL path.
	 * @param enableCRLDP             whether enable CRLDP.
	 * @param validateCerts           whether validate certs.
	 * @param validatePeerCerts       whether validate peer certs.
	 * @param enableOCSP              whether enable OCSP.
	 * @param ocspResponderURL        online OCSP responder URL
	 * @return a secure connector
	 * @since 0.2.1
	 */
	Connector createSecureConnector(Server server, String name, int port, Integer idleTimeout,
									String sslKeystore, String sslKeystorePassword, String sslKeyPassword,
									String host, String sslKeystoreType, String sslKeyAlias,
									String trustStore, String trustStorePassword, String trustStoreType,
									boolean isClientAuthNeeded, boolean isClientAuthWanted,
									List<String> cipherSuitesIncluded, List<String> cipherSuitesExcluded,
									List<String> protocolsIncluded, List<String> protocolsExcluded,
									Boolean sslRenegotiationAllowed,
									String crlPath,
									Boolean enableCRLDP,
									Boolean validateCerts,
									Boolean validatePeerCerts,
									Boolean enableOCSP,
									String ocspResponderURL,
									Boolean checkForwaredHeaders);

}
