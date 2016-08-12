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
package org.ops4j.pax.web.service.tomcat.internal;

import org.ops4j.pax.web.service.spi.ServerControllerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Romaim Gilles
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(ServerControllerFactory.class,
				TomcatServerControllerFactory
						.newInstance(TomcatServerStateFactory
								.newInstance(new TomcatServerFactory())), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// To change body of implemented methods use File | Settings | File
		// Templates.
	}
}
