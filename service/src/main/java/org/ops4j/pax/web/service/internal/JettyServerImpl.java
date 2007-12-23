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
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.FilterMapping;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletMapping;
import org.mortbay.util.LazyList;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;
import org.ops4j.pax.web.service.internal.model.ServiceModel;
import org.ops4j.pax.web.service.internal.model.ServletModel;

class JettyServerImpl implements JettyServer
{

    private static final Log LOG = LogFactory.getLog( JettyServerImpl.class );

    private final JettyServerWrapper m_server;

    JettyServerImpl( final ServiceModel serviceModel )
    {
        m_server = new JettyServerWrapper( serviceModel );
    }

    public void start()
    {
        LOG.info( "starting " + this );
        try
        {
            m_server.start();
        }
        catch( Exception e )
        {
            LOG.error( e );
        }
    }

    public void stop()
    {
        LOG.info( "stopping " + this );
        try
        {
            m_server.stop();
        }
        catch( Exception e )
        {
            LOG.error( e );
        }
    }

    /**
     * @see JettyServer#addConnector(org.mortbay.jetty.Connector)
     */
    public void addConnector( final Connector connector )
    {
        LOG.info( "adding connector" + connector );
        m_server.addConnector( connector );
    }

    /**
     * @see JettyServer#configureContext(java.util.Map, Integer)
     */
    public void configureContext( final Map<String, Object> attributes, final Integer sessionTimeout )
    {
        m_server.configureContext( attributes, sessionTimeout );
    }

    public void addServlet( final ServletModel model )
    {
        LOG.debug( "Adding servlet [" + model + "]" );
        final ServletHolder holder = new ServletHolder( model.getServlet() );
        holder.setName( model.getId() );
        if( model.getInitParams() != null )
        {
            holder.setInitParameters( model.getInitParams() );
        }
        m_server.getOrCreateContext( model.getHttpContext() )
            .addServlet( holder, model.getAlias() + ( "/".equals( model.getAlias() ) ? "*" : "/*" ) );
    }

    public void removeServlet( final ServletModel model )
    {
        LOG.debug( "Removing servlet [" + model + "]" );
        // jetty does not provide a method fro removing a servlet so we have to do it by our own
        // the facts bellow are found by analyzing ServletHolder implementation
        boolean removed = false;
        ServletHandler servletHandler = m_server.getContext( model.getHttpContext() ).getServletHandler();
        ServletHolder[] holders = servletHandler.getServlets();
        if( holders != null )
        {
            ServletHolder holder = servletHandler.getServlet( model.getId() );
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
                        LOG.warn( "Exception during unregistering of servlet [" + model + "]" );
                    }
                }
            }
        }
        if( !removed )
        {
            throw new IllegalStateException( model + " was not found" );
        }
    }

    public void addEventListener( final EventListenerModel model )
    {
        m_server.getOrCreateContext( model.getHttpContext() ).addEventListener( model.getEventListener() );
    }

    public void removeEventListener( final EventListenerModel model )
    {
        final Context context = m_server.getContext( model.getHttpContext() );
        final List<EventListener> listeners =
            new ArrayList<EventListener>( Arrays.asList( context.getEventListeners() ) );
        listeners.remove( model.getEventListener() );
        context.setEventListeners( listeners.toArray( new EventListener[listeners.size()] ) );
    }

    public void removeContext( final HttpContext httpContext )
    {
        m_server.removeContext( httpContext );
    }

    public void addFilter( final FilterModel model )
    {
        LOG.debug( "Adding filter model [" + model + "]" );
        final FilterMapping mapping = new FilterMapping();
        mapping.setFilterName( model.getId() );
        if( model.getUrlPatterns() != null && model.getUrlPatterns().length > 0 )
        {
            mapping.setPathSpecs( model.getUrlPatterns() );
        }
        if( model.getServletNames() != null && model.getServletNames().length > 0 )
        {
            mapping.setServletNames( model.getServletNames() );
        }
        final ServletHandler servletHandler = m_server.getOrCreateContext( model.getHttpContext() ).getServletHandler();
        if( servletHandler == null )
        {
            throw new IllegalStateException( "Internal error: Cannot find the servlet holder" );
        }
        final FilterHolder holder = new FilterHolder( model.getFilter() );
        holder.setName( model.getId() );
        servletHandler.addFilter( holder, mapping );
    }

    public void removeFilter( FilterModel model )
    {
        LOG.debug( "Removing filter model [" + model + "]" );
        final ServletHandler servletHandler = m_server.getContext( model.getHttpContext() ).getServletHandler();
        // first remove filter mappings for the removed filter
        final FilterMapping[] filterMappings = servletHandler.getFilterMappings();
        FilterMapping[] newFilterMappings = null;
        for( FilterMapping filterMapping : filterMappings )
        {
            if( filterMapping.getFilterName().equals( model.getId() ) )
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
        final FilterHolder filterHolder = servletHandler.getFilter( model.getId() );
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
