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

public interface SecurityConfiguration {

	/**
	 * Returns the name of SSL provider to use with <em>secure</em> connector/listener.
	 * @return the name of SSL provider.
	 */
	String getSslProvider();

	/**
	 * Returns the file path or URL to server keystore.
	 * @return path to the keystore.
	 */
	String getSslKeystore();

	/**
	 * Returns the password for entire keystore (not for the key inside it). Can be encrypted using Jasypt.
	 * @return the password for the keystore.
	 */
	String getSslKeystorePassword();

	/**
	 * Returns the password for ssl private key inside a keystore specified by {@link #getSslKeystore()}.
	 * Can be encrypted using Jasypt.
	 * @return the password for ssl private key.
	 */
	String getSslKeyPassword();

	/**
	 * Returns the server keystore type as specifed by {@link java.security.KeyStore#getInstance(String, String)}
	 * @return keystore type.
	 */
	String getSslKeystoreType();

	/**
	 * Returns the server keystore provider as specifed by {@link java.security.KeyStore#getInstance(String, String)}
	 * @return the name of SSL keystore provider.
	 */
	String getSslKeystoreProvider();

	/**
	 * Returns the algorithm for private key. If not specified,
	 * {@link javax.net.ssl.KeyManagerFactory#getDefaultAlgorithm()} will be used (OpenJDK: {@code SunX509}).
	 * @return
	 */
	String getSslKeyManagerFactoryAlgorithm();

	/**
	 * Returns the alias of the ssl private key inside server keystore.
	 * @return the alias of the ssl private key.
	 */
	String getSslKeyAlias();

	/**
	 * Gets location of server truststore. Not mandatory. In such case, JVM default truststore will be used.
	 * @return
	 */
	String getTruststore();

	/**
	 * Returns the password for entire truststore. Can be encrypted using Jasypt.
	 * @return
	 */
	String getTruststorePassword();

	/**
	 * Returns the server truststore type as specifed by {@link java.security.KeyStore#getInstance(String, String)}
	 * @return truststore type.
	 */
	String getTruststoreType();

	/**
	 * Returns the server truststore provider as specifed by {@link java.security.KeyStore#getInstance(String, String)}
	 * @return the name of SSL truststore provider.
	 */
	String getTruststoreProvider();

	/**
	 * Returns the algorithm for truststore entries. If not specified,
	 * {@link javax.net.ssl.TrustManagerFactory#getDefaultAlgorithm()} will be used (OpenJDK: {@code SunX509}).
	 * @return
	 */
	String getTrustManagerFactoryAlgorithm();

	/**
	 * Set <em>client auth wanted</em> flag as in {@link javax.net.ssl.SSLEngine#setWantClientAuth(boolean)}
	 * @return true if we want client certificate authentication
	 */
	Boolean isClientAuthWanted();

	/**
	 * Set <em>client auth needed</em> flag as in {@link javax.net.ssl.SSLEngine#setNeedClientAuth(boolean)}
	 * @return
	 */
	Boolean isClientAuthNeeded();

	/**
	 * Returns protocol name to use in {@link javax.net.ssl.SSLContext#getInstance(String)}. Defaults to {@code TLSv1.2}
	 * @return
	 */
	String getSslProtocol();

	/**
	 * Returns algorithm name to use in {@link java.security.SecureRandom#getInstance(String)}
	 * @return
	 */
	String getSecureRandomAlgorithm();

	/**
	 * Get included protocols to specify in {@link javax.net.ssl.SSLEngine#setEnabledProtocols(String[])}
	 * @return
	 */
	String[] getProtocolsIncluded();

	/**
	 * Get excluded protocols to not pass to {@link javax.net.ssl.SSLEngine#setEnabledProtocols(String[])}
	 * Jetty: {@code org.eclipse.jetty.util.ssl.SslContextFactory#DEFAULT_EXCLUDED_PROTOCOLS}
	 * @return
	 */
	String[] getProtocolsExcluded();

	/**
	 * Get included cipher suites to specify in {@link javax.net.ssl.SSLEngine#setEnabledCipherSuites(String[])}
	 * @return
	 */
	String[] getCiphersuiteIncluded();

	/**
	 * Get excluded cipher suites to not pass to {@link javax.net.ssl.SSLEngine#setEnabledCipherSuites(String[])}
	 * Jetty: {@code org.eclipse.jetty.util.ssl.SslContextFactory#DEFAULT_EXCLUDED_CIPHER_SUITES}
	 * @return
	 */
	String[] getCiphersuiteExcluded();

	/**
	 * Is SSL renegotiation allowed?
	 * @return
	 */
	Boolean isSslRenegotiationAllowed();

