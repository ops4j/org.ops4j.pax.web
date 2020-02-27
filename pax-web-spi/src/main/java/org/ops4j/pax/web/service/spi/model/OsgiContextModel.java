/*
 * Copyright 2007 Alin Dreghiciu.
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper ;

/**
 * <p>This class represents OSGi-specific {@link HttpContext}/{@link ServletContextHelper}
 * and points to single, server-specific {@link javax.servlet.ServletContext} and (at model level) to single
 * {@link ServletContextModel}. It maps <em>directly</em> 1:1 to an OSGi service registered by user:<ul>
 *     <li>{@link HttpContext} with legacy Pax Web servier registration properties</li>
 *     <li>{@link ServletContextHelper} with standard properties and/or annotations</li>
 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}</li>
 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}</li>
 * </ul>
 * Discovered service registration properties are stored as well to ensure proper context selection according
 * to 140.3 Common Whiteboard Properties.</p>
 *
 * <p>The most important role is to wrap actual {@link HttpContext} or
 * {@link ServletContextHelper} that'll be used when given servlet will be accessing
 * own {@link ServletContext}, to comply with Whiteboard Specification.</p>
 *
 * <p>While many {@link OsgiContextModel OSGi-related contexts} may point to single {@link ServletContextModel} and
 * contribute different web elements (like some bundles provide servlets and other bundle provide login configuration),
 * some aspects need conflict resolution - for example session timeout setting. Simply highest ranked
 * {@link OsgiContextModel} will be the one providing the configuration for given {@link ServletContextModel}.</p>
 *
 * <p>Some aspects of {@link ServletContext} visible to registered element are however dependent on which particular
 * {@link OsgiContextModel} was used. Resource access will be done through {@link HttpContext} or
 * {@link ServletContextHelper} and context parameters will be stored in this
 * class (remember: there can be different {@link OsgiContextModel} for the same {@link ServletContextModel}, but
 * providing different init parameters ({@code <context-param>} from {@code web.xml}).</p>
 *
 * <p>Another zen-like question: there may be two different {@link ServletContextHelper}
 * services registered for the same <em>context path</em> with different
 * {@link ServletContextHelper#handleSecurity}. Then two filters are registered
 * to both of such contexts - looks like when sending an HTTP request matching this common <em>context path</em>,
 * both {@code handleSecurity()} methods must be called before entering the filter pipeline. Fortunately
 * specification is clear about it. "140.5 Registering Servlet Filters" says:<blockquote>
 *     Servlet filters are only applied to servlet requests if they are bound to the same Servlet Context Helper
 *     and the same Http Whiteboard implementation.
 * </blockquote></p>
 *
 * <p>In Felix-HTTP, N:1 mapping between many {@link OsgiContextModel} and {@link ServletContextModel} relationship
 * is handled by {@code org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry}. And
 * {@code org.apache.felix.http.base.internal.registry.HandlerRegistry#registrations} is sorted using 3 criteria:<ul>
 *     <li>context path length: longer path, higher priority</li>
 *     <li>service rank: higher rank, higher priority</li>
 *     <li>service id: higher id, lower priority</li>
 * </ul></p>
 *
 * <p><em>Shadowing</em> {@link OsgiContextModel} (see
 * {@link org.osgi.service.http.runtime.dto.DTOConstants#FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE}) can happen
 * <strong>only</strong> when there's name/id conflict, so:<ul>
 *     <li>When there are two contexts with same name and different context path, one is chosen (using ranking)
 *     - that's the way to override {@code default} context, for example by changing its context path</li>
 *     <li>When there are two contexts with different name and same context path, both are used, because there may
 *     be two Whiteboard servlets registered, associated with both OSGi contexts</li>
 *     <li>If one servlet is associated with two {@link OsgiContextModel} pointing to the same context path, only
 *     one should be used - again, according to service ranking</li>
 * </ul></p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.3.0, December 29, 2007
 */
public final class OsgiContextModel extends Identity implements Comparable<OsgiContextModel> {

	/**
	 * 1:1 mapping to <em>server specific</em> {@link ServletContextModel}. This relation may be switched to
	 * different {@link ServletContextModel} if service registration parameters for given {@link HttpContext} or
	 * {@link ServletContextHelper} change.
	 */
	private ServletContextModel servletContextModel;

	/**
	 * <p>Actual OSGi-specific <em>context</em> (can be {@link HttpContext} or
	 * {@link ServletContextHelper} wrapper) that'll be used by {@link ServletContext}
	 * object visible to web elements associated with this OSGi-specific context.</p>
	 *
	 * <p>If this context {@link WebContainerContext#isShared() allows sharing}, {@link OsgiContextModel} can be
	 * populated by different bundles, but still, the helper {@link HttpContext} or
	 * {@link ServletContextHelper} comes from single bundle that has <em>started</em>
	 * configuration/population of given {@link OsgiContextModel}.</p>
	 */
	private final WebContainerContext httpContext;

	/**
	 * Properties used when {@link HttpContext} or {@link ServletContextHelper}
	 * was registered. Used for context selection by any LDAP-style filter.
	 */
	private final Hashtable<String, Object> contextRegistrationProperties = new Hashtable<>();

	/**
	 * <p>Context parameters as defined by {@link ServletContext#getInitParameterNames()} and
	 * represented by {@code <context-param>} elements if {@code web.xml}.</p>
	 *
	 * <p>Keeping the parameters at OSGi-specific <em>context</em> level instead of server-specific <em>context</em>
	 * level allows to access different parameters for servlets registered with different {@link HttpContext} or
	 * {@link ServletContextHelper} while still pointing to the same
	 * {@link ServletContext}.</p>
	 *
	 * <p>These parameters come from {@code context.init.*} service registration properties.</p>
	 */
	private final Map<String, String> contextParams = new HashMap<>();

