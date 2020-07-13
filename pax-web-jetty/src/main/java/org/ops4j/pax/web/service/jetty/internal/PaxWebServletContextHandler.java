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

import java.net.URL;
import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;

import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Pax Web specific {@link ServletContextHandler} representing single <em>web application</em>
 * deployed under unique <em>context path</em>, related 1:1 with single, unique
 * {@link org.ops4j.pax.web.service.spi.model.ServletContextModel}
 */
public class PaxWebServletContextHandler extends ServletContextHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebServletContextHandler.class);

				/**
				 * Access controller context of the bundle that registred the http context.
				 */
				private AccessControlContext accessControllerContext;

				private Map<ServletContainerInitializer, Set<Class<?>>> servletContainerInitializers;

				private URL jettyWebXmlURL;

				private List<String> virtualHosts;

				private final AtomicReference<ServiceRegistration<ServletContext>> registration
						= new AtomicReference<>();

	public PaxWebServletContextHandler(HandlerContainer parent, String contextPath, boolean sessions, boolean security) {
		super(parent, contextPath, sessions, security);

		// TCCL of sessionManager timer threads will be set to thread of pax-web-jetty bundle, not to current TCCL
		ScheduledExecutorScheduler executorScheduler = new ScheduledExecutorScheduler(getSessionHandler().toString() + "Timer", true,
				getClass().getClassLoader());
		_scontext.setAttribute("org.eclipse.jetty.server.session.timer", executorScheduler);

				//		ErrorPageErrorHandler errorPageErrorHandler = new ErrorPageErrorHandler();
				//		if (showStacks != null) {
				//			errorPageErrorHandler.setShowStacks(showStacks);
				//		}
				//		setErrorHandler(errorPageErrorHandler);
	}

				//	public void registerService(BundleContext bundleContext, Dictionary<String, String> properties) {
				//		if (registration.get() == null) {
				//			ServiceRegistration<ServletContext> reg = bundleContext.registerService(
				//					ServletContext.class,
				//					getServletContext(),
				//					properties
				//			);
				//			if (!registration.compareAndSet(null, reg)) {
				//				reg.unregister();
				//			}
				//			LOG.debug("ServletContext registered as service.");
				//		}
				//	}
				//
				//	public void unregisterService() {
				//		ServiceRegistration<ServletContext> reg = registration.getAndSet(null);
				//		if (reg != null) {
				//			LOG.debug("ServletContext unregistered as service.");
				//			try {
				//				reg.unregister();
				//			} catch (IllegalStateException e) {
				//				LOG.info("ServletContext service already removed");
				//			}
				//		}
				//	}

				//	@Override
				//	protected void doStart() throws Exception {
				//
				//		//need to initialize the logger as super doStart is to late already
				//		setLogger(Log.getLogger(getDisplayName() == null ? getContextPath() : getDisplayName()));
				//
				//		// org.eclipse.jetty.webapp.WebAppContext._configurations handles annotation and TLD scanning
				//		// we have to do it manually here
				//		// see org.eclipse.jetty.webapp.WebAppContext.DEFAULT_CONFIGURATION_CLASSES
				//		// Special handling for JASPER
				//		if (isJspAvailable()) { // use JasperClassloader
				//			LOG.info("registering JasperInitializer");
				//			@SuppressWarnings("unchecked")
				//			Class<ServletContainerInitializer> loadClass = (Class<ServletContainerInitializer>) loadClass("org.ops4j.pax.web.jsp.JasperInitializer");
				//			servletContainerInitializers.put(loadClass.newInstance(),
				//					Collections.<Class<?>>emptySet());
				//		}
				//
				//		if (servletContainerInitializers != null) {
				//			List<ServletContainerInitializer> list2 = new LinkedList<>();
				//
				//			final List<ServletContainerInitializer> cdi = new LinkedList<>();
				//			final List<ServletContainerInitializer> jsp = new LinkedList<>();
				//			final List<ServletContainerInitializer> jettyWebSocket = new LinkedList<>();
				//			final List<ServletContainerInitializer> remaining = new LinkedList<>();
				//
				//			servletContainerInitializers.keySet().forEach(sci -> {
				//				String className = sci.getClass().getName();
				//				if ("org.ops4j.pax.cdi.web.impl.CdiServletContainerInitializer".equals(className)) {
				//					cdi.add(sci);
				//				} else if (className.startsWith("org.eclipse.jetty.websocket")) {
				//					jettyWebSocket.add(sci);
				//				} else if ("org.ops4j.pax.web.jsp.JasperInitializer".equals(className)) {
				//					jsp.add(sci);
				//				} else {
				//					remaining.add(sci);
				//				}
				//			});
				//
				//			List<ServletContainerInitializer> list = new LinkedList<>();
				//			list.addAll(cdi);
				//			list.addAll(jettyWebSocket);
				//			list.addAll(jsp);
				//			list.addAll(remaining);
				//
				//			list.forEach(initializer -> {
				////				try {
				////					ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
				////							new Callable<Void>() {
				////								@Override
				////								public Void call() throws IOException,
				////										ServletException {
				////									_scontext.setExtendedListenerTypes(true);
				////									initializer.onStartup(servletContainerInitializers.get(initializer), _scontext);
				////									return null;
				////								}
				////							});
				////					// CHECKSTYLE:OFF
				////				} catch (Exception e) {
				////					if (e instanceof RuntimeException) {
				////						throw (RuntimeException) e;
				////					}
				////					LOG.error("Ignored exception during listener registration",
				////							e);
				////				}
				//			});
				//		}
				//
				//		this.setVirtualHosts(virtualHosts.toArray(EMPTY_STRING_ARRAY));
				//		if (jettyWebXmlURL != null) {
				//
				////			try {
				////				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
				////						new Callable<Void>() {
				////
				////							@Override
				////							public Void call() throws IOException {
				////								//do parsing and altering of webApp here
				////								DOMJettyWebXmlParser jettyWebXmlParser = new DOMJettyWebXmlParser();
				////								jettyWebXmlParser.parse(PaxWebServletContextHandler.this, jettyWebXmlURL.openStream());
				////
				////								return null;
				////							}
				////
				////						});
				////				//CHECKSTYLE:OFF
				////			} catch (Exception e) {
				////				if (e instanceof RuntimeException) {
				////					throw (RuntimeException) e;
				////				}
				////				LOG.error("Ignored exception during listener registration", e);
				////			}
				//			//CHECKSTYLE:ON
				//
				//		}
				//
				//		if (attributes != null) {
				//			for (Map.Entry<String, ?> attribute : attributes.entrySet()) {
				//				_scontext
				//						.setAttribute(attribute.getKey(), attribute.getValue());
				//			}
				//		}
				//		super.doStart();
				//		LOG.debug("Started servlet context for http context [" + httpContext
				//				+ "]");
				//	}

				//	private boolean isJspAvailable() {
				////		try {
				////			return (org.ops4j.pax.web.jsp.JspServletWrapper.class != null);
				////		} catch (NoClassDefFoundError ignore) {
				//		return false;
				////		}
				//	}

				//	@Override
				//	public void callContextInitialized(final ServletContextListener l,
				//			final ServletContextEvent e) {
				//		try {
				//			// toggle state of the dynamic API so that the listener cannot use
				//			// it
				//			if (isProgrammaticListener(l)) {
				//				this.getServletContext().setEnabled(false);
				//			}
				//
				//			if (LOG.isDebugEnabled()) {
				//				LOG.debug("contextInitialized: {}->{}", e, l);
				//			}
				//
				////			try {
				////				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
				////						new Callable<Void>() {
				////
				////							@Override
				////							public Void call() {
				////								l.contextInitialized(e);
				////								return null;
				////							}
				////
				////						});
				////				// CHECKSTYLE:OFF
				////			} catch (Exception ex) {
				////				if (ex instanceof RuntimeException) {
				////					throw (RuntimeException) ex;
				////				}
				////				LOG.error("Ignored exception during listener registration", e);
				////			}
				//
				//		} finally {
				//			// untoggle the state of the dynamic API
				//			this.getServletContext().setEnabled(true);
				//		}
				//	}

				//	@Override
				//	public boolean isProtectedTarget(String target) {
				//		// Fixes PAXWEB-196 and PAXWEB-211
				//		//CHECKSTYLE:OFF
				//		while (target.startsWith("//")) {
				//			target = URIUtil.compactPath(target);
				//		}
				//		//CHECKSTYLE:ON
				//
				//		return StringUtil.startsWithIgnoreCase(target, "/web-inf")
				//				|| StringUtil.startsWithIgnoreCase(target, "/meta-inf")
				//				|| StringUtil.startsWithIgnoreCase(target, "/osgi-inf")
				//				|| StringUtil.startsWithIgnoreCase(target, "/osgi-opt");
				//	}

				//	@Override
				//	protected void startContext() throws Exception {
				//		super.startContext();
				//		LOG.debug("Registering ServletContext as service. ");
				//		BundleContext bundleContext = (BundleContext) this.attributes.get(PaxWebConstants.BUNDLE_CONTEXT_ATTRIBUTE);
				//		Bundle bundle = bundleContext.getBundle();
				//		Dictionary<String, String> properties = new Hashtable<>();
				//		properties.put(PaxWebConstants.PROPERTY_SYMBOLIC_NAME, bundle.getSymbolicName());
				//
				//		Dictionary<?, ?> headers = bundle.getHeaders();
				//		String version = (String) headers
				//				.get(Constants.BUNDLE_VERSION);
				//		if (version != null && version.length() > 0) {
				//			properties.put("osgi.web.version", version);
				//		}
				//		// Context servletContext = context.getServletContext();
				//		String webContextPath = getContextPath();
				//		if (webContextPath != null && !webContextPath.startsWith("/")) {
				//			webContextPath = "/" + webContextPath;
				//		} else if (webContextPath == null) {
				//			LOG.warn(PaxWebConstants.PROPERTY_SERVLETCONTEXT_PATH +
				//					" couldn't be set, it's not configured. Assuming '/'");
				//			webContextPath = "/";
				//		}
				//		properties.put(PaxWebConstants.PROPERTY_SERVLETCONTEXT_PATH, webContextPath);
				//		properties.put(PaxWebConstants.PROPERTY_SERVLETCONTEXT_NAME, getServletContext().getServletContextName());
				//
				//		registerService(bundleContext, properties);
				//		LOG.debug("ServletContext registered as service. ");
				//	}

	/**
	 * <p>Extension of non-static inner {@link org.eclipse.jetty.servlet.ServletContextHandler.Context} class that
	 * allows customization of welcome files used by {@link org.ops4j.pax.web.service.spi.servlet.OsgiServletContext}
	 * scoped <em>resource servlets</em>.</p>
	 */
	public class CustomizedContext extends ServletContextHandler.Context {
		private final String[] welcomeFiles;

		public CustomizedContext(String[] welcomeFiles) {
			this.welcomeFiles = welcomeFiles;
		}

		public String[] getWelcomeFiles() {
			return welcomeFiles;
		}
	}

}
