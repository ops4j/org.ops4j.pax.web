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
package org.ops4j.pax.web.service.spi.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.commons.collections4.map.LRUMap;
import org.ops4j.pax.swissbox.core.BundleClassLoader;
import org.osgi.framework.Bundle;

/**
 * A bundle class loader which delegates resource loading to a list of delegate
 * bundles.
 *
 * @author Harald Wellmann
 */
public class InternalDelegatingBundleClassLoader extends BundleClassLoader {

	private List<Bundle> bundles;

	private int cacheSize = 100; //equals the default size of the LRUMap, might be changed in a later version.

	private LRUMap<String, Vector<URL>> lruCache = new LRUMap<>(cacheSize);

	public InternalDelegatingBundleClassLoader(List<Bundle> bundles) {
		super(bundles.get(0));
		this.bundles = bundles;
	}

	public InternalDelegatingBundleClassLoader(List<Bundle> bundles, ClassLoader parent) {
		super(bundles.get(0), parent);
		this.bundles = bundles;
	}

	public void addBundle(Bundle bundle) {
		bundles.add(bundle);
		lruCache.clear();
	}

	public List<Bundle> getBundles() {
		return bundles;
	}

	public URL findResource(String name) {
		Vector<URL> resources = getFromCache(name);

		if (resources == null) {
			resources = new Vector<>();
			for (Bundle delegate : bundles) {
				try {
					URL resource = delegate.getResource(name);
					if (resource != null) {
						resources.add(resource);
						break;
					}
				} catch (IllegalStateException exc) {
					// ignore
				}
			}
			if (!resources.isEmpty()) {
				addToCache(name, resources);
			}
		}

		Enumeration<URL> elements = resources.elements();
		if (elements.hasMoreElements()) {
			return elements.nextElement();
		}
		return null;
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		Vector<URL> resources = getFromCache(name);

		if (resources == null) {
			resources = new Vector<>();
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
			if (!resources.isEmpty()) {
				addToCache(name, resources);
			}
		}

		return resources.elements();
	}

	protected synchronized void addToCache(String name, Vector<URL> resources) {
		lruCache.put(name, resources);
	}

	protected Vector<URL> getFromCache(String name) {
		return lruCache.get(name);
	}
}