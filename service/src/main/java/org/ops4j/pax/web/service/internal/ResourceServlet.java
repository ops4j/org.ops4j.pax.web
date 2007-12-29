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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.resource.Resource;
import org.osgi.service.http.HttpContext;

class ResourceServlet extends HttpServlet
{

    private final HttpContext m_httpContext;
    private final String m_contextName;
    private final String m_alias;
    private final String m_name;

    ResourceServlet( final HttpContext httpContext,
                     final String contextName,
                     final String alias,
                     final String name )
    {
        m_httpContext = httpContext;
        m_contextName = "/" + contextName;
        m_alias = alias;
        if( "/".equals( name ) )
        {
            m_name = "";
        }
        else
        {
            m_name = name;
        }
    }

    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        String mapping;
        if( m_contextName.equals( m_alias ) )
        {
            mapping = m_name + request.getRequestURI();
        }
        else
        {
            mapping = request.getRequestURI().replaceFirst( m_contextName, "/" );
            mapping = mapping.replaceFirst( m_alias, m_name );
        }
        final URL url = m_httpContext.getResource( mapping );
        if( url != null )
        {
            String mimeType = m_httpContext.getMimeType( mapping );
            if( mimeType == null )
            {
                URLConnection connection = url.openConnection();
                mimeType = connection.getContentType();
                response.setContentType( mimeType );
                // TODO shall we handle also content encoding?
            }
            Resource resource = Resource.newResource( url, false );
            OutputStream out = response.getOutputStream();
            if( out != null ) // null should be just in unit testing
            {
                if( out instanceof HttpConnection.Output )
                {
                    ( (HttpConnection.Output) out ).sendContent( resource.getInputStream() );
                }
                else
                {
                    // Write content normally
                    resource.writeTo( out, 0, resource.length() );
                }
            }
            response.setStatus( HttpServletResponse.SC_OK );
        }
    }

}
