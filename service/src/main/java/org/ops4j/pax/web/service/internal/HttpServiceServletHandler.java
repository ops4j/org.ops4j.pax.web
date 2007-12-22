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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.servlet.ServletHandler;
import org.osgi.service.http.HttpContext;

class HttpServiceServletHandler
    extends ServletHandler
{

    private final HttpContext m_httpContext;

    HttpServiceServletHandler( final HttpContext httpContext )
    {
        Assert.notNull( "Http Context cannot be null", httpContext );
        m_httpContext = httpContext;
    }

    @Override
    public void handle(
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final int dispatchMode )
        throws IOException, ServletException
    {
        final HttpServiceRequestWrapper requestWrapper = new HttpServiceRequestWrapper( request );
        final HttpServiceResponseWrapper responseWrapper = new HttpServiceResponseWrapper( response );
        if( m_httpContext.handleSecurity( requestWrapper, responseWrapper ) )
        {
            super.handle( target, request, response, dispatchMode );
        }
        else
        {
            // on case of security constraints not fullfiled, handleSecurity is supposed to set the right
            // headers but to be sure lets verify the response header for 401 (unauthorized)
            // because if the header is not set the processing will go on with the rest of the contexts
            if( !responseWrapper.isCommitted() )
            {
                if( !responseWrapper.isStatusSet() )
                {
                    responseWrapper.sendError( HttpServletResponse.SC_UNAUTHORIZED );
                }
                else
                {
                    responseWrapper.sendError( responseWrapper.getStatus() );
                }
            }
        }
    }

}
