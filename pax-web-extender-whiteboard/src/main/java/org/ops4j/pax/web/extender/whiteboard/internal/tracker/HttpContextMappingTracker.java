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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.extender.whiteboard.HttpContextMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;

/**
 * Tracks {@link HttpContextMapping}s.
 *
 * @author Alin Dreghiciu
 * @since 0.4.0, April 06, 2008
 */
public class HttpContextMappingTracker
    extends AbstractHttpContextTracker<HttpContextMapping>
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( HttpContextTracker.class );

    /**
     * Constructor.
     *
     * @param extenderContext extender context; cannot be null
     * @param bundleContext   whiteboard extender bundle context; cannot be null
     */
    public HttpContextMappingTracker( final ExtenderContext extenderContext,
                                      final BundleContext bundleContext )
    {
        super( extenderContext, bundleContext, HttpContextMapping.class );
    }

    /**
     * @see AbstractHttpContextTracker#createHttpContextMapping(ServiceReference, Object)
     */
    @Override
    HttpContextMapping createHttpContextMapping( final ServiceReference serviceReference,
                                                 final HttpContextMapping published )
    {
        return published;
    }

}