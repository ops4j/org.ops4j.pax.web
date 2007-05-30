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
package org.ops4j.pax.web.service.internal.ng;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Dictionary;
import java.util.Collection;

public class HttpServiceImpl
    implements HttpService, HttpServiceServerListener
{

    private static final Log m_logger = LogFactory.getLog( HttpService.class );
    
    private Bundle m_bundle;
    private RegistrationRepository m_registrationRepository;
    private HttpServiceServer m_httpServiceServer;

    public HttpServiceImpl(
        final Bundle bundle,
        final RegistrationRepository registrationRepository,
        final HttpServiceServer httpServiceServer)
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Creating http service for: " + bundle );
        }
        if ( bundle == null )
        {
            throw new IllegalArgumentException( "bundle == null" );
        }
        if ( registrationRepository == null )
        {
            throw new IllegalArgumentException( "registrationRepository == null" );
        }
        if ( httpServiceServer == null )
        {
            throw new IllegalArgumentException( "httpServiceServer == null" );
        }
        m_bundle = bundle;
        m_registrationRepository = registrationRepository;
        m_httpServiceServer = httpServiceServer;
        m_httpServiceServer.addListener( this );
    }

    public void registerServlet( final String alias, final Servlet servlet, final Dictionary initParams, final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering Servlet: [" + alias + "] -> " + servlet );
        }        
        HttpContext context = httpContext;
        if ( context == null )
        {
            context = createDefaultHttpContext();
        }
        Registration registration = m_registrationRepository.registerServlet( alias, servlet, initParams, context);
        registration.register( m_httpServiceServer );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registered Servlet: [" + alias + "] -> " + servlet );
        }
    }

    public void registerResources( final String alias, final String name, final HttpContext httpContext )
        throws NamespaceException
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering resource: [" + alias + "] -> " + name );
        }
        // TODO register resources
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registered resource: [" + alias + "] -> " + name );
        }
    }

    public void unregister( final String alias )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Unregistering: [" + alias + "]");
        }
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Unregistered: [" + alias + "]");
        }
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public void stateChanged( final HttpServiceServerEvent event )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Handling event: [" + event + "]");
        }
        if ( event == HttpServiceServerEvent.STARTED )
        {
            Collection<Registration> registrations = m_registrationRepository.get();
            if ( registrations != null )
            {
                for( Registration registration : registrations )
                {
                    registration.register( m_httpServiceServer );
                }
            }
        }

    }
}
