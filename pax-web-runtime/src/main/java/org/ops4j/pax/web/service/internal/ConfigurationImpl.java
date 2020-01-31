/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.JspConfiguration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Service Configuration implementation.</p>
 * <p>This configuration object is a {@link PropertyStore} filled up by resolving properties from
 * underlying {@link PropertyResolver}.</p>
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 22, 2008
 */
public class ConfigurationImpl extends PropertyStore implements Configuration {

	public static Logger LOG = LoggerFactory.getLogger(ConfigurationImpl.class);

	// configuration groups initialized immediately. Configurations may eagerly resolve some properties

	private final ServerConfiguration serverConfiguration;

	/** Property resolver. Cannot be null. */
	private PropertyResolver propertyResolver;

//	/**
//	 * encryptor to decrypt the password
//	 * init it only if necessary
//	 */
//	private StandardPBEStringEncryptor encryptor;

	/**
	 * Creates a new service configuration.
	 *
	 * @param propertyResolver propertyResolver used to resolve properties
	 */
	ConfigurationImpl(final PropertyResolver propertyResolver) {
		NullArgumentException.validateNotNull(propertyResolver, "Property resolver");
		this.propertyResolver = propertyResolver;

		serverConfiguration = new ServerConfigurationImpl();
	}

	@Override
	public ServerConfiguration server() {
		return serverConfiguration;
	}

	@Override
	public SecurityConfiguration security() {
		return null;
	}

	@Override
	public JspConfiguration jsp() {
		return null;
	}

	@Override
	public SessionConfiguration session() {
		return null;
	}

	@Override
	public LogConfiguration logging() {
		return null;
	}

