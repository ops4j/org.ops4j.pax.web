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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultHttpContextMapping;

/**
 * Tracks {@link HttpContext}s.
 *
 * @author Alin Dreghiciu
 * @since 0.1.0, August 21, 2007
 */
public class HttpContextTracker
    extends AbstractHttpContextTracker<HttpContext>
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( HttpContextTracker.class );

    /**
     * Constructor.
     *
     * @param extenderContext extender context; cannot be null
     * @param bundleContext   whiteboard extender bundle context; cannot be null
     */
    private HttpContextTracker( final ExtenderContext extenderContext,
                               final BundleContext bundleContext )
    {
        super( extenderContext, bundleContext );
    }
    
	public static ServiceTracker<HttpContext,HttpContextMapping> createTracker(
			final ExtenderContext extenderContext, final BundleContext bundleContext) {
		return new HttpContextTracker(extenderContext, bundleContext).create( HttpContext.class);
	}

    /**
     * @see AbstractHttpContextTracker#createHttpContextMapping(ServiceReference, Object)
     */
    @Override
    HttpContextMapping createHttpContextMapping( final ServiceReference<HttpContext> serviceReference,
                                                 final HttpContext published )
    {
        Object httpContextId = serviceReference.getProperty( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID );
        if( httpContextId != null && ( !( httpContextId instanceof String )
                                       || ( (String) httpContextId ).trim().length() == 0 ) )
        {
            LOG.warn( "Registered http context [" + published + "] did not contain a valid http context id" );
            return null;
        }
        final DefaultHttpContextMapping mapping = new DefaultHttpContextMapping();
        mapping.setHttpContextId( (String) httpContextId );
        mapping.setHttpContext( published );
        return mapping;
    }

}
