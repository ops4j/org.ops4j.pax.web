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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;

import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.ops4j.pax.web.service.spi.config.Configuration;
import org.ops4j.pax.web.service.spi.model.ServletContextModel;
import org.ops4j.pax.web.service.spi.servlet.SCIWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Pax Web specific {@link ServletContextHandler} representing single <em>web application</em>
 * deployed under unique <em>context path</em>, related 1:1 with single, unique
 * {@link ServletContextModel}
 */
public class PaxWebServletContextHandler extends ServletContextHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PaxWebServletContextHandler.class);

	private final Collection<SCIWrapper> servletContainerInitializers = new LinkedList<>();

				/**
				 * Access controller context of the bundle that registred the http context.
				 */
				private AccessControlContext accessControllerContext;


				private URL jettyWebXmlURL;

				private List<String> virtualHosts;

	/**
	 * Create a slightly extended version of Jetty's {@link ServletContextHandler}. It is still not as complex as
	 * {@code org.eclipse.jetty.webapp.WebAppContext} which does all the sort of XML/annotation configuration, but
	 * we take some of the mechanisms from {@code WebAppContext} if they're useful in Pax Web.
	 *
	 * @param parent
	 * @param contextPath
	 * @param sessions
	 * @param security
	 * @param configuration
	 */
	public PaxWebServletContextHandler(HandlerContainer parent, String contextPath, boolean sessions, boolean security,
			Configuration configuration) {
		super(parent, contextPath, sessions, security);

		// TCCL of sessionManager timer threads will be set to thread of pax-web-jetty bundle, not to current TCCL
		ScheduledExecutorScheduler executorScheduler = new ScheduledExecutorScheduler(getSessionHandler().toString() + "Timer", true,
				getClass().getClassLoader());
		_scontext.setAttribute("org.eclipse.jetty.server.session.timer", executorScheduler);

		// need to initialize the logger as super doStart is to late already
		setLogger(Log.getLogger(getDisplayName() == null ? getContextPath() : getDisplayName()));
	}

	public void setServletContainerInitializers(Collection<SCIWrapper> wrappers) {
		this.servletContainerInitializers.addAll(wrappers);
	}

	@Override
	protected void doStart() throws Exception {
		// 1. Pax Web 7 was explicitly adding org.ops4j.pax.web.jsp.JasperInitializer here, but we're no longer doing it
		//    WebAppContext in Jetty uses org.eclipse.jetty.webapp.JspConfiguration for this purpose

		// 2. TODO: Sort initializers as it was done in Pax Web 7?
//		final List<ServletContainerInitializer> cdi = new LinkedList<>();
//		final List<ServletContainerInitializer> jsp = new LinkedList<>();
//		final List<ServletContainerInitializer> jettyWebSocket = new LinkedList<>();
//		final List<ServletContainerInitializer> remaining = new LinkedList<>();
//
//		servletContainerInitializers.forEach(scim -> {
//			ServletContainerInitializer sci = scim.getContainerInitializer();
//			String className = sci.getClass().getName();
//			if ("org.ops4j.pax.cdi.web.impl.CdiServletContainerInitializer".equals(className)) {
//				cdi.add(sci);
//			} else if (className.startsWith("org.eclipse.jetty.websocket")) {
//				jettyWebSocket.add(sci);
//			} else if ("org.ops4j.pax.web.jsp.JasperInitializer".equals(className)) {
//				jsp.add(sci);
//			} else {
//				remaining.add(sci);
//			}
//		});
//
//		List<ServletContainerInitializer> list = new LinkedList<>();
//		list.addAll(cdi);
//		list.addAll(jettyWebSocket);
//		list.addAll(jsp);
//		list.addAll(remaining);

		// 3. Call the initializers
		servletContainerInitializers.forEach(wrapper -> {
			ClassLoader tccl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(getClassLoader());
				wrapper.onStartup();
			} catch (ServletException e) {
				LOG.error(e.getMessage(), e);
			} finally {
				Thread.currentThread().setContextClassLoader(tccl);
			}
		});

		// 4. TODO: virtual host handling - at this level (like Pax Web 7)?
//		this.setVirtualHosts(virtualHosts.toArray(EMPTY_STRING_ARRAY));

		// 5. TODO: specific Jetty XML? (rather not - it should be handled at the wrapper level, unless the context.xml
		//          is about the context itself (like jetty-web.xml)
//		if (jettyWebXmlURL != null) {
//			try {
//				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
//						new Callable<Void>() {
//
//							@Override
//							public Void call() throws IOException {
//								//do parsing and altering of webApp here
//								DOMJettyWebXmlParser jettyWebXmlParser = new DOMJettyWebXmlParser();
//								jettyWebXmlParser.parse(PaxWebServletContextHandler.this, jettyWebXmlURL.openStream());
//
//								return null;
//							}
//
//						});
//			} catch (Exception e) {
//				if (e instanceof RuntimeException) {
//					throw (RuntimeException) e;
//				}
//				LOG.error("Ignored exception during listener registration", e);
//			}
//		}

		// 6. Pax Web 7 was setting the attributes in real ServletContext, but we already do it according to
		//    Whiteboard specification at OsgiServletContext level

		// 7. Do super work
		super.doStart();
	}

//					@Override
//					public void callContextInitialized(final ServletContextListener l,
//							final ServletContextEvent e) {
//						try {
//							// toggle state of the dynamic API so that the listener cannot use
//							// it
//							if (isProgrammaticListener(l)) {
//								this.getServletContext().setEnabled(false);
//							}
//
//							if (LOG.isDebugEnabled()) {
//								LOG.debug("contextInitialized: {}->{}", e, l);
//							}
//
//							try {
//								ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
//										new Callable<Void>() {
//
//											@Override
//											public Void call() {
//												l.contextInitialized(e);
//												return null;
//											}
//
//										});
//								// CHECKSTYLE:OFF
//							} catch (Exception ex) {
//								if (ex instanceof RuntimeException) {
//									throw (RuntimeException) ex;
//								}
//								LOG.error("Ignored exception during listener registration", e);
//							}
//
//						} finally {
//							// untoggle the state of the dynamic API
//							this.getServletContext().setEnabled(true);
//						}
//					}

//					@Override
//					public boolean isProtectedTarget(String target) {
//						// Fixes PAXWEB-196 and PAXWEB-211
//						//CHECKSTYLE:OFF
//						while (target.startsWith("//")) {
//							target = URIUtil.compactPath(target);
//						}
//						//CHECKSTYLE:ON
//
//						return StringUtil.startsWithIgnoreCase(target, "/web-inf")
//								|| StringUtil.startsWithIgnoreCase(target, "/meta-inf")
//								|| StringUtil.startsWithIgnoreCase(target, "/osgi-inf")
//								|| StringUtil.startsWithIgnoreCase(target, "/osgi-opt");
//					}

}
