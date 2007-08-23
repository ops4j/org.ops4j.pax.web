/*
 * Copyright 2007 Alin Dreghiciu.
 * Copyright 2007 Damian Golda.
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
package org.ops4j.pax.web.extender.internal;

import org.ops4j.pax.web.extender.Resources;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * Tracks resources published as services via a Service AbstractTracker and register/unregister them with an http service.
 *
 * @author Alin Dreghiciu
 * @author Damian Golda
 * @since August 21, 2007
 */
public class ResourcesTracker
    extends AbstractTracker<Resources, ResourcesRegistration>
{

    /**
     * Create a new tracker for the resources.
     *
     * @param bundleContext a bundle context
     */
    public ResourcesTracker( final BundleContext bundleContext )
    {
        super( bundleContext, Resources.class );
    }

    /**
     * Registers published resources with the active http service.
     *
     * @param httpService  the http service in use
     * @param published    the published resources
     * @param registration the registration corresponding to published resources
     *
     * @return true if successful
     */
    @Override
    boolean register( final HttpService httpService, final Resources published,
                      final ResourcesRegistration registration )
        throws Exception
    {
        httpService.registerResources(
            registration.getAlias(),
            published.getResources(),
            new DefaultHttpContext( registration.getBundle() )
        );
        // if there is no exception the registration was succesful
        return true;
    }

    /**
     * Factory method for alias object corresponding to the published resources.
     *
     * @param alias            the alias found in the properties of published resources
     * @param serviceReference service reference for published resources
     * @param published        the actual published resources
     *
     * @return an Registration
     */
    @Override
    ResourcesRegistration createRegistration( final String alias, final ServiceReference serviceReference,
                                              final Resources published )
    {
        return new ResourcesRegistration( alias, serviceReference.getBundle() );
    }

}
