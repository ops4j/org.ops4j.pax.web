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

import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Dictionary;
import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * Tracks servlet published as services via a Service Tracker and register/unregister them with an http service.
 *
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public class ServletTracker
    extends ServiceTracker
    implements HttpServiceListener
{

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog( ServletTracker.class );
    /**
     * The servlet registration property for the alias to be used.
     */
    private static final String ALIAS_PROPERTY = "alias";
    /**
     * The Htttp Service in use.
     */
    private HttpService m_httpService;
    /**
     * HttpService lock.
     */
    Lock lock;
    /**
     * Servlet -> alias map.
     */
    private Map<Servlet, String> m_servlets;

    /**
     * Tracks Servlet services.
     *
     * @param bundleContext a bundle context
     */
    public ServletTracker( final BundleContext bundleContext )
    {
        super( validateBundleContext( bundleContext ), Servlet.class.getName(), null );
        lock = new ReentrantLock();
        m_servlets = Collections.synchronizedMap( new IdentityHashMap<Servlet, String>() );
    }

    /**
     * Validates that the bundle context is not null.
     * If null will throw IllegalArgumentException
     *
     * @param bundleContext a bundle context
     *
     * @return the bundle context if not null
     */
    private static BundleContext validateBundleContext( BundleContext bundleContext )
    {
        if ( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        return bundleContext;
    }

    /**
     * Registers the servet with http service.
     *
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
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
                // if we have a http service then register the servlet
                // otherwise just keep track of them and register later when http service becomes available
                HttpService httpService = null;
                lock.lock();
                try
                {
                    httpService = m_httpService;
                }
                finally
                {
                    lock.unlock();
                }
                if ( httpService != null )
                {
                    if ( registerServlet( httpService, (String) alias, servlet ) )
                    {
                        m_servlets.put( servlet, (String) alias );
                    }
                }
                else
                {
                    m_servlets.put( servlet, (String) alias );
                    LOGGER.info(
                        "Keeping track of servlet [" + servlet + "] with alias [" + alias + "] for later registration"
                    );
                }
            }
        }
        return servlet;
    }

    /**
     * Removes the servlet from http service.
     *
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,Object)
     */
    @Override
    public void removedService( final ServiceReference serviceReference, final Object servlet )
    {
        LOGGER.debug( "Servlet removed [" + serviceReference + "]" );
        super.removedService( serviceReference, servlet );
        HttpService httpService = null;
        lock.lock();
        try
        {
            httpService = m_httpService;
        }
        finally
        {
            lock.unlock();
        }
        if ( httpService != null )
        {
            String alias = m_servlets.get( servlet );
            if ( alias != null )
            {
                m_servlets.remove( servlet );
                httpService.unregister( alias );
            }
        }
    }

    /**
     * If we do not have already a http service register all servltes.
     *
     * @see org.ops4j.pax.web.extender.internal.HttpServiceListener#available(org.osgi.service.http.HttpService)
     */
    public void available( final HttpService httpService )
    {
        if ( httpService == null )
        {
            throw new IllegalArgumentException( "Http service cannot be null" );
        }
        HttpService service = null;
        lock.lock();
        try
        {
            if ( m_httpService != null )
            {
                return;
            }
            m_httpService = httpService;
            service = m_httpService;
        }
        finally
        {
            lock.unlock();
        }
        for ( Map.Entry<Servlet, String> entry : m_servlets.entrySet() )
        {
            registerServlet( httpService, entry.getValue(), entry.getKey() );
        }
    }

    /**
     * @see org.ops4j.pax.web.extender.internal.HttpServiceListener#unavailable(org.osgi.service.http.HttpService)
     */
    public void unavailable( final HttpService httpService )
    {
        if ( httpService == null )
        {
            throw new IllegalArgumentException( "Http service cannot be null" );
        }
        HttpService service = null;
        lock.lock();
        try
        {
            if ( m_httpService == null || m_httpService != httpService )
            {
                return;
            }
            service = m_httpService;
            m_httpService = null;
        }
        finally
        {
            lock.unlock();
        }
        for ( Map.Entry<Servlet, String> entry : m_servlets.entrySet() )
        {
            service.unregister( entry.getValue() );
        }
    }

    /**
     * Registers a servlet with an http service
     *
     * @param alias   alias
     * @param servlet setrvlet to register
     *
     * @return true if successful
     */
    private boolean registerServlet( final HttpService httpService, final String alias, final Servlet servlet )
    {
        try
        {
            httpService.registerServlet( alias, servlet, (Dictionary) null, (HttpContext) null );
            LOGGER.info( "Registered servlet [" + servlet + "] with alias [" + alias + "]" );
            return true;
        }
        catch ( Exception e )
        {
            LOGGER.error( "Could not register servlet [" + servlet + "]", e );
            LOGGER.info(
                "Keeping track of servlet [" + servlet + "] with alias [" + alias
                + "] for later registration"
            );
        }
        return false;
    }
}
