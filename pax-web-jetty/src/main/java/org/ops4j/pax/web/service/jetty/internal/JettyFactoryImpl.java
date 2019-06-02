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

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static org.ops4j.util.xml.ElementHelper.*;


class JettyFactoryImpl implements JettyFactory {

	/**
	 * Associated server model.
	 */
	private final ServerModel serverModel;
	private Logger log = LoggerFactory.getLogger(getClass());
	private Bundle bundle;
	private Comparator<?> priorityComparator;

	/**
	 * Constrcutor.
	 *
	 * @param serverModel asscociated server model
	 * @param bundle
	 */
	JettyFactoryImpl(final ServerModel serverModel, Bundle bundle, Comparator<?> priorityComparator) {
		NullArgumentException.validateNotNull(serverModel, "Service model");
		this.serverModel = serverModel;
		this.bundle = bundle;
		this.priorityComparator = priorityComparator;
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
		return new JettyServerImpl(serverModel, bundle, priorityComparator, threadPool);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ServerConnector createConnector(final Server server, final String name, final int port, int securePort, final String host,
										   final Boolean checkForwaredHeaders) {

        HttpConfiguration httpConfig = getHttpConfiguration(securePort, checkForwaredHeaders, server);


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

			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | InvocationTargetException e) {
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
            if (priorityComparator != null) {
            	@SuppressWarnings("unchecked")
				Comparator<Customizer> comparator = (Comparator<Customizer>) priorityComparator;
            	List<Customizer> customizers = httpConfig.getCustomizers();
            	Collections.sort(customizers, comparator);
            }
        }
        return httpConfig;
    }

    private HttpConfiguration parseAndConfigureHttpConfig(URL jettyResource) throws IOException, ParserConfigurationException, SAXException {
        InputStream inputStream = jettyResource.openStream();

        Element rootElement = getRootElement(inputStream);
        Element[] news = getChildren(rootElement, "New");
        final List<Element> httpConfigElements = Arrays.stream(news)
                .filter(element -> element.hasAttribute("class"))
                .filter(element -> getAttribute(element, "class")
                        .equalsIgnoreCase("org.eclipse.jetty.server.HttpConfiguration"))
                .collect(Collectors.toList());

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

        Arrays.stream(children).filter(element -> element.getTagName().equalsIgnoreCase("Set")).forEach(element -> {
            String name = getAttribute(element, "name");
            String value = getValue(element);
            if (element.hasChildNodes()) {
                Element property = getChild(element, "Property");
                if (property != null)
                    value = property.getAttribute("default");
            }
            confProps.put(name,value);
        });

        Map<String, Method> methods = new HashMap<>();

        Arrays.stream(HttpConfiguration.class.getMethods()).forEach(method -> {
            methods.put(method.getName(), method);
        });

        HttpConfiguration finalHttpConfig = httpConfig;
        confProps.entrySet().stream().forEach(entry -> {
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
        });

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

/*
    private boolean findClassAttribute(XMLStreamReader streamReader) {
        int attributeCount = streamReader.getAttributeCount();
        boolean foundClass = false;
        for (int i = 0; i < attributeCount; i++) {
            QName attributeName = streamReader.getAttributeName(i);
            if (attributeName.getLocalPart().equalsIgnoreCase("class")) {
                String attributeValue = streamReader.getAttributeValue(i);
                if (attributeValue.equalsIgnoreCase("org.eclipse.jetty.server.HttpConfiguration"))
                    return true;
            }
        }

        return false;
    }
*/
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
												 Boolean sslRenegotiationAllowed,
												 String crlPath,
												 Boolean enableCRLDP,
												 Boolean validateCerts,
												 Boolean validatePeerCerts,
												 Boolean enableOCSP,
												 String ocspResponderURL,
												 Boolean checkForwaredHeaders) {

		// SSL Context Factory for HTTPS and SPDY
		SslContextFactory sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setKeyStorePath(sslKeystore);
		sslContextFactory.setKeyStorePassword(sslKeystorePassword);
		sslContextFactory.setKeyManagerPassword(sslKeyPassword);
		sslContextFactory.setNeedClientAuth(isClientAuthNeeded);
		sslContextFactory.setWantClientAuth(isClientAuthWanted);
		sslContextFactory.setEnableCRLDP(enableCRLDP);
		sslContextFactory.setValidateCerts(validateCerts);
		sslContextFactory.setValidatePeerCerts(validatePeerCerts);
		sslContextFactory.setEnableOCSP(enableOCSP);
		if ((null != crlPath) && (!"".equals(crlPath))) {
                    sslContextFactory.setCrlPath(crlPath);
                }
		if ((null != ocspResponderURL) && (!"".equals(ocspResponderURL))) {
                    sslContextFactory.setOcspResponderURL(ocspResponderURL);
                }
		if (sslKeystoreType != null) {
			sslContextFactory.setKeyStoreType(sslKeystoreType);
		}
		// Java key stores may contain more than one private key entry.
		// Specifying the alias tells jetty which one to use.
		if ((null != sslKeyAlias) && (!"".equals(sslKeyAlias))) {
			sslContextFactory.setCertAlias(sslKeyAlias);
		}

		// Quite often it is useful to use a certificate trust store other than the JVM default.
		if ((null != trustStore) && (!"".equals(trustStore))) {
			sslContextFactory.setTrustStorePath(trustStore);
		}
		if ((null != trustStorePassword) && (!"".equals(trustStorePassword))) {
			sslContextFactory.setTrustStorePassword(trustStorePassword);
		}
		if ((null != trustStoreType) && (!"".equals(trustStoreType))) {
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
		} catch (NoSuchAlgorithmException e) {

			throw new RuntimeException("Failed to get supported cipher suites.", e);
		}

		if (cipherSuitesIncluded != null && !cipherSuitesIncluded.isEmpty()) {
			final List<String> cipherSuitesToInclude = new ArrayList<>();
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
			final List<String> cipherSuitesToExclude = new ArrayList<>();
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
		if ((null != protocolsIncluded) && (!protocolsIncluded.isEmpty())) {
			sslContextFactory.setIncludeProtocols(protocolsIncluded.toArray(new String[protocolsIncluded.size()]));
		}
		if ((null != protocolsExcluded) && (!protocolsExcluded.isEmpty())) {
			sslContextFactory.setExcludeProtocols(protocolsExcluded.toArray(new String[protocolsExcluded.size()]));
		}
		if (sslRenegotiationAllowed != null) {
			sslContextFactory.setRenegotiationAllowed(sslRenegotiationAllowed);
		}

		// HTTP Configuration
        HttpConfiguration httpConfig = getHttpConfiguration(port, checkForwaredHeaders, server);

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

				Comparator<String> cipherComparator = (Comparator<String>) FieldUtils.readDeclaredStaticField(comparatorClass, "COMPARATOR");
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

			} catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException | IllegalAccessException | InstantiationException e) {
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
