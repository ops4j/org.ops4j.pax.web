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

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;

/**
 * <p>Super interface extended by all <em>explicit whiteboard elements</em> that can be registered by targeting
 * selected <em>context</em>.</p>
 *
 * <p><em>Context</em> is something different in {@link org.ops4j.pax.web.service.http.HttpService} case (it's represented
 * by {@link org.ops4j.pax.web.service.http.HttpContext}) and in Whiteboard case (it's represented by
 * {@link org.osgi.service.servlet.context.ServletContextHelper}). In both cases, eventually this <em>context</em>
 * is actually backed by a real {@link jakarta.servlet.ServletContext} from Java Servlet API. Though it's not 1:1
 * relation...</p>
 *
 * <p>Both {@link org.ops4j.pax.web.service.http.HttpContext} and {@link org.osgi.service.servlet.context.ServletContextHelper}
 * do not specify a <em>context path</em>. It can be specified <strong>only</strong> by:<ul>
 *     <li>(Pax Web legacy Whiteboard) {@code httpContext.path} service registration property when
 *     whiteboard-registering {@link org.ops4j.pax.web.service.http.HttpContext} service</li>
 *     <li>(Pax Web legacy Whiteboard) whiteboard-registering a {@link HttpContextMapping} service with a path</li>
 *     <li>(OSGI CMPN Whiteboard)
 *     {@link org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_PATH} service
 *     registration property when registering {@link org.osgi.service.servlet.context.ServletContextHelper} service
 *     </li>
 * </ul></p>
 *
 * <p>The referenced <em>context</em> doesn't have to be unique wrt {@link jakarta.servlet.ServletContext}, there may be
 * many <em>contexts</em> registered (as mentioned above) for given <em>context path</em> but using different name.
 * Servlet (or e.g., filter) may be associated with one (or more)
 * {@link org.osgi.service.servlet.context.ServletContextHelper} (new Whiteboard) instance for given <em>context path</em>,
 * but actual (server specific) <em>context</em> (or <em>web application</em>) may be supported by different
 * {@link org.osgi.service.servlet.context.ServletContextHelper} instances.</p>
 *
 * <p>Pax Web will unify behavior of Http Service and Whiteboard style <em>contexts</em> (knowing that underneath
 * there's actual, server-specific {@link jakarta.servlet.ServletContext}) and uniqueness will be checked by String ID
 * (and bundle for bundle-scoped access). Additionally for old-style {@link org.ops4j.pax.web.service.http.HttpContext},
 * a <em>shared</em> flag will be checked to determine whether context may be used by different bundles.
 * Whiteboard (new-style) <em>context</em> is <em>shared</em> by default.</p>
 *
 * <p>In {@link org.ops4j.pax.web.service.http.HttpService} case (no whiteboard), equality of
 * {@link org.ops4j.pax.web.service.http.HttpContext} created by users is implied to be instance equality (same object).
 * Pax Web wraps such contexts and sets {@code custom} context ID in the wrapper.</p>
 *
 * <p>In non-whiteboard approach, servlets are always registered together with associated
 * {@link org.ops4j.pax.web.service.http.HttpContext} when calling method like
 * {@link org.ops4j.pax.web.service.http.HttpService#registerServlet}. User may also provide
 * {@link org.ops4j.pax.web.service.WebContainerContext} or {@link MultiBundleWebContainerContext} when registering
 * a servlet. That means (assuming Pax Web specific <em>shared</em> contexts) it's hard to reference common context
 * without using actual instance of the context, so such instance has to be shared through OSGi registry.</p>
 *
 * <p>To support real sharing of {@link org.ops4j.pax.web.service.http.HttpContext} when using <em>old</em>
 * {@link org.ops4j.pax.web.service.http.HttpService} methods, user first needs to register {@link HttpContextMapping}
 * OSGi service or {@link org.ops4j.pax.web.service.http.HttpContext} service with {@code httpContext.id} registration
 * property:<pre>
 *     // register pure HttpContext service
 *     props.put("httpContext.id", "my-context");
 *     bundleContext.registerService(HttpContext.class, aContext, props);
 *
 *     // or register HttpContextMapping using "explicit whiteboard approach"
 *     aContextMapping = new HttpContextMappingImpl();
 *     aContextMapping.setContextId("my-context");
 *     aContextMapping.setHttpContext(new HttpContext() { ... });
 *     bundleContext.registerService(HttpContextMapping.class, aContextMapping, null);
 * </pre></p>
 *
 * <p>Then, an element (like servlet) may be registered like this:<pre>
 *     context = new DefaultHttpContext(bundleContext.getBundle(), "my-context");
 *     httpService.registerServlet("/alias", servlet, null, context);
 * </pre></p>
 *
 * <p>This trick is based on assumed identity of DefaultHttpContext, which is name+bundle and separates
 * the identity from <em>context behavior</em> ({@link org.ops4j.pax.web.service.http.HttpContext#handleSecurity}), which
 * should not be specified (or emphasized) in every registration.</p>
 */
