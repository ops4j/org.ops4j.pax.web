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

import org.ops4j.pax.web.extender.whiteboard.internal.WhiteboardExtenderContext;
import org.ops4j.pax.web.service.spi.model.elements.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.events.EventListenerEventData;
import org.ops4j.pax.web.service.spi.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.runtime.dto.DTOConstants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link EventListener}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerTracker extends AbstractElementTracker<EventListener, EventListener, EventListenerEventData, EventListenerModel> {

	public static final Logger LOG = LoggerFactory.getLogger(ListenerTracker.class);

	private ListenerTracker(final WhiteboardExtenderContext whiteboardExtenderContext, final BundleContext bundleContext) {
		super(whiteboardExtenderContext, bundleContext);
	}

	public static ServiceTracker<EventListener, EventListenerModel> createTracker(final WhiteboardExtenderContext whiteboardExtenderContext,
			final BundleContext bundleContext) {

		StringBuilder classes = new StringBuilder();
		for (Class<?> c : EventListenerModel.SUPPORTED_LISTENER_CLASSES) {
			classes.append("(objectClass=").append(c.getName()).append(")");
		}
		String filter = String.format("(&(|%s)(%s=*))", classes.toString(), HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
		return new ListenerTracker(whiteboardExtenderContext, bundleContext).create(filter);
	}

	@Override
	protected EventListenerModel createElementModel(ServiceReference<EventListener> serviceReference, Integer rank, Long serviceId) {
		Boolean isListener = Utils.getBooleanProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
		Integer dtoFailureCode = null;
		if (isListener == null || !isListener) {
			// 140.7 Registering Listeners:
			// Events are sent to listeners registered in the Service Registry with the osgi.http.whiteboard.listener
			// service property set to true, independent of case.
			LOG.debug("Listener service reference doesn't have a property {}=true",
					HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
			dtoFailureCode = DTOConstants.FAILURE_REASON_VALIDATION_FAILED;
		}

		EventListenerModel model = new EventListenerModel();
		model.setRegisteringBundle(serviceReference.getBundle());
		model.setElementReference(serviceReference);
		model.setServiceRank(rank);
		model.setServiceId(serviceId);
		if (dtoFailureCode != null) {
			model.setDtoFailureCode(dtoFailureCode);
		}
		return model;
	}

}
