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
package org.ops4j.pax.web.service.undertow.internal;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;

import io.undertow.Undertow;

public class UndertowUtil {

	private static final Logger LOG = LoggerFactory.getLogger(UndertowUtil.class);

	public static XnioWorker createWorker(ClassLoader loader) {
		try {
			if (loader == null) {
				loader = Undertow.class.getClassLoader();
			}
			waitForXnioProvider();
			Xnio xnio = Xnio.getInstance(loader);
			return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
		} catch (IOException ignore) {
			LOG.warn("Xnio Worker failed to be created!", ignore);
			return null;
		}
	}

	public static void waitForXnioProvider() {
		try {
			BundleContext context = FrameworkUtil.getBundle(UndertowUtil.class)
					.getBundleContext();
			ServiceTracker<XnioProvider, XnioProvider> tracker = new ServiceTracker<XnioProvider, XnioProvider>(
					context,
					XnioProvider.class,
					null
			);
			tracker.open();
			tracker.waitForService(5000);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

}
