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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@Command(scope = "web", name = "meta", description = "Retrieve meta-information about Pax Web.")
@Service
public class MetaCommand implements Action {

	@Option(name = "--http-service-info", description = "List obtained HttpService/WebContainer instances.")
	private boolean hsInfo = false;

	@Option(name = "--web-usage-count", description = "Get information about usage count of services for bundles using HttpService/WebContainer.")
	private boolean usageCountInfo = false;

	@Option(name = "-v", aliases = { "--verbose" })
	private boolean verbose = false;

	@Reference
	private BundleContext context;

	@Override
	public Object execute() {
		if (hsInfo) {
			ServiceReference<WebContainer> ref = context.getServiceReference(WebContainer.class);
			if (ref == null) {
				System.out.println(">> Can't get a reference to org.osgi.service.http.HttpService.");
				return null;
			}
			Bundle[] bundles = ref.getUsingBundles();
			Map<Long, Bundle> bundlesMap = new TreeMap<>();
			for (Bundle b : bundles) {
				bundlesMap.put(b.getBundleId(), b);
			}

			System.out.println("Registering Bundle: " + ref.getBundle());
			System.out.println("Service ID: " + ref.getProperty(Constants.SERVICE_ID));
			System.out.println("Service Scope: " + ref.getProperty(Constants.SERVICE_SCOPE));
			System.out.println();

			System.out.println("Bundles referencing the service:");
			final ShellTable table = new ShellTable();
			table.column(new Col("Bundle ID"));
			table.column(new Col("Symbolic Name"));
			table.column(new Col("Version"));
			table.column(new Col("Instance ID"));
			table.column(new Col("WebContainer"));
			bundlesMap.values().forEach(b -> {
				WebContainer wcForBundle = b.getBundleContext().getService(ref);
				table.addRow().addContent(b.getBundleId(), b.getSymbolicName(), b.getVersion(),
						String.format("0x%x", System.identityHashCode(wcForBundle)), wcForBundle);
				b.getBundleContext().ungetService(ref);
			});
			table.print(System.out);
			System.out.println();

			if (verbose) {
				System.out.println("Service Registration Properties:");
				ShellTable table2 = new ShellTable();
				table2.column(new Col("Name"));
				table2.column(new Col("Value"));
				for (String key : ref.getPropertyKeys()) {
					Object v = ref.getProperty(key);
					if (v instanceof String[]) {
						v = "[" + String.join(", ", (String[]) v) + "]";
					}
					table2.addRow().addContent(key, v);
				}
				table2.print(System.out);
				System.out.println();
			}
		}

		if (usageCountInfo) {
			Set<Bundle> bundles = new TreeSet<>();
			for (Bundle b : context.getBundles()) {
				if ("org.ops4j.pax.web.pax-web-extender-war".equals(b.getSymbolicName())
						|| "org.ops4j.pax.web.pax-web-extender-whiteboard".equals(b.getSymbolicName())
						|| "org.ops4j.pax.web.pax-web-karaf".equals(b.getSymbolicName())
				) {
					// some bundles which we always want to show
					bundles.add(b);
				}
			}
			ServiceReference<WebContainer> ref = context.getServiceReference(WebContainer.class);
			if (ref == null) {
				System.out.println(">> Can't get a reference to org.osgi.service.http.HttpService.");
				return null;
			}

			Map<Long, Bundle> bundlesMap = new TreeMap<>();
			Collections.addAll(bundles, ref.getUsingBundles());
			System.out.println("Registering Bundle: " + ref.getBundle());
			System.out.println("Service ID: " + ref.getProperty(Constants.SERVICE_ID));
			System.out.println("Service Scope: " + ref.getProperty(Constants.SERVICE_SCOPE));
			System.out.println();

			System.out.println("Usage Counts for bundles referencing the service:");
			final ShellTable table = new ShellTable();
			table.column(new Col("Bundle ID"));
			table.column(new Col("Symbolic Name"));
			table.column(new Col("Service ID"));
			table.column(new Col("Scope"));
			table.column(new Col("Service objectClass"));
			table.column(new Col("Usage Count"));

			try {
				Field fFelix = context.getClass().getDeclaredField("m_felix");
				fFelix.setAccessible(true);
				Object felix = fFelix.get(context);
				Field fRegistry = felix.getClass().getDeclaredField("m_registry");
				fRegistry.setAccessible(true);
				Object registry = fRegistry.get(felix);
				Field fInUseMap = registry.getClass().getDeclaredField("m_inUseMap");
				fInUseMap.setAccessible(true);
				@SuppressWarnings("unchecked")
				Map<Bundle, Object> inUseMap = (Map<Bundle, Object>) fInUseMap.get(registry);

				for (Bundle b : bundles) {
					Object[] usageCountArray = (Object[]) inUseMap.get(b);
					if (usageCountArray != null) {
						for (Object uc : usageCountArray) {
							Field fCount = uc.getClass().getDeclaredField("m_count");
							fCount.setAccessible(true);
							AtomicLong count = (AtomicLong) fCount.get(uc);
							Field fRef = uc.getClass().getDeclaredField("m_ref");
							fRef.setAccessible(true);
							ServiceReference<?> sr = (ServiceReference<?>) fRef.get(uc);
							table.addRow().addContent(b.getBundleId(), b.getSymbolicName(),
									sr.getProperty(Constants.SERVICE_ID), sr.getProperty(Constants.SERVICE_SCOPE),
									"[" + String.join(", ", Arrays.asList((String[]) sr.getProperty(Constants.OBJECTCLASS))) + "]",
									count == null ? "-" : count.get());
						}
					}
				}
				table.print(System.out);
			} catch (Exception e) {
				System.err.println("Can't use reflection to access usage count data: " + e.getMessage());
				return null;
			}
		}

		return null;
	}

}
