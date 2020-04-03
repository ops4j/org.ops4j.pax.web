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

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.StatusCodes;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.AcceptingChannel;

import static org.junit.Assert.assertTrue;

public class EmbeddedUndertowTest {

	public static Logger LOG = LoggerFactory.getLogger(EmbeddedUndertowTest.class);

	@Test
	public void undertowWithSingleContextAndServlet() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet.addMapping("/s1/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c1", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
	}

	@Test
	public void undertowUsingLowLevelBuilders() throws Exception {
		PathHandler path = Handlers.path();

		// org.xnio.nio.NioXnioWorker.createTcpConnectionServer handles these options:
		//  - org.xnio.Options.RECEIVE_BUFFER
		//  - org.xnio.Options.REUSE_ADDRESSES
		//  - org.xnio.Options.BACKLOG
		// org.xnio.nio.QueuedNioTcpServer.QueuedNioTcpServer handles these options:
		//  - org.xnio.Options.SEND_BUFFER (org.xnio.nio.AbstractNioChannel.DEFAULT_BUFFER_SIZE)
		//  - org.xnio.Options.KEEP_ALIVE (false)
		//  - org.xnio.Options.TCP_OOB_INLINE (false)
		//  - org.xnio.Options.TCP_NODELAY (false)
		//  - org.xnio.Options.READ_TIMEOUT (0)
		//  - org.xnio.Options.WRITE_TIMEOUT (0)
		//  - org.xnio.Options.CONNECTION_HIGH_WATER (Integer.MAX_VALUE)
		//  - org.xnio.Options.CONNECTION_LOW_WATER (CONNECTION_HIGH_WATER)

		// in Wildfly, org.wildfly.extension.undertow.HttpListenerService#createOpenListener creates
		// io.undertow.server.protocol.http.HttpOpenListener and seems to pass more options than it's possible
		// inside io.undertow.Undertow.start() which doesn't pass as many options as can be defined in XML

		Undertow.ListenerBuilder listenerBuilder = new Undertow.ListenerBuilder()
				.setType(Undertow.ListenerType.HTTP)
				.setHost("0.0.0.0")
				.setPort(0)
				.setRootHandler(exchange -> {
					exchange.setStatusCode(StatusCodes.NOT_FOUND);
					exchange.endExchange();
				})
				// org.xnio.Options
				.setOverrideSocketOptions(OptionMap.builder()
						.set(org.xnio.Options.RECEIVE_BUFFER, 1024)
						.getMap());

		Undertow server = Undertow.builder()
				.addListener(listenerBuilder)
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet.addMapping("/s1/*");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c1", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
	}

