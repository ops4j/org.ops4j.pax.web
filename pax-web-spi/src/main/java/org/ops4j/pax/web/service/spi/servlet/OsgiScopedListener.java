/*
 * Copyright 2025 OPS4J.
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
package org.ops4j.pax.web.service.spi.servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.function.Supplier;

/**
 * A class that ensures correct delegation to underlying classloader for given listener.
 */
public class OsgiScopedListener implements ServletContextListener {

	private final ServletContextListener listener;
	private final ServletContext context;

	public OsgiScopedListener(ServletContextListener listener, ServletContext context) {
		this.listener = listener;
		this.context = context;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		this.listener.contextInitialized(new ServletContextEvent(this.context));
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		this.listener.contextDestroyed(new ServletContextEvent(this.context));
	}

	public static EventListener proxyListener(OsgiServletContext context, Supplier<ServletContext> contextSupplier,
											  EventListener listener, EventListenerModel eventListenerModel) {
		int implemented = 0;
		List<Class<?>> interfaces = new ArrayList<>();
		for (Class<?> c : EventListenerModel.SUPPORTED_LISTENER_CLASSES) {
			if (c.isAssignableFrom(listener.getClass())) {
				implemented++;
				interfaces.add(c);
			}
		}
		if (implemented == 0) {
			return listener;
		}
		if (ServletContextListener.class.isAssignableFrom(listener.getClass())) {
			Bundle bundle = eventListenerModel.getRegisteringBundle();
			if (bundle == null || bundle.getBundleContext() == null || bundle.adapt(BundleWiring.class) == null) {
				return listener;
			}
			OsgiScopedServletContext scopedContext = new OsgiScopedServletContext(context, bundle);
			scopedContext.setContextSupplier(contextSupplier);
			if (implemented == 1) {
				// easy - delegate
				return new OsgiScopedListener((ServletContextListener) listener, scopedContext);
			} else {
				// harder - proxy
				ClassLoader loader = bundle.adapt(BundleWiring.class).getClassLoader();
				return (EventListener) Proxy.newProxyInstance(loader, interfaces.toArray(new Class[0]), new Handler(listener, scopedContext));
			}
		}

		// no proxy needed
		return listener;
	}

	public ServletContextListener getDelegate() {
		return listener;
	}

	public static class Handler implements InvocationHandler {

		private final EventListener listener;
		private final OsgiScopedServletContext scopedContext;

		public Handler(EventListener listener, OsgiScopedServletContext scopedContext) {
			this.listener = listener;
			this.scopedContext = scopedContext;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ("contextInitialized".equals(method.getName()) || "contextDestroyed".equals(method.getName())) {
				return method.invoke(proxy, new ServletContextEvent(scopedContext));
			}
			return method.invoke(proxy, args);
		}

		public EventListener getDelegate() {
			return listener;
		}

	}

}
