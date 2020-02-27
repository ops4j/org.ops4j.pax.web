/*
 * Copyright 2020 OPS4J.
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

import java.net.URL;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;

/**
 * <p>{@link WebContainerContext} variant, where methods related to <em>behavior</em> (mime types, resource access,
 * security) are not used and the only aspect that matters is {@link WebContainerContext#getContextId()}</p>
 *
 * <p>Calling {@code register(..., context)} methods with such context requires existing
 * {@link org.osgi.service.http.HttpContext} with given ID. If the referenced context is not marked as
 * <em>shared</em>, the only bundle that can register web elements using such context reference is the same bundle
 * that originally created the referenced context.</p>
 */
public final class ReferencedWebContainerContext implements WebContainerContext {

	/** Bundle using the {@link HttpService}. */
	protected final Bundle bundle;

	private final String contextId;

	public ReferencedWebContainerContext(final Bundle bundle, String contextId) {
		this.bundle = bundle;
		this.contextId = contextId;
	}

	@Override
	public String getContextId() {
		return contextId;
	}

	@Override
	public boolean isShared() {
		// shared, because this context is only a reference
		return true;
	}

	@Override
	public boolean isReference() {
		return true;
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
		return false;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
	}

	@Override
	public URL getResource(String name) {
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return null;
	}

	@Override
	public String getRealPath(String path) {
		return null;
	}

	@Override
	public String getMimeType(String name) {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ReferencedWebContainerContext that = (ReferencedWebContainerContext) o;
		return Objects.equals(bundle, that.bundle) && contextId.equals(that.contextId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bundle, contextId);
	}

	@Override
	public String toString() {
		return "ReferencedWebContainerContext{bundle=" + bundle + ",contextId='" + contextId + "'}";
	}

}
