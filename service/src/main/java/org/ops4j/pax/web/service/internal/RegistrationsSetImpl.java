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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;

public class RegistrationsSetImpl implements RegistrationsSet
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( RegistrationsSetImpl.class );

    private final Map<String, Registration> m_aliases;
    private final Set<Servlet> m_servlets;

    public RegistrationsSetImpl()
    {
        m_aliases = new HashMap<String, Registration>();
        m_servlets = new HashSet<Servlet>();
    }

    public Registrations createRegistrations( HttpContext httpContext )
    {
        return new RegistrationsImpl( this, httpContext );
    }

    public synchronized Registration getByAlias( final String alias )
    {
        return m_aliases.get( alias );
    }

    public synchronized boolean containsAlias( final String alias )
    {
        return m_aliases.containsKey( alias );
    }

    public synchronized boolean containsServlet( final Servlet servlet )
    {
        return m_servlets.contains( servlet );
    }

    public synchronized void addRegistration( final Registration registration )
    {
        m_aliases.put( registration.getAlias(), registration );
        m_servlets.add( registration.getServlet() );
    }

    public synchronized void removeRegistration( final Registration registration )
    {
        m_aliases.remove( registration.getAlias() );
        m_servlets.remove( registration.getServlet() );
    }

    public Registration getMatchingAlias( final String alias )
    {
        final boolean debug = LOG.isDebugEnabled();
        if( debug )
        {
            LOG.debug( "Matching [" + alias + "]..." );
        }
        Registration matched = m_aliases.get( alias );
        if( matched == null && !"/".equals( alias.trim() ) )
        {
            // next, try for a substring by removing the last "/" and everything to the right of the last "/"
            String substring = alias.substring( 0, alias.lastIndexOf( "/" ) ).trim();
            if( substring.length() > 0 )
            {
                matched = getMatchingAlias( substring );
            }
            else
            {
                matched = getMatchingAlias( "/" );
            }
        }
        else if( debug )
        {
            LOG.debug( "Alias [" + alias + "] matched to " + matched );
        }
        return matched;
    }

}
