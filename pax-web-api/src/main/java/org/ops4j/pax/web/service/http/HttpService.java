/*
 * Copyright 2023 OPS4J.
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

package org.ops4j.pax.web.service.http;

import java.util.Dictionary;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

/**
 * The Http Service allows other bundles in the OSGi environment to dynamically
 * register resources and servlets into the URI namespace of Http Service. A
 * bundle may later unregister its resources or servlets.
 *
 * @author $Id: 4ed5578fbcfcedc725a6136b6ac4059f961bbf4c $
 * @noimplement
 * @see HttpContext
 */
public interface HttpService {

    /**
     * Registers a servlet into the URI namespace.
     *
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped.
     *
     * <p>
     * An alias must begin with slash ('/') and must not end with slash ('/'),
     * with the exception that an alias of the form &quot;/&quot; is used to
     * denote the root alias. See the specification text for details on how HTTP
     * requests are mapped to servlet and resource registrations.
     *
     * <p>
     * The Http Service will call the servlet's {@code init} method before
     * returning.
     *
     * <pre>
     * httpService.registerServlet(&quot;/myservlet&quot;, servlet, initparams, context);
     * </pre>
     *
     * <p>
     * Servlets registered with the same {@code HttpContext} object will share
     * the same {@code ServletContext}. The Http Service will call the
     * {@code context} argument to support the {@code ServletContext} methods
     * {@code getResource},{@code getResourceAsStream} and {@code getMimeType},
     * and to handle security for requests. If the {@code context} argument is
     * {@code null}, a default {@code HttpContext} object is used (see
     * {@link #createDefaultHttpContext()}).
     *
     * @param alias      name in the URI namespace at which the servlet is registered
     * @param servlet    the servlet object to register
     * @param initparams initialization arguments for the servlet or
     *                   {@code null} if there are none. This argument is used by the
     *                   servlet's {@code ServletConfig} object.
     * @param context    the {@code HttpContext} object for the registered servlet,
     *                   or {@code null} if a default {@code HttpContext} is to be created
     *                   and used.
     * @throws NamespaceException                 if the registration fails because the alias is
     *                                            already in use.
     * @throws jakarta.servlet.ServletException   if the servlet's {@code init}
     *                                            method throws an exception, or the given servlet object has
     *                                            already been registered at a different alias.
     * @throws java.lang.IllegalArgumentException if any of the arguments are
     *                                            invalid
     */
    void registerServlet(String alias, Servlet servlet, Dictionary<?, ?> initparams, HttpContext context) throws ServletException, NamespaceException;

    /**
     * Registers resources into the URI namespace.
     *
     * <p>
     * The alias is the name in the URI namespace of the Http Service at which
     * the registration will be mapped. An alias must begin with slash ('/') and
     * must not end with slash ('/'), with the exception that an alias of the
     * form &quot;/&quot; is used to denote the root alias. The name parameter
     * must also not end with slash ('/') with the exception that a name of the
     * form &quot;/&quot; is used to denote the root of the bundle. See the
     * specification text for details on how HTTP requests are mapped to servlet
     * and resource registrations.
     * <p>
     * For example, suppose the resource name /tmp is registered to the alias
     * /files. A request for /files/foo.txt will map to the resource name
     * /tmp/foo.txt.
     *
     * <pre>
     * httpservice.registerResources(&quot;/files&quot;, &quot;/tmp&quot;, context);
     * </pre>
     * <p>
     * The Http Service will call the {@code HttpContext} argument to map
     * resource names to URLs and MIME types and to handle security for
     * requests. If the {@code HttpContext} argument is {@code null}, a default
     * {@code HttpContext} is used (see {@link #createDefaultHttpContext()}).
     *
     * @param alias   name in the URI namespace at which the resources are
     *                registered
     * @param name    the base name of the resources that will be registered
     * @param context the {@code HttpContext} object for the registered
     *                resources, or {@code null} if a default {@code HttpContext} is to
     *                be created and used.
     * @throws NamespaceException                 if the registration fails because the alias is
     *                                            already in use.
     * @throws java.lang.IllegalArgumentException if any of the parameters are
     *                                            invalid
     */
    void registerResources(String alias, String name, HttpContext context) throws NamespaceException;

    /**
     * Unregisters a previous registration done by {@code registerServlet} or
     * {@code registerResources} methods.
     *
     * <p>
     * After this call, the registered alias in the URI name-space will no
     * longer be available. If the registration was for a servlet, the Http
     * Service must call the {@code destroy} method of the servlet before
     * returning.
     * <p>
     * If the bundle which performed the registration is stopped or otherwise
     * "unget"s the Http Service without calling {@link #unregister(String)}
     * then Http Service must automatically unregister the registration.
     * However, if the registration was for a servlet, the {@code destroy}
     * method of the servlet will not be called in this case since the bundle
     * may be stopped. {@link #unregister(String)} must be explicitly called to
     * cause the {@code destroy} method of the servlet to be called. This can be
     * done in the {@code BundleActivator.stop} method of the bundle registering
     * the servlet.
     *
     * @param alias name in the URI name-space of the registration to unregister
     * @throws java.lang.IllegalArgumentException if there is no registration
     *                                            for the alias or the calling bundle was not the bundle which
     *                                            registered the alias.
     */
    void unregister(String alias);

    /**
     * Creates a default {@code HttpContext} for registering servlets or
     * resources with the HttpService, a new {@code HttpContext} object is
     * created each time this method is called.
     *
     * <p>
     * The behavior of the methods on the default {@code HttpContext} is defined
     * as follows:
     * <ul>
     * <li>{@code getMimeType} - Does not define any customized MIME types for
     * the Content-Type header in the response, and always returns {@code null}.
     * </li>
     * <li>{@code handleSecurity} - Performs implementation-defined
     * authentication on the request.</li>
     * <li>{@code getResource} - Assumes the named resource is in the context
     * bundle; this method calls the context bundle's {@code Bundle.getResource}
     * method, and returns the appropriate URL to access the resource. On a Java
     * runtime environment that supports permissions, the Http Service needs to
     * be granted {@code org.osgi.framework.AdminPermission[*,RESOURCE]}.</li>
     * </ul>
     *
     * @return a default {@code HttpContext} object.
     * @since 1.1
     */
    HttpContext createDefaultHttpContext();

}
