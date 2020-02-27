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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.ReferencedWebContainerContext;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.task.ServletModelChange;
import org.ops4j.pax.web.service.whiteboard.ContextMapping;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Holds web elements in a global context accross all services (all bundles using the Http Service).</p>
 *
 * <p>This model represents entire web server and holds all web components (servlets, filters, mappings, ...) for
 * quick access. This allows to resolve conflicts, when e.g., two different bundles (by default operating on separate
 * <em>contexts</em> (OSGi contexts like {@link HttpContext}, not Servlet contexts)) want to register servlets using
 * the same alias / URL mapping.</p>
 *
 * @author Alin Dreghiciu
 */
public class ServerModel {

	private static final Logger LOG = LoggerFactory.getLogger(ServerModel.class);

	/** This virtual host name is used if there is no Web-VirtualHosts in manifest. */
	private static final String DEFAULT_VIRTUAL_HOST = "default";

	private final Executor executor;

	/** Unique identified of the Thread from (assumed) single thread pool executor. */
	private final long registrationThreadId;

	/**
	 * <p>Map of all available virtual hosts. Each key is virtual host main name (no alias, no wildcard).</p>
	 *
	 * <p>Web application (in other words, full {@link javax.servlet.ServletContext servlet contexts}) or registered
	 * whiteboard services (always connected with some <em>context</em>) are associated with particular virtual host
	 * <strong>always</strong> indirectly - through associated <em>context</em>.</p>
	 *
	 * <p>WAR bundle can do it using bundle manifest header {@code Web-VirtualHosts}, while a <em>context</em> may
	 * refer to virtual host using methods described in {@link ContextMapping#getVirtualHosts()}.</p>
	 */
	private final Map<String, VirtualHostModel> virtualHosts = new HashMap<>();

	/**
	 * Default host used to <strong>register</strong> web elements, if no particular host is specified. It should
	 * rather be named <em>fallback host</em>, because it should handle requests that don't specify a host or that
	 * specify unknown host. See {@code org.apache.catalina.core.StandardEngine#defaultHost()}.
	 */
	private final VirtualHostModel defaultHost = new VirtualHostModel();

	/**
	 * <p>Global mapping between <em>context path</em> and {@link ServletContextModel}.</p>
	 *
	 * <p>Particular {@link ServletContextModel} may be available only through some (or several) virtual hosts, but
	 * there's no way to:<ul>
	 *     <li>have single {@link ServletContextModel} available <em>under</em> different virtual hosts with different
	 *     context paths</li>
	 *     <li>have two different {@link ServletContextModel} with the same path registered under different
	 *     virtual hosts (though technically it's possible with Tomcat)</li>
	 * </ul></p>
	 */
	private final Map<String, ServletContextModel> servletContexts = new HashMap<>();

	/**
	 * <p>Global mapping between {@link WebContainerContext} (which can represent "old" {@link HttpContext} from
	 * Http Service specification or "new" {@link org.osgi.service.http.context.ServletContextHelper} from
	 * Whiteboard Service specification) and internal information about OSGi-specific <em>context</em>.</p>
	 *
	 * <p>Several mapped {@link OsgiContextModel OSGi contexts} can refer to single {@link ServletContextModel} when
	 * they use single <em>context path</em>. This is specified in OSGi CMPN Http Service and Whiteboard Service
	 * chapters:<ul>
	 *     <li>"old" {@link HttpContext} is by default unique to a bundle and Pax Web specific name is not enough
	 *     to identify a <em>context</em> - there may be two contexts with "default" name, so there should be two
	 *     different {@link OsgiContextModel OSGi contexts}, further pointing to single {@link ServletContextModel}.</li>
	 *     <li>Pax Web introduces <em>shared</em> {@link HttpContext} where e.g., "default" <em>context</em>
	 *     can be used by many bundles and supporting {@link HttpContext} loads resources from different bundles.</li>
	 *     <li>"new" {@link org.osgi.service.http.context.ServletContextHelper} is by default shared by multiple
	 *     bundles, and its name <strong>is</strong> the only distinguishing element, however web elements may
	 *     refer (using a filter) to more than one {@link org.osgi.service.http.context.ServletContextHelper}
	 *     (using wildcard filter or by filtering other service registration properties).</li>
	 *     <li>"old" Http Service specification doesn't mention at all the concept of sharing {@link HttpContext}
	 *     through service registry. That's Pax Web improvement and additional configuration method (like specifying
	 *     <em>context path</em>).</li>
	 *     <li>Registered <em>contexts</em> (and Whiteboard Service spec states this explicitly) may be instances
	 *     of {@link org.osgi.framework.ServiceFactory} which add bundle identity aspect to the context.</li>
	 * </ul></p>
	 *
	 * <p>This mapping is not available for given virtual host, because in Pax Web the relation is reversed comparing
	 * to Tomcat and reflects Jetty approach to virtual hosts (where hosts are <em>attributes</em> of a context).</p>
	 *
	 * <p>This map may be populated during registration of servlet using old {@link org.osgi.service.http.HttpService}
	 * API, but can also be altered in Whiteboard service tracking (both Pax Web tracking of custom interfaces
	 * and official OSGi CMPN Whiteboard tracking of web elements with service registration parameters), where
	 * user registers actual <em>context</em> - also when registered <em>context</em> is a
	 * {@link org.osgi.framework.ServiceFactory}.</p>
	 *
	 * <p>There may also be a case, where some servlet is registered using a context (by default using "/" path) and
	 * then the same {@link HttpContext} itself is registered with path specification. This should lead to
	 * re-registration of servlets (and filers and ...) to a new actual server-specific context.</p>
	 *
	 * <p>This is mapping from user layer to Pax Web layer.</p>
	 */
	private final Map<WebContainerContext, OsgiContextModel> contexts = new HashMap<>();

