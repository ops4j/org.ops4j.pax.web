/*
 * Copyright 2007 Alin Dreghiciu.
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

import java.util.EventListener;

import javax.servlet.Servlet;

import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that unregisters a web application. Cannot be reused, it has to be
 * one per visit.
 */
class UnregisterWebAppVisitorWC implements WebAppVisitor {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(UnregisterWebAppVisitorWC.class);
	/**
	 * HttpService to be used for registration.
	 */
	private final WebContainer webContainer;
	/**
	 * Http context used during registration.
	 */
	private HttpContext httpContext;

//	private BundleClassLoader bundleClassLoader;

	/**
	 * Creates a new unregistration visitor.
	 *
	 * @param webContainer Pax-Web HttpService-implementation
	 * @throws NullArgumentException if web container is null
	 */
	UnregisterWebAppVisitorWC(final WebContainer webContainer) {
//		NullArgumentException.validateNotNull(webContainer, "Web Container");
		this.webContainer = webContainer;
	}

	/**
	 * Unregisters resources related to web app.
	 *
	 * @see WebAppVisitor#visit(BundleWebApplication)
	 */
	public void visit(final BundleWebApplication webApp) {
//		bundleClassLoader = new BundleClassLoader(webApp.getBundle());
//		httpContext = webApp.getHttpContext();
		// Make sure we stop the context first, so that listeners
		// can be called correctly before removing ann objects
		webContainer.begin(httpContext);
		// unregister war content resources
		//CHECKSTYLE:OFF
		try {
			webContainer.unregister("/");
		} catch (IllegalArgumentException badarg) {
			// Ignore, we haven't registered anything
		} catch (Exception ignore) {
			LOG.warn("Unregistration exception. Skipping.", ignore);
		}
		// unregister welcome files
//		try {
//			webContainer.unregisterWelcomeFiles(webApp.getWelcomeFiles(), httpContext);
//		} catch (IllegalArgumentException badarg) {
//			// Ignore, we haven't registered anything
//		} catch (Exception ignore) {
//			LOG.warn("Unregistration exception. Skipping.", ignore);
//		}
//		// unregister JSP support
//		try {
//			webContainer.unregisterJsps(httpContext);
//		} catch (IllegalArgumentException | UnsupportedOperationException badarg) {
//			// Ignore, we haven't registered anything
//		} catch (Exception ignore) {
//			LOG.warn("Unregistration exception. Skipping.", ignore);
//		}
		//CHECKSTYLE:ON
	}

	/**
	 * Unregisters servlet from web container.
	 *
	 * @throws NullArgumentException if servlet is null
	 * @see WebAppVisitor#visit(WebAppServlet)
	 */
	public void visit(final WebAppServlet webAppServlet) {
//		NullArgumentException.validateNotNull(webAppServlet, "Web app servlet");
		Class<? extends Servlet> servletClass = webAppServlet
				.getServletClass();
		if (servletClass == null && webAppServlet.getServletClassName() != null) {
//			try {
//				servletClass = RegisterWebAppVisitorHS.loadClass(Servlet.class, bundleClassLoader,
//						webAppServlet.getServletClassName());
//			} catch (ClassNotFoundException | IllegalAccessException e) {
//				 TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		if (servletClass != null) {
			//CHECKSTYLE:OFF
			try {
				webContainer.unregisterServlets(servletClass);
				webAppServlet.setServletClass(null);
			} catch (Exception ignore) {
				LOG.warn("Unregistration exception. Skipping.", ignore);
			}
			//CHECKSTYLE:ON
		}
	}

	/**
	 * Unregisters filter from web container.
	 *
	 * @throws NullArgumentException if filter is null
	 * @see WebAppVisitor#visit(WebAppFilter)
	 */
	public void visit(final WebAppFilter webAppFilter) {
//		NullArgumentException.validateNotNull(webAppFilter, "Web app filter");
		String filterName = webAppFilter.getFilterName();

		if (filterName != null) {
			//CHECKSTYLE:OFF
			try {
				webContainer.unregisterFilter(filterName);
			} catch (Exception ignore) {
				LOG.warn("Unregistration exception. Skipping.", ignore);
			}
			//CHECKSTYLE:ON
		}
	}

	/**
	 * Unregisters listeners from web container.
	 *
	 * @throws NullArgumentException if listener is null
	 * @see WebAppVisitor#visit(WebAppListener)
	 */
	public void visit(final WebAppListener webAppListener) {
//		NullArgumentException.validateNotNull(webAppListener,
//				"Web app listener");
		final EventListener listener = webAppListener.getListener();
		if (listener != null) {
			//CHECKSTYLE:OFF
			try {
				webContainer.unregisterEventListener(listener);
			} catch (Exception ignore) {
				LOG.warn("Unregistration exception. Skipping.", ignore);
			}
			//CHECKSTYLE:ON
		}
	}

	/**
	 * Unregisters error pages from web container.
	 *
	 * @throws NullArgumentException if error page is null
	 * @see WebAppVisitor#visit(WebAppErrorPage)
	 */
	public void visit(final WebAppErrorPage webAppErrorPage) {
//		NullArgumentException.validateNotNull(webAppErrorPage,
//				"Web app error page");
//		//CHECKSTYLE:OFF
//		try {
//			webContainer.unregisterErrorPage(webAppErrorPage.getError(),
//					httpContext);
//		} catch (Exception ignore) {
//			LOG.warn("Unregistration exception. Skipping.", ignore);
//		}
//		//CHECKSTYLE:ON
	}

	public void visit(WebAppLoginConfig loginConfig) {
//		NullArgumentException.validateNotNull(loginConfig,
//				"Web app login config");
//		webContainer.unregisterLoginConfig(httpContext);
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
//		NullArgumentException.validateNotNull(constraintMapping,
//				"Web app constraint mapping");
//		webContainer.unregisterConstraintMapping(httpContext);
	}

	public void visit(
			WebAppServletContainerInitializer servletContainerInitializer) {
//		NullArgumentException.validateNotNull(servletContainerInitializer,
//				"Servlet Container Initializer");
//		webContainer.unregisterServletContainerInitializer(httpContext);
	}

	public void end() {
	}
}
