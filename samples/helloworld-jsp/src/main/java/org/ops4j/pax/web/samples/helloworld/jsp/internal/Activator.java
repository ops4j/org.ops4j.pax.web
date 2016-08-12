/* Copyright 2008 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.samples.helloworld.jsp.internal;

import java.net.URL;
import java.util.Enumeration;

import javax.servlet.Servlet;

import org.apache.jasper.compiler.JspUtil;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 08, 2007
 */
public final class Activator implements BundleActivator {

	private static final String JSP = "/helloworld/jsp";
	private static final String JSPC = JSP + 'c';
	private static final String DEFAULT_PACKAGE = "org.apache.jsp.";
	/**
	 * WebContainer reference.
	 */
	private ServiceReference<WebContainer> webContainerRef;

	/**
	 * Called when the OSGi framework starts our bundle
	 */
	public void start(BundleContext bc) throws Exception {
		webContainerRef = bc.getServiceReference(WebContainer.class);
		if (webContainerRef != null) {
			final WebContainer webContainer = bc.getService(webContainerRef);
			if (webContainer != null) {
				// create a default context to share between registrations
				final HttpContext httpContext = webContainer
						.createDefaultHttpContext();
				// webContainer.begin(httpContext);
				// register jsp support
				Bundle bundle = bc.getBundle();
				Enumeration<?> entries = bundle.findEntries(JSP, "*", true);
				if (entries != null) {
					while (entries.hasMoreElements()) {
						URL entry = (URL) entries.nextElement();
						String jspFile = entry.toExternalForm().substring(
								entry.toExternalForm().lastIndexOf('/') + 1);
						String urlPattern = JSPC + "/" + jspFile;
						String jspcClassName = DEFAULT_PACKAGE
								+ convertPath(entry.toExternalForm()) + "."
								+ JspUtil.makeJavaIdentifier(jspFile);
						@SuppressWarnings("unchecked")
						Class<Servlet> precompiledClass = (Class<Servlet>) getClass()
								.getClassLoader().loadClass(jspcClassName);
						webContainer.registerServlet(precompiledClass,
								new String[]{urlPattern}, null, httpContext);
					}
				}
				webContainer.registerJsps(new String[]{JSP + "/*"}, // url
						// patterns
						httpContext // http context
				);
				// register images as resources
				webContainer.registerResources("/images", "/images",
						httpContext);
				// webContainer.end(httpContext);
			}
		}
	}

	private String convertPath(String jspPath) {
		// String path =
		// jspPath.replaceFirst("bundle\\:\\/\\/\\d*\\.\\d*.\\d\\/", "");
		String path = jspPath.replaceFirst(
				"(bundle.*://\\d*\\.)((\\w*)|(\\d*\\:\\d*))/", "");
		path = path.substring(0, path.lastIndexOf('/'));
		path = path.replace("/", ".");
		return path;
	}

	/**
	 * Called when the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if (webContainerRef != null) {
			bc.ungetService(webContainerRef);
			webContainerRef = null;
		}
	}
}