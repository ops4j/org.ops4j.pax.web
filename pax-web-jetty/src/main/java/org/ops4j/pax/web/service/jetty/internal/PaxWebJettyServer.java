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

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.ops4j.pax.web.annotations.Review;
import org.ops4j.pax.web.service.AuthenticatorService;
import org.ops4j.pax.web.service.MultiBundleWebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Pax Web extension of {@link Server Jetty server} with specialized {@link HandlerCollection}. That's actual
 * Jetty embedded server with connectors and handlers.</p>
 */
class PaxWebJettyServer extends Server {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebJettyServer.class);

				private static final class ServletContextInfo {

					private final PaxWebServletContextHandler handler;
					private final AtomicInteger refCount = new AtomicInteger(1);

					public ServletContextInfo(PaxWebServletContextHandler handler) {
						super();
						this.handler = handler;
					}

					public int incrementRefCount() {
						return refCount.incrementAndGet();
					}

					public int decrementRefCount() {
						return refCount.decrementAndGet();
					}

					public PaxWebServletContextHandler getHandler() {
						return handler;
					}
				}

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
				private Boolean showStacks;

				private File serverConfigDir;

				private URL serverConfigURL;

				private String defaultAuthMethod;
				private String defaultRealmName;

				private Boolean sessionCookieHttpOnly;

				private Boolean sessionCookieSecure;

				private Integer sessionCookieMaxAge;

				private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
				private final Lock readLock = rwLock.readLock();
				private final Lock writeLock = rwLock.writeLock();
				private Bundle jettyBundle;
