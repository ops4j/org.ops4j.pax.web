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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.ResourceMapping;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultResourceMapping;

public class Activator
    implements BundleActivator
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( Activator.class );

    private ServiceRegistration m_rootServletReg;
    private ServiceRegistration m_servletReg;
    private ServiceRegistration m_resourcesReg;
    private ServiceRegistration m_filterReg;
    private ServiceRegistration m_listenerReg;
    private ServiceRegistration m_httpContextReg;
    private ServiceRegistration m_forbiddenServletReg;

    public void start( final BundleContext bundleContext )
        throws Exception
    {
        Dictionary props;

        // register a custom http context that forbids access
        props = new Hashtable();
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "forbidden" );
        m_httpContextReg =
            bundleContext.registerService( HttpContext.class.getName(), new WhiteboardContext(), props );
        // and an servlet that cannot be accessed due to the above context
        props = new Hashtable();
        props.put( ExtenderConstants.PROPERTY_ALIAS, "/forbidden" );
        props.put( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID, "forbidden" );
        m_forbiddenServletReg =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/forbidden" ), props );

        props = new Hashtable();
        props.put( "alias", "/whiteboard" );
        m_servletReg =
            bundleContext.registerService( Servlet.class.getName(), new WhiteboardServlet( "/whiteboard" ), props );

        props = new Hashtable();
        props.put( "alias", "/" );
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
            props = new Hashtable();
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
    }

    public void stop( BundleContext bundleContext )
        throws Exception
    {
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
