/* Copyright 2007 Alin Dreghiciu.
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
import java.net.URL;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletContainerInitializer;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.security.authentication.SpnegoAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler.JspConfig;
import org.eclipse.jetty.servlet.ServletContextHandler.JspPropertyGroup;
import org.eclipse.jetty.servlet.ServletContextHandler.TagLib;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.ops4j.pax.swissbox.core.BundleUtils;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainerConstants;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.ops4j.pax.web.utils.ServletContainerInitializerScanner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty server with a handler collection specific to Pax Web.
 */
@SuppressWarnings("deprecation")
class JettyServerWrapper extends Server {

	private static final Logger LOG = LoggerFactory.getLogger(JettyServerWrapper.class);

	private static final class ServletContextInfo {

		private final HttpServiceContext handler;
		private final AtomicInteger refCount = new AtomicInteger(1);

		public ServletContextInfo(HttpServiceContext handler) {
			super();
			this.handler = handler;
		}

		public int incrementRefCount() {
			return refCount.incrementAndGet();
		}

		public int decrementRefCount() {
			return refCount.decrementAndGet();
		}

		public HttpServiceContext getHandler() {
			return handler;
		}
	}

	@SuppressWarnings("unused")
	private final ServerModel serverModel;
	private final Map<HttpContext, ServletContextInfo> contexts = new IdentityHashMap<>();
	private Map<String, Object> contextAttributes;
	private Integer sessionTimeout;
	private String sessionCookie;
	private String sessionDomain;
	private String sessionPath;
	private String sessionUrl;
	private String sessionWorkerName;
	private Boolean lazyLoad;
	private String storeDirectory;

	private File serverConfigDir;

	private URL serverConfigURL;

	private Boolean sessionCookieHttpOnly;

	private Boolean sessionCookieSecure;

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Lock readLock = rwLock.readLock();
	private final Lock writeLock = rwLock.writeLock();
	private Bundle jettyBundle;
	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;

