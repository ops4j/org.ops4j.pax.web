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
package org.ops4j.pax.web.service;

/**
 * Web Container related constants.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public interface WebContainerConstants
{

    /**
     * Service PID used for configuration.
     */
    static final String PID = "org.ops4j.pax.web";

    /**
     * Init param name for specifying a context name.
     */
    static final String CONTEXT_NAME = "webapp.context";

    /**
     * Servlet init param name for specifying a servlet name.
     */
    static final String SERVLET_NAME = "servlet-name";

    /**
     * Filter init param name for specifying a filter name.
     */
    static final String FILTER_NAME = "filter-name";

    static final String PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    static final String PROPERTY_HTTP_SECURE_PORT = "org.osgi.service.http.port.secure";
    static final String PROPERTY_HTTP_ENABLED = "org.osgi.service.http.enabled";
    static final String PROPERTY_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";

    static final String PROPERTY_SSL_KEYSTORE = PID + ".ssl.keystore";
    static final String PROPERTY_SSL_KEYSTORE_TYPE = PID + ".ssl.keystore.type";
    static final String PROPERTY_SSL_PASSWORD = PID + ".ssl.password";
    static final String PROPERTY_SSL_KEYPASSWORD = PID + ".ssl.keypassword";

    static final String PROPERTY_SSL_CLIENT_AUTH_WANTED = PID + ".ssl.clientauthwanted";
    static final String PROPERTY_SSL_CLIENT_AUTH_NEEDED = PID + ".ssl.clientauthneeded";
    
    static final String PROPERTY_SESSION_TIMEOUT = PID + ".session.timeout";

    static final String PROPERTY_TEMP_DIR = "javax.servlet.context.tempdir";

    static final String PROPERTY_LISTENING_ADDRESSES = PID + ".listening.addresses";
}
