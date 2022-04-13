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
package org.ops4j.pax.web.service.jetty.internal;

import java.lang.management.ManagementFactory;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Helper class that's used to create various parts of embedded Jetty. {@link Bundle} can be passed, so
 * the factory does some discovery, to check if some Jetty mechanisms can be enabled.</p>
 *
 * <p>There should be single instance of this factory and all {@code createXXX()} methods should operate
 * on passed {@link Configuration}, because those methods work on behalf of possibly many different
 * {@link org.ops4j.pax.web.service.spi.ServerController controllers}.</p>
 */
class JettyFactory {

	private static final Logger LOG = LoggerFactory.getLogger(JettyFactory.class);

	private final Bundle paxWebJettyBundle;
	private final ClassLoader classLoader;

	private boolean alpnAvailable;
	private boolean http2Available;

	JettyFactory(Bundle paxWebJettyBundle, ClassLoader classLoader) {
		this.paxWebJettyBundle = paxWebJettyBundle;
		this.classLoader = classLoader;

		discovery();
	}

	/**
	 * Performs environmental discovery to check if we have some classes available on {@code CLASSPATH}.
	 */
	private void discovery() {
		try {
			classLoader.loadClass("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory");
			alpnAvailable = true;
		} catch (ClassNotFoundException e) {
			alpnAvailable = false;
		}

		try {
			classLoader.loadClass("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory");
			classLoader.loadClass("org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory");
			http2Available = true;
		} catch (ClassNotFoundException e) {
			http2Available = false;
		}
	}

	/**
	 * Create {@link QueuedThreadPool} for Jetty Server
	 *
	 * @param configuration
	 * @return
	 */
	public QueuedThreadPool createThreadPool(Configuration configuration) {
		ServerConfiguration sc = configuration.server();

		// org.eclipse.jetty.util.thread.ThreadPool required by org.eclipse.jetty.server.Server
		// defaults taken from org.eclipse.jetty.util.thread.QueuedThreadPool
		Integer maxThreads = sc.getServerMaxThreads();
		if (maxThreads == null) {
			maxThreads = 200;
		}
		Integer minThreads = sc.getServerMinThreads();
		if (minThreads == null) {
			minThreads = Math.min(8, maxThreads);
		}
		Integer idleTimeout = sc.getServerIdleTimeout();
		if (idleTimeout == null) {
			idleTimeout = 60000;
		}
		String prefix = sc.getServerThreadNamePrefix();

		QueuedThreadPool qtp = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
		if (prefix != null) {
			qtp.setName(prefix);
		}

		// PAXWEB-1127: load org.eclipse.jetty.util.FutureCallback class, so it's there when we shutdown connectors
		// to avoid NPE in org.apache.felix.framework.BundleWiringImpl.searchImports()
		try {
			ServerConnector.class.getClassLoader().loadClass("org.eclipse.jetty.util.FutureCallback");
		} catch (Exception ignored) {
		}

		// load some required classes
		ClassLoader cl = QueuedThreadPool.class.getClassLoader();
		for (int i = 1; i <= 3; i++) {
			try {
				cl.loadClass("org.eclipse.jetty.util.thread.QueuedThreadPool$" + i);
			} catch (Exception ignored) {
			}
		}

		return qtp;
	}

