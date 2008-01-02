/* Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */
public final class Activator
    implements BundleActivator
{

    /**
     * HttpService reference.
     */
    private ServiceReference m_httpServiceRef;

    /**
     * Called when the OSGi framework starts our bundle
     */
    @SuppressWarnings( "unchecked")
    public void start( BundleContext bc )
        throws Exception
    {
        m_httpServiceRef = bc.getServiceReference( HttpService.class.getName() );
        if( m_httpServiceRef != null )
        {
            final HttpService httpService = (HttpService) bc.getService( m_httpServiceRef );
            if( httpService != null )
            {
                // create a default context to share between registrations
                final HttpContext httpContext = httpService.createDefaultHttpContext();
                // register the hello world servlet
                final Dictionary initParams = new Hashtable();
                initParams.put( "from", "HttpService" );
                httpService.registerServlet(
                    "/helloworld/hs",              // alias
                    new HelloWorldServlet(),    // registered servlet
                    initParams,                 // init params
                    httpContext                 // http context
                );
                // register images as resources
                httpService.registerResources(
                    "/images",
                    "/images",
                    httpContext
                );
            }
        }
    }

    /**
     * Called when the OSGi framework stops our bundle
     */
    public void stop( BundleContext bc )
        throws Exception
    {
        if( m_httpServiceRef != null )
        {
            bc.ungetService( m_httpServiceRef );
            m_httpServiceRef = null;
        }
    }
}

