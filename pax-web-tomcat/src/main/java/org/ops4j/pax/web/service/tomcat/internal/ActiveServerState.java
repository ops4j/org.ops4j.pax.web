/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import static org.ops4j.pax.web.service.tomcat.internal.ServerState.States.ACTIVE;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
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
 * @author Romaim Gilles
 */
class ActiveServerState extends AbstractServerState implements ServerState {

	private final ServerState initializedState;
	private final ServerWrapper serverWrapper;

	ActiveServerState(ServerStateFactory serverStateFactory,
					  ServerState initializedState, ServerWrapper serverWrapper) {
		super(serverStateFactory);
		this.initializedState = initializedState;
		this.serverWrapper = serverWrapper;
	}

	@Override
	public Servlet createResourceServlet(ContextModel contextModel,
										 String alias, String name) {
		return serverWrapper.createResourceServlet(contextModel, alias, name);
	}

	@Override
	public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
		serverWrapper.addSecurityConstraintMapping(secMapModel);
	}

	@Override
	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
		serverWrapper.removeSecurityConstraintMapping(secMapModel);
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
		super.addContainerInitializerModel(model);
	}

	static ServerState getInstance(ServerStateFactory serverStateFactory,
								   ServerState initializedState, ServerWrapper server) {
		return new ActiveServerState(serverStateFactory, initializedState,
				server);
	}

	@Override
	public ServerState start() {
		return throwIllegalState();
	}

	@Override
	public ServerState stop() {
		serverWrapper.stop();
		return initializedState;
	}

	@Override
	public boolean isStarted() {
		return true;
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public ServerState configure(Configuration configuration) {
		return stop().configure(configuration).start();
	}

	@Override
	public States getState() {
		return ACTIVE;
	}

	@Override
	public Configuration getConfiguration() {
		return initializedState.getConfiguration();
	}

	@Override
	public void addServlet(ServletModel model) {
		serverWrapper.addServlet(model);
	}

	@Override
	public void removeServlet(ServletModel model) {
		serverWrapper.removeServlet(model);
	}

	@Override
	public void removeContext(HttpContext httpContext) {
		serverWrapper.removeContext(httpContext);
	}

	@Override
	public void addErrorPage(ErrorPageModel model) {
		serverWrapper.addErrorPage(model);
	}

	@Override
	public void removeErrorPage(ErrorPageModel model) {
		serverWrapper.removeErrorPage(model);
	}

	@Override
	public void addFilter(FilterModel filterModel) {
		serverWrapper.addFilter(filterModel);
	}

	@Override
	public void removeFilter(FilterModel filterModel) {
		serverWrapper.removeFilter(filterModel);
	}

	@Override
	public void addEventListener(EventListenerModel eventListenerModel) {
		serverWrapper.addEventListener(eventListenerModel);
	}

	@Override
	public void removeEventListener(EventListenerModel eventListenerModel) {
		serverWrapper.removeEventListener(eventListenerModel);
	}

	@Override
	Collection<String> getSupportedOperations() {
		// TODO

		Collection<String> result = new ArrayList<>();
		result.add("#*(...)");
		return result;
	}

	@Override
	public Integer getHttpPort() {
		return initializedState.getHttpPort();
	}

	@Override
	public Integer getHttpSecurePort() {
		return initializedState.getHttpSecurePort();
	}

	@Override
	public LifeCycle getContext(ContextModel model) {
		return serverWrapper.getContext(model);
	}

	@Override
	public void addWelcomeFiles(WelcomeFileModel model) {
		serverWrapper.addWelcomeFiles(model);
	}

	@Override
	public void removeWelcomeFiles(WelcomeFileModel model) {
		serverWrapper.removeWelcomeFiles(model);
	}
}
