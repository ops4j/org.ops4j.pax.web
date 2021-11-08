/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.extender.war.internal;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.utils.extender.Extension;
import org.apache.tomcat.util.descriptor.web.ServletDef;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.ops4j.pax.web.extender.war.internal.model.BundleWebApplication;
import org.ops4j.pax.web.service.PaxWebConstants;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.util.WebContainerListener;
import org.ops4j.pax.web.service.spi.util.WebContainerManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Similarly to {@code org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext}, this
 * <em>extender context</em> manages interaction between Bundles converted into <em>web applications</em> and
 * dynamically available {@link org.ops4j.pax.web.service.WebContainer} service, where all the web components and
 * web contexts may be installed/registered.</p>
 *
 * <p>This class is the main class of OSGi CMPN R7 128 "Web Applications Specification".</p>
 *
 * <p>Differently than with {@code org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext}, here we
 * have two thread pools configured - one inside {@link WebContainerManager} and one from
 * {@code org.apache.felix.utils.extender.AbstractExtender#executors} where WAB extensions are processed. The
 * pool from Felix extender is used to process lifecycle stages of {@link BundleWebApplication} in a similar way
 * as Aries BlueprintContainers.</p>
 */
public class WarExtenderContext implements WebContainerListener {

	// even the structure of the fields attempts to match the structure of WhiteboardExtenderContext

	private static final Logger LOG = LoggerFactory.getLogger(WarExtenderContext.class);

	private final BundleContext bundleContext;

	/** This is were the lifecycle of {@link WebContainer} is managed. */
	private final WebContainerManager webContainerManager;

	/** Mapping between a {@link Bundle} and a {@link BundleWebApplication} created from the bundle. */
	private final Map<Bundle, BundleWebApplication> webApplications = new HashMap<>();

	/**
	 * ContextPath mapped lists of bundles awaiting allocation of their
	 * {@link PaxWebConstants#CONTEXT_PATH_HEADER context path}
	 */
	private final Map<String, List<Bundle>> webApplicationQueue = new HashMap<>();

	/**
	 * This lock prevents concurrent access to a list of {@link #webApplications} and {@link #webApplicationQueue}.
	 * This is similar to how whiteboard extender context manages a list of <em>bundle applications</em>.
	 */
	private final Lock lock = new ReentrantLock();

	/**
	 * The same pool which is used for entire pax-web-extender-war, to progress through the lifecycle of the
	 * {@link BundleWebApplication}.
	 */
	private final ExecutorService pool;

	/** Used to send events related to entire Web Applications being installed/uninstalled. */
	private final WebApplicationEventDispatcher webApplicationEventDispatcher;

	/** Default, common foundation of all WABs - includes default (override'able) servlet and some welcome files */
	private final WebXml defaultWebXml;

	private final WabConflictListener wabConflictListener;

//				private final ServiceRegistration<WarManager> registration;

	/**
	 * Construct a {@link WarExtenderContext} with asynchronous (production) {@link WebContainerManager}
	 * @param bundleContext
	 * @param pool
	 */
	public WarExtenderContext(BundleContext bundleContext, ExecutorService pool) {
		this(bundleContext, pool, false);
	}

	/**
	 * Full constructor of {@link WarExtenderContext}
	 * @param bundleContext
	 * @param pool
	 * @param synchronous whether the embedded {@link WebContainerManager} should be synchronous (which is useful
	 *        for testing).
	 */
	public WarExtenderContext(BundleContext bundleContext, ExecutorService pool, boolean synchronous) {
		this.bundleContext = bundleContext;
		this.pool = pool;

		// dispatcher of events related to WAB lifecycle (128.5 Events)
		webApplicationEventDispatcher = new WebApplicationEventDispatcher(bundleContext);

		defaultWebXml = findDefaultWebXml();

//		registration = bundleContext.registerService(
//				WarManager.class, webObserver,
//				new Hashtable<>());

		webContainerManager = synchronous
				? new WebContainerManager(bundleContext, this)
				: new WebContainerManager(bundleContext, this, "HttpService->WarExtender");
		webContainerManager.initialize();

		wabConflictListener = new WabConflictListener();
		webApplicationEventDispatcher.getListeners().add(wabConflictListener);
	}

	/**
	 * Cleans up everything related to pax-web-extender-war
	 */
	public void shutdown() {
//		if (webApplicationEventDispatcher != null) {
//			webApplicationEventDispatcher.destroy();
//			webApplicationEventDispatcher = null;
//		}
//
//		if (registration != null) {
//			registration.unregister();
//			registration = null;
//		}

		webContainerManager.shutdown();
		webApplicationEventDispatcher.getListeners().remove(wabConflictListener);
	}

	/**
	 * Send a {@link BundleWebApplication} related event.
	 * @param event
	 */
	public void sendWebEvent(WebApplicationEvent event) {
		webApplicationEventDispatcher.webEvent(event);
	}

	/**
	 * A method supporting {@code AbstractExtender#doCreateExtension(org.osgi.framework.Bundle)} for WAR
	 * publishing purposes.
	 *
	 * @param bundle
	 * @return
	 */
	public Extension createExtension(Bundle bundle, Runnable cleanup) {
		if (bundle.getState() != Bundle.ACTIVE) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Ignoring a bundle {} in non-active state", bundle);
			}
			return null;
		}

		String context = Utils.getManifestHeader(bundle, PaxWebConstants.CONTEXT_PATH_HEADER);
		if (context == null) {
			return null;
		}

		if (!context.startsWith("/") || (!"/".equals(context) && context.endsWith("/"))) {
			LOG.warn("{} manifest header of {} specifies invalid context path: {}. This bundle will not be processed.",
					PaxWebConstants.CONTEXT_PATH_HEADER, bundle, context);
			return null;
		}

		// before Pax Web 8 there was a check whether the bundle can see javax.servlet.Servlet class. But
		// that's too harsh requirement, because a WAR can be used to serve static content only, without
		// registration of any servlet (at least explicitly - as the "default" server should be added for free)

		BundleWebApplication webApplication = new BundleWebApplication(bundle, webContainerManager, this, pool);
		webApplication.setContextPath(context);
		ServiceReference<WebContainer> ref = webContainerManager.currentWebContainerReference();
		if (ref != null) {
			// let's pass WebContainer reference if it's already available
			webApplication.webContainerAdded(ref);
		}
		lock.lock();
		try {
			webApplications.put(bundle, webApplication);
		} finally {
			lock.unlock();
		}

		// before Pax Web 8, the web application was immediately parsed (which is costly operation) and the WebApp
		// immediately entered DEPLOYING state
		// after the parsing was done, the WebApp was added to DefaultWebAppDependencyManager, where it was
		// given its own tracker of HttpService and when the association (WebApp + HttpService) was available,
		// WebAppDependencyHolder service was registered (a wrapper of a WebApp + HttpService), which in turn was
		// checked by WebAppPublisher during publish() called from the Extension.start().
		// publish() ... didn't publish the WebApp, but set up another tracker for WebAppDependencyHolder services,
		// and only when those were available (when notification arrived to WebAppDependencyListener), a final
		// "visitor" was passed to the WebApp, so the (previously parsed) web elements were registered in
		// a WebContainer
		// TL;DR - too many OSGi services were involved

		// returned extension will be started _asynchronously_. Before Pax Web 8, there were 3 hardcoded threads
		// available in the pax-web-extender-war that handled the above workflow. When HttpService was immediately
		// available, everything:
		//  - adding a WebApp to relevant vhost -> contextName -> list<WebApp>
		//  - WebAppDependencyListener getting notified about the availability of WebAppDependencyHolder (because
		//    it was registered before starting the extension in DefaultWebAppDependencyManager.addWebApp())
		//  - passing RegisterWebAppVisitorWC to a WebApp
		//  - interaction with WebContainer through visit() methods of the visitor
		// happened in the same extender (1 of 3) thread.
		//
		// However parsing of the web.xml (and sending of the DEPLOYING event) happened in a thread calling
		// BundleTrackerCustomizer.modifiedBundle() of the extender, which was usually the FelixStartLevel thread.
		//
		// Pax Web 8 uses original thread notifying about a bundle event only to create the extension, while
		// parsing of web.xml and folowing steps are performed in one of the extender threads from configurable
		// ExecutorService
		// Remember - the process (if we have more WABs to process) is not fully parallel, as it's synchronized
		// using pax-web-config thread (from pax-web-runtime) anyway - to interact with single ServerModel in
		// synchronized and consistent way. At least the parsing can be done in parallel

		return new WabExtension(bundle, cleanup);
	}

	@Override
	public void webContainerChanged(ServiceReference<WebContainer> oldReference, ServiceReference<WebContainer> newReference) {
		// having received new reference to WebContainer service, we can finally move forward with registration
		// of the bundles which are found to be WABs
		if (oldReference != null) {
			webContainerRemoved(oldReference);
		}
		if (newReference != null) {
			webContainerAdded(newReference);
		}
	}

	@Override
	public void bundleStopped(Bundle bundle) {
		// we don't have to handle this method from WebContainerListener, as the same is delivered through
		// extension.destroy()
	}

	// --- Handling registration/unregistration of target WebContainer, where we want to register WAB applications

	public void webContainerAdded(ServiceReference<WebContainer> ref) {
		lock.lock();
		try {
			// We're setting a "current" reference to a WebContainer service for each WAB
			// The only thing we can guarantee is that we're not setting a reference when it's already set - every
			// time a reference changes, we first have to unset previous reference (if exists), so the lifecycle
			// is consistent
			// It's easier in pax-web-extender-whiteboard, because there we just have to remember whether the element
			// was already registered (when valid reference was available since the BundleWhiteboardApplication was
			// created) or not. Here each BundleWebApplication maybe be at different stage of their lifecycle, e.g.,
			// the web.xml parsing is performed (which actually doesn't require any available WebContainer service)
			webApplications.values().forEach(wab -> wab.webContainerAdded(ref));
		} finally {
			lock.unlock();
		}
	}

	public void webContainerRemoved(ServiceReference<WebContainer> ref) {
		lock.lock();
		try {
			// as with webContainerAdded, we can't remove a reference when it's not set, but it's still true that
			// the WAB may be at any stage of its lifecycle
			webApplications.values().forEach(wab -> wab.webContainerRemoved(ref));
		} finally {
			lock.unlock();
		}

		webContainerManager.releaseContainer(bundleContext, ref);
	}

	/**
	 * Returns a new {@link WebXmlParser} on each call (SAX Parser safety).
	 * @return
	 */
	public WebXmlParser getParser() {
		WebXmlParser parser = new WebXmlParser(false, false, true);
		parser.setClassLoader(WebXmlParser.class.getClassLoader());
		return parser;
	}

	/**
	 * Returns default {@link WebXml} which should be prepared when the {@link WarExtenderContext} starts.
	 * @return
	 */
	public WebXml getDefaultWebXml() {
		return defaultWebXml;
	}

	/**
	 * Each {@link BundleWebApplication WAB} needs some default "web xml". Just as in Tomcat, which has some
	 * default configuration in {@code CATALINA_HOME/conf/web.xml}.
	 * @return
	 */
	public WebXml findDefaultWebXml() {
		// Global web.xml (in Tomcat it's CATALINA_HOME/conf/web.xml and we package it into pax-web-spi)

		// default-web.xml from Tomcat includes 5 important parts:
		// - "default" servlet + "/" mapping
		// - "jsp" servlet + ".jsp" and ".jspx" mappings
		// - huge list of mime mappings
		// - session timeout set to 30 (minutes)
		// - 3 welcome files: index.html, index.htm and index.jsp
		// MIME mappings are left untouched, but:
		// - "default" servlet should be replaced by container specific version and we can't use
		//   org.apache.catalina.servlets.DefaultServlet!
		// - "jsp" servlet should be replaced by container agnostic org.ops4j.pax.web.jsp.JspServlet and
		//   we have to be sure that pax-web-jsp bundle is available

		WebXml defaultWebXml = new WebXml();
		defaultWebXml.setDistributable(true);
		defaultWebXml.setOverridable(true);
		defaultWebXml.setAlwaysAddWelcomeFiles(false);
		defaultWebXml.setReplaceWelcomeFiles(true);
		try {
			URL defaultWebXmlURI = OsgiContextModel.class.getResource("/org/ops4j/pax/web/service/spi/model/default-web.xml");
			if (defaultWebXmlURI != null) {
				// it can be null when invoked from IDE without proper `mvn package`, which
				// repackages Tomcat's resources into pax-web-spi
				defaultWebXml.setURL(defaultWebXmlURI);
				getParser().parseWebXml(defaultWebXmlURI, defaultWebXml, false);
			}
		} catch (IOException e) {
			LOG.warn("Failure parsing default web.xml: {}", e.getMessage(), e);
		}

		// review the servlets
		for (Iterator<Map.Entry<String, ServletDef>> iterator = defaultWebXml.getServlets().entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<String, ServletDef> e = iterator.next();
			String name = e.getKey();
			ServletDef servlet = e.getValue();
			if ("default".equals(name)) {
				// which means it'll be replaced by container-specific "default" servlet.
				// null class will be changed to "resourceServlet" flag in ServletModel
				servlet.setServletClass(null);
			} else if ("jsp".equals(name)) {
				if (Utils.getPaxWebJspBundle(bundleContext.getBundle()) != null) {
					// change org.apache.jasper.servlet.JspServlet to org.ops4j.pax.web.jsp.JspServlet
					servlet.setServletClass(PaxWebConstants.DEFAULT_JSP_SERVLET_CLASS);
				} else {
					// no support for JSP == no JSP servlet at all
					iterator.remove();
					// no JSP servlet mapping
					defaultWebXml.getServletMappings().entrySet().removeIf(esm -> esm.getValue().equals("jsp"));
					// and no JSP welcome file
					defaultWebXml.getWelcomeFiles().remove("index.jsp");
				}
			}
		}

		return defaultWebXml;
	}

	/**
	 * Each WAB calls this method to prepare its own {@link WebXml}
	 * @param bundle
	 * @return
	 */
	public WebXml findBundleWebXml(Bundle bundle) {
		WebXml mainWebXml = null;

		// WAB specific web.xml. 128.3.1 WAB Definition - This web.xml must be found with the Bundle
		// findEntries() method at the path /WEB-INF/web.xml. The findEntries() method includes fragments,
		// allowing the web.xml to be provided by a fragment.
		// ClassPathUtil is not used - we don't want web.xml entries from wired bundles.
		// I don't expect multiple web.xmls, but in theory it's possible.
		Enumeration<URL> descriptors = bundle.findEntries("WEB-INF", "web.xml", false);
		if (descriptors != null) {
			while (descriptors.hasMoreElements()) {
				URL next = descriptors.nextElement();
				LOG.trace("Processing {}", next);

				WebXml webXml = new WebXml();
				webXml.setURL(next);
				try {
					if (!getParser().parseWebXml(next, webXml, false)) {
						webXml = null;
					}
				} catch (IOException e) {
					LOG.warn("Failure parsing web.xml for bundle {}: {}", bundle, e.getMessage(), e);
				}
				if (mainWebXml == null) {
					mainWebXml = webXml;
				} else {
					mainWebXml.merge(Collections.singleton(webXml));
				}
			}
		}
		if (mainWebXml == null) {
			// if it was empty, we still need something to merge scanned fragments into
			mainWebXml = new WebXml();
		}

		return mainWebXml;
	}

	/**
	 * This method returns a collection of bundle (WAB) ids which all use the same context path. The returned
	 * collection returns all awaiting WABs' IDs, the passed WAB's ID and the ID of the WAB that successfully
	 * <em>allocated</em> the context path.
	 *
	 * @param contextPath
	 * @param wab
	 * @return
	 */
	public Collection<Long> calculateCollisionIds(String contextPath, Bundle wab) {
		Collection<Long> ids = new HashSet<>();

		ids.add(wab.getBundleId());

		lock.lock();
		try {
			List<Bundle> awaiting = webApplicationQueue.get(contextPath);
			if (awaiting != null) {
				awaiting.forEach(w -> ids.add(w.getBundleId()));
			}
			this.webApplications.forEach((w, bwa) -> {
				if (contextPath.equals(bwa.getContextPath())) {
					ids.add(w.getBundleId());
				}
			});
		} finally {
			lock.unlock();
		}

		return ids;
	}

	/**
	 * <p>The {@link Extension} representing a "WAB" (Web Application Bundle) which (according to 128.3 Web Application
	 * Bundle) is a {@link Bundle} with {@code Web-ContextPath} manifest header.</p>
	 *
	 * <p>No other constraints are put onto the WAB (like visibility of {@link javax.servlet.Servlet} class or the
	 * content). Also it doesn't matter whether the bundle originally contained the required manifest header or
	 * the header was applied using some URL handler (like pax-url-war).</p>
	 *
	 * <p>We will use this class only as a wrapper of {@link BundleWebApplication} (pointed to by a {@link Bundle}) and the
	 * responsibility of this extension is only to react to {@link Extension#start()} and {@link Extension#destroy()}
	 * methods. The lifecycle of the actual WAB will be managed through {@link BundleWebApplication} contract.</p>
	 */
	private class WabExtension implements Extension {

		private final Bundle bundle;
		private final Runnable cleanup;

		WabExtension(Bundle bundle, Runnable cleanup) {
			this.bundle = bundle;
			this.cleanup = cleanup;
		}

		@Override
		public void start() throws Exception {
			// start() method is called within a thread of the war extender pool (that's explicit configuration
			// in pax-web-extender-war)

			String name = Thread.currentThread().getName();

			try {
				BundleWebApplication webApp;
				lock.lock();
				try {
					webApp = webApplications.get(bundle);
					if (webApp == null) {
						return;
					}
					// Pax Web before version 8 checked non-standard "Webapp-Deploy" manifest header. I don't think it's
					// necessary now
				} finally {
					lock.unlock();
				}

				long id = bundle.getBundleId();
				Thread.currentThread().setName(name + " (" + webApp.getContextPath() + " [" + id + "])");

				// Aries Blueprint container may be processed by several reschedules of "this" into a configured
				// thread pool. Rescheduling happens at some stages of Blueprint container lifecycle, when it has to
				// wait (for namespace handlers or mandatory services). But ideally, when everything is available and
				// there's no need to wait, we should try doing everything in single thread.
				// starting a webapp immediately (in current thread) will process with parsing and deployment. Only
				// if the WebContainer reference is not available, we'll reschedule the lifecycle processing
				webApp.start();
			} finally {
				Thread.currentThread().setName(name);
			}
		}

		@Override
		public void destroy() throws Exception {
			// destroy() method is called outside of the pax-web-extender-war pool - usually when the bundle of the
			// webapp is stopped (in FelixStartLevel thread or e.g., in Karaf Shell thread) or the bundle of
			// pax-web-extender-war itself is stopped
			// 128.3.8 "Stopping the Web Application Bundle" says this explicitly:
			//     This undeploying must occur synchronously with the WAB's stopping event

			BundleWebApplication webApp = null;
			lock.lock();
			try {
				if (bundle.getState() == Bundle.UNINSTALLED) {
					// the bundle has be uninstalled and can't be started again. We'll never start the associated WAB
					webApp = webApplications.remove(bundle);
				} else if (bundle.getState() == Bundle.STOPPING || bundle.getState() == Bundle.RESOLVED) {
					// the bundle is or has stopped, but it can be started again - in theory we could start the
					// associated WAB again with already parsed web.xml (and web fragments and annotated classes),
					// but the bundle may have stopped because it's being refreshed or new bundle fragments
					// were attached. So it's wiser and safer to start from scratch. So use remove() and not get().
//					webApp = webApplications.get(bundle);
					webApp = webApplications.remove(bundle);
				}
				if (webApp == null) {
					return;
				}

				// Pax Web before 8 interacted here with dependency manager in order to tell the WebApp to unregister
				// a WebAppDependencyHolder registration. But we're handling the lifecycle in different way now.
				webApp.stop();
			} finally {
				if (cleanup != null) {
					cleanup.run();
				}
				lock.unlock();
			}
		}
	}

	/**
	 * A listener that can re-schedule WAB deployments when other WABs' state changes.
	 */
	private class WabConflictListener implements WebApplicationEventListener {
		@Override
		public void webEvent(WebApplicationEvent event) {
			// according to extension JavaDoc (org.apache.felix.utils.extender.Extension.destroy), this method
			// is called for example in the thread that stops the bundle. We have to schedule the handler to another
			// thread
			pool.submit(new DeployTask(event));
		}
	}

	private class DeployTask implements Runnable {
		private final WebApplicationEvent event;

		private DeployTask(WebApplicationEvent event) {
			this.event = event;
		}

		/**
		 * Method called when {@link WebApplicationEvent} is received. We're interested in
		 * {@link WebApplicationEvent.State#UNDEPLOYED} events, because some WABs may have to be rescheduled.
		 */
		@Override
		public void run() {
			Bundle wab = event.getBundle();
			String contextPath = event.getContextPath();

			if (event.getType() == WebApplicationEvent.State.FAILED && event.isAwaitingAllocation()) {
				// some WAB has failed because it wasn't able to allocate the context - another WAB has
				// "taken" it earlier.
				LOG.info("Added {} to await-queue for {}", webApplications.get(wab), contextPath);
				webApplicationQueue.computeIfAbsent(contextPath, cp -> new LinkedList<>()).add(wab);
			}

			if (event.getType() == WebApplicationEvent.State.UNDEPLOYED) {
				if (webApplicationQueue.containsKey(contextPath)) {
					// in case it was awaiting the allocation
					webApplicationQueue.get(contextPath).remove(wab);
				}

				lock.lock();
				try {
					for (Iterator<Bundle> it = webApplicationQueue.get(contextPath).iterator(); it.hasNext(); ) {
						Bundle awaitingWab = it.next();
						if (awaitingWab != wab) {
							// a WAB for some contextPath was UNDEPLOYED, so next awaiting WAB can be deployed
							// (at least attempted to be deployed)
							BundleWebApplication app = webApplications.get(awaitingWab);
							LOG.info("Redeploying {}, because {} is now available", app, contextPath);
							app.deploy();
							it.remove();
							break;
						}
					}
				} finally {
					lock.unlock();
				}
			}
		}
	}

}
