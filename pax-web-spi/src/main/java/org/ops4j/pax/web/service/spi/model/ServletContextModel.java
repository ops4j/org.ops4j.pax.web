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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.JspConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.SessionConfigurationModel;

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

	/**
	 * 1:N relation to OSGi-specific contexts pointing to this server-specific context. This list may change
	 * dynamically, because some existing {@link OsgiContextModel} may have its
	 * {@link org.osgi.service.http.whiteboard.HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_PATH} changed, switching
	 * it to entirely different {@link ServletContextModel}.
	 */
	private final Set<OsgiContextModel> osgiContextModels = new HashSet<>();

	/**
	 * Session configuration as defined by {@link ServletContext#getSessionTimeout()},
	 * {@link ServletContext#getSessionCookieConfig()} and {@link ServletContext#getEffectiveSessionTrackingModes()}.
	 */
	private SessionConfigurationModel sessionConfiguration;

	/** JSP configuration as defined in {@link ServletContext#getJspConfigDescriptor()} */
	private JspConfigurationModel jspConfiguration;

	/** Login configurations as specified by {@code <login-config>} element from {@code web.xml}. */
	private final List<LoginConfigModel> loginConfigurations = new ArrayList<>();

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
	 *
	 * <p>TODO: This map also includes those URL patterns from {@link #servletUrlPatternMapping} which are defined
	 *     as <em>exact</em> mappings in Servlet API specification.</p>
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

	/**
	 * Sorted set (by service rank desc, service id asc, name asc) mapped to <em>any</em> servlet or URL pattern.
	 * These filters are then mapped to required servlets or URLs at server controller level. Even if most of the
	 * requests won't involve calling all the filters, the order kept here is preserved.
	 */
	private final Set<FilterModel> filterModels = new TreeSet<>();

//	/**
//	 * Mapping between full registration url patterns and filter model. Full url
//	 * pattern mean that it has the context name prepended (if context name is
//	 * set) to the actual url pattern. Used to globally find (against all
//	 * registered patterns) the right filter context for the pattern.
//	 * The map is wrapped into map for virtual hosts.
//	 */
//	private final ConcurrentMap<String, ConcurrentMap<String, Set<ServerModel.UrlPattern>>> filterUrlPatterns;

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

	public String getContextPath() {
		return contextPath;
	}

	public Set<OsgiContextModel> getOsgiContextModels() {
		return osgiContextModels;
	}

	public SessionConfigurationModel getSessionConfiguration() {
		return sessionConfiguration;
	}

	public void setSessionConfiguration(SessionConfigurationModel sessionConfiguration) {
		this.sessionConfiguration = sessionConfiguration;
	}

	public JspConfigurationModel getJspConfiguration() {
		return jspConfiguration;
	}

	public void setJspConfiguration(JspConfigurationModel jspConfiguration) {
		this.jspConfiguration = jspConfiguration;
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
