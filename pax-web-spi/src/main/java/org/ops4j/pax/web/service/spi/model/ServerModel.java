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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.ServerController;
import org.ops4j.pax.web.service.spi.context.DefaultHttpContext;
import org.ops4j.pax.web.service.spi.context.DefaultMultiBundleWebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.task.BatchVisitor;
import org.ops4j.pax.web.service.spi.task.FilterModelChange;
import org.ops4j.pax.web.service.spi.task.FilterStateChange;
import org.ops4j.pax.web.service.spi.task.OsgiContextModelChange;
import org.ops4j.pax.web.service.spi.task.ServletContextModelChange;
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
 * @author Grzegorz Grzybek
 */
public class ServerModel implements BatchVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(ServerModel.class);

	/** This virtual host name is used if there is no Web-VirtualHosts in manifest. */
	private static final String DEFAULT_VIRTUAL_HOST = "default";

	private final Executor executor;

	/** Unique identified of the Thread from (assumed) single thread pool executor. */
	private final long registrationThreadId;

	// --- Virtual Host model information

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

	// --- Global context information - not related to any particular bundle

	/**
	 * <p>Global mapping between <em>context path</em> and {@link ServletContextModel}.</p>
	 *
	 * <p>Particular {@link ServletContextModel} may be available only through some (or several) virtual hosts, but
	 * there's no way to:<ul>
	 *     <li>have single {@link ServletContextModel} available <em>under</em> different virtual hosts with different
	 *     context paths</li>
	 *     <li>have two different {@link ServletContextModel} with the same path registered under different
	 *     virtual hosts (though technically it's possible with Tomcat, where <em>virtual host</em> is more
	 *     strictly defined than in Jetty and Undertow).</li>
	 * </ul></p>
	 *
	 * <p>Also, {@link ServletContextModel} is available through many {@link OsgiContextModel} instances,
	 * but the coupling is loose - only through context path.</p>
	 */
	private final Map<String, ServletContextModel> servletContexts = new HashMap<>();

	// --- Bundle-bound context information - for Http Service scenarios, where OsgiContextModel is
	//     identified by HttpContext/WebContainerContext passed to httpService.registerXXX(..., context)
	//     OSGiContextModels created and managed at pax-web-extender-runtime level are not kept here, because
	//     "default" context in Http Service and "default" context in Whiteboard Service should not clash

	/**
	 * <p>Global mapping between {@link WebContainerContext}, which represents "old" {@link HttpContext} from
	 * Http Service specification and internal information about OSGi-specific <em>context</em>. In Whiteboard Service
	 * scenario, the relation is reversed - from user point of view, name of <em>context</em> is used, this name
	 * represents some {@link OsgiContextModel} and from this {@link OsgiContextModel} Pax Web obtains an instance
	 * of {@link WebContainerContext} - because {@link org.osgi.service.http.context.ServletContextHelper}
	 * may be registered as {@link org.osgi.framework.ServiceFactory}.</p>
	 *
	 * <p>Technically, in Http Service scenario, {@link HttpContext} is the entry point and the mapped
	 * {@link OsgiContextModel} contains it (or rather its extension - {@link WebContainerContext}) directly.</p>
	 *
	 * <p>Several mapped {@link OsgiContextModel OSGi contexts} can refer to single {@link ServletContextModel} when
	 * they use single <em>context path</em>. This is specified in OSGi CMPN Http Service and Whiteboard Service
	 * chapters:<ul>
	 *     <li>"old" {@link HttpContext} is by default unique to a bundle and Pax Web specific name is not enough
	 *     to identify a <em>context</em> - there may be two contexts with "default" name, so there should be two
	 *     different {@link OsgiContextModel OSGi contexts}, further pointing to single {@link ServletContextModel}.</li>
	 *     <li>Pax Web introduces <em>shared</em> {@link HttpContext} where e.g., "default" <em>context</em>
	 *     can be used by many bundles and can allow {@link HttpContext} to load resources from different bundles.</li>
	 *     <li>"new" {@link org.osgi.service.http.context.ServletContextHelper} is by default shared by multiple
	 *     bundles, and its name <strong>is</strong> the only distinguishing element, however web elements may
	 *     refer (using an LDAP filter) to more than one {@link org.osgi.service.http.context.ServletContextHelper}
	 *     (using wildcard filter or when filtering by other service registration properties).</li>
	 *     <li>"old" Http Service specification doesn't mention at all the concept of sharing {@link HttpContext}
	 *     through service registry. That's Pax Web improvement and additional configuration method (like specifying
	 *     <em>context path</em>).</li>
	 *     <li>Registered <em>contexts</em> (and Whiteboard Service spec states this explicitly) may be instances
	 *     of {@link org.osgi.framework.ServiceFactory} which add bundle identity aspect to the context. This means
	 *     that even if two bundles register servlets with the same <em>context</em>, the {@code getResource()}
	 *     method loads resources from relevant bundle - different for different servlets.</li>
	 * </ul></p>
	 *
	 * <p>This mapping is not kept at virtual host level, because in Pax Web the relation is reversed comparing
	 * to Tomcat and reflects Jetty approach to virtual hosts (where hosts are <em>attributes</em> of a context).</p>
	 *
	 * <p>This map stores only non-shared (which is default with Http Service specification)
	 * {@link WebContainerContext} instances as keys.</p>
	 *
	 * <p>There's very important decision made here, but also easy to change. In felix.http,
	 * {@code org.apache.felix.http.base.internal.service.DefaultHttpContext} has default hashCode/equals, which
	 * means that "default" {@link HttpContext} is always different. When user
	 * calls {@code registerServlet(..., null)} many times, each servlet is registered to different "context"!.
	 * Http Service specification says ("102.2 Registering Servlets"):
	 * <blockquote>
	 *     Thus, Servlet objects registered with the same HttpContext object must also share the same
	 *     ServletContext object
	 * </blockquote>
	 * Specification doesn't precise what <em>the same</em> means - neither for HttpContext nor for the ServletContext.
	 * In Pax Web, knowing that there's Whiteboard aspect of the "context", we'll distinguish <strong>three</strong>
	 * contexts:<ul>
	 *     <li>standard {@link HttpContext} from Http Service spec - <em>the same</em> will be defined as <em>having
	 *     the same id and bundle</em></li>
	 *     <li>Pax Web shared/multi-bundle variant of {@link HttpContext} - <em>the same</em> will be defined as
	 *     <em>having the same id</em></li>
	 *     <li>standard {@link org.osgi.service.http.context.ServletContextHelper} from Whiteboard Service spec -
	 *     <em>the same</em> will be defined as <em>having the same name</em>, but also service ranking is taken into
	 *     account (when needed). {@link OsgiContextModel} that uses
	 *     {@link org.osgi.service.http.context.ServletContextHelper} is not tracked here.</li>
	 * </ul></p>
	 *
	 * <p>For now (2020-05-20) the decision is that if user registers a servlet with {@code null} {@link HttpContext},
	 * (s)he doesn't expect them to be registered in <em>different</em> contexts. So underneath, the same
	 * {@link OsgiContextModel} (and session and context attributes) is used. Which implies identity not by hashCode,
	 * but by name + bundle (if not shared).</p>
	 *
	 * <p>This map's values are {@link TreeSet} collections, so we can also have rank-sorting here. Normally user
	 * can't have more {@link OsgiContextModel} instances for single {@link WebContainerContext} when using
	 * {@link WebContainer} directly, but when Whiteboard-registering legacy {@link HttpContext} or
	 * {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} services it's possible to override (shadow)
	 * existing models - possibly with other model with the same name, but different context path. This behavior
	 * is Pax Web specific, because Http Service specification doesn't mention at all about context paths and
	 * rank-sorting.</p>
	 */
	private final Map<WebContainerContext, TreeSet<OsgiContextModel>> bundleContexts = new HashMap<>();

	/**
	 * <p>If an {@link OsgiContextModel} is associated with shared/multi-bundle {@link HttpContext}, such context
	 * can be retrieved here by name, which should be unique in Http Service namespace.</p>
	 *
	 * <p><em>Contexts</em> for Whiteboard Service should <strong>not</strong> be kept here. This implies that there's
	 * no way to register a {@link Servlet} through {@link org.osgi.service.http.HttpService} in association with
	 * a <em>context</em> created from Whiteboard trackers. What's surprising is that even if it's technically possible
	 * to do the opposite, it's explicitly forbidden by Whiteboard Service specification. Only filters, listeners
	 * and error pages can be registered to contexts created through {@link org.osgi.service.http.HttpService}.</p>
	 */
	private final Map<String, TreeSet<OsgiContextModel>> sharedContexts = new HashMap<>();

	/**
	 * <p>Whiteboard web elements are also registered into target container through an instance of
	 * {@link WebContainer} and {@link ServerModel}, though they have to be kept in separate map. What's important
	 * is that {@link OsgiContextModel} instances created and managed in pax-web-extender-whiteboard have to be
	 * passed to current {@link ServerController} and {@link ServerModel} is the class containing methods that
	 * configure {@link Batch} instances that are passed to given {@link ServerController}.</p>
	 *
	 * <p>The key in this map is {@link OsgiContextModel#getId()}, because ranking is taken into account
	 * at given {@link ServerController} level.</p>
	 */
	private final Map<String, OsgiContextModel> whiteboardContexts = new HashMap<>();

	// -- Web Elements model information

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
	 *
	 * <p>Note: Http Service doesn't say anything about filters, so there's no identity filter map in
	 * {@link ServerModel}</p>
	 */
	private final Map<Servlet, ServletModel> servlets = new IdentityHashMap<>();

	/**
	 * <p>When new servlet is registered using Whiteboard approach and there's already a servlet registered for
	 * given pattern or name, it MAY be unregistered after resolving the conflict using service ranking. But the
	 * "losing" servlet should not be forgotten - it should become <em>disabled</em> and will be enabled again if
	 * service registration changes.</p>
	 *
	 * <p>This set is reviewed every time existing registration is changed.</p>
	 *
	 * <p>This set is sorted by ranking, so when registration changes, disabled models are searched in proper
	 * order for one to be activated (for alias or name or patterns).</p>
	 */
	private final Set<ServletModel> disabledServletModels = new TreeSet<>();

	/**
	 * <p>Set of all registered filters. Used to block registration of the same filter more than once.
	 *
	 * <p>The map has keys obtained using {@link System#identityHashCode(Object)} to prevent user tricks with
	 * {@link Object#equals(Object)}.</p>
	 */
	private final Map<Filter, FilterModel> filters = new IdentityHashMap<>();

	/**
	 * <p>When new filter is registered with the same name it may either be registered as <em>disabled</em> or
	 * may lead to disabling other existing filters..</p>
	 *
	 * <p>This set is reviewed every time existing registration is changed.</p>
	 */
	private final Set<FilterModel> disabledFilterModels = new TreeSet<>();

	// TODO: Should listeners, security constraints, login configs, welcome files, security roles, error pages
	//       and mime types be checked for unique registration?

	/**
	 * Creates new global model of all web applications with {@link Executor} to be used for configuration and
	 * registration tasks.
	 * @param executor
	 */
	public ServerModel(Executor executor) {
		this.executor = executor;

		try {
			// check thread ID to detect whether we're running within it
			registrationThreadId = CompletableFuture.supplyAsync(() -> Thread.currentThread().getId(), executor).get();
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
	 * After ServerModel is created, we need to create a {@link ServletContextModel} for "/" path and let
	 * given {@lik ServerController} know about it.
	 *
	 * @param serverController
	 */
	public void createDefaultServletContextModel(final ServerController serverController) {
		runSilently(() -> {
			Batch batch = new Batch("Initialization of default servlet context model");

			getOrCreateServletContextModel(PaxWebConstants.DEFAULT_CONTEXT_PATH, batch);

			// OsgiContextModels are bundle-aware and will be created for ServiceModel and bundle-scoped
			// HttpServiceEnabled instance. We'll however create one default shared model
			String sharedContextName = PaxWebConstants.DEFAULT_SHARED_CONTEXT_NAME;
			sharedContexts.computeIfAbsent(sharedContextName, name -> new TreeSet<>())
					.add(createDefaultSharedtHttpContext(sharedContextName));

			// only if validation was fine, pass the batch to ServerController, where the batch may fail again
			serverController.sendBatch(batch);

			// if server runtime has accepted the changes (hoping it'll be in clean state if it didn't), lets
			// actually apply the changes to global model (through ServiceModel)
			batch.accept(this);

			return null;
		});
	}

	/**
	 * Creates named {@link OsgiContextModel} as shared {@link OsgiContextModel}.
	 * @param contextId
	 * @return
	 */
	public OsgiContextModel createDefaultSharedtHttpContext(String contextId) {
		return runSilently(() -> {
			if (sharedContexts.containsKey(contextId)) {
				// safe operation, because we should be in single threaded pool's thread
				OsgiContextModel model = getHighestRankedModel(sharedContexts.get(contextId));
				if (model != null) {
					return model;
				}
			}

			// bundle-agnostic (a.k.a. "shared") context model with supplier from static DEFAULT_CONTEXT_MODEL
			OsgiContextModel model = new OsgiContextModel(null, 0, 0L);
			model.setContextSupplier(OsgiContextModel.DEFAULT_CONTEXT_MODEL.getContextSupplier());
			model.setName(contextId);
			// the behavioral aspects from the template
			WebContainerContext wcc = model.getContextSupplier().apply(null, contextId);

			// the multibundle aspects in new instance - we know what the DEFAULT_CONTEXT_MODEL supplier returns...
			wcc = new DefaultMultiBundleWebContainerContext((WebContainerContextWrapper) wcc);

			model = createNewContextModel(wcc, null, PaxWebConstants.DEFAULT_CONTEXT_PATH);
			associateHttpContext(wcc, model);

			return model;
		});
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

	public <T> T runSilently(ModelRegistrationTask<T> task) {
		try {
			return run(task);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// --- methods that operate on "web contexts"

	/**
	 * <p>Returns {@link ServletContextModel} uniquely identified by <em>context path</em> (as defined by
	 * {@link ServletContext#getContextPath()}. There's single instance of {@link ServletContextModel} even if
	 * it's available in multiple {@link VirtualHostModel virtual hosts} and referenced by many
	 * {@link OsgiContextModel}.</p>
	 *
	 * <p>This method doesn't alter the global model, only adds relevant operation to the {@link Batch}.</p>
	 *
	 * <p>This method is used both in Http Service and Whiteboard Service scenarios because there really has to
	 * be single {@link ServletContextModel} per <em>context path</em>.</p>
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
	 * Gets first {@link OsgiContextModel} from ranked set or {@code null} if not available.
	 * @param rankedSet
	 * @return
	 */
	private OsgiContextModel getHighestRankedModel(Set<OsgiContextModel> rankedSet) {
		if (rankedSet == null) {
			return null;
		}
		Iterator<OsgiContextModel> it = rankedSet.iterator();
		return it.hasNext() ? it.next() : null;
	}

	/**
	 * <p>Returns (new or existing) {@link OsgiContextModel} associated with {@link HttpContext}. When the target
	 * {@link OsgiContextModel} is already available, validation is performed to check if it can be used with
	 * passed {@link HttpContext}. The returned {@link OsgiContextModel} is never
	 * {@link OsgiContextModel#DEFAULT_CONTEXT_MODEL}, which however may be used as template.</p>
	 *
	 * <p>As mentioned in {@link #bundleContexts} doc, the identity is checked for {@link WebContainerContext}
	 * and for "default" contexts (returned from {@code create} methods of
	 * {@link org.ops4j.pax.web.service.WebContainer}) this identity is name+bundle (or only name for <em>shared</em>
	 * contexts), but for user-provided contexts it can be anything.</p>
	 *
	 * <p>This method doesn't alter the global model, only adds relevant operation to the {@link Batch}.</p>
	 *
	 * @param context
	 * @param serviceBundle
	 * @param contextPath
	 * @param batch
	 * @return
	 * @throws IllegalStateException if there exists incompatible association
	 */
	public OsgiContextModel getOrCreateOsgiContextModel(WebContainerContext context,
			Bundle serviceBundle, String contextPath, Batch batch) throws IllegalStateException {
		OsgiContextModel existing = verifyExistingAssociation(context, serviceBundle);
		if (existing != null) {
			return existing;
		}

		ServletContextModel servletContextModel = getOrCreateServletContextModel(contextPath, batch);
		OsgiContextModel osgiContextModel = createNewContextModel(context, serviceBundle, contextPath);

		batch.addOsgiContextModel(osgiContextModel, servletContextModel);
		batch.associateOsgiContextModel(context, osgiContextModel);

		return osgiContextModel;
	}

	/**
	 * Ensures that {@link OsgiContextModel} created and managed in pax-web-extender-whiteboard and passed together
	 * with {@link ElementModel} is correctly registered in this {@link ServerModel} and passed to
	 * {@link ServerController}.
	 * @param contextModel
	 * @param batch
	 */
	public void registerOsgiContextModelIfNeeded(OsgiContextModel contextModel, Batch batch) {
		if (!whiteboardContexts.containsKey(contextModel.getId())) {
			ServletContextModel scm = getOrCreateServletContextModel(contextModel.getContextPath(), batch);
			batch.addOsgiContextModel(contextModel, scm);
		}
	}

	/**
	 * <p>This method ensures that <strong>if</strong> given {@link WebContainerContext} is already mapped to some
	 * {@link OsgiContextModel}, it is permitted to reference it within a scope of a given bundle.</p>
	 *
	 * <p>There are several scenarios, including one where {@link org.osgi.service.http.HttpService}, scoped to
	 * one bundle is used to register a servlet, while passed {@link HttpContext} is scoped to another bundle.</p>
	 *
	 * <p>With whiteboard approach, user can't trick the runtime with two different bundles (one from the passed
	 * {@link HttpContext} and other - from the bundle scoped {@link org.osgi.service.http.HttpService} because
	 * it's designed much better.</p>
	 *
	 * @param context existing extension of {@link HttpContext}, probably created from bundle-scoped
	 *        {@link org.osgi.service.http.HttpService}
	 * @param bundle actual bundle on behalf of each we try to perform a registration of web element - comes from
	 *        the scope of {@link org.osgi.service.http.HttpService} through which the registration is made.
	 * @return if the association exists and is valid, related {@link OsgiContextModel} is returned. {@code null}
	 *         is returned if the association is possible
	 * @throws IllegalStateException if there exists incompatible association
	 */
	public OsgiContextModel verifyExistingAssociation(final WebContainerContext context, final Bundle bundle)
			throws IllegalStateException {
		// quick check in case of references - first among shared contexts, then among bundle-scoped contexts
		// here we don't care much about the bundle of HttpService used
		OsgiContextModel contextModel = context.isShared()
				? getHighestRankedModel(sharedContexts.get(context.getContextId()))
				: getHighestRankedModel(bundleContexts.get(context));

		if (contextModel == null) {
			if (LOG.isTraceEnabled()) {
				LOG.trace(context + " is not yet associated with any context model");
			}
			return null;
		}

		if (!context.isShared()) {
			// OsgiContextModel has to have direct reference to WebContainerContext (i.e., no supplier, no
			// service reference - because these are used only in Whiteboard scenarios)
			if (!contextModel.hasDirectHttpContextInstance()) {
				throw new IllegalStateException("Existing " + contextModel
						+ " doesn't have any HttpContext associated");
			}
			if (!bundle.equals(contextModel.getOwnerBundle())) {
				if (contextModel.getOwnerBundle() != null) {
					throw new IllegalStateException("Existing " + contextModel
							+ " is not shared and can't be used by bundle " + bundle);
				} else {
					// we could access shared contextModel, but we're using non-shared WebContainerContext to access it
					throw new IllegalStateException("Existing " + contextModel
							+ " is shared, but registration is perfomed using non-shared " + context);
				}
			}
		}

		if (context.isShared() && contextModel.getOwnerBundle() != null) {
			throw new IllegalStateException("Existing " + contextModel
					+ " is owned by " + contextModel.getOwnerBundle() + ", but registration is perfomed using shared "
					+ context);
		}

		if (LOG.isTraceEnabled()) {
			LOG.trace(context + " can be associated with " + contextModel + " in the scope of " + bundle);
		}

		return contextModel;
	}

	/**
	 * <p>Method used to create new instance of {@link OsgiContextModel} when the <em>input</em>
	 * {@link WebContainerContext} is not yet associated with any context.</p>
	 *
	 * <p>Dedicated method for this purpose emphasizes the importance and fragility of {@link OsgiContextModel}.
	 * The only other way to create {@link OsgiContextModel} is the Whiteboard approach, when user registers one
	 * of these OSGi services:<ul>
	 *     <li>{@link org.osgi.service.http.context.ServletContextHelper} with service registration parameters</li>
	 *     <li>{@link HttpContext} with service registration parameters - not recommended</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.ServletContextHelperMapping} - Pax Web Whiteboard</li>
	 *     <li>{@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping} - Pax Web Whiteboard</li>
	 * </ul></p>
	 *
	 * <p>The above Whiteboard methods allow to specify (service registration parameters, direct
	 * values in {@link org.ops4j.pax.web.service.whiteboard.ContextMapping}) additional information about
	 * {@link OsgiContextModel}:<ul>
	 *     <li>context path</li>
	 *     <li>context (init) parameters</li>
	 *     <li>virtual hosts</li>
	 * </ul>Here, these attributes are not specified and the created {@link OsgiContextModel} is used only
	 * for Http Service scenario.</p>
	 *
	 * @param webContext
	 * @param serviceBundle
	 * @param contextPath
	 * @return
	 */
	public OsgiContextModel createNewContextModel(WebContainerContext webContext, Bundle serviceBundle,
			String contextPath) {
		OsgiContextModel osgiContextModel = new OsgiContextModel(webContext, serviceBundle, contextPath);

		// this context is not registered using Whiteboard, so we have full right to make it parameterless
		osgiContextModel.getContextParams().clear();
		// explicit proof that no particular VHost is associated, thus context will be available through all VHosts
		osgiContextModel.getVirtualHosts().clear();

		// the context still should behave (almos) like it was registered
		Hashtable<String, Object> registration = osgiContextModel.getContextRegistrationProperties();
		registration.clear();
		// we pretend that this HttpContext/ServletContextModel was:
		//  - registered to NOT represent the Whiteboard's context (org.osgi.service.http.context.ServletContextHelper)
		registration.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
		//  - registered to represent the HttpService's context (org.osgi.service.http.HttpContext)
		registration.put(HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, webContext.getContextId());
		//  - registered with legacy context id parameter
		registration.put(PaxWebConstants.SERVICE_PROPERTY_HTTP_CONTEXT_ID, webContext.getContextId());
		//  - registered by user as OSGi services
		registration.put(Constants.SERVICE_ID, 0L);
		osgiContextModel.setServiceId(0L);
		registration.put(Constants.SERVICE_RANKING, 0);
		osgiContextModel.setServiceRank(0);
		//  - registered with given context path
		registration.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);

		LOG.trace("Created new {}", osgiContextModel);

		return osgiContextModel;
	}

	/**
	 * <p>Simply mark {@link WebContainerContext} as the owner/creator/initiator of given {@link OsgiContextModel}
	 * whether it's shared or bundle-related.</p>
	 */
	public void associateHttpContext(final WebContainerContext context, final OsgiContextModel osgiContextModel) {
		if (context.isShared()) {
			sharedContexts.computeIfAbsent(context.getContextId(), c -> new TreeSet<>()).add(osgiContextModel);
			LOG.debug("Configured shared context {} -> {}", context, osgiContextModel);
		} else {
			bundleContexts.computeIfAbsent(context, c -> new TreeSet<>()).add(osgiContextModel);
			LOG.debug("Created association {} -> {}", context, osgiContextModel);
		}
	}

	/**
	 * Returns (if exists) bundle-scoped {@link OsgiContextModel} with given name. This method retrieves only
	 * "default" contexts created via {@link WebContainer#createDefaultHttpContext()}.
	 * @param name
	 * @param ownerBundle
	 * @return
	 */
	public OsgiContextModel getBundleContextModel(String name, Bundle ownerBundle) {
		// Using new DefaultHttpContext means that we'll retrieve only "default" contexts
		return getHighestRankedModel(bundleContexts.get(new DefaultHttpContext(ownerBundle, name)));
	}

	/**
	 * Returns (if exists) bundle-scoped {@link OsgiContextModel} for given {@link WebContainerContext}. This
	 * method can retrieve all the contexts - even ones created for entirely custom implementations of
	 * {@link HttpContext} interface.
	 * @param context
	 * @return
	 */
	public OsgiContextModel getBundleContextModel(WebContainerContext context) {
		return getHighestRankedModel(bundleContexts.get(context));
	}

	/**
	 * Returns (if exists) bundle-agnostic (shared) {@link OsgiContextModel}
	 * @param name
	 * @return
	 */
	public OsgiContextModel getSharedContextModel(String name) {
		return getHighestRankedModel(sharedContexts.get(name));
	}

	/**
	 * <p>Returns {@link OsgiContextModel} contexts that can be used by given bundle - i.e., can be targets of
	 * Whiteboard web element registered withing given bundle.</p>
	 *
	 * <p>None of the returned contexts will have a registration property named
	 * {@link HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_NAME} because they should match LDAP filters with
	 * {@link HttpWhiteboardConstants#HTTP_SERVICE_CONTEXT_PROPERTY} property <strong>only</strong>.</p>
	 *
	 * @param bundle
	 * @return
	 */
	public List<OsgiContextModel> getOsgiContextModels(Bundle bundle) {
		final List<OsgiContextModel> contexts = new LinkedList<>();

		// bundle contexts
		runSilently(() -> {
			bundleContexts.forEach((context, set) -> {
				if (bundle.equals(context.getBundle())) {
					contexts.add(getHighestRankedModel(set));
				}
			});
			return null;
		});

		// shared contexts
		contexts.addAll(sharedContexts.values().stream().map(this::getHighestRankedModel).collect(Collectors.toSet()));

		return contexts;
	}

	// --- methods that operate on "web elements"

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
	public void addServletModel(final ServletModel model, Batch batch) throws NamespaceException, ServletException {
		if (model.getContextModels().isEmpty()) {
			throw new IllegalArgumentException("Can't register " + model + ", it is not associated with any context");
		}

		if (model.getServlet() != null && servlets.containsKey(model.getServlet())) {
			throw new ServletException("Can't register servlet " + model.getServlet() + ", it has"
					+ " already been registered using " + servlets.get(model.getServlet()));
		}

		// Even if a servlet is associated with more OsgiContextModels, eventually it is a set of unique
		// ServletContextModels where the servlet is registered
		Set<ServletContextModel> targetServletContexts = getServletContextModels(model);

		// name checking, but remember that new model may replace existing model, so check should be performed
		// after possible override. For each servlet context in this map we will have a ServletModel with
		// conflicting servlet name
		Map<ServletContextModel, ServletModel> contextsWithNameConflicts = new HashMap<>();
		for (ServletContextModel sc : targetServletContexts) {
			// don't check ranking here - name conflict data is prepared for Http Service (alias) scenario
			if (sc.getServletNameMapping().containsKey(model.getName())) {
				contextsWithNameConflicts.put(sc, sc.getServletNameMapping().get(model.getName()));
			}
		}

		// alias checking: 102.2 "Registering Servlets". Fast exception in case of conflict. No service ranking checks

		if (model.getAlias() != null) {
			for (ServletContextModel sc : targetServletContexts) {
				if (sc.getAliasMapping().containsKey(model.getAlias())) {
					String msg = String.format("%s can't be registered."
									+ " Context %s already contains servlet mapping for alias %s: %s",
							model, sc.getContextPath(), model.getAlias(), sc.getAliasMapping().get(model.getAlias()));
					throw new NamespaceException(msg);
				}
			}

			// in Whiteboard, we have to check service ranking, potentially disabling existing servlets,
			// but in HttpService case, it's a bit simpler. Only name conflict should be checked and there's nothing
			// about disabling existing servlets by service ranking - plain, crude NamespaceException is thrown
			//
			//    102.2 Registering Servlets
			//
			//    [...] and the Servlet object is registered with the name "/servlet" [...]
			//    If an attempt is made to register a resource or Servlet object under the same name as a currently
			//    registered resource or Servlet object, a NamespaceException is thrown
			//
			// looks like "name" and "alias" concepts are interchangeable in HttpService spec...

			for (Map.Entry<ServletContextModel, ServletModel> entry : contextsWithNameConflicts.entrySet()) {
				String msg = String.format("%s can't be registered. %s already contains servlet named %s: %s",
						model, entry.getKey(), model.getName(), entry.getValue());
				throw new NamespaceException(msg);
			}

			// no alias or name conflicts, add to batch
			batch.addServletModel(this, model);
			return;
		}

		// URL mapping checking: 140.4 "Registering Servlets". Service ranking/id check in case of conflict
		// if there's a conflict, model can be registered only if it wins (by ranking) in each mapped context
		//
		// model can be registered in each of target contexts. But sometimes given context already
		// contains a model for given pattern. We should still register such model, but as "disabled", which
		// means that if existing mapping (with higher ranking) is unregistered (or somehow disabled), the
		// "waiting" mapping "jumps in" as next active one
		// however, if new model wins, the "losing" models should be disabled in all contexts - even if new
		// model has "beaten" the old one only in few of them. This scenario is not well specified in Whiteboard
		// Service specification.
		//
		// we should do it also for servlets registered with alias, because internally, "/alias" is always
		// translated into "/alias/*" URL Mapping

		boolean register = true;
		Set<ServletModel> newlyDisabled = new HashSet<>();

		for (ServletContextModel sc : targetServletContexts) {
			for (String pattern : model.getUrlPatterns()) {
				ServletModel existing = sc.getServletUrlPatternMapping().get(pattern);
				if (existing != null) {
					// service.ranking/service.id checking
					if (model.compareTo(existing) < 0) {
						// we won, but still can lose, because it's not the only pattern (or the only context)
						newlyDisabled.add(existing);
					} else {
						LOG.warn("{} can't be registered now in context {} under \"{}\" mapping. Conflict with {}.",
								model, sc.getContextPath(), pattern, existing);
						register = false;
						break;
					}
				}
			}
			if (!register) {
				break;
			}
		}

		if (!register) {
			LOG.warn("Skipped registration of {} because of existing mappings. Servlet will be added as \"awaiting"
					+ " registration\".", model);
			// register the model as "awaiting" without touching existing mappings and without additional
			// check for name conflicts
			batch.addDisabledServletModel(this, model);
			return;
		}

		// if there were existing URL mappings used by this ServletModel, new model "won" with all of them
		// (didn't lose with any of them), so we have to unregister (deactivate) existing models according to
		// Whiteboard specification.
		//
		//    140.4 Registering Servlets
		//
		//    Servlet and resource service registrations associated with a single Servlet Context share the same
		//    namespace. In case of identical registration patterns, service ranking rules are used to select the
		//    service handling a request. That is, Whiteboard servlets that have patterns shadowed by other Whiteboard
		//    services associated with the same Servlet Context are represented in the failure DTOs.
		//
		// We can't delete the models entirely - they should await for possible future re-registration and also be
		// available for DTO purposes
		for (ServletModel existing : newlyDisabled) {
			// disable it even if it can stay active in some context(s)
			batch.disableServletModel(this, existing);

			// disabled servletModel should stop causing name conflicts in given servletContext
			contextsWithNameConflicts.entrySet().removeIf(e -> {
				boolean nameMatches = e.getValue().getName().equals(existing.getName());
				final boolean[] contextMatches = { false };
				getServletContextModels(existing).forEach(scm -> contextMatches[0] |= e.getKey().equals(scm));
				return nameMatches && contextMatches[0];
			});
		}

		// only after disabling lower ranked models we can check the name conflicts, because servlet
		// with conflicting name inside a context may have just been disabled.
		if (!contextsWithNameConflicts.isEmpty()) {
			LOG.warn("Skipped registration of {} because of existing mappings with name {}."
					+ " Servlet will be added as \"awaiting registration\".", model, model.getName());
			batch.addDisabledServletModel(this, model);
			return;
		}

		if (newlyDisabled.isEmpty()) {
			// nothing prevents us from registering new model for all required contexts, because when nothing
			// was disabled, nothing should be enabled except the new model
			batch.addServletModel(this, model);
			return;
		}

		// it's quite problematic part. we're in the method that only prepares the batch, but doesn't
		// yet change the model itself. Before the model is affected, we'll send this batch to
		// target runtime, so we already need to perform more complex calculation here, using temporary collections

		// each disabled servletModel may be a reason to enable other models. Currently disabled
		// ServerModels (+ our new model) may be enabled ONLY if they can be enabled in ALL associated contexts

		Map<String, Map<String, ServletModel>> currentlyEnabledByName = new HashMap<>();
		Map<String, Map<String, ServletModel>> currentlyEnabledByPattern = new HashMap<>();
		Set<ServletModel> currentlyDisabled = new TreeSet<>();
		prepareServletsSnapshot(currentlyEnabledByName, currentlyEnabledByPattern, currentlyDisabled,
				model, newlyDisabled);

		reEnableServletModels(currentlyDisabled, currentlyEnabledByName, currentlyEnabledByPattern, model, batch);

		if (currentlyDisabled.contains(model)) {
			batch.addDisabledServletModel(this, model);
		}
	}

	/**
	 * Adds relevant batch operations related to unregistration of {@link ServletModel servlet models}. Due to
	 * complexity of specification, simple "unregistration of servlet" may cause registration of one or more
	 * existing, but currently disabled servlets.
	 *
	 * @param models servlet models to unregister from all associated {@link OsgiContextModel}
	 * @param batch
	 * @throws IllegalStateException if anything goes wrong
	 */
	public void removeServletModels(List<ServletModel> models, Batch batch) {
		// each of the servlet models that we're unregistering may be registered in many servlet contexts
		// and in each of those contexts, such unregistration may lead to reactivation of some existing, currently
		// disabled servlet models - similar situation to servlet registration, that may disable some models which in
		// turn may lead to re-registration of other models

		// this is straightforward
		batch.removeServletModels(this, models);

		Map<String, Map<String, ServletModel>> currentlyEnabledByName = new HashMap<>();
		Map<String, Map<String, ServletModel>> currentlyEnabledByPattern = new HashMap<>();
		Set<ServletModel> currentlyDisabled = new TreeSet<>();
		prepareServletsSnapshot(currentlyEnabledByName, currentlyEnabledByPattern, currentlyDisabled,
				null, new HashSet<>(models));

		// review all disabled servlet models (in ranking order) to verify if they can be enabled again
		reEnableServletModels(currentlyDisabled, currentlyEnabledByName, currentlyEnabledByPattern, null, batch);
	}

	/**
	 * Preparation for {@link #reEnableServletModels(Set, Map, Map, ServletModel, Batch)} that does
	 * proper copy of current state of all {@link ServletContextModel}
	 *
	 * @param currentlyEnabledByName
	 * @param currentlyEnabledByPattern
	 * @param currentlyDisabled
	 * @param newlyAdded prepared snapshot will include newly added model as currentlyDisabled
	 *        (to enable it potentially)
	 * @param newlyDisabled prepared snapshot will already have newlyDisabled models removed from snapshot mappings
	 */
	private void prepareServletsSnapshot(Map<String, Map<String, ServletModel>> currentlyEnabledByName,
			Map<String, Map<String, ServletModel>> currentlyEnabledByPattern,
			Set<ServletModel> currentlyDisabled,
			ServletModel newlyAdded, Set<ServletModel> newlyDisabled) {

		currentlyDisabled.addAll(disabledServletModels);

		servletContexts.values().forEach(scm -> {
			String path = scm.getContextPath();
			// deep copies
			HashMap<String, ServletModel> enabledByName = new HashMap<>(scm.getServletNameMapping());
			HashMap<String, ServletModel> enabledByPattern = new HashMap<>(scm.getServletUrlPatternMapping());
			currentlyEnabledByName.put(path, enabledByName);
			currentlyEnabledByPattern.put(path, enabledByPattern);

			// newlyDisabled are scheduled for disabling (in batch), so let's remove them from the snapshot
			if (newlyDisabled != null) {
				newlyDisabled.forEach(sm -> {
					getServletContextModels(sm).forEach(scm2 -> {
						if (scm.equals(scm2)) {
							enabledByName.remove(sm.getName(), sm);
							Arrays.stream(sm.getUrlPatterns()).forEach(pattern -> {
								enabledByPattern.remove(pattern, sm);
							});
						}
					});
				});
			}
		});

		// newlyAdded is for now only "offered" to be registered as active, because if new model causes
		// disabling of existing model, other (disabled) model may be better than the newly registered one
		if (newlyAdded != null) {
			currentlyDisabled.add(newlyAdded);
		}
	}

	/**
	 * <p>Fragile method used both during servlet registration and unregistration. Starting with set of currently
	 * disabled models, this methods prepares batch operations that may enable some of them.</p>
	 *
	 * <p>This method has to be provided with current snapshot of all disabled and registered servlets and will
	 * be called recursively because every "woken up" model may lead to disabling of other models, which again
	 * may enable other models and so on...</p>
	 *
	 * @param currentlyDisabled currently disabled models - this collection may be shrunk in this method. Every
	 *        model removed from this collection will be batched for enabling
	 * @param currentlyEnabledByName temporary state of by-name servlets - may be altered during invocation
	 * @param currentlyEnabledByPattern temporary state of by-URL-pattern servlets - may be altered during invocation
	 * @param modelToEnable newly added model (could be {@code null}) - needed because when adding new servlet, it
	 *        is initialy treated as disabled. We have to decide then whether to enable existing model or add
	 *        this new one
	 * @param batch this {@link Batch} will collect avalanche of possible disable/enable operations
	 */
	private void reEnableServletModels(Set<ServletModel> currentlyDisabled,
			Map<String, Map<String, ServletModel>> currentlyEnabledByName,
			Map<String, Map<String, ServletModel>> currentlyEnabledByPattern,
			ServletModel modelToEnable, Batch batch) {

		Set<ServletModel> newlyDisabled = new LinkedHashSet<>();
		boolean change = false;

		// reviewed using TreeSet, i.e., by proper ranking
		for (Iterator<ServletModel> iterator = currentlyDisabled.iterator(); iterator.hasNext(); ) {
			// this is the highest ranked, currently disabled servlet model
			ServletModel disabled = iterator.next();
			boolean canBeEnabled = true;
			newlyDisabled.clear();

			Set<ServletContextModel> contextsOfDisabledModel = getServletContextModels(disabled);

			// check for name and URL pattern conflicts in all its contexts. There are two outcomes:
			//  - any conflict - model won't be enabled and won't cause any changes
			//  - no conflict (or winning over active, but lower ranked models) - model will be enabled
			//    and some active model (or models) will be disabled
			//  - the good thing is (I hope) that if some active model gets disabled, it can't be enabled in the
			//    same review due to deactivation of yet another model

			for (ServletContextModel sc : contextsOfDisabledModel) {
				String cp = sc.getContextPath();

				// name conflict check
				for (Map.Entry<String, ServletModel> entry : currentlyEnabledByName.get(cp).entrySet()) {
					ServletModel enabled = entry.getValue();
					boolean nameConflict = haveAnyNameConflict(disabled.getName(), enabled.getName(), disabled, enabled);
					if (nameConflict) {
						// name conflict with existing, enabled model. BUT currently disabled model may have
						// higher ranking...
						if (disabled.compareTo(enabled) < 0) {
							// still can be enabled (but we have to check everything) and currently disabled
							// may potentially get disabled
							newlyDisabled.add(enabled);
						} else {
							canBeEnabled = false;
							break;
						}
					}
				}
				if (!canBeEnabled) {
					break;
				}

				// URL mapping check
				for (String pattern : disabled.getUrlPatterns()) {
					ServletModel existingMapping = currentlyEnabledByPattern.get(cp).get(pattern);
					if (existingMapping != null) {
						// URL conflict with existing, enabled model. BUT currently disabled model may have
						// higher ranking...
						if (disabled.compareTo(existingMapping) < 0) {
							// still can be enabled (but we have to check everything) and currently disabled
							// may potentially get disabled
							newlyDisabled.add(existingMapping);
						} else {
							canBeEnabled = false;
							break;
						}
					}
				}
				if (!canBeEnabled) {
					break;
				}
			} // end of check for the conflicts in all the contexts

			// disabled model can be enabled again - in all its contexts
			if (canBeEnabled) {
				newlyDisabled.forEach(model -> {
					// disable the one that have lost
					batch.disableServletModel(ServerModel.this, model);

					// and forget about it in the snapshot
					getServletContextModels(model).forEach(scm -> {
						currentlyEnabledByName.get(scm.getContextPath()).remove(model.getName(), model);
						Arrays.stream(model.getUrlPatterns()).forEach(p -> {
							currentlyEnabledByPattern.get(scm.getContextPath()).remove(p, model);
						});
					});

					// do NOT add newlyDisabled to "currentlyDisabled" - we don't want to check if they can be enabled!
				});

				// update the snapshot - newly enabled model should be visible as the one registered
				// under its name and patterns
				for (ServletContextModel sc : contextsOfDisabledModel) {
					currentlyEnabledByName.get(sc.getContextPath()).put(disabled.getName(), disabled);
					Arrays.stream(disabled.getUrlPatterns())
							.forEach(p -> currentlyEnabledByPattern.get(sc.getContextPath()).put(p, disabled));
				}
				if (modelToEnable != null && modelToEnable.equals(disabled)) {
					batch.addServletModel(this, disabled);
				} else {
					batch.enableServletModel(this, disabled);
				}
				// remove - to check if our new model should later be added as disabled
				iterator.remove();
				change = true;
			}
			if (change) {
				// exit the loop (leaving some currently disabled models not checked) and get ready for recursion
				break;
			}
		} // end of "for" loop that checks all currently disabled models that can potentially be enabled

		if (change) {
			reEnableServletModels(currentlyDisabled, currentlyEnabledByName, currentlyEnabledByPattern,
					modelToEnable, batch);
		}
	}

	/**
	 * <p>Validates {@link FilterModel} and adds relevant batch operations if validation is successful.</p>
	 *
	 * <p>Handling filters is a bit different, because each added filter may have to be added in particular order
	 * (probably in-between some existing filters), so the changes are added into batch as one step.</p>
	 *
	 * @param model filter model to register with all associated {@link OsgiContextModel} needed
	 * @param batch
	 * @throws ServletException
	 * @throws IllegalStateException if anything goes wrong
	 * @throws IllegalArgumentException if validation fails
	 */
	public void addFilterModel(final FilterModel model, Batch batch) throws ServletException {
		if (model.getContextModels().isEmpty()) {
			throw new IllegalArgumentException("Can't register " + model + ", it is not associated with any context");
		}

		if (model.getFilter() != null && filters.containsKey(model.getFilter())) {
			throw new ServletException("Can't register filter " + model.getFilter() + ", it has"
					+ " already been registered using " + filters.get(model.getFilter()));
		}

		Set<ServletContextModel> targetServletContexts = getServletContextModels(model);

		// a bit easier than with servlets - there may be many filters mapped to the same URL patterns and
		// servlet names (and regexps, as allowed by Whiteboard Service specification). Only filter name conflicts
		// should be checked
		// if new model wins, the "losing" models should be disabled in all contexts - even in contexts not targeted
		// by the new model

		boolean register = true;
		Set<FilterModel> newlyDisabled = new HashSet<>();

		// in servlet case, name conflict resolution is done in 2 phases, because existing servlet that causes
		// a conflict may stop doing so because it may be lower ranked wrt URL mapping
		// with filters we can quickly determine the name conflict and potentially know which existing (currently
		// enabled) servlets to disable
		for (ServletContextModel sc : targetServletContexts) {
			FilterModel existing = sc.getFilterNameMapping().get(model.getName());
			if (existing != null) {
				if (model.compareTo(existing) < 0) {
					// new model wins the name conflict.
					newlyDisabled.add(existing);
				} else {
					LOG.warn("{} can't be registered now in context {}. Name conflict with {}.",
							model, sc.getContextPath(), existing);
					register = false;
					break;
				}
			}
		}

		if (!register) {
			LOG.warn("Skipped registration of {} because of name conflict. Filter will be registered as \"awaiting"
					+ " registration\".", model);
			// register the model as "awaiting" without touching existing mappings and without additional
			// check for name conflicts. Such batch operation should only be processed by model, not by actual
			// server runtime
			batch.addDisabledFilterModel(this, model);
			return;
		}

		// by adding new FilterModel we can disable and enable some existing ones. Imagine this scenario:
		// - context /c1
		//    - filter f1(a) with rank 5
		// - context /c2
		//    - filter f2(a) with rank 5
		// - context /c3
		//    - filter f1(a) with rank 5
		//    - filter f1(b) with rank 3 (currently disabled)
		//    - filter f2(a) with rank 5
		//    - filter f2(b) with rank 3 (currently disabled)
		//    - filter f3(a) with rank 5
		//    - filter f3(b) with rank 3 (currently disabled)
		//
		// now imagine registration of new filter "f1(c)" with rank 10:
		//  - if registered only to context /c1, then existing f1(a) filter
		//     - becomes disabled in /c1
		//     - becomes disabled in /c3
		//     - "waiting" f1(b) becomes enabled in /c3, because "f1(c)" was not registered in /c3
		//  - if registered to context /c1 and /c3:
		//     - f1(a) becomes disabled in /c1 and /c3
		//     - f1(b) stays disabled in /c3
		// new f2(c) filter is registered in /c1:
		//  - it's registered without problems
		//  - f2(a) stays enabled in /c2 and /c3

		// this is completely different than in addServletModel() - we're not sending set of disabled servlets
		// and, as last, the newly registered servlet. Instead we have to prepare full list of filters registered into
		// each of the target contexts.
		// we consider ONLY the contexts affected by newly registered filter, but in case of the above scenario,
		// new filter registered only into /c1 affected list of filters in /c3 as well
		// individual operations (disable, enable) are fine from ServerModel perspective, but we need special
		// operation (all-at-once) to be sent to ServerController

		// first - each newly disabled FilterModel should be added to batch as disabled - for the purpose of
		// model altering. Actual server will get list of the filters in one operation
		for (FilterModel existing : newlyDisabled) {
			// disable it even if it can stay active in some other context(s), not targeted by newly registered filter
			batch.disableFilterModel(this, existing);
		}

		// and also if we haven't disabled anything, new model will definitely be added as enabled - but it's NOT
		// the end of processing in case of filters
		if (newlyDisabled.isEmpty()) {
			batch.addFilterModel(this, model);
			// no return here! (unlike in case of servlets)
		}

		// this map will contain ALL filters registered per context path - including currently enabled, newly
		// registered and newly enabled. When set is TreeSet, ordering will be correct
		Map<String, TreeSet<FilterModel>> currentlyEnabledByName = new HashMap<>();
		Set<FilterModel> currentlyDisabled = new TreeSet<>();
		prepareFiltersSnapshot(currentlyEnabledByName, currentlyDisabled, model, newlyDisabled);

		reEnableFilterModels(currentlyDisabled, currentlyEnabledByName, model, batch);

		// finally - full set of filter state changes in all affected servlet contexts
		batch.updateFilters(currentlyEnabledByName);
	}

	public void removeFilterModels(List<FilterModel> models, Batch batch) {
		// this is straightforward
		batch.removeFilterModels(this, models);

		Map<String, TreeSet<FilterModel>> currentlyEnabledByName = new HashMap<>();
		Set<FilterModel> currentlyDisabled = new TreeSet<>();
		prepareFiltersSnapshot(currentlyEnabledByName, currentlyDisabled, null, new HashSet<>(models));

		// review all disabled filter models (in ranking order) to verify if they can be enabled again
		reEnableFilterModels(currentlyDisabled, currentlyEnabledByName, null, batch);

		// finally - full set of filter state changes in all affected servlet contexts
		batch.updateFilters(currentlyEnabledByName);
	}

	/**
	 * Preparation for {@link #reEnableFilterModels(Set, Map, FilterModel, Batch)} that does
	 * proper copy of current state of all {@link ServletContextModel}
	 *
	 * @param currentlyEnabledByName
	 * @param currentlyDisabled
	 * @param newlyAdded prepared snapshot will include newly added model as currentlyDisabled
	 *        (to enable it potentially)
	 * @param newlyDisabled prepared snapshot will already have newlyDisabled models removed from snapshot mappings
	 */
	private void prepareFiltersSnapshot(Map<String, TreeSet<FilterModel>> currentlyEnabledByName,
			Set<FilterModel> currentlyDisabled,
			FilterModel newlyAdded, Set<FilterModel> newlyDisabled) {

		currentlyDisabled.addAll(disabledFilterModels);

		servletContexts.values().forEach(scm -> {
			String path = scm.getContextPath();
			// deep copies
			TreeSet<FilterModel> enabledFilters = new TreeSet<>(scm.getFilterNameMapping().values());
			currentlyEnabledByName.put(path, enabledFilters);

			// newlyDisabled are scheduled for disabling (in batch), so let's remove them from the snapshot
			if (newlyDisabled != null) {
				newlyDisabled.forEach(fm -> {
					getServletContextModels(fm).forEach(scm2 -> {
						if (scm.equals(scm2)) {
							enabledFilters.remove(fm);
						}
					});
				});
			}
		});

		// newlyAdded is for now only "offered" to be registered as active, because if new model causes
		// disabling of existing model, other (disabled) model may be better than the newly registered one
		if (newlyAdded != null) {
			currentlyDisabled.add(newlyAdded);
		}
	}

	/**
	 * <p>Fragile method used both during filter registration and unregistration. Similar to (and simpler than)
	 * equivalent method for servlets.</p>
	 *
	 * <p>This method has to be provided with current snapshot of all disabled and registered filters and will
	 * be called recursively because every "woken up" model may lead to disabling of other models, which again
	 * may enable other models and so on...</p>
	 *
	 * @param currentlyDisabled currently disabled models - this collection may be shrunk in this method. Every
	 *        model removed from this collection will be batched for enabling
	 * @param currentlyEnabledByName temporary state of by-name filters - may be altered during invocation
	 * @param modelToEnable newly added model (could be {@code null}) - needed because when adding new filter, it
	 *        is initialy treated as disabled. We have to decide then whether to enable existing model or add
	 *        this new one
	 * @param batch this {@link Batch} will collect avalanche of possible disable/enable operations
	 */
	private void reEnableFilterModels(Set<FilterModel> currentlyDisabled,
			Map<String, TreeSet<FilterModel>> currentlyEnabledByName, FilterModel modelToEnable, Batch batch) {

		Set<FilterModel> newlyDisabled = new LinkedHashSet<>();
		boolean change = false;

		// reviewed using TreeSet, i.e., by proper ranking
		for (Iterator<FilterModel> iterator = currentlyDisabled.iterator(); iterator.hasNext(); ) {
			// this is the highest ranked, currently disabled filter model
			FilterModel disabled = iterator.next();
			boolean canBeEnabled = true;
			newlyDisabled.clear();

			Set<ServletContextModel> contextsOfDisabledModel = getServletContextModels(disabled);

			for (ServletContextModel sc : contextsOfDisabledModel) {
				String cp = sc.getContextPath();

				// name conflict check
				for (FilterModel enabled : currentlyEnabledByName.get(cp)) {
					boolean nameConflict = haveAnyNameConflict(disabled.getName(), enabled.getName(), disabled, enabled);
					if (nameConflict) {
						// name conflict with existing, enabled model. BUT currently disabled model may have
						// higher ranking...
						if (disabled.compareTo(enabled) < 0) {
							// still can be enabled (but we have to check everything) and currently disabled
							// may potentially get disabled
							newlyDisabled.add(enabled);
						} else {
							canBeEnabled = false;
							break;
						}
					}
				}
				if (!canBeEnabled) {
					break;
				}
			} // end of check for the conflicts in all the contexts

			// disabled model can be enabled again - in all its contexts
			if (canBeEnabled) {
				newlyDisabled.forEach(model -> {
					// disable the one that have lost
					batch.disableFilterModel(ServerModel.this, model);

					// and forget about it in the snapshot
					getServletContextModels(model).forEach(scm -> {
						currentlyEnabledByName.get(scm.getContextPath()).remove(model);
					});

					// do NOT add newlyDisabled to "currentlyDisabled" - we don't want to check if they can be enabled!
				});

				// update the snapshot - newly enabled model should be visible as the one registered
				for (ServletContextModel sc : contextsOfDisabledModel) {
					currentlyEnabledByName.get(sc.getContextPath()).add(disabled);
				}
				if (modelToEnable != null && modelToEnable.equals(disabled)) {
					batch.addFilterModel(this, disabled);
				} else {
					batch.enableFilterModel(this, disabled);
				}
				// remove - to check if our new model should later be added as disabled
				iterator.remove();
				change = true;
			}
			if (change) {
				// exit the loop (leaving some currently disabled models not checked) and get ready for recursion
				break;
			}
		} // end of "for" loop that checks all currently disabled models that can potentially be enabled

		if (change) {
			reEnableFilterModels(currentlyDisabled, currentlyEnabledByName, modelToEnable, batch);
		}
	}

	/**
	 * Check conflict between named models. Name conflict is not a problem if it occurs in disjoint sets of
	 * target servlet contexts. In other words, we can't have servlet "s1" registered in "/c1" and "/c2" and allow
	 * registration of another "s1" servlet in "/c2". However it could be possible if "s1" was originally registered
	 * only in "/c1".
	 *
	 * @param name1
	 * @param name2
	 * @param model1
	 * @param model2
	 * @param <T>
	 * @return
	 */
	private <T> boolean haveAnyNameConflict(String name1, String name2, ElementModel<T> model1, ElementModel<T> model2) {
		// if one model has name conflict with other model, check whether the conflict
		// is in disjoint servlet contexts
		if (name1.equals(name2)) {
			for (ServletContextModel sc1 : getServletContextModels(model1)) {
				for (ServletContextModel sc2 : getServletContextModels(model2)) {
					if (sc1.equals(sc2)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Each {@link ElementModel} is associated with one ore more {@link OsgiContextModel} which are in turn
	 * associated with {@link ServletContextModel} (by context path). Sometimes many {@link OsgiContextModel} models
	 * are associated with the same target servlet context. This method returns unique set of
	 * {@link ServletContextModel} contexts
	 *
	 * @param model
	 * @return
	 */
	private Set<ServletContextModel> getServletContextModels(ElementModel<?> model) {
		return model.getContextModels().stream()
				.map(ocm -> servletContexts.get(ocm.getContextPath())).collect(Collectors.toSet());
	}

	// --- batch operation visit() methods performed without validation, because it was done earlier

	@Override
	public void visit(ServletContextModelChange change) {
		ServletContextModel model = change.getServletContextModel();
		this.servletContexts.put(model.getContextPath(), model);
	}

	@Override
	public void visit(OsgiContextModelChange change) {
		switch (change.getKind()) {
			case ADD: {
				OsgiContextModel model = change.getOsgiContextModel();
				if (!model.hasDirectHttpContextInstance()) {
					// it's a whiteboard context
					whiteboardContexts.put(model.getId(), model);
				}
			}
			default:
				break;
		}
	}

	@Override
	public void visit(ServletModelChange change) {
		switch (change.getKind()) {
			case ADD: {
				ServletModel model = change.getServletModel();

				// add new ServletModel to all target contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> {
					if (change.isDisabled()) {
						// registered initially as disabled
						disabledServletModels.add(model);
					} else {
						sc.getServletNameMapping().put(model.getName(), model);
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
			case DELETE: {
				List<ServletModel> models = change.getServletModels();

				models.forEach(model -> {
					if (model.getServlet() != null) {
						servlets.remove(model.getServlet(), model);
					}
					// could be among disabled ones
					boolean wasDisabled = disabledServletModels.remove(model);

					if (!wasDisabled) {
						// remove ServletModel from all target contexts. disabled model was not available there
						Set<ServletContextModel> servletContexts = getServletContextModels(model);
						servletContexts.forEach(sc -> {
							// use special, 2-arg version of map.remove()
							sc.getServletNameMapping().remove(model.getName(), model);
							if (model.getAlias() != null) {
								sc.getAliasMapping().remove(model.getAlias(), model);
							}
							Arrays.stream(model.getUrlPatterns()).forEach(p -> sc.getServletUrlPatternMapping().remove(p, model));
						});
					}
				});
				break;
			}
			case ENABLE: {
				ServletModel model = change.getServletModel();
				// enable a servlet in all associated contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> sc.enableServletModel(model));
				disabledServletModels.remove(model);
				break;
			}
			case DISABLE: {
				ServletModel model = change.getServletModel();
				disabledServletModels.add(model);
				// disable a servlet in all associated contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> sc.disableServletModel(model));
				break;
			}
			case MODIFY:
			default:
				break;
		}
	}

	@Override
	public void visit(FilterModelChange change) {
		switch (change.getKind()) {
			case ADD: {
				FilterModel model = change.getFilterModel();

				// add new FilterModel to all target contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> {
					if (change.isDisabled()) {
						// registered initially as disabled
						disabledFilterModels.add(model);
					} else {
						sc.getFilterNameMapping().put(model.getName(), model);
					}
				});

				if (model.getFilter() != null) {
					filters.put(model.getFilter(), model);
				}
				break;
			}
			case MODIFY:
				break;
			case DELETE: {
				List<FilterModel> models = change.getFilterModels();

				models.forEach(model -> {
					if (model.getFilter() != null) {
						filters.remove(model.getFilter(), model);
					}
					// could be among disabled ones
					boolean wasDisabled = disabledFilterModels.remove(model);

					if (!wasDisabled) {
						// remove FilterModel from all target contexts. disabled model was not available there
						Set<ServletContextModel> servletContexts = getServletContextModels(model);
						servletContexts.forEach(sc -> {
							// use special, 2-arg version of map.remove()
							sc.getFilterNameMapping().remove(model.getName(), model);
						});
					}
				});
				break;
			}
			case ENABLE: {
				FilterModel model = change.getFilterModel();
				// enable a filter in all associated contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> sc.enableFilterModel(model));
				disabledFilterModels.remove(model);
				break;
			}
			case DISABLE: {
				FilterModel model = change.getFilterModel();
				disabledFilterModels.add(model);
				// disable a filter in all associated contexts
				Set<ServletContextModel> servletContexts = getServletContextModels(model);
				servletContexts.forEach(sc -> sc.disableFilterModel(model));
				break;
			}
			default:
				break;
		}
	}

	@Override
	public void visit(FilterStateChange filterStateChange) {
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
