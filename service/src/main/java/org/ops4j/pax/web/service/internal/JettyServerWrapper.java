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

import java.util.IdentityHashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.SessionHandler;
import org.osgi.service.http.HttpContext;

/**
 * Jetty server with a handler collection specific to Pax Web.
 */
public class JettyServerWrapper
    extends Server
{

    private static final Log LOG = LogFactory.getLog( JettyServerWrapper.class );

    private final RegistrationsCluster m_registrationsCluster;
    private final Map<HttpContext, Context> m_contexts;
    private Map<String, Object> m_contextAttributes;
    private Integer m_sessionTimeout;

    public JettyServerWrapper( RegistrationsCluster registrationsCluster )
    {
        Assert.notNull( "Registration Cluster cannot be null", registrationsCluster );
        m_registrationsCluster = registrationsCluster;
        m_contexts = new IdentityHashMap<HttpContext, Context>();
    }

    @Override
    public void addHandler( Handler handler )
    {
        if( getHandler() == null )
        {
            setHandler( new JettyServerHandlerCollection( m_registrationsCluster ) );
        }
        ( (HandlerCollection) getHandler() ).addHandler( handler );
    }

    public void configureContext( final Map<String, Object> attributes, final Integer sessionTimeout )
    {
        m_contextAttributes = attributes;
        m_sessionTimeout = sessionTimeout;
    }

    Context getContext( final HttpContext httpContext )
    {
        return m_contexts.get( httpContext );
    }

    Context getOrCreateContext( final HttpContext httpContext )
    {
        Context context = m_contexts.get( httpContext );
        if( context == null )
        {
            context = addContext( httpContext );
            m_contexts.put( httpContext, context );
        }
        return context;
    }

    void removeContext( final HttpContext httpContext )
    {
        removeHandler( getContext( httpContext ) );
        m_contexts.remove( httpContext );
    }

    private Context addContext( final HttpContext httpContext )
    {
        Context context = new HttpServiceContext( this, m_contextAttributes, httpContext );
        if( m_sessionTimeout != null )
        {
            configureSessionTimeout( context, m_sessionTimeout );
        }
        if( LOG.isInfoEnabled() )
        {
            LOG.info( "added servlet context: " + context );
        }
        if( isStarted() )
        {
            try
            {
                LOG.debug( "(Re)starting servlet contexts..." );
                // start the server handler if not already started
                Handler serverHandler = getHandler();
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
     * Configures the session time out by extracting the session handlers->sessionManager for the context.
     *
     * @param context the context for which the session timeout should be configured
     * @param minutes timeout in minutes
     */
    private void configureSessionTimeout( final Context context, final Integer minutes )
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

}
