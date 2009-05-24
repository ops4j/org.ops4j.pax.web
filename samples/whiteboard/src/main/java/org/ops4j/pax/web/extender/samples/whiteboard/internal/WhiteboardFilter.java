package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;
import java.util.Date;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WhiteboardFilter
    implements Filter
{

    private static final Log LOG = LogFactory.getLog( WhiteboardFilter.class );

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        LOG.info( "Initialized" );
    }

    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        response.getWriter().println( "Filter was there before. Time: " + new Date().toString() );
        chain.doFilter( request, response );
        response.getWriter().println( "Filter was there after. Time: " + new Date().toString() );
    }

    public void destroy()
    {
        LOG.info( "Destroyed" );
    }
}