	@Test
	public void undertowWithoutUndertow() throws Exception {
		// Wildfly doesn't use io.undertow.Undertow at all, it wraps all required pieces within
		// org.wildfly.extension.undertow.Server
		//
		// here's the structure:
		// org.wildfly.extension.undertow.UndertowService
		//  - defaultContainer (String)
		//  - defaultServer (String)
		//  - defaultVirtualHost (String)
		//  - Set<org.wildfly.extension.undertow.Server>
		//     - name
		//     - defaultHost
		//     - io.undertow.server.handlers.NameVirtualHostHandler
		//        - Map<String, HttpHandler> hosts
		//        - HttpHandler default handler
		//     - org.wildfly.extension.undertow.ServletContainerService
		//        - ...
		//        - io.undertow.servlet.api.ServletStackTraces
		//        - org.wildfly.extension.undertow.SessionCookieConfig (name, domain, httpOnly, ...)
		//        - org.wildfly.extension.undertow.JSPConfig
		//        - io.undertow.servlet.api.ServletContainer
		//        - io.undertow.server.handlers.cache.DirectBufferCache
		//        - io.undertow.servlet.api.SessionPersistenceManager
		//        - default encoding
		//        - ...
		//        - Map<String, String> mimeMappings
		//        - List<String> welcomeFiles
		//        - ...
		//        - Map<String, AuthenticationMechanismFactory> authenticationMechanisms;
		//        - ...
		//     - org.wildfly.extension.undertow.UndertowService (loop to parent)
		//     - io.undertow.server.HttpHandler root handler
		//     - List<org.wildfly.extension.undertow.ListenerService> (THE listeners (connectors))
		//        - org.xnio.XnioWorker
		//        - org.jboss.as.network.SocketBinding binding
		//        - org.jboss.as.network.SocketBinding redirect socket binding
		//        - io.undertow.connector.ByteBufferPool
		//        - org.wildfly.extension.undertow.Server (loop to parent)
		//        - List<io.undertow.server.HandlerWrapper> - wrappers to nest in each other leading eventually
		//          to the root handler in org.wildfly.extension.undertow.Server
		//        - name
		//        - org.xnio.OptionMap listener options
		//        - org.xnio.OptionMap socket options
		//        - io.undertow.server.OpenListener
		//        - enabled, started flags
		//     - Set<org.wildfly.extension.undertow.Host> (virtual hosts?)
		//        - io.undertow.server.handlers.PathHandler
		//        - Set<String> allAliases
		//        - name
		//        - org.wildfly.extension.undertow.AccessLogService
		//        - Set<io.undertow.servlet.api.Deployment>
		//        - Map<String, AuthenticationMechanism> additionalAuthenticationMechanisms
		//        - ...
		//
		// Derived ListenerServices:
		// org.wildfly.extension.undertow.ListenerService
		// |
		// +- org.wildfly.extension.undertow.AjpListenerService
		// |
		// +- org.wildfly.extension.undertow.HttpListenerService
		//    | - io.undertow.server.handlers.ChannelUpgradeHandler
		//    +- org.wildfly.extension.undertow.HttpsListenerService
		//        - javax.net.ssl.SSLContext supplier

		// here's the order of operations when Wildfly 19 starts:
		//  - org.wildfly.extension.undertow.ListenerAdd#performRuntime()
		//     - org.wildfly.extension.undertow.HttpListenerAdd#createService()
		//     - if org.wildfly.extension.undertow.ListenerResourceDefinition#RESOLVE_PEER_ADDRESS,
		//       io.undertow.server.handlers.PeerNameResolvingHandler is added as wrapper handler
		//     - org.wildfly.extension.undertow.ListenerService#setEnabled() (but it's not yet started)
		//     - if org.wildfly.extension.undertow.ListenerResourceDefinition#SECURE,
		//       io.undertow.servlet.handlers.MarkSecureHandler is added as wrapper handler
		//     - if org.wildfly.extension.undertow.ListenerResourceDefinition#DISALLOWED_METHODS,
		//       io.undertow.server.handlers.DisallowedMethodsHandler is added as wrapper handler
		//
		// - org.wildfly.extension.undertow.ListenerService#start()
		//    - org.wildfly.extension.undertow.HttpListenerService#preStart() - adding some info to
		//      io.undertow.server.ListenerRegistry
		//    - org.wildfly.extension.undertow.HttpListenerService#createOpenListener() - creates new
		//      io.undertow.server.protocol.http.HttpOpenListener
		//    - io.undertow.server.OpenListener.setRootHandler() is called with root handler from server, wrapped
		//      inside wrappers from the listener
		//    - org.xnio.ChannelListener<org.xnio.channels.AcceptingChannel<org.xnio.StreamConnection>> is created
		//      by calling org.xnio.ChannelListeners.openListenerAdapter(openListener)
		//    - org.wildfly.extension.undertow.HttpListenerService#startListening()
		//       - org.xnio.XnioWorker.createStreamConnectionServer()
		//          - org.xnio.nio.NioXnioWorker.createTcpConnectionServer()
		//             - java.nio.channels.ServerSocketChannel.open()
		//             - ...
		//             - java.net.ServerSocket.bind(java.net.SocketAddress, int)
		//             - org.xnio.nio.QueuedNioTcpServer2(new org.xnio.nio.NioTcpServer(..., channel, ...)) is created
		//             - org.xnio.nio.QueuedNioTcpServer2#setAcceptListener(openListener)
		//       - org.xnio.nio.QueuedNioTcpServer2.resumeAccepts()
		//       - org.xnio.channels.BoundChannel.getLocalAddress(java.lang.Class<A>) is logged as "listener started"

		// modelled after:
		//  - io.undertow.Undertow.start()
		//  - org.wildfly.extension.undertow.ListenerService.start()

		Xnio xnio = Xnio.getInstance(this.getClass().getClassLoader());
		List<AcceptingChannel<? extends StreamConnection>> channels = new LinkedList<>();

		HttpHandler rootHandler = exchange -> {
			exchange.setStatusCode(StatusCodes.OK);
			exchange.getResponseSender().send("Hello!");
			exchange.endExchange();
		};

		// https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.2/html-single/performance_tuning_guide/index#io_workers
		// https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.2/html-single/performance_tuning_guide/index#io_attributes

		// io.undertow.Undertow.Builder.Builder()

		int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2);
		int workerThreads = ioThreads * 8;
		boolean directBuffers = true; // because > 128MB of Xmx
		int bufferSize = 1024 * 16 - 20; //the 20 is to allow some space for protocol headers, see UNDERTOW-1209

