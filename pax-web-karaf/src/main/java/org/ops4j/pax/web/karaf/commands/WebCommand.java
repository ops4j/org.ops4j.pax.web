/*
 * Copyright 2021 OPS4J.
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
package org.ops4j.pax.web.karaf.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.runtime.HttpServiceRuntime;

public abstract class WebCommand implements Action {

	protected HttpServiceRuntime runtime;

	@Reference
	private BundleContext context;

	@Override
	public final Object execute() {
		ServiceReference<WebContainer> ref = context.getServiceReference(WebContainer.class);
		if (ref == null) {
			System.err.println(">> Can't get a reference to org.osgi.service.http.HttpService.");
			return null;
		}
		WebContainer service = context.getService(ref);
		if (service == null) {
			System.out.println(">> Can't get a HttpService instance from ServiceReference " + ref + " for bundle " + context.getBundle() + ".");
			return null;
		}

		ServiceReference<HttpServiceRuntime> rref = context.getServiceReference(HttpServiceRuntime.class);
		if (rref != null) {
			runtime = context.getService(rref);
		}

		try {
			doExecute(service);
		} finally {
			context.ungetService(ref);
			if (rref != null) {
				context.ungetService(rref);
			}
		}

		return null;
	}

	public abstract void doExecute(WebContainer container);

}
