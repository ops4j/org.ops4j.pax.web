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
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.servlet.OsgiInitializedServlet;
import org.ops4j.pax.web.service.spi.servlet.OsgiScopedServletContext;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContext;
import org.ops4j.pax.web.service.undertow.internal.web.UndertowResourceServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.http.runtime.dto.DTOConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Special {@link ServletInfo} that can be configured from {@link ServletModel}.
 */
public class PaxWebServletInfo extends ServletInfo {

	private final ServletModel servletModel;

	private OsgiContextModel osgiContextModel;

	/** Used to hold 404 servlet instance for the purpose of cloning */
	private Servlet servlet;

	private Class<? extends Servlet> servletClass;

	/** This {@link ServletContext} is scoped to single {@link org.osgi.service.http.context.ServletContextHelper} */
	private final OsgiServletContext osgiServletContext;
	/** This {@link ServletContext} is scoped to particular Whiteboard servlet */
	private final OsgiScopedServletContext servletContext;
	/**
	 * Each servlet will be associated with {@link WebContainerContext} scoped to the bundle which registered
	 * given {@link Servlet}. This has to be <em>unget</em> at the end of servlet's lifecycle.
	 */
	private final WebContainerContext webContainerContext;

	/**
	 * Special flag to mark the holder is one with 404 servlet. In such case we don't run OSGi specific
	 * security methods and we have to tweak filter config to return proper {@link ServletContext}.
	 */
	private boolean is404 = false;

	/**
	 * We have to manage the mapping ourselves to implement override'able servlets... We can't
	 * remove servlets from existing DeploymentImpl object, so we have to ... clear the mapping.
	 */
	private final List<String> mappings = new ArrayList<>();

	private final boolean whiteboardTCCL;

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
		webContainerContext = null;
		this.servlet = servlet;
		this.is404 = is404;
		this.whiteboardTCCL = false;
	}

	public PaxWebServletInfo(ServletModel model, OsgiContextModel osgiContextModel,
			OsgiServletContext osgiServletContext, boolean whiteboardTCCL) {
		// a bit tricky, because we have to call super() first
		super(model.getName(), model.getActualClass(),
				new ServletModelFactory(model,
						new OsgiScopedServletContext(osgiServletContext, model.getRegisteringBundle()),
						whiteboardTCCL));

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
		this.webContainerContext = servletContext.getResolvedWebContainerContext();

		this.whiteboardTCCL = whiteboardTCCL;
	}

	public ServletModel getServletModel() {
		return servletModel;
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

	public OsgiServletContext getOsgiServletContext() {
		return osgiServletContext;
	}

	public void setOsgiContextModel(OsgiContextModel osgiContextModel) {
		this.osgiContextModel = osgiContextModel;
	}

	public OsgiContextModel getOsgiContextModel() {
		return osgiContextModel;
	}

	public WebContainerContext getWebContainerContext() {
		return webContainerContext;
	}

	@Override
	public List<String> getMappings() {
		// override to get modifiable list
		return mappings;
	}

	public ServletInfo addMapping(final String mapping) {
		// just a copy of super.addMapping()
		if (!mapping.startsWith("/") && !mapping.startsWith("*") && !mapping.isEmpty()) {
			//if the user adds a mapping like 'index.html' we transparently translate it to '/index.html'
			mappings.add("/" + mapping);
		} else {
			mappings.add(mapping);
		}
		return this;
	}

	@Override
	public ServletInfo clone() {
		final ServletInfo info;
		if (!is404) {
			info = new PaxWebServletInfo(this.servletModel, this.osgiContextModel, this.osgiServletContext, this.whiteboardTCCL);
		} else {
			info = new PaxWebServletInfo(getName(), servlet, true);

			// this is needed only here, because constructor accepting the model already does it
			info.addMappings(getMappings());
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
		getInitParams().forEach(info::addInitParam);
		return info;
	}

	@Override
	public String toString() {
		return "PaxWebServletInfo{" +
				"mappings=" + mappings +
				", servletClass=" + servletClass +
				", name='" + getName() + '\'' +
				'}';
	}

	/**
	 * An {@link InstanceFactory} that returns {@link Servlet servlet instance} from {@link ServletModel}.
	 */
	private static class ServletModelFactory implements InstanceFactory<Servlet> {

		private final ServletModel model;
		private final OsgiScopedServletContext osgiScopedServletContext;
		private ServiceObjects<Servlet> serviceObjects;

		private final boolean whiteboardTCCL;

		ServletModelFactory(ServletModel model, OsgiScopedServletContext osgiScopedServletContext, boolean whiteboardTCCL) {
			this.model = model;
			this.osgiScopedServletContext = osgiScopedServletContext;
			this.whiteboardTCCL = whiteboardTCCL;
		}

		@Override
		public InstanceHandle<Servlet> createInstance() throws InstantiationException {
			Servlet instance = model.getServlet();
			if (instance == null) {
				if (model.getElementReference() != null) {
					// obtain Servlet using service reference
					BundleContext context = model.getRegisteringBundle().getBundleContext();
					if (context != null) {
						if (!model.isPrototype()) {
							instance = context.getService(model.getElementReference());
						} else {
							serviceObjects = context.getServiceObjects(model.getElementReference());
							instance = serviceObjects.getService();
						}
					}
					if (instance == null) {
						model.setDtoFailureCode(DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE);
						throw new RuntimeException("Can't get a Servlet service from the reference " + model.getElementReference());
					}
				} else if (model.getServletClass() != null) {
					try {
						instance = model.getServletClass().getConstructor().newInstance();
					} catch (Exception e) {
						InstantiationException instantiationException = new InstantiationException(e.getMessage());
						instantiationException.initCause(e);
						throw instantiationException;
					}
				} else if (model.getElementSupplier() != null) {
					instance = model.getElementSupplier().get();
				}
			}

			if (instance != null && model.isResourceServlet()) {
				((UndertowResourceServlet) instance).setWelcomeFiles(osgiScopedServletContext.getWelcomeFiles());
				((UndertowResourceServlet) instance).setWelcomeFilesRedirect(osgiScopedServletContext.isWelcomeFilesRedirect());
			}

			return new ImmediateInstanceHandle<Servlet>(new OsgiInitializedServlet(instance, this.osgiScopedServletContext, whiteboardTCCL)) {
				@Override
				public void release() {
					if (model.getElementReference() != null) {
						try {
							if (!model.isPrototype()) {
								BundleContext context = model.getRegisteringBundle().getBundleContext();
								if (context != null) {
									context.ungetService(model.getElementReference());
								}
							} else if (getInstance() != null) {
								Servlet realServlet = getInstance();
								if (realServlet instanceof OsgiInitializedServlet) {
									realServlet = ((OsgiInitializedServlet) realServlet).getDelegate();
								}
								serviceObjects.ungetService(realServlet);
							}
						} catch (IllegalStateException e) {
							// bundle context has already been invalidated ?
						}
					}
					if (model.getRegisteringBundle() != null) {
						try {
							osgiScopedServletContext.releaseWebContainerContext(model.getRegisteringBundle());
						} catch (IllegalStateException e) {
							// bundle context has already been invalidated ?
						}
					}
				}
			};
		}

		public OsgiScopedServletContext getServletContext() {
			return osgiScopedServletContext;
		}
	}

}
