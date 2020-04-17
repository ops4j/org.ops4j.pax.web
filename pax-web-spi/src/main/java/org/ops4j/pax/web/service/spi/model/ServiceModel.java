/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;

/**
 * <p>Service Model is kept at {@link org.osgi.service.http.HttpService} level, which is bundle-scoped in Pax Web
 * (though Http Service specification doesn't mention the scope of Http Service). Its goal is to remember which
 * <em>web elements</em> were registered from given bundle (both {@link org.osgi.service.http.HttpService} usage
 * and Whiteboard service registrations).</p>
 *
 * <p>This is just organizational separation, because the models are kept anyway at {@link ServerModel} and
 * {@link OsgiContextModel} levels. It's required to correctly handle bundle-scoped unregistrations.</p>
 *
 * <p>This bundle-scoped model is the <em>main</em> {@link BatchVisitor}, though it may delegate some
 * visiting methods to other parts of the model.</p>
 *
 * <p>Even if model elements (like {@link ServletModel} are added here, initial validation (whether given model
 * can be added at all without causing conflicts), validation is performed at {@link ServerModel} level.</p>
 */
public class ServiceModel implements BatchVisitor {

	/**
	 * Full {@link ServerModel}, while this {@link ServiceModel} collects elements registered within the scope
	 * of single bundle-scoped {@link org.osgi.service.http.HttpService} or (using Whiteboard) single
	 * {@link org.osgi.framework.BundleContext#registerService bundle context}.
	 */
	private final ServerModel serverModel;

	/**
	 * <p>Servlets registered under alias in given context path (exact URL pattern) by given bundle-scoped
	 * {@link org.osgi.service.http.HttpService}. Group of disjoint slices of
	 * {@link org.ops4j.pax.web.service.spi.model.ServletContextModel#aliasMapping} for all context mappings.</p>
	 *
	 * <p>Kept to fulfill the contract of {@link org.osgi.service.http.HttpService#unregister(String)}, which
	 * doesn't distinguish servlets registered under the same alias into different <em>contexts</em>.</p>
	 *
	 * <p>Two different servlets can be registered into two different context paths ({@link ServletContextModel})
	 * through two different {@link OsgiContextModel} under the same alias.
	 * {@link org.osgi.service.http.HttpService#unregister(String)} should unregister both of them.</p>
	 *
	 * <p>Also, if single servlet model is registered into two different contexts, both of them should be unregistered
	 * when needed.</p>
	 *
	 * <p>This map is keyed by alias, then by context path, because the original mapping is not global, but scoped to
	 * {@link ServletContextModel}.</p>
	 */
	private final Map<String, Map<String, ServletModel>> aliasMapping = new HashMap<>();

	/** All servlet models registered by given bundle-scoped {@link org.osgi.service.http.HttpService}. */
	private final Set<ServletModel> servletModels = new HashSet<>();

	/** All filter models registered by given bundle-scoped {@link org.osgi.service.http.HttpService}. */
	private final Set<FilterModel> filterModels = new HashSet<>();

	public ServiceModel(ServerModel serverModel) {
		this.serverModel = serverModel;
	}

//	private final Map<EventListener, EventListenerModel> eventListenerModels;
//	private final Map<String, ErrorPageModel> errorPageModels;
//	private final Map<String, WelcomeFileModel> welcomeFileModels;
//	private final Map<HttpContext, OsgiContextModel> contextModels;
//	private final Map<String, SecurityConstraintMappingModel> securityConstraintMappingModels;
//	private final Map<ServletContainerInitializer, ContainerInitializerModel> containerInitializers;
//	private final Map<Object, WebSocketModel> webSockets;

	public Map<String, Map<String, ServletModel>> getAliasMapping() {
		return aliasMapping;
	}

	public Set<ServletModel> getServletModels() {
		return servletModels;
	}

	public Set<FilterModel> getFilterModels() {
		return filterModels;
	}

	@Override
	public void visit(ServletContextModelChange change) {
		serverModel.visit(change);
	}

	@Override
	public void visit(OsgiContextModelChange change) {
		switch (change.getKind()) {
			case ASSOCIATE:
				serverModel.associateHttpContext(change.getContext(), change.getOsgiContextModel());
				break;
			case ADD:
			default:
				break;
		}
	}

	@Override
	public void visit(ServletModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ServletModel model = change.getServletModel();

			// apply the change at ServiceModel level - whether it's disabled or not
			if (model.getAlias() != null) {
				// alias mapping - remember model under each alias->contextPath
				Map<String, ServletModel> contexts = aliasMapping.computeIfAbsent(model.getAlias(), alias -> new HashMap<>());

				for (OsgiContextModel context : change.getServletModel().getContextModels()) {
					String contextPath = context.getServletContextModel().getContextPath();
					contexts.put(contextPath, model);
				}
			}
			servletModels.add(model);

			// and the change should be processed at serverModel level
			serverModel.visit(change);
			return;
		}

