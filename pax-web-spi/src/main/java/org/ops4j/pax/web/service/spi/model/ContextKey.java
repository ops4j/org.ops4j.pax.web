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
package org.ops4j.pax.web.service.spi.model;

import java.util.Objects;

import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.osgi.framework.Bundle;
import org.ops4j.pax.web.service.http.HttpContext;

public class ContextKey {

	public String contextId;
	public Bundle bundle;
	public HttpContext httpContext;

	private ContextKey(String contextId, Bundle bundle) {
		this.contextId = contextId;
		this.bundle = bundle;
	}

	public static ContextKey with(String contextId, Bundle bundle) {
		return new ContextKey(contextId, bundle);
	}

	public static ContextKey of(OsgiContextModel context) {
		return new ContextKey(context.getName(), context.isShared() ? null : context.getOwnerBundle());
	}

	public static ContextKey of(WebContainerContext context) {
		ContextKey key = with(context.getContextId(), context.getBundle());
		if (context instanceof WebContainerContextWrapper) {
			// the key should allow matching ALSO by identity hash code of the wrapped context
			WebContainerContextWrapper wrapper = (WebContainerContextWrapper) context;
			key.httpContext = wrapper.getHttpContext();
		}
		return key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ContextKey that = (ContextKey) o;
		boolean idBundleMatch = Objects.equals(contextId, that.contextId) &&
				Objects.equals(bundle, that.bundle);

		// Comparing httpContexts by identity allows us to have keys with different names which are logically equal.
		// Remember that when user passes arbitrary httpContext instance, it should NOT be named "default"
		return idBundleMatch || (httpContext != null && httpContext == ((ContextKey) o).httpContext);
	}

	@Override
	public int hashCode() {
		return Objects.hash(contextId, bundle);
	}

	@Override
	public String toString() {
		return "Key{" + contextId + ", " + bundle + "}";
	}

}
