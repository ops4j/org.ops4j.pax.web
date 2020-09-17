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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.ContainerInitializerModelChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageModelChange;
import org.ops4j.pax.web.service.spi.task.ErrorPageStateChange;
import org.ops4j.pax.web.service.spi.task.EventListenerModelChange;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.OpCode;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.ops4j.pax.web.service.spi.task.WelcomeFileModelChange;
import org.osgi.framework.Bundle;

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
	private final ServerController serverController;

	private final Bundle serviceBundle;

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

	/** All event listener models registered by given bundle-scoped {@link org.osgi.service.http.HttpService}. */
	private final Set<EventListenerModel> eventListenerModels = new HashSet<>();

	/** All container initializer models registered by given bundle-scoped {@link org.osgi.service.http.HttpService}. */
	private final Set<ContainerInitializerModel> containerInitializerModels = new HashSet<>();

	/** Welcome files are just kept as a sets - separately for each {@link ContextKey}. */
	private final Map<ContextKey, Set<String>> welcomeFiles = new LinkedHashMap<>();

	/** But also we keep welcome file models directly, to be able to remove them when needed */
	private final Set<WelcomeFileModel> welcomeFileModels = new HashSet<>();

	/** Error page models are kept as collection and processed for conflicts at {@link ServerModel} level */
	private final Set<ErrorPageModel> errorPageModels = new HashSet<>();

//	private final Map<String, SecurityConstraintMappingModel> securityConstraintMappingModels;
//	private final Map<Object, WebSocketModel> webSockets;

	public ServiceModel(ServerModel serverModel, ServerController serverController, Bundle serviceBundle) {
		this.serverModel = serverModel;
		this.serviceBundle = serviceBundle;
		this.serverController = serverController;

		createDefaultHttpContext(PaxWebConstants.DEFAULT_CONTEXT_NAME);
	}

	/**
	 * Creates named {@link OsgiContextModel} for the bundle from this {@link ServiceModel} and ensures that
	 * this {@link OsgiContextModel} is stored at {@link ServerModel} level.
	 * @param contextId
	 * @return
	 */
	public OsgiContextModel createDefaultHttpContext(String contextId) {
		return serverModel.runSilently(() -> {
			// Just as ServerModel creates bundle-agnostic ServletContextModel, in ServiceModel we create/acquire
			// bundle-aware OsgiContextModel - this time in a batch to comply to single-writer principle
			Batch batch = new Batch("Initialization of HttpService for " + serviceBundle);
			ServletContextModel scm = serverModel.getOrCreateServletContextModel(PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);

			WebContainerContext wcc = new DefaultHttpContext(serviceBundle, contextId);

			// this will create and store new OsgiContextModel inside ServerModel
			OsgiContextModel model = serverModel.getOrCreateOsgiContextModel(wcc, serviceBundle,
					PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);
			batch.accept(this);
			serverController.sendBatch(batch);

			return model;
		});
	}

	public Map<String, Map<String, ServletModel>> getAliasMapping() {
		return aliasMapping;
	}

	public Set<ServletModel> getServletModels() {
		return servletModels;
	}

	public Set<FilterModel> getFilterModels() {
		return filterModels;
	}

	public Set<EventListenerModel> getEventListenerModels() {
		return eventListenerModels;
	}

	public Set<WelcomeFileModel> getWelcomeFileModels() {
		return welcomeFileModels;
	}

	public Set<ErrorPageModel> getErrorPageModels() {
		return errorPageModels;
	}

	public Set<ContainerInitializerModel> getContainerInitializerModels() {
		return containerInitializerModels;
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
			case DELETE:
				serverModel.visit(change);
				break;
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
					String contextPath = context.getContextPath();
					contexts.put(contextPath, model);
				}
			}
			servletModels.add(model);

			// and the change should be processed at serverModel level
			serverModel.visit(change);
			return;
		}

		if (change.getKind() == OpCode.DELETE) {
			Collection<ServletModel> modelsToRemove = change.getServletModels().keySet();

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

	@Override
	public void visit(EventListenerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			EventListenerModel model = change.getEventListenerModel();
			eventListenerModels.add(model);
		} else if (change.getKind() == OpCode.DELETE) {
			change.getEventListenerModels().forEach(eventListenerModels::remove);
		}

		// the change should be processed at serverModel level as well
		serverModel.visit(change);
	}

	@Override
	public void visit(ContainerInitializerModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			ContainerInitializerModel model = change.getContainerInitializerModel();
			containerInitializerModels.add(model);
		} else if (change.getKind() == OpCode.DELETE) {
			change.getContainerInitializerModels().forEach(containerInitializerModels::remove);
		}

		// the change should be processed at serverModel level as well
		serverModel.visit(change);
	}

	@Override
	public void visit(WelcomeFileModelChange change) {
		WelcomeFileModel model = change.getWelcomeFileModel();
		if (change.getKind() == OpCode.ADD) {
			welcomeFileModels.add(model);
		} else if (change.getKind() == OpCode.DELETE) {
			// this operation MAY not remove anything, because WelcomeFileModel is constructed during
			// unregistration based on passed (the ones being unregistered) welcome files and identity is no
			// preserved. However this operation will work in Whiteboard mode, when correct WelcomeFileModel
			// object is passed to unregistration method
			welcomeFileModels.remove(model);
		}

		for (OsgiContextModel context : model.getContextModels()) {
			// for each context, welcome files from the model are added/removed from a set
			// of welcome files for this context
			ContextKey key = ContextKey.of(context);
			Set<String> welcomes = welcomeFiles.computeIfAbsent(key, k -> new LinkedHashSet<>());
			if (change.getKind() == OpCode.ADD) {
				welcomes.addAll(Arrays.asList(model.getWelcomeFiles()));
			} else if (change.getKind() == OpCode.DELETE) {
				welcomes.removeAll(Arrays.asList(model.getWelcomeFiles()));
				if (welcomes.isEmpty()) {
					welcomeFiles.remove(key);
				}
			}
		}

		serverModel.visit(change);
	}

	@Override
	public void visit(ErrorPageModelChange change) {
		if (change.getKind() == OpCode.ADD) {
			errorPageModels.add(change.getErrorPageModel());
		}

		if (change.getKind() == OpCode.DELETE) {
			for (ErrorPageModel model : change.getErrorPageModels()) {
				this.errorPageModels.remove(model);
			}
		}

		serverModel.visit(change);
	}

	@Override
	public void visit(ErrorPageStateChange change) {
		// no op here. At model level (unlike in server controller level), filters are added/removed individually
	}

}