		// io.undertow.Undertow.start()
		// worker options are passed to
		//  - org.xnio.nio.NioXnioWorker.NioXnioWorker() constructor
		//  - org.xnio.XnioWorker.XnioWorker() (super) constructor

		// "worker" can be used as several things:
		//  - java.util.concurrent.ExecutorService - to execute Runnables. these are passed to
		//    org.xnio.XnioWorker.TaskPool and options related to task/thread numbers and timeouts are used
		//  - org.xnio.XnioIoFactory - to create channels (XNIO extensions of java.nio.channels.InterruptibleChannel)
		//    these methods always call org.xnio.XnioWorker.chooseThread
		//  - XnioWorker methods like org.xnio.XnioWorker.createStreamConnectionServer that create
		//    org.xnio.nio.QueuedNioTcpServer.QueuedNioTcpServer which uses worker's accept thread as own thread

		// worker's createStreamConnectionServer() doesn't use worker's options, only options passed to the call
		// so there's no point in configuring connection related options when creating the worker itself

		OptionMap workerOptions = OptionMap.builder()
				// defaults to "XNIO-<id>" in XnioWorker(): org.xnio.XnioWorker.name
				.set(Options.WORKER_NAME, "my-xnio")
				// defaults to 4 in XnioWorker(): org.xnio.XnioWorker.coreSize
				.set(Options.WORKER_TASK_CORE_THREADS, workerThreads)
				// defaults to false in XnioWorker():
				//  - org.xnio.XnioWorker.taskPool = org.xnio.XnioWorker.TaskPool
				//  - (org.xnio.XnioWorker.TaskPool extends java.util.concurrent.ThreadPoolExecutor).threadFactory
				//  - org.xnio.XnioWorker.WorkerThreadFactory.markThreadAsDaemon
				.set(Options.THREAD_DAEMON, false)
				// defaults to 16 in XnioWorker():
				//  - java.util.concurrent.ThreadPoolExecutor.corePoolSize
				//  - java.util.concurrent.ThreadPoolExecutor.maximumPoolSize
				.set(Options.WORKER_TASK_MAX_THREADS, workerThreads)
				// defaults to 60,000 in XnioWorker():
				//  - java.util.concurrent.ThreadPoolExecutor.keepAliveTime
				.set(Options.WORKER_TASK_KEEPALIVE, 60_000)
				// defaults to 0L in org.xnio.XnioWorker.WorkerThreadFactory.newThread()
				//  - java.lang.Thread.stackSize
				.set(Options.STACK_SIZE, 0L)
				// defaults to Math.max(optionMap.get(Options.WORKER_READ_THREADS, 1), optionMap.get(Options.WORKER_WRITE_THREADS, 1))
				// in org.xnio.nio.NioXnioWorker.NioXnioWorker() constructor
				// defaults to Math.max(Runtime.getRuntime().availableProcessors(), 2) in io.undertow.Undertow.Builder.Builder()
				//  - org.xnio.nio.NioXnioWorker.workerThreads array length
				.set(Options.WORKER_IO_THREADS, ioThreads)
				// these are added in io.undertow.Undertow.start() but I'm not sure they are needed/used
//				.set(Options.CONNECTION_HIGH_WATER, 1_000_000)
//				.set(Options.CONNECTION_LOW_WATER, 1_000_000)
//				.set(Options.CORK, true)
				.getMap();

		// worker has worker threads and single accept thread - which is then copied into
		// org.xnio.nio.QueuedNioTcpServer.thread field

