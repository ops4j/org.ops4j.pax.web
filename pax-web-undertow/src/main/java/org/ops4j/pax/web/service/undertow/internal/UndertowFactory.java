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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.DisallowedMethodsHandler;
import io.undertow.server.handlers.PeerNameResolvingHandler;
import io.undertow.server.handlers.ProxyPeerAddressHandler;
import io.undertow.server.handlers.SSLHeaderHandler;
import io.undertow.server.protocol.http.AlpnOpenListener;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.server.protocol.http2.Http2OpenListener;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;
import io.undertow.server.protocol.proxy.ProxyProtocolOpenListener;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.FilterMappingInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.MarkSecureHandler;
import io.undertow.util.HttpString;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.undertow.configuration.model.IoSubsystem;
import org.ops4j.pax.web.service.undertow.configuration.model.SecurityRealm;
import org.ops4j.pax.web.service.undertow.configuration.model.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioProvider;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;
import org.xnio.ssl.SslConnection;

public class UndertowFactory {

	public static final Option<String> PAX_WEB_CONNECTOR_NAME = Option.simple(UndertowFactory.class, "PAX_WEB_CONNECTOR_NAME", String.class);

	private static final Logger LOG = LoggerFactory.getLogger(UndertowFactory.class);

	private final ClassLoader classLoader;

	private final Xnio xnio;

	private boolean http2Available;

	private long maxMemory;

	private OptionMap commonSocketOptions;

	private XnioWorker defaultWorker;
	private ByteBufferPool defaultBufferPool;

	UndertowFactory(ClassLoader classLoader, XnioProvider xnioProvider) {
		this.classLoader = classLoader;
		this.xnio = xnioProvider.getInstance();

		discovery();
	}

	/**
	 * Performs environmental discovery to check if we have some classes available on {@code CLASSPATH}.
	 */
	private void discovery() {
		maxMemory = Runtime.getRuntime().maxMemory();

		// see org.wildfly.extension.undertow.ListenerService#commonOptions
		this.commonSocketOptions = OptionMap.builder()
				.set(Options.TCP_NODELAY, true)
				// org.xnio.nio.NioXnioWorker.createTcpConnectionServer()
				//  - java.net.ServerSocket.setReuseAddress
				.set(Options.REUSE_ADDRESSES, true)
				.set(Options.BALANCING_TOKENS, 1)
				.set(Options.BALANCING_CONNECTIONS, 2)
				.getMap();

		IoSubsystem.BufferPool defaultBufferPoolDefinition = new IoSubsystem.BufferPool();
		defaultBufferPoolDefinition.setName("default");
		defaultBufferPool = createBufferPool(defaultBufferPoolDefinition);

		try {
			classLoader.loadClass("io.undertow.server.protocol.http2.Http2UpgradeHandler");
			http2Available = true;
		} catch (ClassNotFoundException e) {
			http2Available = false;
		}
	}

	/**
	 * Create an {@link XnioWorker} using model definition
	 * @param definition
	 * @return
	 */
	public XnioWorker createWorker(IoSubsystem.Worker definition) throws IOException {
		return xnio.createWorker(OptionMap.builder()
				.set(Options.WORKER_NAME, definition.getName())
				.set(Options.THREAD_DAEMON, false)
				.set(Options.STACK_SIZE, definition.getStackSize())
				// I/O threads
				.set(Options.WORKER_IO_THREADS, definition.getIoThreads())
				// task threads
				.set(Options.WORKER_TASK_KEEPALIVE, definition.getTaskKeepalive())
				.set(Options.WORKER_TASK_CORE_THREADS, definition.getTaskCoreThreads())
				// WORKER_TASK_MAX_THREADS is set both as java.util.concurrent.ThreadPoolExecutor.corePoolSize and
				// java.util.concurrent.ThreadPoolExecutor.maximumPoolSize
				.set(Options.WORKER_TASK_MAX_THREADS, definition.getTaskMaxThreads())
				.getMap());
	}

	/**
	 * Create a {@lnk ByteBufferPool} using model definition
	 * @param definition
	 * @return
	 */
	public ByteBufferPool createBufferPool(IoSubsystem.BufferPool definition) {
		// in static block of org.wildfly.extension.undertow.ByteBufferPoolDefinition it defaults to:
		//  - false, when under 64MB of Xmx
		//  - true, when above 64MB of Xmx
		boolean direct = maxMemory >= 64 * 1024 * 1024;

		// in static block of org.wildfly.extension.undertow.ByteBufferPoolDefinition and in
		// io.undertow.Undertow.Builder.Builder it depends on Xmx
		Integer bufferSize = definition.getBufferSize();
		if (bufferSize == null) {
			if (maxMemory >= 128 * 1024 * 1024) {
				// the 20 is to allow some space for protocol headers, see UNDERTOW-1209 and
				// io.undertow.Undertow.Builder.Builder()
				bufferSize = 1024 * 16 - 20;
			} else {
				bufferSize = 1024;
			}
		}

		// org.wildfly.extension.undertow.ByteBufferPoolDefinition#LEAK_DETECTION_PERCENT
		int leakDetectionPercent = 0;

		// org.wildfly.extension.undertow.ByteBufferPoolDefinition#THREAD_LOCAL_CACHE_SIZE defaults to 12
		// in io.undertow.Undertow.start() it defaults to 4
		int threadLocalCacheSize = 12;

		// in org.wildfly.extension.undertow.ByteBufferPoolDefinition.BufferPoolAdd#performRuntime() defaults to -1
		int maxPoolSize = -1;

		return new DefaultByteBufferPool(direct, bufferSize, maxPoolSize, threadLocalCacheSize, leakDetectionPercent);
	}