	/**
	 * <p>Virtual Host List as specified when {@link ServletContextHelper},
	 * {@link HttpContext} or {@link org.ops4j.pax.web.service.whiteboard.ContextMapping} was registered.</p>
	 *
	 * <p>For each VHost from the list, related {@link ServletContextModel} should be added to given VHost.
	 * Empty list means the {@link ServletContextModel} is part of all, including default (fallback), VHosts.</p>
	 */
	private final List<String> virtualHosts = new ArrayList<>();

	/**
	 * This is the <em>owner</em> bundle of this <em>context</em>. For {@link org.osgi.service.http.HttpService}
	 * scenario, that's the bundle of bundle-scoped {@link org.osgi.service.http.HttpService} used to create
	 * {@link HttpContext}. For Whiteboard scenario, that's the bundle registering
	 * {@link ServletContextHelper}. For old Pax Web Whiteboard, that can be a
	 * bundle which registered <em>shared</em> {@link HttpContext}.
	 */
	private final Bundle ownerBundle;

	/** Registration rank of associated {@link HttpContext} or {@link ServletContextHelper} */
	private int serviceRank = 0;
	/** Registration service.id of associated {@link HttpContext} or {@link ServletContextHelper} */
	private int serviceId = 0;

	public OsgiContextModel(WebContainerContext httpContext, Bundle ownerBundle) {
		this.httpContext = httpContext;
		this.ownerBundle = ownerBundle;
	}

	public WebContainerContext getHttpContext() {
		return httpContext;
	}

	public Map<String, String> getContextParams() {
		return contextParams;
	}

	public Hashtable<String, Object> getContextRegistrationProperties() {
		return contextRegistrationProperties;
	}

	public List<String> getVirtualHosts() {
		return virtualHosts;
	}

	public Bundle getOwnerBundle() {
		return ownerBundle;
	}

	public ServletContextModel getServletContextModel() {
		return servletContextModel;
	}

	public void setServletContextModel(ServletContextModel servletContextModel) {
		this.servletContextModel = servletContextModel;
	}

	public int getServiceRank() {
		return serviceRank;
	}

	public void setServiceRank(int serviceRank) {
		this.serviceRank = serviceRank;
	}

	public int getServiceId() {
		return serviceId;
	}

	public void setServiceId(int serviceId) {
		this.serviceId = serviceId;
	}

	@Override
	public String toString() {
		return "OsgiContextModel{id=" + getId()
				+ ",contextPath='" + servletContextModel.getContextPath()
				+ "',context=" + httpContext
				+ ",bundle=" + ownerBundle
				+ "}";
	}

	@Override
	public int compareTo(OsgiContextModel o) {
		String cp1 = this.getServletContextModel().getContextPath();
		String cp2 = o.getServletContextModel().getContextPath();

		// reverse check - longer path is "first"
		int pathLength = cp2.length() - cp1.length();
		if (pathLength != 0) {
			return pathLength;
		}

		int pathItself = cp1.compareTo(cp2);
		if (pathItself != 0) {
			// no conflict - different contexts
			return pathItself;
		}

		// reverse check for ranking - higher rank is "first"
		int serviceRank = o.getServiceRank() - this.getServiceRank();
		if (serviceRank != 0) {
			return serviceRank;
		}

		// service ID - lower is "first"
		return this.getServiceId() - o.getServiceId();
	}



















//	/** Access controller context of the bundle that registered the http context. */
//	@Review("it's so rarely used - only in one resource access scenario, though there are many such scenarios.")
//	private final AccessControlContext accessControllerContext;
//
//	/**
//	 * Registered jsp servlets for this context.
//	 */
//	private Map<Servlet, String[]> jspServlets;
//
//	private final Boolean showStacks;
//
//	/**
//	 * Jetty Web XML URL
//	 */
//	private URL jettyWebXmlUrl;

//	@SuppressWarnings("rawtypes")
//	public void setContextParams(final Dictionary contextParameters) {
//		contextParams.clear();
//		if (contextParameters != null && !contextParameters.isEmpty()) {
//			final Enumeration keys = contextParameters.keys();
//			while (keys.hasMoreElements()) {
//				final Object key = keys.nextElement();
//				final Object value = contextParameters.get(key);
//				if (!(key instanceof String) || !(value instanceof String)) {
//					throw new IllegalArgumentException(
//							"Context params keys and values must be Strings");
//				}
//				contextParams.put((String) key, (String) value);
//			}
//			contextName = contextParams.get(PaxWebConstants.CONTEXT_NAME);
//		}
//		if (contextName != null) {
//			contextName = contextName.trim();
//		} else {
//			contextName = "";
//		}
//	}
//	/**
//	 * Getter.
//	 *
//	 * @return jsp servlet
//	 */
//	public Map<Servlet, String[]> getJspServlets() {
//		return jspServlets;
//	}
//	/**
//	 * Getter.
//	 *
//	 * @return the access controller context of the bundle that registred the
//	 * context
//	 */
//	public AccessControlContext getAccessControllerContext() {
//		return accessControllerContext;
//	}
//
//	public Boolean isShowStacks() {
//		return showStacks;
//	}
//
//	public void setJettyWebXmlUrl(URL jettyWebXmlUrl) {
//		this.jettyWebXmlUrl = jettyWebXmlUrl;
//	}
//
//	public URL getJettyWebXmlURL() {
//		return jettyWebXmlUrl;
//	}

}
