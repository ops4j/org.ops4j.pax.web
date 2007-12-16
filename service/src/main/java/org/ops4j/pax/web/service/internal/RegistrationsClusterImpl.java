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
import org.osgi.service.http.HttpContext;

public class RegistrationsClusterImpl implements RegistrationsCluster
{

    private Map<String, Registration> m_aliases;
    private Set<Servlet> m_servlets;

    public RegistrationsClusterImpl()
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

}
