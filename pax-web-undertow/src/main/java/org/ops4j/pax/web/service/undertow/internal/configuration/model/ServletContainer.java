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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import static org.ops4j.pax.web.service.undertow.internal.configuration.model.ObjectFactory.NS_UNDERTOW;

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
		final StringBuilder sb = new StringBuilder("{ ");
		sb.append("name: ").append(name);
		sb.append(", default session timeout: ").append(defaultSessionTimeout);
		sb.append(", jsp config: ").append(jspConfig);
		sb.append(", websockets: ").append(websockets);
		sb.append(", welcome files: ").append(welcomeFiles);
		sb.append(" }");
		return sb.toString();
	}

	@XmlType(name = "jsp-configurationType", namespace = NS_UNDERTOW)
	public static class JspConfig {
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("}");
			return sb.toString();
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
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("path: ").append(path);
			sb.append("}");
			return sb.toString();
		}
	}

	@XmlType(name = "websocketsType", namespace = NS_UNDERTOW)
	public static class Websockets {
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("}");
			return sb.toString();
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
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(" }");
			return sb.toString();
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
			final StringBuilder sb = new StringBuilder("{ ");
			sb.append("name: ").append(name);
			sb.append(", domain: ").append(domain);
			sb.append(", comment: ").append(comment);
			sb.append(", http only: ").append(httpOnly);
			sb.append(", secure: ").append(secure);
			sb.append(", max age: ").append(maxAge);
			sb.append(" }");
			return sb.toString();
		}
	}

}
