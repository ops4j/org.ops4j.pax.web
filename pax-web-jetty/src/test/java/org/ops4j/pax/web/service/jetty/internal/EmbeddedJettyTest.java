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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.ee8.nested.AbstractHandler;
import org.eclipse.jetty.ee8.nested.ContextHandler;
import org.eclipse.jetty.ee8.nested.HttpChannel;
import org.eclipse.jetty.ee8.nested.ServletCoreRequest;
import org.eclipse.jetty.ee8.nested.ServletCoreResponse;
import org.eclipse.jetty.ee8.servlet.DefaultServlet;
import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.eclipse.jetty.ee8.servlet.ServletContextHandler;
import org.eclipse.jetty.ee8.servlet.ServletHandler;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.eclipse.jetty.ee8.servlet.ServletMapping;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ContextResponse;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.internal.HttpChannelState;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EmbeddedJettyTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyTest.class);

	/*
	 * When playing with ee8 parts of Jetty, I had to start from scratch to check what's needed to start jetty
	 * with all required handlers and beans.
	 *
	 * The source of truth could be ${jetty.home}/etc/jetty.xml
	 *
	 * Jetty with some ee modules starts with:
	 * "/tmp/start_1619727248056008658.properties"
	 * "${jetty.home}/etc/jetty-bytebufferpool.xml"
	 * "${jetty.home}/etc/jetty-threadpool.xml"
	 * "${jetty.home}/etc/jetty.xml"
	 * "${jetty.home}/etc/jetty-jaas.xml"
	 * "${jetty.home}/etc/jetty-deploy.xml"
	 * "${jetty.home}/etc/sessions/id-manager.xml"
	 * "${jetty.home}/etc/jetty-ee-webapp.xml"
	 * "${jetty.home}/etc/jetty-http.xml"
	 * "${jetty.home}/etc/jetty-jmx.xml"
	 * "--env" "ee8"
	 * "-cp" "${jetty.home}/lib/jetty-servlet-api-4.0.6.jar"
	 * "-cp" "${jetty.home}/lib/jetty-ee8-nested-12.0.20.jar"
	 * "-cp" "${jetty.home}/lib/jetty-ee8-servlet-12.0.20.jar"
	 * "-cp" "${jetty.home}/lib/jetty-ee8-security-12.0.20.jar"
	 * "-cp" "${jetty.home}/lib/jetty-ee8-webapp-12.0.20.jar"
	 * "contextHandlerClass=org.eclipse.jetty.ee8.webapp.WebAppContext"
	 * "${jetty.home}/etc/jetty-ee8-webapp.xml"
	 * "${jetty.home}/etc/jetty-ee8-deploy.xml"
	 */

	@Test
	public void embeddedServerWithTrivialHandler() throws Exception {
		// 3 arguments for org.eclipse.jetty.server.Server constructor used in etc/jetty.xml
		// 1. etc/jetty-threadpool.xml
		ThreadPool threadPool;
		// 2. inline in etc/jetty.xml
		Scheduler scheduler;
		// 3. etc/jetty-bytebufferpool.xml
		ByteBufferPool bufferPool;

		// <New id="threadPool" class="org.eclipse.jetty.util.thread.QueuedThreadPool">
		//   <Set name="name" property="jetty.threadPool.namePrefix" />
		//   <Set name="minThreads" type="int"><Property name="jetty.threadPool.minThreads" deprecated="threads.min" default="10"/></Set>
		//   <Set name="maxThreads" type="int"><Property name="jetty.threadPool.maxThreads" deprecated="threads.max" default="200"/></Set>
		//   <Set name="reservedThreads" type="int"><Property name="jetty.threadPool.reservedThreads" default="-1"/></Set>
		//   <Set name="useVirtualThreads" property="jetty.threadPool.useVirtualThreads" />
		//   <Set name="idleTimeout" type="int"><Property name="jetty.threadPool.idleTimeout" deprecated="threads.timeout" default="60000"/></Set>
		//   <Set name="maxEvictCount" type="int"><Property name="jetty.threadPool.maxEvictCount" default="1"/></Set>
		//   <Set name="detailedDump" type="boolean"><Property name="jetty.threadPool.detailedDump" default="false"/></Set>
		// </New>
		threadPool = new QueuedThreadPool(10);
		// name is set in modules/threadpool.mod
		((QueuedThreadPool) threadPool).setName("jetty-qtp");
		((QueuedThreadPool) threadPool).setMinThreads(10);
		((QueuedThreadPool) threadPool).setMaxThreads(200);
		((QueuedThreadPool) threadPool).setReservedThreads(1);
		// modules/threadpool.mod
//		((QueuedThreadPool) threadPool).setUseVirtualThreads(false);
		((QueuedThreadPool) threadPool).setIdleTimeout(60000);
		((QueuedThreadPool) threadPool).setMaxEvictCount(1);
		((QueuedThreadPool) threadPool).setDetailedDump(false);

		// <Arg>
		//   <New class="org.eclipse.jetty.util.thread.ScheduledExecutorScheduler">
		//     <Arg name="name"><Property name="jetty.scheduler.name"/></Arg>
		//     <Arg name="daemon" type="boolean"><Property name="jetty.scheduler.daemon" default="false" /></Arg>
		//     <Arg name="threads" type="int"><Property name="jetty.scheduler.threads" default="-1" /></Arg>
		//   </New>
		// </Arg>
		// name is set in modules/server.mod
		scheduler = new ScheduledExecutorScheduler("", false, -1);

		// <New id="byteBufferPool" class="org.eclipse.jetty.io.ArrayByteBufferPool">
		//   <Arg type="int"><Property name="jetty.byteBufferPool.minCapacity" default="0"/></Arg>
		//   <Arg type="int"><Property name="jetty.byteBufferPool.factor" default="4096"/></Arg>
		//   <Arg type="int"><Property name="jetty.byteBufferPool.maxCapacity" default="65536"/></Arg>
		//   <Arg type="int"><Property name="jetty.byteBufferPool.maxBucketSize" default="-1"/></Arg>
		//   <Arg type="long"><Property name="jetty.byteBufferPool.maxHeapMemory" default="0"/></Arg>
		//   <Arg type="long"><Property name="jetty.byteBufferPool.maxDirectMemory" default="0"/></Arg>
		//   <Set name="statisticsEnabled" property="jetty.byteBufferPool.statisticsEnabled" />
		// </New>
		// statisticsEnabled is set in modules/bytebufferpool.mod
		bufferPool = new ArrayByteBufferPool(0, 4096, 65536, -1, 0L, 0L);
		((ArrayByteBufferPool) bufferPool).setStatisticsEnabled(false);

		// main class for a "server" in Jetty. It:
		// - contains connectors (http receivers)
		// - contains request handlers (being a handler itself)
		// - contains a thread pool used by connectors to run request handlers
		Server server = new Server(threadPool, scheduler, bufferPool);

		// default httpConfig for a connector is declared in etc/jetty.xml
		// <New id="httpConfig" class="org.eclipse.jetty.server.HttpConfiguration">
		//   <Set name="secureScheme" property="jetty.httpConfig.secureScheme"/>
		//   <Set name="securePort" property="jetty.httpConfig.securePort"/>
		//   <Set name="outputBufferSize" property="jetty.httpConfig.outputBufferSize"/>
		//   <Set name="outputAggregationSize" property="jetty.httpConfig.outputAggregationSize"/>
		//   <Set name="requestHeaderSize" property="jetty.httpConfig.requestHeaderSize"/>
		//   <Set name="responseHeaderSize" property="jetty.httpConfig.responseHeaderSize"/>
		//   <Set name="maxResponseHeaderSize"><Property name="jetty.httpConfig.maxResponseHeaderSize" default="16384"/></Set>
		//   <Set name="sendServerVersion" property="jetty.httpConfig.sendServerVersion"/>
		//   <Set name="sendDateHeader"><Property name="jetty.httpConfig.sendDateHeader" default="false"/></Set>
		//   <Set name="headerCacheSize" property="jetty.httpConfig.headerCacheSize"/>
		//   <Set name="delayDispatchUntilContent" property="jetty.httpConfig.delayDispatchUntilContent"/>
		//   <Set name="maxErrorDispatches" property="jetty.httpConfig.maxErrorDispatches"/>
		//   <Set name="persistentConnectionsEnabled" property="jetty.httpConfig.persistentConnectionsEnabled"/>
		//   <Set name="httpCompliance"><Call class="org.eclipse.jetty.http.HttpCompliance" name="from"><Arg><Property name="jetty.httpConfig.compliance" deprecated="jetty.http.compliance" default="RFC7230"/></Arg></Call></Set>
		//   <Set name="uriCompliance"><Call class="org.eclipse.jetty.http.UriCompliance" name="from"><Arg><Property name="jetty.httpConfig.uriCompliance" default="DEFAULT"/></Arg></Call></Set>
		//   <Set name="requestCookieCompliance"><Call class="org.eclipse.jetty.http.CookieCompliance" name="from"><Arg><Property name="jetty.httpConfig.requestCookieCompliance" default="RFC6265"/></Arg></Call></Set>
		//   <Set name="responseCookieCompliance"><Call class="org.eclipse.jetty.http.CookieCompliance" name="from"><Arg><Property name="jetty.httpConfig.responseCookieCompliance" default="RFC6265"/></Arg></Call></Set>
		//   <Set name="relativeRedirectAllowed"><Property name="jetty.httpConfig.relativeRedirectAllowed" default="false"/></Set>
		//   <Set name="useInputDirectByteBuffers" property="jetty.httpConfig.useInputDirectByteBuffers"/>
		//   <Set name="useOutputDirectByteBuffers" property="jetty.httpConfig.useOutputDirectByteBuffers"/>
		// </New>
		HttpConfiguration httpConfig = new HttpConfiguration();
		// properties are defaulted in modules/server.mod
		httpConfig.setSecureScheme("https");
		httpConfig.setSecurePort(8443);
		httpConfig.setOutputBufferSize(32768);
		httpConfig.setOutputAggregationSize(8192);
		httpConfig.setRequestHeaderSize(8192);
		httpConfig.setResponseHeaderSize(8192);
		httpConfig.setMaxResponseHeaderSize(16384);
		httpConfig.setSendServerVersion(true);
		httpConfig.setSendDateHeader(false);
		httpConfig.setHeaderCacheSize(1024);
		httpConfig.setDelayDispatchUntilContent(true);
		httpConfig.setMaxErrorDispatches(10);
		httpConfig.setPersistentConnectionsEnabled(true);
		httpConfig.setHttpCompliance(HttpCompliance.RFC7230);
		httpConfig.setUriCompliance(UriCompliance.DEFAULT);
		httpConfig.setRequestCookieCompliance(CookieCompliance.RFC6265);
		httpConfig.setResponseCookieCompliance(CookieCompliance.RFC6265);
		httpConfig.setRelativeRedirectAllowed(false);
		httpConfig.setUseInputDirectByteBuffers(true);
		httpConfig.setUseOutputDirectByteBuffers(true);

		// connectors are added by etc/jetty-http.xml
		// <Call name="addConnector">
		//   <Arg>
		//     <New id="httpConnector" class="org.eclipse.jetty.server.ServerConnector">
		//       <Arg name="server"><Ref refid="Server" /></Arg>
		//       <Arg name="acceptors" type="int"><Property name="jetty.http.acceptors" default="1"/></Arg>
		//       <Arg name="selectors" type="int"><Property name="jetty.http.selectors" default="-1"/></Arg>
		//       <Arg name="factories">
		//         <Array type="org.eclipse.jetty.server.ConnectionFactory">
		//           <Item>
		//             <New class="org.eclipse.jetty.server.HttpConnectionFactory">
		//               <Arg name="config"><Ref refid="httpConfig" /></Arg>
		//             </New>
		//           </Item>
		//         </Array>
		//       </Arg>
		//       <Set name="host" property="jetty.http.host" />
		//       <Set name="port"><Property name="jetty.http.port" default="8080" /></Set>
		//       <Set name="idleTimeout"><Property name="jetty.http.idleTimeout" default="30000"/></Set>
		//       <Set name="acceptorPriorityDelta" property="jetty.http.acceptorPriorityDelta" />
		//       <Set name="acceptQueueSize" property="jetty.http.acceptQueueSize" />
		//       <Set name="reuseAddress"><Property name="jetty.http.reuseAddress" default="true"/></Set>
		//       <Set name="reusePort"><Property name="jetty.http.reusePort" default="false"/></Set>
		//       <Set name="acceptedTcpNoDelay"><Property name="jetty.http.acceptedTcpNoDelay" default="true"/></Set>
		//       <Set name="acceptedReceiveBufferSize" property="jetty.http.acceptedReceiveBufferSize" />
		//       <Set name="acceptedSendBufferSize" property="jetty.http.acceptedSendBufferSize" />
		//     </New>
		//   </Arg>
		// </Call>
		//
		// "connector" accepts remote connections and data. ServerConnector is the main, NIO based connector
		// that can handle HTTP, HTTP/2, SSL and websocket connections
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory(httpConfig));
		// values from modules/http.mod
		connector.setHost("0.0.0.0");
		connector.setPort(0);
		connector.setIdleTimeout(30000);
		connector.setAcceptorPriorityDelta(0);
		connector.setAcceptQueueSize(0);
		connector.setReuseAddress(true);
		connector.setReusePort(false);
		connector.setAcceptedTcpNoDelay(true);
		connector.setAcceptedReceiveBufferSize(-1);
		connector.setAcceptedSendBufferSize(-1);
		LOG.info("Local port before start: {}", connector.getLocalPort());

		// initially server doesn't have connectors, so we have to set them
		server.addConnector(connector);

		// this is done implicitly anyway
		server.setErrorHandler(new ErrorHandler());

		// handler being set in the Server is defined in startup arguments:
		// contextHandlerClass=org.eclipse.jetty.ee8.webapp.WebAppContext
		//
 		// ee8 classes are kind of regenerated from ee9 and are NOT part of Jetty source code
		// transformation is done with jetty-modify-sources:1.0.10:modify-sources-ee9-to-ee8

		// "handler" is invoked by connector when request is received. Jetty comes with this nice
		// hierarchy of handlers and Pax Web itself adds few more:
		//
		// Handler (org.eclipse.jetty.ee9.nested)
		//   AbstractHandler (org.eclipse.jetty.ee9.nested)
		//     AbstractHandlerContainer (org.eclipse.jetty.ee9.nested)
		//       HandlerCollection (org.eclipse.jetty.ee9.nested)
		//         HandlerList (org.eclipse.jetty.ee9.nested)
		//       HandlerWrapper (org.eclipse.jetty.ee9.nested)
		//         AsyncDelayHandler (org.eclipse.jetty.ee9.nested)
		//         BufferedResponseHandler (org.eclipse.jetty.ee9.nested)
		//           FileBufferedResponseHandler (org.eclipse.jetty.ee9.nested)
		//         DebugHandler (org.eclipse.jetty.ee9.nested)
		//         IdleTimeoutHandler (org.eclipse.jetty.ee9.nested)
		//         InetAccessHandler (org.eclipse.jetty.ee9.nested)
		//         ResourceHandler (org.eclipse.jetty.ee9.nested)
		//         ScopedHandler (org.eclipse.jetty.ee9.nested)
		//           ContextHandler (org.eclipse.jetty.ee9.nested)
		//             ServletContextHandler (org.eclipse.jetty.ee9.servlet)
		//               WebAppContext (org.eclipse.jetty.ee9.webapp)
		//                 MavenWebAppContext (org.eclipse.jetty.ee9.maven.plugin)
		//           ServletHandler (org.eclipse.jetty.ee9.servlet)
		//           SessionHandler (org.eclipse.jetty.ee9.nested)
		//         SecuredRedirectHandler (org.eclipse.jetty.ee9.nested)
		//         SecurityHandler (org.eclipse.jetty.ee9.security)
		//           ConstraintSecurityHandler (org.eclipse.jetty.ee9.security)
		//         ThreadLimitHandler (org.eclipse.jetty.ee9.nested)
		//       HotSwapHandler (org.eclipse.jetty.ee9.nested)
		//     ErrorDispatchHandler in AbstractHandler (org.eclipse.jetty.ee9.nested)
		//     ErrorHandler (org.eclipse.jetty.ee9.nested)
		//       ErrorPageErrorHandler (org.eclipse.jetty.ee9.servlet)

		// etc/jetty.xml has this handler:
		// <Set name="handler">
		//   <New id="Contexts" class="org.eclipse.jetty.server.handler.ContextHandlerCollection">
		//     <Set name="dynamic" property="jetty.server.contexts.dynamic"/>
		//   </New>
		// </Set>
		//
 		// later two handlers are added to "Contexts" bean:
		// etc/well-known.xml - org.eclipse.jetty.server.handler.ContextHandler with /.well-known path
		// org.eclipse.jetty.ee8.nested.ContextHandler$CoreContextHandler - by org.eclipse.jetty.deploy.providers.ScanningAppProvider.pathAdded()
		// that creates the handler using org.eclipse.jetty.deploy.App.getContextHandler()
		// org.eclipse.jetty.deploy.providers.ContextProvider.createContextHandler() checks env's "contextHandlerClass" property
		// which is "org.eclipse.jetty.ee8.webapp.WebAppContext"
		// and its hierarchy uses "nested" packages:
		// AbstractHandler (org.eclipse.jetty.ee8.nested)
		//  AbstractHandlerContainer (org.eclipse.jetty.ee8.nested)
		//   HandlerWrapper (org.eclipse.jetty.ee8.nested)
		//    ScopedHandler (org.eclipse.jetty.ee8.nested)
		//     ContextHandler (org.eclipse.jetty.ee8.nested)
		//      ServletContextHandler (org.eclipse.jetty.ee8.servlet)
		//       WebAppContext (org.eclipse.jetty.ee8.webapp)
		//
 		// however Jetty 12 Server needs a "core" handler to be set and the hierarchy is more important:
		// Handler in Request (org.eclipse.jetty.server)
		//   Handler (org.eclipse.jetty.server)
		//     Container in Handler (org.eclipse.jetty.server)
		//       Singleton in Handler (org.eclipse.jetty.server)
		//         SessionHandler (org.eclipse.jetty.session)
		//       Collection in Handler (org.eclipse.jetty.server)
		//     Abstract in Handler (org.eclipse.jetty.server)
		//       NonBlocking in Abstract in Handler (org.eclipse.jetty.server)
		//         Anonymous in WebSocketUpgradeHandler (org.eclipse.jetty.websocket.core.server)
		//       AbstractHandler (org.eclipse.jetty.server.handler)
		//       AbstractContainer in Handler (org.eclipse.jetty.server)
		//         AbstractHandlerContainer (org.eclipse.jetty.server.handler)
		//         Wrapper in Handler (org.eclipse.jetty.server)
		//           WebSocketUpgradeHandler (org.eclipse.jetty.websocket.core.server)
		//           ResourceHandler (org.eclipse.jetty.server.handler)
		//           IdleTimeoutHandler (org.eclipse.jetty.server.handler)
		//           GracefulHandler (org.eclipse.jetty.server.handler)
		//           ConnectHandler (org.eclipse.jetty.server.handler)
		//           ContextHandler (org.eclipse.jetty.server.handler)
		//             ResourceContext in ResourceHandler (org.eclipse.jetty.server.handler)
		//             CoreContextHandler in ContextHandler (org.eclipse.jetty.ee8.nested)
		//             MovedContextHandler (org.eclipse.jetty.server.handler)
		//           RewriteHandler (org.eclipse.jetty.rewrite.handler)
		//           SizeLimitHandler (org.eclipse.jetty.server.handler)
		//             SizeLimitHandler (org.eclipse.jetty.server)
		//           DelayedHandler (org.eclipse.jetty.server.handler)
		//           DebugHandler (org.eclipse.jetty.server.handler)
		//           ShutdownHandler (org.eclipse.jetty.server.handler)
		//           ConditionalHandler (org.eclipse.jetty.server.handler)
		//             Abstract in ConditionalHandler (org.eclipse.jetty.server.handler)
		//               InetAccessHandler (org.eclipse.jetty.server.handler)
		//               QoSHandler (org.eclipse.jetty.server.handler)
		//               ThreadLimitHandler (org.eclipse.jetty.server.handler)
		//               BufferedResponseHandler (org.eclipse.jetty.server.handler)
		//             ElseNext in ConditionalHandler (org.eclipse.jetty.server.handler)
		//             Reject in ConditionalHandler (org.eclipse.jetty.server.handler)
		//             SkipNext in ConditionalHandler (org.eclipse.jetty.server.handler)
		//             DontHandle in ConditionalHandler (org.eclipse.jetty.server.handler)
		//           EventsHandler (org.eclipse.jetty.server.handler)
		//             LatencyRecordingHandler (org.eclipse.jetty.server.handler)
		//             StatisticsHandler (org.eclipse.jetty.server.handler)
		//               MinimumDataRateHandler in StatisticsHandler (org.eclipse.jetty.server.handler)
		//           SecurityHandler (org.eclipse.jetty.security)
		//             PathMapped in SecurityHandler (org.eclipse.jetty.security)
		//           SecuredRedirectHandler (org.eclipse.jetty.server.handler)
		//           TryPathsHandler (org.eclipse.jetty.server.handler)
		//           StateTrackingHandler (org.eclipse.jetty.server.handler)
		//           CrossOriginHandler (org.eclipse.jetty.server.handler)
		//           Server (org.eclipse.jetty.server)
		//           GzipHandler (org.eclipse.jetty.server.handler.gzip)
		//         Sequence in Handler (org.eclipse.jetty.server)
		//           ContextHandlerCollection (org.eclipse.jetty.server.handler)
		//         HotSwapHandler (org.eclipse.jetty.server.handler)
		//         PathMappingsHandler (org.eclipse.jetty.server.handler)
		//       DefaultHandler (org.eclipse.jetty.server.handler)
		//       Redirector in MovedContextHandler (org.eclipse.jetty.server.handler)
		//       CoreToNestedHandler in CoreContextHandler in ContextHandler (org.eclipse.jetty.ee8.nested)
		//   ErrorHandler (org.eclipse.jetty.server.handler)
		//     ReHandlingErrorHandler (org.eclipse.jetty.server.handler)
		//       ByHttpStatus in ReHandlingErrorHandler (org.eclipse.jetty.server.handler)
		//     DynamicErrorHandler in Server (org.eclipse.jetty.server)

		// the trick with Jetty 12 is that org.eclipse.jetty.ee8.nested.ContextHandler() constructor
		// initializes _coreContextHandler field to new org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextHandler()
		// which extends non-nested org.eclipse.jetty.server.handler.ContextHandler and calls
		// super.setHandler(new org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextHandler.CoreToNestedHandler());
		// this CoreToNestedHandler extends org.eclipse.jetty.server.Handler.Abstract
		// and this is (?) where core -> nested is being bridged
		//
 		// org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextHandler.CoreToNestedHandler.handle()
		// takes a org.eclipse.jetty.ee8.nested.HttpChannel from org.eclipse.jetty.server.Request
		// (which creates wrapper or assumes org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextRequest
		// (with ee10 it was org.eclipse.jetty.ee10.servlet.ServletContextRequest)

		// to understand handlers, we need to understand requests, because this is most important hierarchy
		// when dealing with ee8/ee9/ee10:
		// Request (org.eclipse.jetty.server)
		//   ChannelRequest in HttpChannelState (org.eclipse.jetty.server.internal)
		//     ServeAs in Request (org.eclipse.jetty.server)
		//       ServletContextRequest (org.eclipse.jetty.ee10.servlet)
		//   ServerUpgradeRequest (org.eclipse.jetty.websocket.core.server)
		//     ServerUpgradeRequestImpl (org.eclipse.jetty.websocket.core.server.internal)
		//   ServerUpgradeRequest (org.eclipse.jetty.websocket.server)
		//     ServerUpgradeRequestDelegate (org.eclipse.jetty.websocket.server.internal)
		//   ServletCoreRequest (org.eclipse.jetty.ee8.nested)
		//     ForwardRequest in CrossContextDispatcher (org.eclipse.jetty.ee8.nested)
		//     IncludeRequest in CrossContextDispatcher (org.eclipse.jetty.ee8.nested)
		//   ServletCoreRequest (org.eclipse.jetty.ee9.nested)
		//     ForwardRequest in CrossContextDispatcher (org.eclipse.jetty.ee9.nested)
		//     IncludeRequest in CrossContextDispatcher (org.eclipse.jetty.ee9.nested)
		//   ServletCoreRequest (org.eclipse.jetty.ee10.servlet)
		//     ForwardRequest in CrossContextDispatcher (org.eclipse.jetty.ee10.servlet)
		//     IncludeRequest in CrossContextDispatcher (org.eclipse.jetty.ee10.servlet)
		//   Wrapper in Request (org.eclipse.jetty.server)
		//     Anonymous in asReadOnly() in Request (org.eclipse.jetty.server)
		//     Anonymous in customize() in AuthorityCustomizer (org.eclipse.jetty.http2.server)
		//     Anonymous in customize() in HostHeaderCustomizer (org.eclipse.jetty.server)
		//     Anonymous in prepareRequest() in FormAuthenticator (org.eclipse.jetty.security.authentication)
		//     Anonymous in prepareRequest() in OpenIdAuthenticator (org.eclipse.jetty.security.openid)
		//     Anonymous in serveAs() in Request (org.eclipse.jetty.server)
		//     Anonymous in wrap() in ServletContextRequest (org.eclipse.jetty.ee10.servlet)
		//     AttributesWrapper in Request (org.eclipse.jetty.server)
		//       Anonymous in customize() in ForwardedRequestCustomizer (org.eclipse.jetty.server)
		//       ErrorRequest in ErrorHandler (org.eclipse.jetty.server.handler)
		//       SecureRequestWithSslSessionData in SecureRequestCustomizer (org.eclipse.jetty.server)
		//     ContextRequest (org.eclipse.jetty.server.handler)
		//       CoreContextRequest in ContextHandler (org.eclipse.jetty.ee8.nested)
		//       CoreContextRequest in ContextHandler (org.eclipse.jetty.ee9.nested)
		//       ServletContextRequest (org.eclipse.jetty.ee10.servlet)
		//     EventsRequest in EventsHandler (org.eclipse.jetty.server.handler)
		//     GzipRequest (org.eclipse.jetty.server.handler.gzip)
		//     Handler in Rule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in CookiePatternRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in HeaderPatternRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in HeaderRegexRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in InvalidURIRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in RedirectPatternRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in RedirectRegexRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in ResponsePatternRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in apply() in ResponseStatusHeaderRegexRule (org.eclipse.jetty.rewrite.handler)
		//       Anonymous in matchAndApply() in ForceRequestHeaderValueRule (org.eclipse.jetty.rewrite.handler)
		//       HttpURIHandler in Rule (org.eclipse.jetty.rewrite.handler)
		//       Input in RewriteCustomizer (org.eclipse.jetty.rewrite)
		//       LastRuleHandler in RewriteHandler (org.eclipse.jetty.rewrite.handler)
		//     HeaderWrappingRequest (org.eclipse.jetty.server.handler.gzip)
		//     LimitedRequest in ThreadLimitHandler (org.eclipse.jetty.server.handler)
		//     MinimumDataRateRequest in MinimumDataRateHandler in StatisticsHandler (org.eclipse.jetty.server.handler)
		//     NonServletSessionRequest in SessionHandler (org.eclipse.jetty.ee10.servlet)
		//     ProxyRequest in ProxyCustomizer (org.eclipse.jetty.server)
		//     RequestWrapper in StateTrackingHandler (org.eclipse.jetty.server.handler)
		//     RewindChunkRequest in UntilContentDelayedProcess in DelayedHandler (org.eclipse.jetty.server.handler)
		//     SecureRequest in SecureRequestCustomizer (org.eclipse.jetty.server)
		//     ServerUpgradeRequestDelegate (org.eclipse.jetty.websocket.server.internal)
		//     ServerUpgradeRequestImpl (org.eclipse.jetty.websocket.core.server.internal)
		//     SessionRequest in SessionHandler (org.eclipse.jetty.session)
		//     ShutdownTrackingRequest in GracefulHandler (org.eclipse.jetty.server.handler)
		//     SizeLimitRequestWrapper in SizeLimitHandler (org.eclipse.jetty.server.handler)

		// here's the lifecycle of a HttpServletRequest being passed to real servlet:
		// 1. org.eclipse.jetty.http.MetaData.Request is created by
		//    o.e.j.s.internal.HttpConnection.HttpStreamOverHTTP1.headerComplete()
		// 2. org.eclipse.jetty.server.internal.HttpChannelState.ChannelRequest is created by
		//    o.e.j.s.internal.HttpChannelState.onRequest() using #1
		// 3. org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextRequest is created by
		//    o.e.j.s.handler.ContextHandler.handle() using #2
		//    it's a class with org.eclipse.jetty.server.handler.ContextRequest parent
		//    actual handle() is called on org.eclipse.jetty.ee8.nested.ContextHandler.CoreContextHandler already
		// 3a. CoreContextRequest is given httpChannel which is org.eclipse.jetty.ee8.nested.HttpChannel,
		//     which includes _request field of org.eclipse.jetty.ee8.nested.Request class
		// and finally the Request is passed as javax.servlet.http.HttpServletRequest to the servlet
		//
 		// in org.eclipse.jetty.ee8.servlet.ServletHolder.handle(Request baseRequest, ServletRequest request, ServletResponse response)
		// 1st and 2nd arguments are of the same class
		//
 		// in org.eclipse.jetty.ee8.nested.ContextHandler.handle(org.eclipse.jetty.ee8.nested.HttpChannel)
		// org.eclipse.jetty.ee8.nested.ScopedHandler.handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
		// is passed with baseRequest == request which is taken from org.eclipse.jetty.ee8.nested.HttpChannel.getRequest()

		// for completeness, here's the hierarchy of responses
		// Response (org.eclipse.jetty.server)
		//   ChannelResponse in HttpChannelState (org.eclipse.jetty.server.internal)
		//     ErrorResponse in HttpChannelState (org.eclipse.jetty.server.internal)
		//   DeferredResponse in Deferred in AuthenticationState (org.eclipse.jetty.security)
		//     Anonymous in __deferredResponse in DeferredAuthenticationState (org.eclipse.jetty.security.internal)
		//   ServerUpgradeResponse (org.eclipse.jetty.websocket.core.server)
		//     ServerUpgradeResponseImpl (org.eclipse.jetty.websocket.core.server.internal)
		//   ServerUpgradeResponse (org.eclipse.jetty.websocket.server)
		//     ServerUpgradeResponseDelegate (org.eclipse.jetty.websocket.server.internal)
		//   ServletCoreResponse (org.eclipse.jetty.ee8.nested)
		//     IncludeResponse in CrossContextDispatcher (org.eclipse.jetty.ee8.nested)
		//   ServletCoreResponse (org.eclipse.jetty.ee9.nested)
		//     IncludeResponse in CrossContextDispatcher (org.eclipse.jetty.ee9.nested)
		//   ServletCoreResponse (org.eclipse.jetty.ee10.servlet)
		//     IncludeResponse in CrossContextDispatcher (org.eclipse.jetty.ee10.servlet)
		//   Wrapper in Response (org.eclipse.jetty.server)
		//     Anonymous in handle() in SecurityHandler (org.eclipse.jetty.security)
		//     BufferedResponse in BufferedResponseHandler (org.eclipse.jetty.server.handler)
		//     ContextResponse (org.eclipse.jetty.server.handler)
		//       ServletContextResponse (org.eclipse.jetty.ee10.servlet)
		//     EventsResponse in EventsHandler (org.eclipse.jetty.server.handler)
		//     GzipResponseAndCallback (org.eclipse.jetty.server.handler.gzip)
		//     LimitedResponse in ThreadLimitHandler (org.eclipse.jetty.server.handler)
		//     MinimumDataRateResponse in MinimumDataRateHandler in StatisticsHandler (org.eclipse.jetty.server.handler)
		//     ResponseWrapper in StateTrackingHandler (org.eclipse.jetty.server.handler)
		//     ServerUpgradeResponseDelegate (org.eclipse.jetty.websocket.server.internal)
		//     ServerUpgradeResponseImpl (org.eclipse.jetty.websocket.core.server.internal)
		//     SizeLimitResponseWrapper in SizeLimitHandler (org.eclipse.jetty.server.handler)

		server.setHandler(new Handler.Abstract() {
			@Override
			protected void doStart() {
				LOG.info("Starting custom handler during server startup");
			}

			@Override
			public boolean handle(Request request, Response res, Callback callback) {
				Request r0 = Request.as(request, org.eclipse.jetty.server.Request.class);
				MetaData.Request r1 = Request.as(request, MetaData.Request.class);
				HttpChannelState.ChannelRequest r2 = Request.as(request, HttpChannelState.ChannelRequest.class);
				ContextHandler.CoreContextRequest r3 = Request.as(request, ContextHandler.CoreContextRequest.class);
				org.eclipse.jetty.ee8.nested.Request r4 = Request.as(request, org.eclipse.jetty.ee8.nested.Request.class);

				assertNotNull(r0);
				assertNull(r1);
				assertNotNull(r2);
				assertNull(r3);
				assertNull(r4);

				Response rs0 = Response.as(res, org.eclipse.jetty.server.Response.class);
				MetaData.Response rs1 = Response.as(res, MetaData.Response.class);
				HttpChannelState.ChannelResponse rs2 = Response.as(res, HttpChannelState.ChannelResponse.class);
				ContextResponse rs3 = Response.as(res, ContextResponse.class);
				org.eclipse.jetty.ee8.nested.Response rs4 = Response.as(res, org.eclipse.jetty.ee8.nested.Response.class);

				assertNotNull(rs0);
				assertNull(rs1);
				assertNotNull(rs2);
				assertNull(rs3);
				assertNull(rs4);

				rs2.getHeaders().put("Content-Type", "text/plain; charset=UTF-8");
				rs2.write(true, ByteBuffer.wrap("OK\n".getBytes(StandardCharsets.UTF_8)), callback);
				return true;
			}
		});

		server.start();

		LOG.info("Local port after start: {}", connector.getLocalPort());

		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", connector.getLocalPort()));

		s.getOutputStream().write((
				"GET / HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithContextHandler() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		// ServletHandler requires ServletContextHandler, so we need more primitive handlers
		org.eclipse.jetty.ee8.nested.Handler plainHandler1 = new AbstractHandler() {
			@Override
			public void handle(String target, org.eclipse.jetty.ee8.nested.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
				response.setContentType("text/plain");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write("OK1\n");
				response.getWriter().close();
			}
		};

		org.eclipse.jetty.ee8.nested.Handler plainHandler2 = new AbstractHandler() {
			@Override
			public void handle(String target, org.eclipse.jetty.ee8.nested.Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
				response.setContentType("text/plain");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write("OK2\n");
				response.getWriter().close();
			}
		};

		// context handler sets "context" for the request. "Context" consists of class loader, context path, ...
		ContextHandler handler1 = new ContextHandler("/c1");
		handler1.setHandler(plainHandler1);
		// without it, we'll need "GET /c1/ HTTP/1.1" requests
		// or just follow `HTTP/1.1 302 Found` redirect from /c1 to /c1/
		handler1.setAllowNullPathInfo(true);
		ContextHandler handler2 = new ContextHandler("/c2");
		handler2.setHandler(plainHandler2);
		// without it, we'll need "GET /c2/ HTTP/1.1" requests
		handler2.setAllowNullPathInfo(true);

		// either pass core handers from nested handlers
//		ContextHandlerCollection chc = new ContextHandlerCollection(handler1.getCoreContextHandler(), handler2.getCoreContextHandler());
		// or use suppliers!
		ContextHandlerCollection chc = new ContextHandlerCollection();
		chc.addHandler(handler1);
		chc.addHandler(handler2);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /c1 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK1\n"));

		Socket s2 = new Socket();
		s2.connect(new InetSocketAddress("127.0.0.1", port));

		s2.getOutputStream().write((
				"GET /c2 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		buf = new byte[64];
		sw = new StringWriter();
		while ((read = s2.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s2.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandler() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		// passing chc to ServletContextHandler is a bit confusing, because it's not kept as field there. It's
		// only used to call chc.addHandler()
		// fortunately null can be passed as 1st argument in ServletContextHandler constructor
		ContextHandlerCollection chc = new ContextHandlerCollection();

		// servlet context handler extends ContextHandler for easier ContextHandler with _handler = ServletHandler
		// created ServletContextHandler will already have session, security handlers (depending on options) and
		// ServletHandler and we can add servlets/filters through ServletContextHandler
		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		// this single method adds both ServletHolder and ServletMapping
		// calling org.eclipse.jetty.servlet.ServletHandler.addServletWithMapping()
		handler1.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();
			}
		}), "/");

		ServletContextHandler handler2 = new ServletContextHandler(null, "/c2", ServletContextHandler.NO_SESSIONS);
		handler2.setAllowNullPathInfo(true);
		handler2.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK2\n");
				resp.getWriter().close();
			}
		}), "/");

		chc.addHandler(handler1);
		chc.addHandler(handler2);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /c1 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK1\n"));

		Socket s2 = new Socket();
		s2.connect(new InetSocketAddress("127.0.0.1", port));

		s2.getOutputStream().write((
				"GET /c2 HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		buf = new byte[64];
		sw = new StringWriter();
		while ((read = s2.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s2.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAndDynamicInitializers() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);

		// Jetty uses org.eclipse.jetty.server.handler.ContextHandler.Context.setExtendedListenerTypes flag
		// that may alter the specification-defined behavior of adding listeners
		// org.eclipse.jetty.server.handler.ContextHandler.Context._enabled turns on/off dynamic registration to
		// the context, but still a type of listener is verified - ServletContextListener type can be added
		// only if setExtendedListenerTypes(true) was called
		// org.eclipse.jetty.server.handler.ContextHandler.Context._enabled is set to false when first programmatic
		// ServletContextListener is called
		// a "programmatic listener" is any listener added using sc.addListener()
		//
		// Jetty calls context.getServletContext().setExtendedListenerTypes(true) when starting SCIs (which are wrapped
		// in ServletContextListeners...) ONLY
		// If a ServletContextListener wants to add another ServletContextListener (like
		// org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.configure())

		// SCI that adds a ServletContextListener which tries to add ServletContextListener
		handler1.addServletContainerInitializer(new ServletContainerInitializer() {
			@Override
			public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
				// ServletContextListener added from SCI - this is real "programmatic listener"
				ctx.addListener(new ServletContextListener() {
					@Override
					public void contextInitialized(ServletContextEvent sce) {
						// ServletContextListener added from a "programmatic listener"
						// throws (according to the spec) java.lang.UnsupportedOperationException
//						sce.getServletContext().addListener(new ServletContextListener() {
//							@Override
//							public void contextInitialized(ServletContextEvent sce) {
//								ServletContextListener.super.contextInitialized(sce);
//							}
//						});
					}
				});
			}
		});

		// ServletContextListener added "from web.xml"
		handler1.addEventListener(new ServletContextListener() {
			@Override
			public void contextInitialized(ServletContextEvent sce) {
				// ServletContextListener added from a listener - not possible:
				//     java.lang.IllegalArgumentException: Inappropriate listener class org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$7$1
				// however we can set org.eclipse.jetty.server.handler.ContextHandler.Context._extendedListenerTypes to true
				// just as org.eclipse.jetty.ee8.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer.configure()
				// do it
//				sce.getServletContext().addListener(new ServletContextListener() {
//					@Override
//					public void contextInitialized(ServletContextEvent sce) {
//						ServletContextListener.super.contextInitialized(sce);
//					}
//				});
			}
		});

		handler1.addServlet(new ServletHolder("default-servlet", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();

				// can't add new servlet - org.eclipse.jetty.servlet.ServletContextHandler.Context.checkDynamic()
				// prevents it
//				req.getServletContext().addServlet("new-servlet", new HttpServlet() {
//					@Override
//					protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//						resp.setContentType("text/plain");
//						resp.setCharacterEncoding("UTF-8");
//						resp.getWriter().write("OK2\n");
//						resp.getWriter().close();
//					}
//				}).addMapping("/s2");
			}
		}), "/s1");

		chc.addHandler(handler1);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response;

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));
//		// call servlet added dynamically from the servlet
//		response = send(connector.getLocalPort(), "/c1/s2");
//		assertTrue(response.endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithJettyResourceServlet() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();
		ServletContextHandler handler1 = new ServletContextHandler(null, "/", ServletContextHandler.NO_SESSIONS);
		handler1.setAllowNullPathInfo(true);
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "dirAllowed", "false");
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "etags", "true");
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "resourceBase", new File("target").getAbsolutePath());
		handler1.setInitParameter(DefaultServlet.CONTEXT_INIT + "maxCachedFiles", "1000");

		handler1.addServlet(new ServletHolder("default", new DefaultServlet()), "/");

		chc.addHandler(handler1);
		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		// Jetty generates ETag using org.eclipse.jetty.util.resource.Resource.getWeakETag(java.lang.String)
		// which is 'W/"' + base64(hash of name ^ lastModified) + base64(hash of name ^ length) + '"'

		String response = send(port, "/test-classes/log4j2-test.properties");
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		assertTrue(response.contains("rootLogger.appenderRef.file.ref = file"));

		response = send(port, "/test-classes/log4j2-test.properties",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.contains("rootLogger.appenderRef.file.ref = file"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAndOnlyFilter() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(null, "/c1", ServletContextHandler.NO_SESSIONS);

		// without "default 404 servlet", jetty won't invoke a "pipeline" that has only a filter.
		handler1.getServletHandler().setEnsureDefaultServlet(true);

		handler1.setAllowNullPathInfo(true);
		handler1.addFilter(new FilterHolder(new Filter() {
			@Override
			public void init(FilterConfig filterConfig) {
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK\n");
				resp.getWriter().close();
			}
		}), "/*", EnumSet.of(DispatcherType.REQUEST));

		chc.addHandler(handler1);

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /c1/anything HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithServletContextHandlerAddedAfterServerHasStarted() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		ServletContextHandler handler1 = new ServletContextHandler(chc, "/c1", ServletContextHandler.NO_SESSIONS);
		handler1.addServlet(new ServletHolder("s1", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();
			}
		}), "/s1");

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		String response;

		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(connector.getLocalPort(), "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		// add new context

		ServletContextHandler handler2 = new ServletContextHandler(chc, "/c2", ServletContextHandler.NO_SESSIONS);
		handler2.setAllowNullPathInfo(true);
		handler2.addServlet(new ServletHolder("s1", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK2\n");
				resp.getWriter().close();
			}
		}), "/s1");
		handler2.start();

		// add new servlet to existing context

		handler1.addServlet(new ServletHolder("s2", new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK3\n");
				resp.getWriter().close();
			}
		}), "/s2");

		response = send(connector.getLocalPort(), "/c1/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));

		response = send(connector.getLocalPort(), "/c1/s2");
		assertTrue(response.endsWith("\r\n\r\nOK3\n"));

		response = send(connector.getLocalPort(), "/c2/s1");
		assertTrue(response.endsWith("\r\n\r\nOK2\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithWebAppContext() throws Exception {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server, 1, 1, new HttpConnectionFactory());
		connector.setPort(0);
		server.setConnectors(new Connector[] { connector });

		ContextHandlerCollection chc = new ContextHandlerCollection();

		// and finally an extension of ServletContextHandler - WebAppContext, which is again a ServletContextHandler
		// but objects (filters, servlets, ...) are added by org.eclipse.jetty.webapp.Configuration and
		// org.eclipse.jetty.webapp.DescriptorProcessor processors
		WebAppContext wac1 = new WebAppContext();
		wac1.setContextPath("/app1");
		// by default, null path info is not allowed and redirect (with added "/") is sent when requesting just
		// the context URL
		wac1.setAllowNullPathInfo(false);
		// when we don't pass handler collection (or handler wrapper) in constructor, we have to add this
		// specialized context handler manually
		chc.addHandler(wac1);

		// org.eclipse.jetty.ee8.webapp.StandardDescriptorProcessor.end() just adds 4 component lists to WebAppContext's
		// org.eclipse.jetty.ee8.servlet.ServletContextHandler._servletHandler:
		// - servlets
		// - filters
		// - servlet mappings
		// - filter mappings
		//
		// when WebAppContext.doStart() calls org.eclipse.jetty.webapp.WebAppContext.preConfigure(), all
		// org.eclipse.jetty.webapp.WebAppContext._configurationClasses are turned into actual configurators
		// default configuration classes are org.eclipse.jetty.webapp.WebAppContext.DEFAULT_CONFIGURATION_CLASSES
		wac1.setConfigurationClasses(new String[] {
				"org.eclipse.jetty.ee8.webapp.WebXmlConfiguration"
		});

		// to impact WebXmlConfiguration, we need few settings
		wac1.setDefaultsDescriptor(null); // to override "org/eclipse/jetty/webapp/webdefault.xml"

		// prepare pure web.xml without any web app structure
		File webXml = new File("target/web-" + UUID.randomUUID() + ".xml");
		webXml.delete();

		try (FileWriter writer = new FileWriter(webXml)) {
			writer.write("<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
					"    xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"\n" +
					"    version=\"4.0\">\n" +
					"\n" +
					"    <servlet>\n" +
					"        <servlet-name>test-servlet</servlet-name>\n" +
					"        <servlet-class>org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$TestServlet</servlet-class>\n" +
					"    </servlet>\n" +
					"\n" +
					"    <servlet-mapping>\n" +
					"        <servlet-name>test-servlet</servlet-name>\n" +
					"        <url-pattern>/ts</url-pattern>\n" +
					"    </servlet-mapping>\n" +
					"\n" +
					"</web-app>\n");
		}

		// all the metadata from different (webdefaults.xml, web.xml, ...) descriptors are kept in
		// org.eclipse.jetty.webapp.MetaData object inside org.eclipse.jetty.webapp.WebAppContext._metadata
		wac1.setDescriptor(webXml.toURI().toURL().toString());

		// when WebAppContext is started, registered descriptor processors process the descriptors
		// org.eclipse.jetty.webapp.StandardDescriptorProcessor.start():
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterHolderMap with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterHolders with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._filterMappings with existing filters
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletHolderMap with existing servlets
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletHolders with existing servlets
		//    from ServletContextHandler._servletHandler
		//  - populates org.eclipse.jetty.webapp.StandardDescriptorProcessor._servletMappings with existing servlets
		//    from ServletContextHandler._servletHandler
		//
		// org.eclipse.jetty.webapp.StandardDescriptorProcessor.visit() calls one of 19 visitors:
		// _visitors = {java.util.HashMap@2792}  size = 19
		//  - "servlet-mapping" -> "StandardDescriptorProcessor.visitServletMapping()"
		//  - "mime-mapping" -> "StandardDescriptorProcessor.visitMimeMapping()"
		//  - "distributable" -> "StandardDescriptorProcessor.visitDistributable()"
		//  - "locale-encoding-mapping-list" -> "StandardDescriptorProcessor.visitLocaleEncodingList()"
		//  - "servlet" -> "StandardDescriptorProcessor.visitServlet()"
		//  - "security-role" -> "StandardDescriptorProcessor.visitSecurityRole()"
		//  - "listener" -> "StandardDescriptorProcessor.visitListener()"
		//  - "jsp-config" -> "StandardDescriptorProcessor.visitJspConfig()"
		//  - "context-param" -> "StandardDescriptorProcessor.visitContextParam()"
		//  - "filter" -> "StandardDescriptorProcessor.visitFilter()"
		//  - "welcome-file-list" -> "StandardDescriptorProcessor.visitWelcomeFileList()"
		//  - "taglib" -> "StandardDescriptorProcessor.visitTagLib()"
		//  - "deny-uncovered-http-methods" -> "StandardDescriptorProcessor.visitDenyUncoveredHttpMethods()"
		//  - "login-config" -> "StandardDescriptorProcessor.visitLoginConfig() throws java.lang.Exception"
		//  - "display-name" -> "StandardDescriptorProcessor.visitDisplayName()"
		//  - "error-page" -> "StandardDescriptorProcessor.visitErrorPage()"
		//  - "session-config" -> "StandardDescriptorProcessor.visitSessionConfig()"
		//  - "security-constraint" -> "StandardDescriptorProcessor.visitSecurityConstraint()"
		//  - "filter-mapping" -> "StandardDescriptorProcessor.visitFilterMapping()"
		// org.eclipse.jetty.webapp.StandardDescriptorProcessor.end() calls
		// (on org.eclipse.jetty.servlet.ServletContextHandler._servletHandler):
		//  - org.eclipse.jetty.servlet.ServletHandler.setFilters()
		//  - org.eclipse.jetty.servlet.ServletHandler.setServlets()
		//  - org.eclipse.jetty.servlet.ServletHandler.setFilterMappings()
		//  - org.eclipse.jetty.servlet.ServletHandler.setServletMappings()
		//
		// visitServlet()        creates new org.eclipse.jetty.servlet.ServletHolder
		// visitServletMapping() creates new org.eclipse.jetty.servlet.ServletMapping

		server.setHandler(chc);
		server.start();

		int port = connector.getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /app1/ts HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector.getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void embeddedServerWithExternalConfiguration() throws Exception {
		// order is important
		String[] xmls = new String[] {
				"etc/jetty-threadpool.xml",
				"etc/jetty.xml",
				"etc/jetty-connectors.xml",
				"etc/jetty-handlercollection.xml",
				"etc/jetty-webapp.xml",
		};

		// prepare pure web.xml without any web app structure
		File webXml = new File("target/web.xml");
		webXml.delete();

		try (FileWriter writer = new FileWriter(webXml)) {
			writer.write("<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
					"    xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"\n" +
					"    version=\"4.0\">\n" +
					"\n" +
					"    <servlet>\n" +
					"        <servlet-name>test-servlet</servlet-name>\n" +
					"        <servlet-class>org.ops4j.pax.web.service.jetty.internal.EmbeddedJettyTest$TestServlet</servlet-class>\n" +
					"    </servlet>\n" +
					"\n" +
					"    <servlet-mapping>\n" +
					"        <servlet-name>test-servlet</servlet-name>\n" +
					"        <url-pattern>/ts</url-pattern>\n" +
					"    </servlet-mapping>\n" +
					"\n" +
					"</web-app>\n");
		}

		// loop taken from org.eclipse.jetty.xml.XmlConfiguration.main()

		XmlConfiguration last = null;
		Map<String, Object> objects = new LinkedHashMap<>();

		for (String xml : xmls) {
			File f = new File("target/test-classes/" + xml);
			Resource r = new PathResourceFactory().newResource(f.getPath());
			XmlConfiguration configuration = new XmlConfiguration(r);

			if (last != null) {
				configuration.getIdMap().putAll(last.getIdMap());
			}
			configuration.getProperties().put("thread.name.prefix", "jetty-qtp");

			configuration.configure();

			objects.putAll(configuration.getIdMap());

			last = configuration;
		}

		final ServerConnector[] connector = { null };
		objects.forEach((k, v) -> {
			LOG.info("Created {} -> {}", k, v);
			if (connector[0] == null && v instanceof ServerConnector) {
				connector[0] = (ServerConnector) v;
			}
		});

		Server server = (Server) objects.get("Server");
		assertThat(((QueuedThreadPool) server.getThreadPool()).getName(), equalTo("jetty-qtp"));

		server.start();

		int port = connector[0].getLocalPort();

		Socket s1 = new Socket();
		s1.connect(new InetSocketAddress("127.0.0.1", port));

		s1.getOutputStream().write((
				"GET /app1/ts HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + connector[0].getLocalPort() + "\r\n" +
				"Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read = -1;
		StringWriter sw = new StringWriter();
		while ((read = s1.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s1.close();

		assertTrue(sw.toString().endsWith("\r\n\r\nOK\n"));

		server.stop();
		server.join();
	}

	@Test
	public void parseEmptyResource() throws Exception {
		Resource r = new PathResourceFactory().newResource(getClass().getResource("/jetty-empty.xml"));
		XmlConfiguration configuration = new XmlConfiguration(r);
		assertThat(configuration.configure(), equalTo("OK"));
	}

	public static class TestServlet extends HttpServlet {
		@Override
		protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			req.getSession(true);
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write("OK\n");
			resp.getWriter().close();
		}
	}

	private void map(ServletContextHandler h, String name, String[] uris) {
		ServletMapping mapping = new ServletMapping();
		mapping.setServletName(name);
		mapping.setPathSpecs(uris);
		h.getServletHandler().addServletMapping(mapping);
	}

	private String send(int port, String request, String ... headers) throws IOException {
		Socket s = new Socket();
		s.connect(new InetSocketAddress("127.0.0.1", port));

		s.getOutputStream().write((
				"GET " + request + " HTTP/1.1\r\n" +
				"Host: 127.0.0.1:" + port + "\r\n").getBytes());
		for (String header : headers) {
			s.getOutputStream().write((header + "\r\n").getBytes());
		}
		s.getOutputStream().write(("Connection: close\r\n\r\n").getBytes());

		byte[] buf = new byte[64];
		int read;
		StringWriter sw = new StringWriter();
		while ((read = s.getInputStream().read(buf)) > 0) {
			sw.append(new String(buf, 0, read));
		}
		s.close();

		return sw.toString();
	}

	private Map<String, String> extractHeaders(String response) throws IOException {
		Map<String, String> headers = new LinkedHashMap<>();
		try (BufferedReader reader = new BufferedReader(new StringReader(response))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty()) {
					break;
				}
				// I know, security when parsing headers is very important...
				String[] kv = line.split(": ");
				String header = kv[0];
				String value = String.join("", Arrays.asList(kv).subList(1, kv.length));
				headers.put(header, value);
			}
		}
		return headers;
	}

}
