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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WebSocketModel;

/**
 * <p>This class is 1:1 representation of server-specific {@link ServletContext} and is unaware of
 * OSGi-related representation of a <em>context</em> (like {@link org.osgi.service.http.HttpContext} and
 * {@link org.osgi.service.http.context.ServletContextHelper}).</p>
 *
 * <p>It represents a <em>web application</em> (that could be described using single {@code web.xml} descriptor),
 * while the <em>web elements</em> (like servlets, filters, ...) or configuration (session, JSP config, ...) are
 * contributed possibly by many bundles (bundle-scoped {@link org.osgi.service.http.HttpService} services or
 * whiteboard service registrations).</p>
 *
 * <p>Normally, web applications are represented by {@code web.xml} files which are parsed by server-specific
 * parsers:<ul>
 *     <li>Jetty:  {@code org.eclipse.jetty.webapp.StandardDescriptorProcessor.visit()}</li>
 *     <li>Tomcat: {@code org.apache.catalina.startup.ContextConfig#configureContext()},
 *                 {@code org.apache.tomcat.util.descriptor.web.WebXmlParser#parseWebXml()}</li>
 * </ul></p>
 *
 * <p>When user registers (through {@link org.osgi.service.http.HttpService} or Whiteboard) a <em>web element</em>
 * (like servlet), the registration seems unique, but physically, given servlet has to be explicitly registered
 * to all related {@link ServletContextModel} (and eventually - {@link ServletContext}). It is even more important
 * during unregistration - simple call to {@link org.osgi.service.http.HttpService#unregister(String)} has to
 * iterate over all mapped {@link ServletContextModel} contexts.</p>
 *
 * <p>Each {@link OsgiContextModel} pointing to this class may declare <em>virtual hosts</em>. Actual, server
 * specific context has to be mapped (server-specific method) to all these virtual hosts.</p>
 *
 * <p>Registered whiteboard element may refer to many {@link OsgiContextModel} - pointing to different or the same
 * target {@link ServletContextModel}. If two (or more) {@link OsgiContextModel} point to the same
 * {@link ServletContextModel}, priority (service rank + service id) is checked to find single {@link OsgiContextModel}
 * and associated helper.</p>
 *
 * @since 8.0.0
 */
public final class ServletContextModel extends Identity {

	/** <em>Context path</em> as defined by {@link ServletContext#getContextPath()}. */
	private final String contextPath;

	/** Servlet name mapping enforces servlet name uniqueness within a context. */
	private final Map<String, ServletModel> servletNameMapping = new HashMap<>();

	/**
	 * <p>Http Service specification ({@link org.osgi.service.http.HttpService#registerServlet}) uses the concept
	 * of <em>alias</em> and requires it to be unique. It's effectively a n<em>exact</em> URL mapping for a servlet,
	 * according to "12.2 Specification of Mappings" from Servlet specification.</p>
	 *
	 * <p>Normally this is checked at server level, but for the sake of compliance, we have to do it before hitting
	 * the server. And we have to take into account virtual hosts (not available in specification) and
	 * context paths (mentioned only in Whiteboard Service specification).</p>
	 */
	private final Map<String, ServletModel> aliasMapping = new HashMap<>();

	/**
	 * <p>Similar to {@link #aliasMapping}, but for different registration methods - not using alias, but
	 * normal URL patterns (both through {@link org.ops4j.pax.web.service.WebContainer} extended registration
	 * methods and through Whiteboard).</p>
	 *
	 * <p>This map also includes aliases from {@link #aliasMapping}.</p>
	 */
	private final Map<String, ServletModel> servletUrlPatternMapping = new HashMap<>();

	/** Filter name mapping enforces filter name uniqueness within a context. */
	private final Map<String, FilterModel> filterNameMapping = new HashMap<>();

	/** Mapping of WebSocket paths to WebSocket models */
	private final Map<String, WebSocketModel> webSocketUrlPathMapping = new HashMap<>();

