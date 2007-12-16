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

import java.util.Dictionary;
import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class RegistrationsImplTest
{

    private RegistrationsImpl m_underTest;
    private Registration m_registration;
    private Servlet m_servlet;
    private HttpContext m_context;
    private Dictionary m_initParams;
    private RegistrationsCluster m_registrationsCluster;

    @Before
    public void setUp()
    {
        m_registration = createMock( Registration.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_initParams = new Hashtable();
        m_registrationsCluster = new RegistrationsClusterImpl();
        m_underTest = new RegistrationsImpl( m_registrationsCluster, m_context );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullRegistrationCluster()
    {
        new RegistrationsImpl( null, m_context );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullContext()
    {
        new RegistrationsImpl( m_registrationsCluster, null );
    }

    @Test
    public void getAfterServletRegistration()
        throws NamespaceException, ServletException
    {
        // execute
        Registration registered = m_underTest.registerServlet( "/alias", m_servlet, m_initParams );
        assertNotNull( "must return a valid http servlet", registered );
        Registration[] registrations = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", registrations );
        assertEquals( "expected just one model", 1, registrations.length );
        for( Registration registration : registrations )
        {
            assertEquals( "/alias", registration.getAlias() );
        }
    }

    @Test
    public void getAfterResourceRegistration()
        throws NamespaceException
    {
        // execute
        Registration registered = m_underTest.registerResources( "/alias", "/name" );
        assertNotNull( "must return a valid http resource", registered );
        Registration[] registrations = m_underTest.get();
        // verify
        assertNotNull( "registrations cannot be null", registrations );
        assertEquals( "expected just one model", 1, registrations.length );
        for( Registration registration : registrations )
        {
            assertEquals( "/alias", registration.getAlias() );
        }
    }

    @Test
    public void getWithNoRegsitration()
    {
        Registration[] targets = m_underTest.get();
        assertNotNull( "targets cannot be null", targets );
        assertEquals( "targets size", 0, targets.length );
    }

    @Test
    public void unregisterFlow()
        throws NamespaceException, ServletException
    {
        m_underTest.unregister( m_underTest.registerServlet( "/alias", m_servlet, m_initParams ) );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterOfUnregisteredTarget()
    {
        // prepare
        expect( m_registration.getAlias() ).andReturn( "/alias" );
        replay( m_registration );
        // execute
        m_underTest.unregister( m_registration );
        // verify
        verify( m_registration );
    }

    @Test( expected = IllegalArgumentException.class )
    public void unregisterOfNullTarget()
    {
        // execute
        m_underTest.unregister( null );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            null,
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullServlet()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "/test",
            null,
            new Hashtable()
        );
    }

    @Test
    public void registerServletWithNullInitParams()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
            "/test",
            m_servlet,
            null
        );
    }

    @Test
    public void registerServletWithOnlySlashInAlias()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
            "/",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithEndSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "/test/",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "test",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashAndWithEndingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "test/",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
            null,
            "resources"
        );
    }

    @Test
    public void registerResourcesWithOnlySlashInAlias()
        throws NamespaceException
    {
        // must be allowed
        m_underTest.registerResources(
            "/",
            "resources"
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
            "/malformed/",
            "resources"
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithoutStartingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
            "malformed",
            "resources"
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithhoutStartingSlashAndWthEndingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
            "malformed/",
            "resources"
        );
    }

    @Test( expected = NamespaceException.class )
    public void registerServletWithDuplicateAliasWithinTheSameRegistrations()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "/test",
            m_servlet,
            new Hashtable()
        );
        m_underTest.registerServlet(
            "/test",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = ServletException.class )
    public void registerSameServletForDifferentAliasesWithinTheSameRegistrations()
        throws NamespaceException, ServletException
    {
        //execute
        m_underTest.registerServlet(
            "/alias1",
            m_servlet,
            new Hashtable()
        );
        m_underTest.registerServlet(
            "/alias2",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = ServletException.class )
    public void registerSameServletForDifferentAliasesWithinDifferentRegistrations()
        throws NamespaceException, ServletException
    {
        //execute
        m_underTest.registerServlet(
            "/alias1",
            m_servlet,
            new Hashtable()
        );
        m_underTest.registerServlet(
            "/alias2",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = NamespaceException.class )
    public void registerServletWithDuplicateAliasWithinDifferentRegistrations()
        throws NamespaceException, ServletException
    {
        // execute
        new RegistrationsImpl( m_registrationsCluster, m_context ).registerServlet(
            "/test",
            m_servlet,
            new Hashtable()
        );
        new RegistrationsImpl( m_registrationsCluster, m_context ).registerServlet(
            "/test",
            m_servlet,
            new Hashtable()
        );
    }

    @Test( expected = NamespaceException.class )
    public void registerResourcesWithDuplicateAliasWithinTheSameRegistrations()
        throws NamespaceException
    {
        m_underTest.registerResources(
            "/test",
            "resources"
        );
        m_underTest.registerResources(
            "/test",
            "resources"
        );
    }

    @Test( expected = NamespaceException.class )
    public void registerResourceWithDuplicateAliasWithinDifferentRegistrations()
        throws NamespaceException, ServletException
    {
        // execute
        new RegistrationsImpl( m_registrationsCluster, m_context ).registerResources(
            "/test",
            "/name"
        );
        new RegistrationsImpl( m_registrationsCluster, m_context ).registerResources(
            "/test",
            "/name"
        );
    }

}
