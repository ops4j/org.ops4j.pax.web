package org.ops4j.pax.web.service.internal;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.bio.SocketConnector;

class JettyFactoryImpl implements JettyFactory
{
    public JettyServer createServer()
    {
        return new JettyServerImpl();
    }

    public Connector createConnector( int port )
    {
        Connector connector = new SocketConnector();
        connector.setPort( port );
        return connector;
    }
}
