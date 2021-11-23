/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.whiteboard.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.events.WebContextEventListener;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventData;
import org.ops4j.pax.web.service.spi.util.WebContainerListener;
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Whiteboard extender context.</p>
 *
 * <p>This class collects all the customized objects related to <em>whiteboard web elements</em> (like servlets,
 * filters, ...) and <em>whiteboard contexts</em> (like {@link org.osgi.service.http.HttpContext} or
 * {@link org.osgi.service.http.context.ServletContextHelper}). All such customized objects (internal
 * representation of user-registered OSGi services) are then somehow passed to current
 * {@link org.osgi.service.http.HttpService} which should rather be Pax Web's
 * {@link WebContainer} extension of {@link org.osgi.service.http.HttpService}. It's not
 * a requirement, but initially the tests focus on integrating pax-web-extender-whiteboard with pax-web-runtime.</p>
 *
 * <p>The biggest change in Pax Web 8 is that incoming <em>web elements</em> are not registered in <em>web
 * applications</em>, but are directly passed to {@link WebContainer}, because in Pax Web 8 the model has changed,
 * allowing to associate single servlet (or filter, or ...) with more <em>contexts</em>.</p>
 *
 * <p>Most of the methods are synchronized, because they're called from service tracking methods that register and
 * unregister Whiteboard elements and contexts and on the other hand - from service listener methods that may
 * register/unregister target {@link WebContainer}. Synchronization isn't a big problem, because it is assumed that
 * registration/unregistration happens rarely (opposite to actual HTTP request processing) and is expected to
 * take some time.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek (since Pax Web 8)
 * @since 0.4.0, April 01, 2008
 */
public class WhiteboardExtenderContext implements WebContainerListener, WebContextEventListener {

	private static final Logger LOG = LoggerFactory.getLogger(WhiteboardExtenderContext.class);

	/**
	 * Deadlock preventing flag. When {@link WebContainer} is unregistered we'll be getting information about
	 * {@link OsgiContextModel} being removed, but there's no point altering the Whiteboard registrations, because
	 * web elements are uninstalled anyway (awaiting new registration of {@link WebContainer}).
	 */
	final AtomicBoolean acceptWabContexts = new AtomicBoolean(false);

	private final Bundle bundle;

	/** This is were the lifecycle of {@link WebContainer} is managed. */
	private final WebContainerManager webContainerManager;

	private ServiceReference<WebContainer> currentWebContainerReference;

	/**
	 * Per-{@link Bundle} cache of lists of Whiteboard services - to clean them up when bundle is gone. This
	 * map is not concurrent, because it's always accessed within a lock.
	 */
	private final Map<Bundle, BundleWhiteboardApplication> bundleApplications = new HashMap<>();

	/**
	 * This lock prevents concurrent access to a list of {@link #bundleApplications}. We should never get this
	 * lock <em>after</em> entering Pax Web configuration thread. After the lock was obtained, we can call into
	 * this single configuration thread, but not in different order.
	 */
	private final Lock lock = new ReentrantLock();

	//	private final ConcurrentHashMap<WebApplication, Integer> sharedWebApplicationCounter = new ConcurrentHashMap<>();

	/**
	 * <p>Context <em>name</em> to ordered (by ranking / service id) set of {@link OsgiContextModel} mapping.</p>
	 *
	 * <p>With Whiteboard web element registration, target {@link OsgiContextModel} is <strong>always</strong>
	 * referenced by name and there may be many <em>contexts</em> with the same name.
	 * {@link javax.servlet.Servlet} registered to e.g., {@code default} <em>context</em> may be re-registered if
	 * there's <em>better</em> context registered with the same {@code default} name and different ranking.</p>
	 *
	 * <p>The target <em>physical</em> servlet context (which in {@link org.ops4j.pax.web.service.spi.model.ServerModel}
	 * is kept as {@link org.ops4j.pax.web.service.spi.model.ServletContextModel}) is <strong>not</strong> stored
	 * here. Each {@link OsgiContextModel} refer to <em>physical</em> servlet context only by single service
	 * registration property - {@link HttpWhiteboardConstants#HTTP_WHITEBOARD_CONTEXT_PATH} - regardless of the
	 * type of the service registered by user (whether it was <em>official</em> {@link ServletContextHelper} or
	 * "legacy" {@link org.ops4j.pax.web.service.whiteboard.HttpContextMapping}).</p>
	 */
	private final Map<String, TreeSet<OsgiContextModel>> osgiContexts = new ConcurrentHashMap<>();

