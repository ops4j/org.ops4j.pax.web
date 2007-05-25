package org.ops4j.pax.service.internal;

import static org.easymock.EasyMock.createMock;
import org.junit.Test;
import org.junit.Before;
import org.ops4j.pax.web.service.internal.HttpServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: alin.dreghiciu
 * Date: May 25, 2007
 * Time: 11:16:37 AM
 * To change this template use File | Settings | File Templates.
 */

public class HttpServiceImplTest {

    private Bundle bundle;
    private Servlet servlet;
    private HttpContext context;

    @Before
    public void setUp ()
    {
        bundle = createMock( Bundle.class );
        servlet = createMock( Servlet.class );
        context = createMock( HttpContext.class );
    }

    @Test
    public void constructorHappyPath()
    {
         new HttpServiceImpl( bundle );
    }

    @Test ( expected = IllegalArgumentException.class )
    public void constructorWithNullBundle()
    {
         new HttpServiceImpl( null );
    }

    @Test
    public void registerServletHappyPath () throws NamespaceException, ServletException {
        new HttpServiceImpl( bundle )
                .registerServlet(
                        "/test",
                        servlet,
                        new Hashtable( ),
                        context
                );
    }

    @Test ( expected = IllegalArgumentException.class )
    public void registerServletWithNullAlias () throws NamespaceException, ServletException {
        new HttpServiceImpl( bundle )
                .registerServlet(
                        null,
                        servlet,
                        new Hashtable( ),
                        context
                );
    }

    @Test ( expected = IllegalArgumentException.class )
    public void registerServletWithNullServlet () throws NamespaceException, ServletException {
        new HttpServiceImpl( bundle )
                .registerServlet(
                        "/test",
                        null,
                        new Hashtable( ),
                        context
                );
    }

    @Test ( expected = IllegalArgumentException.class )
    public void registerServletWithNullInitParams () throws NamespaceException, ServletException {
        new HttpServiceImpl( bundle )
                .registerServlet(
                        "/test",
                        servlet,
                        null,
                        context
                );
    }

    @Test
    public void registerServletWithNullContext () throws NamespaceException, ServletException {
        // must be allowed
        new HttpServiceImpl( bundle )
                .registerServlet(
                        "/test",
                        servlet,
                        new Hashtable( ),
                        null
                );
    }

}
