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
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */
public final class Activator implements BundleActivator, ServiceTrackerCustomizer<WebContainer, WebContainer> {

	private BundleContext bundleContext;

	private ServiceTracker<WebContainer, WebContainer> tracker;

	private HelloWorldServlet helloWorldServlet;
	private HelloWorldFilter helloWorldFilter;
	private HelloWorldListener helloWorldListener;
	private HelloWorldSessionListener sessionListener;
	private HttpContext httpContext;
	private HelloWorldServlet worldServlet;
	private HelloWorldErrorServlet errorServlet;
	private HelloWorldErrorMakerServlet errorMakerServlet;

	/**
	 * Called when the OSGi framework starts our bundle.
	 */
	public void start(BundleContext bc) throws Exception {
		bundleContext = bc;
		tracker = new ServiceTracker<>(bc, WebContainer.class, this);
		tracker.open();
	}

	/**
	 * Called when the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		WebContainer webContainer = tracker.getService();
		if (webContainer != null) {
			webContainer.unregisterServlet(helloWorldServlet);
			webContainer.unregisterFilter(helloWorldFilter);

			webContainer.unregisterFilter("HelloWorldFilter");
			webContainer.unregisterServlet(worldServlet);

			webContainer.unregisterEventListener(helloWorldListener);
			webContainer.unregisterEventListener(sessionListener);

			webContainer.unregister("/images");
			webContainer.unregister("/html");
			webContainer.unregisterServlet(errorServlet);
			webContainer.unregisterServlet(errorMakerServlet);

			webContainer.unregisterErrorPage("java.lang.Exception", httpContext);
			webContainer.unregisterErrorPage("404", httpContext);

			webContainer.unregisterWelcomeFiles(new String[] { "index.html" }, httpContext);
		}
		tracker.close();
	}

	@Override
	public WebContainer addingService(ServiceReference<WebContainer> reference) {
		final WebContainer webContainer = bundleContext.getService(reference);
		if (webContainer != null) {
			try {
				// not actually needed, because passing null to registerXXX() methods will do the same
				httpContext = webContainer.createDefaultHttpContext();

				// set a session timeout of 10 minutes
				webContainer.setSessionTimeout(10, httpContext);

				// register the hello world servlet for filtering with url pattern
				// no name is passed, so FQCN will be used as servlet name
				final Dictionary<String, String> initParamsServlet = new Hashtable<>();
				initParamsServlet.put("from", "WebContainer");
				helloWorldServlet = new HelloWorldServlet();
				webContainer.registerServlet(helloWorldServlet, new String[] { "/helloworld/wc" },
						initParamsServlet, httpContext);

				// register the hello world filter based on url paterns
				final Dictionary<String, String> initParamsFilter = new Hashtable<>();
				initParamsFilter.put("title", "Hello World (url pattern)");
				helloWorldFilter = new HelloWorldFilter();
				webContainer.registerFilter(helloWorldFilter, new String[] { "/helloworld/wc" }, null,
						initParamsFilter, httpContext);

				worldServlet = new HelloWorldServlet();
				webContainer.registerServlet(worldServlet, "HelloWorld", new String[] { "/helloworld/wc/sn" },
						initParamsServlet, httpContext);

				// register the hello world filter based on servlet name
				// (we need name, otherwise it'd be registered as disabled, because there's already
				// a filter with same FQCN)
				initParamsFilter.put("title", "Hello World (servlet name)");
				webContainer.registerFilter(new HelloWorldFilter(), "HelloWorldFilter", null,
						new String[] { "HelloWorld" }, initParamsFilter, true, httpContext);

				helloWorldListener = new HelloWorldListener();
				webContainer.registerEventListener(helloWorldListener, httpContext);

				sessionListener = new HelloWorldSessionListener();
				webContainer.registerEventListener(sessionListener, httpContext);

				// register images as resources
				webContainer.registerResources("/images", "/images", httpContext);

				// register a welcome file - should be used for ALL resource servlets - default and non default
				webContainer.registerWelcomeFiles(new String[] { "index.html" }, true, httpContext);
				// register static htmls

				webContainer.registerResources("/html", "/html", httpContext);

				errorServlet = new HelloWorldErrorServlet();
				webContainer.registerServlet(errorServlet, new String[] { "/helloworld/wc/error" },
						null, httpContext);

				errorMakerServlet = new HelloWorldErrorMakerServlet();
				webContainer.registerServlet(errorMakerServlet, new String[] { "/helloworld/wc/error/create" },
						null, httpContext);

				// register error page for any Exception
				webContainer.registerErrorPage("java.lang.Exception", "/helloworld/wc/error", httpContext);
				// register error page for 404 (Page not found)
				webContainer.registerErrorPage("404", "/helloworld/wc/error", httpContext);

				webContainer.end(httpContext);
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		return webContainer;
	}

	@Override
	public void modifiedService(ServiceReference<WebContainer> reference, WebContainer service) {
		// ignore
	}

	@Override
	public void removedService(ServiceReference<WebContainer> reference, WebContainer webContainer) {
		// we don't have to unregister in removedService(), because it'll be cleaned anyway
//		webContainer.unregisterServlet(helloWorldServlet);
//		webContainer.unregisterFilter(helloWorldFilter);
//
//		webContainer.unregisterFilter("HelloWorldFilter");
//		webContainer.unregisterServlet(worldServlet);
//
//		webContainer.unregisterEventListener(helloWorldListener);
//		webContainer.unregisterEventListener(sessionListener);
//
//		webContainer.unregister("/images");
//		webContainer.unregister("/html");
//		webContainer.unregisterServlet(errorServlet);
//		webContainer.unregisterServlet(errorMakerServlet);
//
//		webContainer.unregisterErrorPage("java.lang.Exception", httpContext);
//		webContainer.unregisterErrorPage("404", httpContext);
//
//		webContainer.unregisterWelcomeFiles(new String[] { "index.html" }, httpContext);

		bundleContext.ungetService(reference);
	}

}