//				private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;

				private HandlerCollection rootCollections;

	PaxWebJettyServer(ThreadPool threadPool) {
		super(threadPool);

//					rootCollections = new JettyServerHandlerCollection(null);
//					setHandler(rootCollections);
//
//					jettyBundle = FrameworkUtil.getBundle(getClass());
//
//					if (jettyBundle != null) {
//						Filter filterPackage = null;
//						try {
//							filterPackage = jettyBundle.getBundleContext()
//									.createFilter("(objectClass=org.osgi.service.packageadmin.PackageAdmin)");
//						} catch (InvalidSyntaxException e) {
//							LOG.error("InvalidSyntaxException while waiting for PackageAdmin Service", e);
//						}
//						packageAdminTracker = new ServiceTracker<>(jettyBundle.getBundleContext(),
//								filterPackage, null);
//						packageAdminTracker.open();
//					}

	}

	public HandlerCollection getRootHandlerCollection() {
		return rootCollections;
	}

	@Review("Called during startup, sets config common to all future contexts, which is a bit weird")
	public void configureContext(final Map<String, Object> attributes, final Integer timeout, final String cookie,
								 final String domain, final String path, final String url, final Boolean cookieHttpOnly,
								 final Boolean sessionCookieSecure, final String workerName, final Boolean lazy, final String directory,
								 Integer maxAge, final Boolean showStacks) {
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
		this.sessionCookieMaxAge = maxAge;
		this.showStacks = showStacks;
	}

	PaxWebServletContextHandler getContext(final HttpContext httpContext) {
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

//	PaxWebServletContextHandler getOrCreateContext(final ElementModel elementModel) {
//		return getOrCreateContext((OsgiContextModel) null/*elementModel.getContextModel()*/);
//	}

//	PaxWebServletContextHandler getOrCreateContext(final OsgiContextModel model) {
//		final HttpContext httpContext = model.getHttpContext();
//		ServletContextInfo context = null;
//		try {
//			readLock.lock();
//			if (contexts.containsKey(httpContext)) {
//				context = contexts.get(httpContext);
//				context.incrementRefCount();
//			} else {
//				try {
//					readLock.unlock();
//					writeLock.lock();
//					if (!contexts.containsKey(httpContext)) {
//						LOG.debug("Creating new ServletContextHandler for HTTP context [{}] and model [{}]",
//								httpContext, model);
//
//						context = new ServletContextInfo(this.addContext(model));
//						contexts.put(httpContext, context);
//						// don't increment! - it's already == 1 after creation
////						context.incrementRefCount();
//					} else {
//						context = contexts.get(httpContext);
//						context.incrementRefCount();
//					}
//				} finally {
//					readLock.lock();
//					writeLock.unlock();
//				}
//			}
//		} finally {
//			readLock.unlock();
//		}
//		return context.getHandler();
//	}

	void removeContext(final HttpContext httpContext, boolean force) {
		ServletContextInfo context;
		try {
			readLock.lock();
			context = contexts.get(httpContext);
			if (context == null) {
				return;
			}
			int nref = context.decrementRefCount();
			if ((force && !(httpContext instanceof MultiBundleWebContainerContext)) || nref <= 0) {
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
			PaxWebServletContextHandler sch = context.getHandler();
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
			rootCollections.removeHandler(sch);
			sch.destroy();
		}
	}

	@SuppressWarnings("unchecked")
	private PaxWebServletContextHandler addContext(final OsgiContextModel model) {
//		Map<String, Object> attributes = new HashMap<>();
//		attributes.put("javax.servlet.context.tempdir",
//				configuration.server().getTemporaryDirectory());

//		// Fix for PAXWEB-193
//		jettyServer.setServerConfigDir(configuration.server().getConfigurationDir());
//		jettyServer.setServerConfigURL(configuration.server().getConfigurationURL());
//		jettyServer.setDefaultAuthMethod(configuration.security().getDefaultAuthMethod());
//		jettyServer.setDefaultRealmName(configuration.security().getDefaultRealmName());
//		jettyServer.configureContext(attributes,
//				configuration.session().getSessionTimeout(),
//				configuration.session().getSessionCookie(),
//				configuration.session().getSessionDomain(),
//				configuration.session().getSessionPath(),
//				configuration.session().getSessionUrl(),
//				configuration.session().getSessionCookieHttpOnly(),
//				configuration.session().getSessionCookieSecure(),
//				configuration.server().getWorkerName(),
//				configuration.session().getSessionLazyLoad(),
//				configuration.session().getSessionStoreDirectory(),
//				configuration.session().getSessionCookieMaxAge(),
//				configuration.server().isShowStacks());

		//		Bundle bundle = model.getBundle();
//		BundleContext bundleContext = BundleUtils.getBundleContext(bundle);

//		if (packageAdminTracker != null) {
//			ServletContainerInitializerScanner scanner = new ServletContainerInitializerScanner(bundle, jettyBundle, packageAdminTracker.getService());
//			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers = model.getContainerInitializers();
//			if (containerInitializers == null) {
//				containerInitializers = new HashMap<>();
//				model.setContainerInitializers(containerInitializers);
//			}
//			scanner.scanBundles(containerInitializers);
//		}

//		HttpServiceContext context = new HttpServiceContext(rootCollections, model.getContextParams(),
//				getContextAttributes(bundleContext), model.getContextName(), model.getHttpContext(),
//				model.getAccessControllerContext(), model.getContainerInitializers(), model.getJettyWebXmlURL(),
//				model.getVirtualHosts(), model.isShowStacks());
//		context.setClassLoader(model.getClassLoader());
//		Integer modelSessionTimeout = model.getSessionTimeout();
//		if (modelSessionTimeout == null) {
//			modelSessionTimeout = sessionTimeout;
//		}
//		String modelSessionCookie = model.getSessionCookie();
//		if (modelSessionCookie == null) {
//			modelSessionCookie = sessionCookie;
//		}
//		String modelSessionDomain = model.getSessionDomain();
//		if (modelSessionDomain == null) {
//			modelSessionDomain = sessionDomain;
//		}
//		String modelSessionPath = model.getSessionPath();
//		if (modelSessionPath == null) {
//			modelSessionPath = sessionPath;
//		}
//		String modelSessionUrl = model.getSessionUrl();
//		if (modelSessionUrl == null) {
//			modelSessionUrl = sessionUrl;
//		}
//		Boolean modelSessionCookieHttpOnly = model.getSessionCookieHttpOnly();
//		if (modelSessionCookieHttpOnly == null) {
//			modelSessionCookieHttpOnly = sessionCookieHttpOnly;
//		}
//		Boolean modelSessionSecure = model.getSessionCookieSecure();
//		if (modelSessionSecure == null) {
//			modelSessionSecure = sessionCookieSecure;
//		}
//		String workerName = model.getSessionWorkerName();
//		if (workerName == null) {
//			workerName = sessionWorkerName;
//		}
//		Integer maxAge = model.getSessionCookieMaxAge();
//		if (maxAge == null) {
//			maxAge = sessionCookieMaxAge;
//		}
//		if (maxAge == null) {
//			maxAge = -1;
//		}
//		configureSessionManager(context, modelSessionTimeout, modelSessionCookie, modelSessionDomain, modelSessionPath,
//				modelSessionUrl, modelSessionCookieHttpOnly, modelSessionSecure, workerName, lazyLoad, storeDirectory,
//				maxAge);
//
//		if(this.defaultAuthMethod != null && model.getAuthMethod() == null){
//            model.setAuthMethod(this.defaultAuthMethod);
//        }
//        if(this.defaultRealmName != null && model.getRealmName() == null){
//            model.setRealmName(this.defaultRealmName);
//        }
//		if (model.getRealmName() != null && model.getAuthMethod() != null) {
//			configureSecurity(context, model.getRealmName(), model.getAuthMethod(), model.getFormLoginPage(),
//					model.getFormErrorPage());
//		}
//
//		configureJspConfigDescriptor(context, model);
//
//		LOG.debug("Added servlet context: " + context);
//
//		if (isStarted()) {
//			try {
//				LOG.debug("(Re)starting servlet contexts...");
//				// start the server handler if not already started
//				Handler serverHandler = getHandler();
//				if (!serverHandler.isStarted() && !serverHandler.isStarting()) {
//					serverHandler.start();
//				}
////				// if the server handler is a handler collection, seems like
////				// jetty will not automatically
////				// start inner handlers. So, force the start of the created
////				// context
////				if (!context.isStarted() && !context.isStarting()) {
////					LOG.debug("Registering ServletContext as service. ");
////					Dictionary<String, String> properties = new Hashtable<String, String>();
////					properties.put("osgi.web.symbolicname", bundle.getSymbolicName());
////
////					Dictionary<?, ?> headers = bundle.getHeaders();
////					String version = (String) headers.get(Constants.BUNDLE_VERSION);
////					if (version != null && version.length() > 0) {
////						properties.put("osgi.web.version", version);
////					}
////
////					// Context servletContext = context.getServletContext();
////					String webContextPath = context.getContextPath();
////
////					properties.put("osgi.web.contextpath", webContextPath);
////
////					context.registerService(bundleContext, properties);
////					LOG.debug("ServletContext registered as service. ");
////
////				}
//				// CHECKSTYLE:OFF
//			} catch (Exception ignore) {
//				LOG.error("Could not start the servlet context for http context [" + model.getHttpContext() + "]",
//						ignore);
//				if (ignore instanceof MultiException) {
//					LOG.error("MultiException found: ");
//					MultiException mex = (MultiException) ignore;
//					List<Throwable> throwables = mex.getThrowables();
//					for (Throwable throwable : throwables) {
//						LOG.error(throwable.getMessage());
//					}
//				}
//			}
//			// CHECKSTYLE:ON
//		}
		return null;//context;
	}

	private void configureJspConfigDescriptor(PaxWebServletContextHandler context, OsgiContextModel model) {

//		Boolean elIgnored = model.getJspElIgnored();
//		Boolean isXml = model.getJspIsXml();
//		Boolean scriptingInvalid = model.getJspScriptingInvalid();
//
//		JspPropertyGroup jspPropertyGroup = null;
//
//		if (elIgnored != null || isXml != null || scriptingInvalid != null
//				|| model.getJspIncludeCodes() != null
//				|| model.getJspUrlPatterns() != null
//				|| model.getJspIncludePreludes() != null) {
//			jspPropertyGroup = new JspPropertyGroup();
//
//			if (model.getJspIncludeCodes() != null) {
//				for (String includeCoda : model.getJspIncludeCodes()) {
//					jspPropertyGroup.addIncludeCoda(includeCoda);
//				}
//			}
//
//			if (model.getJspUrlPatterns() != null) {
//				for (String urlPattern : model.getJspUrlPatterns()) {
//					jspPropertyGroup.addUrlPattern(urlPattern);
//				}
//			}
//
//			if (model.getJspIncludePreludes() != null) {
//				for (String prelude : model.getJspIncludePreludes()) {
//					jspPropertyGroup.addIncludePrelude(prelude);
//				}
//			}
//
//			if (elIgnored != null) {
//				jspPropertyGroup.setElIgnored(elIgnored.toString());
//			}
//			if (isXml != null) {
//				jspPropertyGroup.setIsXml(isXml.toString());
//			}
//			if (scriptingInvalid != null) {
//				jspPropertyGroup.setScriptingInvalid(scriptingInvalid.toString());
//			}
//
//		}
//
//		TagLib tagLibDescriptor = null;
//
//		if (model.getTagLibLocation() != null || model.getTagLibUri() != null) {
//			tagLibDescriptor = new TagLib();
//			tagLibDescriptor.setTaglibLocation(model.getTagLibLocation());
//			tagLibDescriptor.setTaglibURI(model.getTagLibUri());
//		}
//
//		if (jspPropertyGroup != null || tagLibDescriptor != null) {
//			JspConfig jspConfig = new JspConfig();
//			jspConfig.addJspPropertyGroup(jspPropertyGroup);
//			jspConfig.addTaglibDescriptor(tagLibDescriptor);
//			context.getServletContext().setJspConfigDescriptor(jspConfig);
//		}
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
//				case Constraint.__SPNEGO_AUTH:
//					authenticator = new SpnegoAuthenticator();
//					break;
				default:
					authenticator = getAuthenticator(authMethod);
					break;
			}
		}

		securityHandler.setAuthenticator(authenticator);

		securityHandler.setRealmName(realmName);

	}

	private Authenticator getAuthenticator(String method) {
		ServiceLoader<AuthenticatorService> sl = ServiceLoader.load(AuthenticatorService.class, getClass().getClassLoader());
		for (AuthenticatorService svc : sl) {
			try {
				Authenticator auth = svc.getAuthenticatorService(method, Authenticator.class);
				if (auth != null) {
					return auth;
				}
			} catch (Throwable t) {
				LOG.debug("Unable to load AuthenticatorService for: " + method, t);
			}
		}
		return null;
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
//		attributes.put(WebContainerConstants.BUNDLE_CONTEXT_ATTRIBUTE, bundleContext);
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
	 * @param maxAge         session cookie maxAge
	 */
	private void configureSessionManager(final ServletContextHandler context, final Integer minutes,
										 final String cookie, String domain, String path, final String url, final Boolean cookieHttpOnly,
										 final Boolean secure, final String workerName, final Boolean lazy, final String directory,
										 final int maxAge) {
		LOG.debug("configureSessionManager for context [" + context + "] using - timeout:" + minutes + ", cookie:"
				+ cookie + ", url:" + url + ", cookieHttpOnly:" + cookieHttpOnly + ", workerName:" + workerName
				+ ", lazyLoad:" + lazy + ", storeDirectory: " + directory);

		final SessionHandler sessionHandler = context.getSessionHandler();
		if (sessionHandler != null) {
			if (minutes != null) {
				sessionHandler.setMaxInactiveInterval(minutes * 60);
				LOG.debug("Session timeout set to {} minutes for context [{}]", minutes, context);
			}
			if (cookie != null && !"none".equals(cookie)) {
				sessionHandler.getSessionCookieConfig().setName(cookie);
				LOG.debug("Session cookie set to {} for context [{}]", cookie, context);
				sessionHandler.getSessionCookieConfig().setHttpOnly(cookieHttpOnly);
				LOG.debug("Session cookieHttpOnly set to {} for context [{}]", cookieHttpOnly, context);
			}
			if (domain != null && domain.length() > 0) {
				sessionHandler.getSessionCookieConfig().setDomain(domain);
				LOG.debug("Session cookie domain set to {} for context [{]]", domain, context);
			}
			if (path != null && path.length() > 0) {
				sessionHandler.getSessionCookieConfig().setPath(path);
				LOG.debug("Session cookie path set to {} for context [{}]", path, context);
			}
			if (secure != null) {
				sessionHandler.getSessionCookieConfig().setSecure(secure);
				LOG.debug("Session cookie secure set to {} for context [{]}]", secure, context);
			}
			if (url != null) {
				sessionHandler.setSessionIdPathParameterName(url);
				LOG.debug("Session URL set to {} for context [{}]", url, context);
			}
			if (workerName != null && sessionHandler.getSessionIdManager() != null) {
				((DefaultSessionIdManager) sessionHandler.getSessionIdManager()).setWorkerName(workerName);
				LOG.debug("Worker name set to {} for context [{}]", workerName, context);
			}
		}
	}

	public String getDefaultAuthMethod(){
	    return defaultAuthMethod;
    }

	public void setDefaultAuthMethod(String defaultAuthMethod) {
		this.defaultAuthMethod = defaultAuthMethod;
	}
	public String getDefaultRealmName() {
		return defaultRealmName;
	}

	public void setDefaultRealmName(String defaultRealmName) {
		this.defaultRealmName = defaultRealmName;
	}
}
