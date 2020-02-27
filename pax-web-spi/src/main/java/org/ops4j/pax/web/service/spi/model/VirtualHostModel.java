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
package org.ops4j.pax.web.service.spi.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>A <em>server</em> may have many <em>virtual hosts</em> (including the default one). Many conflict
 * resolutions are checked at this level. In Pax Web, <em>virtual host</em> is a concept related only to
 * namespace organization (like allowing the same context path for different virtual hosts).</p>
 *
 * <p>When an element (context, servlet, filter) is registered in target server runtime, virtual host
 * checking during request processing is performed only by given runtime, not by classes from this package.</p>
 *
 * <p>At runtime, virtual host is usually selected using {@code Host:} HTTP header
 * (see <a href="https://tools.ietf.org/html/rfc2616#section-14.23">HTTP/1.1 RFC 2616</a>), but the underlying
 * server may be configured to use different indicator, e.g., related to connector.</p>
 *
 * <p>In case of Tomcat, the host selection is indicated using:<ul>
 *     <li>If {@code org.apache.catalina.connector.Connector#setUseIPVHosts(true)},
 *     {@link HttpServletRequest#getLocalName()} is used as host selector</li>
 *     <li>If {@code org.apache.catalina.connector.Connector#setUseIPVHosts(false)} (default),
 *     {@link HttpServletRequest#getRemoteHost()} is used as host selector ({@code Host:} header).</li>
 * </ul>
 * And a <em>host</em> is always (even if default) available inside a <em>service</em>.
 * See <a href="https://tomcat.apache.org/tomcat-9.0-doc/config/host.html#Common_Attributes">Tomcat's Virtual host
 * configuration</a>.<br />Wildcards are supported only in aliases of given host.</p>
 *
 * <p>In Jetty, {@code org.eclipse.jetty.servlet.ServletContextHandler} may choose to handle only dedicated
 * virtual host requests. By default it handles all requests, but it may be configured to handle only some requests,
 * where a <em>host</em> is indicated either by {@code Host:} HTTP header or, when virtual host starts with "@",
 * using connector's name. See <a href="https://www.eclipse.org/jetty/documentation/current/configuring-virtual-hosts.html#configuring-a-virtual-host">
 * Jetty Virtual host configuration</a>. Checking is performed in
 * {@code org.eclipse.jetty.server.handler.ContextHandler#checkVirtualHost(org.eclipse.jetty.server.Request)}</p>
 *
 * <p>In Undertow, unless {@code io.undertow.server.handlers.NameVirtualHostHandler} is used, no virtual host
 * handling is performed. However, wildcard and connector based virtual hosts are not supported with this simple
 * handler.</p>
 *
 * <p>Both Tomcat and Jetty support <strong>one level</strong> of wildcards in host names.</p>
 */
public class VirtualHostModel extends Identity {

	private String name;

	/**
	 * <p>Aliases for virtual hosts.</p>
	 *
	 * <p>In Tomcat it's straightforward. Each {@code <Host>} configuration element can have single name, which
	 * is not a wildcard ("canonical, fully qualified, name of the virtual host") and can have list of aliases
	 * ({@code <Host>/<Alias>} elements), which <em>may</em> use single, leading wildcard in the form of
	 * "{@code *.}".</p>
	 *
	 * <p>In Jetty, there are no aliases. There are even no <em>virtual hosts</em> as real entity. Virtual host
	 * identifiers have 4 forms:<ul>
	 *     <li>{@code virtual.host.name} - exact name</li>
	 *     <li>{@code *.host.name} - name with initial wildcard</li>
	 *     <li>{@code virtual.host.name@connectorName} - name (exact or wildcard) with appended connector ID
	 *     (from {@code org.eclipse.jetty.server.AbstractConnector#setName(java.lang.String)})</li>
	 *     <li>{@code @connectorName} - only name of the connector</li>
	 * </ul>
	 * and may be used as <em>virtual host</em> names set on given
	 * {@code org.eclipse.jetty.server.handler.ContextHandler}. This makes it (a bit) easier than in Tomcat, because
	 * in Jetty there's actually single instance and reference to a context. In Tomcat, given {@code StandardContext}
	 * has to be explicitly added to possibly many {@code StandardHost}s.
	 * </p>
	 */
	private final Map<String, List<String>> virtualHostAliases = new HashMap<>();

	/**
	 * <p>Global mapping between physical <em>context path</em> and server-specific {@link ServletContextModel}.
	 * Each {@link ServletContextModel} may be supported/backed/populated by many {@link OsgiContextModel} instances.
	 * There can be only one such {@link ServletContextModel} for given <em>context path</em>.</p>
	 *
	 * <p>A single <em>web application</em> or server-managed {@link javax.servlet.ServletContext} means
	 * the same - disjoint partition of global URI namespace within given <em>virtual host</em>.</p>
	 *
	 * <p>There's no explicit mapping between one {@link ServletContextModel} and many {@link OsgiContextModel} here.
	 * Such mapping is kept internally inside {@link ServletContextModel} and {@link OsgiContextModel}.</p>
	 *
	 * <p>Given {@link ServletContextModel} may be present in several such mappings in different virtual hosts,
	 * but <strong>always under the same context path</strong>.</p>
	 */
	private final Map<String, ServletContextModel> contextsByPath = new HashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, List<String>> getVirtualHostAliases() {
		return virtualHostAliases;
	}

	public Map<String, ServletContextModel> getContextsByPath() {
		return contextsByPath;
	}

	@Override
	public String toString() {
		return "VirtualHostModel{id=" + getId() + ",name='" + name + "'}";
	}

}
