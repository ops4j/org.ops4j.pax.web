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
package org.ops4j.pax.web.itest.base.support;

import java.util.Hashtable;
import javax.servlet.Filter;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class FilterBundleActivator implements BundleActivator {

	private ServiceRegistration<Filter> filterReg;

	@Override
	public void start(BundleContext context) throws Exception {

		// register a filter
		Hashtable<String, String> props = new Hashtable<>();
		props.put(PaxWebConstants.SERVICE_PROPERTY_URL_PATTERNS, "/sharedContext/*");
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, "shared");
		props.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_SHARED, "true");
		filterReg = context.registerService(Filter.class,
				new SimpleOnlyFilter(), props);

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (filterReg != null) {
			filterReg.unregister();
		}
	}

}
