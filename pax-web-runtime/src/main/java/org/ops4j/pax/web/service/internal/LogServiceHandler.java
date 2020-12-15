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

import java.util.concurrent.atomic.AtomicReference;

import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the redirect of ServletEvents to the {@link LogService}
 */
public class LogServiceHandler implements
		ServiceTrackerCustomizer<LogService, LogService>, WebElementEventListener {

	private static final Logger LOG = LoggerFactory
			.getLogger(LogServiceHandler.class);

	private AtomicReference<LogService> logServiceReference = new AtomicReference<>();

	private final BundleContext bundleContext;

	public LogServiceHandler(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Override
	public void registrationEvent(WebElementEvent servletEvent) {
//		final String topic;
//		switch (servletEvent.getType()) {
//			case WebEvent.DEPLOYING:
//				topic = WebTopic.DEPLOYING.toString();
//				break;
//			case WebEvent.DEPLOYED:
//				topic = WebTopic.DEPLOYED.toString();
//				break;
//			case WebEvent.UNDEPLOYING:
//				topic = WebTopic.UNDEPLOYING.toString();
//				break;
//			case WebEvent.UNDEPLOYED:
//				topic = WebTopic.UNDEPLOYED.toString();
//				break;
//			case WebEvent.WAITING:
//				// A Waiting Event is not supported by the specification
//				// therefore it is mapped to FAILED, because of collision.
//				//$FALL-THROUGH$
//			case WebEvent.FAILED:
//				//$FALL-THROUGH$
//			default:
//				topic = WebTopic.FAILED.toString();
//		}
//		LogService logService = logServiceReference.get();
//		if (logService != null) {
//			logService.log(LogService.LOG_DEBUG, topic);
//		} else {
//			LOG.debug(topic);
//		}

	}

	@Override
	public LogService addingService(ServiceReference<LogService> reference) {
		if (reference.isAssignableTo(bundleContext.getBundle(),
				"org.osgi.service.log.LogService")) {
			LogService logService = bundleContext.getService(reference);
			try {
				if (logService instanceof LogService) {
					LogService old = logServiceReference.getAndSet(logService);
					if (old != null) {
						LOG.debug(
								"replace old LogService instance {} by an instance of {}",
								old.getClass().getName(), logService.getClass()
										.getName());
					}
					return logService;
				}
			} catch (NoClassDefFoundError e) {
				LOG.warn("A LogService service was found, but the coresponding class can't be loaded, make sure to have a compatible org.osgi.service.log package package exported with version range [1.3,2.0)");
			}
			// If we came along here, we have no use of this service, so unget
			// it!
			bundleContext.ungetService(reference);
		} else {
			LOG.warn("A LogService service was found, but it is not assignable to this bundle, make sure to have a compatible org.osgi.service.log package package exported with version range [1.3,2.0)");
		}
		return null;
	}

	@Override
	public void modifiedService(ServiceReference<LogService> reference,
								LogService service) {
		// we don't care about properties
	}

	@Override
	public void removedService(ServiceReference<LogService> reference,
							   LogService service) {
		// What ever happens: We unget the service first
		bundleContext.ungetService(reference);
		try {
			if (service instanceof LogService) {
				// We only want to remove it if it is the current reference,
				// otherwhise it could be release and we keep the old one
				logServiceReference.compareAndSet(service, null);
			}
		} catch (NoClassDefFoundError e) {
			// we should never go here, but if this happens silently ignore it
		}

	}

}
