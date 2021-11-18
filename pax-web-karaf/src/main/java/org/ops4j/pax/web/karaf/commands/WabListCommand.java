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

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.WebApplicationModel;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;
import org.osgi.framework.Bundle;

@Command(scope = "web", name = "wab-list", description = "Lists all available Web Application Bundles.")
@Service
public class WabListCommand implements Action {

	@Reference
	private WebContainer webContainer;

	@Override
	public Object execute() {
		ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);
		if (view == null) {
			System.err.println("Can't obtain a reference to WebContainer/HttpService.");
			return null;
		}

		List<WebApplicationModel> webapps = view.listWebApplications();

		final ShellTable table = new ShellTable();
		table.column(new Col("Bundle ID"));
		table.column(new Col("Symbolic Name"));
		table.column(new Col("Context Path"));
		table.column(new Col("State"));

		webapps.forEach(app -> {
			long bundleId = app.getBundle().getBundleId();
			String symbolicName = app.getBundle().getSymbolicName();
			String contextPath = app.getContextPath();
			String state = app.getDeploymentState();
//			String applicationFragments = app.getApplicationFragmentBundles().stream()
//					.map(Bundle::getSymbolicName).collect(Collectors.joining("\n"));
//			String containerFragments = app.getContainerFragmentBundles().stream()
//					.map(Bundle::getSymbolicName).collect(Collectors.joining("\n"));
//			String classPath = app.getWabClassPath().stream().map(URL::getPath).collect(Collectors.joining("\n"));

			table.addRow().addContent(bundleId, symbolicName, contextPath, state);
		});

		table.print(System.out, true);
		return null;
	}

}
