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

import static org.ops4j.util.xml.ElementHelper.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.eclipse.jetty.http.HttpScheme;
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
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

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
	    HttpConfiguration httpConfig = getHttpConfiguration(securePort, checkForwaredHeaders, server);

		log.info("SPDY not available, creating standard ServerConnector for Http");

		// HTTP connector
		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		http.setPort(port);
		http.setHost(host);
		http.setName(name);
		http.setIdleTimeout(30000);

		return http;
	}

	private HttpConfiguration getHttpConfiguration(int securePort, Boolean checkForwaredHeaders, Server server) {

        File serverConfigDir = ((JettyServerWrapper) server).getServerConfigDir();
        URL jettyResource = ((JettyServerWrapper) server).getServerConfigURL();

        // HTTP Configuration
        HttpConfiguration httpConfig = new HttpConfiguration();

        if (jettyResource == null) {
            jettyResource = getClass().getResource("/jetty.xml");
        }
        try {
            if (jettyResource == null && serverConfigDir != null) {
                if (!serverConfigDir.isDirectory()
                        && serverConfigDir.canRead()) {
                    String fileName = serverConfigDir.getName();
                    if (fileName.equalsIgnoreCase("jetty.xml")) {
                        jettyResource = serverConfigDir.toURI().toURL();
                    } else {
                        jettyResource = serverConfigDir.toURI().toURL();
                    }
                }
            }

            if (jettyResource != null) {
                ClassLoader loader = Thread.currentThread()
                        .getContextClassLoader();
                try {
                    httpConfig = parseAndConfigureHttpConfig(jettyResource);
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    log.error("Can't parse jetty.xml for HttpConfiguration!", e);
                }
            }

        } catch (MalformedURLException e) {
            log.error("URI to configure HttpConfiguration via jetty.xml is malformed", e);
        }

        if (httpConfig.getSecureScheme() == null)
            httpConfig.setSecureScheme(HttpScheme.HTTPS.asString());
        if (httpConfig.getSecurePort() == 0)
            httpConfig.setSecurePort(securePort != 0 ? securePort : 8443);
        if (httpConfig.getOutputBufferSize() == 0)
            httpConfig.setOutputBufferSize(32768);

        if (checkForwaredHeaders != null && checkForwaredHeaders) {
            httpConfig.addCustomizer(new ForwardedRequestCustomizer());
        }
        return httpConfig;
    }

    private HttpConfiguration parseAndConfigureHttpConfig(URL jettyResource) throws IOException, ParserConfigurationException, SAXException {
        InputStream inputStream = jettyResource.openStream();

        Element rootElement = getRootElement(inputStream);
        Element[] news = getChildren(rootElement, "New");
        final List<Element> httpConfigElements = new LinkedList<>();
        for (Element element : news) {
            if (element.hasAttribute("class") && getAttribute(element, "class").equalsIgnoreCase("org.eclipse.jetty.server.HttpConfiguration")) {
                httpConfigElements.add(element);
            }
        }

        if (httpConfigElements.size() < 1) {
            log.warn("No HttpConfig Element found in jetty.xml, using default");
            return new HttpConfiguration();
        }

        if (httpConfigElements.size() > 1) {
            log.warn("To many HttpConfig elements found, will use default!");
            return new HttpConfiguration();
        }

        HttpConfiguration httpConfig = new HttpConfiguration();

        final Element httpConfigElement = httpConfigElements.get(0);
        Element[] children = getChildren(httpConfigElement);

        Map<String, String> confProps = new HashMap<>();

        for (Element element : children) {
            if (element.getTagName().equalsIgnoreCase("Set")) {
                String name = getAttribute(element, "name");
                String value = getValue(element);
                if (element.hasChildNodes()) {
                    Element property = getChild(element, "Property");
                    if (property != null)
                        value = property.getAttribute("default");
                }
                confProps.put(name,value);
            }
        }

        Map<String, Method> methods = new HashMap<>();

        for (Method method : HttpConfiguration.class.getMethods()) {
            methods.put(method.getName(), method);
        }

        HttpConfiguration finalHttpConfig = httpConfig;
        for (Map.Entry<String, String> entry : confProps.entrySet()) {
            String key = entry.getKey();
            key = key.substring(0,1).toUpperCase().concat(key.substring(1));
            String name = "set".concat(key);
            Method method = methods.get(name);
            Class<?> parameterType = method.getParameterTypes()[0];
            try {
                Object o = toObject(parameterType, entry.getValue());
                method.invoke(finalHttpConfig, o);
            } catch (IllegalAccessException | NumberFormatException | InvocationTargetException e) {
                log.error("HttpConfiguration failed to set variable {} with method {}", entry.getValue(), name);
            }
        }

        if (finalHttpConfig == null) {
            log.warn("HttpConfiguration is null ... even though it should be initialized!!!");
            httpConfig = new HttpConfiguration();
        } else {
            httpConfig = finalHttpConfig;
        }

        return httpConfig;
    }

	private static Object toObject( Class clazz, String value ) {
        if( Boolean.class == clazz || Boolean.TYPE == clazz ) return Boolean.parseBoolean( value );
        if( Byte.class == clazz || Byte.TYPE == clazz ) return Byte.parseByte( value );
        if( Short.class == clazz || Short.TYPE == clazz ) return Short.parseShort( value );
        if( Integer.class == clazz || Integer.TYPE == clazz ) return Integer.parseInt( value );
        if( Long.class == clazz || Long.TYPE == clazz ) return Long.parseLong( value );
        if( Float.class == clazz || Float.TYPE == clazz ) return Float.parseFloat( value );
        if( Double.class == clazz || Double.TYPE == clazz ) return Double.parseDouble( value );
        return value;
    }
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createSecureConnector(Server server, String name, int port,
			String sslKeystore, String sslKeystorePassword, String sslKeyPassword,
			String host, String sslKeystoreType, String sslKeyAlias,
			String trustStore, String trustStorePassword, String trustStoreType,
			boolean isClientAuthNeeded, boolean isClientAuthWanted,
			List<String> cipherSuitesIncluded, List<String> cipherSuitesExcluded,
			List<String> protocolsIncluded, List<String> protocolsExcluded,
		 	Boolean sslRenegotiationAllowed) {

		// SSL Context Factory for HTTPS and SPDY
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(sslKeystore);
		sslContextFactory.setKeyStorePassword(sslKeystorePassword);
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
		if (sslRenegotiationAllowed != null) {
			sslContextFactory.setRenegotiationAllowed(sslRenegotiationAllowed);
		}

		// HTTP Configuration
        HttpConfiguration httpConfig = getHttpConfiguration(port, null, server);

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
