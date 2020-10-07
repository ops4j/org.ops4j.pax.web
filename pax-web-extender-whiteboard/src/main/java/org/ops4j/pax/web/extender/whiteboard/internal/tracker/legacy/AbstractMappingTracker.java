/*
 * Copyright 2020 OPS4J.
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
package org.ops4j.pax.web.extender.whiteboard.internal.tracker.legacy;

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardContext;
import org.ops4j.pax.web.extender.whiteboard.internal.tracker.AbstractElementTracker;
import org.ops4j.pax.web.service.spi.model.elements.ElementModel;
import org.ops4j.pax.web.service.spi.model.events.ElementEventData;
import org.ops4j.pax.web.service.whiteboard.ContextRelated;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * Special tracker for those <em>incoming</em> we elements which extend {@link ContextRelated}
 *
 * @param <S>
 * @param <R>
 * @param <D>
 * @param <T>
 */
public abstract class AbstractMappingTracker<S extends ContextRelated, R, D extends ElementEventData, T extends ElementModel<R, D>>
		extends AbstractElementTracker<S, R, D, T> {

	protected AbstractMappingTracker(WhiteboardContext whiteboardContext, BundleContext bundleContext) {
		super(whiteboardContext, bundleContext);
	}

	@Override
	protected String determineSelector(boolean legacyMapping, String legacyId, String selector, ServiceReference<S> serviceReference) {
		selector = super.determineSelector(legacyMapping, legacyId, selector, serviceReference);

		if (selector == null) {
			// we have to take contextId/selector from the ContextRelated service itself - that's available
			// only for "legacy mapping registration"

			S service = null;
			try {
				service = dereference(serviceReference);

				selector = service.getContextSelectFilter();
				legacyId = service.getContextId();

				// repeated validation, but for id/selector from the mapping itself.
				if (selector != null && legacyId != null) {
					log.warn("Both context id={} and context selector={} are specified. Using {}.",
							legacyId, selector, selector);
					legacyId = null;
				}

				if (selector == null && legacyId != null) {
					// 140.10 Integration with Http Service Contexts
					selector = String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_SERVICE_CONTEXT_PROPERTY, legacyId);
				}

				if (selector == null) {
					// just "default"
					selector = String.format("(%s=%s)", HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
							HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME);
				}
			} finally {
				if (service != null) {
					// the service was obtained only to extract contextId/selector out of it, so we have to unget()
					bundleContext.ungetService(serviceReference);
				}
			}
		}

		return selector;
	}

	@Override
	protected final T createElementModel(ServiceReference<S> serviceReference, Integer rank, Long serviceId) {

		// for services extending org.ops4j.pax.web.service.whiteboard.ContextRelated, we ALWAYS
		// get the service, unlike with new Whiteboard CMPN specification, where service is obtained
		// just before registration - possibly to register it inside more than one ServletContext

		S service = null;
		try {
			service = dereference(serviceReference);
			log.debug("Creating web element model from legacy whiteboard service {} (id={}): {}",
					serviceReference, serviceId, service);

			return doCreateElementModel(serviceReference.getBundle(), service, rank, serviceId);
		} finally {
			if (service != null) {
				// the service was obtained only to extract the data out of it, so we have to unget()
				bundleContext.ungetService(serviceReference);
			}
		}
	}

	/**
	 * This method has to be implemented by subclass, but implementations can be sure that {@code contexts} are
	 * already resolved, even if specified via e.g., {@link ContextRelated#getContextSelectFilter()}
	 *
	 * @param service
	 * @param rank
	 * @param serviceId
	 * @return
	 */
	protected abstract T doCreateElementModel(Bundle bundle, S service, Integer rank, Long serviceId);

}
