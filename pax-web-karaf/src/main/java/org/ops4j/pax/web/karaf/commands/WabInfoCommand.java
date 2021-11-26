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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.ShellUtil;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.WebApplicationModel;
import org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView;
import org.osgi.framework.Bundle;

@Command(scope = "web", name = "wab-info", description = "Shows information about Web Application Bundle.")
@Service
public class WabInfoCommand extends WebCommand {

	@Argument(name = "wab", description = "WAB specified by context path or bundle ID")
	private Object wab;

	@Override
	public void doExecute(WebContainer webContainer) {
		ReportWebContainerView view = webContainer.adapt(ReportWebContainerView.class);
		if (view == null) {
			System.err.println("Can't obtain a reference to WebContainer/HttpService.");
			return;
		}
		if (wab == null) {
			System.err.println("Please specify the WAB using context path or bundle ID");
			return;
		}

		long bundleId = -1L;
		WebApplicationModel app;
		if (wab.toString().startsWith("/")) {
			app = view.getWebApplication(wab.toString());
		} else {
			try {
				bundleId = Long.parseLong(wab.toString());
				app = view.getWebApplication(bundleId);
			} catch (NumberFormatException e) {
				System.err.println("Can't parse \"" + wab.toString() + "\" as bundle ID.");
				return;
			}
		}

		if (app == null) {
			if (bundleId >= 0) {
				System.err.println("Can't find Web Application Bundle with bundle ID = " + bundleId + ".");
			} else {
				System.err.println("Can't find Web Application Bundle with context path = \"" + wab + "\".");
			}
			return;
		}

		String title = ShellUtil.getBundleName(app.getBundle());
		System.out.println("\n" + title);
		System.out.println(ShellUtil.getUnderlineString(title));

		System.out.println("Context Path: " + app.getContextPath());
		System.out.println("Deployment State: " + app.getDeploymentState());
		List<URL> wabClassPath = app.getWabClassPath();
		if (!wabClassPath.isEmpty()) {
			System.out.println("WAB ClassPath:");
			wabClassPath.forEach(url -> {
				System.out.println(" - " + url);
			});
		}
		List<String> scis = app.getServletContainerInitializers();
		if (!scis.isEmpty()) {
			System.out.println("ServletContainerInitializers:");
			scis.forEach(sci -> {
				System.out.println(" - " + sci);
			});
		}
		List<URL> resources = app.getMetaInfResources();
		if (!resources.isEmpty()) {
			System.out.println("Available /META-INF/resources:");
			resources.forEach(r -> {
				System.out.println(" - " + r);
			});
		}
		List<Bundle> containerBundles = app.getContainerFragmentBundles();
		if (!containerBundles.isEmpty()) {
			System.out.println("Container web fragments (reachable bundles without /META-INF/web-fragment.xml):");
			containerBundles.forEach(b -> {
				System.out.println(" - (" + b.getBundleId() + ") " + b.getSymbolicName() + "/" + b.getVersion());
			});
		}
		List<Bundle> appBundles = app.getApplicationFragmentBundles();
		if (!appBundles.isEmpty()) {
			System.out.println("Application web fragments (reachable bundles containing /META-INF/web-fragment.xml):");
			appBundles.forEach(b -> {
				System.out.println(" - (" + b.getBundleId() + ") " + b.getSymbolicName() + "/" + b.getVersion());
			});
		}
	}

}
