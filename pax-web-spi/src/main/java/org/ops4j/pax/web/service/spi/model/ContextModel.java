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
package org.ops4j.pax.web.service.spi.model;

import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Models a {@link javax.servlet.ServletContext servlet context} related to an {@link HttpContext http context}.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 29, 2007
 */
public class ContextModel extends Identity {

	private final WebContainerContext httpContext;
	private final ClassLoader classLoader;
	private final Map<String, String> contextParams;
	private String contextName;

	/**
	 * Access controller context of the bundle that registered the http context.
	 */
	private AccessControlContext accessControllerContext;
	/**
	 * Registered jsp servlets for this context.
	 */
	private Map<Servlet, String[]> jspServlets;
	/**
	 * Session timeout in minutes.
	 */
	private Integer sessionTimeout;
	/**
	 * Session cookie name.
	 */
	private String sessionCookie;
	/**
	 * Session cookie domain.
	 */
	private String sessionDomain;
	/**
	 * Session cookie path.
	 */
	private String sessionPath;
	/**
	 * Session URL parameter name.
	 */
	private String sessionUrl;
	/**
	 * Session Cookie for HttpOnly
	 */
	private Boolean sessionCookieHttpOnly;
	/**
	 * Session Cookie secure
	 */
	private Boolean sessionCookieSecure;
	/**
	 * Session Cookie max Age in seconds
	 */
	private Integer sessionCookieMaxAge;
	/**
	 * Name appended to session id, used to assist session affinity in a load
	 * balancer.
	 */
	private String sessionWorkerName;
	/**
	 * Bundle that used the http context to register an web element.
	 */
	private final Bundle bundle;
	private final Boolean showStacks;
	/**
	 * The realm name to use with the http context.
	 */
	private String realmName;
	/**
	 * The authorization method used in this http context.
	 */
	private String authMethod;

	/**
	 * Login page for FORM based authentication.
	 */
	private String formLoginPage;

	/**
	 * Error page for FORM based authentication.
	 */
	private String formErrorPage;

	/**
	 * Container Initializers
	 */
	private Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers;

	/**
	 * Jetty Web XML URL
	 */
	private URL jettyWebXmlUrl;

	/**
	 * Virtual Host List
	 */
	private final List<String> virtualHosts;

	/**
	 * Connectors List
	 */
	private final List<String> connectors;

	/**
	 * flag showing if this context is configured through/like a webBundle. Will
	 * be set in case of using the <code>HttpServiceStarted</code>
	 * <code>begin</code> method.
	 */
	private boolean webBundle;

	/**
	 * JSPConfig TagLib location
	 */
	private String tagLibLocation;
	private String tagLibUri;
	private List<String> jspDisplayNames;
	private List<String> jspIncludeCodes;
	private List<String> jspIncludePreludes;
	private List<String> jspUrlPatterns;
	private Boolean jspElIgnored;
	private Boolean jspScriptingInvalid;
	private Boolean jspIsXml;

	/**
	 * @param httpContext
	 * @param bundle
	 * @param classLoader
	 */
	public ContextModel(final WebContainerContext httpContext, final Bundle bundle,
						final ClassLoader classLoader, final Boolean showStacks) {
		this.bundle = bundle;
		NullArgumentException.validateNotNull(httpContext, "Http context");
		NullArgumentException.validateNotNull(classLoader, "Class loader");
		this.classLoader = classLoader;
		this.httpContext = httpContext;
		this.contextParams = new HashMap<>();
		this.jspServlets = new IdentityHashMap<>();
		this.contextName = "";
		// capture access controller context of the bundle that registered the
		// context
		// TODO does this work with an extender bundle?
		this.accessControllerContext = AccessController.getContext();
		this.virtualHosts = new ArrayList<>();
		this.connectors = new ArrayList<>();
		this.webBundle = false;
		this.showStacks = showStacks;
	}

