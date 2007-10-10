/* Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.service;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.web.service.internal.AbstractHttpServiceConfiguration;

public class DefaultHttpServiceConfiguration extends AbstractHttpServiceConfiguration
{

    private static final Log LOGGER = LogFactory.getLog( DefaultHttpServiceConfiguration.class );

    public DefaultHttpServiceConfiguration()
    {
        m_httpPort = 8080;
        m_httpSecurePort = 8443;
        m_httpEnabled = true;
        m_httpSecureEnabled = false;
        m_sslKeystore = System.getProperty( "user.home" ) + File.separator + ".keystore";
        m_sslPassword = null;
        m_sslKeyPassword = m_sslPassword;
        // create a temporary directory
        try
        {
            m_temporaryDirectory = File.createTempFile( ".paxweb", "" );
            m_temporaryDirectory.delete();
            m_temporaryDirectory = new File( m_temporaryDirectory.getAbsolutePath() );
            m_temporaryDirectory.mkdirs();
            m_temporaryDirectory.deleteOnExit();
        }
        catch( Exception e )
        {
            LOGGER.warn( "Could not create temporary directory. Reason: " + e.getMessage() );
            m_temporaryDirectory = null;
        }
    }

}
