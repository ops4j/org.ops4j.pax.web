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

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyFactoryImpl implements JettyFactory {

	private Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Associated server model.
	 */
	private final ServerModel serverModel;
	private Bundle bundle;

	private List<Connector> connectors;

	private List<Handler> handlers;

	/**
	 * Constrcutor.
	 * 
	 * @param serverModel
	 *            asscociated server model
	 * @param bundle
	 */
	JettyFactoryImpl(final ServerModel serverModel, Bundle bundle) {
		this(serverModel, bundle, null, null);
	}

	/**
	 * Constrcutor.
	 * 
	 * @param serverModel
	 *            asscociated server model
	 * @param bundle
	 */
	JettyFactoryImpl(final ServerModel serverModel, Bundle bundle, List<Handler> handlers, List<Connector> connectors) {
		NullArgumentException.validateNotNull(serverModel, "Service model");
		this.serverModel = serverModel;
		this.bundle = bundle;
		this.handlers = handlers;
		this.connectors = connectors;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JettyServer createServer(Integer maxThreads, Integer minThreads, Integer threadIdleTimeout) {
		ThreadPool threadPool;
		if (maxThreads != null && minThreads != null && threadIdleTimeout != null) {
			threadPool = new QueuedThreadPool(maxThreads, minThreads, threadIdleTimeout);
		} else if (maxThreads != null && minThreads != null) {
			threadPool = new QueuedThreadPool(maxThreads, minThreads);
		} else if (maxThreads != null) {
			threadPool = new QueuedThreadPool(maxThreads);
		} else {
			threadPool = new QueuedThreadPool();
		}
		return new JettyServerImpl(serverModel, bundle, handlers, connectors, threadPool);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createConnector(final Server server, final String name, final int port, int securePort, final String host,
			final Boolean checkForwaredHeaders) {

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
		httpConfig.setSecurePort(securePort != 0 ? securePort : 8443);
		httpConfig.setOutputBufferSize(32768);
		if (checkForwaredHeaders) {
			httpConfig.addCustomizer(new ForwardedRequestCustomizer());
		}
		
		ConnectionFactory factory = null;
		
		if (alpnCLassesAvailable()) {
			log.info("HTTP/2 available, adding HTTP/2 to connector");
			// SPDY connector
//			ServerConnector spdy;
			try {
//				Class<?> loadClass = bundle.loadClass("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnector");
//				Constructor<?>[] constructors = loadClass.getConstructors();
//
//				for (Constructor<?> constructor : constructors) {
//					Class<?>[] parameterTypes = constructor.getParameterTypes();
//					if (parameterTypes.length == 1 && parameterTypes[0].equals(Server.class)) {
//						spdy = (ServerConnector) constructor.newInstance(server);
//
//						spdy.setPort(port);
//						spdy.setName(name);
//						spdy.setHost(host);
//						spdy.setIdleTimeout(500000);
//
//						return spdy;
//					}
//				}
//
				Class<?> loadClass = bundle.loadClass("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory");
				factory = (ConnectionFactory) ConstructorUtils.invokeConstructor(loadClass, httpConfig);
				
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			log.info("HTTP/2 not available, creating standard ServerConnector for Http");
			factory = new HttpConnectionFactory(httpConfig);
		}

		// HTTP connector
		ServerConnector http = new ServerConnector(server);
		http.addConnectionFactory(factory);
		http.setPort(port);
		http.setHost(host);
		http.setName(name);
		http.setIdleTimeout(30000);

		return http;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createSecureConnector(Server server, String name, int port,
			String sslKeystore, String sslPassword, String sslKeyPassword,
			String host, String sslKeystoreType, String sslKeyAlias,
			String trustStore, String trustStorePassword, String trustStoreType,
			boolean isClientAuthNeeded, boolean isClientAuthWanted,
			List<String> cipherSuitesIncluded, List<String> cipherSuitesExcluded,
			List<String> protocolsIncluded, List<String> protocolsExcluded) {

		// SSL Context Factory for HTTPS and SPDY
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(sslKeystore);
		sslContextFactory.setKeyStorePassword(sslPassword);
		sslContextFactory.setKeyManagerPassword(sslKeyPassword);
		sslContextFactory.setNeedClientAuth(isClientAuthNeeded);
		sslContextFactory.setWantClientAuth(isClientAuthWanted);
		if (sslKeystoreType != null) {
			sslContextFactory.setKeyStoreType(sslKeystoreType);
		}
		// Java key stores may contain more than one private key entry.
		// Specifying the alias tells jetty which one to use.
		if ( (null != sslKeyAlias) && (!"".equals(sslKeyAlias)) ) {
			sslContextFactory.setCertAlias(sslKeyAlias);
		}

		// Quite often it is useful to use a certificate trust store other than the JVM default.
		if ( (null != trustStore) && (!"".equals(trustStore)) ) {
			sslContextFactory.setTrustStorePath(trustStore);
		}
		if ( (null != trustStorePassword) && (!"".equals(trustStorePassword))) {
			sslContextFactory.setTrustStorePassword(trustStorePassword);
		}
		if ( (null != trustStoreType) && (!"".equals(trustStoreType)) ) {
			sslContextFactory.setTrustStoreType(trustStoreType);
		}

		// In light of well-known attacks against weak encryption algorithms such as RC4,
		// it is usefull to be able to include or exclude certain ciphersuites.
		// Due to the overwhelming number of cipher suites using regex to specify inclusions
		// and exclusions greatly simplifies configuration.
		final String[] cipherSuites;
		try {
			SSLContext context = SSLContext.getDefault();
			SSLSocketFactory sf = context.getSocketFactory();
			cipherSuites = sf.getSupportedCipherSuites();
		}
		catch (NoSuchAlgorithmException e) {

			throw new RuntimeException("Failed to get supported cipher suites.", e);
		}

		if (cipherSuitesIncluded != null && !cipherSuitesIncluded.isEmpty()) {
			final List<String> cipherSuitesToInclude = new ArrayList<String>();
			for (final String cipherSuite : cipherSuites) {
				for (final String includeRegex : cipherSuitesIncluded) {
					if (cipherSuite.matches(includeRegex)) {
						cipherSuitesToInclude.add(cipherSuite);
					}
				}
			}
			sslContextFactory.setIncludeCipherSuites(cipherSuitesToInclude.toArray(new String[cipherSuitesToInclude.size()]));
		}

		if (cipherSuitesExcluded != null && !cipherSuitesExcluded.isEmpty()) {
			final List<String> cipherSuitesToExclude = new ArrayList<String>();
			for (final String cipherSuite : cipherSuites) {
				for (final String excludeRegex : cipherSuitesExcluded) {
					if (cipherSuite.matches(excludeRegex)) {
						cipherSuitesToExclude.add(cipherSuite);
					}
				}
			}
			sslContextFactory.setExcludeCipherSuites(cipherSuitesToExclude.toArray(new String[cipherSuitesToExclude.size()]));
		}

		// In light of attacks against SSL 3.0 as "POODLE" it is useful to include or exclude
		// SSL/TLS protocols as needed.
		if ( (null != protocolsIncluded) && (!protocolsIncluded.isEmpty()) ) {
			sslContextFactory.setIncludeProtocols(protocolsIncluded.toArray(new String[protocolsIncluded.size()]));
		}
		if ( (null != protocolsExcluded) && (!protocolsExcluded.isEmpty()) ) {
			sslContextFactory.setExcludeProtocols(protocolsExcluded.toArray(new String[protocolsExcluded.size()]));
		}

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
		httpConfig.setSecurePort(port);
		httpConfig.setOutputBufferSize(32768);

		// HTTPS Configuration
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		
		List<AbstractConnectionFactory> connectionFactories = new ArrayList<>();
		
		HttpConnectionFactory httpConFactory = new HttpConnectionFactory(httpsConfig);
		
		SslConnectionFactory sslFactory = null;
		AbstractConnectionFactory http2Factory = null;
		
		NegotiatingServerConnectionFactory alpnFactory = null;

		if (alpnCLassesAvailable()) {
			log.info("HTTP/2 available, creating HttpSpdyServerConnector for Https");
			// SPDY connector
			try {
				Class<?> comparatorClass = bundle.loadClass("org.eclipse.jetty.http2.HTTP2Cipher");
				
				Comparator<String> cipherComparator  = (Comparator<String>) FieldUtils.readDeclaredStaticField(comparatorClass, "COMPARATOR");
				sslContextFactory.setCipherComparator(cipherComparator);
				
				sslFactory = new SslConnectionFactory(sslContextFactory, "h2");
				connectionFactories.add(sslFactory);

				
				//org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
				Class<?> loadClass = bundle.loadClass("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory");
//				
//				//ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("spdy/3", "http/1.1");
//				alpnFactory = (NegotiatingServerConnectionFactory) ConstructorUtils.invokeConstructor(loadClass, (Object) new String[] {"ssl", "http/2", "http/1.1"});
//				alpnFactory.setDefaultProtocol("http/1.1");
//				connectionFactories.add(alpnFactory);
				
				//HTTPSPDYServerConnectionFactory spdy = new HTTPSPDYServerConnectionFactory(SPDY.V3, httpConfig);
//				loadClass = bundle.loadClass("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory");
//				loadClass = bundle.loadClass("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory");

				http2Factory = (AbstractConnectionFactory) ConstructorUtils.invokeConstructor(loadClass, httpsConfig);
				connectionFactories.add(http2Factory);
				
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			log.info("SPDY not available, creating standard ServerConnector for Https");
			sslFactory = new SslConnectionFactory(sslContextFactory, "http/1.1");
		}

//		HttpConnectionFactory httpFactory = new HttpConnectionFactory(httpConfig);
//		
//		ServerConnector https = new ServerConnector(server); 
		
		 // HTTPS connector
		ServerConnector https = new ServerConnector(server,
				sslFactory,
				httpConFactory);
		for (AbstractConnectionFactory factory : connectionFactories) {
			https.addConnectionFactory(factory);
		}
		
		https.setPort(port);
		https.setName(name);
		https.setHost(host);
		https.setIdleTimeout(500000);

		/*
		 
		SslContextFactory sslContextFactory = new SslContextFactory();
		HttpConfiguration httpConfig = new HttpConfiguration();
		
		SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, "alpn");
		ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("spdy/3", "http/1.1");
		alpn.setDefaultProtocol("http/1.1");
		HTTPSPDYServerConnectionFactory spdy = new HTTPSPDYServerConnectionFactory(SPDY.V3, httpConfig);
		HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);
		
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, new ConnectionFactory[]{ssl, alpn, spdy, http});
		 
		 */
		
		
		return https;
		
		
	}

	private boolean alpnCLassesAvailable() {
		
		try {
			bundle.loadClass("org.eclipse.jetty.alpn.ALPN");
			
		} catch (ClassNotFoundException e) {
			log.info("No ALPN class available");
			return false;
		}

		try {
			bundle.loadClass("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory");
		} catch (ClassNotFoundException e) {
			log.info("No HTTP2ServerConnectionFactory class available");
			return false;
		}
		
		try {
			bundle.loadClass("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory");
		} catch (ClassNotFoundException e) {
			log.info("No ALPNServerConnectionFactory class available");
			return false;
		}
		
//		try {
//			bundle.loadClass("org.eclipse.jetty.spdy.server.proxy.ProxyHTTPConnectionFactory");
//		} catch (ClassNotFoundException e) {
//			log.info("No ProxyHTTPConnectionFactory found");
//			return false;
//		}
		
		
		return true;
	}

}
