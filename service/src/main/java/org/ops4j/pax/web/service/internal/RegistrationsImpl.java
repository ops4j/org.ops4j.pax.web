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

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class RegistrationsImpl implements Registrations
{

    private static final Log m_logger = LogFactory.getLog( RegistrationsImpl.class );

    private Map<String, HttpTarget> m_registrations;
    private RegistrationsCluster m_registrationsCluster;
    private HashSet<Servlet> m_servlets;

    public RegistrationsImpl( final RegistrationsCluster registrationsCluster )
    {
        Assert.notNull( "registrationsCluster == null", registrationsCluster);
        m_registrationsCluster = registrationsCluster;
        m_registrations = new HashMap<String, HttpTarget>();
        m_servlets = new HashSet<Servlet>();
    }
    
    public HttpTarget[] get()
    {
        Collection<HttpTarget> targets = m_registrations.values();
        return targets.toArray( new HttpTarget[targets.size()] );
    }

    public HttpTarget registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams,
        final HttpContext context )
        throws NamespaceException, ServletException
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Registering Servlet: [" + alias + "] -> " + servlet + " into repository " + this );
        }
        validateRegisterServletArguments( alias, servlet );
        HttpTarget httpTarget = new HttpServlet( alias, servlet, initParams, context );
        m_registrations.put( httpTarget.getAlias(), httpTarget );
        m_servlets.add( servlet );
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
        if ( httpTarget instanceof HttpServlet )
        {
            m_servlets.remove( ((HttpServlet) httpTarget).getServlet() );
        }
    }

    public HttpTarget getByAlias( final String alias )
    {
        return m_registrations.get( alias );
    }

    public boolean containsServlet( final Servlet servlet )
    {
        return m_servlets.contains( servlet ); 
    }

    private void validateRegisterServletArguments( final String alias, final Servlet servlet )
        throws NamespaceException, ServletException
    {
        validateAlias( alias );
        Assert.notNull( "servlet == null", servlet );
        if ( containsServlet ( servlet )) {
            throw new ServletException("servlet already registered with a different alias");
        }
    }

    private void validateRegisterResourcesArguments( final String alias, final String name )
        throws NamespaceException
    {
        validateAlias( alias );
        Assert.notNull( "name == null", name );
        if( name.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "name ends with slash (/)" );
        }
    }    

    private void validateAlias( String alias )
        throws NamespaceException
    {
        Assert.notNull( "alias == null", alias );
        if( !alias.startsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias does not start with slash (/)" );
        }
        // "/" must be allowed
        if( alias.length() > 1 && alias.endsWith( "/" ) )
        {
            throw new IllegalArgumentException( "alias ends with slash (/)" );
        }
        // check for duplicate alias registration within registrations
        if( m_registrations.containsKey( alias ) )
        {
            throw new NamespaceException( "alias is already in use" );
        }
        // check for duplicate alias registration within all registrations
        HttpTarget httpTarget = m_registrationsCluster.getByAlias( alias );
        if ( httpTarget != null )
        {
             throw new NamespaceException( "alias is already in use" );
        }
    }

    // TODO do not allow duplicate servlet registration within the whole service
}
