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

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import javax.servlet.ServletConfig;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	public static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final Bundle BUNDLE = FrameworkUtil.getBundle(Utils.class);

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

				String key;
				String value;

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

	/**
	 * Special method to be called from R6 framwork ({@code ServiceReference#getProperties()} was
	 * added in R7 == org.osgi.framework;version=1.9).
	 * @param reference
	 * @return
	 */
	public static Map<String, String> toMap(final ServiceReference<?> reference) {
		Dictionary<String, Object> dict = new Hashtable<>();
		for (String key : reference.getPropertyKeys()) {
			dict.put(key, reference.getProperty(key));
		}
		return toMap(dict);
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
		InterpolationHelper.performSubstitution(props, BUNDLE == null ? null : BUNDLE.getBundleContext());
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

		String filter;

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
		Object propertyValue;
		if (legacyName != null) {
			propertyValue = serviceReference.getProperty(legacyName);
			if (propertyValue != null) {
				LOG.warn("Legacy {} property specified, R7 {} property should be used instead", legacyName, whiteboardName);
				value = propertyProvider.apply(legacyName, propertyValue);
			}
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

	/**
	 * Gets first {@link OsgiContextModel} from ranked set or {@code null} if not available.
	 * @param rankedSet
	 * @return
	 */
	public static OsgiContextModel getHighestRankedModel(Set<OsgiContextModel> rankedSet) {
		if (rankedSet == null) {
			return null;
		}
		Iterator<OsgiContextModel> it = rankedSet.iterator();
		return it.hasNext() ? it.next() : null;
	}

	/**
	 * Checks whether given {@link URL} uses protocol used by {@link Bundle#getResource(String)} (at least for Felix
	 * and Equinox).
	 *
	 * @param url
	 */
	public static boolean isBundleProtocol(URL url) {
		if (url == null) {
			return false;
		}
		String protocol = url.getProtocol();
		return "bundle".equals(protocol) || "bundleresource".equals(protocol) || "bundleentry".equals(protocol);
	}

	public static Bundle getPaxWebJspBundle(BundleContext bundleContext) {
		if (bundleContext == null) {
			return null;
		}
		for (Bundle b : bundleContext.getBundles()) {
			if (PaxWebConstants.DEFAULT_PAX_WEB_JSP_SYMBOLIC_NAME.equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	public static Bundle getPaxWebJspBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		for (Bundle b : ctx.getBundles()) {
			if (PaxWebConstants.DEFAULT_PAX_WEB_JSP_SYMBOLIC_NAME.equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Specialized method that's used to add Jetty WebSocket bundle that we know to contain required
	 * {@link javax.servlet.ServletContainerInitializer} services.
	 * Actually Jetty has two bundles with SCIs (websocket-server and javax-websocket-server-impl), but the JSR356 one
	 * is wired to the native one
	 * @param bundle
	 * @return
	 */
	public static Bundle getJettyWebSocketBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		Bundle[] bundles = new Bundle[] { null, null };
		for (Bundle b : ctx.getBundles()) {
			if ("org.eclipse.jetty.websocket.javax.websocket.server".equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Specialized method that's used to add Tomcat WebSocket bundle that we know to contain required
	 * {@link javax.servlet.ServletContainerInitializer} services
	 * @param bundle
	 * @return
	 */
	public static Bundle getTomcatWebSocketBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		for (Bundle b : ctx.getBundles()) {
			if ("org.ops4j.pax.web.pax-web-tomcat-websocket".equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Specialized method that's used to add Undertow WebSocket bundle that we know to contain required
	 * {@link javax.servlet.ServletContainerInitializer} services
	 * @param bundle
	 * @return
	 */
	public static Bundle getUndertowWebSocketBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		for (Bundle b : ctx.getBundles()) {
			// undertow-websockets-jsr doesn't include javax.servlet.ServletContainerInitializer service, so
			// we have to provide our own.
			if ("org.ops4j.pax.web.pax-web-undertow-websocket".equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Specialized method that's used to add Undertow WebSocket bundle that we know to contain required
	 * {@link javax.servlet.ServletContainerInitializer} services
	 * @param bundle
	 * @return
	 */
	public static Bundle getPaxWebUndertowWebSocketBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		for (Bundle b : ctx.getBundles()) {
			// undertow-websockets-jsr provides necessary
			// /META-INF/services/javax.websocket.server.ServerEndpointConfig$Configurator
			if ("io.undertow.websockets-jsr".equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Find Pax Web bundle for generic (Whiteboard and HttpService) support for WebSockets
	 * @param bundle
	 * @return
	 */
	public static Bundle getPaxWebWebSocketsBundle(Bundle bundle) {
		BundleContext ctx = bundle == null ? null : bundle.getBundleContext();
		if (ctx == null) {
			return null;
		}
		for (Bundle b : ctx.getBundles()) {
			if ("org.ops4j.pax.web.pax-web-websocket".equals(b.getSymbolicName())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * Returns a named header from a bundle or from one of its attached fragments
	 *
	 * @param bundle
	 * @param key
	 * @return
	 */
	public static String getManifestHeader(Bundle bundle, String key) {
		if (bundle == null || bundle.getHeaders() == null) {
			// strange...
			return null;
		}
		String header = bundle.getHeaders().get(key);
		if (header != null) {
			return header;
		}

		// check fragments
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring != null) {
			List<BundleWire> wires = wiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
			if (wires != null) {
				for (BundleWire wire : wires) {
					Bundle b = wire.getRequirerWiring().getBundle();
					if (b != null && b.getHeaders() != null) {
						header = b.getHeaders().get(key);
						if (header != null) {
							return header;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Checkes whether a {@link Bundle} is a fragment bundle.
	 * @param bundle
	 * @return
	 */
	public static boolean isFragment(Bundle bundle) {
		return bundle != null && bundle.adapt(BundleRevision.class) != null
				&& (bundle.adapt(BundleRevision.class).getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	/**
	 * Checks if given location is a directory
	 * @param location
	 * @return
	 */
	public static boolean isDirectory(URL location) {
		if ("file".equals(location.getProtocol())) {
			try {
				return new File(location.toURI()).isDirectory();
			} catch (URISyntaxException e) {
				return false;
			}
		}
		try (InputStream is = location.openStream()) {
			if (is != null && is.available() == 0) {
				return true;
			}
		} catch (Exception ignored) {
			return false;
		}
		return false;
	}

	/**
	 * Returns {@code true} if two elements models use at least one {@link OsgiContextModel} referring to the same
	 * cotext path.
	 * @param model1
	 * @param model2
	 * @return
	 */
    public static boolean useSameContextPath(ElementModel<?, ?> model1, ElementModel<?, ?> model2) {
		for (OsgiContextModel cm1 : model1.getContextModels()) {
			for (OsgiContextModel cm2 : model2.getContextModels()) {
				if (cm1.getContextPath().equals(cm2.getContextPath())) {
					return true;
				}
			}
		}

		return false;
    }

	public static boolean isConfigurationAdminAvailable(Class<?> cls) {
		try {
			cls.getClassLoader().loadClass("org.osgi.service.cm.ConfigurationAdmin");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Check if {@code org.osgi.service.event.EventAdmin} is available
	 *
	 * @return <code>true</code> if EventAdmin class can be loaded,
	 * <code>false</code> otherwhise
	 */
	public static boolean isEventAdminAvailable(Class<?> cls) {
		try {
			cls.getClassLoader().loadClass("org.osgi.service.event.EventAdmin");
			return true;
		} catch (ClassNotFoundException ignore) {
			return false;
		}
	}

	/**
	 * Check if Jasypt bundle is available
	 *
	 * @return
	 */
	public static boolean isJasyptAvailable(Class<?> cls) {
		try {
			cls.getClassLoader().loadClass("org.jasypt.encryption.StringEncryptor");
			return true;
		} catch (ClassNotFoundException ignore) {
			return false;
		}
	}

}