		if (change.getKind() == OpCode.DELETE) {
			List<ServletModel> modelsToRemove = change.getServletModels();

			// apply the change at ServiceModel level - whether it's disabled or not
			for (ServletModel model : modelsToRemove) {
				if (model.getAlias() != null) {
					aliasMapping.remove(model.getAlias());
				}
				this.servletModels.remove(model);
			}

			// the change should be processed at serverModel level as well
			serverModel.visit(change);
			return;
		}

		if (change.getKind() == OpCode.ENABLE || change.getKind() == OpCode.DISABLE) {
			// only alter server model - enabled or disabled model stays "registered" at serviceModel level
			serverModel.visit(change);
		}
	}

	@Override
	public void visit(FilterModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			FilterModel model = change.getFilterModel();

			// apply the change at ServiceModel level - whether it's disabled or not
			filterModels.add(model);
			// the change should also be processed at serverModel level
			serverModel.visit(change);
			return;
		}

		if (change.getKind() == OpCode.DELETE) {
			List<FilterModel> modelsToRemove = change.getFilterModels();

			// apply the change at ServiceModel level - whether it's disabled or not
			for (FilterModel model : modelsToRemove) {
				this.filterModels.remove(model);
			}
			// the change should be processed at serverModel level as well
			serverModel.visit(change);
			return;
		}

		if (change.getKind() == OpCode.ENABLE || change.getKind() == OpCode.DISABLE) {
			serverModel.visit(change);
		}
	}

	@Override
	public void visit(FilterStateChange change) {
		// no op here. At model level (unlike in server controller level), filters are added/removed individually
	}











//	public synchronized ServletModel getServletModelWithAlias(final String alias) {
//		NullArgumentException.validateNotEmpty(alias, "Alias");
//		return aliasMapping.get(alias);
//	}
//
//	public synchronized void removeServletModel(final ServletModel model) {
//		if (model.getAlias() != null) {
//			aliasMapping.remove(model.getAlias());
//		}
//		servletModels.remove(model);
//	}
//
//	public synchronized ServletModel removeServlet(final Servlet servlet) {
//		final ServletModel model = findServletModel(servlet);
//		if (model == null) {
//			throw new IllegalArgumentException("Servlet [" + servlet
//					+ " is not currently registered in any context");
//		}
//		servletModels.remove(model);
//		return model;
//	}
//
//	public synchronized ServletModel removeServlet(final String servletName) {
//		ServletModel model = findServletModel(servletName);
//		if (model == null) {
//			throw new IllegalArgumentException("Servlet with name [" + servletName + "] is currently not registered in any context");
//		}
//		servletModels.remove(model);
//		return model;
//	}
//
//	private synchronized ServletModel findServletModel(Servlet servlet) {
//		for (ServletModel servletModel : servletModels) {
//			if (servletModel.getServlet() != null
//					&& servletModel.getServlet().equals(servlet)) {
//				return servletModel;
//			}
//		}
//		return null;
//	}
//
//	private synchronized ServletModel findServletModel(String servletName) {
//		for (ServletModel servletModel : servletModels) {
//			if (servletModel.getName() != null && servletModel.getName().equalsIgnoreCase(servletName)) {
//				return servletModel;
//			}
//		}
//		return null;
//	}
//
//	public synchronized Set<ServletModel> removeServletClass(
//			final Class<? extends Servlet> servletClass) {
//		final Set<ServletModel> models = findServletModels(servletClass);
//		if (models == null) {
//			throw new IllegalArgumentException("Servlet class [" + servletClass
//					+ " is not currently registered in any context");
//		}
//		servletModels.removeAll(models);
//		return models;
//	}
//
//	private synchronized Set<ServletModel> findServletModels(
//			final Class<? extends Servlet> servletClass) {
//		Set<ServletModel> foundServletModels = null;
//		for (ServletModel servletModel : servletModels) {
//			if (servletModel.getServletClass() != null
//					&& servletModel.getServletClass().equals(servletClass)) {
//				if (foundServletModels == null) {
//					foundServletModels = new HashSet<>();
//				}
//				foundServletModels.add(servletModel);
//			}
//		}
//		return foundServletModels;
//	}

