/* Copyright 2007 Alin Dreghiciu.
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
import java.util.Arrays;
import java.util.EventListener;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.util.LazyList;
import org.osgi.service.http.HttpContext;

public class JettyServerImpl implements JettyServer
{

    private static final Log m_logger = LogFactory.getLog( JettyServerImpl.class );

    private Server m_server;

    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;
    private Map<HttpContext, Context> m_contexts;

    public JettyServerImpl()
    {
        m_server = new Server();
        m_contexts = new IdentityHashMap<HttpContext, Context>();
    }

    public void start()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "starting " + this );
        }
        try
        {
            m_server.start();
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    public void stop()
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "stopping " + this );
        }
        try
        {
            m_server.stop();
        }
        catch( Exception e )
        {
            if( m_logger.isErrorEnabled() )
            {
                m_logger.error( e );
            }
        }
    }

    /**
     * @see JettyServer#addConnector(org.mortbay.jetty.Connector)
     */
    public void addConnector( final Connector connector )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "adding connector" + connector );
        }
        m_server.addConnector( connector );
    }

    /**
     * @see JettyServer#configureContext(java.util.Map, Integer)
     */
    public void configureContext( Map<String, Object> attributes, final Integer sessionTimeout )
    {
        m_contextAttributes = attributes;
        m_sessionTimeout = sessionTimeout;
    }

    private Context addContext( final HttpContext httpContext, final Registrations registrations )
    {
        Context context =
            new HttpServiceContext( m_server, "/", Context.SESSIONS, m_contextAttributes, httpContext, registrations );
        if( m_sessionTimeout != null )
        {
            configureSessionTimeout( context, m_sessionTimeout );
        }
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "added context: " + context );
        }
        if( m_server.isStarted() )
        {
            try
            {
                m_server.getHandler().start();
            }
            catch( Exception ignore )
            {
                m_logger.error( "Could not start the servlet context for http context [" + httpContext + "]", ignore );
            }
        }
        return context;
    }

    /**
     * Configures the session time out by extracting the session handlers->sessionManager fro the context.
     *
     * @param minutes timeout in minutes
     */
    private void configureSessionTimeout( Context context, Integer minutes )
    {
        final SessionHandler sessionHandler = context.getSessionHandler();
        if( sessionHandler != null )
        {
            final SessionManager sessionManager = sessionHandler.getSessionManager();
            if( sessionManager != null )
            {
                sessionManager.setMaxInactiveInterval( minutes * 60 );
            }
        }
    }

    public String addServlet( final String alias, final Servlet servlet, final Map<String, String> initParams,
                              final HttpContext httpContext, final Registrations registrations )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "adding servlet: [" + alias + "] -> " + servlet );
        }
        ServletHolder holder = new ServletHolder( servlet );
        if( initParams != null )
        {
            holder.setInitParameters( initParams );
        }
        getOrCreateContext( httpContext, registrations ).addServlet( holder, alias + "/*" );
        return holder.getName();
    }

    private Context getContext( final HttpContext httpContext )
    {
        return m_contexts.get( httpContext );
    }

    private Context getOrCreateContext( final HttpContext httpContext, final Registrations registrations )
    {
        Context context = m_contexts.get( httpContext );
        if( context == null )
        {
            context = addContext( httpContext, registrations );
        }
        return context;
    }

    public void removeServlet( final String name, HttpContext httpContext )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "removing servlet: [" + name + "]" );
        }
        // jetty does not provide a method fro removing a servlet so we have to do it by our own
        // the facts bellow are found by analyzing ServletHolder implementation
        boolean removed = false;
        ServletHandler servletHandler = getContext( httpContext ).getServletHandler();
        ServletHolder[] holders = servletHandler.getServlets();
        if( holders != null )
        {
            ServletHolder holder = servletHandler.getServlet( name );
            if( holder != null )
            {
                servletHandler.setServlets( (ServletHolder[]) LazyList.removeFromArray( holders, holder ) );
                // we have to find the servlet mapping by hand :( as there is no method provided by jetty
                // and the remove is done based on equals, that is not implemented by servletmapping
                // so it is == based.
                ServletMapping[] mappings = servletHandler.getServletMappings();
                if( mappings != null )
                {
                    ServletMapping mapping = null;
                    for( ServletMapping item : mappings )
                    {
                        if( holder.getName().equals( item.getServletName() ) )
                        {
                            mapping = item;
                            break;
                        }
                    }
                    if( mapping != null )
                    {
                        servletHandler.setServletMappings(
                            (ServletMapping[]) LazyList.removeFromArray( mappings, mapping )
                        );
                        removed = true;
                    }
                }
            }
        }
        if( !removed )
        {
            throw new IllegalStateException( name + " was not found" );
        }
    }

    public void addEventListener( EventListener listener, HttpContext httpContext, Registrations registrations )
    {
        getOrCreateContext( httpContext, registrations ).addEventListener( listener );
    }

    public void removeEventListener( EventListener listener, HttpContext httpContext )
    {
        Context context = getContext( httpContext );
        List<EventListener> listeners = new ArrayList<EventListener>( Arrays.asList( context.getEventListeners() ) );
        listeners.remove( listener );
        context.setEventListeners( listeners.toArray( new EventListener[listeners.size()] ) );
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( JettyServerImpl.class.getSimpleName() )
            .append( "{" )
            .append( "}" )
            .toString();
    }

}
