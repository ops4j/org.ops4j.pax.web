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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.eclipse.jetty.server.AbstractConnectionFactory;
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
	public JettyServer createServer() {
		return new JettyServerImpl(serverModel, bundle, handlers, connectors);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createConnector(final Server server, final String name, final int port, int securePort, final String host,
			final Boolean checkForwaredHeaders) {

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(securePort != 0 ? securePort : 8443);
		httpConfig.setOutputBufferSize(32768);
		if (checkForwaredHeaders) {
			httpConfig.addCustomizer(new ForwardedRequestCustomizer());
		}
		
		/*
		if (spdyCLassesAvailable()) {
			log.info("SPDY available, creating HttpSpdyServerConnector for Http");
			// SPDY connector
			ServerConnector spdy;
			try {
				Class<?> loadClass = bundle.loadClass("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnector");
				Constructor<?>[] constructors = loadClass.getConstructors();

				for (Constructor<?> constructor : constructors) {
					Class<?>[] parameterTypes = constructor.getParameterTypes();
					if (parameterTypes.length == 1 && parameterTypes[0].equals(Server.class)) {
						spdy = (ServerConnector) constructor.newInstance(server);

						spdy.setPort(port);
						spdy.setName(name);
						spdy.setHost(host);
						spdy.setIdleTimeout(500000);

						return spdy;
					}
				}

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
			}
		}
		*/

		log.info("SPDY not available, creating standard ServerConnector for Http");

		// HTTP connector
		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
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
	public ServerConnector createSecureConnector(Server server, final String name, final int port,
			final String sslKeystore, final String sslPassword, final String sslKeyPassword, final String host,
			final String sslKeystoreType, final boolean isClientAuthNeeded, final boolean isClientAuthWanted) {

		// SSL Context Factory for HTTPS and SPDY
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(sslKeystore);
		sslContextFactory.setKeyStorePassword(sslKeyPassword);
		sslContextFactory.setKeyManagerPassword(sslPassword);
		sslContextFactory.setNeedClientAuth(isClientAuthNeeded);
		sslContextFactory.setWantClientAuth(isClientAuthWanted);
		if (sslKeystoreType != null) {
			sslContextFactory.setKeyStoreType(sslKeystoreType);
		}

		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(port);
		httpConfig.setOutputBufferSize(32768);

		// HTTPS Configuration
		HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());

		
		List<AbstractConnectionFactory> connectionFactories = new ArrayList<>();
		
		HttpConnectionFactory httpConFactory = new HttpConnectionFactory(httpsConfig);
		
		SslConnectionFactory sslFactory = null;
		AbstractConnectionFactory spdyFactory = null;
		
		NegotiatingServerConnectionFactory alpnFactory = null;

		if (spdyCLassesAvailable()) {
			log.info("SPDY available, creating HttpSpdyServerConnector for Https");
			// SPDY connector
			sslFactory = new SslConnectionFactory(sslContextFactory, "alpn");
			connectionFactories.add(sslFactory);
			try {
				
				Class<?> loadClass = bundle.loadClass("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory");
				
				//ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("spdy/3", "http/1.1");
				alpnFactory = (NegotiatingServerConnectionFactory) ConstructorUtils.invokeConstructor(loadClass, (Object) new String[] {"spdy/3", "http/1.1"});
				alpnFactory.setDefaultProtocol("http/1.1");
				connectionFactories.add(alpnFactory);
				
				//HTTPSPDYServerConnectionFactory spdy = new HTTPSPDYServerConnectionFactory(SPDY.V3, httpConfig);
				loadClass = bundle.loadClass("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory");

				spdyFactory = (AbstractConnectionFactory) ConstructorUtils.invokeConstructor(loadClass, 3, httpsConfig);
				connectionFactories.add(spdyFactory);
				
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

	private boolean spdyCLassesAvailable() {
//		return false;
		
		try {
			// bundle.loadClass("org.eclipse.jetty.alpn.ALPN");
			
			ConstructorUtils.invokeConstructor(bundle.loadClass("org.eclipse.jetty.alpn.ALPN"), (Object) new String[] {"spdy/3", "http/1.1"});
			
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
			log.info("No ALPN class available");
			return false;
		}

		try {
			bundle.loadClass("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnectionFactory");
		} catch (ClassNotFoundException e) {
			log.info("No HTTPSPDYServerConnector class available");
			return false;
		}
		
		try {
			bundle.loadClass("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory");
		} catch (ClassNotFoundException e) {
			log.info("No ALPNServerConnectionFactory class available");
			return false;
		}
		
		try {
			bundle.loadClass("org.eclipse.jetty.spdy.server.proxy.ProxyHTTPConnectionFactory");
		} catch (ClassNotFoundException e) {
			log.info("No ProxyHTTPConnectionFactory found");
			return false;
		}
		
		
		return true;
	}

}
