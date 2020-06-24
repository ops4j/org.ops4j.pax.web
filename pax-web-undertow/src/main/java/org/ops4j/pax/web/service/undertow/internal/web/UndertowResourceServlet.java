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
package org.ops4j.pax.web.service.undertow.internal.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.server.handlers.resource.CachingResourceManager;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.Resource;
import io.undertow.server.handlers.resource.ResourceChangeListener;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.handlers.DefaultServlet;

/**
 * <p>Extension of {@link DefaultServlet}, so we can use many of such servlets to serve resources from different bases
 * for the purpose of "resource" handling specified in HttpService and Whiteboard specifications.</p>
 *
 * <p>Due to caching infrastructure of Undertow, we need this servlet to implement {@link ResourceManager}, because
 * {@link io.undertow.server.handlers.resource.CachingResourceManager} should be created up front. The details
 * of {@link ResourceManager} will be set up in {@link javax.servlet.Servlet#init(ServletConfig)}.</p>
 */
public class UndertowResourceServlet extends DefaultServlet implements ResourceManager {

	private ResourceManager cachingResourceManager;

	/** If specified, this is the directory to fetch resource files from */
	private final File baseDirectory;

	/**
	 * If {@link #baseDirectory} is not specified, this is resource prefix to prepend when calling
	 * {@link org.osgi.service.http.context.ServletContextHelper#getResource(String)}
	 */
	private final String chroot;

	/** The real {@link ResourceManager} configured in {@link javax.servlet.Servlet#init(ServletConfig)} */
	private ResourceManager resourceManager;

	public UndertowResourceServlet(File baseDirectory, String chroot) {
		this.baseDirectory = baseDirectory;
		this.chroot = chroot;
	}

	public void setCachingResourceManager(CachingResourceManager cachingResourceManager) {
		this.cachingResourceManager = cachingResourceManager;
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		// we need to call super.init() with:
		//  - a config that returns proper servletContext implementation
		//  - this servletContext has to return proper deployment
		//  - deployment is used to get deployment info and servlet context
		//  - deployment info is needed to call:
		//     - io.undertow.servlet.api.DeploymentInfo.getDefaultServletConfig() (can be null)
		//     - io.undertow.servlet.api.DeploymentInfo.getPreCompressedResources()
		//     - io.undertow.servlet.api.DeploymentInfo.getResourceManager() - that's the most important method
		//       we have to override
		//
		// I know it's not very good design, but pax-web-jetty and pax-web-tomcat use server native "resource servlet"
		// so I tried hard to use Undertow specific "resource servlet" as well
		// this special context is used to obtain tweaked io.undertow.servlet.api.Deployment object
		FlexibleDeployment deployment = new FlexibleDeployment(config.getServletContext(), cachingResourceManager);
		ServletContext flexibleServletContext = new FlexibleServletContextImpl(deployment);

		super.init(new ServletConfig() {
			@Override
			public String getServletName() {
				return config.getServletName();
			}

			@Override
			public ServletContext getServletContext() {
				return flexibleServletContext;
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

		// not we can configure our pieces needed for io.undertow.server.handlers.resource.ResourceManager
		if (baseDirectory != null) {
			this.resourceManager = FileResourceManager.builder()
					.setBase(Paths.get(baseDirectory.getAbsolutePath()))
					.setETagFunction(new FileETagFunction())
					.build();
		} else {
			// resource will be obtained from HttpContext.getResource() / ServletContextHelper.getResource()
			// assuming that servletContext is Osgi[Scoped]ServletContext that delegate to WebContainerContext
			// it's important to get ServletContext from the passed config!
			this.resourceManager = new OsgiResourceManager(chroot, config.getServletContext());
		}
	}

	@Override
	public Resource getResource(String path) throws IOException {
		Resource resource = resourceManager.getResource(path);
		if (resource != null && resource.isDirectory()) {
			// Just because we don't do it. Ever. Please use welcome files if needed.
			return null;
		}
		return resource;
	}

	@Override
	public boolean isResourceChangeListenerSupported() {
		return false;
	}

	@Override
	public void registerResourceChangeListener(ResourceChangeListener listener) {
		// no op
	}

	@Override
	public void removeResourceChangeListener(ResourceChangeListener listener) {
		// no op
	}

	@Override
	public void close() throws IOException {
		resourceManager.close();
	}

}
