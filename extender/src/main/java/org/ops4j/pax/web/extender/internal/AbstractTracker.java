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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks objects published as services via a Service AbstractTracker and register/unregister them with an http service.
 *
 * @author Alin Dreghiciu
 * @since August 21, 2007
 */
public abstract class AbstractTracker<T, R extends Registration>
    extends ServiceTracker
    implements HttpServiceListener
{

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog( AbstractTracker.class );
    /**
     * The registration property for the alias to be used.
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
     * Tracked object -> registration map.
     */
    private Map<T, R> m_tracked;

    /**
     * Create a new tracker for the passed class.
     *
     * @param bundleContext a bundle context
     * @param clazz         class of the tracked objects
     */
    public AbstractTracker( final BundleContext bundleContext, final Class<T> clazz )
    {
        super( validateBundleContext( bundleContext ), clazz.getName(), null );
        lock = new ReentrantLock();
        m_tracked = Collections.synchronizedMap( new IdentityHashMap<T, R>() );
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
        if( bundleContext == null )
        {
            throw new IllegalArgumentException( "Bundle context cannot be null" );
        }
        return bundleContext;
    }

    /**
     * Registers the published object with the active http service if possible.
     * Otherwise just keep track of them for later registration
     *
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public Object addingService( final ServiceReference serviceReference )
    {
        LOGGER.debug( "Service available [" + serviceReference + "]" );
        Object alias = serviceReference.getProperty( ALIAS_PROPERTY );
        T registered = null;
        // only use the correct published objects
        if( alias != null && alias instanceof String && ( (String) alias ).trim().length() > 0 )
        {
            registered = (T) super.addingService( serviceReference );
            if( registered != null )
            {
                // if we have a http service then register the tracked object
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
                R registration = createRegistration( (String) alias, serviceReference, registered );
                if( httpService != null )
                {
                    if( localRegister( httpService, registered, registration ) )
                    {
                        m_tracked.put( registered, registration );
                    }
                }
                else
                {
                    m_tracked.put( registered, registration );
                    LOGGER.info(
                        "Keeping track of registered [" + registered + "] with alias [" + registration.getAlias()
                        + "] for later registration"
                    );
                }
            }
        }
        return registered;
    }

    /**
     * Removes the unpublished object from http service.
     *
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,Object)
     */
    @Override
    public void removedService( final ServiceReference serviceReference, final Object unpublished )
    {
        LOGGER.debug( "Service removed [" + serviceReference + "]" );
        super.removedService( serviceReference, unpublished );
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
        if( httpService != null )
        {
            R alias = m_tracked.get( unpublished );
            if( alias != null )
            {
                m_tracked.remove( unpublished );
                httpService.unregister( alias.getAlias() );
            }
        }
    }

    /**
     * If we do not have already a http service register all tracked objects.
     *
     * @see HttpServiceListener#available(org.osgi.service.http.HttpService)
     */
    public void available( final HttpService httpService )
    {
        if( httpService == null )
        {
            throw new IllegalArgumentException( "Http service cannot be null" );
        }
        HttpService service = null;
        lock.lock();
        try
        {
            if( m_httpService != null )
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
        for( Map.Entry<T, R> entry : m_tracked.entrySet() )
        {
            localRegister( httpService, entry.getKey(), entry.getValue() );
        }
    }

    /**
     * If and http service becomes unavailable and that service is the active http service, unregister all objects that
     * were registered before.
     *
     * @see HttpServiceListener#unavailable(org.osgi.service.http.HttpService)
     */
    public void unavailable( final HttpService httpService )
    {
        if( httpService == null )
        {
            throw new IllegalArgumentException( "Http service cannot be null" );
        }
        HttpService service = null;
        lock.lock();
        try
        {
            if( m_httpService == null || m_httpService != httpService )
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
        for( Map.Entry<T, R> entry : m_tracked.entrySet() )
        {
            service.unregister( entry.getValue().getAlias() );
        }
    }

    /**
     * Registers a published service with the active http service.
     * Delegates to the abstract register() that should perform the actual service registration but handles any
     * exception that can occur. If an exception occurs it will save the published service for a later registration if
     * possible.
     *
     * @param httpService  the http service in use
     * @param published    the published service
     * @param registration the registration corresponding to published service
     *
     * @return true if successful
     */
    private boolean localRegister( final HttpService httpService, final T published, final R registration )
    {
        try
        {
            boolean registered = register( httpService, published, registration );
            LOGGER.info( "Registered [" + published + "] with alias [" + registration.getAlias() + "]" );
            return registered;
        }
        catch( Exception e )
        {
            LOGGER.error( "Could not register [" + published + "]", e );
            LOGGER.info(
                "Keeping track of [" + published + "] with alias [" + registration.getAlias()
                + "] for later registration"
            );
        }
        return false;
    }

    /**
     * Registers a published service with the active http service.
     *
     * @param httpService  the http service in use
     * @param published    the published service
     * @param registration the registration corresponding to published service
     *
     * @return true if successful
     */
    abstract boolean register( final HttpService httpService, final T published, final R registration )
        throws Exception;

    /**
     * Factory method for alias object corresponding to the published service.
     *
     * @param alias            the alias found in the properties of published service
     * @param serviceReference service reference for published service
     * @param published        the actual published service
     *
     * @return an Registration
     */
    abstract R createRegistration( final String alias, final ServiceReference serviceReference, final T published );

}
