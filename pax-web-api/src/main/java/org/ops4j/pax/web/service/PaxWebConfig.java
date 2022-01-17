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
package org.ops4j.pax.web.service;

import java.security.cert.CertStoreParameters;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletContext;

/**
 * <p>Dedicated interface with constants related to configuration. Other constants reside
 * in {@link PaxWebConstants}.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code PID_CFG_} - for property names found in {@code org.ops4j.pax.web} PID</li>
 * </ul></p>
 *
 * <p>When adding new properties, remember to add them in more places:<ul>
 *     <li>Constant name in this interface</li>
 *     <li>Relevant method (if needed) in {@code org.ops4j.pax.web.service.spi.config.Configuration}</li>
 *     <li>Metatype information for default values (if needed) in
 *     {@code pax-web-runtime/src/main/resources/OSGI-INF/metatype/metatype.xml}</li>
 * </ul></p>
 */
public interface PaxWebConfig {

	// --- server configuration properties

	/**
	 * Servlet API 4, 4.8.1 "Temporary Working Directories". According to specification, it should be different for
	 * each {@link ServletContext}, but we also need single global temporary directory.
	 */
	String PID_CFG_TEMP_DIR = ServletContext.TEMPDIR;

	/**
	 * Option to specify single external configuration file.
	 */
	String PID_CFG_SERVER_CONFIGURATION_FILE = "org.ops4j.pax.web.config.file";

	/**
	 * Specify comma-separated list of external, server-specific config locations.
	 * @since Pax Web 8
	 */
	String PID_CFG_SERVER_CONFIGURATION_FILES = "org.ops4j.pax.web.config.files";

	// 102.9 Configuration Properties - the below two properties are not specified in any interface/class

	/**
	 * This property specifies the port used for servlets and resources accessible via HTTP.
	 * The default value for this property is {@code 80} according to specification, but we'll use {@code 8080}.
	 */
	String PID_CFG_HTTP_PORT = "org.osgi.service.http.port";

	/**
	 * This property specifies the port used for servlets and resources accessible via HTTPS.
	 * The default value for this property is {@code 443} according to specificaton, but we'll use {@code 8443}.
	 */
	String PID_CFG_HTTP_PORT_SECURE = "org.osgi.service.http.port.secure";

	/** Should the default non-secure port be enabled? */
	String PID_CFG_HTTP_ENABLED = "org.osgi.service.http.enabled";

	/** Should the default secure port be enabled? */
	String PID_CFG_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";

	/** Comma-separated list of addresses to bind listeners/connectors to. Defaults to {@code 0.0.0.0} */
	String PID_CFG_LISTENING_ADDRESSES = "org.ops4j.pax.web.listening.addresses";

	/** Name to use as <em>default</em> (non-secure) connector, defaults to {@code default}. */
	String PID_CFG_HTTP_CONNECTOR_NAME = "org.osgi.service.http.connector.name";

	/** Name to use as <em>secure</em> connector, defaults to {@code secureDefault}. */
	String PID_CFG_HTTP_SECURE_CONNECTOR_NAME = "org.osgi.service.http.secure.connector.name";

	/**
	 * Jetty: {@code org.eclipse.jetty.server.AbstractConnector#setIdleTimeout(long)}
	 */
	String PID_CFG_CONNECTOR_IDLE_TIMEOUT = "org.ops4j.pax.web.server.connector.idleTimeout";

	/**
	 * Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setIdleTimeout(int)}
	 */
	String PID_CFG_SERVER_IDLE_TIMEOUT = "org.ops4j.pax.web.server.idleTimeout";

	/**
	 * Gets maximum number of threads to use in server runtime.
	 * Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setMaxThreads(int)}
	 */
	String PID_CFG_SERVER_MAX_THREADS = "org.ops4j.pax.web.server.maxThreads";

	/**
	 * Gets minimum number of threads to use in server runtime.
	 * Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setMinThreads(int)}
	 */
	String PID_CFG_SERVER_MIN_THREADS = "org.ops4j.pax.web.server.minThreads";