	public XnioWorker getDefaultWorker(Configuration configuration) {
		if (defaultWorker == null) {
			// default worker if not specified in XML
			IoSubsystem.Worker defaultWorkerDefinition = new IoSubsystem.Worker();
			// this name will be used as org.xnio.XnioWorker.name
			// which will impact names of:
			//  - worker threads: String.format("%s I/O-%d", workerName, worker idx + 1)
			//  - task threads: String.format("%s task-%d", workerName, seq)
			if (configuration.server().getServerThreadNamePrefix() != null) {
				defaultWorkerDefinition.setName(configuration.server().getServerThreadNamePrefix());
			} else {
				defaultWorkerDefinition.setName("XNIO-default");
			}
			if (configuration.server().getServerMaxThreads() != null) {
				defaultWorkerDefinition.setTaskCoreThreads(configuration.server().getServerMaxThreads());
				defaultWorkerDefinition.setTaskMaxThreads(configuration.server().getServerMaxThreads());
			}
			try {
				defaultWorker = createWorker(defaultWorkerDefinition);
			} catch (IOException e) {
				throw new IllegalStateException("Can't create default worker for Undertow: " + e.getMessage(), e);
			}
		}
		return defaultWorker;
	}

	public void closeDefaultPoolAndBuffer() {
		if (defaultWorker != null) {
			defaultWorker.shutdown();
			defaultWorker = null;
		}
		if (defaultBufferPool != null) {
			defaultBufferPool.close();
			IoSubsystem.BufferPool defaultBufferPoolDefinition = new IoSubsystem.BufferPool();
			defaultBufferPoolDefinition.setName("default");
			defaultBufferPool = createBufferPool(defaultBufferPoolDefinition);
		}
	}

	public XnioWorker createLogWorker() throws IOException {
		OptionMap workerOptions = OptionMap.builder()
				.set(Options.WORKER_NAME, "log-xnio")
				.set(Options.WORKER_TASK_CORE_THREADS, 1)
				.set(Options.WORKER_TASK_MAX_THREADS, 1)
				.set(Options.THREAD_DAEMON, true)
				.set(Options.WORKER_IO_THREADS, 1)
				.getMap();

		return xnio.createWorker(workerOptions);
	}

	public ByteBufferPool getDefaultBufferPool() {
		return defaultBufferPool;
	}

	/**
	 * Creates default listener using default definition (which normally is defined in XML)
	 * @param address
	 * @param rootHandler
	 * @param configuration
	 * @return
	 */
	public UndertowFactory.AcceptingChannelWithAddress createDefaultListener(String address, HttpHandler rootHandler,
			Configuration configuration) {
		return createListener(address, rootHandler, configuration, new Server.HttpListener(),
				new InetSocketAddress(address, configuration.server().getHttpPort()));
	}

	/**
	 * Creates default secure listener using default definition (which normally is defined in XML)
	 * @param address
	 * @param rootHandler
	 * @param configuration
	 * @return
	 */
	public UndertowFactory.AcceptingChannelWithAddress createSecureListener(String address, HttpHandler rootHandler,
			Configuration configuration) {
		AcceptingChannelWithAddress listener = createListener(address, rootHandler, configuration, new Server.HttpsListener(),
				new InetSocketAddress(address, configuration.server().getHttpSecurePort()));
		listener.setSecure(true);
		return listener;
	}

