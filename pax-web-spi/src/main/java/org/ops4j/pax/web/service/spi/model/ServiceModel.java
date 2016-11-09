/* Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.spi.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;

import org.ops4j.lang.NullArgumentException;
import org.osgi.service.http.HttpContext;

public class ServiceModel {

	private final Map<String, ServletModel> aliasMapping;
	private final Set<ServletModel> servletModels;
	private final Map<String, FilterModel> filterModels;
	private final Map<EventListener, EventListenerModel> eventListenerModels;
	private final Map<String, LoginConfigModel> loginConfigModels;
	/**
	 * Mapping between the error and error page model.
	 */
	private final Map<String, ErrorPageModel> errorPageModels;
	private final Map<String, WelcomeFileModel> welcomeFileModels;
	private final Map<HttpContext, ContextModel> contextModels;
	private final Map<String, SecurityConstraintMappingModel> securityConstraintMappingModels;
	private final Map<ServletContainerInitializer, ContainerInitializerModel> containerInitializers;
	private final Map<Object, WebSocketModel> webSockets;

	public ServiceModel() {
		this.aliasMapping = new HashMap<>();
		this.servletModels = new HashSet<>();
		this.filterModels = new LinkedHashMap<>();
		this.eventListenerModels = new HashMap<>();
		this.errorPageModels = new HashMap<>();
		this.welcomeFileModels = new HashMap<>(); //PAXWEB-123
		this.contextModels = new HashMap<>();
		this.loginConfigModels = new HashMap<>(); // PAXWEB-210
		// -- added these her too.
		this.securityConstraintMappingModels = new HashMap<>();
		this.containerInitializers = new HashMap<>();
		this.webSockets = new HashMap<>();
	}

	public synchronized ServletModel getServletModelWithAlias(final String alias) {
		NullArgumentException.validateNotEmpty(alias, "Alias");
		return aliasMapping.get(alias);
	}

	public synchronized void addServletModel(final ServletModel model) {
		if (model.getAlias() != null) {
			aliasMapping.put(model.getAlias(), model);
		}
		servletModels.add(model);
		addContextModel(model.getContextModel());
	}

	public synchronized void removeServletModel(final ServletModel model) {
		if (model.getAlias() != null) {
			aliasMapping.remove(model.getAlias());
		}
		servletModels.remove(model);
	}

	public synchronized ServletModel removeServlet(final Servlet servlet) {
		final ServletModel model = findServletModel(servlet);
		if (model == null) {
			throw new IllegalArgumentException("Servlet [" + servlet
					+ " is not currently registered in any context");
		}
		servletModels.remove(servlet);
		return model;
	}

	public synchronized ServletModel removeServlet(final String servletName) {
		ServletModel model = findServletModel(servletName);
		if (model == null) {
			throw new IllegalArgumentException("Servlet with name [" + servletName + "] is currently not registered in any context");
		}
		Servlet servlet = model.getServlet();
		servletModels.remove(servlet);
		return model;
	}

	private synchronized ServletModel findServletModel(Servlet servlet) {
		for (ServletModel servletModel : servletModels) {
			if (servletModel.getServlet() != null
					&& servletModel.getServlet().equals(servlet)) {
				return servletModel;
			}
		}
		return null;
	}

	private synchronized ServletModel findServletModel(String servletName) {
		for (ServletModel servletModel : servletModels) {
			if (servletModel.getName() != null && servletModel.getName().equalsIgnoreCase(servletName)) {
				return servletModel;
			}
		}
		return null;
	}

	public synchronized Set<ServletModel> removeServletClass(
			final Class<? extends Servlet> servletClass) {
		final Set<ServletModel> models = findServletModels(servletClass);
		if (models == null) {
			throw new IllegalArgumentException("Servlet class [" + servletClass
					+ " is not currently registered in any context");
		}
		servletModels.removeAll(models);
		return models;
	}

	private synchronized Set<ServletModel> findServletModels(
			final Class<? extends Servlet> servletClass) {
		Set<ServletModel> foundServletModels = null;
		for (ServletModel servletModel : servletModels) {
			if (servletModel.getServletClass() != null
					&& servletModel.getServletClass().equals(servletClass)) {
				if (foundServletModels == null) {
					foundServletModels = new HashSet<>();
				}
				foundServletModels.add(servletModel);
			}
		}
		return foundServletModels;
	}

	public synchronized void addEventListenerModel(
			final EventListenerModel model) {
		if (eventListenerModels.containsKey(model.getEventListener())) {
			throw new IllegalArgumentException("Listener ["
					+ model.getEventListener() + "] already registered.");
		}
		eventListenerModels.put(model.getEventListener(), model);
		addContextModel(model.getContextModel());
	}

	public synchronized EventListenerModel removeEventListener(
			final EventListener listener) {
		final EventListenerModel model;
		model = eventListenerModels.get(listener);
		if (model == null) {
			throw new IllegalArgumentException("Listener [" + listener
					+ " is not currently registered in any context");
		}
		eventListenerModels.remove(listener);
		return model;
	}

	public synchronized void addFilterModel(final FilterModel model) {
		String name = model.getName();

		Filter filter = model.getFilter();
		Class<? extends Filter> filterClass = model.getFilterClass();

		if (filterModels.containsKey(name)) {
			if (filter != null) {
				throw new IllegalArgumentException("Filter [" + model.getFilter()
						+ "] is already registered.");
			}
			if (filterClass != null) {
				throw new IllegalArgumentException("FilterClass [" + filterClass
						+ "] is already registered.");
			}
		}
		filterModels.put(name, model);
		addContextModel(model.getContextModel());
	}

	public synchronized FilterModel removeFilter(final Filter filter) {
		Set<FilterModel> models = findFilterModels(filter);
		if (models == null || models.isEmpty()) {
			throw new IllegalArgumentException("Filter [" + filter
					+ " is not currently registered in any context");
		}
		filterModels.values().removeAll(models);
		return models.iterator().next();
	}

	public synchronized FilterModel removeFilter(final String filterName) {
		return filterModels.remove(filterName);
	}

	public synchronized FilterModel removeFilter(
			final Class<? extends Filter> filterClass) {
		final Set<FilterModel> models = findFilterModels(filterClass);
		if (models == null || models.isEmpty()) {
			throw new IllegalArgumentException("Servlet class [" + filterClass
					+ " is not currently registered in any context");
		}
		filterModels.values().removeAll(models);
		return models.iterator().next();
	}
	
	/*
	private synchronized Set<Filter> findFilter(
			final Class<? extends Filter> filterClass) {
		Set<Filter> foundFilterModels = null;
		for (FilterModel filterModel : filterModels) {
			if (filterModel.getFilterClass() != null
					&& filterModel.getFilterClass().equals(filterClass)) {
				if (foundFilterModels == null) {
					foundFilterModels = new HashSet<Filter>();
				}
				foundFilterModels.add(filterModel.getFilter());
			}
		}
		return foundFilterModels;
	}
	*/

	private synchronized Set<FilterModel> findFilterModels(
			final Class<? extends Filter> filterClass) {
		Set<FilterModel> foundFilterModels = null;
		for (FilterModel filterModel : filterModels.values()) {
			if (filterModel.getFilterClass() != null
					&& filterModel.getFilterClass().equals(filterClass)) {
				if (foundFilterModels == null) {
					foundFilterModels = new HashSet<>();
				}
				foundFilterModels.add(filterModel);
			}
		}
		return foundFilterModels;
	}

	private synchronized Set<FilterModel> findFilterModels(
			final Filter filter) {
		Set<FilterModel> foundFilterModels = null;
		for (FilterModel filterModel : filterModels.values()) {
			if (filterModel.getFilter() != null
					&& filterModel.getFilter().equals(filter)) {
				if (foundFilterModels == null) {
					foundFilterModels = new HashSet<>();
				}
				foundFilterModels.add(filterModel);
			}
		}
		return foundFilterModels;
	}

	public synchronized ServletModel[] getServletModels() {
		return servletModels.toArray(new ServletModel[servletModels.size()]);
	}

	public synchronized EventListenerModel[] getEventListenerModels() {
		final Collection<EventListenerModel> models = eventListenerModels
				.values();
		return models.toArray(new EventListenerModel[models.size()]);
	}

	public synchronized FilterModel[] getFilterModels() {
		final Collection<FilterModel> models = filterModels.values();
		return models.toArray(new FilterModel[models.size()]);
	}

	public synchronized ErrorPageModel[] getErrorPageModels() {
		final Collection<ErrorPageModel> models = errorPageModels.values();
		return models.toArray(new ErrorPageModel[models.size()]);
	}

	public synchronized void addContextModel(final ContextModel contextModel) {
		if (!contextModels.containsKey(contextModel.getHttpContext())) {
			contextModels.put(contextModel.getHttpContext(), contextModel);
		}
	}

	public synchronized ContextModel[] getContextModels() {
		final Collection<ContextModel> contextModelValues = contextModels
				.values();
		if (contextModelValues.size() == 0) {
			return new ContextModel[0];
		}
		return contextModelValues.toArray(new ContextModel[contextModelValues
				.size()]);
	}

	public synchronized ContextModel getContextModel(final HttpContext httpContext) {
		return contextModels.get(httpContext);
	}

	public synchronized void addErrorPageModel(final ErrorPageModel model) {
		final String key = model.getError() + "|"
				+ model.getContextModel().getId();
		if (errorPageModels.containsKey(key)) {
			throw new IllegalArgumentException("Error page for ["
					+ model.getError() + "] already registered.");
		}
		errorPageModels.put(key, model);
		addContextModel(model.getContextModel());
	}

	public synchronized ErrorPageModel removeErrorPage(final String error,
													   final ContextModel contextModel) {
		final ErrorPageModel model;
		final String key = error + "|" + contextModel.getId();
		model = errorPageModels.get(key);
		if (model == null) {
			throw new IllegalArgumentException("Error page for [" + error
					+ "] cannot be found in the provided http context");
		}
		errorPageModels.remove(key);
		return model;
	}

	public synchronized void addWelcomeFileModel(WelcomeFileModel model) {
		final String key = Arrays.toString(model.getWelcomeFiles()) + "|" + model.getContextModel().getId();
		if (welcomeFileModels.containsKey(key)) {
			throw new IllegalArgumentException("Welcome files for [" + Arrays.toString(model.getWelcomeFiles()) + "] already registered.");
		}
		welcomeFileModels.put(key, model);
		addContextModel(model.getContextModel());
	}

	public synchronized WelcomeFileModel removeWelcomeFileModel(String welcomeFiles, ContextModel contextModel) {
		final WelcomeFileModel model;
		final String key = welcomeFiles + "|" + contextModel.getId();
		model = welcomeFileModels.get(key);
		if (model == null) {
			throw new IllegalArgumentException("WelcomeFiles for [" + welcomeFiles
					+ "] cannot be found in the provided http context");
		}
		welcomeFileModels.remove(key);
		return model;
	}

	public synchronized void addLoginModel(LoginConfigModel model) {
		if (loginConfigModels.containsKey(model.getRealmName())) {
			throw new IllegalArgumentException("Login Config ["
					+ model.getRealmName() + "] is already registered.");
		}
		loginConfigModels.put(model.getRealmName(), model);
		addContextModel(model.getContextModel());
	}

	public synchronized LoginConfigModel[] getLoginModels() {
		Collection<LoginConfigModel> loginModels = loginConfigModels.values();
		return loginModels.toArray(new LoginConfigModel[loginModels.size()]);
	}

	public synchronized void addSecurityConstraintMappingModel(
			SecurityConstraintMappingModel model) {
		if (securityConstraintMappingModels.containsKey(model
				.getConstraintName())) {
			throw new IllegalArgumentException("Security Mapping ["
					+ model.getConstraintName() + "] is already registered.");
		}
		securityConstraintMappingModels.put(model.getConstraintName(), model);
		addContextModel(model.getContextModel());
	}

	public synchronized SecurityConstraintMappingModel[] getSecurityConstraintMappings() {
		Collection<SecurityConstraintMappingModel> collection = securityConstraintMappingModels
				.values();
		return collection.toArray(new SecurityConstraintMappingModel[collection
				.size()]);
	}

	public synchronized void addContainerInitializerModel(
			ContainerInitializerModel model) {
		if (containerInitializers.containsKey(model.getContainerInitializer())) {
			throw new IllegalArgumentException("ServletContainerInitializer "
					+ model.getContainerInitializer() + " already registered");
		}
		containerInitializers.put(model.getContainerInitializer(), model);
	}

	public synchronized void removeContainerInitializerModel(
			ContainerInitializerModel model) {
		//NOOP
	}


	public void addWebSocketModel(WebSocketModel model) {
		if (webSockets.containsKey(model.getWebSocket())) {
			throw new IllegalArgumentException("WebSocket " + model.getWebSocket() + " already registered");
		}
		webSockets.put(model.getWebSocket(), model);
	}


	public void removeWebSocketModel(Object webSocket) {
		webSockets.remove(webSocket);
	}

	/**
	 * Returns true if the context can still be configured. This is possible
	 * before any web components (servlets / filters / listeners / error pages)
	 * are registered. TODO verify what happen once the web elements are
	 * registered and then unregistered. Can still be configured?
	 *
	 * @param httpContext created by the service of this model
	 * @return true, if context can be configured false otherwise
	 */
	public synchronized boolean canBeConfigured(HttpContext httpContext) {
		return canBeConfigured(httpContext, servletModels)
				&& canBeConfigured(httpContext, filterModels.values())
				&& canBeConfigured(httpContext, eventListenerModels.values())
				&& canBeConfigured(httpContext, errorPageModels.values())
				&& canBeConfigured(httpContext, loginConfigModels.values());
	}

	private boolean canBeConfigured(HttpContext httpContext,
									Collection<? extends Model> models) {
		for (Model model : models) {
			ContextModel contextModel = model.getContextModel();
			HttpContext candidateHttpContext = contextModel.getHttpContext();
			if (candidateHttpContext.equals(httpContext)) {
				return false;
			}
		}
		return true;
	}

}