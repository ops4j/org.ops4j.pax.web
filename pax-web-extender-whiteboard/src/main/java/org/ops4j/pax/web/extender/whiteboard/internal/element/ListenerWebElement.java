/*
 * Copyright 2007 Damian Golda.
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
package org.ops4j.pax.web.extender.whiteboard.internal.element;

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

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.whiteboard.ListenerMapping;
import org.ops4j.pax.web.service.spi.whiteboard.WhiteboardListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers/unregisters {@link ListenerMapping} with {@link WebContainer}.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ListenerWebElement<T extends EventListener> extends WebElement<T> implements WhiteboardListener {

	private static final Logger LOG = LoggerFactory.getLogger(ListenerWebElement.class);

	private final ListenerMapping listenerMapping;

	/**
	 * Constructs a new ListenerWebElement
	 * @param ref the service-reference behind the registered http-whiteboard-service
	 * @param listenerMapping ListenerMapping containing all necessary information
	 */
	public ListenerWebElement(final ServiceReference<T> ref, final ListenerMapping listenerMapping) {
		super(ref);
		NullArgumentException.validateNotNull(listenerMapping, "Listener mapping");
		this.listenerMapping = listenerMapping;

		// validate
		final EventListener listener = listenerMapping.getListener();

		if (!(listener instanceof ServletContextListener ||
				listener instanceof ServletContextAttributeListener ||
				listener instanceof ServletRequestListener ||
				listener instanceof ServletRequestAttributeListener ||
				listener instanceof HttpSessionListener ||
				listener instanceof HttpSessionBindingListener ||
				listener instanceof HttpSessionAttributeListener ||
				listener instanceof HttpSessionActivationListener ||
				listener instanceof AsyncListener ||
				listener instanceof ReadListener ||
				listener instanceof WriteListener ||
				listener instanceof HttpSessionIdListener
		)) {
			valid = false;
		}

		if (listenerMapping.getHttpContextId() != null && listenerMapping.getHttpContextId().trim().length() == 0) {
			LOG.warn("Registered listener [{}] did not contain a valid http context id.", getServiceID());
			valid = false;
		}

		Boolean listenerEnabled = ServicePropertiesUtils.getBooleanProperty(
				serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
		if (!Boolean.TRUE.equals(listenerEnabled)) {
			LOG.warn("Registered listener [{}] is not enabled via 'osgi.http.whiteboard.listener' property.", getServiceID());
			valid = false;
		}
	}

	@Override
	public void register(final WebContainer webContainer,
						 final HttpContext httpContext) throws Exception {
//		webContainer.registerEventListener(listenerMapping.getListener(), httpContext);
	}

	@Override
	public void unregister(final WebContainer webContainer,
						   final HttpContext httpContext) {
//		webContainer.unregisterEventListener(listenerMapping.getListener());
	}

	@Override
	public String getHttpContextId() {
		return listenerMapping.getHttpContextId();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "{mapping=" + listenerMapping + "}";
	}

	@Override
	public ListenerMapping getListenerMapping() {
		return listenerMapping;
	}
}