	/**
	 * Creates a <em>listener</em> by invoking main
	 * {@link #createListener(Configuration, Server.Listener, HttpHandler, SecurityRealm, XnioWorker, ByteBufferPool, InetSocketAddress)}
	 * method with passed model definitions
	 * @param address
	 * @param rootHandler
	 * @param configuration
	 * @param def
	 * @param listenerAddress
	 * @return
	 */
	private AcceptingChannelWithAddress createListener(String address, HttpHandler rootHandler,
			Configuration configuration, Server.Listener def, InetSocketAddress listenerAddress) {
		AcceptingChannel<? extends StreamConnection> listener;
		try {
			listener = createListener(configuration, def, rootHandler, null, getDefaultWorker(configuration),
					defaultBufferPool, listenerAddress);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		return new AcceptingChannelWithAddress(listener, listenerAddress);
	}

	/**
	 * Create a <em>listener</em> using  model definition
	 * @param config
	 * @param definition
	 * @param rootHandler handler invoked by the listener being created. Real handler registered for the listener
	 *        may be different (wrapped in listener-specific wrappers)
	 * @param realm mandatory realm definition in case of https listener
	 * @param workerForListener
	 * @param bufferPoolForListener
	 * @param listenerAddress
	 * @return
	 */
	public AcceptingChannel<? extends StreamConnection> createListener(Configuration config, Server.Listener definition,
			HttpHandler rootHandler, SecurityRealm realm,
			XnioWorker workerForListener, ByteBufferPool bufferPoolForListener, InetSocketAddress listenerAddress)
			throws IOException {

		OptionMap.Builder listenerOptionsBuilder = OptionMap.builder().addAll(commonSocketOptions);
		prepareListenerOptionsBuilder(listenerOptionsBuilder, definition);
		OptionMap listenerOptions = listenerOptionsBuilder.getMap();

		OptionMap.Builder undertowOptionsBuilder = OptionMap.builder();
		HttpHandler listenerSpecificHandler = prepareUndertowOptionsBuilder(config, undertowOptionsBuilder, definition, rootHandler);
		OptionMap undertowOptions = undertowOptionsBuilder.getMap();

		OpenListener openListener;
		HttpOpenListener httpListener = new HttpOpenListener(bufferPoolForListener, undertowOptions);
		httpListener.setRootHandler(listenerSpecificHandler);
		openListener = httpListener;

		if (definition instanceof Server.HttpsListener) {
			if (http2Available && definition.isEnableHttp2()) {
				AlpnOpenListener alpnListener = new AlpnOpenListener(bufferPoolForListener, undertowOptions, httpListener);
				Http2OpenListener http2Listener = new Http2OpenListener(bufferPoolForListener, undertowOptions);
				http2Listener.setRootHandler(listenerSpecificHandler);
				int weight = 10; // just as in io.undertow.Undertow.start()
				alpnListener.addProtocol(Http2OpenListener.HTTP2, http2Listener, weight);
//				alpnListener.addProtocol(Http2OpenListener.HTTP2_14, http2Listener, 7);
				openListener = alpnListener;
			}
		}

		ChannelListener<StreamConnection> finalListener = openListener;

		// the "server"
		if (definition instanceof Server.HttpListener) {
			Server.HttpListener http = (Server.HttpListener) definition;
			if (http.isProxyProtocol()) {
				finalListener = new ProxyProtocolOpenListener(openListener, null, bufferPoolForListener, OptionMap.EMPTY);
			}
			ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(finalListener);
			return workerForListener.createStreamConnectionServer(listenerAddress, acceptListener, listenerOptions);
		} else if (definition instanceof Server.HttpsListener) {
			Server.HttpsListener https = (Server.HttpsListener) definition;
			OptionMap.Builder sslParametersBuilder = OptionMap.builder();

			// io.undertow.protocols.ssl.UndertowAcceptingSslChannel.UndertowAcceptingSslChannel requires these options:
			//  - org.xnio.Options.SSL_CLIENT_AUTH_MODE
			//  - org.xnio.Options.SSL_ENABLE_SESSION_CREATION (default: true)
			//  - org.xnio.Options.SSL_ENABLED_CIPHER_SUITES
			//  - org.xnio.Options.SSL_ENABLED_PROTOCOLS
			//  - io.undertow.UndertowOptions.SSL_USER_CIPHER_SUITES_ORDER
			sslParametersBuilder.addAll(listenerOptions);
			if (https.getVerifyClient() != null) {
				sslParametersBuilder.set(Options.SSL_CLIENT_AUTH_MODE, https.getVerifyClient());
			}
			if (https.getSslSessionCacheSize() > 0) {
				sslParametersBuilder.set(Options.SSL_ENABLE_SESSION_CREATION, true);
				sslParametersBuilder.set(Options.SSL_CLIENT_SESSION_CACHE_SIZE, https.getSslSessionCacheSize());
				sslParametersBuilder.set(Options.SSL_SERVER_SESSION_CACHE_SIZE, https.getSslSessionCacheSize());
			}
			sslParametersBuilder.set(Options.SSL_CLIENT_SESSION_TIMEOUT, https.getSslSessionTimeout());
			sslParametersBuilder.set(Options.SSL_SERVER_SESSION_TIMEOUT, https.getSslSessionTimeout());

			SecurityRealm.Engine engine = null;
			if (realm != null && realm.getIdentities() != null && realm.getIdentities().getSsl() != null
					&& realm.getIdentities().getSsl().getEngine() != null) {
				engine = realm.getIdentities().getSsl().getEngine();
			}

			List<String> enabledCipherSuites = https.getEnabledCipherSuites();
			if (enabledCipherSuites.size() > 0 && engine != null && engine.getEnabledCipherSuites().size() > 0) {
				LOG.warn("Enabled cipher suites specified both for https-listener and ssl/engine." +
						" Cipher suites from the https-listener will be used.");
			}
			if (enabledCipherSuites.size() == 0 && engine != null) {
				enabledCipherSuites = engine.getEnabledCipherSuites();
			}
			if (enabledCipherSuites.size() == 0 && config.security().getCiphersuiteIncluded().length > 0) {
				enabledCipherSuites = Arrays.asList(config.security().getCiphersuiteIncluded());
			}
			if (enabledCipherSuites.size() > 0) {
				sslParametersBuilder.set(Options.SSL_ENABLED_CIPHER_SUITES, Sequence.of(enabledCipherSuites));
			}

			List<String> enabledProtocols = https.getEnabledProtocols();
			if (enabledProtocols.size() > 0 && engine != null && engine.getEnabledProtocols().size() > 0) {
				LOG.warn("Enabled protocols specified both for https-listener and ssl/engine." +
						" Protocols from the https-listener will be used.");
			}
			if (enabledProtocols.size() == 0 && engine != null) {
				enabledProtocols = engine.getEnabledProtocols();
			}
			if (enabledProtocols.size() == 0 && config.security().getProtocolsIncluded().length > 0) {
				enabledProtocols = Arrays.asList(config.security().getProtocolsIncluded());
			}
			if (enabledProtocols.size() > 0) {
				sslParametersBuilder.set(Options.SSL_ENABLED_PROTOCOLS, Sequence.of(enabledProtocols));
			}

			// javax.net.ssl.SSLParameters.setUseCipherSuitesOrder()
			sslParametersBuilder.set(UndertowOptions.SSL_USER_CIPHER_SUITES_ORDER, true);
			OptionMap sslParameters = sslParametersBuilder.getMap();

			SSLContext sslContext = buildSSLContext(config, https, realm);
			UndertowXnioSsl xnioSsl = new UndertowXnioSsl(xnio, sslParameters, bufferPoolForListener, sslContext);

			if (https.isProxyProtocol()) {
				finalListener = new ProxyProtocolOpenListener(openListener, xnioSsl, bufferPoolForListener, sslParameters);
			}
			ChannelListener<AcceptingChannel<SslConnection>> acceptListener = ChannelListeners.openListenerAdapter(finalListener);
			return xnioSsl.createSslConnectionServer(workerForListener, listenerAddress, acceptListener, sslParameters);
		} else {
			throw new IllegalArgumentException("Can't handle listener definition " + definition);
		}
	}

	/**
	 * Prepares a builder with socket related options
	 * @param listenerOptions
	 * @param listener
	 */
	private void prepareListenerOptionsBuilder(OptionMap.Builder listenerOptions, Server.Listener listener) {
		listenerOptions.set(Options.RECEIVE_BUFFER, listener.getReceiveBuffer());
		listenerOptions.set(Options.SEND_BUFFER, listener.getSendBuffer());
		listenerOptions.set(Options.BACKLOG, listener.getTcpBacklog());
		listenerOptions.set(Options.KEEP_ALIVE, listener.isTcpKeepAlive());
		listenerOptions.set(Options.READ_TIMEOUT, listener.getReadTimeout());
		listenerOptions.set(Options.WRITE_TIMEOUT, listener.getWriteTimeout());
		listenerOptions.set(Options.CONNECTION_HIGH_WATER, listener.getMaxConnections());
		listenerOptions.set(Options.CONNECTION_LOW_WATER, listener.getMaxConnections());
	}

	/**
	 * Prepares a builder with options (mostly from {@link UndertowOptions}) related to listener configuration.
	 *
	 * @param config
	 * @param undertowOptions
	 * @param listener
	 * @param handler
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private HttpHandler prepareUndertowOptionsBuilder(Configuration config, OptionMap.Builder undertowOptions,
			Server.Listener listener, HttpHandler handler) {
		undertowOptions.set(UndertowOptions.BUFFER_PIPELINED_DATA, listener.isBufferPipelinedData());
		undertowOptions.set(UndertowOptions.ENABLE_STATISTICS, false);

		undertowOptions.set(UndertowOptions.MAX_PARAMETERS, listener.getMaxParameters());
		undertowOptions.set(UndertowOptions.MAX_HEADERS, listener.getMaxHeaders());
		undertowOptions.set(UndertowOptions.MAX_COOKIES, listener.getMaxCookies());
		undertowOptions.set(UndertowOptions.MAX_ENTITY_SIZE, listener.getMaxPostSize());
		undertowOptions.set(UndertowOptions.MAX_HEADER_SIZE, listener.getMaxHeaderSize());
		undertowOptions.set(UndertowOptions.MAX_BUFFERED_REQUEST_SIZE, listener.getMaxBufferedRequestSize());

		undertowOptions.set(UndertowOptions.BUFFER_PIPELINED_DATA, listener.isBufferPipelinedData());
		undertowOptions.set(UndertowOptions.DECODE_URL, listener.isDecodeUrl());
		undertowOptions.set(UndertowOptions.ALLOW_ENCODED_SLASH, listener.isAllowEncodedSlash());
		undertowOptions.set(UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE, listener.isAllowEqualsInCookieValue());
		undertowOptions.set(UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, listener.isAllowUnescapedCharactersInUrl());
		undertowOptions.set(UndertowOptions.URL_CHARSET, listener.getUrlCharset());

		undertowOptions.set(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, listener.isAlwaysSetKeepAlive());
//					undertowOptions.set(UndertowOptions.ALWAYS_SET_DATE, ...);

		undertowOptions.set(UndertowOptions.NO_REQUEST_TIMEOUT, listener.getNoRequestTimeout());
		undertowOptions.set(UndertowOptions.REQUEST_PARSE_TIMEOUT, listener.getRequestParseTimeout());

		// PAXWEB-1232
		undertowOptions.set(UndertowOptions.RECORD_REQUEST_START_TIME, listener.isRecordRequestStartTime());

		if (config.server().getConnectorIdleTimeout() != null) {
			undertowOptions.set(UndertowOptions.IDLE_TIMEOUT, config.server().getConnectorIdleTimeout());
		}

		undertowOptions.set(Options.SECURE, listener.isSecure());

		String defaultConnectorName = config.server().getHttpConnectorName();

		if (listener.isSecure()) {
			handler = new MarkSecureHandler(handler);
			defaultConnectorName = config.server().getHttpSecureConnectorName();
		}
		if (listener.isResolvePeerAddress()) {
			// PAXWEB-1236
			handler = new PeerNameResolvingHandler(handler);
		}
		if (listener.isProxyAddressForwarding() || config.server().checkForwardedHeaders()) {
			// PAXWEB-1233
			handler = new ProxyPeerAddressHandler(handler);
		}
		// https://github.com/ops4j/org.ops4j.pax.web/issues/1664
		handler = new RootHttpOptionsHandler(handler, listener.getDisallowedMethods());
		if (listener.getDisallowedMethods().size() > 0) {
			Set<HttpString> disallowedMethods = new HashSet<>();
			listener.getDisallowedMethods().stream().map(HttpString::tryFromString).forEach(disallowedMethods::add);
			handler = new DisallowedMethodsHandler(handler, disallowedMethods);
		}
		if (listener.isCertificateForwarding()) {
			// org.wildfly.extension.undertow.HttpListenerResourceDefinition#CERTIFICATE_FORWARDING
			handler = new SSLHeaderHandler(handler);
		}
		undertowOptions.set(UndertowOptions.ENABLE_RFC6265_COOKIE_VALIDATION, listener.isRfc6265CookieValidation());
		undertowOptions.set(UndertowOptions.REQUIRE_HOST_HTTP11, listener.isRequireHostHttp11());

		if (listener.isEnableHttp2()) {
			if (!http2Available) {
				LOG.warn("HTTP2 support configured for the listener, but HTTP2 support classes not available");
			} else {
				if (listener instanceof Server.HttpListener) {
					// h2c support - upgrade from HTTP/1.1 to HTTP/2
					handler = new Http2UpgradeHandler(handler);
				} else {
					// alpn support - upgrade to HTTP/2 using TLS extensions, so no additional wrapper handlers here,
					// it'll be supported via wrapper OpenListeners
				}
				undertowOptions.set(UndertowOptions.ENABLE_HTTP2, true);
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH, listener.isHttp2EnablePush());
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE, listener.getHttp2HeaderTableSize());
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, listener.getHttp2InitialWindowSize());
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS, listener.getHttp2MaxConcurrentStreams());
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_MAX_FRAME_SIZE, listener.getHttp2MaxFrameSize());
				undertowOptions.set(UndertowOptions.HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE, listener.getHttp2MaxHeaderListSize());
			}
		}

		String connectorName = listener.getName();
		if (connectorName == null || "".equals(connectorName.trim())) {
			connectorName = defaultConnectorName;
		}
		undertowOptions.set(PAX_WEB_CONNECTOR_NAME, connectorName);

		return handler;
	}

	/**
	 * Build {@link SSLContext} using provided definition. In undertow this is done using
	 * {@code org.xnio.ssl.JsseSslUtils#createSSLContext()}
	 *
	 * @param config
	 * @param definition
	 * @param realm
	 * @return
	 */
	private SSLContext buildSSLContext(Configuration config, Server.HttpsListener definition, SecurityRealm realm) {
		SecurityConfiguration sec = config.security();

		SecurityRealm.Keystore keystore;
		SecurityRealm.Truststore truststore;

		if (realm == null || realm.getAuthentication() == null || realm.getAuthentication().getTruststore() == null) {
			// fallback configuration from PID
			truststore = new SecurityRealm.Truststore();
			truststore.setPath(sec.getTruststore());
			truststore.setPassword(sec.getTruststorePassword());
			truststore.setType(sec.getTruststoreType());
		} else {
			truststore = realm.getAuthentication().getTruststore();
		}
		if (realm == null || realm.getIdentities() == null || realm.getIdentities().getSsl() == null
				|| realm.getIdentities().getSsl().getKeystore() == null) {
			// fallback configuration from PID
			keystore = new SecurityRealm.Keystore();
			keystore.setPath(sec.getSslKeystore());
			keystore.setPassword(sec.getSslKeystorePassword());
			keystore.setType(sec.getTruststoreType());
			keystore.setKeyPassword(sec.getSslKeyPassword());
			keystore.setAlias(sec.getSslKeyAlias());
		} else {
			keystore = realm.getIdentities().getSsl().getKeystore();
		}

		try {
			URL keystoreURL = loadResource(keystore.getPath());
			String keystoreType = keystore.getType() == null ? "JKS" : keystore.getType();
			String keyAlias = keystore.getAlias();
			KeyStore keyStore = getKeyStore(sec, keystoreURL, keystoreType, keystore.getPassword(), sec.getSslKeystoreProvider());

			if (keyAlias != null) {
				// just as in org.jboss.as.domain.management.security.FileKeystore#load(), we have to
				// create temporary, single key entry keystore
				KeyStore newKeystore = KeyStore.getInstance(keystoreType);
				newKeystore.load(null);

				if (keyStore.containsAlias(keyAlias)) {
					KeyStore.ProtectionParameter password
							= new KeyStore.PasswordProtection(keystore.getKeyPassword().toCharArray());
					if (keyStore.isKeyEntry(keyAlias)) {
						KeyStore.Entry entry = keyStore.getEntry(keyAlias, password);
						newKeystore.setEntry(keyAlias, entry, password);
						keyStore = newKeystore;
					} else {
						throw new IllegalArgumentException("Entry \"" + keyAlias + "\" is not private key entry"
								+ " in keystore " + keystore.getPath());
					}
				} else {
					throw new IllegalArgumentException("Entry \"" + keyAlias + "\" not found in keystore " + keystore.getPath());
				}
			}

			// key managers
			String keyManagerFactoryAlgorithm = sec.getSslKeyManagerFactoryAlgorithm();
			if (keyManagerFactoryAlgorithm == null) {
				keyManagerFactoryAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
			}
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm);
			keyManagerFactory.init(keyStore, keystore.getKeyPassword() == null ? null : keystore.getKeyPassword().toCharArray());
			KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

			// trust managers - possibly with OCSP
			TrustManager[] trustManagers = null;
			SecureRandom random = (sec.getSecureRandomAlgorithm() == null) ? null
					: SecureRandom.getInstance(sec.getSecureRandomAlgorithm());
			if (truststore.getPath() != null) {
				URL truststoreURL = loadResource(truststore.getPath());
				String truststoreType = truststore.getType() == null ? "JKS" : truststore.getType();
				KeyStore trustStore = getKeyStore(sec, truststoreURL, truststoreType, truststore.getPassword(), sec.getTruststoreProvider());

				String trustManagerFactoryAlgorithm = sec.getTrustManagerFactoryAlgorithm();
				if (trustManagerFactoryAlgorithm == null) {
					trustManagerFactoryAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
				}
				Collection<? extends CRL> crls = sec.getCrlPath() == null ? null : loadCRL(sec.getCrlPath());

				if (sec.isValidateCerts()) {
					String keystoreCertAlias = keystore.getAlias();
					if (keystoreCertAlias == null) {
						List<String> aliases = Collections.list(keyStore.aliases());
						keystoreCertAlias = aliases.size() == 1 ? aliases.get(0) : null;
					}

					Certificate cert = keystoreCertAlias == null ? null : keyStore.getCertificate(keystoreCertAlias);
					if (cert == null) {
						throw new IllegalArgumentException("No certificate found in the keystore" + (keystoreCertAlias == null ? "" : " for alias \"" + keystoreCertAlias + "\""));
					}

					CertificateValidator validator = new CertificateValidator(trustStore, crls);
					validator.setEnableCRLDP(sec.isEnableCRLDP());
					validator.setEnableOCSP(sec.isEnableOCSP());
					validator.setOcspResponderURL(sec.getOcspResponderURL());
					validator.validate(keyStore, cert);
				}

				// Revocation checking is only supported for PKIX algorithm
				// see org.eclipse.jetty.util.ssl.SslContextFactory.getTrustManagers()
				if (sec.isValidatePeerCerts() && trustManagerFactoryAlgorithm.equalsIgnoreCase("PKIX")) {
					PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

					// Make sure revocation checking is enabled
					pbParams.setRevocationEnabled(true);

					if (crls != null && !crls.isEmpty()) {
						pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
					}

					if (sec.isEnableCRLDP()) {
						// Enable Certificate Revocation List Distribution Points (CRLDP) support
						System.setProperty("com.sun.security.enableCRLDP", "true");
					}

					if (sec.isEnableOCSP()) {
						// Enable On-Line Certificate Status Protocol (OCSP) support
						Security.setProperty("ocsp.enable", "true");

						if (sec.getOcspResponderURL() != null) {
							// Override location of OCSP Responder
							Security.setProperty("ocsp.responderURL", sec.getOcspResponderURL());
						}
					}

					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
					trustManagerFactory.init(new CertPathTrustManagerParameters(pbParams));

					trustManagers = trustManagerFactory.getTrustManagers();
				} else {
					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm);
					trustManagerFactory.init(trustStore);

					trustManagers = trustManagerFactory.getTrustManagers();
				}
			}

			SSLContext context;
			if (null == sec.getSslProvider() || sec.getSslProvider().isEmpty()) {
				context = SSLContext.getInstance("TLS");
			} else {
				context = SSLContext.getInstance("TLS", sec.getSslProvider());
			}

			context.init(keyManagers, trustManagers, random);
			context.getClientSessionContext().setSessionCacheSize(definition.getSslSessionCacheSize());
			context.getClientSessionContext().setSessionTimeout(definition.getSslSessionTimeout());
			context.getServerSessionContext().setSessionCacheSize(definition.getSslSessionCacheSize());
			context.getServerSessionContext().setSessionTimeout(definition.getSslSessionTimeout());

			return context;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to build SSL context", e);
		}
	}

	private URL loadResource(String resource) throws MalformedURLException {
		// TODO: https://github.com/ops4j/org.ops4j.pax.web/issues/1592 in master-improvements branch for all runtimes
		if (resource == null || "".equals(resource.trim())) {
			return null;
		}
		URL url;
		try {
			url = new URL(resource);
		} catch (MalformedURLException e) {
			if (!(resource.startsWith("ftp:") || resource.startsWith("file:") || resource.startsWith("jar:"))) {
				try {
					File file = new File(resource).getCanonicalFile();
					url = file.toURI().toURL();
				} catch (Exception e2) {
					throw e;
				}
			} else {
				throw e;
			}
		}
		return url;
	}

	/**
	 * Load key or truststore
	 * @param sec
	 * @param storePath
	 * @param storeType
	 * @param storePassword
	 * @param provider
	 * @return
	 * @throws Exception
	 */
	private KeyStore getKeyStore(SecurityConfiguration sec,
			URL storePath, String storeType, String storePassword, String provider) throws Exception {
		KeyStore keystore;
		if (provider == null) {
			keystore = KeyStore.getInstance(storeType);
		} else {
			keystore = KeyStore.getInstance(storeType, provider);
		}
		if (storePath != null) {
			try (InputStream is = storePath.openStream()) {
				keystore.load(is, storePassword.toCharArray());
			}
		} else {
			keystore.load(null, storePassword.toCharArray());
		}

		return keystore;
	}

	public Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
		Collection<? extends CRL> crlList = null;

		if (crlPath != null) {
			try (InputStream in = loadResource(crlPath).openStream()) {
				crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
			}
		}

		return crlList;
	}

	/**
	 * <p>Special method that has to clone existing {@link DeploymentInfo}, because we can't access original
	 * filter mapping inside. This methods is used when unregistering filters.</p>
	 *
	 * <p>This is almost a copy of original {@link DeploymentInfo#clone()}</p>
	 *
	 * @param existing
	 * @param clearDynamicFilters
	 * @param clearNonDynamicFilters
	 * @return
	 */
	public DeploymentInfo clearFilters(DeploymentInfo existing, boolean clearDynamicFilters,
			boolean clearNonDynamicFilters) {
		final DeploymentInfo info = new DeploymentInfo()
				.setClassLoader(existing.getClassLoader())
				.setContextPath(existing.getContextPath())
				.setResourceManager(existing.getResourceManager())
				.setMajorVersion(existing.getMajorVersion())
				.setMinorVersion(existing.getMinorVersion())
				.setDeploymentName(existing.getDeploymentName())
				.setClassIntrospecter(existing.getClassIntrospecter());

		for (Map.Entry<String, ServletInfo> e : existing.getServlets().entrySet()) {
			info.addServlet(e.getValue().clone());
		}
		// copy needed filters and mappings
		Set<String> names = new HashSet<>();
		for (Map.Entry<String, FilterInfo> e : existing.getFilters().entrySet()) {
			FilterInfo fi = e.getValue();
			if (fi instanceof PaxWebFilterInfo) {
				FilterModel fm = ((PaxWebFilterInfo) fi).getFilterModel();
				if (fm == null || (!clearDynamicFilters && fm.isDynamic()) || (!clearNonDynamicFilters && !fm.isDynamic())) {
					info.addFilter(fi);
					names.add(fi.getName());
				}
			}
		}
		for (FilterMappingInfo fm : existing.getFilterMappings()) {
			if (names.contains(fm.getFilterName())) {
				if (fm.getMappingType() == FilterMappingInfo.MappingType.SERVLET) {
					info.addFilterServletNameMapping(fm.getFilterName(), fm.getMapping(), fm.getDispatcher());
				}
				if (fm.getMappingType() == FilterMappingInfo.MappingType.URL) {
					info.addFilterUrlMapping(fm.getFilterName(), fm.getMapping(), fm.getDispatcher());
				}
			}
		}

		info.setDisplayName(existing.getDisplayName());
		info.getListeners().addAll(existing.getListeners());
		info.getServletContainerInitializers().addAll(existing.getServletContainerInitializers());
		info.getThreadSetupActions().addAll(existing.getThreadSetupActions());
		info.getInitParameters().putAll(existing.getInitParameters());
		info.getServletContextAttributes().putAll(existing.getServletContextAttributes());
		info.getWelcomePages().addAll(existing.getWelcomePages());
		info.getErrorPages().addAll(existing.getErrorPages());
		info.getMimeMappings().addAll(existing.getMimeMappings());
		info.setExecutor(existing.getExecutor());
		info.setAsyncExecutor(existing.getAsyncExecutor());
		info.setTempDir(existing.getTempDir());
		info.setJspConfigDescriptor(existing.getJspConfigDescriptor());
		info.setDefaultServletConfig(existing.getDefaultServletConfig());
		info.getLocaleCharsetMapping().putAll(existing.getLocaleCharsetMapping());
		info.setSessionManagerFactory(existing.getSessionManagerFactory());
		if (existing.getLoginConfig() != null) {
			info.setLoginConfig(existing.getLoginConfig().clone());
		}
		info.setIdentityManager(existing.getIdentityManager());
		info.setConfidentialPortManager(existing.getConfidentialPortManager());
		info.setDefaultEncoding(existing.getDefaultEncoding());
		info.setUrlEncoding(existing.getUrlEncoding());
		info.getSecurityConstraints().addAll(existing.getSecurityConstraints());
		info.getOuterHandlerChainWrappers().addAll(existing.getOuterHandlerChainWrappers());
		info.getInnerHandlerChainWrappers().addAll(existing.getInnerHandlerChainWrappers());
		info.setInitialSecurityWrapper(existing.getInitialSecurityWrapper());
		info.getSecurityWrappers().addAll(existing.getSecurityWrappers());
		info.getInitialHandlerChainWrappers().addAll(existing.getInitialHandlerChainWrappers());
		info.getSecurityRoles().addAll(existing.getSecurityRoles());
		info.getNotificationReceivers().addAll(existing.getNotificationReceivers());
		info.setAllowNonStandardWrappers(existing.isAllowNonStandardWrappers());
		info.setDefaultSessionTimeout(existing.getDefaultSessionTimeout());
		info.setServletContextAttributeBackingMap(existing.getServletContextAttributeBackingMap());
		info.setServletSessionConfig(existing.getServletSessionConfig());
		info.setHostName(existing.getHostName());
		info.setDenyUncoveredHttpMethods(existing.isDenyUncoveredHttpMethods());
		info.setServletStackTraces(existing.getServletStackTraces());
		info.setInvalidateSessionOnLogout(existing.isInvalidateSessionOnLogout());
		info.setDefaultCookieVersion(existing.getDefaultCookieVersion());
		info.setSessionPersistenceManager(existing.getSessionPersistenceManager());
		for (Map.Entry<String, Set<String>> e : existing.getPrincipalVersusRolesMap().entrySet()) {
			info.getPrincipalVersusRolesMap().put(e.getKey(), new HashSet<>(e.getValue()));
		}
		info.setIgnoreFlush(existing.isIgnoreFlush());
		info.setAuthorizationManager(existing.getAuthorizationManager());
		info.getAuthenticationMechanisms().putAll(existing.getAuthenticationMechanisms());
		info.getServletExtensions().addAll(existing.getServletExtensions());
		info.setJaspiAuthenticationMechanism(existing.getJaspiAuthenticationMechanism());
		info.setSecurityContextFactory(existing.getSecurityContextFactory());
		info.setServerName(existing.getServerName());
		info.setMetricsCollector(existing.getMetricsCollector());
		info.setSessionConfigWrapper(existing.getSessionConfigWrapper());
		info.setEagerFilterInit(existing.isEagerFilterInit());
		info.setDisableCachingForSecuredPages(existing.isDisableCachingForSecuredPages());
		info.setExceptionHandler(existing.getExceptionHandler());
		info.setEscapeErrorMessage(existing.isEscapeErrorMessage());
		info.getSessionListeners().addAll(existing.getSessionListeners());
		info.getLifecycleInterceptors().addAll(existing.getLifecycleInterceptors());
		info.setAuthenticationMode(existing.getAuthenticationMode());
		info.setDefaultMultipartConfig(existing.getDefaultMultipartConfig());
		info.setContentTypeCacheSize(existing.getContentTypeCacheSize());
		info.setSessionIdGenerator(existing.getSessionIdGenerator());
		info.setSendCustomReasonPhraseOnError(existing.isSendCustomReasonPhraseOnError());
		info.setChangeSessionIdOnLogin(existing.isChangeSessionIdOnLogin());
		info.setCrawlerSessionManagerConfig(existing.getCrawlerSessionManagerConfig());
		info.setSecurityDisabled(existing.isSecurityDisabled());
		info.setUseCachedAuthenticationMechanism(existing.isUseCachedAuthenticationMechanism());
		info.setCheckOtherSessionManagers(existing.isCheckOtherSessionManagers());
		info.setDefaultRequestEncoding(existing.getDefaultRequestEncoding());
		info.setDefaultResponseEncoding(existing.getDefaultResponseEncoding());
		info.getPreCompressedResources().putAll(existing.getPreCompressedResources());
		info.setContainerMajorVersion(existing.getContainerMajorVersion());
		info.setContainerMinorVersion(existing.getContainerMinorVersion());
		info.getDeploymentCompleteListeners().addAll(existing.getDeploymentCompleteListeners());
		info.setPreservePathOnForward(existing.isPreservePathOnForward());

		return info;
	}

	public static class AcceptingChannelWithAddress {
		private final AcceptingChannel<? extends StreamConnection> acceptingChannel;
		private final InetSocketAddress address;
		private boolean secure;

		public AcceptingChannelWithAddress(AcceptingChannel<? extends StreamConnection> acceptingChannel, InetSocketAddress address) {
			this.acceptingChannel = acceptingChannel;
			this.address = address;
		}

		public AcceptingChannel<? extends StreamConnection> getAcceptingChannel() {
			return acceptingChannel;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		@Override
		public String toString() {
			return "AcceptingChannelWithAddress{acceptingChannel=" + acceptingChannel +
					", address=" + address +
					", secure=" + secure +
					"}";
		}
	}

}
