/*
 * Copyright 2007 Alin Dreghiciu.
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * A simple wrapper to enclose custom {@link HttpContext http contexts} (which get registered directly to the
 * {@link HttpService}) to a {@link WebContainerContext}.
 */
class WebContainerContextWrapper extends DefaultHttpContext {

	private final HttpContext httpContext;

	WebContainerContextWrapper(final Bundle bundle, final HttpContext httpContext) {
		super(bundle, DefaultContextIds.CUSTOM.getValue());
		NullArgumentException.validateNotNull(httpContext, "HttpContext");
		this.httpContext = httpContext;
	}

	@Override
	public boolean isShared() {
		return false;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return httpContext.handleSecurity(request, response);
	}

	@Override
	public URL getResource(String name) {
		return httpContext.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return httpContext.getMimeType(name);
	}

	@Override
	public String toString() {
		return "WebContainerContextWrapper{bundle=" + bundle + ",contextId='" + contextId + "',delegate=" + httpContext + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result + ((contextId == null) ? 0 : contextId.hashCode());
		result = prime * result + ((httpContext == null) ? 0 : httpContext.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		WebContainerContextWrapper other = (WebContainerContextWrapper) obj;
		if (bundle == null) {
			if (other.bundle != null) {
				return false;
			}
		} else if (!bundle.equals(other.bundle)) {
			return false;
		}
		if (contextId == null) {
			if (other.contextId != null) {
				return false;
			}
		} else if (!contextId.equals(other.contextId)) {
			return false;
		}
		if (httpContext == null) {
			return other.httpContext == null;
		} else {
			return httpContext.equals(other.httpContext);
		}
	}

}
