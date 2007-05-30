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
package org.ops4j.pax.web.service.internal.ng;

import org.osgi.framework.ServiceFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpServiceFactoryImpl implements ServiceFactory
{

    private static final Log m_logger = LogFactory.getLog( HttpServiceFactoryImpl.class );
    
    private HttpServiceServer m_httpServiceServer;

    public HttpServiceFactoryImpl(final HttpServiceServer httpServiceServer )
    {
        m_httpServiceServer = httpServiceServer;
    }

    public Object getService( final Bundle bundle, final ServiceRegistration serviceRegistration)
    {
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "binding bundle: [" + bundle + "] to http service");
        }
        return new HttpServiceImpl( bundle, new RegistrationRepositoryImpl(), m_httpServiceServer );
    }

    public void ungetService(final Bundle bundle, final ServiceRegistration serviceRegistration, final Object object) {
        // TODO automatically unregister from server of servletes & resources
        if( m_logger.isInfoEnabled() )
        {
            m_logger.info( "unbinding bundle: [" + bundle + "]");
        }
    }

}
