/* Copyright 2011 Achim Nierbeck.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ops4j.pax.web.service.jetty.internal.util;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import static org.ops4j.util.xml.ElementHelper.getAttribute;
import static org.ops4j.util.xml.ElementHelper.getChildren;
import static org.ops4j.util.xml.ElementHelper.getRootElement;
import static org.ops4j.util.xml.ElementHelper.getValue;

/**
 * adapted and optimized JettyXmlConfiguration class for reading jetty-web.xml
 * files
 */
public class DOMJettyWebXmlParser {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(DOMJettyWebXmlParser.class);

	//CHECKSTYLE:OFF
	private static final Class<?>[] __primitives = {Boolean.TYPE,
			Character.TYPE, Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE,
			Float.TYPE, Double.TYPE, Void.TYPE};

	private static final Class<?>[] __primitiveHolders = {Boolean.class,
			Character.class, Byte.class, Short.class, Integer.class,
			Long.class, Float.class, Double.class, Void.class};
	private static final Integer ZERO = Integer.valueOf(0);

	Map<String, Object> _idMap = new HashMap<>();
	Map<String, String> _propertyMap = new HashMap<>();

	public Object parse(Object webApp, InputStream inputStream) {
		try {
			final Element rootElement = getRootElement(inputStream);

			configure(webApp, rootElement, ZERO);

		} catch (Exception e) {
			LOG.warn("Exception while configuring webApp!", e);
		}
		return null;
	}
	//CHECKSTYLE:ON

	/* ------------------------------------------------------------ */

	/**
	 * Recursive configuration step. This method applies the remaining Set, Put
	 * and Call elements to the current object.
	 *
	 * @param obj
	 * @param cfg
	 * @param startIdx the child element index to start with
	 * @throws Exception
	 */
	public void configure(Object obj, Element cfg, int startIdx)
			throws Exception {
		String id = getAttribute(cfg, "id");
		if (id != null) {
			_idMap.put(id, obj);
		}

		Element[] children = getChildren(cfg);

		for (int i = startIdx; i < children.length; i++) {
			Element node = children[i];
			//CHECKSTYLE:OFF
			try {
				// TODO: in case switching to jdk 7, this could be a switch!
				String tag = node.getTagName();
				if ("Set".equals(tag)) {
					set(obj, node);
				} else if ("Put".equals(tag)) {
					put(obj, node);
				} else if ("Call".equals(tag)) {
					call(obj, node);
				} else if ("Get".equals(tag)) {
					get(obj, node);
				} else if ("New".equals(tag)) {
					newObj(obj, node);
				} else if ("Array".equals(tag)) {
					newArray(obj, node);
				} else if ("Ref".equals(tag)) {
					refObj(obj, node);
				} else if ("Property".equals(tag)) {
					propertyObj(obj, node);
				} else {
					throw new IllegalStateException("Unknown tag: " + tag);
				}
			} catch (Exception e) {
				LOG.warn("Config error at " + node, e.toString());
				throw e;
			}
			//CHECKSTYLE:ON
		}
	}

	/* ------------------------------------------------------------ */
	private Class<?> nodeClass(Element node) throws ClassNotFoundException {
		String className = getAttribute(node, "class");
		if (className == null) {
			return null;
		}

		return Loader.loadClass(DOMJettyWebXmlParser.class, className, true);
	}

