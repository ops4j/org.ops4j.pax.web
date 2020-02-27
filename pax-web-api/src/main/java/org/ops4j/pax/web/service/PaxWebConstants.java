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

import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.whiteboard.ContextMapping;

/**
 * <p>Different constants used across Pax Web but not related to configuration that may be specified using
 * {@code org.ops4j.pax.web} PID or system/bundle context properties. For configuration related constants, see
 * {@link PaxWebConfig}.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code SERVICE_PROPERTY_} - for names of OSGi service registration properties</li>
 *     <li>{@code INIT_PARAM_} - for legacy init parameters passed to {@link org.osgi.service.http.HttpService}
 *     registration methods that are handled in special way by Pax Web.</li>
 *     <li>{@code DEFAULT_} - for miscellaneous <em>default</em> values (default VHost, default name, default context,
 *     ...</li>
 * </ul></p>
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
public interface PaxWebConstants {

	/** Service PID used for configuration. */
	String PID = "org.ops4j.pax.web";

	/** Actual OSGi Http Service will be registered under these {@code objectClass} names. */
	String[] HTTPSERVICE_REGISTRATION_NAMES = {
			org.osgi.service.http.HttpService.class.getName(),
			org.ops4j.pax.web.service.WebContainer.class.getName()
	};

	/** Default name for <em>context</em> (e.g., {@link org.osgi.service.http.context.ServletContextHelper}) */
	String DEFAULT_CONTEXT_NAME = "default";

	/** Default name for <em>virtual host</em> */
	String DEFAULT_VIRTUAL_HOST_NAME = "default";

	/** Default {@link ServletContext#getContextPath() context path} */
	String DEFAULT_CONTEXT_PATH = "/";

	/**
	 * <p>Pax Web specific service property used when registering:<ul>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}</li>
	 *     <li>{@link org.osgi.service.http.context.ServletContextHelper}</li>
	 * </ul>
	 * services to indicate <em>virtual hosts</em> with which this context should be associated (though for the two
	 * Pax Web specific mappings, {@link ContextMapping#getVirtualHosts()} takes precedence).</p>
	 *
	 * <p>The value should be String, array of Strings or Collection of Strings. When missing, <em>context</em>
	 * is assumed to be associated with <strong>all</strong> virtual hosts.</p>
	 */
	String SERVICE_PROPERTY_VIRTUAL_HOSTS = "httpContext.virtualhosts";

	/**
	 * Legacy service property for context ID.
	 * @deprecated Use {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_NAME}
	 */
	@Deprecated
	String SERVICE_PROPERTY_HTTP_CONTEXT_ID = "httpContext.id";

	/**
	 * <p>Init parameter that can be used to specify servlet name.</p>
	 *
	 * <p>{@link WebContainer} provides registration methods, where servlet name can be specified directly. Also
	 * according to Http Service and Whiteboard Service specifications, servlet name defaults to FQCN of the servlet.
	 * </p>
	 */
	@Deprecated
	String INIT_PARAM_SERVLET_NAME = "servlet-name";













	/**
	 * Init param name for specifying a context name.
	 */
	String CONTEXT_NAME = "webapp.context";

	/**
	 * Filter init param name for specifying a filter name.
	 */
	String FILTER_NAME = "filter-name";

	/**
	 * Filter init param name for specifying a filter-mapping dispatch behaviour
	 * Must be a comma delimited string of:
	 * <ol>
	 * <li>request</li>
	 * <li>forward</li>
	 * <li>include</li>
	 * <li>error</li>
	 * </ol>
	 * <p>
	 * values are not case sensitive.
	 */
	String FILTER_MAPPING_DISPATCHER = "filter-mapping-dispatcher";

	String PROPERTY_HTTP_USE_NIO = "org.osgi.service.http.useNIO";







	String PROPERTY_SESSION_TIMEOUT = PID + ".session.timeout";
	String PROPERTY_SESSION_COOKIE = PID + ".session.cookie";
	String PROPERTY_SESSION_DOMAIN = PID + ".session.domain";
	String PROPERTY_SESSION_PATH = PID + ".session.path";
	String PROPERTY_SESSION_URL = PID + ".session.url";
	String PROPERTY_WORKER_NAME = PID + ".worker.name";
	String PROPERTY_SESSION_COOKIE_HTTP_ONLY = PID + ".session.cookie.httpOnly";
	String PROPERTY_SESSION_COOKIE_SECURE = PID + ".session.cookie.secure";
	String PROPERTY_SESSION_COOKIE_MAX_AGE = PID + ".session.cookie.maxAge";
	String PROPERTY_SESSION_LAZY_LOAD = PID + ".session.lazyload";
	String PROPERTY_SESSION_STORE_DIRECTORY = PID + ".session.storedirectory";


	String PROPERTY_VIRTUAL_HOST_LIST = "org.ops4j.pax.web.default.virtualhosts";
	String PROPERTY_CONNECTOR_LIST = "org.ops4j.pax.web.default.connectors";
    String PROPERTY_DEFAULT_AUTHMETHOD = "org.ops4j.pax.web.default.authmethod";
    String PROPERTY_DEFAULT_REALMNAME = "org.ops4j.pax.web.default.realmname";


	String PROPERTY_SHOW_STACKS = "org.ops4j.pax.web.server.showStacks";

	/**
	 * Servlet context attribute containing the bundle context of the bundle
	 * registering the http context.
	 */
	String BUNDLE_CONTEXT_ATTRIBUTE = "osgi-bundlecontext";

	/**
	 * Manifest header key for web application bundles.
	 */
	String CONTEXT_PATH_KEY = "Web-ContextPath";

	String FILTER_RANKING = "filterRank";
	

	String PROPERTY_ENC_MASTERPASSWORD = PID + ".enc.masterpassword";
	
	String PROPERTY_ENC_ALGORITHM = PID + ".enc.algorithm";
	
	String PROPERTY_ENC_ENABLED = PID + ".enc.enabled";
	
	String PROPERTY_ENC_PREFIX = PID + ".enc.prefix";
	
	String PROPERTY_ENC_SUFFIX = PID + ".enc.suffix";

	String PROPERTY_SERVLETCONTEXT_PATH = "osgi.web.contextpath";
	String PROPERTY_SERVLETCONTEXT_NAME = "osgi.web.contextname";
	String PROPERTY_SYMBOLIC_NAME = "osgi.web.symbolicname";


	/**
	 * Scratch directory for JSPs
	 */
	String PROPERTY_JSP_SCRATCH_DIR = PID + ".jsp.scratch.dir";

	String PROPERTY_JSP_CHECK_INTERVAL = PID + ".jsp.check.interval";

	String PROPERTY_JSP_DEBUG_INFO = PID + ".jsp.debug.info";

	String PROPERTY_JSP_DEVELOPMENT = PID + ".jsp.development";

	String PROPERTY_JSP_ENABLE_POOLING = PID + ".jsp.enable.pooling";

	String PROPERTY_JSP_IE_CLASS_ID = PID + ".jsp.ie.classid";

	String PROPERTY_JSP_JAVA_ENCODING = PID + ".jsp.java.encoding";

	String PROPERTY_JSP_KEEP_GENERATED = PID + ".jsp.keep.generated";

	String PROPERTY_JSP_LOG_VERBOSITY_LEVEL = PID + ".jsp.log.verbosity.level";

	String PROPERTY_JSP_MAPPED_FILE = PID + ".jsp.mapped.file";

	String PROPERTY_JSP_TAGPOOL_MAX_SIZE = PID + ".jsp.tagpool.max.size";

	String PROPERTY_JSP_PRECOMPILATION = PID + ".jsp.precompilation";

}
