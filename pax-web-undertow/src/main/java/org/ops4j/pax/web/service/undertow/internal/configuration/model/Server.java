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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlType(name = "serverType", namespace = NS_UNDERTOW, propOrder = {
		"httpListener",
		"httpsListener",
		"host"
})
public class Server {

	@XmlAttribute
	private String name;

	@XmlElement(name = "http-listener")
	private HttpListener httpListener;

	@XmlElement(name = "https-listener")
	private HttpsListener httpsListener;

	@XmlElement
	private Host host;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HttpListener getHttpListener() {
		return httpListener;
	}

	public void setHttpListener(HttpListener httpListener) {
		this.httpListener = httpListener;
	}

	public HttpsListener getHttpsListener() {
		return httpsListener;
	}

	public void setHttpsListener(HttpsListener httpsListener) {
		this.httpsListener = httpsListener;
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
		sb.append("\n\t\t\thttp listener: ").append(httpListener);
		sb.append("\n\t\t\thttps listener: ").append(httpsListener);
		sb.append("\n\t\t\thost: ").append(host);
		sb.append("\n\t\t}");
		return sb.toString();
	}

	@XmlType(name = "socket-options-type", namespace = NS_UNDERTOW)
	public static abstract class SocketOptions {
		@XmlAttribute(name="receive-buffer")
		protected int receiveBuffer = 0;
		@XmlAttribute(name="send-buffer")
		protected int sendBuffer = 0;
		@XmlAttribute(name="tcp-backlog")
		protected int tcpBacklog = 10000;
		@XmlAttribute(name="tcp-keep-alive")
		protected boolean tcpKeepAlive = false;
		@XmlAttribute(name="read-timeout")
		protected long readTimeoout = 0L;
		@XmlAttribute(name="write-timeout")
		protected long writeTimeoout = 0L;
		@XmlAttribute(name="max-connections")
		protected int maxConnections = 0;

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

		public long getReadTimeoout() {
			return readTimeoout;
		}

		public void setReadTimeoout(long readTimeoout) {
			this.readTimeoout = readTimeoout;
		}

		public long getWriteTimeoout() {
			return writeTimeoout;
		}

		public void setWriteTimeoout(long writeTimeoout) {
			this.writeTimeoout = writeTimeoout;
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
			sb.append("receive buffer: ").append(receiveBuffer);
			sb.append(", send buffer: ").append(sendBuffer);
			sb.append(", tcp backlog: ").append(tcpBacklog);
			sb.append(", tcp KeepAlive: ").append(tcpKeepAlive);
			sb.append(", read timeoout: ").append(readTimeoout);
			sb.append(", write timeoout: ").append(writeTimeoout);
			sb.append(", max connections: ").append(maxConnections);
			sb.append(" }");
			return sb.toString();
		}
	}

	@XmlType(name = "listener-type", namespace = NS_UNDERTOW)
	public static abstract class Listener extends SocketOptions {
		@XmlAttribute
		protected String name;
		@XmlAttribute(name = "socket-binding")
		protected String socketBindingName;
		@XmlAttribute(name = "buffer-pool")
		protected String bufferPoolName = "default";
		@XmlAttribute
		protected boolean enabled = true;
		@XmlAttribute(name = "url-charset")
		protected String urlCharset = "UTF-8";
		@XmlAttribute
		protected boolean secure = false;
		@XmlAttribute(name = "record-request-start-time")
		protected boolean recordRequestStartTime = false;

