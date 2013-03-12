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

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.WebApplication;
import org.ops4j.pax.web.extender.whiteboard.internal.element.WebElement;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
abstract class AbstractTracker<T, W extends WebElement> implements
		ServiceTrackerCustomizer<T, W> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(AbstractTracker.class);
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
	 * @param classes
	 *            array of class of the tracked objects; cannot be null
	 */
	AbstractTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		this.extenderContext = extenderContext;
		this.bundleContext = validateBundleContext(bundleContext);
	}

	protected final ServiceTracker<T, W> create(
			final Class<? extends T> trackedClass) {
		return new ServiceTracker<T, W>(bundleContext, createFilter(
				bundleContext, trackedClass), this);
	}

	/**
	 * Creates an OSGi filter for the classes.
	 * 
	 * @param bundleContext
	 *            a bundle context
	 * @param classes
	 *            array of tracked classes
	 * 
	 * @return osgi filter
	 */
	private static Filter createFilter(final BundleContext bundleContext,
			final Class<?> trackedClass) {
		final StringBuilder filter = new StringBuilder().append("(")
				.append(Constants.OBJECTCLASS).append("=")
				.append(trackedClass.getName()).append(")");
		try {
			return bundleContext.createFilter(filter.toString());
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException(
					"Unexpected InvalidSyntaxException: " + e.getMessage());
		}
	}

	/**
	 * Validates that the bundle context is not null. If null will throw
	 * IllegalArgumentException.
	 * 
	 * @param bundleContext
	 *            a bundle context
	 * 
	 * @return the bundle context if not null
	 */
	private static BundleContext validateBundleContext(
			final BundleContext bundleContext) {
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

		W webElement = createWebElement(serviceReference, registered);
		if (webElement != null) {
			String httpContextId = webElement.getHttpContextId();
			final WebApplication webApplication = extenderContext
					.getWebApplication(serviceReference.getBundle(),
							httpContextId);
			if (httpContextId == null
					&& !webApplication.hasHttpContextMapping()) {
				webApplication
						.setHttpContextMapping(new DefaultHttpContextMapping());
			}
			webApplication.addWebElement(webElement);
			return webElement;
		} else {
			// if no element was created release the service
			return null;
		}
	}

	@Override
	public void modifiedService(ServiceReference<T> reference, W service) {
		// This was never handled - what can be done here?
	}

	/**
	 * @see ServiceTrackerCustomizer#removedService(ServiceReference,Object)
	 */
	@Override
	public void removedService(final ServiceReference<T> serviceReference,
			final W unpublished) {
		LOG.debug("Service removed {}", serviceReference);
		final WebElement webElement = (WebElement) unpublished;
		final WebApplication webApplication = extenderContext
				.getWebApplication(serviceReference.getBundle(),
						webElement.getHttpContextId());
		if (webApplication != null) {
			webApplication.removeWebElement(webElement);
		}
	}

	/**
	 * Factory method for registrations corresponding to the published service.
	 * If the registration cannot be created from the published service (e.g.
	 * not enough metadata) the register method should return null, fact that
	 * will cancel the registration of the service. Aditionally it can log an
	 * error so the user is notified about the problem.
	 * 
	 * @param serviceReference
	 *            service reference for published service
	 * @param published
	 *            the actual published service
	 * 
	 * @return an Registration if could be created or applicable or null if not
	 */
	abstract W createWebElement(final ServiceReference<T> serviceReference,
			final T published);

}
