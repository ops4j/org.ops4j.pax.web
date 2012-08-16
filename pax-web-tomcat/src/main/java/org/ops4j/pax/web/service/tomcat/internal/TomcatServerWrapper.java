/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.startup.Tomcat;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.spi.model.ContextModel;
import org.ops4j.pax.web.service.spi.model.ErrorPageModel;
import org.ops4j.pax.web.service.spi.model.EventListenerModel;
import org.ops4j.pax.web.service.spi.model.FilterModel;
import org.ops4j.pax.web.service.spi.model.Model;
import org.ops4j.pax.web.service.spi.model.SecurityConstraintMappingModel;
import org.ops4j.pax.web.service.spi.model.ServletModel;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Romain Gilles
 */
class TomcatServerWrapper implements ServerWrapper
{
    private static final Logger LOG = LoggerFactory.getLogger( TomcatServerWrapper.class );
    private final EmbeddedTomcat m_server;
    private final Map<HttpContext, Context> m_contexts = new ConcurrentHashMap<HttpContext, Context>();

    private TomcatServerWrapper(EmbeddedTomcat server)
    {
        NullArgumentException.validateNotNull( server, "server" );
        this.m_server = server;
    }

    static ServerWrapper getInstance(EmbeddedTomcat server)
    {
        return new TomcatServerWrapper( server );
    }

    @Override
    public void start()
    {
        LOG.debug( "start server" );
        try
        {
            long t1 = System.nanoTime();
            m_server.getHost();
            m_server.start();
            long t2 = System.nanoTime();
            if( LOG.isInfoEnabled() )
            {
                LOG.info( "TomCat server startup in " + ( ( t2 - t1 ) / 1000000 ) + " ms" );
            }
        } catch( LifecycleException e )
        {
            throw new ServerStartException( m_server.getServer().getInfo(), e );
        }
    }

    @Override
    public void stop()
    {
        LOG.debug( "stop server" );
        LifecycleState state = m_server.getServer().getState();
        if( LifecycleState.STOPPING_PREP.compareTo( state ) <= 0 && LifecycleState.DESTROYED.compareTo( state ) >= 0 )
        {
            throw new IllegalStateException( "stop already called!" );
        }
        else
        {
            try
            {
                m_server.stop();
                m_server.destroy();
            } catch( LifecycleException e )
            {
                throw new ServerStopException( m_server.getServer().getInfo(), e );
            }
        }
    }

    public void addServlet(final ServletModel model)
    {
        LOG.debug( "add servlet [{}]", model );
        Context context = findOrCreateContext( model.getContextModel() );
        String servletName = model.getName();
        addServletMappings( context, servletName, model.getUrlPatterns() );
        Wrapper wrapper = Tomcat.addServlet( context, servletName, model.getServlet() );
        addInitParameters( wrapper, model.getInitParams() );
    }

    @Override
    public void removeServlet(ServletModel model)
    {
        LOG.debug( "remove servlet [{}]", model );
        Context context = findContext( model );
        if( context == null )
        {
            throw new TomcatRemoveServletException( "cannot remove servlet cannot find the associated container: "
                    + model );
        }
        Container servlet = context.findChild( model.getName() );
        if( servlet == null )
        {
            throw new TomcatRemoveServletException( "cannot find the servlet to remove: " + model );
        }
        context.removeChild( servlet );
    }


    public void removeContext(HttpContext httpContext)
    {
        LOG.debug( "remove context [{}]", httpContext );
        Context context = m_contexts.remove( httpContext );
        if( context == null )
        {
            throw new RemoveContextException( "cannot remove the context because it does not exist: " + httpContext );
        }
        try
        {
            context.destroy();
        } catch( LifecycleException e )
        {
            throw new RemoveContextException( "cannot destroy the context: " + httpContext, e );
        }
        throw new UnsupportedOperationException( "not yet implemented :(" );
    }

    public void addEventListener(EventListenerModel eventListenerModel)
    {
        LOG.debug( "add event listener: [{}]", eventListenerModel );
        ServletContext servletContext = findOrCreateServletContext( eventListenerModel );
        servletContext.addListener( eventListenerModel.getEventListener() );
    }

    private ServletContext findOrCreateServletContext(EventListenerModel eventListenerModel)
    {
        Context context = findOrCreateContext( eventListenerModel );
        return context.getServletContext();
    }

    public void removeEventListener(EventListenerModel eventListenerModel)
    {
        LOG.debug( "remove event listener: [{}]", eventListenerModel );
        NullArgumentException.validateNotNull( eventListenerModel, "eventListenerModel" );
        NullArgumentException.validateNotNull( eventListenerModel.getEventListener(), "eventListenerModel#weventListener" );
        Context context = findOrCreateContext( eventListenerModel );
        //TODO open a bug in tomcat
        if( !removeApplicationEventListener( context, eventListenerModel.getEventListener() ) )
        {
            if( !removeApplicationLifecycleListener( context, eventListenerModel.getEventListener() ) )
            {
                throw new RemoveEventListenerException( "cannot remove the event lister it is a not support class : " + eventListenerModel );
            }
        }

    }