	/* ------------------------------------------------------------ */
	/*
	 * Call a set method. This method makes a best effort to find a matching set
	 * method. The type of the value is used to find a suitable set method by 1.
	 * Trying for a trivial type match. 2. Looking for a native type match. 3.
	 * Trying all correctly named methods for an auto conversion. 4. Attempting
	 * to construct a suitable value from original value. @param obj
	 * 
	 * @param node
	 */
	private void set(Object obj, Element node) throws Exception {
		String attr = getAttribute(node, "name");
		String name = "set" + attr.substring(0, 1).toUpperCase()
				+ attr.substring(1);
		Object value = value(obj, node);
		Object[] arg = {value};

		//CHECKSTYLE:OFF
		Class<?> oClass = nodeClass(node);
		if (oClass != null) {
			obj = null;
		} else {
			oClass = obj.getClass();
		}
		//CHECKSTYLE:ON

		Class<?>[] vClass = {Object.class};
		if (value != null) {
			vClass[0] = value.getClass();
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("XML "
					+ (obj != null ? obj.toString() : oClass.getName()) + "."
					+ name + "(" + value + ")");
		}

		// Try for trivial match
		try {
			Method set = oClass.getMethod(name, vClass);
			set.invoke(obj, arg);
			return;
		} catch (IllegalArgumentException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("IllegalArgument while parsing jetty-web.xml", e);
			}
		} catch (IllegalAccessException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("IllegalAccessException while parsing jetty-web.xml",
						e);
			}
		} catch (NoSuchMethodException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("NoSuchMethodException while parsing jetty-web.xml", e);
			}
		}

		// Try for native match
		try {
			Field type = vClass[0].getField("TYPE");
			vClass[0] = (Class<?>) type.get(null);
			Method set = oClass.getMethod(name, vClass);
			set.invoke(obj, arg);
			return;
		} catch (NoSuchFieldException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("NoSuchFieldException while parsing jetty-web.xml", e);
			}
		} catch (IllegalArgumentException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(
						"IllegalArgumentException while parsing jetty-web.xml",
						e);
			}
		} catch (IllegalAccessException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("IllegalAccessException while parsing jetty-web.xml",
						e);
			}
		} catch (NoSuchMethodException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("NoSuchMethodException while parsing jetty-web.xml", e);
			}
		}

		// Try a field
		try {
			Field field = oClass.getField(attr);
			if (Modifier.isPublic(field.getModifiers())) {
				field.set(obj, value);
				return;
			}
		} catch (NoSuchFieldException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("NoSuchFieldException while parsing jetty-web.xml", e);
			}
		}

		// Search for a match by trying all the set methods
		Method[] sets = oClass.getMethods();
		Method set = null;
		for (int s = 0; sets != null && s < sets.length; s++) {

			Class<?>[] paramTypes = sets[s].getParameterTypes();
			if (name.equals(sets[s].getName()) && paramTypes.length == 1) {

				// lets try it
				try {
					set = sets[s];
					sets[s].invoke(obj, arg);
					return;
				} catch (IllegalArgumentException e) {
					if (LOG.isInfoEnabled()) {
						LOG.info(
								"IllegalArgumentException while parsing jetty-web.xml",
								e);
					}
				} catch (IllegalAccessException e) {
					if (LOG.isInfoEnabled()) {
						LOG.info(
								"IllegalAccessException while parsing jetty-web.xml",
								e);
					}
				}

				// Can we convert to a collection
				if (paramTypes[0].isAssignableFrom(Collection.class)
						&& value.getClass().isArray()) {
					try {
						if (paramTypes[0].isAssignableFrom(Set.class)) {
							sets[s].invoke(
									obj,
									new Object[]{new HashSet<>(Arrays
											.asList((Object[]) value))});
						} else {
							sets[s].invoke(obj, new Object[]{Arrays
									.asList((Object[]) value)});
						}
						return;
					} catch (IllegalArgumentException e) {
						if (LOG.isInfoEnabled()) {
							LOG.info(
									"IllegalArgumentException while parsing jetty-web.xml",
									e);
						}
					} catch (IllegalAccessException e) {
						if (LOG.isInfoEnabled()) {
							LOG.info(
									"IllegalAccessException while parsing jetty-web.xml",
									e);
						}
					}
				}
			}
		}

		// Try converting the arg to the last set found.
		if (set != null) {
			try {
				Class<?> sClass = set.getParameterTypes()[0];
				if (sClass.isPrimitive()) {
					for (int t = 0; t < __primitives.length; t++) {
						if (sClass.equals(__primitives[t])) {
							sClass = __primitiveHolders[t];
							break;
						}
					}
				}
				Constructor<?> cons = sClass.getConstructor(vClass);
				arg[0] = cons.newInstance(arg);
				set.invoke(obj, arg);
				return;
			} catch (NoSuchMethodException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"NoSuchMethodException while parsing jetty-web.xml",
							e);
				}
			} catch (IllegalAccessException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"IllegalAccessException while parsing jetty-web.xml",
							e);
				}
			} catch (InstantiationException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"InstantiationException while parsing jetty-web.xml",
							e);
				}
			}
		}

		// No Joy
		throw new NoSuchMethodException(oClass + "." + name + "(" + vClass[0]
				+ ")");
	}

	/* ------------------------------------------------------------ */
	/*
	 * Call a put method.
	 * 
	 * @param obj @param node
	 */
	private void put(Object obj, Element node) throws Exception {
		if (!(obj instanceof Map)) {
			throw new IllegalArgumentException("Object for put is not a Map: "
					+ obj);
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> map = (Map<Object, Object>) obj;

		String name = getAttribute(node, "name");
		Object value = value(obj, node);
		map.put(name, value);
		if (LOG.isDebugEnabled()) {
			LOG.debug("XML " + obj + ".put(" + name + "," + value + ")");
		}
	}

	/* ------------------------------------------------------------ */
	/*
	 * Call a get method. Any object returned from the call is passed to the
	 * configure method to consume the remaining elements. @param obj @param
	 * node
	 * 
	 * @return @exception Exception
	 */
	private Object get(Object obj, Element node) throws Exception {
		Class<?> oClass = nodeClass(node);
		//CHECKSTYLE:OFF
		if (oClass != null) {
			obj = null;
		} else {
			oClass = obj.getClass();
		}
		//CHECKSTYLE:ON

		String name = getAttribute(node, "name");
		String id = getAttribute(node, "id");
		if (LOG.isDebugEnabled()) {
			LOG.debug("XML get " + name);
		}

		//CHECKSTYLE:OFF
		try {
			// try calling a getXxx method.
			Method method = oClass.getMethod("get"
							+ name.substring(0, 1).toUpperCase() + name.substring(1),
					(java.lang.Class[]) null);
			obj = method.invoke(obj, ((java.lang.Object[]) null));
			configure(obj, node, 0);
		} catch (NoSuchMethodException nsme) {
			try {
				Field field = oClass.getField(name);
				obj = field.get(obj);
				configure(obj, node, 0);
			} catch (NoSuchFieldException nsfe) {
				throw nsme;
			}
		}
		//CHECKSTYLE:ON
		if (id != null) {
			_idMap.put(id, obj);
		}
		return obj;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Call a method. A method is selected by trying all methods with matching
	 * names and number of arguments. Any object returned from the call is
	 * passed to the configure method to consume the remaining elements. Note
	 * that if this is a static call we consider only methods declared directly
	 * in the given class. i.e. we ignore any static methods in superclasses.
	 * 
	 * @param obj
	 * 
	 * @param node @return @exception Exception
	 */
	private Object call(Object obj, Element node) throws Exception {
		String id = getAttribute(node, "id");
		Class<?> oClass = nodeClass(node);
		//CHECKSTYLE:OFF
		if (oClass != null) {
			obj = null;
		} else if (obj != null) {
			oClass = obj.getClass();
		}
		if (oClass == null) {
			throw new IllegalArgumentException(node.toString());
		}
		//CHECKSTYLE:ON
		int size = 0;

		Element[] children = getChildren(node);
		int argi = children.length;
		for (int i = 0; i < children.length; i++) {

			Element element = children[i];

			if (!(element.getTagName().equals("Arg"))) {
				argi = i;
				break;
			}
			size++;
		}

		//CHECKSTYLE:OFF
		Object[] arg = new Object[size];
		for (int i = 0, j = 0; j < size; i++) {
			Element element = children[i];
			arg[j++] = value(obj, element);
		}
		//CHECKSTYLE:ON

		String method = getAttribute(node, "name");
		if (LOG.isDebugEnabled()) {
			LOG.debug("XML call " + method);
		}

		try {
			Object n = TypeUtil.call(oClass, method, obj, arg);
			if (id != null) {
				_idMap.put(id, n);
			}
			configure(n, node, argi);
			return n;
		} catch (NoSuchMethodException e) {
			IllegalStateException ise = new IllegalStateException("No Method: "
					+ node + " on " + oClass);
			ise.initCause(e);
			throw ise;
		}

	}

	/* ------------------------------------------------------------ */
	/*
	 * Create a new value object.
	 * 
	 * @param obj @param node @return @exception Exception
	 */
	private Object newObj(Object obj, Element node) throws Exception {
		Class<?> oClass = nodeClass(node);
		String id = getAttribute(node, "id");
		int size = 0;
		Element[] children = getChildren(node);
		int argi = children.length;
		for (int i = 0; i < children.length; i++) {
			Element element = children[i];

			if (!(element.getTagName().equals("Arg"))) {
				argi = i;
				break;
			}
			size++;
		}

		//CHECKSTYLE:OFF
		Object[] arg = new Object[size];
		for (int i = 0, j = 0; j < size; i++) {
			Element o = children[i];
			arg[j++] = value(obj, o);
		}
		//CHECKSTYLE:ON

		if (LOG.isDebugEnabled()) {
			LOG.debug("XML new " + oClass);
		}

		// Lets just try all constructors for now
		Constructor<?>[] constructors = oClass.getConstructors();
		for (int c = 0; constructors != null && c < constructors.length; c++) {
			if (constructors[c].getParameterTypes().length != size) {
				continue;
			}

			Object n = null;
			boolean called = false;
			try {
				n = constructors[c].newInstance(arg);
				called = true;
			} catch (IllegalAccessException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"IllegalAccessException while parsing jetty-web.xml",
							e);
				}
			} catch (InstantiationException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"InstantiationException while parsing jetty-web.xml",
							e);
				}
			} catch (IllegalArgumentException e) {
				if (LOG.isInfoEnabled()) {
					LOG.info(
							"IllegalArgumentException while parsing jetty-web.xml",
							e);
				}
			}
			if (called) {
				if (id != null) {
					_idMap.put(id, n);
				}
				configure(n, node, argi);
				return n;
			}
		}

		throw new IllegalStateException("No Constructor: " + node + " on "
				+ obj);
	}

	/* ------------------------------------------------------------ */
	/*
	 * Reference an id value object.
	 * 
	 * @param obj @param node @return @exception NoSuchMethodException
	 * 
	 * @exception ClassNotFoundException @exception InvocationTargetException
	 */
	private Object refObj(Object obj, Element node) throws Exception {
		String id = getAttribute(node, "id");
		//CHECKSTYLE:OFF
		obj = _idMap.get(id);
		//CHECKSTYLE:ON
		if (obj == null) {
			throw new IllegalStateException("No object for id=" + id);
		}
		configure(obj, node, 0);
		return obj;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Create a new array object.
	 */
	private Object newArray(Object obj, Element node) throws Exception {

		// Get the type
		Class<?> aClass = java.lang.Object.class;
		String type = getAttribute(node, "type");
		final String id = getAttribute(node, "id");
		if (type != null) {
			aClass = TypeUtil.fromName(type);
			if (aClass == null) {
				if ("String".equals(type)) {
					aClass = java.lang.String.class;
				} else if ("URL".equals(type)) {
					aClass = java.net.URL.class;
				} else if ("InetAddress".equals(type)) {
					aClass = java.net.InetAddress.class;
				} else {
					aClass = Loader.loadClass(DOMJettyWebXmlParser.class, type,
							true);
				}
			}
		}

		List<Object> al = null;

		Element[] children = getChildren(node, "Item");
		for (Element item : children) {
			String nid = getAttribute(item, "id");
			Object v = value(obj, item);

			if (al == null) {
				al = new ArrayList<>();
			}

			al.add((v == null && aClass.isPrimitive()) ? ZERO : v);
			if (nid != null) {
				_idMap.put(nid, v);
			}
		}

		Object array = toArray(al, aClass);
		if (id != null) {
			_idMap.put(id, array);
		}
		return array;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Create a new map object.
	 */
	private Object newMap(Object obj, Element node) throws Exception {
		String id = getAttribute(node, "id");

		Map<Object, Object> map = new HashMap<>();
		if (id != null) {
			_idMap.put(id, map);
		}

		Element[] children = getChildren(node);
		for (int i = 0; i < children.length; i++) {

			Element element = children[i];

			if (!element.getTagName().equals("Entry")) {
				throw new IllegalStateException("Not an Entry");
			}

			Element key = null;
			Element value = null;

			Element[] entries = getChildren(element);
			for (int j = 0; j < entries.length; j++) {
				Element item = entries[j];
				if (!item.getTagName().equals("Item")) {
					throw new IllegalStateException("Not an Item");
				}
				if (key == null) {
					key = item;
				} else {
					value = item;
				}
			}

			if (key == null || value == null) {
				throw new IllegalStateException("Missing Item in Entry");
			}
			String kid = getAttribute(key, "id");
			String vid = getAttribute(value, "id");

			Object k = value(obj, key);
			Object v = value(obj, value);
			map.put(k, v);

			if (kid != null) {
				_idMap.put(kid, k);
			}
			if (vid != null) {
				_idMap.put(vid, v);
			}
		}

		return map;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Create a new value object.
	 * 
	 * @param obj @param node @return @exception Exception
	 */
	private Object propertyObj(Object obj, Element node) throws Exception {
		String id = getAttribute(node, "id");
		String name = getAttribute(node, "name");
		String defval = getAttribute(node, "default");
		Object prop = null;
		if (_propertyMap != null && _propertyMap.containsKey(name)) {
			prop = _propertyMap.get(name);
		} else {
			prop = defval;
		}
		if (id != null) {
			_idMap.put(id, prop);
		}
		if (prop != null) {
			configure(prop, node, 0);
		}
		return prop;
	}

	/* ------------------------------------------------------------ */
	/*
	 * Get the value of an element. If no value type is specified, then white
	 * space is trimmed out of the value. If it contains multiple value elements
	 * they are added as strings before being converted to any specified type.
	 * 
	 * @param node
	 */
	private Object value(Object obj, Element node) throws Exception {
		Object value = null;

		// Get the type
		String type = getAttribute(node, "type");

		// Try a ref lookup
		String ref = getAttribute(node, "ref");
		if (ref != null) {
			value = _idMap.get(ref);
		} else {
			// handle trivial case
			if (getChildren(node).length == 0
					&& (getValue(node) == null || getValue(node).length() == 0)) {
				if ("String".equals(type)) {
					return "";
				}
				return null;
			}

			// Trim values
			int first = 0;
			int last = /*
						 * getChildren(node).length - 1; int valLast =
						 */getValue(node).length() - 1;

			// Handle default trim type
			if (type == null || !"String".equals(type)) {
				// Skip leading white
				Object item = null;
				while (first <= last) {
					item = /*
							 * getChildren(node)[first]; val =
							 */getValue(node);
					if (!(item instanceof String)) {
						break;
					}
					item = ((String) item).trim();
					if (((String) item).length() > 0) {
						break;
					}
					first++;
				}

				// Skip trailing white
				while (first < last) {
					item = getValue(node);
					if (!(item instanceof String)) {
						break;
					}
					item = ((String) item).trim();
					if (((String) item).length() > 0) {
						break;
					}
					last--;
				}

				// All white, so return null
				if (first > last) {
					// first check if there are maybe is another child left
					if (getChildren(node).length == 0) {
						return null;
					} else {
						value = itemValue(obj, getChildren(node)[0]);
					}
				} else {
					value = item;
				}
			} else  if (first == last) {
	            // Single Item value
	            value = itemValue(obj,getChildren(node)[0]);
		    } else {
	            value = getValue(node);
	        }
		}
		
		

		// Untyped or unknown
		if (value == null) {
			if ("String".equals(type)) {
				return "";
			}
			return null;
		}

		// Try to type the object
		if (type == null) {
			if (value != null && value instanceof String) {
				return ((String) value).trim();
			}
			return value;
		}

		if ("String".equals(type) || "java.lang.String".equals(type)) {
			return value.toString();
		}

		Class<?> pClass = TypeUtil.fromName(type);
		if (pClass != null) {
			return TypeUtil.valueOf(pClass, value.toString());
		}

		if ("URL".equals(type) || "java.net.URL".equals(type)) {
			if (value instanceof URL) {
				return value;
			}
			try {
				return new URL(value.toString());
			} catch (MalformedURLException e) {
				throw new InvocationTargetException(e);
			}
		}

		if ("InetAddress".equals(type) || "java.net.InetAddress".equals(type)) {
			if (value instanceof InetAddress) {
				return value;
			}
			try {
				return InetAddress.getByName(value.toString());
			} catch (UnknownHostException e) {
				throw new InvocationTargetException(e);
			}
		}

		throw new IllegalStateException("Unknown type " + type);
	}

	/* ------------------------------------------------------------ */
	/*
	 * Get the value of a single element. @param obj @param item @return
	 * 
	 * @exception Exception
	 */
	private Object itemValue(Object obj, Object item) throws Exception {
		// String value
		if (item instanceof String) {
			return item;
		}

		Element node = (Element) item;
		String tag = node.getTagName();
		// TODO: in case of jdk7 usage use swtich instead
		if ("Call".equals(tag)) {
			return call(obj, node);
		}
		if ("Get".equals(tag)) {
			return get(obj, node);
		}
		if ("New".equals(tag)) {
			return newObj(obj, node);
		}
		if ("Ref".equals(tag)) {
			return refObj(obj, node);
		}
		if ("Array".equals(tag)) {
			return newArray(obj, node);
		}
		if ("Map".equals(tag)) {
			return newMap(obj, node);
		}
		if ("Property".equals(tag)) {
			return propertyObj(obj, node);
		}

		if ("SystemProperty".equals(tag)) {
			String name = getAttribute(node, "name");
			String defaultValue = getAttribute(node, "default");
			return System.getProperty(name, defaultValue);
		}

		LOG.warn("Unknown value tag: {}", node);
		return null;
	}

	public static Object toArray(Object list, Class<?> clazz) {
		if (list == null) {
			return Array.newInstance(clazz, 0);
		}

		if (list instanceof List) {
			List<?> l = (List<?>) list;
			if (clazz.isPrimitive()) {
				Object a = Array.newInstance(clazz, l.size());
				for (int i = 0; i < l.size(); i++) {
					Array.set(a, i, l.get(i));
				}
				return a;
			}
			return l.toArray((Object[]) Array.newInstance(clazz, l.size()));

		}

		Object a = Array.newInstance(clazz, 1);
		Array.set(a, 0, list);
		return a;
	}

}
