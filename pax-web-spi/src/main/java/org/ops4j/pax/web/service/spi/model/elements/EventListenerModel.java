/*
 * Copyright 2007 Alin Dreghiciu.
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

import jakarta.servlet.AsyncListener;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContextAttributeListener;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingListener;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardWebContainerView;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.runtime.dto.FailedListenerDTO;
import org.osgi.service.servlet.runtime.dto.ListenerDTO;

import java.util.Arrays;
import java.util.EventListener;
import java.util.LinkedHashSet;
import java.util.Set;

public class EventListenerModel extends ElementModel<EventListener, EventListenerEventData> {

	public static final Class<?>[] SUPPORTED_LISTENER_CLASSES = new Class[] {
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

	private EventListener eventListener;

	// According to 140.7 Registering Listeners, there's no requirement to handle prototype-scoped listeners
	// However TCK tests that and we support it since 2025-04-01
	private EventListener resolvedListener;

	/**
	 * Flag used for models registered using {@link jakarta.servlet.ServletContext#addListener}
	 */
	private boolean dynamic = false;

	/** Flag for models which were targeted for different runtime */
	private boolean notMatched;

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

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	/**
	 * Method to be called by actual runtime to obtain an instance of the listener. With
	 * servlets and filters it is performed by runtime-specific <em>holder</em> classes which
	 * control the lifecycle of servlets/filters, but here we do it ourselves, as there's no
	 * lifecycle of the listener itself from the point of view of JakartaEE Servlets specification.
	 * @return
	 */
	public EventListener resolveEventListener() {
		if (resolvedListener != null) {
			if (!isPrototype()) {
				return resolvedListener;
			} else {
				getRegisteringBundle().getBundleContext().ungetService(getElementReference());
				resolvedListener = null;
			}
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
				BundleContext context = getRegisteringBundle().getBundleContext();
				if (context != null) {
					if (!isPrototype()) {
						resolvedListener = context.getService(getElementReference());
					} else {
						Bundle b = getRegisteringBundle();
						ServiceObjects<EventListener> so = b.getBundleContext().getServiceObjects(getElementReference());
						resolvedListener = so.getService();
					}
				}
			}
			if (resolvedListener == null) {
				dtoFailureCode = DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE;
			} else {
				dtoFailureCode = -1;
			}
			return resolvedListener;
		}
	}

	public void releaseEventListener() {
		if (getElementReference() != null) {
			BundleContext context = getRegisteringBundle().getBundleContext();
			if (context != null) {
				context.ungetService(getElementReference());
			}
		}
		resolvedListener = null;
	}

	public EventListener getResolvedListener() {
		return resolvedListener;
	}

	@Override
	public Boolean performValidation() {
		if (dtoFailureCode != -1) {
			throw new IllegalArgumentException("Event Listener Model is registered with invalid properties");
		}
		int sources = (eventListener != null ? 1 : 0);
		sources += (getElementReference() != null ? 1 : 0);
		sources += (getElementSupplier() != null ? 1 : 0);
		if (sources == 0) {
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			throw new IllegalArgumentException("Event Listener Model must specify one of: listener instance,"
					+ " listener supplier or listener reference");
		}
		if (sources != 1) {
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
			throw new IllegalArgumentException("Event Listener Model should specify a listener uniquely as instance,"
					+ " supplier or service reference");
		}

		dtoFailureCode = -1;
		return Boolean.TRUE;
	}

	public void setNotMatched() {
		this.notMatched = true;
	}

	public boolean isNotMatched() {
		return notMatched;
	}

	public ListenerDTO toDTO() {
		ListenerDTO dto = new ListenerDTO();
		// will be set later
		dto.servletContextId = 0L;
		dto.serviceId = getServiceId();
		Class<?> c = null;
		if (eventListener != null) {
			c = eventListener.getClass();
		} else if (getElementSupplier() != null) {
			c = getElementSupplier().get().getClass();
		} else if (getElementReference() != null) {
			dto.types = Utils.getObjectClasses(getElementReference());
		}
		if (c != null) {
			Set<Class<?>> interfaces = new LinkedHashSet<>();
			while (c != Object.class) {
				interfaces.addAll(Arrays.asList(c.getInterfaces()));
				c = c.getSuperclass();
			}
			dto.types = interfaces.stream().map(Class::getName).distinct().toArray(String[]::new);
		}
		return dto;
	}

	public FailedListenerDTO toFailedDTO(int dtoFailureCode) {
		FailedListenerDTO dto = new FailedListenerDTO();
		dto.servletContextId = 0L;
		dto.serviceId = getServiceId();
		Class<?> c = null;
		if (eventListener != null) {
			c = eventListener.getClass();
		} else if (getElementSupplier() != null) {
			c = getElementSupplier().get().getClass();
		} else if (getElementReference() != null) {
			dto.types = Utils.getObjectClasses(getElementReference());
		}
		if (c != null) {
			Set<Class<?>> interfaces = new LinkedHashSet<>();
			while (c != Object.class) {
				interfaces.addAll(Arrays.asList(c.getInterfaces()));
				c = c.getSuperclass();
			}
			dto.types = interfaces.stream().map(Class::getName).distinct().toArray(String[]::new);
		}
		dto.failureReason = dtoFailureCode;
		return dto;
	}

	@Override
	public String toString() {
		return "EventListenerModel{id=" + getId()
				+ (eventListener != null ? ",listener='" + eventListener + "'" : "")
				+ (getElementSupplier() != null ? ",supplier='" + getElementSupplier() + "'" : "")
				+ (getElementReference() != null ? ",reference='" + getElementReference() + "'" : "")
				+ ",contexts=" + getContextModelsInfo()
				+ "}";
	}

}
