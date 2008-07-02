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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.EventListener;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.WebContainer;

public class HttpServiceProxy
    implements StoppableHttpService
{

    private static final Log LOG = LogFactory.getLog( HttpServiceProxy.class );
    private StoppableHttpService m_delegate;

    public HttpServiceProxy( final StoppableHttpService delegate )
    {
        NullArgumentException.validateNotNull( delegate, "Delegate" );
        m_delegate = delegate;
    }

    public void registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        LOG.info( "Registering servlet: [" + alias + "] -> " + servlet );
        m_delegate.registerServlet( alias, servlet, initParams, httpContext );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        LOG.info( "Registering resource: [" + alias + "] -> " + name );
        m_delegate.registerResources( alias, name, httpContext );
    }

    public void unregister( final String alias )
    {
        LOG.info( "Unregistering [" + alias + "]" );
        m_delegate.unregister( alias );
    }

    public HttpContext createDefaultHttpContext()
    {
        LOG.info( "Creating adefault context" );
        return m_delegate.createDefaultHttpContext();
    }

    public synchronized void stop()
    {
        LOG.info( "Stopping http service: [" + this + "]" );
        final StoppableHttpService stopping = m_delegate;
        m_delegate = new HttpServiceStopped();
        stopping.stop();

    }

    /**
     * @see WebContainer#registerServlet(Servlet, String[], Dictionary, HttpContext)
     */
    public void registerServlet( final Servlet servlet,
                                 final String[] urlPatterns,
                                 final Dictionary initParams,
                                 final HttpContext httpContext )
        throws ServletException
    {
        LOG.info( "Registering servlet [" + servlet + "]" );
        m_delegate.registerServlet( servlet, urlPatterns, initParams, httpContext );
    }

    /**
     * @see WebContainer#unregisterServlet(Servlet)
     */
    public void unregisterServlet( final Servlet servlet )
    {
        LOG.info( "Unregistering servlet [" + servlet + "]" );
        m_delegate.unregisterServlet( servlet );
    }

    /**
     * @see WebContainer#registerEventListener(EventListener, HttpContext) )
     */
    public void registerEventListener( final EventListener listener,
                                       final HttpContext httpContext )
    {
        LOG.info( "Registering event listener [" + listener + "]" );
        m_delegate.registerEventListener( listener, httpContext );
    }

    /**
     * @see WebContainer#unregisterEventListener(EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        LOG.info( "Unregistering event listener [" + listener + "]" );
        m_delegate.unregisterEventListener( listener );
    }

    /**
     * @see WebContainer#registerFilter(Filter, String[], String[], Dictionary, HttpContext)
     */
    public void registerFilter( final Filter filter,
                                final String[] urlPatterns,
                                final String[] aliases,
                                final Dictionary initParams,
                                final HttpContext httpContext )
    {
        LOG.info( "Registering filter [" + filter + "]" );
        m_delegate.registerFilter( filter, urlPatterns, aliases, initParams, httpContext );
    }

    /**
     * @see WebContainer#unregisterFilter(Filter)
     */
    public void unregisterFilter( final Filter filter )
    {
        LOG.info( "Unregistering filter [" + filter + "]" );
        m_delegate.unregisterFilter( filter );
    }

    /**
     * @see WebContainer#setContextParam(Dictionary, HttpContext)
     */
    public void setContextParam( final Dictionary params,
                                 final HttpContext httpContext )
    {
        LOG.info( "Setting context paramters [" + params + "] for http context [" + httpContext + "]" );
        m_delegate.setContextParam( params, httpContext );
    }

    /**
     * {@inheritDoc}
     */
    public void setSessionTimeout( final Integer minutes,
                                   final HttpContext httpContext )
    {
        LOG.info( "Setting session timeout to " + minutes + " minutes for http context [" + httpContext + "]" );
        m_delegate.setSessionTimeout( minutes, httpContext );
    }

    /**
     * @see WebContainer#registerJsps(String[], HttpContext)
     */
    public void registerJsps( final String[] urlPatterns,
                              final HttpContext httpContext )
    {
        LOG.info( "Registering jsps" );
        m_delegate.registerJsps( urlPatterns, httpContext );
    }

    /**
     * @see WebContainer#unregisterJsps(HttpContext)
     */
    public void unregisterJsps( final HttpContext httpContext )
    {
        LOG.info( "Unregistering jsps" );
        m_delegate.unregisterJsps( httpContext );
    }

    /**
     * @see WebContainer#registerErrorPage(String, String, HttpContext)
     */
    public void registerErrorPage( final String error,
                                   final String location,
                                   final HttpContext httpContext )
    {
        LOG.info( "Registering error page [" + error + "]" );
        m_delegate.registerErrorPage( error, location, httpContext );
    }

    /**
     * @see WebContainer#unregisterErrorPage(String, HttpContext)
     */
    public void unregisterErrorPage( final String error,
                                     final HttpContext httpContext )
    {
        LOG.info( "Unregistering error page [" + error + "]" );
        m_delegate.unregisterErrorPage( error, httpContext );
    }

    /**
     * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
     */
    public void registerWelcomeFiles( final String[] welcomeFiles,
                                      final boolean redirect,
                                      final HttpContext httpContext )
    {
        LOG.info( "Registering welcome files [" + Arrays.toString( welcomeFiles ) + "]" );
        m_delegate.registerWelcomeFiles( welcomeFiles, redirect, httpContext );
    }

    /**
     * @see WebContainer#unregisterWelcomeFiles(HttpContext)
     */
    public void unregisterWelcomeFiles( final HttpContext httpContext )
    {
        LOG.info( "Unregistering welcome files" );
        m_delegate.unregisterWelcomeFiles( httpContext );
    }


}
