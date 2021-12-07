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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.ops4j.pax.web.service.undertow.internal.configuration.ParserUtils;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

public class ServletContainer {

	protected static final QName ATT_NAME = new QName("name");
	protected static final QName ATT_DEFAULT_SESSION_TIMEOUT = new QName("default-session-timeout");

	private String name;

	private String defaultSessionTimeout = "30";

	private JspConfig jspConfig;

	private PersistentSessionsConfig persistentSessions;

	private Websockets websockets;

	private final List<WelcomeFile> welcomeFiles = new ArrayList<>();

	private SessionCookie sessionCookie;

	public static ServletContainer create(Map<QName, String> attributes, Locator locator) {
		ServletContainer container = new ServletContainer();
		container.name = attributes.get(ATT_NAME);
		container.defaultSessionTimeout = attributes.get(ATT_DEFAULT_SESSION_TIMEOUT);

		return container;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefaultSessionTimeout() {
		return defaultSessionTimeout;
	}

	public void setDefaultSessionTimeout(String defaultSessionTimeout) {
		this.defaultSessionTimeout = defaultSessionTimeout;
	}

	public JspConfig getJspConfig() {
		return jspConfig;
	}

	public void setJspConfig(JspConfig jspConfig) {
		this.jspConfig = jspConfig;
	}

	public PersistentSessionsConfig getPersistentSessions() {
		return persistentSessions;
	}

	public void setPersistentSessions(PersistentSessionsConfig persistentSessions) {
		this.persistentSessions = persistentSessions;
	}

	public Websockets getWebsockets() {
		return websockets;
	}

	public void setWebsockets(Websockets websockets) {
		this.websockets = websockets;
	}

	public List<WelcomeFile> getWelcomeFiles() {
		return welcomeFiles;
	}

	public SessionCookie getSessionCookie() {
		return sessionCookie;
	}

	public void setSessionCookie(SessionCookie sessionCookie) {
		this.sessionCookie = sessionCookie;
	}

	@Override
	public String toString() {
		return "{ name: " + name +
				", default session timeout: " + defaultSessionTimeout +
				", jsp config: " + jspConfig +
				", session cookie config: " + sessionCookie +
				", websockets: " + websockets +
				", welcome files: " + welcomeFiles +
				" }";
	}

	public static class JspConfig {
		@Override
		public String toString() {
			return "{ }";
		}
	}

	public static class PersistentSessionsConfig {
		private static final QName ATT_PATH = new QName("path");

		private String path;

		public static PersistentSessionsConfig create(Map<QName, String> attributes, Locator locator) {
			PersistentSessionsConfig config = new PersistentSessionsConfig();
			config.path = attributes.get(ATT_PATH);

			return config;
		}

		/**
		 * The path to store the session data. If not specified the data will just be stored in memory only.
		 *
		 * @return
		 */
		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public String toString() {
			return "{ path: " + path + "}";
		}
	}

	public static class Websockets {
		protected static final QName ATT_WORKER = new QName("worker");
		protected static final QName ATT_BUFFER_POOL = new QName("buffer-pool");
		protected static final QName ATT_DISPATCH_TO_WORKER = new QName("dispatch-to-worker");
		protected static final QName ATT_PER_MESSAGE_DEFLATE = new QName("per-message-deflate");
		protected static final QName ATT_DEFLATER_LEVEL = new QName("deflater-level");

		protected String workerName = "default";
		protected String bufferPoolName = "default";
		protected boolean dispatchToWorker = true;
		protected boolean perMessageDeflate = false;
		protected int deflaterLevel = 1; // java.util.zip.Deflater.BEST_SPEED

		public static Websockets create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			Websockets ws = new Websockets();
			ws.workerName = ParserUtils.toStringValue(attributes.get(ATT_WORKER), locator, "default");
			ws.bufferPoolName = ParserUtils.toStringValue(attributes.get(ATT_BUFFER_POOL), locator, "default");
			ws.dispatchToWorker = ParserUtils.toBoolean(attributes.get(ATT_DISPATCH_TO_WORKER), locator, true);
			ws.perMessageDeflate = ParserUtils.toBoolean(attributes.get(ATT_PER_MESSAGE_DEFLATE), locator, false);
			ws.deflaterLevel = ParserUtils.toInteger(attributes.get(ATT_DEFLATER_LEVEL), locator, 1);

			return ws;
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

		public boolean isDispatchToWorker() {
			return dispatchToWorker;
		}

		public void setDispatchToWorker(boolean dispatchToWorker) {
			this.dispatchToWorker = dispatchToWorker;
		}

		public boolean isPerMessageDeflate() {
			return perMessageDeflate;
		}

		public void setPerMessageDeflate(boolean perMessageDeflate) {
			this.perMessageDeflate = perMessageDeflate;
		}

		public int getDeflaterLevel() {
			return deflaterLevel;
		}

		public void setDeflaterLevel(int deflaterLevel) {
			this.deflaterLevel = deflaterLevel;
		}

		@Override
		public String toString() {
			return "{ worker name: " + workerName +
					", buffer pool name: " + bufferPoolName +
					", dispatch: " + dispatchToWorker +
					", per message deflate: " + perMessageDeflate +
					", deflater level: " + deflaterLevel +
					" }";
		}
	}

	public static class WelcomeFile {
		protected static final QName ATT_NAME = new QName("name");

		private String name;

		public static WelcomeFile create(Map<QName, String> attributes, Locator locator) {
			WelcomeFile wf = new WelcomeFile();
			wf.name = attributes.get(ATT_NAME);

			return wf;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "{ name: " + name + " }";
		}
	}

	public static class SessionCookie {
		protected static final QName ATT_NAME = new QName("name");
		protected static final QName ATT_DOMAIN = new QName("domain");
		protected static final QName ATT_COMMENT = new QName("comment");
		protected static final QName ATT_HTTP_ONLY = new QName("http-only");
		protected static final QName ATT_SECURE = new QName("secure");
		protected static final QName ATT_MAX_AGE = new QName("max-age");

		private String name;
		private String domain;
		private String comment;
		private boolean httpOnly = true;
		private boolean secure = true;
		private Integer maxAge;

		public static SessionCookie create(Map<QName, String> attributes, Locator locator) throws SAXParseException {
			SessionCookie config = new SessionCookie();
			config.name = attributes.get(ATT_NAME);
			config.domain = attributes.get(ATT_DOMAIN);
			config.comment = attributes.get(ATT_COMMENT);
			config.httpOnly = ParserUtils.toBoolean(attributes.get(ATT_HTTP_ONLY), locator, true);
			config.secure = ParserUtils.toBoolean(attributes.get(ATT_SECURE), locator, true);
			config.maxAge = ParserUtils.toInteger(attributes.get(ATT_MAX_AGE), locator, null);

			return config;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public boolean isHttpOnly() {
			return httpOnly;
		}

		public void setHttpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public Integer getMaxAge() {
			return maxAge;
		}

		public void setMaxAge(Integer maxAge) {
			this.maxAge = maxAge;
		}

		@Override
		public String toString() {
			return "{ name: " + name +
					", domain: " + domain +
					", comment: " + comment +
					", http only: " + httpOnly +
					", secure: " + secure +
					", max age: " + maxAge +
					" }";
		}
	}

}
