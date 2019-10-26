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
package org.ops4j.pax.web.service.spi;

import java.io.File;
import java.net.URL;
import java.util.List;

public interface Configuration {

	Boolean useNIO();

	Boolean checkForwardedHeaders();

	Integer getHttpPort();

	String getHttpConnectorName();

	Boolean isHttpEnabled();

	Integer getConnectorIdleTimeout();

	Boolean isShowStacks();

	Integer getHttpSecurePort();

	String getHttpSecureConnectorName();

	Boolean isHttpSecureEnabled();

	/**
	 * Set the value of the needClientAuth property
	 *
	 * @return true if we require client certificate authentication
	 */
	Boolean isClientAuthNeeded();

	/**
	 * Set the value of the _wantClientAuth property. This property is used when
	 * opening server sockets.
	 *
	 * @return true if we want client certificate authentication
	 */
	Boolean isClientAuthWanted();

	/**
	 * Returns the path to the keystore.
	 *
	 * @return path to the keystore.
	 */
	String getSslKeystore();

	/**
	 * Returns the keystore type.
	 *
	 * @return keystore type.
	 */
	String getSslKeystoreType();

	/**
	 * Returns the password for the keystore.
	 *
	 * @return the password for the keystore.
	 */
	String getSslKeystorePassword();

	/**
	 * Returns the password for keystore integrity check.
	 *
	 * @return the password for keystore integrity check
	 * @deprecated use getSslKeystorePassword() instead.
	 */
	@Deprecated
	String getSslPassword();

	/**
	 * Returns the alias of the ssl private key.
	 *
	 * @return the alias of the ssl private key.
	 */
	String getSslKeyAlias();

	/**
	 * Returns the password for ssl private key.
	 *
	 * @return the password for ssl private key.
	 */
	String getSslKeyPassword();

	/**
	 * Returns the temporary directory, directory that will be set as
	 * javax.servlet.context.tempdir.
	 *
	 * @return the temporary directory
	 */
	File getTemporaryDirectory();

	/**
	 * Returns the time in minutes after which an incative settion times out. If
	 * returned value is null then no time out will be set (in jetty this will
	 * mean that there will be no timeout)
	 *
	 * @return timeout in minutes
	 */
	Integer getSessionTimeout();

	String getSessionCookie();

	String getSessionDomain();

	String getSessionPath();

	String getSessionUrl();

	Boolean getSessionCookieHttpOnly();

	Boolean getSessionCookieSecure();

	Integer getSessionCookieMaxAge();

	String getSessionStoreDirectory();

	Boolean getSessionLazyLoad();

	String getWorkerName();

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

	String getJspScratchDir();

	Integer getJspCheckInterval();

	Boolean getJspClassDebugInfo();

	Boolean getJspDevelopment();

	Boolean getJspEnablePooling();

	String getJspIeClassId();

	String getJspJavaEncoding();

	Boolean getJspKeepgenerated();

	String getJspLogVerbosityLevel();

	Boolean getJspMappedfile();

	Integer getJspTagpoolMaxSize();

	Boolean isLogNCSAFormatEnabled();

	String getLogNCSAFormat();

	String getLogNCSARetainDays();

	Boolean isLogNCSAAppend();

	Boolean isLogNCSAExtended();

	Boolean isLogNCSADispatch();

	String getLogNCSATimeZone();

	String getLogNCSADirectory();

	Boolean getJspPrecompilation();

	List<String> getVirtualHosts();

	Boolean isLogNCSALatency();

	Boolean isLogNCSACookies();

	Boolean isLogNCSAServer();

	List<String> getCiphersuiteIncluded();

	List<String> getCiphersuiteExcluded();

	List<String> getProtocolsIncluded();

	List<String> getProtocolsExcluded();

	Integer getServerMaxThreads();

	Integer getServerMinThreads();

	Integer getServerIdleTimeout();

	String getTrustStore();

	String getTrustStorePassword();

	String getTrustStoreType();

	Boolean isSslRenegotiationAllowed();
	
	String getCrlPath();
	
	Boolean isEnableCRLDP();
	
	Boolean isValidateCerts();
	
	Boolean isValidatePeerCerts();
	
	Boolean isEnableOCSP();
	
	String getOcspResponderURL();
	
	Boolean isEncEnabled();
	
	String getEncMasterPassword();
	
	String getEncAlgorithm();
	
	String getEncPrefix();
	
	String getEncSuffix();

	/**
	 * The default implementation will be removed on next major release - 8.0.0
	 * No default auth method with be used if implementation is not provided.
	 *
	 * @return the default auth method, null if not implemented
	 */
	default String getDefaultAuthMethod(){
		return null;
	}

	/**
	 * The default implementation will be removed on next major release - 8.0.0
	 * No default realm name with be used if implementation is not provided.
	 *
	 * @return the default realm name, null if not implemented
	 */
	default String getDefaultRealmName(){
		return null;
	}
}
