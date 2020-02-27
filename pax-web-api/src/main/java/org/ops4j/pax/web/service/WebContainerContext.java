/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service;

import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.whiteboard.ContextRelated;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * <p>{@link HttpContext} extension that adds:<ul>
 *     <li><em>identity</em> (String ID) to {@link HttpContext} (knowing that single bundle using such context
 *     is part of the identity)</li>
 *     <li>missing resource-access method matching {@link javax.servlet.ServletContext#getResourcePaths(String)}
 *     method.</li>
 * </ul></p>
 *
 * <p>All methods returning a <em>context</em> in {@link WebContainer} extension of
 * {@link org.osgi.service.http.HttpService} return implementations of this interface.</p>
 *
 * <p>No extension of the original {@link HttpContext} should specify such things as context path, virtual hosts
 * or parameters - these (to match Whiteboard specification) should be specified using service registration
 * paremeters).</p>
 *
 * <p>Internally, Pax Web will wrap "new" {@link org.osgi.service.http.context.ServletContextHelper} instances
 * in some implementation of {@link WebContainerContext} interface.</p>
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.3, March 30, 2009
 */
public interface WebContainerContext extends HttpContext {

	/**
	 * <p>Complement {@link HttpContext#getResource(String)} (that matches
	 * {@link javax.servlet.ServletContext#getResource(String)}), so we have a method matching
	 * {@link javax.servlet.ServletContext#getResourcePaths(String)}.</p>
	 *
	 * <p>from {@link javax.servlet.ServletContext} javadoc: Returns a set of all the paths (String objects)
	 * to entries within the web application whose longest sub-path matches the supplied path argument.
	 * A specified path of "/" indicates the root of the web application.</p>
	 *
	 * <p>The methods requires classLoader access, because embedded bundle JARs (listed in {@code Bundle-ClassPath}
	 * manifest header) should be checked as well.</p>
	 *
	 * @param path the path name for which to return resource paths
	 * @return a set of the resource paths (String objects) or null if no resource paths could be found or if
	 *         the caller does not have the appropriate permissions.
	 */
	Set<String> getResourcePaths(String path);

	/**
	 * Method matching {@link javax.servlet.ServletContext#getRealPath(String)} and
	 * {@link org.osgi.service.http.context.ServletContextHelper#getRealPath(String)}, but not available in
	 * original {@link HttpContext}
	 *
	 * @param path
	 * @return
	 */
	String getRealPath(final String path);

	/**
	 * Method that backports {@link org.osgi.service.http.context.ServletContextHelper#finishSecurity}
	 * into <em>old</em> {@link HttpContext}
	 *
	 * @param request
	 * @param response
	 */
	void finishSecurity(final HttpServletRequest request, final HttpServletResponse response);

	/**
	 * <p>Returns the name (identifier) of this <em>context</em>.</p>
	 *
	 * <p>Such <em>context</em> may then be referenced using:<ul>
	 *     <li>standard (Whiteboard) {@code osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=name)}
	 *     service registration property (140.3 Common Whiteboard Properties)</li>
	 *     <li>standard (Whiteboard) {@code osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.httpservice=name)}
	 *     service registration property (140.10 Integration with Http Service Contexts)</li>
	 *     <li>legacy (Pax Web specific) {@code httpContext.id=name} service registration property</li>
	 *     <li>{@link ContextRelated#getContextSelectFilter()} on registered <em>mappings</em></li>
	 *     <li>{@link ContextRelated#getContextId()} on registered <em>mappings</em></li>
	 * </ul></p>
	 *
	 * <p>There's security concern related to String identification of <em>context</em> - both for
	 * {@link org.osgi.service.http.HttpService#registerServlet} and whiteboard approach. If (as Pax Web allows)
	 * <em>shared</em> context is used, there should be no way of accessing resources from one bundle by another
	 * bundle. Whiteboard specification is more clear about it - resources are loaded from the bundle registering
	 * (publishing) {@link org.osgi.service.http.context.ServletContextHelper} service and there's assumed
	 * <em>sharing</em> of the context between bundles. That's why user chosing {@link MultiBundleWebContainerContext}
	 * has to be aware of <em>opening</em> an access to all bundles sharing such <em>context</em>.</p>
	 *
	 * @return name of the Context
	 */
	String getContextId();

	/**
	 * <p>Should this <em>context</em> (as defined in "102 Http Service" specification, not in "140 Whiteboard Service"
	 * specification) be allowed to use by different bundles?</p>
	 *
	 * <p>Extension of this interface for Whiteboard Specification (to wrap
	 * {@link org.osgi.service.http.context.ServletContextHelper}) should be shared by default.</p>
	 *
	 * @return
	 */
	boolean isShared();

	/**
	 * <p>If context is a reference, this means we use tricky ways of Http Service registration without a need to
	 * pass existing implementation of {@link HttpContext} around.</p>
	 *
	 * <p>Reference may point to shared or to bundle-scoped context. In latter case, it's important to ensure
	 * context equality by ID and bundle <strong>only</strong>.</p>
	 *
	 * @return
	 */
	default boolean isReference() {
		return false;
	}

	/**
	 * Actual bundle on behalf of which the {@link WebContainerContext} was created. It SHOULD be {@code null} for
	 * shared contexts.
	 *
	 * @return
	 */
	Bundle getBundle();

	/**
	 * Default context identifiers.
	 */
	enum DefaultContextIds {
		/**
		 * Used for {@link org.osgi.service.http.HttpService#createDefaultHttpContext()}
		 */
		DEFAULT(PaxWebConstants.DEFAULT_CONTEXT_NAME),

		/**
		 * Used for {@link WebContainer#createDefaultSharedHttpContext()}
		 */
		SHARED("shared"),

		/**
		 * Used internally for all other contexts created to register servlets/filters/... where actual class
		 * directly implements {@link HttpContext} (and not {@link WebContainerContext}).
		 */
		CUSTOM("custom");

		private final String value;

		DefaultContextIds(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

}
