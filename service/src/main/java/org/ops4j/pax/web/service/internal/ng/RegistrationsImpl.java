package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegistrationsImpl implements Registrations
{

    private static final Log m_logger = LogFactory.getLog( RegistrationsImpl.class );

    private Map<String, HttpTarget> m_registrations;

    public RegistrationsImpl()
    {
        m_registrations = new HashMap<String, HttpTarget>();
    }
    
    public HttpTarget[] get()
    {
        Collection<HttpTarget> targets = m_registrations.values();
        return targets.toArray( new HttpTarget[targets.size()] );
    }

    public HttpTarget registerServlet( final String alias, final Servlet servlet, final Dictionary initParams, final HttpContext context )
        throws NamespaceException
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Registering Servlet: [" + alias + "] -> " + servlet + " into repository " + this );
        }
        validateRegisterServletArguments( alias, servlet );
        HttpTarget httpTarget = new HttpServlet( alias, servlet, initParams, context );
        m_registrations.put( httpTarget.getAlias(), httpTarget );
        return httpTarget;
    }

    public HttpTarget registerResources( final String alias, final String name, final HttpContext context )
        throws NamespaceException
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Registering Resource: [" + alias + "] -> " + name + " into repository " + this );
        }
        validateRegisterResourcesArguments( alias, name );
        HttpTarget httpTarget = new HttpResource( alias, name, context );
        m_registrations.put( httpTarget.getAlias(), httpTarget );
        return httpTarget;
    }

    public void unregister( final HttpTarget httpTarget )
    {
        Assert.notNull( "httpTarget == null", httpTarget );
        if (m_registrations.remove( httpTarget.getAlias() ) == null )
        {
            throw new IllegalArgumentException( "httpTarget was not registered before" );
        }
    }

    public HttpTarget getByAlias( String alias )
    {
        return m_registrations.get( alias );
    }

    private void validateRegisterServletArguments( String alias, Servlet servlet )
        throws NamespaceException
        {
        validateAlias( alias );
        if( servlet == null )
        {
            throw new IllegalArgumentException( "servlet == null" );
        }
    }

    private void validateRegisterResourcesArguments( String alias, String name )
        throws NamespaceException
    {
        validateAlias( alias );
        if( name == null )
        {
            throw new IllegalArgumentException( "name == null" );
        }
        if( name.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "name ends with slash (/)" );
        }
    }    

    private void validateAlias( String alias )
        throws NamespaceException
    {
        if( alias == null )
        {
            throw new IllegalArgumentException( "alias == null" );
        }
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias ends with slash (/)" );
        }
        // check for duplicate registration
        if( m_registrations.containsKey( alias ) )
        {
            throw new NamespaceException( "alias is already in use" );
        }
    }

    // TODO handle invalid params on registration (nulls, ...)
    // TODO do not allow duplicate alias registration  within the whole service
    // TODO do not allow duplicate servlet registration within the whole service
}
