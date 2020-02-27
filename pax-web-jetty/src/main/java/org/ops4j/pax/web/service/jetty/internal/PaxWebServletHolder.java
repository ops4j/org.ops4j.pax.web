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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
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
	private ServiceReference<? extends Servlet> serviceReference;

	private final OsgiServletContext servletContext;

	/**
	 * Constructor to use when wrapping internal {@link Servlet servlets} which won't use OSGi machinery.
	 *
	 * @param name
	 * @param servlet
	 */
	PaxWebServletHolder(String name, Servlet servlet) {
		super(name, servlet);
		servletModel = null;
		osgiContextModel = null;
		servletContext = null;
	}

	/**
	 * Initialize {@link PaxWebServletHolder} with {@link ServletModel} and this particular {@link OsgiContextModel}
	 * in which' context we're adding given servlet to Jetty.
	 *
	 * @param sch
	 * @param servletModel
	 * @param osgiContextModel
	 */
	public PaxWebServletHolder(ServletContextHandler sch, ServletModel servletModel,
			OsgiContextModel osgiContextModel) {

		this.servletModel = servletModel;
		this.osgiContextModel = osgiContextModel;

		// name that binds a servlet with its mapping
		setName(servletModel.getName());
		if (servletModel.getServletClass() != null) {
			setHeldClass(servletModel.getServletClass());
		} else if (servletModel.getServlet() != null) {
			setServlet(servletModel.getServlet());
		} else {
			this.serviceReference = servletModel.getElementReference();
		}

		setInitParameters(servletModel.getInitParams());
		setAsyncSupported(servletModel.getAsyncSupported() != null && servletModel.getAsyncSupported());
		if (servletModel.getLoadOnStartup() != null) {
			setInitOrder(servletModel.getLoadOnStartup());
		}
		getRegistration().setMultipartConfig(servletModel.getMultipartConfigElement());

		// setup proper delegation for ServletContext
		servletContext = new OsgiServletContext(sch.getServletContext(),
				osgiContextModel, servletModel.getRegisteringBundle());
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public WebContainerContext getWebContainerContext() {
		return osgiContextModel.getHttpContext();
	}

	public OsgiServletContext getServletContext() {
		return servletContext;
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
	 *
	 * we can change behavior of HttpServletRequest
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
		if (instance == null && servletModel.getElementReference() != null) {
			// obtain Servlet using reference
			instance =  servletModel.getRegisteringBundle().getBundleContext().getService(servletModel.getElementReference());
		}

		// if null, newInstance() will be called
		return instance == null ? null : new OsgiInitializedServlet(instance);
	}

	@Override
	public void destroyInstance(Object o) throws Exception {
		if (servletModel != null && servletModel.getElementReference() != null) {
			servletModel.getRegisteringBundle().getBundleContext().ungetService(servletModel.getElementReference());
		}
		super.destroyInstance(o);
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
		// no need to do anything special, but we have a Bundle reference, so we can use it if needed
		return new OsgiInitializedServlet(super.newInstance());
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
	 * {@link Servlet} wrapper that uses correct {@link ServletConfig} wrapper that returns correct wrapper
	 * for {@link javax.servlet.ServletContext}
	 */
	private class OsgiInitializedServlet implements Servlet {

		final Servlet servlet;

		public OsgiInitializedServlet(Servlet servlet) {
			this.servlet = servlet;
		}

		@Override
		public void init(final ServletConfig config) throws ServletException {
			servlet.init(new ServletConfig() {
				@Override
				public String getServletName() {
					return config.getServletName();
				}

				@Override
				public ServletContext getServletContext() {
					return PaxWebServletHolder.this.servletContext;
				}

				@Override
				public String getInitParameter(String name) {
					return config.getInitParameter(name);
				}

				@Override
				public Enumeration<String> getInitParameterNames() {
					return config.getInitParameterNames();
				}
			});
		}

		@Override
		public ServletConfig getServletConfig() {
			return servlet.getServletConfig();
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			servlet.service(req, res);
		}

		@Override
		public String getServletInfo() {
			return servlet.getServletInfo();
		}

		@Override
		public void destroy() {
			servlet.destroy();
		}
	}

}
