/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * Extends {@link WebAppHttpContext} by implementing {@link WebContainerContext}
 * .
 * 
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.5.1, March 30, 2009
 */
class WebAppWebContainerContext extends WebAppHttpContext implements
		WebContainerContext {

	/**
	 * Constructor matching super. {@inheritDoc}
	 */
	WebAppWebContainerContext(final HttpContext httpContext,
			final String rootPath, final Bundle bundle,
			final WebAppMimeMapping[] mimeMappings) {
		super(httpContext, rootPath, bundle, mimeMappings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getResourcePaths(final String name) {
		final String normalizedName = Path.normalizeResourcePath(rootPath
				+ (name.startsWith("/") ? "" : "/") + name);

		log.debug("Searching bundle [" + bundle + "] for resource paths of ["
				+ name + "], normalized to [" + normalizedName + "]");
		@SuppressWarnings("rawtypes")
		final Enumeration entryPaths = bundle.getEntryPaths(name);
		if (entryPaths == null || !entryPaths.hasMoreElements()) {
			log.debug("No resource paths found");
			return null;
		}
		Set<String> foundPaths = new HashSet<String>();
		while (entryPaths.hasMoreElements()) {
			foundPaths.add((String) entryPaths.nextElement());
		}
		log.debug("Resource paths found: " + foundPaths);
		return foundPaths;
	}

}
