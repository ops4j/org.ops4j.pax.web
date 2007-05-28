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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.resource.Resource;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.*;

public class HttpServiceImpl
    implements HttpService, ManagedService
{
    private static final Log m_logger = LogFactory.getLog( HttpService.class );

    private Server m_server;
    private Bundle m_bundle;
    private HashMap<String, ServletHolder> m_servlets;
    private HashMap<String, Handler> m_resources;
    private DefaultHandler m_defHandler;
    private List<Context> m_contexts;
    private HandlerCollection m_contextHandlers;
    private OsgiContext m_rootContext;

    public HttpServiceImpl( Bundle bundle )
        throws ServletException
    {
        if( bundle == null )
        {
            throw new IllegalArgumentException();
        }
        m_bundle = bundle;
        m_contexts = new ArrayList<Context>();
        int port = Integer.getInteger( "org.osgi.service.http.port", 8080 );
        Object obj = new SocketConnector();
        Connector httpPort = (Connector) obj;
        httpPort.setPort( port );

        int sslport = Integer.getInteger( "org.osgi.service.http.port.secure", 8443 );
        Connector httpsPort = new SocketConnector();
        httpsPort.setPort( sslport );

        m_server = new Server();
        m_server.addConnector( httpPort );
        m_server.addConnector( httpsPort );
        m_defHandler = new DefaultHandler();
        HandlerCollection collection = new HandlerCollection();
        m_server.addHandler( collection );
        m_contextHandlers = new HandlerCollection();
        m_server.addHandler( m_contextHandlers );
        SecurityHandler securityHandler = new OsgiSecurityHandler( "/");
        SessionHandler sessionHandler = new OsgiSessionHandler( "/" );
        ErrorHandler errorHandler = new OsgiErrorHandler( "/" );
        ServletHandler servletHandler = new OsgiServletHandler( "/" );
        m_rootContext = new OsgiContext( m_contextHandlers, sessionHandler, securityHandler, servletHandler, errorHandler );
        m_rootContext.setContextPath( "/" );
        m_rootContext.addEventListener( new ContextListener( "/" ) );
        m_contexts.add( m_rootContext );
        try
        {
            m_rootContext.start();
        }
        catch( Exception e )
        {
            throw new ServletException( "Unable to start Root Context.", e );
        }
        m_server.addHandler( m_defHandler );
        m_servlets = new HashMap<String, ServletHolder>();
        m_resources = new HashMap<String, Handler>();
    }

    void start()
        throws Exception
    {
        for( Context context : m_contexts )
        {
            context.start();
        }
        m_defHandler.start();
        m_server.start();
    }

    void destroy()
        throws Exception
    {
        m_server.stop();
        for( Context context : m_contexts )
        {
            context.stop();
        }
        m_defHandler.stop();
        m_defHandler.destroy();
        for( Context context : m_contexts )
        {
            context.destroy();
        }

        m_server.destroy();
    }

    public void registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        validateRegisterServletArguments( alias, servlet, m_servlets.keySet() );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering Servlet: [" + alias + "] -> " + servlet );
        }
        if( httpContext == null )
        {
            httpContext = createDefaultHttpContext();
        }
        Map<String, String> init = new HashMap<String, String>();
        ServletHolder holder = new ServletHolder( servlet );
        m_rootContext.addContextMapping( holder, httpContext );
        if( initParams != null )
        {
            Enumeration enumeration = initParams.keys();
            while( enumeration.hasMoreElements() )
            {
                String key = (String) enumeration.nextElement();
                String value = (String) initParams.get( key );
                init.put( key, value );
            }
        }
        holder.setInitParameters( init );
        ServletHandler servletHandler = m_rootContext.getServletHandler();
        servletHandler.addServletWithMapping( holder, alias +"/*" );
        m_servlets.put( alias, holder );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registered Servlet: [" + alias + "] --> " + holder );
        }
    }


    public void registerResources( String alias, String name, HttpContext httpContext )
        throws NamespaceException
    {
        validateRegisterResourcesArguments( alias, name, m_resources.keySet() );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registering Resources: [" + alias + "] -> " + name );
        }
        ResourceHandler handler = new ResourceHandler();
        Resource resource = new OsgiResource( alias, name, httpContext );
        handler.setBaseResource( resource );
        m_contextHandlers.addHandler( handler );
        m_resources.put( alias, handler );
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "Registered Resources: [" + alias +"] --> " + handler );
        }
    }

    public void unregister( String alias )
    {
        Handler handler = m_resources.get( alias );
        if( handler != null )
        {
            m_contextHandlers.removeHandler( handler );
            return;
        }
        ServletHolder holder = m_servlets.get( alias );
        try
        {
            holder.stop();
        }
        catch( Exception e )
        {
            m_logger.error( "Incorrect termination of servlet [" + alias +"] --> " + holder, e );
        }
        ServletHandler servletHandler = m_rootContext.getServletHandler();
        ServletHolder[] oldHolders = servletHandler.getServlets();
        List<ServletHolder> holders = Arrays.asList( oldHolders );
        holders.remove( holder );
        m_rootContext.removeContextMapping( holder );
        ServletHolder[] newHolders = holders.toArray( new ServletHolder[0] );
        servletHandler.setServlets( newHolders );
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public void updated( Dictionary dictionary )
        throws ConfigurationException
    {

    }

    private void validateRegisterServletArguments(String alias, Servlet servlet, Set<String> registeredAliases)
            throws NamespaceException {
        validateAlias( alias, registeredAliases );
        if( servlet == null )
        {
            throw new IllegalArgumentException( "servlet == null" );
        }
    }

    private void validateRegisterResourcesArguments(String alias, String name, Set<String> registeredAliases)
            throws NamespaceException {
        validateAlias( alias, registeredAliases );
        if ( name == null ) {
            throw new IllegalArgumentException( "name == null" );
        }
        if( name.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "name ends with slash (/)" );
        }
    }

    private void validateAlias(String alias, Set<String> registeredAliases)
            throws NamespaceException {
        if( alias == null )
        {
            throw new IllegalArgumentException( "alias == null" );
        }
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ))
        {
            throw new IllegalArgumentException( "alias ends with slash (/)" );
        }
        // check for duplicate registration
        if ( registeredAliases.contains( alias ) )
        {
            throw new NamespaceException( "alias is already in use" );
        }
    }
    
}
