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
package org.ops4j.pax.web.service.undertow.internal.context;

import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.servlet.handlers.DefaultServlet;

/**
 * Extension of {@link DefaultServlet}, so we can use many of such servlets to serve resources from different bases
 * for the purpose of "resource" handling specified in HttpService and Whiteboard specifications.
 */
public class FlexibleDefaultServlet extends DefaultServlet {

	private final ResourceManager resourceManager;

	/**
	 * Construct {@link FlexibleDefaultServlet} with {@link ResourceManager} that that will be used by original
	 * implementation of {@link DefaultServlet}.
	 *
	 * @param deployment
	 */
	public FlexibleDefaultServlet(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
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
		ServletContext flexibleServletContext
				= new FlexibleServletContextImpl(new FlexibleDeployment(config.getServletContext(), resourceManager));

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
	}

}