	/**
	 * Mapping between error code / wildcard / exception class name and actual (enabled) {@link ErrorPageModel}
	 * that is used for given error.
	 */
	private final Map<String, ErrorPageModel> errorPageMapping = new HashMap<>();

	public ServletContextModel(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * <p>Marks given {@link ServletModel} as enabled, which means it has to be immediately added to relevant
	 * mappings, because validation was performed earlier.</p>
	 *
	 * @param model
	 */
	public void enableServletModel(ServletModel model) {
		if (model.getAlias() != null) {
			aliasMapping.put(model.getAlias(), model);
		}
		Arrays.stream(model.getUrlPatterns()).forEach(p -> servletUrlPatternMapping.put(p, model));
		servletNameMapping.put(model.getName(), model);
	}

	/**
	 * <p>Marks given {@link ServletModel} as disabled, which means it should no longer be available on active
	 * mappings, and should be moved to list of <em>disabled</em> servlets.</p>
	 *
	 * <p>This method just removes the model from {@link ServletContextModel}, but special care should be taken,
	 * because given {@link ServletModel} has to be remembered as <em>disabled</em> in {@link ServerModel}.</p>
	 *
	 * @param model
	 */
	public void disableServletModel(ServletModel model) {
		if (model.getAlias() != null) {
			aliasMapping.remove(model.getAlias());
		}
		servletUrlPatternMapping.entrySet().removeIf(e -> e.getValue().equals(model));
		servletNameMapping.remove(model.getName());
	}

	/**
	 * <p>Marks given {@link FilterModel} as enabled.</p>
	 *
	 * @param model
	 */
	public void enableFilterModel(FilterModel model) {
		filterNameMapping.put(model.getName(), model);
	}

	/**
	 * <p>Marks given {@link FilterModel} as disabled.</p>
	 *
	 * @param model
	 */
	public void disableFilterModel(FilterModel model) {
		filterNameMapping.remove(model.getName());
	}

	/**
	 * Mark given {@link ErrorPageModel} as enabled
	 * @param model
	 */
	public void enableErrorPageModel(ErrorPageModel model) {
		for (String page : model.getErrorPages()) {
			errorPageMapping.put(page, model);
		}
	}

	/**
	 * Mark given {@link ErrorPageModel} as disabled
	 * @param model
	 */
	public void disableErrorPageModel(ErrorPageModel model) {
		for (String page : model.getErrorPages()) {
			errorPageMapping.remove(page);
		}
	}

	/**
	 * <p>Marks given {@link WebSocketModel} as enabled.</p>
	 *
	 * @param model
	 */
	public void enableWebSocketModel(WebSocketModel model) {
		webSocketUrlPathMapping.put(model.getMappedPath(), model);
	}

	/**
	 * <p>Marks given {@link WebSocketModel} as disabled.</p>
	 *
	 * @param model
	 */
	public void disableWebSocketModel(WebSocketModel model) {
		webSocketUrlPathMapping.remove(model.getMappedPath());
	}

	public String getContextPath() {
		return contextPath;
	}

	public Map<String, ServletModel> getServletNameMapping() {
		return servletNameMapping;
	}

	public Map<String, FilterModel> getFilterNameMapping() {
		return filterNameMapping;
	}

	public Map<String, ServletModel> getAliasMapping() {
		return aliasMapping;
	}

	public Map<String, ServletModel> getServletUrlPatternMapping() {
		return servletUrlPatternMapping;
	}

	public Map<String, ErrorPageModel> getErrorPageMapping() {
		return errorPageMapping;
	}

	public Map<String, WebSocketModel> getWebSocketUrlPathMapping() {
		return webSocketUrlPathMapping;
	}

	@Override
	public String toString() {
		return "ServletContextModel{id=" + getId() + ",contextPath='" + contextPath + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ServletContextModel that = (ServletContextModel) o;
		return contextPath.equals(that.contextPath);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contextPath);
	}

}
