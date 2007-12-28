/*  Copyright 2007 Alin Dreghiciu.
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
import java.io.InputStream;
import java.net.URL;
import java.util.EventListener;
import java.util.Map;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.osgi.service.http.HttpContext;

class HttpServiceContext extends Context
{

    private static final Log LOG = LogFactory.getLog( HttpServiceContext.class );

    /**
     * Context attributes.
     */
    private final Map<String, Object> m_attributes;
    private final HttpContext m_httpContext;

    HttpServiceContext( final Server server,
                        final Map<String, Object> attributes,
                        final HttpContext httpContext )
    {
        super( server, "/", Context.SESSIONS );
        m_attributes = attributes;
        m_httpContext = httpContext;
        _scontext = new SContext();
        setServletHandler( new HttpServiceServletHandler( httpContext ) );
    }

    @Override
    protected void doStart()
        throws Exception
    {
        super.doStart();
        if( m_attributes != null )
        {
            for( Map.Entry<String, ?> attribute : m_attributes.entrySet() )
            {
                _scontext.setAttribute( attribute.getKey(), attribute.getValue() );
            }
        }
        LOG.debug( "Started servlet context for http context [" + m_httpContext + "]" );
    }

    @Override
    protected void doStop()
        throws Exception
    {
        super.doStop();
        LOG.debug( "Stopped servlet context for http context [" + m_httpContext + "]" );
    }

    @Override
    public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
        throws IOException, ServletException
    {
        LOG.debug( "Handling request for [" + target + "] using http context [" + m_httpContext + "]" );
        super.handle( target, request, response, dispatch );
    }

    @Override
    public void setEventListeners( final EventListener[] eventListeners )
    {
        if( _sessionHandler != null )
        {
            _sessionHandler.clearEventListeners();
        }

        super.setEventListeners( eventListeners );
        if( _sessionHandler != null )
        {
            for( int i = 0; eventListeners != null && i < eventListeners.length; i++ )
            {
                EventListener listener = eventListeners[ i ];

                if( ( listener instanceof HttpSessionActivationListener )
                    || ( listener instanceof HttpSessionAttributeListener )
                    || ( listener instanceof HttpSessionBindingListener )
                    || ( listener instanceof HttpSessionListener ) )
                {
                    _sessionHandler.addEventListener( listener );
                }

            }
        }
    }

    /**
     * If the listener is a servlet conetx listener and the context is already started, notify the servlet context
     * listener about the fact that context is started. This has to be done separately as the listener could be added
     * after the context is already started, case when servlet context listeners are not notified anymore.
     *
     * @param listener to be notified.
     */
    @Override
    public void addEventListener( final EventListener listener )
    {
        super.addEventListener( listener );
        if( isStarted() && listener instanceof ServletContextListener )
        {
            ( (ServletContextListener) listener ).contextInitialized( new ServletContextEvent( _scontext ) );
        }
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

    @SuppressWarnings( { "deprecation" } )
    public class SContext extends Context.SContext
    {

        @Override
        public URL getResource( final String path )
        {
            if( LOG.isInfoEnabled() )
            {
                LOG.info( "getting resource: [" + path + "]" );
            }
            URL resource = m_httpContext.getResource( path );
            if( LOG.isInfoEnabled() )
            {
                LOG.info( "found resource: " + resource );
            }
            return resource;
        }

        @Override
        public InputStream getResourceAsStream( final String path )
        {
            URL url = getResource( path );
            if( url != null )
            {
                try
                {
                    return url.openStream();
                }
                catch( IOException e )
                {
                    return null;
                }
            }
            return null;
        }

        @Override
        public String getMimeType( final String name )
        {
            if( LOG.isInfoEnabled() )
            {
                LOG.info( "getting mime type for: [" + name + "]" );
            }
            return m_httpContext.getMimeType( name );
        }

    }

}
