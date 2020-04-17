/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.spi.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import javax.servlet.ServletConfig;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	public static Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final Bundle bundle = FrameworkUtil.getBundle(Utils.class);

	private Utils() { }

	public static boolean same(Dictionary<String, ?> cfg1, Dictionary<String, ?> cfg2) {
		if (cfg1 == null) {
			return cfg2 == null;
		} else if (cfg2 == null) {
			return false;
		} else if (cfg1.size() != cfg2.size()) {
			return false;
		} else {
			boolean result = true;
			Enumeration<String> keys = cfg1.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				Object v1 = cfg1.get(key);
				Object v2 = cfg2.get(key);
				result = Objects.equals(v1, v2);
				if (!result) {
					break;
				}
			}
			return result;
		}
	}

	public static boolean same(Object v1, Object v2) {
		return Objects.equals(v1, v2);
	}

	/**
	 * <p>Helper method to convert incoming {@link Dictionary} with unspecified types to map of Strings.</p>
	 *
	 * <p>Especially useful when translating from ConfigurationAdmin configs to maps specified at e.g.,
	 * {@link ServletConfig#getInitParameterNames()}.</p>
	 *
	 * @param dictionary dictionary of potentially diffent types of keys and values
	 * @return map of Strings
	 */
	public static Map<String, String> toMap(final Dictionary<?, ?> dictionary) {
		Map<String, String> converted = new HashMap<>();
		if (dictionary != null) {
			Enumeration<?> enumeration = dictionary.keys();
			while (enumeration.hasMoreElements()) {
				Object k = enumeration.nextElement();
				Object v = dictionary.get(k);

				String key = null;
				String value = null;

				if (k instanceof String) {
					key = (String) k;
				} else {
					// should not be null
					key = k.toString();
				}
				if (v instanceof String) {
					value = (String) v;
				} else {
					value = v == null ? null : v.toString();
				}
				converted.put(key, value);
			}
		}

		return converted;
	}

	public static String resolve(String value) {
		return resolve(value, null);
	}

	public static String resolve(String value, String defaultValue) {
		return resolve(Collections.emptyMap(), value, defaultValue);
	}

	public static String resolve(Map<String, String> properties, String value, String defaultValue) {
		Map<String, String> props = new HashMap<>();
		props.put("_v", value);
		props.putAll(properties);
		InterpolationHelper.performSubstitution(props, bundle == null ? null : bundle.getBundleContext());
		return props.get("_v");
	}

	/**
	 * Returns {@link org.osgi.framework.Constants#SERVICE_ID} or {@code -1} if not available
	 * @param ref
	 * @return
	 */
	public static long getServiceId(ServiceReference<WebContainer> ref) {
		Object idProperty = ref.getProperty(Constants.SERVICE_ID);
		if (idProperty instanceof Long) {
			return (Long) idProperty;
		}

		return -1L;
	}

	/**
	 * Creates a {@link Filter} based on a non-empty list of classes.
	 * @param bundleContext
	 * @param trackedClass
	 * @return
	 */
	public static Filter createFilter(BundleContext bundleContext, final Class<?>... trackedClass) {
		if (trackedClass.length == 0) {
			throw new IllegalArgumentException("No class specified to create objectClass-based filter.");
		}

		String filter = null;

		if (trackedClass.length == 1) {
			filter = "(" + Constants.OBJECTCLASS + "=" + trackedClass[0].getName() + ")";
		} else {
			StringBuilder filterBuilder = new StringBuilder();
			filterBuilder.append("(|");
			for (Class<?> clazz : trackedClass) {
				filterBuilder.append("(").append(Constants.OBJECTCLASS).append("=").append(clazz.getName()).append(")");
			}
			filterBuilder.append(")");
			filter = filterBuilder.toString();
		}

		try {
			return bundleContext.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unexpected InvalidSyntaxException: " + e.getMessage());
		}
	}

	/**
	 * Returns all {@link Constants#OBJECTCLASS} property values from {@link ServiceReference}
	 * @param ref
	 * @return
	 */
	public static String[] getObjectClasses(ServiceReference<?> ref) {
		Object objectClass = ref.getProperty(Constants.OBJECTCLASS);
		String className = null;
		if (objectClass instanceof String) {
			return new String[] { (String) objectClass };
		} else if (objectClass instanceof String[] && ((String[]) objectClass).length > 0) {
			return (String[]) objectClass;
		} else {
			LOG.warn("Service reference without \"objectClass\" property: {}", ref);
			return new String[0];
		}
	}

	/**
	 * Returns first available {@link Constants#OBJECTCLASS} property value from {@link ServiceReference}
	 * or {@code null} if not available (but it should be).
	 * @param ref
	 * @return
	 */
	public static String getFirstObjectClass(ServiceReference<?> ref) {
		Object objectClass = ref.getProperty(Constants.OBJECTCLASS);
		String className = null;
		if (objectClass instanceof String) {
			className = (String) objectClass;
		} else if (objectClass instanceof String[] && ((String[]) objectClass).length > 0) {
			className = ((String[]) objectClass)[0];
		}

		return className;
	}

	/**
	 * Returns a service property as {@link String}.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param key property key; cannot be null
	 * @return property value; {@code null} if property is not set or property value is not a String
	 * @throws IllegalArgumentException - If service reference is {@code null} or if key is {@code null}
	 */
	public static String getStringProperty(final ServiceReference<?> serviceReference, final String key) {
		validate(serviceReference, key);

		return asString(key, serviceReference.getProperty(key));
	}

	public static String asString(String propertyName, Object value) {
		if (value != null && !(value instanceof String)) {
			LOG.warn("Property {} should be String, but was {}", propertyName, value.getClass().getName());
			return null;
		}

		return (String) value;
	}

	/**
	 * Returns a service property as {@link Integer}.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param key property key; cannot be null
	 * @return property value; {@code null} if property is not set or property value is not a Integer
	 * @throws IllegalArgumentException - If service reference is {@code null} or if key is {@code null}
	 */
	public static Integer getIntegerProperty(final ServiceReference<?> serviceReference, final String key) {
		validate(serviceReference, key);

		return asInteger(key, serviceReference.getProperty(key));
	}

	public static Integer asInteger(String propertyName, Object value) {
		if (value != null && !(value instanceof String) && !(value instanceof Integer)) {
			LOG.warn("Property {} should be String or Integer, but was {}", propertyName, value.getClass().getName());
			return null;
		}

		if (value instanceof Integer) {
			return (Integer) value;
		}

		return value == null ? null : Integer.valueOf((String) value);
	}

	/**
	 * Returns a service property as {@link Long}.
	 *
	 * @param serviceReference service reference; cannot be null
	 * @param key property key; cannot be null
	 * @return property value; {@code null} if property is not set or property value is not a Long
	 * @throws IllegalArgumentException - If service reference is {@code null} or if key is {@code null}
	 */
	public static Long getLongProperty(final ServiceReference<?> serviceReference, final String key) {
		validate(serviceReference, key);

		return asLong(key, serviceReference.getProperty(key));
	}

	public static Long asLong(String propertyName, Object value) {
		if (value != null && !(value instanceof String) && !(value instanceof Long)) {
			LOG.warn("Property {} should be String or Long, but was {}", propertyName, value.getClass().getName());
			return null;
		}

		if (value instanceof Long) {
			return (Long) value;
		}

		return value == null ? null : Long.valueOf((String) value);
	}

	/**
	 * Get a property which can be specified as String or Boolean and return it as Boolean.
	 * @param serviceReference
	 * @param key
	 * @return
	 */
	public static Boolean getBooleanProperty(final ServiceReference<?> serviceReference, final String key) {
		validate(serviceReference, key);

		return asBoolean(key, serviceReference.getProperty(key));
	}

	public static Boolean asBoolean(String propertyName, Object value) {
		if (value != null && !(value instanceof String) && !(value instanceof Boolean)) {
			LOG.warn("Property {} should be String or Boolean, but was {}", propertyName, value.getClass().getName());
			return null;
		}

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		return Boolean.valueOf((String) value);
	}

	private static void validate(ServiceReference<?> serviceReference, String key) {
		if (key == null) {
			throw new IllegalArgumentException("Key has to be specified");
		}
		if (serviceReference == null) {
			throw new IllegalArgumentException("ServiceReference has to be specified");
		}
	}

	/**
	 * Helper method to convert generic property value into array of Strings. Should be used when a property is
	 * expected to be String, String[] or {@code Collection<String>}
	 * @param propertyName
	 * @param value
	 * @return
	 */
	public static String[] asStringArray(String propertyName, Object value) {
		if (value == null) {
			return new String[0];
		} else if (value instanceof String) {
			return new String[] { ((String) value).trim() };
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
		} else {
			LOG.warn("Property {} should be String/String[]/Collection<String>, but was {}",
					propertyName, value.getClass().getName());
		}

		return new String[0];
	}

	/**
	 * Gets a service property by checking legacy and whiteboard property names, printing relevant warnings, if needed.
	 *
	 * @param serviceReference
	 * @param legacyName
	 * @param whiteboardName
	 * @param propertyProvider
	 * @param <T>
	 * @return
	 */
	public static <T> T getPaxWebProperty(ServiceReference<?> serviceReference, String legacyName, String whiteboardName,
			BiFunction<String, Object, T> propertyProvider) {
		T value = null;
		Object propertyValue = serviceReference.getProperty(legacyName);
		if (propertyValue != null) {
			LOG.warn("Legacy {} property specified, R7 {} property should be used instead", legacyName, whiteboardName);
			value = propertyProvider.apply(legacyName, propertyValue);
		}
		propertyValue = serviceReference.getProperty(whiteboardName);
		if (propertyValue != null) {
			if (value != null) {
				LOG.warn("Both legacy {} and R7 {} properties are specified. Using R7 property: {}.", legacyName,
						whiteboardName, propertyValue);
			}
			value = propertyProvider.apply(whiteboardName, propertyValue);
		}

		return value;
	}

}