		// Fuse 7.6 creates 4 workers. org.xnio.XnioWorker.seq is added as suffix for names of the workers
		// 1. XNIO-1: xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap()); - for AccessLogReceiver
		//     - coreSize = 4
		//     - threadCount = 16
		//     - org.xnio.nio.NioXnioWorker.workerThreads size = 1
		//        - org.xnio.nio.WorkerThread with name = XNIO-1 I/O-1
		//     - org.xnio.nio.NioXnioWorker.acceptThread gets name XNIO-1 Accept
		// 2. XNIO-2: xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap()) for o.o.p.w.s.undertow.internal.Context.wsXnioWorker (hawtio WAR)
		// 3. XNIO-3: io.undertow.Undertow.start() which creates internalWorker
		//     - coreSize = 64
		//     - threadCount = 64
		// 4. XNIO-4: xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap()) for o.o.p.w.s.undertow.internal.Context.wsXnioWorker (/cxf servlet)

		XnioWorker worker = xnio.createWorker(workerOptions);

		// org.wildfly.extension.undertow.ListenerService#commonOptions

		OptionMap commonOptions = OptionMap.builder()
				.set(Options.TCP_NODELAY, true)
				// org.xnio.nio.NioXnioWorker.createTcpConnectionServer()
				//  - java.net.ServerSocket.setReuseAddress
				.set(Options.REUSE_ADDRESSES, true)
				.set(Options.BALANCING_TOKENS, 1)
				.set(Options.BALANCING_CONNECTIONS, 2)
				.getMap();

		OptionMap socketOptions = OptionMap.builder()
				.addAll(commonOptions)
				// org.xnio.nio.NioXnioWorker.createTcpConnectionServer()
				//  - java.net.ServerSocket.bind(java.net.SocketAddress, --> int <--)
				.set(Options.BACKLOG, 128)
				// org.xnio.nio.NioXnioWorker.createTcpConnectionServer()
				//  - java.net.ServerSocket.setReceiveBufferSize
				.set(Options.RECEIVE_BUFFER, 0x10000)
				// org.xnio.nio.QueuedNioTcpServer.handleReady()
				//  - java.net.Socket.setSendBufferSize
				.set(Options.SEND_BUFFER, 0x10000)
				//  - java.net.Socket.setKeepAlive
				.set(Options.KEEP_ALIVE, false)
				//  - java.net.Socket.setOOBInline
				.set(Options.TCP_OOB_INLINE, false)
				//  - java.net.Socket.setTcpNoDelay
				.set(Options.TCP_NODELAY, false)
				// in org.xnio.nio.QueuedNioTcpServer.accept(), a connection is created with READ and WRITE timeout
				// options set. these timeout options are then checked inside
				// io.undertow.server.protocol.http.HttpOpenListener.handleEvent(org.xnio.StreamConnection, io.undertow.connector.PooledByteBuffer)
				// to create proper sink/source conduits
				// - io.undertow.conduits.ReadTimeoutStreamSourceConduit inside org.xnio.conduits.ConduitStreamSourceChannel.conduit
				.set(Options.READ_TIMEOUT, 60 * 1000)
				// - io.undertow.conduits.WriteTimeoutStreamSourceConduit inside org.xnio.conduits.ConduitStreamSinkChannel.conduit
				.set(Options.WRITE_TIMEOUT, 60 * 1000)
				// org.xnio.nio.QueuedNioTcpServer.suspendedDueToWatermark
				.set(Options.CONNECTION_HIGH_WATER, 1_000_000).set(Options.CONNECTION_LOW_WATER, 1_000_000)
//				.set(Options.CORK, true)
				.getMap();

		// io.undertow.Undertow.start now creates listeners using provided (via the builder) list of
		// io.undertow.Undertow.ListenerConfigs

