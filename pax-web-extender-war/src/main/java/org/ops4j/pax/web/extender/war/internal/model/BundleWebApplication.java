/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;

import org.apache.felix.utils.extender.Extension;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.MultipartDef;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.SessionConfig;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.ops4j.pax.web.extender.war.internal.WarExtenderContext;
import org.ops4j.pax.web.extender.war.internal.WebApplicationHelper;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.views.WebAppWebContainerView;
import org.ops4j.pax.web.service.spi.servlet.DefaultSessionCookieConfig;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Similarly to {@code BundleWhiteboardApplication} from pax-web-extender-whiteboard, this class collects
 * the web elements being part of a WAB (Web Application Bundle, according to OSGi CMPN chapter 128). In Whiteboard
 * case, the web elements come from OSGi services registered through related {@link org.osgi.framework.BundleContext}
 * and here they come from the {@code web.xml} of such bundle.</p>
 *
 * <p>The web elements themselves, which come from {@code web.xml} (or fragments) are held in separate wrapper object
 * and this class is modelled a bit after {@code BlueprintContainerImpl} from Aries, where the "container" itself
 * is being scheduled over and over again to progress through different lifecycle stages - including stages where
 * the container may wait for the dependencies. Here, there are not many dependencies, but the most important one
 * is the availability of {@link org.ops4j.pax.web.service.WebContainer} service refrence.</p>
 *
 * <p>Before Pax Web 8, the synchronization of states to the availability of
 * {@link org.ops4j.pax.web.service.WebContainer} service was quite confusing, because it involved registration
 * and listening to different <em>intermediary</em> OSGi services like {@code WebAppDependencyHolder}.</p>
 *
 * @author Alin Dreghiciu
 * @author Grzegorz Grzybek
 * @since 0.3.0, December 27, 2007
 */
public class BundleWebApplication {

	public static final Logger LOG = LoggerFactory.getLogger(BundleWebApplication.class);

	/**
	 * The original WAB (according to 128.3 Web Application Bundle) which is the base of this {@link BundleWebApplication}.
	 * Additional bundles (like fragments) may <em>contribute</em> to this web application, but original bundle needs
	 * to be specified.
	 */
	private final Bundle bundle;

	/**
	 * A reference to {@link WarExtenderContext} to access supporting services (like event dispatcher or web.xml
	 * parser).
	 */
	private final WarExtenderContext extenderContext;

	private final WebContainerManager webContainerManager;

	/**
	 * Current {@link ServiceReference} to use when obtaining a {@link WebContainer} from
	 * {@link WebContainerManager}. {@link WebContainerManager} ensures that this reference is consistent - never
	 * set when there's already a reference set without unsetting it first.
	 */
	private volatile ServiceReference<WebContainer> webContainerServiceRef;

	private final Lock refLock = new ReentrantLock();

	/**
	 * The {@link ExecutorService} where this bundle web application is scheduled to progresss through its lifecycle
	 */
	private final ExecutorService pool;

	/** The current state of the web application */
	private final AtomicReference<State> deploymentState = new AtomicReference<>(State.UNCONFIGURED);

	/** Latch to be setup during deployment, so when stop() is called before WAB is DEPLOYED, we can wait */
	private CountDownLatch deployingLatch = null;

	/** Latch to be setup during context allocation, so when stop() is called before WAB is DEPLOYING, we can wait */
	private CountDownLatch allocatingLatch = null;

	/**
	 * The {@link ServletContext#getContextPath() context path} of this web application - can't be taken from
	 * {@code web.xml}, it should be configured <em>externally</em>.
	 */
	private String contextPath;

	// entire "raw" state of the web application - it is later turned into a model of elements from
	// org.ops4j.pax.web.service.spi.model package and later passed to a view of Pax Web's WebWebContainer

	/** Merged {@code web.xml} model from WAB's descriptors and from all reachable "web fragments". */
	private WebXml mainWebXml = null;
	/** A "class space" used to collect information from all reachable "web fragments" */
	private BundleWebApplicationClassSpace classSpace = null;
	/** An {@link OsgiServletContextClassLoader} which will be used when this WAB is deployed to a container. */
	private final OsgiServletContextClassLoader classLoader;
	/**
	 * This is the discovered mapping of SCIs to sets of classes that are related to types from
	 * {@link HandlesTypes} using these relations(see "8.2.4 Shared libraries / runtimes pluggability" of the Servlet
	 * spec):<ul>
	 *     <li>are implementing them</li>
	 *     <li>are extending them</li>
	 *     <li>are annotated with them (class, method and field level) - note that Tomcat only scans types annotated
	 *     at class level (see https://bz.apache.org/bugzilla/show_bug.cgi?id=65244)</li>
	 * </ul>
	 */
	private final Map<ServletContainerInitializer, Set<Class<?>>> sciToHt = new LinkedHashMap<>();

	/** Final batch of the changes/configuration operations related to full web application being deployed */
	private Batch batch = null;

	/** Stored instance of {@link OsgiContextModel} to get access (at undeployment time) to dynamic registrations */
	private OsgiContextModel osgiContextModel = null;

	public BundleWebApplication(Bundle bundle, WebContainerManager webContainerManager,
			WarExtenderContext extenderContext, ExecutorService pool) {
		this.bundle = bundle;
		this.webContainerManager = webContainerManager;
		this.extenderContext = extenderContext;
		this.pool = pool;

		OsgiServletContextClassLoader loader = new OsgiServletContextClassLoader();
		loader.addBundle(bundle);
		// pax-web-tomcat-common used to parse the descriptors
		loader.addBundle(FrameworkUtil.getBundle(WebXmlParser.class));
		this.classLoader = loader;
	}

	@Override
	public String toString() {
		return "Web Application \"" + contextPath
				+ "\" for bundle " + bundle.getSymbolicName() + "/" + bundle.getVersion();
	}

	/**
	 * <p>A {@link BundleWebApplication} can be started only once. Even if "Figure 128.2 State diagram Web Application"
	 * shows that WAB 	 * can go from UNDEPLOYED to DEPLOYING state, we're using
	 * {@link org.apache.felix.utils.extender.AbstractExtender#destroyExtension}, so what will be started after
	 * undeployment is a new instance of {@link BundleWebApplication}. Again it's important to distinguish
	 * {@link BundleWebApplication.State} and {@link WebApplicationEvent.State}.</p>
	 *
	 * <p>This method should be called only from {@link Extension#start()} and within the scope of a thread
	 * from pax-web-extender-war thread pool.</p>
	 */
	public void start() {
		State state = deploymentState.get();
		if (state != State.UNCONFIGURED) {
			throw new IllegalStateException("Can't start " + this + ": it's already in " + state + " state");
		}

		scheduleIfPossible(null, State.CONFIGURING, true);
	}

