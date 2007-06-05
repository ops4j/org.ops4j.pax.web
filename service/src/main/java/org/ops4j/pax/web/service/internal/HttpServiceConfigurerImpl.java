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

import org.ops4j.pax.web.service.HttpServiceConfigurer;
import org.ops4j.pax.web.service.HttpServiceConfiguration;

public class HttpServiceConfigurerImpl implements HttpServiceConfigurer
{
    private ServerController m_serverController;

    HttpServiceConfigurerImpl( final ServerController serverController )
    {
        if ( serverController == null )
        {
            throw new IllegalArgumentException( "httpServiceServer == null");
        }
        m_serverController = serverController;
    }

    public void configure( HttpServiceConfiguration configuration )
    {
        m_serverController.configure( configuration );
        if ( !m_serverController.isStarted() )
        {
            m_serverController.start();
        }
    }

    public HttpServiceConfiguration get()
    {
        return m_serverController.getConfiguration();
    }
}