	@Override
	public <T> T get(String propertyName, Class<T> clazz) {
		// this method is a generic way of accessing properties. If a property is not yet
		// resolved, propertyResolver will be consulted
		if (clazz == Integer.class) {
			return clazz.cast(resolveIntegerProperty(propertyName));
		} else if (clazz == Boolean.class) {
			return clazz.cast(resolveBooleanProperty(propertyName));
		} else if (clazz == String.class) {
			return clazz.cast(resolveStringProperty(propertyName));
		} else {
			throw new IllegalArgumentException("Can't convert \"" + propertyName + "\" to " + clazz);
		}
	}

//	@Override
//	public Dictionary<String, Object> getConfiguration() {
//		return (Dictionary<String, Object>) this.dictionary;
//	}
//
//	public void setDictionary(Dictionary<String, ?> dictionary) {
//		this.dictionary = dictionary;
//	}
//
//	/**
//	 * @see Configuration#getConnectorIdleTimeout()
//	 */
//	@Override
//	public Integer getConnectorIdleTimeout() {
//		return getResolvedIntegerProperty(PROPERTY_CONNECTOR_IDLE_TIMEOUT);
//	}
//
//	@Override
//	public Boolean isShowStacks() {
//		return getResolvedBooleanProperty(PROPERTY_SHOW_STACKS);
//	}
//
//	/**
//	 * @see Configuration#getHttpConnectorName()
//	 */
//	@Override
//	public String getHttpConnectorName() {
//		return getResolvedStringProperty(PROPERTY_HTTP_CONNECTOR_NAME);
//	}
//
//	/**
//	 * @see Configuration#useNIO()
//	 */
//	@Override
//	public Boolean useNIO() {
//		return getResolvedBooleanProperty(PROPERTY_HTTP_USE_NIO);
//	}
//
//	/**
//	 * @see Configuration#checkForwardedHeaders()
//	 */
//	@Override
//	public Boolean checkForwardedHeaders() {
//		return getResolvedBooleanProperty(PROPERTY_HTTP_CHECK_FORWARDED_HEADERS);
//	}
//
//	/**
//	 * @see Configuration#isClientAuthNeeded()
//	 */
//	@Override
//	public Boolean isClientAuthNeeded() {
//		return getResolvedBooleanProperty(PROPERTY_SSL_CLIENT_AUTH_NEEDED);
//	}
//
//	/**
//	 * @see Configuration#isClientAuthWanted()
//	 */
//	@Override
//	public Boolean isClientAuthWanted() {
//		return getResolvedBooleanProperty(PROPERTY_SSL_CLIENT_AUTH_WANTED);
//	}
//
//	/**
//	 * @see Configuration#isHttpEnabled()
//	 */
//	@Override
//	public Boolean isHttpEnabled() {
//		return getResolvedBooleanProperty(PROPERTY_HTTP_ENABLED);
//	}
//
//	/**
//	 * @see Configuration#getHttpSecureConnectorName()
//	 */
//	@Override
//	public String getHttpSecureConnectorName() {
//		return getResolvedStringProperty(PROPERTY_HTTP_SECURE_CONNECTOR_NAME);
//	}
//
//	/**
//	 * @see Configuration#isHttpSecureEnabled()
//	 */
//	@Override
//	public Boolean isHttpSecureEnabled() {
//		return getResolvedBooleanProperty(PROPERTY_HTTP_SECURE_ENABLED);
//	}
//
//	@Override
//	public String getSslProvider() {
//		return getResolvedStringProperty(PROPERTY_SSL_PROVIDER);
//	}
//
//	/**
//	 * @see Configuration#getSslKeystore()
//	 */
//	@Override
//	public String getSslKeystore() {
//		return getResolvedStringProperty(PROPERTY_SSL_KEYSTORE);
//	}
//
//	/**
//	 * @see Configuration#getSslKeystoreType()
//	 */
//	@Override
//	public String getSslKeystoreType() {
//		return getResolvedStringProperty(PROPERTY_SSL_KEYSTORE_TYPE);
//	}
//
//	@Override
//	public String getSslKeystoreProvider() {
//		return getResolvedStringProperty(PROPERTY_SSL_KEYSTORE_PROVIDER);
//	}
//
//	/**
//	 * @see Configuration#getSslKeystorePassword()
//	 */
//	@Override
//	public String getSslKeystorePassword() {
//		String keystorePassword = getResolvedStringProperty(PROPERTY_SSL_KEYSTORE_PASSWORD);
//		// Try the deprecated property.
//		if ((null == keystorePassword) || ("".equals(keystorePassword))) {
//			keystorePassword = getResolvedStringProperty(PROPERTY_SSL_PASSWORD);
//		}
//		return this.decryptPassword(keystorePassword);
//	}
//
//	/**
//	 * @see Configuration#getSslPassword()
//	 */
//	@Override
//	public String getSslPassword() {
//	        String password =  getSslKeystorePassword();
//	        return decryptPassword(password);
//	}
//
//
//	/**
//	 * @see Configuration#getSslKeyPassword()
//	 */
//	@Override
//	public String getSslKeyAlias() {
//		return getResolvedStringProperty(PROPERTY_SSL_KEY_ALIAS);
//
//	}
//
//	/**
//	 * @see Configuration#getSslKeyPassword()
//	 */
//	@Override
//	public String getSslKeyPassword() {
//		String privateKeyPassword = getResolvedStringProperty(PROPERTY_SSL_KEY_PASSWORD);
//		// Try the deprecated property.
//		if ((null == privateKeyPassword) || ("".equals(privateKeyPassword))) {
//			privateKeyPassword = getResolvedStringProperty(PROPERTY_SSL_KEYPASSWORD);
//		}
//		return this.decryptPassword(privateKeyPassword);
//	}
//
//	/**
//	 * @see Configuration#getTrustStore()
//	 */
//	@Override
//	public String getTrustStore() {
//		return getResolvedStringProperty(PROPERTY_SSL_TRUST_STORE);
//	}
//
//	/**
//	 * @see Configuration#getTrustStorePassword()
//	 */
//	@Override
//	public String getTrustStorePassword() {
//		String password = getResolvedStringProperty(PROPERTY_SSL_TRUST_STORE_PASSWORD);
//		return this.decryptPassword(password);
//	}
//
//	/**
//	 * @see Configuration#getTrustStoreType()
//	 */
//	@Override
//	public String getTrustStoreType() {
//		return getResolvedStringProperty(PROPERTY_SSL_TRUST_STORE_TYPE);
//	}
//
//	@Override
//	public String getSslTrustStoreProvider() {
//		return getResolvedStringProperty(PROPERTY_SSL_TRUST_STORE_PROVIDER);
//	}
//
//	@Override
//	public Boolean isSslRenegotiationAllowed() {
//		return getResolvedBooleanProperty(PROPERTY_SSL_RENEGOTIATION_ALLOWED);
//	}
//
//	/**
//	 * @see Configuration#getCiphersuiteIncluded()
//	 */
//	@Override
//	public List<String> getCiphersuiteIncluded() {
//		String cipherIncludeString = getResolvedStringProperty(PROPERTY_CIPHERSUITES_INCLUDED);
//		// Try the deprecated property.
//		if ((null == cipherIncludeString) || ("".equals(cipherIncludeString))) {
//			cipherIncludeString = getResolvedStringProperty(PROPERTY_CIPHERSUITE_INCLUDED);
//		}
//		if (cipherIncludeString == null) {
//			return Collections.emptyList();
//		}
//
//		final String[] split = cipherIncludeString.split(",");
//		return Arrays.asList(split);
//	}
//
//	/**
//	 * @see Configuration#getCiphersuiteExcluded()
//	 */
//	@Override
//	public List<String> getCiphersuiteExcluded() {
//		String cipherExcludeString = getResolvedStringProperty(PROPERTY_CIPHERSUITES_EXCLUDED);
//		// Try the deprecated property.
//		if ((null == cipherExcludeString) || ("".equals(cipherExcludeString))) {
//			cipherExcludeString = getResolvedStringProperty(PROPERTY_CIPHERSUITE_EXCLUDED);
//		}
//		if (cipherExcludeString == null) {
//			return Collections.emptyList();
//		}
//
//		final String[] split = cipherExcludeString.split(",");
//		return Arrays.asList(split);
//	}
//
//	/**
//	 * @see Configuration#getProtocolsIncluded()
//	 */
//	@Override
//	public List<String> getProtocolsIncluded() {
//		String protocolsIncludedString = getResolvedStringProperty(PROPERTY_PROTOCOLS_INCLUDED);
//		if (protocolsIncludedString == null) {
//			return Collections.emptyList();
//		}
//
//		String[] split = protocolsIncludedString.split(",");
//		return Arrays.asList(split);
//	}
//
//	/**
//	 * @see Configuration#getProtocolsExcluded()
//	 */
//	@Override
//	public List<String> getProtocolsExcluded() {
//		String protocolsExcludedString = getResolvedStringProperty(PROPERTY_PROTOCOLS_EXCLUDED);
//		if (protocolsExcludedString == null) {
//			return Collections.emptyList();
//		}
//
//		String[] split = protocolsExcludedString.split(",");
//		return Arrays.asList(split);
//	}
//
//
//	/**
//	 * @see Configuration#getTemporaryDirectory()
//	 */
//	@Override
//	public File getTemporaryDirectory() {
//		try {
//			if (!contains(PROPERTY_TEMP_DIR)) {
//				final String tempDirPath = propertyResolver
//						.get(PROPERTY_TEMP_DIR);
//				File tempDir = null;
//				if (tempDirPath != null) {
//					if (tempDirPath.startsWith("file:")) {
//						tempDir = new File(new URI(tempDirPath));
//					} else {
//						tempDir = new File(tempDirPath);
//					}
//					if (!tempDir.exists()) {
//						tempDir.mkdirs();
//					}
//				}
//				return set(PROPERTY_TEMP_DIR, tempDir);
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property " + PROPERTY_TEMP_DIR
//					+ " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(PROPERTY_TEMP_DIR);
//	}
//
//	@Override
//	public File getConfigurationDir() {
//		try {
//			if (!contains(PROPERTY_SERVER_CONFIGURATION_FILE)) {
//				final String serverConfigurationFileName = propertyResolver
//						.get(PROPERTY_SERVER_CONFIGURATION_FILE);
//				File configurationFile;
//				if (serverConfigurationFileName.startsWith("file:")) {
//					configurationFile = new File(new URI(
//							serverConfigurationFileName));
//				} else {
//					configurationFile = new File(serverConfigurationFileName);
//				}
//				if (!configurationFile.exists()) {
//					LOG.debug("Reading from configured path for the configuration property "
//							+ PROPERTY_SERVER_CONFIGURATION_FILE
//							+ " has failed");
//				}
//				return set(PROPERTY_SERVER_CONFIGURATION_FILE,
//						configurationFile);
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property "
//					+ PROPERTY_SERVER_CONFIGURATION_FILE + " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(PROPERTY_SERVER_CONFIGURATION_FILE);
//	}
//
//	@Override
//	public URL getConfigurationURL() {
//		try {
//			if (!contains(PROPERTY_SERVER_CONFIGURATION_URL)) {
//				final String serverConfigurationURL = propertyResolver
//						.get(PROPERTY_SERVER_CONFIGURATION_URL);
//				URL configurationURL = new URL(serverConfigurationURL);
//				return set(PROPERTY_SERVER_CONFIGURATION_URL, configurationURL);
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property "
//					+ PROPERTY_SERVER_CONFIGURATION_URL + " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(PROPERTY_SERVER_CONFIGURATION_URL);
//	}
//
//	/**
//	 * @see Configuration#getSessionTimeout()
//	 */
//	@Override
//	public Integer getSessionTimeout() {
//		return getResolvedIntegerProperty(PROPERTY_SESSION_TIMEOUT);
//
//	}
//
//	@Override
//	public String getSessionCookie() {
//		return getResolvedStringProperty(PROPERTY_SESSION_COOKIE);
//	}
//
//	@Override
//	public String getSessionDomain() {
//		return getResolvedStringProperty(PROPERTY_SESSION_DOMAIN);
//	}
//
//	@Override
//	public String getSessionPath() {
//		return getResolvedStringProperty(PROPERTY_SESSION_PATH);
//	}
//
//	@Override
//	public String getSessionUrl() {
//		return getResolvedStringProperty(PROPERTY_SESSION_URL);
//	}
//
//	@Override
//	public Boolean getSessionCookieHttpOnly() {
//		return getResolvedBooleanProperty(PROPERTY_SESSION_COOKIE_HTTP_ONLY);
//	}
//
//	@Override
//	public Boolean getSessionCookieSecure() {
//		return getResolvedBooleanProperty(PROPERTY_SESSION_COOKIE_SECURE);
//	}
//
//	@Override
//	public Integer getSessionCookieMaxAge() {
//		return getResolvedIntegerProperty(PROPERTY_SESSION_COOKIE_MAX_AGE);
//	}
//
//	@Override
//	public Boolean getSessionLazyLoad() {
//		return getResolvedBooleanProperty(PROPERTY_SESSION_LAZY_LOAD);
//	}
//
//	@Override
//	public String getSessionStoreDirectory() {
//		return getResolvedStringProperty(PROPERTY_SESSION_STORE_DIRECTORY);
//	}
//
//	@Override
//	public String getWorkerName() {
//		return getResolvedStringProperty(PROPERTY_WORKER_NAME);
//	}
//
//	/**
//	 * @see Configuration#getListeningAddresses()
//	 */
//	@Override
//	public String[] getListeningAddresses() {
//		try {
//			if (!contains(PROPERTY_LISTENING_ADDRESSES)) {
//				String interfacesString = propertyResolver
//						.get(PROPERTY_LISTENING_ADDRESSES);
//				String[] interfaces = interfacesString == null ? new String[0]
//						: interfacesString.split(",");
//				return set(PROPERTY_LISTENING_ADDRESSES, interfaces);
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property "
//					+ PROPERTY_LISTENING_ADDRESSES + " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(PROPERTY_LISTENING_ADDRESSES);
//	}
//
//	@Override
//	public String getJspScratchDir() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedStringProperty(PaxWebConstants.PROPERTY_JSP_SCRATCH_DIR);
//	}
//
//	@Override
//	public Integer getJspCheckInterval() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedIntegerProperty(PaxWebConstants.PROPERTY_JSP_CHECK_INTERVAL);
//	}
//
//	@Override
//	public Boolean getJspClassDebugInfo() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_DEBUG_INFO);
//	}
//
//	@Override
//	public Boolean getJspDevelopment() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_DEVELOPMENT);
//	}
//
//	@Override
//	public Boolean getJspEnablePooling() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_ENABLE_POOLING);
//	}
//
//	@Override
//	public String getJspIeClassId() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedStringProperty(PaxWebConstants.PROPERTY_JSP_IE_CLASS_ID);
//	}
//
//	@Override
//	public String getJspJavaEncoding() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedStringProperty(PaxWebConstants.PROPERTY_JSP_JAVA_ENCODING);
//	}
//
//	@Override
//	public Boolean getJspKeepgenerated() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_KEEP_GENERATED);
//	}
//
//	@Override
//	public String getJspLogVerbosityLevel() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedStringProperty(PaxWebConstants.PROPERTY_JSP_LOG_VERBOSITY_LEVEL);
//	}
//
//	@Override
//	public Boolean getJspMappedfile() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_MAPPED_FILE);
//	}
//
//	@Override
//	public Integer getJspTagpoolMaxSize() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		// Only when JSPs are available the constants can be read.
//		return getResolvedIntegerProperty(PaxWebConstants.PROPERTY_JSP_TAGPOOL_MAX_SIZE);
//	}
//
//	@Override
//	public Boolean getJspPrecompilation() {
//		// Just in case JSP is not available this parameter is useless
//		if (!SupportUtils.isJSPAvailable()) {
//			return null;
//		}
//
//		return getResolvedBooleanProperty(PaxWebConstants.PROPERTY_JSP_PRECOMPILATION);
//	}
//
//	@Override
//	public Boolean isLogNCSAFormatEnabled() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_ENABLED);
//	}
//
//	@Override
//	public String getLogNCSAFormat() {
//		return getResolvedStringProperty(PROPERTY_LOG_NCSA_FORMAT);
//	}
//
//	@Override
//	public String getLogNCSARetainDays() {
//		return getResolvedStringProperty(PROPERTY_LOG_NCSA_RETAINDAYS);
//	}
//
//	@Override
//	public Boolean isLogNCSAAppend() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_APPEND);
//	}
//
//	@Override
//	public Boolean isLogNCSAExtended() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_EXTENDED);
//	}
//
//	@Override
//	public Boolean isLogNCSADispatch() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_DISPATCH);
//	}
//
//	@Override
//	public String getLogNCSATimeZone() {
//		return getResolvedStringProperty(PROPERTY_LOG_NCSA_LOGTIMEZONE);
//	}
//
//	@Override
//	public String getLogNCSADirectory() {
//		return getResolvedStringProperty(PROPERTY_LOG_NCSA_LOGDIR);
//	}
//
//	@Override
//	public String toString() {
//		return new StringBuilder().append(this.getClass().getSimpleName())
//				.append("{").append("http enabled=").append(isHttpEnabled())
//				.append(",http port=").append(getHttpPort())
//				.append(",http secure enabled=").append(isHttpSecureEnabled())
//				.append(",http secure port=").append(getHttpSecurePort())
//				.append(",ssl keystore=").append(getSslKeystore())
//				.append(",ssl keystoreType=").append(getSslKeystoreType())
//				.append(",session timeout=").append(getSessionTimeout())
//				.append(",session url=").append(getSessionUrl())
//				.append(",session cookie=").append(getSessionCookie())
//				.append(",session cookie httpOnly=")
//				.append(getSessionCookieHttpOnly()).append(",worker name=")
//				.append(getWorkerName()).append(",listening addresses=")
//				.append(Arrays.toString(getListeningAddresses())).append("}")
//				.toString();
//	}
//
//	private String getResolvedStringProperty(String property) {
//		try {
//			if (!contains(property)) {
//				return set(property, propertyResolver.get(property));
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property " + property
//					+ " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(property);
//	}
//
//	private Boolean getResolvedBooleanProperty(String property) {
//		try {
//			if (!contains(property)) {
//				String resolvedProperty = propertyResolver.get(property);
//				return set(
//						property,
//						resolvedProperty == null ? null : Boolean
//								.valueOf(resolvedProperty));
//			}
//			//CHECKSTYLE:OFF
//		} catch (Exception ignore) {
//			LOG.debug("Reading configuration property " + property
//					+ " has failed");
//		}
//		//CHECKSTYLE:ON
//		return get(property);
//	}

//	@Override
//	public List<String> getVirtualHosts() {
//		List<String> virtualHosts = new LinkedList<>();
//		String virtualHostListString = this
//				.getResolvedStringProperty(PROPERTY_VIRTUAL_HOST_LIST);
//		if ((virtualHostListString != null)
//				&& (virtualHostListString.length() > 0)) {
//			String[] virtualHostArray = virtualHostListString.split(",");
//			for (String virtualHost : virtualHostArray) {
//				virtualHosts.add(virtualHost.trim());
//			}
//		}
//		String connectorListString = this
//				.getResolvedStringProperty(PROPERTY_CONNECTOR_LIST);
//		if ((connectorListString != null) && (connectorListString.length() > 0)) {
//			String[] connectorArray = connectorListString.split(",");
//			for (String connector : connectorArray) {
//				virtualHosts.add("@" + connector.trim());
//			}
//		}
//		return virtualHosts;
//	}
//
//	@Override
//	public Boolean isLogNCSALatency() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_LATENCY);
//	}
//
//	@Override
//	public Boolean isLogNCSACookies() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_COOKIES);
//	}
//
//	@Override
//	public Boolean isLogNCSAServer() {
//		return getResolvedBooleanProperty(PROPERTY_LOG_NCSA_SERVER);
//	}
//
//	@Override
//	public Integer getServerMaxThreads() {
//		return getResolvedIntegerProperty(PROPERTY_MAX_THREADS);
//	}
//
//	@Override
//	public Integer getServerMinThreads() {
//		return getResolvedIntegerProperty(PROPERTY_MIN_THREADS);
//	}
//
//	@Override
//	public Integer getServerIdleTimeout() {
//		return getResolvedIntegerProperty(PROPERTY_IDLE_TIMEOUT);
//	}
//
//    @Override
//    public String getCrlPath() {
//        return getResolvedStringProperty(PROPERTY_CRL_PATH);
//    }
//
//    @Override
//    public Boolean isEnableCRLDP() {
//        return getResolvedBooleanProperty(PROPERTY_ENABLE_CRLDP);
//    }
//
//    @Override
//    public Boolean isValidateCerts() {
//        return getResolvedBooleanProperty(PROPERTY_VALIDATE_CERTS);
//    }
//
//    @Override
//    public Boolean isValidatePeerCerts() {
//        return getResolvedBooleanProperty(PROPERTY_VALIDATE_PEER_CERTS);
//    }
//
//    @Override
//    public Boolean isEnableOCSP() {
//        return getResolvedBooleanProperty(PROPERTY_ENABLE_OCSP);
//    }
//
//    @Override
//    public String getOcspResponderURL() {
//        return getResolvedStringProperty(PROPERTY_OCSP_RESPONDER_URL);
//    }
//
//    @Override
//    public Boolean isEncEnabled() {
//        Boolean encEnabled =  getResolvedBooleanProperty(PROPERTY_ENC_ENABLED);
//        if (encEnabled) {
//            this.encryptor = new StandardPBEStringEncryptor();
//            this.encryptor.setPassword(getEncMasterPassword());
//            this.encryptor.setAlgorithm(getEncAlgorithm());
//        }
//        return encEnabled;
//    }
//
//    @Override
//    public String getEncMasterPassword() {
//        return getResolvedStringProperty(PROPERTY_ENC_MASTERPASSWORD);
//    }
//
//    @Override
//    public String getEncAlgorithm() {
//        return getResolvedStringProperty(PROPERTY_ENC_ALGORITHM);
//    }
//
//    @Override
//    public String getEncPrefix() {
//        return getResolvedStringProperty(PROPERTY_ENC_PREFIX);
//    }
//
//    @Override
//    public String getEncSuffix() {
//        return getResolvedStringProperty(PROPERTY_ENC_SUFFIX);
//    }
//
//	@Override
//	public String getDefaultAuthMethod() {return getResolvedStringProperty(PROPERTY_DEFAULT_AUTHMETHOD); }
//
//	@Override
//	public String getDefaultRealmName() {return getResolvedStringProperty(PROPERTY_DEFAULT_REALMNAME); }
//
//    private String decryptPassword(String password) {
//        if (this.encryptor == null && isEncEnabled()) {
//            String masterPassword;
//            //get encryptor master password from env variable first
//            masterPassword = System.getenv("PROPERTY_ENC_MASTERPASSWORD");
//            if (masterPassword == null || masterPassword.length() == 0) {
//                //get encryptor master password from system property
//                masterPassword = System.getProperty(PROPERTY_ENC_MASTERPASSWORD);
//            }
//            if (masterPassword == null || masterPassword.length() == 0) {
//                masterPassword = getEncMasterPassword();
//            }
//            this.encryptor = new StandardPBEStringEncryptor();
//            this.encryptor.setPassword(masterPassword);
//            this.encryptor.setAlgorithm(getEncAlgorithm());
//        }
//        if (this.encryptor != null) {
//            if (password.startsWith(getEncPrefix()) && password.endsWith(getEncSuffix())) {
//                //encrypted password, need decrypt it
//                String encryptedPassword = password.substring(getEncPrefix().length(), password.length() - getEncSuffix().length());
//                return this.encryptor.decrypt(encryptedPassword);
//            }
//        }
//        return password;
//    }

