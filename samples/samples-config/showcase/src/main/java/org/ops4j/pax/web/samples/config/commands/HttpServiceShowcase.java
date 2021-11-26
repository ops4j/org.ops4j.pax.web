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
package org.ops4j.pax.web.samples.config.commands;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.ops4j.pax.web.samples.config.commands.web.TestServlet;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.HttpServiceRuntimeConstants;

@Command(scope = "sample", name = "hs", description = "Registration/Unregistration using pure HttpService")
@Service
public class HttpServiceShowcase implements Action {

	@Option(name = "-d", description = "Delete (unregister) web elements registered by previous action.")
	private boolean unregister = false;

	@Option(name = "-b", description = "Bundle id for which HttpService instance should be retrieved (defaults to samples/showcase bundle).")
	private long bundleId = -1L;

	@Argument(name = "action", index = 0, description = "One of known scenarios (see source code) to check.")
	private String action = "help";

	@Argument(name = "arg1", index = 1, description = "Argument 1")
	private String arg1;

	@Argument(name = "arg2", index = 2, description = "Argument 2")
	private String arg2;

	@Reference
	private BundleContext context;

	@Override
	public Object execute() {
		if ("help".equals(action)) {
			System.out.println("This internal command allows testing several HttpService scenarios.");
			System.out.println("For example, we can register a servlet using HttpService for bundle ID 42 using:\n\n" +
					"\tsample:hs -b 42 servlet /s1\n\n" +
					"This will register a servlet under /s1 alias (which means /s1/* mapping) using:\n\n" +
					"\torg.osgi.service.http.HttpService.registerServlet(\"/s1\", new TestServlet(), null, null);\n\n" +
					"To unregister the servlet, call:\n\n" +
					"\tsample:hs -b 42 -d servlet /s1\n\n" +
					"Supported actions:\n" +
					" - servlet alias [context-name] - register a servlet under given alias and optionally for \"context-name\" (defaults to \"default\")\n" +
					" - -d servlet alias - unregister given alias\n" +
					" - context name [path] - call org.ops4j.pax.web.service.WebContainer.createDefaultHttpContext(\"name\")\n" +
					"   optionally doing the Pax-Web magic to reconfigure context path of the HttpService context.\n" +
					"   (note: there's no way to remove such context)\n" +
					" - service - just obtain a HttpService for a given bundle\n" +
					" - -d service - unget HttpService for a given bundle (decrementing the usage count)");
			return null;
		}

		if (bundleId != -1L) {
			context = context.getBundle(bundleId).getBundleContext();
		}
		System.out.println(">> Using context for bundle " + context.getBundle());

		ServiceReference<HttpService> ref = context.getServiceReference(HttpService.class);
		if (ref == null) {
			System.out.println(">> Can't get a reference to org.osgi.service.http.HttpService.");
			return null;
		}

		switch (action) {
			case "service": {
				if (!unregister) {
					HttpService service = context.getService(ref);
					if (service == null) {
						System.out.println(">> Can't get a HttpService instance from ServiceReference " + ref + " for bundle " + context.getBundle() + ".");
						return null;
					}
					System.out.println(">> Obtained HttpService instance: " + service + ".");
					System.out.println(">> NOTES:");
					System.out.println(">>  - simple getService() doesn't create any context - it's created when calling method like httpService.registerServlet() or creating a context using org.ops4j.pax.web.service.WebContainer.createDefaultHttpContext(\"name\")");
				} else {
					context.ungetService(ref);
					System.out.println(">> Called org.osgi.framework.BundleContext.ungetService() on ServiceReference " + ref + " for bundle " + context.getBundle() + ".");
				}
				break;
			}
			case "context": {
				if (unregister) {
					System.err.println("We won't unregister the context, because it's not possible.");
					return null;
				}
				if (arg1 == null || "".equals(arg1.trim())) {
					arg1 = "default";
				}
				WebContainer service = (WebContainer) context.getService(ref);
				System.out.println(">> Calling org.ops4j.pax.web.service.WebContainer.createDefaultHttpContext(\"" + arg1 + "\").");
				HttpContext ctx = service.createDefaultHttpContext(arg1);
				System.out.println(">> Got context: " + ctx);
				if (arg2 != null && !"/".equals(arg2.trim())) {
					System.out.println(">> Using Pax-Web Whiteboard trick to alter HttpService-based context to use \"" + arg2 + "\" context path.");

					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, arg1);
					properties.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_PATH, arg2);
					context.registerService(HttpContext.class, ctx, properties);
					System.out.println(">> Registered " + ctx + " as org.osgi.service.http.HttpContext OSGi service. It'll replace the context managed by pax-web-runtime.");
				}

				break;
			}
			case "servlet": {
				WebContainer service = (WebContainer) context.getService(ref);
				if (service == null) {
					System.out.println(">> Can't get a HttpService instance from ServiceReference " + ref + " for bundle " + context.getBundle() + ".");
					return null;
				}
				if (arg1 == null || "".equals(arg1.trim())) {
					System.err.println("Please specify the servlet alias.");
					return null;
				}
				if (arg2 == null || "".equals(arg2.trim())) {
					arg2 = "default";
				}
				if (!unregister) {
					System.out.println(">> Using HttpService instance: " + service + " to register a servlet under \"" + arg1 + "\" alias.");
					try {
						Dictionary<String, String> initParams = new Hashtable<>();
						// legacy way to specify unique name. Otherwise it'll be taken from class name and it has to be unique
						String name = arg1.length() > 1 ? arg1.substring(1) : arg1;
						initParams.put(PaxWebConstants.INIT_PARAM_SERVLET_NAME, name);
						service.registerServlet(arg1, new TestServlet(name, arg1, "HttpService"), initParams, service.createDefaultHttpContext(arg2));

						ServiceReference<HttpServiceRuntime> rref = context.getServiceReference(HttpServiceRuntime.class);
						HttpServiceRuntime runtime = context.getService(rref);
						String base = ((String[])runtime.getRuntimeDTO().serviceDTO.properties.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT))[0];
						base = base.substring(0, base.length() - 1);
						base = base.replace("0.0.0.0", "127.0.0.1");

						// check the context path (with a bit of hacking)
						if (runtime instanceof ServerModel) {
							ServerModel model = (ServerModel) runtime;
							Set<OsgiContextModel> contexts = new HashSet<>(model.getWhiteboardContexts());
							String contextPath = "/";
							try {
								Field fBundleContexts = model.getClass().getDeclaredField("bundleContexts");
								fBundleContexts.setAccessible(true);
								@SuppressWarnings("unchecked")
								Collection<TreeSet<OsgiContextModel>> bundleContexts = ((Map<?, TreeSet<OsgiContextModel>>)fBundleContexts.get(model)).values();
								bundleContexts.forEach(contexts::addAll);

								for (OsgiContextModel ocm : contexts) {
									if (ocm.hasDirectHttpContextInstance() && arg2.equals(ocm.getName())) {
										contextPath = ocm.getContextPath();
										break;
									}
								}

								if (arg1.startsWith("/")) {
									arg1 = arg1.substring(1);
								}
								if (!base.endsWith("/")) {
									base = base + "/";
								}
								if (contextPath.startsWith("/")) {
									contextPath = contextPath.substring(1);
								}
								if (!contextPath.equals("") && !contextPath.endsWith("/")) {
									contextPath = contextPath + "/";
								}
								System.out.println(">>>> Registered successfully. You can test it using `curl -i " + base + contextPath + arg1 + "`");
							} catch (Exception e) {
								System.out.println(">>>> Registered successfully. Can't provide the curl example...");
							}
						} else {
							System.out.println(">>>> Registered successfully. Can't provide the curl example...");
						}
						System.out.println(">> NOTES:");
						System.out.println(">>  - no org.osgi.framework.BundleContext.ungetService() was called, because it'd automatically unregister everything for this HttpService instance!");
						context.ungetService(rref);
					} catch (ServletException | NamespaceException e) {
						System.err.println("Exception registering servlet: " + e.getMessage());
					}
				} else {
					System.out.println(">> Using HttpService instance: " + service + " to unregister a servlet with \"" + arg1 + "\" alias.");
					try {
						service.unregister(arg1);
					} catch (Exception e) {
						System.err.println("Exception unregistering servlet: " + e.getMessage());
					}
					// unget twice - 1st for the getService() called during registration, 2nd - for getService()
					// called during unregistration.
					context.ungetService(ref);
					context.ungetService(ref);
					System.out.println(">> Called org.osgi.framework.BundleContext.ungetService() on ServiceReference " + ref + " for bundle " + context.getBundle() + ".");
				}

				break;
			}
			default:
		}

		return null;
	}

}
