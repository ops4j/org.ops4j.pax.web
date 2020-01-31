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

import java.util.List;

public interface SecurityConfiguration {

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

	List<String> getCiphersuiteIncluded();

	List<String> getCiphersuiteExcluded();

	List<String> getProtocolsIncluded();

	List<String> getProtocolsExcluded();

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

	/**
	 * Returns the name of SSL keystore provider.
	 * @return the name of SSL keystore provider.
	 */
	String getSslKeystoreProvider();

	/**
	 * Returns the name of SSL truststore provider.
	 * @return the name of SSL truststore provider.
	 */
	String getSslTrustStoreProvider();

	/**
	 * Returns the name of SSL provider.
	 * @return the name of SSL provider.
	 */
	String getSslProvider();

	/**
	 * The default implementation will be removed on next major release - 8.0.0
	 * No default auth method with be used if implementation is not provided.
	 *
	 * @return the default auth method, null if not implemented
	 */
	default String getDefaultAuthMethod() {
		return null;
	}

	/**
	 * The default implementation will be removed on next major release - 8.0.0
	 * No default realm name with be used if implementation is not provided.
	 *
	 * @return the default realm name, null if not implemented
	 */
	default String getDefaultRealmName() {
		return null;
	}

}
