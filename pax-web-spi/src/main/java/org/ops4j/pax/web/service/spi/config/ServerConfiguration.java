/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.spi.config;

import java.io.File;
import java.net.URL;
import java.util.List;

public interface ServerConfiguration {

	/**
	 * Get a TCP port to use for HTTP protocol. Uses {@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_PORT}
	 * property.
	 * @return
	 */
	Integer getHttpPort();

	/**
	 * Get a TCP port to use for HTTPS protocol. Uses {@link org.ops4j.pax.web.service.PaxWebConfig#PID_CFG_HTTP_PORT_SECURE}
	 * property.
	 * @return
	 */
	Integer getHttpSecurePort();





	Boolean useNIO();

	Boolean checkForwardedHeaders();


	String getHttpConnectorName();

	Boolean isHttpEnabled();

	Integer getConnectorIdleTimeout();

	Boolean isShowStacks();


	String getHttpSecureConnectorName();

	Boolean isHttpSecureEnabled();

	/**
	 * Returns the temporary directory, directory that will be set as
	 * javax.servlet.context.tempdir.
	 *
	 * @return the temporary directory
	 */
	File getTemporaryDirectory();

	/**
	 * Returns the addresses to bind to
	 *
	 * @return addresses
	 */
	String[] getListeningAddresses();

	/**
	 * Returns the directory containing the external configuration
	 *
	 * @return configuration directory
	 */
	File getConfigurationDir();

	/**
	 * Returns the URL of external web server configuration
	 *
	 * @return configuration URL
	 */
	URL getConfigurationURL();

	Integer getServerMaxThreads();

	Integer getServerMinThreads();

	Integer getServerIdleTimeout();

	List<String> getVirtualHosts();

	String getWorkerName();

	Boolean isEncEnabled();

	String getEncMasterPassword();

	String getEncAlgorithm();

	String getEncPrefix();

	String getEncSuffix();

}
