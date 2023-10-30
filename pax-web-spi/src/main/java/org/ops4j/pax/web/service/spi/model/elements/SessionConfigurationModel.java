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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.HashSet;
import java.util.Set;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.servlet.DefaultSessionCookieConfig;

/**
 * <p>Set of parameters configuring HTTP Sessions, referenced from {@link OsgiContextModel}.</p>
 *
 * <p>This model reflects {@code <session-config>} element from {@code web.xml}, while some server-specific
 * information (like Jetty's {@code sessionIdPathParameterName}) can be specified globally through
 * {@link org.ops4j.pax.web.service.spi.config.Configuration}.</p>
 */
public class SessionConfigurationModel {

	/** {@code <session-config>/<session-timeout>} */
	private Integer sessionTimeout;

	/** {@code <session-config>/<cookie-config>} */
	private SessionCookieConfig sessionCookieConfig = new DefaultSessionCookieConfig();

	/** {@code <session-config>/<tracking-mode>} */
	private final Set<SessionTrackingMode> trackingModes = new HashSet<>();

	// The configuration of sessions outside web.xml is server specific and covers:
	// Jetty (org.eclipse.jetty.server.session.SessionHandler):
	//  - CheckingRemoteSessionIdEncoding(boolean) - "if absolute URLs are check for remoteness before being session encoded"
	//  - MaxInactiveInterval(int) - "max period of inactivity, after which the session is invalidated, in seconds"
	//  - NodeIdInSessionId(boolean) - "if the cluster node id (worker id) will be returned as part of the session id by HttpSession#getId(). Default is false"
	//  - RefreshCookieAge(int) - "time before a session cookie is re-set (in s)"
	//  - SameSite(enum None/Strict/Lax) - "Session cookie sameSite mode. Currently this is encoded in the session comment until sameSite is supported by SessionCookieConfig"
	//  - SecureRequestOnly(boolean) (different than SessionCookieConfig.setSecure()) - "if session cookie is to be marked as secure only on HTTPS requests"
	//  - SessionIdPathParameterName(string), default "jsessionid" - "the URL path parameter name for session id URL rewriting (null or "none" for no rewriting)"
	//  - UsingCookies(boolean), default true, set if javax.servlet.SessionTrackingMode.COOKIE is used
	// Tomcat (org.apache.catalina.core.StandardContext):
	//  - sessionCookiePathUsesTrailingSlash(boolean), default false - "Is a / added to the end of the session cookie path to ensure browsers, particularly IE, don't send a session cookie for context /foo with requests intended for context /foobar."
	// Undertow:
	//  - io.undertow.servlet.api.DeploymentInfo.servletSessionConfig
	//  - io.undertow.servlet.api.DeploymentInfo.sessionManagerFactory
	//  - io.undertow.servlet.api.DeploymentInfo.sessionPersistenceManager
	//
	// Additionaly we should be able to configure "file session persistence" - in all server runtimes:
	//  - org.eclipse.jetty.server.session.FileSessionDataStoreFactory
	//  - org.apache.catalina.session.StandardManager/org.apache.catalina.session.FileStore
	//  - io.undertow.server.session.SessionManager/io.undertow.servlet.api.SessionPersistenceManager (no built-in file persistence manager in Undertow)

	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(Integer sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public SessionCookieConfig getSessionCookieConfig() {
		return sessionCookieConfig;
	}

	public void setSessionCookieConfig(SessionCookieConfig sessionCookieConfig) {
		this.sessionCookieConfig = sessionCookieConfig;
	}

	public Set<SessionTrackingMode> getTrackingModes() {
		return trackingModes;
	}

}
