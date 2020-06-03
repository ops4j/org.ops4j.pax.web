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
import javax.servlet.Filter;
import javax.servlet.GenericFilter;
import javax.servlet.http.HttpFilter;

import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.utils.FilterAnnotationScanner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks OSGi services that should result in registration of a {@link Filter}.
 *
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @author Grzegorz Grzybek
 * @since 0.4.0, April 05, 2008
 */
public class FilterTracker extends AbstractElementTracker<Filter, Filter, FilterModel> {

	private FilterTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<Filter, FilterModel> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new FilterTracker(extenderContext, bundleContext)
				.create(Filter.class, GenericFilter.class, HttpFilter.class);
	}

	@Override
	@SuppressWarnings("deprecation")
	protected FilterModel createElementModel(ServiceReference<Filter> serviceReference, Integer rank, Long serviceId) {
		LOG.debug("Creating fiiter model from R7 whiteboard service {} (id={})", serviceReference, serviceId);

		// 1. filter name
		String name = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.INIT_PARAM_FILTER_NAME, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME,
				Utils::asString);

		// 2a. URL patterns
		String[] urlPatterns = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN,
				Utils::asStringArray);

		// 2b. Regex patterns
		Object propertyValue = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX);
		String[] regexUrlPatterns = Utils.asStringArray(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, propertyValue);

		// 2c. Servlet names
		String[] servletNames = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_SERVLET_NAMES, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET,
				Utils::asStringArray);

		// 3. init params
		Map<String, String> initParams = new LinkedHashMap<>();
		String legacyInitPrefix = Utils.getStringProperty(serviceReference, PaxWebConstants.SERVICE_PROPERTY_INIT_PREFIX);
		if (legacyInitPrefix != null) {
			LOG.warn("Legacy {} property found, filter init parameters should be prefixed with {} instead",
					PaxWebConstants.SERVICE_PROPERTY_INIT_PREFIX,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX);
		}
		// are there any service properties prefixed with legacy "init." prefix (or one specified by "init-prefix"?
		final String[] prefix = new String[] { legacyInitPrefix == null ? PaxWebConstants.DEFAULT_INIT_PREFIX_PROP : legacyInitPrefix };
		boolean hasLegacyInitProperty = Arrays.stream(serviceReference.getPropertyKeys())
				.anyMatch(p -> p.startsWith(prefix[0]));
		if (hasLegacyInitProperty) {
			LOG.warn("Legacy filter init parameters found (with prefix: {}), init parameters should be prefixed with"
					+ " {} instead", prefix[0], HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX);
		} else {
			prefix[0] = HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX;
		}
		for (String key : serviceReference.getPropertyKeys()) {
			if (key.startsWith(prefix[0])) {
				String value = Utils.getStringProperty(serviceReference, key);
				if (value != null) {
					initParams.put(key.substring(prefix[0].length()), value);
				}
			}
		}
		if (initParams.isEmpty()) {
			// TODO: this message should probably be removed at some point
			LOG.info("Whiteboard filter has no init parameters specified. In Pax Web 8, service registration "
					+ "properties are no longer copied as init parameters.");
		}

		// 4. async-supported
		Boolean asyncSupported = Utils.getPaxWebProperty(serviceReference,
				PaxWebConstants.SERVICE_PROPERTY_ASYNC_SUPPORTED, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED,
				Utils::asBoolean);

		// 5. dispatcher types
		propertyValue = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
		String[] dispatcherTypeNames = Utils.asStringArray(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER, propertyValue);

			//		initParams.put(PaxWebConstants.FILTER_MAPPING_DISPATCHER, dispatcherInitString);

		// 6. Check the filter annotations - we need a class of actual filter
		Filter service = null;
		try {
			service = bundleContext.getService(serviceReference);
			if (service != null) {
				FilterAnnotationScanner scanner = new FilterAnnotationScanner(service.getClass());
				if (scanner.scanned) {
					// 1. filter name
					if (scanner.filterName != null) {
						if (name != null) {
							LOG.warn("Filter name specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", name, scanner.filterName, name);
						} else {
							name = scanner.filterName;
						}
					}
					// 2a. URL patterns
					if (scanner.urlPatterns != null && scanner.urlPatterns.length > 0) {
						if (urlPatterns != null && urlPatterns.length > 0) {
							LOG.warn("Filter URL patterns specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", Arrays.asList(urlPatterns), Arrays.asList(scanner.urlPatterns),
									Arrays.asList(urlPatterns));
						} else {
							urlPatterns = scanner.urlPatterns;
						}
					}
					// 2c. Servlet names
					if (scanner.servletNames != null && scanner.servletNames.length > 0) {
						if (servletNames != null && servletNames.length > 0) {
							LOG.warn("Filter servlet names specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", Arrays.asList(servletNames), Arrays.asList(scanner.servletNames),
									Arrays.asList(servletNames));
						} else {
							servletNames = scanner.servletNames;
						}
					}
					// 3. init params
					if (scanner.webInitParams != null) {
						if (!initParams.isEmpty()) {
							LOG.warn("Filter init parameters specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", initParams, scanner.webInitParams, initParams);
						} else {
							initParams.putAll(scanner.webInitParams);
						}
					}
					// 5. async-supported
					if (scanner.asyncSupported != null) {
						if (asyncSupported != null && asyncSupported != scanner.asyncSupported) {
							LOG.warn("Filter async flag specified using both service property ({}) and annotation ({})."
									+ " Choosing {}.", asyncSupported, scanner.asyncSupported, asyncSupported);
						} else {
							asyncSupported = scanner.asyncSupported;
						}
					}
				}
			}
		} finally {
			if (service != null) {
				bundleContext.ungetService(serviceReference);
			}
		}

		// pass everything to a handy builder
		FilterModel.Builder builder = new FilterModel.Builder()
				.withServiceRankAndId(rank, serviceId)
				.withFilterReference(serviceReference.getBundle(), serviceReference)
				.withUrlPatterns(urlPatterns)
				.withServletNames(servletNames)
				.withRegexMapping(regexUrlPatterns)
				.withFilterName(name)
				.withInitParams(initParams)
				.withAsyncSupported(asyncSupported)
				.withDispatcherTypes(dispatcherTypeNames);

		return builder.build();
	}

	// from removed FilterWebElement

//	@Override
//	public void register(final WebContainer webContainer, final HttpContext httpContext) throws Exception {
//		//TODO: DispatcherTypes EnumSet !!
//		//--> this might be done by adding those to the initParams as it's interpreted by the whiteboard-extender
////		webContainer.registerFilter(
////					filterMapping.getFilter()/*.getClass()*/,
////					filterMapping.getUrlPatterns(),
////					filterMapping.getServletNames(),
////					DictionaryUtils.adapt(filterMapping.getInitParams()),
////					filterMapping.getAsyncSupported(),
////					httpContext);
//	}
//
//
//	@Override
//	public void unregister(final WebContainer webContainer, final HttpContext httpContext) {
//		Filter filter = filterMapping.getFilter();
////		webContainer.unregisterFilter(filter);
//	}

}
