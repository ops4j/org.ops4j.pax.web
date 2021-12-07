/*
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
package org.ops4j.pax.web.service.undertow.configuration.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import io.undertow.protocols.http2.Http2Channel;
import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xnio.SslClientAuthMode;

public class Server {

	protected static final QName ATT_NAME = new QName("name");

	private String name;

	private final List<HttpListener> httpListeners = new LinkedList<>();

	private final List<HttpsListener> httpsListeners = new LinkedList<>();

	private Host host;

	public static Server create(Map<QName, String> attributes, Locator locator) {
		Server server = new Server();
		server.name = attributes.get(ATT_NAME);

		return server;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<HttpListener> getHttpListeners() {
		return httpListeners;
	}

	public List<HttpsListener> getHttpsListeners() {
		return httpsListeners;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	@Override
	public String toString() {
		return "{\n\t\t\tname: " + name +
				"\n\t\t\thttp listeners: " + httpListeners +
				"\n\t\t\thttps listeners: " + httpsListeners +
				"\n\t\t\thost: " + host +
				"\n\t\t}";
	}

	// In Wildfly, org.wildfly.extension.undertow.ListenerResourceDefinition (and derived
	// HttpListenerResourceDefinition and HttpsListenerResourceDefinition) describe XML structure of listeners
	// org.wildfly.extension.undertow.UndertowSubsystemParser_4_0#listenerBuilder() declares common attributes

	// socket-options-type:
	//  - BACKLOG = OptionAttributeDefinition.builder("tcp-backlog", Options.BACKLOG).setDefaultValue(new ModelNode(10000)).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
	//  - RECEIVE_BUFFER = OptionAttributeDefinition.builder("receive-buffer", Options.RECEIVE_BUFFER).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
	//  - SEND_BUFFER = OptionAttributeDefinition.builder("send-buffer", Options.SEND_BUFFER).setAllowExpression(true).setValidator(new IntRangeValidator(1)).build();
	//  - KEEP_ALIVE = OptionAttributeDefinition.builder("tcp-keep-alive", Options.KEEP_ALIVE).setAllowExpression(true).build();
	//  - READ_TIMEOUT = OptionAttributeDefinition.builder("read-timeout", Options.READ_TIMEOUT).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).build();
	//  - WRITE_TIMEOUT = OptionAttributeDefinition.builder("write-timeout", Options.WRITE_TIMEOUT).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.MILLISECONDS).build();
	//  - MAX_CONNECTIONS = OptionAttributeDefinition.builder(Constants.MAX_CONNECTIONS, Options.CONNECTION_HIGH_WATER).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();

	public abstract static class SocketOptions {

		// org.wildfly.extension.undertow.ListenerResourceDefinition#SOCKET_OPTIONS

		/**
		 * XNIO: org.xnio.Options#RECEIVE_BUFFER, Java: java.net.ServerSocket#setReceiveBufferSize(int),
		 * java.net.SocketOptions#SO_RCVBUF, default: 0x10000 (org.xnio.nio.AbstractNioChannel#DEFAULT_BUFFER_SIZE)
		 */
		protected int receiveBuffer = 0x10000;

		/**
		 * XNIO: org.xnio.Options#SEND_BUFFER, Java: java.net.Socket#setSendBufferSize(int),
		 * java.net.SocketOptions#SO_SNDBUF, default: 0x10000 (org.xnio.nio.AbstractNioChannel#DEFAULT_BUFFER_SIZE)
		 */
		protected int sendBuffer = 0x10000;

		/**
		 * XNIO: org.xnio.Options#BACKLOG, Java: 2nd parameter of
		 * java.net.ServerSocket#bind(java.net.SocketAddress, int), default: 128 or 50 when using 1-arg bind().
		 */
		protected int tcpBacklog = 128;

		/**
		 * XNIO: org.xnio.Options#KEEP_ALIVE, Java: java.net.Socket#setKeepAlive(boolean),
		 * java.net.SocketOptions#SO_KEEPALIVE, default: false
		 */
		protected boolean tcpKeepAlive = false;

		/**
		 * XNIO: org.xnio.Options#READ_TIMEOUT (in ms), default: 0
		 */
		protected int readTimeout = 0;

		/**
		 * XNIO: org.xnio.Options#WRITE_TIMEOUT (in ms), default: 0
		 */
		protected int writeTimeout = 0;

		/**
		 * XNIO: org.xnio.Options#CONNECTION_HIGH_WATER
		 */
		protected int maxConnections = Integer.MAX_VALUE;

		public int getReceiveBuffer() {
			return receiveBuffer;
		}

		public void setReceiveBuffer(int receiveBuffer) {
			this.receiveBuffer = receiveBuffer;
		}

		public int getSendBuffer() {
			return sendBuffer;
		}

		public void setSendBuffer(int sendBuffer) {
			this.sendBuffer = sendBuffer;
		}

		public int getTcpBacklog() {
			return tcpBacklog;
		}

		public void setTcpBacklog(int tcpBacklog) {
			this.tcpBacklog = tcpBacklog;
		}

		public boolean isTcpKeepAlive() {
			return tcpKeepAlive;
		}

		public void setTcpKeepAlive(boolean tcpKeepAlive) {
			this.tcpKeepAlive = tcpKeepAlive;
		}

		public int getReadTimeout() {
			return readTimeout;
		}

		public void setReadTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
		}

		public int getWriteTimeout() {
			return writeTimeout;
		}

		public void setWriteTimeout(int writeTimeout) {
			this.writeTimeout = writeTimeout;
		}

		public int getMaxConnections() {
			return maxConnections;
		}

		public void setMaxConnections(int maxConnections) {
			this.maxConnections = maxConnections;
		}

		@Override
		public String toString() {
			return "{ " + toStringParameters() + " }";
		}

		protected String toStringParameters() {
			return "receive buffer: " + receiveBuffer +
					", send buffer: " + sendBuffer +
					", tcp backlog: " + tcpBacklog +
					", tcp KeepAlive: " + tcpKeepAlive +
					", read timeoout: " + readTimeout +
					", write timeoout: " + writeTimeout +
					", max connections: " + maxConnections;
		}
	}

	// listener-type:
	//  - MAX_HEADER_SIZE = OptionAttributeDefinition.builder("max-header-size", UndertowOptions.MAX_HEADER_SIZE).setDefaultValue(new ModelNode(UndertowOptions.DEFAULT_MAX_HEADER_SIZE)).setAllowExpression(true).setMeasurementUnit(MeasurementUnit.BYTES).setValidator(new IntRangeValidator(1)).build();
	//  - MAX_ENTITY_SIZE = OptionAttributeDefinition.builder(Constants.MAX_POST_SIZE, UndertowOptions.MAX_ENTITY_SIZE).setDefaultValue(new ModelNode(10485760L)).setValidator(new LongRangeValidator(0)).setMeasurementUnit(MeasurementUnit.BYTES).setAllowExpression(true).build();
	//  - BUFFER_PIPELINED_DATA = OptionAttributeDefinition.builder("buffer-pipelined-data", UndertowOptions.BUFFER_PIPELINED_DATA).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
	//  - MAX_PARAMETERS = OptionAttributeDefinition.builder("max-parameters", UndertowOptions.MAX_PARAMETERS).setDefaultValue(new ModelNode(1000)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
	//  - MAX_HEADERS = OptionAttributeDefinition.builder("max-headers", UndertowOptions.MAX_HEADERS).setDefaultValue(new ModelNode(200)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
	//  - MAX_COOKIES = OptionAttributeDefinition.builder("max-cookies", UndertowOptions.MAX_COOKIES).setDefaultValue(new ModelNode(200)).setValidator(new IntRangeValidator(1)).setAllowExpression(true).build();
	//  - ALLOW_ENCODED_SLASH = OptionAttributeDefinition.builder("allow-encoded-slash", UndertowOptions.ALLOW_ENCODED_SLASH).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
	//  - DECODE_URL = OptionAttributeDefinition.builder("decode-url", UndertowOptions.DECODE_URL).setDefaultValue(ModelNode.TRUE).setAllowExpression(true).build();
	//  - URL_CHARSET = OptionAttributeDefinition.builder("url-charset", UndertowOptions.URL_CHARSET).setDefaultValue(new ModelNode("UTF-8")).setAllowExpression(true).build();
	//  - ALWAYS_SET_KEEP_ALIVE = OptionAttributeDefinition.builder("always-set-keep-alive", UndertowOptions.ALWAYS_SET_KEEP_ALIVE).setDefaultValue(ModelNode.TRUE).setAllowExpression(true).build();
	//  - MAX_BUFFERED_REQUEST_SIZE = OptionAttributeDefinition.builder(Constants.MAX_BUFFERED_REQUEST_SIZE, UndertowOptions.MAX_BUFFERED_REQUEST_SIZE).setDefaultValue(new ModelNode(16384)).setValidator(new IntRangeValidator(1)).setMeasurementUnit(MeasurementUnit.BYTES).setAllowExpression(true).build();
	//  - RECORD_REQUEST_START_TIME = OptionAttributeDefinition.builder("record-request-start-time", UndertowOptions.RECORD_REQUEST_START_TIME).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
	//  - ALLOW_EQUALS_IN_COOKIE_VALUE = OptionAttributeDefinition.builder("allow-equals-in-cookie-value", UndertowOptions.ALLOW_EQUALS_IN_COOKIE_VALUE).setDefaultValue(ModelNode.FALSE).setAllowExpression(true).build();
	//  - NO_REQUEST_TIMEOUT = OptionAttributeDefinition.builder("no-request-timeout", UndertowOptions.NO_REQUEST_TIMEOUT).setDefaultValue(new ModelNode(60000)).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setRequired(false).setAllowExpression(true).build();
	//  - REQUEST_PARSE_TIMEOUT = OptionAttributeDefinition.builder("request-parse-timeout", UndertowOptions.REQUEST_PARSE_TIMEOUT).setMeasurementUnit(MeasurementUnit.MILLISECONDS).setRequired(false).setAllowExpression(true).build();
	//  - RFC6265_COOKIE_VALIDATION = OptionAttributeDefinition.builder("rfc6265-cookie-validation", UndertowOptions.ENABLE_RFC6265_COOKIE_VALIDATION).setDefaultValue(ModelNode.FALSE).setRequired(false).setAllowExpression(true).build();
	//  - ALLOW_UNESCAPED_CHARACTERS_IN_URL = OptionAttributeDefinition.builder("allow-unescaped-characters-in-url", UndertowOptions.ALLOW_UNESCAPED_CHARACTERS_IN_URL).setDefaultValue(ModelNode.FALSE).setRequired(false).setAllowExpression(true).build();

	public abstract static class Listener extends SocketOptions {
		// org.ops4j.pax.web.service.undertow.configuration.model.Server.SocketOptions
		protected static final QName ATT_RECEIVE_BUFFER = new QName("receive-buffer");
		protected static final QName ATT_SEND_BUFFER = new QName("send-buffer");
		protected static final QName ATT_TCP_BACKLOG = new QName("tcp-backlog");
		protected static final QName ATT_TCP_KEEP_ALIVE = new QName("tcp-keep-alive");
		protected static final QName ATT_READ_TIMEOUT = new QName("read-timeout");
		protected static final QName ATT_WRITE_TIMEOUT = new QName("write-timeout");
		protected static final QName ATT_MAX_CONNECTIONS = new QName("max-connections");
		// org.ops4j.pax.web.service.undertow.configuration.model.Server.Listener
		protected static final QName ATT_NAME = new QName("name");
		protected static final QName ATT_SOCKET_BINDING = new QName("socket-binding");
		protected static final QName ATT_WORKER = new QName("worker");
		protected static final QName ATT_BUFFER_POOL = new QName("buffer-pool");
		protected static final QName ATT_ENABLED = new QName("enabled");
		protected static final QName ATT_RESOLVE_PEER_ADDRESS = new QName("resolve-peer-address");
		protected static final QName ATT_DISALLOWED_METHODS = new QName("disallowed-methods");
		protected static final QName ATT_SECURE = new QName("secure");
		protected static final QName ATT_MAX_POST_SIZE = new QName("max-post-size");
		protected static final QName ATT_BUFFER_PIPELINED_DATA = new QName("buffer-pipelined-data");
		protected static final QName ATT_MAX_HEADER_SIZE = new QName("max-header-size");
		protected static final QName ATT_MAX_PARAMETERS = new QName("max-parameters");
		protected static final QName ATT_MAX_HEADERS = new QName("max-headers");
		protected static final QName ATT_MAX_COOKIES = new QName("max-cookies");
		protected static final QName ATT_ALLOW_ENCODED_SLASH = new QName("allow-encoded-slash");
		protected static final QName ATT_DECODE_URL = new QName("decode-url");
		protected static final QName ATT_URL_CHARSET = new QName("url-charset");
		protected static final QName ATT_ALWAYS_SET_KEEP_ALIVE = new QName("always-set-keep-alive");
		protected static final QName ATT_MAX_BUFFERED_REQUEST_SIZE = new QName("max-buffered-request-size");
		protected static final QName ATT_RECORD_REQUEST_START_TIME = new QName("record-request-start-time");
		protected static final QName ATT_ALLOW_EQUALS_IN_COOKIE_VALUE = new QName("allow-equals-in-cookie-value");
		protected static final QName ATT_NO_REQUEST_TIMEOUT = new QName("no-request-timeout");
		protected static final QName ATT_REQUEST_PARSE_TIMEOUT = new QName("request-parse-timeout");
		protected static final QName ATT_RFC6265_COOKIE_VALIDATION = new QName("rfc6265-cookie-validation");
		protected static final QName ATT_ALLOW_UNESCAPED_CHARACTERS_IN_URL = new QName("allow-unescaped-characters-in-url");

		protected String name;

		// meta information
		// generic attributes defined in static block of org.wildfly.extension.undertow.ListenerResourceDefinition
		// ATTRIBUTES = new LinkedHashSet<>(Arrays.asList(SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED, RESOLVE_PEER_ADDRESS, DISALLOWED_METHODS, SECURE));

		protected String socketBindingName;
		protected String workerName = "default";
		protected String bufferPoolName = "default";
		protected boolean enabled = true;
		protected boolean resolvePeerAddress = false;
		protected List<String> disallowedMethods = new LinkedList<>(Collections.singletonList("TRACE"));
		protected boolean secure = false;

		// org.wildfly.extension.undertow.ListenerResourceDefinition#LISTENER_OPTIONS

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_ENTITY_SIZE
		 */
		protected long maxPostSize = 10485760L;

		/**
		 * Undertow: io.undertow.UndertowOptions#BUFFER_PIPELINED_DATA
		 */
		protected boolean bufferPipelinedData = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_HEADER_SIZE
		 */
		protected int maxHeaderSize = 1048576;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_PARAMETERS
		 */
		protected int maxParameters = 1000;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_HEADERS
		 */
		protected int maxHeaders = 200;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_COOKIES
		 */
		protected int maxCookies = 200;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_ENCODED_SLASH
		 */
		protected boolean allowEncodedSlash = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#DECODE_URL
		 */
		protected boolean decodeUrl = true;

		/**
		 * Undertow: io.undertow.UndertowOptions#URL_CHARSET
		 */
		protected String urlCharset = StandardCharsets.UTF_8.name();

		/**
		 * Undertow: io.undertow.UndertowOptions#ALWAYS_SET_KEEP_ALIVE
		 */
		protected boolean alwaysSetKeepAlive = true;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_BUFFERED_REQUEST_SIZE
		 */
		protected int maxBufferedRequestSize = 16384;

		/**
		 * Undertow: io.undertow.UndertowOptions#RECORD_REQUEST_START_TIME
		 */
		protected boolean recordRequestStartTime = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_EQUALS_IN_COOKIE_VALUE
		 */
		protected boolean allowEqualsInCookieValue = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#NO_REQUEST_TIMEOUT
		 */
		protected int noRequestTimeout = 60000;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUEST_PARSE_TIMEOUT
		 */
		protected int requestParseTimeout = 60000;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_RFC6265_COOKIE_VALIDATION
		 */
		protected boolean rfc6265CookieValidation = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_UNESCAPED_CHARACTERS_IN_URL
		 */
		protected boolean allowUnescapedCharactersInUrl = false;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSocketBindingName() {
			return socketBindingName;
		}

		public void setSocketBindingName(String socketBindingName) {
			this.socketBindingName = socketBindingName;
		}

		public String getWorkerName() {
			return workerName;
		}

		public void setWorkerName(String workerName) {
			this.workerName = workerName;
		}

		public String getBufferPoolName() {
			return bufferPoolName;
		}

		public void setBufferPoolName(String bufferPoolName) {
			this.bufferPoolName = bufferPoolName;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isResolvePeerAddress() {
			return resolvePeerAddress;
		}

		public void setResolvePeerAddress(boolean resolvePeerAddress) {
			this.resolvePeerAddress = resolvePeerAddress;
		}

		public List<String> getDisallowedMethods() {
			return disallowedMethods;
		}

		public void setDisallowedMethods(List<String> disallowedMethods) {
			this.disallowedMethods = disallowedMethods;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public long getMaxPostSize() {
			return maxPostSize;
		}

		public void setMaxPostSize(long maxPostSize) {
			this.maxPostSize = maxPostSize;
		}

		public boolean isBufferPipelinedData() {
			return bufferPipelinedData;
		}

		public void setBufferPipelinedData(boolean bufferPipelinedData) {
			this.bufferPipelinedData = bufferPipelinedData;
		}

		public int getMaxHeaderSize() {
			return maxHeaderSize;
		}

		public void setMaxHeaderSize(int maxHeaderSize) {
			this.maxHeaderSize = maxHeaderSize;
		}

		public int getMaxParameters() {
			return maxParameters;
		}

		public void setMaxParameters(int maxParameters) {
			this.maxParameters = maxParameters;
		}

		public int getMaxHeaders() {
			return maxHeaders;
		}

		public void setMaxHeaders(int maxHeaders) {
			this.maxHeaders = maxHeaders;
		}

		public int getMaxCookies() {
			return maxCookies;
		}

		public void setMaxCookies(int maxCookies) {
			this.maxCookies = maxCookies;
		}

		public boolean isAllowEncodedSlash() {
			return allowEncodedSlash;
		}

		public void setAllowEncodedSlash(boolean allowEncodedSlash) {
			this.allowEncodedSlash = allowEncodedSlash;
		}

		public boolean isDecodeUrl() {
			return decodeUrl;
		}

		public void setDecodeUrl(boolean decodeUrl) {
			this.decodeUrl = decodeUrl;
		}

		public String getUrlCharset() {
			return urlCharset;
		}

		public void setUrlCharset(String urlCharset) {
			this.urlCharset = urlCharset;
		}

		public boolean isAlwaysSetKeepAlive() {
			return alwaysSetKeepAlive;
		}

		public void setAlwaysSetKeepAlive(boolean alwaysSetKeepAlive) {
			this.alwaysSetKeepAlive = alwaysSetKeepAlive;
		}

		public int getMaxBufferedRequestSize() {
			return maxBufferedRequestSize;
		}

		public void setMaxBufferedRequestSize(int maxBufferedRequestSize) {
			this.maxBufferedRequestSize = maxBufferedRequestSize;
		}

		public boolean isRecordRequestStartTime() {
			return recordRequestStartTime;
		}

		public void setRecordRequestStartTime(boolean recordRequestStartTime) {
			this.recordRequestStartTime = recordRequestStartTime;
		}

		public boolean isAllowEqualsInCookieValue() {
			return allowEqualsInCookieValue;
		}

		public void setAllowEqualsInCookieValue(boolean allowEqualsInCookieValue) {
			this.allowEqualsInCookieValue = allowEqualsInCookieValue;
		}

		public int getNoRequestTimeout() {
			return noRequestTimeout;
		}

		public void setNoRequestTimeout(int noRequestTimeout) {
			this.noRequestTimeout = noRequestTimeout;
		}

		public int getRequestParseTimeout() {
			return requestParseTimeout;
		}

		public void setRequestParseTimeout(int requestParseTimeout) {
			this.requestParseTimeout = requestParseTimeout;
		}

		public boolean isRfc6265CookieValidation() {
			return rfc6265CookieValidation;
		}

		public void setRfc6265CookieValidation(boolean rfc6265CookieValidation) {
			this.rfc6265CookieValidation = rfc6265CookieValidation;
		}

		public boolean isAllowUnescapedCharactersInUrl() {
			return allowUnescapedCharactersInUrl;
		}

		public void setAllowUnescapedCharactersInUrl(boolean allowUnescapedCharactersInUrl) {
			this.allowUnescapedCharactersInUrl = allowUnescapedCharactersInUrl;
		}

		public abstract boolean isProxyAddressForwarding();

		public abstract boolean isCertificateForwarding();

		public abstract boolean isRequireHostHttp11();

		public abstract boolean isEnableHttp2();

		public abstract boolean isHttp2EnablePush();

		public abstract int getHttp2HeaderTableSize();

		public abstract int getHttp2InitialWindowSize();

		public abstract Integer getHttp2MaxConcurrentStreams();

		public abstract int getHttp2MaxFrameSize();

		public abstract Integer getHttp2MaxHeaderListSize();

		@Override
		protected String toStringParameters() {
			final StringBuilder sb = new StringBuilder();
			sb.append("name: ").append(name);
			sb.append(", ").append(super.toStringParameters());
			sb.append(", socket binding name: ").append(socketBindingName);
			sb.append(", worker name: ").append(workerName);
			sb.append(", buffer pool name: ").append(bufferPoolName);
			sb.append(", enabled: ").append(enabled);
			sb.append(", resolve peer address: ").append(resolvePeerAddress);
			sb.append(", disallowed methods: ").append(disallowedMethods);
			sb.append(", secure: ").append(isSecure());
			sb.append(", max post size: ").append(maxPostSize);
			sb.append(", buffer pipelined data: ").append(bufferPipelinedData);
			sb.append(", max header size: ").append(maxHeaderSize);
			sb.append(", max parameters: ").append(maxParameters);
			sb.append(", max headers: ").append(maxHeaders);
			sb.append(", max cookies: ").append(maxCookies);
			sb.append(", allow encoded slash: ").append(allowEncodedSlash);
			sb.append(", decode url: ").append(decodeUrl);
			sb.append(", url charset: ").append(urlCharset);
			sb.append(", always set keep alive: ").append(alwaysSetKeepAlive);
			sb.append(", max buffered request size: ").append(maxBufferedRequestSize);
			sb.append(", record request start time: ").append(recordRequestStartTime);
			sb.append(", allow equals in cookie value: ").append(allowEqualsInCookieValue);
			sb.append(", no request timeout: ").append(noRequestTimeout);
			sb.append(", request parse timeout: ").append(requestParseTimeout);
			sb.append(", rfc6265 cookie validation: ").append(rfc6265CookieValidation);
			return sb.toString();
		}
	}

	// http-listener-type (some atrributes have different type in XSD and different in Wildfly extension model)

	public static class HttpListener extends Listener {
		protected static final QName ATT_CERTIFICATE_FORWARDING = new QName("certificate-forwarding");
		protected static final QName ATT_REDIRECT_SOCKET = new QName("redirect-socket");
		protected static final QName ATT_PROXY_ADDRESS_FORWARDING = new QName("proxy-address-forwarding");
		protected static final QName ATT_ENABLE_HTTP2 = new QName("enable-http2");
		protected static final QName ATT_HTTP2_ENABLE_PUSH = new QName("http2-enable-push");
		protected static final QName ATT_HTTP2_HEADER_TABLE_SIZE = new QName("http2-header-table-size");
		protected static final QName ATT_HTTP2_INITIAL_WINDOW_SIZE = new QName("http2-initial-window-size");
		protected static final QName ATT_HTTP2_MAX_CONCURRENT_STREAMS = new QName("http2-max-concurrent-streams");
		protected static final QName ATT_HTTP2_MAX_FRAME_SIZE = new QName("http2-max-frame-size");
		protected static final QName ATT_HTTP2_MAX_HEADER_LIST_SIZE = new QName("http2-max-header-list-size");
		protected static final QName ATT_REQUIRE_HOST_HTTP11 = new QName("require-host-http11");
		protected static final QName ATT_PROXY_PROTOCOL = new QName("proxy-protocol");

		private boolean certificateForwarding = false;

		private String redirectSocket;

		private boolean proxyAddressForwarding = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_HTTP2
		 */
		private boolean enableHttp2 = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_ENABLE_PUSH
		 */
		private boolean http2EnablePush = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE, defaults to
		 * io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT
		 */
		private int http2HeaderTableSize = io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, defaults to
		 * io.undertow.protocols.http2.Http2Channel#DEFAULT_INITIAL_WINDOW_SIZE
		 */
		private int http2InitialWindowSize = Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS
		 */
		private Integer http2MaxConcurrentStreams;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_FRAME_SIZE
		 */
		private int http2MaxFrameSize = Http2Channel.DEFAULT_MAX_FRAME_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE
		 */
		private Integer http2MaxHeaderListSize;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUIRE_HOST_HTTP11
		 */
		private boolean requireHostHttp11 = false;

		private boolean proxyProtocol = false;

		public static HttpListener create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			HttpListener listener = new HttpListener();
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.SocketOptions
			listener.receiveBuffer = ParserUtils.toInteger(attributes.get(ATT_RECEIVE_BUFFER), locator, 0x10000);
			listener.sendBuffer = ParserUtils.toInteger(attributes.get(ATT_SEND_BUFFER), locator, 0x10000);
			listener.tcpBacklog = ParserUtils.toInteger(attributes.get(ATT_TCP_BACKLOG), locator, 128);
			listener.tcpKeepAlive = ParserUtils.toBoolean(attributes.get(ATT_TCP_KEEP_ALIVE), locator, false);
			listener.readTimeout = ParserUtils.toInteger(attributes.get(ATT_READ_TIMEOUT), locator, 0);
			listener.writeTimeout = ParserUtils.toInteger(attributes.get(ATT_WRITE_TIMEOUT), locator, 0);
			listener.maxConnections = ParserUtils.toInteger(attributes.get(ATT_MAX_CONNECTIONS), locator, Integer.MAX_VALUE);
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.Listener
			listener.name = attributes.get(ATT_NAME);
			listener.socketBindingName = attributes.get(ATT_SOCKET_BINDING);
			listener.workerName = ParserUtils.toStringValue(attributes.get(ATT_WORKER), locator, "default");
			listener.bufferPoolName = ParserUtils.toStringValue(attributes.get(ATT_BUFFER_POOL), locator, "default");
			listener.enabled = ParserUtils.toBoolean(attributes.get(ATT_ENABLED), locator, true);
			listener.resolvePeerAddress = ParserUtils.toBoolean(attributes.get(ATT_RESOLVE_PEER_ADDRESS), locator, false);
			listener.disallowedMethods.addAll(ParserUtils.toStringList(attributes.get(ATT_DISALLOWED_METHODS), locator));
			listener.secure = ParserUtils.toBoolean(attributes.get(ATT_SECURE), locator, false);
			listener.maxPostSize = ParserUtils.toLong(attributes.get(ATT_MAX_POST_SIZE), locator, 10485760L);
			listener.bufferPipelinedData = ParserUtils.toBoolean(attributes.get(ATT_BUFFER_PIPELINED_DATA), locator, false);
			listener.maxHeaderSize = ParserUtils.toInteger(attributes.get(ATT_MAX_HEADER_SIZE), locator, 1048576);
			listener.maxParameters = ParserUtils.toInteger(attributes.get(ATT_MAX_PARAMETERS), locator, 1000);
			listener.maxHeaders = ParserUtils.toInteger(attributes.get(ATT_MAX_HEADERS), locator, 200);
			listener.maxCookies = ParserUtils.toInteger(attributes.get(ATT_MAX_COOKIES), locator, 200);
			listener.allowEncodedSlash = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_ENCODED_SLASH), locator, false);
			listener.decodeUrl = ParserUtils.toBoolean(attributes.get(ATT_DECODE_URL), locator, true);
			listener.urlCharset = ParserUtils.toStringValue(attributes.get(ATT_URL_CHARSET), locator, StandardCharsets.UTF_8.name());
			listener.alwaysSetKeepAlive = ParserUtils.toBoolean(attributes.get(ATT_ALWAYS_SET_KEEP_ALIVE), locator, true);
			listener.maxBufferedRequestSize = ParserUtils.toInteger(attributes.get(ATT_MAX_BUFFERED_REQUEST_SIZE), locator, 16384);
			listener.recordRequestStartTime = ParserUtils.toBoolean(attributes.get(ATT_RECORD_REQUEST_START_TIME), locator, false);
			listener.allowEqualsInCookieValue = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_EQUALS_IN_COOKIE_VALUE), locator, false);
			listener.noRequestTimeout = ParserUtils.toInteger(attributes.get(ATT_NO_REQUEST_TIMEOUT), locator, 60000);
			listener.requestParseTimeout = ParserUtils.toInteger(attributes.get(ATT_REQUEST_PARSE_TIMEOUT), locator, 60000);
			listener.rfc6265CookieValidation = ParserUtils.toBoolean(attributes.get(ATT_RFC6265_COOKIE_VALIDATION), locator, false);
			listener.allowUnescapedCharactersInUrl = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_UNESCAPED_CHARACTERS_IN_URL), locator, false);
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.HttpListener
			listener.certificateForwarding = ParserUtils.toBoolean(attributes.get(ATT_CERTIFICATE_FORWARDING), locator, false);
			listener.redirectSocket = attributes.get(ATT_REDIRECT_SOCKET);
			listener.proxyAddressForwarding = ParserUtils.toBoolean(attributes.get(ATT_PROXY_ADDRESS_FORWARDING), locator, false);
			listener.enableHttp2 = ParserUtils.toBoolean(attributes.get(ATT_ENABLE_HTTP2), locator, false);
			listener.http2EnablePush = ParserUtils.toBoolean(attributes.get(ATT_HTTP2_ENABLE_PUSH), locator, false);
			listener.http2HeaderTableSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_HEADER_TABLE_SIZE), locator, io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT);
			listener.http2InitialWindowSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_INITIAL_WINDOW_SIZE), locator, Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE);
			listener.http2MaxConcurrentStreams = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_CONCURRENT_STREAMS), locator, null);
			listener.http2MaxFrameSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_FRAME_SIZE), locator, Http2Channel.DEFAULT_MAX_FRAME_SIZE);
			listener.http2MaxHeaderListSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_HEADER_LIST_SIZE), locator, null);
			listener.requireHostHttp11 = ParserUtils.toBoolean(attributes.get(ATT_REQUIRE_HOST_HTTP11), locator, false);
			listener.proxyProtocol = ParserUtils.toBoolean(attributes.get(ATT_PROXY_PROTOCOL), locator, false);

			return listener;
		}

		@Override
		public boolean isCertificateForwarding() {
			return certificateForwarding;
		}

		public void setCertificateForwarding(boolean certificateForwarding) {
			this.certificateForwarding = certificateForwarding;
		}

		public String getRedirectSocket() {
			return redirectSocket;
		}

		public void setRedirectSocket(String redirectSocket) {
			this.redirectSocket = redirectSocket;
		}

		@Override
		public boolean isProxyAddressForwarding() {
			return proxyAddressForwarding;
		}

		public void setProxyAddressForwarding(boolean proxyAddressForwarding) {
			this.proxyAddressForwarding = proxyAddressForwarding;
		}

		public boolean isEnableHttp2() {
			return enableHttp2;
		}

		public void setEnableHttp2(boolean enableHttp2) {
			this.enableHttp2 = enableHttp2;
		}

		public boolean isHttp2EnablePush() {
			return http2EnablePush;
		}

		public void setHttp2EnablePush(boolean http2EnablePush) {
			this.http2EnablePush = http2EnablePush;
		}

		public int getHttp2HeaderTableSize() {
			return http2HeaderTableSize;
		}

		public void setHttp2HeaderTableSize(int http2HeaderTableSize) {
			this.http2HeaderTableSize = http2HeaderTableSize;
		}

		public int getHttp2InitialWindowSize() {
			return http2InitialWindowSize;
		}

		public void setHttp2InitialWindowSize(int http2InitialWindowSize) {
			this.http2InitialWindowSize = http2InitialWindowSize;
		}

		public Integer getHttp2MaxConcurrentStreams() {
			return http2MaxConcurrentStreams;
		}

		public void setHttp2MaxConcurrentStreams(Integer http2MaxConcurrentStreams) {
			this.http2MaxConcurrentStreams = http2MaxConcurrentStreams;
		}

		public int getHttp2MaxFrameSize() {
			return http2MaxFrameSize;
		}

		public void setHttp2MaxFrameSize(int http2MaxFrameSize) {
			this.http2MaxFrameSize = http2MaxFrameSize;
		}

		public Integer getHttp2MaxHeaderListSize() {
			return http2MaxHeaderListSize;
		}

		public void setHttp2MaxHeaderListSize(Integer http2MaxHeaderListSize) {
			this.http2MaxHeaderListSize = http2MaxHeaderListSize;
		}

		@Override
		public boolean isRequireHostHttp11() {
			return requireHostHttp11;
		}

		public void setRequireHostHttp11(boolean requireHostHttp11) {
			this.requireHostHttp11 = requireHostHttp11;
		}

		public boolean isProxyProtocol() {
			return proxyProtocol;
		}

		public void setProxyProtocol(boolean proxyProtocol) {
			this.proxyProtocol = proxyProtocol;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append(toStringParameters());
			sb.append(" }");
			return sb.toString();
		}

		@Override
		protected String toStringParameters() {
			final StringBuilder sb = new StringBuilder();
			sb.append(super.toStringParameters());
			sb.append(", certificate forwarding: ").append(certificateForwarding);
			sb.append(", redirect socket: ").append(redirectSocket);
			sb.append(", proxy address forwarding: ").append(proxyAddressForwarding);
			sb.append(", enable http2: ").append(enableHttp2);
			sb.append(", http2 enable push: ").append(http2EnablePush);
			sb.append(", http2 header table size: ").append(http2HeaderTableSize);
			sb.append(", http2 initial window size: ").append(http2InitialWindowSize);
			sb.append(", http2 max concurrent streams: ").append(http2MaxConcurrentStreams);
			sb.append(", http2 max frame size: ").append(http2MaxFrameSize);
			sb.append(", http2 max header list size: ").append(http2MaxHeaderListSize);
			sb.append(", require host http11: ").append(requireHostHttp11);
			sb.append(", proxy protocol: ").append(proxyProtocol);
			return sb.toString();
		}
	}

	public static class HttpsListener extends Listener {
		protected static final QName ATT_SSL_CONTEXT = new QName("ssl-context");
		protected static final QName ATT_CERTIFICATE_FORWARDING = new QName("certificate-forwarding");
		protected static final QName ATT_PROXY_ADDRESS_FORWARDING = new QName("proxy-address-forwarding");
		protected static final QName ATT_SECURITY_REALM = new QName("security-realm");
		protected static final QName ATT_VERIFY_CLIENT = new QName("verify-client");
		protected static final QName ATT_ENABLED_CIPHER_SUITES = new QName("enabled-cipher-suites");
		protected static final QName ATT_ENABLED_PROTOCOLS = new QName("enabled-protocols");
		protected static final QName ATT_ENABLE_HTTP2 = new QName("enable-http2");
		protected static final QName ATT_ENABLE_SPDY = new QName("enable-spdy");
		protected static final QName ATT_SSL_SESSION_CACHE_SIZE = new QName("ssl-session-cache-size");
		protected static final QName ATT_SSL_SESSION_TIMEOUT = new QName("ssl-session-timeout");
		protected static final QName ATT_HTTP2_ENABLE_PUSH = new QName("http2-enable-push");
		protected static final QName ATT_HTTP2_HEADER_TABLE_SIZE = new QName("http2-header-table-size");
		protected static final QName ATT_HTTP2_INITIAL_WINDOW_SIZE = new QName("http2-initial-window-size");
		protected static final QName ATT_HTTP2_MAX_CONCURRENT_STREAMS = new QName("http2-max-concurrent-streams");
		protected static final QName ATT_HTTP2_MAX_FRAME_SIZE = new QName("http2-max-frame-size");
		protected static final QName ATT_HTTP2_MAX_HEADER_LIST_SIZE = new QName("http2-max-header-list-size");
		protected static final QName ATT_REQUIRE_HOST_HTTP11 = new QName("require-host-http11");
		protected static final QName ATT_PROXY_PROTOCOL = new QName("proxy-protocol");

		private String sslContext;

		private boolean certificateForwarding = false;

		private boolean proxyAddressForwarding = false;

		// legacy in urn:jboss:domain:undertow:4.0 - but still used in pax-web
		// but "ssl-context" used in Wildfly integrates harder with the Wildfly itself, so "security-realm" is
		// the mechanism to configure security
		private String securityRealm;

		/**
		 * XNIO: org.xnio.Options#SSL_CLIENT_AUTH_MODE
		 */
		private SslClientAuthMode verifyClient = SslClientAuthMode.NOT_REQUESTED;

		/**
		 * XNIO: org.xnio.Options#SSL_ENABLED_CIPHER_SUITES
		 */
		private List<String> enabledCipherSuites = new ArrayList<>();

		/**
		 * XNIO: org.xnio.Options#SSL_ENABLED_PROTOCOLS
		 */
		private List<String> enabledProtocols = new ArrayList<>();

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_HTTP2
		 */
		private boolean enableHttp2 = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_SPDY
		 */
		@Deprecated
		private boolean enableSpdy = false;

		/**
		 * XNIO: org.xnio.Options#SSL_SERVER_SESSION_CACHE_SIZE
		 */
		private int sslSessionCacheSize = 0;

		/**
		 * XNIO: org.xnio.Options#SSL_SERVER_SESSION_TIMEOUT
		 */
		private int sslSessionTimeout = 0;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_ENABLE_PUSH
		 */
		private boolean http2EnablePush = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE, defaults to
		 * io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT
		 */
		private int http2HeaderTableSize = io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, defaults to
		 * io.undertow.protocols.http2.Http2Channel#DEFAULT_INITIAL_WINDOW_SIZE
		 */
		private int http2InitialWindowSize = Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS
		 */
		private Integer http2MaxConcurrentStreams;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_FRAME_SIZE
		 */
		private int http2MaxFrameSize = Http2Channel.DEFAULT_MAX_FRAME_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE
		 */
		private Integer http2MaxHeaderListSize;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUIRE_HOST_HTTP11
		 */
		private boolean requireHostHttp11 = false;

		private boolean proxyProtocol = false;

		public static HttpsListener create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			HttpsListener listener = new HttpsListener();
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.SocketOptions
			listener.receiveBuffer = ParserUtils.toInteger(attributes.get(ATT_RECEIVE_BUFFER), locator, 0x10000);
			listener.sendBuffer = ParserUtils.toInteger(attributes.get(ATT_SEND_BUFFER), locator, 0x10000);
			listener.tcpBacklog = ParserUtils.toInteger(attributes.get(ATT_TCP_BACKLOG), locator, 128);
			listener.tcpKeepAlive = ParserUtils.toBoolean(attributes.get(ATT_TCP_KEEP_ALIVE), locator, false);
			listener.readTimeout = ParserUtils.toInteger(attributes.get(ATT_READ_TIMEOUT), locator, 0);
			listener.writeTimeout = ParserUtils.toInteger(attributes.get(ATT_WRITE_TIMEOUT), locator, 0);
			listener.maxConnections = ParserUtils.toInteger(attributes.get(ATT_MAX_CONNECTIONS), locator, Integer.MAX_VALUE);
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.Listener
			listener.name = attributes.get(ATT_NAME);
			listener.socketBindingName = attributes.get(ATT_SOCKET_BINDING);
			listener.workerName = ParserUtils.toStringValue(attributes.get(ATT_WORKER), locator, "default");
			listener.bufferPoolName = ParserUtils.toStringValue(attributes.get(ATT_BUFFER_POOL), locator, "default");
			listener.enabled = ParserUtils.toBoolean(attributes.get(ATT_ENABLED), locator, true);
			listener.resolvePeerAddress = ParserUtils.toBoolean(attributes.get(ATT_RESOLVE_PEER_ADDRESS), locator, false);
			listener.disallowedMethods.addAll(ParserUtils.toStringList(attributes.get(ATT_DISALLOWED_METHODS), locator));
			listener.secure = ParserUtils.toBoolean(attributes.get(ATT_SECURE), locator, false);
			listener.maxPostSize = ParserUtils.toLong(attributes.get(ATT_MAX_POST_SIZE), locator, 10485760L);
			listener.bufferPipelinedData = ParserUtils.toBoolean(attributes.get(ATT_BUFFER_PIPELINED_DATA), locator, false);
			listener.maxHeaderSize = ParserUtils.toInteger(attributes.get(ATT_MAX_HEADER_SIZE), locator, 1048576);
			listener.maxParameters = ParserUtils.toInteger(attributes.get(ATT_MAX_PARAMETERS), locator, 1000);
			listener.maxHeaders = ParserUtils.toInteger(attributes.get(ATT_MAX_HEADERS), locator, 200);
			listener.maxCookies = ParserUtils.toInteger(attributes.get(ATT_MAX_COOKIES), locator, 200);
			listener.allowEncodedSlash = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_ENCODED_SLASH), locator, false);
			listener.decodeUrl = ParserUtils.toBoolean(attributes.get(ATT_DECODE_URL), locator, true);
			listener.urlCharset = ParserUtils.toStringValue(attributes.get(ATT_URL_CHARSET), locator, StandardCharsets.UTF_8.name());
			listener.alwaysSetKeepAlive = ParserUtils.toBoolean(attributes.get(ATT_ALWAYS_SET_KEEP_ALIVE), locator, true);
			listener.maxBufferedRequestSize = ParserUtils.toInteger(attributes.get(ATT_MAX_BUFFERED_REQUEST_SIZE), locator, 16384);
			listener.recordRequestStartTime = ParserUtils.toBoolean(attributes.get(ATT_RECORD_REQUEST_START_TIME), locator, false);
			listener.allowEqualsInCookieValue = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_EQUALS_IN_COOKIE_VALUE), locator, false);
			listener.noRequestTimeout = ParserUtils.toInteger(attributes.get(ATT_NO_REQUEST_TIMEOUT), locator, 60000);
			listener.requestParseTimeout = ParserUtils.toInteger(attributes.get(ATT_REQUEST_PARSE_TIMEOUT), locator, 60000);
			listener.rfc6265CookieValidation = ParserUtils.toBoolean(attributes.get(ATT_RFC6265_COOKIE_VALIDATION), locator, false);
			listener.allowUnescapedCharactersInUrl = ParserUtils.toBoolean(attributes.get(ATT_ALLOW_UNESCAPED_CHARACTERS_IN_URL), locator, false);
			// org.ops4j.pax.web.service.undertow.configuration.model.Server.HttpListener
			listener.sslContext = attributes.get(ATT_SSL_CONTEXT);
			listener.certificateForwarding = ParserUtils.toBoolean(attributes.get(ATT_CERTIFICATE_FORWARDING), locator, false);
			listener.proxyAddressForwarding = ParserUtils.toBoolean(attributes.get(ATT_PROXY_ADDRESS_FORWARDING), locator, false);
			listener.securityRealm = attributes.get(ATT_SECURITY_REALM);
			String vc = attributes.get(ATT_VERIFY_CLIENT);
			if (vc == null) {
				listener.verifyClient = SslClientAuthMode.NOT_REQUESTED;
			} else {
				try {
					listener.verifyClient = SslClientAuthMode.valueOf(vc);
				} catch (IllegalArgumentException e) {
					throw new SAXParseException("Can't parse \"" + vc + "\" as valid value for \"verify-client\" attribute", locator);
				}
			}
			listener.enabledCipherSuites.addAll(ParserUtils.toStringList(attributes.get(ATT_ENABLED_CIPHER_SUITES), locator));
			listener.enabledProtocols.addAll(ParserUtils.toStringList(attributes.get(ATT_ENABLED_PROTOCOLS), locator));
			listener.enableHttp2 = ParserUtils.toBoolean(attributes.get(ATT_ENABLE_HTTP2), locator, false);
			listener.enableSpdy = ParserUtils.toBoolean(attributes.get(ATT_ENABLE_HTTP2), locator, false);
			listener.sslSessionCacheSize = ParserUtils.toInteger(attributes.get(ATT_SSL_SESSION_CACHE_SIZE), locator, 0);
			listener.sslSessionTimeout = ParserUtils.toInteger(attributes.get(ATT_SSL_SESSION_TIMEOUT), locator, 0);
			listener.http2EnablePush = ParserUtils.toBoolean(attributes.get(ATT_HTTP2_ENABLE_PUSH), locator, false);
			listener.http2HeaderTableSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_HEADER_TABLE_SIZE), locator, io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT);
			listener.http2InitialWindowSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_INITIAL_WINDOW_SIZE), locator, Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE);
			listener.http2MaxConcurrentStreams = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_CONCURRENT_STREAMS), locator, null);
			listener.http2MaxFrameSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_FRAME_SIZE), locator, Http2Channel.DEFAULT_MAX_FRAME_SIZE);
			listener.http2MaxHeaderListSize = ParserUtils.toInteger(attributes.get(ATT_HTTP2_MAX_HEADER_LIST_SIZE), locator, null);
			listener.requireHostHttp11 = ParserUtils.toBoolean(attributes.get(ATT_REQUIRE_HOST_HTTP11), locator, false);
			listener.proxyProtocol = ParserUtils.toBoolean(attributes.get(ATT_PROXY_PROTOCOL), locator, false);

			return listener;
		}

		public String getSslContext() {
			return sslContext;
		}

		public void setSslContext(String sslContext) {
			this.sslContext = sslContext;
		}

		@Override
		public boolean isCertificateForwarding() {
			return certificateForwarding;
		}

		public void setCertificateForwarding(boolean certificateForwarding) {
			this.certificateForwarding = certificateForwarding;
		}

		@Override
		public boolean isProxyAddressForwarding() {
			return proxyAddressForwarding;
		}

		public void setProxyAddressForwarding(boolean proxyAddressForwarding) {
			this.proxyAddressForwarding = proxyAddressForwarding;
		}

		public String getSecurityRealm() {
			return securityRealm;
		}

		public void setSecurityRealm(String securityRealm) {
			this.securityRealm = securityRealm;
		}

		public SslClientAuthMode getVerifyClient() {
			return verifyClient;
		}

		public void setVerifyClient(SslClientAuthMode verifyClient) {
			this.verifyClient = verifyClient;
		}

		public List<String> getEnabledCipherSuites() {
			return enabledCipherSuites;
		}

		public void setEnabledCipherSuites(List<String> enabledCipherSuites) {
			this.enabledCipherSuites = enabledCipherSuites;
		}

		public List<String> getEnabledProtocols() {
			return enabledProtocols;
		}

		public void setEnabledProtocols(List<String> enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

		public boolean isEnableHttp2() {
			return enableHttp2;
		}

		public void setEnableHttp2(boolean enableHttp2) {
			this.enableHttp2 = enableHttp2;
		}

		public boolean isEnableSpdy() {
			return enableSpdy;
		}

		public void setEnableSpdy(boolean enableSpdy) {
			this.enableSpdy = enableSpdy;
		}

		public int getSslSessionCacheSize() {
			return sslSessionCacheSize;
		}

		public void setSslSessionCacheSize(int sslSessionCacheSize) {
			this.sslSessionCacheSize = sslSessionCacheSize;
		}

		public int getSslSessionTimeout() {
			return sslSessionTimeout;
		}

		public void setSslSessionTimeout(int sslSessionTimeout) {
			this.sslSessionTimeout = sslSessionTimeout;
		}

		public boolean isHttp2EnablePush() {
			return http2EnablePush;
		}

		public void setHttp2EnablePush(boolean http2EnablePush) {
			this.http2EnablePush = http2EnablePush;
		}

		public int getHttp2HeaderTableSize() {
			return http2HeaderTableSize;
		}

		public void setHttp2HeaderTableSize(int http2HeaderTableSize) {
			this.http2HeaderTableSize = http2HeaderTableSize;
		}

		public int getHttp2InitialWindowSize() {
			return http2InitialWindowSize;
		}

		public void setHttp2InitialWindowSize(int http2InitialWindowSize) {
			this.http2InitialWindowSize = http2InitialWindowSize;
		}

		public Integer getHttp2MaxConcurrentStreams() {
			return http2MaxConcurrentStreams;
		}

		public void setHttp2MaxConcurrentStreams(Integer http2MaxConcurrentStreams) {
			this.http2MaxConcurrentStreams = http2MaxConcurrentStreams;
		}

		public int getHttp2MaxFrameSize() {
			return http2MaxFrameSize;
		}

		public void setHttp2MaxFrameSize(int http2MaxFrameSize) {
			this.http2MaxFrameSize = http2MaxFrameSize;
		}

		public Integer getHttp2MaxHeaderListSize() {
			return http2MaxHeaderListSize;
		}

		public void setHttp2MaxHeaderListSize(Integer http2MaxHeaderListSize) {
			this.http2MaxHeaderListSize = http2MaxHeaderListSize;
		}

		@Override
		public boolean isRequireHostHttp11() {
			return requireHostHttp11;
		}

		public void setRequireHostHttp11(boolean requireHostHttp11) {
			this.requireHostHttp11 = requireHostHttp11;
		}

		public boolean isProxyProtocol() {
			return proxyProtocol;
		}

		public void setProxyProtocol(boolean proxyProtocol) {
			this.proxyProtocol = proxyProtocol;
		}

		@Override
		public boolean isSecure() {
			return true;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append(toStringParameters());
			sb.append(" }");
			return sb.toString();
		}

		@Override
		protected String toStringParameters() {
			final StringBuilder sb = new StringBuilder();
			sb.append(super.toStringParameters());
			sb.append(", ssl context: ").append(sslContext);
			sb.append(", certificate forwarding: ").append(certificateForwarding);
			sb.append(", proxy address forwarding: ").append(proxyAddressForwarding);
			sb.append(", security realm: ").append(securityRealm);
			sb.append(", verify client: ").append(verifyClient);
			sb.append(", enabled cipher suites: ").append(enabledCipherSuites);
			sb.append(", enabled protocols: ").append(enabledProtocols);
			sb.append(", enable http2: ").append(enableHttp2);
			sb.append(", enable spdy: ").append(enableSpdy);
			sb.append(", ssl session cache size: ").append(sslSessionCacheSize);
			sb.append(", ssl session timeout: ").append(sslSessionTimeout);
			sb.append(", http2 enable push: ").append(http2EnablePush);
			sb.append(", http2 header table size: ").append(http2HeaderTableSize);
			sb.append(", http2 initial window size: ").append(http2InitialWindowSize);
			sb.append(", http2 max concurrent streams: ").append(http2MaxConcurrentStreams);
			sb.append(", http2 max frame size: ").append(http2MaxFrameSize);
			sb.append(", http2 max header list size: ").append(http2MaxHeaderListSize);
			sb.append(", require host http11: ").append(requireHostHttp11);
			sb.append(", proxy protocol: ").append(proxyProtocol);
			return sb.toString();
		}
	}

	public static class Host {
		protected static final QName ATT_NAME = new QName("name");
		protected static final QName ATT_ALIAS = new QName("alias");

		private String name;
		private String alias;
		private final List<Location> location = new ArrayList<>();
		private AccessLog accessLog;
		private final List<FilterRef> filterRef = new ArrayList<>();

		// <xs:element name="console-access-log" type="consoleAccessLogType" minOccurs="0"/>

		public static Host create(Map<QName, String> attributes, Locator locator) {
			Host host = new Host();
			host.name = attributes.get(ATT_NAME);
			host.alias = attributes.get(ATT_ALIAS);

			return host;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public List<Location> getLocations() {
			return location;
		}

		public AccessLog getAccessLog() {
			return accessLog;
		}

		public void setAccessLog(AccessLog accessLog) {
			this.accessLog = accessLog;
		}

		public List<FilterRef> getFilterRefs() {
			return filterRef;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(", alias: ").append(alias);
			sb.append(", locations: ").append(location);
			sb.append(", access log: ").append(accessLog);
			sb.append(", filter refs: ").append(filterRef);
			sb.append(" }");
			return sb.toString();
		}

		public static class Location {
			protected static final QName ATT_NAME = new QName("name");
			protected static final QName ATT_HANDLER = new QName("handler");

			private String name;
			private String handler;
			private final List<FilterRef> filterRef = new ArrayList<>();

			public static Location create(Map<QName, String> attributes, Locator locator) {
				Location location = new Location();
				location.name = attributes.get(ATT_NAME);
				location.handler = attributes.get(ATT_HANDLER);

				return location;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getHandler() {
				return handler;
			}

			public void setHandler(String handler) {
				this.handler = handler;
			}

			public List<FilterRef> getFilterRefs() {
				return filterRef;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("{ ");
				sb.append("name: ").append(name);
				sb.append(", handler: ").append(handler);
				sb.append(", filterRef: ").append(filterRef);
				sb.append(" }");
				return sb.toString();
			}
		}

		public static class AccessLog {
			protected static final QName ATT_PATTERN = new QName("pattern");
			protected static final QName ATT_DIRECTORY = new QName("directory");
			protected static final QName ATT_PREFIX = new QName("prefix");
			protected static final QName ATT_SUFFIX = new QName("suffix");
			protected static final QName ATT_ROTATE = new QName("rotate");

			private String pattern = "common";
			private String directory;
			private String prefix = "access_log.";
			private String suffix = "log";
			private String rotate = "true";
			//<xs:attribute name="worker" use="optional" type="xs:string" default="default"/>
			//<xs:attribute name="relative-to" use="optional" type="xs:string" />
			//<xs:attribute name="use-server-log" use="optional" type="xs:string" default="false"/>
			//<xs:attribute name="extended" use="optional" type="xs:string" default="false" />
			//<xs:attribute name="predicate" use="optional" type="xs:string" />

			public static AccessLog create(Map<QName, String> attributes, Locator locator) {
				AccessLog al = new AccessLog();
				al.pattern = ParserUtils.toStringValue(attributes.get(ATT_PATTERN), locator, "common");
				al.directory = attributes.get(ATT_DIRECTORY);
				al.prefix = ParserUtils.toStringValue(attributes.get(ATT_PREFIX), locator, "access_log.");
				al.suffix = ParserUtils.toStringValue(attributes.get(ATT_SUFFIX), locator, "log");
				al.rotate = ParserUtils.toStringValue(attributes.get(ATT_ROTATE), locator, "true");

				return al;
			}

			public String getPattern() {
				return pattern;
			}

			public void setPattern(String pattern) {
				this.pattern = pattern;
			}

			public String getDirectory() {
				return directory;
			}

			public void setDirectory(String directory) {
				this.directory = directory;
			}

			public String getPrefix() {
				return prefix;
			}

			public void setPrefix(String prefix) {
				this.prefix = prefix;
			}

			public String getSuffix() {
				return suffix;
			}

			public void setSuffix(String suffix) {
				this.suffix = suffix;
			}

			public String getRotate() {
				return rotate;
			}

			public void setRotate(String rotate) {
				this.rotate = rotate;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("{ ");
				sb.append("pattern: '").append(pattern).append('\'');
				sb.append(", directory: ").append(directory);
				sb.append(", prefix: ").append(prefix);
				sb.append(", suffix: ").append(suffix);
				sb.append(", rotate: ").append(rotate);
				sb.append(" }");
				return sb.toString();
			}
		}

		public static class FilterRef {
			protected static final QName ATT_NAME = new QName("name");
			protected static final QName ATT_PREDICATE = new QName("predicate");

			private String name;
			private String predicate;

			public static FilterRef create(Map<QName, String> attributes, Locator locator) {
				FilterRef ref = new FilterRef();
				ref.name = attributes.get(ATT_NAME);
				ref.predicate = attributes.get(ATT_PREDICATE);

				return ref;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getPredicate() {
				return predicate;
			}

			public void setPredicate(String predicate) {
				this.predicate = predicate;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("{ ");
				sb.append("name: ").append(name);
				sb.append(", predicate: ").append(predicate);
				sb.append(" }");
				return sb.toString();
			}
		}
	}

}
