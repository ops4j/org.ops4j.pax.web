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
package org.ops4j.pax.web.service.jetty.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.ops4j.pax.web.service.jetty.internal.web.RootBundleURLResource;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerKey;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.ops4j.pax.web.service.spi.util.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Pax Web specific {@link ServletContextHandler} representing single <em>web application</em>
 * deployed under unique <em>context path</em>, related 1:1 with single, unique
 * {@link ServletContextModel}
 */
public class PaxWebServletContextHandler extends ServletContextHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebServletContextHandler.class);

	// This collection will be ordered by rank/serviceId of the ContainerInitializerModels
	private final Collection<SCIWrapper> servletContainerInitializers = new TreeSet<>();

				private AccessControlContext accessControllerContext;
				private List<String> virtualHosts;

	private ServletContext osgiServletContext;

	/**
	 * This maps keeps all the listeners in order, as expected by OSGi CMPN R7 Whiteboard specification.
	 */
	private final Map<EventListenerKey, EventListener> rankedListeners = new TreeMap<>();

	/**
	 * Here we'll keep the listeners without associated {@link EventListenerModel}
	 */
	private final List<EventListener> orderedListeners = new ArrayList<>();

	/**
	 * A {@link ThreadLocal} value that helps us collect these attributes that are set by these SCIs that operate
	 * on Jetty's {@link org.eclipse.jetty.server.handler.ContextHandler} directly. These attributes
	 * have to be cleared when SCIs change or even when the context is restarted.
	 */
	private final ThreadLocal<Boolean> isCallingSCI = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/**
	 * The collected names of the attributes set directly to Jetty's {@link org.eclipse.jetty.server.handler.ContextHandler}
	 * which have to be cleared when the container is restarted
	 */
	private final Set<String> attributesToClearBeforeRestart = new HashSet<>();

	/**
	 * Create a slightly extended version of Jetty's {@link ServletContextHandler}. It is still not as complex as
	 * {@code org.eclipse.jetty.webapp.WebAppContext} which does all the sort of XML/annotation configuration, but
	 * we take some of the mechanisms from {@code WebAppContext} if they're useful in Pax Web.
	 *
	 * @param parent
	 * @param contextPath
	 * @param configuration
	 */
	public PaxWebServletContextHandler(HandlerContainer parent, String contextPath, Configuration configuration) {
		super(parent, contextPath, true, true);

		// TCCL of sessionManager timer threads will be set to thread of pax-web-jetty bundle, not to current TCCL
		ScheduledExecutorScheduler executorScheduler = new ScheduledExecutorScheduler(getSessionHandler().toString() + "Timer", true,
				getClass().getClassLoader());
		_scontext.setAttribute("org.eclipse.jetty.server.session.timer", executorScheduler);

		// need to initialize the logger as super doStart is too late already
		setLogger(Log.getLogger(getDisplayName() == null ? getContextPath() : getDisplayName()));

		// "128.3.5 Static Content" is the only place where protected directories are mentioned. We'll handle them
		// at request processing stage and configure here
		setProtectedTargets(new String[] { "/WEB-INF", "/META-INF", "/OSGI-INF", "/OSGI-OPT" });
	}

	/**
	 * Helper method to be used from {@link org.ops4j.pax.web.service.jetty.internal.web.JettyResourceServlet}
	 * and from {@link #getResource(String)}
	 *
	 * @param url
	 * @return
	 * @throws MalformedURLException
	 */
	public static Resource toJettyResource(URL url) throws MalformedURLException {
		// we have to check if the URL points to the root of the bundle. Felix throws IOException
		// when opening connection for URIs like "bundle://22.0:1/"
		if (url != null) {
			if ("bundle".equals(url.getProtocol()) || "bundleentry".equals(url.getProtocol())) {
				if ("/".equals(url.getPath())) {
					// Felix, root of the bundle - return a resource which says it's a directory
					return new RootBundleURLResource(Resource.newResource(url));
				} else if (!url.getPath().endsWith("/")) {
					// unfortunately, due to https://issues.apache.org/jira/browse/FELIX-6294
					// we have to check ourselves if it's a directory and possibly append a slash
					// just as org.eclipse.osgi.storage.bundlefile.BundleFile#fixTrailingSlash() does it
					try (Resource potentialDirectory = Resource.newResource(url)) {
						if (potentialDirectory.exists() && potentialDirectory.length() == 0) {
							URL fixedURL = new URL(url.toExternalForm() + "/");
							Resource properDirectory = Resource.newResource(fixedURL);
							if (properDirectory.exists()) {
								return properDirectory;
							}
						}
					}
				}
			}
		}

		// resource can be provided by custom HttpContext/ServletContextHelper, so we can't really
		// affect lastModified for caching purposes
		return Resource.newResource(url);
	}


	public void setServletContainerInitializers(Collection<SCIWrapper> wrappers) {
		this.servletContainerInitializers.clear();
		this.servletContainerInitializers.addAll(wrappers);
	}

	/**
	 * We have to ensure that this {@link org.eclipse.jetty.server.handler.ContextHandler} will always return
	 * proper instance of {@link javax.servlet.ServletContext} - especially in the events passed to listeners
	 * @param osgiServletContext
	 */
	public void setOsgiServletContext(ServletContext osgiServletContext) {
		this.osgiServletContext = osgiServletContext;
	}

	@Override
	public void callContextInitialized(ServletContextListener l, ServletContextEvent e) {
		super.callContextInitialized(l, new ServletContextEvent(osgiServletContext));
	}

	@Override
	public void callContextDestroyed(ServletContextListener l, ServletContextEvent e) {
		super.callContextDestroyed(l, new ServletContextEvent(osgiServletContext));
	}

	@Override
	public void addEventListener(EventListener listener) {
		// called for example by
		// org.eclipse.jetty.websocket.server.NativeWebSocketServletContainerInitializer.configure()
		// so we should treat it as a listener added with rank=0
		addEventListener(null, listener);
	}

	/**
	 * Special {@code addEventListener()} that should be called instead of {@link #addEventListener(EventListener)},
	 * because we want to sort the listeners according to Whiteboard/ranking rules.
	 * @param model
	 * @param listener
	 */
	public void addEventListener(EventListenerModel model, EventListener listener) {
		// we are not adding the listener to org.eclipse.jetty.server.handler.ContextHandler._eventListeners
		// now - we'll add them just before the context is started, so we're sure that the order is correct.
		// this is especially important for ServletContextListeners

		if (model == null || model.isDynamic()) {
			orderedListeners.add(listener);
		} else {
			rankedListeners.put(EventListenerKey.ofModel(model), listener);
		}

		if (!ServletContextListener.class.isAssignableFrom(listener.getClass())) {
			// otherwise it'll be added anyway when context is started, because such listener can
			// be added only for stopped context
			if (isStarted()) {
				// we have to add it, because there'll be no restart
				super.addEventListener(listener);
			}
		}
	}

	@Override
	protected void addProgrammaticListener(EventListener listener) {
		// we have to hijack this method and add the listener later in correct order
		orderedListeners.add(listener);
	}

	@Override
	public void removeEventListener(EventListener listener) {
		removeEventListener(null, listener);
	}

	/**
	 * Special {@code removeEventListener()} that manages the ordering of the listeners.
	 * @param model
	 * @param listener
	 */
	public void removeEventListener(EventListenerModel model, EventListener listener) {
		if (model == null || model.isDynamic()) {
			orderedListeners.remove(listener);
		} else {
			rankedListeners.remove(EventListenerKey.ofModel(model));
		}
	}

	@Override
	protected void startContext() throws Exception {
		// there are no org.eclipse.jetty.servlet.ServletContextHandler.ServletContainerInitializerCaller beans
		// because we manage SCIs ourselves

		for (String name : attributesToClearBeforeRestart) {
			removeAttribute(name);
		}
		attributesToClearBeforeRestart.clear();

		servletContainerInitializers.forEach(wrapper -> {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				getServletContext().setExtendedListenerTypes(true);
				Thread.currentThread().setContextClassLoader(getClassLoader());
				isCallingSCI.set(true);
				wrapper.onStartup();
			} catch (ServletException e) {
				LOG.error(e.getMessage(), e);
			} finally {
				isCallingSCI.remove();
				Thread.currentThread().setContextClassLoader(tccl);
				getServletContext().setExtendedListenerTypes(false);
			}
		});

		// SCIs may have added some listeners which we've hijacked, to order them according
		// to Whiteboard/ranking rules. Now it's perfect time to add them in correct order
		for (int pos = 0; pos < orderedListeners.size(); pos++) {
			EventListener el = orderedListeners.get(pos);
			rankedListeners.put(EventListenerKey.ofPosition(pos), el);
		}

		for (EventListener el : rankedListeners.values()) {
			super.addEventListener(el);
		}

		// this method will start the just added listeners in the order we wanted
		super.startContext();
	}

	@Override
	public void setAttribute(String name, Object value) {
		if (isCallingSCI.get()) {
			attributesToClearBeforeRestart.add(name);
		}
		super.setAttribute(name, value);
	}

	@Override
	protected void doStart() throws Exception {
		// 1. Pax Web 7 was explicitly adding org.ops4j.pax.web.jsp.JasperInitializer here, but we're no longer doing it
		//    WebAppContext in Jetty uses org.eclipse.jetty.webapp.JspConfiguration for this purpose

		// 2. Pax Web 7 was sorting the initializers according to arbitrary rules - we don't have to do it in Pax Web 8

		// 3. Call the initializers - in startContext()

		// 4. Virtual Host/Connector handling - in JettyServerWrapper.ensureServletContextStarted()

		// 5. jetty-web.xml are already handled

		// 6. Pax Web 7 was setting the attributes in real ServletContext, but we already do it according to
		//    Whiteboard specification at OsgiServletContext level

		// 7. Do super work
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		// setEventListeners() method is called during doStop(), existing, durable listeners are added again, but
		// then durable listeners are cleared, so the "preserved" listener will be lost next time
		// TODO: file a Github issue for eclipse/jetty-project

		// doStop() will do a lot of work, but among others, it'll clear durable listeners.
		super.doStop();

		// 2021-08-25: because we started keeping listeners ordered, we just need to clean all
		// the listeners, which will be added back when the context is started
		setEventListeners(new EventListener[0]);
		getSessionHandler().clearEventListeners();

		// remove the listeners without associated EventListenerModel from rankedListeners map
		rankedListeners.entrySet().removeIf(e -> e.getKey().getRanklessPosition() >= 0);
		// ALL listeners added without a model (listeners added by SCIs and other listeners) will be cleared
		orderedListeners.clear();
	}

	@Override
	protected void requestInitialized(Request baseRequest, HttpServletRequest request) {
		// a little trick, so we can give the client a single JSESSIONID, but internally, we can use it to access
		// different OsgiContextModel-related session for a context
		// This method is a good place to do it - ServletHandler is too late and
		// org.eclipse.jetty.server.Request.getUserIdentityScope() is already available here.
		// We have to do it in first scoped handler's handle() method, because Keycloak accesses the session
		// in org.keycloak.adapters.jetty.Jetty94RequestAuthenticator before ServletHandler is called

		if (getSessionHandler() instanceof PaxWebSessionHandler) {
			PaxWebSessionHandler sessionHandler = (PaxWebSessionHandler) getSessionHandler();
			String sid = baseRequest.getRequestedSessionId();
			if (sid != null && baseRequest.getSession(false) == null) {
				String baseSid = sessionHandler.getSessionIdManager().getId(sid);
				baseSid += PaxWebSessionIdManager.getSessionIdSuffix(baseRequest);
				sid = sessionHandler.getSessionIdManager().getExtendedId(baseSid, request);
				HttpSession session = sessionHandler.getHttpSession(sid);

				if (session != null && sessionHandler.isValid(session)) {
					baseRequest.enterSession(session);
					baseRequest.setSession(session);
				}
			}
		}

		super.requestInitialized(baseRequest, request);
	}

	/**
	 * Special override for libraries using {@link ContextHandler#getCurrentContext()} directly. It should eventually
	 * delegate to {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext#getResource(String)}.
	 * @param path
	 * @return
	 * @throws MalformedURLException
	 */
	@Override
	public Resource getResource(String path) throws MalformedURLException {
		String childPath = Path.securePath(path);
		if (childPath == null) {
			return null;
		}
		if (childPath.startsWith("/")) {
			childPath = childPath.substring(1);
		}

		URL url = osgiServletContext.getResource("/" + childPath);
		Resource resource = toJettyResource(url);
		if (resource == null) {
			return super.getResource(path);
		}
		return resource;
	}

	@Override
	protected SessionHandler newSessionHandler() {
		return new PaxWebSessionHandler();
	}

}
