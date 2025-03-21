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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.PushBuilder;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.coyote.http2.PublicHpackDecoder;
import org.apache.coyote.http2.PublicHpackEncoder;
import org.apache.tomcat.util.http.MimeHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedTomcatHttp2Test {

	public static final Logger LOG = LoggerFactory.getLogger(EmbeddedTomcatHttp2Test.class);

	// https://httpwg.org/specs/rfc7540.html
	// https://sookocheff.com/post/networking/how-does-http-2-work/

	private PublicHpackDecoder decoder;
	private Map<Integer, String> responses;

	@BeforeEach
	public void resetState() {
		decoder = new PublicHpackDecoder();
		responses = new HashMap<>();
	}

	@Test
	public void http11NioExchange() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
//		connector.setPort(0);
		connector.setPort(8123);
		connector.addUpgradeProtocol(new Http2Protocol());
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				if ("/index.txt".equals(req.getPathInfo())) {
					// normal request
					LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
					resp.setContentType("text/plain");
					resp.setCharacterEncoding("UTF-8");
					resp.getWriter().write("OK\n");
					resp.getWriter().close();
				}
			}
		};

		Context c1 = new StandardContext();
		c1.setName("c1");
		c1.setPath("");
		c1.setMapperContextRootRedirectEnabled(false);
		c1.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				c1.setConfigured(true);
			}
		});
		host.addChild(c1);

		Wrapper wrapper1 = new StandardWrapper();
		wrapper1.setServlet(servlet);
		wrapper1.setName("servlet");

		c1.addChild(wrapper1);
		c1.addServletMappingDecoded("/test/*", wrapper1.getName(), false);

		server.start();

		int port = connector.getLocalPort();
		LOG.info("Local port after start: {}", port);

		AtomicInteger count = new AtomicInteger(1);
		ExecutorService pool = Executors.newFixedThreadPool(4, r -> new Thread(r, "pool-thread-" + count.getAndIncrement()));

		Selector selector = Selector.open();

		SocketChannel sc = SocketChannel.open();
		// Before a channel is registered with a selector, it _must_ be in non-blocking mode
		sc.configureBlocking(false);

		// https://stackoverflow.com/questions/6540346/java-solaris-nio-op-connect-problem
		// https://stackoverflow.com/questions/19740436/does-order-of-operation-and-registration-matter-on-selectable-channel
		//  - registration for OP_CONNECTION should be done ONLY after sc.connect() returns false
		//  - then after key.isConnectable() returns true, sc.finishConnect() has to be called
		//    but it still may return false
		//  - only after sc.finishConnect() returns true, OP_CONNECT may be removed from interested ops

		SelectionKey key = null;
		int selected;

		boolean connected = sc.connect(new InetSocketAddress("127.0.0.1", port));
		LOG.info("Connected: {}", connected);
		if (!connected) {
			key = sc.register(selector, SelectionKey.OP_CONNECT);
			while (true) {
				boolean done = false;
				selected = selector.select(100L);
				if (selected > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey k : keys) {
						if (k.isConnectable()) {
							boolean finished = ((SocketChannel) k.channel()).finishConnect();
							LOG.info("Finished connecting: {}", finished);
							if (finished) {
								k.interestOps(k.interestOps() & ~SelectionKey.OP_CONNECT);
								done = true;
							}
						}
					}
				}
				if (done) {
					break;
				}
			}
		}

		// we're connected, we can prepare for writing
		ByteBuffer buffer = ByteBuffer.wrap(new byte[4096]);
		if (key == null) {
			key = sc.register(selector, SelectionKey.OP_WRITE);
		} else {
			key.interestOps(SelectionKey.OP_WRITE);
		}

		buffer.put("GET /test/index.txt?x=y&a=b HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
		buffer.put("Host: 127.0.0.1\r\n".getBytes(StandardCharsets.UTF_8));
		buffer.put("Connection: close\r\n\r\n".getBytes(StandardCharsets.UTF_8));

		while (true) {
			boolean done = false;
			selected = selector.select(100L);
			if (selected > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey k : keys) {
					if (k.isWritable()) {
						buffer.flip();
						int written = ((SocketChannel) k.channel()).write(buffer);
						LOG.info("Written {} bytes", written);
						done = true;
						k.interestOps(k.interestOps() & ~SelectionKey.OP_WRITE);
					}
				}
			}
			if (done) {
				break;
			}
		}

		buffer.clear();
		key.interestOps(SelectionKey.OP_READ);

		while (true) {
			boolean done = false;
			selected = selector.select(100L);
			if (selected > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey k : keys) {
					if (k.isReadable()) {
						int read = ((SocketChannel) k.channel()).read(buffer);
						LOG.info("Read {} bytes", read);
						done = read > 0;
					}
				}
			}
			if (done) {
				break;
			}
		}

		buffer.flip();
		LOG.info("Response\n{}", new String(buffer.array(), 0, buffer.limit()));

		// end of processing
		key.cancel();

		selector.close();

		server.stop();
		server.destroy();
	}

	@Test
	public void http2NioExchange() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
