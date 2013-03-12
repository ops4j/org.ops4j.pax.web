/* Copyright 2009 David Conde.
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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.spi.util.Path;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSharedWebContainerContext implements
		SharedWebContainerContext {
	private static final Logger LOG = LoggerFactory
			.getLogger(DefaultSharedWebContainerContext.class);

	private Queue<Bundle> bundles = new ConcurrentLinkedQueue<Bundle>();

	@Override
	public boolean registerBundle(Bundle bundle) {
		if (!bundles.contains(bundle)) {
			bundles.add(bundle);
			return true;
		}
		return false;
	}

	@Override
	public boolean deregisterBundle(Bundle bundle) {
		return bundles.remove(bundle);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		for (Bundle bundle : bundles) {
			Set<String> paths = getResourcePaths(bundle, path);
			if (paths != null) {
				return paths;
			}
		}
		return null;
	}

	@Override
	public String getMimeType(String arg0) {
		return null;
	}

	@Override
	public URL getResource(String path) {
		for (Bundle bundle : bundles) {
			URL pathUrl = getResource(bundle, path);
			if (pathUrl != null) {
				return pathUrl;
			}
		}
		return null;
	}

	private URL getResource(Bundle bundle, final String name) {
		final String normalizedname = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource ["
				+ normalizedname + "]");
		return bundle.getResource(normalizedname);
	}

	private Set<String> getResourcePaths(Bundle bundle, final String name) {
		final String normalizedname = Path.normalizeResourcePath(name);
		LOG.debug("Searching bundle [" + bundle + "] for resource paths of ["
				+ normalizedname + "]");
		final Enumeration<String> entryPaths = bundle
				.getEntryPaths(normalizedname);
		if (entryPaths == null || !entryPaths.hasMoreElements()) {
			return null;
		}
		Set<String> foundPaths = new HashSet<String>();
		while (entryPaths.hasMoreElements()) {
			foundPaths.add(entryPaths.nextElement());
		}
		return foundPaths;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest arg0,
			HttpServletResponse arg1) throws IOException {
		return true;
	}

}
