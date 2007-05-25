/*  Copyright 2007 Niclas Hedhman.
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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImpl
    implements HttpService, ManagedService
{
    private Server m_server;
    private Bundle m_bundle;
    private HashMap<String, Handler> m_aliases;

    public HttpServiceImpl( Bundle bundle )
    {
        if ( bundle == null ) {
            throw new IllegalArgumentException();
        }
        
        int port =  Integer.getInteger( "org.osgi.service.http.port", 80 ).intValue();
        Object obj = new SocketConnector();
        Connector httpPort = (Connector) obj;
        httpPort.setPort( port );

        int sslport =  Integer.getInteger( "org.osgi.service.http.port.secure", 443 ).intValue();
        Connector httpsPort = new SocketConnector();
        httpsPort.setPort( sslport );

        m_server = new Server(  );
        m_aliases = new HashMap<String, Handler>();
    }

    public void destroy()
    {
        m_server.destroy();
    }

    public void registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext httpContext )
        throws ServletException, NamespaceException
    {
        Map<String, String> init = new HashMap<String, String>();
        Enumeration enumeration = initParams.keys();
        while( enumeration.hasMoreElements() )
        {
            String key = (String) enumeration.nextElement();
            String value = (String) initParams.get( key );
            init.put( key, value );
        }

        ServletHolder holder = new ServletHolder( servlet );
        holder.setInitParameters( init );
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping( holder, alias );
        m_server.addHandler( handler );
        m_aliases.put( alias, handler );
    }

    public void registerResources( String alias, String name, HttpContext httpContext )
        throws NamespaceException
    {
        ResourceHandler handler = new ResourceHandler();
        Resource resource = new OsgiResource( alias, name, httpContext );
        handler.setBaseResource( resource );
        m_server.addHandler( handler );
        m_aliases.put( alias, handler );
    }

    public void unregister( String alias )
    {
        Handler handler = m_aliases.get( alias );
        if( handler != null )
        {
            m_server.removeHandler( handler );
        }
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContextImpl( m_bundle );
    }

    public void updated( Dictionary dictionary )
        throws ConfigurationException
    {

    }
}
