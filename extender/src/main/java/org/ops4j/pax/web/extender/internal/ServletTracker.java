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
package org.ops4j.pax.web.extender.internal;

import java.util.Dictionary;
import javax.servlet.Servlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Tracks servlet published as services via a Service AbstractTracker and register/unregister them with an http service.
 *
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public class ServletTracker
    extends AbstractTracker<Servlet, Registration>
{

    /**
     * Create a new tracker for the servlets.
     *
     * @param bundleContext a bundle context
     */
    public ServletTracker( final BundleContext bundleContext )
    {
        super( bundleContext, Servlet.class );
    }

    /**
     * Registers a published servlet with the active http service.
     *
     * @param httpService  the http service in use
     * @param published    the published servlet
     * @param registration the registration corresponding to published servlet
     *
     * @return true if successful
     */
    @Override
    boolean register( final HttpService httpService, final Servlet published, final Registration registration )
        throws Exception
    {
        httpService.registerServlet( registration.getAlias(), published, (Dictionary) null, (HttpContext) null );
        // if there is no exception the registration was succesful
        return true;
    }

    /**
     * Factory method for alias object corresponding to the published servlet.
     *
     * @param alias            the alias found in the properties of published servlet
     * @param serviceReference service reference for published servlet
     * @param published        the actual published servlet
     *
     * @return an Registration
     */
    @Override
    Registration createRegistration( final String alias, final ServiceReference serviceReference,
                                     final Servlet published )
    {
        return new Registration( alias );
    }

}
