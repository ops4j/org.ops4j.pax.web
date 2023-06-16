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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.OpenListener;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.protocol.http.HttpOpenListener;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.handlers.DefaultServlet;
import io.undertow.servlet.handlers.ServletChain;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.ETag;
import io.undertow.util.StatusCodes;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddedUndertowTest {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedUndertowTest.class);

	@Test
	public void undertowWithSingleContextAndServlet() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
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
	public void embeddedServerWithUndertowResourceServlet() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new DefaultServlet();

		ServletInfo servlet = Servlets.servlet("default", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet.addInitParam("directory-listing", "true");

		// with "/" mapping, io.undertow.servlet.handlers.ServletPathMatch is used with
		// io.undertow.servlet.handlers.ServletPathMatchesData.PathMatch.requireWelcomeFileMatch = true, that's why
		// resource provider is consulted already in io.undertow.servlet.handlers.ServletInitialHandler.handleRequest
		// before even invoking DefaultServlet
		servlet.addMapping("/");

		DirectBufferCache cache = new DirectBufferCache(1024, 64, 1024 * 1024);
		// this = {io.undertow.server.handlers.resource.FileResourceManager@1925}
		//   base: java.io.File  = {java.io.File@1926} "target"
		//   transferMinSize: long  = 1024 (0x400)
		//   caseSensitive: boolean  = true
		//   followLinks: boolean  = false
		//   safePaths: java.lang.String[]  = null
		ResourceManager fileResourceManager = FileResourceManager.builder()
				.setBase(Paths.get("target"))
				// taken from org.apache.catalina.webresources.AbstractResource.getETag()
				.setETagFunction(path1 -> new ETag(true, path1.toFile().length() + "-" + path1.toFile().lastModified()))
				.build();
		CachingResourceManager manager = new CachingResourceManager(1024, 1024 * 1024, cache,
				fileResourceManager, 3_600_000/*ms*/);

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.setResourceManager(manager)
				.addServlets(servlet);

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		// Undertow doesn't generate ETag by default, so it depends on what we'll set in
		// io.undertow.server.handlers.resource.PathResourceManager.Builder.setETagFunction

		String response = send(port, "/test-classes/log4j2-test.properties");
		Map<String, String> headers = extractHeaders(response);
		assertTrue(response.contains("ETag: W/"));
		assertTrue(response.contains("rootLogger.appenderRef.stdout.ref = stdout"));

		response = send(port, "/test-classes/log4j2-test.properties",
				"If-None-Match: " + headers.get("ETag"),
				"If-Modified-Since: " + headers.get("Date"));
		assertTrue(response.contains("HTTP/1.1 304"));
		assertFalse(response.contains("rootLogger.appenderRef.stdout.ref = stdout"));

		server.stop();
	}

	@Test
	public void dynamicListeners() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");
				resp.getWriter().write("OK1\n");
				resp.getWriter().close();

				// java.lang.IllegalStateException: UT010041: The servlet context has already been initialized, you can only call this method from a ServletContainerInitializer or a ServletContextListener
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
		};

		ServletInfo servlet = Servlets.servlet("default", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet.addMapping("/s1");

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.addServletExtension(new ServletExtension() {
					@Override
					public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {
						// ServletContextListener added by ServletExtension
						deploymentInfo.addListener(Servlets.listener(ServletContextListener.class, new ImmediateInstanceFactory<>(new ServletContextListener() {
							@Override
							public void contextInitialized(ServletContextEvent sce) {
								// ServletContextListener added from a listener added from extension
								// throws java.lang.RuntimeException: java.lang.IllegalArgumentException: \
								//     UT010043: Cannot add servlet context listener from a programatically added listener
//								sce.getServletContext().addListener(new ServletContextListener() {
//									@Override
//									public void contextInitialized(ServletContextEvent sce) {
//										ServletContextListener.super.contextInitialized(sce);
//									}
//								});
							}
						})));
					}
				})
				.addServletContainerInitializer(new ServletContainerInitializerInfo(ServletContainerInitializer.class,
						new ImmediateInstanceFactory<ServletContainerInitializer>(new ServletContainerInitializer() {
							@Override
							public void onStartup(Set<Class<?>> c, ServletContext ctx) {
								// ServletContextListener added from SCI - this is real "programmatic listener"
								ctx.addListener(new ServletContextListener() {
									@Override
									public void contextInitialized(ServletContextEvent sce) {
										// ServletContextListener added from a "programmatic listener"
										// throws (according to the spec)
										// java.lang.RuntimeException: java.lang.UnsupportedOperationException: \
										//     UT010042: This method cannot be called from a servlet context listener that has been added programatically
//										sce.getServletContext().addListener(new ServletContextListener() {
//											@Override
//											public void contextInitialized(ServletContextEvent sce) {
//												ServletContextListener.super.contextInitialized(sce);
//											}
//										});
									}
								});
							}
						}), null))
				// listener added from web.xml/web-fragment.xml/@WebListener
				.addListener(Servlets.listener(ServletContextListener.class, new ImmediateInstanceFactory<>(new ServletContextListener() {
					@Override
					public void contextInitialized(ServletContextEvent sce) {
						// ServletContextListener added from a listener added from web.xml
						// throws java.lang.RuntimeException: java.lang.IllegalArgumentException: \
						//     UT010043: Cannot add servlet context listener from a programatically added listener
//						sce.getServletContext().addListener(new ServletContextListener() {
//							@Override
//							public void contextInitialized(ServletContextEvent sce) {
//								ServletContextListener.super.contextInitialized(sce);
//							}
//						});
					}
				})))
				.addServlet(servlet)
				.addServlet(Servlets.servlet(DefaultServlet.class).addMapping("/"))
				.setUrlEncoding("UTF-8");

		ServletContainer container = Servlets.newContainer();
		DeploymentManager dm = container.addDeployment(deploymentInfo);
		dm.deploy();
		HttpHandler handler = dm.start();

		path.addPrefixPath("/c", handler);

		server.start();

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();

		String response;

		response = send(port, "/c/s2");
		assertTrue(response.startsWith("HTTP/1.1 404"));
		response = send(port, "/c/s1");
		assertTrue(response.endsWith("\r\n\r\nOK1\n"));
//		// call servlet added dynamically from the servlet
//		response = send(port, "/c/s2");
//		assertTrue(response.endsWith("\r\n\r\nOK2\n"));

		server.stop();
	}

	@Test
	public void undertowWithRequestWrappers() throws Exception {
		PathHandler path = Handlers.path();
		Undertow server = Undertow.builder()
				.addHttpListener(0, "0.0.0.0")
				.setHandler(path)
				.build();

		HttpServlet servletInstance = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		Filter filterInstance = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
				super.doFilter(req, res, chain);
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet.addMapping("/s1/*");

		FilterInfo filter = Servlets.filter("f1", filterInstance.getClass(), new ImmediateInstanceFactory<>(filterInstance));

		DeploymentInfo deploymentInfo = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet)
				.addFilters(filter)
				.addFilterServletNameMapping("f1", "s1", DispatcherType.REQUEST);

		deploymentInfo.addInnerHandlerChainWrapper(handler -> {
			System.out.println("1. Wrapping " + handler);
			return exchange -> {
				System.out.println("1. Invoking on " + exchange);
				handler.handleRequest(exchange);
			};
		});
		deploymentInfo.addSecurityWrapper(handler -> {
			System.out.println("2. Wrapping " + handler);
			return exchange -> {
				System.out.println("2. Invoking on " + exchange);
				handler.handleRequest(exchange);
			};
		});
		deploymentInfo.addOuterHandlerChainWrapper(handler -> {
			System.out.println("3a. Wrapping " + handler);
			return exchange -> {
				// let's replace ServletChain here
				ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
				ServletChain currentServlet = src.getCurrentServlet();
				System.out.println("3. Invoking on " + exchange);
				handler.handleRequest(exchange);
			};
		});
		// outer handler added later will be called earlier
