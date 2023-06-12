/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.whiteboard;

import java.util.Map;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;

/**
 * <p><em>Servlet mapping</em> contains all the information required to register a {@link Servlet} (either directly or
 * using <em>whiteboard</em> pattern).</p>
 *
 * <p>This interface may be used directly, when user registers OSGi services using it as
 * {@link org.osgi.framework.Constants#OBJECTCLASS} (<em>explicit whiteboard approach</em>) or will be used internally,
 * when user registers actual {@link Servlet} instance with service properties (or class annotations)
 * (as recommended by Whiteboard specification).</p>
 *
 * <p>Registering a {@link Servlet} can be done in two ways:<ul>
 *     <li>registering a service with this interface - all the information is included in service itself
 *     (<em>explicit Whiteboard approach</em>)</li>
 *     <li>registering a {@link Servlet} service, while required properties (mapping, name, parameters) are specified
 *     using service registration properties or annotations (OSGi CMPN Whiteboard approach)</li>
 * </ul></p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public interface ServletMapping extends ContextRelated {

	/**
	 * <p>Get an actual {@link Servlet} instance to register.</p>
	 * <p>In <em>whiteboard</em> method, this is actual OSGi service instance.</p>
	 *
	 * @return the servlet to register
	 */
	Servlet getServlet();

	/**
	 * <p>Get a class of {@link Servlet} to register. Matches {@code <servlet>/<servlet-class>} element from
	 * {@code web.xml}. If {@link #getServlet()} is also specified, servlet class isn't used.</p>
	 * <p>There's no <em>whiteboard</em> specific method to specify this class.</p>
	 *
	 * @return the servlet's class to instantiate and register
	 */
	Class<? extends Servlet> getServletClass();

	/**
	 * <p>Get a name of the servlet being registered. Matches {@code <servlet>/<servlet-name>} element from
	 * {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_NAME}
	 *     property</li>
	 *     <li>{@code servlet-name} service registration property (legacy Pax Web Whiteboard approach)</li>
	 * </ul></p>
	 * <p>If not specified, the name defaults to fully qualified class name of the servlet.</p>
	 *
	 * @return name of the Servlet being mapped.
	 */
	String getServletName();

	/**
	 * <p>Get URL patterns to map into the servlet being registered. URL patterns should be specified according
	 * to Servlet API 4 specification (chapter 12.2) and OSGi CMPN R6+ Whiteboard specification (chapter 140.4).
	 * It matches {@code <servlet-mapping>/<url-pattern>} elements from {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_PATTERN}
	 *     property</li>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletPattern} annotation</li>
	 *     <li>{@code urlPatterns} service registration property (legacy Pax Web Whiteboard approach)</li>
	 * </ul>When passing service registration property, it should be one of {@code String}, {@code String[]} or
	 * {@code Collection<String>} types.</p>
	 *
	 * @return an array of url patterns servlet maps to
	 */
	String[] getUrlPatterns();

	/**
	 * <p>Get an <em>alias</em> to use for servlet registration. An <em>alias</em> is defined in OSGi CMPN specification
	 * of {@link org.ops4j.pax.web.service.http.HttpService HTTP Service} and is often (even in specification) confused with
	 * <em>servlet name</em>: <em>name in the URI namespace</em>. For the purpose of Pax Web and consistency, single
	 * <em>alias</em> is treated as one-element array of URL Patterns if the patterns are not specified.</p>
	 * <p>There's no <em>whiteboard</em> specific method to specify an <em>alias</em>.</p>
	 *
	 * @return resource alias - effectively changed into 1-element array of urlPatterns
	 */
	String getAlias();

	/**
	 * <p>Get error page declarations to use for the servlet being registered. The declarations mark the servlet
	 * as <em>error servlet</em> matching {@code <error-page>/<error-code>} and {@code <error-page>/<exception-type>}
	 * elementss from {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_ERROR_PAGE}
	 *     property</li>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletErrorPage} annotation</li>
	 * </ul></p>
	 *
	 * @return Servlet async-supported flag
	 */
	String[] getErrorPages();

	/**
	 * <p>Get flag for supporting asynchronous servlet invocation. It matches {@code <servlet>/<async-supported>}
	 * element from {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED}
	 *     property</li>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported}
	 *     annotation</li>
	 * </ul></p>
	 *
	 * @return Servlet async-supported flag
	 */
	Boolean getAsyncSupported();

	/**
	 * <p>Get {@link MultipartConfigElement} to configuration multipart support for the servlet being registered.
	 * See Servlet API 4 specification (chapter 3.2 File upload) for details. It matches
	 * {@code <servlet>/<multipart-config>} element from {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as:<ul>
	 *     <li>{@code org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_MULTIPART_*}
	 *     properties</li>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.propertytypes.HttpWhiteboardServletMultipart} annotation</li>
	 * </ul></p>
	 *
	 * @return Servlet multipart configuration
	 */
	MultipartConfigElement getMultipartConfig();

	/**
	 * <p>Get init parameters for the servlet being registered. It matches {@code <servlet>/<init-param>}
	 * elements from {@code web.xml}.</p>
	 * <p>In <em>whiteboard</em> method, this can be specified as (no annotation here):<ul>
	 *     <li>{@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX}
	 *     prefixed properties (OSGi CMPN Whiteboard approach)</li>
	 *     <li>{@code init.} prefixed properties (or prefix may be specified using {@code init-prefix}
	 *     service registration property (legacy Pax Web Whiteboard approach)</li>
	 * </ul></p>
	 *
	 * @return map of initialization parameters.
	 */
	Map<String, String> getInitParameters();

	/**
	 * <p>Get <em>load on startup</em> value for the servlet being registered. It matches
	 * {@code <servlet>/<load-on-startup>} element from {@code web.xml}.</p>
	 * <p>Whiteboard specification doesn't mention this parameter.</p>
	 *
	 * @return Servlet load-on-startup configuration
	 */
	Integer getLoadOnStartup();

}
