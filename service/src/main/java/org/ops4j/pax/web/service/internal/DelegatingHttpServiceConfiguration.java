package org.ops4j.pax.web.service.internal;

import org.ops4j.pax.web.service.HttpServiceConfiguration;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class DelegatingHttpServiceConfiguration extends AbstractHttpServiceConfiguration
{

    private final Log m_logger;

    private HttpServiceConfiguration m_httpServiceConfiguration;

    public DelegatingHttpServiceConfiguration( final HttpServiceConfiguration httpServiceConfiguration )
    {
        m_httpServiceConfiguration = httpServiceConfiguration;
        m_logger = LogFactory.getLog( this.getClass() );
    }

    public int getHttpPort()
    {
        if( m_httpServiceConfiguration != null && m_httpPort == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "http port not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.getHttpPort();
        }
        return super.getHttpPort();
    }

    public int getHttpSecurePort()
    {
        if( m_httpServiceConfiguration != null && m_httpSecurePort == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "http secure port not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.getHttpSecurePort();
        }
        return super.getHttpSecurePort();
    }

    public boolean isHttpEnabled()
    {
        if( m_httpServiceConfiguration != null && m_httpEnabled == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "http enabled not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.isHttpEnabled();
        }
        return super.isHttpEnabled();
    }

    public boolean isHttpSecureEnabled()
    {
        if( m_httpServiceConfiguration != null && m_httpSecureEnabled == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "http secure enabled not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.isHttpSecureEnabled();
        }
        return super.isHttpSecureEnabled();
    }

}
