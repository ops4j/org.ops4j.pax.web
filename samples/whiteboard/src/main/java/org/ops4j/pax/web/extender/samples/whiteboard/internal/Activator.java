/*
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
package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.util.Dictionary;
import java.util.EventListener;
import java.util.Hashtable;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultWelcomeFileMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultErrorPageMapping;

public class Activator
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( Activator.class );

    private ServiceRegistration m_rootServletReg;
    private ServiceRegistration m_servletReg;
    private ServiceRegistration m_resourcesReg;
    private ServiceRegistration m_filterReg;
    private ServiceRegistration m_listenerReg;
    private ServiceRegistration m_httpContextReg;
    private ServiceRegistration m_forbiddenServletReg;
    private ServiceRegistration m_exceptionServletRegistration;
    private ServiceRegistration m_welcomeFileRegistration;
    private ServiceRegistration m_404errorpageRegistration;
    private ServiceRegistration m_uncaughtExceptionRegistration;
    private ServiceRegistration m_rootResourceMappingRegistration;

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        Dictionary<String, String> props;

        // register a custom http context that forbids access
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "forbidden" );
        m_httpContextReg =
            bundleContext.registerService( HttpContext.class.getName(), new WhiteboardContext(), props );
        // and an servlet that cannot be accessed due to the above context
        props = new Hashtable<String, String>();
        props.put( ExtenderConstants.PROPERTY_ALIAS, "/forbidden" );
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "forbidden" );
        m_forbiddenServletReg =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/forbidden" ), props );

        props = new Hashtable<String, String>();
        props.put( "alias", "/whiteboard" );
        m_servletReg =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/whiteboard" ), props );

        props = new Hashtable<String, String>();
        props.put( "alias", "/root" );
        m_rootServletReg =
            bundleContext.registerService( HttpServlet.class.getName(), new WhiteboardServlet( "/root" ), props );

        DefaultResourceMapping resourceMapping = new DefaultResourceMapping();
        resourceMapping.setAlias( "/whiteboardresources" );
        resourceMapping.setPath( "/images" );
        m_resourcesReg =
            bundleContext.registerService( ResourceMapping.class.getName(), resourceMapping, null );

        try
        {
            // register a filter
            props = new Hashtable<String, String>();
            props.put( ExtenderConstants.PROPERTY_URL_PATTERNS, "/whiteboard/filtered/*" );
            m_filterReg =
                bundleContext.registerService( Filter.class.getName(), new WhiteboardFilter(), props );
        }
        catch( NoClassDefFoundError ignore )
        {
            // in this case most probably that we do not have a servlet version >= 2.3
            // required by our filter
            LOG.warn( "Cannot start filter example (javax.servlet version?): " + ignore.getMessage() );
        }

        try
        {
            // register a servlet request listener
            m_listenerReg =
                bundleContext.registerService( EventListener.class.getName(), new WhiteboardListener(), null );
        }
        catch( NoClassDefFoundError ignore )
        {
            // in this case most probably that we do not have a servlet version >= 2.4
            // required by our request listener
            LOG.warn( "Cannot start filter example (javax.servlet version?): " + ignore.getMessage() );
        }

        // servlet to test exceptions and error pages
        props = new Hashtable<String, String>();
        props.put( "alias", "/exception" );
        m_exceptionServletRegistration =
            bundleContext.registerService( HttpServlet.class.getName(), new ExceptionServlet(), props );

        // register resource at root of bundle
        DefaultResourceMapping rootResourceMapping = new DefaultResourceMapping();
        rootResourceMapping.setAlias( "/" );
        rootResourceMapping.setPath( "" );
        m_rootResourceMappingRegistration =
            bundleContext.registerService( ResourceMapping.class.getName(), rootResourceMapping, null );

        // register welcome page - interesting how it will work with the root servlet, i.e. will it showdow it
        DefaultWelcomeFileMapping welcomeFileMapping = new DefaultWelcomeFileMapping();
        welcomeFileMapping.setRedirect( true );
        welcomeFileMapping.setWelcomeFiles( new String[]{ "index.html", "welcome.html" } );
        m_welcomeFileRegistration =
            bundleContext.registerService( WelcomeFileMapping.class.getName(), welcomeFileMapping, null );

        // register error pages for 404 and java.lang.Exception
        DefaultErrorPageMapping errorpageMapping = new DefaultErrorPageMapping();
        errorpageMapping.setError( "404" );
        errorpageMapping.setLocation( "/404.html" );

        m_404errorpageRegistration =
            bundleContext.registerService( ErrorPageMapping.class.getName(), errorpageMapping, null );

        // java.lang.Exception
        DefaultErrorPageMapping exceptionErrorMapping = new DefaultErrorPageMapping();
        exceptionErrorMapping.setError( java.lang.Exception.class.getName() );
        exceptionErrorMapping.setLocation( "/uncaughtException.html" );
        m_uncaughtExceptionRegistration =
            bundleContext.registerService( ErrorPageMapping.class.getName(), exceptionErrorMapping, null );
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
        if( m_rootResourceMappingRegistration != null )
        {
            m_rootResourceMappingRegistration.unregister();
            m_rootResourceMappingRegistration = null;
        }
        if( m_uncaughtExceptionRegistration != null )
        {
            m_uncaughtExceptionRegistration.unregister();
            m_uncaughtExceptionRegistration = null;
        }
        if( m_404errorpageRegistration != null )
        {
            m_404errorpageRegistration.unregister();
            m_404errorpageRegistration = null;
        }
        if( m_welcomeFileRegistration != null )
        {
            m_welcomeFileRegistration.unregister();
            m_welcomeFileRegistration = null;
        }
        if( m_rootServletReg != null )
        {
            m_rootServletReg.unregister();
            m_rootServletReg = null;
        }
        if( m_servletReg != null )
        {
            m_servletReg.unregister();
            m_servletReg = null;
        }
        if( m_exceptionServletRegistration != null )
        {
            m_exceptionServletRegistration.unregister();
            m_exceptionServletRegistration = null;
        }
        if( m_resourcesReg != null )
        {
            m_resourcesReg.unregister();
            m_resourcesReg = null;
        }
        if( m_filterReg != null )
        {
            m_filterReg.unregister();
            m_filterReg = null;
        }
        if( m_listenerReg != null )
        {
            m_listenerReg.unregister();
            m_listenerReg = null;
        }
        if( m_httpContextReg != null )
        {
            m_httpContextReg.unregister();
            m_httpContextReg = null;
        }
        if( m_forbiddenServletReg != null )
        {
            m_forbiddenServletReg.unregister();
            m_forbiddenServletReg = null;
        }
    }
}
