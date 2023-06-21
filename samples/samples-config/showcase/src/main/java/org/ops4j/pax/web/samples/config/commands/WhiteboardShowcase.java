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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServlet;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.ops4j.pax.web.samples.config.commands.web.TestServlet;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;

@Command(scope = "sample", name = "whiteboard", description = "Registration/Unregistration using Whiteboard")
@Service
public class WhiteboardShowcase implements Action {

	@Option(name = "-d", description = "Delete (unregister) web elements registered by previous action.")
	private boolean unregister = false;

	@Option(name = "-b", description = "Bundle id to use when registering Whiteboard services (defaults to samples/showcase bundle).")
	private long bundleId = -1L;

	@Option(name = "-r", description = "Service rank used when registering the Whiteboard service.")
	private int rank = 0;

	@Option(name = "-s", description = "Service id to use when unregistering Whiteboard services.")
	private long serviceId = -1L;

	@Option(name = "-cs", description = "Context select filter when registering a servlet. Use quotes. Defaults to \"(osgi.http.whiteboard.context.name=default)\".")
	private String contextSelector;

	@Argument(name = "action", index = 0, description = "One of known scenarios (see source code) to check.")
	private String action = "help";

	@Argument(name = "arg1", index = 1, description = "Argument 1")
	private String arg1;

	@Argument(name = "arg2", index = 2, description = "Argument 2")
	private String arg2;

	@Argument(name = "arg3", index = 3, description = "Argument 3")
	private String arg3;

	@Reference
	private BundleContext context;

	@Override
	public Object execute() throws InvalidSyntaxException {
		if ("help".equals(action)) {
			System.out.println("This internal command allows testing several Whiteboard scenarios.");
			System.out.println("For example, we can register a servlet using Whiteboard registration for bundle ID 42 using:\n\n" +
					"\tsample:whiteboard -b 42 servlet s1 /s1/*\n\n" +
					"This will register a servlet with s1 name and /s1/* URL mapping using:\n\n" +
					"\torg.osgi.framework.BundleContext.registerService(HttpServlet.class, new TestServlet(), properties);\n\n" +
					"To unregister the servlet, call (\"s1\" argument is the name):\n\n" +
					"\tsample:whiteboard -b 42 -d servlet s1\n\n" +
					"Supported actions:\n" +
					" - [-cs context-select-filter] servlet name mapping [context-name] - register a servlet under given name, with one mapping and optionally for \"context-name\" (defaults to \"default\")\n" +
					" - -d servlet name - unregister a servlet by given name\n" +
					" - [-r service-rank=0] context [context-name=default] [context-path=/] - register a ServletContext helper with rank, name and context path\n" +
					" - -d -s service-id context - unregister a context by its registration id (to check using web:context-list command)\n"
			);
			return null;
		}

		if (bundleId != -1L) {
			context = context.getBundle(bundleId).getBundleContext();
		}
		System.out.println(">> Using context for bundle " + context.getBundle());

		switch (action) {
			case "context": {
				if (arg1 == null || "".equals(arg1.trim())) {
					arg1 = "default";
				}
				if (arg2 == null || "".equals(arg2.trim())) {
					arg2 = "/";
				}
				if (!unregister) {
					System.out.println(">> Registering org.osgi.service.http.context.ServletContextHelper with \"" + arg1 + "\" name, \"" + arg2 + "\" context path and for " + context.getBundle() + ".");
					ServletContextHelper sch = new ServletContextHelper() {
					};
					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, arg1);
					properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, arg2);
					properties.put(Constants.SERVICE_RANKING, rank);
					context.registerService(ServletContextHelper.class, sch, properties);
				} else {
					if (serviceId >= 0) {
						System.out.println(">> Unregistering org.osgi.service.http.context.ServletContextHelper with " + serviceId + " service.id and for " + context.getBundle() + ".");
					} else {
						System.out.println(">> Unregistering org.osgi.service.http.context.ServletContextHelper with \"" + arg1 + "\" name and for " + context.getBundle() + ".");
					}
					// we need reflection here
					Collection<ServiceReference<ServletContextHelper>> refs;
					if (serviceId >= 0) {
						refs = context.getServiceReferences(ServletContextHelper.class, String.format("(%s=%d)",
								Constants.SERVICE_ID, serviceId));
					} else {
						refs = context.getServiceReferences(ServletContextHelper.class, String.format("(%s=%s)",
								HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, arg1));
					}
					for (ServiceReference<ServletContextHelper> ref : refs) {
						try {
							Method m = ref.getClass().getDeclaredMethod("getRegistration");
							m.setAccessible(true);
							ServiceRegistration<?> reg = (ServiceRegistration<?>) m.invoke(ref);
							reg.unregister();
							System.out.println(">>>> Unregistered " + reg + " successfully.");
						} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							System.err.println(">> Can't get a registration from reference: " + e.getMessage());
						}
					}
				}

