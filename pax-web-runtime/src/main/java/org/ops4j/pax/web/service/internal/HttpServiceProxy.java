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

import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EventListener;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.SharedWebContainerContext;
import org.ops4j.pax.web.service.WebContainer;

public class HttpServiceProxy
    implements StoppableHttpService
{

    private static final Logger LOG = LoggerFactory.getLogger( HttpServiceProxy.class );
    private StoppableHttpService m_delegate;

    public HttpServiceProxy( final StoppableHttpService delegate )
    {
        NullArgumentException.validateNotNull( delegate, "Delegate" );
        LOG.debug("HttpServiceProxy created for HttpService {}", delegate);
        m_delegate = delegate;
    }

    public void registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        LOG.debug( "Registering servlet: [" + alias + "] -> " + servlet );
        m_delegate.registerServlet( alias, servlet, initParams, httpContext );
    }

    public void registerResources(
        final String alias,
        final String name,
        final HttpContext httpContext )
        throws NamespaceException
    {
        LOG.debug( "Registering resource: [" + alias + "] -> " + name );
        m_delegate.registerResources( alias, name, httpContext );
    }

    public void unregister( final String alias )
    {
        LOG.debug( "Unregistering [" + alias + "]" );
        m_delegate.unregister( alias );
    }

    public HttpContext createDefaultHttpContext()
    {
        LOG.debug( "Creating a default context" );
        return m_delegate.createDefaultHttpContext();
    }

    public synchronized void stop()
    {
        LOG.debug( "Stopping http service: [" + this + "]" );
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
        LOG.debug( "Registering servlet [" + servlet + "]" );
        m_delegate.registerServlet( servlet, urlPatterns, initParams, httpContext );
    }

    /**
     * @see WebContainer#registerServlet(javax.servlet.Servlet, String, String[],java.util.Dictionary,org.osgi.service.http.HttpContext)
     */
    public void registerServlet( final Servlet servlet,
                                 final String servletName,
                                 final String[] urlPatterns,
                                 final Dictionary initParams,
                                 final HttpContext httpContext )
        throws ServletException
    {
        LOG.debug( "Registering servlet [" + servlet + "] with name [" + servletName + "]" );
        m_delegate.registerServlet( servlet, servletName, urlPatterns, initParams, httpContext );
    }

    /**
     * @see WebContainer#unregisterServlet(Servlet)
     */
    public void unregisterServlet( final Servlet servlet )
    {
        LOG.debug( "Unregistering servlet [" + servlet + "]" );
        m_delegate.unregisterServlet( servlet );
    }

    /**
     * @see org.ops4j.pax.web.service.WebContainer#registerServlet(java.lang.Class, java.lang.String[], java.util.Dictionary, org.osgi.service.http.HttpContext)
     */
    public void registerServlet(Class<? extends Servlet> servletClass, 
                                String[] urlPatterns, 
                                Dictionary initParams, 
                                HttpContext httpContext) 
        throws ServletException 
    {
        LOG.debug("Registering servlet class [{}]", servletClass);
    	m_delegate.registerServlet(servletClass, urlPatterns, initParams, httpContext);
    }
    
    /**
     * @see org.ops4j.pax.web.service.WebContainer#unregisterServlets(java.lang.Class)
     */
    public void unregisterServlets(Class<? extends Servlet> servletClass) {
        LOG.debug("Unregistering servlet class [{}]", servletClass);
        m_delegate.unregisterServlets(servletClass);
    }
    
    /**
     * @see WebContainer#registerEventListener(EventListener, HttpContext) )
     */
    public void registerEventListener( final EventListener listener,
                                       final HttpContext httpContext )
    {
        LOG.debug( "Registering event listener [" + listener + "]" );
        m_delegate.registerEventListener( listener, httpContext );
    }

    /**
     * @see WebContainer#unregisterEventListener(EventListener)
     */
    public void unregisterEventListener( final EventListener listener )
    {
        LOG.debug( "Unregistering event listener [" + listener + "]" );
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
        LOG.debug( "Registering filter [" + filter + "]" );
        m_delegate.registerFilter( filter, urlPatterns, aliases, initParams, httpContext );
    }

    /**
     * @see WebContainer#unregisterFilter(Filter)
     */
    public void unregisterFilter( final Filter filter )
    {
        LOG.debug( "Unregistering filter [" + filter + "]" );
        m_delegate.unregisterFilter( filter );
    }

    /**
     * @see WebContainer#setContextParam(Dictionary, HttpContext)
     */
    public void setContextParam( final Dictionary params,
                                 final HttpContext httpContext )
    {
        LOG.debug( "Setting context paramters [" + params + "] for http context [" + httpContext + "]" );
        m_delegate.setContextParam( params, httpContext );
    }

    /**
     * {@inheritDoc}
     */
    public void setSessionTimeout( final Integer minutes,
                                   final HttpContext httpContext )
    {
        LOG.debug( "Setting session timeout to " + minutes + " minutes for http context [" + httpContext + "]" );
        m_delegate.setSessionTimeout( minutes, httpContext );
    }

    /**
     * @see WebContainer#registerJsps(String[], HttpContext)
     */
    public void registerJsps( final String[] urlPatterns,
                              final HttpContext httpContext )
    {
        LOG.debug( "Registering jsps" );
        m_delegate.registerJsps( urlPatterns, httpContext );
    }

    /**
     * @see WebContainer#registerJsps(String[], Dictionary, HttpContext)
     */
    public void registerJsps( final String[] urlPatterns,
                              final Dictionary initParams,
                              final HttpContext httpContext )
    {
        LOG.debug( "Registering jsps" );
        m_delegate.registerJsps( urlPatterns, initParams, httpContext );
    }

    /**
     * @see WebContainer#unregisterJsps(HttpContext)
     */
    public void unregisterJsps( final HttpContext httpContext )
    {
        LOG.debug( "Unregistering jsps" );
        m_delegate.unregisterJsps( httpContext );
    }

    /**
     * @see WebContainer#unregisterJsps(HttpContext)
     */
    public void unregisterJsps( final String[] urlPatterns,
                                final HttpContext httpContext )
    {
        LOG.debug( "Unregistering jsps" );
        m_delegate.unregisterJsps( urlPatterns, httpContext );
    }    

    /**
     * @see WebContainer#registerErrorPage(String, String, HttpContext)
     */
    public void registerErrorPage( final String error,
                                   final String location,
                                   final HttpContext httpContext )
    {
        LOG.debug( "Registering error page [" + error + "]" );
        m_delegate.registerErrorPage( error, location, httpContext );
    }

    /**
     * @see WebContainer#unregisterErrorPage(String, HttpContext)
     */
    public void unregisterErrorPage( final String error,
                                     final HttpContext httpContext )
    {
        LOG.debug( "Unregistering error page [" + error + "]" );
        m_delegate.unregisterErrorPage( error, httpContext );
    }

    /**
     * @see WebContainer#registerWelcomeFiles(String[], boolean, HttpContext)
     */
    public void registerWelcomeFiles( final String[] welcomeFiles,
                                      final boolean redirect,
                                      final HttpContext httpContext )
    {
        LOG.debug( "Registering welcome files [" + Arrays.toString( welcomeFiles ) + "]" );
        m_delegate.registerWelcomeFiles( welcomeFiles, redirect, httpContext );
    }

    /**
     * @see WebContainer#unregisterWelcomeFiles(HttpContext)
     */
    public void unregisterWelcomeFiles( final HttpContext httpContext )
    {
        LOG.debug( "Unregistering welcome files" );
        m_delegate.unregisterWelcomeFiles( httpContext );
    }

	public void registerLoginConfig(String authMethod, String realmName, String formLoginPage, String formErrorPage, HttpContext httpContext) {
		LOG.debug("Registering LoginConfig for realm [ "+realmName+" ]");
		m_delegate.registerLoginConfig(authMethod, realmName, formLoginPage, formErrorPage, httpContext);
	}

	public void unregisterLoginConfig(final HttpContext httpContext) {
		LOG.debug("Unregistering LoginConfig");
		m_delegate.unregisterLoginConfig(httpContext);
	}


	public void registerConstraintMapping(String constraintName,
			String url, String mapping, String dataConstraint,
			boolean authentication, List<String> roles,
			HttpContext httpContext) {
		LOG.debug("Registering constraint mapping for [ "+constraintName+" ] ");
		m_delegate.registerConstraintMapping(constraintName, url, mapping, dataConstraint, authentication, roles , httpContext);
		
	}

	public void unregisterConstraintMapping(final HttpContext httpContext) {
		LOG.debug("Unregister constraint mapping");
		m_delegate.unregisterConstraintMapping(httpContext);
	}

	public SharedWebContainerContext getDefaultSharedHttpContext() {
		return m_delegate.getDefaultSharedHttpContext();
	}

	public void registerServletContainerInitializer(
			ServletContainerInitializer servletContainerInitializer,
			Class[] classes, final HttpContext httpContext) {
		m_delegate.registerServletContainerInitializer(servletContainerInitializer, classes, httpContext);
	}

	public void unregisterServletContainerInitializer(HttpContext m_httpContext) {
		m_delegate.unregisterServletContainerInitializer(m_httpContext);		
	}

	public void registerJettyWebXml(URL jettyWebXmlURL,
			HttpContext m_httpContext) {
		m_delegate.registerJettyWebXml(jettyWebXmlURL, m_httpContext);
	}

	@Override
	public void registerJspServlet(String[] urlPatterns, HttpContext httpContext, String jspFile) {
		m_delegate.registerJspServlet(urlPatterns, httpContext, jspFile);
	}

	@Override
	public void registerJspServlet(String[] urlPatterns, Dictionary initParams, HttpContext httpContext, String jspFile) {
		m_delegate.registerJspServlet(urlPatterns, initParams, httpContext, jspFile);
	}

	@Override
	public void setVirtualHosts(List<String> virtualHosts,
			HttpContext httpContext) {
		m_delegate.setVirtualHosts(virtualHosts, httpContext);	
	}

	@Override
	public void setConnectors(List<String> connectors, HttpContext httpContext) {
		m_delegate.setConnectors(connectors, httpContext);	
	}

    public void begin(HttpContext m_httpContext) {
        m_delegate.begin(m_httpContext);
    }

    public void end(HttpContext m_httpContext) {
        m_delegate.end(m_httpContext);
    }
}
