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

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */
public final class Activator implements BundleActivator {
	/**
	 * WebContainer reference.
	 */
	private ServiceReference<WebContainer> webContainerRef;
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
		/*
		 * The pax-web-war service can take a little longer to start, we can't
		 * say how much it will take, so we do need to sit down a while and wait
		 * it's availability in order to use it's reference.
		 *
		 * This is a MUST, it's really important - mostly when using the
		 * config.ini file (Equinox).
		 *
		 * Anaximandro April 19, 2010.
		 */
		int counter = 0;
		boolean started = false;
		while (!started) {

			webContainerRef = bc.getServiceReference(WebContainer.class);
			started = webContainerRef != null;
			if (started) {
				final WebContainer webContainer = bc
						.getService(webContainerRef);
				if (webContainer != null) {
					httpContext = webContainer
							.createDefaultHttpContext();
					// set a session timeout of 10 minutes
			//					webContainer.setSessionTimeout(10, httpContext);
					// register the hello world servlet for filtering with url
					// pattern
					final Dictionary<String, String> initParamsServlet = new Hashtable<>();
					initParamsServlet.put("from", "WebContainer");
					helloWorldServlet = new HelloWorldServlet();
					webContainer.registerServlet(helloWorldServlet, // registered
							// servlet
							new String[]{"/helloworld/wc"}, // url patterns
							initParamsServlet, // init params
							httpContext // http context
					);
					// register the hello world filter based on url paterns
					final Dictionary<String, String> initParamsFilter = new Hashtable<>();
					initParamsFilter.put("title", "Hello World (url pattern)");
					helloWorldFilter = new HelloWorldFilter();
					webContainer.registerFilter(helloWorldFilter, // registered
							// filter
							new String[]{"/helloworld/wc"}, // url patterns
							null, // servlet names
							initParamsFilter, // init params
							httpContext // http context
					);
					worldServlet = new HelloWorldServlet();
					webContainer.registerServlet(worldServlet, // registered
							// servlet
							"HelloWorld", // servlet name
							new String[]{"/helloworld/wc/sn"}, // url
							// patterns
							initParamsServlet, // init params
							httpContext // http context
					);
					// register the hello world filter based on servlet name
					initParamsFilter.put("title", "Hello World (servlet name)");
					webContainer.registerFilter(new HelloWorldFilter(), // registered
							// filter
							null, // url patterns
							new String[]{"HelloWorld"}, // servlet names
							initParamsFilter, // init params
							httpContext // http context
					);
					helloWorldListener = new HelloWorldListener();
					webContainer.registerEventListener(
							helloWorldListener, // registered request
							// listener
							httpContext // http context
					);
					sessionListener = new HelloWorldSessionListener();
					webContainer.registerEventListener(
							sessionListener, // registered
							// session
							// listener
							httpContext // http context
					);
					// register images as resources
					webContainer.registerResources("/images", "/images",
							httpContext);
			//					// register a welcome file - should be used for ALL resource servlets
			//					// - default and non default
			//					webContainer.registerWelcomeFiles(
			//							new String[] { "index.html" }, true, httpContext);
					// register static htmls
					webContainer.registerResources("/html", "/html",
							httpContext);
					errorServlet = new HelloWorldErrorServlet();
					webContainer.registerServlet(errorServlet, // registered
							// servlet
							new String[]{"/helloworld/wc/error"}, // url
							// patterns
							null, // no init params
							httpContext // http context
					);
					errorMakerServlet = new HelloWorldErrorMakerServlet();
					webContainer.registerServlet(
							errorMakerServlet, // registered
							// servlet
							new String[]{"/helloworld/wc/error/create"}, // url
							// patterns
							null, // no init params
							httpContext // http context
					);
			//					// register error page for any Exception
			//					webContainer.registerErrorPage("java.lang.Exception", // fully
			//							// qualified
			//							// name
			//							"/helloworld/wc/error", // path to error servlet
			//							httpContext // http context
			//					);
			//					// register error page for 404 (Page not found)
			//					webContainer.registerErrorPage("404", // error code
			//							"/helloworld/wc/error", // path to error servlet
			//							httpContext // http context
			//					);

					webContainer.end(httpContext);
				}
			} else {
				// wait, throw exception after 5 retries.
				if (counter > 10) {
					throw new Exception(
							"Could not start the helloworld-wc service, WebContainer service not started or not available.");
				} else {
					counter++;
					Thread.sleep(counter * 1000);
				}
			}
		}
	}

	/**
	 * Called when the OSGi framework stops our bundle
	 */
	public void stop(BundleContext bc) throws Exception {
		if (webContainerRef != null) {
			WebContainer webContainer = (WebContainer) bc
					.getService(webContainerRef);

			webContainer.unregisterServlet(helloWorldServlet);
			webContainer.unregisterFilter(helloWorldFilter);

			webContainer.unregisterFilter("HelloWorld");
			webContainer.unregisterServlet(worldServlet);

			webContainer.unregisterEventListener(helloWorldListener);
			webContainer.unregisterEventListener(sessionListener);

			webContainer.unregister("/images");
			webContainer.unregister("/html");
			webContainer.unregisterServlet(errorServlet);
			webContainer.unregisterServlet(errorMakerServlet);

			//			webContainer.unregisterErrorPage("java.lang.Exception", httpContext);
			//			webContainer.unregisterErrorPage("404", httpContext);
			//
			//			webContainer.unregisterWelcomeFiles(new String[]{"index.html"}, httpContext);

			webContainer = null;

			bc.ungetService(webContainerRef);
			webContainerRef = null;
		}
	}
}