		OptionMap serverOptions = OptionMap.builder()
				// Conduit that adds support to close a channel once for a specified time no
				// reads and no writes were performed:
				// - io.undertow.conduits.IdleTimeoutConduit.expireTime
				//   inside both org.xnio.conduits.ConduitStreamSourceChannel.conduit and
				//   org.xnio.conduits.ConduitStreamSinkChannel.conduit
				// IDLE_TIMEOUT is also used as fallback when no READ_TIMEOUT/WRITE_TIMEOUT is specified on
				// accepted connection
				// - io.undertow.conduits.ReadTimeoutStreamSourceConduit.expireTime
				// - io.undertow.conduits.WriteTimeoutStreamSinkConduit.expireTime
				.set(UndertowOptions.IDLE_TIMEOUT, 60 * 1000)
				// io.undertow.server.protocol.http.HttpReadListener.parseTimeoutUpdater
				.set(UndertowOptions.REQUEST_PARSE_TIMEOUT, -1).set(UndertowOptions.NO_REQUEST_TIMEOUT, -1)
				.getMap();
		// undertow options are passed from listeners to actual connections, parsers, etc. accessed using
		// io.undertow.server.AbstractServerConnection.getUndertowOptions() - by checking where this method is called
		// we can check which options are used where
		OptionMap undertowOptions = OptionMap.builder()
				.set(UndertowOptions.BUFFER_PIPELINED_DATA, true)
				// io.undertow.server.protocol.http.HttpOpenListener.statisticsEnabled
				.set(UndertowOptions.ENABLE_STATISTICS, false)
				// io.undertow.server.protocol.http.HttpRequestParser.maxParameters
				.set(UndertowOptions.MAX_PARAMETERS, UndertowOptions.DEFAULT_MAX_PARAMETERS)
				// io.undertow.server.protocol.http.HttpRequestParser.maxHeaders
				.set(UndertowOptions.MAX_HEADERS, UndertowOptions.DEFAULT_MAX_HEADERS)
				// io.undertow.server.protocol.http.HttpRequestParser.allowEncodedSlash
				.set(UndertowOptions.ALLOW_ENCODED_SLASH, false)
				// io.undertow.server.protocol.http.HttpRequestParser.decode
				.set(UndertowOptions.DECODE_URL, true)
				// io.undertow.server.protocol.http.HttpRequestParser.charset
				.set(UndertowOptions.URL_CHARSET, StandardCharsets.UTF_8.name())
				// io.undertow.server.protocol.http.HttpRequestParser.maxCachedHeaderSize
				.set(UndertowOptions.MAX_CACHED_HEADER_SIZE, UndertowOptions.DEFAULT_MAX_CACHED_HEADER_SIZE)
				// io.undertow.server.protocol.http.HttpRequestParser.allowUnescapedCharactersInUrl
				.set(UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL, false)
				.addAll(serverOptions)
				.getMap();

		// where are timeout options used?
		//
		// hierarchy of "connections"
		// org.xnio.Connection
		// +- org.xnio.MessageConnection
		// +- org.xnio.StreamConnection
		//    +- org.xnio.nio.AbstractNioStreamConnection
		//       +- org.xnio.nio.NioPipeStreamConnection
		//       +- org.xnio.nio.NioSocketStreamConnection
		//    +- io.undertow.server.protocol.proxy.ProxyProtocolReadListener.AddressWrappedConnection
		//    +- org.xnio.ssl.SslConnection
		//       +- org.xnio.ssl.JsseSslStreamConnection
		//       +- io.undertow.protocols.ssl.UndertowSslConnection
		//
		// hierarchy of "servers"
		// org.xnio.channels.AcceptingChannel
		// +- org.xnio.nio.NioTcpServer
		// +- org.xnio.nio.QueuedNioTcpServer
		//
		// options supported by "connections":
		//  - org.xnio.nio.NioSocketStreamConnection.OPTIONS
		//  - org.xnio.nio.NioPipeStreamConnection: option == Options.READ_TIMEOUT && sourceConduit != null || option == Options.WRITE_TIMEOUT && sinkConduit != null
		//  - io.undertow.protocols.ssl.UndertowSslConnection: Options.SECURE, Options.SSL_CLIENT_AUTH_MODE + delegate
		//  - org.xnio.ssl.JsseSslStreamConnection: Options.SECURE, Options.SSL_CLIENT_AUTH_MODE + delegate
		//
		// options supported by "servers":
		//  - org.xnio.nio.NioTcpServer.options
		//  - org.xnio.nio.QueuedNioTcpServer.options

		// buffers to use
		ByteBufferPool buffers = new DefaultByteBufferPool(directBuffers, bufferSize, -1, 4);

		// OpenListener (a.k.a. "connector")
		//  - org.wildfly.extension.undertow.HttpListenerService#createOpenListener
		//  - org.wildfly.extension.undertow.HttpsListenerService#createOpenListener
		//  - org.wildfly.extension.undertow.HttpsListenerService#createAlpnOpenListener
		OpenListener openListener = new HttpOpenListener(buffers, undertowOptions);

		// for HTTPS + HTTP2
//		AlpnOpenListener alpn = new AlpnOpenListener((ByteBufferPool) null, null, new HttpOpenListener((ByteBufferPool) null, null));
//		alpn.addProtocol(Http2OpenListener.HTTP2, new Http2OpenListener((ByteBufferPool) null, null, "h2"), 10);
//		alpn.addProtocol(Http2OpenListener.HTTP2_14, new Http2OpenListener((ByteBufferPool) null, null, "h2-14"), 9);

