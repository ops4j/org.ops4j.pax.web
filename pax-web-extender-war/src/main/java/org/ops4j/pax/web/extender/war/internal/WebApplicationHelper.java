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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.ops4j.pax.web.service.spi.context.DefaultServletContextHelper;
import org.ops4j.pax.web.service.spi.util.Path;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class adjusts {@link org.osgi.service.http.context.ServletContextHelper} from the Whiteboard specification
 * (chapter 140) to OSGi CMPN Web Applications specification (chapter 128). The most important thing is that
 * {@link org.osgi.service.http.context.ServletContextHelper#getResource} uses {@link Bundle#getEntry} method
 * which doesn't consider fragments, while chapter "128.3.5 Static Content" says:<blockquote>
 *     For a WAB, these resources must be found according to the findEntries method, this method includes fragments.
 * </blockquote></p>
 */
public class WebApplicationHelper extends DefaultServletContextHelper {

	public static final Logger LOG = LoggerFactory.getLogger(WebApplicationHelper.class);

	/** Additional roots of the {@code META-INF/resources} locations within WAB's reachable bundles/embedded JARs */
	private final Map<Bundle, URL> metainfResourceRoots;

	public WebApplicationHelper(Bundle runtimeBundle, Map<Bundle, URL> metainfResourceRoots) {
		super(runtimeBundle);
		this.metainfResourceRoots = metainfResourceRoots;
	}

	// TODO: even if resources may be cached at Jetty/Tomcat/UndertowResourceServlet, we should cache URLs here

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
			String root = null;
			String path = null;
			if (!normalizedPath.contains("/")) {
				root = "/";
				path = normalizedPath;
			} else {
				int lastSlash = normalizedPath.lastIndexOf('/');
				if (lastSlash == normalizedPath.length() - 1) {
					// case when asking for e.g., "static/" - we should rather look for "static" in "" and not ""
					// in "static"
					normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
					if (!normalizedPath.contains("/")) {
						root = "/";
						path = normalizedPath;
					} else {
						lastSlash = normalizedPath.lastIndexOf('/');
						root = normalizedPath.substring(0, lastSlash);
						path = normalizedPath.substring(lastSlash + 1);
					}
				} else {
					root = normalizedPath.substring(0, lastSlash);
					path = normalizedPath.substring(lastSlash + 1);
				}
			}

			e = bundle.findEntries(root, path, false);
			if (e != null) {
				return e.nextElement();
			}

			// in Pax Web 7 the WebAppHttpContext for WABs falled back to org.osgi.framework.Bundle.getResource() call
			// which is the default implementation for HttpService scenario, here we are explicitly NOT doing this,
			// because we don't want to return resources fom the Bundle-ClassPath.
			// It's still possible if user knows the Bundle-ClassPath, but still, roots mentioned in
			// "128.3.5 Static Content" are always forbidden at very early stage of request processing.
			// So only if user puts classes to root of the WAB or to some other directory different than WEB-INF,
			// META-INF, OSGI-INF or OSGI-OPT we are returning them when accessing using known root.

			try {
				for (Map.Entry<Bundle, URL> entry : metainfResourceRoots.entrySet()) {
					List<URL> entries = ClassPathUtil.findEntries(entry.getKey(), new URL[] { entry.getValue() },
							root, path, false);
					if (entries.size() > 0) {
						return entries.get(0);
					}
				}
			} catch (IOException ex) {
				LOG.warn("Error accessing {}/{} from META-INF/resources: {}", root, path, ex.getMessage(), ex);
			}
		}

		return null;
	}

}
