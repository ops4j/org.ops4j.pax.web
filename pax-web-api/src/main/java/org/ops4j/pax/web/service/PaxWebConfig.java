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
package org.ops4j.pax.web.service;

import javax.servlet.ServletContext;

/**
 * <p>Dedicated interface with constants related to configuration. Other constants reside
 * in {@link PaxWebConstants}.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code WEB_CFG_} - for system or context property names</li>
 *     <li>{@code PID_CFG_} - for property names found in {@code org.ops4j.pax.web} PID</li>
 *     <li>!! {@code PROPERTY_JSP_} - for property names found in {@code org.ops4j.pax.web} PID</li>
 * </ul></p>
 */
public interface PaxWebConfig {

	// --- server configuration properties

	/**
	 * Servlet API 4, 4.8.1 "Temporary Working Directories". According to specification, it should be different for
	 * each {@link ServletContext}, but we also need single global temporary directory.
	 */
	String PID_CFG_TEMP_DIR = ServletContext.TEMPDIR;

	// 102.9 Configuration Properties - not specified in any interface/class

	/**
	 * This property specifies the port used for servlets and resources accessible via HTTP.
	 * The default value for this property is {@code 80}.
	 */
	String PID_CFG_HTTP_PORT = "org.osgi.service.http.port";

	/**
	 * This property specifies the port used for servlets and resources accessible via HTTPS.
	 * The default value for this property is {@code 443}.
	 */
	String PID_CFG_HTTP_PORT_SECURE = "org.osgi.service.http.port.secure";

	// --- security configuration properties

}
