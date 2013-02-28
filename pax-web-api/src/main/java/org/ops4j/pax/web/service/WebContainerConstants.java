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

    /**
     * Filter init param name for specifying a filter-mapping dispatch behaviour
     * Must be a comma delimited string of:
     * <ol>
     * <li>request</li>
     * <li>forward</li>
     * <li>include</li>
     * <li>error</li>
     * </ol>
     *
     * values are not case sensitive.
     */
    static final String FILTER_MAPPING_DISPATCHER = "filter-mapping-dispatcher".intern();

    static final String PROPERTY_HTTP_USE_NIO = "org.osgi.service.http.useNIO";
    static final String PROPERTY_HTTP_PORT = "org.osgi.service.http.port";
    static final String PROPERTY_HTTP_CONNECTOR_NAME = "org.osgi.service.http.connector.name";
    static final String PROPERTY_HTTP_SECURE_PORT = "org.osgi.service.http.port.secure";
    static final String PROPERTY_HTTP_ENABLED = "org.osgi.service.http.enabled";
    static final String PROPERTY_HTTP_SECURE_ENABLED = "org.osgi.service.http.secure.enabled";
    static final String PROPERTY_HTTP_SECURE_CONNECTOR_NAME = "org.osgi.service.http.secure.connector.name";

    static final String PROPERTY_SSL_KEYSTORE = PID + ".ssl.keystore";
    static final String PROPERTY_SSL_KEYSTORE_TYPE = PID + ".ssl.keystore.type";
    static final String PROPERTY_SSL_PASSWORD = PID + ".ssl.password";
    static final String PROPERTY_SSL_KEYPASSWORD = PID + ".ssl.keypassword";

    static final String PROPERTY_SSL_CLIENT_AUTH_WANTED = PID + ".ssl.clientauthwanted";
    static final String PROPERTY_SSL_CLIENT_AUTH_NEEDED = PID + ".ssl.clientauthneeded";

    static final String PROPERTY_SESSION_TIMEOUT = PID + ".session.timeout";
    static final String PROPERTY_SESSION_COOKIE = PID + ".session.cookie";
    static final String PROPERTY_SESSION_URL = PID + ".session.url";
    static final String PROPERTY_WORKER_NAME = PID + ".worker.name";
    static final String PROPERTY_SESSION_COOKIE_HTTP_ONLY = PID + ".session.cookie.httpOnly";
    static final String PROPERTY_SESSION_LAZY_LOAD = PID + ".session.lazyload";
    static final String PROPERTY_SESSION_STORE_DIRECTORY = PID + ".session.storedirectory";

    static final String PROPERTY_TEMP_DIR = "javax.servlet.context.tempdir";

    static final String PROPERTY_LISTENING_ADDRESSES = PID + ".listening.addresses";

    static final String PROPERTY_LOG_NCSA_ENABLED = "org.ops4j.pax.web.log.ncsa.enabled";
    static final String PROPERTY_LOG_NCSA_FORMAT = "org.ops4j.pax.web.log.ncsa.format";
    static final String PROPERTY_LOG_NCSA_RETAINDAYS = "org.ops4j.pax.web.log.ncsa.retaindays";
    static final String PROPERTY_LOG_NCSA_APPEND = "org.ops4j.pax.web.log.ncsa.append";
    static final String PROPERTY_LOG_NCSA_EXTENDED = "org.ops4j.pax.web.log.ncsa.extended";
    static final String PROPERTY_LOG_NCSA_DISPATCH = "org.ops4j.pax.web.log.ncsa.dispatch";
    static final String PROPERTY_LOG_NCSA_LOGTIMEZONE = "org.ops4j.pax.web.log.ncsa.logtimezone";
    static final String PROPERTY_LOG_NCSA_LOGDIR = "org.ops4j.pax.web.log.ncsa.directory";
    static final String PROPERTY_VIRTUAL_HOST_LIST = "org.ops4j.pax.web.default.virtualhosts";
    static final String PROPERTY_CONNECTOR_LIST = "org.ops4j.pax.web.default.connectors";

    /**
     * Servlet context attribute containing the bundle context of the bundle registereing the http context.
     */
    static final String BUNDLE_CONTEXT_ATTRIBUTE = "osgi-bundlecontext";
    
    static final String PROPERTY_SERVER_CONFIGURATION_FILE = PID + ".config.file";

    static final String PROPERTY_SERVER_CONFIGURATION_URL = PID + ".config.url";
    
    /** Manifest header key for web application bundles. */
    static final String CONTEXT_PATH_KEY = "Web-ContextPath";
    
}