	/**
	 * Prefix to use for server thread names.
	 * Jetty: {@code org.eclipse.jetty.util.thread.QueuedThreadPool#setName(java.lang.String)}
	 */
	String PID_CFG_SERVER_THREAD_NAME_PREFIX = "org.ops4j.pax.web.server.threadNamePrefix";

	/**
	 * Jetty: adds {@code org.eclipse.jetty.server.ForwardedRequestCustomizer} to {@code HttpConfiguration}
	 */
	String PID_CFG_HTTP_CHECK_FORWARDED_HEADERS = "org.osgi.service.http.checkForwardedHeaders";

	/**
	 * Comma-separated list of virtual hosts to set on <em>every deployed context</em> if the context itself
	 * doesn't specify such configuration. Defaults to empty list.
	 */
	String PID_CFG_VIRTUAL_HOST_LIST = "org.ops4j.pax.web.default.virtualhosts";
	/**
	 * Comma-separated list of connector names to set on <em>every deployed context</em> if the context itself
	 * doesn't specify such configuration. Defaults to empty list.
	 */
	String PID_CFG_CONNECTOR_LIST = "org.ops4j.pax.web.default.connectors";

	/**
	 * Option to specify number of threads for internal web element/context (un)registration event dispatching.
	 * Defaults to {@code 1} ("3" was hardcoded before Pax Web 8).
	 */
	String PID_CFG_EVENT_DISPATCHER_THREAD_COUNT = "org.ops4j.pax.web.server.eventDispatcherThreadCount";

	/**
	 * {@link org.osgi.framework.BundleContext} property to configure thread count for WAR
	 * extender. Before Pax Web 8 it was hardcoded to 3 (which is still the default value).
	 */
	String BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_THREADS = "org.ops4j.pax.web.extender.war.threads";

	/**
	 * Context property listing symbolic names of the bundles or jar names (in {@code /WEB-INF/lib}) patterns to skip
	 * when searching for TLDs, web fragments and annotated classes. This property can have special value of
	 * {@code "default"} (no quotes) which roughly matches Tomcat's {@code tomcat.util.scan.StandardJarScanFilter.jarsToSkip}
	 * property
	 */
	String BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_JARS_TO_SKIP = "org.ops4j.pax.web.extender.war.jarsToSkip";

	/**
	 * Context property listing symbolic names of the bundles or jar names (in {@code /WEB-INF/lib}) patterns to skan
	 * when searching for TLDs, web fragments and annotated classes. Normally all the reachable jars are scanned, but
	 * if something is matched by {@link #BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_JARS_TO_SKIP}, we can skan it by adding the pattern
	 * to this property (by default this property has empty value, which means that all non-skipped libraries are
	 * scanned).
	 */
	String BUNDLE_CONTEXT_PROPERTY_WAR_EXTENDER_JARS_TO_SCAN = "org.ops4j.pax.web.extender.war.jarsToScan";

	// --- security configuration properties

	/**
	 * <p>Property to specify SSL provider to use for <em>secure</em> connector.</p>
	 *
	 * <p>Jetty: {@code org.eclipse.jetty.util.ssl.SslContextFactory#setProvider()}. Used in:<ul>
	 *     <li>{@link java.security.cert.CertificateFactory#getInstance(String, String)} - default {@code SUN}</li>
	 *     <li>{@link java.security.cert.CertStore#getInstance(String, CertStoreParameters, String)} - default {@code SUN}</li>
	 *     <li>{@link javax.net.ssl.KeyManagerFactory#getInstance(String, String)} - default {@code SunJSSE}</li>
	 *     <li>{@link java.security.SecureRandom#getInstance(String, String)} - default {@code SUN}</li>
	 *     <li>{@link javax.net.ssl.SSLContext#getInstance(String, String)} - default {@code SunJSSE}</li>
	 *     <li>{@link javax.net.ssl.TrustManagerFactory#getInstance(String, String)} - default {@code SunJSSE}</li>
	 * </ul></p>
	 */
	String PID_CFG_SSL_PROVIDER = "org.ops4j.pax.web.ssl.provider";

