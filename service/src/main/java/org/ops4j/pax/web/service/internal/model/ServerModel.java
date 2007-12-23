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
package org.ops4j.pax.web.service.internal.model;

import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.Assert;

public class ServerModel
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ServiceModel.class );

    private final Map<String, ServletModel> m_aliasMapping;
    private final Set<Servlet> m_servlets;
    private final Map<Filter, FilterModel> m_filterModels;
    private final Map<EventListener, EventListenerModel> m_eventListenerModels;
    private final Set<HttpContext> m_httpContexts;

    public ServerModel()
    {
        m_aliasMapping = new HashMap<String, ServletModel>();
        m_servlets = new HashSet<Servlet>();
        m_filterModels = new HashMap<Filter, FilterModel>();
        m_eventListenerModels = new HashMap<EventListener, EventListenerModel>();
        m_httpContexts = new HashSet<HttpContext>();
    }

    public synchronized ServletModel getServletModelWithAlias( final String alias )
    {
        Assert.notNull( "Alias cannot be null", alias );
        Assert.notEmpty( "Alias cannot be empty", alias );
        return m_aliasMapping.get( alias );
    }

    public synchronized boolean containsServletModelWithAlias( final String alias )
    {
        return m_aliasMapping.containsKey( alias );
    }

    public synchronized boolean containsServlet( final Servlet servlet )
    {
        return m_servlets.contains( servlet );
    }

    public synchronized void addServletModel( final ServletModel model )
    {
        m_aliasMapping.put( model.getAlias(), model );
        m_servlets.add( model.getServlet() );
        m_httpContexts.add( model.getHttpContext() );
    }

    public synchronized void removeServletModel( final ServletModel model )
    {
        m_aliasMapping.remove( model.getAlias() );
        m_servlets.remove( model.getServlet() );
    }

    public void addEventListenerModel( final EventListenerModel model )
    {
        synchronized( m_eventListenerModels )
        {
            if( m_eventListenerModels.containsKey( model.getEventListener() ) )
            {
                throw new IllegalArgumentException( "Listener [" + model.getEventListener() + "] already registered." );
            }
            m_eventListenerModels.put( model.getEventListener(), model );
            m_httpContexts.add( model.getHttpContext() );
        }
    }

    public EventListenerModel removeEventListener( final EventListener listener )
    {
        final EventListenerModel model;
        synchronized( m_eventListenerModels )
        {
            model = m_eventListenerModels.get( listener );
            if( model == null )
            {
                throw new IllegalArgumentException(
                    "Listener [" + listener + " is not currently registered in any context"
                );
            }
            m_eventListenerModels.remove( listener );
            return model;
        }
    }

    public void addFilterModel( final FilterModel model )
    {
        synchronized( m_filterModels )
        {
            if( m_filterModels.containsKey( model.getFilter() ) )
            {
                throw new IllegalArgumentException( "Filter [" + model.getFilter() + "] is already registered." );
            }
            m_filterModels.put( model.getFilter(), model );
            m_httpContexts.add( model.getHttpContext() );
        }
    }

    public FilterModel removeFilter( final Filter filter )
    {
        final FilterModel model;
        synchronized( m_filterModels )
        {
            model = m_filterModels.get( filter );
            if( model == null )
            {
                throw new IllegalArgumentException(
                    "Filter [" + filter + " is not currently registered in any context"
                );
            }
            m_filterModels.remove( filter );
            return model;
        }
    }

    public ServletModel[] getServletModels()
    {
        final Collection<ServletModel> models = m_aliasMapping.values();
        return models.toArray( new ServletModel[models.size()] );
    }

    public EventListenerModel[] getEventListenerModels()
    {
        final Collection<EventListenerModel> models = m_eventListenerModels.values();
        return models.toArray( new EventListenerModel[models.size()] );
    }

    public FilterModel[] getFilterModels()
    {
        final Collection<FilterModel> models = m_filterModels.values();
        return models.toArray( new FilterModel[models.size()] );
    }

    public HttpContext[] getHtpContexts()
    {
        return m_httpContexts.toArray( new HttpContext[m_httpContexts.size()] );
    }
}