	/**
	 * We also have to store {@link OsgiContextModel} contexts in flat list to perform {@link Filter} based
	 * lookup.
	 */
	private final List<OsgiContextModel> osgiContextsList = new ArrayList<>();

	public WhiteboardExtenderContext(BundleContext bundleContext) {
		this(bundleContext, false);
	}

	public WhiteboardExtenderContext(BundleContext bundleContext, boolean synchronous) {
		this.bundle = bundleContext.getBundle();

		// remember ONLY "default" (mapped to "/") context model
		OsgiContextModel model = OsgiContextModel.DEFAULT_CONTEXT_MODEL;
		Bundle owner = FrameworkUtil.getBundle(this.getClass());
		if (owner != null) {
			model.setOwnerBundle(FrameworkUtil.getBundle(this.getClass()));
		}
		osgiContexts.computeIfAbsent(model.getName(), n -> new TreeSet<>()).add(model);
		osgiContextsList.add(model);

		webContainerManager = synchronous
				? new WebContainerManager(bundleContext, this)
				: new WebContainerManager(bundleContext, this, "HttpService->Whiteboard");
		webContainerManager.initialize();
	}

	@Override
	public void webContainerChanged(ServiceReference<WebContainer> oldReference, ServiceReference<WebContainer> newReference) {
		// when WebContainer service reference is changed, we should unregister any collected Whiteboard web element
		// or context from previous WebContainer and register them in new WebContainer
		// eventually the WebContainer reference will be passed to each BundleWhiteboardApplication
		if (oldReference != null) {
			webContainerRemoved(oldReference);
		}
		if (newReference != null) {
			webContainerAdded(newReference);
		}
	}

	/**
	 * Method called from some {@link BundleListener} to clean up the cache of bundle-related Whiteboard services
	 * @param bundle
	 */
	@Override
	public void bundleStopped(Bundle bundle) {
		BundleWhiteboardApplication application;
		lock.lock();
		try {
			application = bundleApplications.remove(bundle);
		} finally {
			lock.unlock();
		}
		if (application != null) {
			LOG.debug("Clearing Whiteboard cache for {}", bundle);
			application.cleanup();
		}
	}

