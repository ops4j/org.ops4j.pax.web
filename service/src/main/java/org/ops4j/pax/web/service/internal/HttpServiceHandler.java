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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.ServletHandler;
import org.osgi.service.http.HttpContext;

public class HttpServiceHandler extends ServletHandler
{
    private RegistrationsCluster m_registrationsCluster;
    private static ThreadLocal<HttpContext> m_activeHttpContext;

    public HttpServiceHandler( final RegistrationsCluster registrationsCluster )
    {
        m_registrationsCluster = registrationsCluster;
        m_activeHttpContext = new ThreadLocal<HttpContext>();
    }

    @Override
    public void handle( String target, HttpServletRequest request, HttpServletResponse response, int dispatchMode )
        throws IOException, ServletException
    {
        handle( target, target, request, response, dispatchMode );
    }

    private void handle( String requestTarget, String target, HttpServletRequest request, HttpServletResponse response, int dispatchMode )
        throws IOException, ServletException
    {
        String match = target;
        boolean handled = false;
        while ( !"".equals( match ))
        {
            HttpTarget httpTarget = m_registrationsCluster.getByAlias( match );
            if ( httpTarget != null )
            {
                if ( httpTarget.getHttpContext().handleSecurity( request, response ) )
                {
                    HttpTarget.Type targetType = httpTarget.getType();
                    if ( targetType == HttpTarget.Type.SERVLET )
                    {
                        handled = handleServlet( httpTarget, requestTarget, request, response, dispatchMode );
                    }
                    else if ( targetType == HttpTarget.Type.RESOURCE )
                    {
                        handled = handleResource( requestTarget, request, response, (HttpResource) httpTarget );
                    }
                    else
                    {
                        throw new IllegalStateException( "unsupported target type: " + targetType );
                    }
                    if( handled )
                    {
                        markAsHandled( request );
                        break;
                    }                    
                }
                else
                {
                    // on case of security constraints not fullfiled, handleSecurity is supposed to set the right headers
                    // TODO check if the request must be marked as handled
                    return;
                }
            }
            match = match.substring( 0, match.lastIndexOf( "/" ) );
        }
        // if still not handled try out "/"
        if( !handled && !"/".equals( target ) ) 
        {
            handle( requestTarget, "/", request, response, dispatchMode );
            return;
        }
        if ( !handled )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
    }

    public boolean  handleServlet(
        final HttpTarget httpTarget,
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        int dispatchMode )
        throws IOException, ServletException
    {
        try
        {
            setActiveHttpContext( httpTarget.getHttpContext() );
            super.handle( target, request, response, dispatchMode );
        }
        finally
        {
            removeActiveHttpContext();
        }
        return true;
    }

    private boolean handleResource(
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final HttpResource httpTarget)
        throws IOException
    {
        String mapping = null;
        String alias = httpTarget.getAlias();
        String name = httpTarget.getName();
        if ( "/".equals( name) ) {
            name = "";
        }
        if ( "/".equals( alias ) )
        {
            mapping = name + target;
        }
        else
        {
            mapping = target.replaceFirst( alias, name); 
        }
        HttpContext httpContext = httpTarget.getHttpContext();
        URL url = httpContext.getResource( mapping );
        if ( url != null )
        {
            InputStream inputStream = null;
            String mimeType = httpContext.getMimeType( mapping );
            if ( mimeType == null)
            {
                URLConnection connection = url.openConnection();
                mimeType = connection.getContentType();
                response.setContentType( mimeType );
                // TODO shall we handle also content encoding?
                inputStream = connection.getInputStream();
            }
            else
            {
                inputStream = url.openStream();
            }
            OutputStream outputStream = response.getOutputStream();
            if ( outputStream != null) // null should be just in unit testing
            {
                int contentLength = StreamUtils.copy( response.getOutputStream(), inputStream );
                response.setContentLength( contentLength );
            }
        }
        return url != null;
        // TODO find out if handle security shall be called again when returned url is null         
    }

    private void markAsHandled( final HttpServletRequest request )
    {
        if ( request instanceof Request )
        {
           ((Request) request).setHandled( true );
        }
    }

    private static void setActiveHttpContext( final HttpContext httpContext )
    {
        m_activeHttpContext.set( httpContext );
    }

    public static HttpContext getActiveHttpContext()
    {
        return m_activeHttpContext.get();
    }

    private static void removeActiveHttpContext()
    {
        m_activeHttpContext.remove();
    }

}
