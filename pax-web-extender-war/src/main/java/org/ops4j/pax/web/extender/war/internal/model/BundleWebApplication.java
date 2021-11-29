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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.JspPropertyGroupDescriptor;

import org.apache.felix.utils.extender.Extension;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.MultipartDef;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.SessionConfig;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.ops4j.pax.web.extender.war.internal.WarExtenderContext;
import org.ops4j.pax.web.extender.war.internal.WebApplicationHelper;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.context.WebContainerContextWrapper;
import org.ops4j.pax.web.service.spi.model.ContextMetadataModel;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.info.WebApplicationInfo;
import org.ops4j.pax.web.service.spi.model.elements.ContainerInitializerModel;
import org.ops4j.pax.web.service.spi.model.elements.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.elements.FilterModel;
import org.ops4j.pax.web.service.spi.model.elements.LoginConfigModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConfigurationModel;
import org.ops4j.pax.web.service.spi.model.elements.SecurityConstraintModel;
import org.ops4j.pax.web.service.spi.model.elements.ServletModel;
import org.ops4j.pax.web.service.spi.model.elements.WelcomeFileModel;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.views.WebAppWebContainerView;
import org.ops4j.pax.web.service.spi.servlet.DefaultSessionCookieConfig;
import org.ops4j.pax.web.service.spi.servlet.OsgiServletContextClassLoader;
import org.ops4j.pax.web.service.spi.task.Batch;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
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
	 * Map of URLs to {@code META-INF/resources} of reachable bundles and WAB's embedded JARs keyed by the
	 * bundles used to access those roots.
	 */
	private final Map<Bundle, URL> metainfResourceRoots = new LinkedHashMap<>();

	private final Set<URL> faceletTagLibDescriptors = new LinkedHashSet<>();

	private final List<URL> serverSpecificDescriptors = new ArrayList<>();

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

	/**
	 * Stored instance of {@link org.osgi.service.http.HttpContext} that wraps
	 * {@link org.osgi.service.http.context.ServletContextHelper}, so we're able to reference this context
	 * both in Whiteboard and HttpService scenarios.
 	 */
	private WebContainerContext httpContext = null;

	/**
	 * If our WAB manages to <em>take</em> given context path, we'll have {@link OsgiContextModel} available
	 * for us to configure (using {@code web.xml}, fragments, ...).
	 */
	private OsgiContextModel allocatedOsgiContextModel = null;

	/**
	 * Allocated {@link ServletContextModel}
	 */
	private ServletContextModel allocatedServletContextModel = null;

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
	 * {@code org.apache.felix.utils.extender.AbstractExtender#destroyExtension}, so what will be started after
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

		scheduleIfPossible(State.UNCONFIGURED, State.CONFIGURING, true);
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

		// get a WebContainer for the last time - it doesn't have to be available (no need to lock here)
		ServiceReference<WebContainer> ref = webContainerServiceRef;
		WebAppWebContainerView view = webContainerManager.containerView(bundle, ref, WebAppWebContainerView.class);

		State state = deploymentState.get();

		// depending on current state, we may have to clean up more resources or just finish quickly
		switch (state) {
			case UNCONFIGURED:
			case CONFIGURING:
			case UNDEPLOYED:
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

		// whether the undeployment failed, succeeded or wasn't needed, we set the final state
		deploymentState.set(State.UNDEPLOYED);

		webContainerManager.releaseContainer(bundle, ref);
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
			WebAppWebContainerView view = webContainerManager.containerView(bundle, webContainerServiceRef,
					WebAppWebContainerView.class);

			State state = deploymentState.get();

			if (view == null) {
				LOG.info("WebContainer reference {} was removed, {} should already be undeployed.", ref, this);
				// but still let's start with new allocation attempt (in new WebContainer that'll be set in future)
				switch (state) {
					case WAITING_FOR_CONTEXT:
					case ALLOCATING_CONTEXT:
					case DEPLOYING:
					case DEPLOYED:
						deploymentState.set(State.WAITING_FOR_CONTEXT);
						break;
					default:
						// leave as is (earlier stages of deployment)
				}
			} else {
				// as in stop(), we should UNDEPLOY the application, but (unlike as in stop()) to the stage, where
				// it could potentially be DEPLOYED again. re-registration of WebContainer service won't change the
				// information content of current WAB (when WAB is restarted/refreshed, it may get new bundle
				// fragments attached, thus new web fragments may become available), so it's safe to reuse already
				// parsed web.xml + fragments + annotations

				// similar state check as with stop(), but with slightly different handling
				switch (state) {
					case UNCONFIGURED:
					case CONFIGURING:
					case UNDEPLOYED:
					case UNDEPLOYING:
					case WAITING_FOR_WEB_CONTAINER:
					case FAILED:
						// do not change the state at all - keep as is
						break;
					case WAITING_FOR_CONTEXT:
						// switch back to waiting for WebContainer
						deploymentState.set(State.WAITING_FOR_WEB_CONTAINER);
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
						deploymentState.set(State.WAITING_FOR_WEB_CONTAINER);
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
								deploymentState.set(State.UNDEPLOYED);
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
						deploymentState.set(State.UNDEPLOYED);
						break;
					default:
						break;
				}
			}

			webContainerManager.releaseContainer(bundle, ref);
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
			WebAppWebContainerView view = webContainerManager.containerView(bundle, webContainerServiceRef,
					WebAppWebContainerView.class);
			if (view == null) {
				if (deploymentState.compareAndSet(currentState, State.WAITING_FOR_WEB_CONTAINER)) {
					LOG.debug("WebContainer service reference is not available. {} enters Grace Period state.", this);
					// note: there may be duplicate WAITING events if web container is not available before
					// context path reservation and before actual registration of webapp's web elements.
					extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.WAITING, bundle, contextPath, null));
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
	public void deploy() {
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
				extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.DEPLOYING, bundle, contextPath, null));

				// Collect deployment information by processing the web.xml descriptor and other sources of metadata
				processMetadata();

				// after web.xml/web-fragment.xml/annotations are read, we have to check if the context path
				// is available
				State st = deploymentState.get();
				if (st == State.CONFIGURING || st == State.ALLOCATING_CONTEXT) {
					// webContainerRemoved() could already switched the state to ALLOCATING_CONTEXT, but we need to
					// create the allocating latch
					if (deploymentState.compareAndSet(st, State.ALLOCATING_CONTEXT)) {
						if (allocatingLatch != null && allocatingLatch.getCount() > 0) {
							// for now let's keep this brute check
							throw new IllegalStateException("[dev error] Previous context allocation attempt didn't finish"
									+ " properly. Existing latch found.");
						}
						allocatingLatch = new CountDownLatch(1);
					}
				}
			}

			// even if after the above if{} was executed and the state is expected to be ALLOCATING_CONTEXT,
			// we may already be in different state - e.g., STOPPING
			// but most common scenario is that we continue the WAB deployment

			state = deploymentState.get();
			if (state == State.ALLOCATING_CONTEXT || state == State.WAITING_FOR_CONTEXT) {
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
						WebApplicationEvent event = new WebApplicationEvent(WebApplicationEvent.State.FAILED, bundle, contextPath, null);
						event.setAwaitingAllocation(true);
						event.setCollisionIds(extenderContext.calculateCollisionIds(contextPath, bundle));
						extenderContext.sendWebEvent(event);
					}
					return;
				}

				allocatedServletContextModel = view.getServletContext(bundle, contextPath);
				allocatedOsgiContextModel = view.getOsgiContext(bundle, contextPath);

				LOG.info("Allocated context for {}: {}", contextPath, allocatedOsgiContextModel);

				// We have: 1) mainWebXml (altered by fragments), 2) SCIs, 3) ClassLoader, 4) ClassSpace (support)
				// so we can build set of ServletModels, FilterModels, ... that'll be sent to WebContainer
				// implementation as pre-built batch.
				// The OsgiContextModel was obtained when allocating the context
				// This is important advantage of pax-web-extender-war over pax-web-extender-whiteboard. Here
				// we have complete web application, while in Whiteboard, we build it element by element.
				// Here we can do it "transactionally" without bothering about conflicts etc.
				buildModel();

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

			// TODO: special situation for aries-cdi, which is also an extender. its two CDI extensions:
			//  - org.apache.aries.cdi.extension.servlet.weld.WeldServletExtension
			//  - org.apache.aries.cdi.extension.el.jsp.ELJSPExtension
			//  register two servlet context listeners via whiteboard
			// We have to (somehow) wait for this extender before we actually register the web application...

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
					extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.DEPLOYED, bundle, contextPath, httpContext));
				}
			}
		} catch (Throwable t) {
			deploymentState.set(State.FAILED);

			// we are not configuring a listener that sends the events to LogService (we have one for EventAdmin),
			// because we use SLF4J logger directly (backed by pax-logging)
			LOG.error("Problem processing {}: {}", this, t.getMessage(), t);

			extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.FAILED, bundle, contextPath, null, t));
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
			deploymentState.set(State.UNDEPLOYING);
			extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYING, bundle, contextPath, null));
			// 1. undeploy all the web elements from current WAB
			Batch uninstall = batch.uninstall("Undeployment of " + this);
			uninstall.setShortDescription("undeploy " + this.contextPath);
			view.sendBatch(uninstall);

			// 2. free the context
			releaseContext(view, true);
			extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.UNDEPLOYED, bundle, contextPath, null));
		} catch (Exception e) {
			// 128.3.8 Stopping the Web Application Bundle: Any failure during undeploying should be logged but must
			// not stop the cleaning up of resources and notification of (other) listeners as well as handling any
			// collisions.
			LOG.warn("Problem undeploying {}: {}", this, e.getMessage(), e);
			extenderContext.sendWebEvent(new WebApplicationEvent(WebApplicationEvent.State.FAILED, bundle, contextPath, null, e));
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
			if (e.getCause() != null && e.getCause() instanceof RejectedExecutionException) {
				LOG.debug("{} was not undeployed, config executor stopped.", this);
				return;
			}
			if (propagateException) {
				throw new RuntimeException(e);
			} else {
				LOG.warn("Problem releasing context for {}: {}", this, e.getMessage(), e);
			}
		} finally {
			allocatedOsgiContextModel = null;
		}
	}

	// --- Lifecycle processing methods

	/**
	 * <p>Parse all the possible descriptors to create final web application model. The rules specified in Servlet
	 * specification should be applied. We're following the order from
	 * {@code org.apache.catalina.startup.ContextConfig#webConfig()}. And we're even using Tomcat's {@code web.xml}
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

				// Now, search for additional resources:
				// 1) configure /META-INF/resources locations -
				//    according to "4.6 Resources" of the Servlet API 4 specification (OSGi CMPN 128 doesn't say anything about it)
				//    we'll check all the JARs from Bundle-ClassPath and those reachable (via Import-Package or Require-Bundle)
				//    bundles that have META-INF/web-fragment.xml - all locations that contain META-INF/resources will be added
				//    to org.ops4j.pax.web.extender.war.internal.WebApplicationHelper, so they're searched for web resources
				// 2) /META-INF/**/*.taglib.xml to set up javax.faces.FACELETS_LIBRARIES context parameter
				//    First, I wanted to provide /META-INF/services/org.apache.myfaces.spi.FaceletConfigResourceProvider
				//    in pax-web-jsp (or pax-web-extender-war), but it wouldn't really work for WABs embedding
				//    myfaces + primefaces. Pax Web bundle would need optional import of org.apache.myfaces.spi package
				//    or we'd have to for WAB creators to import our package with the class implementing this service...
				//    That's why a better approach is to prepopulate javax.faces.FACELETS_LIBRARIES context init parameter

				try {
					for (URL url : classSpace.getWabClassPath()) {
						URL metainfResource = new URL(url, "META-INF/resources/");
						if (Utils.isDirectory(metainfResource)) {
							metainfResourceRoots.put(bundle, metainfResource);
						}
						// e.g., jar:bundle://46.0:0/WEB-INF/lib/primefaces-10.0.0.jar!/META-INF/primefaces-p.taglib.xml
						faceletTagLibDescriptors.addAll(ClassPathUtil.findEntries(bundle, new URL[] { url },
								"META-INF", "*.taglib.xml", true));
					}
					for (Map.Entry<Bundle, URL> e : classSpace.getApplicationFragmentBundles().entrySet()) {
						URL metainfResource = new URL(e.getValue(), "META-INF/resources/");
						if (Utils.isDirectory(metainfResource)) {
							metainfResourceRoots.put(e.getKey(), metainfResource);
						}
						faceletTagLibDescriptors.addAll(ClassPathUtil.findEntries(e.getKey(), new URL[] { e.getValue() },
								"META-INF", "*.taglib.xml", true));
					}
				} catch (MalformedURLException ignored) {
				}

				// kind of extension point - both Jetty and Tomcat allow container-specific (web-)context
				// configuration.
				//
				// Jetty uses org.eclipse.jetty.webapp.JettyWebXmlConfiguration and searches for jetty8-web.xml,
				// jetty-web.xml and web-jetty.xml (in this order) - all searched for in WEB-INF of the WAR
				// see https://www.eclipse.org/jetty/documentation/jetty-9/index.html#jetty-web-xml-config
				//
				// Tomcat uses org.apache.catalina.startup.ContextConfig.processContextConfig() +
				// ContextConfig.createContextDigester() and processes META-INF/context.xml packaged in the WAR
				// note that WEB-INF/tomcat-web.xml is an alternative web.xml descriptor, not a digester-parsed
				// XML file.
				// see https://tomcat.apache.org/tomcat-9.0-doc/config/context.html#Defining_a_context
				//
				// Undertow doesn't have such "extension point"

				// Jetty - search in WEB-INF - also in attached OSGi fragments
				Enumeration<URL> descriptors = bundle.findEntries("WEB-INF", "jetty-web.xml", false);
				if (descriptors == null) {
					descriptors = bundle.findEntries("WEB-INF", "web-jetty.xml", false);
				}
				if (descriptors != null) {
					while (descriptors.hasMoreElements()) {
						URL url = descriptors.nextElement();
						LOG.debug("Found Jetty-specific descriptor: {}", url);
						serverSpecificDescriptors.add(url);
					}
				}

				// Tomcat - search in META-INF - both in top-level of the WAR and in WEB-INF/classes/META-INF
				// (or generally - in non-jar entries on WAB's Bundle-ClassPath)
				descriptors = bundle.findEntries("META-INF", "context.xml", false);
				if (descriptors != null) {
					while (descriptors.hasMoreElements()) {
						URL url = descriptors.nextElement();
						LOG.debug("Found Tomcat-specific descriptor: {}", url);
						serverSpecificDescriptors.add(url);
					}
				}
				List<URL> urls = ClassPathUtil.findEntries(bundle, ClassPathUtil.getClassPathNonJars(bundle), "META-INF", "context.xml", false);
				for (URL url : urls) {
					LOG.debug("Found Tomcat-specific descriptor: {}", url);
					serverSpecificDescriptors.add(url);
				}

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
		wabBatch.setShortDescription("deploy " + this.contextPath);

		wabBatch.beginTransaction(contextPath);

		// we simply take the allocated (and associated with our bundle and our bundle's Web-ContextPath) context
		ServletContextModel scm = allocatedServletContextModel;
		// but we still add it to the batch - ServerModel has special handling for allocated Servlet/OsgiContextModels
		wabBatch.addServletContextModel(scm);

		// 1. The most important part - the OsgiContextModel which bridges web elements from the WAB to actual
		// servlet context in several aspects - it scopes access to resources and provides proper classloader.
		// Remember that the same OsgiContextModel is used in THREE areas:
		//  - HttpService (isWhiteboard() == false, isWab() == false, hasDirectHttpContextInstance() == true)
		//  - Whiteboard (isWhiteboard() == true, hasDirectHttpContextInstance() == false)
		//  - WAB (isWab() == true, hasDirectHttpContextInstance() == true)
		// the hasDirectHttpContextInstance() == false in WAB is important - we can't allow to re-register
		// web elements from the WAB when new OsgiContextModel is created - there are several checks in
		// pax-web-extender-whiteboard and pax-web-runtime that determine whether a web element can be re-registered
		// to new "context". The elements from WAB simply CAN'T do it

		// we simply take the allocated (and associated with our bundle and our bundle's Web-ContextPath) context
		final OsgiContextModel ocm = allocatedOsgiContextModel;

		ocm.setWab(true);
		ocm.setServiceId(0);
		ocm.setServiceRank(Integer.MAX_VALUE);
		// very important step - we pass a classloader, which contains reachable bundles - bundles discovered when
		// WAB's metadata was parsed/processed
		ocm.setClassLoader(this.classLoader);
		// this is important - we should be able to reference the context by path
		ocm.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, this.contextPath);
		// we should NOT set osgi.http.whiteboard.context.name=default, otherwise, Whiteboard elements without
		// context selector would surprisingly be registered to WAB context with non "/" context path
		// so if Whiteboard element is targetting a WAB, it should ONLY use osgi.http.whiteboard.context.path in
		// osgi.http.whiteboard.context.select selector