	/*
	 * org.eclipse.jetty.server.ConnectionFactory hierarchy in Jetty 9.4.x:
	 *
	 * org.eclipse.jetty.server.AbstractConnectionFactory - base abstract class of all factories
	 *   |
	 *   +- org.eclipse.jetty.server.ProxyConnectionFactory - PROXY connection factory
	 *   |  (https://www.haproxy.org/download/2.2/doc/proxy-protocol.txt)
	 *   |
	 *   +- org.eclipse.jetty.server.HttpConnectionFactory - main "HTTP/1.1" connection factory
	 *   |  (https://tools.ietf.org/html/rfc7230)
	 *   |
	 *   +- org.eclipse.jetty.server.SslConnectionFactory - main "SSL" connection factory which uses
	 *   |  javax.net.ssl.SSLEngine for each connection
	 *   |
	 *   +- org.eclipse.jetty.fcgi.server.ServerFCGIConnectionFactory - "fcgi/1.0" connection factory
	 *   |
	 *   +- org.eclipse.jetty.server.OptionalSslConnectionFactory - connection factory that can detect SSL/TLS
	 *   |  connection or switch to other protocol
	 *   |
	 *   +- org.eclipse.jetty.server.NegotiatingServerConnectionFactory
	 *   |  |
	 *   |  +- org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
	 *   |     (https://tools.ietf.org/html/rfc7301)
	 *   |
	 *   +- org.eclipse.jetty.http2.server.AbstractHTTP2ServerConnectionFactory - abstract connection factory
	 *      that supports "h2" protocol
	 *      |
	 *      +- org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
	 *      |  |
	 *      |  +- org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory - "h2c" connection factory
	 *      |
	 *      +- org.eclipse.jetty.http2.server.RawHTTP2ServerConnectionFactory
	 *
	 * org.eclipse.jetty.embedded.Http2Server shows how to configure H2 and ALPN. There are two ServerConnectors:
	 * 1. "default" ServerConnector with two factories (in that order):
	 *     - org.eclipse.jetty.server.HttpConnectionFactory with org.eclipse.jetty.server.HttpConfiguration
	 *     - org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory with the same
	 *       org.eclipse.jetty.server.HttpConfiguration
	 * 2. "secure" ServerConnector with:
	 *     - org.eclipse.jetty.server.SslConnectionFactory with the same above
	 *       org.eclipse.jetty.server.HttpConfiguration + additional org.eclipse.jetty.server.SecureRequestCustomizer
	 *     - org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory with "alpn" as protocols and
	 *       "HTTP/1.1" as default (fallback) protocol
	 *     - org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory with same httpsConfig as in
	 *       SslConnectionFactory
	 *     - org.eclipse.jetty.server.HttpConnectionFactory with same httpsConfig
	 *
	 * For ALPN, Jetty uses one of:
	 *  - org.eclipse.jetty.alpn.openjdk8.server.OpenJDK8ServerALPNProcessor (requires org.eclipse.jetty.alpn.ALPN
	 *    on bootclasspath): https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html
	 *    JETTY_HOME/modules/alpn.mod specifies default protocols as "jetty.alpn.protocols=h2,http/1.1"
	 *  - org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor (https://github.com/google/conscrypt/)
	 *
	 * For HTTP/2, protocol IDs and implementations are specified at
	 * https://github.com/http2/http2-spec/wiki/Implementations
	 */

	/**
	 * Create {@link Connector} that matches <em>non secure</em> connector defined in Http Service specification
	 *
	 * @param server
	 * @param httpConfigs
	 * @param address
	 * @param configuration
	 * @return
	 */
	public Connector createDefaultConnector(Server server, Map<String, HttpConfiguration> httpConfigs,
			String address, Configuration configuration) {
		ServerConfiguration sc = configuration.server();

		// is there existing HttpConfiguration from jetty*.xml?
		HttpConfiguration httpConfig = getOrCreateHttpConfiguration(httpConfigs, sc);

		ServerConnector defaultConnector = new ServerConnector(server);
		defaultConnector.clearConnectionFactories();

		defaultConnector.setHost(address);
		defaultConnector.setPort(sc.getHttpPort());
		defaultConnector.setName(sc.getHttpConnectorName());
		if (sc.getConnectorIdleTimeout() != null) {
			defaultConnector.setIdleTimeout(sc.getConnectorIdleTimeout());
		}

		defaultConnector.addConnectionFactory(new HttpConnectionFactory(httpConfig));
		if (http2Available) {
			LOG.info("HTTP/2 ClearText support available, adding \"h2c\" protocol support to default connector");
			defaultConnector.addConnectionFactory(new HTTP2CServerConnectionFactory(httpConfig));
		}

		LOG.info("Default Jetty connector created: {}", defaultConnector);

		return defaultConnector;
	}

