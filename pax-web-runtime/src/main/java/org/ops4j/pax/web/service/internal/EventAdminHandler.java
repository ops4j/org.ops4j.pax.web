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

import org.ops4j.pax.web.service.spi.model.events.WebApplicationEvent;
import org.ops4j.pax.web.service.spi.model.events.WebApplicationEventListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This class listens to {@link WebApplicationEvent}s and redirect them to the
 * {@link EventAdmin} service according to chapter "128.5 Events" of OSGi CMPN R7 Specification.</p>
 *
 * <p>This handler is part of pax-web-runtime, but processes events related to WABs. No events related
 * to Whiteboard/HttpService element registration are passed to Event Admin (for now?) because
 * Whiteboard Specification (and HttpService Specification) doesn't mention anything about Event Admin.</p>
 */
public class EventAdminHandler implements WebApplicationEventListener, ServiceTrackerCustomizer<EventAdmin, EventAdmin> {

	private static final Logger LOG = LoggerFactory.getLogger(EventAdminHandler.class);

	private final AtomicReference<EventAdmin> eventAdminReference = new AtomicReference<>();
	private final BundleContext bundleContext;

	public EventAdminHandler(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public void webEvent(WebApplicationEvent event) {
		EventAdmin eventAdmin = eventAdminReference.get();
		if (eventAdmin != null) {
			final String topic = event.getType().getTopic();
			Dictionary<String, Object> properties = new Hashtable<>();

			// 128.5 Events
			properties.put("bundle", event.getBundle());
			properties.put("bundle.symbolicName", event.getBundleName());
			properties.put("bundle.id", event.getBundleId());
			properties.put("bundle.version", event.getBundleVersion());
			properties.put("context.path", event.getContextPath());
			properties.put("timestamp", event.getTimestamp());
			properties.put("extender.bundle", event.getExtenderBundle());
			properties.put("extender.bundle.id", event.getExtenderBundleId());
			properties.put("extender.bundle.symbolicName", event.getExtenderBundleName());
			properties.put("extender.bundle.version", event.getExtenderBundleVersion());

			if (event.getException() != null) {
				properties.put("exception", event.getException());
			}
			if (event.getCollisionIds() != null) {
				properties.put("collision", event.getContextPath());
				properties.put("collision.bundles", event.getCollisionIds());
			}

			Event ev = new Event(topic, properties);
			eventAdmin.postEvent(ev);
		}
	}

	@Override
	public EventAdmin addingService(ServiceReference<EventAdmin> reference) {
		if (reference.isAssignableTo(bundleContext.getBundle(), "org.osgi.service.event.EventAdmin")) {
			EventAdmin eventService = bundleContext.getService(reference);
			if (eventService != null) {
				EventAdmin old = eventAdminReference.getAndSet(eventService);
				if (old != null) {
					LOG.debug("replace old EventAdmin instance {} by an instance of {}",
							old.getClass().getName(), eventService.getClass().getName());
				}
				return eventService;
			}
		} else {
			LOG.warn("An EventAdmin service was found, but it is not assignable to this bundle, make sure to have a compatible org.osgi.service.event package exported.");
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<EventAdmin> reference, EventAdmin service) {
		// we don't care about properties
	}

	@Override
	public void removedService(ServiceReference<EventAdmin> reference, EventAdmin service) {
		bundleContext.ungetService(reference);
		// We only want to remove it if it is the current reference,
		// otherwhise it could be release and we keep the old one
		eventAdminReference.compareAndSet(service, null);
	}

}