	// -- private resolution methods
	//  - eagerXXXProperty methods assume the property was resolved when configuration was created
	//  - resolveXXXProperty methods resolve properties as needed

	private Integer eagerIntegerProperty(String property) {
		return super.get(property);
	}

	private Integer resolveIntegerProperty(String property) {
		if (!contains(property)) {
			ensurePropertyResolver(property);
			try {
				String resolvedProperty = propertyResolver.get(property);
				return set(property, resolvedProperty == null ? null : Integer.valueOf(propertyResolver.get(property)));
			} catch (Exception e) {
				LOG.debug("Reading configuration property " + property + " has failed: {}", e.getMessage());
			}
		}
		return super.get(property);
	}

	private Boolean resolveBooleanProperty(String property) {
		if (!contains(property)) {
			ensurePropertyResolver(property);
			try {
				String resolvedProperty = propertyResolver.get(property);
				return set(property, resolvedProperty == null ? null : Boolean.valueOf(propertyResolver.get(property)));
			} catch (Exception e) {
				LOG.debug("Reading configuration property " + property + " has failed: {}", e.getMessage());
			}
		}
		return super.get(property);
	}

	private String resolveStringProperty(String property) {
		if (!contains(property)) {
			ensurePropertyResolver(property);
			try {
				String resolvedProperty = propertyResolver.get(property);
				return set(property, resolvedProperty == null ? null : propertyResolver.get(property));
			} catch (Exception e) {
				LOG.debug("Reading configuration property " + property + " has failed: {}", e.getMessage());
			}
		}
		return super.get(property);
	}

