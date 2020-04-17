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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import org.ops4j.pax.web.service.whiteboard.ListenerMapping;

/**
 * Tracks {@link ListenerMapping}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerMappingTracker /*extends AbstractElementTracker<ListenerMapping, ListenerMappingWebElement>*/ {

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
//	private ListenerMappingTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		super(extenderContext, bundleContext);
//	}

//	/**
//	 * Constructs a new ListenerMappingWebElement
//	 * @param ref the service-reference behind the registered http-whiteboard-service
//	 * @param listenerMapping ListenerMapping containing all necessary information
//	 */
//	public ListenerMappingWebElement(final ServiceReference<ListenerMapping> ref, final ListenerMapping listenerMapping) {
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
//	public static ServiceTracker<ListenerMapping, ListenerMappingWebElement> createTracker(
//			final ExtenderContext extenderContext, final BundleContext bundleContext) {
//		return new ListenerMappingTracker(extenderContext, bundleContext).create(ListenerMapping.class);
//	}
//
//	/**
//	 * @see AbstractElementTracker#createWebElement(ServiceReference, Object)
//	 */
//	@Override
//	ListenerMappingWebElement createWebElement(final ServiceReference<ListenerMapping> serviceReference,
//			final ListenerMapping published) {
//		return new ListenerMappingWebElement(serviceReference, published);
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