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

import org.ops4j.pax.web.service.internal.AbstractHttpServiceConfiguration;
import org.ops4j.pax.web.service.internal.DelegatingHttpServiceConfiguration;

public class SimpleHttpServiceConfiguration extends DelegatingHttpServiceConfiguration
{
    
    public SimpleHttpServiceConfiguration()
    {
        this( null );
    }

    public SimpleHttpServiceConfiguration( final HttpServiceConfiguration httpServiceConfiguration )
    {
        super( httpServiceConfiguration );
    }

    public void copyFrom( final HttpServiceConfiguration httpServiceConfiguration )
    {
        setHttpPort( httpServiceConfiguration.getHttpPort() );
        setHttpSecurePort( httpServiceConfiguration.getHttpSecurePort() );
        setHttpEnabled( httpServiceConfiguration.isHttpEnabled() );
        setHttpSecureEnabled( httpServiceConfiguration.isHttpSecureEnabled() );
    }

    public void setHttpPort( final Integer httpPort )
    {
        m_httpPort = httpPort;
    }

    public void setHttpSecurePort( final Integer httpSecurePort )
    {
        m_httpSecurePort = httpSecurePort;
    }

    public void setHttpEnabled( final Boolean httpEnabled )
    {
        m_httpEnabled = httpEnabled;
    }

    public void setHttpSecureEnabled( final Boolean httpSecureEnabled )
    {
        m_httpSecureEnabled = httpSecureEnabled;
    }    
}