	/**
	 * <p>If an {@link OsgiContextModel} is associated with shared {@link HttpContext} or with
	 * {@link org.osgi.service.http.context.ServletContextHelper} (which is shared by default), such context
	 * can be retrieved here by name, which should be unique.</p>
	 *
	 * <p>When new shared context is registered (in any way) that should override existing one (e.g., by
	 * service ranking), it should be replaced.</p>
	 *
	 * <p>See 140.2 The Servlet Context, description of {@code osgi.http.whiteboard.context.name} property.</p>
	 */
	private final Map<String, OsgiContextModel> sharedContexts = new HashMap<>();

	/**
	 * <p>If an {@link OsgiContextModel} is associated with non-shared {@link HttpContext}, such context
	 * can be retrieved here by <em>reference</em> == ID + bundle.</p>
	 *
	 * <p>This map contains a subset of {@link #contexts} keyed by contexts that should provide
	 * correct equality check with {@link ReferencedWebContainerContext}.</p>
	 */
	private final Map<WebContainerContext, OsgiContextModel> bundleContexts = new HashMap<>();

	/**
	 * <p>Set of all registered servlets. Used to block registration of the same servlet more than once.
	 * Http Service specification explicitly forbids registration of the same instance. Whiteboard Service
	 * specification only mentions that a servlet may be registered in multiple Whiteboard runtimes available in
	 * JVM.</p>
	 *
	 * <p>Though according to Whiteboard Service spec, a servlet may be registered into many <em>contexts</em> and
	 * in Pax Web - also to many <em>virtual hosts</em>, it's happening internally - user still can't register
	 * a servlet multiple times (whether using {@link org.osgi.service.http.HttpService} or Whiteboard registration.</p>
	 *
	 * <p>The map has keys obtained using {@link System#identityHashCode(Object)} to prevent user tricks with
	 * {@link Object#equals(Object)}. The value is not a {@link Servlet} but it's {@link ServletModel model}
	 * to better show error messages.</p>
	 */
	private final Map<Servlet, ServletModel> servlets = new IdentityHashMap<>();

	/**
	 * <p>When new servlet is registered using Whiteboard approach and there's already a servlet registered for
	 * given pattern, it MAY be unregistered after resolving the conflict using service ranking. But the "losing"
	 * servlet should not be forgotten - it should become <em>disabled</em> and will be enabled again if
	 * service registration changes.</p>
	 *
	 * <p>This set is reviewed every time existing registration is changed.</p>
	 */
	private final Set<ServletModel> disabledServletModels = new HashSet<>();

	/**
	 * <p>Set of all registered filters. Used to block registration of the same filter more than once.
	 *
	 * <p>The map has keys obtained using {@link System#identityHashCode(Object)} to prevent user tricks with
	 * {@link Object#equals(Object)}.</p>
	 */
	private final Map<Filter, FilterModel> filters = new IdentityHashMap<>();

	// TODO: Should listeners, security constraints, login configs, welcome files, security roles, error pages and
	//       and mime types be checked for unique registration?

	/**
	 * Each registration (through {@link org.osgi.service.http.HttpService} and Whiteboard), after passing initial
	 * tests, is turned into an {@link ElementModel} and put into this global map. That's how we later revert the
	 * registrations (unregister elements) with full conflict detection - like preventing unregistration of servlet
	 * by alias, when given alias was registered by different bundle-scoped {@link org.osgi.service.http.HttpService}
	 *  - we can precisely warn about existing registration details.
	 */
	@Review("To check if it's needed")
	private final Map<ElementModel, RegistrationInfo> registrations = new HashMap<>();