//	public synchronized void addEventListenerModel(
//			final EventListenerModel model) {
//		if (eventListenerModels.containsKey(model.getEventListener())) {
//			throw new IllegalArgumentException("Listener ["
//					+ model.getEventListener() + "] already registered.");
//		}
//		eventListenerModels.put(model.getEventListener(), model);
//		addContextModel(model.getContextModels());
//	}
//
//	public synchronized EventListenerModel removeEventListener(
//			final EventListener listener) {
//		final EventListenerModel model;
//		model = eventListenerModels.get(listener);
//		if (model == null) {
//			throw new IllegalArgumentException("Listener [" + listener
//					+ " is not currently registered in any context");
//		}
//		eventListenerModels.remove(listener);
//		return model;
//	}
//
//	public synchronized void addFilterModel(final FilterModel model) {
//		String name = model.getName();
//
//		Filter filter = model.getFilter();
//		Class<? extends Filter> filterClass = model.getFilterClass();
//
//		if (filterModels.containsKey(name)) {
//			if (filter != null) {
//				throw new IllegalArgumentException("Filter [" + model.getFilter()
//						+ "] is already registered.");
//			}
//			if (filterClass != null) {
//				throw new IllegalArgumentException("FilterClass [" + filterClass
//						+ "] is already registered.");
//			}
//		}
//		filterModels.put(name, model);
//		addContextModel(model.getContextModels());
//	}
//
//	public synchronized FilterModel removeFilter(final Filter filter) {
//		Set<FilterModel> models = findFilterModels(filter);
//		if (models == null || models.isEmpty()) {
//			throw new IllegalArgumentException("Filter [" + filter
//					+ " is not currently registered in any context");
//		}
//		filterModels.values().removeAll(models);
//		return models.iterator().next();
//	}
//
//	public synchronized FilterModel removeFilter(final String filterName) {
//		return filterModels.remove(filterName);
//	}
//
//	public synchronized FilterModel removeFilter(
//			final Class<? extends Filter> filterClass) {
//		final Set<FilterModel> models = findFilterModels(filterClass);
//		if (models == null || models.isEmpty()) {
//			throw new IllegalArgumentException("Servlet class [" + filterClass
//					+ " is not currently registered in any context");
//		}
//		filterModels.values().removeAll(models);
//		return models.iterator().next();
//	}
	
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

