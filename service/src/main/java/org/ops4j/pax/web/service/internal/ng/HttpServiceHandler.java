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
package org.ops4j.pax.web.service.internal.ng;

import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.Request;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

public class HttpServiceHandler extends ServletHandler
{
    private RegistrationsCluster m_registrationsCluster;

    public HttpServiceHandler( RegistrationsCluster registrationsCluster )
    {
        m_registrationsCluster = registrationsCluster;
    }

    public void handle( String target, HttpServletRequest request, HttpServletResponse response, int type )
        throws IOException, ServletException
    {
        String match = target;
        while ( !"".equals( match ))
        {
            HttpTarget httpTarget = m_registrationsCluster.getByAlias( match );
            if ( httpTarget != null && httpTarget.getHttpContext().handleSecurity( request, response ) )
            {
                if ( httpTarget instanceof HttpServlet ) {
                    handleServlet( match, request, response, type );
                }
            }
            if( ((Request) request).isHandled() )
            {
                break;
            }
            match = match.substring( 0, match.lastIndexOf( "/" ) );
        }
        // if still not handled try out "/"
        if( !((Request) request).isHandled() && !"/".equals( target ))
        {
            handle( "/", request, response, type );
        }
    }

    public void handleServlet( String target, HttpServletRequest request, HttpServletResponse response, int type )
        throws IOException, ServletException
    {
        super.handle( target, request, response, type );
    }
}
