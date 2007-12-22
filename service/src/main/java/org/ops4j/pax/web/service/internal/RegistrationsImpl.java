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
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class RegistrationsImpl implements Registrations
{

    private static final Log LOG = LogFactory.getLog( RegistrationsImpl.class );

    private final Map<String, Registration> m_registrations;
    private final RegistrationsCluster m_registrationsCluster;
    private final HttpContext m_httpContext;

    public RegistrationsImpl( final RegistrationsCluster registrationsCluster, final HttpContext httpContext )
    {
        Assert.notNull( "registrationsCluster == null", registrationsCluster );
        Assert.notNull( "httpContext == null", httpContext );
        m_registrationsCluster = registrationsCluster;
        m_httpContext = httpContext;
        m_registrations = new HashMap<String, Registration>();
    }

    public Registration[] get()
    {
        synchronized( m_registrations )
        {
            Collection<Registration> targets = m_registrations.values();
            return targets.toArray( new Registration[targets.size()] );
        }
    }

    public Registration registerServlet(
        final String alias,
        final Servlet servlet,
        final Dictionary initParams )
        throws NamespaceException, ServletException
    {
        if( LOG.isDebugEnabled() )
        {
            LOG.debug( "Registering servlet: [" + alias + "] -> " + servlet + " into repository " + this );
        }
        synchronized( m_registrations )
        {
            validateRegisterServletArguments( alias, servlet );
            final Registration registration = new RegistrationImpl( alias, servlet, initParams, m_httpContext );
            m_registrations.put( registration.getAlias(), registration );
            m_registrationsCluster.addRegistration( registration );
            return registration;
        }
    }

    public Registration registerResources( final String alias, final String name )
        throws NamespaceException
    {
        if( LOG.isDebugEnabled() )
        {
            LOG.debug( "Registering resource: [" + alias + "] -> " + name + " into repository " + this );
        }
        synchronized( m_registrations )
        {
            validateRegisterResourcesArguments( alias, name );
            final ResourceServlet servlet = new ResourceServlet();
            final Registration registration = new RegistrationImpl( alias, name, servlet, m_httpContext );
            servlet.setRegistration( registration );
            m_registrations.put( registration.getAlias(), registration );
            m_registrationsCluster.addRegistration( registration );
            return registration;
        }
    }

    public void unregister( final Registration registration )
    {
        Assert.notNull( "model == null", registration );
        synchronized( m_registrations )
        {
            if( m_registrations.remove( registration.getAlias() ) == null )
            {
                throw new IllegalArgumentException( "model was not registered before" );
            }
            m_registrationsCluster.removeRegistration( registration );
        }
    }

    public Registration getByAlias( final String alias )
    {
        LOG.debug( "Matching alias: [" + alias + "] in http context [" + m_httpContext + "]" );
        final Registration registration;
        synchronized( m_registrations )
        {
            registration = m_registrations.get( alias );
        }
        if( registration != null )
        {
            LOG.debug( "matched alias: [" + alias + "] -> " + registration );
        }
        else
        {
            LOG.debug( "alias: [" + alias + "] not matched" );
        }
        return registration;
    }

    public void unregisterAll()
    {
        synchronized( m_registrations )
        {
            for( Map.Entry<String, Registration> entry : m_registrations.entrySet() )
            {
                m_registrationsCluster.removeRegistration( entry.getValue() );
            }
            m_registrations.clear();
        }
    }

    private void validateRegisterServletArguments( final String alias, final Servlet servlet )
        throws NamespaceException, ServletException
    {
        validateAlias( alias );
        Assert.notNull( "servlet == null", servlet );
        // check for duplicate servlet registration on any contexts
        if( m_registrationsCluster.containsServlet( servlet ) )
        {
            throw new ServletException( "servlet already registered with a different alias" );
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
        // check for duplicate alias model within registrations
        if( m_registrations.containsKey( alias ) )
        {
            throw new NamespaceException( "alias is already in use" );
        }
        // check for duplicate alias model within all registrations
        if( m_registrationsCluster.containsAlias( alias ) )
        {
            throw new NamespaceException( "alias is already in use in another context" );
        }
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "registrations=" ).append( m_registrations )
            .append( "}" )
            .toString();
    }

}
