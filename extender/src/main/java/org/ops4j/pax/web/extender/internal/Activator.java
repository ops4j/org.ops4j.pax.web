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
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import javax.servlet.Servlet;

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
     * The Scanner service service tracker.
     */
    private ServiceTracker m_serviceTracker;
    /**
     * The servlet registration property for the alias to be used.
     */
    private static final String ALIAS_PROPERTY = "alias";

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start( final BundleContext bundleContext )
        throws Exception
    {
        if ( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        m_bundleContext = bundleContext;
        trackScanners();
        LOGGER.info( "Pax Web Extender started" );
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if ( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        if ( m_serviceTracker != null )
        {
            m_serviceTracker.close();
        }
        LOGGER.info( "Pax Web Extender stopped" );
    }

    /**
     * Tracks servlets published as a service via a Service tracker.
     */
    private void trackScanners()
    {
        m_serviceTracker = new ServiceTracker( m_bundleContext, Servlet.class.getName(), null )
        {
            /**
             * Registers the servet with http service.
             *
             * @see ServiceTracker#addingService(org.osgi.framework.ServiceReference)
             */
            @Override
            public Object addingService( final ServiceReference serviceReference )
            {
                LOGGER.debug( "Servlet available [" + serviceReference + "]" );
                Object alias = serviceReference.getProperty( ALIAS_PROPERTY );
                Servlet servlet = null;
                // only use the right registered scanners
                if ( alias != null && alias instanceof String && ( (String) alias ).trim().length() > 0 )
                {
                    servlet = (Servlet) super.addingService( serviceReference );
                    if ( servlet != null )
                    {

                    }
                }
                return servlet;
            }

            /**
             * Removes the scanner from the provision service.
             *
             * @see ServiceTracker#removedService(org.osgi.framework.ServiceReference,Object)
             */
            @Override
            public void removedService( ServiceReference serviceReference, Object object )
            {
                LOGGER.debug( "Servlte removed [" + serviceReference + "]" );
                super.removedService( serviceReference, object );
                if ( !( object instanceof Servlet ) )
                {
                    throw new IllegalArgumentException(
                        "Invalid tracked object [" + object.getClass() + "]. Expected an " + Servlet.class.getName()
                    );
                }

            }
        };
        m_serviceTracker.open();
    }


}
