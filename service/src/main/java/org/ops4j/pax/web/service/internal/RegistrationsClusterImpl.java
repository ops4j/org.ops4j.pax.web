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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegistrationsClusterImpl implements RegistrationsCluster
{

    private static final Log m_logger = LogFactory.getLog( RegistrationsCluster.class );

    private Set<Registrations> m_repositories = new HashSet<Registrations>();

    public void remove( final Registrations registrations )
    {
        // TODO implement remove (called when a bundle releases the service)
    }

    public Registration getByAlias( final String alias )
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "matching alias: [" + alias + "]" );
        }
        for( Registrations registrations : m_repositories )
        {
            Registration registration = registrations.getByAlias( alias );
            if( registration != null )
            {
                if( m_logger.isInfoEnabled() )
                {
                    m_logger.info( "matched alias: [" + alias + "] -> " + registration );
                }
                return registration;
            }
        }
        if( m_logger.isInfoEnabled() )
        {
            m_logger.debug( "alias: [" + alias + "] not matched" );
        }
        return null;
    }

    public Registrations create()
    {
        Registrations registrations = new RegistrationsImpl( this );
        m_repositories.add( registrations );
        return registrations;
    }

    public boolean containsServlet( final Servlet servlet )
    {
        for( Registrations registrations : m_repositories )
        {
            if( registrations.containsServlet( servlet ) )
            {
                return true;
            }
        }
        return false;
    }

}
