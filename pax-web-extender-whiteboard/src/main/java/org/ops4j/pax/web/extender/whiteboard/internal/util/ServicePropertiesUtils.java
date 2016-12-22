/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities related to service properties.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, March 16, 2008
 */
public class ServicePropertiesUtils {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServicePropertiesUtils.class);

	/**
	 * Utility class constructor.
	 */
	private ServicePropertiesUtils() {
		// utility class
	}

	/**
	 * Returns a property as String.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param key              property key; cannot be null
	 * @return property value; null if property is not set or property value is
	 * not a String
	 * @throws NullArgumentException - If service reference is null - If key is null
	 */
	public static String getStringProperty(
			final ServiceReference<?> serviceReference, final String key) {
		NullArgumentException.validateNotNull(serviceReference, "Service reference");
		NullArgumentException.validateNotEmpty(key, true, "Property key");

		Object value = serviceReference.getProperty(key);
		if (value != null && !(value instanceof String)) {
			LOG.error("Property [" + key + "] value must be a String");
			return null;
		}
		return (String) value;
	}

	public static Boolean getBooleanProperty(final ServiceReference<?> serviceReference, final String key) {
		String stringProperty = getStringProperty(serviceReference, key);

		return Boolean.parseBoolean(stringProperty);
	}

	/**
	 * Returns a property as Integer.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param key              property key; cannot be null
	 * @return property value; null if property is not set or property value is
	 * not an Integer
	 * @throws NullArgumentException - If service reference is null - If key is null
	 */
	public static Integer getIntegerProperty(final ServiceReference<?> serviceReference, final String key) {
		NullArgumentException.validateNotNull(serviceReference, "Service reference");
		NullArgumentException.validateNotEmpty(key, true, "Property key");
		final Object value = serviceReference.getProperty(key);
		if (value instanceof Integer) {
			return (Integer) value;
		} else if (value != null) {
			try {
				return Integer.parseInt(String.valueOf(value));
			} catch (NumberFormatException e) {
				final String message = String.format("Property [%s] value must be an Integer: %s", key, e.getMessage());
				LOG.error(message, e);
				return null;
			}
		} else {
			return null;
		}
	}

	public static String[] getArrayOfStringProperty(final ServiceReference<?> serviceReference, final String key) {
		NullArgumentException.validateNotNull(serviceReference, "Service reference");
		NullArgumentException.validateNotEmpty(key, true, "Property key");

		Object value = serviceReference.getProperty(key);

		if (value instanceof String) {
			return new String[]{((String) value).trim()};
		} else if (value instanceof String[]) {
			return (String[]) value;
		} else if (value instanceof Collection<?>) {
			Collection<?> collectionValues = (Collection<?>) value;
			String[] values = new String[collectionValues.size()];

			int i = 0;
			for (Object current : collectionValues) {
				values[i++] = current != null ? String.valueOf(current).trim() : null;
			}

			return values;
		}

		return null;
	}

	/**
	 * Returns the subset of properties that start with the prefix. The returned
	 * dictionary will have as keys the original key without the prefix.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param prefix           property keys prefix; cannot be null
	 * @return subset of properties or null if there is no property that starts
	 * with expected prefix
	 */
	public static Map<String, Object> getSubsetStartingWith(
			final ServiceReference<?> serviceReference, final String prefix) {
		final Map<String, Object> subset = new HashMap<>();
		for (String key : serviceReference.getPropertyKeys()) {
			if (key != null && key.startsWith(prefix)
					&& key.trim().length() > prefix.length()) {
				subset.put(key.substring(prefix.length()),
						serviceReference.getProperty(key));
			}
		}
		if (subset.isEmpty()) {
			return null;
		}
		return subset;
	}

	static public Object mergePropertyListOfStringsToArrayOfStrings(final Object objectToMerge, final List<String> listOfStrings) {
		Set<String> setToMerge = new HashSet<>();
		setToMerge.addAll(listOfStrings);
		if (objectToMerge instanceof String
				&& ((String) objectToMerge).trim().length() != 0) {
			setToMerge.add((String) objectToMerge);
		} else if (objectToMerge instanceof String[]) {
			setToMerge.addAll(Arrays.asList((String[]) objectToMerge));
		}
		return setToMerge.toArray(new String[setToMerge.size()]);
	}


	/**
	 * Utility method to extract the httpContextID from the service reference.
	 * This can either be included with the "old" Pax-Web style or the new OSGi R6 Whiteboard style.
	 *
	 * @param serviceReference - service reference where the httpContextID needs to be extracted from.
	 * @return the http context id
	 */
	static public String extractHttpContextId(final ServiceReference<?> serviceReference) {
		String httpContextId = getStringProperty(serviceReference, ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID);

		//TODO: Make sure the current HttpContextSelect works together with R6
		if (httpContextId == null) {
			String httpContextSelector = getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
			if (httpContextSelector != null) {
				httpContextSelector = httpContextSelector.substring(1, httpContextSelector.length());
				httpContextId = httpContextSelector.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME.length() + 1);
				httpContextId = httpContextId.substring(0, httpContextId.length() - 1);
			}
		}
		return httpContextId;
	}

	/**
	 * Utility method to extract the shared state of the HttpContext
	 *
	 * @param serviceReference
	 * @return
	 */
	static public Boolean extractSharedHttpContext(final ServiceReference<?> serviceReference) {
		Boolean sharedHttpContext = Boolean
				.parseBoolean((String) serviceReference
						.getProperty(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED));

		if (serviceReference.getProperty(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT) != null) {
			sharedHttpContext = true;
		}
		return sharedHttpContext;
	}


}
