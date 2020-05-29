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
package org.ops4j.pax.web.service.undertow.internal;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Special {@link ServletInfo} that can be configured from {@link ServletModel}.
 */
public class PaxWebServletInfo extends ServletInfo {

	private final ServletModel servletModel;

	private final OsgiContextModel osgiContextModel;

	/** Used to hold 404 servlet instance for the purpose of cloning */
	private Servlet servlet;

	private Class<? extends Servlet> servletClass;
	private ServiceReference<? extends Servlet> serviceReference;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard servlet */
	private final OsgiScopedServletContext servletContext;

	/**
	 * Special flag to mark the holder is one with 404 servlet. In such case we don't run OSGi specific
	 * security methods and we have to tweak filter config to return proper {@link ServletContext}.
	 */
	private boolean is404 = false;

	/**
	 * Constructor to use when wrapping internal {@link Servlet servlets} which won't use OSGi machinery.
	 *
	 * @param name
	 * @param servlet
	 */
	PaxWebServletInfo(String name, Servlet servlet, boolean is404) {
		super(name, servlet.getClass(), new ImmediateInstanceFactory<>(servlet));
		servletModel = null;
		osgiContextModel = null;
		osgiServletContext = null;
		servletContext = null;
		this.servlet = servlet;
		this.is404 = is404;
	}

	public PaxWebServletInfo(ServletModel model, OsgiContextModel osgiContextModel,
				OsgiServletContext osgiServletContext) {
		super(model.getName(), model.getActualClass(),
				new ServletModelFactory(model,
						new OsgiScopedServletContext(osgiServletContext, model.getRegisteringBundle())));

		this.osgiContextModel = osgiContextModel;
		this.osgiServletContext = osgiServletContext;

		this.servletModel = model;

		for (String pattern : model.getUrlPatterns()) {
			addMapping(pattern);
		}

		servletModel.getInitParams().forEach(this::addInitParam);

		setAsyncSupported(servletModel.getAsyncSupported() != null && servletModel.getAsyncSupported());
		if (servletModel.getLoadOnStartup() != null) {
			setLoadOnStartup(servletModel.getLoadOnStartup());
		}
		setMultipartConfig(servletModel.getMultipartConfigElement());

		this.servletContext = ((ServletModelFactory)super.getInstanceFactory()).getServletContext();
	}

	public Bundle getRegisteringBundle() {
		return servletModel.getRegisteringBundle();
	}

	public boolean is404() {
		return is404;
	}

	public OsgiScopedServletContext getServletContext() {
		return servletContext;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	@Override
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public ServletInfo clone() {
		final ServletInfo info;
		if (!is404) {
			info = new PaxWebServletInfo(this.servletModel, this.osgiContextModel, this.osgiServletContext);
		} else {
			info = new PaxWebServletInfo(getName(), servlet, true);

			// these 2 are needed only here, because constructor accepting the model already does it
			info.addMappings(getMappings());
			getInitParams().forEach(info::addInitParam);
		}
		info.setJspFile(getJspFile());
		info.setLoadOnStartup(getLoadOnStartup());
		info.setEnabled(isEnabled());
		info.setAsyncSupported(isAsyncSupported());
		info.setRunAs(getRunAs());
		info.setMultipartConfig(getMultipartConfig());
		info.setExecutor(getExecutor());
		info.setRequireWelcomeFileMapping(isRequireWelcomeFileMapping());
		getSecurityRoleRefs().forEach(r -> info.addSecurityRoleRef(r.getRole(), r.getLinkedRole()));
		getHandlerChainWrappers().forEach(info::addHandlerChainWrapper);
		if (getServletSecurityInfo() != null) {
			info.setServletSecurityInfo(getServletSecurityInfo().clone());
		}
		return info;
	}

	/**
	 * An {@link InstanceFactory} that returns {@link Servlet servlet instance} from {@link ServletModel}.
	 */
	private static class ServletModelFactory implements InstanceFactory<Servlet> {

		private final ServletModel model;
		private final OsgiScopedServletContext osgiScopedServletContext;

		public ServletModelFactory(ServletModel model, OsgiScopedServletContext osgiScopedServletContext) {
			this.model = model;
			this.osgiScopedServletContext = osgiScopedServletContext;
		}

		@Override
		public InstanceHandle<Servlet> createInstance() throws InstantiationException {
			Servlet instance = model.getServlet();
			if (instance == null) {
				if (model.getElementReference() != null) {
					// obtain Servlet using reference
					instance = model.getRegisteringBundle().getBundleContext().getService(model.getElementReference());
					if (instance == null) {
						throw new RuntimeException("Can't get a Servlet service from the reference " + model.getElementReference());
					}
				} else if (model.getServletClass() != null) {
					try {
						instance = model.getServletClass().newInstance();
					} catch (Exception e) {
						InstantiationException instantiationException = new InstantiationException(e.getMessage());
						instantiationException.initCause(e);
						throw instantiationException;
					}
				}
			}

			return new ImmediateInstanceHandle<Servlet>(new OsgiInitializedServlet(instance, this.osgiScopedServletContext));
		}

		public OsgiScopedServletContext getServletContext() {
			return osgiScopedServletContext;
		}
	}

}
