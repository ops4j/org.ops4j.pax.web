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
package org.ops4j.pax.web.service.spi.context;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * A simple wrapper to enclose custom {@link HttpContext http contexts} (which gets registered directly to the
 * {@link org.osgi.service.http.HttpService}) or {@link ServletContextHelper} (registered via Whiteboard service)
 * in an implementation of {@link org.ops4j.pax.web.service.WebContainerContext}.
 */
public class WebContainerContextWrapper extends DefaultHttpContext {

	// 102. Http Service specification
	private final HttpContext httpContext;
	// 140. Whiteboard Service specification
	private final ServletContextHelper servletContextHelper;

	private final boolean shared;

	public WebContainerContextWrapper(final Bundle bundle, final HttpContext httpContext) {
		this(bundle, httpContext, uniqueId(httpContext));
	}

	public WebContainerContextWrapper(final Bundle bundle, final ServletContextHelper helper) {
		this(bundle, helper, uniqueId(helper));
	}

	public WebContainerContextWrapper(final Bundle bundle, final HttpContext httpContext, final String name) {
		super(bundle, name == null ? uniqueId(httpContext) : name);
		this.httpContext = httpContext;
		this.servletContextHelper = null;
		this.shared = httpContext instanceof WebContainerContext && ((WebContainerContext) httpContext).isShared();
	}

	public WebContainerContextWrapper(final Bundle bundle, final ServletContextHelper helper, final String name) {
		this(bundle, helper, name, true);
	}

	public WebContainerContextWrapper(final Bundle bundle, final ServletContextHelper helper, final String name,
			boolean shared) {
		super(bundle, name == null ? uniqueId(helper) : name);
		this.httpContext = null;
		this.servletContextHelper = helper;
		this.shared = shared;
	}

	public static String uniqueId(Object context) {
		return String.format("context:%d", System.identityHashCode(context));
	}

	@Override
	public boolean isShared() {
		return shared;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (servletContextHelper != null) {
			return servletContextHelper.handleSecurity(request, response);
		}
		if (httpContext != null) {
			return httpContext.handleSecurity(request, response);
		}

		// just return true, because user may have registered a HttpContextMapping or ServletContextHelperMapping
		// without actual HttpContext or ServletContextHelper - maybe only to provide an alternative
		// context path
		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		if (servletContextHelper != null) {
			servletContextHelper.finishSecurity(request, response);
		}
		// no-op in HttpContext case
	}

	@Override
	public URL getResource(String name) {
		if (servletContextHelper != null) {
			return servletContextHelper.getResource(name);
		}
		return httpContext.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		if (servletContextHelper != null) {
			return servletContextHelper.getMimeType(name);
		}
		return httpContext.getMimeType(name);
	}

	@Override
	public Set<String> getResourcePaths(String name) {
		if (servletContextHelper != null) {
			return servletContextHelper.getResourcePaths(name);
		}
		return super.getResourcePaths(name);
	}

	@Override
	public String getRealPath(String path) {
		if (servletContextHelper != null) {
			return servletContextHelper.getRealPath(path);
		}
		return super.getRealPath(path);
	}

	public HttpContext getHttpContext() {
		return httpContext;
	}

	public ServletContextHelper getServletContextHelper() {
		return servletContextHelper;
	}

	@Override
	public String toString() {
		return "WebContainerContextWrapper{"
				+ (bundle == null ? "shared=true" : "bundle=" + bundle)
				+ ",contextId='" + contextId
				+ "',delegate=" + (servletContextHelper == null ? httpContext : servletContextHelper)
				+ "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bundle == null) ? 0 : bundle.hashCode());
		result = prime * result + ((contextId == null) ? 0 : contextId.hashCode());
		result = prime * result + ((httpContext == null) ? 0 : httpContext.hashCode());
		result = prime * result + ((servletContextHelper == null) ? 0 : servletContextHelper.hashCode());
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

		// only one of these can be set
		if (httpContext == null) {
			if (servletContextHelper == null) {
				return other.servletContextHelper == null;
			} else {
				return servletContextHelper.equals(other.servletContextHelper);
			}
		} else {
			return httpContext.equals(other.httpContext);
		}
	}

}
