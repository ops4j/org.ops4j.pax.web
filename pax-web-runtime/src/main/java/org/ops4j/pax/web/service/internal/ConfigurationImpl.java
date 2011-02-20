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
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.lang.NullArgumentException;
import static org.ops4j.pax.web.service.WebContainerConstants.*;

import org.ops4j.pax.web.service.internal.util.JspSupportUtils;
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
    private static final Logger LOG = LoggerFactory.getLogger( ConfigurationImpl.class );

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
            LOG.debug( "Reading configuration property " + PROPERTY_TEMP_DIR + " has failed" );
        }
        return get( PROPERTY_TEMP_DIR );
    }
    
	public File getConfigurationDir() {
		try
        {
            if( !contains( PROPERTY_SERVER_CONFIGURATION_FILE ) )
            {
                final String serverConfigurationFileName = m_propertyResolver.get( PROPERTY_SERVER_CONFIGURATION_FILE );
                File configurationFile;
                if( serverConfigurationFileName.startsWith( "file:" ) )
                {
                    configurationFile = new File( new URI( serverConfigurationFileName ) );
                }
                else
                {
                    configurationFile = new File( serverConfigurationFileName );
                }
                if( !configurationFile.exists() )
                {
					LOG.debug("Reading from configured path for the configuration property "
							+ PROPERTY_SERVER_CONFIGURATION_FILE + " has failed");
                }				
                return set( PROPERTY_SERVER_CONFIGURATION_FILE, configurationFile );
            }
        }
        catch( Exception ignore )
        {
            LOG.debug( "Reading configuration property " + PROPERTY_SERVER_CONFIGURATION_FILE + " has failed" );
        }
        return null;
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
            LOG.debug( "Reading configuration property " + PROPERTY_LISTENING_ADDRESSES + " has failed" );
        }
        return get( PROPERTY_LISTENING_ADDRESSES );
    }
    
	public String getJspScratchDir() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedStringProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_SCRATCH_DIR);
	}

	public Integer getJspCheckInterval() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedIntegerProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_CHECK_INTERVAL);
	}

	public Boolean getJspClassDebugInfo() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedBooleanProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEBUG_INFO);
	}

	public Boolean getJspDevelopment() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedBooleanProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_DEVELOPMENT);
	}

	public Boolean getJspEnablePooling() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedBooleanProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_ENABLE_POOLING);
	}

	public String getJspIeClassId() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedStringProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_IE_CLASS_ID);
	}

	public String getJspJavaEncoding() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedStringProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_JAVA_ENCODING);
	}

	public Boolean getJspKeepgenerated() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedBooleanProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_KEEP_GENERATED);
	}

	public String getJspLogVerbosityLevel() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedStringProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_LOG_VERBOSITY_LEVEL);
	}

	public Boolean getJspMappedfile() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedBooleanProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_MAPPED_FILE);
	}

	public Integer getJspTagpoolMaxSize() {
		//Just in case JSP is not available this parameter is useless
		if (!JspSupportUtils.jspSupportAvailable())
			return null;
		
		//Only when JSPs are available the constants can be read. 
        return getResolvedIntegerProperty(org.ops4j.pax.web.jsp.JspWebdefaults.PROPERTY_JSP_TAGPOOL_MAX_SIZE);
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
                Arrays.toString(getListeningAddresses())
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
            LOG.debug( "Reading configuration property " + property + " has failed" );
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
            LOG.debug( "Reading configuration property " + property + " has failed" );
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
            LOG.debug( "Reading configuration property " + property + " has failed" );
        }
        return get( property );
    }
}
