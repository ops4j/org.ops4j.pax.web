/* Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.web.service.internal;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
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
 * resources.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 */
class DefaultHttpContext implements WebContainerContext {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(DefaultHttpContext.class);

	/**
	 * Bundle using the {@link HttpService}.
	 */
	private final Bundle bundle;

	private String contextID;

	/**
	 * Constructor.
	 *
	 * @param bundle that bundle using the {@link HttpService}l cannot be null
	 * @throws IllegalArgumentException - If bundle is null
	 */
	DefaultHttpContext(final Bundle bundle, String contextID) {
		NullArgumentException.validateNotNull(bundle, "Bundle");
		this.bundle = bundle;
		this.contextID = contextID == null ? "default" : contextID;
	}

	/**
	 * There is no security by default, so always return "true". {@inheritDoc}
	 */
	@Override
	public boolean handleSecurity(final HttpServletRequest httpServletRequest,
								  final HttpServletResponse httpServletResponse) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public URL getResource(final String name) {
		final String normalizedname = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource ["
				+ normalizedname + "]");
		return bundle.getResource(normalizedname);
	}

	/**
	 * Allways returns null as there is no default way to find out the mime
	 * type. {@inheritDoc}
	 */
	@Override
	public String getMimeType(String name) {
		return null;
	}

	/**
	 * Search resource paths within the bundle jar. {@inheritDoc}
	 */
	@Override
	public Set<String> getResourcePaths(final String name) {
		final String normalizedname = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource paths of ["
				+ normalizedname + "]");
		final Enumeration<String> entryPaths = bundle
				.getEntryPaths(normalizedname);
		if (entryPaths == null || !entryPaths.hasMoreElements()) {
			return null;
		}
		Set<String> foundPaths = new HashSet<>();
		while (entryPaths.hasMoreElements()) {
			foundPaths.add(entryPaths.nextElement());
		}
		return foundPaths;
	}

	@Override
	public String getContextId() {
		return contextID;
	}

	@Override
	public String toString() {
		return "DefaultHttpContext [bundle=" + bundle + ", contextID="
				+ contextID + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result
				+ ((contextID == null) ? 0 : contextID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		// CHECKSTYLE:OFF
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DefaultHttpContext other = (DefaultHttpContext) obj;
		if (bundle == null) {
			if (other.bundle != null) {
				return false;
			}
		} else if (!bundle.equals(other.bundle)) {
			return false;
		}
		if (contextID == null) {
			if (other.contextID != null) {
				return false;
			}
		} else if (!contextID.equals(other.contextID)) {
			return false;
		}
		return true;
		// CHECKSTYLE:ON
	}

}
