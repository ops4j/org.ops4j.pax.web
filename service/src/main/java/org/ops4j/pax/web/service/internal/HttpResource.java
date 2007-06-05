package org.ops4j.pax.web.service.internal;

import org.osgi.service.http.HttpContext;

public class HttpResource implements HttpTarget
{
    private String m_alias;
    private String m_name;
    private HttpContext m_httpContext;

    public HttpResource( String alias, String name, HttpContext httpContext )
    {
        //To change body of created methods use File | Settings | File Templates.
        m_alias = alias;
        m_name = name;
        m_httpContext = httpContext;
    }

    public void register( final ServerController serverController )
    {
        // TODO implement register
    }

    public void unregister( ServerController serverController )
    {
        // TODO implement unregister
    }

    public String getAlias()
    {
        return m_alias;
    }

    public String getName()
    {
        return m_name;
    }

    public HttpContext getHttpContext()
    {
        return m_httpContext;
    }

    public Type getType()
    {
        return Type.RESOURCE;
    }
}
