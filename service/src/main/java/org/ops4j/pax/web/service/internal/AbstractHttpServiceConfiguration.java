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
package org.ops4j.pax.web.service.internal;

import java.io.File;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class AbstractHttpServiceConfiguration
    implements HttpServiceConfiguration
{

    protected Integer m_httpPort;
    protected Integer m_httpSecurePort;
    protected Boolean m_httpEnabled;
    protected Boolean m_httpSecureEnabled;
    protected String m_sslKeystore;
    protected String m_sslPassword;
    protected String m_sslKeyPassword;
    protected File m_temporaryDirectory;

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

    /**
     * @see HttpServiceConfiguration#getSslKeystore()
     */
    public String getSslKeystore()
    {
        return m_sslKeystore;
    }

    /**
     * @see HttpServiceConfiguration#getSslPassword()
     */
    public String getSslPassword()
    {
        return m_sslPassword;
    }

    /**
     * @see HttpServiceConfiguration#getSslKeyPassword()
     */
    public String getSslKeyPassword()
    {
        return m_sslKeyPassword;
    }

    /**
     * @see HttpServiceConfiguration#getTemporaryDirectory()
     */
    public File getTemporaryDirectory()
    {
        return m_temporaryDirectory;
    }

    @Override
    public String toString()
    {
        return new StringBuilder()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpEnabled=" ).append( m_httpEnabled )
            .append( ", httpPort=" ).append( m_httpPort )
            .append( ", httpSecureEnabled=" ).append( m_httpSecureEnabled )
            .append( ", httpSecurePort=" ).append( m_httpSecurePort )
            .append( ", temporaryDirectory=" ).append( m_temporaryDirectory == null
                                                       ? "(not set)"
                                                       : m_temporaryDirectory.getAbsolutePath()
        )
            .append( "}" )
            .toString();
    }

}
