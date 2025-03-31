package org.ops4j.pax.web.service.jetty.internal.web;

import jakarta.servlet.ServletContext;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.ops4j.pax.web.service.jetty.internal.PaxWebServletContextHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/*
 * Copyright 2025 OPS4J.
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
public class PaxWebResource extends Resource {

	private final ServletContext servletContext;
	// used when the context has based resource using absolute file: URI
	private final PathResource baseUrlResource;
	// used in normal OSGi cases, where chroot is a base for WAB or the web context to prepend before resolved path
	private final String chroot;

	private final Resource realBaseResource;
	private boolean directFileMapping = false;

	public PaxWebResource(ServletContext servletContext, PathResource baseUrlResource, String chroot) {
		this.servletContext = servletContext;
		this.baseUrlResource = baseUrlResource;
		this.chroot = chroot;
		if (this.baseUrlResource != null) {
			realBaseResource = this.baseUrlResource;
		} else if (this.chroot != null) {
			realBaseResource = new EmptyResource() {
				@Override
				public Resource resolve(String subUriPath) {
					return PaxWebResource.this.resolve(subUriPath);
				}
			};
			try {
				URL url = this.servletContext.getResource(chroot);
				directFileMapping = url != null && !url.getPath().endsWith("/");
			} catch (MalformedURLException ignored) {
			}
		} else {
			throw new IllegalArgumentException("baseUrlResource and chroot can't both be null");
		}
	}

	@Override
	public Path getPath() {
		return realBaseResource.getPath();
	}

	@Override
	public boolean isDirectory() {
		return realBaseResource.isDirectory();
	}

	@Override
	public boolean isReadable() {
		return realBaseResource.isReadable();
	}

	@Override
	public URI getURI() {
		return realBaseResource.getURI();
	}

	@Override
	public String getName() {
		return realBaseResource.getName();
	}

	@Override
	public String getFileName() {
		return realBaseResource.getFileName();
	}

	@Override
	public Resource resolve(String pathInContext) {
		// our (commons-io) normalized path
		String childPath = org.ops4j.pax.web.service.spi.util.Path.securePath(pathInContext);
		if (childPath == null) {
			return null;
		}
		if (childPath.startsWith("/")) {
			childPath = childPath.substring(1);
		}

		try {
			if (baseUrlResource != null) {
				// Pax Web special - direct access to configured directory with proper metadata handling
				// (size, lastModified) for caching purposes
				if (childPath.isEmpty()) {
					// root directory access. Just return base resource and let super class handle welcome files
					return baseUrlResource;
				}
				return baseUrlResource.resolve(childPath);
			} else {
				// HttpService/Whiteboard behavior - resourceBase is prepended to argument for context resource
				// remember - under ServletContext there should be WebContainerContext that wraps
				// HttpContext or ServletContextHelper
				// before Pax Web 8 there was explicit delegation to HttpContext, but now, it's hidden
				// under Osgi(Scoped)ServletContext
				URL url = servletContext.getResource(directFileMapping ? chroot : chroot + "/" + childPath);

				// See: https://github.com/ops4j/org.ops4j.pax.web/issues/2014
				// Everything is fine with Felix - it doesn't even seem to support directory-based bundles.
				// However under Equinox and bundles available from directory (with META-INF/MANIFEST.MF
				// available) we have problems determining whether given URL is a directory (for the purpose of
				// welcome files).
				// In Equinox we have two schemes:
				//  - org.eclipse.osgi.storage.url.BundleResourceHandler.OSGI_RESOURCE_URL_PROTOCOL = "bundleresource"
				//  - org.eclipse.osgi.storage.url.BundleResourceHandler.OSGI_ENTRY_URL_PROTOCOL = "bundleentry"
				// let's ignore the resource one, as it is related to Bundle.getResource() (classloaders). So
				// we have to return a Jetty Resource that _exists_ and _is directory_ because
				// that's what org.eclipse.jetty.server.CachedContentFactory.load() checks...
				if (url != null && "bundleentry".equals(url.getProtocol())
						&& url.getPath() != null && url.getPath().endsWith("/")) {
					return new EquinoxBundleentryDirectoryURLResource(PaxWebServletContextHandler.toJettyResource(url));
				}

				return PaxWebServletContextHandler.toJettyResource(url);
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
