/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.SessionCookieConfig;

import org.ops4j.pax.web.service.PaxWebConfig;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.JspConfiguration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.ResourceConfiguration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.servlet.DefaultSessionCookieConfig;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Service Configuration implementation.</p>
 *
 * <p>This configuration object is a {@link PropertyStore} filled up by resolving properties from
 * underlying {@link PropertyResolver}.</p>
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 22, 2008
 */
public class ConfigurationImpl extends PropertyStore implements Configuration {

	public static final Logger LOG = LoggerFactory.getLogger(ConfigurationImpl.class);

	private final String id;

	// configuration groups initialized immediately. Configurations may eagerly resolve some properties

	private final ServerConfiguration serverConfiguration;
	private final SecurityConfiguration securityConfiguration;
	private final ResourceConfiguration resourceConfiguration;
	private final SessionConfiguration sessionConfiguration;
	private final JspConfiguration jspConfiguration;
	private final LogConfiguration logConfiguration;

	/** Property resolver. Cannot be null. */
	private final PropertyResolver propertyResolver;

	/** Low level access to as many source properties as possible */
	private final Map<String, String> sourceProperties;

//	/**
//	 * encryptor to decrypt the password
//	 * init it only if necessary
//	 */
//	private StandardPBEStringEncryptor encryptor;

	/**
	 * Creates a new service configuration.
	 *
	 * @param propertyResolver propertyResolver used to resolve properties
	 * @param sourceProperties
	 */
	ConfigurationImpl(final PropertyResolver propertyResolver, Map<String, String> sourceProperties) {
		this.propertyResolver = propertyResolver;

		// review existing properties and override them with ones from the resolver (if they exist)
		for (String key : sourceProperties.keySet()) {
			String v = propertyResolver.get(key);
			if (v != null && !"".equals(v)) {
				// override, so for example a framework property overrides a value from metatype.xml
				sourceProperties.put(key, v);
			}
		}

		this.sourceProperties = Collections.unmodifiableMap(sourceProperties);

		id = UUID.randomUUID().toString();

		serverConfiguration = new ServerConfigurationImpl();
		securityConfiguration = new SecurityConfigurationImpl();
		resourceConfiguration = new ResourceConfigurationImpl();
		sessionConfiguration = new SessionConfigurationImpl();
		jspConfiguration = new JspConfigurationImpl(serverConfiguration);
		logConfiguration = new LogConfigurationImpl();
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public ServerConfiguration server() {
		return serverConfiguration;
	}

	@Override
	public SecurityConfiguration security() {
		return securityConfiguration;
	}

	@Override
	public ResourceConfiguration resources() {
		return resourceConfiguration;
	}

	@Override
	public JspConfiguration jsp() {
		return jspConfiguration;
	}

	@Override
	public SessionConfiguration session() {
		return sessionConfiguration;
	}

	@Override
	public LogConfiguration logging() {
		return logConfiguration;
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

	@Override
	public Map<String, String> all() {
		return this.sourceProperties;
	}

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

	private Long resolveLongProperty(String property) {
		if (!contains(property)) {
			ensurePropertyResolver(property);
			try {
				String resolvedProperty = propertyResolver.get(property);
				return set(property, resolvedProperty == null ? null : Long.valueOf(propertyResolver.get(property)));
			} catch (Exception e) {
				LOG.debug("Reading configuration property " + property + " has failed: {}", e.getMessage());
			}
		}
		return super.get(property);
	}

	private Boolean eagerBooleanProperty(String property) {
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

		private File tmpDir = null;
		private String[] listeningAddresses = null;
		private File[] externalConfigurations = null;

		private final int eventDispatcherThreadCount;

		private final boolean showStacks;

		@SuppressWarnings("deprecation")
		private ServerConfigurationImpl() {
			// eager resolution of some important properties
			resolveIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT);
			resolveIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE);
			resolveBooleanProperty(PaxWebConfig.PID_CFG_HTTP_ENABLED);
			resolveBooleanProperty(PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED);

			// tmp directory
			String tmpDir = resolveStringProperty(PaxWebConfig.PID_CFG_TEMP_DIR);
			if (tmpDir == null) {
				tmpDir = resolveStringProperty("java.io.tmpdir");
			}
			if (tmpDir == null) {
				throw new IllegalStateException("Can't determine java.io.tmpdir property");
			}
			tmpDir = Utils.resolve(tmpDir);
			File possibleTmpDir = null;
			if (tmpDir.startsWith("file:")) {
				possibleTmpDir = new File(URI.create(tmpDir));
			} else {
				possibleTmpDir = new File(tmpDir);
			}
			if (possibleTmpDir.isFile()) {
				throw new IllegalStateException(possibleTmpDir + " can't be used as temporary directory");
			}
			if (!possibleTmpDir.isDirectory()) {
				possibleTmpDir.mkdirs();
			}
			this.tmpDir = possibleTmpDir;

			// listening address(es)
			String listeningAddresses = resolveStringProperty(PaxWebConfig.PID_CFG_LISTENING_ADDRESSES);
			if (listeningAddresses == null || "".equals(listeningAddresses.trim())) {
				listeningAddresses = "0.0.0.0";
			}
			this.listeningAddresses = listeningAddresses.split("\\s*,\\s*");

			// external config location
			String externalFile = resolveStringProperty(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILES);
			if (externalFile == null) {
				externalFile = resolveStringProperty(PaxWebConfig.PID_CFG_SERVER_CONFIGURATION_FILE);
			}
			if (externalFile == null || "".equals(externalFile.trim())) {
				this.externalConfigurations = new File[0];
			} else {
				String[] locations = externalFile.split("\\s*,\\s*");
				File[] fileLocations = new File[locations.length];
				int idx = 0;
				for (String location : locations) {
					File f = new File(location);
					if (!f.isFile()) {
						throw new IllegalArgumentException("External configuration " + f + " is not available");
					}
					fileLocations[idx++] = f;
				}
				this.externalConfigurations = fileLocations;
			}
			Integer eventDispatcherThreadCount = resolveIntegerProperty(PaxWebConfig.PID_CFG_EVENT_DISPATCHER_THREAD_COUNT);
			this.eventDispatcherThreadCount = eventDispatcherThreadCount == null ? 1 : eventDispatcherThreadCount;

			Boolean stacks = resolveBooleanProperty(PaxWebConfig.PID_CFG_SHOW_STACKS);
			showStacks = stacks != null && stacks;
		}

		@Override
		public File getTemporaryDirectory() {
			return tmpDir;
		}

		@Override
		public File[] getConfigurationFiles() {
			return externalConfigurations;
		}

		@Override
		public Integer getHttpPort() {
			return eagerIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT);
		}

