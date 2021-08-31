/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.lang.reflect.InvocationTargetException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

/**
 * <p>Jetty {@link ServletHolder} that can perform tasks described in Http Service and Whiteboard Service
 * specifications:<ul>
 *     <li>Servlet instance creation using class name and proper class loader (or instantiation of given class)</li>
 *     <li>Servlet instance retrieval from OSGi registry (to ensure proper behavior if the service is
 *     bundle scoped or prototype scoped {@link org.osgi.framework.ServiceFactory})</li>
 * </ul></p>
 */
public class PaxWebServletHolder extends ServletHolder {

	private final ServletModel servletModel;
	private final OsgiContextModel osgiContextModel;

	private ServletMapping mapping;

	private ServiceReference<Servlet> servletReference;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard servlet */
	private final OsgiScopedServletContext servletContext;

	private ServiceObjects<Servlet> serviceObjects;

	/**
	 * Each servlet will be associated with {@link WebContainerContext} scoped to the bundle which registered
	 * given {@link Servlet}.
	 */
	private final WebContainerContext webContainerContext;

	/**
	 * Special flag to mark the holder is one with 404 servlet. In such case we don't run OSGi specific
	 * security methods and we have to tweak filter config to return proper {@link ServletContext}.
	 */
	private boolean is404;

	/**
	 * Constructor to use when wrapping internal {@link Servlet servlets} which won't use OSGi machinery.
	 *
	 * @param name
	 * @param servlet
	 */
	PaxWebServletHolder(String name, Servlet servlet, boolean is404) {
		super(name, servlet);
		servletModel = null;
		osgiContextModel = null;
		osgiServletContext = null;
		servletContext = null;
		webContainerContext = null;
		this.is404 = is404;
	}

	/**
	 * Initialize {@link PaxWebServletHolder} with {@link ServletModel} and this particular {@link OsgiContextModel}
	 * in which' context we're adding given servlet to Jetty.
	 *
	 * @param servletModel
	 * @param osgiContextModel
	 * @param osgiServletContext {@link org.osgi.service.http.context.ServletContextHelper} specific
	 *        {@link ServletContext}
	 */
	public PaxWebServletHolder(ServletModel servletModel, OsgiContextModel osgiContextModel,
			OsgiServletContext osgiServletContext) {
		super();

		this.servletModel = servletModel;
		this.osgiContextModel = osgiContextModel;
		this.osgiServletContext = osgiServletContext;

		// name that binds a servlet with its mapping
		setName(servletModel.getName());
		if (servletModel.getServletClass() != null) {
			setHeldClass(servletModel.getServletClass());
		} else if (servletModel.getServlet() != null) {
			setServlet(servletModel.getServlet());
			setHeldClass(servletModel.getServlet().getClass());
		} else {
			this.servletReference = servletModel.getElementReference();
		}

		setInitParameters(servletModel.getInitParams());
		setAsyncSupported(servletModel.getAsyncSupported() != null && servletModel.getAsyncSupported());
		if (servletModel.getLoadOnStartup() != null) {
			setInitOrder(servletModel.getLoadOnStartup());
		}
		getRegistration().setMultipartConfig(servletModel.getMultipartConfigElement());

		// setup proper delegation for ServletContext
		servletContext = new OsgiScopedServletContext(this.osgiServletContext, servletModel.getRegisteringBundle());

		// instead of doing it once per request, we can get servlet-scoped WebContainerContext now
		webContainerContext = osgiContextModel.resolveHttpContext(servletModel.getRegisteringBundle());
	}

	public ServletModel getServletModel() {
		return servletModel;
	}

