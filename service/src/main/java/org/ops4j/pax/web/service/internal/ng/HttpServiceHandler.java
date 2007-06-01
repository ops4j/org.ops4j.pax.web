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

import java.io.IOException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.Request;

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
                        handled = handleServlet( match, request, response, type );
                    }
                    else if ( targetType == HttpTarget.Type.RESOURCE )
                    {
                        handled = handleResource( target, request, response, (HttpResource) httpTarget );
                    }
                    else
                    {
                        throw new IllegalStateException( "unsupported target type: " + targetType );
                    }
                    if( handled )
                    {
                        break;
                    }                    
                }
                else
                {
                    // on case of security constraints nto fullfilled handleSecurity is supposed to set the right headers
                    // TODO check if the request must be marked as handled
                    return;
                }
            }
            match = match.substring( 0, match.lastIndexOf( "/" ) );
        }
        // if still not handled try out "/"
        if( !handled && !"/".equals( target ) ) 
        {
            handle( "/", request, response, type );
        }
        // TODO HttpServletResponse.SC_NOT_FOUND if no matching
    }

    public boolean  handleServlet( String target, HttpServletRequest request, HttpServletResponse response, int type )
        throws IOException, ServletException
    {
        super.handle( target, request, response, type );
        return true;
    }

    private boolean handleResource(
        final String target,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final HttpResource httpTarget)
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
        URL url = httpTarget.getHttpContext().getResource( mapping );
        return url != null;
    }
    
}
