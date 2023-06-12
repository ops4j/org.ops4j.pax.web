/*
 * Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.web.service.spi.context;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.util.Path;
import org.osgi.framework.Bundle;
import org.ops4j.pax.web.service.http.HttpContext;
import org.ops4j.pax.web.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Default implementation of {@link HttpContext} that uses the bundle to lookup
 * resources (as specified in 102.10.2 "public interface HttpContext").</p>
 *
 * <p>Note that OSGi CMPN "128.3.5 Static Content" chapter is the only place where "protected directories" are
 * mentioned, so only the implementation of {@link HttpContext} specific to pax-web-extender-war may contain
 * such protection. Besides Undertow's default servlet, Tomcat's StandardContextValve and Jetty's ContextHandler
 * provide such protection so we'll leverage those.</p>
 *
 * <p>Actually we should not restrict access to the <em>protected</em> directories, because they usually contain
 * important configuration files for e.g., JSF.</p>
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Grzegorz Grzybek
 */
public class DefaultHttpContext implements WebContainerContext {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpContext.class);

	/** Bundle using the {@link HttpService}. */
	protected final Bundle bundle;

	/** Name of the context - together with {@link Bundle} it's part of context's identity */
	protected final String contextId;

	/**
	 * Constructor for bundle-scoped context
	 * @param bundle
	 */
	public DefaultHttpContext(final Bundle bundle) {
		this.bundle = bundle;
		this.contextId = PaxWebConstants.DEFAULT_CONTEXT_NAME;
	}

	/**
	 * Constructor for bundle-scoped context with user-provided name
	 * @param bundle
	 * @param contextId
	 */
	public DefaultHttpContext(final Bundle bundle, String contextId) {
		this.bundle = bundle;
		this.contextId = (contextId == null || contextId.trim().equals(""))
				? PaxWebConstants.DEFAULT_CONTEXT_NAME : contextId;
	}

	/**
	 * Constructor for bundle-agnostic contexts.
	 * @param contextId
	 */
	protected DefaultHttpContext(String contextId) {
		this.bundle = null;
		this.contextId = contextId;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * There is no security by default, so always return "true".
	 * {@inheritDoc}
	 */
	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	public URL getResource(final String name) {
		return getResource(bundle, name);
	}

	/**
	 * Allways returns null as there is no default way to find out the mime type.
	 * {@inheritDoc}
	 */
	@Override
	public String getMimeType(String name) {
		return null;
	}

	@Override
	public Set<String> getResourcePaths(final String name) {
		return getResourcePaths(bundle, name);
	}

	@Override
	public String getContextId() {
		return contextId;
	}

	@Override
	public String toString() {
		return "DefaultHttpContext{bundle=" + bundle + ",contextId='" + contextId + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof UniqueWebContainerContextWrapper) {
			o = ((UniqueWebContainerContextWrapper) o).getDelegate();
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultHttpContext that = (DefaultHttpContext) o;
		return bundle.equals(that.bundle) &&
				contextId.equals(that.contextId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bundle, contextId);
	}

	/**
	 * This method supports {@link ServletContext#getResource(String)} and in default implementation of
	 * {@link HttpContext} interface, according to "102.10.2 public interface HttpContext", "The behavior of the
	 * methods on the default HttpContext is defined as follows:[...] This method calls the
	 * servlet bundle's Bundle.getResource method, and returns the appropriate URL to access the resource"
	 *
	 * @param bundle
	 * @param name
	 * @return
	 */
	protected URL getResource(Bundle bundle, String name) {
		// "name" is passed from jakarta.servlet.ServletContext.getResource() which always should start with a slash
		final String normalizedName = Path.normalizeResourcePath(name);
		LOG.trace("Searching bundle [" + bundle + "] for resource [" + normalizedName + "]");
		return bundle.getResource(normalizedName);
	}

	/**
	 * <p>This method is invoked by default when calling {@link ServletContext#getResourcePaths(String)} in OSGi
	 * <em>context</em> backed by {@link HttpContext}.
	 * Just as the Servlet API method, here we list only one level of entries (no recursive searching).</p>
	 *
	 * <p>Chapter 140.2.4 "Set<String> getResourcePaths(String)" of CMPN spec says:<blockquote>
	 *     Default Behavior - Assumes the resources are in the bundle registering the Whiteboard service.
	 *     Its Bundle.findEntries method is called to obtain the listing.
	 * </blockquote>
	 * Chapter 128.6.3 "Resource Lookup" says:<blockquote>
	 *     The getResourcePaths method must map to the Bundle getEntryPaths method, its return type is a Set and can
	 *     not handle multiples. However, the paths from the getEntryPaths method are relative while the methods of
	 *     the getResourcePaths must be absolute.
	 * </blockquote>
	 * That's a bit contradictive... {@link Bundle#findEntries} returns URLs (and checks fragments and lets us choose
	 * recurse flag), while {@link Bundle#getEntryPaths} returns names (and doesn't check fragments).... In Felix:<ul>
	 *     <li>{@code findEntries}: {@code new EntryFilterEnumeration(revision, always-fragments, path, filePattern, recurse, url-values)} and
	 *         (resolves a bundle if not resolved)</li>
	 *     <li>{@code getEntryPaths}: {@code new EntryFilterEnumeration(revision, no-fragments, path, "*", no-recurse, no-url-values)}</li>
	 * </ul></p>
	 *
	 * <p>The contradiction is resolved like this: {@link DefaultHttpContext} will implement this method based on
	 * the Whiteboard behavior because Http Service specification doesn't mention this method at all and
	 * Web Applications specification doesn't introduce any special support interface to be used <em>under</em>
	 * {@link ServletContext}.</p>
	 *
	 * @param bundle
	 * @param name
	 * @return
	 */
	protected Set<String> getResourcePaths(Bundle bundle, String name) {
		final String normalizedName = Path.normalizeResourcePath(name);
		LOG.trace("Searching bundle [" + bundle + "] for resource paths of [" + normalizedName + "]");

		if (normalizedName != null && bundle != null) {
			int state = bundle.getState();
			if (state == Bundle.INSTALLED || state == Bundle.STOPPING || state == Bundle.UNINSTALLED) {
				// because org.osgi.framework.Bundle.findEntries() for INSTALLED bundle may lead to resolution of
				// the bundle, I don't want to risk a deadlock when this method is called from any extender (different
				// thread)
				return null;
			}
			final Enumeration<URL> e = bundle.findEntries(normalizedName, null, false);
			if (e != null) {
				final Set<String> result = new LinkedHashSet<String>();
				while (e.hasMoreElements()) {
					String path = e.nextElement().getPath();
					result.add(path);
				}
				return result;
			}
		}
		return null;
	}

}
