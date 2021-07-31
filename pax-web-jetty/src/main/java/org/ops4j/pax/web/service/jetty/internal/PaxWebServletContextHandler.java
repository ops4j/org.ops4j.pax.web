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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
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

	// This collection will be ordered by rank/serviceId of the ContainerInitializerModels
	private final Collection<SCIWrapper> servletContainerInitializers = new TreeSet<>();

				private AccessControlContext accessControllerContext;
				private URL jettyWebXmlURL;
				private List<String> virtualHosts;

	private ServletContext osgiServletContext;

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

		// need to initialize the logger as super doStart is to late already
		setLogger(Log.getLogger(getDisplayName() == null ? getContextPath() : getDisplayName()));

		// "128.3.5 Static Content" is the only place where protected directories are mentioned. We'll handle them
		// at request processing stage and configure here
		setProtectedTargets(new String[] { "/WEB-INF", "/META-INF", "/OSGI-INF", "/OSGI-OPT" });
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

		// 2. Pax Web 7 was sorting the initializers - we don't have to do it in Pax Web 8

		// 3. Call the initializers - in startContext()

		// 4. TODO: virtual host handling - at this level (like Pax Web 7)?
//		this.setVirtualHosts(virtualHosts.toArray(EMPTY_STRING_ARRAY));

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
		// we have to fix this
		// TODO: file a Github issue for eclipse/jetty-project

		// here we remember the "durable" listeners
		List<EventListener> toKeep = new ArrayList<>();
		for (EventListener el : getEventListeners()) {
			if (isDurableListener(el)) {
				toKeep.add(el);
			}
		}
		super.doStop();

		// here we'll remove them and add again, so they're again marked as "durable", because
		// org.eclipse.jetty.server.handler.ContextHandler._durableListeners was cleared in super.doStop().
		for (EventListener el : toKeep) {
			removeEventListener(el);
			addEventListener(el);
		}
	}

}
