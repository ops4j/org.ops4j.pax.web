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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.configuration.model.ObjectFactory.NS_UNDERTOW;

@XmlType(name = "servletContainerType", namespace = NS_UNDERTOW, propOrder = {
		"jspConfig",
		"persistentSessions",
		"websockets",
		"welcomeFiles",
		"sessionCookie"
})
public class ServletContainer {

	@XmlAttribute
	private String name;

	@XmlAttribute(name = "default-session-timeout")
	private String defaultSessionTimeout = "30";

	@XmlElement(name = "jsp-config")
	private JspConfig jspConfig;

	@XmlElement(name = "persistent-sessions")
	private PersistentSessionsConfig persistentSessions;

	@XmlElement
	private Websockets websockets;

	@XmlElementWrapper(name = "welcome-files")
	@XmlElement(name = "welcome-file")
	private final List<WelcomeFile> welcomeFiles = new ArrayList<>();

	@XmlElement(name = "session-cookie")
	private SessionCookie sessionCookie;

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
				", websockets: " + websockets +
				", welcome files: " + welcomeFiles +
				" }";
	}

	@XmlType(name = "jsp-configurationType", namespace = NS_UNDERTOW)
	public static class JspConfig {
		@Override
		public String toString() {
			return "{ }";
		}
	}

	@XmlType(name = "persistent-sessionsType", namespace = NS_UNDERTOW)
	public static class PersistentSessionsConfig {
		@XmlAttribute
		private String path;

		/**
		 * The path to store the session data. If not specified the data will just be stored in memory only.
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

	@XmlType(name = "websocketsType", namespace = NS_UNDERTOW)
	public static class Websockets {
		@XmlAttribute(name = "worker")
		protected String workerName = "default";
		@XmlAttribute(name = "buffer-pool")
		protected String bufferPoolName = "default";
		@XmlAttribute(name = "dispatch-to-worker")
		protected boolean dispatchToWorker = true;
		@XmlAttribute(name = "per-message-deflate")
		protected boolean perMessageDeflate = false;
		@XmlAttribute(name = "deflater-level")
		protected int deflaterLevel = 1; // java.util.zip.Deflater.BEST_SPEED

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

	@XmlType(name = "welcome-fileType", namespace = NS_UNDERTOW)
	public static class WelcomeFile {
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
			return "{ name: " + name + " }";
		}
	}

	@XmlType(name = "session-cookieType", namespace = NS_UNDERTOW)
	public static class SessionCookie {
		@XmlAttribute
		private String name;
		@XmlAttribute
		private String domain;
		@XmlAttribute
		private String comment;
		@XmlAttribute(name = "http-only")
		private boolean httpOnly;
		@XmlAttribute
		private boolean secure;
		@XmlAttribute(name = "max-age")
		private Integer maxAge;

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
