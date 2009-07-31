/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.net.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.lang.NullArgumentException;
import static org.ops4j.pax.web.service.WebContainerConstants.*;
import org.ops4j.pax.web.service.spi.Configuration;
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;

/**
 * Service Configuration implementation.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 22, 2008
 */
public class ConfigurationImpl extends PropertyStore
    implements Configuration
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ConfigurationImpl.class );

    /**
     * Property resolver. Cannot be null.
     */
    private final PropertyResolver m_propertyResolver;

    /**
     * Creates a new service configuration.
     *
     * @param propertyResolver propertyResolver used to resolve properties; mandatory
     */
    public ConfigurationImpl( final PropertyResolver propertyResolver )
    {
        NullArgumentException.validateNotNull( propertyResolver, "Property resolver" );
        m_propertyResolver = propertyResolver;
    }

    /**
     * @see Configuration#getHttpPort()
     */
    public Integer getHttpPort()
    {
        return getResolvedIntegerProperty( PROPERTY_HTTP_PORT );
    }

    /**
     * @see Configuration#useNIO()
     */
    public Boolean useNIO()
    {
        return getResolvedBooleanProperty( PROPERTY_HTTP_USE_NIO );
    }

    /**
     * @see Configuration#isClientAuthNeeded()
     */
    public Boolean isClientAuthNeeded()
    {
        return getResolvedBooleanProperty( PROPERTY_SSL_CLIENT_AUTH_NEEDED );
    }

    /**
     * @see Configuration#isClientAuthWanted()
     */
    public Boolean isClientAuthWanted()
    {
        return getResolvedBooleanProperty( PROPERTY_SSL_CLIENT_AUTH_WANTED );
    }

    /**
     * @see Configuration#isHttpEnabled()
     */
    public Boolean isHttpEnabled()
    {
        return getResolvedBooleanProperty( PROPERTY_HTTP_ENABLED );
    }

    /**
     * @see Configuration#getHttpSecurePort()
     */
    public Integer getHttpSecurePort()
    {
        return getResolvedIntegerProperty( PROPERTY_HTTP_SECURE_PORT );
    }

    /**
     * @see Configuration#isHttpSecureEnabled()
     */
    public Boolean isHttpSecureEnabled()
    {
        return getResolvedBooleanProperty( PROPERTY_HTTP_SECURE_ENABLED );
    }

    /**
     * @see Configuration#getSslKeystore()
     */
    public String getSslKeystore()
    {
        return getResolvedStringProperty( PROPERTY_SSL_KEYSTORE );
    }

    /**
     * @see Configuration#getSslKeystoreType()
     */
    public String getSslKeystoreType()
    {
        return getResolvedStringProperty( PROPERTY_SSL_KEYSTORE_TYPE );
    }

    /**
     * @see Configuration#getSslPassword()
     */
    public String getSslPassword()
    {
        return getResolvedStringProperty( PROPERTY_SSL_PASSWORD );
    }

    /**
     * @see Configuration#getSslKeyPassword()
     */
    public String getSslKeyPassword()
    {
        return getResolvedStringProperty( PROPERTY_SSL_KEYPASSWORD );
    }

    /**
     * @see Configuration#getTemporaryDirectory()
     */
    public File getTemporaryDirectory()
    {
        try
        {
            if( !contains( PROPERTY_TEMP_DIR ) )
            {
                final String tempDirPath = m_propertyResolver.get( PROPERTY_TEMP_DIR );
                File tempDir;
                if( tempDirPath.startsWith( "file:" ) )
                {
                    tempDir = new File( new URI( tempDirPath ) );
                }
                else
                {
                    tempDir = new File( tempDirPath );
                }
                if( !tempDir.exists() )
                {
                    tempDir.mkdirs();
                }
                return set( PROPERTY_TEMP_DIR, tempDir );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_TEMP_DIR + " has failed" );
        }
        return get( PROPERTY_TEMP_DIR );
    }

    /**
     * @see Configuration#getSessionTimeout()
     */
    public Integer getSessionTimeout()
    {
        return getResolvedIntegerProperty( PROPERTY_SESSION_TIMEOUT );

    }

    public String getSessionCookie()
    {
        return getResolvedStringProperty( PROPERTY_SESSION_COOKIE );
    }

    public String getSessionUrl()
    {
        return getResolvedStringProperty( PROPERTY_SESSION_URL );
    }

    public String getWorkerName()
    {
        return getResolvedStringProperty( PROPERTY_WORKER_NAME );
    }

    /**
     * @see Configuration#getListeningAddresses()
     */
    public String[] getListeningAddresses()
    {
        try
        {
            if( !contains( PROPERTY_LISTENING_ADDRESSES ) )
            {
                String interfacesString = m_propertyResolver.get( PROPERTY_LISTENING_ADDRESSES );
                String[] interfaces = interfacesString == null ? new String[0] : interfacesString.split( "," );
                return set( PROPERTY_LISTENING_ADDRESSES, interfaces );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_LISTENING_ADDRESSES + " has failed" );
        }
        return get( PROPERTY_LISTENING_ADDRESSES );
    }

    @Override
    public String toString()
    {
        return new StringBuilder().append( this.getClass().getSimpleName() ).append( "{" ).append( "http enabled=" )
            .append( isHttpEnabled() ).append( ",http port=" ).append( getHttpPort() ).append( ",http secure enabled=" )
            .append( isHttpSecureEnabled() ).append( ",http secure port=" ).append( getHttpSecurePort() ).append(
                ",ssl keystore="
            ).append( getSslKeystore() ).append( ",ssl keystoreType=" ).append(
                getSslKeystoreType()
            ).append( ",session timeout=" ).append( getSessionTimeout() ).append(
                ",session url="
            ).append( getSessionUrl() ).append( ",session cookie=" ).append( getSessionCookie() )
            .append( ",worker name=" ).append( getWorkerName() ).append( ",listening addresses=" ).append(
                getListeningAddresses()
            ).append( "}" ).toString();
    }

    private String getResolvedStringProperty( String property )
    {
        try
        {
            if( !contains( property ) )
            {
                return set( property, m_propertyResolver.get( property ) );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + property + " has failed" );
        }
        return get( property );
    }

    private Boolean getResolvedBooleanProperty( String property )
    {
        try
        {
            if( !contains( property ) )
            {
                return set( property, Boolean.valueOf( m_propertyResolver.get( property ) ) );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + property + " has failed" );
        }
        return get( property );
    }

    private Integer getResolvedIntegerProperty( String property )
    {
        try
        {
            if( !contains( property ) )
            {
                return set( property, Integer.valueOf( m_propertyResolver.get( property ) ) );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + property + " has failed" );
        }
        return get( property );
    }

}
