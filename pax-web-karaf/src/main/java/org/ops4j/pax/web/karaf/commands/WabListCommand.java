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

import java.util.Set;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

@Command(scope = "web", name = "wab-list", description = "Lists all available, deployed Web Application Bundles.")
@Service
public class WabListCommand extends WebCommand {

	@Override
	public void doExecute(WebContainer webContainer) {
		ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);
		if (view == null) {
			System.err.println("Can't obtain a reference to WebContainer/HttpService.");
			return;
		}

		Set<WebApplicationInfo> webapps = view.listWebApplications();

		String url = null;
		if (runtime != null) {
			String base = ((String[])runtime.getRuntimeDTO().serviceDTO.properties.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT))[0];
			base = base.substring(0, base.length() - 1);
			base = base.replace("0.0.0.0", "127.0.0.1");
			url = base;
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
		}

		final ShellTable table = new ShellTable();
		table.column(new Col("Context Path"));
		table.column(new Col("Bundle ID"));
		table.column(new Col("Symbolic Name"));
		table.column(new Col("State"));
		if (url != null) {
			table.column(new Col("Base URL"));
		}

		String finalUrl = url;
		webapps.forEach(app -> {
			if (!app.isWab()) {
				return;
			}
			long bundleId = app.getBundle().getBundleId();
			String symbolicName = app.getBundle().getSymbolicName();
			String contextPath = app.getContextPath();
			String state = app.getDeploymentState();

			Row row = table.addRow();
			row.addContent(contextPath, bundleId, symbolicName, state);
			if (finalUrl != null) {
				row.addContent(finalUrl + contextPath);
			}
		});

		table.print(System.out, true);
	}

}
