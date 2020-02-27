/*
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
package org.ops4j.pax.web.service.tomcat.internal;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.ServerEvent;
import org.ops4j.pax.web.service.spi.ServerListener;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Romain Gilles
 */
class TomcatServerController implements ServerController {

	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServerController.class);
	private ServerState serverState;

	private final Set<ServerListener> listeners = newThreadSafeSet();

	private TomcatServerController(ServerState initialState) {
		this.serverState = initialState;
	}

	private Set<ServerListener> newThreadSafeSet() {
		// return new ConcurrentSkipListSet<ServerListener>();
		return new CopyOnWriteArraySet<>();
	}

	@Override
	public void start() {
		LOG.debug("start server");
		serverState = serverState.start();
		fireStateChange(ServerEvent.STARTED);
	}

	@Override
	public void stop() {
		LOG.debug("stop server");
		serverState = serverState.stop();
		fireStateChange(ServerEvent.STOPPED);
	}

	@Override
	public boolean isStarted() {
		return serverState.isStarted();
	}

	@Override
	public boolean isConfigured() {
		return serverState.isConfigured();
	}

	@Override
	public void configure(Configuration configuration) {
		LOG.debug("set configuration");
		serverState = serverState.configure(configuration);
		fireStateChange(ServerEvent.CONFIGURED);
		this.start();
	}

	@Override
	public Configuration getConfiguration() {
		return serverState.getConfiguration();
	}

	@Override
	public void addListener(ServerListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ServerListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void removeContext(HttpContext httpContext) {
		serverState.removeContext(httpContext);
	}

	@Override
	public void addServlet(ServletModel model) {
		serverState.addServlet(model);
	}

	@Override
	public void removeServlet(ServletModel model) {
		serverState.removeServlet(model);
	}

	@Override
	public void addEventListener(EventListenerModel eventListenerModel) {
		serverState.addEventListener(eventListenerModel);
	}

	@Override
	public void removeEventListener(EventListenerModel eventListenerModel) {
		serverState.removeEventListener(eventListenerModel);
	}

	@Override
	public void addFilter(FilterModel filterModel) {
		serverState.addFilter(filterModel);
	}

	@Override
	public void removeFilter(FilterModel filterModel) {
		serverState.removeFilter(filterModel);
	}

	@Override
	public void addErrorPage(ErrorPageModel model) {
		serverState.addErrorPage(model);
	}

	@Override
	public void removeErrorPage(ErrorPageModel model) {
		serverState.removeErrorPage(model);
	}

	@Override
	public LifeCycle getContext(OsgiContextModel model) {
		return serverState.getContext(model);
	}

	@Override
	public Integer getHttpPort() {
		return serverState.getHttpPort();
	}

	@Override
	public Integer getHttpSecurePort() {
		return serverState.getHttpSecurePort();
	}

	@Override
	public Servlet createResourceServlet(OsgiContextModel contextModel,
										 String alias, String name) {
		return serverState.createResourceServlet(contextModel, alias, name);
	}

	@Override
	public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
		serverState.addSecurityConstraintMapping(secMapModel);
	}

	@Override
	public void removeSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel) {
		serverState.removeSecurityConstraintMapping(secMapModel);
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
		serverState.addContainerInitializerModel(model);
	}

	private void fireStateChange(ServerEvent event) {
		for (ServerListener listener : listeners) {
			listener.stateChanged(event);
		}
	}

	static ServerController newInstance(ServerState serverState) {
		return new TomcatServerController(serverState);
	}

	@Override
	public void addWelcomFiles(WelcomeFileModel model) {
		serverState.addWelcomeFiles(model);
	}

	@Override
	public void removeWelcomeFiles(WelcomeFileModel model) {
		serverState.removeWelcomeFiles(model);
	}

}
