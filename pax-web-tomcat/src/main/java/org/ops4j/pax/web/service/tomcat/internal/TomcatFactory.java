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

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.catalina.Executor;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ConnectorCreateRule;
import org.apache.catalina.startup.ContextConfig;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.compat.JreCompat;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.Rule;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * <p>Helper class that's used to create various parts of embedded Tomcat similar to Jetty equivalent.</p>
 *
 * <p>There should be single instance of this factory and all {@code createXXX()} methods should operate
 * on passed {@link Configuration}, because those methods work on behalf of possibly many different
 * {@link org.ops4j.pax.web.service.spi.ServerController controllers}.</p>
 */
public class TomcatFactory {

	private static final Logger LOG = LoggerFactory.getLogger(TomcatFactory.class);

	private final ClassLoader classLoader;

	private boolean alpnAvailable;
	private boolean http2Available;

	TomcatFactory(ClassLoader classLoader) {
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

		Connector defaultConnector = new PaxWebConnector("org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol");

		defaultConnector.setProperty("PaxWebConnectorName", sc.getHttpConnectorName());
		defaultConnector.setProperty("address", address);
		defaultConnector.setPort(sc.getHttpPort());
		defaultConnector.setScheme("http");
		defaultConnector.setSecure(false);
		if (sc.isHttpSecureEnabled()) {
			defaultConnector.setRedirectPort(sc.getHttpSecurePort());
		}

		PaxWebHttp11Nio2Protocol protocol = (PaxWebHttp11Nio2Protocol) defaultConnector.getProtocolHandler();
		protocol.setConnector(defaultConnector);

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

		Connector secureConnector = new PaxWebConnector("org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol");

		secureConnector.setProperty("PaxWebConnectorName", sc.getHttpSecureConnectorName());
		secureConnector.setProperty("address", address);
		secureConnector.setPort(sc.getHttpSecurePort());
		secureConnector.setScheme("https");
		secureConnector.setSecure(true);
		secureConnector.setProperty("SSLEnabled", "true");

		PaxWebHttp11Nio2Protocol protocol = (PaxWebHttp11Nio2Protocol) secureConnector.getProtocolHandler();
		protocol.setConnector(secureConnector);

		protocol.setSslImplementationName("org.apache.tomcat.util.net.jsse.JSSEImplementation");

		secureConnector.setXpoweredBy(false);
		secureConnector.setAllowTrace(false);
		protocol.setServer(null);
		protocol.setServerRemoveAppProvidedValues(true);

		// don't set an executor here, as we'd get warning:
		// "The NIO2 connector requires an exclusive executor to operate properly on shutdown"
//		protocol.setExecutor(executor);

		if (sc.getConnectorIdleTimeout() != null) {
			secureConnector.setProperty("connectionTimeout", sc.getConnectorIdleTimeout().toString());
		}

		// --- server keystore for server's own identity

		String sslKeystore = secc.getSslKeystore();
		if (sslKeystore != null) {
			protocol.setKeystoreFile(sslKeystore);
		}
		if (secc.getSslKeystorePassword() != null) {
			protocol.setKeystorePass(secc.getSslKeystorePassword());
		}
		if (secc.getSslKeyPassword() != null) {
			protocol.setKeyPass(secc.getSslKeyPassword());
		}

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

		String sslTruststore = secc.getTruststore();
		if (sslTruststore != null) {
			protocol.setTruststoreFile(sslTruststore);
		}
		if (secc.getTruststorePassword() != null) {
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

		if (secc.isClientAuthWanted() != null && secc.isClientAuthWanted()) {
			protocol.setClientAuth("want");
		}
		if (secc.isClientAuthNeeded() != null && secc.isClientAuthNeeded()) {
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


	/**
	 * Returns a Tomcat-specific XML processor to parse {@code tomcat-server.xml}.
	 * @return
	 * @param configuration
	 */
	public Digester createServerDigester(Configuration configuration) {
		Digester digester = new PaxWebCatalina().createStartDigester();

		// special rule for catalinaHome / catalinaBase attributes
		digester.getRules().match("", "Server").add(new BaseDirsRule(digester));

		// special rule for Connector/@protocol - ensure that "HTTP/1.1" is changed to
		// "org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol"
		List<Rule> rules = digester.getRules().match("", "Server/Service/Connector");
		int idx = 0;
		for (Iterator<Rule> iterator = rules.iterator(); iterator.hasNext(); ) {
			Rule rule = iterator.next();
			if (rule instanceof ConnectorCreateRule) {
				iterator.remove();
				break;
			}
			idx++;
		}
		rules.add(idx, new PaxWebConnectorCreateRule(digester));
		rules.add(new PaxWebConnectorSetName(digester, configuration.server()));

		return digester;
	}

	public Digester createContextDigester() {
		return new PaxWebCatalinaContextConfig().createContextDigester();
	}

	private static class PaxWebCatalina extends Catalina {
		PaxWebCatalina() {
		}

		@Override
		public Digester createStartDigester() {
			Digester digester = super.createStartDigester();
			digester.setClassLoader(PaxWebCatalina.class.getClassLoader());
			return digester;
		}
	}

	private static class PaxWebCatalinaContextConfig extends ContextConfig {
		PaxWebCatalinaContextConfig() {
		}

		@Override
		public Digester createContextDigester() {
			return super.createContextDigester();
		}
	}

	/**
	 * A class on which Tomcat digester can call {@link #setServer(Server)}
	 */
	public static class ServerHolder {
		private Server server;

		public Server getServer() {
			return server;
		}

		public void setServer(Server server) {
			this.server = server;
		}
	}

	/**
	 * Original {@link Digester} for Tomcat's {@code server.xml} can't set {@code catalinaHome}
	 * and {@code catalinaBase} properties, because there's no String->File converter. We'll fix this
	 * by adding new special {@link Rule}.
	 */
	private static class BaseDirsRule extends Rule {
		BaseDirsRule(Digester digester) {
			this.digester = digester;
		}

		@Override
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			Object top = digester.peek();
			if (top instanceof StandardServer) {
				String home = attributes.getValue("catalinaHome");
				if (home != null && !"".equals(home)) {
					boolean ok = false;
					File catalinaHome = new File(home);
					if (catalinaHome.isFile()) {
						LOG.warn("Can't set catalina home to {}. It is an existing file.", home);
					} else if (!catalinaHome.isDirectory()) {
						if (!catalinaHome.mkdirs()) {
							LOG.warn("Can't set catalina home to {}. Can't create directory.", home);
						} else {
							ok = true;
						}
					} else {
						ok = true;
					}
					if (ok) {
						((StandardServer) top).setCatalinaHome(new File(home));
					}
				}
				String base = attributes.getValue("catalinaBase");
				if (base != null && !"".equals(base)) {
					boolean ok = false;
					File catalinaBase = new File(base);
					if (catalinaBase.isFile()) {
						LOG.warn("Can't set catalina base to {}. It is an existing file.", base);
					} else if (!catalinaBase.isDirectory()) {
						if (!catalinaBase.mkdirs()) {
							LOG.warn("Can't set catalina base to {}. Can't create directory.", base);
						} else {
							ok = true;
						}
					} else {
						ok = true;
					}
					if (ok) {
						((StandardServer) top).setCatalinaBase(new File(base));
					}
				}
			}
			super.begin(namespace, name, attributes);
		}
	}

	/**
	 * Special rule that always uses Pax Web specific protocol handler class name for any Tomcat configuration
	 * that <em>should</em> be replaced by Pax Web protocol handler. It's mostly used to be able to set the "name"
	 * property for a connector.
	 */
	private static class PaxWebConnectorCreateRule extends ConnectorCreateRule {
		PaxWebConnectorCreateRule(Digester digester) {
			this.digester = digester;
		}

		@Override
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			String protocolName = attributes.getValue("protocol");
			if (protocolName == null || "HTTP/1.1".equals(protocolName)
					|| "org.apache.coyote.http11.Http11NioProtocol".equals(protocolName)
					|| "org.apache.coyote.http11.Http11Nio2Protocol".equals(protocolName)) {
				int idx = attributes.getIndex("protocol");
				attributes = new AttributesImpl(attributes);
				((AttributesImpl) attributes).setValue(idx, "org.ops4j.pax.web.service.tomcat.internal.PaxWebHttp11Nio2Protocol");
			}

			// we need different connector class, so we don't call super.begin()

			Service svc = (Service) digester.peek();
			Executor ex = null;
			String executorName = attributes.getValue("executor");
			if (executorName != null ) {
				ex = svc.getExecutor(executorName);
			}
			/*String */protocolName = attributes.getValue("protocol");
			PaxWebConnector con = new PaxWebConnector(protocolName);
			if (ex != null) {
				con.getProtocolHandler().setExecutor(ex);
//				setExecutor(con, ex);
			}
			String sslImplementationName = attributes.getValue("sslImplementationName");
			if (sslImplementationName != null) {
				((PaxWebHttp11Nio2Protocol) con.getProtocolHandler()).setSslImplementationName(sslImplementationName);
//				setSSLImplementationName(con, sslImplementationName);
			}
			digester.push(con);

			// do NOT call super.begin()
//			super.begin(namespace, name, attributes);
		}
	}

	/**
	 * Special rule that sets connector's name for the purpose of vhost/connector matching.
	 */
	private static class PaxWebConnectorSetName extends Rule {
		private final ServerConfiguration serverConfiguration;

		PaxWebConnectorSetName(Digester digester, ServerConfiguration server) {
			this.digester = digester;
			this.serverConfiguration = server;
		}

		@Override
		public void begin(String namespace, String name, Attributes attributes) throws Exception {
			Object connector = digester.peek();
			if (connector instanceof Connector) {
				String connectorName = attributes.getValue("name");
				if (connectorName == null || "".equals(connectorName.trim())) {
					connectorName = ((Connector) connector).getSecure() ? serverConfiguration.getHttpSecureConnectorName()
							: serverConfiguration.getHttpConnectorName();
				}
				((PaxWebHttp11Nio2Protocol) ((Connector) connector).getProtocolHandler()).setPaxWebConnectorName(connectorName);
			}
		}
	}

}
