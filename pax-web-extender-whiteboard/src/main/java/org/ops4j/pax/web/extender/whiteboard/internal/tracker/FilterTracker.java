/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.annotation.WebInitParam;

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.FilterWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultFilterMapping;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.utils.FilterAnnotationScanner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link Filter}s.
 *
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @since 0.4.0, April 05, 2008
 */
public class FilterTracker extends AbstractTracker<Filter, FilterWebElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(FilterTracker.class);

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private FilterTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	public static ServiceTracker<Filter, FilterWebElement> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new FilterTracker(extenderContext, bundleContext).create(Filter.class);
	}

	/**
	 * @see AbstractTracker#createWebElement(ServiceReference, Object)
	 */
	@Override
	FilterWebElement createWebElement(final ServiceReference<Filter> serviceReference, final Filter published) {

		Object urlPatternsProp = serviceReference.getProperty(ExtenderConstants.PROPERTY_URL_PATTERNS);

		if (urlPatternsProp == null) {
			urlPatternsProp = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
		} else {
			String[] whiteBoardProp = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
			if (whiteBoardProp != null) {
				urlPatternsProp = ServicePropertiesUtils.mergePropertyListOfStringsToArrayOfStrings(urlPatternsProp,
						Arrays.asList(whiteBoardProp));
			}
		}

		String[] regexUrlProps = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX);
		if (regexUrlProps != null) {
			urlPatternsProp = ServicePropertiesUtils.mergePropertyListOfStringsToArrayOfStrings(urlPatternsProp,
					Arrays.asList(regexUrlProps));
		}

		String[] urlPatterns = null;

		FilterAnnotationScanner annotationScan = new FilterAnnotationScanner(published.getClass());

		if (annotationScan.scanned) {
			if (urlPatternsProp == null) {
				urlPatternsProp = annotationScan.urlPatterns;
			} else {
				Set<String> patterns = new HashSet<>();
				patterns.addAll(Arrays.asList(annotationScan.urlPatterns));
				if (urlPatternsProp instanceof String && ((String) urlPatternsProp).trim().length() != 0) {
					patterns.add((String) urlPatternsProp);
				} else if (urlPatternsProp instanceof String[]) {
					patterns.addAll(Arrays.asList((String[]) urlPatternsProp));
				}
				urlPatternsProp = patterns.toArray(new String[patterns.size()]);
			}

		}

		if (urlPatternsProp != null) {
			if (urlPatternsProp instanceof String) {
				urlPatterns = new String[] { (String) urlPatternsProp };
			} else if (urlPatternsProp instanceof String[]) {
				urlPatterns = (String[]) urlPatternsProp;
			}
		}
		Object servletNamesProp = serviceReference.getProperty(ExtenderConstants.PROPERTY_SERVLET_NAMES);
		if (servletNamesProp == null) {
			servletNamesProp = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET);
		}

		if (annotationScan.scanned) {
			if (servletNamesProp == null) {
				servletNamesProp = annotationScan.servletNames;
			} else {
				Set<String> patterns = new HashSet<>();
				patterns.addAll(Arrays.asList(annotationScan.servletNames));
				if (servletNamesProp instanceof String && ((String) servletNamesProp).trim().length() != 0) {
					patterns.add((String) servletNamesProp);
				} else if (servletNamesProp instanceof String[]) {
					patterns.addAll(Arrays.asList((String[]) servletNamesProp));
				}
				servletNamesProp = patterns.toArray(new String[patterns.size()]);
			}
		}

		String[] servletNames = null;
		if (servletNamesProp != null) {
			if (servletNamesProp instanceof String) {
				servletNames = new String[] { (String) servletNamesProp };
			} else if (servletNamesProp instanceof String[]) {
				servletNames = (String[]) servletNamesProp;
			}
		}

		String httpContextId = ServicePropertiesUtils.extractHttpContextId(serviceReference);

		final String[] initParamKeys = serviceReference.getPropertyKeys();
		// make all the service parameters available as initParams to
		// registering the Filter
		Map<String, String> initParams = new HashMap<>();
		for (String key : initParamKeys) {
			try {
				String value = serviceReference.getProperty(key) == null ? ""
						: serviceReference.getProperty(key).toString();
				if (key.startsWith(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX)) {
					initParams.put(key.replaceFirst(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_INIT_PARAM_PREFIX, ""), value);
				} else {
					initParams.put(key, value);
				}
				// CHECKSTYLE:OFF
			} catch (Exception ignore) {
				// ignore
			}
			// CHECKSTYLE:ON
		}

		if (annotationScan.scanned) {
			for (WebInitParam initParam : annotationScan.webInitParams) {
				initParams.put(initParam.name(), initParam.value());
			}
		}

		Boolean asyncSupported = Boolean.FALSE;
		// spec 140.16.2.11: "The value of this service property must be of type Boolean."
		Object value = serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
		if (value instanceof Boolean) {
			asyncSupported = (Boolean) value;
		} else {
			// let's relax the requirement and allow Strings as Booleans as well
			// if that's not what we want, `git blame` will show you whom to contact
			asyncSupported = ServicePropertiesUtils.getBooleanProperty(serviceReference,
					HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
		}

		if (annotationScan.scanned) {
			asyncSupported = annotationScan.asyncSupported;
		}

		String[] dispatcherTypeProps = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
		DispatcherType[] dispatcherTypes = null;

		if (annotationScan.scanned) {
			if (dispatcherTypeProps == null) {
				dispatcherTypes = annotationScan.dispatcherTypes;
			} else {
				dispatcherTypes = annotationScan.dispatcherTypes;
				List<DispatcherType> dispatcherTypeList = new ArrayList<>(Arrays.asList(dispatcherTypes));
				for (String dispatcherTypeProp : dispatcherTypeProps) {
					dispatcherTypeList.add(DispatcherType.valueOf(dispatcherTypeProp));
				}
				dispatcherTypes = dispatcherTypeList.toArray(new DispatcherType[dispatcherTypeList.size()]);
			}
		}

		String dispatcherInitString = null;
		if (dispatcherTypes != null) {
			StringBuffer buff = new StringBuffer();
			for (DispatcherType dispatcherType : dispatcherTypes) {
				buff = buff.append(dispatcherType.toString()).append(",");
			}
			dispatcherInitString = buff.toString();
			dispatcherInitString = dispatcherInitString.substring(dispatcherInitString.length() - 1);
		}

		if (dispatcherInitString != null) {
			initParams.put(PaxWebConstants.FILTER_MAPPING_DISPATCHER, dispatcherInitString);
		}

		Integer serviceRank = ServicePropertiesUtils.getIntegerProperty(serviceReference, Constants.SERVICE_RANKING);
		if (serviceRank != null) {
			initParams.put(PaxWebConstants.FILTER_RANKING, serviceRank.toString());
		}


		final DefaultFilterMapping mapping = new DefaultFilterMapping();
		mapping.setFilter(published);
		mapping.setAsyncSupported(asyncSupported);
		mapping.setHttpContextId(httpContextId);
		mapping.setUrlPatterns(urlPatterns);
		mapping.setServletNames(servletNames);
		mapping.setInitParams(initParams);
		mapping.setDispatcherType(dispatcherTypes);
		mapping.setName(ServicePropertiesUtils.getStringProperty(
				serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME));
		return new FilterWebElement<>(serviceReference, mapping);
	}
}
