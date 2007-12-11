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

import java.util.Dictionary;
import java.util.EventListener;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class StartedHttpService
    implements StoppableHttpService
{

    private static final Log LOG = LogFactory.getLog( StartedHttpService.class );

    private Bundle m_bundle;
    private Map<HttpContext, Registrations> m_contexts;
    private ServerController m_serverController;
    private Map<EventListener, HttpContext> m_eventListeners;
    private RegistrationsCluster m_registrationsCluster;

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
        m_contexts = new IdentityHashMap<HttpContext, Registrations>();
        m_serverController = serverController;
        m_registrationsCluster = registrationsCluster;
        m_eventListeners = new IdentityHashMap<EventListener, HttpContext>();

        m_serverController.addListener( new ServerListener()
        {
            public void stateChanged( final ServerEvent event )
            {
                LOG.info( "Handling event: [" + event + "]" );

                if( event == ServerEvent.STARTED )
                {
                    for( Registrations regs : m_contexts.values() )
                    {
                        Registration[] registrations = regs.get();
                        if( registrations != null )
                        {
                            for( Registration registration : registrations )
                            {
                                LOG.info( "Registering [" + registration + "]" );
                                registration.register( m_serverController );
                            }
                        }
                    }
                    for( Map.Entry<EventListener, HttpContext> entry : m_eventListeners.entrySet() )
                    {
                        LOG.info( "Registering [" + entry.getKey() + "]" );
                        m_serverController.addEventListener( entry.getKey(), entry.getValue(),
                                                             findRegistrations( entry.getValue() )
                        );
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
            findRegistrations( httpContext ).registerServlet( alias, servlet, initParams );
        registration.register( m_serverController );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        Registration registration = findRegistrations( httpContext ).registerResources( alias, name );
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
        m_contexts.get( registration.getHttpContext() ).unregister( registration );
        Registration[] registrations = m_contexts.get( registration.getHttpContext() ).get();
        if( registrations == null || registrations.length == 0 )
        {
            removeContext( registration.getHttpContext() );
        }
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public synchronized void stop()
    {
        for( HttpContext context : m_contexts.keySet() )
        {
            removeContext( context );
        }
    }

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#registerEventListener(java.util.EventListener, HttpContext)
     */
    public void registerEventListener( final EventListener listener, HttpContext httpContext )
    {
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        if( m_eventListeners.containsKey( listener ) )
        {
            throw new IllegalArgumentException( "Listener [" + listener + "] already registered." );
        }
        m_eventListeners.put( listener, httpContext );
        m_serverController.addEventListener( listener, httpContext, findRegistrations( httpContext ) );
    }

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#unregisterEventListener(java.util.EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        HttpContext httpContext = m_eventListeners.get( listener );
        if( httpContext == null )
        {
            throw new IllegalArgumentException( "Listener [" + listener + " was never registered" );
        }
        m_eventListeners.remove( listener );
        m_serverController.removeEventListener( listener, httpContext );
    }

    private Registrations findRegistrations( final HttpContext httpContext )
    {
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        Registrations registrations;
        if( !m_contexts.containsKey( httpContext ) )
        {
            registrations = m_registrationsCluster.create( context );
            m_contexts.put( context, registrations );
        }
        else
        {
            registrations = m_contexts.get( httpContext );
        }
        return registrations;
    }

    private void removeContext( final HttpContext httpContext )
    {
        //TODO remove context from server
        //m_serverController.removeContext( httpContext );
        m_contexts.remove( httpContext );
    }

}
