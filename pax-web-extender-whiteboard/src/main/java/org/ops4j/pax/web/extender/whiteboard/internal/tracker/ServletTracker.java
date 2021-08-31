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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.GenericServlet;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.util.ServletAnnotationScanner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks OSGi services that should result in registration of a {@link Servlet}. This tracker
 * is <em>canonical</em> because it tracks services of a class specified in Whiteboard Service spec.
 *
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class ServletTracker extends AbstractElementTracker<Servlet, Servlet, ServletEventData, ServletModel> {

	private ServletTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<Servlet, ServletModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {
		return new ServletTracker(whiteboardExtenderContext, bundleContext)
				.create(Servlet.class, GenericServlet.class, HttpServlet.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected ServletModel createElementModel(ServiceReference<Servlet> serviceReference, Integer rank, Long serviceId) {
		log.debug("Creating servlet model from R7 whiteboard service {} (id={})", serviceReference, serviceId);

		// 1. legacy "alias" (for HttpService.registerServlet(alias, ...)
		String alias = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS);
		if (alias != null) {
			log.warn("Legacy {} property specified, alias should be used only for HttpService registrations",
					PaxWebConstants.SERVICE_PROPERTY_SERVLET_ALIAS);
		}

		// 2. URL patterns
		String[] urlPatterns = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
				Utils::asStringArray);

		// 3. servlet name
		String name = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.INIT_PARAM_SERVLET_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME,
				Utils::asString);

		// 4. init params
		Map<String, String> initParams = new LinkedHashMap<>();
		String legacyInitPrefix = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_INIT_PREFIX);
		if (legacyInitPrefix != null) {
			log.warn("Legacy {} property found, servlet init parameters should be prefixed with {} instead",
					PaxWebConstants.SERVICE_PROPERTY_INIT_PREFIX,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX);
		}
		// are there any service properties prefixed with legacy "init." prefix (or one specified by "init-prefix"?
		final String[] prefix = new String[] { legacyInitPrefix == null ? PaxWebConstants.DEFAULT_INIT_PREFIX_PROP : legacyInitPrefix };
		boolean hasLegacyInitProperty = Arrays.stream(serviceReference.getPropertyKeys())
				.anyMatch(p -> p.startsWith(prefix[0]));
		if (hasLegacyInitProperty) {
			log.warn("Legacy servlet init parameters found (with prefix: {}), init parameters should be prefixed with"
					+ " {} instead", prefix[0], HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX);
		} else {
			prefix[0] = HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_INIT_PARAM_PREFIX;
		}
		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(prefix[0])) {
				String value = Utils.getStringProperty(serviceReference, key);
				if (value != null) {
					initParams.put(key.substring(prefix[0].length()), value);
				}
			}
		}

		// 5. async-supported
		Boolean asyncSupported = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_ASYNC_SUPPORTED, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED,
				Utils::asBoolean);

		// 6. load-on-startup
		Integer loadOnStartup = Utils.getIntegerProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_LOAD_ON_STARTUP);

		// 7. multipart configuration - not supported in Pax Web legacy
		MultipartConfigElement multiPartConfig = null;
		Boolean multiPartEnabled = Utils.getBooleanProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_ENABLED);
		if (multiPartEnabled != null && multiPartEnabled) {
			// if location == null, it'll be resolved later in WebContainer, when org.ops4j.pax.web PID is available
			String location = Utils.getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_LOCATION);
			Long maxFileSize = Utils.getLongProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXFILESIZE);
			Long maxRequestSize = Utils.getLongProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_MAXREQUESTSIZE);
			Integer fileSizeThreshold = Utils.getIntegerProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_MULTIPART_FILESIZETHRESHOLD);

			multiPartConfig = new MultipartConfigElement(null,
					maxFileSize == null ? -1L : maxFileSize,
					maxRequestSize == null ? -1L : maxRequestSize,
					fileSizeThreshold == null ? 0 : fileSizeThreshold);
		}

		// 8. error pages
		String[] errorDeclarations = Utils.getPaxWebProperty(serviceReference,
				null, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE,
				Utils::asStringArray);

		// 9. Check the servlet annotations - we need a class of actual servlet
		Servlet service = null;
		try {
			// if the service is prototype-scoped, new instance will be created, but never used (e.g., never
			// get its init() called)
			service = serviceReference.getBundle().getBundleContext().getService(serviceReference);
			if (service != null) {
				ServletAnnotationScanner scanner = new ServletAnnotationScanner(service.getClass());
				if (scanner.scanned) {
					// 2. URL patterns
					if (scanner.urlPatterns != null && scanner.urlPatterns.length > 0) {
						if (urlPatterns != null && urlPatterns.length > 0) {
							log.warn("Servlet URL patterns specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", Arrays.asList(urlPatterns), Arrays.asList(scanner.urlPatterns),
									Arrays.asList(urlPatterns));
						} else {
							urlPatterns = scanner.urlPatterns;
						}
					}
					// 3. servlet name
					if (scanner.servletName != null) {
						if (name != null) {
							log.warn("Servlet name specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", name, scanner.servletName, name);
						} else {
							name = scanner.servletName;
						}
					}
					// 4. init params
					if (scanner.webInitParams != null) {
						if (!initParams.isEmpty()) {
							log.warn("Servlet init parameters specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", initParams, scanner.webInitParams, initParams);
						} else {
							initParams.putAll(scanner.webInitParams);
						}
					}
					// 5. async-supported
					if (scanner.asyncSupported != null) {
						if (asyncSupported != null && asyncSupported != scanner.asyncSupported) {
							log.warn("Servlet async flag specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", asyncSupported, scanner.asyncSupported, asyncSupported);
						} else {
							asyncSupported = scanner.asyncSupported;
						}
					}
					// 6. load-on-startup
					if (scanner.loadOnStartup != null) {
						if (loadOnStartup != null) {
							log.warn("Load-on-startup value specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", loadOnStartup, scanner.loadOnStartup, loadOnStartup);
						} else {
							loadOnStartup = scanner.loadOnStartup;
						}
					}
					// 7. multipart configuration - not supported in Pax Web legacy
					if (scanner.multiPartConfigAnnotation != null) {
						if (multiPartConfig != null) {
							log.warn("Multipart configuration specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", multiPartConfig, scanner.multiPartConfigAnnotation, multiPartConfig);
						} else {
							multiPartConfig = new MultipartConfigElement(scanner.multiPartConfigAnnotation);
						}
					}
				}
			}
		} finally {
			if (service != null) {
				serviceReference.getBundle().getBundleContext().ungetService(serviceReference);
			}
		}

		// pass everything to a handy builder
		ServletModel.Builder builder = new ServletModel.Builder()
				.withServiceRankAndId(rank, serviceId)
				.withServletReference(serviceReference.getBundle(), serviceReference)
				.withAlias(alias)
				.withUrlPatterns(urlPatterns)
				.withServletName(name)
				.withInitParams(initParams)
				.withAsyncSupported(asyncSupported)
				.withLoadOnStartup(loadOnStartup)
				.withMultipartConfigElement(multiPartConfig)
				.withErrorDeclarations(errorDeclarations);

		return builder.build();
	}

}
