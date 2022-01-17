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
package org.ops4j.pax.web.service.whiteboard;

import java.util.Map;
import javax.servlet.ServletContext;

import org.osgi.service.http.HttpContext;

/**
 * Interface common for <em>mappings</em> related to <em>context</em>:<ul>
 *     <li>{@link org.osgi.service.http.HttpContext} for Http Service scenario</li>
 *     <li>{@link org.osgi.service.http.context.ServletContextHelper} for Whiteboard Service scenario</li>
 * </ul>
 */
public interface ContextMapping {

	String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * <p>Get an ID of the context that may be referenced later:<ul>
	 *     <li>Using {@code httpContext.id} service registration property (legacy Pax Web Whiteboard approach)</li>
	 *     <li>Using {@code osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=name)} service
	 *     registration property (OSGi CMPN Whiteboard approach)</li>
	 * </ul></p>
	 *
	 * @return context identifier; cannot be null
	 */
	String getContextId();

	/**
	 * <p>Get a <em>context path</em> (directly matching {@link ServletContext#getContextPath()} from Servlet API.
	 * This allows to augment {@link HttpContext} with context path, because original specification doesn't do it
	 * at all.</p>
	 *
	 * <p>When registering {@link HttpContext} directly, <em>context path</em> may be specified as
	 * {@code httpContext.id} service registration property.</p>
	 *
	 * <p>For complete picture, OSGi CMPN Whiteboard's {@link org.osgi.service.http.context.ServletContextHelper}
	 * may have <em>context path</em> specified using {@code osgi.http.whiteboard.context.name} service registration
	 * property or {@link org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContext} annotation.</p>
	 *
	 * @return context path as in servlet context path; can be null
	 */
	String getContextPath();

	/**
	 * <p>Get context initialization parameters as defined by {@link ServletContext#getInitParameterNames()}
	 * ({@code <context-param>} elements in {@code web.xml}).</p>
	 *
	 * <p>Please notice the "init" vs. "context" parameters confusion. Here - it's the same.</p>
	 *
	 * @return context parameters; can be null
	 */
	Map<String, String> getInitParameters();

	/**
	 * <p>Get virtual host names to which this context is mapped. Virtual hosts may be targeted by deployments
	 * in several ways:<ul>
	 *     <li>Legacy Pax Web way: by passing {@code httpContext.virtualhosts} <em>init parameter</em> (yes - init
	 *     parameter, not service registration property) with mapped context ({@link HttpContextMapping}).</li>
	 *     <li>Legacy (?) Pax Web way: by setting {@code Web-VirtualHosts} manifest header in WAR bundle</li>
	 *     <li>New (?) Pax Web way: by implementing this method when registering a context using
	 *     ({@link HttpContextMapping} or {@link ServletContextHelperMapping}).</li>
	 * </ul></p>
	 *
	 * <p>By default, a <em>context</em> is mapped to <strong>all</strong> virtual hosts, which means that any
	 * web element registered with such <em>context</em> will be available in all virtual hosts.</p>
	 *
	 * <p>Elements like servlets, filters are <strong>never</strong> directly associated with <em>virtual hosts</em>.
	 * It can only be done via association with some <em>context</em>.</p>
	 *
	 * @return list of virtual hosts to which this context should be mapped. If {@code null} or 0-sized array is
	 *         returned, context will be mapped to all virtual hosts.
	 */
	default String[] getVirtualHosts() {
		return EMPTY_STRING_ARRAY;
	}

	/**
	 * <p>Get connector names through which this context should be accessible. This is mostly modelled for Jetty,
	 * but Tomcat and Undertow also have some tricky configuration.</p>
	 * @return
	 */
	default String[] getConnectors() {
		return EMPTY_STRING_ARRAY;
	}

}
