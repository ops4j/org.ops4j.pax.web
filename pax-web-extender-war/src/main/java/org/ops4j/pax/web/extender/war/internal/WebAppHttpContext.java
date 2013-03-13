/*
 * Copyright 2007 Damian Golda.
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
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.model.WebAppMimeMapping;
import org.ops4j.pax.web.extender.war.internal.util.Path;
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
class WebAppHttpContext implements HttpContext {

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
	 * The http context to delegate to.
	 */
	private final HttpContext httpContext;
	/**
	 * Mime mappings.
	 */
	private final Map<String, String> mimeMappings;

	/**
	 * Creates a new http context that delegates to the specified http context
	 * but get's resources from the specified bundle.
	 * 
	 * @param httpContext
	 *            wrapped http context
	 * @param bundle
	 *            bundle to search for resorce
	 * @param webAppMimeMappings
	 *            an array of mime mappings
	 * 
	 * @throws NullArgumentException
	 *             if http context or bundle is null
	 */
	WebAppHttpContext(final HttpContext httpContext, final String rootPath,
			final Bundle bundle, final WebAppMimeMapping[] webAppMimeMappings) {
		NullArgumentException.validateNotNull(httpContext, "http context");
		NullArgumentException.validateNotNull(bundle, "Bundle");
		if (log.isDebugEnabled()) {
			log.debug("Creating WebAppHttpContext for {}", httpContext);
		}
		this.httpContext = httpContext;
		this.rootPath = rootPath;
		this.bundle = bundle;
		mimeMappings = new HashMap<String, String>();
		for (WebAppMimeMapping mimeMapping : webAppMimeMappings) {
			mimeMappings.put(mimeMapping.getExtension(),
					mimeMapping.getMimeType());
		}
	}

	/**
	 * Delegate to wrapped http context.
	 * 
	 * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
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
				+ (name.startsWith("/") ? "" : "/") + name);
		URL url = null;
		log.debug("Searching bundle " + bundle + " for resource [{}], normalized to [{}]", name, normalizedName);
		
		/*
		url = bundle.getEntry(name);
		if (url == null) {
			//try with normalized name
			url = bundle.getEntry(normalizedName);
		}
		if (url == null) {
			//try with getResource
			url = bundle.getResource(name);
		}
		
		if (url == null) {
			//try getResource with normalized name
			url = bundle.getResource(normalizedName);
		}
		*/

		//still no vail let's get on wiht it. 
		if (url == null && normalizedName != null && normalizedName.trim().length() > 0) {
			String path = "";
			log.debug("getResource Failed, fallback uses findEntries");
			String file = normalizedName;
			int idx = file.lastIndexOf('/');
			if (idx > 0) {
				path = normalizedName.substring(0, idx);
				file = normalizedName.substring(idx + 1);
			}
			@SuppressWarnings("rawtypes")
			Enumeration e = bundle.findEntries(path, file, false);
			if (e != null && e.hasMoreElements()) {
				url = (URL) e.nextElement();
			}
			
		}
		if (url != null) {
			log.debug("Resource found as url [{}]", url);
		} else {
			log.debug("Resource not found");
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
}
