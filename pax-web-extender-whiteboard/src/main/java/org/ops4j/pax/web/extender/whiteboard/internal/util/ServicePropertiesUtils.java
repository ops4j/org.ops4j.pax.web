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

import java.util.HashMap;
import java.util.Map;

import org.ops4j.lang.NullArgumentException;
import org.osgi.framework.ServiceReference;
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
	 * @param serviceReference
	 *            service reference; cannot be null
	 * @param key
	 *            property key; canot be null
	 * 
	 * @return property value; null if property is not set or property value is
	 *         not a String
	 * 
	 * @throws NullArgumentException
	 *             - If service reference is null - If key is null
	 */
	public static String getStringProperty(
			final ServiceReference<?> serviceReference, final String key) {
		NullArgumentException.validateNotNull(serviceReference,
				"Service reference");
		NullArgumentException.validateNotEmpty(key, true, "Property key");

		Object value = serviceReference.getProperty(key);
		if (value != null && !(value instanceof String)) {
			LOG.error("Property [" + key + "] value must be a String");
			return null;
		}
		return (String) value;
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

	/**
	 * Returns the subset of properties that start with the prefix. The returned
	 * dictionary will have as keys the original key without the prefix.
	 * 
	 * @param serviceReference
	 *            service reference; cannot be null
	 * @param prefix
	 *            property keys prefix; cannot be null
	 * 
	 * @return subset of properties or null if there is no property that starts
	 *         with expected prefix
	 */
	public static Map<String, Object> getSubsetStartingWith(
			final ServiceReference<?> serviceReference, final String prefix) {
		final Map<String, Object> subset = new HashMap<String, Object>();
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

}
