/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.service.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.ops4j.pax.web.service.WebContainerConstants.*;
import org.ops4j.util.property.DictionaryPropertyResolver;

/**
 * Default property resolver.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0 January 23, 2008
 */
class DefaultPropertyResolver
    extends DictionaryPropertyResolver
{

    private static final Log LOG = LogFactory.getLog( DefaultPropertyResolver.class );

    public DefaultPropertyResolver()
    {
        super( getDefaltProperties() );
    }

    private static Dictionary getDefaltProperties()
    {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put( PROPERTY_HTTP_PORT, "8080" );
        properties.put( PROPERTY_HTTP_ENABLED, Boolean.TRUE.toString() );
        properties.put( PROPERTY_HTTP_SECURE_PORT, "8443" );
        properties.put( PROPERTY_HTTP_SECURE_ENABLED, Boolean.FALSE.toString() );
        properties.put( PROPERTY_SSL_KEYSTORE, System.getProperty( "user.home" ) + File.separator + ".keystore" );
        //properties.put( PROPERTY_SSL_PASSWORD, null );
        //properties.put( PROPERTY_SSL_KEYPASSWORD, null );
        // create a temporary directory
        try
        {
            File temporaryDirectory = File.createTempFile( ".paxweb", "" );
            temporaryDirectory.delete();
            temporaryDirectory = new File( temporaryDirectory.getAbsolutePath() );
            temporaryDirectory.mkdirs();
            temporaryDirectory.deleteOnExit();
            properties.put( PROPERTY_TEMP_DIR, temporaryDirectory.getCanonicalPath() );
        }
        catch( Exception e )
        {
            LOG.warn( "Could not create temporary directory. Reason: " + e.getMessage() );
            //properties.put( PROPERTY_TEMP_DIR, null );
        }
        //properties.put( PROPERTY_SESSION_TIMEOUT, null ); // no timeout

        return properties; 
    }

}