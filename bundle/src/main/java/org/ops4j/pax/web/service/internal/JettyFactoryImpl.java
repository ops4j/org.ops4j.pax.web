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
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.service.internal.model.ServerModel;

class JettyFactoryImpl
    implements JettyFactory
{

    private final ServerModel m_serverModel;

    JettyFactoryImpl( final ServerModel serverModel )
    {
        NullArgumentException.validateNotNull( serverModel, "Service model" );
        m_serverModel = serverModel;
    }

    public JettyServer createServer()
    {
        return new JettyServerImpl( m_serverModel );
    }

    public Connector createConnector( final int port, final String host ,boolean useNIO )
    {
        if(useNIO){
            SelectChannelConnector nioConnector=new NIOSocketConnectorWrapper();
            nioConnector.setHost(host);
            nioConnector.setPort(port);
            nioConnector.setUseDirectBuffers(true);
            return nioConnector;
        }else{
            Connector connector = new SocketConnectorWrapper();
            connector.setPort( port );
            connector.setHost( host );
            return connector;
        }
    }

    /**
     * @see JettyFactory#createSecureConnector(int,String,String,String,String)
     */
    public Connector createSecureConnector( int port, String sslKeystore, String sslPassword, String sslKeyPassword, String host, String sslKeystoreType, boolean isClientAuthNeeded, boolean isClientAuthWanted  )
    {
        SslSocketConnector connector = new SslSocketConnector();
        connector.setPort( port );
        connector.setKeystore( sslKeystore );
        connector.setPassword( sslPassword );
        connector.setKeyPassword( sslKeyPassword );
        connector.setHost( host );
        
        connector.setNeedClientAuth(isClientAuthNeeded);
        connector.setWantClientAuth(isClientAuthWanted);
        
        if(sslKeystoreType != null) {
            connector.setKeystoreType(sslKeystoreType);
        }
        return connector;
    }
}
