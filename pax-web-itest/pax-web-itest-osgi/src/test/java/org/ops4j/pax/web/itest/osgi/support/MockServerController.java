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
package org.ops4j.pax.web.itest.osgi.support;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.servlet.Servlet;

import org.ops4j.pax.web.service.internal.ConfigurationImpl;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.config.JspConfiguration;
import org.ops4j.pax.web.service.spi.config.LogConfiguration;
import org.ops4j.pax.web.service.spi.config.SecurityConfiguration;
import org.ops4j.pax.web.service.spi.config.ServerConfiguration;
import org.ops4j.pax.web.service.spi.config.SessionConfiguration;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.ops4j.pax.web.service.spi.model.WelcomeFileModel;
import org.osgi.service.http.HttpContext;

/**
 * {@link ServerController} for test purposes
 */
public class MockServerController implements ServerController {

	private Configuration configuration;
	private boolean started;

	@Override
	public void start() {
		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public void configure(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public void addListener(ServerListener listener) {
	}

	@Override
	public void removeListener(ServerListener listener) {
	}

	@Override
	public void removeContext(HttpContext httpContext) {
	}

	@Override
	public void addServlet(ServletModel model) {
	}

	@Override
	public void removeServlet(ServletModel model) {
	}

	@Override
	public void addEventListener(EventListenerModel eventListenerModel) {
	}

	@Override
	public void removeEventListener(EventListenerModel eventListenerModel) {
	}

	@Override
	public void addFilter(FilterModel filterModel) {
	}

	@Override
	public void removeFilter(FilterModel filterModel) {
	}

	@Override
	public void addErrorPage(ErrorPageModel model) {
	}

	@Override
	public void removeErrorPage(ErrorPageModel model) {
	}

	@Override
	public void addWelcomFiles(WelcomeFileModel model) {
	}

	@Override
	public void removeWelcomeFiles(WelcomeFileModel model) {
	}

	@Override
	public LifeCycle getContext(ContextModel model) {
		return new LifeCycle() {
			@Override
			public void start() throws Exception {
			}

			@Override
			public void stop() throws Exception {
			}
		};
	}

	@Override
	public Integer getHttpPort() {
		return null;
	}

	@Override
	public Integer getHttpSecurePort() {
		return null;
	}

	@Override
	public Servlet createResourceServlet(ContextModel contextModel, String alias, String name) {
		return null;
	}

	@Override
	public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
	}

	@Override
	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
	}

}
