/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.undertow.bundle.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Grzegorz Grzybek
 */
public class CompositeActivator implements BundleActivator {

	private final BundleActivator paxWebActivator;
	private final BundleActivator undertowActivator;
	private final BundleActivator xnioActivator;

	public CompositeActivator() {
		paxWebActivator = new org.ops4j.pax.web.service.internal.Activator();
		undertowActivator = new org.ops4j.pax.web.service.undertow.internal.Activator();
		xnioActivator = new org.xnio.nio.OsgiActivator();
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		xnioActivator.start(bundleContext);
		undertowActivator.start(bundleContext);
		paxWebActivator.start(bundleContext);
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		paxWebActivator.stop(bundleContext);
		undertowActivator.stop(bundleContext);
		xnioActivator.stop(bundleContext);
	}

}
