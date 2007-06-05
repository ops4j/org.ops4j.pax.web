package org.ops4j.pax.web.service.internal.ng;

import java.util.Set;
import java.util.HashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.Servlet;

public class RegistrationsClusterImpl implements RegistrationsCluster
{

    private static final Log m_logger = LogFactory.getLog( RegistrationsCluster.class );
    
    private Set<Registrations> m_repositories = new HashSet<Registrations>();

    public void remove( final Registrations registrations )
    {
        // TODO implement remove (called when a bundle releases the service)
    }

    public HttpTarget getByAlias( final String alias )
    {
        if ( m_logger.isInfoEnabled() ) {
            m_logger.info( "matching alias: [" + alias + "]" );
        }
        for ( Registrations registrations : m_repositories )
        {
            HttpTarget httpTarget = registrations.getByAlias( alias );
            if ( httpTarget != null )
            {
                if ( m_logger.isInfoEnabled() ) {
                    m_logger.info( "matched alias: [" + alias + "] -> " + httpTarget );
                }
                return httpTarget;
            }
        }
        if ( m_logger.isInfoEnabled() ) {
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
        for ( Registrations registrations : m_repositories )
        {
            if ( registrations.containsServlet( servlet) )
            {
                return true;
            }
        }
        return false;
    }
    
}
