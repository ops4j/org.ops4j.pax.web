package org.ops4j.pax.web.service.internal.model;

import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.service.internal.Registrations;

public class ContextModel extends Identity
{

    protected final HttpContext m_httpContext;
    protected final Registrations m_registrations;

    public ContextModel( final HttpContext httpContext, final Registrations registrations )
    {
        m_registrations = registrations;
        m_httpContext = httpContext;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Registrations getRegistrations()
    {
        return m_registrations;
    }

}