	/** File or URL to location of Keystore with server certificate and private key. */
	String PID_CFG_SSL_KEYSTORE = "org.ops4j.pax.web.ssl.keystore";
	/** Password for entire server keystore */
	String PID_CFG_SSL_KEYSTORE_PASSWORD = "org.ops4j.pax.web.ssl.keystore.password";
	/** Type of server keystore to use as specified by {@link java.security.KeyStore#getInstance(String, String)} */
	String PID_CFG_SSL_KEYSTORE_TYPE = "org.ops4j.pax.web.ssl.keystore.type";
	/** Provider of server keystore as specified by {@link java.security.KeyStore#getInstance(String, String)} */
	String PID_CFG_SSL_KEYSTORE_PROVIDER = "org.ops4j.pax.web.ssl.keystore.provider";

	/** Password for private key entry inside server keystore */
	String PID_CFG_SSL_KEY_PASSWORD = "org.ops4j.pax.web.ssl.key.password";
	/** Algorithm to use for {@link javax.net.ssl.KeyManagerFactory#getInstance(String)} */
	String PID_CFG_SSL_KEY_MANAGER_FACTORY_ALGORITHM = "org.ops4j.pax.web.ssl.keyManagerFactory.algorithm";
	/** Alias of private key entry in server keystore to use of no SNI is enabled */
	String PID_CFG_SSL_KEY_ALIAS = "org.ops4j.pax.web.ssl.key.alias";

	/** File or URL to location of server truststore. */
	String PID_CFG_SSL_TRUSTSTORE = "org.ops4j.pax.web.ssl.truststore";
	/** Password for entire server truststore */
	String PID_CFG_SSL_TRUSTSTORE_PASSWORD = "org.ops4j.pax.web.ssl.truststore.password";
	/** Type of server truststore to use as specified by {@link java.security.KeyStore#getInstance(String, String)} */
	String PID_CFG_SSL_TRUSTSTORE_TYPE = "org.ops4j.pax.web.ssl.truststore.type";
	/** Provider of server truststore as specified by {@link java.security.KeyStore#getInstance(String, String)} */
	String PID_CFG_SSL_TRUSTSTORE_PROVIDER = "org.ops4j.pax.web.ssl.truststore.provider";
	/** Algorithm to use for {@link javax.net.ssl.TrustManagerFactory#getInstance(String)} */
	String PID_CFG_SSL_TRUST_MANAGER_FACTORY_ALGORITHM = "org.ops4j.pax.web.ssl.trustManagerFactory.algorithm";

	/** Flag for {@link javax.net.ssl.SSLEngine#setWantClientAuth(boolean)} */
	String PID_CFG_SSL_CLIENT_AUTH_WANTED = "org.ops4j.pax.web.ssl.clientauth.wanted";
	/** Flag for {@link javax.net.ssl.SSLEngine#setNeedClientAuth(boolean)} */
	String PID_CFG_SSL_CLIENT_AUTH_NEEDED = "org.ops4j.pax.web.ssl.clientauth.needed";

	/** Protocol to use with {@link javax.net.ssl.SSLContext#getInstance(String)}. Defaults to {@code TLSv1.2} */
	String PID_CFG_SSL_PROTOCOL = "org.ops4j.pax.web.ssl.protocol";

	/** Algorithm to use with {@link java.security.SecureRandom#getInstance(String)}. */
	String PID_CFG_SSL_SECURE_RANDOM_ALGORITHM = "org.ops4j.pax.web.ssl.secureRandom.algorithm";

