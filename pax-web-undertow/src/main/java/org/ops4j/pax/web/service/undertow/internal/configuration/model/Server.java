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
package org.ops4j.pax.web.service.undertow.internal.configuration.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import io.undertow.protocols.http2.Http2Channel;
import org.xnio.SslClientAuthMode;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlType(name = "serverType", namespace = NS_UNDERTOW, propOrder = {
		"httpListeners",
		"httpsListeners",
		"host"
})
public class Server {

	@XmlAttribute
	private String name;

	@XmlElement(name = "http-listener")
	private final List<HttpListener> httpListeners = new LinkedList<>();

	@XmlElement(name = "https-listener")
	private final List<HttpsListener> httpsListeners = new LinkedList<>();

	@XmlElement
	private Host host;

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
		final StringBuilder sb = new StringBuilder("{\n");
		sb.append("\t\t\tname: ").append(name);
		sb.append("\n\t\t\thttp listeners: ").append(httpListeners);
		sb.append("\n\t\t\thttps listeners: ").append(httpsListeners);
		sb.append("\n\t\t\thost: ").append(host);
		sb.append("\n\t\t}");
		return sb.toString();
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

	@XmlType(name = "socket-options-type", namespace = NS_UNDERTOW)
	public abstract static class SocketOptions {

		// org.wildfly.extension.undertow.ListenerResourceDefinition#SOCKET_OPTIONS

		/**
		 * XNIO: org.xnio.Options#RECEIVE_BUFFER, Java: java.net.ServerSocket#setReceiveBufferSize(int),
		 * java.net.SocketOptions#SO_RCVBUF, default: 0x10000 (org.xnio.nio.AbstractNioChannel#DEFAULT_BUFFER_SIZE)
		 */
		@XmlAttribute(name = "receive-buffer")
		protected int receiveBuffer = 0x10000;

		/**
		 * XNIO: org.xnio.Options#SEND_BUFFER, Java: java.net.Socket#setSendBufferSize(int),
		 * java.net.SocketOptions#SO_SNDBUF, default: 0x10000 (org.xnio.nio.AbstractNioChannel#DEFAULT_BUFFER_SIZE)
		 */
		@XmlAttribute(name = "send-buffer")
		protected int sendBuffer = 0x10000;

		/**
		 * XNIO: org.xnio.Options#BACKLOG, Java: 2nd parameter of
		 * java.net.ServerSocket#bind(java.net.SocketAddress, int), default: 128 or 50 when using 1-arg bind().
		 */
		@XmlAttribute(name = "tcp-backlog")
		protected int tcpBacklog = 128;

		/**
		 * XNIO: org.xnio.Options#KEEP_ALIVE, Java: java.net.Socket#setKeepAlive(boolean),
		 * java.net.SocketOptions#SO_KEEPALIVE, default: false
		 */
		@XmlAttribute(name = "tcp-keep-alive")
		protected boolean tcpKeepAlive = false;

		/**
		 * XNIO: org.xnio.Options#READ_TIMEOUT (in ms), default: 0
		 */
		@XmlAttribute(name = "read-timeout")
		protected int readTimeout = 0;

		/**
		 * XNIO: org.xnio.Options#WRITE_TIMEOUT (in ms), default: 0
		 */
		@XmlAttribute(name = "write-timeout")
		protected int writeTimeout = 0;

		/**
		 * XNIO: org.xnio.Options#CONNECTION_HIGH_WATER
		 */
		@XmlAttribute(name = "max-connections")
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
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append(toStringParameters());
			sb.append(" }");
			return sb.toString();
		}