//		ocm.getContextRegistrationProperties().put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, PaxWebConstants.DEFAULT_CONTEXT_NAME);

		// this is the best place to think about how to reference the underlying "context"
		// in HttpService and Whiteboard scenarios.
		//  - For HttpService, the OsgiContextModel needs a direct reference to HttpContext object and
		//    we'll create it here - can be later caught through WebApplicationEventListener
		//    in order to combine Whiteboard and HttpService scenarios, we'll wrap it in WebContainerContextWrapper
		//  - For Whiteboard, we need a name to reference using osgi.http.whiteboard.context.select property
		//    and we'll use contextPath as the name

		WebApplicationHelper contextHelper = new WebApplicationHelper(bundle, metainfResourceRoots);
		// do NOT use "default" as the name of the context, so it's NOT patched by
		// osgi.http.whiteboard.context.select=(osgi.http.whiteboard.context.name=default)
		httpContext = new WebContainerContextWrapper(bundle, contextHelper, contextPath, false);
		ocm.setHttpContext(httpContext);

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
		if (!faceletTagLibDescriptors.isEmpty()) {
			final Map<String, URL> faceletsUrlMapping = new HashMap<>();
			String faceletsLibraries = faceletTagLibDescriptors.stream()
					.map(u -> {
						String ef = u.toExternalForm();
						String path = ef.contains("!/") ? ef.substring(ef.indexOf("!/") + 2) : u.getPath();
						faceletsUrlMapping.putIfAbsent(path, u);
						return path;
					})
					.collect(Collectors.joining(";"));
			ocm.getContextParams().put("javax.faces.FACELETS_LIBRARIES", faceletsLibraries);
			ocm.getInitialContextAttributes().put(PaxWebConstants.CONTEXT_PARAM_PAX_WEB_FACELETS_LIBRARIES, faceletsUrlMapping);
		}

		// 1.4. Context initial parameters
		ocm.getContextParams().putAll(mainWebXml.getContextParams());

		// 1.5. Security - also part of OsgiContextModel
		// <login-config>
		LoginConfig loginConfig = mainWebXml.getLoginConfig();
		SecurityConfigurationModel securityConfiguration = ocm.getSecurityConfiguration();
		if (loginConfig != null) {
			LoginConfigModel lcm = new LoginConfigModel();
			lcm.setAuthMethod(loginConfig.getAuthMethod());
			lcm.setRealmName(loginConfig.getRealmName());
			lcm.setFormLoginPage(loginConfig.getLoginPage());
			lcm.setFormErrorPage(loginConfig.getErrorPage());
			securityConfiguration.setLoginConfig(lcm);
		}
		// <security-role>
		securityConfiguration.getSecurityRoles().addAll(mainWebXml.getSecurityRoles());
		// <security-constraint>
		for (SecurityConstraint sc : mainWebXml.getSecurityConstraints()) {
			SecurityConstraintModel constraint = new SecurityConstraintModel();
			// <display-name> (no <name> at this level)
			constraint.setName(sc.getDisplayName());
			// <web-resource-collection> elements
			for (SecurityCollection wrc : sc.findCollections()) {
				SecurityConstraintModel.WebResourceCollection collection = new SecurityConstraintModel.WebResourceCollection();
				collection.setName(wrc.getName());
				collection.getMethods().addAll(Arrays.asList(wrc.findMethods()));
				collection.getOmittedMethods().addAll(Arrays.asList(wrc.findOmittedMethods()));
				collection.getPatterns().addAll(Arrays.asList(wrc.findPatterns()));
				constraint.getWebResourceCollections().add(collection);
			}
			// <auth-constraint> elements
			constraint.setAuthRolesSet(sc.getAuthConstraint());
			constraint.getAuthRoles().addAll(Arrays.asList(sc.findAuthRoles()));
			// in case the roles were missing and used in <auth-constraint>, we have to add them here
			securityConfiguration.getSecurityRoles().addAll(constraint.getAuthRoles());
			// <user-data-constraint>
			if (sc.getUserConstraint() != null && !"".equals(sc.getUserConstraint().trim())) {
				if (ServletSecurity.TransportGuarantee.NONE.toString().equals(sc.getUserConstraint())) {
					constraint.setTransportGuarantee(ServletSecurity.TransportGuarantee.NONE);
				} else {
					constraint.setTransportGuarantee(ServletSecurity.TransportGuarantee.CONFIDENTIAL);
				}
			}

			securityConfiguration.getSecurityConstraints().add(constraint);
		}

		// 1.6 context specific configuration URLs
		ocm.getServerSpecificDescriptors().addAll(serverSpecificDescriptors);

		wabBatch.addOsgiContextModel(ocm, scm);
		wabBatch.associateOsgiContextModel(httpContext, ocm);

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
		//  + [JT] <login-config>
		//  + [JT] <mime-mapping>
		//  - [JT] <post-construct>
		//  - [JT] <pre-destroy>
		//  + [.T] <request-character-encoding> (Servlet 4)
		//  - [JT] <resource-env-ref>
		//  - [JT] <resource-ref>
		//  + [.T] <response-character-encoding> (Servlet 4)
		//  + [JT] <security-constraint>
		//  + [JT] <security-role>
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
		//  + login config
		//  + metadata complete (a.k.a. "ignore annotations") - Tomcat calls org.apache.tomcat.InstanceManager.destroyInstance()
		//    on an instance of filter/servlet after f.destroy()/s.destroy() if annotations are not ignored.
		//    the annotations are javax.annotation.PostConstruct/javax.annotation.PreDestroy
		//  + mime mapping
		//  + request character encoding
		//  + response character encoding
		//  + security constraints
		//  + security roles
		//  + servlets
		//  + servlets' security roles
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

		// Tomcat, after calling org.apache.catalina.startup.ContextConfig.webConfig(),
		// calls org.apache.catalina.startup.ContextConfig.applicationAnnotationsConfig() which processes
		// additional annotations on servlets/filters/listeners
		// for all elements:
		//  - class level:
		//     - @javax.annotation.Resource -> org.apache.catalina.deploy.NamingResourcesImpl.addEnvironment()
		//       or org.apache.catalina.deploy.NamingResourcesImpl.addService()
		//       or org.apache.catalina.deploy.NamingResourcesImpl.addResource()
		//       or org.apache.catalina.deploy.NamingResourcesImpl.addMessageDestinationRef()
		//       or org.apache.catalina.deploy.NamingResourcesImpl.addResourceEnvRef()
		//     - @javax.annotation.Resources
		//     - @EJB (commented out in Tomcat code)
		//     - @WebServiceRef (commented out in Tomcat code)
		//     - @javax.annotation.security.DeclareRoles -> org.apache.catalina.Context.addSecurityRole()
		//  - field level:
		//     - @javax.annotation.Resource
		//  - method level:
		//     - @javax.annotation.Resource
		// additionally for servlets:
		//  - class level:
		//     - @javax.annotation.security.RunAs -> org.apache.catalina.Wrapper.setRunAs() (only for EJB I guess)
		//     - @javax.servlet.annotation.ServletSecurity -> org.apache.catalina.Context.addServletSecurity()
		//
		// we will add relevant information below

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
			if (servletMappings.get(sn) == null) {
				// the servlet may have been overriden by other servlet with the same mapping (for example
				// if you have "/" mapped servlet not named "default")
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
			builder.setOverridable(def.isOverridable());

			if (servletClass != null) {
				// scan for annotations
				// @javax.annotation.security.DeclareRoles
				collectDeclaredRoles(servletClass, securityConfiguration);

				// @javax.annotation.security.RunAs
				RunAs runAs = servletClass.getAnnotation(RunAs.class);
				if (runAs != null) {
					builder.setRunAs(runAs.value());
				}

				// @javax.servlet.annotation.ServletSecurity
				ServletSecurity servletSecurity = servletClass.getAnnotation(ServletSecurity.class);
				if (servletSecurity != null) {
					// similar to <security-constraint>, but with the URLs taken from servlet mapping
					// we have to additionally check for conflicts with generic constraints from web.xml (and fragments)

					// see org.apache.catalina.core.StandardContext.addServletSecurity()
					for (String urlPattern : mappings) {
						boolean foundConflict = false;

						for (SecurityConstraintModel seccm : securityConfiguration.getSecurityConstraints()) {
							for (SecurityConstraintModel.WebResourceCollection col : seccm.getWebResourceCollections()) {
								if (col.getPatterns().contains(urlPattern)) {
									// servlet with @ServletSecurity uses mapping which has already an
									// associated <security-constraint>
									foundConflict = true;
									break;
								}
							}
							if (foundConflict) {
								break;
							}
						}

						if (foundConflict) {
							LOG.warn("Servlet {} annotated with @ServletSecurity has conflict with existing <security-constraints> from the descriptor(s)" +
									" for the pattern \"{}\"", servletClass, urlPattern);
						} else {
							// add the per method constraints
							HttpMethodConstraint[] withMethods = servletSecurity.httpMethodConstraints();
							List<String> omittedMethods = new ArrayList<>();
							for (HttpMethodConstraint c : withMethods) {
								SecurityConstraintModel constraint = new SecurityConstraintModel();
								SecurityConstraintModel.WebResourceCollection col = new SecurityConstraintModel.WebResourceCollection();
								constraint.getWebResourceCollections().add(col);
								constraint.setTransportGuarantee(c.transportGuarantee());
								constraint.getAuthRoles().addAll(Arrays.asList(c.rolesAllowed()));
								if (constraint.getAuthRoles().isEmpty()) {
									// I think it'll do what I want - no roles means DENY all
									constraint.setAuthRolesSet(c.emptyRoleSemantic() == ServletSecurity.EmptyRoleSemantic.DENY);
								} else {
									constraint.setAuthRolesSet(true);
								}
								col.getPatterns().add(urlPattern);
								col.getMethods().add(c.value());
								omittedMethods.add(c.value());

								securityConfiguration.getSecurityConstraints().add(constraint);
							}

							HttpConstraint c = servletSecurity.value();
							// Add the constraint for all the other methods
							if (c != null) {
								SecurityConstraintModel constraint = new SecurityConstraintModel();
								SecurityConstraintModel.WebResourceCollection col = new SecurityConstraintModel.WebResourceCollection();
								constraint.getWebResourceCollections().add(col);
								constraint.setTransportGuarantee(c.transportGuarantee());
								constraint.getAuthRoles().addAll(Arrays.asList(c.rolesAllowed()));
								if (constraint.getAuthRoles().isEmpty()) {
									// I think it'll do what I want - no roles means DENY all
									constraint.setAuthRolesSet(c.value() == ServletSecurity.EmptyRoleSemantic.DENY);
								} else {
									constraint.setAuthRolesSet(true);
								}
								col.getPatterns().add(urlPattern);
								col.getOmittedMethods().addAll(omittedMethods);

								securityConfiguration.getSecurityConstraints().add(constraint);
							}
						}
					}
				}
			}
			if (def.getSecurityRoleRefs() != null) {
				def.getSecurityRoleRefs().forEach(srr -> builder.addRoleLink(srr.getName(), srr.getLink()));
			}

			wabBatch.addServletModel(builder.build());
		});

		// 4. filters and their mappings
		Map<String, TreeMap<FilterModel, List<OsgiContextModel>>> allFilterModels = new HashMap<>();
		TreeMap<FilterModel, List<OsgiContextModel>> filterModels = new TreeMap<>();
		allFilterModels.put(contextPath, filterModels);
		final Map<String, List<FilterMap>> filterMappings = new HashMap<>();
		mainWebXml.getFilterMappings().forEach(fm -> {
			filterMappings.computeIfAbsent(fm.getFilterName(), n -> new LinkedList<>()).add(fm);
		});
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
			FilterModel.Builder builder = new FilterModel.Builder()
					.withRegisteringBundle(bundle)
					.withFilterName(fn)
					.withFilterClass(filterClass)
					.withAsyncSupported("true".equals(def.getAsyncSupported()))
					.withInitParams(def.getParameterMap())
					.withOsgiContextModel(ocm);
			FilterModel fm = builder.build();

			for (FilterMap map : filterMappings.get(fn)) {
				FilterModel.Mapping m = new FilterModel.Mapping();
				String[] dtn = map.getDispatcherNames();
				if (dtn == null || dtn.length == 0) {
					m.setDispatcherTypes(new DispatcherType[] { DispatcherType.REQUEST });
				} else {
					m.setDispatcherTypes(Arrays.stream(dtn)
							.map(name -> DispatcherType.valueOf(name.toUpperCase(Locale.ROOT)))
							.toArray(DispatcherType[]::new));
				}
				m.setServletNames(map.getServletNames());
				m.setUrlPatterns(map.getURLPatterns());
				fm.getMappingsPerDispatcherTypes().add(m);
			}

			if (filterClass != null) {
				// scan for annotations
				collectDeclaredRoles(filterClass, securityConfiguration);
			}

			filterModels.put(fm, null);
			// this is for Server/Service model
			wabBatch.addFilterModel(fm);
		});
		if (filterModels.size() > 0) {
			// this is for ServerController
			wabBatch.updateFilters(allFilterModels, false);
		}

		// 5. listeners
		for (String listener : mainWebXml.getListeners()) {
			Class<EventListener> listenerClass;
			try {
				listenerClass = (Class<EventListener>) classLoader.loadClass(listener);
				EventListener eventListener = listenerClass.newInstance();
				EventListenerModel elm = new EventListenerModel(eventListener);
				elm.setRegisteringBundle(bundle);
				elm.addContextModel(ocm);

				// scan for annotations
				collectDeclaredRoles(listenerClass, securityConfiguration);

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
		if (epModels.size() > 0) {
			// this is for ServerController
			wabBatch.updateErrorPages(allEpModels);
		}

		// 8. At the end, Tomcat adds SCIs to the context
		this.sciToHt.forEach((sci, classes) -> {
			Class<?>[] classesArray = classes.isEmpty() ? null : classes.toArray(new Class<?>[0]);
			ContainerInitializerModel cim = new ContainerInitializerModel(sci, classesArray);
			cim.setRegisteringBundle(bundle);
			cim.addContextModel(ocm);
			// we add this SCI even if it _may_ be targetted at different runtime. For example Undertow's websockets
			// bundle may be available, but the target runtime is Tomcat or Jetty. We will fitler out such SCI
			// in the ServerController itself later.
			wabBatch.addContainerInitializerModel(cim);
		});

		wabBatch.commitTransaction(contextPath);

		this.batch = wabBatch;
	}

	private void collectDeclaredRoles(Class<?> clazz, SecurityConfigurationModel securityConfiguration) {
		DeclareRoles dr = clazz.getAnnotation(DeclareRoles.class);
		if (dr != null && dr.value() != null) {
			for (String role : dr.value()) {
				securityConfiguration.getSecurityRoles().add(role);
			}
		}
	}

	// --- Web Application model access

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public State getDeploymentState() {
		return deploymentState.get();
	}

	/**
	 * Reporting method for the purpose of {@link org.ops4j.pax.web.service.spi.model.views.ReportWebContainerView}.
	 * We never want to return any object that's actually used for deployment and request processing, so simply
	 * each {@link BundleWebApplication} can return a report about itself.
	 * @return
	 */
	public WebApplicationInfo asWebApplicationModel() {
		WebApplicationInfo model = new WebApplicationInfo(allocatedOsgiContextModel);
		model.setBundle(bundle);
		model.setWab(true);
		model.setDeploymentState(deploymentState.get().getStateName());
		model.setContextPath(contextPath);
		model.getServletContainerInitializers().addAll(sciToHt.keySet().stream()
				.map(sci -> sci.getClass().getName()).collect(Collectors.toList()));
		model.getMetaInfResources().addAll(metainfResourceRoots.values());
		model.getDescriptors().addAll(serverSpecificDescriptors);
		// non-JAR entries of the Bundle-ClassPath
		URL[] urls = ClassPathUtil.getClassPathNonJars(bundle);
		for (URL url : urls) {
			model.getWabClassPath().add(url);
		}
		model.getWabClassPath().addAll(classSpace.getWabClassPath());
		model.getContainerFragmentBundles().addAll(classSpace.getContainerFragmentBundles());
		model.getApplicationFragmentBundles().addAll(classSpace.getApplicationFragmentBundles().keySet());

		return model;
	}

	// --- utility methods

	/**
	 * An internal state of the {@link BundleWebApplication} - there are more such states than
	 * {@link WebApplicationEvent.State events} related to {@link BundleWebApplication} lifecycle. These states
	 * are not directly communicated to observers, but are used to control the lifecycle of the web application.
	 */
	public enum State {
		/** Initial state after BundleWebApplication has just been created */
		UNCONFIGURED("Not configured"),

		/** State, where parsing of web.xml, fragments and annotations are processed */
		CONFIGURING("Configuring"),

		/** State after web configuration is ready, but we have to check if the target context (path) is free */
		ALLOCATING_CONTEXT("Allocating"),

		/**
		 * State in which we're actually registering web elements from web.xml/web-fragment.xml/annotations into
		 * allocated context {@link ServletContext context}.
		 */
		DEPLOYING("Deploying"),

		/** Final state - completely and successfully deployed application */
		DEPLOYED("Deployed"),

		/** State where {@link WebContainer} reference is needed, but it's not available */
		WAITING_FOR_WEB_CONTAINER("Awaiting registration"),

		/**
		 * <p>State after web configuration is ready, but simply the target context path is used by another WAB
		 * (or Whiteboard/HttpService context).</p>
		 *
		 * <p>Before Pax Web 8, "WAITING" state was used when a WAB was held due to conflict with existing WAB that
		 * used the same context path (and virtual hosts). Now we have more specialized state and "WAITING" is no
		 * longer used</p>
		 */
		WAITING_FOR_CONTEXT("Awaiting context"),

		/** The WAB is in the process of undeployment */
		UNDEPLOYING("Undeploying"),

		/** The WAB is fully undeployed */
		UNDEPLOYED("Undeployed"),

		/**
		 * Failed beyond any repair. This state is used after parsing errors or other validation errors. FAILED
		 * {@link BundleWebApplication} will never be attempted to be deployed again. For a case where WAB is
		 * waiting for a context path to be <em>free</em>, see {@link State#WAITING_FOR_CONTEXT}.
		 */
		FAILED("Failed");

		private final String stateName;

		State(String stateName) {
			this.stateName = stateName;
		}

		public String getStateName() {
			return stateName;
		}
	}

//	/**
//	 * Virtual Host List.
//	 */
//	private final List<String> virtualHostList;
//	/**
//	 * Connectors List
//	 */
//	private final List<String> connectorList;
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
