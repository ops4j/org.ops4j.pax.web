package org.ops4j.pax.web.service.internal.ng;

import java.util.Set;
import java.util.HashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by IntelliJ IDEA.
 * User: alin.dreghiciu
 * Date: May 31, 2007
 * Time: 12:34:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegistrationsClusterImpl implements RegistrationsCluster
{

    private static final Log m_logger = LogFactory.getLog( RegistrationsCluster.class );
    
    private Set<Registrations> m_repositories = new HashSet<Registrations>();

    public void add( final Registrations registrations )
    {
        m_repositories.add( registrations );
    }

    public void remove( final Registrations registrations )
    {
        // TODO implement remove (called when a bundle releases the service)
    }

    public HttpTarget getByAlias( final String alias )
    {
        if ( m_logger.isInfoEnabled() ) {
            m_logger.info( "matching alias: [" + alias + "]" );
        }
        for ( Registrations repository : m_repositories )
        {
            HttpTarget httpTarget = repository.getByAlias( alias );
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
    
}