	public WebContainerContext getHttpContext() {
		return httpContext;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * @return the formLoginPage
	 */
	public String getFormLoginPage() {
		return formLoginPage;
	}

	/**
	 * @return the formErrorPage
	 */
	public String getFormErrorPage() {
		return formErrorPage;
	}

	@SuppressWarnings("rawtypes")
	public void setContextParams(final Dictionary contextParameters) {
		contextParams.clear();
		if (contextParameters != null && !contextParameters.isEmpty()) {
			final Enumeration keys = contextParameters.keys();
			while (keys.hasMoreElements()) {
				final Object key = keys.nextElement();
				final Object value = contextParameters.get(key);
				if (!(key instanceof String) || !(value instanceof String)) {
					throw new IllegalArgumentException(
							"Context params keys and values must be Strings");
				}
				contextParams.put((String) key, (String) value);
			}
			contextName = contextParams.get(WebContainerConstants.CONTEXT_NAME);
		}
		if (contextName != null) {
			contextName = contextName.trim();
		} else {
			contextName = "";
		}
	}

	/**
	 * Getter.
	 *
	 * @return map of context params
	 */
	public Map<String, String> getContextParams() {
		return contextParams;
	}

	/**
	 * Getter.
	 *
	 * @return context name
	 */
	public String getContextName() {
		return contextName;
	}

	/**
	 * Getter.
	 *
	 * @return jsp servlet
	 */
	public Map<Servlet, String[]> getJspServlets() {
		return jspServlets;
	}

	/**
	 * Getter.
	 *
	 * @return the access controller context of the bundle that registred the
	 * context
	 */
	public AccessControlContext getAccessControllerContext() {
		return accessControllerContext;
	}

	/**
	 * Getter.
	 *
	 * @return session timeout
	 */
	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	/**
	 * Setter.
	 *
	 * @param sessionTimeout value to set
	 */
	public void setSessionTimeout(Integer sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	/**
	 * Getter.
	 *
	 * @return session cookie name
	 */
	public String getSessionCookie() {
		return sessionCookie;
	}

	/**
	 * Setter.
	 *
	 * @param sessionCookie session cookie name
	 */
	public void setSessionCookie(final String sessionCookie) {
		this.sessionCookie = sessionCookie;
	}

	/**
	 * Getter.
	 *
	 * @return session cookie domain
	 */
	public String getSessionDomain() {
		return sessionDomain;
	}

	/**
	 * Setter.
	 *
	 * @param sessionDomain session cookie domain
	 */
	public void setSessionDomain(final String sessionDomain) {
		this.sessionDomain = sessionDomain;
	}

	/**
	 * Getter.
	 *
	 * @return session cookie path
	 */
	public String getSessionPath() {
		return sessionPath;
	}

	/**
	 * Setter.
	 *
	 * @param sessionPath session cookie path
	 */
	public void setSessionPath(final String sessionPath) {
		this.sessionPath = sessionPath;
	}

	/**
	 * Getter.
	 *
	 * @return session url name
	 */
	public String getSessionUrl() {
		return sessionUrl;
	}

	/**
	 * Setter.
	 *
	 * @param sessionUrl session url name
	 */
	public void setSessionUrl(final String sessionUrl) {
		this.sessionUrl = sessionUrl;
	}

	/**
	 * @return session cookie HTTP Only
	 */
	public Boolean getSessionCookieHttpOnly() {
		return sessionCookieHttpOnly;
	}

	public void setSessionCookieHttpOnly(final Boolean sessionCookieHttpOnly) {
		this.sessionCookieHttpOnly = sessionCookieHttpOnly;
	}

	/**
	 * Getter.
	 *
	 * @return session cookie secure
	 */
	public Boolean getSessionCookieSecure() {
		return sessionCookieSecure;
	}

	/**
	 * Setter.
	 *
	 * @param sessionCookieSecure session cookie secure flag
	 */
	public void setSessionCookieSecure(final Boolean sessionCookieSecure) {
		this.sessionCookieSecure = sessionCookieSecure;
	}

	/**
	 * Getter.
	 *
	 * @return session cookie max age
	 */
	public Integer getSessionCookieMaxAge() {
		return sessionCookieMaxAge;
	}

	/**
	 * Setter.
	 *
	 * @param sessionCookieMaxAge session cookie max age
	 */
	public void setSessionCookieMaxAge(Integer sessionCookieMaxAge) {
		this.sessionCookieMaxAge = sessionCookieMaxAge;
	}

	/**
	 * Getter.
	 *
	 * @return session worker name
	 */
	public String getSessionWorkerName() {
		return sessionWorkerName;
	}

	/**
	 * Setter.
	 *
	 * @param sessionWorkerName session worker name
	 */
	public void setSessionWorkerName(final String sessionWorkerName) {
		this.sessionWorkerName = sessionWorkerName;
	}

	/**
	 * Getter.
	 *
	 * @return bundle associated with this context
	 */
	public Bundle getBundle() {
		return bundle;
	}

	public Boolean isShowStacks() {
		return showStacks;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder()
				.append(this.getClass().getSimpleName()).append("{")
				.append("id=").append(getId()).append(",name=")
				.append(contextName).append(",httpContext=")
				.append(httpContext).append(",contextParams=")
				.append(contextParams).append(",virtualHosts={");
		for (String virtualHost : virtualHosts) {
			sb.append(virtualHost).append(",");
		}
		sb.append("},connectors={");
		for (String connector : connectors) {
			sb.append(connector).append(",");
		}
		return sb.append("}}").toString();
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

	public void setAuthMethod(String authMethod) {
		this.authMethod = authMethod;
	}

	public void setFormLoginPage(String formLoginPage) {
		this.formLoginPage = formLoginPage;
	}

	public void setFormErrorPage(String formErrorPage) {
		this.formErrorPage = formErrorPage;
	}

	/**
	 * @return the realmName
	 */
	public String getRealmName() {
		return realmName;
	}

	/**
	 * @return the authMethod
	 */
	public String getAuthMethod() {
		return authMethod;
	}

	/**
	 * @return the containerInitializers
	 */
	public Map<ServletContainerInitializer, Set<Class<?>>> getContainerInitializers() {
		return containerInitializers;
	}

	/**
	 * @param containerInitializers the containerInitializers to set
	 */
	public void setContainerInitializers(
			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers) {
		this.containerInitializers = containerInitializers;
	}

	public void addContainerInitializer(
			ServletContainerInitializer containerInitializer,
			Set<Class<?>> classes) {
		if (this.containerInitializers == null) {
			containerInitializers = new LinkedHashMap<>();
		}
		containerInitializers.put(containerInitializer, classes);
	}

	public void setVirtualHosts(List<String> virtualHosts) {
		this.virtualHosts.clear();
		this.virtualHosts.addAll(virtualHosts);
	}

	public List<String> getVirtualHosts() {
		return virtualHosts;
	}

	public void setJettyWebXmlUrl(URL jettyWebXmlUrl) {
		this.jettyWebXmlUrl = jettyWebXmlUrl;
	}

	public URL getJettyWebXmlURL() {
		return jettyWebXmlUrl;
	}

	/**
	 * @return the webBundle
	 */
	public boolean isWebBundle() {
		return webBundle;
	}

	/**
	 * @param webBundle the webBundle to set
	 */
	public void setWebBundle(boolean webBundle) {
		this.webBundle = webBundle;
	}

	public void addTagLibLocation(String tagLibLocation) {
		this.tagLibLocation = tagLibLocation;
	}

	public String getTagLibLocation() {
		return tagLibLocation;
	}

	public void addTagLibUri(String tagLibUri) {
		this.tagLibUri = tagLibUri;
	}

	public String getTagLibUri() {
		return tagLibUri;
	}

	public void addJspDisplayNames(List<String> displayNames) {
		this.jspDisplayNames = displayNames;
	}

	public List<String> getJspDisplayNames() {
		return jspDisplayNames;
	}

	public void addJspIncludeCodes(List<String> includeCodes) {
		this.jspIncludeCodes = includeCodes;
	}

	public List<String> getJspIncludeCodes() {
		return jspIncludeCodes;
	}

	public void addJspIncludePreludes(List<String> includePreludes) {
		this.jspIncludePreludes = includePreludes;
	}

	public List<String> getJspIncludePreludes() {
		return jspIncludePreludes;
	}

	public void addJspUrlPatterns(List<String> urlPatterns) {
		this.jspUrlPatterns = urlPatterns;
	}

	public List<String> getJspUrlPatterns() {
		return jspUrlPatterns;
	}

	public void addJspElIgnored(Boolean elIgnored) {
		this.jspElIgnored = elIgnored;
	}

	public Boolean getJspElIgnored() {
		return jspElIgnored;
	}

	public void addJspScriptingInvalid(Boolean scriptingInvalid) {
		this.jspScriptingInvalid = scriptingInvalid;
	}

	public Boolean getJspScriptingInvalid() {
		return jspScriptingInvalid;
	}

	public void addJspIsXml(Boolean isXml) {
		this.jspIsXml = isXml;
	}

	public Boolean getJspIsXml() {
		return jspIsXml;
	}
}
