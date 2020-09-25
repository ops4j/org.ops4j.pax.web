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
package org.ops4j.pax.web.service.tomcat.internal;

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.catalina.Executor;
import org.apache.catalina.Server;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.compat.JreCompat;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Helper class that's used to create various parts of embedded Tomcat similar to Jetty equivalent.</p>
 *
 * <p>There should be single instance of this factory and all {@code createXXX()} methods should operate
 * on passed {@link Configuration}, because those methods work on behalf of possibly many different
 * {@link org.ops4j.pax.web.service.spi.ServerController controllers}.</p>
 */
public class TomcatFactory {

	private static final Logger LOG = LoggerFactory.getLogger(TomcatFactory.class);

	private final Bundle paxWebTomcatBundle;
	private final ClassLoader classLoader;

	private boolean alpnAvailable;
	private boolean http2Available;

	TomcatFactory(Bundle paxWebTomcatBundle, ClassLoader classLoader) {
		this.paxWebTomcatBundle = paxWebTomcatBundle;
		this.classLoader = classLoader;

		discovery();
	}

	/**
	 * Performs environmental discovery to check if we have some classes available on {@code CLASSPATH}.
	 */
	private void discovery() {
		// that's how it's done in Tomcat. We may try to use Jetty ALPN bootclasspath approach later
		alpnAvailable = JreCompat.isJre9Available();

		try {
			classLoader.loadClass("org.apache.coyote.http2.Http2Protocol");
			http2Available = true;
		} catch (ClassNotFoundException e) {
			http2Available = false;
		}
	}

	/*
	 * Tomcat uses several "layers" of a running container.
	 *  - "server" (org.apache.catalina.core.StandardServer) - entire Catalina instance with "services" (usually 1)
	 *  - "service" (org.apache.catalina.core.StandardService) - groups connectors sharing single "container"
	 *  - "container" - either "engine", "host", "context" or "wrapper"
	 *  - "connector" (org.apache.catalina.connector.Connector) - translates network request into logical invocation
	 *  - "engine" (org.apache.catalina.core.StandardEngine) - the only acceptable "container" in "standard service"
	 *  - "host" (org.apache.catalina.core.StandardHost) - "engine" holds many of those virtual hosts
	 *  - "context" (org.apache.catalina.core.StandardContext) - single "web application" (ServletContext)
	 *  - "wrapper" (org.apache.catalina.core.StandardWrapper) - holds single servlet instance
	 */

	public Executor createThreadPool(Configuration configuration) {
		ServerConfiguration sc = configuration.server();

		// defaults taken from org.apache.catalina.core.StandardThreadExecutor
		Integer maxThreads = sc.getServerMaxThreads();
		if (maxThreads == null) {
			maxThreads = 200;
		}
		Integer minThreads = sc.getServerMinThreads();
		if (minThreads == null) {
			// Tomcat uses 25 by default, but let's take 8
			minThreads = Math.min(8, maxThreads);
		}
		Integer idleTimeout = sc.getServerIdleTimeout();
		if (idleTimeout == null) {
			idleTimeout = 60000;
		}
		String prefix = sc.getServerThreadNamePrefix();

		StandardThreadExecutor executor = new StandardThreadExecutor();
		executor.setName("default");
		executor.setMaxThreads(maxThreads);
		executor.setMinSpareThreads(minThreads);
		executor.setMaxIdleTime(idleTimeout);

		if (prefix != null) {
			executor.setNamePrefix(prefix);
		}

		return executor;
	}

	/*
	 * Simpler (than in Jetty) hierarchy of connector/protocol related classes in Tomcat
	 * org.apache.coyote.ProtocolHandler
	 *   |
	 *   +- org.apache.coyote.AbstractProtocol
	 *      |
	 *      +- org.apache.coyote.ajp.AbstractAjpProtocol
	 *      |  |
	 *      |  +- org.apache.coyote.ajp.AjpAprProtocol
	 *      |  +- org.apache.coyote.ajp.AjpNio2Protocol
	 *      |  +- org.apache.coyote.ajp.AjpNioProtocol
	 *      |
	 *      +- org.apache.coyote.http11.AbstractHttp11Protocol
	 *         |
	 *         +- org.apache.coyote.http11.AbstractHttp11JsseProtocol
	 *         |  |
	 *         |  +- org.apache.coyote.http11.Http11Nio2Protocol
	 *         |  +- org.apache.coyote.http11.Http11NioProtocol
	 *         |
	 *         +- org.apache.coyote.http11.Http11AprProtocol
	 */

