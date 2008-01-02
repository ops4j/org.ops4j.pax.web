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
package org.ops4j.pax.web.samples.helloworld.wc.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.pax.web.service.WebContainerConstants;

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
     * WebContainer reference.
     */
    private ServiceReference m_webContainerRef;

    /**
     * Called when the OSGi framework starts our bundle
     */
    @SuppressWarnings( "unchecked" )
    public void start( BundleContext bc )
        throws Exception
    {
        m_webContainerRef = bc.getServiceReference( WebContainer.class.getName() );
        if( m_webContainerRef != null )
        {
            final WebContainer webContainer = (WebContainer) bc.getService( m_webContainerRef );
            if( webContainer != null )
            {
                // create a default context to share between registrations
                final HttpContext httpContext = webContainer.createDefaultHttpContext();
                // register the hello world servlet for filtering with url pattern
                final Dictionary initParamsServlet = new Hashtable();
                initParamsServlet.put( "from", "WebContainer" );
                webContainer.registerServlet(
                    new HelloWorldServlet(),                // registered servlet
                    new String[]{ "/helloworld/wc/*" },     // url patterns
                    initParamsServlet,                      // init params
                    httpContext                             // http context
                );
                // register the hello world filter based on url paterns
                final Dictionary initParamsFilter = new Hashtable();
                initParamsFilter.put( "title", "Hello World (url pattern)" );
                webContainer.registerFilter(
                    new HelloWorldFilter(),                 // registered filter
                    new String[]{ "/helloworld/wc" },     // url patterns
                    null,                                   // servlet names
                    initParamsFilter,                       // init params
                    httpContext                             // http context
                );
                // register the hello world servlet for filtering with servlet name
                initParamsServlet.put( WebContainerConstants.SERVLET_NAME, "HelloWorld" );
                webContainer.registerServlet(
                    new HelloWorldServlet(),                // registered servlet
                    new String[]{ "/helloworld/wc/sn/*" },     // url patterns
                    initParamsServlet,                      // init params
                    httpContext                             // http context
                );
                // register the hello world filter based on servlet name
                initParamsFilter.put( "title", "Hello World (servlet name)" );
                webContainer.registerFilter(
                    new HelloWorldFilter(),                 // registered filter
                    null,                                   // url patterns
                    new String[]{ "HelloWorld" },           // servlet names
                    initParamsFilter,                       // init params
                    httpContext                             // http context
                );
                // register a request listener
                webContainer.registerEventListener(
                    new HelloWorldListener(),               // registered listener
                    httpContext                             // http context
                );
                // register images as resources
                webContainer.registerResources(
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
        if( m_webContainerRef != null )
        {
            bc.ungetService( m_webContainerRef );
            m_webContainerRef = null;
        }
    }
}