	/**
	 * Create {@link Connector} that matches <em>secure</em> connector defined in Http Service specification
	 *
	 * @param server
	 * @param httpConfigs
	 * @param address
	 * @param configuration
	 * @return
	 */
	public Connector createSecureConnector(Server server, Map<String, HttpConfiguration> httpConfigs,
			String address, Configuration configuration) {
		ServerConfiguration sc = configuration.server();
		SecurityConfiguration secc = configuration.security();

		// is there existing HttpConfiguration from jetty*.xml?
		HttpConfiguration httpsConfig = getOrCreateHttpConfiguration(httpConfigs, sc);
		if (httpsConfig.getCustomizer(SecureRequestCustomizer.class) == null) {
			httpsConfig.addCustomizer(new SecureRequestCustomizer());
		}

		// see org.eclipse.jetty.embedded.Http2Server#main() for how it SHOULD be done

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		if (secc.getSslProvider() != null) {
			sslContextFactory.setProvider(secc.getSslProvider());
		}

		if (http2Available) {
			sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		}

		// --- server keystore for server's own identity

		String sslKeystore = secc.getSslKeystore();
		if (sslKeystore == null) {
			throw new IllegalArgumentException("Location of server keystore is not specified"
					+ " (org.ops4j.pax.web.ssl.keystore property).");
		}
		sslContextFactory.setKeyStorePath(sslKeystore);

		if (secc.getSslKeystorePassword() == null) {
			throw new IllegalArgumentException("Missing server keystore password.");
		}
		if (secc.getSslKeyPassword() == null) {
			throw new IllegalArgumentException("Missing private key password.");
		}
		sslContextFactory.setKeyStorePassword(secc.getSslKeystorePassword());
		sslContextFactory.setKeyManagerPassword(secc.getSslKeyPassword());

		if (secc.getSslKeyManagerFactoryAlgorithm() != null) {
			sslContextFactory.setKeyManagerFactoryAlgorithm(secc.getSslKeyManagerFactoryAlgorithm());
		}
		if (secc.getSslKeyAlias() != null) {
			sslContextFactory.setCertAlias(secc.getSslKeyAlias());
		}
		if (secc.getSslKeystoreType() != null) {
			sslContextFactory.setKeyStoreType(secc.getSslKeystoreType());
		}
		if (secc.getSslKeystoreProvider() != null && !"".equals(secc.getSslKeystoreProvider().trim())) {
			sslContextFactory.setKeyStoreProvider(secc.getSslKeystoreProvider());
		}

		// --- server truststore for client validation

		sslContextFactory.setTrustStorePath(secc.getTruststore());

		if (secc.getTruststore() != null) {
			if (secc.getTruststorePassword() == null) {
				throw new IllegalArgumentException("Missing server truststore password.");
			}
			sslContextFactory.setTrustStorePassword(secc.getTruststorePassword());
		}
		if (secc.getTruststoreType() != null) {
			sslContextFactory.setTrustStoreType(secc.getTruststoreType());
		}
		if (secc.getTruststoreProvider() != null && !"".equals(secc.getTruststoreProvider().trim())) {
			sslContextFactory.setTrustStoreProvider(secc.getTruststoreProvider());
		}
		if (secc.getTrustManagerFactoryAlgorithm() != null) {
			sslContextFactory.setTrustManagerFactoryAlgorithm(secc.getTrustManagerFactoryAlgorithm());
		}

		if (secc.isClientAuthWanted() != null) {
			sslContextFactory.setWantClientAuth(secc.isClientAuthWanted());
		}
		if (secc.isClientAuthNeeded() != null) {
			sslContextFactory.setNeedClientAuth(secc.isClientAuthNeeded());
		}

		// https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#jssenames
		// javax.net.ssl.SSLParameters.setEndpointIdentificationAlgorithm()
		// setEndpointIdentificationAlgorithm turns on sun.security.ssl.X509TrustManagerImpl.checkIdentity()
		// invocation in trust manager which requires SAN extension in client certificate
//		sslContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
		sslContextFactory.setTrustAll(false);
		// if endpointIdentificationAlgorithm == null, HostnameVerifier has to be passed instead
		sslContextFactory.setHostnameVerifier(null);

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

		if (secc.getProtocolsIncluded() != null) {
			sslContextFactory.setIncludeProtocols(secc.getProtocolsIncluded());
		}
		if (secc.getProtocolsExcluded() != null) {
			sslContextFactory.setExcludeProtocols(secc.getProtocolsExcluded());
		}
		if (secc.getCiphersuiteIncluded() != null) {
			sslContextFactory.setIncludeCipherSuites(secc.getCiphersuiteIncluded());
		}
		if (secc.getCiphersuiteExcluded() != null) {
			sslContextFactory.setExcludeCipherSuites(secc.getCiphersuiteExcluded());
		}

		if (secc.getSslProtocol() != null) {
			sslContextFactory.setProtocol(secc.getSslProtocol());
		}

		if (secc.getSecureRandomAlgorithm() != null) {
			sslContextFactory.setSecureRandomAlgorithm(secc.getSecureRandomAlgorithm());
		}

		// javax.net.ssl.SSLParameters.setUseCipherSuitesOrder()
		sslContextFactory.setUseCipherSuitesOrder(true);

		// --- SSL session and renegotiation

		if (secc.isSslRenegotiationAllowed() != null) {
			sslContextFactory.setRenegotiationAllowed(secc.isSslRenegotiationAllowed());
		}
		if (secc.getSslRenegotiationLimit() != null) {
			sslContextFactory.setRenegotiationLimit(secc.getSslRenegotiationLimit());
		}

		if (secc.getSslSessionsEnabled() != null) {
			sslContextFactory.setSessionCachingEnabled(secc.getSslSessionsEnabled());
		}
		if (secc.getSslSessionCacheSize() != null) {
			sslContextFactory.setSslSessionCacheSize(secc.getSslSessionCacheSize());
		}
		if (secc.getSslSessionTimeout() != null) {
			sslContextFactory.setSslSessionTimeout(secc.getSslSessionTimeout());
		}

		// whether certificates in server keystore should be validated on load. It's not about
		// validating certificates from incoming SSL connections!
		if (secc.isValidateCerts() != null) {
			sslContextFactory.setValidateCerts(secc.isValidateCerts());
		}
		// whether certificates in server truststore should validated on load. Only if TrustManagerFactory algorithm
		// is "PKIX" and not "SunX509"
		if (secc.isValidatePeerCerts() != null) {
			sslContextFactory.setValidatePeerCerts(secc.isValidatePeerCerts());
		}

		if (secc.getCrlPath() != null && !"".equals(secc.getCrlPath().trim())) {
			sslContextFactory.setCrlPath(secc.getCrlPath());
		}
		if (secc.isEnableOCSP() != null) {
			sslContextFactory.setEnableOCSP(secc.isEnableOCSP());
		}
		if (secc.isEnableCRLDP() != null) {
			sslContextFactory.setEnableCRLDP(secc.isEnableCRLDP());
		}
		if (secc.getOcspResponderURL() != null && !"".equals(secc.getOcspResponderURL().trim())) {
			sslContextFactory.setOcspResponderURL(secc.getOcspResponderURL());
		}
		if (secc.getMaxCertPathLength() != null) {
			sslContextFactory.setMaxCertPathLength(secc.getMaxCertPathLength());
		}

		// whew. org.eclipse.jetty.util.ssl.SslContextFactory.Server is configured now

		ServerConnector secureConnector = new ServerConnector(server, null, null, null, -1, -1);
		secureConnector.clearConnectionFactories();

		secureConnector.setHost(address);
		secureConnector.setPort(sc.getHttpSecurePort());
		secureConnector.setName(sc.getHttpSecureConnectorName());
		if (sc.getConnectorIdleTimeout() != null) {
			secureConnector.setIdleTimeout(sc.getConnectorIdleTimeout());
		}

		// connection factories of secure connector in order presented in org.eclipse.jetty.embedded.Http2Server:
		// 1. org.eclipse.jetty.server.SslConnectionFactory with "alpn" specified as _next_ protocol
		// 2. org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory with HttpVersion.HTTP_1_1 as _default_ protocol
		// 3. org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory with https config
		// 4. org.eclipse.jetty.server.HttpConnectionFactory as last connection factory

		SslConnectionFactory ssl = null;
		if (alpnAvailable) {
			LOG.info("ALPN support available, adding \"alpn\" protocol support to secure connector");

			secureConnector.addConnectionFactory(new SslConnectionFactory(sslContextFactory, "ALPN"));

			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				OsgiServletContextClassLoader cl = new OsgiServletContextClassLoader();
				Bundle bundle = FrameworkUtil.getBundle(this.getClass());
				if (bundle != null) {
					// non unit-test
					cl.addBundle(bundle);
					for (Bundle b : bundle.getBundleContext().getBundles()) {
						String sn = b.getSymbolicName();
						if ("org.eclipse.jetty.io".equals(sn)
								|| "org.eclipse.jetty.alpn.java.server".equals(sn)
								|| "org.eclipse.jetty.alpn.openjdk8.server".equals(sn)) {
							cl.addBundles(b);
						}
					}
					Thread.currentThread().setContextClassLoader(cl);
				}
				ALPNServerConnectionFactory alpnConnectionFactory = new ALPNServerConnectionFactory();
				// if no protocol can be negotiated, we'll force HTTP/1.1
				alpnConnectionFactory.setDefaultProtocol(HttpVersion.HTTP_1_1.asString());
				secureConnector.addConnectionFactory(alpnConnectionFactory);
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		} else {
			// no ALPN, so no HTTP/2
			// if we want to support HTTP/2 over TLS without ALPN, alpn extension
			// (nr 16 in https://www.iana.org/assignments/tls-extensiontype-values/tls-extensiontype-values.xhtml#tls-extensiontype-values-1)
			// can't be sent in ClientHello, so we'd need HttpConnectionFactory and then HTTP2CServerConnectionFactory
			// and require:
			//  > GET / HTTP/1.1
			//  > ...
			//  > Connection: Upgrade, HTTP2-Settings
			//  > Upgrade: h2c
			//  > HTTP2-Settings: ...
			//
			// to be sent over SSL and wait for HTTP2CServerConnectionFactory to switch the connection:
			//  < HTTP/1.1 101 Switching Protocols
			//
			// without ALPN, the only factory that can upgrade connections (in Jetty - instance of
			// org.eclipse.jetty.server.ConnectionFactory.Upgrading) is
			// org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory
			//
			// that's why we DON'T support this scenario

			LOG.info("No ALPN support available, no way to upgrade to HTTP/2 over SSL, no \"h2\" protocol "
					+ "support added.");

			secureConnector.addConnectionFactory(new SslConnectionFactory(sslContextFactory,
					HttpVersion.HTTP_1_1.asString()));
		}

		if (http2Available) {
			LOG.info("HTTP/2 support available, adding \"h2\" protocol support to secure connector");

			secureConnector.addConnectionFactory(new HTTP2ServerConnectionFactory(httpsConfig));
		}

		// final connection factory
		secureConnector.addConnectionFactory(new HttpConnectionFactory(httpsConfig));

		LOG.info("Secure Jetty connector created: {}", secureConnector);

		return secureConnector;
	}

