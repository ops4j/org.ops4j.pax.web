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
import java.util.List;
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
    private Registrations m_registrations;
    private ServerController m_serverController;
    private List<EventListener> m_eventListeners;

    public StartedHttpService(
        final Bundle bundle,
        final ServerController serverController,
        final Registrations registrations )
    {
        LOG.info( "Creating http service for: " + bundle );

        Assert.notNull( "bundle == null", bundle );
        Assert.notNull( "registrationRepository == null", registrations );
        Assert.notNull( "httpServiceServer == null", serverController );

        m_bundle = bundle;
        m_registrations = registrations;
        m_serverController = serverController;
        m_eventListeners = new ArrayList<EventListener>();

        m_serverController.addListener( new ServerListener()
        {
            public void stateChanged( final ServerEvent event )
            {
                LOG.info( "Handling event: [" + event + "]" );

                if( event == ServerEvent.STARTED )
                {
                    Registration[] registrations = m_registrations.get();
                    if( registrations != null )
                    {
                        for( Registration registration : registrations )
                        {
                            LOG.info( "Registering [" + registration + "]" );
                            registration.register( m_serverController );
                        }
                    }
                    for( EventListener listener : m_eventListeners )
                    {
                        LOG.info( "Registering [" + listener + "]" );
                        m_serverController.addEventListener( listener );
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
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        Registration registration = m_registrations.registerServlet( alias, servlet, initParams, context );
        registration.register( m_serverController );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        Registration registration = m_registrations.registerResources( alias, name, context );
        registration.register( m_serverController );
    }

    public void unregister( final String alias )
    {
        LOG.info( "Unregistering [" + alias + "] from repository " + m_registrations );

        Assert.notNull( "alias == null", alias );
        Assert.notEmpty( "alias is empty", alias );
        Registration registration = m_registrations.getByAlias( alias );
        Assert.notNull( "alias was never registered", registration );
        registration.unregister( m_serverController );
        m_registrations.unregister( registration );
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public synchronized void stop()
    {
        Registration[] targets = m_registrations.get();
        if( targets != null )
        {
            LOG.info( "Unregistering from repository " + m_registrations );
            for( Registration target : targets )
            {
                LOG.info( "Unregistering [" + target + "]" );
                m_registrations.unregister( target );
                target.unregister( m_serverController );
            }
            for( EventListener listener : m_eventListeners )
            {
                LOG.info( "Unregistering [" + listener + "]" );
                m_serverController.removeEventListener( listener );
            }
        }
    }

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#registerEventListener(java.util.EventListener)
     */
    public void registerEventListener( final EventListener listener )
    {
        m_eventListeners.add( listener );
        m_serverController.addEventListener( listener );
    }

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#unregisterEventListener(java.util.EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        m_eventListeners.remove( listener );
        m_serverController.removeEventListener( listener );
    }
}
