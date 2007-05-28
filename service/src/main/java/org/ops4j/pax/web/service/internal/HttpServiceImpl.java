/*  Copyright 2007 Niclas Hedhman.
 *  Copyright 2007 Alin Dreghiciu.
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
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImpl
    implements HttpService, ManagedService
{
    private static final Log m_logger = LogFactory.getLog( HttpService.class );

    private Bundle m_bundle;
    private OsgiHandler m_mainHandler;

    public HttpServiceImpl( Bundle bundle, OsgiHandler mainHandler )
    {
        m_mainHandler = mainHandler;
        if( bundle == null )
        {
            throw new IllegalArgumentException();
        }
        m_bundle = bundle;
    }

    public void registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering Servlet: [" + alias + "] -> " + servlet );
        }
        try
        {
            m_mainHandler.registerServlet( alias, servlet, initParams, httpContext );
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "Registered Servlet: [" + alias + "]"  );
            }
        }
        catch( NamespaceException e )
        {
            m_logger.error( "Failed registering Servlet: [" + alias + "] --> " + servlet, e );
            throw e;
        }
        catch( ServletException e )
        {
            m_logger.error( "Failed registering Servlet: [" + alias + "] --> " + servlet, e );
            throw e;
        }
    }

    public void registerResources( String alias, String name, HttpContext httpContext )
        throws NamespaceException
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering Resources: [" + alias + "] -> " + name );
        }
        m_mainHandler.registerResource( alias, name, httpContext );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registered Resources: [" + alias + "]" );
        }
    }

    public void unregister( String alias )
    {
        try
        {
            m_mainHandler.unregister( alias );
        }
        catch( Exception e )
        {
            m_logger.error( "Unable to unregister [" + alias + "].", e );
        }
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public void updated( Dictionary dictionary )
        throws ConfigurationException
    {

    }

}
