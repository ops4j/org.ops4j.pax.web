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

import java.util.HashMap;
import org.mortbay.jetty.HandlerContainer;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.SessionHandler;
import org.osgi.service.http.HttpContext;

public class OsgiContext extends Context
{
    private HashMap<ServletHolder, HttpContext> m_mappings;

    public OsgiContext( HandlerContainer parent, SessionHandler sessionHandler, SecurityHandler securityHandler,
                            ServletHandler servletHandler, ErrorHandler errorHandler
    )
    {
        super( parent, sessionHandler, securityHandler, servletHandler, errorHandler );
        m_mappings = new HashMap<ServletHolder, HttpContext>();
    }

    void addContextMapping( ServletHolder holder, HttpContext context )
    {
        m_mappings.put( holder, context );
    }

    void removeContextMapping( ServletHolder holder )
    {
        m_mappings.remove( holder );
    }

    HttpContext getHttpContextForServlet( ServletHolder holder )
    {
        return m_mappings.get( holder );
    }
    
}