				break;
			}
			case "servlet": {
				if (arg1 == null || "".equals(arg1.trim())) {
					System.err.println("Please specify the servlet name.");
					return null;
				}
				if (!unregister) {
					if (arg2 == null || "".equals(arg2.trim())) {
						System.err.println("Please specify the servlet mapping.");
						return null;
					} else {
						// *.ext
						Pattern p1 = Pattern.compile("^\\*\\.[^.*]+$");
						// /path/*
						Pattern p2 = Pattern.compile("^/[^*]*\\*$");
						// /path
						Pattern p3 = Pattern.compile("^/[^*]+$");
						if (!("/".equals(arg2) || p1.matcher(arg2).matches() || p2.matcher(arg2).matches() || p3.matcher(arg2).matches())) {
							System.err.println("Please specify valid mapping.");
							return null;
						}
					}
				}
				if (arg3 == null || "".equals(arg3.trim())) {
					arg3 = "default";
				}

				if (!unregister) {
					Dictionary<String, Object> properties = new Hashtable<>();
					properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, arg1);
					properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, arg2);
					if (contextSelector == null || "".equals(contextSelector.trim())) {
						contextSelector = String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, arg3);
					}
					properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, contextSelector);
					properties.put(Constants.SERVICE_RANKING, rank);

					System.out.println(">> Registering a servlet with \"" + arg1 + "\" name, \"" + arg2 + "\" pattern, \"" + contextSelector + "\" context selector and for " + context.getBundle() + ".");
					context.registerService(HttpServlet.class, new TestServlet(arg1, arg2, "Whiteboard"), properties);

					// check the URL base
					ServiceReference<HttpServiceRuntime> rref = context.getServiceReference(HttpServiceRuntime.class);
					HttpServiceRuntime runtime = context.getService(rref);
					String base = ((String[])runtime.getRuntimeDTO().serviceDTO.properties.get(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT))[0];
					base = base.substring(0, base.length() - 1);
					base = base.replace("0.0.0.0", "127.0.0.1");

					// check the context path (with a bit of hacking)
					if (runtime instanceof ServerModel) {
						ServerModel model = (ServerModel) runtime;
						Set<OsgiContextModel> contexts = new HashSet<>(model.getAllWhiteboardContexts());
						String contextPath = "/";
						try {
							Field fBundleContexts = model.getClass().getDeclaredField("bundleContexts");
							fBundleContexts.setAccessible(true);
							@SuppressWarnings("unchecked")
							Collection<TreeSet<OsgiContextModel>> bundleContexts = ((Map<?, TreeSet<OsgiContextModel>>)fBundleContexts.get(model)).values();
							bundleContexts.forEach(contexts::addAll);

							Filter f = context.createFilter(contextSelector);
							for (OsgiContextModel ocm : contexts) {
								if (f.matches(ocm.getContextRegistrationProperties())) {
									contextPath = ocm.getContextPath();
									break;
								}
							}

							if (arg2.startsWith("/")) {
								arg2 = arg2.substring(1);
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
							System.out.println(">>>> Registered successfully. You can test it using `curl -i " + base + contextPath + arg2.replace("*", "anything") + "`");
						} catch (Exception e) {
							System.out.println(">>>> Registered successfully. Can't provide the curl example...");
						}
					} else {
						System.out.println(">>>> Registered successfully. Can't provide the curl example...");
					}
					context.ungetService(rref);
				} else {
					// we need reflection here
					Collection<ServiceReference<HttpServlet>> refs = context.getServiceReferences(HttpServlet.class, String.format("(%s=%s)",
							HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME, arg1));
					if (refs.size() > 0) {
						ServiceReference<HttpServlet> ref = refs.iterator().next();
						try {
							Method m = ref.getClass().getDeclaredMethod("getRegistration");
							m.setAccessible(true);
							ServiceRegistration<?> reg = (ServiceRegistration<?>) m.invoke(ref);
							reg.unregister();
							System.out.println(">>>> Unregistered " + reg + " successfully.");
						} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							System.err.println(">> Can't get a registration from reference: " + e.getMessage());
						}
					}
				}

				break;
			}
			default:
				break;
		}

		return null;
	}

}
