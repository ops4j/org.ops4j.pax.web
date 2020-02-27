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

import java.util.HashMap;
import java.util.Map;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtendedHttpServiceRuntime;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.WebApplication;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletContextHelperElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
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
public class ServletContextHelperTracker<T extends ServletContextHelper> implements ServiceTrackerCustomizer<T, ServletContextHelperElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTracker.class);
	/**
	 * Extender context.
	 */
	private final ExtenderContext extenderContext;
	private final BundleContext bundleContext;
	private final ExtendedHttpServiceRuntime httpServiceRuntime;

	/**
	 * Constructor.
	 *  @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 * @param httpServiceRuntime
	 */
	private ServletContextHelperTracker(final ExtenderContext extenderContext,
										final BundleContext bundleContext,
										final ExtendedHttpServiceRuntime httpServiceRuntime) {
		// super( validateBundleContext( bundleContext ), createFilter(
		// bundleContext, trackedClass ), null );
		NullArgumentException.validateNotNull(extenderContext, "Extender context");
		this.extenderContext = extenderContext;
		this.bundleContext = validateBundleContext(bundleContext);
		this.httpServiceRuntime = httpServiceRuntime;
	}

	// static <T extends Servlet> ServiceTracker<T, ServletWebElement>
	public final ServiceTracker<T, ServletContextHelperElement> create(final Class<? extends T> trackedClass) {
		return new ServiceTracker<>(bundleContext, createFilter(bundleContext, trackedClass), this);
	}

	public static <T extends ServletContextHelper> ServiceTracker<T, ServletContextHelperElement> createTracker(
			final ExtenderContext extenderContext, final BundleContext bundleContext, ExtendedHttpServiceRuntime httpServiceRuntime) {
		return new ServletContextHelperTracker<T>(extenderContext, bundleContext, httpServiceRuntime)
				.create((Class<T>) ServletContextHelper.class);
	}

	/**
	 * Creates an OSGi filter for the classes.
	 *
	 * @param bundleContext
	 *            a bundle context
	 * @param classes
	 *            array of tracked classes
	 * @return osgi filter
	 */
	private static Filter createFilter(final BundleContext bundleContext, final Class<?>... classes) {
		final StringBuilder filter = new StringBuilder();
		if (classes != null) {
			if (classes.length > 1) {
				filter.append("(|");
			}
			for (Class<?> clazz : classes) {
				filter.append("(").append(Constants.OBJECTCLASS).append("=").append(clazz.getName()).append(")");
			}
			if (classes.length > 1) {
				filter.append(")");
			}
		}
		try {
			return bundleContext.createFilter(filter.toString());
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
	public ServletContextHelperElement addingService(final ServiceReference<T> serviceReference) {
		LOGGER.debug("ServletContextHelperService available " + serviceReference);
		T registered = bundleContext.getService(serviceReference);

		String servletCtxtName = ServicePropertiesUtils.getStringProperty(serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);


		String ctxtPath = ServicePropertiesUtils.getStringProperty(serviceReference,
				HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);

		if (ctxtPath != null && ctxtPath.startsWith("/")) {
			ctxtPath = ctxtPath.substring(1);
		}

		final DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
		mapping.setHttpContextId(servletCtxtName);
		mapping.setHttpContextShared(true);
		mapping.setPath(ctxtPath);
		Map<String, String> parameters = mapping.getInitParameters();
		if (parameters == null) {
			parameters = new HashMap<>();
		}
		mapping.setParameters(parameters);

		parameters.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");

//		Dictionary<String, Object> props = new Hashtable<>();
//		props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
//		bundleContext.registerService(HttpContextMapping.class, mapping, props);

		ServletContextHelperElement<T> servletContextHelperElement = new ServletContextHelperElement<>(serviceReference, mapping, registered);

		if (servletContextHelperElement.isValid()) {
			final WebApplication webApplication = extenderContext.getWebApplication(serviceReference.getBundle(),
					servletCtxtName, true);
			webApplication.setServletContextHelper(registered, mapping);
		}

		LOGGER.debug("Registering ServletContextHelper");

		httpServiceRuntime.addWhiteboardElement(servletContextHelperElement);
		return servletContextHelperElement;
	}

	@Override
	public void modifiedService(ServiceReference<T> reference, ServletContextHelperElement service) {
		// was not implemented before
	}

	/**
	 * @see ServiceTracker#removedService(ServiceReference, Object)
	 */
	@Override
	public void removedService(final ServiceReference<T> serviceReference, final ServletContextHelperElement unpublished) {
		LOGGER.debug("ServletContextHelperService removed " + serviceReference);

		if (unpublished.isValid()) {
			Boolean sharedHttpContext = ServicePropertiesUtils.extractSharedHttpContext(serviceReference);

			final WebApplication webApplication = extenderContext.getExistingWebApplication(
					serviceReference.getBundle(),
					unpublished.getHttpContextMapping().getContextId(),
					sharedHttpContext);

			boolean remove = true;

			if (sharedHttpContext) {
				LOGGER.debug("Shared Context ... ");
				Integer sharedWebApplicationCounter = extenderContext.getSharedWebApplicationCounter(webApplication);
				LOGGER.debug("... counter:" + sharedWebApplicationCounter);
				if (sharedWebApplicationCounter != null && sharedWebApplicationCounter > 0) {
					remove = false;
					Integer reduceSharedWebApplicationCount = extenderContext
							.reduceSharedWebApplicationCount(webApplication);
					LOGGER.debug("reduced counter:" + reduceSharedWebApplicationCount);
					if (reduceSharedWebApplicationCount == 0) {
						remove = true;
					}
				}
			}

			if (webApplication != null && remove) {
				webApplication.setServletContextHelper(null, null);
			}
			httpServiceRuntime.removeWhiteboardElement(unpublished);
		}
	}

}