	/**
	 * If {@link HttpConfiguration} was created earlier (when parsing {@code jetty*.xml} files) return the first
	 * one after doing some tweaks. If there was no {@link HttpConfiguration}, create one using {@link Configuration}.
	 *
	 * @param httpConfigs
	 * @param sc
	 * @return
	 */
	private HttpConfiguration getOrCreateHttpConfiguration(Map<String, HttpConfiguration> httpConfigs,
			ServerConfiguration sc) {
		HttpConfiguration httpConfig = null;
		if (httpConfigs.size() > 0) {
			httpConfig = httpConfigs.values().iterator().next();
			if (httpConfigs.size() > 1) {
				LOG.warn("More than one HttpConfiguration found in external Jetty configuration. Using {}.",
						httpConfig);
			}
		} else {
			httpConfig = new HttpConfiguration();
			httpConfig.setSendXPoweredBy(false);
			httpConfig.setSendServerVersion(false);
		}

		if (httpConfig.getSecureScheme() == null) {
			httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
		}
		if (httpConfig.getSecurePort() <= 0) {
			httpConfig.setSecurePort(sc.getHttpSecurePort());
		}
		if (httpConfig.getOutputBufferSize() <= 0) {
			// default from org.eclipse.jetty.server.HttpConfiguration._outputBufferSize
			httpConfig.setOutputBufferSize(32768);
		}
		httpConfig.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);