	/**
	 * Comma separated list of included protocol names, as in
	 * {@link javax.net.ssl.SSLEngine#setEnabledProtocols(String[])}. Protocol names are taken from
	 * {@code sun.security.ssl.ProtocolVersion}
	 */
	String PID_CFG_PROTOCOLS_INCLUDED = "org.ops4j.pax.web.ssl.protocols.included";
	/**
	 * Comma separated list of excluded protocol names. All supported without excluded will be used in
	 * {@link javax.net.ssl.SSLEngine#setEnabledProtocols(String[])}
	 */
	String PID_CFG_PROTOCOLS_EXCLUDED = "org.ops4j.pax.web.ssl.protocols.excluded";
	/**
	 * Comma separated list of included cipher suite names, as in
	 * {@link javax.net.ssl.SSLEngine#setEnabledCipherSuites(String[])}. Cipher suite names are taken from
	 * {@code sun.security.ssl.CipherSuite}
	 */
	String PID_CFG_CIPHERSUITES_INCLUDED = "org.ops4j.pax.web.ssl.ciphersuites.included";
	/**
	 * Comma separated list of excluded cipher suite names. All supported without excluded will be used in
	 * {@link javax.net.ssl.SSLEngine#setEnabledCipherSuites(String[])}
	 */
	String PID_CFG_CIPHERSUITES_EXCLUDED = "org.ops4j.pax.web.ssl.ciphersuites.excluded";

	/** Allow SSL renegotiation */
	String PID_CFG_SSL_RENEGOTIATION_ALLOWED = "org.ops4j.pax.web.ssl.renegotiationAllowed";
	/** SSL renegotiation limit */
	String PID_CFG_SSL_RENEGOTIATION_LIMIT = "org.ops4j.pax.web.ssl.renegotiationLimit";

	/**
	 * Are SSL Sessions enabled? If {@code true} (which is default), such hint is passed to
	 * {@link SSLContext#createSSLEngine(String, int)}.
	 */
	String PID_CFG_SSL_SESSION_ENABLED = "org.ops4j.pax.web.ssl.session.enabled";
	/** SSL Session cache size. Defaults to {@code -1} */
	String PID_CFG_SSL_SESSION_CACHE_SIZE = "org.ops4j.pax.web.ssl.session.cacheSize";
	/** SSL Session timeout. Defaults to {@code -1} */
	String PID_CFG_SSL_SESSION_TIMEOUT = "org.ops4j.pax.web.ssl.session.timeout";

	/** Whether certificates in server keystore should be validated on load */
	String PID_CFG_VALIDATE_CERTS = "org.ops4j.pax.web.validateCerts";
	/** Whether certificates in server truststore should be validated on load */
	String PID_CFG_VALIDATE_PEER_CERTS = "org.ops4j.pax.web.validatePeerCerts";

	/** Should On-Line Certificate Status Protocol (OCSP) be enabled? */
	String PID_CFG_ENABLE_OCSP = "org.ops4j.pax.web.enableOCSP";
	/** Should Certificate Revocation List Distribution Points support (CRLDP) be enabled? */
	String PID_CFG_ENABLE_CRLDP = "org.ops4j.pax.web.enableCRLDP";
	/** Location of CRL file to use with {@link java.security.cert.CertificateFactory#generateCRLs} for X.509 factory */
	String PID_CFG_CRL_PATH = "org.ops4j.pax.web.crlPath";
	/** OCSP responder URL, though it doesn't seem to be used by Jetty. */
	String PID_CFG_OCSP_RESPONDER_URL = "org.ops4j.pax.web.ocspResponderURL";
	/** Return max length of cert path to use during certificate validation */
	String PID_CFG_MAX_CERT_PATH_LENGTH = "org.ops4j.pax.web.maxCertPathLength";

	/** Return max nonce age for DIGEST authentication (in ms), defaults to 60s (60000ms) */
	String PID_CFG_DIGESTAUTH_MAX_NONCE_AGE = "org.ops4j.pax.web.digestAuth.maxNonceAge";
	/** Return max nonce count for DIGEST authentication, defaults to 1024 */
	String PID_CFG_DIGESTAUTH_MAX_NONCE_COUNT = "org.ops4j.pax.web.digestAuth.maxNonceCount";
	/** Returns whether to forward (false, default) to form-login error page or use redirect (true) */
	String PID_CFG_FORMAUTH_REDIRECT = "org.ops4j.pax.web.formAuth.errorRedirect";

