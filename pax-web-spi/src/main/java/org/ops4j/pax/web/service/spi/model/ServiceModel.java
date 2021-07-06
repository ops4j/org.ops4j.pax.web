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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public static final Logger LOG = LoggerFactory.getLogger(ServiceModel.class);

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
	}

	public WebContainerContext getOrCreateDefaultHttpContext(String contextId) {
		return serverModel.runSilently(() -> {
			OsgiContextModel ctx = serverModel.getBundleContextModel(contextId, serviceBundle);
			if (ctx == null) {
				// create one in batch through ServiceModel and ensure its stored at ServerModel as well
				ctx = createDefaultHttpContext(contextId);
			}
			return ctx.resolveHttpContext(serviceBundle);
		});
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
			Batch batch = new Batch("Initialization of HttpContext \"" + contextId + "\" for " + serviceBundle);

			WebContainerContext wcc = new DefaultHttpContext(serviceBundle, contextId);

			// this will create and store new OsgiContextModel inside ServerModel
			OsgiContextModel model = serverModel.getOrCreateOsgiContextModel(wcc, serviceBundle,
					PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);
			batch.accept(this);
			serverController.sendBatch(batch);

			return model;
		});
	}

	public MultiBundleWebContainerContext getOrCreateDefaultSharedHttpContext(String contextId) {
		return serverModel.runSilently(() -> {
			OsgiContextModel ctx = serverModel.getSharedContextModel(contextId);
			if (ctx == null) {
				// create one in batch through as shared contexts are not associated with any "owner" bundle
				ctx = serverModel.createDefaultSharedtHttpContext(contextId, serverController);
			}
			return (MultiBundleWebContainerContext) ctx.resolveHttpContext(serviceBundle);
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
			case DISASSOCIATE:
				serverModel.disassociateHttpContext(change.getContext(), change.getOsgiContextModel());
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
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

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

			if (model.getErrorPageModel() != null) {
				errorPageModels.add(model.getErrorPageModel());
			}

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

				if (model.getErrorPageModel() != null) {
					errorPageModels.remove(model.getErrorPageModel());
				}
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
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

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
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

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
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

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
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

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
			ErrorPageModel model = change.getErrorPageModel();
			if (change.getNewModels().size() > 0) {
				// it means we need to set them in the model now
				model.changeContextModels(change.getNewModels());
			}

			errorPageModels.add(model);
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

	/**
	 * <p>Fragile method that should check all the web elements registered into this {@link ServiceModel} but only
	 * if these elements were NOT registered through Whiteboard and were registered using {@code oldContext}.
	 * If conditions match, each such element should be <em>unregistered</em> and then <em>registered</em> again
	 * in order to <em>switch</em> its HttpService-related context.</p>
	 *
	 * <p>This method is an equivalent of
	 * {@code org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardContext#reRegisterWebElements()}.</p>
	 *
	 * @param oldContext
	 * @param newContext may be null - in such case we have to determine new context
	 * @param batch
	 */
	public void reRegisterWebElementsIfNeeded(OsgiContextModel oldContext, OsgiContextModel newContext, Batch batch) {
		OsgiContextModel target = newContext;
		if (target == null) {
			// it should be either highest ranked model or default model if we're re-registering because last
			// Whiteboard-registered context was removed
			if (oldContext.isShared()) {
				target = serverModel.getSharedContextModel(oldContext.getName(), oldContext);
				if (target == null) {
					target = serverModel.getSharedDefaultContextModel(oldContext.getName());
				}
			} else {
				target = serverModel.getBundleContextModel(oldContext.resolveHttpContext(null), oldContext);
				if (target == null) {
					target = serverModel.getBundleDefaultContextModel(ContextKey.of(oldContext));
				}
			}
		}

		if (target == null) {
			// strange case, but it's possible. wc.registerXXX() was called AFTER a Whiteboard HttpContext
			// was registered. so when it's gone, there should be some context - and we should create
			// the default "default" (or named) context (HttpService based) for "/" path
			if (oldContext.isShared()) {
				// this should create the context and association
				MultiBundleWebContainerContext context = this.getOrCreateDefaultSharedHttpContext(oldContext.getName());
				target = serverModel.getSharedContextModel(context.getContextId());
			} else {
				// this should create the context and association
				WebContainerContext context = this.getOrCreateDefaultHttpContext(oldContext.getName());
				target = serverModel.getBundleContextModel(context);
			}
		}

		boolean force = newContext == null;

		// all web elements that are associated ONLY with the oldContext should be re-registered
		// also we should NOT touch the elements that come from Whiteboard (have a filter configured)

		// there's fragile operation here - we're operating in a batch, so we're not changing the content
		// of Server/ServiceModel to remove the models correctly from existing contexts,
		// but batch operations should accept a (1-element size) list of new models to set later

		LOG.info("Re-registering web elements from {} to {}", oldContext, target);

		// TODO: I can still imagine a scenario, where a web element should not simply be re-registered into
		//       new context, because this new context may already contain higher-ranked web elements.
		//       But for now, let's NOT treat it as generic scenario, but only as a way to alter HttpService
		//       related contexts using Whiteboard services

		for (ContainerInitializerModel cim : containerInitializerModels) {
			if (needsReRegistration(cim, oldContext, target, force)) {
				batch.removeContainerInitializerModels(Collections.singletonList(cim));
				batch.addContainerInitializerModel(cim, target);
			}
		}

		for (EventListenerModel elm : eventListenerModels) {
			if (needsReRegistration(elm, oldContext, target, force)) {
				batch.removeEventListenerModels(Collections.singletonList(elm));
				batch.addEventListenerModel(elm, target);
			}
		}

		for (ServletModel sm : servletModels) {
			if (needsReRegistration(sm, oldContext, target, force)) {
				Map<ServletModel, Boolean> modelsAndStates = new LinkedHashMap<>();
				modelsAndStates.put(sm, !serverModel.getDisabledServletModels().contains(sm));
				batch.removeServletModels(modelsAndStates);
				batch.addServletModel(sm, target);
			}
		}

		for (WelcomeFileModel wfm : welcomeFileModels) {
			if (needsReRegistration(wfm, oldContext, target, force)) {
				batch.removeWelcomeFileModel(wfm);
				batch.addWelcomeFileModel(wfm, target);
			}
		}

		Set<ErrorPageModel> affectedErrorPageModels = new TreeSet<>();
		for (ErrorPageModel epm : errorPageModels) {
			if (needsReRegistration(epm, oldContext, target, force)) {
				batch.removeErrorPageModels(Collections.singletonList(epm));
				batch.addErrorPageModel(epm, target);
				affectedErrorPageModels.add(epm);
			}
		}
		if (affectedErrorPageModels.size() > 0) {
			// server controllers accept only ErrorPageStateChange operations, not ErrorPageModelChange(s)
			Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> state = new HashMap<>();
			String path1 = oldContext.getContextPath();
			String path2 = target.getContextPath();

			Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> currentlyEnabledByPath = new HashMap<>();
			serverModel.prepareErrorPageSnapshot(currentlyEnabledByPath, new TreeSet<>(), null, new HashSet<>());

			if (!path1.equals(path2)) {
				// change in old context (and catch the error page models that should be moved to different context)
				TreeMap<ErrorPageModel, List<OsgiContextModel>> p1 = new TreeMap<>();
				TreeMap<ErrorPageModel, List<OsgiContextModel>> p2 = new TreeMap<>();
				Map<ErrorPageModel, List<OsgiContextModel>> epModels1 = currentlyEnabledByPath.get(path1);
				if (epModels1 != null && epModels1.size() > 0) {
					// context with path1 has some models - if there are affected models, they have to be removed
					for (ErrorPageModel fm : epModels1.keySet()) {
						if (!affectedErrorPageModels.contains(fm)) {
							p1.put(fm, null);
						} else {
							p2.put(fm, Collections.singletonList(target));
						}
					}
					if (!p2.isEmpty()) {
						// a change!
						state.put(path1, p1);
					}
				}

				// change in new context
				Map<ErrorPageModel, List<OsgiContextModel>> epModels2 = currentlyEnabledByPath.get(path2);
				if (epModels2 != null) {
					p2.putAll(epModels2);
				}
				state.put(path2, p2);
			} else {
				// no need to do anything, because error page model is only a declaration/configuration
			}
			batch.updateErrorPages(state);
		}

		Set<FilterModel> affectedFilterModels = new TreeSet<>();
		for (FilterModel fm : filterModels) {
			if (needsReRegistration(fm, oldContext, target, force)) {
				batch.removeFilterModels(Collections.singletonList(fm));
				batch.addFilterModel(fm, target);
				affectedFilterModels.add(fm);
			}
		}
		if (affectedFilterModels.size() > 0) {
			// server controllers accept only FilterStateChange operations, not FilterModelChange(s)
			// NOTE: filters registered using Whiteboard are re-registered one by one through Whiteboard view and
			// the re-registration goes through all the recalculations of model state - even if a filter is associated
			// with more contexts.
			// Here, we operate on more filters at once, but none of them has more than one OsgiContextModel
			// associated
			Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> state = new HashMap<>();
			String path1 = oldContext.getContextPath();
			String path2 = target.getContextPath();

			Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> currentlyEnabledByPath = new HashMap<>();
			serverModel.prepareFiltersSnapshot(currentlyEnabledByPath, new TreeSet<>(), null, new HashSet<>());

			if (!path1.equals(path2)) {
				// ensure that servlet context for the path of oldContext doesn't contain the affected filters
				// but remember, oldContext may point to a servlet context that is pointed to by another osgi context
				// which is not affected, so not ALL filters are moved from old context path...
				// that's why we have to configure two contexts

				// change in old context (and catch the filter models that should be moved to different context)
				TreeMap<FilterModel, List<OsgiContextModel>> p1 = new TreeMap<>();
				TreeMap<FilterModel, List<OsgiContextModel>> p2 = new TreeMap<>();
				Map<FilterModel, List<OsgiContextModel>> filterModels1 = currentlyEnabledByPath.get(path1);
				if (filterModels1 != null && filterModels1.size() > 0) {
					// context with path1 has some filters - if there are affected filters, they have to be removed
					for (FilterModel fm : filterModels1.keySet()) {
						if (!affectedFilterModels.contains(fm)) {
							p1.put(fm, null);
						} else {
							// here's where FilterModel is marked to be added with new OsgiContextModel though
							// FilterModel itself still contains old OsgiContextModel
							p2.put(fm, Collections.singletonList(target));
						}
					}
					if (!p2.isEmpty()) {
						// a change!
						state.put(path1, p1);
					}
				}

				// change in new context
				Map<FilterModel, List<OsgiContextModel>> filterModels2 = currentlyEnabledByPath.get(path2);
				if (filterModels2 != null) {
					p2.putAll(filterModels2);
				}
				state.put(path2, p2);
			} else {
				// just reconfigure one context, because filters may switch OsgiContextModels pointing to single
				// servlet context (context path)

				TreeMap<FilterModel, List<OsgiContextModel>> newState = new TreeMap<>();
				Map<FilterModel, List<OsgiContextModel>> filterModels1 = currentlyEnabledByPath.get(path1);
				if (filterModels1 != null && filterModels1.size() > 0) {
					// context with path1 has some filters - if there are affected filters, they have to be removed
					for (FilterModel fm : filterModels1.keySet()) {
						if (!affectedFilterModels.contains(fm)) {
							newState.put(fm, null);
						} else {
							newState.put(fm, Collections.singletonList(target));
						}
					}
				}
				state.put(path1, newState);
			}
			batch.updateFilters(state, false);
		}
	}

	/**
	 * Checks whether existing {@link ElementModel} is associated with given context only - to determine
	 * whether such element should be re-registered.
	 *
	 * @param model
	 * @param oldContext
	 * @param newContext
	 * @param force whether to force re-registration in case existing context was removed
	 * @return
	 */
	private boolean needsReRegistration(ElementModel<?, ?> model, OsgiContextModel oldContext,
			OsgiContextModel newContext, boolean force) {
		if (model.getContextModels().size() != 1) {
			return false;
		}
		OsgiContextModel currentContext = model.getContextModels().get(0);
		return currentContext == oldContext
				&& (force || !oldContext.isWhiteboard() || newContext.compareTo(oldContext) < 0);
	}

}
