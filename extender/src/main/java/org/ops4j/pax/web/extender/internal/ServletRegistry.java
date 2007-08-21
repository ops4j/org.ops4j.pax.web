/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Registers/Unregisters servlets and keeps track of them.
 */
public class ServletRegistry
{

    /**
     * Servlet -> alias mapping.
     */
    private final Map<Servlet, String> m_servlets;
    /**
     * The Http Service in use.
     */
    private HttpService m_httpService;

    /**
     * Creates a new servlet registry.
     *
     * @param httpService Http service to use
     */
    public ServletRegistry( final HttpService httpService )
    {
        if ( httpService == null )
        {
            throw new IllegalArgumentException( "http Service cannot be null" );
        }
        m_servlets = new HashMap<Servlet, String>();
        m_httpService = httpService;
    }

    /**
     * Registers a servlet with http service, using the provided alias.
     * The HttpContext will be the default HttpContext.
     *
     * @param alias   servlet alias
     * @param servlet servlet to register
     *
     * @throws Exception re-thrown from HttpService
     */
    void register( final String alias, final Servlet servlet )
        throws Exception
    {
        // no checks are required as Http Service should do all of the checks
        m_httpService.registerServlet( alias, servlet, (Dictionary) null, (HttpContext) null );
        synchronized ( m_servlets )
        {
            m_servlets.put( servlet, alias );
        }
    }

    /**
     * Unregisters a servlet from the Http Service if the servlet was registered before.
     *
     * @param servlet the servlet to unregister
     */
    public void unregister( final Servlet servlet )
    {
        String alias;
        synchronized ( m_servlets )
        {
            alias = m_servlets.get( servlet );
            // if we do not have the servlet then we do not unregister it
            if ( alias == null )
            {
                return;
            }
            m_servlets.remove( servlet );
        }
        m_httpService.unregister( alias );
    }

    /**
     * Unregisters all registered servlets.
     */
    void unregisterAll()
    {
        for ( Servlet servlet : m_servlets.keySet() )
        {
            unregister( servlet );
        }
    }

}