/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service.spi.model.elements;

import java.util.EventListener;

import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;

public class EventListenerModel extends ElementModel<EventListener, EventListenerEventData> {

	private EventListener eventListener;
	private EventListener resolvedListener;

	public EventListenerModel() {
	}

	public EventListenerModel(final EventListener eventListener) {
		this.eventListener = eventListener;
	}

	@Override
	public void register(WhiteboardWebContainerView view) {
		view.registerListener(this);
	}

	@Override
	public void unregister(WhiteboardWebContainerView view) {
		view.unregisterListener(this);
	}

	@Override
	public EventListenerEventData asEventData() {
		EventListenerEventData eventListenerEventData = new EventListenerEventData(eventListener);
		setCommonEventProperties(eventListenerEventData);
		return eventListenerEventData;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/**
	 * Method to be called by actual runtime to obtain an instance of the listener. With
	 * servlets and filters it is performed by runtime-specific <em>holder</em> classes which
	 * control the lifecycle of servlets/filters, but here we do it ourselves, as there's no
	 * lifecycle of the listener itself from the point of view of JavaEE Servlets specification.
	 * @return
	 */
	public EventListener resolveEventListener() {
		if (resolvedListener != null) {
			return resolvedListener;
		}
		synchronized (this) {
			if (resolvedListener != null) {
				return resolvedListener;
			}
			if (eventListener != null) {
				resolvedListener = eventListener;
			} else if (getElementSupplier() != null) {
				// check supplier
				resolvedListener = getElementSupplier().get();
			}
			if (getElementReference() != null) {
				// last, but official check
				resolvedListener = getRegisteringBundle().getBundleContext().getService(getElementReference());
			}
			return resolvedListener;
		}
	}

	/**
	 * When a listener is removed from native servlet container, it should be <em>unget</em> here - this
	 * is esiecially important with service references.
	 * @param listener
	 */
	public void ungetEventListener(EventListener listener) {
		if (listener == null || listener != resolvedListener) {
			return;
		}
		if (getElementReference() != null) {
			getRegisteringBundle().getBundleContext().ungetService(getElementReference());
		}
	}

	@Override
	public Boolean performValidation() {
		int sources = (eventListener != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		sources += (getElementSupplier() != null ? 1 : 0);
		if (sources == 0) {
			throw new IllegalArgumentException("Event Listener Model must specify one of: listener instance,"
					+ " listener supplier or listener reference");
		}
		if (sources != 1) {
			throw new IllegalArgumentException("Event Listener Model should specify a listener uniquely as instance,"
					+ " supplier or service reference");
		}
		return Boolean.TRUE;
	}

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

}
