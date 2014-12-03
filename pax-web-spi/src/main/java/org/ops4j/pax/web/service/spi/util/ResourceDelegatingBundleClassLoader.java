/*
 * Copyright 2013 Harald Wellmann
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
package org.ops4j.pax.web.service.spi.util;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bundle class loader which delegates resource loading to a list of delegate
 * bundles.
 * 
 * @author Harald Wellmann
 */
public class ResourceDelegatingBundleClassLoader extends BundleClassLoader {

    private static final Logger LOG = LoggerFactory
			.getLogger(ResourceDelegatingBundleClassLoader.class);
			
	private List<Bundle> bundles;
	
	private static ThreadLocal<MRUCache<String, Enumeration<URL>>> mruCache = new ThreadLocal<MRUCache<String, Enumeration<URL>>>();

	public ResourceDelegatingBundleClassLoader(List<Bundle> bundles) {
		super(bundles.get(0));
		this.bundles = bundles;
	}

	public ResourceDelegatingBundleClassLoader(List<Bundle> bundles,
			ClassLoader parent) {
		super(bundles.get(0), parent);
		this.bundles = bundles;
	}

	public void addBundle(Bundle bundle) {
		bundles.add(bundle);
	}

	public List<Bundle> getBundles() {
		return bundles;
	}

	protected URL findResource(String name) {
		for (Bundle delegate : bundles) {
			try {
				URL resource = delegate.getResource(name);
				if (resource != null) {
					return resource;
				}
			} catch (IllegalStateException exc) {
				// ignore
			}
		}
		return null;
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		Enumeration<URL> url = getFromCahce(name);
		if (url != null) {
			return url;
		} else {
			Vector<URL> resources = new Vector<URL>();

			for (Bundle delegate : bundles) {
				try {
					Enumeration<URL> urls = delegate.getResources(name);
					if (urls != null) {
						while (urls.hasMoreElements()) {
							resources.add(urls.nextElement());
						}
					}
				} catch (IllegalStateException exc) {
					// ignore
				}
			}

			url = resources.elements();
			addToCahce(name, url);
			return url;
		}

	}

	{
		String cacheSizeValue = System.getProperty(
				"pax.web.classloader.cache.size", "500");
		try {
			cacheSize = Long.parseLong(cacheSizeValue);
		} catch (NumberFormatException e) {
			LOG.warn(
					"Properties pax.web.classloader.cache.size: {} is not a valid value.",
					System.getProperty("pax.web.classloader.cache.size"));
		}
	}

	private static long cacheSize = 500;

	protected void addToCahce(String name, Enumeration<URL> urls) {
		if (mruCache.get() == null) {
			mruCache.set(new MRUCache<String, Enumeration<URL>>(cacheSize));
		}

		mruCache.get().put(name, urls);
	}

	protected Enumeration<URL> getFromCahce(String name) {
		Enumeration<URL> url = null;
		if (mruCache.get() != null) {
			url = mruCache.get().get(name);
		}

		return url;
	}

	public class MRUCache<K, V> extends LinkedHashMap<K, V> {

		/**
		 * The default initial capacity - MUST be a power of two.
		 */
		static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

		/**
		 * The load factor used when none specified in constructor.
		 */
		static final float DEFAULT_LOAD_FACTOR = 0.75f;

		public MRUCache(long maxCapacity) {
			super(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, true);

			max = maxCapacity;
		}

		/**
	 * 
	 */
		private static final long serialVersionUID = -3903517976398799492L;

		private static final long MAX_ENTRIES = 100;

		private long max = MAX_ENTRIES;

		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > max;
		}

	}
}