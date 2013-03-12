/*
 * Copyright 2009 Alin Dreghiciu.
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
package org.ops4j.pax.web.deployer.internal;

import java.util.Hashtable;

import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * WAR deployer activator
 * 
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.7.0, July 31, 2009
 */
public class Activator implements BundleActivator {

	public void start(final BundleContext bundleContext) throws Exception {
		bundleContext.registerService(
				new String[] { ArtifactUrlTransformer.class.getName(),
						ArtifactListener.class.getName() }, new WarDeployer(),
				new Hashtable<String, Object>());
	}

	public void stop(final BundleContext bundleContext) throws Exception {
		// Automatically unregistered
	}

}