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

public interface JettyFactory {

	JettyServer createServer();

	Connector createConnector(String name, int port, String host, boolean useNIO);

	/**
	 * Creates a secure (SSL) connector.
	 * 
	 * @param name
	 *            the name to give to this connector
	 * @param port
	 *            the port on which the secure port should run
	 * @param sslKeystore
	 *            the path to the keystore
	 * @param sslPassword
	 *            password used for keystore integrity check
	 * @param sslKeyPassword
	 *            keystore password.
	 * @param host
	 *            the address on which the secure port should listen
	 * 
	 * @return a secure connector
	 * 
	 * @since 0.2.1
	 */
	Connector createSecureConnector(String name, int port, String sslKeystore,
			String sslPassword, String sslKeyPassword, String host,
			String sslKeystoreType, boolean isClientAuthNeeded,
			boolean isClientAuthWanted);

}
