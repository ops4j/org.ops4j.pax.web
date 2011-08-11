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
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.annotations.AnnotationParser;
import org.eclipse.jetty.annotations.ClassInheritanceHandler;
import org.eclipse.jetty.annotations.ContainerInitializerAnnotationHandler;
import org.eclipse.jetty.annotations.ContainerInitializerConfiguration;
import org.eclipse.jetty.annotations.WebFilterAnnotationHandler;
import org.eclipse.jetty.annotations.WebListenerAnnotationHandler;
import org.eclipse.jetty.annotations.WebServletAnnotationHandler;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.Configuration;
import org.ops4j.pax.swissbox.core.ContextClassLoaderUtils;
import org.ops4j.pax.web.service.WebContainerContext;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HttpServiceContext extends ServletContextHandler {
	// class HttpServiceContext extends WebAppContext {

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServiceContext.class);

	/**
	 * Context attributes.
	 */
	private final Map<String, Object> m_attributes;
	private final HttpContext m_httpContext;
	/**
	 * Access controller context of the bundle that registred the http context.
	 */
	private final AccessControlContext m_accessControllerContext;
	
	private final List<ServletContainerInitializer> servletContainerInitializers;

	HttpServiceContext(final HandlerContainer parent,
			final Map<String, String> initParams,
			final Map<String, Object> attributes, final String contextName,
			final HttpContext httpContext,
			final AccessControlContext accessControllerContext) {
		super(parent, "/" + contextName, SESSIONS | SECURITY);
		// super(parent, null, "/" + contextName );
		getInitParams().putAll(initParams);
		m_attributes = attributes;
		m_httpContext = httpContext;
		m_accessControllerContext = accessControllerContext;
		servletContainerInitializers = new ArrayList<ServletContainerInitializer>();

		_scontext = new SContext();
		setServletHandler(new HttpServiceServletHandler(httpContext));
		setErrorHandler(new ErrorPageErrorHandler());
	}

	@Override
	protected void doStart() throws Exception {
//		configure();
		
		Set<Class<?>> clazzes = null; //Annotatet classess should be in here

		for (ServletContainerInitializer servletContainerInitializer : servletContainerInitializers) {
			servletContainerInitializer.onStartup(clazzes, _scontext);
		}
		
		super.doStart();
		if (m_attributes != null) {
			for (Map.Entry<String, ?> attribute : m_attributes.entrySet()) {
				_scontext
						.setAttribute(attribute.getKey(), attribute.getValue());
			}
		}
		LOG.debug("Started servlet context for http context [" + m_httpContext
				+ "]");
	}

	/*
	private void configure() {
		
		
		AnnotationParser parser = new AnnotationParser();
        //Discoverable annotations - those that you have to look for without loading a class
        
		// TODO: different Handlers needed 
		parser.registerAnnotationHandler("javax.servlet.annotation.WebServlet", new WebServletAnnotationHandler(context));
        parser.registerAnnotationHandler("javax.servlet.annotation.WebFilter", new WebFilterAnnotationHandler(context));
        parser.registerAnnotationHandler("javax.servlet.annotation.WebListener", new WebListenerAnnotationHandler(context));
        
        ClassInheritanceHandler classHandler = new ClassInheritanceHandler();
        parser.registerClassHandler(classHandler);
        
        registerServletContainerInitializer(parser);
        
	}
	*/

	private void registerServletContainerInitializer(AnnotationParser parser) {
		
		//We use the ServiceLoader mechanism to find the ServletContainerInitializer classes to inspect
        ServiceLoader<ServletContainerInitializer> loadedInitializers = ServiceLoader.load(ServletContainerInitializer.class, _scontext.getClassLoader());
       
        if (loadedInitializers != null)
        {
            for (ServletContainerInitializer service : loadedInitializers)
            {
//                if (!isFromExcludedJar(context, service))
//                { 
                    HandlesTypes annotation = service.getClass().getAnnotation(HandlesTypes.class);
//                    ContainerInitializer initializer = new ContainerInitializer();
//                    initializer.setTarget(service);
                    servletContainerInitializers.add(service);
//                    initializers.add(initializer);
                    if (annotation != null)
                    {
                        Class[] classes = annotation.value();
                        if (classes != null)
                        {
//                            initializer.setInterestedTypes(classes);
                            for (Class c: classes)
                            {
                                if (c.isAnnotation())
                                {
                                    if (Log.isDebugEnabled()) Log.debug("Registering annotation handler for "+c.getName());
                                   // parser.registerAnnotationHandler(c.getName(), new ContainerInitializerAnnotationHandler(initializer, c)); //TODO: better handler needed
                                }
                            }
                        }
                        else
                            if (Log.isDebugEnabled()) Log.debug("No classes in HandlesTypes on initializer "+service.getClass());
//                    }
//                    else
//                        if (Log.isDebugEnabled()) Log.debug("No annotation on initializer "+service.getClass());
                }
            }
        }
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		LOG.debug("Stopped servlet context for http context [" + m_httpContext
				+ "]");
	}

	@Override
	public void doHandle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		LOG.debug("Handling request for [" + target + "] using http context ["
				+ m_httpContext + "]");
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
	 * If the listener is a servlet conetx listener and the context is already
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
		if (isStarted() && listener instanceof ServletContextListener) {
			try {
				ContextClassLoaderUtils.doWithClassLoader(getClassLoader(),
						new Callable<Void>() {

							public Void call() {
								((ServletContextListener) listener)
										.contextInitialized(new ServletContextEvent(
												_scontext));
								return null;
							}

						});
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				LOG.error("Ignored exception during listener registration", e);
			}
		}
	}

	@Override
	protected boolean isProtectedTarget(String target) { // Fixes PAXWEB-196 and
															// PAXWEB-211
		while (target.startsWith("//"))
			target = URIUtil.compactPath(target);

		return StringUtil.startsWithIgnoreCase(target, "/web-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/meta-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/osgi-inf")
				|| StringUtil.startsWithIgnoreCase(target, "/osgi-opt");
	}

	@Override
	public String toString() {
		return new StringBuilder().append(this.getClass().getSimpleName())
				.append("{").append("httpContext=").append(m_httpContext)
				.append("}").toString();
	}
	
	public class SContext extends Context {

		@Override
		public String getRealPath(final String path) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("getting real path: [" + path + "]");
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
							if (LOG.isDebugEnabled()) {
								LOG.debug("found real path: [" + realPath + "]");
							}
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
							public URL run() throws Exception {
								return m_httpContext.getResource(p);
							}
						}, m_accessControllerContext);
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
								public InputStream run() throws Exception {
									try {
										return url.openStream();
									} catch (IOException e) {
										LOG.warn("URL canot be accessed: "
												+ e.getMessage());
									}
									return null;
								}

							}, m_accessControllerContext);
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
		@Override
		public Set getResourcePaths(final String path) {
			if (m_httpContext instanceof WebContainerContext) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("getting resource paths for : [" + path + "]");
				}
				try {
					final Set<String> paths = AccessController.doPrivileged(
							new PrivilegedExceptionAction<Set<String>>() {
								public Set<String> run() throws Exception {
									return ((WebContainerContext) m_httpContext)
											.getResourcePaths(path);
								}
							}, m_accessControllerContext);
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
			return m_httpContext.getMimeType(name);
		}

	}

}
