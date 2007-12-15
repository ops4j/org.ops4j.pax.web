package org.ops4j.pax.web.service.internal.model;

import javax.servlet.Filter;

public class FilterModel extends BasicModel
{

    private final Filter m_filter;
    private final String[] m_urlPatterns;
    private final String[] m_servletNames;

    public FilterModel( final Filter filter,
                               final String[] urlPatterns,
                               final String[] servletNames,
                               final ContextModel contextModel )
    {
        super( contextModel );

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

}
