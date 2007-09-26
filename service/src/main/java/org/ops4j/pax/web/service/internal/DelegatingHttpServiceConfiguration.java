package org.ops4j.pax.web.service.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

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

    /**
     * @see HttpServiceConfiguration#getSslKeystore()
     */
    public String getSslKeystore()
    {
        if( m_httpServiceConfiguration != null && m_sslKeystore == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "ssl keystore not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.getSslKeystore();
        }
        return super.getSslKeystore();
    }

    /**
     * @see HttpServiceConfiguration#getSslPassword()
     */
    public String getSslPassword()
    {
        if( m_httpServiceConfiguration != null && m_sslPassword == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "ssl password not set. fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.getSslPassword();
        }
        return super.getSslPassword();
    }

    /**
     * @see HttpServiceConfiguration#getSslKeyPassword()
     */
    public String getSslKeyPassword()
    {
        if( m_httpServiceConfiguration != null && m_sslKeyPassword == null )
        {
            if( m_logger.isInfoEnabled() )
            {
                m_logger.info( "ssl keystore password not set. Fallback to " + m_httpServiceConfiguration.getClass() );
            }
            return m_httpServiceConfiguration.getSslKeyPassword();
        }
        return super.getSslKeyPassword();
    }

}
