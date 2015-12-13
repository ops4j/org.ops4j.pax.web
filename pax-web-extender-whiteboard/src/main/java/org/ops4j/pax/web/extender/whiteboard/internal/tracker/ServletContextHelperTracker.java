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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.WebApplication;
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
public class ServletContextHelperTracker<T> implements
		ServiceTrackerCustomizer<T, ServletContextHelper> {

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AbstractTracker.class);
	/**
	 * Extender context.
	 */
	private final ExtenderContext extenderContext;
	private final BundleContext bundleContext;

	/**
	 * Constructor.
	 * 
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	ServletContextHelperTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		// super( validateBundleContext( bundleContext ), createFilter(
		// bundleContext, trackedClass ), null );
		NullArgumentException.validateNotNull(extenderContext,
				"Extender context");
		this.extenderContext = extenderContext;
		this.bundleContext = validateBundleContext(bundleContext);
	}

	//static <T extends Servlet> ServiceTracker<T, ServletWebElement>
	public final ServiceTracker<T, ServletContextHelper> create(
			final Class<? extends T> trackedClass) {
		return new ServiceTracker<T, ServletContextHelper>(bundleContext,
				createFilter(bundleContext, trackedClass), this);
	}
	
    public static <T extends ServletContextHelper> ServiceTracker<T, ServletContextHelper> createTracker(
            final ExtenderContext extenderContext, final BundleContext bundleContext) {
        return new ServletContextHelperTracker<T>(extenderContext, bundleContext).create((Class<? extends T>) ServletContextHelper.class);
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
			final Class<?>... classes) {
		final StringBuilder filter = new StringBuilder();
		if (classes != null) {
			if (classes.length > 1) {
				filter.append("(|");
			}
			for (Class<?> clazz : classes) {
				filter.append("(").append(Constants.OBJECTCLASS).append("=")
						.append(clazz.getName()).append(")");
			}
			if (classes.length > 1) {
				filter.append(")");
			}
		}
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
	public ServletContextHelper addingService(
			final ServiceReference<T> serviceReference) {
		LOGGER.debug("Service available " + serviceReference);
		ServletContextHelper registered = (ServletContextHelper) bundleContext.getService(serviceReference);

		String servletCtxtName = ServicePropertiesUtils.getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
		
		if (servletCtxtName == null) {
		    return null; //skip as it's a mandatory property
		}
		
		String ctxtPath = ServicePropertiesUtils.getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
		if (ctxtPath == null) {
		    return null; //skip as it's a mandatory property
		}
		
		if (ctxtPath.startsWith("/")) {
		    ctxtPath = ctxtPath.substring(1);
		}
		
		final DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
        mapping.setHttpContextId((String) servletCtxtName);        
        mapping.setHttpContextShared(true);
        mapping.setPath(ctxtPath);
        Map<String, String> parameters = mapping.getParameters();
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        mapping.setParameters(parameters);
        
        parameters.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED, "true");
        bundleContext.registerService(HttpContextMapping.class, mapping, props);
        
        final WebApplication webApplication = extenderContext.getWebApplication(serviceReference.getBundle(), servletCtxtName, true);
		webApplication.setServletContextHandler(registered);
		
		return registered;
	}

	@Override
	public void modifiedService(ServiceReference<T> reference,
	        ServletContextHelper service) {
		// was not implemented before
	}

	/**
	 * @see ServiceTracker#removedService(ServiceReference,Object)
	 */
	@Override
	public void removedService(final ServiceReference<T> serviceReference,
			final ServletContextHelper unpublished) {
		LOGGER.debug("Service removed " + serviceReference);

//		Boolean sharedHttpContext = Boolean
//				.parseBoolean((String) serviceReference
//						.getProperty(ExtenderConstants.PROPERTY_HTTP_CONTEXT_SHARED));
//
//		final HttpContextMapping mapping = (HttpContextMapping) unpublished;
//		final WebApplication webApplication = extenderContext
//				.getExistingWebApplication(serviceReference.getBundle(),
//						mapping.getHttpContextId(), sharedHttpContext);
//		
//		boolean remove = true;
//		
//		if (sharedHttpContext) {
//			Integer sharedWebApplicationCounter = extenderContext.getSharedWebApplicationCounter(webApplication);
//			if (sharedWebApplicationCounter != null && sharedWebApplicationCounter > 0) {
//				remove = false;
//				Integer reduceSharedWebApplicationCount = extenderContext.reduceSharedWebApplicationCount(webApplication);
//				if (reduceSharedWebApplicationCount == 0) {
//					remove = true;
//				}
//			}
//		}
//		
//		if (webApplication != null && remove) {
//			webApplication.setHttpContextMapping(null);
//		}
	}

}
