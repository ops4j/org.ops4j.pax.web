/*
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of HttpContext, which gets resources from the bundle
 * that registered the service. It delegates to the provided http context beside
 * for getResource that should look in the original bundle.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, December 27, 2007
 */
class WebAppHttpContext extends WebContainerContextWrapper {

    static final URL NO_URL;

    /**
     * The http context to delegate to.
     */
    protected final HttpContext httpContext;

    /**
	 * Logger.
	 */
	final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The bundle that registered the service.
	 */
	final Bundle bundle;
	/**
	 * The root path of the web app inside the bundle.
	 */
	final String rootPath;
	/**
	 * Mime mappings.
	 */
	private final Map<String, String> mimeMappings;

	private final ConcurrentMap<String, URL> resourceCache = new ConcurrentHashMap<>();

	static {
		try {
			NO_URL = new URL("http:");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a new http context that delegates to the specified http context
	 * but get's resources from the specified bundle.
	 *
	 * @param httpContext        wrapped http context
	 * @param bundle             bundle to search for resorce
	 * @param webAppMimeMappings an array of mime mappings
	 * @throws NullArgumentException if http context or bundle is null
	 */
	WebAppHttpContext(final HttpContext httpContext, final String rootPath,
					  final Bundle bundle, final WebAppMimeMapping[] webAppMimeMappings) {
        super(bundle, httpContext);
		if (log.isDebugEnabled()) {
			log.debug("Creating WebAppHttpContext for {}", httpContext);
		}
		this.httpContext = httpContext;
		this.rootPath = rootPath;
		this.bundle = bundle;
		mimeMappings = new HashMap<>();
		for (WebAppMimeMapping mimeMapping : webAppMimeMappings) {
			mimeMappings.put(mimeMapping.getExtension(),
					mimeMapping.getMimeType());
		}
	}

	/**
	 * Delegate to wrapped http context.
	 *
	 * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	public boolean handleSecurity(final HttpServletRequest request,
								  final HttpServletResponse response) throws IOException {
		return httpContext.handleSecurity(request, response);
	}

	/**
	 * Searches for the resource in the bundle that published the service.
	 *
	 * @see org.osgi.service.http.HttpContext#getResource(String)
	 */
	public URL getResource(final String name) {
		final String normalizedName = Path.normalizeResourcePath(rootPath
				+ (name.startsWith("/") ? "" : "/") + name).trim();

		log.debug("Searching bundle " + bundle
						+ " for resource [{}], normalized to [{}]", name,
				normalizedName);

		URL url = resourceCache.get(normalizedName);

		if (url == null && !normalizedName.isEmpty()) {
			url = bundle.getEntry(normalizedName);
			if (url == null) {
				log.debug("getEntry failed, trying with /META-INF/resources/ in bundle class space");
				// Search attached bundles for web-fragments
				Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(bundle, new HashSet<>());
				for (Bundle bundleInClassSpace : bundlesInClassSpace) {
					url = bundleInClassSpace.getEntry("/META-INF/resources/" + normalizedName);
					if (url != null) {
						break;
					}
				}
			}
			// obviously still not found might be available from a attached bundle resource
			if (url == null) {
				log.debug("getEntry failed, fallback to getResource");
				url = bundle.getResource(normalizedName);
			}
			if (url == null) {
				url = NO_URL;
			}
			resourceCache.putIfAbsent(normalizedName, url);
		}

		if (url != null && url != NO_URL) {
			log.debug("Resource found as url [{}]", url);
		} else {
			log.debug("Resource not found");
			url = null;
		}
		return url;
	}

	/**
	 * Find the mime type in the mime mappings. If not found delegate to wrapped
	 * http context.
	 *
	 * @see org.osgi.service.http.HttpContext#getMimeType(String)
	 */
	public String getMimeType(final String name) {
		String mimeType = null;
		if (name != null && name.length() > 0 && name.contains(".")) {
			final String[] segments = name.split("\\.");
			mimeType = mimeMappings.get(segments[segments.length - 1]);
		}
		if (mimeType == null) {
			mimeType = httpContext.getMimeType(name);
		}
		return mimeType;
	}

	@Override
	public String toString() {
		return "WebAppHttpContext{" + bundle.getSymbolicName() + " - "
				+ bundle.getBundleId() + '}';
	}
}
