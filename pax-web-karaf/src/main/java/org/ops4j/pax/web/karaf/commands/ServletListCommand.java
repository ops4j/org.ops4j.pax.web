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
import org.apache.karaf.shell.support.table.ShellTable;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.info.ServletInfo;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;

@Command(scope = "web", name = "servlet-list", description = "Lists all available servlets.")
@Service
public class ServletListCommand extends WebCommand {

	@Override
	public void doExecute(WebContainer container) {
		ReportWebContainerView view = container.adapt(ReportWebContainerView.class);
		if (view == null) {
			System.err.println("Can't obtain a reference to WebContainer/HttpService.");
			return;
		}

		Set<ServletInfo> servlets = view.listServlets();

		final ShellTable table = new ShellTable();
		table.column(new Col("Bundle ID"));
		table.column(new Col("Name"));
		table.column(new Col("Class"));
		table.column(new Col("Context Path(s)"));
		table.column(new Col("URLs"));
		table.column(new Col("Type"));
		table.column(new Col("Context Filter"));

		servlets.forEach(s -> {
			long bundleId = s.getBundle().getBundleId();
			String name = s.getServletName();
			String cls = s.getServletClass();
			String contexts = String.join(", ", s.getContexts());
			String mappings = String.join(", ", s.getMapping());
			String type = s.getType();
			String filter = s.getContextFilter();

			table.addRow().addContent(bundleId, name, cls, contexts, mappings, type, filter);
		});

		table.print(System.out, true);
	}

}
