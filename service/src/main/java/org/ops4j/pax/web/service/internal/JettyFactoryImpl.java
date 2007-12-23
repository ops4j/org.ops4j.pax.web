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

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.ops4j.pax.web.service.internal.model.ServiceModel;

class JettyFactoryImpl
    implements JettyFactory
{

    private final ServiceModel m_serviceModel;

    JettyFactoryImpl( final ServiceModel serviceModel )
    {
        Assert.notNull( "Service Model cannot be null", serviceModel );
        m_serviceModel = serviceModel;
    }

    public JettyServer createServer()
    {
        return new JettyServerImpl( m_serviceModel );
    }

    public Connector createConnector( final int port )
    {
        Connector connector = new SocketConnectorWrapper();
        connector.setPort( port );
        return connector;
    }

    /**
     * @see JettyFactory#createSecureConnector(int,String,String,String)
     */
    public Connector createSecureConnector( int port, String sslKeystore, String sslPassword, String sslKeyPassword )
    {
        SslSocketConnector connector = new SslSocketConnector();
        connector.setPort( port );
        connector.setKeystore( sslKeystore );
        connector.setPassword( sslPassword );
        connector.setKeyPassword( sslKeyPassword );
        return connector;
    }
}
