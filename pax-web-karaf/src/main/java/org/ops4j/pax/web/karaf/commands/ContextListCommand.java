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

import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;

@Command(scope = "web", name = "context-list", description = "Lists all available web contexts. These may originate from HttpService, Whiteboard or WAB Extender.")
@Service
public class ContextListCommand extends WebCommand {

	@Override
	public void doExecute(WebContainer container) {
		ReportWebContainerView view = container.adapt(ReportWebContainerView.class);
		if (view == null) {
			System.err.println("Can't obtain a reference to WebContainer/HttpService.");
			return;
		}

		Set<WebApplicationInfo> webapps = view.listWebApplications();

		final ShellTable table = new ShellTable();
		table.column(new Col("Bundle ID"));
		table.column(new Col("Symbolic Name"));
		table.column(new Col("Context Path"));
		table.column(new Col("Context Name"));
		table.column(new Col("Rank"));
		table.column(new Col("Service ID"));
		table.column(new Col("Type"));
		table.column(new Col("Scope"));
		table.column(new Col("Registration Properties"));

		final boolean[] hadStaticScope = { false };
		final boolean[] wasReplaced = { false };

		webapps.forEach(app -> {
			long bundleId = app.getBundle().getBundleId();
			String symbolicName = app.getBundle().getSymbolicName();
			String contextPath = app.getContextPath();
			String name = app.getName();
			String rank = app.getServiceRank() == Integer.MAX_VALUE ? "MAX" : Integer.toString(app.getServiceRank());
			long sid = app.getServiceId();
			String type;
			if (app.isWab()) {
				type = "WAB";
			} else if (app.isWhiteboard()) {
				type = "Whiteboard";
			} else {
				if (app.isReplaced()) {
					type = "HttpService+";
					wasReplaced[0] = true;
				} else {
					type = "HttpService";
				}
			}
			String scope = app.getScope();
			hadStaticScope[0] |= scope.endsWith("*");
			List<String> props = app.getContextRegistrationIdProperties();

			if (props.isEmpty()) {
				table.addRow().addContent(bundleId, symbolicName, contextPath, name, rank, sid, type, scope, "-");
			} else {
				final int[] n = { 0 };
				props.forEach(p -> {
					if (n[0]++ == 0) {
						table.addRow().addContent(bundleId, symbolicName, contextPath, name, rank, sid, type, scope, p);
					} else {
						table.addRow().addContent("", "", "", "", "", "", "", "", p);
					}
				});
			}
		});

		table.print(System.out, true);
		if (hadStaticScope[0]) {
			System.out.println("\n*) This context is using ServletContextHelper/HttpContext without resolving an org.osgi.framework.ServiceReference.");
		}
		if (wasReplaced[0]) {
			System.out.println("\n+) This context is HttpService related, but was shadowed by special Pax-Web Whiteboard registration (the only way to alter HttpContext's context path).");
		}
	}

}
