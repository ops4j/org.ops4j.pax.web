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
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * <p>Different constants used across Pax Web but not related to configuration that may be specified using
 * {@code org.ops4j.pax.web} PID or system/bundle context properties. For configuration related constants, see
 * {@link PaxWebConfig}.</p>
 * <p>Constants names use the following prefixes:<ul>
 *     <li>{@code SERVICE_PROPERTY_} - for names of OSGi service registration properties</li>
 *     <li>{@code INIT_PARAM_} - for legacy init parameters passed to {@link org.osgi.service.http.HttpService}
 *     registration methods that are handled in special way by Pax Web.</li>
 *     <li>{@code CONTEXT_PARAM_} - for {@link ServletContext} attributes set by Pax Web.</li>
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
	String DEFAULT_CONTEXT_NAME = HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME;

	/** Name for default <em>shared</em> contexts - Pax Web specific */
	String DEFAULT_SHARED_CONTEXT_NAME = "shared";

	/** Default name for <em>virtual host</em> */
	String DEFAULT_VIRTUAL_HOST_NAME = "default";

	/** Default (not overrideable) JSP servlet name */
	String DEFAULT_JSP_SERVLET_NAME = "jsp";

	/** The only supported JSP servlet class name */
	String DEFAULT_JSP_SERVLET_CLASS = "org.ops4j.pax.web.jsp.JspServlet";

	/** The only supported {@link javax.servlet.ServletContainerInitializer} class that configures JSP engine */
	String DEFAULT_JSP_SCI_CLASS = "org.ops4j.pax.web.jsp.JasperInitializer";

	/** Default {@link ServletContext#getContextPath() context path} */
	String DEFAULT_CONTEXT_PATH = "/";

	/** Default session timeout */
	Integer DEFAULT_SESSION_TIMEOUT = 30;

	/** Symbolic Name of pax-web-jsp bundle */
	String DEFAULT_PAX_WEB_JSP_SYMBOLIC_NAME = "org.ops4j.pax.web.pax-web-jsp";

	/**
	 * Service registration property to mark services as <em>internal</em>, so they're not processed by Pax Web
	 * Whiteboard extender as normal Whiteboard services.
	 */
	String SERVICE_PROPERTY_INTERNAL = "org.ops4j.pax.web.internal";

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
	@Deprecated
	String SERVICE_PROPERTY_VIRTUAL_HOSTS = "httpContext.virtualhosts";

	/**
	 * Legacy service property for context ID.
	 * @deprecated Use {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_NAME}
	 */
	@Deprecated
	String SERVICE_PROPERTY_HTTP_CONTEXT_ID = "httpContext.id";

	/**
	 * Legacy property name for a legacy "shared" flag for {@link org.osgi.service.http.HttpContext} services.
	 * @deprecated User {@link org.osgi.service.http.context.ServletContextHelper} services which are "shared"
	 * by default
	 */
	@Deprecated
	String SERVICE_PROPERTY_HTTP_CONTEXT_SHARED = "httpContext.shared";

	/**
	 * Legacy context path.
	 * @deprecated Use {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_PATH}
	 */
	@Deprecated
	String SERVICE_PROPERTY_HTTP_CONTEXT_PATH = "httpContext.path";

	/** The legacy registration property for the alias to be used by a servlet/resource registration. */
	@Deprecated
	String SERVICE_PROPERTY_SERVLET_ALIAS = "alias";

	/** The legacy registration property for the url patterns to be used by a filter registration. */
	@Deprecated
	String SERVICE_PROPERTY_URL_PATTERNS = "urlPatterns";

	/** The legacy registration property for the servlet names to be used by a filter registration. */
	@Deprecated
	String SERVICE_PROPERTY_SERVLET_NAMES = "servletNames";

	/** Legacy registration property for {@code <async-supported>} configuration */
	@Deprecated
	String SERVICE_PROPERTY_ASYNC_SUPPORTED = "async-supported";

	/** Legacy registration property for {@code <load-on-startup>} configuration */
	@Deprecated
	String SERVICE_PROPERTY_LOAD_ON_STARTUP = "load-on-startup";

	/**
	 * The registration property for filtering init parameters. All init parameters starting with something different
	 * then the init-prefix will be ignored and not added to the init params of the servlet/filter/context.
	 */
	@Deprecated
	String SERVICE_PROPERTY_INIT_PREFIX = "init-prefix";

	/**
	 * The default init-prefix.
	 * @deprecated {@code servlet.init./filter.init./context.init.} should be used instead - see
	 * {@link HttpWhiteboardConstants}.
	 */
	@Deprecated
	String DEFAULT_INIT_PREFIX_PROP = "init.";

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
	 * <p>Init parameter that can be used to specify filter name.</p>
	 *
	 * <p>{@link WebContainer} provides registration methods, where filter name can be specified directly. Also
	 * according to Http Service and Whiteboard Service specifications, filter name defaults to FQCN of the servlet.
	 * </p>
	 */
	@Deprecated
	String INIT_PARAM_FILTER_NAME = "filter-name";

	/**
	 * Filter init param name for specifying a filter-mapping dispatch behaviour Must be a comma delimited string of:
	 * <ul>
	 *     <li>{@link javax.servlet.DispatcherType#REQUEST}</li>
	 *     <li>{@link javax.servlet.DispatcherType#FORWARD}</li>
	 *     <li>{@link javax.servlet.DispatcherType#INCLUDE}</li>
	 *     <li>{@link javax.servlet.DispatcherType#ERROR}</li>
	 *     <li>{@link javax.servlet.DispatcherType#ASYNC}</li>
	 * </ul>
	 * <p>
	 * values are not case sensitive.
	 */
	@Deprecated
	String FILTER_MAPPING_DISPATCHER = "filter-mapping-dispatcher";

	/**
	 * Servlet context attribute containing the bundle context of the bundle registering the http context
	 * according to 128.6.1 "Bundle Context Access" chapter of OSGi CMPN 128 "Web Applications Specification"
	 */
	String CONTEXT_PARAM_BUNDLE_CONTEXT = "osgi-bundlecontext";

	/** For legacy Spring-DM support, {@link org.osgi.framework.BundleContext} should be available under this param */
	String CONTEXT_PARAM_SPRING_BUNDLE_CONTEXT = "org.springframework.osgi.web.org.osgi.framework.BundleContext";

	// --- 3 properties defined in 128.3.4 "Publishing the Servlet Context" (and one Pax Web specific)

	/** Symbolic name of the WAB bundle or bundle registering OSGi servlet context */
	String SERVICE_PROPERTY_WEB_SYMBOLIC_NAME = "osgi.web.symbolicname";

	/** Version of the WAB bundle or bundle registering OSGi servlet context */
	String SERVICE_PROPERTY_WEB_VERSION = "osgi.web.version";

	/** Context path of the WAB bundle or bundle registering OSGi servlet context */
	String SERVICE_PROPERTY_WEB_SERVLETCONTEXT_PATH = "osgi.web.contextpath";

	/** Context name of the WAB bundle or bundle registering OSGi servlet context (Pax Web addition) */
	String SERVICE_PROPERTY_WEB_SERVLETCONTEXT_NAME = "osgi.web.contextname";










	/**
	 * Init param name for specifying a context name.
	 */
	String CONTEXT_NAME = "webapp.context";

	String PROPERTY_HTTP_USE_NIO = "org.osgi.service.http.useNIO";

	String PROPERTY_VIRTUAL_HOST_LIST = "org.ops4j.pax.web.default.virtualhosts";
	String PROPERTY_CONNECTOR_LIST = "org.ops4j.pax.web.default.connectors";
	String PROPERTY_DEFAULT_AUTHMETHOD = "org.ops4j.pax.web.default.authmethod";
	String PROPERTY_DEFAULT_REALMNAME = "org.ops4j.pax.web.default.realmname";

	/**
	 * Manifest header key for web application bundles.
	 */
	String CONTEXT_PATH_KEY = "Web-ContextPath";




	String PROPERTY_ENC_MASTERPASSWORD = PID + ".enc.masterpassword";

	String PROPERTY_ENC_ALGORITHM = PID + ".enc.algorithm";

	String PROPERTY_ENC_ENABLED = PID + ".enc.enabled";

	String PROPERTY_ENC_PREFIX = PID + ".enc.prefix";

	String PROPERTY_ENC_SUFFIX = PID + ".enc.suffix";

}
