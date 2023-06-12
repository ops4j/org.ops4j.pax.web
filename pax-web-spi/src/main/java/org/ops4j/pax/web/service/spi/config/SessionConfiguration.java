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
package org.ops4j.pax.web.service.spi.config;

import java.io.File;
import jakarta.servlet.SessionCookieConfig;

/**
 * <p>While some session configuration parameters (those specified in {@code web.xml} and
 * {@link jakarta.servlet.SessionCookieConfig}) can be specified through {@link org.ops4j.pax.web.service.WebContainer}
 * and during WAR deployment, some server-specific options can be configured only globally through
 * {@link org.ops4j.pax.web.service.PaxWebConstants#PID}.</p>
 *
 * <p>Global (this) configuration is also used when no context-specific session configuration is defined.</p>
 */
public interface SessionConfiguration {

	// --- configuration that matches web.xml and jakarta.servlet.SessionCookieConfig

	/**
	 * {@code <session-config>/<session-timeout>} - returns the time in minutes after which an inative settion times out.
	 * Defaults to 30 minutes (as in Tomcat).
	 * @return timeout in minutes
	 */
	Integer getSessionTimeout();

	/**
	 * {@code <session-config>/<cookie-config>/<name>} - if not specified, defaults to {@code JSESSIONID}
	 * @return
	 */
	String getSessionCookieName();

	/**
	 * {@code <session-config>/<cookie-config>/<domain>}
	 * @return
	 */
	String getSessionCookieDomain();

	/**
	 * {@code <session-config>/<cookie-config>/<path>} - if not specified, it will match the context path.
	 * @return
	 */
	String getSessionCookiePath();

	/**
	 * {@code <session-config>/<cookie-config>/<comment>}
	 * @return
	 */
	String getSessionCookieComment();

	/**
	 * {@code <session-config>/<cookie-config>/<http-only>}
	 * @return
	 */
	Boolean getSessionCookieHttpOnly();

	/**
	 * {@code <session-config>/<cookie-config>/<secure>}
	 * @return
	 */
	Boolean getSessionCookieSecure();

	/**
	 * {@code <session-config>/<cookie-config>/<max-age>}
	 * @return
	 */
	Integer getSessionCookieMaxAge();

	/**
	 * No equivalent in {@code web.xml}. {@code SameSite} attribute for session cookie.
	 * @return
	 */
	String getSessionCookieSameSite();

	// --- configuration that's not related to web.xml or jakarta.servlet.SessionCookieConfig

	/**
	 * Allows configuration of Jetty's SessionHandler.SessionIdPathParameterName. By default it's {@code jsessionid}.
	 * @return
	 */
	String getSessionUrlPathParameter();

	/**
	 * PAXWEB-144: Allows configuration of Jetty's SessionHandler.SessionIdManager.workerName to assist session
	 * affinity in a load balancer.
	 * @return
	 */
	String getSessionWorkerName();

	/**
	 * All server runtimes allow configuration of <em>file session persistence</em> and with this property we
	 * can specify the persistent location (directory) of such session storage.
	 * @return
	 */
	String getSessionStoreDirectoryLocation();

	/**
	 * If {@link #getSessionStoreDirectoryLocation()} returns valid location, this method returns the corresponding
	 * {@link File} object
	 * @return
	 */
	File getSessionStoreDirectory();

	/**
	 * This method gathers some of individual session configuration parameters and returns ready to use
	 * {@link SessionCookieConfig} object.
	 * @return
	 */
	SessionCookieConfig getDefaultSessionCookieConfig();

}
