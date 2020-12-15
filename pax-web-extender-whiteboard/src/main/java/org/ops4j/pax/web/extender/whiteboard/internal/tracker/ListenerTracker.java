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

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks {@link EventListener}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerTracker extends AbstractElementTracker<EventListener, EventListener, EventListenerEventData, EventListenerModel> {

	private static final Class<?>[] SUPPORTED_LISTENER_CLASSES = new Class[] {
			// OSGi CMPN R7 Whiteboard Service
			ServletContextListener.class,
			ServletContextAttributeListener.class,
			ServletRequestListener.class,
			ServletRequestAttributeListener.class,
			HttpSessionAttributeListener.class,
			HttpSessionIdListener.class,
			HttpSessionListener.class,
			// Pax Web additions
			HttpSessionActivationListener.class,
			HttpSessionBindingListener.class,
			AsyncListener.class,
			ReadListener.class,
			WriteListener.class
	};

	private ListenerTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<EventListener, EventListenerModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {

		StringBuilder classes = new StringBuilder();
		for (Class<?> c : SUPPORTED_LISTENER_CLASSES) {
			classes.append("(objectClass=").append(c.getName()).append(")");
		}
		String filter = String.format("(&(|%s)(%s=*))", classes.toString(), HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
		return new ListenerTracker(whiteboardExtenderContext, bundleContext).create(filter);
	}

	@Override
	protected EventListenerModel createElementModel(ServiceReference<EventListener> serviceReference, Integer rank, Long serviceId) {
		EventListenerModel model = new EventListenerModel();
		model.setRegisteringBundle(serviceReference.getBundle());
		model.setElementReference(serviceReference);
		model.setServiceRank(rank);
		model.setServiceId(serviceId);
		return model;
	}

}
