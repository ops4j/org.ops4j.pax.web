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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.servlet.ServletContext;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletDispatcher;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.core.ApplicationListeners;
import io.undertow.servlet.core.ErrorPages;
import io.undertow.servlet.core.ManagedFilters;
import io.undertow.servlet.core.ManagedServlets;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.servlet.spec.ServletContextImpl;

/**
 * Thin implementation of {@link Deployment} when only {@link Deployment#getDeploymentInfo()} is needed.
 */
public class FlexibleDeployment implements Deployment {

	private final DeploymentInfo deploymentInfo;
	private final ServletContextImpl servletContext;

	public FlexibleDeployment(final ServletContext servletContext, final ResourceManager resourceManager) {
		final Map<String, String> preCompressedResources = new HashMap<>();
		preCompressedResources.put("gzip", ".gz");
		this.deploymentInfo = new DeploymentInfo() {
			@Override
			public ResourceManager getResourceManager() {
				return resourceManager;
			}

			@Override
			public Map<String, String> getPreCompressedResources() {
				return preCompressedResources;
			}
		};

		this.servletContext = new ServletContextImpl(null, this) {
			@Override
			public String getMimeType(String file) {
				return servletContext.getMimeType(file);
			}
		};
	}

	@Override
	public DeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

	@Override
	public ServletContainer getServletContainer() {
		return null;
	}

	@Override
	public ApplicationListeners getApplicationListeners() {
		return null;
	}

	@Override
	public ManagedServlets getServlets() {
		return null;
	}

	@Override
	public ManagedFilters getFilters() {
		return null;
	}

	@Override
	public ServletContextImpl getServletContext() {
		return servletContext;
	}

	@Override
	public HttpHandler getHandler() {
		return null;
	}

	@Override
	public ServletPathMatches getServletPaths() {
		return null;
	}

	@Override
	public <T, C> ThreadSetupHandler.Action<T, C> createThreadSetupAction(ThreadSetupHandler.Action<T, C> target) {
		return null;
	}

	@Override
	public ErrorPages getErrorPages() {
		return null;
	}

	@Override
	public Map<String, String> getMimeExtensionMappings() {
		return null;
	}

	@Override
	public ServletDispatcher getServletDispatcher() {
		return null;
	}

	@Override
	public SessionManager getSessionManager() {
		return null;
	}

	@Override
	public Executor getExecutor() {
		return null;
	}

	@Override
	public Executor getAsyncExecutor() {
		return null;
	}

	@Override
	public Charset getDefaultCharset() {
		return null;
	}

	@Override
	public Charset getDefaultRequestCharset() {
		return null;
	}

	@Override
	public Charset getDefaultResponseCharset() {
		return null;
	}

	@Override
	public List<AuthenticationMechanism> getAuthenticationMechanisms() {
		return null;
	}

	@Override
	public DeploymentManager.State getDeploymentState() {
		return null;
	}

	@Override
	public Set<String> tryAddServletMappings(ServletInfo servletInfo, String... urlPatterns) {
		return null;
	}

}