		if (sc.checkForwardedHeaders() != null && sc.checkForwardedHeaders()) {
			httpConfig.addCustomizer(new ForwardedRequestCustomizer());
//			if (priorityComparator != null) {
//				@SuppressWarnings("unchecked")
//				Comparator<Customizer> comparator = (Comparator<Customizer>) priorityComparator;
//				List<Customizer> customizers = httpConfig.getCustomizers();
//				Collections.sort(customizers, comparator);
//			}
		}

		return httpConfig;
	}

	/**
	 * If possible, enable JMX support for Jetty. See {@code JETTY_HOME/etc/jetty-jmx.xml}.
	 *
	 * @param server
	 */
	public MBeanContainer enableJmxIfPossible(Server server) {
		try {
			ClassLoader cl = classLoader;
			if (paxWebJettyBundle != null) {
				cl = paxWebJettyBundle.adapt(BundleWiring.class).getClassLoader();
			}
			cl.loadClass("javax.management.JMX");
			cl.loadClass("org.eclipse.jetty.jmx.MBeanContainer");

			LOG.info("Adding JMX support to Jetty server");

			MBeanContainer mBeanContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
			server.addBean(mBeanContainer);

			return mBeanContainer;
		} catch (Throwable ignored) {
			LOG.info("No JMX available. Skipping Jetty JMX configuration.");
			return null;
		}
	}

}