	// --- logging configuration properties

	/** Should we enable "NCSA Logger"? */
	String PID_CFG_LOG_NCSA_ENABLED = "org.ops4j.pax.web.log.ncsa.enabled";

	/** Directory to store request log files */
	String PID_CFG_LOG_NCSA_LOGDIR = "org.ops4j.pax.web.log.ncsa.directory";

	/**
	 * Jetty: defaults to "yyyy_mm_dd.request.log", should contain {@code yyyy_mm_dd}.
	 * Tomcat: it should NOT contain {@code yyyy_mm_dd}, so please specify your own name.
	 * Undertow: will be appended with "log" or "yyyy-MM-dd.log"
	 */
	String PID_CFG_LOG_NCSA_LOGFILE = "org.ops4j.pax.web.log.ncsa.file";

	/**
	 * Date format to use when current file is renamed during rollover
	 * Jetty: org.eclipse.jetty.util.RolloverFileOutputStream._fileDateFormat = ROLLOVER_FILE_DATE_FORMAT
	 */
	String PID_CFG_LOG_NCSA_LOGFILE_DATE_FORMAT = "org.ops4j.pax.web.log.ncsa.file.date.format";

	/**
	 * Whether to append to log file
	 */
	String PID_CFG_LOG_NCSA_APPEND = "org.ops4j.pax.web.log.ncsa.append";

	/**
	 * Number of days to retain request files
	 * Jetty: org.eclipse.jetty.server.RequestLogWriter._retainDays
	 */
	String PID_CFG_LOG_NCSA_RETAINDAYS = "org.ops4j.pax.web.log.ncsa.retaindays";

	/**
	 * Jetty whether to use CustomRequestLog#EXTENDED_NCSA_FORMAT or CustomRequestLog#NCSA_FORMAT
	 */
	String PID_CFG_LOG_NCSA_EXTENDED = "org.ops4j.pax.web.log.ncsa.extended";

	/**
	 * Timezone to use in logs
	 * Jetty: org.eclipse.jetty.server.RequestLogWriter#_timeZone
	 */
	String PID_CFG_LOG_NCSA_LOGTIMEZONE = "org.ops4j.pax.web.log.ncsa.logtimezone";

	/** Whether NCSA log file access should be buffered. Defaults to {@code true}. */
	String PID_CFG_LOG_NCSA_BUFFERED = "org.ops4j.pax.web.log.ncsa.buffered";

	// --- default/resource servlet configuration - common properties for "default" servlets of all the containers

