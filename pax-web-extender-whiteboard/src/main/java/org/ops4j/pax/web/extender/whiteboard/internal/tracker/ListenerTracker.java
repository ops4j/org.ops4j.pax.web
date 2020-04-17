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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link EventListener}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerTracker /*extends AbstractElementTracker<EventListener, ListenerWebElement>*/ {

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
//	private ListenerTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}

//	/**
//	 * Constructs a new ListenerWebElement
//	 * @param ref the service-reference behind the registered http-whiteboard-service
//	 * @param listenerMapping ListenerMapping containing all necessary information
//	 */
//	public ListenerWebElement(final ServiceReference<T> ref, final ListenerMapping listenerMapping) {
//		super(ref);
//		NullArgumentException.validateNotNull(listenerMapping, "Listener mapping");
//		this.listenerMapping = listenerMapping;
//
//		// validate
//		final EventListener listener = listenerMapping.getListener();
//
//		if (!(listener instanceof ServletContextListener ||
//				listener instanceof ServletContextAttributeListener ||
//				listener instanceof ServletRequestListener ||
//				listener instanceof ServletRequestAttributeListener ||
//				listener instanceof HttpSessionListener ||
//				listener instanceof HttpSessionBindingListener ||
//				listener instanceof HttpSessionAttributeListener ||
//				listener instanceof HttpSessionActivationListener ||
//				listener instanceof AsyncListener ||
//				listener instanceof ReadListener ||
//				listener instanceof WriteListener ||
//				listener instanceof HttpSessionIdListener
//		)) {
//			valid = false;
//		}
//
//		if (listenerMapping.getHttpContextId() != null && listenerMapping.getHttpContextId().trim().length() == 0) {
//			LOG.warn("Registered listener [{}] did not contain a valid http context id.", getServiceID());
//			valid = false;
//		}
//
//		Boolean listenerEnabled = ServicePropertiesUtils.getBooleanProperty(
//				serviceReference,
//				HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
//		if (!Boolean.TRUE.equals(listenerEnabled)) {
//			LOG.warn("Registered listener [{}] is not enabled via 'osgi.http.whiteboard.listener' property.", getServiceID());
//			valid = false;
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	public static ServiceTracker<EventListener, ListenerWebElement> createTracker(final ExtenderContext extenderContext,
//			final BundleContext bundleContext) {
//		return new ListenerTracker(extenderContext, bundleContext).create(EventListener.class,
//				ServletContextListener.class, ServletContextAttributeListener.class, ServletRequestListener.class,
//				ServletRequestAttributeListener.class, HttpSessionListener.class, HttpSessionBindingListener.class,
//				HttpSessionAttributeListener.class, HttpSessionActivationListener.class, AsyncListener.class,
//				ReadListener.class, WriteListener.class, HttpSessionIdListener.class);
//	}
//
//	/**
//	 * @see AbstractElementTracker#createElementModel(ServiceReference, Object)
//	 */
//	@Override
//	ListenerWebElement createWebElement(final ServiceReference<EventListener> serviceReference,
//			final EventListener published) {
//
//		String httpContextId = ServicePropertiesUtils.extractHttpContextId(serviceReference);
//
//		final DefaultListenerMapping mapping = new DefaultListenerMapping();
//		mapping.setHttpContextId(httpContextId);
//		mapping.setListener(published);
//		return new ListenerWebElement<>(serviceReference, mapping);
//	}

//	@Override
//	public void register(final WebContainer webContainer,
//						 final HttpContext httpContext) throws Exception {
////		webContainer.registerEventListener(listenerMapping.getListener(), httpContext);
//	}
//
//	@Override
//	public void unregister(final WebContainer webContainer,
//						   final HttpContext httpContext) {
////		webContainer.unregisterEventListener(listenerMapping.getListener());
//	}

}