//		connector.setPort(0);
		connector.setPort(8123);
		connector.addUpgradeProtocol(new Http2Protocol());
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
				if (!"/index.html".equals(req.getPathInfo())) {
					// normal request
					resp.setCharacterEncoding("UTF-8");
					if (req.getPathInfo().endsWith("css")) {
						resp.setContentType("text/css");
						resp.addHeader("X-Request-CSS", req.getHeader("X-Request-P1"));
						resp.getWriter().write("body { margin: 0 }\n");
					} else if (req.getPathInfo().endsWith("js")) {
						resp.setContentType("text/javascript");
						resp.addHeader("X-Request-JS", req.getHeader("X-Request-P2"));
						resp.getWriter().write("window.alert(\"hello world\");\n");
					}
					resp.getWriter().close();
				} else {
					// first - normal data
					resp.setContentType("text/plain");
					resp.setCharacterEncoding("UTF-8");
					resp.addHeader("X-Request", req.getRequestURI());
					resp.getWriter().write("OK\n");
					// request with PUSH_PROMISE: https://httpwg.org/specs/rfc7540.html#PUSH_PROMISE
					PushBuilder pushBuilder = req.newPushBuilder();
					if (pushBuilder != null) {
						pushBuilder.path("test/default.css").addHeader("X-Request-P1", req.getRequestURI()).push();
						pushBuilder.path("test/app.js").removeHeader("X-Request-P1").addHeader("X-Request-P2", req.getRequestURI()).push();
					}
					resp.getWriter().close();
				}
			}
		};

		Context c1 = new StandardContext();
		c1.setName("c1");
		c1.setPath("");
		c1.setMapperContextRootRedirectEnabled(false);
		c1.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				c1.setConfigured(true);
			}
		});
		host.addChild(c1);

		Wrapper wrapper1 = new StandardWrapper();
		wrapper1.setServlet(servlet);
		wrapper1.setName("servlet");

		c1.addChild(wrapper1);
		c1.addServletMappingDecoded("/test/*", wrapper1.getName(), false);

		server.start();

		int port = connector.getLocalPort();
		LOG.info("Local port after start: {}", port);

		AtomicInteger count = new AtomicInteger(1);
		ExecutorService pool = Executors.newFixedThreadPool(4, r -> new Thread(r, "pool-thread-" + count.getAndIncrement()));

		Selector selector = Selector.open();

		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		SelectionKey key = null;
		int selected;

		boolean connected = sc.connect(new InetSocketAddress("127.0.0.1", port));
		LOG.info("Connected: {}", connected);
		if (!connected) {
			key = sc.register(selector, SelectionKey.OP_CONNECT);
			while (true) {
				boolean done = false;
				selected = selector.select(100L);
				if (selected > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey k : keys) {
						if (k.isConnectable()) {
							boolean finished = ((SocketChannel) k.channel()).finishConnect();
							LOG.info("Finished connecting: {}", finished);
							if (finished) {
								k.interestOps(k.interestOps() & ~SelectionKey.OP_CONNECT);
								done = true;
							}
						}
					}
				}
				if (done) {
					break;
				}
			}
		}

		// we're connected, we can prepare for writing
		ByteBuffer buffer = ByteBuffer.wrap(new byte[4096]);
		if (key == null) {
			key = sc.register(selector, SelectionKey.OP_WRITE);
		} else {
			key.interestOps(SelectionKey.OP_WRITE);
		}

		buffer.put("GET /test/index.html?x=y&a=b HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
		buffer.put("Host: 127.0.0.1\r\n".getBytes(StandardCharsets.UTF_8));
		// https://httpwg.org/specs/rfc7540.html#rfc.section.3.2.1
		buffer.put("Connection: Upgrade, HTTP2-Settings\r\n".getBytes(StandardCharsets.UTF_8));
		buffer.put("Upgrade: h2c\r\n".getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream settings = new DataOutputStream(baos);
		// SETTINGS_HEADER_TABLE_SIZE (0x1)
		settings.writeShort(0x01);
		settings.writeInt(0x1000); // 4096 by default
		// SETTINGS_ENABLE_PUSH (0x2)
		settings.writeShort(0x02);
		settings.writeInt(0x01); // 1 by default, 0 disables PUSH_PROMISE frame
		// SETTINGS_MAX_CONCURRENT_STREAMS (0x3)
		settings.writeShort(0x03);
		settings.writeInt(0xFF); // no default value. recommended >100
		// SETTINGS_INITIAL_WINDOW_SIZE (0x4)
		settings.writeShort(0x04);
		settings.writeInt(0xFFFE); // defaults to 65535
		// SETTINGS_MAX_FRAME_SIZE (0x5)
		settings.writeShort(0x05);
		settings.writeInt(0x4000); // defaults to 16384 (2^14)
		// SETTINGS_MAX_HEADER_LIST_SIZE (0x6)
		settings.writeShort(0x06);
		settings.writeInt(0x8000); // no default value.
		settings.close();
		buffer.put(("HTTP2-Settings: " + new String(Base64.getUrlEncoder().encode(baos.toByteArray())) + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));

		// Starting HTTP/2 for "http" URIs - https://httpwg.org/specs/rfc7540.html#discover-http
		LOG.info("===== Sending GET HTTP/1.1 request with Connection: Upgrade, HTTP2-Settings");
		LOG.info("Request\n{}", new String(buffer.array(), 0, buffer.position()));
		send(selector, key, buffer);

		// now we should get "HTTP/1.1 101 Switching Protocols", but we may get some HTTP/2 frames immediately, but in more reads.
		LOG.info("===== Receiving initial response");
		receive(selector, key, buffer);

		String fullResponse = new String(buffer.array(), 0, buffer.limit());
		String response = fullResponse;
		int rnrn = response.indexOf("\r\n\r\n");
		if (rnrn > 0) {
			response = response.substring(0, rnrn + 4);
		}
		LOG.info("Response\n{}", response);
		assertThat(fullResponse).startsWith("HTTP/1.1 101 \r\n");
		buffer.position(rnrn + 4);
		boolean remains = true;
		while (remains) {
			remains = decodeFrame(buffer, true);
		}

		// Jetty already sends the response. In Tomcat, we need the magic "PRI * HTTP/2.0" string.

		// https://httpwg.org/specs/rfc7540.html#ConnectionHeader
		// [...] the connection preface starts with the string PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n).

		buffer.clear();
		buffer.put("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
		// This sequence MUST be followed by a SETTINGS frame (Section 6.5), which MAY be empty
		buffer.put(new byte[] {
				0x00, 0x00, 0x00,      // length
				0x04,                  // type = SETTINGS
				0x00,                  // flags
				0x00, 0x00, 0x00, 0x00 // stream identifier
		});

		// Starting HTTP/2 for "http" URIs - https://httpwg.org/specs/rfc7540.html#discover-http
		LOG.info("===== Sending PRI * HTTP/2.0 request");
		send(selector, key, buffer);

		// even at this stage, we should have everything including response HEADERS and DATA frames
		// this doesn't mean we shouldn't ACK server's SETTINGS
		// request was encoded in initial upgrade request

		// ACK server SETTINGS
		buffer.clear();
		// This sequence MUST be followed by a SETTINGS frame (Section 6.5), which MAY be empty
		buffer.put(new byte[] {
				0x00, 0x00, 0x00,      // length
				0x04,                  // type = SETTINGS
				0x01,                  // flags = ACK
				0x00, 0x00, 0x00, 0x00 // stream identifier
		});
		LOG.info("===== Sending ACK for sid=0 with SETTINGS response");
		send(selector, key, buffer);

		LOG.info("===== Receiving response");
		receive(selector, key, buffer);
		remains = true;
		while (remains) {
			remains = decodeFrame(buffer, true);
		}

		// end of processing
		key.cancel();

		selector.close();

		server.stop();
		server.destroy();

		// indexed by HTTP/2 Stream ID
		assertThat(responses.get(1)).isEqualTo("OK\n");
		// https://github.com/apache/tomcat/commit/d28d6836b80d0709c56a3ab24d515788498c760e
//		assertThat(responses.get(2)).isEqualTo("body { margin: 0 }\n");
//		assertThat(responses.get(4)).isEqualTo("window.alert(\"hello world\");\n");
	}

	@Test
	public void http2NioExchangeWithDirectUpgrade() throws Exception {
		Server server = new StandardServer();
		server.setCatalinaBase(new File("target"));

		Service service = new StandardService();
		service.setName("Catalina");
		server.addService(service);

		Executor executor = new StandardThreadExecutor();
		service.addExecutor(executor);

		Connector connector = new Connector("HTTP/1.1");
//		connector.setPort(0);
		connector.setPort(8123);
		connector.addUpgradeProtocol(new Http2Protocol());
		service.addConnector(connector);

		Engine engine = new StandardEngine();
		engine.setName("Catalina");
		engine.setDefaultHost("localhost");
		service.setContainer(engine);

		Host host = new StandardHost();
		host.setName("localhost");
		host.setAppBase(".");
		engine.addChild(host);

		Servlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				LOG.info("Handling request {} from {}:{}", req.getRequestURI(), req.getRemoteAddr(), req.getRemotePort());
				if (!"/index.html".equals(req.getPathInfo())) {
					// normal request
					resp.setCharacterEncoding("UTF-8");
					if (req.getPathInfo() != null && req.getPathInfo().endsWith("css")) {
						resp.setContentType("text/css");
						resp.addHeader("X-Request-CSS", req.getHeader("X-Request-P1"));
						resp.getWriter().write("body { margin: 0 }\n");
					} else if (req.getPathInfo() != null && req.getPathInfo().endsWith("js")) {
						resp.setContentType("text/javascript");
						resp.addHeader("X-Request-JS", req.getHeader("X-Request-P2"));
						resp.getWriter().write("window.alert(\"hello world\");\n");
					}
					resp.getWriter().close();
				} else {
					// first - normal data
					resp.setContentType("text/plain");
					resp.setCharacterEncoding("UTF-8");
					resp.addHeader("X-Request", req.getRequestURI());
					resp.getWriter().write("OK\n");
					// request with PUSH_PROMISE: https://httpwg.org/specs/rfc7540.html#PUSH_PROMISE
					PushBuilder pushBuilder = req.newPushBuilder();
					if (pushBuilder != null) {
						pushBuilder.path("test/default.css").addHeader("X-Request-P1", req.getRequestURI()).push();
						pushBuilder.path("test/app.js").removeHeader("X-Request-P1").addHeader("X-Request-P2", req.getRequestURI()).push();
					}
					resp.getWriter().close();
				}
			}
		};

		Context c1 = new StandardContext();
		c1.setName("c1");
		c1.setPath("");
		c1.setMapperContextRootRedirectEnabled(false);
		c1.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				c1.setConfigured(true);
			}
		});
		host.addChild(c1);

		Wrapper wrapper1 = new StandardWrapper();
		wrapper1.setServlet(servlet);
		wrapper1.setName("servlet");

		c1.addChild(wrapper1);
		c1.addServletMappingDecoded("/test/*", wrapper1.getName(), false);

		server.start();

		int port = connector.getLocalPort();
		LOG.info("Local port after start: {}", port);

		AtomicInteger count = new AtomicInteger(1);
		ExecutorService pool = Executors.newFixedThreadPool(4, r -> new Thread(r, "pool-thread-" + count.getAndIncrement()));

		Selector selector = Selector.open();

		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		SelectionKey key = null;
		int selected;

		boolean connected = sc.connect(new InetSocketAddress("127.0.0.1", port));
		LOG.info("Connected: {}", connected);
		if (!connected) {
			key = sc.register(selector, SelectionKey.OP_CONNECT);
			while (true) {
				boolean done = false;
				selected = selector.select(100L);
				if (selected > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey k : keys) {
						if (k.isConnectable()) {
							boolean finished = ((SocketChannel) k.channel()).finishConnect();
							LOG.info("Finished connecting: {}", finished);
							if (finished) {
								k.interestOps(k.interestOps() & ~SelectionKey.OP_CONNECT);
								done = true;
							}
						}
					}
				}
				if (done) {
					break;
				}
			}
		}

		// we're connected, we can prepare for writing
		ByteBuffer buffer = ByteBuffer.wrap(new byte[4096]);
		if (key == null) {
			key = sc.register(selector, SelectionKey.OP_WRITE);
		} else {
			key.interestOps(SelectionKey.OP_WRITE);
		}

		// https://httpwg.org/specs/rfc7540.html#ConnectionHeader
		// [...] the connection preface starts with the string PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n).

		buffer.clear();
		buffer.put("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8));
		// This sequence MUST be followed by a SETTINGS frame (Section 6.5), which MAY be empty
		buffer.put(new byte[] {
				0x00, 0x00, 0x24,      // length (6*6)
				0x04,                  // type = SETTINGS
				0x00,                  // flags
				0x00, 0x00, 0x00, 0x00 // stream identifier
		});

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream settings = new DataOutputStream(baos);
		// SETTINGS_HEADER_TABLE_SIZE (0x1)
		settings.writeShort(0x01);
		settings.writeInt(0x1000); // 4096 by default
		// SETTINGS_ENABLE_PUSH (0x2)
		settings.writeShort(0x02);
		settings.writeInt(0x01); // 1 by default, 0 disables PUSH_PROMISE frame
		// SETTINGS_MAX_CONCURRENT_STREAMS (0x3)
		settings.writeShort(0x03);
		settings.writeInt(0xFF); // no default value. recommended >100
		// SETTINGS_INITIAL_WINDOW_SIZE (0x4)
		settings.writeShort(0x04);
		settings.writeInt(0xFFFE); // defaults to 65535
		// SETTINGS_MAX_FRAME_SIZE (0x5)
		settings.writeShort(0x05);
		settings.writeInt(0x4000); // defaults to 16384 (2^14)
		// SETTINGS_MAX_HEADER_LIST_SIZE (0x6)
		settings.writeShort(0x06);
		settings.writeInt(0x8000); // no default value.
		settings.close();
		buffer.put(baos.toByteArray(), 0, 36);

		// Starting HTTP/2 for "http" URIs - https://httpwg.org/specs/rfc7540.html#discover-http
		LOG.info("===== Sending PRI * HTTP/2.0 request");
		send(selector, key, buffer);

		// we should NOT get any HTTP/1.1 response - just HTTP/2 frames - most probably SETTINGS and WINDOW_UPDATE
		LOG.info("===== Receiving response");
		receive(selector, key, buffer);
		boolean remains = true;
		while (remains) {
			remains = decodeFrame(buffer, true);
		}

		// ACK server SETTINGS
		buffer.clear();
		// This sequence MUST be followed by a SETTINGS frame (Section 6.5), which MAY be empty
		buffer.put(new byte[] {
				0x00, 0x00, 0x00,      // length
				0x04,                  // type = SETTINGS
				0x01,                  // flags = ACK
				0x00, 0x00, 0x00, 0x00 // stream identifier
		});
		LOG.info("===== Sending ACK for sid=0 with SETTINGS response");
		send(selector, key, buffer);

		// With "PRI * HTTP/2.0" there's no actual request sent, so we have to prepare one
		buffer.clear();
		buffer.put(new byte[] {
				0x00, 0x00, 0x42,      // length - to be calculated
				0x01,                  // type = HEADERS
				0x04 | 0x01,           // flags - END_HEADERS | END_STREAM
				0x00, 0x00, 0x00, 0x01 // stream identifier - arbitrary, should be taken from a sequence
		});
		PublicHpackEncoder encoder = new PublicHpackEncoder();
		MimeHeaders headers = new MimeHeaders();
		headers.addValue(":method").setString("GET");
		headers.addValue(":scheme").setString("http");
		headers.addValue(":path").setString("/test/index.html?x=y&a=b");
		headers.addValue("host").setString("127.0.0.1");
		int p1 = buffer.position();
		encoder.encode(headers, buffer);
		// assume it's one byte
		buffer.array()[2] = (byte) (buffer.position() - p1);

		send(selector, key, buffer);

		receive(selector, key, buffer);
		remains = true;
		while (remains) {
			remains = decodeFrame(buffer, true);
		}

		// end of processing
		key.cancel();

		selector.close();

		server.stop();
		server.destroy();

		// indexed by HTTP/2 Stream ID
		assertThat(responses.get(1)).isEqualTo("OK\n");
		// https://github.com/apache/tomcat/commit/d28d6836b80d0709c56a3ab24d515788498c760e
//		assertThat(responses.get(2)).isEqualTo("body { margin: 0 }\n");
//		assertThat(responses.get(4)).isEqualTo("window.alert(\"hello world\");\n");
	}

	private void send(Selector selector, SelectionKey key, ByteBuffer buffer) throws IOException {
		int selected;
		key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		while (true) {
			boolean done = false;
			selected = selector.select(100L);
			if (selected > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey k : keys) {
					if (k.isWritable()) {
						buffer.flip();
						int written = ((SocketChannel) k.channel()).write(buffer);
						LOG.info("Written {} bytes", written);
						done = true;
						k.interestOps(k.interestOps() & ~SelectionKey.OP_WRITE);
					}
				}
				selector.selectedKeys().clear();
			}
			if (done) {
				break;
			}
		}
		buffer.clear();
	}

	private void receive(Selector selector, SelectionKey key, ByteBuffer buffer) throws IOException {
		int selected;
		key.interestOps(SelectionKey.OP_READ);

		while (true) {
			selected = selector.select(200L);
			if (selected > 0) {
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey k : keys) {
					if (k.isReadable()) {
						while (true) {
							int read = ((SocketChannel) k.channel()).read(buffer);
							if (read <= 0) {
								break;
							}
							LOG.info("Read {} bytes", read);
						}

					}
				}
				// this is needed if we want to read more!
				selector.selectedKeys().clear();
				continue;
			}
			break;
		}

		buffer.flip();
	}

	// https://httpwg.org/specs/rfc7540.html#FramingLayer
	private boolean decodeFrame(ByteBuffer buffer, boolean incoming) throws IOException {
		if (!buffer.hasRemaining()) {
			return false;
		}
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer.array(), buffer.position(), buffer.limit()));
		// 24 bit length
		int length = 0;
		int b1 = dis.readUnsignedByte();
		int b2 = dis.readUnsignedByte();
		int b3 = dis.readUnsignedByte();
		length |= b1 << 16;
		length |= b2 << 8;
		length |= b3;
		// 8 bit type - The frame type determines the format and semantics of the frame
		byte type = dis.readByte();
		// 8 bit flags - specific to the frame type
		byte flags = dis.readByte();
		int sid = dis.readInt();
		byte[] payload = new byte[length];
		int read = dis.read(payload, 0, length);
		if (read >= 0) {
			assertThat(read).isEqualTo(length);
		}
		buffer.position(buffer.position() + 3 + 1 + 1 + 4 + length);

		DataInputStream fdis = new DataInputStream(new ByteArrayInputStream(payload, 0, length));

		switch (type) {
			case 0x00: { // DATA https://httpwg.org/specs/rfc7540.html#DATA
				if (incoming) {
					LOG.info("< Received DATA frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending DATA frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				boolean endStream = (flags & 0x01) != 0;
				boolean padded = (flags & 0x08) != 0;
				int offset = 0;
				if (padded) {
					offset++;
				}
				String data = new String(payload, offset, length - offset);
				LOG.info(" - DATA: >>>{}<<<", data);
				responses.put(sid, data);
				break;
			}
			case 0x01: { // HEADERS https://httpwg.org/specs/rfc7540.html#HEADERS
				if (incoming) {
					LOG.info("< Received HEADERS frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending HEADERS frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				boolean endStream = (flags & 0x01) != 0;
				boolean last = (flags & 0x04) != 0;
				boolean padded = (flags & 0x08) != 0;
				boolean priority = (flags & 0x20) != 0;
				byte padLength = padded ? fdis.readByte() : 0;
				int streamDependency = priority ? fdis.readInt() : 0;
				int weight = priority ? (int) fdis.readByte() : 0;
				// https://httpwg.org/specs/rfc7540.html#HeaderBlock
				// see org.eclipse.jetty.http2.client.HTTP2ClientConnectionFactory.newConnection
				int offset = 0;
				if (padded) {
					offset++;
				}
				if (priority) {
					offset += 5;
				}
				decoder.reset();
				decoder.decode(ByteBuffer.wrap(payload, offset, length - offset));
				for (Map.Entry<String, String> f : decoder.getHeaders().entrySet()) {
					LOG.info(" - HEADERS::\"{}\": {}", f.getKey(), f.getValue());
				}
				break;
			}
			case 0x02: { // PRIORITY https://httpwg.org/specs/rfc7540.html#PRIORITY
				if (incoming) {
					LOG.info("< Received PRIORITY frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending PRIORITY frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				break;
			}
			case 0x03: { // RST_STREAM https://httpwg.org/specs/rfc7540.html#RST_STREAM
				if (incoming) {
					LOG.info("< Received RST_STREAM frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending RST_STREAM frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				break;
			}
			case 0x04: { // SETTINGS https://httpwg.org/specs/rfc7540.html#SETTINGS
				if (incoming) {
					LOG.info("< Received SETTINGS frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending SETTINGS frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				int c = 0;
				while (c < length) {
					short id = fdis.readShort();
					int v = fdis.readInt();
					c += 6;
					switch (id) {
						case 0x01: { // SETTINGS_HEADER_TABLE_SIZE
							LOG.info(" - SETTINGS::SETTINGS_HEADER_TABLE_SIZE: {}", v);
							break;
						}
						case 0x02: { // SETTINGS_ENABLE_PUSH
							LOG.info(" - SETTINGS::SETTINGS_ENABLE_PUSH: {}", v);
							break;
						}
						case 0x03: { // SETTINGS_MAX_CONCURRENT_STREAMS
							LOG.info(" - SETTINGS::SETTINGS_MAX_CONCURRENT_STREAMS: {}", v);
							break;
						}
						case 0x04: { // SETTINGS_INITIAL_WINDOW_SIZE
							LOG.info(" - SETTINGS::SETTINGS_INITIAL_WINDOW_SIZE: {}", v);
							break;
						}
						case 0x05: { // SETTINGS_MAX_FRAME_SIZE
							LOG.info(" - SETTINGS::SETTINGS_MAX_FRAME_SIZE: {}", v);
							break;
						}
						case 0x06: { // SETTINGS_MAX_HEADER_LIST_SIZE
							LOG.info(" - SETTINGS::SETTINGS_MAX_HEADER_LIST_SIZE: {}", v);
							break;
						}
						default:
							break;
					}
				}
				break;
			}
			case 0x05: { // PUSH_PROMISE https://httpwg.org/specs/rfc7540.html#PUSH_PROMISE
				boolean last = (flags & 0x04) != 0;
				boolean padded = (flags & 0x08) != 0;
				byte padLength = padded ? fdis.readByte() : 0;
				// promised stream ID
				int psid = fdis.readInt();
				int offset = 4;
				if (padded) {
					offset++;
				}
				if (incoming) {
					LOG.info("< Received PUSH_PROMISE frame (flags: {}, sid: {}, length: {}, psid: {})", flags, sid, length, psid);
				} else {
					LOG.info("> Sending PUSH_PROMISE frame (flags: {}, sid: {}, length: {}, psid: {})", flags, sid, length, psid);
				}
				decoder.reset();
				decoder.decode(ByteBuffer.wrap(payload, offset, length - offset));
				for (Map.Entry<String, String> f : decoder.getHeaders().entrySet()) {
					LOG.info(" - HEADERS::\"{}\": {}", f.getKey(), f.getValue());
				}
				break;
			}
			case 0x06: { // PING https://httpwg.org/specs/rfc7540.html#PING
				if (incoming) {
					LOG.info("< Received PING frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending PING frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				break;
			}
			case 0x07: { // GOAWAY https://httpwg.org/specs/rfc7540.html#GOAWAY
				if (incoming) {
					LOG.info("< Received GOAWAY frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending GOAWAY frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				break;
			}
			case 0x08: { // WINDOW_UPDATE https://httpwg.org/specs/rfc7540.html#WINDOW_UPDATE
				if (incoming) {
					LOG.info("< Received WINDOW_UPDATE frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending WINDOW_UPDATE frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				int v = fdis.readInt();
				LOG.info(" - WINDOW_UPDATE::Window Size Increment: {}", v & ~0x80000000);
				break;
			}
			case 0x09: { // CONTINUATION https://httpwg.org/specs/rfc7540.html#CONTINUATION
				if (incoming) {
					LOG.info("< Received CONTINUATION frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				} else {
					LOG.info("> Sending CONTINUATION frame (flags: {}, sid: {}, length: {})", flags, sid, length);
				}
				break;
			}
			default:
				break;
		}

		return buffer.hasRemaining();
	}

}
