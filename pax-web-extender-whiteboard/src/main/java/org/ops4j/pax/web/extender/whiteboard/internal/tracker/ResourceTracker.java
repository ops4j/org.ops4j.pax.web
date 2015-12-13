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

import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ResourceWebElement;
import org.ops4j.pax.web.extender.whiteboard.internal.util.ServicePropertiesUtils;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link EventListener}s.
 * 
 * @author Alin Dreghiciu
 * @since 0.4.0, April 05, 2008
 */
public class ResourceTracker extends
		AbstractTracker<Object, ResourceWebElement> {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ResourceTracker.class);

	/**
	 * Constructor.
	 * 
	 * @param extenderContext
	 *            extender context; cannot be null
	 * @param bundleContext
	 *            extender bundle context; cannot be null
	 */
	private ResourceTracker(final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		super(extenderContext, bundleContext);
	}

	@SuppressWarnings("unchecked")
	public static ServiceTracker<Object, ResourceWebElement> createTracker(
			final ExtenderContext extenderContext,
			final BundleContext bundleContext) {
		return new ResourceTracker(extenderContext, bundleContext)
				.create(new Class[] {
                        Object.class
                });
	}

	/**
	 * @see AbstractTracker#createWebElement(ServiceReference, Object)
	 */
	@Override
	ResourceWebElement createWebElement(
			final ServiceReference<Object> serviceReference,
			final Object published) {

	    String[] resourcePattern = ServicePropertiesUtils.getArrayOfStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
	    String prefix = ServicePropertiesUtils.getStringProperty(serviceReference, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);
	    
	    if (resourcePattern != null && prefix != null) {

    	    String httpContextId = ServicePropertiesUtils.getStringProperty(serviceReference, ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID);
    	    
            if (httpContextId == null) {
                String httpContextSelector = ServicePropertiesUtils.getStringProperty(serviceReference,HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
                if (httpContextSelector != null) {
                    httpContextSelector = httpContextSelector.substring(1, httpContextSelector.length());
                    httpContextId = httpContextSelector.substring(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME.length()+1);
                    httpContextId = httpContextId.substring(0, httpContextId.length()-1);
                }
            }
    	    
    		if (httpContextId != null && httpContextId.trim().length() == 0) {
    			LOG.warn("Registered listener [" + published
    					+ "] did not contain a valid http context id");
    			return null;
    		}
    		final DefaultResourceMapping mapping = new DefaultResourceMapping();
    		mapping.setHttpContextId((String) httpContextId);

    		mapping.setAlias(resourcePattern[0]); //TODO: make sure multiple patterns are supported
    		mapping.setPath(prefix);
    		
    		return new ResourceWebElement(mapping);
	    } else {
	        return null;
	    }
	}

}
