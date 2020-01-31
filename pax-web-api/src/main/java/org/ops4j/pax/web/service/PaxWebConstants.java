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
package org.ops4j.pax.web.service;

import javax.servlet.ServletContext;

/**
 * <p>Different constants used across Pax Web but not related to configuration that may be specified using
 * {@code org.ops4j.pax.web} PID or system/bundle context properties. For configuration related constants, see
 * {@link PaxWebConfig}.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code SERVICE_PROPERTY_} - for names of OSGi service properties</li>
 * </ul></p>
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public interface PaxWebConstants {

	/** Service PID used for configuration. */
	String PID = "org.ops4j.pax.web";

	/** Actual OSGi Http Service will be registered under these {@code objectClass} names. */
	String[] HTTPSERVICE_REGISTRATION_NAMES = {
			org.osgi.service.http.HttpService.class.getName(),
			org.ops4j.pax.web.service.WebContainer.class.getName()
	};

	/**
	 * Init param name for specifying a context name.
	 */
	String CONTEXT_NAME = "webapp.context";

	/**
	 * Servlet init param name for specifying a servlet name.
	 */
	String SERVLET_NAME = "servlet-name";

	/**
	 * Filter init param name for specifying a filter name.
	 */
	String FILTER_NAME = "filter-name";

	/**
	 * Filter init param name for specifying a filter-mapping dispatch behaviour
	 * Must be a comma delimited string of:
	 * <ol>
	 * <li>request</li>
	 * <li>forward</li>
	 * <li>include</li>
	 * <li>error</li>
	 * </ol>
	 * <p>
	 * values are not case sensitive.
	 */
	String FILTER_MAPPING_DISPATCHER = "filter-mapping-dispatcher";

	String PROPERTY_HTTP_USE_NIO = "org.osgi.service.http.useNIO";
	String PROPERTY_HTTP_CHECK_FORWARDED_HEADERS = "org.osgi.service.http.checkForwardedHeaders";
	String PROPERTY_HTTP_CONNECTOR_NAME = "org.osgi.service.http.connector.name";
	String PROPERTY_HTTP_ENABLED = "org.osgi.service.http.enabled";
	String PROPERTY_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";
	String PROPERTY_HTTP_SECURE_CONNECTOR_NAME = "org.osgi.service.http.secure.connector.name";

	String PROPERTY_SSL_PROVIDER = PID + ".ssl.provider";

	String PROPERTY_SSL_KEYSTORE = PID + ".ssl.keystore";
	String PROPERTY_SSL_KEYSTORE_TYPE = PID + ".ssl.keystore.type";
	String PROPERTY_SSL_KEYSTORE_PASSWORD = PID + ".ssl.keystore.password";
	String PROPERTY_SSL_KEYSTORE_PROVIDER = PID + ".ssl.keystore.provider";
	/**
	 * @deprecated use PROPERTY_SSL_KEYSTORE_PASSWORD instead.
	 */
	@Deprecated
	String PROPERTY_SSL_PASSWORD = PID + ".ssl.password";
	/**
	 * @deprecated use PROPERTY_SSL_KEY_PASSWORD instead.
	 */
	@Deprecated
	String PROPERTY_SSL_KEYPASSWORD = PID + ".ssl.keypassword";
	String PROPERTY_SSL_KEY_ALIAS = PID + ".ssl.key.alias";
	String PROPERTY_SSL_KEY_PASSWORD = PID + ".ssl.key.password";

	String PROPERTY_SSL_TRUST_STORE = PID + ".ssl.truststore";
	String PROPERTY_SSL_TRUST_STORE_PASSWORD = PID + ".ssl.truststore.password";
	String PROPERTY_SSL_TRUST_STORE_TYPE = PID + ".ssl.truststore.type";
	String PROPERTY_SSL_TRUST_STORE_PROVIDER = PID + ".ssl.truststore.provider";

	String PROPERTY_SSL_CLIENT_AUTH_WANTED = PID + ".ssl.clientauthwanted";
	String PROPERTY_SSL_CLIENT_AUTH_NEEDED = PID + ".ssl.clientauthneeded";

	/**
	 * @deprecated use PROPERTY_CIPHERSUITES_INCLUDED instead.
	 */
	@Deprecated
	String PROPERTY_CIPHERSUITE_INCLUDED = PID + "ssl.cyphersuites.included";
	/**
	 * @deprecated use PROPERTY_CIPHERSUITES_EXCLUDED instead.
	 */
	@Deprecated
	String PROPERTY_CIPHERSUITE_EXCLUDED = PID + "ssl.cyphersuites.excluded";

	String PROPERTY_PROTOCOLS_INCLUDED = PID + ".ssl.protocols.included";
	String PROPERTY_PROTOCOLS_EXCLUDED = PID + ".ssl.protocols.excluded";
	String PROPERTY_CIPHERSUITES_INCLUDED = PID + ".ssl.ciphersuites.included";
	String PROPERTY_CIPHERSUITES_EXCLUDED = PID + ".ssl.ciphersuites.excluded";
	String PROPERTY_SSL_RENEGOTIATION_ALLOWED = PID + ".ssl.renegotiationAllowed";

	String PROPERTY_SESSION_TIMEOUT = PID + ".session.timeout";
	String PROPERTY_SESSION_COOKIE = PID + ".session.cookie";
	String PROPERTY_SESSION_DOMAIN = PID + ".session.domain";
	String PROPERTY_SESSION_PATH = PID + ".session.path";
	String PROPERTY_SESSION_URL = PID + ".session.url";
	String PROPERTY_WORKER_NAME = PID + ".worker.name";
	String PROPERTY_SESSION_COOKIE_HTTP_ONLY = PID + ".session.cookie.httpOnly";
	String PROPERTY_SESSION_COOKIE_SECURE = PID + ".session.cookie.secure";
	String PROPERTY_SESSION_COOKIE_MAX_AGE = PID + ".session.cookie.maxAge";
	String PROPERTY_SESSION_LAZY_LOAD = PID + ".session.lazyload";
	String PROPERTY_SESSION_STORE_DIRECTORY = PID + ".session.storedirectory";

	String PROPERTY_LISTENING_ADDRESSES = PID + ".listening.addresses";

	String PROPERTY_LOG_NCSA_ENABLED = "org.ops4j.pax.web.log.ncsa.enabled";
	String PROPERTY_LOG_NCSA_FORMAT = "org.ops4j.pax.web.log.ncsa.format";
	String PROPERTY_LOG_NCSA_RETAINDAYS = "org.ops4j.pax.web.log.ncsa.retaindays";
	String PROPERTY_LOG_NCSA_APPEND = "org.ops4j.pax.web.log.ncsa.append";
	String PROPERTY_LOG_NCSA_EXTENDED = "org.ops4j.pax.web.log.ncsa.extended";
	String PROPERTY_LOG_NCSA_DISPATCH = "org.ops4j.pax.web.log.ncsa.dispatch";
	String PROPERTY_LOG_NCSA_LOGTIMEZONE = "org.ops4j.pax.web.log.ncsa.logtimezone";
	String PROPERTY_LOG_NCSA_LOGDIR = "org.ops4j.pax.web.log.ncsa.directory";
	String PROPERTY_LOG_NCSA_LATENCY = "org.ops4j.pax.web.log.ncsa.latency";
	String PROPERTY_LOG_NCSA_COOKIES = "org.ops4j.pax.web.log.ncsa.cookies";
	String PROPERTY_LOG_NCSA_SERVER = "org.ops4j.pax.web.log.ncsa.server";

	String PROPERTY_VIRTUAL_HOST_LIST = "org.ops4j.pax.web.default.virtualhosts";
	String PROPERTY_CONNECTOR_LIST = "org.ops4j.pax.web.default.connectors";
    String PROPERTY_DEFAULT_AUTHMETHOD = "org.ops4j.pax.web.default.authmethod";
    String PROPERTY_DEFAULT_REALMNAME = "org.ops4j.pax.web.default.realmname";

	String PROPERTY_MAX_THREADS = "org.ops4j.pax.web.server.maxThreads";

	String PROPERTY_MIN_THREADS = "org.ops4j.pax.web.server.minThreads";

	String PROPERTY_IDLE_TIMEOUT = "org.ops4j.pax.web.server.idleTimeout";
	String PROPERTY_CONNECTOR_IDLE_TIMEOUT = "org.ops4j.pax.web.server.connector.idleTimeout";
	String PROPERTY_SHOW_STACKS = "org.ops4j.pax.web.server.showStacks";

	/**
	 * Servlet context attribute containing the bundle context of the bundle
	 * registering the http context.
	 */
	String BUNDLE_CONTEXT_ATTRIBUTE = "osgi-bundlecontext";

	String PROPERTY_SERVER_CONFIGURATION_FILE = PID + ".config.file";

	String PROPERTY_SERVER_CONFIGURATION_URL = PID + ".config.url";

	/**
	 * Manifest header key for web application bundles.
	 */
	String CONTEXT_PATH_KEY = "Web-ContextPath";

	String FILTER_RANKING = "filterRank";
	
	String PROPERTY_CRL_PATH = PID + ".crlPath";
	
	String PROPERTY_ENABLE_CRLDP = PID + ".enableCRLDP";
	
	String PROPERTY_VALIDATE_CERTS = PID + ".validateCerts";
	
	String PROPERTY_VALIDATE_PEER_CERTS = PID + ".validatePeerCerts";
	
	String PROPERTY_ENABLE_OCSP = PID + ".enableOCSP";
	
	String PROPERTY_OCSP_RESPONDER_URL = PID + ".ocspResponderURL";
	
	String PROPERTY_ENC_MASTERPASSWORD = PID + ".enc.masterpassword";
	
	String PROPERTY_ENC_ALGORITHM = PID + ".enc.algorithm";
	
	String PROPERTY_ENC_ENABLED = PID + ".enc.enabled";
	
	String PROPERTY_ENC_PREFIX = PID + ".enc.prefix";
	
	String PROPERTY_ENC_SUFFIX = PID + ".enc.suffix";

	String PROPERTY_SERVLETCONTEXT_PATH = "osgi.web.contextpath";
	String PROPERTY_SERVLETCONTEXT_NAME = "osgi.web.contextname";
	String PROPERTY_SYMBOLIC_NAME = "osgi.web.symbolicname";


	/**
	 * Scratch directory for JSPs
	 */
	String PROPERTY_JSP_SCRATCH_DIR = PID + ".jsp.scratch.dir";

	String PROPERTY_JSP_CHECK_INTERVAL = PID + ".jsp.check.interval";

	String PROPERTY_JSP_DEBUG_INFO = PID + ".jsp.debug.info";

	String PROPERTY_JSP_DEVELOPMENT = PID + ".jsp.development";

	String PROPERTY_JSP_ENABLE_POOLING = PID + ".jsp.enable.pooling";

	String PROPERTY_JSP_IE_CLASS_ID = PID + ".jsp.ie.classid";

	String PROPERTY_JSP_JAVA_ENCODING = PID + ".jsp.java.encoding";

	String PROPERTY_JSP_KEEP_GENERATED = PID + ".jsp.keep.generated";

	String PROPERTY_JSP_LOG_VERBOSITY_LEVEL = PID + ".jsp.log.verbosity.level";

	String PROPERTY_JSP_MAPPED_FILE = PID + ".jsp.mapped.file";

	String PROPERTY_JSP_TAGPOOL_MAX_SIZE = PID + ".jsp.tagpool.max.size";

	String PROPERTY_JSP_PRECOMPILATION = PID + ".jsp.precompilation";

}