		//<xs:attribute name="worker" type="xs:string" default="default"/>
		//<xs:attribute name="resolve-peer-address" type="xs:boolean" default="false"/>
		//<xs:attribute name="max-post-size" type="xs:long" default="10485760"/>
		//<xs:attribute name="buffer-pipelined-data" type="xs:boolean" default="false"/>
		//<xs:attribute name="max-header-size" type="xs:long" default="1048576"/>
		//<xs:attribute name="max-parameters" type="xs:long" default="1000"/>
		//<xs:attribute name="max-headers" type="xs:long" default="200"/>
		//<xs:attribute name="max-cookies" type="xs:long" default="200"/>
		//<xs:attribute name="allow-encoded-slash" type="xs:boolean" default="false"/>
		//<xs:attribute name="decode-url" type="xs:boolean" default="true"/>
		//<xs:attribute name="always-set-keep-alive" type="xs:boolean" default="true"/>
		//<xs:attribute name="max-buffered-request-size" type="xs:long" default="16384"/>
		//<xs:attribute name="allow-equals-in-cookie-value" type="xs:boolean" default="false"/>
		//<xs:attribute name="no-request-timeout" type="xs:int" default="60000"/>
		//<xs:attribute name="request-parse-timeout" type="xs:int"/>
		//<xs:attribute name="disallowed-methods" type="stringList" default="TRACE"/>
		//<xs:attribute name="rfc6265-cookie-validation" type="xs:boolean" default="false"/>

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

		public String getUrlCharset() {
			return urlCharset;
		}

		public void setUrlCharset(String urlCharset) {
			this.urlCharset = urlCharset;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public boolean isRecordRequestStartTime() {
			return recordRequestStartTime;
		}

		public void setRecordRequestStartTime(boolean recordRequestStartTime) {
			this.recordRequestStartTime = recordRequestStartTime;
		}
	}

	@XmlType(name = "http-listener-type", namespace = NS_UNDERTOW)
	public static class HttpListener extends Listener {
		@XmlAttribute(name = "redirect-socket")
		private String redirectSocket;
		//<xs:attribute name="certificate-forwarding" use="optional" type="xs:string" default="false">
		//<xs:attribute name="redirect-socket" use="optional" type="xs:string">
		@XmlAttribute(name = "proxy-address-forwarding")
		private String proxyAddressForwarding;
		@XmlAttribute(name = "resolve-peer-address")
		private String peerHostLookup;
		//<xs:attribute name="enable-http2" use="optional" type="xs:string">
		//<xs:attribute name="http2-enable-push" type="xs:boolean" use="optional" />
		//<xs:attribute name="http2-header-table-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-initial-window-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-concurrent-streams" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-frame-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-header-list-size" type="xs:int" use="optional" />
		//<xs:attribute name="require-host-http11" type="xs:boolean" use="optional" default="false"/>

		public String getRedirectSocket() {
			return redirectSocket;
		}

		public void setRedirectSocket(String redirectSocket) {
			this.redirectSocket = redirectSocket;
		}

		public String getProxyAddressForwarding() {
			return proxyAddressForwarding;
		}

		public void setProxyAddressForwarding(String proxyAddressForwarding) {
			this.proxyAddressForwarding = proxyAddressForwarding;
		}

		public String getPeerHostLookup() {
	                return peerHostLookup;
	        }

	        public void setPeerHostLookup(String peerHostLookup) {
	                this.peerHostLookup = peerHostLookup;
	        }
	        
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(", receive buffer: ").append(receiveBuffer);
			sb.append(", send buffer: ").append(sendBuffer);
			sb.append(", tcp backlog: ").append(tcpBacklog);
			sb.append(", tcp KeepAlive: ").append(tcpKeepAlive);
			sb.append(", read timeoout: ").append(readTimeoout);
			sb.append(", write timeoout: ").append(writeTimeoout);
			sb.append(", max connections: ").append(maxConnections);
			sb.append(", socket binding name: ").append(socketBindingName);
			sb.append(", buffer pool name: ").append(bufferPoolName);
			sb.append(", enabled: ").append(enabled);
			sb.append(", url charset: ").append(urlCharset);
			sb.append(", secure: ").append(secure);
			sb.append(", redirect socket: ").append(redirectSocket);
			sb.append(", proxy address forwarding: ").append(proxyAddressForwarding);
			sb.append(", peer host lookup: ").append(peerHostLookup);
			sb.append(" }");
			return sb.toString();
		}

	}