	public Connector createDefaultConnector(Server server, String address, Executor executor,
			Configuration configuration) {
		ServerConfiguration sc = configuration.server();

		Connector defaultConnector = new Connector("org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol");

		defaultConnector.setProperty("address", address);
		defaultConnector.setPort(sc.getHttpPort());
		defaultConnector.setScheme("http");
		defaultConnector.setSecure(false);
		if (sc.isHttpSecureEnabled()) {
			defaultConnector.setRedirectPort(sc.getHttpSecurePort());
		}

		PaxWebHttp11Nio2Protocol protocol = (PaxWebHttp11Nio2Protocol) defaultConnector.getProtocolHandler();

		defaultConnector.setXpoweredBy(false);
		defaultConnector.setAllowTrace(false);
		protocol.setServer(null);
		protocol.setServerRemoveAppProvidedValues(true);

		// don't set an executor here, as we'd get warning:
		// "The NIO2 connector requires an exclusive executor to operate properly on shutdown"
//		defaultConnector.getProtocolHandler().setExecutor(executor);

		if (sc.getConnectorIdleTimeout() != null) {
			defaultConnector.setProperty("connectionTimeout", sc.getConnectorIdleTimeout().toString());
		}

		if (http2Available) {
			LOG.info("HTTP/2 ClearText support available, adding \"h2c\" protocol support to default connector");
			defaultConnector.addUpgradeProtocol(new Http2Protocol());
		}

		LOG.info("Default Tomcat connector created: {}", defaultConnector);

		return defaultConnector;
	}

