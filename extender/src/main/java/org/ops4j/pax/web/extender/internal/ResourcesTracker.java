/*
 * Copyright 2007 Alin Dreghiciu, Damian Golda.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.extender.Resources;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.http.HttpService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Map;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * Tracks resources published as services via a Service Tracker and register/unregister them with an http service.
 *
 * @author Alin Dreghiciu
 * @author Damian Golda
 * @since August 21, 2007
 */
public class ResourcesTracker
    extends ServiceTracker
    implements HttpServiceListener
{

    /**
     * Logger.
     */
    private static final Log LOGGER = LogFactory.getLog( ResourcesTracker.class );
    /**
     * The resources registration property for the alias to be used.
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
     * Resources -> (alias,bundle) map.
     */
    private Map<Resources, ResourcesData> m_resources;

    /**
     * Tracks Resources services.
     *
     * @param bundleContext a bundle context
     */
    public ResourcesTracker( final BundleContext bundleContext )
    {
        super( validateBundleContext( bundleContext ), Resources.class.getName(), null );
        lock = new ReentrantLock();
        m_resources = Collections.synchronizedMap( new IdentityHashMap<Resources, ResourcesData>() );
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
     * Registers the resources with http service.
     *
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public Object addingService( final ServiceReference serviceReference )
    {
        LOGGER.debug( "Resources available [" + serviceReference + "]" );
        Object alias = serviceReference.getProperty( ALIAS_PROPERTY );
        Resources resources = null;
        // only use the right registered scanners
        if ( alias != null && alias instanceof String && ( (String) alias ).trim().length() > 0 )
        {
            // get bundle registering resources for correct loading of resources 
            Bundle bundle = serviceReference.getBundle();
            resources = (Resources) super.addingService( serviceReference );
            if ( resources != null )
            {
                // if we have a http service then register the resources
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
                ResourcesData resourcesData = new ResourcesData((String)alias, bundle);
                if ( httpService != null )
                {
                    if ( registerResources( httpService, resourcesData, resources) )
                    {
                        m_resources.put( resources, resourcesData );
                    }
                }
                else
                {
                    m_resources.put( resources, resourcesData );
                    LOGGER.info(
                        "Keeping track of resources [" + resources + "] with alias [" + alias + "] for later registration"
                    );
                }
            }
        }
        return resources;
    }

    /**
     * Removes the resources from http service.
     *
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,Object)
     */
    @Override
    public void removedService( final ServiceReference serviceReference, final Object resources )
    {
        LOGGER.debug( "Resources removed [" + serviceReference + "]" );
        super.removedService( serviceReference, resources );
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
            ResourcesData resourcesData = m_resources.get( resources );
            if ( resourcesData != null )
            {
                m_resources.remove( resources );
                httpService.unregister( resourcesData.getAlias() );
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
        for ( Map.Entry<Resources, ResourcesData> entry : m_resources.entrySet() )
        {
            registerResources( httpService, entry.getValue(), entry.getKey() );
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
        for ( Map.Entry<Resources, ResourcesData> entry : m_resources.entrySet() )
        {
            service.unregister( entry.getValue().getAlias() );
        }
    }

    /**
     * Registers a resources with an http service
     *
     * @param alias   alias
     * @param resources resources to register
     *
     * @return true if successful
     */
    private boolean registerResources( final HttpService httpService, final ResourcesData resourcesData, final Resources resources )
    {
        try
        {
            // Must create own HttpContext to read resource from correct bundle
            DefaultHttpContext httpContext = new DefaultHttpContext(resourcesData.getBundle());
            httpService.registerResources( resourcesData.getAlias(), resources.getResources(), httpContext );
            LOGGER.info( "Registered resources [" + resources + "] with alias [" + resourcesData.getAlias() + "]" );
            return true;
        }
        catch ( Exception e )
        {
            LOGGER.error( "Could not register resources [" + resources + "]", e );
            LOGGER.info(
                "Keeping track of resources [" + resources + "] with alias [" + resourcesData.getAlias()
                + "] for later registration"
            );
        }
        return false;
    }
}
