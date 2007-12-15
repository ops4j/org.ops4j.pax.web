package org.ops4j.pax.web.service.internal;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import static org.easymock.EasyMock.*;
import org.junit.Test;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;

public class FilterTest extends IntegrationTests
{

    @Test
    public void filterIsCalledOnUrlPattern()
        throws NamespaceException, ServletException, IOException
    {
        Servlet servlet = createMock( Servlet.class );
        servlet.init( (ServletConfig) notNull() );
        servlet.destroy();

        Filter filter = createMock( Filter.class );
        filter.init( (FilterConfig) notNull() );
        filter.doFilter( (ServletRequest) notNull(), (ServletResponse) notNull(), (FilterChain) notNull() );
        filter.destroy();

        replay( servlet, filter );

        HttpContext context = m_httpService.createDefaultHttpContext();
        m_httpService.registerServlet( "/test", servlet, null, context );
        m_httpService.registerFilter( filter, new String[]{ "/*" }, null, context );

        HttpMethod method = new GetMethod( "http://localhost:8080/test" );
        m_client.executeMethod( method );
        method.releaseConnection();

        m_httpService.unregister( "/test" );
        m_httpService.unregisterFilter( filter );

        verify( servlet, filter );
    }

    @Test
    public void filterIsCalledOnServlet()
        throws NamespaceException, ServletException, IOException
    {
        Servlet servlet = createMock( Servlet.class );
        servlet.init( (ServletConfig) notNull() );
        servlet.destroy();

        Filter filter = createMock( Filter.class );
        filter.init( (FilterConfig) notNull() );
        filter.doFilter( (ServletRequest) notNull(), (ServletResponse) notNull(), (FilterChain) notNull() );
        filter.destroy();

        replay( servlet, filter );

        HttpContext context = m_httpService.createDefaultHttpContext();
        m_httpService.registerServlet( "/test", servlet, null, context );
        m_httpService.registerFilter( filter, null, new String[]{ "/test" }, context );

        HttpMethod method = new GetMethod( "http://localhost:8080/test" );
        m_client.executeMethod( method );
        method.releaseConnection();

        m_httpService.unregister( "/test" );
        m_httpService.unregisterFilter( filter );

        verify( servlet, filter );
    }


}