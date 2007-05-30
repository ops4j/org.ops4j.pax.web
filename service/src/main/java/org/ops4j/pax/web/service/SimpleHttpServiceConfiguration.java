package org.ops4j.pax.web.service;

import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class SimpleHttpServiceConfiguration
    implements HttpServiceConfiguration
{
    private final static int DEFAULT_HTTP_PORT = 8080;
    private final static int DEFAULT_HTTP_SECURE_PORT = 8443;

    private int m_httpPort = DEFAULT_HTTP_PORT;
    private int m_httpSecurePort = DEFAULT_HTTP_SECURE_PORT;

    private boolean m_httpEnabled = true;
    private boolean m_httpSecureEnabled = false;

    public SimpleHttpServiceConfiguration()
    {
        
    }

    public SimpleHttpServiceConfiguration(final HttpServiceConfiguration configuration)
    {
        if ( configuration != null )
        {
            setHttpPort( configuration.getHttpPort() );
            setHttpSecurePort( configuration.getHttpSecurePort() );
            setHttpEnabled( configuration.isHttpEnabled() );
            setHttpSecureEnabled( configuration.isHttpSecureEnabled() );
        }
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

    public void setHttpPort( final int httpPort )
    {
        m_httpPort = httpPort;
    }

    public void setHttpSecurePort( final int httpSecurePort )
    {
        m_httpSecurePort = httpSecurePort;
    }

    public void setHttpEnabled( final boolean httpEnabled )
    {
        m_httpEnabled = httpEnabled;
    }

    public void setHttpSecureEnabled( final boolean httpSecureEnabled )
    {
        m_httpSecureEnabled = httpSecureEnabled;
    }    
}
