package org.ops4j.pax.web.service.internal;

import org.mortbay.jetty.Connector;

interface JettyFactory
{
    JettyServer createServer();
    Connector createConnector(int port);
}