		@Override
		public Integer getHttpSecurePort() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_HTTP_PORT_SECURE);
		}

		@Override
		public Boolean isHttpEnabled() {
			return eagerBooleanProperty(PaxWebConfig.PID_CFG_HTTP_ENABLED);
		}

		@Override
		public Boolean isHttpSecureEnabled() {
			return eagerBooleanProperty(PaxWebConfig.PID_CFG_HTTP_SECURE_ENABLED);
		}

		@Override
		public String[] getListeningAddresses() {
			return listeningAddresses;
		}

		@Override
		public Integer getConnectorIdleTimeout() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_CONNECTOR_IDLE_TIMEOUT);
		}

		@Override
		public String getHttpConnectorName() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_HTTP_CONNECTOR_NAME);
		}

		@Override
		public String getHttpSecureConnectorName() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_HTTP_SECURE_CONNECTOR_NAME);
		}

		@Override
		public Integer getServerIdleTimeout() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SERVER_IDLE_TIMEOUT);
		}

		@Override
		public Integer getServerMaxThreads() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SERVER_MAX_THREADS);
		}

		@Override
		public Integer getServerMinThreads() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SERVER_MIN_THREADS);
		}

		@Override
		public String getServerThreadNamePrefix() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SERVER_THREAD_NAME_PREFIX);
		}

		@Override
		public Boolean checkForwardedHeaders() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_HTTP_CHECK_FORWARDED_HEADERS);
		}

		@Override
		public Integer getEventDispatcherThreadCount() {
			return this.eventDispatcherThreadCount;
		}

		@Override
		public Boolean isShowStacks() {
			return showStacks;
		}

		@Override
		public List<String> getVirtualHosts() {
			return Collections.emptyList();
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

	private class SecurityConfigurationImpl implements SecurityConfiguration {

		private String[] includedProtocols = new String[0];
		private String[] excludedProtocols = new String[0];
		private String[] includedCipherSuites = new String[0];
		private String[] excludedCipherSuites = new String[0];

		SecurityConfigurationImpl() {
			String includedProtocols = resolveStringProperty(PaxWebConfig.PID_CFG_PROTOCOLS_INCLUDED);
			String excludedProtocols = resolveStringProperty(PaxWebConfig.PID_CFG_PROTOCOLS_EXCLUDED);
			String includedCipherSuites = resolveStringProperty(PaxWebConfig.PID_CFG_CIPHERSUITES_INCLUDED);
			String excludedCipherSuites = resolveStringProperty(PaxWebConfig.PID_CFG_CIPHERSUITES_EXCLUDED);

			if (includedProtocols != null && !"".equals(includedProtocols.trim())) {
				this.includedProtocols = includedProtocols.split("\\s*,\\s*");
			}
			if (excludedProtocols != null && !"".equals(excludedProtocols.trim())) {
				this.excludedProtocols = excludedProtocols.split("\\s*,\\s*");
			}
			if (includedCipherSuites != null && !"".equals(includedCipherSuites.trim())) {
				this.includedCipherSuites = includedCipherSuites.split("\\s*,\\s*");
			}
			if (excludedCipherSuites != null && !"".equals(excludedCipherSuites.trim())) {
				this.excludedCipherSuites = excludedCipherSuites.split("\\s*,\\s*");
			}
		}

		@Override
		public String getSslProvider() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_PROVIDER);
		}

		@Override
		public String getSslKeystore() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEYSTORE);
		}

		@Override
		public String getSslKeystorePassword() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PASSWORD);
		}

		@Override
		public String getSslKeystoreType() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEYSTORE_TYPE);
		}

		@Override
		public String getSslKeystoreProvider() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEYSTORE_PROVIDER);
		}

		@Override
		public String getSslKeyPassword() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEY_PASSWORD);
		}

		@Override
		public String getSslKeyManagerFactoryAlgorithm() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEY_MANAGER_FACTORY_ALGORITHM);
		}

		@Override
		public String getSslKeyAlias() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_KEY_ALIAS);
		}

		@Override
		public String getTruststore() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE);
		}

		@Override
		public String getTruststorePassword() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PASSWORD);
		}

		@Override
		public String getTruststoreType() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_TYPE);
		}

		@Override
		public String getTruststoreProvider() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_TRUSTSTORE_PROVIDER);
		}

		@Override
		public String getTrustManagerFactoryAlgorithm() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_TRUST_MANAGER_FACTORY_ALGORITHM);
		}

		@Override
		public Boolean isClientAuthWanted() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_WANTED);
		}

		@Override
		public Boolean isClientAuthNeeded() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SSL_CLIENT_AUTH_NEEDED);
		}

		@Override
		public String getSslProtocol() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_PROTOCOL);
		}

		@Override
		public String getSecureRandomAlgorithm() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SSL_SECURE_RANDOM_ALGORITHM);
		}

		@Override
		public String[] getProtocolsIncluded() {
			return this.includedProtocols;
		}

		@Override
		public String[] getProtocolsExcluded() {
			return this.excludedProtocols;
		}

		@Override
		public String[] getCiphersuiteIncluded() {
			return this.includedCipherSuites;
		}

		@Override
		public String[] getCiphersuiteExcluded() {
			return this.excludedCipherSuites;
		}

		@Override
		public Boolean isSslRenegotiationAllowed() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_ALLOWED);
		}

		@Override
		public Integer getSslRenegotiationLimit() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SSL_RENEGOTIATION_LIMIT);
		}

		@Override
		public Boolean getSslSessionsEnabled() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SSL_SESSION_ENABLED);
		}

		@Override
		public Integer getSslSessionCacheSize() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SSL_SESSION_CACHE_SIZE);
		}

		@Override
		public Integer getSslSessionTimeout() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SSL_SESSION_TIMEOUT);
		}

		@Override
		public Boolean isValidateCerts() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_VALIDATE_CERTS);
		}

		@Override
		public Boolean isValidatePeerCerts() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_VALIDATE_PEER_CERTS);
		}

		@Override
		public Boolean isEnableOCSP() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_ENABLE_OCSP);
		}

		@Override
		public Boolean isEnableCRLDP() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_ENABLE_CRLDP);
		}

		@Override
		public String getCrlPath() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_CRL_PATH);
		}

		@Override
		public String getOcspResponderURL() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_OCSP_RESPONDER_URL);
		}

		@Override
		public Integer getMaxCertPathLength() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_MAX_CERT_PATH_LENGTH);
		}

		@Override
		public Long getDigestAuthMaxNonceAge() {
			return resolveLongProperty(PaxWebConfig.PID_CFG_DIGESTAUTH_MAX_NONCE_AGE);
		}

		@Override
		public Integer getDigestAuthMaxNonceCount() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_DIGESTAUTH_MAX_NONCE_COUNT);
		}

		@Override
		public Boolean getFormAuthRedirect() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_FORMAUTH_REDIRECT);
		}
	}

	private class ResourceConfigurationImpl implements ResourceConfiguration {

		@Override
		public boolean acceptRanges() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_ACCEPT_RANGES);
		}

		@Override
		public boolean redirectWelcome() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_REDIRECT_WELCOME);
		}

		@Override
		public boolean dirListing() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_DIR_LISTING);
		}

		@Override
		public Integer maxCacheEntries() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRIES);
		}

		@Override
		public Integer maxCacheEntrySize() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRY_SIZE);
		}

		@Override
		public Integer maxTotalCacheSize() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_MAX_TOTAL_SIZE);
		}

		@Override
		public Integer maxCacheTTL() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_DEFAULT_SERVLET_CACHE_TTL);
		}
	}

	private class SessionConfigurationImpl implements SessionConfiguration {

		private final int sessionTimeout;
		private final String sessionCookieName;
		private final String sessionCookiePathName;
		private final DefaultSessionCookieConfig defaultSessionCookieConfig;

		SessionConfigurationImpl() {
			Integer sessionTimeout = resolveIntegerProperty(PaxWebConfig.PID_CFG_SESSION_TIMEOUT);
			if (sessionTimeout == null) {
				sessionTimeout = 30;
			}
			this.sessionTimeout = sessionTimeout;
			String sessionCookieName = resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_NAME);
			if (sessionCookieName == null) {
				sessionCookieName = "JSESSIONID";
			}
			this.sessionCookieName = sessionCookieName;
			String sessionCookiePathName = resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_URL);
			if (sessionCookiePathName == null) {
				sessionCookiePathName = "jsessionid";
			}
			this.sessionCookiePathName = sessionCookiePathName;

			this.defaultSessionCookieConfig = new DefaultSessionCookieConfig();
			defaultSessionCookieConfig.setName(sessionCookieName);
			if (getSessionCookieDomain() != null) {
				defaultSessionCookieConfig.setDomain(getSessionCookieDomain());
			}
			if (getSessionCookiePath() != null) {
				defaultSessionCookieConfig.setPath(getSessionCookiePath());
			}
			if (getSessionCookieComment() != null) {
				defaultSessionCookieConfig.setComment(getSessionCookieComment());
			}
			// http only by default
			defaultSessionCookieConfig.setHttpOnly(getSessionCookieHttpOnly() == null || getSessionCookieHttpOnly());
			// not secure by default
			defaultSessionCookieConfig.setSecure(getSessionCookieSecure() != null && getSessionCookieSecure());
			if (getSessionCookieMaxAge() != null) {
				defaultSessionCookieConfig.setMaxAge(getSessionCookieMaxAge());
			}
		}

		@Override
		public Integer getSessionTimeout() {
			return sessionTimeout;
		}

		@Override
		public String getSessionCookieName() {
			return sessionCookieName;
		}

		@Override
		public String getSessionCookieDomain() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_DOMAIN);
		}

		@Override
		public String getSessionCookiePath() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_PATH);
		}

		@Override
		public String getSessionCookieComment() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_COMMENT);
		}

		@Override
		public Boolean getSessionCookieHttpOnly() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_HTTP_ONLY);
		}

		@Override
		public Boolean getSessionCookieSecure() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_SECURE);
		}

		@Override
		public Integer getSessionCookieMaxAge() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_SESSION_COOKIE_MAX_AGE);
		}

		@Override
		public String getSessionUrlPathParameter() {
			return sessionCookiePathName;
		}

		@Override
		public String getSessionWorkerName() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_WORKER_NAME);
		}

		@Override
		public String getSessionStoreDirectoryLocation() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_SESSION_STORE_DIRECTORY);
		}

		@Override
		public File getSessionStoreDirectory() {
			String location = getSessionStoreDirectoryLocation();
			if (location == null || "".equals(location.trim())) {
				return null;
			}

			File result = null;
			if (location.startsWith("file:")) {
				try {
					URL locationUrl = new URL(location);
					result = new File(locationUrl.toURI());
				} catch (MalformedURLException | URISyntaxException e) {
					LOG.warn("Invalid URL for session persistence: {}", location, e);
				}
			} else {
				result = new File(location);
			}

			if (result != null) {
				if (!result.isDirectory()) {
					LOG.warn("Directory {} is not accessible, skipping configuration of file session persistence",
							location);
					return null;
				}
			}

			return result;
		}

		@Override
		public SessionCookieConfig getDefaultSessionCookieConfig() {
			return defaultSessionCookieConfig;
		}
	}

	private class LogConfigurationImpl implements LogConfiguration {

		private final Boolean ncsaBuffered;

		private LogConfigurationImpl() {
			// eager resolution of some important properties
			resolveBooleanProperty(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED);
			Boolean buffered = resolveBooleanProperty(PaxWebConfig.PID_CFG_LOG_NCSA_BUFFERED);
			ncsaBuffered = buffered == null || buffered;
		}

		@Override
		public Boolean isLogNCSAFormatEnabled() {
			return eagerBooleanProperty(PaxWebConfig.PID_CFG_LOG_NCSA_ENABLED);
		}

		@Override
		public String getLogNCSADirectory() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGDIR);
		}

		@Override
		public String getLogNCSAFile() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE);
		}

		@Override
		public Boolean isLogNCSAAppend() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_LOG_NCSA_APPEND);
		}

		@Override
		public String getLogNCSAFilenameDateFormat() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGFILE_DATE_FORMAT);
		}

		@Override
		public Integer getLogNCSARetainDays() {
			return resolveIntegerProperty(PaxWebConfig.PID_CFG_LOG_NCSA_RETAINDAYS);
		}

		@Override
		public Boolean isLogNCSAExtended() {
			return resolveBooleanProperty(PaxWebConfig.PID_CFG_LOG_NCSA_EXTENDED);
		}

		@Override
		public String getLogNCSATimeZone() {
			return resolveStringProperty(PaxWebConfig.PID_CFG_LOG_NCSA_LOGTIMEZONE);
		}

		@Override
		public Boolean getLogNCSABuffered() {
			return ncsaBuffered;
		}
	}

	private class JspConfigurationImpl implements JspConfiguration {

		private final ServerConfiguration serverConfig;
		private String globalScratchDir = null;

		JspConfigurationImpl(ServerConfiguration serverConfiguration) {
			this.serverConfig = serverConfiguration;

			String location = resolveStringProperty(PaxWebConfig.PID_CFG_JSP_SCRATCH_DIR);
			File result = null;
			if (location != null) {
				if (location.startsWith("file:")) {
					try {
						URL locationUrl = new URL(location);
						result = new File(locationUrl.toURI());
					} catch (MalformedURLException | URISyntaxException e) {
						LOG.warn("Invalid URL for JSP global scratch directory: {}", location, e);
					}
				} else {
					result = new File(location);
				}
			}

			if (result != null) {
				result.mkdirs();
				if (!result.isDirectory()) {
					LOG.warn("Directory {} is not accessible, JSP scratch dir will be relative to tmp dir and context dependent",
							location);
				}
				try {
					globalScratchDir = result.getCanonicalPath();
				} catch (IOException e) {
					LOG.warn("Unexpected problem when checking JSP scratch dir {}", result, e);
				}
			}
		}

		@Override
		public String getJspScratchDir(OsgiContextModel context) {
			if (globalScratchDir != null) {
				return globalScratchDir;
			}
			File dir = new File(serverConfig.getTemporaryDirectory(), "jsp/" + context.getTemporaryLocation());
			try {
				dir.mkdirs();
				return dir.getCanonicalPath();
			} catch (IOException e) {
				LOG.warn("Unexpected problem when checking JSP scratch dir {}", dir, e);
				return dir.getAbsolutePath();
			}
		}

		@Override
		public String getGloablJspScratchDir() {
			return globalScratchDir;
		}
	}

}