	/**
	 * <p>Boolean property to specify whether default servlet should reply with {@code Accept-Ranges: bytes} header.</p>
	 * <p><ul>
	 *     <li>Jetty: {@code acceptRanges} boolean init parameter</li>
	 *     <li>Tomcat: {@code useAcceptRanges} boolean init parameter</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_ACCEPT_RANGES = "org.ops4j.pax.web.resource.acceptRanges";

	/**
	 * <p>Boolean property to specify whether <em>welcome file</em> should be served immediately, or by redirect.</p>
	 * <p><ul>
	 *     <li>Jetty: {@code redirectWelcome} boolean init parameter</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_REDIRECT_WELCOME = "org.ops4j.pax.web.resource.redirectWelcome";

	/**
	 * <p>Boolean property to specify whether <em>dir index</em> should be present when accessing <em>dir
	 * resource</em>.</p>
	 * <p><ul>
	 *     <li>Jetty: {@code dirAllowed} boolean init parameter</li>
	 *     <li>Tomcat: {@code listings} boolean init parameter</li>
	 *     <li>Undertow: {@code directory-listing} boolean init parameter</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_DIR_LISTING = "org.ops4j.pax.web.resource.dirListing";

	/**
	 * <p>Integer property to specify maximum number of cache entries (per single <em>resource manager</em>).</p>
	 * <p><ul>
	 *     <li>Jetty: {@code maxCachedFiles} integer init parameter</li>
	 *     <li>Tomcat: no such option (but there's default 5s TTL)</li>
	 *     <li>Undertow: separate {@code io.undertow.server.handlers.cache.LRUCache#maxEntries} for <em>metadata</em>
	 *     and "regions" + "slices" parameters in {@code LimitedBufferSlicePool}</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRIES = "org.ops4j.pax.web.resource.cache.maxEntries";

	/**
	 * <p>Integer property to specify maximum size (kB) of single cache entry (file) (per single <em>resource manager</em>).</p>
	 * <p><ul>
	 *     <li>Jetty: {@code maxCachedFileSize} integer init parameter</li>
	 *     <li>Tomcat: {@code org.apache.catalina.webresources.StandardRoot#setCacheObjectMaxSize()} (kB)</li>
	 *     <li>Undertow: {@code io.undertow.server.handlers.resource.CachingResourceManager#maxFileSize()} (B)</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_CACHE_MAX_ENTRY_SIZE = "org.ops4j.pax.web.resource.cache.maxEntrySize";

	/**
	 * <p>Integer property to specify maximum total size (kB) of the cache (per single <em>resource manager</em>).</p>
	 * <p><ul>
	 *     <li>Jetty: {@code maxCacheSize} boolean init parameter</li>
	 *     <li>Tomcat: {@code org.apache.catalina.webresources.StandardRoot#setCacheMaxSize()} (kB)</li>
	 *     <li>Undertow: {@code new LimitedBufferSlicePool(bufferAllocator, sliceSize, sliceSize * slicesPerPage, maxMemory / (sliceSize * slicesPerPage))}</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_CACHE_MAX_TOTAL_SIZE = "org.ops4j.pax.web.resource.cache.maxTotalSize";

	/**
	 * <p>Integer property to specify TTL for cache entries (ms)</p>
	 * <p><ul>
	 *     <li>Jetty: no such option, entries are evicted by last access time</li>
	 *     <li>Tomcat: {@code org.apache.catalina.webresources.StandardRoot#setCacheTtl(long)} (ms)</li>
	 *     <li>Undertow: {@code io.undertow.server.handlers.resource.CachingResourceManager#maxAge} and
	 *         {@code io.undertow.server.handlers.cache.LRUCache#maxAge}</li>
	 * </ul></p>
	 */
	String PID_CFG_DEFAULT_SERVLET_CACHE_TTL = "org.ops4j.pax.web.resource.cache.ttl";

	/** Boolean property to configure the container to show or hide stack traces in <em>error handler</em>. */
	String PID_CFG_SHOW_STACKS = "org.ops4j.pax.web.server.showStacks";

	// --- session configuration properties - for all the contexts

	/** Integer property that specifies timeout of sessions in minutes (defaults to 30) */
	String PID_CFG_SESSION_TIMEOUT = "org.ops4j.pax.web.session.timeout";
	/** String property that specifies session cookie name (defaults to {@code JSESSIONID}) */
	String PID_CFG_SESSION_COOKIE_NAME = "org.ops4j.pax.web.session.cookie.name";
	/** String property that specifies session cookie domain */
	String PID_CFG_SESSION_COOKIE_DOMAIN = "org.ops4j.pax.web.session.cookie.domain";
	/** String property that specifies session cookie path (defaults to context path) */
	String PID_CFG_SESSION_COOKIE_PATH = "org.ops4j.pax.web.session.cookie.path";
	/** String property that specifies session cookie comment */
	String PID_CFG_SESSION_COOKIE_COMMENT = "org.ops4j.pax.web.session.cookie.comment";
	/** Boolean property that specifies <em>http only</em> flag for session cookie. Defaults to {@code true} */
	String PID_CFG_SESSION_COOKIE_HTTP_ONLY = "org.ops4j.pax.web.session.cookie.httpOnly";
	/** Boolean property that specifies <em>secure</em> flag for session cookie. Defaults to {@code false} */
	String PID_CFG_SESSION_COOKIE_SECURE = "org.ops4j.pax.web.session.cookie.secure";
	/** Integer property that specifies max age of session cookie */
	String PID_CFG_SESSION_COOKIE_MAX_AGE = "org.ops4j.pax.web.session.cookie.maxAge";