		openListener.setRootHandler(rootHandler);

		ChannelListener<AcceptingChannel<StreamConnection>> acceptListener = ChannelListeners.openListenerAdapter(openListener);

		// the "server"
		AcceptingChannel<StreamConnection> server = worker.createStreamConnectionServer(new InetSocketAddress("0.0.0.0", 0), acceptListener, socketOptions);
		server.resumeAccepts();
		channels.add(server);

		int port = ((InetSocketAddress)server.getLocalAddress()).getPort();

		String response = send(port, "/");
		assertTrue(response.endsWith("\r\n\r\nHello!"));

		for (AcceptingChannel<? extends StreamConnection> channel : channels) {
			IoUtils.safeClose(channel);
		}
	}

	@Test
	public void addContextAfterServerHasStarted() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			public void init(ServletConfig config) throws ServletException {
				LOG.info("init() called on {} with {}", this, config);
				LOG.info(" - servlet name: {}", config.getServletName());
				LOG.info(" - context path: {}", config.getServletContext().getContextPath());
				super.init(config);
			}

			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("c1s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet.addMapping("/s1/*");

		// io.undertow.servlet.api.DeploymentInfo is equivalent of javax.servlet.ServletContext
		DeploymentInfo deploymentInfo1 = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();

		DeploymentManager dm1 = container.addDeployment(deploymentInfo1);
		dm1.deploy();
		HttpHandler handler = dm1.start();

		path.addPrefixPath("/c1", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		// add new context

		ServletInfo servlet2 = Servlets.servlet("c2s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet2.addMapping("/s1/*");

		DeploymentInfo deploymentInfo2 = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c2")
				.setDisplayName("Default Application 2")
				.setDeploymentName("d2")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet2);

		DeploymentManager dm2 = container.addDeployment(deploymentInfo2);
		// deploy() will initialize io.undertow.servlet.core.DeploymentManagerImpl.deployment
		dm2.deploy();
		// start() produces actual io.undertow.server.HttpHandler
		HttpHandler handler2 = dm2.start();
		path.addPrefixPath("/c2", handler2);

		// add new servlet to existing context

		ServletInfo servlet3 = Servlets.servlet("c1s2", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servlet3.addMapping("/s2/*");

		deploymentInfo1.addServlet(servlet3);

		// either removal and addition of the same deployment:
//		container.getDeployment(deploymentInfo.getDeploymentName()).undeploy();
//		container.removeDeployment(deploymentInfo);
//		dm = container.addDeployment(deploymentInfo);
//		dm.deploy();
//		handler = dm.start();
		// where handler needs to be replaced:
//		path.removePrefixPath("/c1");
//		path.addPrefixPath("/c1", handler);

		// or adding a servlet to existing io.undertow.servlet.api.DeploymentManager without altering the handlers
//		container.getDeploymentByPath("/c1").getDeployment().getServlets().addServlet(servlet3);
		dm1.getDeployment().getServlets().addServlet(servlet3);

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.endsWith("| /c1 | /s2 | null |"));

		response = send(port, "/c2/s1");
		assertTrue(response.endsWith("| /c2 | /s1 | null |"));

		server.stop();
	}

	@Test
	public void undertowUrlMapping() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servletForRoot = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));


		// Servlet API 4, 12.2 Specification of Mappings

		// A string beginning with a '/' character and ending with a '/*' suffix is used for path mapping.
		servletForRoot.addMapping("/p/*");

		// A string beginning with a '*.' prefix is used as an extension mapping.
		servletForRoot.addMapping("*.action");

		// The empty string ("") is a special URL pattern that exactly maps to the application's context root, i.e.,
		// requests of the form http://host:port/<context-root>/. In this case the path info is '/' and the
		// servlet path and context path is empty string ("").
		servletForRoot.addMapping("");

		// A string containing only the '/' character indicates the "default" servlet of the application.
		// In this case the servlet path is the request URI minus the context path and the path info is null.
//		servletForRoot.addMapping("/");

		// All other strings are used for exact matches only.
		servletForRoot.addMapping("/x");

		ServletInfo servletForOther = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<HttpServlet>(servletInstance));
		servletForOther.addMapping("/p/*");
		servletForOther.addMapping("*.action");
		servletForOther.addMapping("");
//		servletForOther.addMapping("/");
		servletForOther.addMapping("/x");

		DeploymentInfo rootContext = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/") // null or "" are converted to "/"
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servletForRoot);

		DeploymentInfo otherContext = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servletForOther);

		ServletContainer container = Servlets.newContainer();

		DeploymentManager rootDeployment = container.addDeployment(rootContext);
		rootDeployment.deploy();
		HttpHandler rootHandler = rootDeployment.start();
		DeploymentManager otherDeployment = container.addDeployment(otherContext);
		otherDeployment.deploy();
		HttpHandler otherHandler = otherDeployment.start();

		// the above handlers consist of:
		// rootHandler = {io.undertow.server.handlers.URLDecodingHandler@2350}
		//  next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.HttpContinueReadHandler@2365}
		//   handler: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.ServletInitialHandler@2368}
		//    next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2371}
		//     trueHandler: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2382}
		//      trueHandler: io.undertow.server.HttpHandler  = {io.undertow.security.handlers.SecurityInitialHandler@2383}
		//       next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler@2387}
		//        next: io.undertow.server.HttpHandler  = {io.undertow.security.handlers.AuthenticationMechanismsHandler@2393}
		//         next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler@2394}
		//          next: io.undertow.server.HttpHandler  = {io.undertow.server.handlers.PredicateHandler@2397}
		//           falseHandler: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler@2400}
		//            next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.security.SSLInformationAssociationHandler@2401}
		//             next: io.undertow.server.HttpHandler  = {io.undertow.servlet.handlers.ServletDispatchingHandler@2384}

		path.addPrefixPath(otherContext.getContextPath(), otherHandler);
		path.addPrefixPath(rootContext.getContextPath(), rootHandler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		// Undertow mapping is done in more flexible way (than in Jetty and Tomcat):
		// - host finding: only if root handler is io.undertow.server.handlers.NameVirtualHostHandler
		// - context finding: checking io.undertow.util.PathMatcher.paths in io.undertow.server.handlers.PathHandler
		// - servlet finding:
		//    - io.undertow.servlet.handlers.ServletInitialHandler.handleRequest() gets
		//      io.undertow.servlet.handlers.ServletPathMatch from io.undertow.servlet.handlers.ServletInitialHandler.paths
		//      and puts it into new io.undertow.servlet.handlers.ServletRequestContext() as exchange attachment
		//    - io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest() takes this attachment, its chain
		//      and calls io.undertow.servlet.handlers.ServletChain.handler.handleRequest()
		//    - the above handler calls init() if needed (for the first time) and passes to
		//      io.undertow.servlet.handlers.ServletHandler.handleRequest()
		//    - javax.servlet.Servlet.service() is called

		String response;

		// ROOT context
		response = send(port, "/p/anything");
		assertTrue(response.endsWith("|  | /p | /anything |"));
		response = send(port, "/anything.action");
		assertTrue(response.endsWith("|  | /anything.action | null |"));
		// just can't send `GET  HTTP/1.1` request
//		response = send(port, "");
		response = send(port, "/");
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("|  | / | null |"));
		response = send(port, "/x");
		assertTrue(response.endsWith("|  | /x | null |"));
		response = send(port, "/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		// /c1 context
		response = send(port, "/c1/p/anything");
		assertTrue(response.endsWith("| /c1 | /p | /anything |"));
		response = send(port, "/c1/anything.action");
		assertTrue(response.endsWith("| /c1 | /anything.action | null |"));
		response = send(port, "/c1");
		// if org.apache.catalina.Context.setMapperContextRootRedirectEnabled(true):
//		assertTrue(response.contains("HTTP/1.1 302"));
		assertTrue(response.contains("HTTP/1.1 404"));
		response = send(port, "/c1/");
		// https://bz.apache.org/bugzilla/show_bug.cgi?id=64109
		assertTrue("Special, strange Servlet API 4 mapping rule", response.endsWith("| /c1 | / | null |"));
		response = send(port, "/c1/x");
		assertTrue(response.endsWith("| /c1 | /x | null |"));
		response = send(port, "/c1/y");
		assertTrue(response.contains("HTTP/1.1 404"));

		server.stop();
	}

	private String send(int port, String request) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		return sw.toString();
	}

}
