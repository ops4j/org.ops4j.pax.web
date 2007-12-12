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

import java.util.HashSet;
import java.util.Set;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;

public class RegistrationsClusterImpl implements RegistrationsCluster
{

    private Set<Registrations> m_registrations = new HashSet<Registrations>();

    public void remove( final Registrations registrations )
    {
        m_registrations.remove( registrations );
    }

    public Registration getByAlias( final String alias )
    {
        for( Registrations registrations : m_registrations )
        {
            Registration registration = registrations.getByAlias( alias );
            if( registration != null )
            {
                return registration;
            }
        }
        return null;
    }

    public Registrations create( HttpContext httpContext )
    {
        Registrations registrations = new RegistrationsImpl( this, httpContext );
        m_registrations.add( registrations );
        return registrations;
    }

    public boolean containsServlet( final Servlet servlet )
    {
        for( Registrations registrations : m_registrations )
        {
            if( registrations.containsServlet( servlet ) )
            {
                return true;
            }
        }
        return false;
    }

}