	/**
	 * Get limit of SSL renegotiations
	 * @return
	 */
	Integer getSslRenegotiationLimit();

	/**
	 * Is SSL Session creation enabled? (as hint to {@link javax.net.ssl.SSLEngine}.
	 * @return
	 */
	Boolean getSslSessionsEnabled();

	/**
	 * Cache size for SSL Sessions as in {@link javax.net.ssl.SSLSessionContext#setSessionCacheSize(int)}
	 * @return
	 */
	Integer getSslSessionCacheSize();

	/**
	 * Timeout for SSL Sessions (in seconds) as in {@link javax.net.ssl.SSLSessionContext#setSessionTimeout(int)}
	 * @return
	 */
	Integer getSslSessionTimeout();

	/**
	 * Should certificates in server keystore be validated when keystore is loaded? If {@code true}:<ul>
	 *     <li>Jetty will use {@code org.eclipse.jetty.util.security.CertificateValidator}, which underneath uses
	 *     {@link java.security.cert.CertPathValidator#validate)}.</li>
	 * </ul>
	 * @return
	 */
	Boolean isValidateCerts();

	/**
	 * Should certificates in server truststore be validated when truststore is loaded?
	 * @return
	 */
	Boolean isValidatePeerCerts();

	/**
	 * Should On-Line Certificate Status Protocol (OCSP) be enabled?<ul>
	 *     <li>Jetty calls {@link java.security.Security#setProperty} to set {@code ocsp.enable} property</li>
	 * </ul>
	 * @return
	 */
	Boolean isEnableOCSP();

	/**
	 * Should Certificate Revocation List Distribution Points support (CRLDP) be enabled?<ul>
	 *     <li>Jetty sets {@code com.sun.security.enableCRLDP} system property</li>
	 * </ul>
	 * @return
	 */
	Boolean isEnableCRLDP();

	/**
	 * Get location of CRL list. The list is loaded using {@link java.security.cert.CertificateFactory#generateCRLs}
	 * for {@code X.509} {@link java.security.cert.CertificateFactory}.
	 * @return
	 */
	String getCrlPath();

	/**
	 * Return URL for OCSP responder, though it doesn't seem to be used by Jetty.
	 * @return
	 */
	String getOcspResponderURL();

	/**
	 * Return max length of cert path to use during certificate validation
	 * @return
	 */
	Integer getMaxCertPathLength();

	/**
	 * Returns max nonce age for DIGEST authentication (in ms), defaults to 60s (60000ms)
	 * @return
	 */
	Long getDigestAuthMaxNonceAge();

	/**
	 * Returns max nonce count for DIGEST authentication, defaults to 1024
	 * @return
	 */
	Integer getDigestAuthMaxNonceCount();

	/**
	 * Returns whether to redirect (by default) or forward to error page during FORM authentication
	 * @return
	 */
	Boolean getFormAuthRedirect();

	/**
	 * Checks if configuration values are expected to be encrypted - this triggers a configuration (or tracking)
	 * of Jasypt StringEncryptor (optional dependency)
	 * @return
	 */
	Boolean isEncEnabled();

	/**
	 * Returns a prefix for encrypted property values - defaults to {@code ENC(}
	 * @return
	 */
	String getEncPrefix();

	/**
	 * Returns a prefix for encrypted property values - defaults to {@code )}
	 * @return
	 */
	String getEncSuffix();

	/**
	 * Returns a provider to use for {@link javax.crypto.SecretKeyFactory}. Defaults (Oracle/OpenJDK) to {@code SunJCE}
	 * @return
	 */
	String getEncProvider();

	/**
	 * Returns an algorithm ID to use for {@link javax.crypto.SecretKeyFactory#getInstance(String)}. Defaults to
	 * {@code PBEWithHmacSHA256AndAES_128}
	 * @return
	 */
	String getEncAlgorithm();

	/**
	 * Returns plain text master password to use.
	 * @return
	 */
	String getEncMasterPassword();

	/**
	 * Returns an environment variable name that holds plain text master password to use.
	 * @return
	 */
	String getEncMasterPasswordEnvVariable();

	/**
	 * Returns an system property name that holds plain text master password to use.
	 * @return
	 */
	String getEncMasterPasswordSystemProperty();

	/**
	 * Returns an iteration count to use for PBE encryption
	 * @return
	 */
	Integer getEncIterationCount();

	/**
	 * Returns a decryptor ID. This will be used to construct a
	 * {@code (&(objectClass=org.jasypt.encryption.StringEncryptor)(decryptor=&lt;decryptor ID&gt;))} filter to lookup
	 * for encryptor/decryptor service.
	 * @return
	 */
	String getEncOSGiDecryptorId();

}