	private void ensurePropertyResolver(String property) {
		if (this.propertyResolver == null) {
			throw new IllegalStateException("Can't access property resolver to resolve \"" + property + "\" property");
		}
	}

	private class ServerConfigurationImpl implements ServerConfiguration {

		private ServerConfigurationImpl() {
			// eager resolution of some important properties
			resolveIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT);
			resolveIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE);
		}

		@Override
		public Integer getHttpPort() {
			return eagerIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT);
		}

		@Override
		public Integer getHttpSecurePort() {
			return eagerIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE);
		}

		@Override
		public Boolean useNIO() {
			return null;
		}

		@Override
		public Boolean checkForwardedHeaders() {
			return null;
		}

		@Override
		public String getHttpConnectorName() {
			return null;
		}

		@Override
		public Boolean isHttpEnabled() {
			return null;
		}

		@Override
		public Integer getConnectorIdleTimeout() {
			return null;
		}

		@Override
		public Boolean isShowStacks() {
			return null;
		}

		@Override
		public String getHttpSecureConnectorName() {
			return null;
		}

		@Override
		public Boolean isHttpSecureEnabled() {
			return null;
		}

		@Override
		public File getTemporaryDirectory() {
			return null;
		}

		@Override
		public String[] getListeningAddresses() {
			return new String[0];
		}

		@Override
		public File getConfigurationDir() {
			return null;
		}

		@Override
		public URL getConfigurationURL() {
			return null;
		}

		@Override
		public Integer getServerMaxThreads() {
			return null;
		}

		@Override
		public Integer getServerMinThreads() {
			return null;
		}

		@Override
		public Integer getServerIdleTimeout() {
			return null;
		}

		@Override
		public List<String> getVirtualHosts() {
			return Collections.emptyList();
		}

		@Override
		public String getWorkerName() {
			return null;
		}

		@Override
		public Boolean isEncEnabled() {
			return null;
		}

		@Override
		public String getEncMasterPassword() {
			return null;
		}

		@Override
		public String getEncAlgorithm() {
			return null;
		}

		@Override
		public String getEncPrefix() {
			return null;
		}

		@Override
		public String getEncSuffix() {
			return null;
		}
	}

}