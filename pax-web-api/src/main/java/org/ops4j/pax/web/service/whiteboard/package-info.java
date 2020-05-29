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
 * <p>This package of pax-web-api contains interfaces representing all the <em>web components</em> (or
 * <em>web elements</em>) that can be registered within running HTTP/Servlet engine.</p>
 *
 * <p>Pax Web provided <em>Whiteboard</em> approach before OSGi CMPN R6+ Whiteboard specification, so
 * entire configuration of registered web elements (servlets, filters, ...) can be passed directly and not
 * only as service properties (Whiteboard specification allows only the latter).</p>
 *
 * <p>Summarizing, <em>whiteboard</em> implementation in Pax Web allows registration of:<ul>
 *     <li>actual {@code javax.servlet} elements (servlet, filters, ...), where additional configuration is specified
 *     as service registration properties - as described in OSGi CMPN Whiteboard specification.</li>
 *     <li>OSGi services with interfaces ({@link org.osgi.framework.Constants#OBJECTCLASS}) from this package. This
 *     method may be called <em>explicit whiteboard approach</em> and is specific to Pax Web itself.</li>
 * </ul></p>
 *
 * <p>Because internally, <em>whiteboard</em> and {@link org.osgi.service.http.HttpService} approaches are
 * implemented in very similar fashion, these interfaces are used throughout entire Pax Web.</p>
 *
 * <p>The <em>Mapping</em> suffix of names for interfaces from this package matches the convention taken from
 * Jetty, but other containers also have similar concept groupping actual servlet with it's registration
 * parameters:<ul>
 *     <li>{@code org.eclipse.jetty.servlet.ServletMapping}</li>
 *     <li>{@code org.apache.catalina.core.StandardWrapper}</li>
 *     <li>{@code io.undertow.servlet.api.ServletInfo}</li>
 * </ul>
 * So all interfaces representing registered <em>element</em> with additional parameters are called
 * <em>XxxMapping</em>.</p>
 */
package org.ops4j.pax.web.service.whiteboard;
