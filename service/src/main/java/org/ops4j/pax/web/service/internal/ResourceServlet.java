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

public class ResourceServlet extends HttpServlet
{

    public static final String REQUEST_HANDLED = ResourceServlet.class.getName() + ".handled";

    private Registration m_registration;

    public void setRegistration( final Registration registration )
    {
        Assert.notNull( "registration == null", registration );
        m_registration = registration;
    }

    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        if( m_registration == null )
        {
            throw new IllegalStateException( "registration not set" );
        }
        String alias = m_registration.getAlias();
        String name = m_registration.getName();
        if( "/".equals( name ) )
        {
            name = "";
        }
        String mapping;
        if( "/".equals( alias ) )
        {
            mapping = name + request.getRequestURI();
        }
        else
        {
            mapping = request.getRequestURI().replaceFirst( alias, name );
        }
        HttpContext httpContext = m_registration.getHttpContext();
        URL url = httpContext.getResource( mapping );
        if( url != null )
        {
            String mimeType = httpContext.getMimeType( mapping );
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
        else
        {
            request.setAttribute( REQUEST_HANDLED, Boolean.FALSE );
        }
    }

}
