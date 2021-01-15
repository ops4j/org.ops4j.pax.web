/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that registers a web application using a standard http service.
 * Cannot be reused, it has to be one per visit.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */

class RegisterWebAppVisitorHS implements WebAppVisitor {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RegisterWebAppVisitorHS.class);
	/**
	 * HttpService to be used for registration.
	 */
	private final HttpService httpService;
	/**
	 * Created http context (during webapp visit)
	 */
	private HttpContext httpContext;
	/**
	 * Class loader to be used in the created web app.
	 */
	private ClassLoader bundleClassLoader;

	/**
	 * Creates a new registration visitor.
	 *
	 * @param httpService http service to be used for registration. Cannot be null.
	 * @throws NullArgumentException if http service is null
	 */
	RegisterWebAppVisitorHS(final HttpService httpService) {
//		NullArgumentException.validateNotNull(httpService, "Http Service");
		this.httpService = httpService;
	}

	/**
	 * Creates a default context that will be used for all following
	 * registrations and registers a resource for root of war.
	 *
	 * @throws NullArgumentException if web app is null
	 * @see WebAppVisitor#visit(BundleWebApplication)
	 */
	public void visit(final BundleWebApplication webApp) {
//		NullArgumentException.validateNotNull(webApp, "Web app");
//		bundleClassLoader = new BundleClassLoader(webApp.getBundle());
//		httpContext = new WebAppHttpContext(
//				httpService.createDefaultHttpContext(), webApp.getRootPath(),
//				webApp.getBundle(), webApp.getMimeMappings());
		try {
			LOG.info("Pax Web not available. Skipping context params registration");
			httpService.registerResources("/", "default", httpContext);
			//CHECKSTYLE:OFF
		} catch (Throwable ignore) {
			LOG.error("Registration exception. Skipping.", ignore);
		}
		//CHECKSTYLE:ON
	}

	/**
	 * Registers servlets with http context.
	 *
	 * @throws NullArgumentException if servlet is null
	 * @see WebAppVisitor#visit(WebAppServlet)
	 */
	public void visit(final WebAppServlet webAppServlet) {
//		NullArgumentException.validateNotNull(webAppServlet, "Web app servlet");
		final String[] aliases = webAppServlet.getAliases();
		if (aliases != null && aliases.length > 0) {
			for (final String alias : aliases) {
				try {
					final Servlet servlet = newInstance(Servlet.class,
							bundleClassLoader,
							webAppServlet.getServletClassName());
					httpService.registerServlet(alias, servlet,
							convertInitParams(webAppServlet.getInitParams()),
							httpContext);
					//CHECKSTYLE:OFF
				} catch (Throwable ignore) {
					LOG.error("Registration exception. Skipping.", ignore);
				}
				//CHECKSTYLE:ON
			}
		} else {
			LOG.warn("Servlet [" + webAppServlet
					+ "] does not have any alias. Skipped.");
		}
	}

	/**
	 * Does nothing as standard http service does not support filters.
	 *
	 * @see WebAppVisitor#visit(WebAppFilter)
	 */
	public void visit(final WebAppFilter webAppFilter) {
		LOG.info("Pax Web not available. Skipping filter registration for ["
				+ webAppFilter + "]");
	}

	/**
	 * Does nothing as standard http service does not support listeners.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(final WebAppListener webAppListener) {
		LOG.info("Pax Web not available. Skipping listener registration for ["
				+ webAppListener + "]");
	}

	/**
	 * Does nothing as standard http service does not support error pages.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(final WebAppErrorPage webAppErrorPage) {
		LOG.info("Pax Web not available. Skipping error page registration for ["
				+ webAppErrorPage + "]");
	}

	/**
	 * Does nothing as standard http service does not support login config.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(WebAppLoginConfig loginConfig) {
		LOG.info("Pax Web not available. Skipping login config registration for ["
				+ loginConfig + "]");
	}

	/**
	 * Does nothing as standard http service does not support constraint
	 * mappings.
	 *
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(WebAppConstraintMapping constraintMapping) {
		LOG.info("Pax Web not available. Skipping constraint mapping registration for ["
				+ constraintMapping + "]");
	}

	/**
	 * Does nothing
	 */
	public void end() {
	}

	/**
	 * Creates an instance of a class from class name.
	 *
	 * @param clazz       class of the required object
	 * @param classLoader class loader to use to load the class
	 * @param className   class name for the object to create
	 * @return created object
	 * @throws NullArgumentException  if any of the parameters is null
	 * @throws ClassNotFoundException re-thrown
	 * @throws IllegalAccessException re-thrown
	 * @throws InstantiationException re-thrown
	 */
	public static <T> T newInstance(final Class<T> clazz,
									final ClassLoader classLoader, final String className)
			throws ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		return loadClass(clazz, classLoader, className).newInstance();
	}

	/**
	 * Load a class from class name.
	 *
	 * @param clazz       class of the required object
	 * @param classLoader class loader to use to load the class
	 * @param className   class name for the class to load
	 * @return class object
	 * @throws NullArgumentException  if any of the parameters is null
	 * @throws ClassNotFoundException re-thrown
	 * @throws IllegalAccessException re-thrown
	 */
	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadClass(final Class<T> clazz,
												   final ClassLoader classLoader, final String className)
			throws ClassNotFoundException, IllegalAccessException {
//		NullArgumentException.validateNotNull(clazz, "Class");
//		NullArgumentException.validateNotNull(classLoader, "ClassLoader");
//		NullArgumentException.validateNotNull(className, "Servlet Class");
		return (Class<? extends T>) classLoader.loadClass(className);
	}

	/**
	 * Converts an array of init params to a Dictionary.
	 *
	 * @param initParams array to be converted
	 * @return Dictionary of init params
	 */
	public static Dictionary<String, String> convertInitParams(
			final WebAppInitParam[] initParams) {
		if (initParams == null || initParams.length == 0) {
			return null;
		}
		Hashtable<String, String> dictionary = new Hashtable<>();
		for (WebAppInitParam initParam : initParams) {
			dictionary.put(initParam.getParamName(), initParam.getParamValue());
		}
		return dictionary;
	}

}
