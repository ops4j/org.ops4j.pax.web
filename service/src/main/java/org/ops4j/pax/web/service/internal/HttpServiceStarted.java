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
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.ops4j.pax.swissbox.lang.BundleClassLoader;
import org.ops4j.pax.web.service.internal.model.EventListenerModel;
import org.ops4j.pax.web.service.internal.model.FilterModel;
import org.ops4j.pax.web.service.internal.model.ResourceModel;
import org.ops4j.pax.web.service.internal.model.ServerModel;
import org.ops4j.pax.web.service.internal.model.ServiceModel;
import org.ops4j.pax.web.service.internal.model.ServletModel;
import org.ops4j.pax.web.service.internal.util.Assert;

class HttpServiceStarted
    implements StoppableHttpService
{

    private static final Log LOG = LogFactory.getLog( HttpServiceStarted.class );

    private final Bundle m_bundle;
    private final ClassLoader m_bundleClassLoader;
    private final ServerController m_serverController;

    private final ServiceModel m_serviceModel;
    private final ServerModel m_serverModel;

    HttpServiceStarted( final Bundle bundle,
                        final ServerController serverController,
                        final ServiceModel serviceModel )
    {
        LOG.info( "Creating http service for: " + bundle );

        Assert.notNull( "Bundle cannot be null", bundle );
        Assert.notNull( "Server Controller cannot be null", serverController );
        Assert.notNull( "Service Model cannot be null", serviceModel );

        m_bundle = bundle;
        m_bundleClassLoader = new BundleClassLoader( bundle );
        m_serverController = serverController;
        m_serviceModel = serviceModel;
        m_serverModel = new ServerModel();

        m_serverController.addListener( new ServerListener()
        {
            public void stateChanged( final ServerEvent event )
            {
                LOG.info( "Handling event: [" + event + "]" );

                if( event == ServerEvent.STARTED )
                {
                    for( ServletModel model : m_serverModel.getServletModels() )
                    {
                        m_serverController.addServlet( model );
                    }
                    for( EventListenerModel model : m_serverModel.getEventListenerModels() )
                    {
                        m_serverController.addEventListener( model );
                    }
                    for( FilterModel filterModel : m_serverModel.getFilterModels() )
                    {
                        m_serverController.addFilter( filterModel );
                    }
                }
            }
        }
        );
    }

    public synchronized void stop()
    {
        for( ServletModel model : m_serverModel.getServletModels() )
        {
            m_serviceModel.removeServletModel( model );
        }
        for( HttpContext httpContext : m_serverModel.getHttpContexts() )
        {
            m_serverController.removeContext( httpContext );
        }
    }

    public void registerServlet( final String alias,
                                 final Servlet servlet,
                                 final Dictionary initParams,
                                 final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        final ServletModel model =
            new ServletModel(
                getOrCreateContext( httpContext ),
                servlet,
                alias,
                initParams,
                createClassLoader()
            );
        m_serviceModel.addServletModel( model );
        m_serverModel.addServletModel( model );
        m_serverController.addServlet( model );
    }

    public void registerResources( final String alias,
                                   final String name,
                                   final HttpContext httpContext )
        throws NamespaceException
    {
        final HttpContext realHttpContext = getOrCreateContext( httpContext );
        final ResourceServlet servlet = new ResourceServlet( realHttpContext, alias, name );
        final ResourceModel model =
            new ResourceModel(
                realHttpContext,
                servlet,
                alias,
                name,
                createClassLoader()
            );
        try
        {
            m_serviceModel.addServletModel( model );
        }
        catch( ServletException ignore )
        {
            // this should never happen as the servlet is created each time so it cannot already be registered before
            LOG.warn( "Internal error, please report ", ignore );
        }
        m_serverModel.addServletModel( model );
        m_serverController.addServlet( model );
    }

    public void unregister( final String alias )
    {
        final ServletModel model = m_serverModel.getServletModelWithAlias( alias );
        Assert.notNull( "Alias [" + alias + "] was never registered", model );
        m_serviceModel.removeServletModel( model );
        m_serverModel.removeServletModel( model );
        m_serverController.removeServlet( model );
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public void registerEventListener( final EventListener listener, final HttpContext httpContext )
    {
        final EventListenerModel model =
            new EventListenerModel(
                getOrCreateContext( httpContext ),
                listener,
                createClassLoader()
            );
        m_serverModel.addEventListenerModel( model );
        m_serverController.addEventListener( model );
    }

    public void unregisterEventListener( final EventListener listener )
    {
        m_serverController.removeEventListener( m_serverModel.removeEventListener( listener ) );
    }

    public void registerFilter( final Filter filter,
                                final String[] urlPatterns,
                                final String[] aliases,
                                final Dictionary initParams,
                                final HttpContext httpContext )
    {
        // first convert servlet aliases to servlet id's
        String[] servletIds = null;
        if( aliases != null && aliases.length > 0 )
        {
            List<String> servletIdsList = new ArrayList<String>();
            for( String alias : aliases )
            {
                final ServletModel servletModel = m_serverModel.getServletModelWithAlias( alias );
                if( servletModel == null )
                {
                    throw new IllegalArgumentException( "Unknown alias [" + alias + "]" );
                }
                servletIdsList.add( servletModel.getId() );
            }
            servletIds = servletIdsList.toArray( new String[servletIdsList.size()] );
        }
        final FilterModel model =
            new FilterModel(
                getOrCreateContext( httpContext ),
                filter,
                urlPatterns,
                servletIds,
                initParams,
                createClassLoader()
            );
        m_serverModel.addFilterModel( model );
        m_serverController.addFilter( model );
    }

    public void unregisterFilter( final Filter filter )
    {
        m_serverController.removeFilter( m_serverModel.removeFilter( filter ) );
    }

    /**
     * @see org.ops4j.pax.web.service.ExtendedHttpService#setContextParam(Dictionary, HttpContext)
     */
    public void setContextParam( final Dictionary params, final HttpContext httpContext )
    {
        Assert.notNull( "Http context cannot be null", httpContext );
        // TODO implement context param setting
    }

    private HttpContext getOrCreateContext( final HttpContext httpContext )
    {
        HttpContext context = httpContext;
        if( context == null )
        {
            context = createDefaultHttpContext();
        }
        return context;
    }

    private ClassLoader createClassLoader()
    {
        // check first if we have a context class loader
        // this helps extenders to set the clas loader to the original bundle.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        LOG.debug( "Context Class Loader: [" + classLoader + "]" );
        // if not set then use a classloader that delegates to the bundle
        if( classLoader == null )
        {
            classLoader = m_bundleClassLoader;
        }
        return classLoader;
    }

}
