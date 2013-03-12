package org.ops4j.pax.web.service.tomcat.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.Servlet;

import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.pax.web.service.spi.LifeCycle;
import org.ops4j.pax.web.service.spi.model.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;

/**
 * @author Romain Gilles
 */
abstract class AbstractServerState implements ServerState {
	private final ServerStateFactory serverStateFactory;

	public AbstractServerState(ServerStateFactory serverStateFactory) {
		this.serverStateFactory = serverStateFactory;
	}

	ServerStateFactory getServerStateFactory() {
		return serverStateFactory;
	}

	<T> T throwIllegalState() throws IllegalStateException {
		return throwIllegalState(getState(), getSupportedOperations());
	}

	private <T> T throwIllegalState(States serverState,
			Collection<String> supportedOperations) {
		throw new IllegalStateException(
				String.format(
						"server current state is: %s. The only supported operation(s): %s",
						serverState, supportedOperations));
	}

	Collection<String> getSupportedOperations() {
		ArrayList<String> result = new ArrayList<String>();
		result.add(formatSupportedOperation("configure", Configuration.class));
		return result;
	}

	String formatSupportedOperation(String methodName, Class<?>... parameters) {
		StringBuilder result = new StringBuilder();
		result.append('#').append(methodName).append('(');
		if (parameters != null) {
			Iterator<Class<?>> iterator = Arrays.asList(parameters).iterator();
			if (iterator.hasNext()) {
				result.append(iterator.next().getSimpleName());
			}
			while (iterator.hasNext()) {
				result.append(", ").append(iterator.next().getSimpleName());
			}
		}
		result.append(')');
		return result.toString();
	}

	@Override
	public Configuration getConfiguration() {
		return throwIllegalState();
	}

	@Override
	public void removeContext(HttpContext httpContext) {
		throwIllegalState();
	}

	@Override
	public void addServlet(ServletModel model) {
		throwIllegalState();
	}

	@Override
	public void removeServlet(ServletModel model) {
		throwIllegalState();
	}

	@Override
	public void addEventListener(EventListenerModel eventListenerModel) {
		throwIllegalState();
	}

	@Override
	public void removeEventListener(EventListenerModel eventListenerModel) {
		throwIllegalState();
	}

	@Override
	public void addFilter(FilterModel filterModel) {
		throwIllegalState();
	}

	@Override
	public void removeFilter(FilterModel filterModel) {
		throwIllegalState();
	}

	@Override
	public void addErrorPage(ErrorPageModel model) {
		throwIllegalState();
	}

	@Override
	public void removeErrorPage(ErrorPageModel model) {
		throwIllegalState();
	}

	@Override
	public Integer getHttpPort() {
		return throwIllegalState();
	}

	@Override
	public Integer getHttpSecurePort() {
		return throwIllegalState();
	}

	@Override
	public Servlet createResourceServlet(ContextModel contextModel,
			String alias, String name) {
		return throwIllegalState();
	}

	@Override
	public void addSecurityConstraintMapping(
			SecurityConstraintMappingModel secMapModel) {
		throwIllegalState();
	}

	@Override
	public void addContainerInitializerModel(ContainerInitializerModel model) {
		throwIllegalState();
	}

	@Override
	public LifeCycle getContext(ContextModel model) {
		return throwIllegalState();
	}
}