    private boolean removeApplicationLifecycleListener(Context context, EventListener eventListener)
    {

        if( !isApplicationLifecycleListener( eventListener ) )
        {
            return false;
        }
        List<Object> applicationLifecycleListeners = Arrays.asList( context.getApplicationLifecycleListeners() );
        if( applicationLifecycleListeners.remove( eventListener ) )
        {
            context.setApplicationLifecycleListeners( applicationLifecycleListeners.toArray() );
            return true;
        }
        return false;
    }

    private boolean isApplicationLifecycleListener(EventListener eventListener)
    {
        return ( eventListener instanceof HttpSessionListener
                || eventListener instanceof ServletContextListener );
    }

    private boolean removeApplicationEventListener(Context context, EventListener eventListener)
    {
        if( !isApplicationEventListener( eventListener ) )
        {
            return false;
        }
        List<Object> applicationEventListener = Arrays.asList( context.getApplicationEventListeners() );
        if( applicationEventListener.remove( eventListener ) )
        {
            context.setApplicationEventListeners( applicationEventListener.toArray() );
            return true;
        }
        return false;
    }

    private boolean isApplicationEventListener(EventListener eventListener)
    {
        return ( eventListener instanceof ServletContextAttributeListener ||
                eventListener instanceof ServletRequestListener ||
                eventListener instanceof ServletRequestAttributeListener ||
                eventListener instanceof HttpSessionAttributeListener );
    }

    public void addFilter(FilterModel filterModel)
    {
        throw new UnsupportedOperationException( "not yet implemented :(" );
    }

    public void removeFilter(FilterModel filterModel)
    {
        throw new UnsupportedOperationException( "not yet implemented :(" );
    }

    public void addErrorPage(ErrorPageModel model)
    {
        Context context = findContext( model );
        if( context == null )
        {
            throw new AddErrorPageException( "cannot retrieve the associated context: " + model );
        }
        ErrorPage errorPage = createErrorPage( model );
        context.addErrorPage( errorPage );
    }

    private ErrorPage createErrorPage(ErrorPageModel model)
    {
        NullArgumentException.validateNotNull( model, "model" );
        NullArgumentException.validateNotNull( model.getLocation(), "model#location" );
        NullArgumentException.validateNotNull( model.getError(), "model#error" );
        ErrorPage errorPage = new ErrorPage();
        errorPage.setLocation( model.getLocation() );
        Integer errorCode = parseErrorCode( model.getError() );
        if( errorCode != null )
        {
            errorPage.setErrorCode( errorCode );
        }
        else
        {
            errorPage.setExceptionType( model.getError() );
        }
        return errorPage;
    }

    private Integer parseErrorCode(String errorCode)
    {
        try
        {
            return Integer.parseInt( errorCode );
        } catch( NumberFormatException e )
        {
            return null;
        }
    }

    public void removeErrorPage(ErrorPageModel model)
    {
        Context context = findContext( model );
        if( context == null )
        {
            throw new RemoveErrorPageException( "cannot retrieve the associated context: " + model );
        }
        ErrorPage errorPage = createErrorPage( model );
        context.removeErrorPage( errorPage );
    }

    public Servlet createResourceServlet(ContextModel contextModel, String alias, String name)
    {
        throw new UnsupportedOperationException( "not yet implemented :(" );
    }

    public void addSecurityConstraintMapping(SecurityConstraintMappingModel secMapModel)
    {
        throw new UnsupportedOperationException( "not yet implemented :(" );
    }

    private void addServletMappings(Context context, String servletName, String[] urlPatterns)
    {
        NullArgumentException.validateNotNull( urlPatterns, "urlPatterns" );
        for( String urlPattern : urlPatterns )
        {//TODO add a enhancement to tomcat it is in the specification so tomcat should provide it out of the box
            context.addServletMapping( urlPattern, servletName );
        }
    }

    private void addInitParameters(Wrapper wrapper, Map<String, String> initParameters)
    {
        NullArgumentException.validateNotNull( initParameters, "initParameters" );
        NullArgumentException.validateNotNull( wrapper, "wrapper" );
        for( Map.Entry<String, String> initParam : initParameters.entrySet() )
        {
            wrapper.addInitParameter( initParam.getKey(), initParam.getValue() );
        }
    }

    private Context findOrCreateContext(Model model)
    {
        NullArgumentException.validateNotNull( model, "model" );
        return findOrCreateContext( model.getContextModel() );
    }

    private Context findOrCreateContext(ContextModel contextModel)
    {
        Context context = findContext( contextModel );
        if( context == null )
        {
            context = createContext( contextModel );
        }
        return context;
    }

    private Context createContext(ContextModel contextModel)
    {
        //        Context context = m_server.addContext(m_server.getHost(),contextModel.);
        Context context = m_server.addContext( contextModel.getContextName(), m_server.getBasedir() );
//        context.setParentClassLoader(contextModel.getClassLoader()); TODO maybe
        m_contexts.put( contextModel.getHttpContext(), context );
        return context;
    }

    private Context findContext(ContextModel contextModel)
    {
        String contextName = contextModel.getContextName();
        return m_server.findContext( contextName );
    }

    private Context findContext(Model model)
    {
        return findContext( model.getContextModel() );
    }
}
