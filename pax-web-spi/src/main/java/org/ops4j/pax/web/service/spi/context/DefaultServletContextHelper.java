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

import org.osgi.framework.Bundle;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * Default {@link ServletContextHelper} as specified in "140.2 The Servlet Context". Actually there's no need
 * to implement anything, because all the methods in {@link ServletContextHelper} are non-abstract, but the
 * methods are here for documentation purposes (references to specification chapters).
 */
public class DefaultServletContextHelper extends ServletContextHelper {

	protected final Bundle bundle;

	public DefaultServletContextHelper(Bundle runtimeBundle) {
		super(runtimeBundle);
		this.bundle = runtimeBundle;
	}

	@Override
	public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 140.2.5 Security Handling
		return true;
	}

	@Override
	public void finishSecurity(HttpServletRequest request, HttpServletResponse response) {
		// 140.2.5 Security Handling
	}

	@Override
	public URL getResource(String name) {
		// 140.2.3 URL getResource(String)
		// But there's one problem. Default implementation of ServletContextHelper (from osgi.cmpn)
		// trims the leading "/" - even if the name equals to "/". CMPN Whiteboard specification
		// doesn't care about "welcome files", but we do, so "/" can't be replaced with "", because it
		// actually MAKES a difference when calling Bundle.getEntry()...
		if ("/".equals(name) && bundle != null) {
			return bundle.getEntry("/");
		}
		return super.getResource(name);
	}

	@Override
	public String getMimeType(String name) {
		// 140.2.1 String getMimeType(String)
		return null;
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		// 140.2.4 Set<String> getResourcePaths(String)
		return super.getResourcePaths(path);
	}

	@Override
	public String getRealPath(String path) {
		// 140.2.2 String getRealPath(String)
		return null;
	}

}
