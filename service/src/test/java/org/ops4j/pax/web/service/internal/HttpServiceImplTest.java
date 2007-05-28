/*  Copyright 2007 Alin Dreghiciu.
 *  Copyright 2007 Niclas Hedhman.
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
package org.ops4j.pax.service.internal;

import java.util.Hashtable;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import static org.easymock.EasyMock.createMock;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.web.service.internal.HttpServiceImpl;
import org.ops4j.pax.web.service.internal.OsgiHandler;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImplTest
{

    private Bundle m_bundle;
    private Servlet m_servlet;
    private HttpContext m_context;
    private OsgiHandler m_osgiHandler;
    private HttpServiceImpl m_underTest;

    @Before
    public void setUp()
        throws ServletException
    {
        m_bundle = createMock( Bundle.class );
        m_servlet = createMock( Servlet.class );
        m_context = createMock( HttpContext.class );
        m_osgiHandler = createMock( OsgiHandler.class );
        m_underTest = new HttpServiceImpl( m_bundle, m_osgiHandler);
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
        throws ServletException
    {
        new HttpServiceImpl( null, m_osgiHandler );
    }

    @Test
    public void registerServletHappyPath()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                null,
                m_servlet,
                new Hashtable(),
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullServlet()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test",
                null,
                new Hashtable(),
                m_context
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
                null,
                m_context
        );
    }

    @Test
    public void registerServletWithNullContext()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
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
                new Hashtable(),
                null
            );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerServletWithEndSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test/",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "test",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashAndWithEndingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
            "test/",
                m_servlet,
            new Hashtable(),
            null
        );
    }

    //@Test( expected = NamespaceException.class )
    public void registerServletWithDuplicateAlias()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
        m_underTest.registerServlet(
                "/test",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                null,
                "resources",
                m_context
        );
    }

    @Test
    public void registerResourcesWithOnlySlashInAlias()
        throws NamespaceException
    {
        // must be allowed
        m_underTest.registerResources(
                "/",
                "resources",
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/malformed/",
                "resources",
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithoutStartingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "malformed",
                "resources",
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithhoutStartingSlashAndWthEndingSlashInAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "malformed/",
                "resources",
                m_context
        );
    }

    //@Test( expected = NamespaceException.class )
    public void registerResourcesWithDuplicateAlias()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/test",
                "resources",
                m_context
        );
        m_underTest.registerResources(
                "/test",
                "resources",
                m_context
        );
    }

    // check 102.11.3.3 ServletException description
    //@Test( expected = ServletException.class )
    public void registerSameServletForDifferentAliases()
        throws NamespaceException, ServletException
    {
        m_underTest.registerServlet(
                "/alias1",
                m_servlet,
                new Hashtable(),
                null
            );
        m_underTest.registerServlet(
                "/alias2",
                m_servlet,
                new Hashtable(),
                null
            );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullName()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/",
                null,
                m_context
        );
    }

    @Test
    public void registerResourcesWithEmptyName()
        throws NamespaceException
    {
        // must be allowed ?
        m_underTest.registerResources(
                "/",
                "",
                m_context
        );
    }

    //@Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInName()
        throws NamespaceException
    {
        m_underTest.registerResources(
                "/",
                "resources/",
                m_context
        );
    }

    @Test
    public void registerResourcesWithNullContext()
        throws NamespaceException
    {
        // must be allowed
        m_underTest.registerResources(
                "/",
                "resources",
                null
            );
    }


}
