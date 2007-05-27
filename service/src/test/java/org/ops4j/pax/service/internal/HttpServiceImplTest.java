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
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImplTest
{

    private Bundle bundle;
    private Servlet servlet;
    private HttpContext context;
    private HttpServiceImpl m_serviceUnderTest;

    @Before
    public void setUp()
        throws ServletException
    {
        bundle = createMock( Bundle.class );
        servlet = createMock( Servlet.class );
        context = createMock( HttpContext.class );
        m_serviceUnderTest = new HttpServiceImpl( bundle );
    }

    @Test( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
        throws ServletException
    {
        new HttpServiceImpl( null );
    }

    @Test
    public void registerServletHappyPath()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest.registerServlet(
                "/test",
                servlet,
                new Hashtable(),
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullAlias()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest.registerServlet(
                null,
                servlet,
                new Hashtable(),
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithNullServlet()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest.registerServlet(
                "/test",
                null,
                new Hashtable(),
                context
            );
    }

    @Test
    public void registerServletWithNullInitParams()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_serviceUnderTest.registerServlet(
                "/test",
                servlet,
                null,
                context
            );
    }

    @Test
    public void registerServletWithNullContext()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_serviceUnderTest.registerServlet(
                "/test",
                servlet,
                new Hashtable(),
                null
            );
    }

    @Test
    public void registerServletWithOnlySlashInAlias()
        throws NamespaceException, ServletException
    {
        // must be allowed
        m_serviceUnderTest.registerServlet(
                "/",
                servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithEndSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest.registerServlet(
                "/test/",
                servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest.registerServlet(
                "test",
                servlet,
                new Hashtable(),
                null
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerServletWithoutStartingSlashAndWithEndingSlashInAlias()
        throws NamespaceException, ServletException
    {
        m_serviceUnderTest = new HttpServiceImpl( bundle );
        m_serviceUnderTest.registerServlet(
            "test/",
            servlet,
            new Hashtable(),
            null
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullAlias()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                null,
                "resources",
                context
            );
    }

    @Test
    public void registerResourcesWithOnlySlashInAlias()
        throws NamespaceException
    {
        // must be allowed
        m_serviceUnderTest.registerResources(
                "/",
                "resources",
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInAlias()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                "/malformed/",
                "resources",
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithoutStartingSlashInAlias()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                "malformed",
                "resources",
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithhoutStartingSlashAndWthEndingSlashInAlias()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                "malformed/",
                "resources",
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithNullName()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                "/",
                null,
                context
            );
    }

    @Test
    public void registerResourcesWithEmptyName()
        throws NamespaceException
    {
        // must be allowed ?
        m_serviceUnderTest.registerResources(
                "/",
                "",
                context
            );
    }

    @Test( expected = IllegalArgumentException.class )
    public void registerResourcesWithEndSlashInName()
        throws NamespaceException
    {
        m_serviceUnderTest.registerResources(
                "/",
                "resources/",
                context
            );
    }

    @Test
    public void registerResourcesWithNullContext()
        throws NamespaceException
    {
        // must be allowed
        m_serviceUnderTest.registerResources(
                "/",
                "resources",
                null
            );
    }

}