	/**
	 * <p>A {@link BundleWebApplication} can also be stopped only once. After the WAB (a web-enabled {@link Bundle})
	 * is stopped, it's associated {@link BundleWebApplication} is removed from the extender and when the WAB is
	 * started again, new instance is created</p>
	 *
	 * <p>Before Pax Web 8, if WAB was stopped immediately after it has been started, it had to wait for full
	 * deployment. Now we're able to stop it for example after web.xml has been parsed, but before the web elements
	 * were actually registered. The moment of stopping affects the amount of resources to cleanup.</p>
	 */
	public void stop() {
		// while start() should be called only in UNCONFIGURED state, we should be ready to stop() the WAB
		// in any state - even during web.xml parsing process.
		// also this method is called from org.apache.felix.utils.extender.Extension.destroy() method, which doesn't
		// use pax-web-extender-war pool. And most probably we're in bundle-stopping thread. This is in accordance
		// with "128.3.8 Stopping the Web Application Bundle": "This undeploying must occur synchronously with the
		// WAB's stopping event" and we should not schedule UNDEPLOYING of this WAB, but instead perform everything
		// in current thread.
		// Otherwise, the undeployment might've been happening after Felix/Equinox already cleaned up everything
		// related to WAB's bundle

		State state = deploymentState.getAndSet(State.UNDEPLOYING);
		extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYING, bundle));

		// get a WebContainer for the last time - it doesn't have to be available (no need to lock here)
		ServiceReference<WebContainer> ref = webContainerServiceRef;
		WebAppWebContainerView view = webContainerManager.containerView(bundle.getBundleContext(),
				ref, WebAppWebContainerView.class);

		// depending on current state, we may have to clean up more resources or just finish quickly
		switch (state) {
			case UNCONFIGURED:
			case CONFIGURING:
			case UNDEPLOYED:
			case UNDEPLOYING:
			case WAITING_FOR_WEB_CONTAINER:
				// web container is not available, but there's nothing to clean up
			case WAITING_FOR_CONTEXT:
				// it'll never be considered again, as the extension is already removed from
				// org.ops4j.pax.web.extender.war.internal.WarExtenderContext.webApplications
			case FAILED:
				LOG.debug("Stopping {} in {} state. No need to cleanup anything.", this, state);
				break;
			case ALLOCATING_CONTEXT:
				// the WAB is in the process of allocating the context and we have to wait till it finishes
				try {
					if (!allocatingLatch.await(10, TimeUnit.SECONDS)) {
						LOG.warn("Timeout waiting for end of context allocation for {}."
								+ " Can't free the context, leaving it in inconsistent state.", this);
					} else {
						if (view != null) {
							LOG.info("Undeploying {} after its context has been allocated", this);
							releaseContext(view, false);
						} else {
							LOG.warn("Successful wait for context allocation for {}, but WebContainer is no longer "
									+ "available and we can't release the context.", this);
						}
					}
				} catch (InterruptedException e) {
					LOG.warn("Thread interrupted while waiting for end of context allocation for {}."
							+ " Can't free the context, leaving it in inconsistent state.", this);
					Thread.currentThread().interrupt();
				}
				break;
			case DEPLOYING:
				// it's not deployed yet, but it'd not be wise to interrup() - let's just wait for DEPLOYED. That's
				// similar to pre Pax Web 8, where entire SimpleExtension#start() and SimpleExtension#destroy()
				// were synchronized on an extension object itself.
				try {
					if (!deployingLatch.await(10, TimeUnit.SECONDS)) {
						LOG.warn("Timeout waiting for end of deployment of {}."
								+ " Can't undeploy the application which may be left in inconsistent state.", this);
					} else {
						if (view != null) {
							LOG.info("Undeploying {} after waiting for its full deployment", this);
							undeploy(view);
						} else {
							LOG.warn("Successful wait for full deployment of {}, but WebContainer is no longer "
									+ "available and we can't undeploy it", this);
						}
					}
				} catch (InterruptedException e) {
					LOG.warn("Thread interrupted while waiting for end of deployment of {}."
							+ " Can't undeploy the application which may be left in inconsistent state.", this);
					Thread.currentThread().interrupt();
				}
				break;
			case DEPLOYED:
				// this is the most typical case - application was fully deployed, not it has to be fully undeployed.
				// the is the same case as implemented before Pax Web 8, where everything was fully synchronized -
				// simply even after web.xml has just started to be parsed, stop() had to wait for full deployment.
				if (view != null) {
					LOG.info("Undeploying fully deployed {}", this);
					undeploy(view);
				} else {
					LOG.info("Can't undeploy {} - WebContainer reference is no longer available", this);
				}
				break;
			default:
				break;
		}

		// whether the undeployment failed, succeeded or wasn't needed, we broadcast an event and set final stage.
		deploymentState.set(State.UNDEPLOYED);
		extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYED, bundle));

		webContainerManager.releaseContainer(bundle.getBundleContext(), ref);
	}

	/**
	 * Private {@code schedule()} method modelled after Aries {@code BlueprintContainerImpl#schedule()}. It's
	 * important to be aware that the {@link ExecutorService} used may NOT be a single-thread pool. So we can't
	 * guarantee (same as with Aries Blueprint) that rescheduled invocation will be happening in the same thread (and
	 * as a consequence, sequentially after current run cycle).
	 *
	 * @param expectedState can be specified to prevent scheduling, if current state is different
	 * @param newState
	 * @param synchronous whether to schedule synchronously (run {@link #deploy()} directly) or not (pass to
	 *        a thread pool)
	 */
	private boolean scheduleIfPossible(State expectedState, State newState, boolean synchronous) {
		if (expectedState != null) {
			if (deploymentState.compareAndSet(expectedState, newState)) {
				if (!synchronous) {
					pool.submit(this::deploy);
				} else {
					deploy();
				}
				return true;
			}
			return false;
		} else {
			deploymentState.set(newState);
			if (!synchronous) {
				pool.submit(this::deploy);
			} else {
				deploy();
			}
			return true;
		}
	}

	public void webContainerAdded(ServiceReference<WebContainer> ref) {
		// There's whiteboard-equivalent method BundleWhiteboardApplication.webContainerAdded(), but it's easier
		// to implement, as BundleWhiteboardApplication doesn't have state-based lifecycle. Here we can't simply
		// register the web elements from parsed web.xml (and fragmnets + annotated web elements of the WAB), because
		// parsing may have not yet been finished
		//
		// the important thing to remember is that the WebContainer ref may be passed in two possible threads:
		// 1) when the BundleWebApplication is created, before WabExtension is returned to felix-extender, in
		//    a thread that calls org.osgi.util.tracker.BundleTrackerCustomizer.addingBundle(). This may be
		//    FelixStartLevel thread or e.g., Karaf Shell Console thread.
		//    In this case, the BundleWebApplication is definitely NOT scheduled yet
		// 2) a thread from single-thread pool managed in org.ops4j.pax.web.service.spi.util.WebContainerManager. And
		//    this is the case were we have to coordinate how WebContainer ref is set and how this BundleWebApplication
		//    already progressed through its lifecycle

		refLock.lock();
		try {
			webContainerServiceRef = ref;

			// No need to schedule if WAB is not waiting for the container.
			// Also, even if the WAB was already in state AFTER "allocating context" and prepared to register WAB's web
			// elements, after new WebContainer reference is set, we have to get back to ALLOCATING_CONTEXT, as the new
			// reference may be for a WebContainer with completely different setup
			scheduleIfPossible(State.WAITING_FOR_WEB_CONTAINER, State.ALLOCATING_CONTEXT, false);
		} finally {
			refLock.unlock();
		}
	}

	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		if (ref != webContainerServiceRef) {
			throw new IllegalStateException("Removing unknown WebContainer reference " + ref
					+ ", expecting " + webContainerServiceRef);
		}

		// previously set WebContainer ref was removed. But we may be in the pending process of WAB registration,
		// web.xml parsing or waiting for the reference.
		// while stop() is definitely an end of this BundleWebApplication (because stopped WAB may start with
		// new bundle fragments attached, with potentially new web fragments), disappearance of WebContainer reference
		// for DEPLOYED web application should bring it back to ALLOCATION_CONTEXT state, so we preserve the parsed
		// state of web elements

		refLock.lock();
		try {
			WebAppWebContainerView view = webContainerManager.containerView(bundle.getBundleContext(),
					webContainerServiceRef, WebAppWebContainerView.class);

			if (view == null) {
				LOG.warn("WebContainer reference {} was removed, but {} can't access the WebContainer service"
						+ " and it can't be undeployed.", ref, this);
				// but still let's start with new allocation attempt (in new WebContainer that'll be set in future)
				deploymentState.set(State.ALLOCATING_CONTEXT);
			} else {
				// as in stop(), we should UNDEPLOY the application, but (unlike as in stop()) to the stage, where
				// it could potentially be DEPLOYED again. re-registration of WebContainer service won't change the
				// information content of current WAB (when WAB is restarted/refreshed, it may get new bundle
				// fragments attached, thus new web fragments may become available), so it's safe to reuse already
				// parsed web.xml + fragments + annotations

				State state = deploymentState.getAndSet(State.UNDEPLOYING);
				extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYING, bundle));

				// similar state check as with stop(), but with slightly different handling
				switch (state) {
					case UNCONFIGURED:
					case CONFIGURING:
					case UNDEPLOYED:
					case UNDEPLOYING:
					case WAITING_FOR_WEB_CONTAINER:
					case WAITING_FOR_CONTEXT:
						break;
					case FAILED:
						// FAILED state of WAB is different that FAILED event (which may mean the context is not free),
						// but with new WebContainer reference that may soon be set, we'll give this WAB another chance
						break;
					case ALLOCATING_CONTEXT:
						try {
							if (!allocatingLatch.await(10, TimeUnit.SECONDS)) {
								LOG.warn("Timeout waiting for end of context allocation for {}."
										+ " Can't free the context, leaving it in inconsistent state.", this);
							} else {
								LOG.info("Undeploying {} after its context has been allocated", this);
								releaseContext(view, false);
							}
						} catch (InterruptedException e) {
							LOG.warn("Thread interrupted while waiting for end of context allocation for {}."
									+ " Can't free the context, leaving it in inconsistent state.", this);
							Thread.currentThread().interrupt();
						}
						break;
					case DEPLOYING:
						try {
							if (!deployingLatch.await(10, TimeUnit.SECONDS)) {
								LOG.warn("Timeout waiting for end of deployment of {}."
										+ " Can't undeploy the application which may be left in inconsistent state"
										+ " (in previous WebContainer).", this);
							} else {
								LOG.info("Undeploying {} from previous WebContainer after waiting for its full"
										+ " deployment", this);
								undeploy(view);
							}
						} catch (InterruptedException e) {
							LOG.warn("Thread interrupted while waiting for end of deployment of {}."
									+ " Can't undeploy the application which may be left in inconsistent state.", this);
							Thread.currentThread().interrupt();
						}
						break;
					case DEPLOYED:
						LOG.info("Undeploying fully deployed {}", this);
						undeploy(view);
						break;
					default:
						break;
				}

				// with the removal of WebContainer reference, we broadcast UNDEPLOYED event, but the state is as
				// if the WAB was waiting for the context to be allocated. WAITING_FOR_WEB_CONTAINER state
				// means that if the WebContainer is available again, the WAB will be scheduled to ALLOCATING_CONTEXT
				// state
				deploymentState.set(State.WAITING_FOR_WEB_CONTAINER);
				extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYED, bundle));
			}

			webContainerManager.releaseContainer(bundle.getBundleContext(), ref);
			webContainerServiceRef = null;
		} finally {
			refLock.unlock();
		}
	}

	/**
	 * Gets a {@link WebContainer} and if it's not available, this {@link BundleWebApplication} automatically
	 * enters {@link State#WAITING_FOR_WEB_CONTAINER} state.
	 * @return
	 * @param currentState a guarding state, so if it is changed externally (to for example {@link State#UNDEPLOYING}),
	 *        container won't enter {@link State#WAITING_FOR_WEB_CONTAINER} state.
	 */
	private WebAppWebContainerView currentWebContainer(State currentState) {
		refLock.lock();
		try {
			WebAppWebContainerView view = webContainerManager.containerView(bundle.getBundleContext(),
					webContainerServiceRef, WebAppWebContainerView.class);
			if (view == null) {
				if (deploymentState.compareAndSet(currentState, State.WAITING_FOR_WEB_CONTAINER)) {
					LOG.debug("WebContainer service reference is not available. {} enters Grace Period state.", this);
					// note: there may be duplicate WAITING events if web container is not available before
					// context path reservation and before actual registration of webapp's web elements.
					extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.WAITING, bundle));
				}
			}
			return view;
		} finally {
			refLock.unlock();
		}
	}

	/**
	 * <p>Method invoked within the thread of pax-web-extender-war thread pool. It's final goal is to progress to
	 * {@code DEPLOYED}</em> state, but there may be some internal states in between.</p>
	 *
	 * <p>This method is quite similar (in terms of its structure), but very simplified comparing to
	 * {@code org.apache.aries.blueprint.container.BlueprintContainerImpl#doRun()}, especially in relation to "waiting
	 * for service". In Blueprint this may involve waiting for multiple services and with additional constraints
	 * (mandatory, optional), while here, it's only single, mandatory {@link org.ops4j.pax.web.service.WebContainer}
	 * service.</p>
	 */
	private void deploy() {
		try {
			// progress through states in a loop - when everything is available, we can simply transition to
			// final state in single thread/task run. If we need to wait for anything, we'll break the loop
			if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
				return;
			}

			// This state machine should match OSGi CMPN 128 specification:
			//  - 128.3.2 Starting the Web Application Bundle
			//     1. "Wait for the WAB to become ready" - ensured by
			//        org.apache.felix.utils.extender.AbstractExtender.modifiedBundle. The specification fragment:
			//        "The following steps can take place asynchronously with the starting of the WAB." means that
			//        we can (and in fact we always do) call deploy() in a thread from pax-web-extender-war pool,
			//        instead of a thread that started the bundle (most probably FelixStartLevel or Karaf's
			//        feature installation thread)
			//     2. Post an org/osgi/service/web/DEPLOYING event
			//     3. Validate that the Web-ContextPath manifest header does not match the Context Path of any
			//        other currently deployed web application.
			//     4. The Web Runtime processes deployment information by processing the web.xml descriptor,
			//        if present. In Pax Web we're also checking bundle fragments, web fragments and annotated
			//        classes within (or reachable from) the WAB.
			//     5. Publish the Servlet Context as a service with identifying service properties - ensured by
			//        org.ops4j.pax.web.service.spi.task.BatchVisitor.visit(OsgiContextModelChange) and
			//        org.ops4j.pax.web.service.spi.servlet.OsgiServletContext.register()
			//     6. Post an org/osgi/service/web/DEPLOYED event
			//
			//  - 128.3.8 Stopping the Web Application Bundle
			//     1. A web application is stopped by stopping the corresponding WAB - handled by
			//        org.apache.felix.utils.extender.AbstractExtender.destroyExtension(). The specification fragment:
			//        "This undeploying must occur synchronously with the WAB's stopping event" means that we can't
			//        perform the undeployment in a thread from pax-web-extender-war thread pool and instead use the
			//        thred that stops the bundle. I'm not sure why this is the case, but we have to stick to
			//        specification.
			//     2. An org/osgi/service/web/UNDEPLOYING event is posted
			//     3. Unregister the corresponding Servlet Context service - implemented in
			//        org.ops4j.pax.web.service.spi.servlet.OsgiServletContext.unregister
			//     4. The Web Runtime must stop serving content from the Web Application - sure it must!
			//     5. The Web Runtime must clean up any Web Application specific resources - yes
			//     6. Emit an org/osgi/service/web/UNDEPLOYED event
			//     7. It is possible that there are one or more colliding WABs because they had the same Context
			//        Path as this stopped WAB. If such colliding WABs exists then the Web Extender must attempt to
			//        deploy the colliding WAB with the lowest bundle id.
			//
			// Although the validation of Web-ContextPath is specified to be done before parsing of web.xml, we
			// actually want to parse web.xml earlier. First - we'll log parsing errors as soon as possible and
			// move the WAB into "failed permanently" state (so it's never attempted to be deployed again) and
			// second - even if there's a conflict with existing context path, we'll be ready (parsed/configured)
			// at the time when we can try the deployment again (after the conflicting context is gone)
			//
			// Each if{} branch should only change the deploymentState using compareAndSet(), because in parallel
			// there may be an external change to BundleWebApplication state. And while we can imagine a stop() call
			// during the process of parsing web.xml, we're not implementing (for now: 2021-01-08) thread interruption
			// here, so each if{} is kind of "atomic". But as a consequence, we have to get() the state again
			// after each if{}.

			State state = deploymentState.get();
			if (state == State.CONFIGURING) {
				LOG.info("Configuring {}", this);
				// Post an org/osgi/service/web/DEPLOYING event
				extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.DEPLOYING, bundle));

				// Collect deployment information by processing the web.xml descriptor and other sources of metadata
				processMetadata();

				// We have: 1) mainWebXml (altered by fragments), 2) SCIs, 3) ClassLoader, 4) ClassSpace (support)
				// so we can build set of ServletModels, FilterModels, ... that'll be sent to WebContainer
				// implementation as pre-built batch.
				// This is important advantage of pax-web-extender-war over pax-web-extender-whiteboard. Here
				// we have complete web application, while in Whiteboard, we build it element by element.
				// Here we can do it "transactionally" without bothering about conflicts etc.
				buildModel();

				// after web.xml/web-fragment.xml/annotations are read, we have to check if the context path
				// is available
				if (deploymentState.compareAndSet(State.CONFIGURING, State.ALLOCATING_CONTEXT)) {
					if (allocatingLatch != null && allocatingLatch.getCount() > 0) {
						// for now let's keep this brute check
						throw new IllegalStateException("[dev error] Previous context allocation attempt didn't finish"
								+ " properly. Existing latch found.");
					}
					allocatingLatch = new CountDownLatch(1);
				}
			}

			// even if after the above if{} was executed and the state is expected to be ALLOCATING_CONTEXT,
			// we may already be in different state - e.g., STOPPING
			// but most common scenario is that we continue the WAB deployment

			state = deploymentState.get();
			if (state == State.ALLOCATING_CONTEXT) {
				LOG.debug("Checking if {} context path is available", contextPath);

				// but need a WebContainer to allocate the context
				WebAppWebContainerView view = currentWebContainer(state);
				if (view == null) {
					return;
				}
				if (!view.allocateContext(bundle, contextPath)) {
					LOG.debug("Context path {} is already used. {} will wait for this context to be available.",
							contextPath, this);
					// 128.3.2 Starting the Web Application Bundle, point 3): If the Context Path value is already in
					// use by another Web Application, then the Web Application must not be deployed, and the
					// deployment fails. [...] If the prior Web Application with the same Context Path is undeployed
					// later, this Web Application should be considered as a candidate.
					if (deploymentState.compareAndSet(state, State.WAITING_FOR_CONTEXT)) {
						extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.FAILED, bundle));
					}
					return;
				}
				// from now on, this.contextPath is "ours" and we can do anything with it
				if (deploymentState.compareAndSet(state, State.DEPLOYING)) {
					// count down this latch, so it's free even before the deployment attempt
					allocatingLatch.countDown();

					if (deployingLatch != null && deployingLatch.getCount() > 0) {
						// for now let's keep this brute check
						throw new IllegalStateException("[dev error] Previous deployment attempt didn't finish properly."
								+ " Existing latch found.");
					}
					deployingLatch = new CountDownLatch(1);
				}
			}

			state = deploymentState.get();
			if (state == State.DEPLOYING) {
				LOG.debug("Registering {} in WebContainer", contextPath);

				// we have to get the view again, as we may have been rescheduled after waiting for WebContainer
				WebAppWebContainerView view = currentWebContainer(state);
				if (view == null) {
					return;
				}

				// this is were the full WAR/WAB information is passed as a model to WebContainer (through special view)
				view.sendBatch(batch);

				if (deploymentState.compareAndSet(state, State.DEPLOYED)) {
					extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.DEPLOYED, bundle));
				}
			}
		} catch (Throwable t) {
			deploymentState.set(State.FAILED);

			// we are not configuring a listener that sends the events to LogService (we have one for EventAdmin),
			// because we use SLF4J logger directly (backed by pax-logging)
			LOG.error("Problem processing {}: {}", this, t.getMessage(), t);

			extenderContext.webEvent(new WebApplicationEvent(WebApplicationEvent.State.FAILED, bundle, t));
		} finally {
			// context allocation or deployment may end up with missing WebContainer reference, an exception or
			// success. In all the cases we have to trigger corresponding latches, so potential parallel stop()
			// knows about it
			if (allocatingLatch != null) {
				allocatingLatch.countDown();
			}
			if (deployingLatch != null) {
				deployingLatch.countDown();
			}
		}
	}

	/**
	 * <p>Not schedulable equivalent of {@link #deploy()}. It fully undeploys the {@link BundleWebApplication} using
	 * passed {@link WebAppWebContainerView}.</p>
	 *
	 * <p>While {@link #deploy()} method (the "starting" phase of WAB's lifecycle) may be rescheduled few times when
	 * {@link WebContainer} is not available, here we just have to do everything in one shot.</p>
	 * @param view
	 */
	private void undeploy(WebAppWebContainerView view) {
		if (view == null) {
			throw new IllegalArgumentException("Can't undeploy " + this + " without valid WebContainer");
		}
		try {
			// 1. undeploy all the web elements from current WAB
			view.sendBatch(batch.uninstall("Undeployment of " + this));

			// 2. free the context
			releaseContext(view, true);
		} catch (Exception e) {
			// 128.3.8 Stopping the Web Application Bundle: Any failure during undeploying should be logged but must
			// not stop the cleaning up of resources and notification of (other) listeners as well as handling any
			// collisions.
			LOG.warn("Problem undeploying {}: {}", this, e.getMessage(), e);
		}
	}

	/**
	 * This wasn't present in Pax Web before version 8. Now we can stop/undeploy the application after it has
	 * allocated the context, but before it managed to register any web elements there.
	 * @param view
	 * @param propagateException
	 */
	private void releaseContext(WebAppWebContainerView view, boolean propagateException) {
		if (view == null) {
			throw new IllegalArgumentException("Can't undeploy " + this + " without valid WebContainer");
		}
		try {
			view.releaseContext(bundle, contextPath);
		} catch (Exception e) {
			if (propagateException) {
				throw new RuntimeException(e);
			} else {
				LOG.warn("Problem releasing context for {}: {}", this, e.getMessage(), e);
			}
		}
	}

	// --- Lifecycle processing methods

	/**
	 * <p>Parse all the possible descriptors to create final web application model. The rules specified in Servlet
	 * specification should be applied. We're following the order from
	 * {@code org.apache.catalina.startup.ContextConfig#webConfig()}. And we're even using Tomcat's {@code }web.xml}
	 * parser.</p>
	 *
	 * <p>After the descriptors/annotations are parsed, the model stays in current {@link BundleWebApplication}
	 * whether the {@link WebContainer} is available or not. However if the bundle itself is stopped and started
	 * again (which may happen during refresh), we have to parse everything again, because new model parts may
	 * become available in OSGi bundle fragments.</p>
	 */
	private void processMetadata() {
		// Servlet spec, 8.1 Annotations and pluggability:
		//  - "metadata-complete" attribute on web descriptor defines whether this deployment descriptor and any web
		//    fragments, if any, are complete, or whether the class files available to this module and packaged with
		//    this application should be examined for annotations that specify deployment information.
		//  - classes using annotations will have their annotations processed only if they are located in the
		//    WEB-INF/classes directory, or if they are packaged in a jar file located in WEB-INF/lib within the
		//    application - in OSGi, we're processing jars (and locations) from Bundle-ClassPath
		//  - Annotations that do not have equivalents in the deployment XSD include
		//    javax.servlet.annotation.HandlesTypes and all of the CDI-related annotations. These annotations must be
		//    processed during annotation scanning, regardless of the value of "metadata-complete".
		//  - there are annotations to be processed from different packages:
		//     - javax.servlet.annotation
		//     - javax.annotation
		//     - javax.annotation.security
		//     - javax.annotation.sql
		//     - javax.ejb
		//     - javax.jms
		//     - javax.mail
		//     - javax.persistence
		//     - javax.resource
		//     - javax.jws.*
		//     - javax.xml.ws.*
		//
		// Servlet spec, 8.2.1 Modularity of web.xml:
		//  - A web fragment is a part or all of the web.xml that can be specified and included in a library or
		//    framework jar's META-INF directory. A plain old jar file in the WEB-INF/lib directory with no
		//    web-fragment.xml is also considered a fragment. Any annotations specified in it will be processed [...]
		//  - fragment's top level element for the descriptor MUST be web-fragment and the corresponding descriptor
		//    file MUST be called web-fragment.xml
		//  - web-fragment.xml descriptor must be in the META-INF/ directory of the jar file.
		//  - In order for any other types of resources (e.g., class files) of the framework to be made available to a
		//    web application, it is sufficient for the framework to be present anywhere in the classloader delegation
		//    chain of the web application. In other words, only JAR files bundled in a web application's WEB-INF/lib
		//    directory, but not those higher up in the class loading delegation chain, need to be scanned for
		//    web-fragment.xml
		//
		// (for example, myfaces-impl-2.3.3.jar contains /META-INF/web-fragment.xml with
		//    <listener>
		//        <listener-class>org.apache.myfaces.webapp.StartupServletContextListener</listener-class>
		//    </listener>
		//
		// Servlet spec, 8.2.4 Shared libraries / runtimes pluggability:
		//  - The ServletContainerInitializer class is looked up via the jar services API. In JavaEE env, it's
		//    traversing up the ClassLoader hierarchy up to web container's top CL. But in OSGi there's no "top"
		//
		// There's something wrong with metadata-complete...:
		//  - Servlet spec 8.1: This attribute defines whether this deployment descriptor and any web fragments, if
		//    any, are complete, or whether the class files available to this module and packaged with this application
		//    should be examined for annotations that specify deployment information.
		//  - Servlet spec 15.5.1: If metadata-complete is set to " true ", the deployment tool only examines the
		//    web.xml file and must ignore annotations such as @WebServlet , @WebFilter , and @WebListener present in
		//    the class files of the application, and must also ignore any web-fragment.xml descriptors packaged in
		//    a jar file in WEB-INF/lib.
		// So I'm not sure whether to process web-fragment.xml or not...

		// OSGi CMPN 128.3.1 WAB Definition:
		//  - web.xml must be found with the Bundle findEntries method at the path WEB-INF/web.xml. The findEntries
		//    method includes [bundle] fragments
		//
		// OSGi CMPN 128.6.4 Resource Injection and Annotations:
		//  - The Web Application web.xml descriptor can specify the metadata-complete attribute on the web-app
		//    element. This attribute defines whether the web.xml descriptor is complete, or whether the classes in the
		//    bundle should be examined for deployment annotations. If the metadata-complete attribute is set to true,
		//    the Web Runtime must ignore any servlet annotations present in the class files of the Web Application.
		//    Otherwise, if the metadata-complete attribute is not specified, or is set to false, the container should
		//    process the class files of the Web Application for annotations, if supported.
		//    So nothing about META-INF/web-fragment.xml descriptors...
		//
		// OSGi CMPN 128 spec doesn't say anything at all about ServletContainerInitializers...

		// org.apache.catalina.startup.ContextConfig#webConfig() works like this:
		//  1. "default web.xml" is treated as fragment (so lower priority than app's "main" web.xml) and comes from:
		//   - "global"
		//     - org.apache.catalina.core.StandardContext.getDefaultWebXml()
		//     - org.apache.catalina.startup.ContextConfig.getDefaultWebXml()
		//     - in Tomcat (standalone) 9.0.41 it's file:/data/servers/apache-tomcat-9.0.41/conf/web.xml
		//       - overridable=true, distributable=true, alwaysAddWelcomeFiles=false, replaceWelcomeFiles=true
		//       - two servlets:
		//         - "default" -> org.apache.catalina.servlets.DefaultServlet
		//         - "jsp" -> org.apache.jasper.servlet.JspServlet
		//       - three mappings:
		//         - "*.jspx" -> "jsp"
		//         - "*.jsp" -> "jsp"
		//         - "/" -> "default"
		//       - three welcome files:
		//         - "index.html"
		//         - "index.htm"
		//         - "index.jsp"
		//   - "host"
		//     - org.apache.catalina.Host.getConfigBaseFile()
		//     - in Tomcat (standalone) 9.0.41 it's null
		//   - "default" and "host" web.xml are merged together
		//  2. "tomcat web.xml" - also as fragment
		//   - /WEB-INF/tomcat-web.xml from context (org.apache.catalina.WebResourceRoot.getResource())
		//     - overridable=true, distributable=true, alwaysAddWelcomeFiles=false, replaceWelcomeFiles=true
		//  3. "context web.xml"
		//   - /WEB-INF/web.xml from context (javax.servlet.ServletContext.getResourceAsStream())
		//  4. ContextConfig.processJarsForWebFragments() - fragments from org.apache.tomcat.JarScanner.scan()
		//   - META-INF/web-fragment.xml from each JAR in /WEB-INF/lib
		//   - in Tomcat (standalone) 9.0.41, /examples context has /WEB-INF/lib/taglibs-standard-impl-1.2.5.jar
		//     and /WEB-INF/lib/taglibs-standard-spec-1.2.5.jar, but the latter is skipped by default
		//     (org.apache.tomcat.util.scan.StandardJarScanFilter.defaultSkip)
		//   - As per http://java.net/jira/browse/SERVLET_SPEC-36, if the main web.xml is marked as metadata-complete,
		//     JARs are still processed for SCIs.
		//   - Tomcat checks all classloaders starting from javax.servlet.ServletContext.getClassLoader() up to
		//     the parent of java.lang.ClassLoader.getSystemClassLoader()
		//   - fragments are ordered using org.apache.tomcat.util.descriptor.web.WebXml.orderWebFragments()
		//  5. org.apache.catalina.startup.ContextConfig.processServletContainerInitializers()
		//   - /META-INF/services/javax.servlet.ServletContainerInitializer files are loaded from CL hierarchy
		//   - order may be consulted from "javax.servlet.context.orderedLibs" attribute (see Servlet spec,
		//     8.3 JSP container pluggability) - this order affects SCIs
		//   - these are found in Tomcat 9.0.41 hierarchy:
		//     - "jar:file:/data/servers/apache-tomcat-9.0.41/lib/tomcat-websocket.jar!/META-INF/services/javax.servlet.ServletContainerInitializer"
		//     - "jar:file:/data/servers/apache-tomcat-9.0.41/lib/jasper.jar!/META-INF/services/javax.servlet.ServletContainerInitializer"
		//   - these provide the following SCIs:
		//     - org.apache.tomcat.websocket.server.WsSci
		//       - @javax.servlet.annotation.HandlesTypes is:
		//         - interface javax.websocket.server.ServerEndpoint
		//         - interface javax.websocket.server.ServerApplicationConfig
		//         - class javax.websocket.Endpoint
		//     - org.apache.jasper.servlet.JasperInitializer
		//   - the HandlesTypes are not yet scanned for the classes to pass to SCIs
		//   - META-INF/services/javax.servlet.ServletContainerInitializer is not loaded from the WAR itself, only from
		//     its JARs - because java.lang.ClassLoader.getResources() is used both for parent classloaders and
		//     the WAR itself. When orderedLibs are present, direct JAR access is used for the non-excluded /WEB-INF/lib/*.jar
		//  6. if metadata-complete == false, classes from /WEB-INF/classes are checked (Tomcat uses BCEL)
		//   - but if it's true, classes still should be scanned for @HandlesTypes for SCIs from jars not excluded
		//     in absolute ordering (Tomcat uses "if (!webXml.isMetadataComplete() || typeInitializerMap.size() > 0)")
		//   - All *.class files are checked using org.apache.tomcat.util.bcel.classfile.ClassParser
		//  7. tomcat-web.xml is merged in
		//  8. default web.xml is merged in
		//  9. org.apache.catalina.startup.ContextConfig.convertJsps() - servlets with JSP files are converted
		// 10. org.apache.catalina.startup.ContextConfig.configureContext() - finally parsed web elements are applied
		//     to org.apache.catalina.Context
		//
		// In Jetty, web.xml parsing is performed by org.eclipse.jetty.webapp.StandardDescriptorProcessor and
		// org.eclipse.jetty.plus.webapp.PlusDescriptorProcessor
		// WebApp configuration happens during context start:
		// org.eclipse.jetty.server.handler.ContextHandler.doStart()
		//   org.eclipse.jetty.webapp.WebAppContext.startContext()
		//     - use org.eclipse.jetty.webapp.Configuration instances to configure the contexts (like adding metadata
		//       from different sources)
		//     org.eclipse.jetty.webapp.MetaData.resolve() - based on prepared metadata (sources)
		//     org.eclipse.jetty.servlet.ServletContextHandler.startContext()
		// Default Jetty's configuration classes are:
		//  - "org.eclipse.jetty.webapp.WebInfConfiguration"
		//     - it prepares resources/paths to be used later - from parent classloader (container paths) and
		//       /WEB-INF/lib (webinf paths)
		//  - "org.eclipse.jetty.webapp.WebXmlConfiguration"
		//     - default descriptor is taken from org/eclipse/jetty/webapp/webdefault.xml from jetty-webapp
		//       webdefault.xml is a bit more complex than Tomcat's conf/web.xml. It adds:
		//        - org.eclipse.jetty.servlet.listener.ELContextCleaner
		//        - org.eclipse.jetty.servlet.listener.IntrospectorCleaner
		//        - org.eclipse.jetty.servlet.DefaultServlet mapped to "/"
		//        - org.eclipse.jetty.jsp.JettyJspServlet (extends org.apache.jasper.servlet.JspServlet) mapped
		//          to *.jsp, *.jspf, *.jspx, *.xsp, *.JSP, *.JSPF, *.JSPX, *.XSP
		//        - 30 minutes session timeout
		//        - welcome files: index.html, index.htm, index.jsp
		//        - <locale-encoding-mapping-list>
		//        - <security-constraint> that disables TRACE verb
		//     - normal /WEB-INF/web.xml
		//  - "org.eclipse.jetty.webapp.MetaInfConfiguration" - scanning JARs using
		//    org.eclipse.jetty.webapp.MetaInfConfiguration.scanJars():
		//     - selected container jars
		//     - /WEB-INF/lib/*.jar
		//    this is were the ordering takes place. These resources are being searched for:
		//     - org.eclipse.jetty.webapp.MetaInfConfiguration.scanForResources() - META-INF/resources
		//     - org.eclipse.jetty.webapp.MetaInfConfiguration.scanForFragment() - META-INF/web-fragment.xml
		//     - org.eclipse.jetty.webapp.MetaInfConfiguration.scanForTlds() - META-INF/**/*.tld
		//  - "org.eclipse.jetty.webapp.FragmentConfiguration"
		//     - fragments scanned by MetaInfConfiguration are processed
		//     - MetaInfConfiguration doesn't call org.eclipse.jetty.webapp.MetaData.addFragment(), but only prepares
		//       "org.eclipse.jetty.webFragments" context attribute to be processed here
		//  - "org.eclipse.jetty.webapp.JettyWebXmlConfiguration
		//     - WEB-INF/jetty8-web.xml, WEB-INF/jetty-web.xml (seems to be the preferred one),
		//       WEB-INF/web-jetty.xml (in that order) are checked - first found is used
		//     - parsed using org.eclipse.jetty.xml.XmlConfiguration

		// additionally:
		//  - Tomcat handles context.xml files set by org.apache.catalina.core.StandardContext.setDefaultContextXml()
		//  - Jetty handles jetty[0-9]?-web.xml, jetty-web.xml or web-jetty.xml
		// These are container specific files that can alter (respectively):
		//  - org.apache.catalina.core.StandardContext
		//  - org.eclipse.jetty.webapp.WebAppContext

		long start = System.currentTimeMillis();

		ClassLoader tccl = Thread.currentThread().getContextClassLoader();
		try {
			// TCCL set only at the time of parsing - not at the time of deployment of the model to a WebContainer
			// because during deployment, all classes (filters, servlets, listeners, ...) should already be
			// instantiated.
			// When target (Jetty/Tomcat/Undertow) ServletContext is started, the TCCL will be set anyway, so
			// listeners and initializers are invoked correctly (e.g., with access to Tomcat scanner).
			Thread.currentThread().setContextClassLoader(this.classLoader);

			// something like Tomcat's CATALINA_HOME/conf/web.xml
			WebXml defaultWebXml = extenderContext.getDefaultWebXml();
			// actual WEB-INF/web.xml from a WAB

			LOG.debug("Searching for web.xml descriptor in {}", bundle);
			mainWebXml = extenderContext.findBundleWebXml(bundle);

			// at this stage, we don't have javax.servlet.ServletContext available yet. We don't even know
			// where this WAB is going to be deployed (Tomcat? Jetty? Undertow?). We don't even know whether
			// the contextPath for this WAB is available.

			// Let's start constructing WAB's "class path" which will eventually be transformed into complete
			// WAB's ClassLoader accessible through javax.servlet.ServletContext.getClassLoader() - but this will
			// happen later, after the WAB is really deployed
			classSpace = new BundleWebApplicationClassSpace(bundle, extenderContext);

			try {
				// complex initialization that actually processes entire "class space" and determines the
				// ordered fragments that will later be used to discover/load SCIs and annotated classes
				LOG.debug("Searching for web fragments");
				classSpace.initialize(mainWebXml, classLoader);
				boolean ok = classSpace.isFragmentParsingOK();

				if (!ok) {
					LOG.warn("There were problems when parsing web-fragment.xml descriptors."
							+ " Scanning for ServletContainerInitializers and annotated classes won't be performed.");
				}

				// if parsing was successful (if it was required, though it may be disabled using empty
				// <absolute-ordering>), proceed to scanning for ServletContainerInitializers
				// see org.apache.catalina.startup.ContextConfig.processServletContainerInitializers()
				if (ok) {
					LOG.debug("Searching for ServletContainerInitializers (SCIs)");
				}
				final List<ServletContainerInitializer> detectedSCIs = ok ? classSpace.loadSCIs()
						: Collections.emptyList();

				// all SCIs are loaded using proper ClassLoaders.
				// For each SCI we need their @HandlesTypes, so we can detect them later from selected fragments.
				// Elements of @HandlesTypes may be classes, interfaces or annotations, so when actual scanning
				// is performed, each scanned class is either compared with @HandlesTypes or checked for
				// @HandlesTypes annotation

				// This is direct mapping from values of @javax.servlet.annotation.HandlesTypes to SCIs that
				// express their interest in these values
				Map<Class<?>, Set<ServletContainerInitializer>> htToSci = new HashMap<>();

				for (ServletContainerInitializer sci : detectedSCIs) {
					sciToHt.put(sci, new HashSet<>());
				}

				boolean thereAreHTClasses = false;
				boolean thereAreHTAnnotations = false;

				if (ok) {
					for (ServletContainerInitializer sci : detectedSCIs) {
						HandlesTypes ht = sci.getClass().getAnnotation(HandlesTypes.class);
						if (ht != null && ht.value().length > 0) {
							for (Class<?> c : ht.value()) {
								htToSci.computeIfAbsent(c, _c -> new HashSet<>()).add(sci);
								if (!thereAreHTClasses && !c.isAnnotation()) {
									thereAreHTClasses = true;
								}
								if (!thereAreHTAnnotations && c.isAnnotation()) {
									thereAreHTAnnotations = true;
								}
							}
						}
					}
				}

				// now the deep scanning for annotated elements - performed when metadata-complete=false or if
				// there are any SCIs with @HandlesTypes
				// see org.apache.catalina.startup.ContextConfig.processClasses()
				if (ok && (!mainWebXml.isMetadataComplete() || !htToSci.isEmpty())) {
					LOG.debug("Scanning for annotated classes and/or types declared in @HandlesTypes SCI annotations");
					classSpace.scanClasses(htToSci, sciToHt, thereAreHTClasses, thereAreHTAnnotations);
				}

				// at this stage we have full mapping of SCIs to sets of classes to pass to their onStartup() method
				// and also all the WebXml fragments are possibly altered using annotated servlets/filters/listeners
				// the last thing is the WebXml merging and Pax Web happily reuses
				// org.apache.tomcat.util.descriptor.web.WebXml functionality to do that

				if (!mainWebXml.isMetadataComplete() && ok) {
					// only in this case, merge the ordered (web) fragments in
					ok = mainWebXml.merge(classSpace.getOrderedFragments());
				}

				// merge in the default web.xml (with default and JSP servlets)
				mainWebXml.merge(Collections.singleton(defaultWebXml));

				if (ok) {
					// convert "jsp servlets"
					ServletDef jsp = mainWebXml.getServlets().get(PaxWebConstants.DEFAULT_JSP_SERVLET_NAME);
					Map<String, String> jspInitParams = jsp == null ? Collections.emptyMap() : jsp.getParameterMap();
					for (Iterator<Map.Entry<String, ServletDef>> it = mainWebXml.getServlets().entrySet().iterator(); it.hasNext(); ) {
						Map.Entry<String, ServletDef> entry = it.next();
						String name = entry.getKey();
						ServletDef def = entry.getValue();
						if (def.getJspFile() != null) {
							if (jsp == null) {
								LOG.warn("A servlet named {} is used with jsp-file, but there's no \"jsp\" servlet configured. Removing.",
										def.getServletName());
								it.remove();
								mainWebXml.getServletMappings().entrySet()
										.removeIf(e -> e.getValue().equals(def.getServletName()));
							} else {
								String jspFile = def.getJspFile();
								if (!jspFile.startsWith("/")) {
									jspFile = "/" + jspFile;
								}
								def.setServletClass(jsp.getServletClass());
								jspInitParams.forEach(def::addInitParameter);
								def.addInitParameter("jspFile", jspFile);
								// clear this value out
								def.setJspFile(null);
							}
						}
					}
				}

				// Tomcat already configures an instance of org.apache.catalina.core.StandardContext here, but
				// Pax Web will do it later

				// configure /META-INF/resources locations
				// TODO: /META-INF/resources require 1:N alias:location mapping

				LOG.debug("Finished metadata and fragment processing for {} in {}ms", bundle, System.currentTimeMillis() - start);
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(tccl);
		}
	}

	/**
	 * This method turns the raw data collected from descriptors and annotations into a model of elements from
	 * {@code org.ops4j.pax.web.service.spi.model} package configured in a transactional {@link Batch}.
	 */
	@SuppressWarnings("unchecked")
	private void buildModel() {
		final Batch wabBatch = new Batch("Deployment of " + this);

		wabBatch.beginTransaction(contextPath);

		ServletContextModel scm = new ServletContextModel(contextPath);
		wabBatch.addServletContextModel(scm);

		// TOCHECK: 3rd option for whiteboard/httpservice context model? it's about disassociation of the context
		//          during undeployment - but what about whiteboard filters registered to WAB's context?

		// 1. The most important part - the OsgiContextModel which bridges web elements from the WAB to actual
		// servlet context in several aspects - it scopes access to resources and provides proper classloader.
		// Remember that the same OsgiContextModel is used in THREE areas:
		//  - HttpService (isWhiteboard() == false, hasDirectHttpContextInstance() == true)
		//  - Whiteboard (isWhiteboard() == true, hasDirectHttpContextInstance() == false)
		//  - WAB (isWhiteboard() == false, hasDirectHttpContextInstance() == false)
		// the hasDirectHttpContextInstance() == false in WAB is important - we can't allow to re-register
		// web elements from the WAB when new OsgiContextModel is created - there are several checks in
		// pax-web-extender-whiteboard and pax-web-runtime that determine whether a web element can be re-registered
		// to new "context". The elements from WAB simply CAN'T do it
		// TODO: we should only allow other bundles to obtain WebContainer instance within the scope of bundle
		//       context of the WAB, for example to register additional web elements (usually filters) altering
		//       the web application of the WAB

		final OsgiContextModel ocm = new OsgiContextModel(null, this.bundle, this.contextPath, false);
		ocm.setServiceId(0);
		ocm.setServiceRank(Integer.MAX_VALUE);
		ocm.setContextSupplier((context, contextName) -> {
			Bundle b = context == null ? null : context.getBundle();
			return new WebContainerContextWrapper(b, new WebApplicationHelper(b), contextName);
		});
		// very important step - we pass a classloader, which contains reachable bundles - bundles discovered when
		// WAB's metadata was parsed/processed
		ocm.setClassLoader(this.classLoader);

		// 1.1. Session configuration is passed directly in OsgiContextModel
		SessionConfig webXmlSessionConfig = mainWebXml.getSessionConfig();
		if (webXmlSessionConfig != null) {
			if (webXmlSessionConfig.getSessionTimeout() != null) {
				ocm.setSessionTimeout(webXmlSessionConfig.getSessionTimeout());
			}

			SessionCookieConfig scc = new DefaultSessionCookieConfig();
			ocm.setSessionCookieConfig(scc);
			if (webXmlSessionConfig.getCookieName() != null) {
				scc.setName(webXmlSessionConfig.getCookieName());
			}
			if (webXmlSessionConfig.getCookieDomain() != null) {
				scc.setDomain(webXmlSessionConfig.getCookieDomain());
			}
			if (webXmlSessionConfig.getCookiePath() != null) {
				scc.setPath(webXmlSessionConfig.getCookiePath());
			}
			if (webXmlSessionConfig.getCookieMaxAge() != null) {
				scc.setMaxAge(webXmlSessionConfig.getCookieMaxAge());
			}
			if (webXmlSessionConfig.getCookieHttpOnly() != null) {
				scc.setHttpOnly(webXmlSessionConfig.getCookieHttpOnly());
			}
			if (webXmlSessionConfig.getCookieSecure() != null) {
				scc.setSecure(webXmlSessionConfig.getCookieSecure());
			}
			if (webXmlSessionConfig.getCookieComment() != null) {
				scc.setComment(webXmlSessionConfig.getCookieComment());
			}

			if (webXmlSessionConfig.getSessionTrackingModes() != null) {
				ocm.getSessionConfiguration().getTrackingModes().addAll(webXmlSessionConfig.getSessionTrackingModes());
			}
		}

		// 1.2. JSP configuration is also part of OsgiContextModel - the highest ranked one for given
		//      ServletContextModelwill be taken. Used in org.apache.jasper.servlet.TldScanner.scanJspConfig()
		ocm.getJspConfigDescriptor().getTaglibs().clear();
		ocm.getJspConfigDescriptor().getJspPropertyGroups().clear();
		JspConfigDescriptor jspConfig = mainWebXml.getJspConfigDescriptor();
		if (jspConfig != null) {
			ocm.addTagLibs(jspConfig.getTaglibs());
			for (JspPropertyGroupDescriptor group : jspConfig.getJspPropertyGroups()) {
				ocm.addJspPropertyGroupDescriptor(group);
			}
		}

		// 1.3. Some initial attributes. "osgi-bundlecontext" and
		//      "org.springframework.osgi.web.org.osgi.framework.BundleContext" will be set in the constructor
		//      of OsgiServletContext, because these are needed also in Whiteboard and HttpService scenarios.
		if (classSpace.getOrderedLibs() != null) {
			ocm.getInitialContextAttributes().put(ServletContext.ORDERED_LIBS, classSpace.getOrderedLibs());
		}

		// 1.4. Context initial parameters
		ocm.getContextParams().putAll(mainWebXml.getContextParams());

		wabBatch.addOsgiContextModel(ocm, scm);
		this.osgiContextModel = ocm;

		// elements from web.xml are processed to create a Batch that'll be send to a dedicated view of a WebContainer.
		// Tomcat configures its context directly in org.apache.catalina.startup.ContextConfig.configureContext()

		// web.xml (http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd) defines the below top-level elements.
		// "J" means "processed by Jetty 9", "T" means "processed by Tomcat 9", "+" means already handled by Pax Web,
		// "-" means not yet handled, "x" means there's no need to handle it
		//  + [JT] <absolute-ordering> (used only when parsing metadata)
		//  + [JT] <context-param>
		//  + [JT] <deny-uncovered-http-methods>
		//  + [JT] <display-name>
		//  + [JT] <distributable>
		//  - [JT] <env-entry>
		//  + [JT] <error-page>
		//  + [JT] <filter>
		//  + [JT] <filter-mapping>
		//  + [JT] <jsp-config>
		//  + [JT] <listener>
		//  + [JT] <locale-encoding-mapping-list>
		//  - [JT] <login-config>
		//  + [JT] <mime-mapping>
		//  - [JT] <post-construct>
		//  - [JT] <pre-destroy>
		//  + [.T] <request-character-encoding> (Servlet 4)
		//  - [JT] <resource-env-ref>
		//  - [JT] <resource-ref>
		//  + [.T] <response-character-encoding> (Servlet 4)
		//  - [JT] <security-constraint>
		//  - [JT] <security-role>
		//  + [JT] <servlet>
		//  + [JT] <servlet-mapping>
		//  + [JT] <session-config>
		//  + [JT] <welcome-file-list>
		//
		// These won't be handled:
		//  x [..] <administered-object>
		//  x [..] <connection-factory>
		//  x [..] <data-source>
		//  x [..] <default-context-path>
		//  x [..] <description>
		//  x [.T] <ejb-local-ref>
		//  x [.T] <ejb-ref>
		//  x [..] <icon>
		//  x [..] <jms-connection-factory>
		//  x [..] <jms-destination>
		//  x [..] <mail-session>
		//  x [.T] <message-destination>
		//  x [JT] <message-destination-ref>
		//  x [..] <module-name>
		//  x [..] <persistence-context-ref>
		//  x [..] <persistence-unit-ref>
		//  x [.T] <service-ref>
		//
		// See:
		// Tomcat:
		//  - parsing: org.apache.tomcat.util.descriptor.web.WebRuleSet.addRuleInstances()
		//  - configuration: org.apache.catalina.startup.ContextConfig.configureContext()
		// Jetty:
		//  - parsing: org.eclipse.jetty.webapp.StandardDescriptorProcessor + org.eclipse.jetty.plus.webapp.PlusDescriptorProcessor
		//  - configuration: org.eclipse.jetty.webapp.MetaData.resolve()

		// The detailed and ordered context configuration process in Tomcat is alphabetical:
		//  + public id
		//  + effective major/minor version
		//  + context (init) parameters
		//  + deny uncovered HTTP methods
		//  + display name
		//  + distributable flag
		//  + error pages
		//  + filters and their mappings
		//  + jsp config descriptor
		//  + listeners
		//  + locale encoding mapping paramters
		//  - login config
		//  + metadata complete (a.k.a. "ignore annotations") - Tomcat calls org.apache.tomcat.InstanceManager.destroyInstance()
		//    on an instance of filter/servlet after f.destroy()/s.destroy() if annotations are not ignored.
		//    the annotations are javax.annotation.PostConstruct/javax.annotation.PreDestroy
		//  + mime mapping
		//  + request character encoding
		//  + response character encoding
		//  - security constraints
		//  - security roles
		//  + servlets
		//  - servlets' security roles
		//  + servlets' multipart config
		//  + servlets' mappings
		//  + session config and session cookie config
		//  + welcome files
		//  + jsp property groups
		//  - postconstruct and predestroy methods
		//
		// These web elements are configured in Tomcat context's org.apache.catalina.Context.getNamingResources():
		//  - ejb local refs
		//  - ejbs
		//  - env entries
		//  - message destination refs
		//  - resource env refs
		//  - resource refs
		//  - service refs

		// but we'll do it in a more organized way

		// 2. some metadata related with context, but kept at OsgiContextModel level - not everything has to
		//    be propagated to Server/Service model

		// 2.1. public id, major, minor version, display name, distributable flag, metadata complete flag,
		//      req/res character encoding (Servlet API 4), ...
		ContextMetadataModel meta = new ContextMetadataModel();
		meta.setPublicId(mainWebXml.getPublicId());
		meta.setMajorVersion(mainWebXml.getMajorVersion());
		meta.setMinorVersion(mainWebXml.getMinorVersion());
		meta.setMetadataComplete(mainWebXml.isMetadataComplete());
		meta.setDistributable(mainWebXml.isDistributable());
		meta.setDisplayName(mainWebXml.getDisplayName());
		meta.setRequestCharacterEncoding(mainWebXml.getRequestCharacterEncoding());
		meta.setResponseCharacterEncoding(mainWebXml.getResponseCharacterEncoding());
		meta.setDenyUncoveredHttpMethods(mainWebXml.getDenyUncoveredHttpMethods());
		wabBatch.configureMetadata(meta, ocm);

		// 2.2. MIME mapping - come from Tomcat's default web.xml packaged inside pax-web-spi. This web.xml
		//      contains huge list of <mime-mapping>s. Also Locale-Encoding mapping
		wabBatch.configureMimeAndEncodingMappings(mainWebXml.getMimeMappings(), mainWebXml.getLocaleEncodingMappings(), ocm);

		// 3. servlets, their mapping and multipart config
		final Map<String, List<String>> servletMappings = new LinkedHashMap<>();
		mainWebXml.getServletMappings().forEach((pattern, sn) -> {
			servletMappings.computeIfAbsent(sn, n -> new LinkedList<>()).add(pattern);
		});
		mainWebXml.getServlets().forEach((sn, def) -> {
			Class<Servlet> servletClass = null;
			try {
				if (def.getServletClass() != null) {
					servletClass = (Class<Servlet>) classLoader.loadClass(def.getServletClass());
				}
			} catch (ClassNotFoundException e) {
				LOG.warn("Can't load servlet class {} in the context of {}: {}",
						def.getServletClass(), this, e.getMessage(), e);
				return;
			}
			String[] mappings = servletMappings.get(sn).toArray(new String[0]);
			ServletModel.Builder builder = new ServletModel.Builder()
					.withRegisteringBundle(bundle)
					.withServletName(sn)
					.withServletClass(servletClass)
					.withUrlPatterns(mappings)
					.withAsyncSupported(def.getAsyncSupported())
					.withLoadOnStartup(def.getLoadOnStartup())
					.withInitParams(def.getParameterMap())
					.withOsgiContextModel(ocm);

			MultipartDef md = def.getMultipartDef();
			if (md != null) {
				long maxFileSize = md.getMaxFileSize() == null ? -1L : Long.parseLong(md.getMaxFileSize());
				long maxRequestSize = md.getMaxRequestSize() == null ? -1L : Long.parseLong(md.getMaxRequestSize());
				int fileSizeThreshold = md.getFileSizeThreshold() == null ? 0 : Integer.parseInt(md.getFileSizeThreshold());
				MultipartConfigElement mpConfig = new MultipartConfigElement(md.getLocation(), maxFileSize, maxRequestSize, fileSizeThreshold);
				builder.withMultipartConfigElement(mpConfig);
			}
			if (servletClass == null) {
				// the actuall class will depend on the target runtime
				builder.resourceServlet(true);
			}
			if (PaxWebConstants.DEFAULT_JSP_SERVLET_NAME.equals(sn)) {
				builder.jspServlet(true);
			}
			wabBatch.addServletModel(builder.build());
		});

		// 4. filters and their mappings
		Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> allFilterModels = new HashMap<>();
		TreeMap<FilterModel, List<OsgiContextModel>> filterModels = new TreeMap<>();
		allFilterModels.put(contextPath, filterModels);
		Map<String, FilterMap> filterMappings = new HashMap<>();
		mainWebXml.getFilterMappings().forEach(fn -> filterMappings.put(fn.getFilterName(), fn));
		mainWebXml.getFilters().forEach((fn, def) -> {
			Class<Filter> filterClass = null;
			try {
				if (def.getFilterClass() != null) {
					filterClass = (Class<Filter>) classLoader.loadClass(def.getFilterClass());
				}
			} catch (ClassNotFoundException e) {
				LOG.warn("Can't load filter class {} in the context of {}: {}",
						def.getFilterClass(), this, e.getMessage(), e);
				return;
			}
			String[] mappings = filterMappings.get(fn).getURLPatterns();
			String[] servletNames = filterMappings.get(fn).getServletNames();
			String[] dispatcherTypes = filterMappings.get(fn).getDispatcherNames();
			FilterModel.Builder builder = new FilterModel.Builder()
					.withRegisteringBundle(bundle)
					.withFilterName(fn)
					.withFilterClass(filterClass)
					.withUrlPatterns(mappings)
					.withServletNames(servletNames)
					.withDispatcherTypes(dispatcherTypes)
					.withAsyncSupported("true".equals(def.getAsyncSupported()))
					.withInitParams(def.getParameterMap())
					.withOsgiContextModel(ocm);
			FilterModel fm = builder.build();
			filterModels.put(fm, null);
			// this is for Server/Service model
			wabBatch.addFilterModel(fm);
		});
		// this is for ServerController
		wabBatch.updateFilters(allFilterModels, false);

		// 5. listeners
		for (String listener : mainWebXml.getListeners()) {
			Class<EventListener> listenerClass = null;
			try {
				listenerClass = (Class<EventListener>) classLoader.loadClass(listener);
				EventListener eventListener = listenerClass.newInstance();
				EventListenerModel elm = new EventListenerModel(eventListener);
				elm.setRegisteringBundle(bundle);
				elm.addContextModel(ocm);
				wabBatch.addEventListenerModel(elm);
			} catch (ClassNotFoundException e) {
				LOG.warn("Can't load listener class {} in the context of {}: {}", listener, this, e.getMessage(), e);
			} catch (InstantiationException | IllegalAccessException e) {
				LOG.warn("Can't instantiate listener class {} in the context of {}: {}", listener, this, e.getMessage(), e);
			}
		}

		// 6. welcome files - without a way to configure redirect flag
		WelcomeFileModel wfm = new WelcomeFileModel(mainWebXml.getWelcomeFiles().toArray(new String[0]), false);
		wfm.setRegisteringBundle(bundle);
		wfm.addContextModel(ocm);
		wabBatch.addWelcomeFileModel(wfm);

		// 7. error pages
		Map<String, TreeMap<ErrorPageModel, List<OsgiContextModel>>> allEpModels = new HashMap<>();
		TreeMap<ErrorPageModel, List<OsgiContextModel>> epModels = new TreeMap<>();
		allEpModels.put(contextPath, epModels);
		Map<String, List<String>> locationToPage = new LinkedHashMap<>();
		mainWebXml.getErrorPages().values().forEach(ep -> {
			// "name" == error code or exception name
			locationToPage.computeIfAbsent(ep.getLocation(), l -> new ArrayList<>()).add(ep.getName());
		});
		locationToPage.forEach((l, pages) -> {
			ErrorPageModel epm = new ErrorPageModel(pages.toArray(new String[0]), l);
			epm.setRegisteringBundle(bundle);
			epm.addContextModel(ocm);
			epm.performValidation();
			epModels.put(epm, null);
			// this is for Server/Service model
			wabBatch.addErrorPageModel(epm);
		});
		// this is for ServerController
		wabBatch.updateErrorPages(allEpModels);

		// 8. At the end, Tomcat adds SCIs to the context
		this.sciToHt.forEach((sci, classes) -> {
			Class<?>[] classesArray = classes.isEmpty() ? null : classes.toArray(new Class<?>[0]);
			ContainerInitializerModel cim = new ContainerInitializerModel(sci, classesArray);
			cim.setRegisteringBundle(bundle);
			cim.addContextModel(ocm);
			wabBatch.addContainerInitializerModel(cim);
		});

		wabBatch.commitTransaction(contextPath);

		this.batch = wabBatch;
	}

	// --- Web Application model access

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	// --- utility methods

	/**
	 * An internal state of the {@link BundleWebApplication} - there are more such states than
	 * {@link WebApplicationEvent.State events} related to {@link BundleWebApplication} lifecycle. These states
	 * are not directly communicated to observers, but are used to control the lifecycle of the web application.
	 */
	enum State {
		/** Initial state after BundleWebApplication has just been created */
		UNCONFIGURED,

		/** State, where parsing of web.xml, fragments and annotations are processed */
		CONFIGURING,

		/** State after web configuration is ready, but we have to check if the target context (path) is free */
		ALLOCATING_CONTEXT,

		/**
		 * State in which we're actually registering web elements from web.xml/web-fragment.xml/annotations into
		 * allocated context {@link ServletContext context}.
		 */
		DEPLOYING,

		/** Final state - completely and successfully deployed application */
		DEPLOYED,

		/** State where {@link WebContainer} reference is needed, but it's not available */
		WAITING_FOR_WEB_CONTAINER,

		/**
		 * <p>State after web configuration is ready, but simply the target context path is used by another WAB
		 * (or Whiteboard/HttpService context).</p>
		 *
		 * <p>Before Pax Web 8, "WAITING" state was used when a WAB was held due to conflict with existing WAB that
		 * used the same context path (and virtual hosts). Now we have more specialized state and "WAITING" is no
		 * longer used</p>
		 */
		WAITING_FOR_CONTEXT,

		/** The WAB is in the process of undeployment */
		UNDEPLOYING,

		/** The WAB is fully undeployed */
		UNDEPLOYED,

		/**
		 * Failed beyond any repair. This state is used after parsing errors or other validation errors. FAILED
		 * {@link BundleWebApplication} will never be attempted to be deployed again. For a case where WAB is
		 * waiting for a context path to be <em>free</em>, see {@link State#WAITING_FOR_CONTEXT}.
		 */
		FAILED
	}

//	static final Comparator<WebAppServlet> WEB_APP_SERVLET_COMPARATOR =
//			(servlet1, servlet2) -> servlet1.getLoadOnStartup() - servlet2.getLoadOnStartup();
//
//	/**
//	 * The URL to the web.xml for the web app.
//	 */
//	private URL webXmlURL;
//	/**
//	 * Context parameters.
//	 */
//	private final Set<WebAppInitParam> contextParams;
//	/**
//	 * Listeners.
//	 */
//	private final List<WebAppListener> listeners;
//	/**
//	 * Virtual Host List.
//	 */
//	private final List<String> virtualHostList;
//	/**
//	 * Connectors List
//	 */
//	private final List<String> connectorList;
//
//	/**
//	 * SecurityConstraints
//	 */
//	private final List<WebAppConstraintMapping> constraintsMapping;
//
//	private final List<WebAppSecurityRole> securityRoles;
//
//	private final List<WebAppLoginConfig> loginConfig;
//
//	private URL jettyWebXmlURL;
//
//	private WebAppInitParam getWebAppInitParam(String name) {
//		for (WebAppInitParam p : contextParams) {
//			if (name.equals(p.getParamName())) {
//				return p;
//			}
//		}
//		return null;
//	}
//
//	/**
//	 * Setter.
//	 *
//	 * @param contextName value to set. Cannot be null.
//	 * @throws NullArgumentException if context name is null
//	 */
//	public void setContextName(final String contextName) {
//		NullArgumentException.validateNotNull(contextName, "Context name");
//		this.contextName = contextName;
//
//		// remove the previous setting.
//		WebAppInitParam prev = getWebAppInitParam("webapp.context");
//		if (prev != null) {
//			contextParams.remove(prev);
//		}
//
//		// set the context name into the context params
//		final WebAppInitParam initParam = new WebAppInitParam();
//		initParam.setParamName("webapp.context");
//		initParam.setParamValue(contextName);
//		contextParams.add(initParam);
//	}
//
//	/**
//	 * Add a listener.
//	 *
//	 * @param listener to add
//	 * @throws NullArgumentException if listener or listener class is null
//	 */
//	public void addListener(final WebAppListener listener) {
//		NullArgumentException.validateNotNull(listener, "Listener");
//		NullArgumentException.validateNotNull(listener.getListenerClass(),
//				"Listener class");
//		if (!listeners.contains(listener)) {
//			listeners.add(listener);
//		}
//	}
//
//	/**
//	 * Add a context param.
//	 *
//	 * @param contextParam to add
//	 * @throws NullArgumentException if context param, param name or param value is null
//	 */
//	public void addContextParam(final WebAppInitParam contextParam) {
//		NullArgumentException.validateNotNull(contextParam, "Context param");
//		NullArgumentException.validateNotNull(contextParam.getParamName(),
//				"Context param name");
//		NullArgumentException.validateNotNull(contextParam.getParamValue(),
//				"Context param value");
//		contextParams.add(contextParam);
//	}
//
//	/**
//	 * Return all context params.
//	 *
//	 * @return an array of all context params
//	 */
//	public WebAppInitParam[] getContextParams() {
//		return contextParams.toArray(new WebAppInitParam[contextParams.size()]);
//	}
//
//	/**
//	 * Add a security constraint
//	 *
//	 * @param constraintMapping
//	 * @throws NullArgumentException if security constraint is null
//	 */
//	public void addConstraintMapping(
//			final WebAppConstraintMapping constraintMapping) {
//		NullArgumentException.validateNotNull(constraintMapping,
//				"constraint mapping");
//		constraintsMapping.add(constraintMapping);
//	}
//
//	/**
//	 * @return list of {@link WebAppConstraintMapping}
//	 */
//	public WebAppConstraintMapping[] getConstraintMappings() {
//		return constraintsMapping
//				.toArray(new WebAppConstraintMapping[constraintsMapping.size()]);
//	}
//
//	/**
//	 * Adds a security role
//	 *
//	 * @param securityRole
//	 */
//	public void addSecurityRole(final WebAppSecurityRole securityRole) {
//		NullArgumentException.validateNotNull(securityRole, "Security Role");
//		securityRoles.add(securityRole);
//	}
//
//	/**
//	 * @return list of {@link WebAppSecurityRole}
//	 */
//	public WebAppSecurityRole[] getSecurityRoles() {
//		return securityRoles.toArray(new WebAppSecurityRole[securityRoles
//				.size()]);
//	}
//
//	/**
//	 * Adds a login config
//	 *
//	 * @param webApploginConfig
//	 */
//	public void addLoginConfig(final WebAppLoginConfig webApploginConfig) {
//		NullArgumentException
//				.validateNotNull(webApploginConfig, "Login Config");
//		NullArgumentException.validateNotNull(
//				webApploginConfig.getAuthMethod(),
//				"Login Config Authorization Method");
//		// NullArgumentException.validateNotNull(loginConfig.getRealmName(),
//		// "Login Config Realm Name");
//		loginConfig.add(webApploginConfig);
//	}
//
//	/**
//	 * @return list of {@link WebAppLoginConfig}
//	 */
//	public WebAppLoginConfig[] getLoginConfigs() {
//		return loginConfig.toArray(new WebAppLoginConfig[loginConfig.size()]);
//	}
//
//	/**
//	 * Accepts a visitor for inner elements.
//	 *
//	 * @param visitor visitor
//	 */
//	public void accept(final WebAppVisitor visitor) {
//		for (WebAppListener listener : listeners) {
//			visitor.visit(listener);
//		}
//		if (!constraintsMapping.isEmpty()) {
//			for (WebAppConstraintMapping constraintMapping : constraintsMapping) {
//				visitor.visit(constraintMapping);
//			}
//
//		}
//	}
//
//	public URL getWebXmlURL() {
//		return webXmlURL;
//	}
//
//	public void setWebXmlURL(URL webXmlURL) {
//		this.webXmlURL = webXmlURL;
//	}
//
//	public void setJettyWebXmlURL(URL jettyWebXmlURL) {
//		this.jettyWebXmlURL = jettyWebXmlURL;
//	}
//
//	public URL getJettyWebXmlURL() {
//		return jettyWebXmlURL;
//	}
//
//	public void setVirtualHostList(List<String> virtualHostList) {
//		this.virtualHostList.clear();
//		this.virtualHostList.addAll(virtualHostList);
//	}
//
//	public List<String> getVirtualHostList() {
//		return virtualHostList;
//	}
//
//	public void setConnectorList(List<String> connectorList) {
//		this.connectorList.clear();
//		this.connectorList.addAll(connectorList);
//	}
//
//	public List<String> getConnectorList() {
//		return connectorList;
//	}

}
