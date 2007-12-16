/* Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.web.service.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.ops4j.pax.web.service.ExtendedHttpService;
import org.ops4j.pax.web.service.internal.model.ContextModel;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;

public class StartedHttpService
    implements StoppableHttpService
{

    private static final Log LOG = LogFactory.getLog( StartedHttpService.class );

    private Bundle m_bundle;
    private ServerController m_serverController;
    private RegistrationsCluster m_registrationsCluster;
    private final Map<HttpContext, ContextModel> m_contextModels;
    private final Map<Filter, FilterModel> m_filterModels;
    private final Map<EventListener, EventListenerModel> m_eventListenerModels;

    public StartedHttpService(
        final Bundle bundle,
        final ServerController serverController,
        final RegistrationsCluster registrationsCluster )
    {
        LOG.info( "Creating http service for: " + bundle );

        Assert.notNull( "bundle == null", bundle );
        Assert.notNull( "httpServiceServer == null", serverController );
        Assert.notNull( "registrationsCluster == null", registrationsCluster );

        m_bundle = bundle;
        m_serverController = serverController;
        m_registrationsCluster = registrationsCluster;

        m_contextModels = new IdentityHashMap<HttpContext, ContextModel>();
        m_filterModels = new IdentityHashMap<Filter, FilterModel>();
        m_eventListenerModels = new IdentityHashMap<EventListener, EventListenerModel>();

        m_serverController.addListener( new ServerListener()
        {
            public void stateChanged( final ServerEvent event )
            {
                LOG.info( "Handling event: [" + event + "]" );

                if( event == ServerEvent.STARTED )
                {
                    for( ContextModel contextReg : m_contextModels.values() )
                    {
                        Registration[] registrations = contextReg.getRegistrations().get();
                        if( registrations != null )
                        {
                            for( Registration registration : registrations )
                            {
                                LOG.info( "Registering [" + registration + "]" );
                                registration.register( m_serverController );
                            }
                        }
                    }
                    for( EventListenerModel eventListenerModel : m_eventListenerModels.values() )
                    {
                        LOG.info( "Registering [" + eventListenerModel + "]" );
                        m_serverController.addEventListener( eventListenerModel );
                    }
                    for( FilterModel filterModel : m_filterModels.values() )
                    {
                        LOG.info( "Registering [" + filterModel + "]" );
                        m_serverController.addFilter( filterModel );
                    }
                }
            }
        }
        );
    }

    public void registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        final Registration registration =
            getOrCreateContext( httpContext ).getRegistrations().registerServlet( alias, servlet, initParams );
        registration.register( m_serverController );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        final Registration registration =
            getOrCreateContext( httpContext ).getRegistrations().registerResources( alias, name );
        registration.register( m_serverController );
    }

    public void unregister( final String alias )
    {
        LOG.info( "Unregistering [" + alias + "]" );

        Assert.notNull( "alias == null", alias );
        Assert.notEmpty( "alias is empty", alias );
        Registration registration = m_registrationsCluster.getByAlias( alias );
        Assert.notNull( "alias was never registered", registration );

        registration.unregister( m_serverController );

        final Registrations registrations =
            m_contextModels.get( registration.getHttpContext() ).getRegistrations();
        registrations.unregister( registration );
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public synchronized void stop()
    {
        synchronized( m_contextModels )
        {
            final Set<HttpContext> contextsSet = m_contextModels.keySet();
            final HttpContext[] contexts = contextsSet.toArray( new HttpContext[contextsSet.size()] );
            for( HttpContext context : contexts )
            {
                removeContext( context );
            }
        }
    }

    /**
     * @see ExtendedHttpService#registerEventListener(EventListener, HttpContext)
     */
    public void registerEventListener( final EventListener listener, final HttpContext httpContext )
    {
        final EventListenerModel eventListenerModel;
        synchronized( m_eventListenerModels )
        {
            if( m_eventListenerModels.containsKey( listener ) )
            {
                throw new IllegalArgumentException( "Listener [" + listener + "] already registered." );
            }
            eventListenerModel = new EventListenerModel( listener, getOrCreateContext( httpContext ) );
            m_eventListenerModels.put( listener, eventListenerModel );
        }
        m_serverController.addEventListener( eventListenerModel );
    }

    /**
     * @see ExtendedHttpService#unregisterEventListener(EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        final EventListenerModel eventListenerModel;
        synchronized( m_eventListenerModels )
        {
            eventListenerModel = m_eventListenerModels.get( listener );
            if( eventListenerModel == null )
            {
                throw new IllegalArgumentException(
                    "Listener [" + listener + " is not currently registered in any context"
                );
            }
            m_eventListenerModels.remove( listener );
        }
        m_serverController.removeEventListener( eventListenerModel );
    }

    /**
     * @see ExtendedHttpService#registerFilter(Filter, String[], String[], HttpContext)
     */
    public void registerFilter( final Filter filter, final String[] urlPatterns, final String[] aliases,
                                final HttpContext httpContext )
    {
        final FilterModel filterModel;
        synchronized( m_filterModels )
        {
            if( m_filterModels.containsKey( filter ) )
            {
                throw new IllegalArgumentException( "Filter [" + filter + "] is already registered." );
            }
            final ContextModel contextModel = getOrCreateContext( httpContext );
            String[] servletIds = null;
            if( aliases != null && aliases.length > 0 )
            {
                List<String> servletIdsList = new ArrayList<String>();
                for( String alias : aliases )
                {
                    final Registration registration = contextModel.getRegistrations().getByAlias( alias );
                    if( registration == null )
                    {
                        throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
                    }
                    servletIdsList.add( registration.getAlias() );
                }
                servletIds = servletIdsList.toArray( new String[servletIdsList.size()] );
            }
            filterModel = new FilterModel( filter, urlPatterns, servletIds, contextModel );
            m_filterModels.put( filter, filterModel );
        }
        m_serverController.addFilter( filterModel );
    }

    /**
     * @see ExtendedHttpService#unregisterFilter(Filter)
     */
    public void unregisterFilter( final Filter filter )
    {
        final FilterModel filterModel;
        synchronized( m_filterModels )
        {
            filterModel = m_filterModels.get( filter );
            if( filterModel == null )
            {
                throw new IllegalArgumentException(
                    "Filter [" + filter + " is not currently registered in any context"
                );
            }
            m_filterModels.remove( filter );
        }
        m_serverController.removeFilter( filterModel );
    }

    private ContextModel getOrCreateContext( final HttpContext httpContext )
    {
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        synchronized( m_contextModels )
        {
            if( !m_contextModels.containsKey( context ) )
            {
                m_contextModels.put(
                    context,
                    new ContextModel( context, m_registrationsCluster.createRegistrations( context ) )
                );
            }
        }
        return m_contextModels.get( context );
    }

    private void removeContext( final HttpContext httpContext )
    {
        synchronized( m_contextModels )
        {
            m_serverController.removeContext( httpContext );
            m_contextModels.get( httpContext ).getRegistrations().unregisterAll();
            m_contextModels.remove( httpContext );
        }
    }

}
