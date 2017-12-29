/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.EventListener;

import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ListenerWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultListenerMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link EventListener}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerTracker extends AbstractTracker<EventListener, ListenerWebElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ListenerTracker.class);

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private ListenerTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	@SuppressWarnings("unchecked")
	public static ServiceTracker<EventListener, ListenerWebElement> createTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new ListenerTracker(extenderContext, bundleContext).create(EventListener.class,
				ServletContextListener.class, ServletContextAttributeListener.class, ServletRequestListener.class,
				ServletRequestAttributeListener.class, HttpSessionListener.class, HttpSessionBindingListener.class,
				HttpSessionAttributeListener.class, HttpSessionActivationListener.class, AsyncListener.class,
				ReadListener.class, WriteListener.class, HttpSessionIdListener.class);
	}

	/**
	 * @see AbstractTracker#createWebElement(ServiceReference, Object)
	 */
	@Override
	ListenerWebElement createWebElement(final ServiceReference<EventListener> serviceReference,
			final EventListener published) {

		String httpContextId = ServicePropertiesUtils.extractHttpContextId(serviceReference);

		final DefaultListenerMapping mapping = new DefaultListenerMapping();
		mapping.setHttpContextId(httpContextId);
		mapping.setListener(published);
		return new ListenerWebElement<>(serviceReference, mapping);
	}

}
