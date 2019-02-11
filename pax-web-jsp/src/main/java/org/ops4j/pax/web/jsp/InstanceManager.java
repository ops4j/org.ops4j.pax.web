/*
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
package org.ops4j.pax.web.jsp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.servlet.Filter;

import org.apache.jasper.security.SecurityUtil;

public class InstanceManager implements org.apache.tomcat.InstanceManager {

	private final Map<String, Map<String, String>> injectionMap = new HashMap<>();

	private final Properties restrictedFilters = new Properties();
	private final Properties restrictedListeners = new Properties();
	private final Map<Class<?>, List<AnnotationCacheEntry>> annotationCache = new WeakHashMap<>();

	@Override
	public Object newInstance(String className) throws IllegalAccessException,
			InvocationTargetException, InstantiationException,
			ClassNotFoundException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		Class<?> clazz = loadClassMaybePrivileged(className, classLoader);
		return newInstance(clazz.newInstance(), clazz);
	}

	@Override
	public Object newInstance(final String className,
							  final ClassLoader classLoader) throws IllegalAccessException,
			InvocationTargetException, InstantiationException,
			ClassNotFoundException {
		Class<?> clazz = classLoader.loadClass(className);
		return newInstance(clazz.newInstance(), clazz);
	}

	@Override
	public void newInstance(Object o) throws IllegalAccessException,
			InvocationTargetException {
		newInstance(o, o.getClass());
	}

	@Override
	public Object newInstance(Class<?> clazz) throws IllegalAccessException,
			InvocationTargetException, NamingException, InstantiationException {
		Object instance;
		try {
			instance = newInstance(clazz.getName());
		} catch (ClassNotFoundException e) {
			throw new InstantiationException("can't create object for class "
					+ clazz);
		}
		return newInstance(instance, clazz);
	}

	private Object newInstance(Object instance, Class<?> clazz)
			throws IllegalAccessException, InvocationTargetException {
		Map<String, String> injections = injectionMap.get(clazz.getName());
		populateAnnotationsCache(clazz, injections);
		return instance;
	}

	@Override
	public void destroyInstance(Object instance) throws IllegalAccessException,
			InvocationTargetException {
		preDestroy(instance, instance.getClass());
	}

	/**
	 * Call preDestroy method on the specified instance recursively from deepest
	 * superclass to actual class.
	 *
	 * @param instance object to call preDestroy methods on
	 * @param clazz    (super) class to examine for preDestroy annotation.
	 * @throws IllegalAccessException                      if preDestroy method is inaccessible.
	 * @throws java.lang.reflect.InvocationTargetException if call fails
	 */
	protected void preDestroy(Object instance, final Class<?> clazz)
			throws IllegalAccessException, InvocationTargetException {
		Class<?> superClass = clazz.getSuperclass();
		if (superClass != Object.class) {
			preDestroy(instance, superClass);
		}

		// At the end the postconstruct annotated
		// method is invoked
		List<AnnotationCacheEntry> annotations = null;
		synchronized (annotationCache) {
			annotations = annotationCache.get(clazz);
		}
		if (annotations == null) {
			// instance not created through the instance manager
			return;
		}
		for (AnnotationCacheEntry entry : annotations) {
			if (entry.getType() == AnnotationCacheEntryType.PRE_DESTROY) {
				Method preDestroy = getMethod(clazz, entry);
				synchronized (preDestroy) {
					boolean accessibility = preDestroy.isAccessible();
					preDestroy.setAccessible(true);
					preDestroy.invoke(instance);
					preDestroy.setAccessible(accessibility);
				}
			}
		}
	}

	/**
	 * Make sure that the annotations cache has been populated for the provided
	 * class.
	 *
	 * @param clazz      clazz to populate annotations for
	 * @param injections map of injections for this class from xml deployment
	 *                   descriptor
	 * @throws IllegalAccessException                      if injection target is inaccessible
	 * @throws java.lang.reflect.InvocationTargetException if injection fails
	 */
	protected void populateAnnotationsCache(Class<?> clazz,
											Map<String, String> injections) throws IllegalAccessException,
			InvocationTargetException {

		while (clazz != null) {
			List<AnnotationCacheEntry> annotations = null;
			synchronized (annotationCache) {
				annotations = annotationCache.get(clazz);
			}
			if (annotations == null) {
				annotations = new ArrayList<>();

				// Initialize methods annotations
				Method[] methods = null;
				methods = clazz.getDeclaredMethods();

				Method postConstruct = null;
				Method preDestroy = null;
				for (Method method : methods) {

					if (method.isAnnotationPresent(PostConstruct.class)) {
						if ((postConstruct != null)
								|| (method.getParameterTypes().length != 0)
								|| (Modifier.isStatic(method.getModifiers()))
								|| (method.getExceptionTypes().length > 0)
								|| (!method.getReturnType().getName()
								.equals("void"))) {
							throw new IllegalArgumentException(
									"Invalid PostConstruct annotation");
						}
						postConstruct = method;
					}

					if (method.isAnnotationPresent(PreDestroy.class)) {
						if ((preDestroy != null || method.getParameterTypes().length != 0)
								|| (Modifier.isStatic(method.getModifiers()))
								|| (method.getExceptionTypes().length > 0)
								|| (!method.getReturnType().getName()
								.equals("void"))) {
							throw new IllegalArgumentException(
									"Invalid PreDestroy annotation");
						}
						preDestroy = method;
					}
				}
				if (postConstruct != null) {
					annotations.add(new AnnotationCacheEntry(postConstruct
							.getName(), postConstruct.getParameterTypes(),
							null, AnnotationCacheEntryType.POST_CONSTRUCT));
				}
				if (preDestroy != null) {
					annotations.add(new AnnotationCacheEntry(preDestroy
							.getName(), preDestroy.getParameterTypes(), null,
							AnnotationCacheEntryType.PRE_DESTROY));
				}
				if (annotations.size() == 0) {
					// Use common empty list to save memory
					annotations = Collections.emptyList();
				}
				synchronized (annotationCache) {
					annotationCache.put(clazz, annotations);
				}
			}
			// CHECKSTYLE:OFF
			clazz = clazz.getSuperclass();
			// CHECKSTYLE:ON
		}
	}

	/**
	 * Makes cache size available to unit tests.
	 */
	protected int getAnnotationCacheSize() {
		synchronized (annotationCache) {
			return annotationCache.size();
		}
	}

	protected Class<?> loadClassMaybePrivileged(final String className,
												final ClassLoader classLoader) throws ClassNotFoundException {
		Class<?> clazz;
		if (SecurityUtil.isPackageProtectionEnabled()) {
			try {
				clazz = AccessController
						.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {

							@Override
							public Class<?> run() throws Exception {
								return classLoader.loadClass(className);
							}
						});
			} catch (PrivilegedActionException e) {
				Throwable t = e.getCause();
				if (t instanceof ClassNotFoundException) {
					throw (ClassNotFoundException) t;
				}
				throw new RuntimeException(t);
			}
		} else {
			clazz = classLoader.loadClass(className);
		}
		checkAccess(clazz);
		return clazz;
	}

	private void checkAccess(Class<?> clazz) {
		if (Filter.class.isAssignableFrom(clazz)) {
			checkAccess(clazz, restrictedFilters);
		} else {
			checkAccess(clazz, restrictedListeners);
		}
	}

	private void checkAccess(Class<?> clazz, Properties restricted) {
		while (clazz != null) {
			if ("restricted".equals(restricted.getProperty(clazz.getName()))) {
				throw new SecurityException("Restricted " + clazz);
			}
			// CHECKSTYLE:OFF
			clazz = clazz.getSuperclass();
			// CHECKSTYLE:ON
		}

	}

	public static String getName(Method setter) {
		StringBuilder name = new StringBuilder(setter.getName());

		// remove 'set'
		name.delete(0, 3);

		// lowercase first char
		name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

		return name.toString();
	}

	private static Method getMethod(final Class<?> clazz,
									final AnnotationCacheEntry entry) {
		Method result = null;

		try {
			result = clazz.getDeclaredMethod(entry.getAccessibleObjectName(),
					entry.getParamTypes());
		} catch (NoSuchMethodException e) {
			// Should never happen. On that basis don't log it.
		}
		return result;
	}

	private static final class AnnotationCacheEntry {
		private final String accessibleObjectName;
		private final Class<?>[] paramTypes;
		private final AnnotationCacheEntryType type;

		AnnotationCacheEntry(String accessibleObjectName,
									Class<?>[] paramTypes, String name,
									AnnotationCacheEntryType type) {
			this.accessibleObjectName = accessibleObjectName;
			if (paramTypes != null) {
				this.paramTypes = Arrays.copyOf(paramTypes, paramTypes.length);
			} else {
				this.paramTypes = null;
			}
			this.type = type;
		}

		public String getAccessibleObjectName() {
			return accessibleObjectName;
		}

		public Class<?>[] getParamTypes() {
			return paramTypes;
		}

		public AnnotationCacheEntryType getType() {
			return type;
		}
	}

	private enum AnnotationCacheEntryType {
		FIELD, SETTER, POST_CONSTRUCT, PRE_DESTROY
	}

}
