/*
 * Copyright 2015 Achim Nierbeck
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

/**
 * <p>SPI model hierarchy contains classes to internally represent all the information
 * required during registration (adding to running server) of web components (servlets, filters,
 * welcome files, error pages, security constraints, ...). Such hierarchy is completely unaware of request
 * mapping and it should be treated purely as static database of registrations.</p>
 *
 * <p>This package containing classes that try to hold server independent model of components, though
 * it's modelled after actual capabilities of Jetty, Tomcat and Undertow.</p>
 *
 * <p>In Tomcat, we have these <em>concepts</em>:<ul>
 *     <li><em>Server</em> ({@code org.apache.catalina.core.StandardServer}) is entire Catalina instance to
 *     run one or more <em>services</em>.</li>
 *     <li><em>Service</em> ({@code org.apache.catalina.core.StandardService}) groups one or more <em>connectors</em>
 *     that share a single <em>container</em> (<em>engine</em>).</li>
 *     <li><em>Connector</em> ({@code org.apache.catalina.connector.Connector}) works inside a <em>service</em>
 *     and receives a request translating it into logical invocation</li>
 *     <li><em>Engine</em> ({@code org.apache.catalina.core.StandardEngine}) resides as single <em>container</em>
 *     inside a <em>service</em>. It's main purpose is to hold <em>virtual hosts</em> (one of them is default).</li>
 *     <li><em>Virtual host</em> ({@code org.apache.catalina.core.StandardHost}) processes requests according to
 *     {@code Host} HTTP header or non-matched requests if acting as <em>default virtual host</em> for an
 *     <em>engine</em>. For Tomcat purpose, a <em>virtual host</em> has dedicated <em>app base</em>.</li>
 *     <li><em>Context</em> ({@code org.apache.catalina.core.StandardContext}) represents
 *     {@link jakarta.servlet.ServletContext} or in other words, entire <em>web application</em>. The most important
 *     thing is that it uses single, unique <em>context path</em>.</li>
 * </ul></p>
 *
 * <p>Tomcat uses quite straightforward processing: connector - virtual host - context - servlet. In standard
 * {@code conf/server.xml} it's not easy to use single <em>context</em> in several <em>virtual hosts</em>, but it's
 * possible to use such configuration in embedded mode.</p>
 *
 * <p>In Jetty, we have these <em>concepts</em>:<ul>
 *     <li><em>Server</em> contains <em>connectors</em> which translates a network request into <em>handler</em>
 *     invocation.</li>
 *     <li><em>Connector</em> handles specific protocol and works inside <em>server</em>.</li>
 *     <li><em>Handler</em> is generic interface to handle HTTP request after it's been parsed/processed by
 *     <em>connector</em>. Even a <em>server</em> is a <em>handler</em> and everything depends on which delegate was
 *     set inside the <em>server</em> itself.</li>
 *     <li><em>Handler collection</em> is a special handler recommended to set as <em>delegate</em> inside
 *     a <em>server</em>. Each handler from the collection is called until one of them marks request as "handled".</li>
 *     <li><em>Context</em> ({@code org.eclipse.jetty.server.handler.ContextHandler} and subclasses) represents</li>
 *     {@link jakarta.servlet.ServletContext} (or in other words, entire <em>web application</em>) and is unique wrt
 *     <em>context path</em>. Standalone Jetty (in {@code etc/jetty.xml} and Pax Web itself) choose to use
 *     <em>handler collection</em> containing context handlers.</li>
 *     <li><em>Virtual host</em> is not a special handler - it's part of handling request inside <em>context
 *     handler</em>. Such handler rejests requests (doesn't mark it as "handled") if <em>virtual host</em> test
 *     fails.</li>
 * </ul></p>
 *
 * <p>Jetty allows special syntax where <em>virtual host</em> name (or pattern) is followed by {@code @connectorName}
 * allowing to filter request based on <em>connector</em> through which the request had arrived.</p>
 *
 * <p>In Undertow, we have these <em>concepts</em>:<ul>
 *     <li><em>Server</em> ({@code io.undertow.Undertow}) contains <em>listeners</em> and single, default
 *     <em>handler</em> where user may choose many <em>handlers</em> from rich hierarchy.</li>
 *     <li><em>Listeners</em> receives a request and calls single <em>handler</em> inside a <em>server</em>.</li>
 *     <li><em>Virtual host</em> is just one of the handlers
 *     ({@code io.undertow.server.handlers.NameVirtualHostHandler}) and doesn't have to be set in a <em>server</em>.</li>
 *     <li><em>Context</em>, which matches {@link jakarta.servlet.ServletContext} has <em>context path</em> and
 *     represents entire <em>web application</em> (or WAR deployment), allowing to register servlets, filters, etc.</li>
 * </ul></p>
 *
 * <p>Undertow doesn't do special context mapping and uses {@code io.undertow.server.handlers.PathHandler} to do it.
 * found <em>prefix</em> is set using {@code io.undertow.server.HttpServerExchange#setResolvedPath()}.</p>
 *
 * <p>Now, the above, server-specific concepts have to be mapped into Pax Web and OSGi specifications (both
 * "102 Http Service" and "140 Whiteboard Service". The mapping starts ... in the middle with a concept of a
 * <em>context</em>.</p>
 *
 * <p>There are two <em>contexts</em> depending on OSGi CMPN specification considered:<ul>
 *     <li>{@link org.ops4j.pax.web.service.http.HttpContext} ("102 Http Service")</li>
 *     <li>{@link org.osgi.service.servlet.context.ServletContextHelper} ("140 Whiteboard Service")</li>
 * </ul></p>
 *
 * <p>However, these <em>contexts</em> are not mapped 1:1 with single {@link jakarta.servlet.ServletContext}
 * representing a <em>web application</em> as seen by actual server implementation. The above are <em>used</em>
 * by some other <em>servlet context</em> which is mapped 1:1 with original one (according to
 * <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.http.whiteboard.html#d0e120290">OSGi CMPN R7</a>).
 * But there's still additional {@link jakarta.servlet.ServletContext} actually visible by servlets/filters/etc.
 * depending on the {@link org.ops4j.pax.web.service.http.HttpContext} or
 * {@link org.osgi.service.servlet.context.ServletContextHelper} with which the servlet/filter/etc. was registered
 * to preserve bundle-affinity.</p>
 *
 * <p>First, original, server-specific {@link jakarta.servlet.ServletContext}, identified uniquely by <em>context
 * path</em> is created everytime a {@link org.ops4j.pax.web.service.http.HttpContext} or
 * {@link org.osgi.service.servlet.context.ServletContextHelper} is tracked from service registry, with specified
 * <em>context path</em>. Such context is represented by {@link org.ops4j.pax.web.service.spi.model.ServletContextModel}
 * and is agnostic wrt any bundle. When more HttpContext/ServletContextHelper services are registered for a given
 * <em>context path</em>, there's no conflict - simply more OSGi-specific <em>contexts</em> point to server-specific
 * <em>context</em>.</p>
 *
 * <p>Then, registered servlet/filter/etc. is associated with some OSGi-specific <em>context</em> using <em>context
 * name</em> ({@code osgi.http.whiteboard.context.name} or {@code httpContext.id}) and when there are more
 * OSGi-specific <em>contexts</em> (even if pointing to different server-specific <em>contexts</em>), service
 * ranking is used to resolve the conflict. Such OSGi-specific <em>context</em> is represented by
 * {@link org.ops4j.pax.web.service.spi.model.OsgiContextModel}.</p>
 *
 * <p>Additionally, what servlets, filters see is yet another {@link ServletContext} with proper delegation
 * to relevant class loader. Such <em>context</em> has no representation in the model.</p>
 *
 * <p>When servlet (or filter, or ...) is registered, it is registered with one (or more!) <em>contexts</em> and
 * when actual server implementation is asked to register such element the registration has to be done for all
 * actual {@link jakarta.servlet.ServletContext} instances.</p>
 *
 * <p>There's a requirement, that {@link org.osgi.service.servlet.context.ServletContextHelper} instances have to
 * be available in OSGi registry (including the default one registered by Whiteboard implementation itself). It is
 * not a case with Http Service and {@link org.ops4j.pax.web.service.http.HttpContext}.</p>
 */
package org.ops4j.pax.web.service.spi.model;
