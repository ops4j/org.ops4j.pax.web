/* 
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Activates the pax web extender.
 *
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public class Activator
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog( Activator.class );
    /**
     * The bundle context.
     */
    private BundleContext m_bundleContext;
    /**
     * The servlet tracker.
     */
    private ServletTracker m_servletTracker;
    /**
     * The resources tracker.
     */
    private ResourcesTracker m_resourcesTracker;
    /**
     * The http service tracker.
     */
    private ServiceTracker m_httpServiceTracker;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start( final BundleContext bundleContext )
        throws Exception
    {
        if( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        m_bundleContext = bundleContext;
        trackServlets();
        trackResources();
        trackHttpServices();
        LOGGER.info( "Pax Web Extender started" );
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        if( m_servletTracker != null )
        {
            m_servletTracker.close();
        }
        if( m_resourcesTracker != null )
        {
            m_resourcesTracker.close();
        }
        if( m_httpServiceTracker != null )
        {
            m_httpServiceTracker.close();
        }
        LOGGER.info( "Pax Web Extender stopped" );
    }

    /**
     * Tracks servlets.
     */
    private void trackServlets()
    {
        m_servletTracker = new ServletTracker( m_bundleContext );
        m_servletTracker.open();
    }

    /**
     * Tracks resources.
     */
    private void trackResources()
    {
        m_resourcesTracker = new ResourcesTracker( m_bundleContext );
        m_resourcesTracker.open();
    }

    /**
     * Tracks http service.
     */
    private void trackHttpServices()
    {
        m_httpServiceTracker = new HttpServiceTracker( m_bundleContext,
                                                       new HttpServiceListener[]{ m_servletTracker, m_resourcesTracker }
        );
        m_httpServiceTracker.open();
    }

}