		protected String toStringParameters() {
			final StringBuilder sb = new StringBuilder();
			sb.append("receive buffer: ").append(receiveBuffer);
			sb.append(", send buffer: ").append(sendBuffer);
			sb.append(", tcp backlog: ").append(tcpBacklog);
			sb.append(", tcp KeepAlive: ").append(tcpKeepAlive);
			sb.append(", read timeoout: ").append(readTimeout);
			sb.append(", write timeoout: ").append(writeTimeout);
			sb.append(", max connections: ").append(maxConnections);
			return sb.toString();
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

	@XmlType(name = "listener-type", namespace = NS_UNDERTOW)
	public abstract static class Listener extends SocketOptions {

		@XmlAttribute
		protected String name;

		// meta information
		// generic attributes defined in static block of org.wildfly.extension.undertow.ListenerResourceDefinition
		// ATTRIBUTES = new LinkedHashSet<>(Arrays.asList(SOCKET_BINDING, WORKER, BUFFER_POOL, ENABLED, RESOLVE_PEER_ADDRESS, DISALLOWED_METHODS, SECURE));

		@XmlAttribute(name = "socket-binding")
		protected String socketBindingName;
		@XmlAttribute(name = "worker")
		protected String workerName = "default";
		@XmlAttribute(name = "buffer-pool")
		protected String bufferPoolName = "default";
		@XmlAttribute
		protected boolean enabled = true;
		@XmlAttribute(name = "resolve-peer-address")
		protected boolean resolvePeerAddress = false;
		@XmlAttribute(name = "disallowed-methods")
		protected List<String> disallowedMethods = new LinkedList<>(Collections.singletonList("TRACE"));
		@XmlAttribute
		protected boolean secure = false;

		// org.wildfly.extension.undertow.ListenerResourceDefinition#LISTENER_OPTIONS

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_ENTITY_SIZE
		 */
		@XmlAttribute(name = "max-post-size")
		protected long maxPostSize = 10485760L;

		/**
		 * Undertow: io.undertow.UndertowOptions#BUFFER_PIPELINED_DATA
		 */
		@XmlAttribute(name = "buffer-pipelined-data")
		protected boolean bufferPipelinedData = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_HEADER_SIZE
		 */
		@XmlAttribute(name = "max-header-size")
		protected int maxHeaderSize = 1048576;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_PARAMETERS
		 */
		@XmlAttribute(name = "max-parameters")
		protected int maxParameters = 1000;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_HEADERS
		 */
		@XmlAttribute(name = "max-headers")
		protected int maxHeaders = 200;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_COOKIES
		 */
		@XmlAttribute(name = "max-cookies")
		protected int maxCookies = 200;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_ENCODED_SLASH
		 */
		@XmlAttribute(name = "allow-encoded-slash")
		protected boolean allowEncodedSlash = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#DECODE_URL
		 */
		@XmlAttribute(name = "decode-url")
		protected boolean decodeUrl = true;

		/**
		 * Undertow: io.undertow.UndertowOptions#URL_CHARSET
		 */
		@XmlAttribute(name = "url-charset")
		protected String urlCharset = StandardCharsets.UTF_8.name();

		/**
		 * Undertow: io.undertow.UndertowOptions#ALWAYS_SET_KEEP_ALIVE
		 */
		@XmlAttribute(name = "always-set-keep-alive")
		protected boolean alwaysSetKeepAlive = true;

		/**
		 * Undertow: io.undertow.UndertowOptions#MAX_BUFFERED_REQUEST_SIZE
		 */
		@XmlAttribute(name = "max-buffered-request-size")
		protected int maxBufferedRequestSize = 16384;

		/**
		 * Undertow: io.undertow.UndertowOptions#RECORD_REQUEST_START_TIME
		 */
		@XmlAttribute(name = "record-request-start-time")
		protected boolean recordRequestStartTime = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_EQUALS_IN_COOKIE_VALUE
		 */
		@XmlAttribute(name = "allow-equals-in-cookie-value")
		protected boolean allowEqualsInCookieValue = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#NO_REQUEST_TIMEOUT
		 */
		@XmlAttribute(name = "no-request-timeout")
		protected int noRequestTimeout = 60000;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUEST_PARSE_TIMEOUT
		 */
		@XmlAttribute(name = "request-parse-timeout")
		protected int requestParseTimeout = 60000;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_RFC6265_COOKIE_VALIDATION
		 */
		@XmlAttribute(name = "rfc6265-cookie-validation")
		protected boolean rfc6265CookieValidation = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ALLOW_UNESCAPED_CHARACTERS_IN_URL
		 */
		@XmlAttribute(name = "allow-unescaped-characters-in-url")
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

	@XmlType(name = "http-listener-type", namespace = NS_UNDERTOW)
	public static class HttpListener extends Listener {

		@XmlAttribute(name = "certificate-forwarding")
		private boolean certificateForwarding = false;

		@XmlAttribute(name = "redirect-socket")
		private String redirectSocket;

		@XmlAttribute(name = "proxy-address-forwarding")
		private boolean proxyAddressForwarding = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_HTTP2
		 */
		@XmlAttribute(name = "enable-http2")
		private boolean enableHttp2 = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_ENABLE_PUSH
		 */
		@XmlAttribute(name = "http2-enable-push")
		private boolean http2EnablePush = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE, defaults to
		 * io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT
		 */
		@XmlAttribute(name = "http2-header-table-size")
		private int http2HeaderTableSize = io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, defaults to
		 * io.undertow.protocols.http2.Http2Channel#DEFAULT_INITIAL_WINDOW_SIZE
		 */
		@XmlAttribute(name = "http2-initial-window-size")
		private int http2InitialWindowSize = Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS
		 */
		@XmlAttribute(name = "http2-max-concurrent-streams")
		private Integer http2MaxConcurrentStreams;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_FRAME_SIZE
		 */
		@XmlAttribute(name = "http2-max-frame-size")
		private int http2MaxFrameSize = Http2Channel.DEFAULT_MAX_FRAME_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE
		 */
		@XmlAttribute(name = "http2-max-header-list-size")
		private Integer http2MaxHeaderListSize;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUIRE_HOST_HTTP11
		 */
		@XmlAttribute(name = "require-host-http11")
		private boolean requireHostHttp11 = false;

		@XmlAttribute(name = "proxy-protocol")
		private boolean proxyProtocol = false;

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

	@XmlType(name = "https-listener-type", namespace = NS_UNDERTOW)
	public static class HttpsListener extends Listener {

		@XmlAttribute(name = "ssl-context")
		private String sslContext;

		@XmlAttribute(name = "certificate-forwarding")
		private boolean certificateForwarding = false;

		@XmlAttribute(name = "proxy-address-forwarding")
		private boolean proxyAddressForwarding = false;

		// legacy in urn:jboss:domain:undertow:4.0 - but still used in pax-web
		// but "ssl-context" used in Wildfly integrates harder with the Wildfly itself, so "security-realm" is
		// the mechanism to configure security
		@XmlAttribute(name = "security-realm")
		private String securityRealm;

		/**
		 * XNIO: org.xnio.Options#SSL_CLIENT_AUTH_MODE
		 */
		@XmlAttribute(name = "verify-client")
		private SslClientAuthMode verifyClient = SslClientAuthMode.NOT_REQUESTED;

		/**
		 * XNIO: org.xnio.Options#SSL_ENABLED_CIPHER_SUITES
		 */
		@XmlAttribute(name = "enabled-cipher-suites")
		private List<String> enabledCipherSuites = new ArrayList<>();

		/**
		 * XNIO: org.xnio.Options#SSL_ENABLED_PROTOCOLS
		 */
		@XmlAttribute(name = "enabled-protocols")
		private List<String> enabledProtocols = new ArrayList<>();

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_HTTP2
		 */
		@XmlAttribute(name = "enable-http2")
		private boolean enableHttp2 = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#ENABLE_SPDY
		 */
		@XmlAttribute(name = "enable-spdy")
		@Deprecated
		private boolean enableSpdy = false;

		/**
		 * XNIO: org.xnio.Options#SSL_SERVER_SESSION_CACHE_SIZE
		 */
		@XmlAttribute(name = "ssl-session-cache-size")
		private int sslSessionCacheSize = 0;

		//<xs:attribute name="" use="optional" type="xs:string"/>
		/**
		 * XNIO: org.xnio.Options#SSL_SERVER_SESSION_TIMEOUT
		 */
		@XmlAttribute(name = "ssl-session-timeout")
		private int sslSessionTimeout = 0;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_ENABLE_PUSH
		 */
		@XmlAttribute(name = "http2-enable-push")
		private boolean http2EnablePush = false;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE, defaults to
		 * io.undertow.UndertowOptions#HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT
		 */
		@XmlAttribute(name = "http2-header-table-size")
		private int http2HeaderTableSize = io.undertow.UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE_DEFAULT;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, defaults to
		 * io.undertow.protocols.http2.Http2Channel#DEFAULT_INITIAL_WINDOW_SIZE
		 */
		@XmlAttribute(name = "http2-initial-window-size")
		private int http2InitialWindowSize = Http2Channel.DEFAULT_INITIAL_WINDOW_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS
		 */
		@XmlAttribute(name = "http2-max-concurrent-streams")
		private Integer http2MaxConcurrentStreams;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_FRAME_SIZE
		 */
		@XmlAttribute(name = "http2-max-frame-size")
		private int http2MaxFrameSize = Http2Channel.DEFAULT_MAX_FRAME_SIZE;

		/**
		 * Undertow: io.undertow.UndertowOptions#HTTP2_SETTINGS_MAX_HEADER_LIST_SIZE
		 */
		@XmlAttribute(name = "http2-max-header-list-size")
		private Integer http2MaxHeaderListSize;

		/**
		 * Undertow: io.undertow.UndertowOptions#REQUIRE_HOST_HTTP11
		 */
		@XmlAttribute(name = "require-host-http11")
		private boolean requireHostHttp11 = false;

		@XmlAttribute(name = "proxy-protocol")
		private boolean proxyProtocol = false;

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

	@XmlType(name = "hostType", namespace = NS_UNDERTOW, propOrder = {
			"location",
			"accessLog",
			"filterRef"
	})
	public static class Host {

		@XmlAttribute
		private String name;
		@XmlAttribute
		private String alias;
		@XmlElement
		private final List<Location> location = new ArrayList<>();
		@XmlElement(name = "access-log")
		private AccessLog accessLog;
		@XmlElement(name = "filter-ref")
		private final List<FilterRef> filterRef = new ArrayList<>();
		// <xs:element name="console-access-log" type="consoleAccessLogType" minOccurs="0"/>

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

		public List<Location> getLocation() {
			return location;
		}

		public AccessLog getAccessLog() {
			return accessLog;
		}

		public void setAccessLog(AccessLog accessLog) {
			this.accessLog = accessLog;
		}

		public List<FilterRef> getFilterRef() {
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

		@XmlType(name = "locationType", namespace = NS_UNDERTOW)
		public static class Location {

			@XmlAttribute
			private String name;
			@XmlAttribute
			private String handler;
			@XmlElement(name = "filter-ref")
			private final List<FilterRef> filterRef = new ArrayList<>();

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

			public List<FilterRef> getFilterRef() {
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

		@XmlType(name = "accessLogType", namespace = NS_UNDERTOW)
		public static class AccessLog {

			@XmlAttribute
			private String pattern = "common";
			@XmlAttribute
			private String directory;
			@XmlAttribute
			private String prefix = "access_log.";
			@XmlAttribute
			private String suffix = "log";
			@XmlAttribute
			private String rotate = "true";
			//<xs:attribute name="worker" use="optional" type="xs:string" default="default"/>
			//<xs:attribute name="relative-to" use="optional" type="xs:string" />
			//<xs:attribute name="use-server-log" use="optional" type="xs:string" default="false"/>
			//<xs:attribute name="extended" use="optional" type="xs:string" default="false" />
			//<xs:attribute name="predicate" use="optional" type="xs:string" />

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
				sb.append("pattern: ").append(pattern).append('\'');
				sb.append(", directory: ").append(directory);
				sb.append(", prefix: ").append(prefix);
				sb.append(", suffix: ").append(suffix);
				sb.append(", rotate: ").append(rotate);
				sb.append(" }");
				return sb.toString();
			}
		}

		@XmlType(name = "filter-refType", namespace = NS_UNDERTOW)
		public static class FilterRef {

			@XmlAttribute
			private String name;
			@XmlAttribute
			private String predicate;

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
				sb.append("predicate: ").append(predicate);
				sb.append(" }");
				return sb.toString();
			}
		}
	}

}