	public Connector createSecureConnector(Server server, String address, Executor executor,
			Configuration configuration) {
		ServerConfiguration sc = configuration.server();
		SecurityConfiguration secc = configuration.security();

		// In Tomcat, "new" SSL/TLS configuration is done via org.apache.tomcat.util.net.SSLHostConfig object
		// but this object is configured via old/legacy/soon-deprecated setters on
		// org.apache.coyote.http11.AbstractHttp11Protocol class

		Connector secureConnector = new Connector("org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol");

		secureConnector.setProperty("address", address);
		secureConnector.setPort(sc.getHttpSecurePort());
		secureConnector.setScheme("https");
		secureConnector.setSecure(true);
		secureConnector.setProperty("SSLEnabled", "true");

		PaxWebHttp11Nio2Protocol protocol = (PaxWebHttp11Nio2Protocol) secureConnector.getProtocolHandler();

		protocol.setSslImplementationName("org.apache.tomcat.util.net.jsse.JSSEImplementation");

		secureConnector.setXpoweredBy(false);
		secureConnector.setAllowTrace(false);
		protocol.setServer(null);
		protocol.setServerRemoveAppProvidedValues(true);

		// don't set an executor here, as we'd get warning:
		// "The NIO2 connector requires an exclusive executor to operate properly on shutdown"
//		protocol.setExecutor(executor);

		// --- server keystore for server's own identity

		String sslKeystore = secc.getSslKeystore();
		if (sslKeystore == null) {
			throw new IllegalArgumentException("Location of server keystore is not specified"
					+ " (org.ops4j.pax.web.ssl.keystore property).");
		}
		protocol.setKeystoreFile(sslKeystore);

		if (secc.getSslKeystorePassword() == null) {
			throw new IllegalArgumentException("Missing server keystore password.");
		}
		if (secc.getSslKeyPassword() == null) {
			throw new IllegalArgumentException("Missing private key password.");
		}
		protocol.setKeystorePass(secc.getSslKeystorePassword());
		protocol.setKeyPass(secc.getSslKeyPassword());

		if (secc.getSslKeyManagerFactoryAlgorithm() != null) {
			LOG.debug("Not supported SSL Key Algorithm parameter");
		}
		if (secc.getSslKeyAlias() != null) {
			protocol.setKeyAlias(secc.getSslKeyAlias());
		}
		if (secc.getSslKeystoreType() != null) {
			protocol.setKeystoreType(secc.getSslKeystoreType());
		}
		if (secc.getSslKeystoreProvider() != null && !"".equals(secc.getSslKeystoreProvider().trim())) {
			protocol.setKeystoreProvider(secc.getSslKeystoreProvider());
		}

		// --- server truststore for client validation

		protocol.setTruststoreFile(secc.getTruststore());

		if (secc.getTruststore() != null) {
			if (secc.getTruststorePassword() == null) {
				throw new IllegalArgumentException("Missing server truststore password.");
			}
			protocol.setTruststorePass(secc.getTruststorePassword());
		}
		if (secc.getTruststoreType() != null) {
			protocol.setTruststoreType(secc.getTruststoreType());
		}
		if (secc.getTruststoreProvider() != null && !"".equals(secc.getTruststoreProvider().trim())) {
			protocol.setTruststoreProvider(secc.getTruststoreProvider());
		}
		if (secc.getTrustManagerFactoryAlgorithm() != null) {
			protocol.setTruststoreAlgorithm(secc.getTrustManagerFactoryAlgorithm());
		}

		if (secc.isClientAuthWanted() != null) {
			protocol.setClientAuth("want");
		}
		if (secc.isClientAuthNeeded() != null) {
			protocol.setClientAuth("require");
		}

		// --- protocols and cipher suites

		String[] supportedProtocols = new String[0];
		String[] supportedCipherSuites = new String[0];

		try {
			SSLParameters params = SSLContext.getDefault().getSupportedSSLParameters();
			supportedProtocols = params.getProtocols();
			supportedCipherSuites = params.getCipherSuites();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException("Problem checking supported protocols and ciphers suites: "
					+ e.getMessage(), e);
		}

		if (secc.getProtocolsIncluded().length > 0) {
			protocol.setSslEnabledProtocols(String.join(",", secc.getProtocolsIncluded()));
		}
		if (secc.getCiphersuiteIncluded().length > 0) {
			protocol.setSSLCipherSuite(String.join(":", secc.getCiphersuiteIncluded()));
		}

		if (secc.getSslProtocol() != null) {
			protocol.setSSLProtocol(secc.getSslProtocol());
		}

		if (secc.getSecureRandomAlgorithm() != null) {
			LOG.debug("Not supported Secure Random Algorithm parameter");
		}

		// javax.net.ssl.SSLParameters.setUseCipherSuitesOrder()
		protocol.setUseServerCipherSuitesOrder(true);

		// --- SSL session and renegotiation

		if (secc.isSslRenegotiationAllowed() != null) {
			LOG.debug("Not supported SSL Renegotiation Allowed parameter");
		}
		if (secc.getSslRenegotiationLimit() != null) {
			LOG.debug("Not supported SSL Renegotiation Limit parameter");
		}

		if (secc.getSslSessionsEnabled() != null && !secc.getSslSessionsEnabled()) {
			protocol.setSessionCacheSize(0);
		} else if (secc.getSslSessionCacheSize() != null) {
			protocol.setSessionCacheSize(secc.getSslSessionCacheSize());
		}
		if (secc.getSslSessionTimeout() != null) {
			protocol.setSessionTimeout(secc.getSslSessionTimeout());
		}

		// TODO: certificate validation in Tomcat
//		// whether certificates in server keystore should be validated on load. It's not about
//		// validating certificates from incoming SSL connections!
//		if (secc.isValidateCerts() != null) {
//			sslContextFactory.setValidateCerts(secc.isValidateCerts());
//		}
//		// whether certificates in server truststore should validated on load. Only if TrustManagerFactory algorithm
//		// is "PKIX" and not "SunX509"
//		if (secc.isValidatePeerCerts() != null) {
//			sslContextFactory.setValidatePeerCerts(secc.isValidatePeerCerts());
//		}
//
//		if (secc.getCrlPath() != null && !"".equals(secc.getCrlPath().trim())) {
//			sslContextFactory.setCrlPath(secc.getCrlPath());
//		}
//		if (secc.isEnableOCSP() != null) {
//			sslContextFactory.setEnableOCSP(secc.isEnableOCSP());
//		}
//		if (secc.isEnableCRLDP() != null) {
//			sslContextFactory.setEnableCRLDP(secc.isEnableCRLDP());
//		}
//		if (secc.getOcspResponderURL() != null && !"".equals(secc.getOcspResponderURL().trim())) {
//			sslContextFactory.setOcspResponderURL(secc.getOcspResponderURL());
//		}
//		if (secc.getMaxCertPathLength() != null) {
//			sslContextFactory.setMaxCertPathLength(secc.getMaxCertPathLength());
//		}

		if (http2Available) {
			LOG.info("HTTP/2 support available, adding \"h2\" protocol support to secure connector");

			secureConnector.addUpgradeProtocol(new Http2Protocol());
		}

		LOG.info("Secure Tomcat connector created: {}", secureConnector);

		return secureConnector;
	}

}
