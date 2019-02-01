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
import java.util.List;

import org.ops4j.pax.web.service.spi.internal.InternalDelegatingBundleClassLoader;
import org.osgi.framework.Bundle;

/**
 * A bundle class loader which delegates resource loading to a list of delegate
 * bundles.
 * It's part of exported API of pax-web-spi bundle. Delegates to {@code InternalDelegatingBundleClassLoader}
 * which uses private-packaged swissbox classes.
 *
 * @author Harald Wellmann
 */
public class ResourceDelegatingBundleClassLoader extends ClassLoader {

	private final InternalDelegatingBundleClassLoader delegate;

	public ResourceDelegatingBundleClassLoader(List<Bundle> bundles) {
		delegate = new InternalDelegatingBundleClassLoader(bundles);
	}

	public ResourceDelegatingBundleClassLoader(List<Bundle> bundles, ClassLoader parent) {
		delegate = new InternalDelegatingBundleClassLoader(bundles, parent);
	}

	public void addBundle(Bundle bundle) {
		delegate.addBundle(bundle);
	}

	public List<Bundle> getBundles() {
		return delegate.getBundles();
	}

	protected URL findResource(String name) {
		return delegate.findResource(name);
	}

	@Override
	protected Enumeration<URL> findResources(String name) throws IOException {
		return delegate.findResources(name);
	}

}
