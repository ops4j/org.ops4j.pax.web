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
package org.ops4j.pax.web.service.spi.context;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.framework.Bundle;

public class UniqueWebContainerContextWrapper implements WebContainerContext {

	private final WebContainerContext delegate;

	public UniqueWebContainerContextWrapper(WebContainerContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		return delegate.getResourcePaths(path);
	}

	@Override
	public String getRealPath(String path) {
		return delegate.getRealPath(path);
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		delegate.finishSecurity(request, response);
	}

	@Override
	public String getContextId() {
		return delegate.getContextId();
	}

	@Override
	public boolean isShared() {
		return delegate.isShared();
	}

	@Override
	public Bundle getBundle() {
		return delegate.getBundle();
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return delegate.handleSecurity(request, response);
	}

	@Override
	public URL getResource(String name) {
		return delegate.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		return delegate.getMimeType(name);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public WebContainerContext getDelegate() {
		return delegate;
	}

}
