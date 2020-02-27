/*
 * Copyright 2009 David Conde.
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

import java.net.URL;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link MultiBundleWebContainerContext}.
 */
public class DefaultMultiBundleWebContainerContext extends DefaultHttpContext implements MultiBundleWebContainerContext {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultMultiBundleWebContainerContext.class);

	private final Queue<Bundle> bundles = new ConcurrentLinkedQueue<>();

	public DefaultMultiBundleWebContainerContext() {
		super(DefaultContextIds.SHARED.getValue());
	}

	public DefaultMultiBundleWebContainerContext(String contextId) {
		super((contextId == null || contextId.trim().equals("")) ? DefaultContextIds.SHARED.getValue() : contextId);
	}

	@Override
	public boolean isShared() {
		return true;
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	public URL getResource(final String name) {
		for (Bundle bundle : bundles) {
			URL pathUrl = getResource(bundle, name);
			if (pathUrl != null) {
				return pathUrl;
			}
		}
		return null;
	}

	/**
	 * Search resource paths within the bundle jar as in
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResourcePaths}. {@inheritDoc}
	 */
	@Override
	public Set<String> getResourcePaths(final String name) {
		for (Bundle bundle : bundles) {
			Set<String> paths = getResourcePaths(bundle, name);
			if (paths != null) {
				return paths;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "DefaultMultiBundleHttpContext{contextId='" + contextId + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DefaultMultiBundleWebContainerContext that = (DefaultMultiBundleWebContainerContext) o;
		return contextId.equals(that.contextId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contextId);
	}

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

}