//	private synchronized Set<FilterModel> findFilterModels(
//			final Class<? extends Filter> filterClass) {
//		Set<FilterModel> foundFilterModels = null;
//		for (FilterModel filterModel : filterModels.values()) {
//			if (filterModel.getFilterClass() != null
//					&& filterModel.getFilterClass().equals(filterClass)) {
//				if (foundFilterModels == null) {
//					foundFilterModels = new HashSet<>();
//				}
//				foundFilterModels.add(filterModel);
//			}
//		}
//		return foundFilterModels;
//	}
//
//	private synchronized Set<FilterModel> findFilterModels(
//			final Filter filter) {
//		Set<FilterModel> foundFilterModels = null;
//		for (FilterModel filterModel : filterModels.values()) {
//			if (filterModel.getFilter() != null
//					&& filterModel.getFilter().equals(filter)) {
//				if (foundFilterModels == null) {
//					foundFilterModels = new HashSet<>();
//				}
//				foundFilterModels.add(filterModel);
//			}
//		}
//		return foundFilterModels;
//	}
//
//	public synchronized ServletModel[] getServletModels() {
//		return servletModels.toArray(new ServletModel[servletModels.size()]);
//	}
//
//	public synchronized EventListenerModel[] getEventListenerModels() {
//		final Collection<EventListenerModel> models = eventListenerModels
//				.values();
//		return models.toArray(new EventListenerModel[models.size()]);
//	}
//
//	public synchronized FilterModel[] getFilterModels() {
//		final Collection<FilterModel> models = filterModels.values();
//		return models.toArray(new FilterModel[models.size()]);
//	}
//
//	public synchronized ErrorPageModel[] getErrorPageModels() {
//		final Collection<ErrorPageModel> models = errorPageModels.values();
//		return models.toArray(new ErrorPageModel[models.size()]);
//	}
//
//	public synchronized void addContextModel(final List<OsgiContextModel> contextModels) {
////		if (!contextModels.containsKey(contextModel.getHttpContext())) {
////			contextModels.put(contextModel.getHttpContext(), contextModel);
////		}
//	}
//
//	public synchronized OsgiContextModel[] getContextModels() {
//		final Collection<OsgiContextModel> contextModelValues = contextModels
//				.values();
//		if (contextModelValues.isEmpty()) {
//			return new OsgiContextModel[0];
//		}
//		return contextModelValues.toArray(new OsgiContextModel[contextModelValues
//				.size()]);
//	}
//
//	public synchronized OsgiContextModel getContextModel(final HttpContext httpContext) {
//		return contextModels.get(httpContext);
//	}
//
//	public synchronized void addErrorPageModel(final ErrorPageModel model) {
////		final String key = model.getError() + "|"
////				+ model.getContextModel().getId();
////		if (errorPageModels.containsKey(key)) {
////			throw new IllegalArgumentException("Error page for ["
////					+ model.getError() + "] already registered.");
////		}
////		errorPageModels.put(key, model);
//		addContextModel(model.getContextModels());
//	}
//
//	public synchronized ErrorPageModel removeErrorPage(final String error,
//													   final OsgiContextModel contextModel) {
//		final ErrorPageModel model;
//		final String key = error + "|" + contextModel.getId();
//		model = errorPageModels.get(key);
//		if (model == null) {
//			throw new IllegalArgumentException("Error page for [" + error
//					+ "] cannot be found in the provided http context");
//		}
//		errorPageModels.remove(key);
//		return model;
//	}
//
//	public synchronized void addWelcomeFileModel(WelcomeFileModel model) {
////		final String key = Arrays.toString(model.getWelcomeFiles()) + "|" + model.getContextModel().getId();
////		if (welcomeFileModels.containsKey(key)) {
////			throw new IllegalArgumentException("Welcom files for [" + Arrays.toString(model.getWelcomeFiles()) + "] already registered.");
////		}
////		welcomeFileModels.put(key, model);
//		addContextModel(model.getContextModels());
//	}
//
//	public synchronized WelcomeFileModel removeWelcomeFileModel(String welcomeFiles, OsgiContextModel contextModel) {
//		final WelcomeFileModel model;
//		final String key = welcomeFiles + "|" + contextModel.getId();
//		model = welcomeFileModels.get(key);
//		if (model == null) {
//			throw new IllegalArgumentException("WelcomeFiles for [" + welcomeFiles
//					+ "] cannot be found in the provided http context");
//		}
//		welcomeFileModels.remove(key);
//		return model;
//	}
//
//	public synchronized void addSecurityConstraintMappingModel(
//			SecurityConstraintMappingModel model) {
//		if (securityConstraintMappingModels.containsKey(model
//				.getConstraintName())) {
//			throw new IllegalArgumentException("Security Mapping ["
//					+ model.getConstraintName() + "] is already registered.");
//		}
//		securityConstraintMappingModels.put(model.getConstraintName(), model);
//		addContextModel(model.getContextModels());
//	}
//
//	public synchronized SecurityConstraintMappingModel[] getSecurityConstraintMappings() {
//		Collection<SecurityConstraintMappingModel> collection = securityConstraintMappingModels
//				.values();
//		return collection.toArray(new SecurityConstraintMappingModel[collection
//				.size()]);
//	}
//
//	public synchronized void removeSecurityConstraintMappingModel(SecurityConstraintMappingModel model) {
//		securityConstraintMappingModels.remove(model.getConstraintName());
//	}
//
//	public synchronized void addContainerInitializerModel(
//			ContainerInitializerModel model) {
//		if (containerInitializers.containsKey(model.getContainerInitializer())) {
//			throw new IllegalArgumentException("ServletContainerInitializer "
//					+ model.getContainerInitializer() + " already registered");
//		}
//		containerInitializers.put(model.getContainerInitializer(), model);
//	}
//
//	public synchronized void removeContainerInitializerModel(
//			ContainerInitializerModel model) {
//		//NOOP
//	}
//
//
//	public void addWebSocketModel(WebSocketModel model) {
//		if (webSockets.containsKey(model.getWebSocket())) {
//			throw new IllegalArgumentException("WebSocket " + model.getWebSocket() + " already registered");
//		}
//		webSockets.put(model.getWebSocket(), model);
//	}
//
//
//	public void removeWebSocketModel(Object webSocket) {
//		webSockets.remove(webSocket);
//	}
//
//	/**
//	 * Returns true if the context can still be configured. This is possible
//	 * before any web components (servlets / filters / listeners / error pages)
//	 * are registered. TODO verify what happen once the web elements are
//	 * registered and then unregistered. Can still be configured?
//	 *
//	 * @param httpContext created by the service of this model
//	 * @return true, if context can be configured false otherwise
//	 */
//	public synchronized boolean canBeConfigured(HttpContext httpContext) {
//		return canBeConfigured(httpContext, servletModels)
//				&& canBeConfigured(httpContext, filterModels.values())
//				&& canBeConfigured(httpContext, eventListenerModels.values())
//				&& canBeConfigured(httpContext, errorPageModels.values());
//	}
//
//	private boolean canBeConfigured(HttpContext httpContext,
//									Collection<? extends ElementModel> models) {
////		for (Model model : models) {
////			OsgiContextModel contextModel = model.getContextModel();
////			HttpContext candidateHttpContext = contextModel.getHttpContext();
////			if (candidateHttpContext.equals(httpContext)) {
////				return false;
////			}
////		}
//		return true;
//	}

}