	public Bundle getRegisteringBundle() {
		return servletModel.getRegisteringBundle();
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public WebContainerContext getWebContainerContext() {
		return webContainerContext;
	}

	public boolean is404() {
		return is404;
	}

	@Override
	public void doStart() throws Exception {
		if (servletReference != null) {
			// Jetty's ServletHolder needs a servlet class to do some verification. We have to provide it if
			// using ServiceReference<Servlet>. Fortunately this satisfies Jetty.
			setHeldClass(Servlet.class);
		} else if (servletModel != null && servletModel.getElementSupplier() != null) {
			// this will set the class as well
			setInstance(servletModel.getElementSupplier().get());
		}

		super.doStart();
	}

	/*
	 * getInstance()/newInstance() methods are called in two scenarios (read the calls top-down):
	 *
	 * 1) ServletHolder added to started ServletContextHandler:
	 *  - org.eclipse.jetty.servlet.ServletHandler.addServletMapping(ServletHandler.java:922)
	 *  - org.eclipse.jetty.servlet.ServletHandler.setServletMappings(ServletHandler.java:1531)
	 *  - org.eclipse.jetty.servlet.ServletHandler.updateMappings(ServletHandler.java:1448)
	 *  - org.eclipse.jetty.servlet.ServletHandler.initialize(ServletHandler.java:744)
	 *  - org.eclipse.jetty.servlet.ServletHolder.initialize(ServletHolder.java:425)
	 *  - org.eclipse.jetty.servlet.ServletHolder.initServlet(ServletHolder.java:568)
	 *  - org.eclipse.jetty.servlet.BaseHolder.getInstance(BaseHolder.java:177)
	 *
	 * 2) ServletHolder added to stopped ServletContextHandler which is started later:
	 *  - org.eclipse.jetty.servlet.ServletContextHandler.doStart(ServletContextHandler.java:275)
	 *  - org.eclipse.jetty.server.handler.ContextHandler.doStart(ContextHandler.java:822)
	 *  - org.eclipse.jetty.servlet.ServletContextHandler.startContext(ServletContextHandler.java:360)
	 *  - org.eclipse.jetty.servlet.ServletHandler.initialize(ServletHandler.java:744)
	 *  - org.eclipse.jetty.servlet.ServletHolder.initialize(ServletHolder.java:425)
	 *  - org.eclipse.jetty.servlet.ServletHolder.initServlet(ServletHolder.java:568)
	 *  - org.eclipse.jetty.servlet.BaseHolder.getInstance(BaseHolder.java:177)
	 */

	/**
	 * <p>This method is called first by {@code org.eclipse.jetty.servlet.ServletHolder#initServlet()} in expectance
	 * of existing {@link Servlet} instance.</p>
	 *
	 * <p>Http Service / Whiteboard Service specification describe scenario where server instance (and also
	 * other Whiteboard instances, including {@link org.osgi.service.http.context.ServletContextHelper} itself) are
	 * obtained from OSGi Service Registry - knowing that {@link Servlet} may come from
	 * {@link org.osgi.framework.ServiceFactory}. That's why we override this method for this special purpose.</p>
	 *
	 * @return
	 */
	@Override
	protected synchronized Servlet getInstance() {
		Servlet instance = super.getInstance();
		if (instance == null && servletReference != null) {
			if (!servletModel.isPrototype()) {
				instance = servletModel.getRegisteringBundle().getBundleContext().getService(servletReference);
			} else {
				serviceObjects = servletModel.getRegisteringBundle().getBundleContext().getServiceObjects(servletReference);
				instance = serviceObjects.getService();
			}
		}
		if (instance == null && servletModel.getElementSupplier() != null) {
			instance = servletModel.getElementSupplier().get();
		}

		if (instance != null && servletModel != null && servletModel.isResourceServlet()) {
			((JettyResourceServlet) instance).setWelcomeFiles(osgiServletContext.getWelcomeFiles());
			((JettyResourceServlet) instance).setWelcomeFilesRedirect(osgiServletContext.isWelcomeFilesRedirect());
		}

		// if null, newInstance() will be called
		// In Tomcat configuration is taken from the StandardWrapper, here
		// org.eclipse.jetty.servlet.ServletHolder._config is private, so we need special OsgiInitializedServlet
		return instance == null ? null : new OsgiInitializedServlet(instance, servletContext);
	}

	@Override
	public void destroyInstance(Object o) {
		super.destroyInstance(o);
		if (servletModel != null && servletReference != null) {
			if (!servletModel.isPrototype()) {
				servletModel.getRegisteringBundle().getBundleContext().ungetService(servletReference);
			} else {
				Servlet realServlet = (Servlet) o;
				if (realServlet instanceof Wrapper) {
					realServlet = ((Wrapper) realServlet).getWrapped();
				}
				if (realServlet instanceof OsgiInitializedServlet) {
					realServlet = ((OsgiInitializedServlet) realServlet).getDelegate();
				}
				serviceObjects.ungetService(realServlet);
			}
		}
	}

	/**
	 * If no existing instance of {@link Servlet} is available, this method is supposed to create one using
	 * class/className
	 *
	 * @return
	 * @throws ServletException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 */
	@Override
	protected Servlet newInstance() throws ServletException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
		// no need to do anything special, but we have a Bundle reference, so we could use it if needed
		return new OsgiInitializedServlet(super.newInstance(), servletContext);
	}

	/**
	 * An override only to make it public, so it can be called from {@link PaxWebServletHandler}.
	 *
	 * @param baseRequest
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws UnavailableException
	 */
	@Override
	public void prepare(Request baseRequest, ServletRequest request, ServletResponse response) throws ServletException, UnavailableException {
		super.prepare(baseRequest, request, response);
	}

	/**
	 * Allows keeping {@link ServletMapping} that was used together with this {@link ServletHolder}
	 * @param mapping
	 */
	public void setMapping(ServletMapping mapping) {
		this.mapping = mapping;
	}

	/**
	 * Gets a {@link ServletMapping} that was used during registration of this {@link ServletHolder} in single
	 * {@link PaxWebServletHandler}.
	 * @return
	 */
	public ServletMapping getMapping() {
		return mapping;
	}

}