	@Override
	public void wabContextRegistered(OsgiContextModel model) {
		if (!acceptWabContexts.get()) {
			return;
		}
		lock.lock();
		try {
			reRegisterWebElements();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void wabContextUnregistered(OsgiContextModel model) {
		if (!acceptWabContexts.get()) {
			return;
		}
		lock.lock();
		try {
			reRegisterWebElements();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Cleans up everything related to pax-web-extender-whiteboard
	 */
	public void shutdown() {
		webContainerManager.shutdown();
		currentWebContainerReference = null;
	}

	/**
	 * <p>Intermediary method used during Whiteboard element registration, that returns list of target
	 * {@link OsgiContextModel OSGi contexts} to register tracked Whiteboard element with.</p>
	 *
	 * <p>Remember - target {@link WebContainer} may <strong>not</strong> be available yet, but web elements
	 * tracked by pax-web-extender-whiteboard should know up front with which OSGi context models they'll
	 * be associated with. It's clearly stated in specification (140.3 Common Whiteboard Properties):<blockquote>
	 *    An LDAP-style filter to select the associated ServletContextHelper service to use. Any service property of
	 *    the Servlet Context Helper can be filtered on. If this property is missing the default Servlet Context Helper
	 *    is used.
	 *    [...]
	 *    Any service property of the Servlet Context Helper can be filtered on. If this property is missing the
	 *    default Servlet Context Helper is used.
	 * </blockquote></p>
	 *
	 * <p>If many {@link OsgiContextModel}s match, only highest ranked models for given name are returned.</p>
	 *
	 * @param bundle {@link Bundle} of the Whiteboard element for which we're looking for the associated contexts
	 * @param selector
	 * @return list of {@link OsgiContextModel} to associate the service (e.g., {@link javax.servlet.Servlet}) with.
	 */
	public List<OsgiContextModel> resolveContexts(Bundle bundle, Filter selector) {
		lock.lock();
		try {
			if (selector == null) {
				// easy - highest ranked "default" context model
				OsgiContextModel defaultModel
						= osgiContexts.get(HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME).iterator().next();
				return Collections.singletonList(defaultModel);
			}

			// more difficult, as according to "140.3 Common Whiteboard Properties", "Table 140.3 Common properties":
			//
			//     osgi.http.whiteboard.context.select [...] An LDAP-style filter to select the associated
			//     ServletContextHelper service to use. Any service property of the Servlet Context Helper can be filtered
			//     on. If this property is missing the default Servlet Context Helper is used.
			//
			// so we can even target two different "default" ServletContextHelpers and actual physical ServletContextModel
			// will be determined at registration time
			List<OsgiContextModel> targetContexts = new ArrayList<>();

			// check _contexts_ managed at pax-web-extender-whiteboard level
			for (OsgiContextModel model : osgiContextsList) {
				// one line "140.3 Common Whiteboard Properties" implementation of LDAP filter matching
				if (selector.matchCase(model.getContextRegistrationProperties())) {
					targetContexts.add(model);
				}
			}

			// get all the bundle-scoped contexts from HttpService. These will never
			// have "osgi.http.whiteboard.context.name" property specified, only
			// "osgi.http.whiteboard.context.httpservice" property, so no special check should be performed - just
			// matching the selector
			// In order for "typical" selector for Whiteboard -> HttpService:
			//     osgi.http.whiteboard.context.select = (osgi.http.whiteboard.context.httpservice=*)
			// to work, we're explicitly skipping "shared" contexts - user will still be able to use such shared
			// HttpService contexts (specific to Pax Web), but with more effort.
			for (OsgiContextModel model : getBundleApplication(bundle).getWebContainerOsgiContextModels()) {
				if ((!model.isShared() || model.isWab())
						&& selector.matchCase(model.getContextRegistrationProperties())) {
					targetContexts.add(model);
				}
			}

			// "140.2 The Servlet Context"
			//
			//     If multiple Servlet Context Helper services are registered with the same name, the one with the highest
			//     Service Ranking is used
			//
			// so we can end with several OsgiContextModels with the same name - we need only one of each - with
			// highest rank. Whiteboard and HttpService models are treated equally here.
			Map<String, OsgiContextModel> uniqueContexts = new HashMap<>();
			for (OsgiContextModel c : targetContexts) {
				if (!uniqueContexts.containsKey(c.getName()) || uniqueContexts.get(c.getName()).compareTo(c) > 0) {
					uniqueContexts.put(c.getName(), c);
				}
			}

			targetContexts.clear();
			targetContexts.addAll(uniqueContexts.values());

			return targetContexts;
		} finally {
			lock.unlock();
		}
	}

	// --- Handling registration/unregistration of target WebContainer, where we want to register Whiteboard services

	public void webContainerAdded(ServiceReference<WebContainer> ref) {
		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, ref);
		if (view != null) {
			// install global, default OSGi Context Model using bundle context of pax-web-extender-whiteboard bundle
			view.addWhiteboardOsgiContextModel(OsgiContextModel.DEFAULT_CONTEXT_MODEL);
			// register a listener, so when WABs are installed/uninstalled, their OsgiContextModels are used as
			// the context with highest priority - hiding both the context managed by pax-web-runtime and the contexts
			// registered by pax-web-extender-whiteboard
			view.registerWabOsgiContextListener(this);
		}

		currentWebContainerReference = ref;

		// install using new reference which will be dereferenced using a bundle for particular application
		installWhiteboardApplications(ref);

		acceptWabContexts.set(true);
	}

	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		acceptWabContexts.set(false);

		currentWebContainerReference = null;

		// uninstall all managed whiteboard applications from the WebContainer using the reference being removed
		uninstallWhiteboardApplications(ref);

		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, ref);
		if (view != null) {
			// uninstall global, default OSGi Context Model
			view.removeWhiteboardOsgiContextModel(OsgiContextModel.DEFAULT_CONTEXT_MODEL);
		}
		// finally now we can actually release the service
		webContainerManager.releaseContainer(bundle, ref);
	}

	/**
	 * This method iterates over all the {@link BundleWhiteboardApplication} and registers all the Whiteboard contexts
	 * and elements (tracked so far) in newly registered {@link WebContainer}.
	 * @param ref
	 */
	private void installWhiteboardApplications(ServiceReference<WebContainer> ref) {
		lock.lock();
		try {
			// This is were current WebContainer service reference is passed to all known BundleWhiteboardApplications
			// which may (or may not) have collected (tracked) already some web elements/contexts.
			// The important (but not difficult) responsibility of BundleWhiteboardApplications is just to remember
			// whether the web element/context is already registered - that's much easier than in case
			// of pax-web-extender-war
			bundleApplications.values().forEach(ba -> ba.webContainerAdded(ref));
		} finally {
			lock.unlock();
		}
	}

	/**
	 * This method iterates over all the {@link BundleWhiteboardApplication} and unregisters all the contexts
	 * and elements from current {@link WebContainer}, which is about to removed.
	 * @param ref
	 */
	private void uninstallWhiteboardApplications(ServiceReference<WebContainer> ref) {
		lock.lock();
		try {
			bundleApplications.values().forEach(ba -> ba.webContainerRemoved(ref));
		} finally {
			lock.unlock();
		}
	}

	// --- Handling registration/unregistration of Whiteboard web elements and contexts
	//     Those methods perform 3 actions:
	//     1) mark incoming customized web element or context as associated with given "bundle whiteboard application"
	//     2) handle Whiteboard DTO registry
	//     3) register given customized object in actual WebContainer from pax-web-runtime (if available)

	public void addWebContext(Bundle bundle, OsgiContextModel model) {
		lock.lock();
		try {
			osgiContexts.computeIfAbsent(model.getName(), cp -> new TreeSet<>()).add(model);
			osgiContextsList.add(model);

			getBundleApplication(bundle).addWebContext(model);

			reRegisterWebElements();
		} finally {
			lock.unlock();
		}
	}

	public void removeWebContext(Bundle bundle, OsgiContextModel model) {
		lock.lock();
		try {
			osgiContexts.get(model.getName()).remove(model);
			osgiContextsList.remove(model);

			reRegisterWebElements();

			getBundleApplication(bundle).removeWebContext(model);

			WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, currentWebContainerReference);
			if (view != null) {
				view.clearFailedDTOInformation(model);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * <p>This method is called every time a {@link OsgiContextModel} is added/changed/removed, because it may lead
	 * to different conditions of registration for existing {@link ElementModel}s.</p>
	 *
	 * <p>This is the actual place where we implement a scenario:<ul>
	 *     <li>{@link javax.servlet.Servlet} is registered with some selector matching for example "default" context
	 *         for "/" contextPath</li>
	 *     <li>We change properties of existing "default" context or register different context with "default" name,
	 *         higher ranking and different context (e.g., "/x")</li>
	 *     <li>Existing {@link javax.servlet.Servlet} should be re-registered from "/" to "/x" path without bothering
	 *         user who's registered the {@link javax.servlet.Servlet}.</li>
	 * </ul></p>
	 */
	private void reRegisterWebElements() {
		// remember - we're operating within ExtenderContext.lock

		List<BundleWhiteboardApplication> apps = new ArrayList<>(bundleApplications.values());
		for (BundleWhiteboardApplication app : apps) {
			WhiteboardWebContainerView view = app.getWhiteboardContainer();
			for (ElementModel<?, ?> webElement : app.getWebElements()) {
				Filter filter = webElement.getContextFilter();
				List<OsgiContextModel> newMatching = resolveContexts(webElement.getRegisteringBundle(), filter);
				List<OsgiContextModel> oldMatching = webElement.getContextModels();

				// 0.
				if (newMatching.size() == oldMatching.size() && newMatching.containsAll(oldMatching)) {
					continue;
				}

				// 1. unregistration because of no matching contexts
				if (newMatching.size() == 0) {
					LOG.debug("Unregistering {} because its context selection filter doesn't match any context", webElement);
					if (view != null) {
						// first unregister
						webElement.unregister(view);
					}
					// then change
					webElement.changeContextModels(newMatching);
					webElement.setDtoFailureCode(DTOConstants.FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING);
					continue;
				}

				// 2. easy registration after some models matched
				if (oldMatching.size() == 0) {
					// first change
					webElement.changeContextModels(newMatching);
					LOG.debug("Registering {} because its context selection filter started matching existing contexts", webElement);
					if (view != null) {
						// then register
						webElement.setDtoFailureCode(-1);
						webElement.register(view);
					}
					continue;
				}

				// 3. generic case - unregistration from removed models, registration to new models

				// now the tricky part - initially I wanted to optimize - remove the model only from "removed"
				// contexts and add it only to "added" ones. First difficulty (actually easy to workaround) occurred
				// when I saw ServletModel disappearing from the ServerModel, but the more important problem which
				// turned out to be solution was: when additional context is added that matches a selector of
				// existing ServletModel, then in simple scenario indeed - existing servlet should be registered
				// in new context. But if there's different ServletModel, with conflicting name or URL patterns
				// which is now disabled/waiting because its selector only matches the new context, the first servlet
				// should eventually be disabled in ALL contexts, because it'll be disabled in the new context!
				//
				// so it's really easier - FULLY unregister the element from all current contexts and then
				// register to all the new contexts
				if (view != null) {
					LOG.debug("Unregistering {} because its context selection filter matched new set of contexts", webElement);
					webElement.unregister(view);
				}
				webElement.changeContextModels(newMatching);
				if (view != null) {
					LOG.debug("Registering {} again after its context selection filter matched new set of contexts", webElement);
					webElement.register(view);
				}
			}
		}
	}

	public <R, D extends WebElementEventData, T extends ElementModel<R, D>> void addWebElement(Bundle bundle, T webElement) {
		lock.lock();
		try {
			getBundleApplication(bundle).addWebElement(webElement);
		} finally {
			lock.unlock();
		}
	}

	public <R, D extends WebElementEventData, T extends ElementModel<R, D>> void removeWebElement(Bundle bundle, T webElement) {
		lock.lock();
		try {
			getBundleApplication(bundle).removeWebElement(webElement);

			WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, currentWebContainerReference);
			if (view != null) {
				view.clearFailedDTOInformation(webElement);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Return (create if needed) a new bundle-scoped {@link BundleWhiteboardApplication}.
	 *
	 * @param bundle
	 * @return
	 */
	private BundleWhiteboardApplication getBundleApplication(final Bundle bundle) {
		if (bundle == null) {
			// PAXWEB-500 - it might happen that the bundle is already gone!
			return null;
		}

		BundleWhiteboardApplication bundleApplication;
		lock.lock();
		try {
			bundleApplication = bundleApplications.get(bundle);
			if (bundleApplication == null) {
				bundleApplication = new BundleWhiteboardApplication(bundle, webContainerManager);
				ServiceReference<WebContainer> ref = webContainerManager.currentWebContainerReference();
				if (ref != null) {
					bundleApplication.webContainerAdded(ref);
				}
				bundleApplications.put(bundle, bundleApplication);
			}
		} finally {
			lock.unlock();
		}

		return bundleApplication;
	}

	/**
	 * This method is invoked after checking that element model {@link ElementModel#isValid() is not valid}.
	 * This is the way to pass failed DTO information, because successful DTO information is passed automatically
	 * @param webElement
	 */
	public void configureFailedDTOs(ElementModel<?, ?> webElement) {
		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, currentWebContainerReference);
		if (view != null) {
			view.failedDTOInformation(webElement);
		}
	}

	/**
	 * This method is invoked after checking that context model {@link OsgiContextModel#isValid() is not valid}.
	 * This is the way to pass failed DTO information, because successful DTO information is passed automatically
	 * when web elements using the {@link OsgiContextModel} are registered to the runtime.
	 * @param webContext
	 */
	public void configureFailedDTOs(OsgiContextModel webContext) {
		WhiteboardWebContainerView view = webContainerManager.whiteboardView(bundle, currentWebContainerReference);
		if (view != null) {
			view.failedDTOInformation(webContext);
		}
	}

}