	/** For Jetty, we can specify the URL path parameter for session URL rewriting. Defauts to {@code jsessionid}. */
	String PID_CFG_SESSION_URL = "org.ops4j.pax.web.session.url";
	/** Allows configuration of Jetty's SessionHandler.SessionIdManager.workerName */
	String PID_CFG_SESSION_WORKER_NAME = "org.ops4j.pax.web.session.worker.name";

	/** When specified and is a valid directory location, <em>file session persistence</em> will be enabled */
	String PID_CFG_SESSION_STORE_DIRECTORY = "org.ops4j.pax.web.session.storedirectory";

	// --- JSP configuration

	/** Global Scratch directory for JSPs - by default it is relative to global tmp dir and depends on the context */
	String PID_CFG_JSP_SCRATCH_DIR = "org.ops4j.pax.web.jsp.scratch.dir";

	// Properties related to Jasypt encryption - both direct usage of Jasypt and via OSGi services

	String PID_CFG_ENC_PROPERTY_PREFIX = "org.ops4j.pax.web.enc.";

	/**
	 * Boolean property that tells pax-web-runtime that the configuration may contain encrypted values.
	 * Defaults to {@code false}.
	 */
	String PID_CFG_ENC_ENABLED = "org.ops4j.pax.web.enc.enabled";

	/** String property for the prefix for encrypted values. Defaults to {@code ENC(} */
	String PID_CFG_ENC_PREFIX = "org.ops4j.pax.web.enc.prefix";
	/** String property for the suffix for encrypted values. Defaults to {@code )} */
	String PID_CFG_ENC_SUFFIX = "org.ops4j.pax.web.enc.suffix";

	// we can either specify everything ourselves ...

	/** Provider name to use for PBE encryption - defaults to {@code SunJCE} on Oracle/OpenJDK */
	String PID_CFG_ENC_PROVIDER = "org.ops4j.pax.web.enc.provider";
	/**
	 * Algorithm name to use for PBE encryption - see {@link javax.crypto.SecretKeyFactory#getInstance(java.lang.String)}
	 * defaults to {@code PBEWithHmacSHA256AndAES_128}
	 */
	String PID_CFG_ENC_ALGORITHM = "org.ops4j.pax.web.enc.algorithm";
	/** Plain text encryption password */
	String PID_CFG_ENC_MASTERPASSWORD = "org.ops4j.pax.web.enc.masterpassword";
	/** Environment variable to use for encryption password */
	String PID_CFG_ENC_MASTERPASSWORD_ENV = "org.ops4j.pax.web.enc.masterpassword.env.variable";
	/** System property to use for encryption password */
	String PID_CFG_ENC_MASTERPASSWORD_SYS = "org.ops4j.pax.web.enc.masterpassword.sys.property";
	/** IC parameter for PBE encryption - defaults to 1000 */
	String PID_CFG_ENC_ITERATION_COUNT = "org.ops4j.pax.web.enc.iterationcount";

	// ... or simply reference a StringEncryptor someone else has configured

	/**
	 * Similarly to Pax JDBC/JMS, we can reference an OSGi service with {@code objectClass=org.jasypt.encryption.StringEncryptor}
	 * and {@code decryptor} properties. A filter is created to track the OSGi service used to decrypt configuration values.
	 */
	String PID_CFG_ENC_OSGI_DECRYPTOR = "org.ops4j.pax.web.enc.osgi.decryptor";

}
