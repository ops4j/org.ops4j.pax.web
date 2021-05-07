/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.extender.war.internal;

import java.net.URL;
import java.util.Enumeration;

import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.util.Path;
import org.osgi.framework.Bundle;

/**
 * <p>This class adjusts {@link org.osgi.service.http.context.ServletContextHelper} from the Whiteboard specification
 * (chapter 140) to OSGi CMPN Web Applications specification (chapter 128). The most important thing is that
 * {@link org.osgi.service.http.context.ServletContextHelper#getResource} uses {@link Bundle#getEntry} method
 * which doesn't consider fragments, while chapter "128.3.5 Static Content" says:<blockquote>
 *     For a WAB, these resources must be found according to the findEntries method, this method includes fragments.
 * </blockquote></p>
 */
public class WebApplicationHelper extends DefaultServletContextHelper {

	public WebApplicationHelper(Bundle runtimeBundle) {
		super(runtimeBundle);
	}

	@Override
	public URL getResource(String name) {
		if ("/".equals(name)) {
			return super.getResource(name);
		} else {
			Enumeration<URL> e = null;
			String normalizedPath = Path.normalizeResourcePath(name);
			// 128.6.3 Resource Lookup: Since the getResource and getResourceAsStream methods do not support wildcards
			// while the findEntries method does it is necessary to escape the wildcard asterisk ('*' \u002A) with
			// prefixing it with a reverse solidus ('\' \u005C). This implies that a reverse solidus must be escaped
			// with an extra reverse solidus. For example, the path foo\bar* must be escaped to foo\\bar\*.
			normalizedPath = normalizedPath.replace("\\", "\\\\").replace("*", "\\*");
			if (!normalizedPath.contains("/")) {
				e = bundle.findEntries("/", normalizedPath, false);
			} else {
				int lastSlash = normalizedPath.lastIndexOf('/');
				e = bundle.findEntries(normalizedPath.substring(0, lastSlash), normalizedPath.substring(lastSlash + 1), false);
			}
			if (e != null) {
				return e.nextElement();
			}
		}

		return null;
	}

	// TODO: this helper should contain the bundles from OsgiServletContextClassLoader that should be checked
	//       for resources (also in META-INF/resources)

}
