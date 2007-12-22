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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.FilterMapping;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.util.LazyList;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;

public class JettyServerImpl implements JettyServer
{

    private static final Log LOG = LogFactory.getLog( JettyServerImpl.class );

    private final Server m_server;

    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;
    private final Map<HttpContext, Context> m_contexts;

    public JettyServerImpl()
    {
        m_server = new ServerWrapper();
        m_contexts = new IdentityHashMap<HttpContext, Context>();
    }

    public void start()
    {
        if( LOG.isInfoEnabled() )
        {
            LOG.info( "starting " + this );
        }
        try
        {
            m_server.start();
        }
        catch( Exception e )
        {
            if( LOG.isErrorEnabled() )
            {
                LOG.error( e );
            }
        }
    }

    public void stop()
    {
        if( LOG.isInfoEnabled() )
        {
            LOG.info( "stopping " + this );
        }
        try
        {
            m_server.stop();
        }
        catch( Exception e )
        {
            if( LOG.isErrorEnabled() )
            {
                LOG.error( e );
            }
        }
    }

    /**
     * @see JettyServer#addConnector(org.mortbay.jetty.Connector)
     */
    public void addConnector( final Connector connector )
    {
        if( LOG.isInfoEnabled() )
        {
            LOG.info( "adding connector" + connector );
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
            new HttpServiceContext( m_server, m_contextAttributes, httpContext, registrations );
        if( m_sessionTimeout != null )
        {
            configureSessionTimeout( context, m_sessionTimeout );
        }
        if( LOG.isInfoEnabled() )
        {
            LOG.info( "added servlet context: " + context );
        }
        if( m_server.isStarted() )
        {
            try
            {
                LOG.debug( "(Re)starting servlet contexts..." );
                // start the server handler if not already started
                Handler serverHandler = m_server.getHandler();
                if( !serverHandler.isStarted() && !serverHandler.isStarting() )
                {
                    serverHandler.start();
                }
                // if the server handler is a handler collection, seems like jetty will not automatically
                // start inner handlers. So, force the start of the created context
                if( !context.isStarted() && !context.isStarting() )
                {
                    context.start();
                }
            }
            catch( Exception ignore )
            {
                LOG.error( "Could not start the servlet context for http context [" + httpContext + "]", ignore );
            }
        }
        return context;
    }

    /**
     * Configures the session time out by extracting the session handlers->sessionManager fro the context.
     *
     * @param context the context for which the session timeout should be configured
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
        if( LOG.isDebugEnabled() )
        {
            LOG.debug( "adding servlet: [" + alias + "] -> " + servlet );
        }
        ServletHolder holder = new ServletHolder( servlet );
        holder.setName( alias );
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
            m_contexts.put( httpContext, context );
        }
        return context;
    }

    public void removeServlet( final String name, HttpContext httpContext )
    {
        if( LOG.isDebugEnabled() )
        {
            LOG.debug( "removing servlet: [" + name + "]" );
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
                // if servlet is still started stop the servlet holder (=servlet.destroy()) as Jetty will not do that
                if( holder.isStarted() )
                {
                    try
                    {
                        holder.stop();
                    }
                    catch( Exception ignore )
                    {
                        LOG.warn( "Exception during unregistering of servlet [" + name + "]" );
                    }
                }
            }
        }
        if( !removed )
        {
            throw new IllegalStateException( name + " was not found" );
        }
    }

    public void addEventListener( final EventListenerModel eventListenerModel )
    {
        getOrCreateContext(
            eventListenerModel.getContextModel().getHttpContext(),
            eventListenerModel.getContextModel().getRegistrations()
        ).addEventListener( eventListenerModel.getEventListener() );
    }

    public void removeEventListener( final EventListenerModel eventListenerModel )
    {
        final Context context = getContext( eventListenerModel.getContextModel().getHttpContext() );
        final List<EventListener> listeners =
            new ArrayList<EventListener>( Arrays.asList( context.getEventListeners() ) );
        listeners.remove( eventListenerModel.getEventListener() );
        context.setEventListeners( listeners.toArray( new EventListener[listeners.size()] ) );
    }

    public void removeContext( HttpContext httpContext )
    {
        m_server.removeHandler( getContext( httpContext ) );
        m_contexts.remove( httpContext );
    }

    public void addFilter( final FilterModel filterModel )
    {
        LOG.debug( "Adding filter model [" + filterModel + "]" );
        final FilterMapping mapping = new FilterMapping();
        mapping.setFilterName( filterModel.getId() );
        if( filterModel.getUrlPatterns() != null && filterModel.getUrlPatterns().length > 0 )
        {
            mapping.setPathSpecs( filterModel.getUrlPatterns() );
        }
        if( filterModel.getServletNames() != null && filterModel.getServletNames().length > 0 )
        {
            mapping.setServletNames( filterModel.getServletNames() );
        }
        final ServletHandler servletHandler =
            getOrCreateContext( filterModel.getContextModel().getHttpContext(),
                                filterModel.getContextModel().getRegistrations()
            ).getServletHandler();
        if( servletHandler == null )
        {
            throw new IllegalStateException( "Internal error: Cannot find the servlet holder" );
        }
        final FilterHolder holder = new FilterHolder( filterModel.getFilter() );
        holder.setName( filterModel.getId() );
        servletHandler.addFilter( holder, mapping );
    }

    public void removeFilter( FilterModel filterModel )
    {
        LOG.debug( "Removing filter model [" + filterModel + "]" );
        final ServletHandler servletHandler =
            getContext( filterModel.getContextModel().getHttpContext() ).getServletHandler();
        // first remove filter mappings for the removed filter
        final FilterMapping[] filterMappings = servletHandler.getFilterMappings();
        FilterMapping[] newFilterMappings = null;
        for( FilterMapping filterMapping : filterMappings )
        {
            if( filterMapping.getFilterName().equals( filterModel.getId() ) )
            {
                if( newFilterMappings == null )
                {
                    newFilterMappings = filterMappings;
                }
                newFilterMappings = (FilterMapping[]) LazyList.removeFromArray( newFilterMappings, filterMapping );
            }
        }
        servletHandler.setFilterMappings( newFilterMappings );
        // then remove the filter
        final FilterHolder filterHolder = servletHandler.getFilter( filterModel.getId() );
        final FilterHolder[] filterHolders = servletHandler.getFilters();
        final FilterHolder[] newFilterHolders =
            (FilterHolder[]) LazyList.removeFromArray( filterHolders, filterHolder );
        servletHandler.setFilters( newFilterHolders );
        // if filter is still started stop the filter (=filter.destroy()) as Jetty will not do that
        if( filterHolder.isStarted() )
        {
            try
            {
                filterHolder.stop();
            }
            catch( Exception ignore )
            {
                LOG.warn( "Exception during unregistering of filter [" + filterHolder.getFilter() + "]" );
            }
        }
    }

    private class ServerWrapper
        extends Server
    {

        @Override
        public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
            throws IOException, ServletException
        {
            super.handle( target, request, response, dispatch );
            // if there was no match try to match "/"
            if( !response.isCommitted() && !"/".equals( target.trim() ) )
            {
                LOG.debug( "Path [" + target + "] not matched. Try [/]." );
                super.handle( "/", request, response, dispatch );
            }
        }

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
