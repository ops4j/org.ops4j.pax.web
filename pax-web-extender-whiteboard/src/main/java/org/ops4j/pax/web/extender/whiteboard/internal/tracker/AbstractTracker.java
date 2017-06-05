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

import javax.servlet.Servlet;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.WebApplication;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.osgi.framework.*;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks objects published as services via a Service Tracker.
 *
 * @author Alin Dreghiciu
 * @since 0.2.0, August 21, 2007
 */
abstract class AbstractTracker<T, W extends WebElement> implements ServiceTrackerCustomizer<T, W> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTracker.class);
	
	/**
	 * Track any service.
	 */
	private static final String ANY_SERVICE = "(" + Constants.OBJECTCLASS + "=*)";
	
	/**
	 * Extender context.
	 */
	private final ExtenderContext extenderContext;

	/**
	 * Extender context.
	 */
	private final BundleContext bundleContext;

	/**
	 * Constructor.
	 *
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	AbstractTracker(final ExtenderContext extenderContext, final BundleContext bundleContext) {
		this.extenderContext = extenderContext;
		this.bundleContext = validateBundleContext(bundleContext);
	}

	protected final ServiceTracker<T, W> create(final Class<? extends T> trackedClass) {
		return new ServiceTracker<>(bundleContext, createFilter(bundleContext, trackedClass), this);
	}
	
	/**
	 * Creates a new tracker that tracks the defined service types.
	 *
	 * @param trackedClass
	 *            the classes defining the service types to track; an empty array tracks any service
	 * @return a configured osgi service tracker
	 */
	@SafeVarargs
	protected final ServiceTracker<T, W> create(final Class<? extends T>... trackedClass) {
		return new ServiceTracker<>(bundleContext, createFilter(bundleContext, trackedClass), this);
	}

	/**
	 * Creates a new tracker that tracks services by generic filter
	 *
	 * @param filter generic filter to use for tracker
	 * @return a configured osgi service tracker
	 */
	protected final ServiceTracker<T, W> create(String filter) {
		try {
			return new ServiceTracker<>(bundleContext, bundleContext.createFilter(filter), this);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unexpected InvalidSyntaxException: " + e.getMessage());
		}
	}

	/**
	 * Creates an OSGi filter for the classes.
	 *
	 * @param bundleContext
	 *            a bundle context
	 * @param trackedClass
	 *            the class being tracked
	 * @return osgi filter
	 */
	private static Filter createFilter(final BundleContext bundleContext, final Class<?> trackedClass) {
		final String filter = "(" + Constants.OBJECTCLASS + "=" + trackedClass.getName() + ")";
		try {
			return bundleContext.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unexpected InvalidSyntaxException: " + e.getMessage());
		}
	}

	private static Filter createFilter(final BundleContext bundleContext, final Class<?>... trackedClass) {
		if (trackedClass.length == 1) {
			return createFilter(bundleContext, trackedClass[0]);
		}

		String filter;
		if (trackedClass.length == 0) {
			filter = ANY_SERVICE;
		} else {
			StringBuilder filterBuilder = new StringBuilder();
			filterBuilder.append("(|");
			for (Class<?> clazz : trackedClass) {
				filterBuilder.append("(").append(Constants.OBJECTCLASS).append("=").append(clazz.getName()).append(")");
			}
			filterBuilder.append(")");
			filter = filterBuilder.toString();
		}

		try {
			return bundleContext.createFilter(filter);
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Unexpected InvalidSyntaxException: " + e.getMessage());
		}
	}

	/**
	 * Validates that the bundle context is not null. If null will throw
	 * IllegalArgumentException.
	 *
	 * @param bundleContext
	 *            a bundle context
	 * @return the bundle context if not null
	 */
	private static BundleContext validateBundleContext(final BundleContext bundleContext) {
		NullArgumentException.validateNotNull(bundleContext, "Bundle context");
		return bundleContext;
	}

	/**
	 * @see ServiceTracker#addingService(ServiceReference)
	 */
	@Override
	public W addingService(final ServiceReference<T> serviceReference) {
		LOG.debug("Service available {}", serviceReference);
		T registered = bundleContext.getService(serviceReference);

		Boolean sharedHttpContext = ServicePropertiesUtils.extractSharedHttpContext(serviceReference);

		W webElement = createWebElement(serviceReference, registered);
		if (webElement != null) {
			String httpContextId = webElement.getHttpContextId();
			final WebApplication webApplication = extenderContext.getWebApplication(serviceReference.getBundle(),
					httpContextId, sharedHttpContext);
            if (httpContextId == null && !webApplication.hasHttpContextMapping()
                    // PAXWEB-1090 create DefaultHttpContext when default-whiteboard-contextId available without mapping
                    || HttpWhiteboardConstants.HTTP_WHITEBOARD_DEFAULT_CONTEXT_NAME.equalsIgnoreCase(httpContextId)) {
				webApplication.setHttpContextMapping(new DefaultHttpContextMapping());
			}
			webApplication.addWebElement(webElement);
			return webElement;
		} else {
			// if no element was created release the service
			bundleContext.ungetService(serviceReference);
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<T> reference, W service) {
		// This was never handled - what can be done here?
	}

	/**
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference, Object)
	 */
	@Override
	public void removedService(final ServiceReference<T> serviceReference, final W webElement) {
		LOG.debug("Service removed {}", serviceReference);

		Boolean sharedHttpContext = ServicePropertiesUtils.extractSharedHttpContext(serviceReference);

		final WebApplication webApplication = extenderContext.getExistingWebApplication(serviceReference.getBundle(),
				webElement.getHttpContextId(), sharedHttpContext);
		boolean remove = true;

		if (sharedHttpContext) {
			LOG.debug("Shared Context ... ");
			Integer sharedWebApplicationCounter = extenderContext.getSharedWebApplicationCounter(webApplication);
			LOG.debug("... counter:"+sharedWebApplicationCounter);
			if (sharedWebApplicationCounter != null && sharedWebApplicationCounter > 0) {
				remove = false;
				Integer reduceSharedWebApplicationCount = extenderContext
						.reduceSharedWebApplicationCount(webApplication);
				LOG.debug("reduced counter:"+reduceSharedWebApplicationCount);
				if (reduceSharedWebApplicationCount == 0) {
					remove = true;
				}
			}

			T registered = bundleContext.getService(serviceReference);
			if (!remove && Servlet.class.isAssignableFrom(registered.getClass())) {
				// special case where the removed service is a servlet, all
				// other filters etc. should be stopped now too.
				remove = true;
			}
			LOG.debug("service can be removed: "+remove);
			bundleContext.ungetService(serviceReference);
		}

		if (webApplication != null && remove) {
			if (webApplication.removeWebElement(webElement)) {
				extenderContext.removeWebApplication(webApplication);
			}
		}
	}

	/**
	 * Factory method for registrations corresponding to the published service.
	 * If the registration cannot be created from the published service (e.g.
	 * not enough metadata) the register method should return null, fact that
	 * will cancel the registration of the service. Additionally it can log an
	 * error so the user is notified about the problem.
	 *
	 * @param serviceReference
	 *            service reference for published service
	 * @param published
	 *            the actual published service
	 * @return an Registration if could be created or applicable or null if not
	 */
	abstract W createWebElement(final ServiceReference<T> serviceReference, final T published);

}
