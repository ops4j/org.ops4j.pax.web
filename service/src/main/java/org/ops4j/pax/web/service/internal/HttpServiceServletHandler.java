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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.ServletHandler;

public class HttpServiceServletHandler extends ServletHandler
{

    private static final Log LOG = LogFactory.getLog( HttpServiceServletHandler.class );

    private final Registrations m_registrations;

    public HttpServiceServletHandler( final Registrations registrations )
    {
        m_registrations = registrations;
    }

    @Override
    public void handle(
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final int dispatchMode )
        throws IOException, ServletException
    {
        if( LOG.isDebugEnabled() )
        {
            LOG.debug( "handling request: [" + target + "]" );
        }
        String match = target;
        boolean handled = false;
        // keep on looking for a match till is handled or the request sub-syting becomes empty or "/"
        while( !handled
               && !"".equals( match )
               && ( "/".equals( target ) || !"/".equals( match ) ) )
        {
            final Registration registration = m_registrations.getByAlias( match );
            if( registration != null )
            {
                final HttpServiceRequestWrapper requestWrapper = new HttpServiceRequestWrapper( request );
                final HttpServiceResponseWrapper responseWrapper = new HttpServiceResponseWrapper( response );

                if( registration.getHttpContext().handleSecurity( requestWrapper, responseWrapper ) )
                {
                    internalHandle( target, request, dispatchMode, responseWrapper );
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
                handled = responseWrapper.isStatusSet();
            }
            // next, try for a substring by removing the last "/" and everything to the right of the last "/"
            match = match.substring( 0, match.lastIndexOf( "/" ) );
        }
        if( handled && !response.isCommitted() )
        {
            // force commit
            response.flushBuffer();
        }
    }

    /**
     * Delegates to super. Provided in order to be overriden by subclasses.
     *
     * @see ServletHandler#handle(String, HttpServletRequest, HttpServletResponse, int)
     */
    protected void internalHandle( String target, HttpServletRequest request, int dispatchMode,
                                   HttpServletResponse response )
        throws IOException, ServletException
    {
        super.handle( target, request, response, dispatchMode );
    }

}
