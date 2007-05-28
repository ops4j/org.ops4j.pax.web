/*  Copyright 2007 Niclas Hedhman.
 *  Copyright 2007 Alin Dreghiciu.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerWrapper;
import org.mortbay.jetty.servlet.ServletHandler;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class OsgiHandler extends HandlerWrapper
    implements Handler
{
    private Map<String, Registration> m_registrations;
    private Map<HttpContext, ContextHandler> m_contextMapping;

    public OsgiHandler()
    {
        m_registrations = new HashMap<String, Registration>();
        m_contextMapping = new HashMap<HttpContext, ContextHandler>();
    }

    public void registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
        throws Exception
    {
        validateRegisterServletArguments( alias, servlet );
        ContextHandler contextHandler = m_contextMapping.get( context );
        if( contextHandler == null )
        {
            contextHandler = new OsgiContextHandler( context );
            m_contextMapping.put( context, contextHandler );
        }
        ServletRegistration registration = new ServletRegistration( alias, servlet, initParams, context );
        m_registrations.put( alias, registration );
    }

    public void registerResource( String alias, String name, HttpContext context )
        throws NamespaceException
    {
        validateRegisterResourcesArguments( alias, name );
        ResourceRegistration registration = new ResourceRegistration( alias, name, context );
        m_registrations.put( alias, registration );
    }

    public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatch )
        throws IOException, ServletException
    {
        String match = target;
        while( "".equals( match ) )
        {
            for( Registration registration : m_registrations.values() )
            {
                String alias = registration.getAlias();
                if( alias.equals( match ) )
                {
                    HttpContext context = registration.getContext();
                    if( context.handleSecurity( request, response ) )
                    {
                        if( registration instanceof ResourceRegistration )
                        {
                            String internalName = ((ResourceRegistration) registration).getName();
                            String resourceName;
                            if( "/".equals( alias ) )
                            {
                                resourceName = internalName + target;
                            }
                            else
                            {
                                resourceName = internalName + target.substring( alias.length() );
                            }
                            URL url = context.getResource( resourceName );
                            if( url != null )
                            {
                                String mimeType = context.getMimeType( resourceName );
                                response.setContentType( mimeType );
                                copyStream( response.getOutputStream(), url.openStream() );
                                ((Request) request).setHandled( true );
                            }
                        }
                        else
                        {
                            ServletHandler servletHandler = ((ServletRegistration) registration).getServletHandler();
                            servletHandler.handle( target, request, response, dispatch );
                        }
                    }
                }
            }
            if( ((Request) request).isHandled() )
            {
                return;
            }
            int pos = match.lastIndexOf( "/" );
            match = match.substring( 0, pos );
        }
    }

    private int copyStream( OutputStream outputStream, InputStream inputStream )
        throws IOException
    {
        if( !(outputStream instanceof BufferedOutputStream) )
        {
            outputStream = new BufferedOutputStream( outputStream );
        }
        if( !(inputStream instanceof BufferedInputStream) )
        {
            inputStream = new BufferedInputStream( inputStream );
        }
        int count = 0;
        try
        {
            int b = inputStream.read();
            while( b != -1 )
            {
                outputStream.write( b );
                count++;
                b = inputStream.read();
            }
        }
        finally
        {
            inputStream.close();
        }
        return count;
    }

    public void unregister( String alias )
        throws Exception
    {
        Registration reg = m_registrations.remove( alias );
        if( reg instanceof ServletRegistration )
        {
            ServletHandler servletHandler = ((ServletRegistration) reg).getServletHandler();
            servletHandler.stop();
            servletHandler.destroy();
        }
    }

    private void validateRegisterServletArguments( String alias, Servlet servlet )
        throws NamespaceException
    {
        validateAlias( alias );
        if( servlet == null )
        {
            throw new IllegalArgumentException( "servlet == null" );
        }
    }

    private void validateRegisterResourcesArguments( String alias, String name )
        throws NamespaceException
    {
        validateAlias( alias );
        if( name == null )
        {
            throw new IllegalArgumentException( "name == null" );
        }
        if( name.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "name ends with slash (/)" );
        }
    }

    private void validateAlias( String alias )
        throws NamespaceException
    {
        if( alias == null )
        {
            throw new IllegalArgumentException( "alias == null" );
        }
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias ends with slash (/)" );
        }
        // check for duplicate registration
        if( m_registrations.containsKey( alias ) )
        {
            throw new NamespaceException( "alias is already in use" );
        }
    }
}