	@XmlType(name = "https-listener-type", namespace = NS_UNDERTOW)
	public static class HttpsListener extends HttpListener {
		// new in urn:jboss:domain:undertow:4.0 - but unimplemented in pax-web-undertow
		@XmlAttribute(name="ssl-context")
		private String sslContext;
		// legacy in urn:jboss:domain:undertow:4.0 - but still used in pax-web
		@XmlAttribute(name="security-realm")
		private String securityRealm;
		@XmlAttribute(name="verify-client")
		private String verifyClient;
		@XmlAttribute(name="enabled-cipher-suites")
		private List<String> enabledCipherSuites = new ArrayList<>();
		@XmlAttribute(name="enabled-protocols")
		private List<String> enabledProtocols = new ArrayList<>();
		//<xs:attribute name="certificate-forwarding" use="optional" type="xs:string" default="false">
		@XmlAttribute(name = "proxy-address-forwarding")
		private String proxyAddressForwarding;
		@XmlAttribute(name = "resolve-peer-address")
                private String peerHostLookup;
		//<xs:attribute name="enable-http2" use="optional" type="xs:string">
		//<xs:attribute name="enable-spdy" use="optional" type="xs:string">
		//<xs:attribute name="ssl-session-cache-size" use="optional" type="xs:string"/>
		//<xs:attribute name="ssl-session-timeout" use="optional" type="xs:string"/>
		//<xs:attribute name="http2-enable-push" type="xs:boolean" use="optional" />
		//<xs:attribute name="http2-header-table-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-initial-window-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-concurrent-streams" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-frame-size" type="xs:int" use="optional" />
		//<xs:attribute name="http2-max-header-list-size" type="xs:int" use="optional" />
		//<xs:attribute name="require-host-http11" type="xs:boolean" use="optional" default="false"/>

		public String getSslContext() {
			return sslContext;
		}

		public void setSslContext(String sslContext) {
			this.sslContext = sslContext;
		}

		public String getSecurityRealm() {
			return securityRealm;
		}

		public void setSecurityRealm(String securityRealm) {
			this.securityRealm = securityRealm;
		}

		public String getVerifyClient() {
			return verifyClient;
		}

		public void setVerifyClient(String verifyClient) {
			this.verifyClient = verifyClient;
		}

		public List<String> getEnabledCipherSuites() {
			return enabledCipherSuites;
		}

		public List<String> getEnabledProtocols() {
			return enabledProtocols;
		}

		@Override
		public String getProxyAddressForwarding() {
			return proxyAddressForwarding;
		}

		@Override
		public void setProxyAddressForwarding(String proxyAddressForwarding) {
			this.proxyAddressForwarding = proxyAddressForwarding;
		}
		
		@Override
		public String getPeerHostLookup() {
                    return peerHostLookup;
                }

		@Override
                public void setPeerHostLookup(String peerHostLookup) {
                    this.peerHostLookup = peerHostLookup;
                }

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(", receive buffer: ").append(receiveBuffer);
			sb.append(", send buffer: ").append(sendBuffer);
			sb.append(", tcp backlog: ").append(tcpBacklog);
			sb.append(", tcp KeepAlive: ").append(tcpKeepAlive);
			sb.append(", read timeoout: ").append(readTimeoout);
			sb.append(", write timeoout: ").append(writeTimeoout);
			sb.append(", max connections: ").append(maxConnections);
			sb.append(", socket binding name: ").append(socketBindingName);
			sb.append(", buffer pool name: ").append(bufferPoolName);
			sb.append(", enabled: ").append(enabled);
			sb.append(", url charset: ").append(urlCharset);
			sb.append(", secure: ").append(secure);
			sb.append(", security realm: ").append(securityRealm);
			sb.append(", verify client: ").append(verifyClient);
			sb.append(", enabled cipher suites: ").append(enabledCipherSuites);
			sb.append(", enabled protocols: ").append(enabledProtocols);
			sb.append(", proxy address forwarding: ").append(proxyAddressForwarding);
			sb.append(", peer host lookup: ").append(peerHostLookup);
			sb.append(" }");
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
		private List<Location> location = new ArrayList<>();
		@XmlElement(name = "access-log")
		private AccessLog accessLog;
		@XmlElement(name = "filter-ref")
		private List<FilterRef> filterRef = new ArrayList<>();

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
			private List<FilterRef> filterRef = new ArrayList<>();

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

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			@Override
			public String toString() {
				final StringBuilder sb = new StringBuilder("{ ");
				sb.append("name: ").append(name);
				sb.append(" }");
				return sb.toString();
			}
		}
	}

}
