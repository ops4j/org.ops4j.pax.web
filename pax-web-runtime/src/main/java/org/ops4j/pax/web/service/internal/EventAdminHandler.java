/*
 * Copyright 2013 Christoph Läubrich.
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
package org.ops4j.pax.web.service.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.web.service.spi.ServletEvent;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens to {@link ServletEvent}s and redirect them to the
 * {@link EventAdmin} service
 */
public class EventAdminHandler implements ServletListener,
		ServiceTrackerCustomizer<EventAdmin, EventAdmin> {

	private static final Logger LOG = LoggerFactory
			.getLogger(EventAdminHandler.class);

	private AtomicReference<EventAdmin> eventAdminReference = new AtomicReference<>();
	private final BundleContext bundleContext;

	public EventAdminHandler(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public void servletEvent(ServletEvent servletEvent) {
		EventAdmin eventAdmin = eventAdminReference.get();
		if (eventAdmin != null) {
			final String topic;
			switch (servletEvent.getType()) {
				case DEPLOYING:
					topic = WebEvent.WebTopic.DEPLOYING.toString();
					break;
				case DEPLOYED:
					topic = WebEvent.WebTopic.DEPLOYED.toString();
					break;
				case UNDEPLOYING:
					topic = WebEvent.WebTopic.UNDEPLOYING.toString();
					break;
				case UNDEPLOYED:
					topic = WebEvent.WebTopic.UNDEPLOYED.toString();
					break;
				case WAITING:
					// A Waiting Event is not supported by the specification
					// therefore it is mapped to FAILED, because of collision.
					//$FALL-THROUGH$
				case FAILED:
					//$FALL-THROUGH$
				default:
					topic = WebEvent.WebTopic.FAILED.toString();
			}
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put(
					"servlet.alias",
					servletEvent.getAlias() == null ? "" : servletEvent
							.getAlias());
			properties.put(
					"servlet.name",
					servletEvent.getServletName() == null ? "" : servletEvent
							.getServletName());
			properties.put(
					"servlet.urlparameter",
					servletEvent.getUrlParameter() == null ? "" : servletEvent
							.getUrlParameter());
			if (servletEvent.getServletClassName() != null) {
				properties.put("servlet.servlet", servletEvent.getServletClassName());
			}
			properties.put("timestamp", servletEvent.getTimestamp());
			if (servletEvent.getHttpContext() != null) {
				properties.put("servlet.httpcontext",
						servletEvent.getHttpContext());
			}
			Event event = new Event(topic, properties);
			eventAdmin.postEvent(event);
		}
	}

	@Override
	public EventAdmin addingService(ServiceReference<EventAdmin> reference) {
		if (reference.isAssignableTo(bundleContext.getBundle(),
				"org.osgi.service.event.EventAdmin")) {
			EventAdmin eventService = bundleContext.getService(reference);
			try {
				if (eventService instanceof EventAdmin) {
					EventAdmin old = eventAdminReference
							.getAndSet(eventService);
					if (old != null) {
						LOG.debug(
								"replace old EventAdmin instance {} by an instance of {}",
								old.getClass().getName(), eventService
										.getClass().getName());
					}
					return eventService;
				}
			} catch (NoClassDefFoundError e) {
				LOG.warn("An EventAdmin service was found, but the corresponding class can't be loaded, make sure to have a compatible org.osgi.service.event package exported with version range [1.3,2.0)");
			}
			// If we came along here, we have no use of this service, so unget
			// it!
			bundleContext.ungetService(reference);
		} else {
			LOG.warn("An EventAdmin service was found, but it is not assignable to this bundle, make sure to have a compatible org.osgi.service.event package exported with version range [1.3,2.0)");
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<EventAdmin> reference,
								EventAdmin service) {
		// we don't care about properties
	}

	@Override
	public void removedService(ServiceReference<EventAdmin> reference,
							   EventAdmin service) {
		// What ever happens: We unget the service first
		bundleContext.ungetService(reference);
		try {
			if (service instanceof EventAdmin) {
				// We only want to remove it if it is the current reference,
				// otherwhise it could be release and we keep the old one
				eventAdminReference.compareAndSet(service, null);
			}
		} catch (NoClassDefFoundError e) {
			// we should never go here, but if this happens silently ignore it
		}
	}

}
