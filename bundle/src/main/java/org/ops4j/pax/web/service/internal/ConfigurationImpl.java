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
import org.ops4j.util.property.PropertyResolver;
import org.ops4j.util.property.PropertyStore;

/**
 * Service Configuration implementation.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 22, 2008
 */
public class ConfigurationImpl
    extends PropertyStore
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
        try
        {
            if( !contains( PROPERTY_HTTP_PORT ) )
            {
                return set( PROPERTY_HTTP_PORT,
                            Integer.valueOf( m_propertyResolver.get( PROPERTY_HTTP_PORT ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_HTTP_PORT + " has failed" );
        }
        return get( PROPERTY_HTTP_PORT );
    }
    /**
     * @see Configuration#useNIO()
     */
    public Boolean useNIO()
    {
        try
        {
            if( !contains( PROPERTY_HTTP_USE_NIO ) )
            {
                return set( PROPERTY_HTTP_USE_NIO,
                            Boolean.valueOf( m_propertyResolver.get( PROPERTY_HTTP_USE_NIO ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_HTTP_USE_NIO + " has failed" );
        }
        return get( PROPERTY_HTTP_USE_NIO );
    }
    /**
     * @see Configuration#isClientAuthNeeded()
     */
    public Boolean isClientAuthNeeded()
    {
        try
        {
            if( !contains( PROPERTY_SSL_CLIENT_AUTH_NEEDED ) )
            {
                return set( PROPERTY_SSL_CLIENT_AUTH_NEEDED,
                            Boolean.valueOf( m_propertyResolver.get( PROPERTY_SSL_CLIENT_AUTH_NEEDED ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_CLIENT_AUTH_NEEDED + " has failed" );
        }
        return get( PROPERTY_SSL_CLIENT_AUTH_NEEDED );
    }
    
    /**
     * @see Configuration#isClientAuthWanted()
     */
    public Boolean isClientAuthWanted()
    {
        try
        {
            if( !contains( PROPERTY_SSL_CLIENT_AUTH_WANTED ) )
            {
                return set( PROPERTY_SSL_CLIENT_AUTH_WANTED,
                            Boolean.valueOf( m_propertyResolver.get( PROPERTY_SSL_CLIENT_AUTH_WANTED ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_CLIENT_AUTH_WANTED + " has failed" );
        }
        return get( PROPERTY_SSL_CLIENT_AUTH_WANTED );
    } 
    
    /**
     * @see Configuration#isHttpEnabled()
     */
    public Boolean isHttpEnabled()
    {
        try
        {
            if( !contains( PROPERTY_HTTP_ENABLED ) )
            {
                return set( PROPERTY_HTTP_ENABLED,
                            Boolean.valueOf( m_propertyResolver.get( PROPERTY_HTTP_ENABLED ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_HTTP_ENABLED + " has failed" );
        }
        return get( PROPERTY_HTTP_ENABLED );
    }

    /**
     * @see Configuration#getHttpSecurePort()
     */
    public Integer getHttpSecurePort()
    {
        try
        {
            if( !contains( PROPERTY_HTTP_SECURE_PORT ) )
            {
                return set( PROPERTY_HTTP_SECURE_PORT,
                            Integer.valueOf( m_propertyResolver.get( PROPERTY_HTTP_SECURE_PORT ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_HTTP_SECURE_PORT + " has failed" );
        }
        return get( PROPERTY_HTTP_SECURE_PORT );
    }

    /**
     * @see Configuration#isHttpSecureEnabled()
     */
    public Boolean isHttpSecureEnabled()
    {
        try
        {
            if( !contains( PROPERTY_HTTP_SECURE_ENABLED ) )
            {
                return set( PROPERTY_HTTP_SECURE_ENABLED,
                            Boolean.valueOf(
                                m_propertyResolver.get( PROPERTY_HTTP_SECURE_ENABLED )
                            )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_HTTP_SECURE_ENABLED + " has failed" );
        }
        return get( PROPERTY_HTTP_SECURE_ENABLED );
    }

    /**
     * @see Configuration#getSslKeystore()
     */
    public String getSslKeystore()
    {
        try
        {
            if( !contains( PROPERTY_SSL_KEYSTORE ) )
            {
                return set( PROPERTY_SSL_KEYSTORE,
                            m_propertyResolver.get( PROPERTY_SSL_KEYSTORE )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_KEYSTORE + " has failed" );
        }
        return get( PROPERTY_SSL_KEYSTORE );
    }
    
    /**
     * @see Configuration#getSslKeystoreType()
     */
    public String getSslKeystoreType()
    {
        try
        {
            if( !contains( PROPERTY_SSL_KEYSTORE_TYPE ) )
            {
            	return set( PROPERTY_SSL_KEYSTORE_TYPE,
                		m_propertyResolver.get( PROPERTY_SSL_KEYSTORE_TYPE )                          
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_KEYSTORE_TYPE + " has failed" );
        }
        return get( PROPERTY_SSL_KEYSTORE_TYPE );
    }

    /**
     * @see Configuration#getSslPassword()
     */
    public String getSslPassword()
    {
        try
        {
            if( !contains( PROPERTY_SSL_PASSWORD ) )
            {
                return set( PROPERTY_SSL_PASSWORD,
                            m_propertyResolver.get( PROPERTY_SSL_PASSWORD )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_PASSWORD + " has failed" );
        }
        return get( PROPERTY_SSL_PASSWORD );
    }

    /**
     * @see Configuration#getSslKeyPassword()
     */
    public String getSslKeyPassword()
    {
        try
        {
            if( !contains( PROPERTY_SSL_KEYPASSWORD ) )
            {
                return set( PROPERTY_SSL_KEYPASSWORD,
                            m_propertyResolver.get( PROPERTY_SSL_KEYPASSWORD )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SSL_KEYPASSWORD + " has failed" );
        }
        return get( PROPERTY_SSL_KEYPASSWORD );
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
        try
        {
            if( !contains( PROPERTY_SESSION_TIMEOUT ) )
            {
                return set( PROPERTY_SESSION_TIMEOUT,
                            Integer.valueOf( m_propertyResolver.get( PROPERTY_SESSION_TIMEOUT ) )
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_SESSION_TIMEOUT + " has failed" );
        }
        return get( PROPERTY_SESSION_TIMEOUT );

    }

    /**
     * @see Configuration#getListeningAddresses()
     */
    public String[] getListeningAddresses()
    {
        try
        {
            if( !contains(PROPERTY_LISTENING_ADDRESSES) )
            {
                String interfacesString = m_propertyResolver.get(PROPERTY_LISTENING_ADDRESSES);
                String[] interfaces = interfacesString == null? new String[0] : interfacesString.split(",");
                return set(PROPERTY_LISTENING_ADDRESSES,
                            interfaces
                );
            }
        }
        catch( Exception ignore )
        {
            LOG.warn( "Reading configuration property " + PROPERTY_LISTENING_ADDRESSES + " has failed" );
        }
        return get(PROPERTY_LISTENING_ADDRESSES);
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "http enabled=" ).append( isHttpEnabled() )
            .append( ",http port=" ).append( getHttpPort() )
            .append( ",http secure enabled=" ).append( isHttpSecureEnabled() )
            .append( ",http secure port=" ).append( getHttpSecurePort() )
            .append( ",ssl keystore=" ).append( getSslKeystore() )
            .append( ",ssl keystoreType=" ).append( getSslKeystoreType() )
            .append( ",session timeout=" ).append( getSessionTimeout() )
            .append( ",listening addresses=" ).append( getListeningAddresses() )
            .append( "}" )
            .toString();
    }

}