//		deploymentInfo.addOuterHandlerChainWrapper(handler -> {
//			System.out.println("3b. Wrapping " + handler);
//			return exchange -> {
//				// let's replace ServletChain here
//				ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
//				ServletChain currentServlet = src.getCurrentServlet();
//				System.out.println("3. Invoking on " + exchange);
//				handler.handleRequest(exchange);
//			};
//		});
		deploymentInfo.addInitialHandlerChainWrapper(handler -> {
			System.out.println("4. Wrapping " + handler);
			return exchange -> {
				System.out.println("4. Invoking on " + exchange);
				handler.handleRequest(exchange);
			};
		});

		// when processing request, the stack trace will be:
		// "XNIO-1 task-1@2434" prio=5 tid=0x17 nid=NA runnable
		//  java.lang.Thread.State: RUNNABLE
		//      at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest$2.service(EmbeddedUndertowTest.java:139)
		//      at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:590)
		//      at io.undertow.servlet.handlers.ServletHandler.handleRequest(ServletHandler.java:74)
		//      at io.undertow.servlet.handlers.security.ServletSecurityRoleHandler.handleRequest(ServletSecurityRoleHandler.java:62)
		//      at io.undertow.servlet.handlers.ServletChain$1.handleRequest(ServletChain.java:68)
		//      at io.undertow.servlet.handlers.ServletDispatchingHandler.handleRequest(ServletDispatchingHandler.java:36)
		// 1.   at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest.lambda$null$0(EmbeddedUndertowTest.java:164)
		//      at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest$$Lambda$5.682910755.handleRequest(Unknown Source:-1)
		//      at io.undertow.servlet.handlers.RedirectDirHandler.handleRequest(RedirectDirHandler.java:68)
		//      at io.undertow.servlet.handlers.security.SSLInformationAssociationHandler.handleRequest(SSLInformationAssociationHandler.java:132)
		//      at io.undertow.servlet.handlers.security.ServletAuthenticationCallHandler.handleRequest(ServletAuthenticationCallHandler.java:57)
		// 2.   at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest.lambda$null$2(EmbeddedUndertowTest.java:171)
		//      at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest$$Lambda$6.765242091.handleRequest(Unknown Source:-1)
		//      at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
		//      at io.undertow.security.handlers.AbstractConfidentialityHandler.handleRequest(AbstractConfidentialityHandler.java:46)
		//      at io.undertow.servlet.handlers.security.ServletConfidentialityConstraintHandler.handleRequest(ServletConfidentialityConstraintHandler.java:64)
		//      at io.undertow.security.handlers.AuthenticationMechanismsHandler.handleRequest(AuthenticationMechanismsHandler.java:60)
		//      at io.undertow.servlet.handlers.security.CachedAuthenticatedSessionHandler.handleRequest(CachedAuthenticatedSessionHandler.java:77)
		//      at io.undertow.security.handlers.AbstractSecurityContextAssociationHandler.handleRequest(AbstractSecurityContextAssociationHandler.java:43)
		//      at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
		// 3.   at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest.lambda$null$4(EmbeddedUndertowTest.java:178)
		//      at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest$$Lambda$7.1719072416.handleRequest(Unknown Source:-1)
		//      at io.undertow.server.handlers.PredicateHandler.handleRequest(PredicateHandler.java:43)
		//      at io.undertow.servlet.handlers.ServletInitialHandler.handleFirstRequest(ServletInitialHandler.java:269)
		//      at io.undertow.servlet.handlers.ServletInitialHandler.access$100(ServletInitialHandler.java:78)
		//      at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:133)
		//      at io.undertow.servlet.handlers.ServletInitialHandler$2.call(ServletInitialHandler.java:130)
		//      at io.undertow.servlet.core.ServletRequestContextThreadSetupAction$1.call(ServletRequestContextThreadSetupAction.java:48)
		//      at io.undertow.servlet.core.ContextClassLoaderSetupAction$1.call(ContextClassLoaderSetupAction.java:43)
		//      at io.undertow.servlet.handlers.ServletInitialHandler.dispatchRequest(ServletInitialHandler.java:249)
		//      at io.undertow.servlet.handlers.ServletInitialHandler.access$000(ServletInitialHandler.java:78)
		//      at io.undertow.servlet.handlers.ServletInitialHandler$1.handleRequest(ServletInitialHandler.java:99)
		//      at io.undertow.server.Connectors.executeRootHandler(Connectors.java:376)
		//      at io.undertow.server.HttpServerExchange$1.run(HttpServerExchange.java:830)
		//      at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
		//      at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
		//      at java.lang.Thread.run(Thread.java:748)

		// the 4th wrapper is called initially (before internal dispatching):
		// "XNIO-1 I/O-2@2220" prio=5 tid=0xf nid=NA runnable
		//  java.lang.Thread.State: RUNNABLE
		//      at io.undertow.servlet.handlers.ServletInitialHandler.handleRequest(ServletInitialHandler.java:172)
		// 4.   at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest.lambda$null$6(EmbeddedUndertowTest.java:185)
		//      at org.ops4j.pax.web.service.undertow.internal.EmbeddedUndertowTest$$Lambda$8.1520387953.handleRequest(Unknown Source:-1)
		//      at io.undertow.server.handlers.HttpContinueReadHandler.handleRequest(HttpContinueReadHandler.java:65)
		//      at io.undertow.server.handlers.URLDecodingHandler.handleRequest(URLDecodingHandler.java:68)
		//      at io.undertow.server.handlers.PathHandler.handleRequest(PathHandler.java:91)
		//      at io.undertow.server.Connectors.executeRootHandler(Connectors.java:376)
		//      at io.undertow.server.protocol.http.HttpReadListener.handleEventWithNoRunningRequest(HttpReadListener.java:255)
		//      at io.undertow.server.protocol.http.HttpReadListener.handleEvent(HttpReadListener.java:136)
		//      at io.undertow.server.protocol.http.HttpOpenListener.handleEvent(HttpOpenListener.java:162)
		//      at io.undertow.server.protocol.http.HttpOpenListener.handleEvent(HttpOpenListener.java:100)
		//      at io.undertow.server.protocol.http.HttpOpenListener.handleEvent(HttpOpenListener.java:57)
		//      at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
		//      at org.xnio.ChannelListeners$10.handleEvent(ChannelListeners.java:291)
		//      at org.xnio.ChannelListeners$10.handleEvent(ChannelListeners.java:286)
		//      at org.xnio.ChannelListeners.invokeChannelListener(ChannelListeners.java:92)
		//      at org.xnio.nio.QueuedNioTcpServer$1.run(QueuedNioTcpServer.java:129)
		//      at org.xnio.nio.WorkerThread.safeRun(WorkerThread.java:582)
		//      at org.xnio.nio.WorkerThread.run(WorkerThread.java:466)

		// why 2 separate invocations?
		// 1) io.undertow.server.Connectors.executeRootHandler() is called directly from Open/Read listener
		// 2) then io.undertow.servlet.handlers.ServletInitialHandler.handleRequest(), after setting proper
		//    io.undertow.servlet.handlers.ServletRequestContext.ATTACHMENT_KEY, dispatches the request further
		//    using task thread

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
				.setRootHandler(path)
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
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		ServletInfo servlet = Servlets.servlet("s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
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
		// deploy() creates new io.undertow.servlet.spec.ServletContextImpl(), so it seems it can
		// be called only once
		dm.deploy();
		HttpHandler handler = dm.start();

		server.start();

		path.addPrefixPath("/c1", handler);

		int port = ((InetSocketAddress) server.getListenerInfo().get(0).getAddress()).getPort();
		LOG.info("Local port after start: {}", port);

		String response;

		response = send(port, "/c1/s1");
		assertTrue(response.endsWith("| /c1 | /s1 | null |"));

		response = send(port, "/c2/s1");
		assertTrue(response.contains("HTTP/1.1 404"));

		response = send(port, "/c1/s2");
		assertTrue(response.contains("HTTP/1.1 404"));

		ServletInfo servlet2 = Servlets.servlet("s2", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet2.addMapping("/s2/*");
		// this is not necessary. Not even reasonable, because the deployment info is originally cloned inside
		// the deployment
//		deploymentInfo.addServlet(servlet2);
		// this is where new definition has to be added
		dm.getDeployment().getDeploymentInfo().addServlet(servlet2);
		dm.getDeployment().getServlets().addServlet(servlet2);

		// addServlet() has cleared io.undertow.servlet.handlers.ServletPathMatches.pathMatchCache LRU cache
		// and io.undertow.servlet.handlers.ServletPathMatches.data mapping
		// so io.undertow.servlet.handlers.ServletPathMatches.setupServletChains() will be called again

		response = send(port, "/c1/s2");
		assertTrue(response.endsWith("| /c1 | /s2 | null |"));

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

		int port = ((InetSocketAddress) server.getLocalAddress()).getPort();

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
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request: {}", req.toString());
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("UTF-8");

				String response = String.format("| %s | %s | %s |", req.getContextPath(), req.getServletPath(), req.getPathInfo());
				resp.getWriter().write(response);
				resp.getWriter().close();
			}
		};

		// io.undertow.servlet.api.ServletContainer is THE container in Undertow that maps context paths (and names)
		// into "deployments" (contexts?) represented by io.undertow.servlet.api.DeploymentManager
		// from transformation perspective, user adds a "deployment" as io.undertow.servlet.api.DeploymentInfo
		// and it is turned into io.undertow.servlet.api.DeploymentManager (which holds the clone of original
		// "deployment info")
		ServletContainer container = Servlets.newContainer();

		// another example of "info" like class. "servlet info" is added to "deployment info" (directly, not as clone)
		// which means "servlet is registered inside servlet context"
		ServletInfo servlet = Servlets.servlet("c1s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet.addMapping("/s1/*");

		// "deployment info" represents a full information about single "servlet context" which can simply be
		// treated as "JakartaEE web application" with single context path
		// this info is in 1:1 relation with single web.xml descriptor
		DeploymentInfo deploymentInfo1 = Servlets.deployment()
				.setClassLoader(this.getClass().getClassLoader())
				.setContextPath("/c1")
				.setDisplayName("Default Application")
				.setDeploymentName("d1")
				.setUrlEncoding("UTF-8")
				.addServlets(servlet);

		// we start the server already, it can already handle requests, but its root handler doesn't contain
		// any mapping inside io.undertow.server.handlers.PathHandler
		server.start();

		// now the transformation time.
		// io.undertow.servlet.api.ServletContainer.addDeployment() turns a "deployment info" into
		// "deployment manager", which can be treated as "physical deployment" with some lifecycle.
		// added "deployment info" is:
		//  - cloned
		//  - tracked under its unique name
		//  - tracked under its unique context path
		//  - returned as io.undertow.servlet.api.DeploymentManager object that controls the lifecycle of
		//    associated "physical deployment"
		DeploymentManager dm1 = container.addDeployment(deploymentInfo1);
		assertThat(dm1.getState()).isEqualTo(DeploymentManager.State.UNDEPLOYED);

		// "deploying" clones the already cloned "deployment info" again and turns it into "physical deployment"
		// represented by io.undertow.servlet.api.Deployment, which allows read access to various aspects
		// of "web application".
		// the problem with Undertow is that while we can add new servlets to existing "deployment", we can't
		// remove them...
		// "deploy" does few important things:
		//  - creates instance of io.undertow.servlet.spec.ServletContextImpl (THE jakarta.servlet.ServletContext)
		//  - creates instance of io.undertow.servlet.core.DeploymentImpl
		//  - prepares all the "web elements" by turning "info" into "physical representation" of e.g., servlet
		//     - e.g., io.undertow.servlet.api.ServletInfo is turned into io.undertow.servlet.core.ManagedServlet
		//       and io.undertow.servlet.handlers.ServletHandler
		//  - starting from singleton io.undertow.servlet.handlers.ServletDispatchingHandler.INSTANCE, handlers
		//    declared in deployment info (and other configuration) are created (wrappers of wrapeprs of ...)
		//  - one of the important (fixed) handlers is io.undertow.servlet.handlers.ServletInitialHandler that
		//    puts an exchange attachment in the form of io.undertow.servlet.handlers.ServletRequestContext
		//  - this attachment is then taken in ServletDispatchingHandler and invoked
		//  - the final handler is put as io.undertow.servlet.core.DeploymentImpl.initialHandler and then
		//    returned from (as-is) io.undertow.servlet.api.DeploymentManager.start()
		dm1.deploy();
		assertThat(dm1.getState()).isEqualTo(DeploymentManager.State.DEPLOYED);

		// "start" starts all lifecycle objects (servlets, filters, listeners)
		HttpHandler handler = dm1.start();
		assertThat(dm1.getState()).isEqualTo(DeploymentManager.State.STARTED);

		path.addPrefixPath("/c1", handler);

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

		ServletInfo servlet2 = Servlets.servlet("c2s1", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
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

		ServletInfo servlet3 = Servlets.servlet("c1s2", servletInstance.getClass(), new ImmediateInstanceFactory<>(servletInstance));
		servlet3.addMapping("/s2/*");

		response = send(port, "/c1/s2");
		assertTrue(response.startsWith("HTTP/1.1 404"));

		// either removal and addition of the same deployment:
//		deploymentInfo1.addServlet(servlet3);
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

		response = send(port, "/c2/s1");
		assertTrue(response.endsWith("| /c2 | /s1 | null |"));

		response = send(port, "/c1/s2");
		assertTrue(response.endsWith("| /c1 | /s2 | null |"));

		server.stop();
	}

	private String send(int port, String request, String... headers) throws IOException {
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
				if (line.trim().equals("")) {
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
