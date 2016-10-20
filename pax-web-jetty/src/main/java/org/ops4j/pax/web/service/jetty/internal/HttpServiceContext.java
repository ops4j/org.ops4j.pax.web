/*  Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.jetty.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.server.session.JDBCSessionIdManager;
import org.eclipse.jetty.server.session.JDBCSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.jetty.internal.util.DOMJettyWebXmlParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceContext extends ServletContextHandler {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceContext.class);

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	/**
	 * Context attributes.
	 */
	private final Map<String, Object> attributes;
	private final HttpContext httpContext;
	/**
	 * Access controller context of the bundle that registred the http context.
	 */
	private final AccessControlContext accessControllerContext;

	private final Map<ServletContainerInitializer, Set<Class<?>>> servletContainerInitializers;

	private final URL jettyWebXmlURL;

	private final List<String> virtualHosts;

	private final AtomicReference<ServiceRegistration<ServletContext>> registration
            = new AtomicReference<ServiceRegistration<ServletContext>>();

	HttpServiceContext(
			final HandlerContainer parent,
			final Map<String, String> initParams,
			final Map<String, Object> attributes,
			final String contextName,
			final HttpContext httpContext,
			final AccessControlContext accessControllerContext,
			final Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers,
			URL jettyWebXmlUrl, List<String> virtualHosts) {
		super(parent, "/" + contextName, SESSIONS | SECURITY);
		LOG.info("registering context {}, with context-name: {}", httpContext,
				contextName);
		getInitParams().putAll(initParams);
		this.attributes = attributes;
		this.httpContext = httpContext;
		this.accessControllerContext = accessControllerContext;
		setDisplayName(httpContext.toString());
		this.servletContainerInitializers = containerInitializers != null ? containerInitializers
				: new HashMap<ServletContainerInitializer, Set<Class<?>>>();
		this.virtualHosts = new ArrayList<String>(virtualHosts);
		jettyWebXmlURL = jettyWebXmlUrl;

		_scontext = new SContext();

		// TCCL of sessionManager timer threads will be set to thread of pax-web-jetty bundle, not to current TCCL
		AbstractSessionManager sessionManager = (AbstractSessionManager) getSessionHandler().getSessionManager();
		ScheduledExecutorScheduler executorScheduler = new ScheduledExecutorScheduler(sessionManager.toString() + "Timer", true,
				getClass().getClassLoader());
		sessionManager.addBean(executorScheduler, true);
		_scontext.setAttribute("org.eclipse.jetty.server.session.timer", executorScheduler);

		setServletHandler(new HttpServiceServletHandler(httpContext));
		setErrorHandler(new ErrorPageErrorHandler());

	}

	public void registerService(BundleContext bundleContext, Dictionary<String, String> properties) {
        if (registration.get() == null) {
            ServiceRegistration<ServletContext> reg = bundleContext.registerService(
                    ServletContext.class,
                    getServletContext(),
                    properties
            );
            if (!registration.compareAndSet(null, reg)) {
                reg.unregister();
            }
            LOG.debug("ServletContext registered as service.");
        }
    }

    public void unregisterService() {
        ServiceRegistration<ServletContext> reg = registration.getAndSet(null);
        if (reg != null) {
            LOG.debug("ServletContext unregistered as service.");
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                LOG.info("ServletContext service already removed");
            }
        }
    }

    @Override
	protected void doStart() throws Exception {

    	//need to initialize the logger as super doStart is to late already
    	setLogger(Log.getLogger(getDisplayName() == null?getContextPath():getDisplayName()));

		// Special handling for JASPER
		if (isJspAvailable()) { // use JasperClassloader
			LOG.info("registering JasperInitializer");
			@SuppressWarnings("unchecked")
			Class<ServletContainerInitializer> loadClass = (Class<ServletContainerInitializer>) loadClass("org.ops4j.pax.web.jsp.JasperInitializer");
			servletContainerInitializers.put(loadClass.newInstance(),
					Collections.<Class<?>> emptySet());
		}
		//
		// if (isWebsocketAvailable()) {
		// LOG.info("registering WebSocketServerContainerInitializer");
		// @SuppressWarnings("unchecked")
		// Class<ServletContainerInitializer> loadClass =
		// (Class<ServletContainerInitializer>)
		// loadClass("org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer");
		// servletContainerInitializers.put(loadClass.newInstance(),
		// Collections.<Class<?>> emptySet());
		// }

		if (servletContainerInitializers != null) {

			for (final Entry<ServletContainerInitializer, Set<Class<?>>> entry : servletContainerInitializers
					.entrySet()) {


				try {
					ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
							new Callable<Void>() {

								@Override
								public Void call() throws IOException,
										ServletException {
									entry.getKey().onStartup(entry.getValue(),
											_scontext);

									return null;
								}

							});
					// CHECKSTYLE:OFF
				} catch (Exception e) {
					if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					}
					LOG.error("Ignored exception during listener registration",
							e);
				}

			}
		}

		this.setVirtualHosts(virtualHosts.toArray(EMPTY_STRING_ARRAY));
		if (jettyWebXmlURL != null) {

			try {
				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
						new Callable<Void>() {

							@Override
							public Void call() throws IOException {
								//do parsing and altering of webApp here
								DOMJettyWebXmlParser jettyWebXmlParser = new DOMJettyWebXmlParser();
								jettyWebXmlParser.parse(HttpServiceContext.this, jettyWebXmlURL.openStream());

								return null;
							}

						});
				//CHECKSTYLE:OFF
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				LOG.error("Ignored exception during listener registration", e);
			}
			//CHECKSTYLE:ON

		}

		if (attributes != null) {
			for (Map.Entry<String, ?> attribute : attributes.entrySet()) {
				_scontext
						.setAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		super.doStart();
		LOG.debug("Started servlet context for http context [" + httpContext
				+ "]");
	}

	private boolean isJspAvailable() {
		try {
			return (org.ops4j.pax.web.jsp.JspServletWrapper.class != null);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	@Override
	protected void doStop() throws Exception {
        unregisterService();
		super.doStop();
		LOG.debug("Stopped servlet context for http context [" + httpContext
				+ "]");
	}

	@Override
	public void doHandle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		LOG.debug("Handling request for [" + target + "] using http context ["
				+ httpContext + "]");
		super.doHandle(target, baseRequest, request, response);
	}

	@Override
	public void setEventListeners(final EventListener[] eventListeners) {
		if (_sessionHandler != null) {
			_sessionHandler.clearEventListeners();
		}

		super.setEventListeners(eventListeners);
		if (_sessionHandler != null) {
			for (int i = 0; eventListeners != null && i < eventListeners.length; i++) {
				EventListener listener = eventListeners[i];

				if ((listener instanceof HttpSessionActivationListener)
						|| (listener instanceof HttpSessionAttributeListener)
						|| (listener instanceof HttpSessionBindingListener)
						|| (listener instanceof HttpSessionListener)) {
					_sessionHandler.addEventListener(listener);
				}

			}
		}
	}

	/**
	 * If the listener is a servlet context listener and the context is already
	 * started, notify the servlet context listener about the fact that context
	 * is started. This has to be done separately as the listener could be added
	 * after the context is already started, case when servlet context listeners
	 * are not notified anymore.
	 *
	 * @param listener
	 *            to be notified.
	 */
	@Override
	public void addEventListener(final EventListener listener) {
		super.addEventListener(listener);
        if ((listener instanceof HttpSessionActivationListener)
            || (listener instanceof HttpSessionAttributeListener)
            || (listener instanceof HttpSessionBindingListener)
            || (listener instanceof HttpSessionListener))
        {
            if (_sessionHandler!=null)
                _sessionHandler.addEventListener(listener);
        }

	}

	@Override
	public void callContextInitialized(final ServletContextListener l,
			final ServletContextEvent e) {
		try {
			// toggle state of the dynamic API so that the listener cannot use
			// it
			if (isProgrammaticListener(l))
				this.getServletContext().setEnabled(false);

			if (LOG.isDebugEnabled())
				LOG.debug("contextInitialized: {}->{}", e, l);

			try {
				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
						new Callable<Void>() {

							@Override
							public Void call() {
								l.contextInitialized(e);
								return null;
							}

						});
				// CHECKSTYLE:OFF
			} catch (Exception ex) {
				if (ex instanceof RuntimeException) {
					throw (RuntimeException) ex;
				}
				LOG.error("Ignored exception during listener registration", e);
			}

		} finally {
			// untoggle the state of the dynamic API
			this.getServletContext().setEnabled(true);
		}
	}

	@Override
	public boolean isProtectedTarget(String target) {
		// Fixes PAXWEB-196 and PAXWEB-211
		//CHECKSTYLE:OFF
		while (target.startsWith("//")) {
			target = URIUtil.compactPath(target);
		}
		//CHECKSTYLE:ON

		return StringUtil.startsWithIgnoreCase(target, "/web-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/meta-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/osgi-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/osgi-opt");
	}

	@Override
	protected SessionHandler newSessionHandler() {
		Server server = getServer();
		SessionIdManager sessionIdManager = null;
		if (server != null) {
			sessionIdManager = server.getSessionIdManager();
		}
		if (sessionIdManager instanceof JDBCSessionIdManager) {
			LOG.debug(
					"Creating JDBCSessionManager for SessionIdManager {} and Server {}",
					sessionIdManager.getClass().getName(), server.getClass()
							.getName());
			JDBCSessionManager sessionManager = new JDBCSessionManager();
			sessionManager.setSessionIdManager(sessionIdManager);
			SessionHandler sessionHandler = new SessionHandler(sessionManager);
			sessionHandler.setServer(server);
			sessionManager.setSessionHandler(sessionHandler);
			return sessionHandler;
		} else {
			LateInvalidatingHashSessionManager sessionManager = new LateInvalidatingHashSessionManager();
			if (sessionIdManager != null) {
				LOG.debug(
						"Creating LateInvalidatingHashSessionManager for SessionIdManager {}",
						sessionIdManager.getClass().getName());
				sessionManager.setSessionIdManager(sessionIdManager);
			} else {
				LOG.debug("Creating default LateInvalidatingHashSessionManager, no SessionIdManager currently set");
			}
			return new SessionHandler(sessionManager);
		}
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("httpContext=").append(httpContext)
				.append("}").toString();
	}

	@Override
	public boolean addBean(Object o) {
		return super.addBean(o);
	}

	public class SContext extends ServletContextHandler.Context {

		@Override
		public String getRealPath(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting real path: [{}]", path);
			}

			URL resource = getResource(path);
			if (resource != null) {
				String protocol = resource.getProtocol();
				if (protocol.equals("file")) {
					String fileName = resource.getFile();
					if (fileName != null) {
						File file = new File(fileName);
						if (file.exists()) {
							String realPath = file.getAbsolutePath();
							LOG.debug("found real path: [{}]", realPath);
							return realPath;
						}
					}
				}
			}
			return null;
		}

		@Override
		public URL getResource(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting resource: [" + path + "]");
			}
			URL resource = null;

			// IMPROVEMENT start PAXWEB-314
			try {
				resource = new URL(path);
				LOG.debug("resource: [" + path
						+ "] is already a URL, returning");
				return resource;
			} catch (MalformedURLException e) {
				// do nothing, simply log
				LOG.debug("not a URL or invalid URL: [" + path
						+ "], treating as a file path");
			}
			// IMPROVEMENT end PAXWEB-314

			// FIX start PAXWEB-233
			final String p;
			if (path != null && path.endsWith("/") && path.length() > 1) {
				p = path.substring(0, path.length() - 1);
			} else {
				p = path;
			}
			// FIX end

			try {
				resource = AccessController.doPrivileged(
						new PrivilegedExceptionAction<URL>() {
							@Override
							public URL run() throws Exception {
								return httpContext.getResource(p);
							}
						}, accessControllerContext);
				if (LOG.isDebugEnabled()) {
					LOG.debug("found resource: " + resource);
				}
			} catch (PrivilegedActionException e) {
				LOG.warn("Unauthorized access: " + e.getMessage());
			}
			return resource;
		}

		@Override
		public InputStream getResourceAsStream(final String path) {
			final URL url = getResource(path);
			if (url != null) {
				try {
					return AccessController.doPrivileged(
							new PrivilegedExceptionAction<InputStream>() {
								@Override
								public InputStream run() throws Exception {
									try {
										return url.openStream();
									} catch (IOException e) {
										LOG.warn("URL canot be accessed: "
												+ e.getMessage());
									}
									return null;
								}

							}, accessControllerContext);
				} catch (PrivilegedActionException e) {
					LOG.warn("Unauthorized access: " + e.getMessage());
				}

			}
			return null;
		}

		/**
		 * Delegate to http context in case that the http context is an
		 * {@link WebContainerContext}. {@inheritDoc}
		 */
		// Cannot remove this warning as it is an issue with the
		// javax.servlet.ServletContext interface
		@SuppressWarnings("unchecked")
		@Override
		public Set<String> getResourcePaths(final String path) {
			if (httpContext instanceof WebContainerContext) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("getting resource paths for : [" + path + "]");
				}
				try {
					final Set<String> paths = AccessController.doPrivileged(
							new PrivilegedExceptionAction<Set<String>>() {
								@Override
								public Set<String> run() throws Exception {
									return ((WebContainerContext) httpContext)
											.getResourcePaths(path);
								}
							}, accessControllerContext);
					if (paths == null) {
						return null;
					}
					// Servlet specs mandates that the paths must start with an
					// slash "/"
					final Set<String> slashedPaths = new HashSet<String>();
					for (String foundPath : paths) {
						if (foundPath != null) {
							if (foundPath.trim().startsWith("/")) {
								slashedPaths.add(foundPath.trim());
							} else {
								slashedPaths.add("/" + foundPath.trim());
							}
						}
					}
					if (LOG.isDebugEnabled()) {
						LOG.debug("found resource paths: " + paths);
					}
					return slashedPaths;
				} catch (PrivilegedActionException e) {
					LOG.warn("Unauthorized access: " + e.getMessage());
					return null;
				}
			} else {
				return super.getResourcePaths(path);
			}
		}

		@Override
		public String getMimeType(final String name) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting mime type for: [" + name + "]");
			}
			// Check the OSGi HttpContext
			String mime = httpContext.getMimeType(name);
			if (mime != null) {
				return mime;
			}

			// Delegate to the parent class (the Jetty
			// ServletContextHandler.Context)
			return super.getMimeType(name);
		}

	}

	@Override
	protected void startContext() throws Exception {
		super.startContext();
		LOG.debug("Registering ServletContext as service. ");
		BundleContext bundleContext = (BundleContext) this.attributes.get(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE);
		Bundle bundle = bundleContext.getBundle();
		Dictionary<String, String> properties = new Hashtable<>();
		properties.put(WebContainerConstants.PROPERTY_SYMBOLIC_NAME, bundle.getSymbolicName());

		Dictionary<?, ?> headers = bundle.getHeaders();
		String version = (String) headers
				.get(Constants.BUNDLE_VERSION);
		if (version != null && version.length() > 0) {
			properties.put("osgi.web.version", version);
		}
		// Context servletContext = context.getServletContext();
		String webContextPath = getContextPath();
		if (webContextPath != null && !webContextPath.startsWith("/")) {
			webContextPath = "/" + webContextPath;
		}else if(webContextPath == null){
			LOG.warn(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH +
					" couldn't be set, it's not configured. Assuming '/'");
			webContextPath = "/";
		}
		properties.put(WebContainerConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);

		registerService(bundleContext, properties);
		LOG.debug("ServletContext registered as service. ");
	}

}