public interface ContextRelated {

	/**
	 * <p>Get an LDAP-style filter to select {@link org.osgi.service.servlet.context.ServletContextHelper} instances
	 * to use when registering given element. This is generic, defined in Whiteboard
	 * specification, way of associating the servlet (and filter, and listener, ...) with a <em>context</em>.</p>
	 *
	 * <p>In generic scenario, when custom <em>context</em> ({@link org.osgi.service.servlet.context.ServletContextHelper}
	 * in case of Whiteboard) is registered using:<pre>
	 *     Dictionary<String, Object> props = new Hashtable<>();
	 *     props.put("osgi.http.whiteboard.context.name", "my-context");
	 *     props.put("osgi.http.whiteboard.context.path", "/my-context");
	 *     context.registerService(ServletContextHelper.class, ..., props);
	 * </pre>
	 * Servlet may be associated with such context using:<pre>
	 *     Dictionary<String, Object> props = new Hashtable<>();
	 *     props.put("osgi.http.whiteboard.context.select", "(osgi.http.whiteboard.context.name=my-context)");
	 *     context.registerService(Servlet.class, ..., props);
	 * </pre></p>
	 *
	 * <p>Special Whiteboard-Http Service scenario (OSGi R7 CMPN 140.10 "Integration with Http Service Contexts")
	 * mentions that a <em>whiteboard</em> element (<strong>excluding</strong> servlet, but let's not add such
	 * restriction in Pax Web) may be associated with a <em>context</em> representing an <em>old</em>
	 * {@link org.ops4j.pax.web.service.http.HttpContext} using special context selection:<pre>
	 *     Dictionary<String, Object> props = new Hashtable<>();
	 *     props.put("osgi.http.whiteboard.context.select", "(osgi.http.whiteboard.context.httpservice=*)");
	 *     context.registerService(Servlet.class, ..., props);
	 * </pre></p>
	 *
	 * <p>This specification fragment says:<blockquote>
	 *     A Http Whiteboard service which should be registered with a Http Context from the Http Service can
	 *     achieve this by targeting a {@link org.osgi.service.servlet.context.ServletContextHelper} with the
	 *     registration property {@code osgi.http.whiteboard.context.httpservice}.
	 * </blockquote></p>
	 *
	 * <p>This seems a bit artificial:<ul>
	 *     <li>We can't distinguish actual <em>old-style context</em> ({@link org.ops4j.pax.web.service.http.HttpContext})</li>
	 *     <li>Specification requires the implementation to register special
	 *     {@link org.osgi.service.servlet.context.ServletContextHelper} <em>representing</em>
	 *     a {@link org.ops4j.pax.web.service.http.HttpContext}</li>
	 * </ul></p>
	 *
	 * @return
	 */
	String getContextSelectFilter();

	/**
	 * <p>Get an ID of the <em>context</em> in which the servlet should be registered. This is handy simplification
	 * of context association if no {@link #getContextSelectFilter()} is specified.</p>
	 *
	 * <p>In <em>Whiteboard</em> scenario, the <em>context</em> (actually,
	 * {@link org.osgi.service.servlet.context.ServletContextHelper}) is selected using an LDAP-like filter, see
	 * {@link #getContextSelectFilter()}, which allows association with many servlet contexts. To simplify things,
	 * this method may just indicate single {@link org.osgi.service.servlet.context.ServletContextHelper} by name.
	 * It can be done using one of (in order of decreasing priority):<ul>
	 *     <li>standard (Whiteboard)
	 *     {@code osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=name)} service registration
	 *     property</li>
	 *     <li>legacy (Pax Web specific) {@code httpContext.id=name} service registration property</li>
	 *     <li>this method (if user registers an instance of {@link ContextRelated}.</li>
	 * </ul></p>
	 *
	 * @return id of single <em>context</em> this whiteboard element should be associated with
	 */
	String getContextId();

}
