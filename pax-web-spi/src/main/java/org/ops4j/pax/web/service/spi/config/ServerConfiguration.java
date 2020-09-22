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
import java.util.List;

import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.model.events.ElementEvent;

public interface ServerConfiguration {

	/**
	 * Returns the temporary directory, directory that will be set as {@link javax.servlet.ServletContext#TEMPDIR}.
	 *
	 * @return the temporary directory
	 */
	File getTemporaryDirectory();

	/**
	 * Returns the files to read external server configuration from. It's super useful for Jetty, where
	 * we can easily simulate {@code JETTY_HOME/etc} directory. Files must be in correct order (as enforced by
	 * given runtime). It can also be single file location or even {@code null} to let the runtime be configured
	 * using only PID configuration.
	 *
	 * @since Pax Web 8
	 * @return configuration directory
	 */
	File[] getConfigurationFiles();

	/**
	 * Get a TCP port to use for HTTP protocol. Uses {@link PaxWebConfig#PID_CFG_HTTP_PORT}
	 * property.
	 * @return
	 */
	Integer getHttpPort();

	/**
	 * Get a TCP port to use for HTTPS protocol. Uses {@link PaxWebConfig#PID_CFG_HTTP_PORT_SECURE}
	 * property.
	 * @return
	 */
	Integer getHttpSecurePort();

	/**
	 * Is default http connector/listener enabled? Uses {@link PaxWebConfig#PID_CFG_HTTP_ENABLED}
	 * @return
	 */
	Boolean isHttpEnabled();

	/**
	 * Is default https connector/listener enabled? Uses {@link PaxWebConfig#PID_CFG_HTTP_SECURE_ENABLED}
	 * @return
	 */
	Boolean isHttpSecureEnabled();

	/**
	 * Returns the addresses to bind connector/listener to. Defaults to one element array with {@code 0.0.0.0}.
	 * @return
	 */
	String[] getListeningAddresses();

	/**
	 * Gets the name to use for <em>default</em> connector/listener.
	 * @return
	 */
	String getHttpConnectorName();

	/**
	 * Gets the name to use for <em>secure</em> connector/listener.
	 * @return
	 */
	String getHttpSecureConnectorName();

	/**
	 * <p>Gets the <em>idle timeout</em> (in milliseconds) to be used with server connectors.
	 * <em>Idle timeout</em> is like <em>socket read timeout</em>, but on server side.<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.server.AbstractConnector#setIdleTimeout(long)}</li>
	 *     <li>Tomcat: {@code org.apache.coyote.AbstractProtocol#setConnectionTimeout(int)}</li>
	 * </ul></p>
	 * @return
	 */
	Integer getConnectorIdleTimeout();

	/**
	 * <p>Gets the server thread idle timeout in milliseconds.
	 * <ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setIdleTimeout(int)}</li>
	 *     <li>Tomcat: {@code org.apache.catalina.core.StandardThreadExecutor#setMaxIdleTime(int)}</li>
	 * </ul></p>
	 * @return
	 */
	Integer getServerIdleTimeout();

	/**
	 * <p>Gets maximum number of threads to use in server runtime. This value MAY mean something different in
	 * different runtimes.<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setMaxThreads(int)}</li>
	 *     <li>Tomcat: {@code org.apache.catalina.core.StandardThreadExecutor#setMaxThreads(int)}</li>
	 * </ul></p>
	 * @return
	 */
	Integer getServerMaxThreads();

	/**
	 * <p>Gets minimum number of threads to use in server runtime. This value MAY mean something different in
	 * different runtimes.<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setMinThreads(int)}</li>
	 *     <li>Tomcat: {@code org.apache.catalina.core.StandardThreadExecutor#setMinSpareThreads(int)}</li>
	 * </ul></p>
	 * @return
	 */
	Integer getServerMinThreads();

	/**
	 * If target container allows, this method specifies a prefix for thread names to use.<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setName(java.lang.String)} (defaults
	 *     to "qtp" + hashcode).</li>
	 *     <li>Tomcat: {@code org.apache.catalina.core.StandardThreadExecutor#setNamePrefix(java.lang.String)}</li>
	 * </ul>
	 * @return
	 */
	String getServerThreadNamePrefix();

	/**
	 * Should the connector handle {@code X-Forwarded-*} / {@code X-Proxied-*} headers?<ul>
	 *     <li>Jetty: {@code org.eclipse.jetty.server.ForwardedRequestCustomizer}</li>
	 *     <li>Undertow: {@code io.undertow.server.handlers.ProxyPeerAddressHandler}</li>
	 * </ul>
	 * @return
	 */
	Boolean checkForwardedHeaders();

	/**
	 * Internal Pax Web configuration option to specify number of threads to dispatch
	 * {@link ElementEvent} events.
	 * @return
	 */
	Integer getEventDispatcherThreadCount();

	/**
	 * Flag that specifies whether stack traces should be visible in error pages.
	 * @return
	 */
	Boolean isShowStacks();











	List<String> getVirtualHosts();





	Boolean isEncEnabled();

	String getEncMasterPassword();

	String getEncAlgorithm();

	String getEncPrefix();

	String getEncSuffix();

}