	JettyServerWrapper(ServerModel serverModel, ThreadPool threadPool) {
		super(threadPool);
		this.serverModel = serverModel;
		setHandler(new JettyServerHandlerCollection(serverModel));

		jettyBundle = FrameworkUtil.getBundle(getClass());

		if (jettyBundle != null) {
			Filter filterPackage = null;
			try {
				filterPackage = jettyBundle.getBundleContext()
						.createFilter("(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
			} catch (InvalidSyntaxException e) {
				LOG.error("InvalidSyntaxException while waiting for PackageAdmin Service", e);
			}
			packageAdminTracker = new ServiceTracker<>(jettyBundle.getBundleContext(),
					filterPackage, null);
			packageAdminTracker.open();
		}

	}

	public void configureContext(final Map<String, Object> attributes, final Integer timeout, final String cookie,
								 final String domain, final String path, final String url, final Boolean cookieHttpOnly,
								 final Boolean sessionCookieSecure, final String workerName, final Boolean lazy, final String directory) {
		this.contextAttributes = attributes;
		this.sessionTimeout = timeout;
		this.sessionCookie = cookie;
		this.sessionDomain = domain;
		this.sessionPath = path;
		this.sessionUrl = url;
		this.sessionCookieHttpOnly = cookieHttpOnly;
		this.sessionCookieSecure = sessionCookieSecure;
		this.sessionWorkerName = workerName;
		lazyLoad = lazy;
		this.storeDirectory = directory;
	}

	HttpServiceContext getContext(final HttpContext httpContext) {
		readLock.lock();
		try {
			ServletContextInfo servletContextInfo = contexts.get(httpContext);
			if (servletContextInfo != null) {
				return servletContextInfo.getHandler();
			}
			return null;
		} finally {
			readLock.unlock();
		}
	}

	HttpServiceContext getOrCreateContext(final Model model) {
		return getOrCreateContext(model.getContextModel());
	}

	HttpServiceContext getOrCreateContext(final ContextModel model) {
		final HttpContext httpContext = model.getHttpContext();
		ServletContextInfo context = null;
		try {
			readLock.lock();
			if (contexts.containsKey(httpContext)) {
				context = contexts.get(httpContext);
				context.incrementRefCount();
			} else {
				try {
					readLock.unlock();
					writeLock.lock();
					if (!contexts.containsKey(httpContext)) {
						LOG.debug("Creating new ServletContextHandler for HTTP context [{}] and model [{}]",
								httpContext, model);

						context = new ServletContextInfo(this.addContext(model));
						contexts.put(httpContext, context);
						// don't increment! - it's already == 1 after creation
//						context.incrementRefCount();
					} else {
						context = contexts.get(httpContext);
						context.incrementRefCount();
					}
				} finally {
					readLock.lock();
					writeLock.unlock();
				}
			}
		} finally {
			readLock.unlock();
		}
		return context.getHandler();
	}

	void removeContext(final HttpContext httpContext, boolean force) {
		ServletContextInfo context;
		try {
			readLock.lock();
			context = contexts.get(httpContext);
			if (context == null) {
				return;
			}
			int nref = context.decrementRefCount();
			if ((force && !(httpContext instanceof SharedWebContainerContext)) || nref <= 0) {
				try {
					readLock.unlock();
					writeLock.lock();
					LOG.debug("Removing ServletContextHandler for HTTP context [{}].", httpContext);
					context = contexts.remove(httpContext);
				} finally {
					readLock.lock();
					writeLock.unlock();
				}
			} else {
				LOG.debug("ServletContextHandler for HTTP context [{}] referenced [{}] times.", httpContext, nref);
				return;
			}
		} finally {
			readLock.unlock();
		}
		// Destroy the context outside of the locking region
		if (context != null) {
			HttpServiceContext sch = context.getHandler();
			sch.unregisterService();
			try {
				sch.stop();
			} catch (Throwable t) { // CHECKSTYLE:SKIP
				// Ignore
			}
			sch.getServletHandler().setServer(null);
			sch.getSecurityHandler().setServer(null);
			sch.getSessionHandler().setServer(null);
			sch.getErrorHandler().setServer(null);
			((HandlerCollection) getHandler()).removeHandler(sch);
			sch.destroy();
		}
	}

	@SuppressWarnings("unchecked")
	private HttpServiceContext addContext(final ContextModel model) {
		Bundle bundle = model.getBundle();
		BundleContext bundleContext = BundleUtils.getBundleContext(bundle);

		if (packageAdminTracker != null) {
			ServletContainerInitializerScanner scanner = new ServletContainerInitializerScanner(bundle, jettyBundle, packageAdminTracker.getService());
			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = model.getContainerInitializers();
			if (containerInitializers == null) {
				containerInitializers = new HashMap<>();
				model.setContainerInitializers(containerInitializers);
			}
			scanner.scanBundles(containerInitializers);
		}

		HttpServiceContext context = new HttpServiceContext((HandlerContainer) getHandler(), model.getContextParams(),
				getContextAttributes(bundleContext), model.getContextName(), model.getHttpContext(),
				model.getAccessControllerContext(), model.getContainerInitializers(), model.getJettyWebXmlURL(),
				model.getVirtualHosts());
		context.setClassLoader(model.getClassLoader());
		Integer modelSessionTimeout = model.getSessionTimeout();
		if (modelSessionTimeout == null) {
			modelSessionTimeout = sessionTimeout;
		}
		String modelSessionCookie = model.getSessionCookie();
		if (modelSessionCookie == null) {
			modelSessionCookie = sessionCookie;
		}
		String modelSessionDomain = model.getSessionDomain();
		if (modelSessionDomain == null) {
			modelSessionDomain = sessionDomain;
		}
		String modelSessionPath = model.getSessionPath();
		if (modelSessionPath == null) {
			modelSessionPath = sessionPath;
		}
		String modelSessionUrl = model.getSessionUrl();
		if (modelSessionUrl == null) {
			modelSessionUrl = sessionUrl;
		}
		Boolean modelSessionCookieHttpOnly = model.getSessionCookieHttpOnly();
		if (modelSessionCookieHttpOnly == null) {
			modelSessionCookieHttpOnly = sessionCookieHttpOnly;
		}
		Boolean modelSessionSecure = model.getSessionCookieSecure();
		if (modelSessionSecure == null) {
			modelSessionSecure = sessionCookieSecure;
		}
		String workerName = model.getSessionWorkerName();
		if (workerName == null) {
			workerName = sessionWorkerName;
		}
		configureSessionManager(context, modelSessionTimeout, modelSessionCookie, modelSessionDomain, modelSessionPath,
				modelSessionUrl, modelSessionCookieHttpOnly, modelSessionSecure, workerName, lazyLoad, storeDirectory);

		if (model.getRealmName() != null && model.getAuthMethod() != null) {
			configureSecurity(context, model.getRealmName(), model.getAuthMethod(), model.getFormLoginPage(),
					model.getFormErrorPage());
		}

		configureJspConfigDescriptor(context, model);

		LOG.debug("Added servlet context: " + context);

		if (isStarted()) {
			try {
				LOG.debug("(Re)starting servlet contexts...");
				// start the server handler if not already started
				Handler serverHandler = getHandler();
				if (!serverHandler.isStarted() && !serverHandler.isStarting()) {
					serverHandler.start();
				}
//				// if the server handler is a handler collection, seems like
//				// jetty will not automatically
//				// start inner handlers. So, force the start of the created
//				// context
//				if (!context.isStarted() && !context.isStarting()) {
//					LOG.debug("Registering ServletContext as service. ");
//					Dictionary<String, String> properties = new Hashtable<String, String>();
//					properties.put("osgi.web.symbolicname", bundle.getSymbolicName());
//
//					Dictionary<?, ?> headers = bundle.getHeaders();
//					String version = (String) headers.get(Constants.BUNDLE_VERSION);
//					if (version != null && version.length() > 0) {
//						properties.put("osgi.web.version", version);
//					}
//
//					// Context servletContext = context.getServletContext();
//					String webContextPath = context.getContextPath();
//
//					properties.put("osgi.web.contextpath", webContextPath);
//
//					context.registerService(bundleContext, properties);
//					LOG.debug("ServletContext registered as service. ");
//
//				}
				// CHECKSTYLE:OFF
			} catch (Exception ignore) {
				LOG.error("Could not start the servlet context for http context [" + model.getHttpContext() + "]",
						ignore);
				if (ignore instanceof MultiException) {
					LOG.error("MultiException found: ");
					MultiException mex = (MultiException) ignore;
					List<Throwable> throwables = mex.getThrowables();
					for (Throwable throwable : throwables) {
						LOG.error(throwable.getMessage());
					}
				}
			}
			// CHECKSTYLE:ON
		}
		return context;
	}

	private void configureJspConfigDescriptor(HttpServiceContext context, ContextModel model) {

		Boolean elIgnored = model.getJspElIgnored();
		Boolean isXml = model.getJspIsXml();
		Boolean scriptingInvalid = model.getJspScriptingInvalid();

		JspPropertyGroup jspPropertyGroup = null;

		if (elIgnored != null || isXml != null || scriptingInvalid != null
				|| model.getJspIncludeCodes() != null
				|| model.getJspUrlPatterns() != null
				|| model.getJspIncludePreludes() != null) {
			jspPropertyGroup = new JspPropertyGroup();

			if (model.getJspIncludeCodes() != null) {
				for (String includeCoda : model.getJspIncludeCodes()) {
					jspPropertyGroup.addIncludeCoda(includeCoda);
				}
			}

			if (model.getJspUrlPatterns() != null) {
				for (String urlPattern : model.getJspUrlPatterns()) {
					jspPropertyGroup.addUrlPattern(urlPattern);
				}
			}

			if (model.getJspIncludePreludes() != null) {
				for (String prelude : model.getJspIncludePreludes()) {
					jspPropertyGroup.addIncludePrelude(prelude);
				}
			}

			if (elIgnored != null) {
				jspPropertyGroup.setElIgnored(elIgnored.toString());
			}
			if (isXml != null) {
				jspPropertyGroup.setIsXml(isXml.toString());
			}
			if (scriptingInvalid != null) {
				jspPropertyGroup.setScriptingInvalid(scriptingInvalid.toString());
			}

		}

		TagLib tagLibDescriptor = null;

		if (model.getTagLibLocation() != null || model.getTagLibUri() != null) {
			tagLibDescriptor = new TagLib();
			tagLibDescriptor.setTaglibLocation(model.getTagLibLocation());
			tagLibDescriptor.setTaglibURI(model.getTagLibUri());
		}

		if (jspPropertyGroup != null || tagLibDescriptor != null) {
			JspConfig jspConfig = new JspConfig();
			jspConfig.addJspPropertyGroup(jspPropertyGroup);
			jspConfig.addTaglibDescriptor(tagLibDescriptor);
			context.getServletContext().setJspConfigDescriptor(jspConfig);
		}
	}

	/**
	 * Sets the security authentication method and the realm name on the
	 * security handler. This has to be done before the context is started.
	 *
	 * @param context
	 * @param realmName
	 * @param authMethod
	 * @param formLoginPage
	 * @param formErrorPage
	 */
	private void configureSecurity(ServletContextHandler context, String realmName, String authMethod,
								   String formLoginPage, String formErrorPage) {
		final SecurityHandler securityHandler = context.getSecurityHandler();

		Authenticator authenticator = null;
		if (authMethod == null) {
			LOG.warn("UNKNOWN AUTH METHOD: " + authMethod);
		} else {
			switch (authMethod) {
				case Constraint.__FORM_AUTH:
					authenticator = new FormAuthenticator();
					securityHandler.setInitParameter(FormAuthenticator.__FORM_LOGIN_PAGE, formLoginPage);
					securityHandler.setInitParameter(FormAuthenticator.__FORM_ERROR_PAGE, formErrorPage);
					break;
				case Constraint.__BASIC_AUTH:
					authenticator = new BasicAuthenticator();
					break;
				case Constraint.__DIGEST_AUTH:
					authenticator = new DigestAuthenticator();
					break;
				case Constraint.__CERT_AUTH:
					authenticator = new ClientCertAuthenticator();
					break;
				case Constraint.__CERT_AUTH2:
					authenticator = new ClientCertAuthenticator();
					break;
				case Constraint.__SPNEGO_AUTH:
					authenticator = new SpnegoAuthenticator();
					break;
				default:
					LOG.warn("UNKNOWN AUTH METHOD: " + authMethod);
					break;
			}
		}

		securityHandler.setAuthenticator(authenticator);

		securityHandler.setRealmName(realmName);

	}

	/**
	 * Returns a list of servlet context attributes out of configured properties
	 * and attribues containing the bundle context associated with the bundle
	 * that created the model (web element).
	 *
	 * @param bundleContext bundle context to be set as attribute
	 * @return context attributes map
	 */
	private Map<String, Object> getContextAttributes(final BundleContext bundleContext) {
		final Map<String, Object> attributes = new HashMap<>();
		if (contextAttributes != null) {
			attributes.putAll(contextAttributes);
		}
		attributes.put(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE, bundleContext);
		attributes.put("org.springframework.osgi.web.org.osgi.framework.BundleContext", bundleContext);
		return attributes;
	}

	/**
	 * Configures the session time out by extracting the session
	 * handlers->sessionManager for the context.
	 *
	 * @param context        the context for which the session timeout should be configured
	 * @param minutes        timeout in minutes
	 * @param cookie         Session cookie name. Defaults to JSESSIONID. If set to null or
	 *                       "none" no cookies will be used.
	 * @param domain         Session cookie domain name. Default to the current host.
	 * @param path           Session cookie path. default to the current servlet context
	 *                       path.
	 * @param url            session URL parameter name. Defaults to jsessionid. If set to
	 *                       null or "none" no URL rewriting will be done.
	 * @param cookieHttpOnly configures if the Cookie is valid for http only and therefore
	 *                       not available to javascript.
	 * @param secure         Configures if the session cookie is only transfered via https
	 *                       even if its created during a non-secure request. Defaults to
	 *                       false which means the session cookie is set to be secure if
	 *                       its created during a https request.
	 * @param workerName     name appended to session id, used to assist session affinity
	 *                       in a load balancer
	 */
	private void configureSessionManager(final ServletContextHandler context, final Integer minutes,
										 final String cookie, String domain, String path, final String url, final Boolean cookieHttpOnly,
										 final Boolean secure, final String workerName, final Boolean lazy, final String directory) {
		LOG.debug("configureSessionManager for context [" + context + "] using - timeout:" + minutes + ", cookie:"
				+ cookie + ", url:" + url + ", cookieHttpOnly:" + cookieHttpOnly + ", workerName:" + workerName
				+ ", lazyLoad:" + lazy + ", storeDirectory: " + directory);

		final SessionHandler sessionHandler = context.getSessionHandler();
		if (sessionHandler != null) {
			final SessionManager sessionManager = sessionHandler.getSessionManager();
			if (sessionManager != null) {
				if (minutes != null) {
					sessionManager.setMaxInactiveInterval(minutes * 60);
					LOG.debug("Session timeout set to " + minutes + " minutes for context [" + context + "]");
				}
				if (cookie == null || "none".equals(cookie)) {
					if (sessionManager instanceof AbstractSessionManager) {
						((AbstractSessionManager) sessionManager).setUsingCookies(false);
						LOG.debug("Session cookies disabled for context [" + context + "]");
					} else {
						LOG.debug(
								"SessionManager isn't of type AbstractSessionManager therefore using cookies unchanged!");
					}
				} else {
					sessionManager.getSessionCookieConfig().setName(cookie);
					LOG.debug("Session cookie set to " + cookie + " for context [" + context + "]");

					sessionManager.getSessionCookieConfig().setHttpOnly(cookieHttpOnly);
					LOG.debug("Session cookieHttpOnly set to " + cookieHttpOnly + " for context [" + context + "]");
				}
				if (domain != null && domain.length() > 0) {
					sessionManager.getSessionCookieConfig().setDomain(domain);
					LOG.debug("Session cookie domain set to " + domain + " for context [" + context + "]");
				}
				if (path != null && path.length() > 0) {
					sessionManager.getSessionCookieConfig().setPath(path);
					LOG.debug("Session cookie path set to " + path + " for context [" + context + "]");
				}
				if (secure != null) {
					sessionManager.getSessionCookieConfig().setSecure(secure);
					LOG.debug("Session cookie secure set to " + secure + " for context [" + context + "]");
				}
				if (url != null) {
					sessionManager.setSessionIdPathParameterName(url);
					LOG.debug("Session URL set to " + url + " for context [" + context + "]");
				}
				if (workerName != null) {
					SessionIdManager sessionIdManager = sessionManager.getSessionIdManager();
					if (sessionIdManager == null) {
						sessionIdManager = new HashSessionIdManager();
						sessionManager.setSessionIdManager(sessionIdManager);
					}
					if (sessionIdManager instanceof AbstractSessionIdManager) {
						AbstractSessionIdManager s = (AbstractSessionIdManager) sessionIdManager;
						s.setWorkerName(workerName);
						LOG.debug("Worker name set to " + workerName + " for context [" + context + "]");
					}
				}
				// PAXWEB-461
				if (lazy != null) {
					LOG.debug("is LazyLoad active? {}", lazy);
					if (sessionManager instanceof HashSessionManager) {
						((HashSessionManager) sessionManager).setLazyLoad(lazy);
					}
				}
				if (directory != null) {
					LOG.debug("storeDirectoy set to: {}", directory);
					if (sessionManager instanceof HashSessionManager) {
						File storeDir = null;
						try {
							storeDir = new File(directory);
							((HashSessionManager) sessionManager).setStoreDirectory(storeDir);
						} catch (IOException e) {
							LOG.warn("IOException while trying to set the StoreDirectory on the session Manager", e);
						}
					}
				}
			}
		}
	}

	/**
	 * @param serverConfigDir the serverConfigDir to set
	 */
	public void setServerConfigDir(File serverConfigDir) {
		this.serverConfigDir = serverConfigDir;
	}

	/**
	 * @return the serverConfigDir
	 */
	public File getServerConfigDir() {
		return serverConfigDir;
	}

	public URL getServerConfigURL() {
		return serverConfigURL;
	}

	public void setServerConfigURL(URL serverConfigURL) {
		this.serverConfigURL = serverConfigURL;
	}
}
