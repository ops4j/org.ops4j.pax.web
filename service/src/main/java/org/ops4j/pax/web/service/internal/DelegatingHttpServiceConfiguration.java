package org.ops4j.pax.web.service.internal;

import java.io.File;
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

    /**
     * @see HttpServiceConfiguration#getHttpPort()  
     */
    @Override
    public int getHttpPort()
    {
        if( m_httpServiceConfiguration != null && m_httpPort == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "http port not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getHttpPort();
        }
        return super.getHttpPort();
    }

    /**
     * @see HttpServiceConfiguration#getHttpSecurePort()
     */
    @Override
    public int getHttpSecurePort()
    {
        if( m_httpServiceConfiguration != null && m_httpSecurePort == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "http secure port not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getHttpSecurePort();
        }
        return super.getHttpSecurePort();
    }

    /**
     * @see HttpServiceConfiguration#isHttpEnabled()
     */
    @Override
    public boolean isHttpEnabled()
    {
        if( m_httpServiceConfiguration != null && m_httpEnabled == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "http enabled not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.isHttpEnabled();
        }
        return super.isHttpEnabled();
    }

    /**
     * @see HttpServiceConfiguration#isHttpSecureEnabled()
     */
    @Override
    public boolean isHttpSecureEnabled()
    {
        if( m_httpServiceConfiguration != null && m_httpSecureEnabled == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "http secure enabled not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.isHttpSecureEnabled();
        }
        return super.isHttpSecureEnabled();
    }

    /**
     * @see HttpServiceConfiguration#getSslKeystore()
     */
    @Override
    public String getSslKeystore()
    {
        if( m_httpServiceConfiguration != null && m_sslKeystore == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "ssl keystore not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getSslKeystore();
        }
        return super.getSslKeystore();
    }

    /**
     * @see HttpServiceConfiguration#getSslPassword()
     */
    @Override
    public String getSslPassword()
    {
        if( m_httpServiceConfiguration != null && m_sslPassword == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "ssl password not set. fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getSslPassword();
        }
        return super.getSslPassword();
    }

    /**
     * @see HttpServiceConfiguration#getSslKeyPassword()
     */
    @Override
    public String getSslKeyPassword()
    {
        if( m_httpServiceConfiguration != null && m_sslKeyPassword == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "ssl keystore password not set. Fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getSslKeyPassword();
        }
        return super.getSslKeyPassword();
    }

    /**
     * @see HttpServiceConfiguration#getTemporaryDirectory()
     */
    @Override
    public File getTemporaryDirectory()
    {
        if( m_httpServiceConfiguration != null && m_temporaryDirectory == null )
        {
            if( m_logger.isDebugEnabled() )
            {
                m_logger.debug( "temporary directory not set. Fallback to " + m_httpServiceConfiguration );
            }
            return m_httpServiceConfiguration.getTemporaryDirectory();
        }
        return super.getTemporaryDirectory();
    }

}