	public ServerModel(Executor executor) {
		this.executor = executor;

		try {
			// check thread ID to detect whether we're running within it
			registrationThreadId = CompletableFuture.supplyAsync(() -> Thread.currentThread().getId()).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e.getMessage(), e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				// no idea what went wrong
				throw new RuntimeException(e.getCause().getMessage(), e.getCause());
			}
		}
	}

	public Executor getExecutor() {
		return executor;
	}

	/**
	 * <p>Utility method of the global {@link ServerModel} to ensure serial invocation of configuration/registration
	 * tasks. Such tasks can freely manipulate all internal Pax Web Runtime models without a need for
	 * synchronization.</p>
	 *
	 * <p>The task is executed by {@link java.util.concurrent.Executor} associated with this {@link ServerModel}.</p>
	 *
	 * @param task
	 * @param <T>
	 * @return
	 * @throws ServletException
	 * @throws NamespaceException
	 */
	public <T> T run(ModelRegistrationTask<T> task) throws ServletException, NamespaceException {
		if (Thread.currentThread().getId() == registrationThreadId) {
			// we can run immediately
			return task.run();
		}

		try {
			return CompletableFuture.supplyAsync(() -> {
				try {
					return task.run();
				} catch (ServletException e) {
					throw new ModelRegistrationException(e);
				} catch (NamespaceException e) {
					throw new ModelRegistrationException(e);
				}
			}, executor).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof ModelRegistrationException) {
				((ModelRegistrationException)e.getCause()).throwTheCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				// no idea what went wrong
				throw new RuntimeException(e.getCause().getMessage(), e.getCause());
			}
		}

		// ??
		return null;
	}

	// Important - all the methods here have to be run within single configuration thread from global
	// thread pool of pax-web-runtime.
	// org.ops4j.pax.web.service.spi.model.ServerModel.register() makes it easier

	/**
	 * <p>This method ensures that <strong>if</strong> given {@link WebContainerContext} is already mapped to some
	 * {@link OsgiContextModel}, it is permitted to reference it by given bundle.</p>
	 *
	 * <p>There are several scenarios, including one where {@link org.osgi.service.http.HttpService}, scoped to
	 * one bundle is used to register a servlet, while passed {@link HttpContext} is scoped to another bundle.</p>
	 *
	 * <p>With whiteboard approach, user can't trick the runtime with two different bundles (one from the passed
	 * {@link HttpContext} and other - from the bundle scoped {@link org.osgi.service.http.HttpService}.</p>
	 *
	 * @param context existing extension of {@link HttpContext}, probably created from bundle-scoped
	 *        {@link org.osgi.service.http.HttpService}
	 * @param bundle actual bundle on behalf of each we try to perform a registration of web element - comes from
	 *        the scope of {@link org.osgi.service.http.HttpService} through which the registration is made.
	 * @return if the association exists and is valid, related {@link OsgiContextModel} is returned. {@code null}
	 *         is returned if the association is possible
	 * @throws IllegalStateException if there exists incompatible association
	 */
	public OsgiContextModel checkValidAssociation(final WebContainerContext context, final Bundle bundle)
			throws IllegalStateException {
		OsgiContextModel contextModel = null;

		// quick check in case of references - first among shared contexts, then among bundle-scoped contexts
		// here we don't care much about the bundle of HttpService used
		if (context.isReference()) {
			contextModel = sharedContexts.get(context.getContextId());
			if (contextModel == null) {
				contextModel = bundleContexts.get(context);
			}

			if (contextModel == null) {
				throw new IllegalStateException(context + " doesn't refer to any existing context");
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace(context + " can be associated with " + contextModel);
			}
			return contextModel;
		}

		// next check that's most common - registration when there's already a context present
		contextModel = contexts.get(context);

		if (contextModel != null) {
			if (!contextModel.getHttpContext().getBundle().equals(bundle)) {
				if (!contextModel.getHttpContext().isShared()) {
					// context is OK, points to proper model, but looks like "stolen".
					// bundle (scope of HttpService) is not correct, should be the same
					throw new IllegalStateException("Existing " + contextModel
							+ " is not shared and can't be used by bundle " + bundle);
				}
				if (!context.isShared()) {
					// we could access shared contextModel, but we're using non-shared WebContainerContext to access it
					throw new IllegalStateException("Existing " + contextModel
							+ " is shared, but registration is perfomed using non-shared " + context);
				}
			}

			if (LOG.isTraceEnabled()) {
				LOG.trace(context + " can be associated with " + contextModel + " in the scope of " + bundle);
			}

			return contextModel;
		}

		// shared context check - enforced identity by contextId only
		// according to 140.2 The Servlet Context, "osgi.http.whiteboard.context.name" description
		if (context.isShared()) {
			// whether there is shared context or not, it's ok - even if there are non-shared contexts
			// with the same name
			contextModel = sharedContexts.get(context.getContextId());

			if (LOG.isTraceEnabled()) {
				if (contextModel != null) {
					LOG.trace(context + " can be associated with " + contextModel);
				} else {
					LOG.trace(context + " is not yet associated with any context model");
				}
			}

			return contextModel;
		}

		// non shared context, no associated contextModel
		// OK, even if there may be other (shared or non shared, but scoped for different bundle) contexts
		// with the same name, but the thing is that given context+bundle has no associated ContextModel yet
		if (LOG.isTraceEnabled()) {
			LOG.trace(context + " is not yet associated with any context model");
		}

		return null;
	}

	/**
	 * <p>Returns {@link ServletContextModel} uniquely identified by <em>context path</em> (as defined by
	 * {@link ServletContext#getContextPath()}. There's single instance of {@link ServletContextModel} even if
	 * it's available in multiple {@link VirtualHostModel virtual hosts}.</p>
	 *
	 * <p>This method doesn't alter the global model, only adds relevant operation to the {@link Batch}.</p>
	 *
	 * @param contextPath
	 * @param batch
	 */
	public ServletContextModel getOrCreateServletContextModel(String contextPath, Batch batch) {
		ServletContextModel existing = servletContexts.get(contextPath);
		if (existing != null) {
			return existing;
		}

		ServletContextModel servletContextModel = new ServletContextModel(contextPath);
		batch.addServletContextModel(this, servletContextModel);

		LOG.info("Created new {}", servletContextModel);

		return servletContextModel;
	}

	/**
	 * <p>Returns {@link OsgiContextModel} associated with {@link HttpContext} and when the target
	 * {@link OsgiContextModel} is already available, checks if it can be used with input {@link HttpContext}.</p>
	 *
	 * <p>Single, bundle-scoped instance of {@link org.osgi.service.http.HttpService} (this class) may manage different
	 * {@link OsgiContextModel servlet contexts} for different instances of {@link HttpContext}. But when
	 * non-shared {@link HttpContext} is already associated with some {@link OsgiContextModel}, it'll be reused.</p>
	 *
	 * <p>This method doesn't alter the global model, only adds relevant operation to the {@link Batch}.</p>
	 *
	 * @param context
	 * @param serviceBundle
	 * @param batch
	 * @return
	 * @throws IllegalStateException if there exists incompatible association
	 */
	public OsgiContextModel getOrCreateOsgiContextModel(final WebContainerContext context, Bundle serviceBundle, Batch batch)
			throws IllegalStateException {
		OsgiContextModel existing = checkValidAssociation(context, serviceBundle);
		if (existing != null) {
			return existing;
		}

		OsgiContextModel osgiContextModel = createNewContextModel(context, serviceBundle, batch);

		batch.associateOsgiContextModel(context, osgiContextModel);

		return osgiContextModel;
	}

	/**
	 * <p>Method used to create new instance of {@link OsgiContextModel} when the <em>input</em>
	 * {@link WebContainerContext} is not yet associated with any context.</p>
	 *
	 * <p>Dedicated method for this purpose emphasizes the importance and fragility of {@link OsgiContextModel}.
	 * The only other way to create {@link OsgiContextModel} is the Whiteboard approach, when user registers one
	 * of these services:<ul>
	 *     <li>{@link org.osgi.service.http.context.ServletContextHelper} with annotations and/or service
	 *     registration parameters</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping}</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}</li>
	 * </ul></p>
	 *
	 * <p>The above Whiteboard methods allow to specify (annotations, service registration parameters, direct
	 * values in {@link org.ops4j.pax.web.service.whiteboard.ContextMapping}) additional information about
	 * {@link OsgiContextModel}:<ul>
	 *     <li>context path</li>
	 *     <li>context (init) parameters</li>
	 *     <li>virtual hosts</li>
	 * </ul>Here, these attributes are not specified, so the resulting {@link OsgiContextModel} will be associated
	 * with {@link ServletContextModel} having "/" path.</p>
	 *
	 * @param webContext
	 * @param serviceBundle
	 * @param batch
	 * @return
	 */
	private OsgiContextModel createNewContextModel(WebContainerContext webContext, Bundle serviceBundle, Batch batch) {
		OsgiContextModel osgiContextModel = new OsgiContextModel(webContext, serviceBundle);

		ServletContextModel scModel
				= getOrCreateServletContextModel(PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);
		scModel.getOsgiContextModels().add(osgiContextModel);

		osgiContextModel.setServletContextModel(scModel);

		// this context is not registered using Whiteboard, so we have full right to make it parameterless
		osgiContextModel.getContextParams().clear();
		// explicit proof that no particular VHost is associated, thus context will be available through all VHosts
		osgiContextModel.getVirtualHosts().clear();

		// the context still should behave like it was registered
		Hashtable<String, Object> registration = osgiContextModel.getContextRegistrationProperties();
		registration.clear();
		// name however will be taken from WebContainerContext
		registration.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, webContext.getContextId());
		registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, webContext.getContextId());
		registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, PaxWebConstants.DEFAULT_CONTEXT_PATH);

		// artificial service registration properties
		registration.put(Constants.SERVICE_ID, 0);
		registration.put(Constants.SERVICE_RANKING, 0);
		osgiContextModel.setServiceId(0);
		osgiContextModel.setServiceRank(0);

		batch.addOsgiContextModel(osgiContextModel);

		LOG.trace("Created new {}", osgiContextModel);

		return osgiContextModel;
	}

	/**
	 * <p>Simply mark {@lnk WebContainerContext} as the owner/creator/initiator of given {@link OsgiContextModel}.
	 */
	public void associateHttpContext(final WebContainerContext context, final OsgiContextModel osgiContextModel) {
		contexts.put(context, osgiContextModel);

		LOG.debug("Created association {} -> {}", context, osgiContextModel);

		if (context.isShared()) {
			sharedContexts.put(context.getContextId(), osgiContextModel);
		} else {
			ReferencedWebContainerContext reference = new ReferencedWebContainerContext(context.getBundle(), context.getContextId());
			// or:
			//ReferencedWebContainerContext reference = new ReferencedWebContainerContext(osgiContextModel.getHttpContext().getBundle(), context.getContextId());
			bundleContexts.put(reference, osgiContextModel);
		}
	}

	/**
	 * Validates {@link ServletModel} and adds relevant batch operations if validation is successful. Due to
	 * complexity of specification, simple "registration of servlet" may cause unregistration of one or more
	 * existing servlets, when current registration uses existing URL mapping, but has higher ranking.
	 *
	 * @param model servlet model to register with all associated {@link OsgiContextModel} needed
	 * @param batch
	 * @throws NamespaceException if servlet alias is already registered (assuming the servlet was registered for
	 *         an "alias")
	 * @throws ServletException if servlet is already registered
	 * @throws IllegalStateException if anything goes wrong
	 * @throws IllegalArgumentException if validation fails
	 */
	public void addServletModel(final ServletModel model, Batch batch)
			throws NamespaceException, ServletException {
		if (model.getContextModels().isEmpty()) {
			throw new IllegalArgumentException("Can't register " + model + ", it is not associated with any context");
		}

		if (model.getServlet() != null && servlets.containsKey(model.getServlet())) {
			throw new ServletException("Can't register servlet " + model.getServlet() + ", it has"
					+ " been already registered using " + servlets.get(model.getServlet()));
		}

		// Even if a servlet is associated with more OsgiContextModels, eventually it is a set of unique
		// ServletContextModels where the servlet is registered
		Set<ServletContextModel> servletContexts = model.getServletContextModels();

		// name checking, but remember that new model may replace existing model, so check should be performed
		// after possible override
		Map<ServletContextModel, ServletModel> contextWithNameConflict = new HashMap<>();
		for (ServletContextModel sc : servletContexts) {
			if (sc.getNameMapping().containsKey(model.getName())) {
				contextWithNameConflict.put(sc, sc.getNameMapping().get(model.getName()));
			}
		}

		// alias checking: 102.2 "Registering Servlets". Exception in case of conflict
		if (model.getAlias() != null) {
			for (ServletContextModel sc : servletContexts) {
				if (sc.getAliasMapping().containsKey(model.getAlias())) {
					String msg = String.format("%s can't be registered. %s already contains mapping for alias %s: %s",
							model, sc, model.getAlias(), sc.getAliasMapping().get(model.getAlias()));
					throw new NamespaceException(msg);
				}
			}

			// no problems, add to batch
			batch.addServletModel(this, model);
		}

		// URL mapping checking: 140.4 "Registering Servlets". Service ranking/id check in case of conflict
		// if there's a conflict, model can be registered only if it wins (by ranking) in each mapped context
		//
		// model can be registered in each of target contexts. But sometimes given context already
		// contains a model for given pattern. We should still register such model, but as "disabled", which
		// means that if existing mapping (with higher ranking) is unregistered, the "waiting" mapping
		// "jumps in" as next active one
		if (model.getAlias() == null) {
			boolean register = true;
			Set<ServletModel> modelsToDisable = new LinkedHashSet<>();

			for (ServletContextModel sc : servletContexts) {
				for (String pattern : model.getUrlPatterns()) {
					ServletModel existing = sc.getServletUrlPatternMapping().get(pattern);
					if (existing != null) {
						// service.ranking/service.id checking
						if (model.compareTo(existing) < 0) {
							// we won, but still can lose, because it's not the only pattern
							modelsToDisable.add(existing);
						} else {
							LOG.warn("{} can't be registered now in {} under \"{}\" mapping. Conflict with {}.",
									model, sc, pattern, existing);
							register = false;
						}
					}
				}
			}

			if (!register) {
				LOG.warn("Skipped registration of {} because of existing mappings.", model);
				// register the model as "awaiting" without touching existing mappings and without additional
				// check for name conflicts
				batch.addDisabledServletModel(this, model);
			} else {
				// if there were existing URL mappings for this ServletModel, new model "won" with all of them
				// (didn't lose with any of them), so we have to unregister existing models according to
				// Whiteboard specification. We can't delete the models entirely - they should await for possible
				// future re-registration and also be available for DTO purposes
				for (ServletModel existing : modelsToDisable) {
					// disable it even if it can stay active in some context
					batch.disableServletModel(this, existing);

					// disabled servletModel should stop causing name conflicts in given servletContext
					contextWithNameConflict.entrySet().removeIf(e -> {
						boolean nameMatches = e.getValue().getName().equals(existing.getName());
						final boolean[] contextMatches = { false };
						existing.getContextModels().forEach(contextModel -> {
							contextMatches[0] |= e.getKey().equals(contextModel.getServletContextModel());
						});
						return nameMatches && contextMatches[0];
					});
				}

				// only after disabling lower ranked models we can check the name conflicts, because servlet
				// with conflicting name inside a context may have just been disabled
				for (Map.Entry<ServletContextModel, ServletModel> entry : contextWithNameConflict.entrySet()) {
					String msg = String.format("%s can't be registered. %s already contains servlet named %s: %s",
							model, entry.getKey(), model.getName(), entry.getValue());
					throw new ServletException(msg);
				}

				batch.addServletModel(this, model);

				if (!modelsToDisable.isEmpty()) {
					// some model have beed disabled. It means that maybe some currently disabled models
					// may be enabled?

					// it's quite problematic part. we're in the method that only prepares the batch, but don't
					// yet change the model itself. Before the model is affected, we'll send this batch to
					// target runtime, so we already need to perform a refresh here, so controller sees the changes.

					// each disabled servletModel may be a reason to enable other models. Currently disabled
					// ServerModel may be enabled ONLY if it can be enabled in ALL associated contexts
					for (ServletModel currentlyDisabled : disabledServletModels) {
						boolean canBeEnabled = true;
						Set<ServletContextModel> scm = currentlyDisabled.getServletContextModels();

						if (currentlyDisabled.getName().equals(model.getName())) {
							if (scm.stream()
									.anyMatch(sc -> model.getContextModels().stream()
											.anyMatch(oc -> oc.getServletContextModel().equals(sc)))) {
								// can't enable due to name conflict with currently registered
								// in one of common contexts
								continue;
							}
						}
						for (ServletContextModel sc : scm) {
							// we can try enabling a ServletModel if neither of its mappings is used
							// by other enabled models
							for (String pattern : currentlyDisabled.getUrlPatterns()) {
								ServletModel existingMapping = sc.getServletUrlPatternMapping().get(pattern);
								if (existingMapping != null && !modelsToDisable.contains(existingMapping)) {
									canBeEnabled = false;
									break;
								}
							}
							if (!canBeEnabled) {
								break;
							}
						}
						if (canBeEnabled) {
							batch.enableServletModel(this, currentlyDisabled);
						}
					}
				}
			}
		}
	}

	// --- batch operation visit() methods performed without validation, because it was done earlier

	public void visit(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		this.servletContexts.put(model.getContextPath(), model);
	}

	public void visit(ServletModelChange change) {
		ServletModel model = change.getServletModel();

		switch (change.getKind()) {
			case ADD: {
				// add new ServletModel to all target contexts
				Set<ServletContextModel> servletContexts = model.getServletContextModels();
				servletContexts.forEach(sc -> {
					if (change.isDisabled()) {
						disabledServletModels.add(model);
					} else {
						sc.getNameMapping().put(model.getName(), model);
						if (model.getAlias() != null) {
							sc.getAliasMapping().put(model.getAlias(), model);
						}
						Arrays.stream(model.getUrlPatterns()).forEach(p -> sc.getServletUrlPatternMapping().put(p, model));
					}
				});

				if (model.getServlet() != null) {
					servlets.put(model.getServlet(), model);
				}
				break;
			}
			case MODIFY:
				break;
			case DELETE:
				break;
			case ENABLE: {
				// enable a servlet in all associated contexts
				Set<ServletContextModel> servletContexts = model.getServletContextModels();
				servletContexts.forEach(sc -> sc.enableServletModel(model));
				disabledServletModels.remove(model);
				break;
			}
			case DISABLE: {
				disabledServletModels.add(model);
				// disable a servlet in all associated contexts
				Set<ServletContextModel> servletContexts = model.getServletContextModels();
				servletContexts.forEach(sc -> sc.disableServletModel(model));
				break;
			}
			default:
				break;
		}
	}















	private List<String> resolveVirtualHosts(ElementModel elementModel) {
		return null;
//		List<String> virtualHosts = elementModel.getContextModel().getVirtualHosts();
//		if (virtualHosts == null || virtualHosts.isEmpty()) {
//			virtualHosts = new ArrayList<>();
//			virtualHosts.add(DEFAULT_VIRTUAL_HOST);
//		}
//		return virtualHosts;
	}

	private String resolveVirtualHost(String hostName) {
//		if (bundlesByVirtualHost.containsKey(hostName)) {
//			return hostName;
//		} else {
			return DEFAULT_VIRTUAL_HOST;
//		}
	}

	private List<String> resolveVirtualHosts(Bundle bundle) {
		List<String> virtualHosts = new ArrayList<>();
//		for (Map.Entry<String, List<Bundle>> entry : bundlesByVirtualHost.entrySet()) {
//			if (entry.getValue().contains(bundle)) {
//				virtualHosts.add(entry.getKey());
//			}
//		}
		if (virtualHosts.isEmpty()) {
			virtualHosts.add(DEFAULT_VIRTUAL_HOST);
		}
		return virtualHosts;
	}

	private void associateBundle(List<String> virtualHosts, Bundle bundle) {
		for (String virtualHost : virtualHosts) {
//			List<Bundle> bundles = bundlesByVirtualHost.get(virtualHost);
//			if (bundles == null) {
//				bundles = new ArrayList<>();
//				bundlesByVirtualHost.put(virtualHost, bundles);
//			}
//			bundles.add(bundle);
		}
	}

	private void deassociateBundle(List<String> virtualHosts, Bundle bundle) {
		for (String virtualHost : virtualHosts) {
//			List<Bundle> bundles = bundlesByVirtualHost.get(virtualHost);
//			bundles.remove(bundle);
//			if (bundles.isEmpty()) {
//				bundlesByVirtualHost.remove(virtualHost);
//			}
		}
	}

	/**
	 * Unregisters a servlet model.
	 *
	 * @param model servlet model to unregister
	 */
	public void removeServletModel(final ServletModel model) {
//		try {
//			deassociateBundle(model.getContextModel().getVirtualHosts(), model.getContextModel().getBundle());
//			for (String virtualHost : resolveVirtualHosts(model)) {
//				if (model.getAlias() != null) {
////					aliasMapping.get(virtualHost).remove(getFullPath(model.getContextModel(), model.getAlias()));
//				}
//				if (model.getServlet() != null) {
//					servlets.get(virtualHost).remove(model.getServlet());
//				}
//				if (model.getUrlPatterns() != null) {
//					for (String urlPattern : model.getUrlPatterns()) {
////						servletUrlPatterns.get(virtualHost).remove(getFullPath(model.getContextModel(), urlPattern));
//					}
//				}
//			}
//		} finally {
//		}
	}

	/**
	 * Registers a filter model.
	 *
	 * @param model filter model to register
	 */
	public void addFilterModel(final FilterModel model) {
//		if (model.getUrlPatterns() != null) {
//			try {
//				associateBundle(model.getContextModel().getVirtualHosts(), model.getContextModel().getBundle());
//				for (String virtualHost : resolveVirtualHosts(model)) {
//					for (String urlPattern : model.getUrlPatterns()) {
//						final UrlPattern newUrlPattern = new UrlPattern(getFullPath(model.getContextModel(), urlPattern),
//								model);
//						String fullPath = getFullPath(model.getContextModel(), urlPattern);
//						if (filterUrlPatterns.get(virtualHost) == null) {
//							filterUrlPatterns.put(virtualHost, new ConcurrentHashMap<>());
//						}
//						Set<UrlPattern> urlSet = filterUrlPatterns.get(virtualHost).get(fullPath);
//						if (urlSet == null) {
//							//initialize first
//							urlSet = new HashSet<>();
//						}
//						urlSet.add(newUrlPattern);
//						filterUrlPatterns.get(virtualHost).put(fullPath, urlSet);
//						//					final UrlPattern existingPattern = filterUrlPatterns.putIfAbsent(
//						//							getFullPath(model.getContextModel(), urlPattern), newUrlPattern);
//						//					if (existingPattern != null) {
//						//						// this should never happen but is a good assertion
//						//						LOG.error("Internal error (please report): Cannot associate url mapping "
//						//								+ getFullPath(model.getContextModel(), urlPattern) + " to " + newUrlPattern
//						//								+ " because is already associated to " + existingPattern);
//						//					}
//					}
//				}
//			} finally {
//			}
//		}
	}

	/**
	 * Unregister a filter model.
	 *
	 * @param model filter model to unregister
	 */
	public void removeFilterModel(final FilterModel model) {
//		if (model.getUrlPatterns() != null) {
//			try {
//				deassociateBundle(model.getContextModel().getVirtualHosts(), model.getContextModel().getBundle());
//				for (String virtualHost : resolveVirtualHosts(model)) {
//					for (String urlPattern : model.getUrlPatterns()) {
//						String fullPath = getFullPath(model.getContextModel(), urlPattern);
//						Set<UrlPattern> urlSet = filterUrlPatterns.get(virtualHost).get(fullPath);
//						UrlPattern toDelete = null;
//						for (UrlPattern pattern : urlSet) {
//							FilterModel filterModel = (FilterModel) pattern.getElementModel();
//							Class<?> filter = filterModel.getFilterClass();
//							Class<?> matchFilter = model.getFilterClass();
//							if (filter != null && filter.equals(matchFilter)) {
//								toDelete = pattern;
//								break;
//							}
//							Object filterInstance = filterModel.getFilter();
//							if (filterInstance != null && filterInstance == model.getFilter()) {
//								toDelete = pattern;
//								break;
//							}
//						}
//						urlSet.remove(toDelete);
//					}
//				}
//			} finally {
//			}
//		}
	}


	/**
	 * Deassociate all http context assiciated to the provided bundle. The
	 * bellow code is only correct in the context that there is no other thread
	 * is calling the association method in the mean time. This should not
	 * happen as once a bundle is releasing the HttpService the service is first
	 * entering a stopped state ( before the call to this method is made), state
	 * that will not perform the registration calls anymore.
	 *
	 * @param bundle bundle to be deassociated from http contexts
	 */
	public void deassociateHttpContexts(final Bundle bundle) {
//		List<String> virtualHosts = resolveVirtualHosts(bundle);
//		virtualHosts.stream()
//				.map(virtualHost -> httpContexts.get(virtualHost))
//				.map(entry -> entry.entrySet())
//				.flatMap(setEntry -> setEntry.stream())
//				.filter(entry -> entry.getValue() == bundle)
//				.forEach(entry -> httpContexts.remove(entry.getKey()));
	}

	public OsgiContextModel matchPathToContext(final String path) {
		return matchPathToContext("", path);
	}

	public OsgiContextModel matchPathToContext(final String hostName, final String path) {
		final boolean debug = LOG.isDebugEnabled();
		if (debug) {
			LOG.debug("Matching [" + path + "]...");
		}
		String virtualHost = resolveVirtualHost(hostName);
		UrlPattern urlPattern = null;
		// first match servlets
		try {
//			Optional<Map<String, UrlPattern>> optionalServletUrlPatterns = Optional.ofNullable(servletUrlPatterns.get(virtualHost));
//			if (optionalServletUrlPatterns.isPresent()) {
//				urlPattern = matchPathToContext(optionalServletUrlPatterns.get(), path);
//			}
		} finally {
		}
		// then if there is no matched servlet look for filters
//		if (urlPattern == null) {
//			Optional<ConcurrentMap<String, Set<UrlPattern>>> optionalFilterUrlPattern = Optional.ofNullable(filterUrlPatterns.get(virtualHost));
////			if (optionalFilterUrlPattern.isPresent()) {
////				urlPattern = matchFilterPathToContext(filterUrlPatterns.get(virtualHost), path);
////			}
//		}
		OsgiContextModel matched = null;
//		if (urlPattern != null) {
//			matched = urlPattern.getElementModel().getContextModel();
//		}
		if (debug) {
			if (matched != null) {
				LOG.debug("Path [" + path + "] matched to " + urlPattern);
			} else {
				LOG.debug("Path [" + path + "] does not match any context");
			}
		}
		return matched;
	}

	private static UrlPattern matchFilterPathToContext(final Map<String, Set<UrlPattern>> urlPatternsMap, final String path) {
		Set<String> keySet = urlPatternsMap.keySet();
		for (String key : keySet) {
			Set<UrlPattern> patternsMap = urlPatternsMap.get(key);

			for (UrlPattern urlPattern : patternsMap) {
				Map<String, UrlPattern> tempMap = new HashMap<>();
				tempMap.put(key, urlPattern);
				UrlPattern pattern = matchPathToContext(tempMap, path);
				if (pattern != null) {
					return pattern;
				}
			}
		}
		return null;
	}

	private static UrlPattern matchPathToContext(final Map<String, UrlPattern> urlPatternsMap, final String path) {
		UrlPattern matched = null;
		String servletPath = path;

		while ((matched == null) && (!"".equals(servletPath))) {
			// Match the asterisks first that comes just after the current
			// servlet path, so that it satisfies the longest path req
			if (servletPath.endsWith("/")) {
				matched = urlPatternsMap.get(servletPath + "*");
			} else {
				matched = urlPatternsMap.get(servletPath + "/*");
			}

			// try to match the exact resource if the above fails
			if (matched == null) {
				matched = urlPatternsMap.get(servletPath);
			}

			// now try to match the url backwards one directory at a time
			if (matched == null) {
				String lastPathSegment = servletPath.substring(servletPath.lastIndexOf("/") + 1);
				servletPath = servletPath.substring(0, servletPath.lastIndexOf("/"));
				// case 1: the servlet path is /
				if (("".equals(servletPath)) && ("".equals(lastPathSegment))) {
					break;
				} else if ("".equals(lastPathSegment)) {
					// case 2 the servlet path ends with /
					matched = urlPatternsMap.get(servletPath + "/*");
					continue;
				} else if (lastPathSegment.contains(".")) {
					// case 3 the last path segment has a extension that needs
					// to be
					// matched
					String extension = lastPathSegment.substring(lastPathSegment.lastIndexOf("."));
					if (extension.length() > 1) {
						// case 3.5 refer to second bulleted point of heading
						// Specification of Mappings
						// in servlet specification
						// PATCH - do not go global too early. Next 3 lines
						// modified.
						// matched = urlPatternsMap.get("*" + extension);
						if (matched == null) {
							matched = urlPatternsMap.get(("".equals(servletPath) ? "*" : servletPath + "/*")
									+ extension);
						}
					}
				} else {
					// case 4 search for the wild cards at the end of servlet
					// path
					// of the next iteration
					if (servletPath.endsWith("/")) {
						matched = urlPatternsMap.get(servletPath + "*");
					} else {
						matched = urlPatternsMap.get(servletPath + "/*");
					}
				}

				// case 5 if all the above fails look for the actual mapping
				if (matched == null) {
					matched = urlPatternsMap.get(servletPath);
				}

				// case 6 the servlet path has / followed by context name, this
				// case is
				// selected at the end of the directory, when none of the them
				// matches.
				// So we try to match to root.
				if ((matched == null) && ("".equals(servletPath)) && (!"".equals(lastPathSegment))) {
					matched = urlPatternsMap.get("/");
				}
			}
		}
		return matched;
	}

	/**
	 * Returns the full path (including the context name if set)
	 *
	 * @param model a context model
	 * @param path  path to be prepended
	 * @return full path
	 */
	private static String getFullPath(final OsgiContextModel model, final String path) {
		String fullPath = path.trim();
//		if (model.getContextName().length() > 0) {
//			fullPath = "/" + model.getContextName();
//			if (!"/".equals(path.trim())) {
//				if ((!(fullPath.endsWith("/"))) && (!(path.startsWith("/")))) {
//					fullPath += "/";
//				}
//				fullPath = fullPath + path;
//			}
//		}
		return fullPath;
	}

	/**
	 * Touple of full url pattern and registered model (servlet/filter) for the
	 * model.
	 */
	private static class UrlPattern {

		private final Pattern pattern;
		private final ElementModel elementModel;

		UrlPattern(final String pattern, final ElementModel elementModel) {
			this.elementModel = elementModel;
			String patternToUse = pattern;
			if (!patternToUse.contains("*")) {
				patternToUse = patternToUse + (pattern.endsWith("/") ? "*" : "/*");
			}
			patternToUse = patternToUse.replace(".", "\\.");
			patternToUse = patternToUse.replace("*", ".*");
			this.pattern = Pattern.compile(patternToUse);
		}

		ElementModel getElementModel() {
			return elementModel;
		}

		@Override
		public String toString() {
			return new StringBuilder().append("{").append("pattern=").append(pattern.pattern()).append(",model=")
					.append(elementModel).append("}").toString();
		}
	}

}
