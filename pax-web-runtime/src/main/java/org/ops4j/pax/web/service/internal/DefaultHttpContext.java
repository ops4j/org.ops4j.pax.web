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
package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.util.Path;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HttpContext} that uses the bundle to lookup
 * resources (as specified in 102.10.2 "public interface HttpContext").
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 */
public class DefaultHttpContext implements WebContainerContext {

	private final Logger LOG = LoggerFactory.getLogger(DefaultHttpContext.class);

	/** Bundle using the {@link HttpService}. */
	protected final Bundle bundle;

	protected final String contextId;

	public DefaultHttpContext(final Bundle bundle) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		this.bundle = bundle;
		this.contextId = DefaultContextIds.DEFAULT.getValue();
	}

	public DefaultHttpContext(final Bundle bundle, String contextId) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		this.bundle = bundle;
		this.contextId = (contextId == null || contextId.trim().equals("")) ? DefaultContextIds.DEFAULT.getValue() : contextId;
	}

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
	 * There is no security by default, so always return "true". {@inheritDoc}
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
	 * Allways returns null as there is no default way to find out the mime type. {@inheritDoc}
	 */
	@Override
	public String getMimeType(String name) {
		return null;
	}

	/**
	 * Search resource paths within the bundle jar as in
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResourcePaths}. {@inheritDoc}
	 */
	@Override
	public Set<String> getResourcePaths(final String name) {
		return getResourcePaths(bundle, name);
	}

	@Override
	public String getRealPath(String path) {
		return null;
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
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DefaultHttpContext that = (DefaultHttpContext) o;
		return bundle.equals(that.bundle) &&
				contextId.equals(that.contextId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bundle, contextId);
	}

	protected URL getResource(Bundle bundle, String name) {
		final String normalizedname = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource [" + normalizedname + "]");
		return bundle.getResource(normalizedname);
	}

	protected Set<String> getResourcePaths(Bundle bundle, String name) {
		final String normalizedName = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource paths of [" + normalizedName + "]");

		if ((normalizedName != null) && (bundle != null)) {
			final Enumeration<URL> e = bundle.findEntries(normalizedName, null, false);
			if (e != null) {
				final Set<String> result = new LinkedHashSet<String>();
				while (e.hasMoreElements()) {
					result.add(e.nextElement().getPath());
				}
				return result;
			}
		}
		return null;
	}

}
