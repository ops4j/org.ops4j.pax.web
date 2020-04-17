/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.spi.config;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * <p>Interface for accessing configuration that's normally available/provided using Configuration Admin
 * properties or Meta Type information. If possible, server implementations should access Pax Web properties
 * through methods of this interface, not through general property names in some dictionary.</p>
 *
 * <p>Splitting original {@code Configuration} object into <em>groups</em> should help with
 * future extensibility.</p>
 *
 * <p>Actual server implementations should access <strong>entire</strong> configuration using this interface
 * without a need to create extra {@code org.ops4j.util.property.PropertyResolver} instances.</p>
 */
@ProviderType
public interface Configuration {

	/**
	 * Get unique identifier of the configuration. Because
	 * {@link org.ops4j.pax.web.service.spi.ServerControllerFactory} may create more
	 * {@link org.ops4j.pax.web.service.spi.ServerController server controllers}, but only for different
	 * configurations.
	 *
	 * @return
	 */
	String id();

	/**
	 * Accesses server-wide configuration that covers listeners, directory locations, ports, etc.
	 * @return
	 */
	ServerConfiguration server();

	/**
	 * Accesses security related configuration for SSL/TLS, certificates and authentication.
	 * @return
	 */
	SecurityConfiguration security();

	/**
	 * Accesses JSP related configuration used for Tomcat/Jasper engine configuration
	 * @return
	 */
	JspConfiguration jsp();

	/**
	 * Accesses session related configuration
	 * @return
	 */
	SessionConfiguration session();

	/**
	 * Accesses configuration related to server logging and access logging.
	 * @return
	 */
	LogConfiguration logging();

	/**
	 * Accesses Jetty-specific configuration
	 * @return
	 */
	JettyConfiguration jetty();

	/**
	 * Accesses single typed property by name.
	 * @param property
	 * @return
	 */
	<T> T get(String property, Class<T> clazz);

	/**
	 * Low level access to all String properties with String values. Useful when passing to server-specific
	 * configuration mechanisms.
	 * @return
	 */
	Map<String, String> all();

}
