package org.ops4j.pax.web.service.internal.model;

import java.util.Arrays;
import javax.servlet.Filter;
import org.osgi.service.http.HttpContext;

public class FilterModel
    extends Model
{

    private final Filter m_filter;
    private final String[] m_urlPatterns;
    private final String[] m_servletNames;

    public FilterModel( final HttpContext httpContext,
                        final Filter filter,
                        final String[] urlPatterns,
                        final String[] servletNames )
    {
        super( httpContext );

        if( urlPatterns == null && servletNames == null )
        {
            throw new IllegalArgumentException(
                "Registered filter must have at least one url pattern or servlet mapping"
            );
        }

        m_filter = filter;
        m_urlPatterns = urlPatterns;
        m_servletNames = servletNames;
    }

    public Filter getFilter()
    {
        return m_filter;
    }

    public String[] getUrlPatterns()
    {
        return m_urlPatterns;
    }

    public String[] getServletNames()
    {
        return m_servletNames;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "id=" ).append( m_id )
            .append( ",urlPatterns=" ).append( Arrays.toString( m_urlPatterns ) )
            .append( ",servletNames=" ).append( Arrays.toString( m_servletNames ) )
            .append( ",filter=" ).append( m_filter )
            .append( ",httpContext=" ).append( m_httpContext )
            .append( "}" )
            .toString();
    }

}
