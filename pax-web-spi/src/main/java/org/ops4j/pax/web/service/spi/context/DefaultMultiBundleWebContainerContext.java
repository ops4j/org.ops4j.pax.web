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
package org.ops4j.pax.web.service.spi.context;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link MultiBundleWebContainerContext}. Its identity consists <strong>only</strong>
 * of the context's name.
 */
public final class DefaultMultiBundleWebContainerContext implements MultiBundleWebContainerContext {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultMultiBundleWebContainerContext.class);

	/** Delegate {@link org.ops4j.pax.web.service.WebContainerContext} for behavioral aspects */
	private final DefaultHttpContext delegate;

	private final Queue<Bundle> bundles = new ConcurrentLinkedQueue<>();

	public DefaultMultiBundleWebContainerContext(DefaultHttpContext delegate) {
		this.delegate = delegate;
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
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return delegate.handleSecurity(request, response);
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		delegate.finishSecurity(request, response);
	}

	@Override
	public URL getResource(final String name) {
		for (Bundle bundle : bundles) {
			URL pathUrl = delegate.getResource(bundle, name);
			if (pathUrl != null) {
				return pathUrl;
			}
		}
		return null;
	}

	@Override
	public String getMimeType(String name) {
		return delegate.getMimeType(name);
	}

	/**
	 * <p>Search resource paths within all the bundles sharing this context.</p>
	 *
	 * <p>{@inheritDoc}</p>
	 */
	@Override
	public Set<String> getResourcePaths(final String name) {
		for (Bundle bundle : bundles) {
			Set<String> paths = delegate.getResourcePaths(bundle, name);
			if (paths != null) {
				return paths;
			}
		}
		return null;
	}

	@Override
	public String getContextId() {
		return delegate.getContextId();
	}

	@Override
	public String toString() {
		return "DefaultMultiBundleHttpContext{contextId='" + delegate.getContextId() + "'}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultMultiBundleWebContainerContext that = (DefaultMultiBundleWebContainerContext) o;
		return getContextId().equals(that.getContextId());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getContextId());
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
