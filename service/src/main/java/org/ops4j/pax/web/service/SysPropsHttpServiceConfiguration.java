package org.ops4j.pax.web.service;

public class SysPropsHttpServiceConfiguration
    implements HttpServiceConfiguration
{

    private final static int DEFAULT_HTTP_PORT = 80;
    private final static int DEFAULT_HTTP_SECURE_PORT = 443;

    private int m_httpPort;
    private int m_httpSecurePort;

    private boolean m_httpEnabled = true;
    private boolean m_httpSecureEnabled = true;

    public SysPropsHttpServiceConfiguration()
    {
        m_httpPort = Integer.getInteger( "org.osgi.service.http.port", DEFAULT_HTTP_PORT);
        m_httpSecurePort = Integer.getInteger( "org.osgi.service.http.port.secure", DEFAULT_HTTP_SECURE_PORT);
    }

    public int getHttpPort()
    {
        return m_httpPort;
    }

    public boolean isHttpEnabled()
    {
        return m_httpEnabled;
    }

    public int getHttpSecurePort()
    {
        return m_httpSecurePort;
    }

    public boolean isHttpSecureEnabled()
    {
        return m_httpSecureEnabled;
    }

}
